b pages# Gatling Java Performance Testing Framework

A comprehensive performance testing suite built with **Gatling Java API** and **Spring Boot**, featuring automated dual report generation (HTML + JUnit XML) with zero external dependencies.

## 🚀 Quick Start

### Prerequisites
- **Java 17+** 
- **Maven 3.6+**
- **Git**

### 1. Clone and Setup
```bash
git clone <your-repo-url>
cd Gatling-Java
mvn clean compile
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
- Execute Gatling performance tests
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
mvn spring-boot:run
# Full Spring Boot application with logging and actuator endpoints
```

#### Run Gatling Tests
```bash
# Run specific simulation
mvn gatling:test -Dgatling.simulationClass=simulations.JavaApiTestSimulation

# Or run with performance profile
mvn test -Pperformance
```

#### Generate JUnit XML Report
```bash
# Using Java implementation (no Python dependency)
mvn exec:java -Dexec.mainClass="com.example.reporting.GatlingJUnitReportGenerator"
```

### Option 3: Maven-Only Approach
```bash
# Complete workflow with Maven
mvn clean gatling:test
mvn exec:java -Dexec.mainClass="com.example.reporting.GatlingJUnitReportGenerator"
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

target/gatling/                   # 📁 Original Gatling output
├── javaapitestsimulation-*/     # 📊 Timestamped HTML reports
└── junit/                       # 🧪 JUnit XML reports
```

## 🔧 Test Configuration

### Available Simulations
- **`JavaApiTestSimulation`** - Main performance test with realistic load patterns
- **`QuickTestSimulation`** - Fast validation test for CI pipelines
- **`JavaPerformanceTest`** - Extended load testing scenarios

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

**Maven compilation errors:**
```bash
# Clean and recompile
mvn clean compile
```

**Reports not generated:**
```bash
# Check if Gatling tests completed successfully
ls -la target/gatling/
# Manually run report generator
mvn exec:java -Dexec.mainClass="com.example.reporting.GatlingJUnitReportGenerator"
```

### Log Locations
- **Gatling logs**: `target/gatling/*/simulation.log`
- **Spring Boot logs**: Console output during `mvn spring-boot:run`
- **Mock API logs**: Console output during `python3 mock-api.py`

## 🔄 CI/CD Integration

### Jenkins Pipeline Example
```groovy
pipeline {
    stages {
        stage('Performance Tests') {
            steps {
                sh './run-tests-with-reports.sh'
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
    - ./run-tests-with-reports.sh
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
  run: ./run-tests-with-reports.sh

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
