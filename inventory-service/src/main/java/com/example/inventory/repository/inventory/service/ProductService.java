package com.example.inventory.repository.inventory.service;

import com.example.inventory.repository.inventory.dto.CreateProductRequest;
import com.example.inventory.repository.inventory.dto.ProductResponse;
import com.example.inventory.repository.inventory.dto.UpdateStockRequest;
import com.example.inventory.repository.inventory.event.ProductCreatedEvent;
import com.example.inventory.repository.inventory.exception.ProductNotFoundException;
import com.example.inventory.repository.inventory.model.Product;
import com.example.inventory.repository.inventory.repository.ProductRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository repository;
    private final ApplicationEventPublisher eventPublisher;

    // Spring inyecta automáticamente los beans que implementan estas interfaces
    public ProductService(ProductRepository repository,
                          ApplicationEventPublisher eventPublisher) {
        this.repository     = repository;
        this.eventPublisher = eventPublisher;
    }

    @Transactional
    public ProductResponse createProduct(CreateProductRequest req) {
        log.info("Creando producto {} con stock {}",
                req.name(), req.stock());

        Product product = Product.create(
                req.name(),
                req.stock()
        );

        Product saved = repository.save(product);
        log.info("Producto #{} creado", saved.getId());

        eventPublisher.publishEvent(new ProductCreatedEvent(saved));

        return ProductResponse.from(saved);
    }

    public ProductResponse findById(Long productId) {
        return ProductResponse.from(findOrThrow(productId));
    }

    public Page<ProductResponse> findAll(Pageable pageable) {
        return repository.findAll(pageable).map(ProductResponse::from);
    }

    private Product findOrThrow(Long productId) {
        return repository.findById(productId)
                .orElseThrow(() -> new ProductNotFoundException(productId));
    }

    @Transactional
    public ProductResponse reserveStock(Long productId, UpdateStockRequest req) {
        Product product = findOrThrow(productId);

        product.reserveStock(req.stock());

        return ProductResponse.from(repository.save(product));
    }

    @Transactional
    public ProductResponse releaseStock(Long productId, UpdateStockRequest req) {
        Product product = findOrThrow(productId);

        product.releaseStock(req.stock());

        return ProductResponse.from(repository.save(product));
    }
}
