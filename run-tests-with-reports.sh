#!/bin/bash

# Automated Gatling Test Execution with Report Generation
# This script demonstrates the complete workflow using Java-based JUnit XML generation

echo "🚀 Starting Gatling Test Execution with Java-based JUnit XML Generation"
echo "=================================================================="

# Step 1: Clean previous results
echo "📁 Cleaning previous test results..."
rm -rf target/gatling reports
mkdir -p reports

# Step 2: Run Gatling tests (this will now auto-generate JUnit XML via Maven exec plugin)
echo "🧪 Running Gatling performance tests..."
mvn clean gatling:test -Dgatling.simulationClass=simulations.JavaApiTestSimulation

# Step 3: Check if Gatling tests succeeded
if [ $? -ne 0 ]; then
    echo "❌ Gatling tests failed!"
    exit 1
fi

# Step 4: Find the latest Gatling results directory
LATEST_GATLING_DIR=$(find target/gatling -name "javaapitestsimulation-*" -type d | sort | tail -1)

if [ -z "$LATEST_GATLING_DIR" ]; then
    echo "❌ No Gatling results found!"
    exit 1
fi

echo "📊 Latest Gatling results found in: $LATEST_GATLING_DIR"

# Step 5: Copy HTML reports to the reports directory
echo "📋 Copying HTML reports to reports directory..."
cp -r "$LATEST_GATLING_DIR"/* reports/

# Step 6: Generate JUnit XML report using Java implementation
echo "📋 Generating JUnit XML report using Java implementation..."
mvn exec:java -Dexec.mainClass="com.example.reporting.GatlingJUnitReportGenerator" -Dexec.classpathScope=compile

# Step 7: Copy JUnit XML report to reports directory
if [ -f "target/gatling/junit/TEST-JavaApiTestSimulation.xml" ]; then
    echo "📋 Copying JUnit XML report to reports directory..."
    cp target/gatling/junit/TEST-JavaApiTestSimulation.xml reports/
else
    echo "⚠️  JUnit XML report not found at expected location"
    exit 1
fi

# Step 8: Generate summary
echo ""
echo "✅ Test Execution Complete!"
echo "=================================================================="
echo "📁 Reports Location: $(pwd)/reports/"
echo "📊 HTML Report: reports/index.html"
echo "🧪 JUnit XML: reports/TEST-JavaApiTestSimulation.xml"
echo ""

# Step 9: Display quick summary from JUnit XML
if [ -f "reports/TEST-JavaApiTestSimulation.xml" ]; then
    TOTAL_TESTS=$(grep -o 'tests="[0-9]*"' reports/TEST-JavaApiTestSimulation.xml | cut -d'"' -f2)
    FAILURES=$(grep -o 'failures="[0-9]*"' reports/TEST-JavaApiTestSimulation.xml | cut -d'"' -f2)
    DURATION=$(grep -o 'time="[0-9.]*"' reports/TEST-JavaApiTestSimulation.xml | cut -d'"' -f2)

    echo "📈 Test Summary:"
    echo "   Total Test Cases: $TOTAL_TESTS"
    echo "   Failures: $FAILURES"
    echo "   Success Rate: $((($TOTAL_TESTS - $FAILURES) * 100 / $TOTAL_TESTS))%"
    echo "   Duration: ${DURATION}s"
fi

echo ""
echo "🌐 Open reports/index.html in your browser to view detailed results"
echo "🔧 JUnit XML report generated using pure Java implementation (no Python dependency)"
