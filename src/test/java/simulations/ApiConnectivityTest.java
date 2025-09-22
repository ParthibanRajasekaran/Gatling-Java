package simulations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class ApiConnectivityTest {

    private static final String BASE_URL = "http://localhost:8080";
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void testApiHealthCheck() throws IOException {
        System.out.println("🔍 Testing API Health Check...");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/api/users/health");

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getCode();
                System.out.printf("   Health check status: %d\n", statusCode);

                assertEquals(200, statusCode, "Health check should return 200 OK");

                if (response.getEntity() != null) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    System.out.printf("   Response: %s\n", responseBody);
                }
            }
        }

        System.out.println("✅ Health check test passed!");
    }

    @Test
    void testGetAllUsers() throws IOException {
        System.out.println("📋 Testing Get All Users...");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/api/users");

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getCode();
                System.out.printf("   Get users status: %d\n", statusCode);

                assertEquals(200, statusCode, "Get all users should return 200 OK");

                if (response.getEntity() != null) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    JsonNode users = objectMapper.readTree(responseBody);

                    assertTrue(users.isArray(), "Response should be a JSON array");
                    System.out.printf("   Found %d users\n", users.size());

                    if (users.size() > 0) {
                        JsonNode firstUser = users.get(0);
                        assertTrue(firstUser.has("id"), "User should have ID field");
                        assertTrue(firstUser.has("name"), "User should have name field");
                        assertTrue(firstUser.has("email"), "User should have email field");
                        System.out.printf("   First user: %s\n", firstUser.get("name").asText());
                    }
                }
            }
        }

        System.out.println("✅ Get all users test passed!");
    }

    @Test
    void testCreateUser() throws IOException {
        System.out.println("➕ Testing Create User...");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            String userData = "{\"name\":\"Test User\",\"email\":\"test@example.com\",\"age\":25}";

            HttpPost request = new HttpPost(BASE_URL + "/api/users");
            request.setEntity(new StringEntity(userData, ContentType.APPLICATION_JSON));

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getCode();
                System.out.printf("   Create user status: %d\n", statusCode);

                assertEquals(200, statusCode, "Create user should return 200 OK");

                if (response.getEntity() != null) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    JsonNode createdUser = objectMapper.readTree(responseBody);

                    assertTrue(createdUser.has("id"), "Created user should have ID");
                    assertEquals("Test User", createdUser.get("name").asText(), "Name should match");
                    assertEquals("test@example.com", createdUser.get("email").asText(), "Email should match");
                    assertEquals(25, createdUser.get("age").asInt(), "Age should match");

                    System.out.printf("   Created user with ID: %s\n", createdUser.get("id").asText());
                }
            }
        }

        System.out.println("✅ Create user test passed!");
    }

    @Test
    void testGetSpecificUser() throws IOException {
        System.out.println("🔍 Testing Get Specific User...");

        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpGet request = new HttpGet(BASE_URL + "/api/users/1");

            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getCode();
                System.out.printf("   Get user by ID status: %d\n", statusCode);

                // User might exist (200) or not exist (404) - both are valid responses
                assertTrue(statusCode == 200 || statusCode == 404,
                    "Get user by ID should return 200 or 404");

                if (statusCode == 200 && response.getEntity() != null) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    JsonNode user = objectMapper.readTree(responseBody);

                    assertTrue(user.has("id"), "User should have ID field");
                    System.out.printf("   Found user: %s\n", user.get("name").asText());
                } else if (statusCode == 404) {
                    System.out.println("   User with ID 1 not found (404) - this is expected");
                }
            }
        }

        System.out.println("✅ Get specific user test passed!");
    }
}
