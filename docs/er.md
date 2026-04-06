# ER Design

## Entities

- `user_account`
  - id (PK)
  - username (UNIQUE)
  - password
  - created_at
- `product`
  - id (PK)
  - name
  - price
  - created_at
- `inventory`
  - id (PK)
  - product_id (UNIQUE, FK -> product.id)
  - stock
  - updated_at
- `orders`
  - id (PK)
  - user_id (FK -> user_account.id)
  - total_amount
  - status
  - created_at
- `order_item`
  - id (PK)
  - order_id (FK -> orders.id)
  - product_id (FK -> product.id)
  - quantity
  - unit_price

## Relationship

- user_account 1 --- n orders
- orders 1 --- n order_item
- product 1 --- 1 inventory
- product 1 --- n order_item

