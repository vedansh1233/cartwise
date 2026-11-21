package dev.vedansh.cartwise.catalog;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import dev.vedansh.cartwise.catalog.CatalogDtos.CategoryView;
import dev.vedansh.cartwise.catalog.CatalogDtos.ProductView;
import dev.vedansh.cartwise.common.PageResponse;

@RestController
@RequestMapping("/api")
public class CatalogController {

    private final CatalogService catalog;

    public CatalogController(CatalogService catalog) {
        this.catalog = catalog;
    }

    @GetMapping("/categories")
    public List<CategoryView> categories() {
        return catalog.categories();
    }

    /**
     * Search the catalog. sort = newest (default) | price_asc | price_desc | rating | name.
     */
    @GetMapping("/products")
    public PageResponse<ProductView> products(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(defaultValue = "false") boolean inStock,
            @RequestParam(defaultValue = "newest") String sort,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "12") int size) {
        return catalog.search(q, category, minPrice, maxPrice, inStock, sort, page, size, false);
    }

    @GetMapping("/products/{slug}")
    public ProductView product(@PathVariable String slug) {
        return catalog.bySlug(slug);
    }
}
