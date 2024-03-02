package com.shopsmart.orderservice.service;

import com.shopsmart.orderservice.dto.ItemListDto;
import com.shopsmart.orderservice.dto.OrderRequest;
import com.shopsmart.orderservice.model.ItemList;
import com.shopsmart.orderservice.model.Order;
import com.shopsmart.orderservice.repository.OrderRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;

    public void placeOrder(OrderRequest orderRequest) {
        Order order = new Order();

        order.setOrderNumber(UUID.randomUUID().toString());
        order.setItemList(orderRequest.getItemListDto().stream()
                .map(this::mapToDto)
                .collect(Collectors.toList())
        );

        orderRepository.save(order);
    }

    private ItemList mapToDto(ItemListDto itemListDto) {
        ItemList itemList = new ItemList();

        itemList.setQuantity(itemListDto.getQuantity());
        itemList.setPrice(itemListDto.getPrice());
        itemList.setSkuCode(itemListDto.getSkuCode());

        return itemList;
    }
}
