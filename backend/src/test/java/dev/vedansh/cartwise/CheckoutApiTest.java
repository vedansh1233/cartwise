package dev.vedansh.cartwise;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

import dev.vedansh.cartwise.catalog.ProductRepository;

class CheckoutApiTest extends ApiTestSupport {

    @Autowired
    ProductRepository products;

    private long productId(String slug) {
        return products.findBySlugAndActiveTrue(slug).orElseThrow().getId();
    }

    private void addToCart(String token, long productId, int qty, int expectedStatus) throws Exception {
        mvc.perform(post("/api/cart/items").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"productId\":%d,\"quantity\":%d}".formatted(productId, qty)))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void cartTotalsIncludeShippingUnderThreshold() throws Exception {
        String token = register("Ravi", "ravi@example.com");
        addToCart(token, productId("classic-cotton-t-shirt"), 1, 200);
        mvc.perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.subtotal").value(499.0))
                .andExpect(jsonPath("$.shipping").value(49.0))
                .andExpect(jsonPath("$.total").value(548.0));
        mvc.perform(put("/api/cart/items/" + productId("classic-cotton-t-shirt"))
                        .header(HttpHeaders.AUTHORIZATION, token).contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":2}"))
                .andExpect(jsonPath("$.subtotal").value(998.0))
                .andExpect(jsonPath("$.shipping").value(49.0));
        addToCart(token, productId("clean-code"), 1, 200);
        mvc.perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.shipping").value(0.0))
                .andExpect(jsonPath("$.itemCount").value(3));
    }

    @Test
    void cannotAddMoreThanStockOrOutOfStockItems() throws Exception {
        String token = register("Meera", "meera@example.com");
        addToCart(token, productId("adjustable-dumbbells-10kg"), 3, 400); // only 2 in stock
        addToCart(token, productId("ceramic-coffee-mug-set"), 1, 400);    // sold out
    }

    @Test
    void checkoutReservesStockEmptiesCartAndSnapshotsPrices() throws Exception {
        String token = register("Kabir", "kabir@example.com");
        long bat = productId("english-willow-cricket-bat");
        int before = products.findById(bat).orElseThrow().getStock();
        addToCart(token, bat, 2, 200);

        String body = mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutBody("9876501234", "201301")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("PLACED"))
                .andExpect(jsonPath("$.items", hasSize(1)))
                .andExpect(jsonPath("$.total").value(5998.0))
                .andReturn().getResponse().getContentAsString();

        org.assertj.core.api.Assertions.assertThat(products.findById(bat).orElseThrow().getStock()).isEqualTo(before - 2);
        mvc.perform(get("/api/cart").header(HttpHeaders.AUTHORIZATION, token)).andExpect(jsonPath("$.items", hasSize(0)));

        Integer orderId = JsonPath.read(body, "$.id");
        mvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(jsonPath("$.items[0].id").value(orderId));
    }

    @Test
    void cancellingAPlacedOrderRestoresStock() throws Exception {
        String token = register("Isha", "isha@example.com");
        long mat = productId("yoga-mat-6mm");
        int before = products.findById(mat).orElseThrow().getStock();
        addToCart(token, mat, 3, 200);
        String body = mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutBody("9123456780", "110001")))
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer orderId = JsonPath.read(body, "$.id");

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CANCELLED"));
        org.assertj.core.api.Assertions.assertThat(products.findById(mat).orElseThrow().getStock()).isEqualTo(before);

        mvc.perform(post("/api/orders/" + orderId + "/cancel").header(HttpHeaders.AUTHORIZATION, token))
                .andExpect(status().isConflict());
    }

    @Test
    void checkoutValidatesAddressAndEmptyCart() throws Exception {
        String token = register("Neel", "neel@example.com");
        mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutBody("9876501234", "201301")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Your cart is empty"));

        addToCart(token, productId("clean-code"), 1, 200);
        mvc.perform(post("/api/orders").header(HttpHeaders.AUTHORIZATION, token)
                        .contentType(MediaType.APPLICATION_JSON).content(checkoutBody("12345", "2013")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields['address.phone']").exists())
                .andExpect(jsonPath("$.fields['address.pincode']").exists());
    }

    @Test
    void customersCannotSeeOtherCustomersOrders() throws Exception {
        String demo = login(DEMO, DEMO_PASSWORD);
        String body = mvc.perform(get("/api/orders").header(HttpHeaders.AUTHORIZATION, demo))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer demoOrderId = JsonPath.read(body, "$.items[0].id");

        String stranger = register("Stranger", "stranger@example.com");
        mvc.perform(get("/api/orders/" + demoOrderId).header(HttpHeaders.AUTHORIZATION, stranger))
                .andExpect(status().isNotFound());
    }
}
