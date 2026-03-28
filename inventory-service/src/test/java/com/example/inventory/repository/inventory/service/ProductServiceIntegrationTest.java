package com.example.inventory.repository.inventory.service;

import com.example.inventory.repository.inventory.dto.CreateProductRequest;
import com.example.inventory.repository.inventory.dto.UpdateStockRequest;
import com.example.inventory.repository.inventory.exception.InvalidReserveAmountException;
import com.example.inventory.repository.inventory.exception.ProductNotFoundException;
import com.example.inventory.repository.inventory.repository.ProductRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.annotation.Transactional;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest
@Testcontainers
@Transactional
@DisplayName("ProductService + PostgreSQL — Integration Tests")
class ProductServiceIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("inventory_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired ProductService    productService;
    @Autowired ProductRepository productRepository;

    // ══════════════════════════════════════════════════════════════════════════
    // createProduct
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("crear producto persiste en PostgreSQL con todos los campos")
    void createProduct_persistsToDatabase() {
        var response = productService.createProduct(new CreateProductRequest("Laptop", 10));

        var found = productRepository.findById(response.id()).orElseThrow();
        assertThat(found.getName()).isEqualTo("Laptop");
        assertThat(found.getStock()).isEqualTo(10);
    }

    @Test
    @DisplayName("crear producto sin stock → stock 0 por defecto")
    void createProduct_nullStock_defaultsToZero() {
        var response = productService.createProduct(new CreateProductRequest("Mouse", null));

        var found = productRepository.findById(response.id()).orElseThrow();
        assertThat(found.getStock()).isEqualTo(0);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // reserveStock
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("reservar stock válido → stock reducido en DB")
    void reserveStock_reducesStockInDatabase() {
        var product = productService.createProduct(new CreateProductRequest("Laptop", 10));

        productService.reserveStock(product.id(), new UpdateStockRequest(3));

        var found = productRepository.findById(product.id()).orElseThrow();
        assertThat(found.getStock()).isEqualTo(7);
    }

    @Test
    @DisplayName("reservar más del stock disponible → excepción, stock no cambia")
    void reserveStock_insufficient_throwsAndDoesNotPersist() {
        var product = productService.createProduct(new CreateProductRequest("Laptop", 5));

        assertThatThrownBy(() ->
                productService.reserveStock(product.id(), new UpdateStockRequest(99))
        ).isInstanceOf(InvalidReserveAmountException.class);

        var found = productRepository.findById(product.id()).orElseThrow();
        assertThat(found.getStock()).isEqualTo(5);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // releaseStock
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("liberar stock → stock aumentado en DB")
    void releaseStock_increasesStockInDatabase() {
        var product = productService.createProduct(new CreateProductRequest("Laptop", 10));

        productService.releaseStock(product.id(), new UpdateStockRequest(5));

        var found = productRepository.findById(product.id()).orElseThrow();
        assertThat(found.getStock()).isEqualTo(15);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findById
    // ══════════════════════════════════════════════════════════════════════════

    @Test
    @DisplayName("buscar producto inexistente → ProductNotFoundException")
    void findById_nonExistent_throwsNotFoundException() {
        assertThatThrownBy(() -> productService.findById(999_999L))
                .isInstanceOf(ProductNotFoundException.class)
                .hasMessageContaining("999999");
    }
}
