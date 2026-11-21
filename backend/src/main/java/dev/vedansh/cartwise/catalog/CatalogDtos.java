package dev.vedansh.cartwise.catalog;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class CatalogDtos {

    private CatalogDtos() {
    }

    public record CategoryView(Long id, String name, String slug, String icon) {
        static CategoryView of(Category c) {
            return new CategoryView(c.getId(), c.getName(), c.getSlug(), c.getIcon());
        }
    }

    public record ProductView(Long id, String name, String slug, String description, BigDecimal price, BigDecimal mrp,
            int discountPercent, int stock, boolean inStock, CategoryView category, String imageUrl, String emoji,
            String accent, double rating, boolean active) {

        public static ProductView of(Product p) {
            int discount = 0;
            if (p.getMrp() != null && p.getMrp().compareTo(p.getPrice()) > 0) {
                discount = p.getMrp().subtract(p.getPrice()).multiply(BigDecimal.valueOf(100))
                        .divide(p.getMrp(), 0, java.math.RoundingMode.DOWN).intValue();
            }
            return new ProductView(p.getId(), p.getName(), p.getSlug(), p.getDescription(), p.getPrice(), p.getMrp(),
                    discount, p.getStock(), p.getStock() > 0, CategoryView.of(p.getCategory()), p.getImageUrl(),
                    p.getEmoji(), p.getAccent(), p.getRating(), p.isActive());
        }
    }

    /** Admin create/update payload. */
    public record ProductRequest(
            @NotBlank @Size(max = 120) String name,
            @NotBlank @Size(max = 2000) String description,
            @NotNull @DecimalMin(value = "1.00") @Digits(integer = 10, fraction = 2) BigDecimal price,
            @DecimalMin(value = "1.00") @Digits(integer = 10, fraction = 2) BigDecimal mrp,
            @Min(0) @Max(100000) int stock,
            @NotNull Long categoryId,
            @Size(max = 500) @Pattern(regexp = "^(https?://.*)?$", message = "must be an http(s) URL") String imageUrl,
            @Size(max = 16) String emoji,
            @Size(max = 16) @Pattern(regexp = "^(#[0-9a-fA-F]{6})?$", message = "must be a hex colour like #22c55e") String accent,
            boolean active) {
    }
}
