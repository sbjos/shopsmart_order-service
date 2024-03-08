package com.shopsmart.orderservice;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.shopsmart.orderservice.dto.OrderItemListDto;
import com.shopsmart.orderservice.dto.OrderRequest;
import com.shopsmart.orderservice.repository.OrderRepository;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.math.BigDecimal;
import java.util.List;

import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

// Integration Test
// Run inventory-service for successful test
@SpringBootTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@AutoConfigureMockMvc
class OrderServiceApplicationTests {
	@Autowired
	private MockMvc mockMvc;
	@Autowired
	private ObjectMapper objectMapper;
	@Autowired
	private OrderRepository orderRepository;

	@Test
	@Order(1)
	void placeOrder() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.post("/api/order")
						.contentType(MediaType.APPLICATION_JSON)
						.content(orders()))
				.andExpect(status().isCreated());
	}

	@Test
	@Order(2)
	void getAllOrder() throws Exception {
		mockMvc.perform(MockMvcRequestBuilders.get("/api/order")
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	@Order(3)
	void getOrder() throws Exception {
		Long id = orderRepository.findAll().get(0).getId();

		mockMvc.perform(MockMvcRequestBuilders.get("/api/order/{id}", id)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	@Test
	@Order(4)
	void cancelOrder() throws Exception {
		Long id = orderRepository.findAll().get(0).getId();

		mockMvc.perform(MockMvcRequestBuilders.delete("/api/order/{id}", id)
						.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isOk());
	}

	private String orders() throws JsonProcessingException {
		OrderItemListDto orderItemListDto1 = new OrderItemListDto();
		OrderItemListDto orderItemListDto2 = new OrderItemListDto();

		orderItemListDto1.setId(7L);
		orderItemListDto1.setSkuCode("Test Sku");
		orderItemListDto1.setQuantity(25);
		orderItemListDto1.setPrice(BigDecimal.valueOf(250));

		orderItemListDto2.setId(5L);
		orderItemListDto2.setSkuCode("Test Sku2");
		orderItemListDto2.setQuantity(12);
		orderItemListDto2.setPrice(BigDecimal.valueOf(1245));

		List<OrderItemListDto> orderList = List.of(
				orderItemListDto1,
				orderItemListDto2
		);

		return objectMapper.writeValueAsString(OrderRequest.builder()
				.orderItemListDto(orderList)
				.build()
		);
	}
}
