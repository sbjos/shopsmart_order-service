package com.shopsmart.orderservice.dto;

import com.shopsmart.orderservice.model.ItemList;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class OrderRequest {
    private List<ItemListDto> itemListDto;
}
