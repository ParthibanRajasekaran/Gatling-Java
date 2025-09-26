package simulations;

import io.gatling.app.Gatling;
import io.gatling.core.config.GatlingPropertiesBuilder;

/**
 * Custom Gatling runner that executes JavaApiTestSimulation with proper configuration
 */
public class GatlingRunner {

    public static void main(String[] args) {
        // Build Gatling properties with explicit configuration
        GatlingPropertiesBuilder props = new GatlingPropertiesBuilder()
            .simulationClass("simulations.JavaApiTestSimulation")
            .resultsDirectory("build/reports/gatling")
            .runDescription("Performance Test Execution");

        // Execute Gatling with the configured properties
        System.exit(Gatling.fromMap(props.build()));
    }
}
