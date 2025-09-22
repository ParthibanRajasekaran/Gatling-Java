# Gatling Java Performance Testing Framework (Gradle Edition)

A comprehensive performance testing suite built with **Gatling Java API** and **Spring Boot**, featuring automated dual report generation (HTML + JUnit XML) using **Gradle build system** with zero external dependencies.

## 🚀 Quick Start

### Prerequisites
- **Java 17+** 
- **No Gradle installation required** (uses Gradle Wrapper)
- **Git**

### 1. Clone and Setup
```bash
git clone <your-repo-url>
cd Gatling-Java
./gradlew build
```

## 🏃‍♂️ Running Tests

### Option 1: Automated Complete Workflow (Recommended)
```bash
# Start mock API server (in background) and run all tests
./run-tests-with-reports.sh
```
This script will:
- Clean previous results
- Start the mock API server
- Execute Gatling performance tests with Gradle
- Generate both HTML and JUnit XML reports
- Copy all reports to the `reports/` directory
- Display test summary

### Option 2: Manual Step-by-Step

#### Start the API Server
Choose one of these options:

**Option A: Python Mock API (Lightweight)**
```bash
python3 mock-api.py
# Server starts on http://localhost:8080
# Provides endpoints: /api/users/health, /api/users, /api/users/{id}
```

**Option B: Spring Boot API (Full Featured)**
```bash
./gradlew runApi
# Full Spring Boot application with logging and actuator endpoints
```

#### Run Gatling Tests
```bash
# Run specific simulation with Gradle
./gradlew gatlingRun --simulation=simulations.JavaApiTestSimulation

# Or run performance test workflow
./gradlew performanceTest

# Quick validation test
./gradlew quickPerformanceTest
```

#### Generate JUnit XML Report
```bash
# Using Java implementation (no Python dependency)
./gradlew generateJUnitXml
```

### Option 3: Gradle-Only Approach
```bash
# Complete workflow with Gradle
./gradlew clean performanceTest
```

## 📊 Viewing Reports

### HTML Reports (Interactive Dashboard)
```bash
# Open in browser
open reports/index.html
# or
firefox reports/index.html
```

**HTML Report Features:**
- 📈 **Interactive performance charts** with response time distributions
- 🎯 **Detailed request metrics** for each endpoint
- 📋 **Global statistics** with percentiles and throughput
- 🔍 **Individual request analysis** pages

### JUnit XML Reports (CI/CD Integration)
```bash
# View JUnit XML
cat reports/TEST-JavaApiTestSimulation.xml

# Import into your CI/CD system
# Jenkins: Archive as test results
# GitLab CI: Use artifacts and junit reports
# GitHub Actions: Use test reporting actions
```

**JUnit XML Features:**
- ✅ **9 test cases** (3 performance + 6 assertion tests)
- 📊 **Detailed properties** with response time metrics
- 🎯 **Performance assertions** (response time < 5000ms, success rate > 90%)
- 🔧 **CI/CD compatible** format with proper timestamps

## 📁 Report Locations

After running tests, reports are available in multiple locations:

```
reports/                          # 📁 Centralized reports directory
├── index.html                   # 🌐 Main HTML dashboard
├── TEST-JavaApiTestSimulation.xml # 🧪 JUnit XML report
├── req_*.html                   # 📄 Individual endpoint reports
├── js/ and style/               # 🎨 Supporting assets
└── simulation.log               # 📋 Raw Gatling data

build/reports/gatling/            # 📁 Gradle Gatling output
├── javaapitestsimulation-*/     # 📊 Timestamped HTML reports
└── junit/                       # 🧪 JUnit XML reports
```

## 🔧 Available Gradle Tasks

### Gatling Tasks
```bash
./gradlew gatlingRun              # Run all Gatling simulations
./gradlew gatlingRun --simulation=simulations.JavaApiTestSimulation  # Run specific simulation
./gradlew quickPerformanceTest    # Run quick validation tests
./gradlew performanceTest         # Complete performance test workflow
```

### Application Tasks
```bash
./gradlew runApi                  # Start Spring Boot API server
./gradlew generateJUnitXml        # Generate JUnit XML reports
./gradlew build                   # Build the entire project
./gradlew clean                   # Clean build artifacts
```

### Available Simulations
- **`JavaApiTestSimulation`** - Main performance test with realistic load patterns
- **`QuickTestSimulation`** - Fast validation test for CI pipelines
- **`JavaPerformanceTest`** - Extended load testing scenarios

## 🎯 Build System Benefits

### Gradle with Gatling Plugin Advantages
- 🚀 **Native Gatling integration** - purpose-built for performance testing
- 📦 **Zero Maven overhead** - lightweight, focused build system
- 🔧 **Simplified configuration** - Gatling plugin handles complexity
- ⚡ **Faster builds** - optimized for test execution
- 🎯 **Task-based workflow** - granular control over test execution

