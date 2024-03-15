package com.shopsmart.orderservice.controller;

import com.shopsmart.orderservice.dto.OrderRequest;
import com.shopsmart.orderservice.dto.OrderResponse;
import com.shopsmart.orderservice.service.OrderService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping(value = "/api/order")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @PostMapping
    public ResponseEntity<OrderResponse> placeOrder(@RequestBody OrderRequest orderRequest) {
        OrderResponse order = orderService.placeOrder(orderRequest).join();

        if (order.getOrderItemList() == null)
            return new ResponseEntity<>(order, HttpStatus.OK);
        return new ResponseEntity<>(order, HttpStatus.CREATED);
    }

    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    public List<OrderResponse> getAllOrder() {
        return orderService.getAllOrder();
    }

    @GetMapping(value = "/{orderNumber}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse getOrder(@PathVariable String orderNumber) {
        return orderService.getOrder(orderNumber);
    }

    @DeleteMapping(value = "/{orderNumber}")
    @ResponseStatus(HttpStatus.OK)
    public OrderResponse cancelOrder(@PathVariable String orderNumber) {
        return orderService.cancelOrder(orderNumber);
    }
}
