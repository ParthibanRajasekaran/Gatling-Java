package simulations;

import com.codahale.metrics.*;
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
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
public class JavaPerformanceTest {

    private static final String BASE_URL = "http://localhost:8080";
    private static final int THREAD_POOL_SIZE = 20;
    private static final int TOTAL_REQUESTS = 100;
    
    private MetricRegistry metrics;
    private Timer healthCheckTimer;
    private Timer getAllUsersTimer;
    private Timer getUserByIdTimer;
    private Timer createUserTimer;
    private Timer updateUserTimer;
    private Timer deleteUserTimer;
    private Counter successCounter;
    private Counter failureCounter;
    
    private ExecutorService executorService;
    private ObjectMapper objectMapper;

    @BeforeAll
    void setUp() {
        // Initialize metrics
        metrics = new MetricRegistry();
        healthCheckTimer = metrics.timer("health-check");
        getAllUsersTimer = metrics.timer("get-all-users");
        getUserByIdTimer = metrics.timer("get-user-by-id");
        createUserTimer = metrics.timer("create-user");
        updateUserTimer = metrics.timer("update-user");
        deleteUserTimer = metrics.timer("delete-user");
        successCounter = metrics.counter("successful-requests");
        failureCounter = metrics.counter("failed-requests");
        
        // Initialize thread pool and JSON mapper
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
        latch.await(60, TimeUnit.SECONDS);
        
        Duration totalTime = Duration.between(startTime, Instant.now());
        double requestsPerSecond = TOTAL_REQUESTS / (totalTime.toMillis() / 1000.0);
        
        System.out.printf("✅ Browse Users Load Test Completed in %d ms\n", totalTime.toMillis());
        System.out.printf("   Throughput: %.2f requests/second\n", requestsPerSecond);
    }

    @Test
    void runUserManagementLoadTest() throws InterruptedException {
        System.out.println("\n📝 Running User Management Load Test...");
        
        CountDownLatch latch = new CountDownLatch(TOTAL_REQUESTS / 2); // Fewer CRUD operations
        AtomicInteger completedRequests = new AtomicInteger(0);
        
        Instant startTime = Instant.now();
        
        // Execute concurrent CRUD operations
        for (int i = 0; i < TOTAL_REQUESTS / 2; i++) {
            final int requestId = i;
            executorService.submit(() -> {
                try {
                    performUserManagementScenario(requestId);
                } finally {
                    latch.countDown();
                    int completed = completedRequests.incrementAndGet();
                    if (completed % 5 == 0) {
                        System.out.printf("   Completed: %d/%d CRUD operations\n", completed, TOTAL_REQUESTS / 2);
                    }
                }
            });
        }
        
        // Wait for all requests to complete
        latch.await(120, TimeUnit.SECONDS);
        
        Duration totalTime = Duration.between(startTime, Instant.now());
        
        System.out.printf("✅ User Management Load Test Completed in %d ms\n", totalTime.toMillis());
    }

    @Test
    void generatePerformanceReport() {
        System.out.println("\n📊 Performance Test Results:");
        System.out.println("============================");
        
        // Print timer statistics
        printTimerStats("Health Check", healthCheckTimer);
        printTimerStats("Get All Users", getAllUsersTimer);
        printTimerStats("Get User By ID", getUserByIdTimer);
        printTimerStats("Create User", createUserTimer);
        printTimerStats("Update User", updateUserTimer);
        printTimerStats("Delete User", deleteUserTimer);
        
        // Print success/failure rates
        long totalSuccess = successCounter.getCount();
        long totalFailures = failureCounter.getCount();
        long totalRequests = totalSuccess + totalFailures;
        double successRate = totalRequests > 0 ? (totalSuccess * 100.0 / totalRequests) : 0;
        
        System.out.println("\n📈 Overall Statistics:");
        System.out.printf("   Total Requests: %d\n", totalRequests);
        System.out.printf("   Successful: %d (%.2f%%)\n", totalSuccess, successRate);
        System.out.printf("   Failed: %d (%.2f%%)\n", totalFailures, 100 - successRate);
        
        // Assertions for test success
        assert successRate >= 95.0 : "Success rate should be at least 95%";
        System.out.println("\n✅ All performance benchmarks passed!");
    }

    private void performBrowseUsersScenario(int requestId) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            
            // 1. Health Check
            executeRequest(client, new HttpGet(BASE_URL + "/api/users/health"), healthCheckTimer);
            Thread.sleep(100); // Think time
            
            // 2. Get All Users
            executeRequest(client, new HttpGet(BASE_URL + "/api/users"), getAllUsersTimer);
            Thread.sleep(100); // Think time
            
            // 3. Get Specific User
            executeRequest(client, new HttpGet(BASE_URL + "/api/users/1"), getUserByIdTimer);
            
        } catch (Exception e) {
            failureCounter.inc();
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
            try (CloseableHttpResponse response = executeRequest(client, createRequest, createUserTimer)) {
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
                executeRequest(client, updateRequest, updateUserTimer);
                
                Thread.sleep(200); // Think time
                
                // 3. Delete User
                HttpDelete deleteRequest = new HttpDelete(BASE_URL + "/api/users/" + createdUserId);
                executeRequest(client, deleteRequest, deleteUserTimer);
            }
            
        } catch (Exception e) {
            failureCounter.inc();
            System.err.printf("   ❌ CRUD scenario failed for request %d: %s\n", requestId, e.getMessage());
        }
    }

    private CloseableHttpResponse executeRequest(CloseableHttpClient client, HttpUriRequestBase request, Timer timer) throws IOException {
        Timer.Context context = timer.time();
        try {
            CloseableHttpResponse response = client.execute(request);
            if (response.getCode() >= 200 && response.getCode() < 400) {
                successCounter.inc();
            } else {
                failureCounter.inc();
            }
            return response;
        } finally {
            context.stop();
        }
    }

    private void printTimerStats(String name, Timer timer) {
        if (timer.getCount() > 0) {
            Snapshot snapshot = timer.getSnapshot();
            System.out.printf("   %s:\n", name);
            System.out.printf("     Count: %d requests\n", timer.getCount());
            System.out.printf("     Mean: %.2f ms\n", snapshot.getMean() / 1_000_000);
            System.out.printf("     95th percentile: %.2f ms\n", snapshot.get95thPercentile() / 1_000_000);
            System.out.printf("     99th percentile: %.2f ms\n", snapshot.get99thPercentile() / 1_000_000);
            System.out.printf("     Max: %.2f ms\n", snapshot.getMax() / 1_000_000.0);
        }
    }
}
