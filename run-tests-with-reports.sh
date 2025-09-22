#!/bin/bash

# Automated Gatling Test Execution with Report Generation (Gradle Version)
# This script demonstrates the complete workflow using Gradle with Gatling plugin

echo "🚀 Starting Gatling Test Execution with Gradle-based Build System"
echo "=================================================================="

# Step 1: Clean previous results
echo "📁 Cleaning previous test results..."
rm -rf build/reports/gatling reports
mkdir -p reports

# Step 2: Run Gatling tests using Gradle
echo "🧪 Running Gatling performance tests with Gradle..."
./gradlew clean gatlingRun --simulation=simulations.JavaApiTestSimulation

# Step 3: Check if Gatling tests succeeded
if [ $? -ne 0 ]; then
    echo "❌ Gatling tests failed!"
    exit 1
fi

# Step 4: Find the latest Gatling results directory
LATEST_GATLING_DIR=$(find build/reports/gatling -name "javaapitestsimulation-*" -type d | sort | tail -1)

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
./gradlew generateJUnitXml

# Step 7: Copy JUnit XML report to reports directory
if [ -f "build/gatling/junit/TEST-JavaApiTestSimulation.xml" ]; then
    echo "📋 Copying JUnit XML report to reports directory..."
    cp build/gatling/junit/TEST-JavaApiTestSimulation.xml reports/
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
echo "🔧 JUnit XML report generated using Java implementation with Gradle build system"
echo "🏗️  Build system: Gradle with Gatling plugin (no Maven dependency)"
