package com.shopsmart.orderservice.service;

import brave.Span;
import brave.Tracer;
import com.shopsmart.orderservice.dto.InventoryResponse;
import com.shopsmart.orderservice.dto.OrderItemListDto;
import com.shopsmart.orderservice.dto.OrderRequest;
import com.shopsmart.orderservice.dto.OrderResponse;
import com.shopsmart.orderservice.exception.OrderNotFoundException;
import com.shopsmart.orderservice.exception.OutOfStockException;
import com.shopsmart.orderservice.model.OrderItemList;
import com.shopsmart.orderservice.model.Order;
import com.shopsmart.orderservice.repository.OrderRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.github.resilience4j.timelimiter.annotation.TimeLimiter;
import io.micrometer.observation.Observation;
import io.micrometer.observation.ObservationRegistry;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final WebClient.Builder webclientBuilder;
    private final OrderRepository orderRepository;
    private final ObservationRegistry observationRegistry;

    @CircuitBreaker(name = "inventory", fallbackMethod = "fallbackMethod")
    @TimeLimiter(name = "inventory")
    @Retry(name = "inventory")
    public CompletableFuture<OrderResponse> placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderItemList(orderRequest.getOrderItemListDto().stream()
                .map(this::mapFromDto)
                .collect(Collectors.toList())
        );

        Map<String, Integer> orderSkuStockMap = new HashMap<>();

        for (OrderItemList orderItems : order.getOrderItemList()) {
            orderSkuStockMap.put(orderItems.getSkuCode(), orderItems.getQuantity());
        }

        // Calling inventory-service to verify if the product is in stock before creating the order.
        log.info("Calling inventory-service");

        Observation inventoryServiceObservation = Observation.createNotStarted("inventory-service-lookup",
                this.observationRegistry);

        inventoryServiceObservation.lowCardinalityKeyValue("call", "inventory-service");
        return inventoryServiceObservation.observe(() -> {
            InventoryResponse[] inventoryList = inventoryList(orderSkuStockMap);

            // Verifying if they are all available.
            boolean isProductInStock = Arrays.stream(inventoryList)
                    .allMatch(inventory ->
                            inventory.getInStock() >= orderSkuStockMap.get(
                                    inventory.getSkuCode()
                            )
                    );

            // If all available, order is saved.
            if (isProductInStock) {
                orderRepository.save(order);
                log.info("Order created");
                return CompletableFuture.supplyAsync(() -> mapToOrderResponse(order));
            } else {
                // If not all available, order will not be saved.
                log.info("Some items are out of stock.", new OutOfStockException("Out of stock"));
                return CompletableFuture.supplyAsync(() ->
                        outOfStockItems(List.of(inventoryList), orderSkuStockMap));
            }
        });
    }

    public List<OrderResponse> getAllOrder() {
        log.info("Fetching order list");
        List<Order> orderList = orderRepository.findAll();

        if (!orderList.isEmpty()) {
            log.info("Order list found");
            return orderList.stream()
                    .map(this::mapToOrderResponse)
                    .toList();

        } else {
            log.info("Order list not found", new OrderNotFoundException("Order list not found"));
        }
        return List.of();
    }

    public OrderResponse getOrder(String orderNumber) {
        OrderResponse response = new OrderResponse();

        try {
            response = mapToOrderResponse(findOrder(orderNumber));
            log.info("Order {} found", orderNumber);

        } catch (OrderNotFoundException e) {
            log.info("Order {} not found", orderNumber, e);
        }
        return response;
    }

    public OrderResponse cancelOrder(String orderNumber) {
        OrderResponse response = new OrderResponse();

        try {
            Order order = findOrder(orderNumber);
            log.info("Order {} found", orderNumber);
            orderRepository.delete(order);
            log.info("Order {} deleted", orderNumber);
            response = mapToOrderResponse(order);

        } catch (OrderNotFoundException e) {
            log.info("Order {} not found", orderNumber, e);
        }
        return response;
    }

    private Order findOrder(String orderNumber) {
        log.info("Fetching order {}", orderNumber);
        return orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() ->
                        new OrderNotFoundException(String.format("Order %s not found", orderNumber))
                );
    }

    private OrderItemList mapFromDto(OrderItemListDto orderItemListDto) {
        OrderItemList orderItemList = new OrderItemList();

        orderItemList.setQuantity(orderItemListDto.getQuantity());
        orderItemList.setPrice(orderItemListDto.getPrice());
        orderItemList.setSkuCode(orderItemListDto.getSkuCode());

        return orderItemList;
    }

    private OrderResponse mapToOrderResponse(Order order) {
        return OrderResponse.builder()
                .id(order.getId())
                .orderNumber(order.getOrderNumber())
                .orderItemList(order.getOrderItemList())
                .build();
    }

    // Adds out of stock items to a list
    private OrderResponse outOfStockItems(List<InventoryResponse> inventory, Map<String, Integer> request) {
        List<InventoryResponse> outOfStockItems = new ArrayList<>();

        for (InventoryResponse response : inventory) {
            if (response.getInStock() < request.get(response.getSkuCode())) {
                outOfStockItems.add(response);
            }
        }
        return OrderResponse.builder().outOfStock(outOfStockItems).build();
    }

    // Finds available stocks in inventory-service
    private InventoryResponse[] inventoryList(Map<String, Integer> orderSkuStockMap) {
        return webclientBuilder.build().get()
                .uri("http://inventory-service/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode",
                                orderSkuStockMap.keySet()).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block(); // Will do a synchronous request. If not, it will perform an asynchronous request.
    }

    // Fallback method for connection issues with inventory-service
    private CompletableFuture<OrderResponse> fallbackMethod(OrderRequest orderRequest, RuntimeException RuntimeException) {
        log.info("Investigate inventory-service integrity");
        throw new RuntimeException("Something went wrong");
    }
}
