#!/usr/bin/env python3
"""
Simple mock API server for testing Gatling reports
"""
from http.server import HTTPServer, BaseHTTPRequestHandler
import json
import time

class MockAPIHandler(BaseHTTPRequestHandler):
    def do_GET(self):
        if self.path == '/api/users/health':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            response = {"status": "healthy", "timestamp": int(time.time())}
            self.wfile.write(json.dumps(response).encode())
        elif self.path == '/api/users':
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            users = [
                {"id": 1, "name": "John Doe", "email": "john@example.com"},
                {"id": 2, "name": "Jane Smith", "email": "jane@example.com"}
            ]
            self.wfile.write(json.dumps(users).encode())
        elif self.path.startswith('/api/users/'):
            user_id = self.path.split('/')[-1]
            self.send_response(200)
            self.send_header('Content-type', 'application/json')
            self.end_headers()
            user = {"id": int(user_id), "name": f"User {user_id}", "email": f"user{user_id}@example.com"}
            self.wfile.write(json.dumps(user).encode())
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        print(f"[{time.strftime('%Y-%m-%d %H:%M:%S')}] {format % args}")

if __name__ == '__main__':
    server = HTTPServer(('localhost', 8080), MockAPIHandler)
    print("Mock API server starting on http://localhost:8080")
    print("Available endpoints:")
    print("  GET /api/users/health")
    print("  GET /api/users")
    print("  GET /api/users/{id}")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        print("\nShutting down mock API server")
        server.shutdown()
