package dev.vedansh.cartwise.config;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import dev.vedansh.cartwise.cart.CartService;
import dev.vedansh.cartwise.catalog.Category;
import dev.vedansh.cartwise.catalog.CategoryRepository;
import dev.vedansh.cartwise.catalog.Product;
import dev.vedansh.cartwise.catalog.ProductRepository;
import dev.vedansh.cartwise.order.OrderDtos.AddressRequest;
import dev.vedansh.cartwise.order.OrderDtos.CheckoutRequest;
import dev.vedansh.cartwise.order.OrderService;
import dev.vedansh.cartwise.order.OrderStatus;
import dev.vedansh.cartwise.order.PaymentMethod;
import dev.vedansh.cartwise.user.Role;
import dev.vedansh.cartwise.user.User;
import dev.vedansh.cartwise.user.UserRepository;

/**
 * Fills an empty database with a demo catalog, two demo accounts and a few orders so the storefront and the
 * admin dashboard have something to show. Disable with cartwise.seed.enabled=false.
 */
@Component
@ConditionalOnProperty(name = "cartwise.seed.enabled", havingValue = "true")
public class DemoDataSeeder implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(DemoDataSeeder.class);

    private final CategoryRepository categories;
    private final ProductRepository products;
    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final CartService cart;
    private final OrderService orders;

    public DemoDataSeeder(CategoryRepository categories, ProductRepository products, UserRepository users,
            PasswordEncoder encoder, CartService cart, OrderService orders) {
        this.categories = categories;
        this.products = products;
        this.users = users;
        this.encoder = encoder;
        this.cart = cart;
        this.orders = orders;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (categories.count() > 0) {
            return;
        }
        Map<String, Category> c = new LinkedHashMap<>();
        for (String[] row : new String[][] {
                {"Electronics", "electronics", "🎧"}, {"Fashion", "fashion", "👕"},
                {"Home & Kitchen", "home-kitchen", "🏠"}, {"Books", "books", "📚"},
                {"Sports & Fitness", "sports-fitness", "🏏"}, {"Beauty", "beauty", "🧴"}}) {
            c.put(row[1], categories.save(new Category(row[0], row[1], row[2])));
        }

        p("Wireless Earbuds Pro", "Bluetooth 5.3 earbuds with active noise cancellation, 30-hour battery with the case and IPX5 sweat resistance.", "2499", "4999", 42, c.get("electronics"), "🎧", "#6366f1", 4.4);
        p("Smartwatch Active 2", "1.9\" AMOLED display, heart-rate and SpO2 tracking, 100+ sports modes and 7-day battery life.", "3999", "6999", 18, c.get("electronics"), "⌚", "#0ea5e9", 4.2);
        p("Portable Bluetooth Speaker", "Punchy 20W sound, deep bass, waterproof IPX7 body and 12 hours of playback.", "1799", "2999", 25, c.get("electronics"), "🔊", "#f97316", 4.3);
        p("Mechanical Keyboard TKL", "Tenkeyless layout, hot-swappable brown switches, white backlight and USB-C.", "3299", null, 4, c.get("electronics"), "⌨️", "#64748b", 4.6);
        p("20000mAh Power Bank", "22.5W fast charging, two USB-A ports and one USB-C port, charges a phone up to four times.", "1499", "2199", 60, c.get("electronics"), "🔋", "#22c55e", 4.1);

        p("Classic Cotton T-Shirt", "Soft 180 GSM combed cotton crew-neck tee with a regular fit. Machine washable.", "499", "799", 120, c.get("fashion"), "👕", "#ef4444", 4.0);
        p("Slim Fit Denim Jeans", "Stretch denim with a mid-rise slim fit and a dark indigo wash.", "1299", "2299", 35, c.get("fashion"), "👖", "#2563eb", 4.2);
        p("Running Shoes Flex", "Lightweight mesh upper with a cushioned sole for daily runs and gym sessions.", "2199", "3999", 3, c.get("fashion"), "👟", "#14b8a6", 4.5);

        p("Non-Stick Cookware Set", "3-piece induction-ready set: frying pan, kadhai and tawa with a granite finish.", "1899", "3499", 20, c.get("home-kitchen"), "🍳", "#a855f7", 4.3);
        p("Stainless Steel Water Bottle", "1 litre double-wall vacuum flask that keeps drinks hot for 12 hours and cold for 24.", "599", "999", 80, c.get("home-kitchen"), "🧊", "#06b6d4", 4.4);
        p("Ceramic Coffee Mug Set", "Set of 4 matte-finish 350 ml mugs, microwave and dishwasher safe.", "699", null, 0, c.get("home-kitchen"), "☕", "#b45309", 4.1);

        p("Clean Code", "A handbook of agile software craftsmanship: write code that is easy to read and change.", "549", "899", 30, c.get("books"), "📘", "#1d4ed8", 4.7);
        p("Atomic Habits", "Tiny changes, remarkable results: a practical guide to building good habits.", "399", "799", 55, c.get("books"), "📗", "#16a34a", 4.8);
        p("The Pragmatic Programmer", "Timeless advice for developers, from personal responsibility to career growth.", "649", "999", 12, c.get("books"), "📙", "#ea580c", 4.6);

        p("Yoga Mat 6mm", "Anti-slip textured TPE mat with carry strap, 183 x 61 cm.", "799", "1299", 40, c.get("sports-fitness"), "🧘", "#8b5cf6", 4.3);
        p("Adjustable Dumbbells 10kg", "Pair of 5kg dumbbells with removable plates and comfortable grip handles.", "1599", "2499", 2, c.get("sports-fitness"), "🏋️", "#475569", 4.2);
        p("English Willow Cricket Bat", "Grade 3 English willow bat with a full-size cane handle and grip.", "2999", "4499", 9, c.get("sports-fitness"), "🏏", "#ca8a04", 4.4);

        p("Vitamin C Face Serum", "10% vitamin C serum with hyaluronic acid for brighter, hydrated skin. 30 ml.", "449", "699", 70, c.get("beauty"), "🧴", "#f59e0b", 4.2);

        User admin = users.save(new User("Store Admin", "admin@cartwise.dev", encoder.encode("Admin@1234"), Role.ADMIN));
        User demo = users.save(new User("Demo Customer", "demo@cartwise.dev", encoder.encode("Demo@1234"), Role.CUSTOMER));

        AddressRequest address = new AddressRequest("Demo Customer", "9876543210", "Flat 12, Sector 62", "Noida",
                "Uttar Pradesh", "201301");
        order(demo.getId(), address, PaymentMethod.UPI_DEMO, OrderStatus.DELIVERED, "atomic-habits", "clean-code");
        order(demo.getId(), address, PaymentMethod.COD, OrderStatus.SHIPPED, "wireless-earbuds-pro");
        order(demo.getId(), address, PaymentMethod.UPI_DEMO, null, "yoga-mat-6mm", "stainless-steel-water-bottle");

        log.info("Seeded demo data: {} products, admin={} demo={}", products.count(), admin.getEmail(), demo.getEmail());
    }

    private void p(String name, String description, String price, String mrp, int stock, Category category,
            String emoji, String accent, double rating) {
        String slug = name.toLowerCase().replaceAll("[^a-z0-9]+", "-").replaceAll("(^-|-$)", "");
        products.save(new Product(name, slug, description, new BigDecimal(price),
                mrp == null ? null : new BigDecimal(mrp), stock, category, emoji, accent, rating));
    }

    private void order(Long userId, AddressRequest address, PaymentMethod method, OrderStatus finalStatus,
            String... slugs) {
        for (String slug : slugs) {
            Product product = products.findBySlugAndActiveTrue(slug).orElseThrow();
            cart.add(userId, product.getId(), 1);
        }
        var placed = orders.checkout(userId, new CheckoutRequest(address, method));
        if (finalStatus != null) {
            for (OrderStatus s : new OrderStatus[] {OrderStatus.PACKED, OrderStatus.SHIPPED, OrderStatus.DELIVERED}) {
                orders.updateStatus(placed.id(), s);
                if (s == finalStatus) {
                    break;
                }
            }
        }
    }
}
