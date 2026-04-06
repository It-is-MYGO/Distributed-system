# API Spec (RESTful)

## User Service

- `POST /api/user/register`
  - body: `{ "username": "alice", "password": "123456" }`
  - response: `{ "message": "注册成功", "token": "jwt..." }`

- `POST /api/user/login`
  - body: `{ "username": "alice", "password": "123456" }`
  - response: `{ "message": "登录成功", "token": "jwt..." }`

- `GET /api/user/me` (JWT required)
  - header: `Authorization: Bearer <token>`
  - response: current user object

## Product + Inventory Service

- `POST /api/product` (JWT required)
  - body: `{ "name": "iPhone", "price": 6999.00, "stock": 100 }`
  - response: created product

- `GET /api/product` (JWT required)
  - response: product list

## Order Service

- `POST /api/order` (JWT required)
  - body: `{ "productId": 1, "quantity": 2 }`
  - response: created order

- `GET /api/order/mine` (JWT required)
  - response: current user's orders

