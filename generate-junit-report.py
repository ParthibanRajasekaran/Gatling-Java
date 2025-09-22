#!/usr/bin/env python3
"""
Convert Gatling simulation.log to JUnit XML format
"""
import xml.etree.ElementTree as ET
import sys
import os
import re
from datetime import datetime

def parse_gatling_log(log_file):
    """Parse Gatling simulation.log file and extract test results"""
    results = {
        'total_requests': 0,
        'successful_requests': 0,
        'failed_requests': 0,
        'test_cases': [],
        'start_time': None,
        'end_time': None,
        'duration': 0,
        'simulation_name': 'JavaApiTestSimulation'
    }

    with open(log_file, 'r') as f:
        for line in f:
            parts = line.strip().split('\t')
            if len(parts) < 3:
                continue

            record_type = parts[0]

            if record_type == 'RUN':
                # RUN	simulations.JavaApiTestSimulation	javaapitestsimulation	1758575836567	Performance Test Execution	3.11.5
                if len(parts) >= 4:
                    results['simulation_name'] = parts[1]
                    results['start_time'] = int(parts[3])

            elif record_type == 'REQUEST':
                # REQUEST		Health Check	1758575837624	1758575837633	OK
                if len(parts) >= 6:
                    request_name = parts[2]
                    start_time = int(parts[3])
                    end_time = int(parts[4])
                    status = parts[5]

                    results['total_requests'] += 1

                    test_case = {
                        'name': request_name,
                        'classname': results['simulation_name'],
                        'time': (end_time - start_time) / 1000.0,  # Convert to seconds
                        'status': status,
                        'timestamp': start_time
                    }

                    if status == 'OK':
                        results['successful_requests'] += 1
                    else:
                        results['failed_requests'] += 1
                        test_case['failure'] = f"Request failed with status: {status}"

                    results['test_cases'].append(test_case)

                    # Update end time
                    if results['end_time'] is None or end_time > results['end_time']:
                        results['end_time'] = end_time

    if results['start_time'] and results['end_time']:
        results['duration'] = (results['end_time'] - results['start_time']) / 1000.0

    return results

