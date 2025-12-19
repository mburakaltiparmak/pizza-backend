package com.example.pizza.config.performance;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.data.web.config.EnableSpringDataWebSupport;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Spring Data Pagination için global ayarlar.
 *
 * Özellikler:
 * - Default page size: 10
 * - Max page size: 50
 * - Default sort: id,asc
 * - One-indexed parameters: false (0-based)
 *
 * Kullanım:
 * GET /api/category?page=0&size=10&sort=name,asc
 * GET /api/product?page=1&size=20&sort=price,desc
 */
@Slf4j
@Configuration
@EnableSpringDataWebSupport(
        pageSerializationMode = EnableSpringDataWebSupport.PageSerializationMode.VIA_DTO
)
public class PaginationConfig implements WebMvcConfigurer {

    /**
     * Varsayılan sayfa boyutu
     * Kullanıcı size parametresi vermezse bu değer kullanılır
     */
    private static final int DEFAULT_PAGE_SIZE = 10;

    /**
     * Maksimum sayfa boyutu
     * Kullanıcı daha büyük size isterse bu değere çekilir
     */
    private static final int MAX_PAGE_SIZE = 50;

    /**
     * Varsayılan ilk sayfa numarası (0-indexed)
     */
    private static final int DEFAULT_PAGE_NUMBER = 0;

    /**
     * Pageable resolver konfigürasyonu
     *
     * Controller metodlarında Pageable parametresi kullanılabilmesi için gerekli.
     */
    @Override
    public void addArgumentResolvers(List<HandlerMethodArgumentResolver> resolvers) {
        PageableHandlerMethodArgumentResolver resolver = new PageableHandlerMethodArgumentResolver();

        // Default ayarlar
        resolver.setFallbackPageable(
                PageRequest.of(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE)
        );

        // Maksimum sayfa boyutu
        resolver.setMaxPageSize(MAX_PAGE_SIZE);

        // Sayfa parametresi ismi (varsayılan: "page")
        resolver.setPageParameterName("page");

        // Size parametresi ismi (varsayılan: "size")
        resolver.setSizeParameterName("size");

        // 0-based indexing (false = 0'dan başlar)
        resolver.setOneIndexedParameters(false);

        resolvers.add(resolver);

        log.info("Pagination configured - Default size: {}, Max size: {}",
                DEFAULT_PAGE_SIZE, MAX_PAGE_SIZE);
    }

    @Bean
    public Pageable defaultPageable() {
        return PageRequest.of(DEFAULT_PAGE_NUMBER, DEFAULT_PAGE_SIZE);
    }
}