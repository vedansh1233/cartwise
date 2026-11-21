package dev.vedansh.cartwise.cart;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import dev.vedansh.cartwise.cart.CartDtos.CartLine;
import dev.vedansh.cartwise.cart.CartDtos.CartView;
import dev.vedansh.cartwise.catalog.CatalogDtos.ProductView;
import dev.vedansh.cartwise.catalog.Product;
import dev.vedansh.cartwise.catalog.ProductRepository;
import dev.vedansh.cartwise.common.ApiException;
import dev.vedansh.cartwise.user.UserRepository;

@Service
public class CartService {

    static final int MAX_PER_PRODUCT = 10;

    private final CartItemRepository items;
    private final ProductRepository products;
    private final UserRepository users;

    public CartService(CartItemRepository items, ProductRepository products, UserRepository users) {
        this.items = items;
        this.products = products;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public CartView view(Long userId) {
        return toView(items.findByUserIdOrderByIdAsc(userId));
    }

    @Transactional
    public CartView add(Long userId, Long productId, int quantity) {
        Product product = products.findById(productId).filter(Product::isActive)
                .orElseThrow(() -> ApiException.notFound("Product"));
        CartItem item = items.findByUserIdAndProductId(userId, productId)
                .orElseGet(() -> new CartItem(users.getReferenceById(userId), product, 0));
        setQuantity(item, product, item.getQuantity() + quantity);
        items.save(item);
        return view(userId);
    }

    @Transactional
    public CartView update(Long userId, Long productId, int quantity) {
        CartItem item = items.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> ApiException.notFound("Cart item"));
        setQuantity(item, item.getProduct(), quantity);
        return view(userId);
    }

    @Transactional
    public CartView remove(Long userId, Long productId) {
        items.findByUserIdAndProductId(userId, productId).ifPresent(items::delete);
        return view(userId);
    }

    private static void setQuantity(CartItem item, Product product, int quantity) {
        if (quantity > MAX_PER_PRODUCT) {
            throw ApiException.badRequest("You can buy at most " + MAX_PER_PRODUCT + " of one product");
        }
        if (quantity > product.getStock()) {
            throw ApiException.badRequest(product.getStock() == 0
                    ? product.getName() + " is out of stock"
                    : "Only " + product.getStock() + " left of " + product.getName());
        }
        item.setQuantity(quantity);
    }

    static CartView toView(List<CartItem> cart) {
        List<CartLine> lines = cart.stream().map(ci -> {
            Product p = ci.getProduct();
            BigDecimal line = p.getPrice().multiply(BigDecimal.valueOf(ci.getQuantity()));
            boolean available = p.isActive() && p.getStock() >= ci.getQuantity();
            return new CartLine(p.getId(), ProductView.of(p), ci.getQuantity(), line, available);
        }).toList();
        BigDecimal subtotal = lines.stream().map(CartLine::lineTotal).reduce(BigDecimal.ZERO, BigDecimal::add)
                .setScale(2);
        BigDecimal shipping = Pricing.shippingFor(subtotal);
        int count = lines.stream().mapToInt(CartLine::quantity).sum();
        return new CartView(lines, count, subtotal, shipping, subtotal.add(shipping), Pricing.FREE_SHIPPING_FROM);
    }
}
