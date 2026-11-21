package dev.vedansh.cartwise.order;

import java.util.EnumSet;
import java.util.Set;

/** Order lifecycle. Each status lists the statuses it may move to next. */
public enum OrderStatus {
    PLACED, PACKED, SHIPPED, DELIVERED, CANCELLED;

    public Set<OrderStatus> next() {
        return switch (this) {
            case PLACED -> EnumSet.of(PACKED, CANCELLED);
            case PACKED -> EnumSet.of(SHIPPED, CANCELLED);
            case SHIPPED -> EnumSet.of(DELIVERED);
            case DELIVERED, CANCELLED -> EnumSet.noneOf(OrderStatus.class);
        };
    }

    public boolean canMoveTo(OrderStatus target) {
        return next().contains(target);
    }
}
