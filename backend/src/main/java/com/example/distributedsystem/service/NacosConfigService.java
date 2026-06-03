package com.example.distributedsystem.service;

import com.example.distributedsystem.entity.GovernanceConfig;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

@Service
public class NacosConfigService {
    private static final String DATA_ID = "mall-governance.properties";
    private static final String GROUP = "DEFAULT_GROUP";

    private final HttpClient httpClient = HttpClient.newHttpClient();
    private final String serverAddr;

    public NacosConfigService(@Value("${app.nacos.server-addr:http://localhost:8848}") String serverAddr) {
        this.serverAddr = normalize(serverAddr);
    }

    public Map<String, Object> publish(List<GovernanceConfig> configs) {
        StringBuilder content = new StringBuilder();
        for (GovernanceConfig config : configs) {
            content.append(config.getConfigKey()).append('=').append(config.getConfigValue()).append('\n');
        }
        String body = "dataId=" + encode(DATA_ID)
                + "&group=" + encode(GROUP)
                + "&content=" + encode(content.toString());
        HttpRequest request = HttpRequest.newBuilder(URI.create(serverAddr + "/nacos/v1/cs/configs"))
                .header("Content-Type", "application/x-www-form-urlencoded;charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();
        String response = send(request);
        return Map.of("server", serverAddr, "dataId", DATA_ID, "group", GROUP, "published", "true".equalsIgnoreCase(response), "raw", response);
    }

    public Map<String, String> pull() {
        String url = serverAddr + "/nacos/v1/cs/configs?dataId=" + encode(DATA_ID) + "&group=" + encode(GROUP);
        HttpRequest request = HttpRequest.newBuilder(URI.create(url)).GET().build();
        String body = send(request);
        Properties properties = new Properties();
        try {
            properties.load(new java.io.StringReader(body));
        } catch (IOException e) {
            throw new IllegalStateException("Nacos 配置解析失败", e);
        }
        Map<String, String> values = new LinkedHashMap<>();
        for (String name : properties.stringPropertyNames()) {
            values.put(name, properties.getProperty(name));
        }
        return values;
    }

    public Map<String, Object> metadata() {
        return Map.of("server", serverAddr, "dataId", DATA_ID, "group", GROUP);
    }

    private String send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() >= 400) {
                throw new IllegalStateException("Nacos 请求失败：" + response.statusCode() + " " + response.body());
            }
            return response.body();
        } catch (IOException e) {
            throw new IllegalStateException("无法连接 Nacos：" + serverAddr, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("Nacos 请求被中断", e);
        }
    }

    private String normalize(String value) {
        String address = value == null || value.isBlank() ? "http://localhost:8848" : value.trim();
        return address.startsWith("http://") || address.startsWith("https://") ? address : "http://" + address;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
