package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class JavaApiTestSimulation extends Simulation {

    // HTTP Protocol Configuration
    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling Java Test");

    // Simple API Test Scenario
    private final ScenarioBuilder apiTestScenario = scenario("Java API Test")
        .exec(
            http("Health Check")
                .get("/api/users/health")
                .check(status().is(200))
        )
        .pause(Duration.ofSeconds(1))
        .exec(
            http("Get All Users")
                .get("/api/users")
                .check(status().is(200))
        )
        .pause(Duration.ofSeconds(1))
        .exec(
            http("Get User by ID")
                .get("/api/users/1")
                .check(status().in(200, 404))
        );

    // Setup the simulation
    {
        setUp(
            apiTestScenario.injectOpen(
                atOnceUsers(5),
                rampUsers(10).during(Duration.ofSeconds(30))
            ).protocols(httpProtocol)
        ).assertions(
            global().responseTime().max().lt(5000),
            global().successfulRequests().percent().gt(90.0)
        );
    }
}
