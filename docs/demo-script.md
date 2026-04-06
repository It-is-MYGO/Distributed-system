# Demo Script (End-to-End)

## 1) Start app

```powershell
mvn -DskipTests spring-boot:run
```

## 2) Register and copy token

```powershell
curl -X POST http://localhost:8080/api/user/register `
  -H "Content-Type: application/json" `
  -d "{\"username\":\"alice\",\"password\":\"123456\"}"
```

## 3) Create product with stock

```powershell
curl -X POST http://localhost:8080/api/product `
  -H "Authorization: Bearer <TOKEN>" `
  -H "Content-Type: application/json" `
  -d "{\"name\":\"demo-product\",\"price\":99.90,\"stock\":20}"
```

## 4) Place order

```powershell
curl -X POST http://localhost:8080/api/order `
  -H "Authorization: Bearer <TOKEN>" `
  -H "Content-Type: application/json" `
  -d "{\"productId\":1,\"quantity\":2}"
```

## 5) Query my orders

```powershell
curl -X GET http://localhost:8080/api/order/mine `
  -H "Authorization: Bearer <TOKEN>"
```

