package dev.vedansh.cartwise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import com.jayway.jsonpath.JsonPath;

class AdminApiTest extends ApiTestSupport {

    @Test
    void customersAreForbiddenFromAdminEndpoints() throws Exception {
        String demo = login(DEMO, DEMO_PASSWORD);
        mvc.perform(get("/api/admin/stats").header(HttpHeaders.AUTHORIZATION, demo)).andExpect(status().isForbidden());
        mvc.perform(get("/api/admin/stats")).andExpect(status().isUnauthorized());
    }

    @Test
    void statsReflectSeededOrders() throws Exception {
        String admin = login(ADMIN, ADMIN_PASSWORD);
        mvc.perform(get("/api/admin/stats").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.orders").value(3))
                .andExpect(jsonPath("$.ordersByStatus.DELIVERED").value(1))
                .andExpect(jsonPath("$.ordersByStatus.SHIPPED").value(1))
                .andExpect(jsonPath("$.ordersByStatus.PLACED").value(1))
                .andExpect(jsonPath("$.lowStockProducts").value(4));
    }

    @Test
    void adminCanCreateProductWithGeneratedSlug() throws Exception {
        String admin = login(ADMIN, ADMIN_PASSWORD);
        String product = """
                {"name":"Clean Code","description":"Second edition","price":599,"mrp":899,"stock":10,
                 "categoryId":4,"emoji":"📘","accent":"#1d4ed8","active":true}
                """;
        mvc.perform(post("/api/admin/products").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content(product))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").value("clean-code-2"));

        mvc.perform(post("/api/admin/products").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Bad\",\"description\":\"x\",\"price\":0,\"stock\":-1,\"categoryId\":1,\"accent\":\"red\",\"active\":true}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fields.price").exists())
                .andExpect(jsonPath("$.fields.stock").exists())
                .andExpect(jsonPath("$.fields.accent").exists());
    }

    @Test
    void orderStatusMustFollowTheLifecycle() throws Exception {
        String admin = login(ADMIN, ADMIN_PASSWORD);
        String body = mvc.perform(get("/api/admin/orders").param("status", "PLACED").header(HttpHeaders.AUTHORIZATION, admin))
                .andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.items[0].id");

        mvc.perform(patch("/api/admin/orders/" + id + "/status").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"DELIVERED\"}"))
                .andExpect(status().isConflict());
        mvc.perform(patch("/api/admin/orders/" + id + "/status").header(HttpHeaders.AUTHORIZATION, admin)
                        .contentType(MediaType.APPLICATION_JSON).content("{\"status\":\"PACKED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("PACKED"))
                .andExpect(jsonPath("$.customerEmail").value(DEMO));
    }
}
