package com.example.inventory.repository.inventory.controller;

import com.example.inventory.repository.inventory.dto.CreateProductRequest;
import com.example.inventory.repository.inventory.dto.ProductResponse;
import com.example.inventory.repository.inventory.dto.UpdateStockRequest;
import com.example.inventory.repository.inventory.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @PostMapping
    public ResponseEntity<ProductResponse> createProduct(
            @Valid @RequestBody CreateProductRequest req,
            UriComponentsBuilder uriBuilder) {

        ProductResponse response = productService.createProduct(req);

        URI location = uriBuilder
                .path("/api/v1/products/{id}")
                .buildAndExpand(response.id())
                .toUri();

        return ResponseEntity.created(location).body(response);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        return ResponseEntity.ok(productService.findById(id));
    }

    @GetMapping
    public Page<ProductResponse> listProducts(
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        var pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        return productService.findAll(pageable);
    }

    @PatchMapping("/{id}/reserve")
    public ResponseEntity<ProductResponse> reserveStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest req) {

        return ResponseEntity.ok(productService.reserveStock(id, req));
    }

    @PatchMapping("/{id}/release")
    public ResponseEntity<ProductResponse> releaseStock(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStockRequest req) {

        return ResponseEntity.ok(productService.releaseStock(id, req));
    }
}
