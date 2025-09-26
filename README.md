# Gatling Java Performance Testing with GitHub Actions

A complete Gatling Java performance testing solution with automated report generation and deployment to GitHub Pages.

## Features

- ✅ Complete Spring Boot API with REST endpoints for testing
- ✅ Gatling performance test simulations written in Java
- ✅ Automated CI/CD pipeline with GitHub Actions
- ✅ Dual reporting: Interactive HTML reports and CI-compatible JUnit XML
- ✅ Performance assertions and build failure conditions
- ✅ Gradle build automation for local and remote testing
- ✅ Automatic deployment of Gatling reports to GitHub Pages

## Quick Start

### Prerequisites
- Java 17+
- Gradle 8.4+ (or use the provided wrapper)

### Run Locally

```bash
# Run the Spring Boot API in a separate terminal
./gradlew run

# In another terminal, run the Gatling tests
./gradlew gatlingRun

# To generate JUnit XML reports after a Gatling run
./gradlew generateJUnitXml
```

## Project Structure

```
├── .github/workflows/
│   └── performance-tests.yml     # GitHub Actions CI/CD pipeline
├── src/main/java/com/example/
│   ├── api/                    # Spring Boot API source
│   └── reporting/
│       └── GatlingJUnitReportGenerator.java  # JUnit XML generator
├── src/test/java/simulations/
│   └── JavaApiTestSimulation.java    # Main performance test simulation
├── build.gradle                      # Build configuration
└── README.md                       # This file
```

## Reports

### HTML Reports (via GitHub Pages)
- **Location**: Automatically deployed and accessible via a URL in your repository's "Pages" settings.
- **Features**: Interactive charts, detailed metrics, and response time analysis.

### JUnit XML Reports (for CI/CD Integration)
- **Location**: `build/gatling/junit/TEST-[SimulationName].xml`
- **Features**: Integrates with GitHub Actions test reporting to show pass/fail status directly in the workflow summary.

## API Endpoints Tested

- `GET /api/users/health` - Health check for the user service.
- `GET /api/users` - Retrieves all users.
- `GET /api/users/{id}` - Retrieves a specific user by their ID.

## Performance Assertions

The build will fail if these conditions are not met:
- **Max response time**: < 5000ms
- **Success rate**: > 90%
- These are configured directly in `JavaApiTestSimulation.java`.

## CI/CD Pipeline

The entire testing and reporting process is automated via GitHub Actions in `.github/workflows/performance-tests.yml`.

The pipeline performs the following steps:
1.  **Builds** the project.
2.  **Starts** the mock API server.
3.  **Runs** the Gatling performance tests.
4.  **Generates** JUnit XML reports for CI feedback.
5.  **Publishes** test results to the GitHub Actions summary.
6.  **Deploys** the full HTML Gatling report to GitHub Pages.

## Development

### Available Gradle Tasks
- `run`: Starts the Spring Boot API.
- `build`: Compiles and builds the entire project.
- `gatlingRun`: Runs only the Gatling performance tests.
- `generateJUnitXml`: Converts the latest Gatling log into a JUnit XML report.
- `test`: Runs standard unit tests (if any).

### Customization
To change performance test logic, such as the number of users or API endpoints:
- Modify `src/test/java/simulations/JavaApiTestSimulation.java`.
```java
// Example: Change user load
setUp(
    apiTestScenario.injectOpen(
        atOnceUsers(10), // Change number of immediate users
        rampUsers(20).during(Duration.ofSeconds(60)) // Change ramp-up
    ).protocols(httpProtocol)
);
```

## Using the JUnit Report Generator in Other Projects

The `GatlingJUnitReportGenerator.java` class is a portable utility for converting Gatling's `simulation.log` into a CI-friendly JUnit XML report. Here’s how to use it in any Gatling Java + Gradle project.

### 1. Copy the Generator File
Copy `src/main/java/com/example/reporting/GatlingJUnitReportGenerator.java` into your new project's source tree (e.g., `src/main/java/com/yourcompany/reporting/GatlingJUnitReportGenerator.java`).

### 2. Create a Gradle Task
Add the following task to your `build.gradle` file. This allows you to run the generator from the command line with `./gradlew generateJUnitXml`.

```groovy
task generateJUnitXml(type: JavaExec) {
    group = 'reporting'
    description = 'Generate JUnit XML report from Gatling results'
    classpath = sourceSets.main.runtimeClasspath
    // IMPORTANT: Update this path to match where you copied the file
    mainClass = 'com.yourcompany.reporting.GatlingJUnitReportGenerator'
}
```

### 3. Customize for Your Simulation
The generator needs to know which simulation's results to parse. Open the `GatlingJUnitReportGenerator.java` file you copied and **update the `SIMULATION_NAME_PREFIX` constant** to match the name of your simulation class (in lowercase).

For example, if your simulation is named `MyApiSimulation.java`, you would change the line to:
```java
private static final String SIMULATION_NAME_PREFIX = "myapisimulation"; // <-- CHANGE THIS
```

This ensures the generator finds the correct report directory and creates a properly named XML file.

## License

This project is a template for robust performance testing and can be adapted for any Gatling Java project.
