package dev.vedansh.cartwise;

import static org.hamcrest.Matchers.everyItem;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.lessThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

class CatalogApiTest extends ApiTestSupport {

    @Test
    void catalogIsPublicAndPaginated() throws Exception {
        mvc.perform(get("/api/products").param("size", "5"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(5)))
                .andExpect(jsonPath("$.totalItems").value(18))
                .andExpect(jsonPath("$.totalPages").value(4));
        mvc.perform(get("/api/categories")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(6)));
    }

    @Test
    void filtersByCategoryAndMaxPrice() throws Exception {
        mvc.perform(get("/api/products").param("category", "books").param("maxPrice", "600"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[*].category.slug", everyItem(org.hamcrest.Matchers.is("books"))))
                .andExpect(jsonPath("$.items[*].price", everyItem(lessThanOrEqualTo(600.0))));
    }

    @Test
    void searchMatchesNameOrDescriptionAndSortsByPrice() throws Exception {
        mvc.perform(get("/api/products").param("q", "bluetooth").param("sort", "price_asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.items", hasSize(2)))
                .andExpect(jsonPath("$.items[0].name").value("Portable Bluetooth Speaker"));
    }

    @Test
    void inStockFilterHidesSoldOutProducts() throws Exception {
        mvc.perform(get("/api/products").param("inStock", "true").param("size", "50"))
                .andExpect(jsonPath("$.items[*].stock", everyItem(greaterThan(0))))
                .andExpect(jsonPath("$.totalItems").value(17));
    }

    @Test
    void productDetailShowsDiscount() throws Exception {
        mvc.perform(get("/api/products/wireless-earbuds-pro"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.price").value(2499.0))
                .andExpect(jsonPath("$.discountPercent").value(50));
        mvc.perform(get("/api/products/does-not-exist")).andExpect(status().isNotFound());
    }

    @Test
    void invalidPriceRangeIs400() throws Exception {
        mvc.perform(get("/api/products").param("minPrice", "900").param("maxPrice", "100"))
                .andExpect(status().isBadRequest());
    }
}
