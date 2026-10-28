package com.rounak.productsecureapi.serviceImpl;

import com.rounak.productsecureapi.annotation.AdminOnly;
import com.rounak.productsecureapi.annotation.Auditable;
import com.rounak.productsecureapi.dto.product.ProductCreate;
import com.rounak.productsecureapi.dto.product.ProductResponse;
import com.rounak.productsecureapi.dto.product.ProductUpdate;
import com.rounak.productsecureapi.entity.Product;
import com.rounak.productsecureapi.exception.ProductNotFoundException;
import com.rounak.productsecureapi.repository.ProductRepository;
import com.rounak.productsecureapi.service.ProductService;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.cache.annotation.Caching;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional(readOnly = true)
public class ProductServiceImpl implements ProductService {
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    @AdminOnly
    @Transactional
    @CacheEvict(value = "productList", allEntries = true)
    @Auditable(action = "CREATE_PRODUCT", entityType = "Product")
    public ProductResponse createProduct(ProductCreate request) {
        Product product = new Product(
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.category()
        );

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    @Override
    @Cacheable(value = "productList", key = "'all'")
    public List<ProductResponse> getAllProducts() {
        return productRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Override
    @Cacheable(value = "productById", key = "#id")
    public ProductResponse getProductById(Long id) {
        Product product = findProductOrThrow(id);
        return toResponse(product);
    }

    @Override
    @Transactional
    @AdminOnly
    @Caching(evict = {
            @CacheEvict(value = "productList", allEntries = true),
            @CacheEvict(value = "productById", key = "#id")
    })
    @Auditable(action = "UPDATE_PRODUCT", entityType = "Product")

    public ProductResponse updateProduct(
            Long id,
            ProductUpdate request
    ) {
        Product product = findProductOrThrow(id);

        product.updateDetails(
                request.name(),
                request.description(),
                request.price(),
                request.stock(),
                request.category()
        );

        Product savedProduct = productRepository.save(product);

        return toResponse(savedProduct);
    }

    @AdminOnly
    @Override
    @Transactional
    @Caching(evict = {
            @CacheEvict(value = "productList", allEntries = true),
            @CacheEvict(value = "productById", key = "#id")
    })
    @Auditable(action = "DELETE_PRODUCT", entityType = "Product")
    public void deleteProduct(Long id) {
        Product product = findProductOrThrow(id);
        productRepository.delete(product);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() ->
                        new ProductNotFoundException(id)
                );
    }

    private ProductResponse toResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getPrice(),
                product.getStock(),
                product.getCategory()
        );
    }
}
