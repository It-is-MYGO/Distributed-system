package com.example.distributedsystem.controller;

import com.example.distributedsystem.dto.ProductCreateRequest;
import com.example.distributedsystem.entity.Product;
import com.example.distributedsystem.entity.ProductDetailImage;
import com.example.distributedsystem.entity.ProductReview;
import com.example.distributedsystem.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/product")
public class ProductController {
    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<Product> create(@Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(productService.create(request));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Product> update(@PathVariable Long id, @Valid @RequestBody ProductCreateRequest request) {
        return ResponseEntity.ok(productService.update(id, request));
    }

    @GetMapping
    public ResponseEntity<List<Product>> list() {
        return ResponseEntity.ok(productService.list());
    }

    @GetMapping("/search")
    public ResponseEntity<Page<Product>> search(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "default") String orderBy) {
        return ResponseEntity.ok(productService.search(keyword, page, size, orderBy));
    }

    @GetMapping("/recommend/similar")
    public ResponseEntity<List<Product>> recommend(
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) List<Long> excludeIds,
            @RequestParam(defaultValue = "6") int limit) {
        return ResponseEntity.ok(productService.recommendSimilar(categoryId, excludeIds, limit));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Product> detail(@PathVariable Long id) {
        Product product = productService.getDetail(id);
        if (product == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(product);
    }

    @GetMapping("/{id}/images")
    public ResponseEntity<List<ProductDetailImage>> images(@PathVariable Long id) {
        return ResponseEntity.ok(productService.listImages(id));
    }

    @GetMapping("/{id}/reviews")
    public ResponseEntity<List<ProductReview>> reviews(@PathVariable Long id) {
        return ResponseEntity.ok(productService.listReviews(id));
    }
}
