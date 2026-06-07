#!/usr/bin/env python3
"""
DAG Agent Benchmark Analysis and Report Generation Tool

Usage:
    python3 benchmark-analyzer.py <metrics_csv_file> [--output <output_dir>]

Example:
    python3 benchmark-analyzer.py benchmark_results_20240526_120000/metrics.csv
"""

import csv
import sys
import os
import json
from pathlib import Path
from statistics import mean, median, stdev
from datetime import datetime
import argparse


class BenchmarkAnalyzer:
    """Analyze benchmark CSV metrics and generate reports."""

    def __init__(self, csv_file, output_dir=None):
        """Initialize analyzer with metrics CSV file."""
        self.csv_file = csv_file
        self.output_dir = output_dir or os.path.dirname(csv_file) or "."
        self.metrics = []
        self.latencies = []
        self.load_metrics()

    def load_metrics(self):
        """Load metrics from CSV file."""
        if not os.path.exists(self.csv_file):
            raise FileNotFoundError(f"Metrics file not found: {self.csv_file}")

        with open(self.csv_file, 'r') as f:
            reader = csv.DictReader(f)
            for row in reader:
                try:
                    row['latency_ms'] = float(row['latency_ms'])
                    row['submission_time_ms'] = float(row['submission_time_ms'])
                    self.metrics.append(row)
                    if row['status'] == 'success':
                        self.latencies.append(row['latency_ms'])
                except (ValueError, KeyError) as e:
                    print(f"Warning: Skipping invalid row: {row}", file=sys.stderr)
                    continue

        if not self.metrics:
            raise ValueError("No valid metrics found in CSV file")

    def get_summary(self):
        """Generate summary statistics."""
        if not self.metrics:
            return {}

        total = len(self.metrics)
        success = sum(1 for m in self.metrics if m['status'] == 'success')
        failed = total - success

        summary = {
            'total_tasks': total,
            'successful_tasks': success,
            'failed_tasks': failed,
            'success_rate_percent': (success / total * 100) if total > 0 else 0,
        }

        if self.latencies:
            summary.update({
                'latency_min_ms': min(self.latencies),
                'latency_max_ms': max(self.latencies),
                'latency_mean_ms': round(mean(self.latencies), 2),
                'latency_median_ms': median(self.latencies),
                'latency_stddev_ms': round(stdev(self.latencies), 2) if len(self.latencies) > 1 else 0,
            })

            # Calculate percentiles
            sorted_latencies = sorted(self.latencies)
            summary['latency_p50_ms'] = sorted_latencies[int(len(sorted_latencies) * 0.50)]
            summary['latency_p95_ms'] = sorted_latencies[int(len(sorted_latencies) * 0.95)]
            summary['latency_p99_ms'] = sorted_latencies[int(len(sorted_latencies) * 0.99)]

        return summary

    def get_throughput(self):
        """Calculate throughput metrics."""
        if not self.metrics or len(self.metrics) < 2:
            return {'throughput_tasks_per_sec': 0}

        # Find time range
        times = [float(m['submission_time_ms']) for m in self.metrics]
        time_min = min(times)
        time_max = max(times)
        duration_ms = time_max - time_min

        if duration_ms <= 0:
            return {'throughput_tasks_per_sec': 0}

        successful = sum(1 for m in self.metrics if m['status'] == 'success')
        throughput = (successful / duration_ms) * 1000  # Convert to per second

        return {
            'throughput_tasks_per_sec': round(throughput, 2),
            'test_duration_ms': int(duration_ms),
        }

    def get_latency_distribution(self):
        """Get latency distribution buckets."""
        if not self.latencies:
            return {}

        buckets = {
            '<1ms': len([l for l in self.latencies if l < 1]),
            '1-5ms': len([l for l in self.latencies if 1 <= l < 5]),
            '5-10ms': len([l for l in self.latencies if 5 <= l < 10]),
            '10-20ms': len([l for l in self.latencies if 10 <= l < 20]),
            '20-50ms': len([l for l in self.latencies if 20 <= l < 50]),
            '50-100ms': len([l for l in self.latencies if 50 <= l < 100]),
            '>100ms': len([l for l in self.latencies if l >= 100]),
        }

        return buckets

    def generate_json_report(self, filename=None):
        """Generate JSON report."""
        if filename is None:
            filename = os.path.join(self.output_dir, 'benchmark_analysis.json')

        report = {
            'generated_at': datetime.now().isoformat(),
            'summary': self.get_summary(),
            'throughput': self.get_throughput(),
            'latency_distribution': self.get_latency_distribution(),
        }

        with open(filename, 'w') as f:
            json.dump(report, f, indent=2)

        return filename

    def generate_text_report(self, filename=None):
        """Generate human-readable text report."""
        if filename is None:
            filename = os.path.join(self.output_dir, 'benchmark_analysis.txt')

        summary = self.get_summary()
        throughput = self.get_throughput()
        distribution = self.get_latency_distribution()

        with open(filename, 'w') as f:
            f.write("=" * 70 + "\n")
            f.write("DAG Agent Benchmark Analysis Report\n")
            f.write("=" * 70 + "\n\n")

            f.write(f"Generated: {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}\n")
            f.write(f"Source File: {os.path.abspath(self.csv_file)}\n\n")

            # Summary Statistics
            f.write("SUMMARY STATISTICS\n")
            f.write("-" * 70 + "\n")
            f.write(f"Total Tasks:           {summary.get('total_tasks', 0)}\n")
            f.write(f"Successful:            {summary.get('successful_tasks', 0)}\n")
            f.write(f"Failed:                {summary.get('failed_tasks', 0)}\n")
            f.write(f"Success Rate:          {summary.get('success_rate_percent', 0):.2f}%\n\n")

            # Throughput
            f.write("THROUGHPUT\n")
            f.write("-" * 70 + "\n")
            f.write(f"Throughput:            {throughput.get('throughput_tasks_per_sec', 0)} tasks/sec\n")
            f.write(f"Test Duration:         {throughput.get('test_duration_ms', 0)} ms\n\n")

            # Latency Statistics
            if summary.get('latency_min_ms') is not None:
                f.write("LATENCY STATISTICS (ms)\n")
                f.write("-" * 70 + "\n")
                f.write(f"Minimum:               {summary.get('latency_min_ms', 0):.2f} ms\n")
                f.write(f"Maximum:               {summary.get('latency_max_ms', 0):.2f} ms\n")
                f.write(f"Mean:                  {summary.get('latency_mean_ms', 0):.2f} ms\n")
                f.write(f"Median (P50):          {summary.get('latency_p50_ms', 0):.2f} ms\n")
                f.write(f"P95:                   {summary.get('latency_p95_ms', 0):.2f} ms\n")
                f.write(f"P99:                   {summary.get('latency_p99_ms', 0):.2f} ms\n")
                f.write(f"Std Dev:               {summary.get('latency_stddev_ms', 0):.2f} ms\n\n")

            # Latency Distribution
            if distribution:
                f.write("LATENCY DISTRIBUTION\n")
                f.write("-" * 70 + "\n")
                total_latencies = sum(distribution.values())
                for bucket, count in distribution.items():
                    percentage = (count / total_latencies * 100) if total_latencies > 0 else 0
                    f.write(f"{bucket:12} : {count:5} ({percentage:5.1f}%)\n")
                f.write("\n")

            f.write("=" * 70 + "\n")
            f.write("End of Report\n")
            f.write("=" * 70 + "\n")

        return filename

    def generate_html_report(self, filename=None):
        """Generate HTML report with charts."""
        if filename is None:
            filename = os.path.join(self.output_dir, 'benchmark_analysis.html')

        summary = self.get_summary()
        throughput = self.get_throughput()
        distribution = self.get_latency_distribution()

        # Prepare distribution data for chart
        dist_labels = list(distribution.keys())
        dist_values = list(distribution.values())

        html_content = f"""
<!DOCTYPE html>
<html>
<head>
    <title>DAG Agent Benchmark Analysis Report</title>
    <script src="https://cdn.jsdelivr.net/npm/chart.js"></script>
    <style>
        * {{
            margin: 0;
            padding: 0;
            box-sizing: border-box;
        }}
        body {{
            font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, Oxygen, Ubuntu, Cantarell, sans-serif;
            background: linear-gradient(135deg, #667eea 0%, #764ba2 100%);
            padding: 20px;
            min-height: 100vh;
        }}
        .container {{
            max-width: 1200px;
            margin: 0 auto;
            background: white;
            border-radius: 10px;
            box-shadow: 0 10px 40px rgba(0,0,0,0.3);
            padding: 40px;
        }}
        h1 {{
            color: #333;
            margin-bottom: 10px;
            text-align: center;
        }}
        .subtitle {{
            text-align: center;
            color: #666;
            margin-bottom: 30px;
            font-size: 14px;
        }}
        .metrics-grid {{
            display: grid;
            grid-template-columns: repeat(auto-fit, minmax(250px, 1fr));
            gap: 20px;
            margin-bottom: 40px;
        }}
        .metric-card {{
            background: #f8f9fa;
            border-left: 4px solid #667eea;
            padding: 20px;
            border-radius: 5px;
            transition: transform 0.2s;
        }}
        .metric-card:hover {{
            transform: translateY(-5px);
            box-shadow: 0 5px 20px rgba(0,0,0,0.1);
        }}
        .metric-label {{
            color: #666;
            font-size: 12px;
            text-transform: uppercase;
            font-weight: 600;
            margin-bottom: 8px;
        }}
        .metric-value {{
            color: #333;
            font-size: 28px;
            font-weight: bold;
        }}
        .metric-unit {{
            color: #999;
            font-size: 14px;
            margin-left: 5px;
        }}
        .chart-container {{
            position: relative;
            height: 400px;
            margin-bottom: 40px;
            background: #f8f9fa;
            padding: 20px;
            border-radius: 5px;
        }}
        .chart-title {{
            font-size: 16px;
            font-weight: 600;
            color: #333;
            margin-bottom: 20px;
        }}
        .status {{
            padding: 5px 10px;
            border-radius: 3px;
            font-size: 12px;
            font-weight: 600;
        }}
        .status.good {{
            background: #d4edda;
            color: #155724;
        }}
        .status.warning {{
            background: #fff3cd;
            color: #856404;
        }}
        .status.bad {{
            background: #f8d7da;
            color: #721c24;
        }}
        .footer {{
            border-top: 1px solid #eee;
            margin-top: 40px;
            padding-top: 20px;
            color: #999;
            font-size: 12px;
        }}
        table {{
            width: 100%;
            border-collapse: collapse;
            margin-bottom: 30px;
        }}
        th, td {{
            padding: 12px;
            text-align: left;
            border-bottom: 1px solid #eee;
        }}
        th {{
            background: #f8f9fa;
            font-weight: 600;
            color: #333;
        }}
        tr:hover {{
            background: #f8f9fa;
        }}
    </style>
</head>
<body>
    <div class="container">
        <h1>DAG Agent Benchmark Analysis Report</h1>
        <div class="subtitle">Generated {datetime.now().strftime('%Y-%m-%d %H:%M:%S')}</div>

        <!-- Key Metrics -->
        <div class="metrics-grid">
            <div class="metric-card">
                <div class="metric-label">Total Tasks</div>
                <div class="metric-value">{summary.get('total_tasks', 0)}</div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Success Rate</div>
                <div class="metric-value">{summary.get('success_rate_percent', 0):.2f}<span class="metric-unit">%</span></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Throughput</div>
                <div class="metric-value">{throughput.get('throughput_tasks_per_sec', 0)}<span class="metric-unit">tasks/s</span></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">P95 Latency</div>
                <div class="metric-value">{summary.get('latency_p95_ms', 0):.2f}<span class="metric-unit">ms</span></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">P99 Latency</div>
                <div class="metric-value">{summary.get('latency_p99_ms', 0):.2f}<span class="metric-unit">ms</span></div>
            </div>
            <div class="metric-card">
                <div class="metric-label">Mean Latency</div>
                <div class="metric-value">{summary.get('latency_mean_ms', 0):.2f}<span class="metric-unit">ms</span></div>
            </div>
        </div>

        <!-- Summary Table -->
        <div class="chart-container" style="height: auto;">
            <div class="chart-title">Test Summary</div>
            <table>
                <tr>
                    <th>Metric</th>
                    <th>Value</th>
                </tr>
                <tr>
                    <td>Successful Tasks</td>
                    <td><span class="status good">{summary.get('successful_tasks', 0)}</span></td>
                </tr>
                <tr>
                    <td>Failed Tasks</td>
                    <td><span class="status bad">{summary.get('failed_tasks', 0)}</span></td>
                </tr>
                <tr>
                    <td>Test Duration</td>
                    <td>{throughput.get('test_duration_ms', 0):.0f} ms</td>
                </tr>
                <tr>
                    <td>Min Latency</td>
                    <td>{summary.get('latency_min_ms', 0):.2f} ms</td>
                </tr>
                <tr>
                    <td>Max Latency</td>
                    <td>{summary.get('latency_max_ms', 0):.2f} ms</td>
                </tr>
                <tr>
                    <td>Std Dev</td>
                    <td>{summary.get('latency_stddev_ms', 0):.2f} ms</td>
                </tr>
            </table>
        </div>

        <!-- Latency Distribution Chart -->
        <div class="chart-container">
            <div class="chart-title">Latency Distribution</div>
            <canvas id="latencyChart"></canvas>
        </div>

        <div class="footer">
            <strong>Source:</strong> {os.path.abspath(self.csv_file)}<br>
            <strong>Tool:</strong> DAG Agent Benchmark Analyzer
        </div>
    </div>

    <script>
        const ctx = document.getElementById('latencyChart').getContext('2d');
        const latencyChart = new Chart(ctx, {{
            type: 'bar',
            data: {{
                labels: {json.dumps(dist_labels)},
                datasets: [{{
                    label: 'Number of Requests',
                    data: {json.dumps(dist_values)},
                    backgroundColor: [
                        '#667eea',
                        '#764ba2',
                        '#f093fb',
                        '#4facfe',
                        '#00f2fe',
                        '#43e97b',
                        '#fa709a',
                    ],
                    borderColor: '#fff',
                    borderWidth: 2,
                }}]
            }},
            options: {{
                responsive: true,
                maintainAspectRatio: false,
                scales: {{
                    y: {{
                        beginAtZero: true,
                    }}
                }},
                plugins: {{
                    legend: {{
                        display: true,
                        position: 'top',
                    }}
                }}
            }}
        }});
    </script>
</body>
</html>
"""

        with open(filename, 'w') as f:
            f.write(html_content)

        return filename

    def print_summary(self):
        """Print summary to console."""
        summary = self.get_summary()
        throughput = self.get_throughput()
        distribution = self.get_latency_distribution()

        print("\n" + "=" * 70)
        print("DAG Agent Benchmark Analysis Summary")
        print("=" * 70 + "\n")

        print("SUMMARY STATISTICS")
        print("-" * 70)
        print(f"Total Tasks:           {summary.get('total_tasks', 0)}")
        print(f"Successful:            {summary.get('successful_tasks', 0)}")
        print(f"Failed:                {summary.get('failed_tasks', 0)}")
        print(f"Success Rate:          {summary.get('success_rate_percent', 0):.2f}%\n")

        print("THROUGHPUT")
        print("-" * 70)
        print(f"Throughput:            {throughput.get('throughput_tasks_per_sec', 0)} tasks/sec")
        print(f"Test Duration:         {throughput.get('test_duration_ms', 0)} ms\n")

        if summary.get('latency_min_ms') is not None:
            print("LATENCY STATISTICS (ms)")
            print("-" * 70)
            print(f"Minimum:               {summary.get('latency_min_ms', 0):.2f} ms")
            print(f"Maximum:               {summary.get('latency_max_ms', 0):.2f} ms")
            print(f"Mean:                  {summary.get('latency_mean_ms', 0):.2f} ms")
            print(f"Median (P50):          {summary.get('latency_p50_ms', 0):.2f} ms")
            print(f"P95:                   {summary.get('latency_p95_ms', 0):.2f} ms")
            print(f"P99:                   {summary.get('latency_p99_ms', 0):.2f} ms")
            print(f"Std Dev:               {summary.get('latency_stddev_ms', 0):.2f} ms\n")

        if distribution:
            print("LATENCY DISTRIBUTION")
            print("-" * 70)
            total_latencies = sum(distribution.values())
            for bucket, count in distribution.items():
                percentage = (count / total_latencies * 100) if total_latencies > 0 else 0
                print(f"{bucket:12} : {count:5} ({percentage:5.1f}%)")
            print()

        print("=" * 70 + "\n")


def main():
    """Main entry point."""
    parser = argparse.ArgumentParser(
        description='DAG Agent Benchmark Analysis and Report Generator'
    )
    parser.add_argument('csv_file', help='Path to metrics CSV file')
    parser.add_argument('--output', '-o', help='Output directory for reports')

    args = parser.parse_args()

    try:
        analyzer = BenchmarkAnalyzer(args.csv_file, args.output)

        # Print summary to console
        analyzer.print_summary()

        # Generate reports
        json_file = analyzer.generate_json_report()
        text_file = analyzer.generate_text_report()
        html_file = analyzer.generate_html_report()

        print(f"\nReports generated:")
        print(f"  - JSON Report: {json_file}")
        print(f"  - Text Report: {text_file}")
        print(f"  - HTML Report: {html_file}")
        print(f"\nOpen {html_file} in a browser to view interactive charts.\n")

    except Exception as e:
        print(f"Error: {e}", file=sys.stderr)
        sys.exit(1)


if __name__ == '__main__':
    main()

