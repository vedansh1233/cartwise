package dev.vedansh.cartwise.order;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.vedansh.cartwise.auth.CurrentUser;
import dev.vedansh.cartwise.common.PageResponse;
import dev.vedansh.cartwise.order.OrderDtos.CheckoutRequest;
import dev.vedansh.cartwise.order.OrderDtos.OrderSummary;
import dev.vedansh.cartwise.order.OrderDtos.OrderView;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orders;

    public OrderController(OrderService orders) {
        this.orders = orders;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public OrderView checkout(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody CheckoutRequest req) {
        return orders.checkout(CurrentUser.id(jwt), req);
    }

    @GetMapping
    public PageResponse<OrderSummary> mine(@AuthenticationPrincipal Jwt jwt,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "10") int size) {
        return orders.myOrders(CurrentUser.id(jwt), page, size);
    }

    @GetMapping("/{id}")
    public OrderView one(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orders.myOrder(CurrentUser.id(jwt), id);
    }

    @PostMapping("/{id}/cancel")
    public OrderView cancel(@AuthenticationPrincipal Jwt jwt, @PathVariable Long id) {
        return orders.cancelMine(CurrentUser.id(jwt), id);
    }
}
