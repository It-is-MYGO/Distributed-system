# Container Deployment, LB, Static Separation, Cache

## 1) Start all services

```powershell
docker compose up -d --build
```

Services:

- MySQL: `localhost:3306`
- Redis: `localhost:6379`
- Nginx: `http://localhost:80`
- App instances: `app1:8081`, `app2:8082` (internal network)

## 2) Verify load balancing

- Round-robin API entry: `/api/*`
- Least-connection API entry: `/api-lc/*`

Check backend logs:

```powershell
docker compose logs -f app1 app2
```

You will see logs like:
`instance=app-1 count=... uri=/api/product status=200`

## 3) Dynamic/static separation

- Static pages are served by Nginx from `nginx/html`.
- API requests are proxied by Nginx to backend upstream.
- Open:
  - `http://localhost/` (login page)
  - `http://localhost/product.html` (product page)

## 4) Redis cache strategy for product detail

Endpoint:

- `GET /api/product/{id}` (or `/api-lc/product/{id}`)

Implemented:

- Cache penetration: cache null placeholder for non-existing product ID.
- Cache breakdown: short lock key to avoid hot-key DB stampede.
- Cache avalanche: random TTL jitter.

## 5) JMeter pressure test

### 5.1 Static file pressure test

1. Add **Thread Group**
2. Add **HTTP Request**
   - Protocol: `http`
   - Server Name: `localhost`
   - Port: `80`
   - Path: `/product.html`
3. Add **View Results Tree** and **Summary Report**
4. Run and check average latency.

### 5.2 Backend API pressure test

1. Login once in browser and copy JWT token.
2. Add **HTTP Header Manager**:
   - `Authorization: Bearer <token>`
   - `Content-Type: application/json`
3. Request target examples:
   - Round-robin: `GET /api/product`
   - Least-conn: `GET /api-lc/product`
4. Compare TPS/avg response time between static and dynamic requests.

### 5.3 Check request distribution

During API pressure run:

```powershell
docker compose logs --since=5m app1 app2
```

Count `instance=app-1` and `instance=app-2` lines. For round-robin they should be close.

