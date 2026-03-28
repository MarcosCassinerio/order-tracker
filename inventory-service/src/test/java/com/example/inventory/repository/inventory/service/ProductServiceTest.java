package com.example.inventory.repository.inventory.service;

import com.example.inventory.repository.inventory.dto.CreateProductRequest;
import com.example.inventory.repository.inventory.dto.ProductResponse;
import com.example.inventory.repository.inventory.dto.UpdateStockRequest;
import com.example.inventory.repository.inventory.event.ProductCreatedEvent;
import com.example.inventory.repository.inventory.exception.InvalidReserveAmountException;
import com.example.inventory.repository.inventory.exception.ProductNotFoundException;
import com.example.inventory.repository.inventory.model.Product;
import com.example.inventory.repository.inventory.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ProductService — Unit Tests")
class ProductServiceTest {

    @Mock private ProductRepository        repository;
    @Mock private ApplicationEventPublisher eventPublisher;

    @InjectMocks private ProductService service;

    private Product sampleProduct;

    @BeforeEach
    void setUp() {
        sampleProduct = Product.create("Laptop", 10);
    }

    // ══════════════════════════════════════════════════════════════════════════
    // createProduct
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("createProduct")
    class CreateProductTests {

        @Test
        @DisplayName("request válido → producto guardado y evento publicado")
        void validRequest_savesProductAndPublishesEvent() {
            when(repository.save(any())).thenReturn(sampleProduct);

            var req = new CreateProductRequest("Laptop", 10);
            ProductResponse response = service.createProduct(req);

            assertThat(response.name()).isEqualTo("Laptop");
            assertThat(response.stock()).isEqualTo(10);

            ArgumentCaptor<ProductCreatedEvent> eventCaptor = ArgumentCaptor.forClass(ProductCreatedEvent.class);
            verify(eventPublisher).publishEvent(eventCaptor.capture());
            assertThat(eventCaptor.getValue().product()).isEqualTo(sampleProduct);
        }

        @Test
        @DisplayName("stock negativo → IllegalArgumentException")
        void negativeStock_throwsIllegalArgument() {
            var req = new CreateProductRequest("Laptop", -1);

            assertThatThrownBy(() -> service.createProduct(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("negativo");
        }

        @Test
        @DisplayName("stock null → se crea con stock 0")
        void nullStock_defaultsToZero() {
            var product = Product.create("Mouse", 0);
            when(repository.save(any())).thenReturn(product);

            var req = new CreateProductRequest("Mouse", null);
            ProductResponse response = service.createProduct(req);

            assertThat(response.stock()).isEqualTo(0);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // findById
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("findById")
    class FindByIdTests {

        @Test
        @DisplayName("producto existente → devuelve DTO mapeado")
        void existingProduct_returnsMappedDto() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleProduct));

            ProductResponse response = service.findById(1L);

            assertThat(response.name()).isEqualTo("Laptop");
            assertThat(response.stock()).isEqualTo(10);
        }

        @Test
        @DisplayName("producto inexistente → ProductNotFoundException")
        void missingProduct_throwsNotFoundException() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.findById(99L))
                    .isInstanceOf(ProductNotFoundException.class)
                    .hasMessageContaining("99");
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // reserveStock
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("reserveStock")
    class ReserveStockTests {

        @Test
        @DisplayName("reserva válida → stock reducido")
        void validReserve_reducesStock() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleProduct));
            when(repository.save(any())).thenReturn(sampleProduct);

            var req = new UpdateStockRequest(3);
            service.reserveStock(1L, req);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStock()).isEqualTo(7);
        }

        @Test
        @DisplayName("reserva mayor al stock → InvalidReserveAmountException")
        void insufficientStock_throwsException() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleProduct));

            var req = new UpdateStockRequest(99);

            assertThatThrownBy(() -> service.reserveStock(1L, req))
                    .isInstanceOf(InvalidReserveAmountException.class);
        }

        @Test
        @DisplayName("producto inexistente → ProductNotFoundException")
        void missingProduct_throwsNotFoundException() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.reserveStock(99L, new UpdateStockRequest(1)))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    // releaseStock
    // ══════════════════════════════════════════════════════════════════════════
    @Nested
    @DisplayName("releaseStock")
    class ReleaseStockTests {

        @Test
        @DisplayName("release válido → stock aumentado")
        void validRelease_increasesStock() {
            when(repository.findById(1L)).thenReturn(Optional.of(sampleProduct));
            when(repository.save(any())).thenReturn(sampleProduct);

            var req = new UpdateStockRequest(5);
            service.releaseStock(1L, req);

            ArgumentCaptor<Product> captor = ArgumentCaptor.forClass(Product.class);
            verify(repository).save(captor.capture());
            assertThat(captor.getValue().getStock()).isEqualTo(15);
        }

        @Test
        @DisplayName("producto inexistente → ProductNotFoundException")
        void missingProduct_throwsNotFoundException() {
            when(repository.findById(99L)).thenReturn(Optional.empty());

            assertThatThrownBy(() -> service.releaseStock(99L, new UpdateStockRequest(1)))
                    .isInstanceOf(ProductNotFoundException.class);
        }
    }
}
