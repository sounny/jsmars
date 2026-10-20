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
        failures_detail = page.evaluate("""() => {
            const fails = Array.from(document.querySelectorAll('.test.fail'));
            return fails.map(f => ({
                title: f.querySelector('h2')?.innerText || '',
                error: f.querySelector('.error')?.innerText || ''
            }));
        }""")
        print(f"Mocha Test Run: {stats['passes']} passed, {stats['failures']} failed.")
        print(stats['statsText'])
        if failures_detail:
            print("FAILURES:")
            for fd in failures_detail:
                clean_title = fd['title'].encode('ascii', errors='replace').decode('ascii')
                clean_err = fd['error'].encode('ascii', errors='replace').decode('ascii')
                print(f"  - {clean_title}: {clean_err}")
        assert stats['failures'] == 0, f"Found {stats['failures']} failing tests!"
        browser.close()
        print("ALL UNIT TESTS PASSED!")
finally:
    httpd.shutdown()
