package dev.vedansh.cartwise.order;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.vedansh.cartwise.cart.CartItem;
import dev.vedansh.cartwise.cart.CartItemRepository;
import dev.vedansh.cartwise.cart.Pricing;
import dev.vedansh.cartwise.catalog.Product;
import dev.vedansh.cartwise.catalog.ProductRepository;
import dev.vedansh.cartwise.common.ApiException;
import dev.vedansh.cartwise.common.PageResponse;
import dev.vedansh.cartwise.order.OrderDtos.AddressRequest;
import dev.vedansh.cartwise.order.OrderDtos.CheckoutRequest;
import dev.vedansh.cartwise.order.OrderDtos.OrderSummary;
import dev.vedansh.cartwise.order.OrderDtos.OrderView;
import dev.vedansh.cartwise.user.UserRepository;

@Service
public class OrderService {

    private final OrderRepository orders;
    private final CartItemRepository cartItems;
    private final ProductRepository products;
    private final UserRepository users;

    public OrderService(OrderRepository orders, CartItemRepository cartItems, ProductRepository products,
            UserRepository users) {
        this.orders = orders;
        this.cartItems = cartItems;
        this.products = products;
        this.users = users;
    }

    /**
     * Turns the user's cart into an order in one transaction: lock each product row, check stock, reserve it,
     * snapshot prices, then empty the cart. Products are locked in id order to avoid deadlocks between checkouts.
     */
    @Transactional
    public OrderView checkout(Long userId, CheckoutRequest req) {
        List<CartItem> cart = cartItems.findByUserIdOrderByIdAsc(userId).stream()
                .sorted(Comparator.comparing(ci -> ci.getProduct().getId()))
                .toList();
        if (cart.isEmpty()) {
            throw ApiException.badRequest("Your cart is empty");
        }
        AddressRequest a = req.address();
        CustomerOrder order = new CustomerOrder(users.getReferenceById(userId),
                new ShippingAddress(a.fullName().trim(), a.phone(), a.line1().trim(), a.city().trim(), a.state().trim(),
                        a.pincode()),
                req.paymentMethod());

        BigDecimal subtotal = BigDecimal.ZERO;
        for (CartItem item : cart) {
            Product product = products.findByIdForUpdate(item.getProduct().getId())
                    .orElseThrow(() -> ApiException.notFound("Product"));
            if (!product.isActive()) {
                throw ApiException.conflict(product.getName() + " is no longer available");
            }
            if (product.getStock() < item.getQuantity()) {
                throw ApiException.conflict(product.getStock() == 0
                        ? product.getName() + " just went out of stock"
                        : "Only " + product.getStock() + " left of " + product.getName());
            }
            product.reserve(item.getQuantity());
            order.addItem(product, item.getQuantity());
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(item.getQuantity())));
        }
        subtotal = subtotal.setScale(2);
        order.setTotals(subtotal, Pricing.shippingFor(subtotal));
        orders.save(order);
        cartItems.deleteAllForUser(userId);
        return OrderView.of(order, false);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> myOrders(Long userId, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending());
        return PageResponse.of(orders.findByUserId(userId, pageable), o -> OrderSummary.of(o, false));
    }

    @Transactional(readOnly = true)
    public OrderView myOrder(Long userId, Long orderId) {
        CustomerOrder o = ownOrder(userId, orderId);
        return OrderView.of(o, false);
    }

    @Transactional
    public OrderView cancelMine(Long userId, Long orderId) {
        CustomerOrder o = ownOrder(userId, orderId);
        if (o.getStatus() != OrderStatus.PLACED) {
            throw ApiException.conflict("Only orders that have not been packed yet can be cancelled");
        }
        changeStatus(o, OrderStatus.CANCELLED);
        return OrderView.of(o, false);
    }

    // ---- admin ----

    @Transactional(readOnly = true)
    public PageResponse<OrderSummary> allOrders(OrderStatus status, int page, int size) {
        var pageable = PageRequest.of(Math.max(page, 0), Math.min(Math.max(size, 1), 50), Sort.by("createdAt").descending());
        var result = status == null ? orders.findAllBy(pageable) : orders.findByStatus(status, pageable);
        return PageResponse.of(result, o -> OrderSummary.of(o, true));
    }

    @Transactional(readOnly = true)
    public OrderView anyOrder(Long orderId) {
        return OrderView.of(orders.findWithItemsById(orderId).orElseThrow(() -> ApiException.notFound("Order")), true);
    }

    @Transactional
    public OrderView updateStatus(Long orderId, OrderStatus target) {
        CustomerOrder o = orders.findWithItemsById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        changeStatus(o, target);
        return OrderView.of(o, true);
    }

    private void changeStatus(CustomerOrder o, OrderStatus target) {
        if (!o.getStatus().canMoveTo(target)) {
            throw new ApiException(HttpStatus.CONFLICT,
                    "Cannot change an order from " + o.getStatus() + " to " + target);
        }
        if (target == OrderStatus.CANCELLED) {
            // Give the stock back, locking rows in a stable order.
            o.getItems().stream()
                    .sorted(Comparator.comparing(i -> i.getProduct().getId()))
                    .forEach(i -> products.findByIdForUpdate(i.getProduct().getId())
                            .ifPresent(p -> p.restock(i.getQuantity())));
        }
        o.moveTo(target);
    }

    private CustomerOrder ownOrder(Long userId, Long orderId) {
        CustomerOrder o = orders.findWithItemsById(orderId).orElseThrow(() -> ApiException.notFound("Order"));
        if (!o.getUser().getId().equals(userId)) {
            // Same response as a missing order, so ids of other customers' orders are not revealed.
            throw ApiException.notFound("Order");
        }
        return o;
    }
}
