# Distributed System Design

## 1. Architecture (Service Split)

```text
[Client]
   |
   v
[API Gateway / Spring Security]
   |
   +--> User Service      (register, login, profile)
   +--> Product Service   (create/list product)
   +--> Inventory Service (stock query, stock deduct)
   +--> Order Service     (create order, query my orders)
            |
            +--> MySQL
```

Current implementation is modular-monolith to speed up delivery. The modules are already split by domain and can be extracted to independent services later.

## 2. Service Responsibilities

- User Service: account creation, credential validation, JWT issuing.
- Product Service: product info management.
- Inventory Service: stock management and deduction.
- Order Service: transactional order creation with inventory deduction.

## 3. Main Flow (Place Order)

1. Client sends JWT to create order endpoint.
2. Order Service resolves current user by JWT subject.
3. Product and inventory are validated.
4. Inventory is deducted with conditional SQL (`stock >= quantity`).
5. Order and order item are persisted in one transaction.

