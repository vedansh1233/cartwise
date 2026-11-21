package dev.vedansh.cartwise.cart;

import java.math.BigDecimal;
import java.util.List;

import dev.vedansh.cartwise.catalog.CatalogDtos.ProductView;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public final class CartDtos {

    private CartDtos() {
    }

    public record AddItemRequest(@NotNull Long productId, @Min(1) @Max(10) int quantity) {
    }

    public record UpdateItemRequest(@Min(1) @Max(10) int quantity) {
    }

    public record CartLine(Long productId, ProductView product, int quantity, BigDecimal lineTotal, boolean available) {
    }

    public record CartView(List<CartLine> items, int itemCount, BigDecimal subtotal, BigDecimal shipping,
            BigDecimal total, BigDecimal freeShippingThreshold) {
    }
}
