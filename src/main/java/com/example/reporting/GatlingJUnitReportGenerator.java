package com.example.reporting;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Java implementation for converting Gatling simulation.log to JUnit XML format
 * This replaces the Python script with a pure Java solution
 */
public class GatlingJUnitReportGenerator {

    private static final Logger LOGGER = Logger.getLogger(GatlingJUnitReportGenerator.class.getName());
    private static final String GATLING_DIR = "build/reports/gatling";
    private static final String JUNIT_DIR = "build/gatling/junit";
    private static final String OUTPUT_FILE = "TEST-JavaApiTestSimulation.xml";

    public static void main(String[] args) {
        try {
            GatlingJUnitReportGenerator generator = new GatlingJUnitReportGenerator();
            TestResults results = generator.parseGatlingLog();
            generator.createJUnitXml(results);
            generator.printSummary(results);
        } catch (Exception e) {
            LOGGER.log(Level.SEVERE, "Error generating JUnit XML report: " + e.getMessage(), e);
            System.exit(1);
        }
    }

    /**
     * Parse Gatling simulation.log file and extract test results
     */
    public TestResults parseGatlingLog() throws IOException {
        Path logFile = findLatestSimulationLog();
        TestResults results = new TestResults();

        try (BufferedReader reader = Files.newBufferedReader(logFile)) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split("\t");
                if (parts.length < 3) continue;

                String recordType = parts[0];

                if ("RUN".equals(recordType)) {
                    // RUN	simulations.JavaApiTestSimulation	javaapitestsimulation	1758575836567	Performance Test Execution	3.11.5
                    if (parts.length >= 4) {
                        results.simulationName = parts[1];
                        results.startTime = Long.parseLong(parts[3]);
                    }
                } else if ("REQUEST".equals(recordType)) {
                    // REQUEST		Health Check	1758575837624	1758575837633	OK
                    if (parts.length >= 6) {
                        String requestName = parts[2];
                        long startTime = Long.parseLong(parts[3]);
                        long endTime = Long.parseLong(parts[4]);
                        String status = parts[5];

                        TestCase testCase = new TestCase();
                        testCase.name = requestName;
                        testCase.classname = results.simulationName;
                        testCase.time = (endTime - startTime) / 1000.0; // Convert to seconds
                        testCase.status = status;
                        testCase.timestamp = startTime;

                        results.totalRequests++;
                        if ("OK".equals(status)) {
                            results.successfulRequests++;
                        } else {
                            results.failedRequests++;
                            testCase.failure = "Request failed with status: " + status;
                        }

                        results.testCases.add(testCase);

                        // Update end time
                        if (results.endTime == null || endTime > results.endTime) {
                            results.endTime = endTime;
                        }
                    }
                }
            }
        }

        if (results.startTime != null && results.endTime != null) {
            results.duration = (results.endTime - results.startTime) / 1000.0;
        }

        return results;
    }

    /**
     * Create JUnit XML from parsed results
     */
    public void createJUnitXml(TestResults results) throws Exception {
        DocumentBuilderFactory docFactory = DocumentBuilderFactory.newInstance();
        DocumentBuilder docBuilder = docFactory.newDocumentBuilder();
        Document doc = docBuilder.newDocument();

        // Create root testsuite element
        Element testsuite = doc.createElement("testsuite");
        doc.appendChild(testsuite);

        testsuite.setAttribute("name", results.simulationName);
        testsuite.setAttribute("errors", "0");
        testsuite.setAttribute("skipped", "0");
        testsuite.setAttribute("time", String.format("%.3f", results.duration));

        if (results.startTime != null) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(results.startTime), ZoneId.systemDefault());
            testsuite.setAttribute("timestamp", dateTime.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        }
        testsuite.setAttribute("hostname", "localhost");
        testsuite.setAttribute("package", "simulations");

        // Group test cases by request name
        Map<String, RequestSummary> requestSummaries = groupTestCases(results.testCases);

        // Create test case elements
        List<Element> testCaseElements = new ArrayList<>();
        for (Map.Entry<String, RequestSummary> entry : requestSummaries.entrySet()) {
            String name = entry.getKey();
            RequestSummary summary = entry.getValue();

            // Main performance test case
            Element testcase = createPerformanceTestCase(doc, results.simulationName, name, summary);
            testCaseElements.add(testcase);

            // Response time assertion test case
            Element responseTimeTest = createResponseTimeAssertionTestCase(doc, results.simulationName, name, summary);
            testCaseElements.add(responseTimeTest);

            // Success rate assertion test case
            Element successRateTest = createSuccessRateAssertionTestCase(doc, results.simulationName, name, summary);
            testCaseElements.add(successRateTest);
        }

        // Add all test cases to testsuite
        for (Element testCase : testCaseElements) {
            testsuite.appendChild(testCase);
        }

        // Update test counts
        testsuite.setAttribute("tests", String.valueOf(testCaseElements.size()));

        long failureCount = testCaseElements.stream()
            .mapToLong(tc -> tc.getElementsByTagName("failure").getLength())
            .sum();
        testsuite.setAttribute("failures", String.valueOf(failureCount));

        // Add testsuite-level properties
        Element suiteProperties = doc.createElement("properties");
        testsuite.appendChild(suiteProperties);

        addProperty(doc, suiteProperties, "total_requests", String.valueOf(results.totalRequests));
        addProperty(doc, suiteProperties, "test_duration_seconds", String.format("%.3f", results.duration));
        addProperty(doc, suiteProperties, "requests_per_second",
            results.duration > 0 ? String.format("%.2f", results.totalRequests / results.duration) : "0");

        // Write XML to file
        writeXmlToFile(doc);
    }

    private Map<String, RequestSummary> groupTestCases(List<TestCase> testCases) {
        Map<String, RequestSummary> summaries = new HashMap<>();

        for (TestCase testCase : testCases) {
            RequestSummary summary = summaries.computeIfAbsent(testCase.name, k -> new RequestSummary());

            summary.count++;
            summary.totalTime += testCase.time;
            summary.times.add(testCase.time);
            summary.minTime = Math.min(summary.minTime, testCase.time);
            summary.maxTime = Math.max(summary.maxTime, testCase.time);

            if (!"OK".equals(testCase.status)) {
                summary.failures++;
                summary.status = "FAILED";
            }
        }

        return summaries;
    }

    private Element createPerformanceTestCase(Document doc, String simulationName, String name, RequestSummary summary) {
        Element testcase = doc.createElement("testcase");
        testcase.setAttribute("classname", simulationName);
        testcase.setAttribute("name", name + "_Performance_Test");
        testcase.setAttribute("time", String.format("%.3f", summary.totalTime / summary.count));

        // Add properties
        Element properties = doc.createElement("properties");
        testcase.appendChild(properties);

        addProperty(doc, properties, "request_count", String.valueOf(summary.count));
        addProperty(doc, properties, "min_response_time_ms", String.valueOf((int)(summary.minTime * 1000)));
        addProperty(doc, properties, "max_response_time_ms", String.valueOf((int)(summary.maxTime * 1000)));
        addProperty(doc, properties, "avg_response_time_ms", String.valueOf((int)((summary.totalTime / summary.count) * 1000)));

        double successRate = ((summary.count - summary.failures) / (double) summary.count) * 100;
        addProperty(doc, properties, "success_rate_percent", String.format("%.1f", successRate));

        // Add system-out with detailed metrics
        Element systemOut = doc.createElement("system-out");
        testcase.appendChild(systemOut);

        String metrics = """
            === Performance Test Results for %s ===
            Total Requests: %d
            Successful Requests: %d
            Failed Requests: %d
            Success Rate: %.1f%%
            Response Times (ms):
              Min: %d
              Max: %d
              Average: %d
            Performance Assertions:
              Max Response Time < 5000ms: %s
              Success Rate > 90%%: %s""".formatted(
            name,
            summary.count,
            summary.count - summary.failures,
            summary.failures,
            successRate,
            (int)(summary.minTime * 1000),
            (int)(summary.maxTime * 1000),
            (int)((summary.totalTime / summary.count) * 1000),
            summary.maxTime * 1000 < 5000 ? "PASS" : "FAIL",
            successRate > 90 ? "PASS" : "FAIL"
        );
        systemOut.setTextContent(metrics);

        return testcase;
    }

    private Element createResponseTimeAssertionTestCase(Document doc, String simulationName, String name, RequestSummary summary) {
        Element testcase = doc.createElement("testcase");
        testcase.setAttribute("classname", simulationName);
        testcase.setAttribute("name", name + "_Response_Time_Under_5000ms");
        testcase.setAttribute("time", "0.001");

        if (summary.maxTime * 1000 >= 5000) {
            Element failure = doc.createElement("failure");
            failure.setAttribute("message", String.format("Max response time %dms exceeds 5000ms threshold", (int)(summary.maxTime * 1000)));
            failure.setAttribute("type", "AssertionError");
            testcase.appendChild(failure);
        }

        return testcase;
    }

    private Element createSuccessRateAssertionTestCase(Document doc, String simulationName, String name, RequestSummary summary) {
        Element testcase = doc.createElement("testcase");
        testcase.setAttribute("classname", simulationName);
        testcase.setAttribute("name", name + "_Success_Rate_Above_90_Percent");
        testcase.setAttribute("time", "0.001");

        double successRate = ((summary.count - summary.failures) / (double) summary.count) * 100;
        if (successRate < 90) {
            Element failure = doc.createElement("failure");
            failure.setAttribute("message", String.format("Success rate %.1f%% is below 90%% threshold", successRate));
            failure.setAttribute("type", "AssertionError");
            testcase.appendChild(failure);
        }

        return testcase;
    }

    private void addProperty(Document doc, Element properties, String name, String value) {
        Element property = doc.createElement("property");
        property.setAttribute("name", name);
        property.setAttribute("value", value);
        properties.appendChild(property);
    }

    private void writeXmlToFile(Document doc) throws Exception {
        // Create output directory
        Path junitDir = Paths.get(JUNIT_DIR);
        Files.createDirectories(junitDir);

        // Write XML to file
        TransformerFactory transformerFactory = TransformerFactory.newInstance();
        Transformer transformer = transformerFactory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        DOMSource source = new DOMSource(doc);
        StreamResult result = new StreamResult(new File(junitDir.toFile(), OUTPUT_FILE));
        transformer.transform(source, result);
    }

    private Path findLatestSimulationLog() throws IOException {
        Path gatlingDir = Paths.get(GATLING_DIR);
        if (!Files.exists(gatlingDir)) {
            throw new IOException("Gatling results directory not found: " + GATLING_DIR);
        }

        try (Stream<Path> stream = Files.list(gatlingDir)) {
            Optional<Path> latestSimDir = stream
                .filter(Files::isDirectory)
                .filter(path -> path.getFileName().toString().startsWith("javaapitestsimulation"))
                .max(Comparator.comparing(path -> path.getFileName().toString()));

            if (latestSimDir.isEmpty()) {
                throw new IOException("No Gatling simulation results found");
            }

            Path logFile = latestSimDir.get().resolve("simulation.log");
            if (!Files.exists(logFile)) {
                throw new IOException("simulation.log not found in " + latestSimDir.get());
            }

            return logFile;
        }
    }

    private void printSummary(TestResults results) {
        System.out.println("JUnit XML report generated: " + JUNIT_DIR + "/" + OUTPUT_FILE);
        System.out.println("Total requests: " + results.totalRequests);
        System.out.println("Successful: " + results.successfulRequests);
        System.out.println("Failed: " + results.failedRequests);

        if (results.totalRequests > 0) {
            double successRate = (results.successfulRequests / (double) results.totalRequests) * 100;
            System.out.println("Success rate: " + String.format("%.2f%%", successRate));
        } else {
            System.out.println("Success rate: N/A");
        }

        System.out.println("Test duration: " + String.format("%.2f seconds", results.duration));
    }

    // Inner classes with proper visibility
    public static class TestResults {
        String simulationName;
        Long startTime;
        Long endTime;
        double duration;
        int totalRequests = 0;
        int successfulRequests = 0;
        int failedRequests = 0;
        List<TestCase> testCases = new ArrayList<>();
    }

    public static class TestCase {
        String name;
        String classname;
        double time;
        String status;
        long timestamp;
        String failure;
    }

    public static class RequestSummary {
        int count = 0;
        int failures = 0;
        double totalTime = 0.0;
        double minTime = Double.MAX_VALUE;
        double maxTime = 0.0;
        String status = "OK";
        List<Double> times = new ArrayList<>();
    }
}
