package dev.vedansh.cartwise.admin;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import dev.vedansh.cartwise.order.OrderStatus;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record TopProduct(String name, long unitsSold, BigDecimal revenue) {
    }

    public record Stats(BigDecimal revenue, long orders, long customers, long products, long lowStockProducts,
            Map<OrderStatus, Long> ordersByStatus, List<TopProduct> topProducts) {
    }
}
