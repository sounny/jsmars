import http.server
import socketserver
import threading
import time
from playwright.sync_api import sync_playwright

PORT = 8902
Handler = http.server.SimpleHTTPRequestHandler
httpd = socketserver.TCPServer(('', PORT), Handler)
server_thread = threading.Thread(target=httpd.serve_forever, daemon=True)
server_thread.start()

try:
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        page.goto(f"http://localhost:{PORT}/tests/index.html")
        page.wait_for_selector("#mocha-stats", timeout=20000)
        time.sleep(3)
        stats = page.evaluate("""() => {
            const passes = document.querySelectorAll('.test.pass').length;
            const failures = document.querySelectorAll('.test.fail').length;
            const statsText = document.querySelector('#mocha-stats')?.innerText || '';
            return { passes, failures, statsText };
        }""")
        print(f"Mocha Test Run: {stats['passes']} passed, {stats['failures']} failed.")
        print(stats['statsText'])
        assert stats['failures'] == 0, f"Found {stats['failures']} failing tests!"
        browser.close()
        print("ALL UNIT TESTS PASSED!")
finally:
    httpd.shutdown()
