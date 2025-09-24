# Gatling JUnit Report Generator - Integration Guide

## Overview
This JUnit report generator converts Gatling simulation.log files into JUnit XML format, enabling integration with CI/CD pipelines, test reporting dashboards, and build tools that expect JUnit XML output.

## Integration with Any Gatling Java Repository

### 1. Copy Required Files
Copy these files to your existing Gatling Java project:

```
src/main/java/com/example/reporting/GatlingJUnitReportGenerator.java
```

### 2. Add Gradle Dependencies
Add to your `build.gradle`:

```groovy
dependencies {
    // Standard Gatling dependencies
    gatling 'io.gatling.highcharts:gatling-charts-highcharts:3.11.5'
    
    // Required for JUnit XML generation
    implementation 'org.w3c:dom:2.3.0'
    implementation 'javax.xml.parsers:jaxp-api:1.4.5'
}
```

### 3. Add Gradle Tasks
Add these tasks to your `build.gradle`:

```groovy
// Task to generate JUnit XML reports
task generateJUnitXml(type: JavaExec) {
    group = 'reporting'
    description = 'Generate JUnit XML report from Gatling results'
    classpath = sourceSets.main.runtimeClasspath
    mainClass = 'com.example.reporting.GatlingJUnitReportGenerator'
    
    dependsOn 'gatlingRun'
}

// Complete performance test workflow
task performanceTest {
    group = 'verification'
    description = 'Run performance tests with dual report generation'
    
    dependsOn 'clean', 'compileJava', 'gatlingRun', 'generateJUnitXml'
    
    doLast {
        println "✅ Performance tests completed!"
        println "📊 HTML Report: build/reports/gatling/[simulation-name]/index.html"
        println "🧪 JUnit XML: build/gatling/junit/TEST-[SimulationName].xml"
    }
}
```

## Available Gradle Goals/Tasks

### Primary Goals to Use:

1. **`gradle performanceTest`** (RECOMMENDED)
   - Runs complete workflow: clean → compile → gatling tests → generate JUnit XML
   - Best for CI/CD pipelines
   - Generates both HTML and XML reports

2. **`gradle gatlingRun`**
   - Runs only Gatling performance tests
   - Generates standard HTML reports
   - Use when you only need Gatling reports

3. **`gradle generateJUnitXml`**
   - Converts existing Gatling results to JUnit XML
   - Use after running `gatlingRun` separately
   - Requires Gatling results to already exist

4. **`gradle quickPerformanceTest`**
   - Runs lightweight performance validation
   - Good for development/debugging

### CI/CD Integration Examples:

#### Jenkins Pipeline
```groovy
pipeline {
    agent any
    stages {
        stage('Performance Tests') {
            steps {
                sh './gradlew performanceTest'
            }
            post {
                always {
                    // Publish JUnit XML results
                    junit 'build/gatling/junit/*.xml'
                    
                    // Archive HTML reports
                    publishHTML([
                        allowMissing: false,
                        alwaysLinkToLastBuild: true,
                        keepAll: true,
                        reportDir: 'build/reports/gatling',
                        reportFiles: 'index.html',
                        reportName: 'Gatling Performance Report'
                    ])
                }
            }
        }
    }
}
```

#### GitHub Actions
```yaml
- name: Run Performance Tests
  run: ./gradlew performanceTest

- name: Publish Test Results
  uses: dorny/test-reporter@v1
  if: always()
  with:
    name: Performance Tests
    path: 'build/gatling/junit/*.xml'
    reporter: java-junit

- name: Upload HTML Report
  uses: actions/upload-artifact@v3
  with:
    name: gatling-report
    path: build/reports/gatling/
```

## Output Formats

### 1. Standard Gatling HTML Report
- **Location**: `build/reports/gatling/[simulation-name]/index.html`
- **Features**: Interactive charts, detailed metrics, request/response analysis
- **Best for**: Manual analysis, stakeholder presentations

### 2. JUnit XML Report
- **Location**: `build/gatling/junit/TEST-[SimulationName].xml`
- **Features**: Test cases for each endpoint, performance assertions, CI/CD integration
- **Best for**: Build pipeline integration, automated reporting

## Customization Options

### 1. Modify Output Directories
Update constants in `GatlingJUnitReportGenerator.java`:
```java
private static final String GATLING_DIR = "build/reports/gatling";
private static final String JUNIT_DIR = "build/test-results/junit";
private static final String OUTPUT_FILE = "TEST-PerformanceTests.xml";
```

### 2. Adjust Performance Thresholds
Modify assertion criteria:
```java
// Response time threshold (default: 5000ms)
summary.maxTime * 1000 < 3000 ? "PASS" : "FAIL"

// Success rate threshold (default: 90%)
successRate > 95 ? "PASS" : "FAIL"
```

### 3. Custom Test Case Naming
Update test case generation logic to match your naming conventions.

## File Structure After Integration

```
your-gatling-project/
├── build.gradle                           # Updated with new tasks
├── src/
│   ├── main/java/
│   │   └── your/package/reporting/
│   │       └── GatlingJUnitReportGenerator.java  # Copy this file
│   └── test/java/
│       └── simulations/
│           └── YourSimulation.java         # Your existing tests
├── build/
│   ├── reports/gatling/                    # Standard HTML reports
│   └── gatling/junit/                      # Generated JUnit XML
└── reports/                                # Centralized reports (optional)
```

## Benefits of Dual Report Generation

1. **HTML Reports**: Rich, interactive analysis for developers
2. **JUnit XML**: Standardized format for CI/CD integration
3. **Performance Assertions**: Automatic pass/fail based on thresholds
4. **Build Integration**: Seamless integration with existing test suites
5. **Historical Tracking**: Trend analysis in CI/CD dashboards

## Troubleshooting

### Common Issues:
1. **Missing simulation.log**: Ensure Gatling tests ran successfully first
2. **Directory not found**: Check GATLING_DIR path matches your setup
3. **JUnit XML not recognized**: Verify XML structure meets your CI tool requirements

### Debug Mode:
Add this to see detailed logging:
```java
System.setProperty("java.util.logging.level", "ALL");
```

## Conclusion

This JUnit report generator provides a bridge between Gatling's powerful performance testing capabilities and standard CI/CD tooling. Use `gradle performanceTest` as your primary goal for complete workflow automation.
