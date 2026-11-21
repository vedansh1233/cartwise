package dev.vedansh.cartwise.admin;

import java.math.BigDecimal;
import java.util.EnumMap;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import dev.vedansh.cartwise.admin.AdminDtos.Stats;
import dev.vedansh.cartwise.admin.AdminDtos.TopProduct;
import dev.vedansh.cartwise.catalog.CatalogDtos.ProductRequest;
import dev.vedansh.cartwise.catalog.CatalogDtos.ProductView;
import dev.vedansh.cartwise.catalog.CatalogService;
import dev.vedansh.cartwise.catalog.ProductRepository;
import dev.vedansh.cartwise.common.PageResponse;
import dev.vedansh.cartwise.order.OrderDtos.OrderSummary;
import dev.vedansh.cartwise.order.OrderDtos.OrderView;
import dev.vedansh.cartwise.order.OrderDtos.StatusUpdateRequest;
import dev.vedansh.cartwise.order.OrderRepository;
import dev.vedansh.cartwise.order.OrderService;
import dev.vedansh.cartwise.order.OrderStatus;
import dev.vedansh.cartwise.user.Role;
import dev.vedansh.cartwise.user.UserRepository;
import jakarta.validation.Valid;

/** Everything under /api/admin requires the ADMIN role (see SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    static final int LOW_STOCK = 5;

    private final CatalogService catalog;
    private final OrderService orders;
    private final OrderRepository orderRepo;
    private final ProductRepository productRepo;
    private final UserRepository userRepo;

    public AdminController(CatalogService catalog, OrderService orders, OrderRepository orderRepo,
            ProductRepository productRepo, UserRepository userRepo) {
        this.catalog = catalog;
        this.orders = orders;
        this.orderRepo = orderRepo;
        this.productRepo = productRepo;
        this.userRepo = userRepo;
    }

    @GetMapping("/stats")
    @Transactional(readOnly = true)
    public Stats stats() {
        Map<OrderStatus, Long> byStatus = new EnumMap<>(OrderStatus.class);
        for (OrderStatus s : OrderStatus.values()) {
            byStatus.put(s, 0L);
        }
        for (Object[] row : orderRepo.countByStatus()) {
            byStatus.put((OrderStatus) row[0], (Long) row[1]);
        }
        var top = orderRepo.topProducts(PageRequest.of(0, 5)).stream()
                .map(r -> new TopProduct((String) r[0], ((Number) r[1]).longValue(), (BigDecimal) r[2]))
                .toList();
        long customers = userRepo.countByRole(Role.CUSTOMER);
        return new Stats(orderRepo.revenue(), orderRepo.count(), customers, productRepo.count(),
                productRepo.countByStockLessThan(LOW_STOCK), byStatus, top);
    }

    @GetMapping("/products")
    public PageResponse<ProductView> products(@RequestParam(required = false) String q,
            @RequestParam(required = false) String category, @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return catalog.search(q, category, null, null, false, "name", page, size, true);
    }

    @PostMapping("/products")
    @ResponseStatus(HttpStatus.CREATED)
    public ProductView create(@Valid @RequestBody ProductRequest req) {
        return catalog.create(req);
    }

    @PutMapping("/products/{id}")
    public ProductView update(@PathVariable Long id, @Valid @RequestBody ProductRequest req) {
        return catalog.update(id, req);
    }

    @GetMapping("/orders")
    public PageResponse<OrderSummary> orders(@RequestParam(required = false) OrderStatus status,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return orders.allOrders(status, page, size);
    }

    @GetMapping("/orders/{id}")
    public OrderView order(@PathVariable Long id) {
        return orders.anyOrder(id);
    }

    @PatchMapping("/orders/{id}/status")
    public OrderView updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateRequest req) {
        return orders.updateStatus(id, req.status());
    }
}
