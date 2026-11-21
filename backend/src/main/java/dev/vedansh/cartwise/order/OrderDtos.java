package dev.vedansh.cartwise.order;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public final class OrderDtos {

    private OrderDtos() {
    }

    public record AddressRequest(
            @NotBlank @Size(max = 80) String fullName,
            @NotBlank @Pattern(regexp = "^[6-9]\\d{9}$", message = "must be a valid 10-digit mobile number") String phone,
            @NotBlank @Size(max = 200) String line1,
            @NotBlank @Size(max = 60) String city,
            @NotBlank @Size(max = 60) String state,
            @NotBlank @Pattern(regexp = "^\\d{6}$", message = "must be a 6-digit PIN code") String pincode) {
    }

    public record CheckoutRequest(@NotNull @Valid AddressRequest address, @NotNull PaymentMethod paymentMethod) {
    }

    public record StatusUpdateRequest(@NotNull OrderStatus status) {
    }

    public record OrderLine(Long productId, String productName, String emoji, BigDecimal unitPrice, int quantity,
            BigDecimal lineTotal) {
    }

    public record AddressView(String fullName, String phone, String line1, String city, String state, String pincode) {
    }

    public record OrderView(Long id, OrderStatus status, Set<OrderStatus> nextStatuses, PaymentMethod paymentMethod,
            AddressView address, List<OrderLine> items, int itemCount, BigDecimal subtotal, BigDecimal shipping,
            BigDecimal total, Instant createdAt, Instant updatedAt, String customerEmail) {

        static OrderView of(CustomerOrder o, boolean withCustomer) {
            ShippingAddress a = o.getAddress();
            List<OrderLine> lines = o.getItems().stream()
                    .map(i -> new OrderLine(i.getProduct().getId(), i.getProductName(), i.getEmoji(), i.getUnitPrice(),
                            i.getQuantity(), i.lineTotal()))
                    .toList();
            return new OrderView(o.getId(), o.getStatus(), o.getStatus().next(), o.getPaymentMethod(),
                    new AddressView(a.getFullName(), a.getPhone(), a.getLine1(), a.getCity(), a.getState(), a.getPincode()),
                    lines, lines.stream().mapToInt(OrderLine::quantity).sum(), o.getSubtotal(), o.getShipping(),
                    o.getTotal(), o.getCreatedAt(), o.getUpdatedAt(), withCustomer ? o.getUser().getEmail() : null);
        }
    }

    /** Compact row for order lists. */
    public record OrderSummary(Long id, OrderStatus status, int itemCount, BigDecimal total, Instant createdAt,
            String customerEmail, String customerName) {

        static OrderSummary of(CustomerOrder o, boolean withCustomer) {
            int count = o.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
            return new OrderSummary(o.getId(), o.getStatus(), count, o.getTotal(), o.getCreatedAt(),
                    withCustomer ? o.getUser().getEmail() : null, withCustomer ? o.getUser().getName() : null);
        }
    }
}
