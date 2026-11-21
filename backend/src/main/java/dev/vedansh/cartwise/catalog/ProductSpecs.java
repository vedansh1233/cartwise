package dev.vedansh.cartwise.catalog;

import java.math.BigDecimal;

import org.springframework.data.jpa.domain.Specification;

/** Composable filters for the product search endpoint. */
final class ProductSpecs {

    private ProductSpecs() {
    }

    static Specification<Product> active() {
        return (root, q, cb) -> cb.isTrue(root.get("active"));
    }

    static Specification<Product> matches(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        String like = "%" + text.trim().toLowerCase() + "%";
        return (root, q, cb) -> cb.or(
                cb.like(cb.lower(root.get("name")), like),
                cb.like(cb.lower(root.get("description")), like));
    }

    static Specification<Product> inCategory(String slug) {
        if (slug == null || slug.isBlank()) {
            return null;
        }
        return (root, q, cb) -> cb.equal(root.get("category").get("slug"), slug);
    }

    static Specification<Product> priceBetween(BigDecimal min, BigDecimal max) {
        if (min == null && max == null) {
            return null;
        }
        return (root, q, cb) -> {
            if (min == null) {
                return cb.le(root.get("price"), max);
            }
            if (max == null) {
                return cb.ge(root.get("price"), min);
            }
            return cb.between(root.get("price"), min, max);
        };
    }

    static Specification<Product> inStockOnly(boolean onlyInStock) {
        return onlyInStock ? (root, q, cb) -> cb.gt(root.get("stock"), 0) : null;
    }
}
