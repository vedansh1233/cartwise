package dev.vedansh.cartwise.catalog;

import java.math.BigDecimal;
import java.text.Normalizer;
import java.util.List;
import java.util.Locale;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.vedansh.cartwise.catalog.CatalogDtos.CategoryView;
import dev.vedansh.cartwise.catalog.CatalogDtos.ProductRequest;
import dev.vedansh.cartwise.catalog.CatalogDtos.ProductView;
import dev.vedansh.cartwise.common.ApiException;
import dev.vedansh.cartwise.common.PageResponse;

@Service
public class CatalogService {

    private final ProductRepository products;
    private final CategoryRepository categories;

    public CatalogService(ProductRepository products, CategoryRepository categories) {
        this.products = products;
        this.categories = categories;
    }

    @Transactional(readOnly = true)
    public List<CategoryView> categories() {
        return categories.findAll(Sort.by("name")).stream().map(CategoryView::of).toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<ProductView> search(String q, String category, BigDecimal minPrice, BigDecimal maxPrice,
            boolean inStock, String sort, int page, int size, boolean includeInactive) {
        if (minPrice != null && maxPrice != null && minPrice.compareTo(maxPrice) > 0) {
            throw ApiException.badRequest("minPrice cannot be greater than maxPrice");
        }
        List<Specification<Product>> filters = java.util.stream.Stream.of(
                        includeInactive ? null : ProductSpecs.active(),
                        ProductSpecs.matches(q),
                        ProductSpecs.inCategory(category),
                        ProductSpecs.priceBetween(minPrice, maxPrice),
                        ProductSpecs.inStockOnly(inStock))
                .filter(java.util.Objects::nonNull)
                .toList();
        Specification<Product> spec = Specification.allOf(filters);
        PageRequest pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), toSort(sort));
        return PageResponse.of(products.findAll(spec, pageable), ProductView::of);
    }

    @Transactional(readOnly = true)
    public ProductView bySlug(String slug) {
        return products.findBySlugAndActiveTrue(slug).map(ProductView::of)
                .orElseThrow(() -> ApiException.notFound("Product"));
    }

    @Transactional
    public ProductView create(ProductRequest req) {
        Category category = category(req.categoryId());
        String slug = uniqueSlug(req.name());
        Product p = new Product(req.name().trim(), slug, req.description().trim(), req.price(), req.mrp(), req.stock(),
                category, blankToNull(req.emoji()), blankToNull(req.accent()), 0);
        p.update(p.getName(), p.getDescription(), p.getPrice(), p.getMrp(), p.getStock(), category,
                blankToNull(req.imageUrl()), p.getEmoji(), p.getAccent(), req.active());
        return ProductView.of(products.save(p));
    }

    @Transactional
    public ProductView update(Long id, ProductRequest req) {
        Product p = products.findById(id).orElseThrow(() -> ApiException.notFound("Product"));
        p.update(req.name().trim(), req.description().trim(), req.price(), req.mrp(), req.stock(),
                category(req.categoryId()), blankToNull(req.imageUrl()), blankToNull(req.emoji()),
                blankToNull(req.accent()), req.active());
        return ProductView.of(p);
    }

    private Category category(Long id) {
        return categories.findById(id).orElseThrow(() -> ApiException.badRequest("Unknown category"));
    }

    private String uniqueSlug(String name) {
        String base = Normalizer.normalize(name, Normalizer.Form.NFD).replaceAll("[^\\p{ASCII}]", "")
                .toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        if (base.isEmpty()) {
            base = "product";
        }
        String slug = base;
        for (int i = 2; products.existsBySlug(slug); i++) {
            slug = base + "-" + i;
        }
        return slug;
    }

    private static Sort toSort(String sort) {
        return switch (sort == null ? "" : sort) {
            case "price_asc" -> Sort.by("price").ascending();
            case "price_desc" -> Sort.by("price").descending();
            case "rating" -> Sort.by("rating").descending();
            case "name" -> Sort.by("name").ascending();
            default -> Sort.by("createdAt").descending().and(Sort.by("id").descending());
        };
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
