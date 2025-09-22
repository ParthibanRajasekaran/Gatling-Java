package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;

import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class SimpleGetSimulation extends Simulation {

    // HTTP Protocol Configuration
    HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling Java Performance Test");

    // Simple Scenario for GET requests
    ScenarioBuilder browseUsersScenario = scenario("Browse Users Java Scenario")
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

    {
        setUp(
            browseUsersScenario.injectOpen(
                atOnceUsers(3),
                rampUsers(5).during(Duration.ofSeconds(10))
            ).protocols(httpProtocol)
        ).assertions(
            global().responseTime().max().lt(3000),
            global().successfulRequests().percent().gt(95.0)
        );
    }
}
