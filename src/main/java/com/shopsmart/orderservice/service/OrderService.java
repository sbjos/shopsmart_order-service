package com.shopsmart.orderservice.service;

import com.shopsmart.orderservice.dto.InventoryResponse;
import com.shopsmart.orderservice.dto.OrderItemListDto;
import com.shopsmart.orderservice.dto.OrderRequest;
import com.shopsmart.orderservice.dto.OrderResponse;
import com.shopsmart.orderservice.exception.OrderNotFoundException;
import com.shopsmart.orderservice.exception.OutOfStockException;
import com.shopsmart.orderservice.model.OrderItemList;
import com.shopsmart.orderservice.model.Order;
import com.shopsmart.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class OrderService {

    private final WebClient webClient;

    private final OrderRepository orderRepository;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();
        order.setOrderNumber(UUID.randomUUID().toString());
        order.setOrderItemList(orderRequest.getOrderItemListDto().stream()
                .map(this::mapFromDto)
                .collect(Collectors.toList())
        );

        Map<String, Integer> orderSkuStocktMap = new HashMap<>();

        for (OrderItemList orderItems : order.getOrderItemList()) {
            orderSkuStocktMap.put(orderItems.getSkuCode(), orderItems.getQuantity());
        }

        // Calling inventory-service to verify if the product is in stock before creating the order.
        InventoryResponse[] inventoryList = webClient.get()
                .uri("http://localhost:8082/api/inventory",
                        uriBuilder -> uriBuilder.queryParam("skuCode",orderSkuStocktMap.keySet()).build())
                .retrieve()
                .bodyToMono(InventoryResponse[].class)
                .block(); // Will do a synchronous request. If not, it will perform an asynchronous request.

        // Verifying if they are all available.
        boolean isProductInStock = Arrays.stream(inventoryList)
                .allMatch(inventory -> inventory.getInStock() >= orderSkuStocktMap.get(inventory.getSkuCode()));

//        for (InventoryResponse response : inventoryList) {
//            isProductInStock = orderSkuStocktMap.get(response.getSkuCode()) >= response.getInStock();
//        }

        // If all available, order is saved.
        if (isProductInStock) {
            orderRepository.save(order);

            log.info("{} is saved", order.getOrderNumber());
        } else {
            // If not all available, order will not be saved.
            throw new OutOfStockException("Product out of stock");
        }
    }

    public List<OrderResponse> getAllOrder() {
        List<Order> orderList = orderRepository.findAll();

        if (!orderList.isEmpty()) {
            return orderList.stream()
                    .map(this::mapToOrderResponse)
                    .toList();
        } else {
            throw new OrderNotFoundException("Product list not found");
        }
    }

    public OrderResponse getOrder(Long id) {
        return mapToOrderResponse(findOrder(id));
    }

    public void cancelOrder(Long id) {
        Order order = findOrder(id);
        orderRepository.delete(order);
    }

    private Order findOrder(Long id) {
        return orderRepository.findById(id)
                .orElseThrow(() ->
                        new OrderNotFoundException(String.format("Order %s not found", id))
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
}
