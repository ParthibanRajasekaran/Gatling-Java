# Gatling Java Performance Testing with JUnit XML Report Generation

A complete Gatling Java performance testing solution with dual report generation (HTML + JUnit XML).

## Features

- ✅ Complete Spring Boot API with REST endpoints
- ✅ Gatling performance test simulations
- ✅ JUnit XML report generator for CI/CD integration
- ✅ Dual reporting: HTML reports + XML reports
- ✅ Performance assertions and thresholds
- ✅ Gradle build automation with custom tasks

## Quick Start

### Prerequisites
- Java 17+
- Gradle 8.4+ (or use wrapper)

### Run Performance Tests

```bash
# Complete workflow (recommended)
./gradlew performanceTest

# Individual steps
./gradlew runApi        # Start API server
./gradlew gatlingRun    # Run Gatling tests
./gradlew generateJUnitXml  # Generate JUnit XML
```

## Project Structure

```
├── src/main/java/com/example/
│   ├── api/                    # Spring Boot API
│   │   ├── ApiApplication.java
│   │   ├── controller/UserController.java
│   │   ├── model/User.java
│   │   └── service/UserService.java
│   └── reporting/
│       └── GatlingJUnitReportGenerator.java  # JUnit XML generator
├── src/test/java/simulations/
│   ├── JavaApiTestSimulation.java    # Main performance tests
│   ├── QuickTestSimulation.java      # Quick validation tests
│   └── [other simulation files]
├── build.gradle                      # Build configuration
└── INTEGRATION_GUIDE.md              # Detailed integration guide
```

## Reports Generated

### HTML Reports (Standard Gatling)
- Location: `build/reports/gatling/[simulation]/index.html`
- Features: Interactive charts, detailed metrics, response analysis

### JUnit XML Reports (CI/CD Integration)
- Location: `build/gatling/junit/TEST-[SimulationName].xml`
- Features: Performance test cases, assertions, CI/CD compatibility

## API Endpoints

- `GET /api/health` - Health check
- `GET /api/users` - Get all users
- `GET /api/users/{id}` - Get user by ID

## Performance Thresholds

- Max response time: < 5000ms
- Success rate: > 90%
- Configurable in `GatlingJUnitReportGenerator.java`

## Integration with Any Gatling Java Project

This JUnit report generator can be used with any Gatling Java repository. See `INTEGRATION_GUIDE.md` for detailed instructions.

### Key Files to Copy:
1. `GatlingJUnitReportGenerator.java` - The report generator
2. Gradle task configurations from `build.gradle`

## CI/CD Integration

### Jenkins
```groovy
junit 'build/gatling/junit/*.xml'
publishHTML([reportDir: 'build/reports/gatling', reportFiles: 'index.html'])
```

### GitHub Actions
```yaml
- uses: dorny/test-reporter@v1
  with:
    path: 'build/gatling/junit/*.xml'
    reporter: java-junit
```

## Development

### Available Gradle Tasks
- `performanceTest` - Complete workflow
- `generateJUnitXml` - Convert Gatling results to JUnit XML
- `gatlingRun` - Run Gatling tests only
- `quickPerformanceTest` - Lightweight validation
- `runApi` - Start Spring Boot API

### Customization
Modify thresholds and behavior in `GatlingJUnitReportGenerator.java`:
```java
private static final String GATLING_DIR = "build/reports/gatling";
private static final String JUNIT_DIR = "build/gatling/junit";
// Adjust response time and success rate thresholds
```

## License

This project demonstrates performance testing patterns and can be adapted for any Gatling Java project.
