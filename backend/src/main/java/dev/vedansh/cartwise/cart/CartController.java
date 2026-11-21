package dev.vedansh.cartwise.cart;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import dev.vedansh.cartwise.auth.CurrentUser;
import dev.vedansh.cartwise.cart.CartDtos.AddItemRequest;
import dev.vedansh.cartwise.cart.CartDtos.CartView;
import dev.vedansh.cartwise.cart.CartDtos.UpdateItemRequest;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cart;

    public CartController(CartService cart) {
        this.cart = cart;
    }

    @GetMapping
    public CartView get(@AuthenticationPrincipal Jwt jwt) {
        return cart.view(CurrentUser.id(jwt));
    }

    @PostMapping("/items")
    public CartView add(@AuthenticationPrincipal Jwt jwt, @Valid @RequestBody AddItemRequest req) {
        return cart.add(CurrentUser.id(jwt), req.productId(), req.quantity());
    }

    @PutMapping("/items/{productId}")
    public CartView update(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId,
            @Valid @RequestBody UpdateItemRequest req) {
        return cart.update(CurrentUser.id(jwt), productId, req.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartView remove(@AuthenticationPrincipal Jwt jwt, @PathVariable Long productId) {
        return cart.remove(CurrentUser.id(jwt), productId);
    }
}
