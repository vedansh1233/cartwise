package dev.vedansh.cartwise.cart;

import java.math.BigDecimal;

/** Shipping rule shared by the cart preview and checkout so both always agree. */
public final class Pricing {

    public static final BigDecimal FREE_SHIPPING_FROM = new BigDecimal("999.00");
    public static final BigDecimal SHIPPING_FEE = new BigDecimal("49.00");

    private Pricing() {
    }

    public static BigDecimal shippingFor(BigDecimal subtotal) {
        if (subtotal.signum() == 0 || subtotal.compareTo(FREE_SHIPPING_FROM) >= 0) {
            return BigDecimal.ZERO.setScale(2);
        }
        return SHIPPING_FEE;
    }
}
