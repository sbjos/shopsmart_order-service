# Order Service

---
A microservice of Shopsmart application responsible to manage orders.

This service makes a call to the inventory-service to verify if items are available before
confirming the order. If it is not available, it will return the list of items that are not available.

Once order is confirmed, a notification is sent to the user.

- Build in Java 17.
- Spring boot with webflux
- Connects to eureka server handled by discover-service
- circuitbreaker with resilience4j
- Tracing with micrometer with zipkin
- kafka for async comm - notification. 
- Handles POJO using lombok.