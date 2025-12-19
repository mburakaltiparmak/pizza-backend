package com.example.pizza.controller;

import com.example.pizza.service.product.ProductSearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/search")
@CrossOrigin(origins = "*")
@RequiredArgsConstructor
public class SearchController {

    private final ProductSearchService productSearchService;

    /**
     * Autocomplete endpoint (Prefix matching)
     *
     * GET /api/search/suggestions?query=piz
     * Response: ["pizza", "pizza margherita", "pizza pepperoni"]
     */
    @GetMapping("/suggestions")
    public ResponseEntity<List<String>> getSuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {

        // Validation
        if (query == null || query.trim().length() < 2) {
            return ResponseEntity.badRequest().build();
        }

        if (limit < 1 || limit > 10) {
            limit = 5; // Default to 5 if invalid
        }

        log.debug("Autocomplete request: query={}, limit={}", query, limit);

        List<String> suggestions = productSearchService.getSuggestions(query, limit);

        return ResponseEntity.ok(suggestions);
    }

    /**
     * Fuzzy suggestions endpoint (Typo tolerance)
     *
     * GET /api/search/suggestions/fuzzy?query=pizaa
     * Response: ["pizza", "pizza margherita"]
     */
    @GetMapping("/suggestions/fuzzy")
    public ResponseEntity<List<String>> getFuzzySuggestions(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit) {

        // Validation
        if (query == null || query.trim().length() < 3) {
            // Fuzzy matching requires minimum 3 characters
            return ResponseEntity.badRequest().build();
        }

        if (limit < 1 || limit > 10) {
            limit = 5;
        }

        log.debug("Fuzzy suggestions request: query={}, limit={}", query, limit);

        List<String> suggestions = productSearchService.getFuzzySuggestions(query, limit);

        return ResponseEntity.ok(suggestions);
    }
}