def create_junit_xml(results, output_file):
    """Create JUnit XML from parsed results"""
    root = ET.Element('testsuite')
    root.set('name', results['simulation_name'])
    root.set('tests', str(results['total_requests']))
    root.set('failures', str(results['failed_requests']))
    root.set('errors', '0')
    root.set('skipped', '0')
    root.set('time', f"{results['duration']:.3f}")  # Round to 3 decimal places
    root.set('timestamp', datetime.fromtimestamp(results['start_time'] / 1000).strftime('%Y-%m-%dT%H:%M:%S') if results['start_time'] else '')
    root.set('hostname', 'localhost')
    root.set('package', 'simulations')

    # Group test cases by request name to create meaningful test cases
    request_summary = {}
    for test_case in results['test_cases']:
        name = test_case['name']
        if name not in request_summary:
            request_summary[name] = {
                'count': 0,
                'failures': 0,
                'total_time': 0,
                'min_time': float('inf'),
                'max_time': 0,
                'status': 'OK',
                'times': []
            }

        summary = request_summary[name]
        summary['count'] += 1
        summary['total_time'] += test_case['time']
        summary['times'].append(test_case['time'])
        summary['min_time'] = min(summary['min_time'], test_case['time'])
        summary['max_time'] = max(summary['max_time'], test_case['time'])

        if test_case['status'] != 'OK':
            summary['failures'] += 1
            summary['status'] = 'FAILED'

    # Create individual test case elements for each request instance
    # This provides more granular reporting for CI/CD systems
    test_case_id = 1
    for name, summary in request_summary.items():
        # Create a representative test case for this endpoint
        testcase = ET.SubElement(root, 'testcase')
        testcase.set('classname', f"{results['simulation_name']}")
        testcase.set('name', f"{name}_Performance_Test")
        testcase.set('time', f"{summary['total_time'] / summary['count']:.3f}")  # Average time rounded to 3 decimals

        # Add assertions as separate test cases
        # Response Time Assertion
        response_time_test = ET.SubElement(root, 'testcase')
        response_time_test.set('classname', f"{results['simulation_name']}")
        response_time_test.set('name', f"{name}_Response_Time_Under_5000ms")
        response_time_test.set('time', '0.001')

        if summary['max_time'] * 1000 >= 5000:  # Convert to ms and check
            failure = ET.SubElement(response_time_test, 'failure')
            failure.set('message', f'Max response time {int(summary["max_time"] * 1000)}ms exceeds 5000ms threshold')
            failure.set('type', 'AssertionError')

        # Success Rate Assertion
        success_rate_test = ET.SubElement(root, 'testcase')
        success_rate_test.set('classname', f"{results['simulation_name']}")
        success_rate_test.set('name', f"{name}_Success_Rate_Above_90_Percent")
        success_rate_test.set('time', '0.001')

        success_rate = ((summary['count'] - summary['failures']) / summary['count']) * 100
        if success_rate < 90:
            failure = ET.SubElement(success_rate_test, 'failure')
            failure.set('message', f'Success rate {success_rate:.1f}% is below 90% threshold')
            failure.set('type', 'AssertionError')

        # Add performance properties to the main test case
        properties = ET.SubElement(testcase, 'properties')

        prop_count = ET.SubElement(properties, 'property')
        prop_count.set('name', 'request_count')
        prop_count.set('value', str(summary['count']))

        prop_min = ET.SubElement(properties, 'property')
        prop_min.set('name', 'min_response_time_ms')
        prop_min.set('value', str(int(summary['min_time'] * 1000)))

        prop_max = ET.SubElement(properties, 'property')
        prop_max.set('name', 'max_response_time_ms')
        prop_max.set('value', str(int(summary['max_time'] * 1000)))

        prop_avg = ET.SubElement(properties, 'property')
        prop_avg.set('name', 'avg_response_time_ms')
        prop_avg.set('value', str(int((summary['total_time'] / summary['count']) * 1000)))

        prop_success = ET.SubElement(properties, 'property')
        prop_success.set('name', 'success_rate_percent')
        prop_success.set('value', f"{success_rate:.1f}")

        # Add system-out with detailed metrics (more CI/CD friendly format)
        system_out = ET.SubElement(testcase, 'system-out')
        metrics = [
            f"=== Performance Test Results for {name} ===",
            f"Total Requests: {summary['count']}",
            f"Successful Requests: {summary['count'] - summary['failures']}",
            f"Failed Requests: {summary['failures']}",
            f"Success Rate: {success_rate:.1f}%",
            f"Response Times (ms):",
            f"  Min: {int(summary['min_time'] * 1000)}",
            f"  Max: {int(summary['max_time'] * 1000)}",
            f"  Average: {int((summary['total_time'] / summary['count']) * 1000)}",
            f"Performance Assertions:",
            f"  Max Response Time < 5000ms: {'PASS' if summary['max_time'] * 1000 < 5000 else 'FAIL'}",
            f"  Success Rate > 90%: {'PASS' if success_rate > 90 else 'FAIL'}"
        ]
        system_out.text = '\n'.join(metrics)

    # Add testsuite-level properties
    suite_properties = ET.SubElement(root, 'properties')

    prop_total = ET.SubElement(suite_properties, 'property')
    prop_total.set('name', 'total_requests')
    prop_total.set('value', str(results['total_requests']))

    prop_duration = ET.SubElement(suite_properties, 'property')
    prop_duration.set('name', 'test_duration_seconds')
    prop_duration.set('value', f"{results['duration']:.3f}")

    prop_rps = ET.SubElement(suite_properties, 'property')
    prop_rps.set('name', 'requests_per_second')
    prop_rps.set('value', f"{results['total_requests'] / results['duration']:.2f}" if results['duration'] > 0 else "0")

    # Update test counts to reflect the actual number of test cases created
    total_test_cases = len(list(root.findall('testcase')))
    root.set('tests', str(total_test_cases))

    # Count actual failures
    failure_count = len(list(root.findall('.//failure')))
    root.set('failures', str(failure_count))

    # Write XML to file
    tree = ET.ElementTree(root)
    ET.indent(tree, space="  ", level=0)
    tree.write(output_file, encoding='utf-8', xml_declaration=True)

def main():
    # Find the latest Gatling results directory
    gatling_dir = 'target/gatling'
    if not os.path.exists(gatling_dir):
        print(f"Error: Gatling results directory not found: {gatling_dir}")
        sys.exit(1)

    # Find the most recent simulation directory
    sim_dirs = [d for d in os.listdir(gatling_dir) if os.path.isdir(os.path.join(gatling_dir, d)) and d.startswith('javaapitestsimulation')]
    if not sim_dirs:
        print("Error: No Gatling simulation results found")
        sys.exit(1)

    latest_sim_dir = max(sim_dirs)
    log_file = os.path.join(gatling_dir, latest_sim_dir, 'simulation.log')

    if not os.path.exists(log_file):
        print(f"Error: simulation.log not found in {log_file}")
        sys.exit(1)

    # Create output directory
    junit_dir = os.path.join(gatling_dir, 'junit')
    os.makedirs(junit_dir, exist_ok=True)

    # Parse results and create JUnit XML
    results = parse_gatling_log(log_file)
    output_file = os.path.join(junit_dir, 'TEST-JavaApiTestSimulation.xml')
    create_junit_xml(results, output_file)

    print(f"JUnit XML report generated: {output_file}")
    print(f"Total requests: {results['total_requests']}")
    print(f"Successful: {results['successful_requests']}")
    print(f"Failed: {results['failed_requests']}")
    print(f"Success rate: {round((results['successful_requests'] / results['total_requests']) * 100, 2)}%" if results['total_requests'] > 0 else "N/A")
    print(f"Test duration: {results['duration']:.2f} seconds")

if __name__ == '__main__':
    main()
