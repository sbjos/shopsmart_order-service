package com.shopsmart.orderservice.model;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.Persistent;

import java.util.List;

@Entity
@Table(name = "orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Order {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;
    private String orderNumber;

    @OneToMany(cascade = CascadeType.ALL)
    private List<ItemList> itemList;
}