### Performance Thresholds
The tests validate these performance criteria:
- ⏱️ **Response Time**: < 5000ms (configurable)
- ✅ **Success Rate**: > 90% (configurable)
- 👥 **Load Pattern**: 5 immediate users + 10 users ramped over 30 seconds

### Modifying Test Parameters
Edit `src/test/java/simulations/JavaApiTestSimulation.java`:
```java
setUp(
    apiTestScenario.injectOpen(
        atOnceUsers(5),                    // Immediate users
        rampUsers(10).during(Duration.ofSeconds(30))  // Ramp-up
    ).protocols(httpProtocol)
).assertions(
    global().responseTime().max().lt(5000),    // Max response time
    global().successfulRequests().percent().gt(90.0)  // Success rate
);
```

## 🎯 API Endpoints Being Tested

The performance tests target these endpoints:

| Endpoint | Method | Purpose | Expected Response |
|----------|--------|---------|-------------------|
| `/api/users/health` | GET | Health check | `{"status": "healthy", "timestamp": 123...}` |
| `/api/users` | GET | Get all users | Array of user objects |
| `/api/users/{id}` | GET | Get user by ID | Single user object |

## 🛠️ Troubleshooting

### Common Issues

**Port 8080 already in use:**
```bash
# Find and kill process using port 8080
lsof -ti:8080 | xargs kill -9
```

**Tests failing with connection refused:**
```bash
# Ensure API server is running
curl http://localhost:8080/api/users/health
# Should return: {"status": "healthy", "timestamp": ...}
```

**Gradle build errors:**
```bash
# Clean and rebuild
./gradlew clean build
```

**Reports not generated:**
```bash
# Check if Gatling tests completed successfully
ls -la build/reports/gatling/
# Manually run report generator
./gradlew generateJUnitXml
```

### Log Locations
- **Gatling logs**: `build/reports/gatling/*/simulation.log`
- **Gradle logs**: Console output during `./gradlew` commands
- **Spring Boot logs**: Console output during `./gradlew runApi`
- **Mock API logs**: Console output during `python3 mock-api.py`

## 🔄 CI/CD Integration

### Jenkins Pipeline Example
```groovy
pipeline {
    stages {
        stage('Performance Tests') {
            steps {
                sh './gradlew clean performanceTest'
                publishHTML([
                    allowMissing: false,
                    alwaysLinkToLastBuild: true,
                    keepAll: true,
                    reportDir: 'reports',
                    reportFiles: 'index.html',
                    reportName: 'Gatling Performance Report'
                ])
                junit 'reports/TEST-JavaApiTestSimulation.xml'
            }
        }
    }
}
```

### GitLab CI Example
```yaml
performance_tests:
  script:
    - ./gradlew clean performanceTest
  artifacts:
    reports:
      junit: reports/TEST-JavaApiTestSimulation.xml
    paths:
      - reports/
    expire_in: 1 week
```

### GitHub Actions Example
```yaml
- name: Run Performance Tests
  run: ./gradlew clean performanceTest

- name: Publish Test Results
  uses: dorny/test-reporter@v1
  if: always()
  with:
    name: Performance Test Results
    path: reports/TEST-JavaApiTestSimulation.xml
    reporter: java-junit
```

## 📈 Understanding Results

### Good Performance Indicators
- ✅ **Response times** under 100ms for simple APIs
- ✅ **Success rate** of 100% or close to it
- ✅ **Stable response times** across different load levels
- ✅ **No failed requests** during normal load

### Performance Metrics Explained
- **Mean Response Time**: Average time for all requests
- **95th Percentile**: 95% of requests completed within this time
- **Requests/sec**: Throughput measurement
- **Success Rate**: Percentage of successful requests

## 🏗️ Build System Migration

### From Maven to Gradle
This project has been migrated from Maven to Gradle with Gatling plugin for:
- **Better Gatling integration** - native plugin support
- **Simplified configuration** - less XML, more concise build scripts
- **Faster execution** - optimized for performance testing workflows
- **Task-based approach** - granular control over test execution phases

### Key Differences
| Aspect | Maven | Gradle (Current) |
|--------|--------|------------------|
| **Build File** | `pom.xml` | `build.gradle` |
| **Test Execution** | `mvn gatling:test` | `./gradlew gatlingRun` |
| **Report Generation** | `mvn exec:java` | `./gradlew generateJUnitXml` |
| **Complete Workflow** | Custom Maven phases | `./gradlew performanceTest` |

## 🤝 Contributing

To add new test scenarios:
1. Create new simulation in `src/test/java/simulations/`
2. Follow the existing pattern in `JavaApiTestSimulation.java`
3. Update this README with new simulation details

## 📄 License

This project is licensed under the MIT License.

## 🆘 Support

For issues or questions:
1. Check the troubleshooting section above
2. Review the sample reports in the `reports/` directory
3. Examine the generated logs for error details
4. Run `./gradlew tasks --group=gatling` to see available Gatling tasks
