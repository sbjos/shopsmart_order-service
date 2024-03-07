package com.shopsmart.orderservice.service;

import com.shopsmart.orderservice.config.WebConfig;
import com.shopsmart.orderservice.dto.OrderItemListDto;
import com.shopsmart.orderservice.dto.OrderRequest;
import com.shopsmart.orderservice.dto.OrderResponse;
import com.shopsmart.orderservice.exception.OrderNotFoundException;
import com.shopsmart.orderservice.model.OrderItemList;
import com.shopsmart.orderservice.model.Order;
import com.shopsmart.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
                .toList()
        );

        // Calling inventory-service to verify if the product is in stock before creating the order.
        boolean productInStock = webClient.get()
                .uri("http://localhost:8082/api/inventory")
                .retrieve()
                .bodyToMono(Boolean.class)
                .block(); // Will do a synchronous request. If not, it will perform an asynchronous request.

        orderRepository.save(order);

        log.info("{} is saved", order.getOrderNumber());
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
