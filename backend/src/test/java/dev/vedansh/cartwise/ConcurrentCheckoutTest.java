package dev.vedansh.cartwise;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.RepeatedTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionTemplate;

import dev.vedansh.cartwise.catalog.ProductRepository;

/** Really concurrent checkouts: these commit, so they use their own database and no test transaction. */
@TestPropertySource(properties = "spring.datasource.url=jdbc:h2:mem:concurrency;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000")
@Transactional(propagation = Propagation.NOT_SUPPORTED)
class ConcurrentCheckoutTest extends ApiTestSupport {

    private static final String SLUG = "yoga-mat-6mm";

    @Autowired
    ProductRepository products;
    @Autowired
    JdbcTemplate jdbc;
    @Autowired
    PlatformTransactionManager txManager;

    private final ExecutorService pool = Executors.newFixedThreadPool(2);

    @AfterEach
    void stopPool() {
        pool.shutdownNow();
    }

    private long productWithStock(int stock) {
        jdbc.update("update product set stock = ? where slug = ?", stock, SLUG);
        return products.findBySlugAndActiveTrue(SLUG).orElseThrow().getId();
    }

    private int stock() {
        return products.findBySlugAndActiveTrue(SLUG).orElseThrow().getStock();
    }

    private int orderCount() {
        return jdbc.queryForObject("select count(*) from customer_order", Integer.class);
    }

    private String buyerWithOneInCart(long productId) throws Exception {
        String token = register("Buyer", UUID.randomUUID() + "@example.com");
        mvc.perform(post("/api/cart/items").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":1}".formatted(productId)))
                .andExpect(status().isOk());
        return token;
    }

    private MockHttpServletResponse checkout(String token) throws Exception {
        return mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, token)
                .contentType(MediaType.APPLICATION_JSON).content(checkoutBody("9876501234", "201301")))
                .andReturn().getResponse();
    }

    private List<MockHttpServletResponse> together(String first, String second) throws Exception {
        CountDownLatch go = new CountDownLatch(1);
        Future<MockHttpServletResponse> a = pool.submit(() -> { go.await(); return checkout(first); });
        Future<MockHttpServletResponse> b = pool.submit(() -> { go.await(); return checkout(second); });
        go.countDown();
        return List.of(a.get(20, TimeUnit.SECONDS), b.get(20, TimeUnit.SECONDS));
    }

    /** Deterministic: the buyer's checkout is already waiting for the row when the last unit is sold. */
    @Test
    void buyerWhoWaitedForTheLastUnitGetsOutOfStock() throws Exception {
        long id = productWithStock(1);
        String token = buyerWithOneInCart(id);
        int ordersBefore = orderCount();

        Future<MockHttpServletResponse> waiting = new TransactionTemplate(txManager).execute(tx -> {
            var locked = products.findByIdForUpdate(id).orElseThrow();
            Future<MockHttpServletResponse> f = pool.submit(() -> checkout(token));
            try {
                Thread.sleep(1500);
            } catch (InterruptedException e) {
                throw new IllegalStateException(e);
            }
            assertThat(f.isDone()).as("checkout must wait for the row lock").isFalse();
            locked.reserve(1);
            return f;
        });

        MockHttpServletResponse response = waiting.get(20, TimeUnit.SECONDS);
        assertThat(response.getStatus()).isEqualTo(409);
        assertThat(response.getContentAsString()).contains("out of stock");
        assertThat(stock()).isZero();
        assertThat(orderCount()).isEqualTo(ordersBefore);
    }

    @RepeatedTest(10)
    void twoBuyersCannotBothBuyTheLastUnit() throws Exception {
        long id = productWithStock(1);
        String first = buyerWithOneInCart(id);
        String second = buyerWithOneInCart(id);
        int ordersBefore = orderCount();

        var responses = together(first, second);

        assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 409);
        assertThat(stock()).isZero();
        assertThat(orderCount()).isEqualTo(ordersBefore + 1);
    }

    @RepeatedTest(10)
    void twoBuyersBothSucceedWhenThereIsEnoughStock() throws Exception {
        long id = productWithStock(5);
        String first = buyerWithOneInCart(id);
        String second = buyerWithOneInCart(id);

        var responses = together(first, second);

        assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactly(201, 201);
        assertThat(stock()).isEqualTo(3);
    }

    @RepeatedTest(10)
    void submittingTheSameCartTwiceCreatesOneOrder() throws Exception {
        long id = productWithStock(5);
        String token = buyerWithOneInCart(id);
        int ordersBefore = orderCount();

        var responses = together(token, token);

        assertThat(responses).extracting(MockHttpServletResponse::getStatus).containsExactlyInAnyOrder(201, 400);
        assertThat(stock()).isEqualTo(4);
        assertThat(orderCount()).isEqualTo(ordersBefore + 1);
    }
}
