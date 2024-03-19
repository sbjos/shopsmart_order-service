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
    public ResponseEntity<?> placeOrder(@RequestBody OrderRequest orderRequest) {
        OrderResponse response = orderService.placeOrder(orderRequest).join();

        if (response.getOrderItemList() == null)
            return new ResponseEntity<>(response, HttpStatus.CONFLICT);
        return new ResponseEntity<>(response, HttpStatus.CREATED);
    }

    @GetMapping
    public ResponseEntity<?> getAllOrder() {
        List<OrderResponse> response = orderService.getAllOrder();

        if (response.isEmpty())
            return new ResponseEntity<>("Order list not found", HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @GetMapping(value = "/{orderNumber}")
    public ResponseEntity<?> getOrder(@PathVariable String orderNumber) {
        OrderResponse response = orderService.getOrder(orderNumber);

        if (response.getId() == null)
            return new ResponseEntity<>(String.format("Order %s not found", orderNumber),
                    HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }

    @DeleteMapping(value = "/{orderNumber}")
    public ResponseEntity<?> cancelOrder(@PathVariable String orderNumber) {
        OrderResponse response = orderService.cancelOrder(orderNumber);

        if (response.getId() == null)
            return new ResponseEntity<>(String.format("Order %s not found", orderNumber),
                    HttpStatus.NOT_FOUND);
        return new ResponseEntity<>(response, HttpStatus.OK);
    }
}
