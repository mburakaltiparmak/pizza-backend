package com.example.pizza.service.product;

import com.example.pizza.dto.product.ProductResponse;
import com.example.pizza.entity.product.Product;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ProductService {

    List<ProductResponse> getAllProducts();

    Product findById(Long id);

    Product save(Product product, MultipartFile file) throws IOException;

    Product update(Long id, Product product, MultipartFile file) throws IOException;

    // FIX: Metod imzası Long id olarak güncellendi
    void delete(Long id);

    Product saveCustomPizza(Product product) throws IOException;

    List<Product> getProductsByCategory(Long categoryId);

    // --- Pagination Methods ---

    Page<ProductResponse> getAllProducts(Pageable pageable);

    Page<ProductResponse> getProductsByCategory(Long categoryId, Pageable pageable);

    Page<Product> searchByName(String name, Pageable pageable);

    Page<Product> findByPriceRange(Double minPrice, Double maxPrice, Pageable pageable);

    Page<ProductResponse> findByStockAvailability(boolean inStock, Pageable pageable);
}