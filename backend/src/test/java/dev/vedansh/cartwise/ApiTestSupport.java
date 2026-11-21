package dev.vedansh.cartwise;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import com.jayway.jsonpath.JsonPath;

/**
 * Full-stack tests against the seeded demo data. Each test runs in a transaction that is rolled back,
 * so stock and orders changed by one test never leak into another.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public abstract class ApiTestSupport {

    protected static final String ADMIN = "admin@cartwise.dev";
    protected static final String ADMIN_PASSWORD = "Admin@1234";
    protected static final String DEMO = "demo@cartwise.dev";
    protected static final String DEMO_PASSWORD = "Demo@1234";

    @Autowired
    protected MockMvc mvc;

    protected String login(String email, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    protected String register(String name, String email) throws Exception {
        String body = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"%s\",\"email\":\"%s\",\"password\":\"Secret@123\"}".formatted(name, email)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        return "Bearer " + JsonPath.read(body, "$.token");
    }

    protected static String checkoutBody(String phone, String pincode) {
        return """
                {"paymentMethod":"COD","address":{"fullName":"Test Buyer","phone":"%s","line1":"221B Sector 18",
                 "city":"Noida","state":"Uttar Pradesh","pincode":"%s"}}
                """.formatted(phone, pincode);
    }
}
