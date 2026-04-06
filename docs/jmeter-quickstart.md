# JMeter Quickstart

## 1. Prepare

- Ensure services are up:
  - `docker compose up -d --build`
- Open browser:
  - `http://localhost/`
- Register/Login once and copy JWT token.

## 2. Static pressure test

1. Open JMeter
2. `File -> Open` and choose `jmeter/static-load-test.jmx`
3. Click **Start**
4. Check `Summary Report`:
   - Avg
   - Throughput
   - Error %

## 3. API pressure test

1. Open `jmeter/api-load-test.jmx`
2. In Test Plan variables, change:
   - `token=你的JWT`
   - `apiBase=/api` (round robin) or `apiBase=/api-lc` (least_conn)
3. Start test and compare results.

## 4. Verify load balancing distribution

Run while test is active:

```powershell
docker compose logs --since=3m app1 app2
```

Observe `instance=app-1` and `instance=app-2` counts from `RequestLogFilter` logs.

