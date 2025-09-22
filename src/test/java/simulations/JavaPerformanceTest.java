package simulations;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.hc.client5.http.classic.methods.*;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.CloseableHttpResponse;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;

import java.io.IOException;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JavaPerformanceTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final int THREAD_POOL_SIZE = 20;
    private static final int TOTAL_REQUESTS = 50;

    private ExecutorService executorService;
    private ObjectMapper objectMapper;

    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong successfulRequests = new AtomicLong(0);
    private final AtomicLong failedRequests = new AtomicLong(0);
    private final AtomicLong totalResponseTime = new AtomicLong(0);

    @BeforeAll
    void setUp() {
        executorService = Executors.newFixedThreadPool(THREAD_POOL_SIZE);
        objectMapper = new ObjectMapper();

        System.out.println("=== Java Performance Testing Suite Started ===");
        System.out.println("Base URL: " + BASE_URL);
        System.out.println("Thread Pool Size: " + THREAD_POOL_SIZE);
        System.out.println("Total Requests per scenario: " + TOTAL_REQUESTS);
        System.out.println("================================================");
    }

    @Test
    void runBrowseUsersLoadTest() throws InterruptedException {
        System.out.println("\n🔍 Running Browse Users Load Test...");

        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS);
        AtomicInteger completedRequests = new AtomicInteger(0);

        Instant startTime = Instant.now();

        // Execute concurrent requests
        for (int i = 0; i < TOTAL_REQUESTS; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    performBrowseUsersScenario(requestId);
                } finally {
                    latch.countDown();
                    int completed = completedRequests.incrementAndGet();
                    if (completed % 10 == 0) {
                        System.out.printf("   Completed: %d/%d requests\n", completed, TOTAL_REQUESTS);
                    }
                }
            });
        }

        // Wait for all requests to complete
        boolean completed = latch.await(60, TimeUnit.SECONDS);

        Duration totalTime = Duration.between(startTime, Instant.now());
        double requestsPerSecond = TOTAL_REQUESTS / (totalTime.toMillis() / 1000.0);

        System.out.printf("✅ Browse Users Load Test Completed in %d ms\n", totalTime.toMillis());
        System.out.printf("   Throughput: %.2f requests/second\n", requestsPerSecond);
        System.out.printf("   All requests completed: %s\n", completed ? "Yes" : "No");
    }

    @Test
    void runUserManagementLoadTest() throws InterruptedException {
        System.out.println("\n📝 Running User Management Load Test...");

        int crudOperations = TOTAL_REQUESTS / 4; // Fewer CRUD operations
        CountDownLatch latch = new CountDownLatch(crudOperations);
        AtomicInteger completedRequests = new AtomicInteger(0);

        Instant startTime = Instant.now();

        // Execute concurrent CRUD operations
        for (int i = 0; i < crudOperations; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    performUserManagementScenario(requestId);
                } finally {
                    latch.countDown();
                    int completed = completedRequests.incrementAndGet();
                    if (completed % 5 == 0) {
                        System.out.printf("   Completed: %d/%d CRUD operations\n", completed, crudOperations);
                    }
                }
            });
        }

        // Wait for all requests to complete
        boolean completed = latch.await(120, TimeUnit.SECONDS);

        Duration totalTime = Duration.between(startTime, Instant.now());

        System.out.printf("✅ User Management Load Test Completed in %d ms\n", totalTime.toMillis());
        System.out.printf("   All operations completed: %s\n", completed ? "Yes" : "No");
    }

    @Test
    void generatePerformanceReport() {
        System.out.println("\n📊 Performance Test Results:");
        System.out.println("============================");

        long total = totalRequests.get();
        long successful = successfulRequests.get();
        long failed = failedRequests.get();
        long avgResponseTime = total > 0 ? totalResponseTime.get() / total : 0;

        double successRate = total > 0 ? (successful * 100.0 / total) : 0;

        System.out.printf("   Total Requests: %d\n", total);
        System.out.printf("   Successful: %d (%.2f%%)\n", successful, successRate);
        System.out.printf("   Failed: %d (%.2f%%)\n", failed, 100 - successRate);
        System.out.printf("   Average Response Time: %d ms\n", avgResponseTime);

        // Basic performance assertions
        if (total > 0) {
            assert successRate >= 90.0 : String.format("Success rate %.2f%% is below 90%%", successRate);
            assert avgResponseTime < 5000 : String.format("Average response time %d ms is above 5000ms", avgResponseTime);
            System.out.println("\n✅ All performance benchmarks passed!");
        } else {
            System.out.println("\n⚠️ No requests were executed");
        }
    }

    private void performBrowseUsersScenario(int requestId) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            // 1. Health Check
            executeRequest(client, new HttpGet(BASE_URL + "/api/users/health"));
            Thread.sleep(100); // Think time

            // 2. Get All Users
            executeRequest(client, new HttpGet(BASE_URL + "/api/users"));
            Thread.sleep(100); // Think time

            // 3. Get Specific User
            executeRequest(client, new HttpGet(BASE_URL + "/api/users/1"));

        } catch (Exception e) {
            failedRequests.incrementAndGet();
            System.err.printf("   ❌ Browse scenario failed for request %d: %s\n", requestId, e.getMessage());
        }
    }

    private void performUserManagementScenario(int requestId) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {

            // 1. Create User
            String userData = String.format(
                "{\"name\":\"TestUser%d\",\"email\":\"testuser%d@example.com\",\"age\":%d}",
                requestId, requestId, 25 + (requestId % 40)
            );

            HttpPost createRequest = new HttpPost(BASE_URL + "/api/users");
            createRequest.setEntity(new StringEntity(userData, ContentType.APPLICATION_JSON));

            String createdUserId = null;
            try (CloseableHttpResponse response = executeRequest(client, createRequest)) {
                if (response.getCode() == 200) {
                    String responseBody = new String(response.getEntity().getContent().readAllBytes());
                    JsonNode jsonNode = objectMapper.readTree(responseBody);
                    createdUserId = jsonNode.get("id").asText();
                }
            }

            if (createdUserId != null) {
                Thread.sleep(200); // Think time

                // 2. Update User
                String updateData = String.format(
                    "{\"name\":\"UpdatedTestUser%d\",\"email\":\"updated.testuser%d@example.com\",\"age\":%d}",
                    requestId, requestId, 30 + (requestId % 35)
                );

                HttpPut updateRequest = new HttpPut(BASE_URL + "/api/users/" + createdUserId);
                updateRequest.setEntity(new StringEntity(updateData, ContentType.APPLICATION_JSON));
                executeRequest(client, updateRequest);

                Thread.sleep(200); // Think time

                // 3. Delete User
                HttpDelete deleteRequest = new HttpDelete(BASE_URL + "/api/users/" + createdUserId);
                executeRequest(client, deleteRequest);
            }

        } catch (Exception e) {
            failedRequests.incrementAndGet();
            System.err.printf("   ❌ CRUD scenario failed for request %d: %s\n", requestId, e.getMessage());
        }
    }

    private CloseableHttpResponse executeRequest(CloseableHttpClient client, HttpUriRequestBase request) throws IOException {
        long startTime = System.currentTimeMillis();
        try {
            CloseableHttpResponse response = client.execute(request);
            long responseTime = System.currentTimeMillis() - startTime;

            totalRequests.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);

            if (response.getCode() >= 200 && response.getCode() < 400) {
                successfulRequests.incrementAndGet();
            } else {
                failedRequests.incrementAndGet();
            }
            return response;
        } catch (IOException e) {
            long responseTime = System.currentTimeMillis() - startTime;
            totalRequests.incrementAndGet();
            totalResponseTime.addAndGet(responseTime);
            failedRequests.incrementAndGet();
            throw e;
        }
    }
}
