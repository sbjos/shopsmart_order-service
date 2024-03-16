package com.shopsmart.orderservice.event;

import com.shopsmart.orderservice.dto.InventoryResponse;
import com.shopsmart.orderservice.model.OrderItemList;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class OrderPlacedEvent {
    private Long id;
    private String orderNumber;
    private List<OrderItemList> orderItemList;
    private List<InventoryResponse> outOfStock;
}
