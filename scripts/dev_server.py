import http.server
import socketserver
import sys
import os

PORT = 8080

class DevHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def end_headers(self):
        self.send_header('Cache-Control', 'no-cache, no-store, must-revalidate')
        self.send_header('Pragma', 'no-cache')
        self.send_header('Expires', '0')
        self.send_header('Access-Control-Allow-Origin', '*')
        super().end_headers()

    def guess_type(self, path):
        if path.endswith('.webmanifest'):
            return 'application/manifest+json'
        if path.endswith('.json'):
            return 'application/json'
        if path.endswith('.js'):
            return 'text/javascript'
        return super().guess_type(path)

def run():
    port = int(sys.argv[1]) if len(sys.argv) > 1 else PORT
    handler = DevHTTPRequestHandler
    # Allow port reuse
    socketserver.TCPServer.allow_reuse_address = True
    try:
        with socketserver.TCPServer(("", port), handler) as httpd:
            print(f"JSMARS Dev Server listening at http://localhost:{port}/")
            httpd.serve_forever()
    except OSError as e:
        print(f"Failed to bind port {port}: {e}")
        sys.exit(1)

if __name__ == "__main__":
    run()
