package com.example.inventory.repository.inventory.controller;

import com.example.inventory.repository.inventory.dto.CreateProductRequest;
import com.example.inventory.repository.inventory.dto.ProductResponse;
import com.example.inventory.repository.inventory.dto.UpdateStockRequest;
import com.example.inventory.repository.inventory.exception.GlobalExceptionHandler;
import com.example.inventory.repository.inventory.exception.InvalidReserveAmountException;
import com.example.inventory.repository.inventory.exception.ProductNotFoundException;
import com.example.inventory.repository.inventory.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = {ProductController.class, GlobalExceptionHandler.class})
@DisplayName("ProductController — MockMvc Tests")
class ProductControllerTest {

    @Autowired MockMvc     mockMvc;
    @Autowired ObjectMapper mapper;

    @MockBean ProductService productService;

    private ProductResponse sampleResponse() {
        return new ProductResponse(1L, "Laptop", new BigDecimal("10.00"), Instant.now(), Instant.now());
    }

    // ══════════════════════════════════════════════════════════════════════════
    // POST /api/v1/products
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("POST /api/v1/products")
    class CreateProductTests {

        @Test
        @DisplayName("request válido → 201 Created + Location header + body")
        void validRequest_returns201WithLocation() throws Exception {
            when(productService.createProduct(any())).thenReturn(sampleResponse());

            var req = new CreateProductRequest("Laptop", new BigDecimal("10.00"));

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "http://localhost/api/v1/products/1"))
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Laptop"))
                    .andExpect(jsonPath("$.stock").value(10.00));
        }

        @Test
        @DisplayName("name nulo → 400 Bad Request")
        void missingName_returns400() throws Exception {
            var req = new CreateProductRequest(null, new BigDecimal("10.00"));

            mockMvc.perform(post("/api/v1/products")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(req)))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.title").value("Validation Error"))
                    .andExpect(jsonPath("$.errors[0]").value("name: name es requerido"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // GET /api/v1/products/{id}
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("GET /api/v1/products/{id}")
    class GetProductTests {

        @Test
        @DisplayName("producto existente → 200 OK con body")
        void existingProduct_returns200() throws Exception {
            when(productService.findById(1L)).thenReturn(sampleResponse());

            mockMvc.perform(get("/api/v1/products/1"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.id").value(1))
                    .andExpect(jsonPath("$.name").value("Laptop"))
                    .andExpect(jsonPath("$.stock").value(10.00));
        }

        @Test
        @DisplayName("producto inexistente → 404 Not Found")
        void missingProduct_returns404() throws Exception {
            when(productService.findById(99L)).thenThrow(new ProductNotFoundException(99L));

            mockMvc.perform(get("/api/v1/products/99"))
                    .andExpect(status().isNotFound())
                    .andExpect(jsonPath("$.title").value("Product Not Found"))
                    .andExpect(jsonPath("$.status").value(404));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PATCH /api/v1/products/{id}/reserve
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PATCH /api/v1/products/{id}/reserve")
    class ReserveStockTests {

        @Test
        @DisplayName("reserva válida → 200 OK con stock actualizado")
        void validReserve_returns200() throws Exception {
            var updated = new ProductResponse(1L, "Laptop", new BigDecimal("7.00"), Instant.now(), Instant.now());
            when(productService.reserveStock(eq(1L), any())).thenReturn(updated);

            mockMvc.perform(patch("/api/v1/products/1/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(new UpdateStockRequest(new BigDecimal("3.00")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock").value(7.00));
        }

        @Test
        @DisplayName("stock insuficiente → 409 Conflict")
        void insufficientStock_returns409() throws Exception {
            when(productService.reserveStock(eq(1L), any()))
                    .thenThrow(new InvalidReserveAmountException("Stock insuficiente"));

            mockMvc.perform(patch("/api/v1/products/1/reserve")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(new UpdateStockRequest(new BigDecimal("99.00")))))
                    .andExpect(status().isConflict())
                    .andExpect(jsonPath("$.title").value("Insufficient Stock"));
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // PATCH /api/v1/products/{id}/release
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("PATCH /api/v1/products/{id}/release")
    class ReleaseStockTests {

        @Test
        @DisplayName("release válido → 200 OK con stock actualizado")
        void validRelease_returns200() throws Exception {
            var updated = new ProductResponse(1L, "Laptop", new BigDecimal("15.00"), Instant.now(), Instant.now());
            when(productService.releaseStock(eq(1L), any())).thenReturn(updated);

            mockMvc.perform(patch("/api/v1/products/1/release")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(mapper.writeValueAsString(new UpdateStockRequest(new BigDecimal("5.00")))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.stock").value(15.00));
        }
    }
}
