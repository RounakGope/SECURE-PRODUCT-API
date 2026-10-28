package com.rounak.productsecureapi.service;

import com.rounak.productsecureapi.dto.product.ProductCreate;
import com.rounak.productsecureapi.dto.product.ProductResponse;
import com.rounak.productsecureapi.dto.product.ProductUpdate;
import org.springframework.stereotype.Service;

import java.util.List;


public interface ProductService {
    ProductResponse createProduct(ProductCreate request);

    List<ProductResponse> getAllProducts();

    ProductResponse getProductById(Long id);

    ProductResponse updateProduct(Long id, ProductUpdate request);

    void deleteProduct(Long id);
}
