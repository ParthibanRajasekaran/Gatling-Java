package simulations;

import io.gatling.javaapi.core.*;
import io.gatling.javaapi.http.*;
import java.time.Duration;

import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.*;

public class QuickTestSimulation extends Simulation {

    private final HttpProtocolBuilder httpProtocol = http
        .baseUrl("http://localhost:8080")
        .acceptHeader("application/json")
        .contentTypeHeader("application/json")
        .userAgentHeader("Gatling Java Quick Test");

    private final ScenarioBuilder quickTestScenario = scenario("Quick API Test")
        .exec(session -> {
            System.out.println("Running quick test for user: " + session.userId());
            return session;
        })
        .exec(
            http("Health Check")
                .get("/api/users/health")
                .check(status().is(200))
                .check(responseTimeInMillis().lte(1000))
        )
        .pause(Duration.ofSeconds(1))
        .exec(
            http("Get All Users")
                .get("/api/users")
                .check(status().is(200))
                .check(jsonPath("$[0].id").exists())
                .check(responseTimeInMillis().lte(2000))
        );

    {
        setUp(
            quickTestScenario.injectOpen(
                atOnceUsers(5),
                rampUsers(10).during(Duration.ofSeconds(30))
            ).protocols(httpProtocol)
        ).assertions(
            global().responseTime().max().lt(3000),
            global().successfulRequests().percent().gte(95.0),
            details("Health Check").responseTime().max().lt(1000),
            details("Get All Users").responseTime().percentile3().lt(2000)
        );
    }
}
