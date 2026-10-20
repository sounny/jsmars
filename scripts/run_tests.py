#!/usr/bin/env python3
"""
scripts/run_tests.py
====================
Repeatable Headless Automated Test Runner for JSMARS Mocha Unit Test Suite.

Runs the complete 1,180+ test suite in tests/index.html using Playwright,
collecting pass/fail assertions and printing formatted test diagnostics.

Usage:
    python scripts/run_tests.py
"""

import http.server
import socket
import socketserver
import sys
import threading
from playwright.sync_api import sync_playwright

def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('', 0))
        return s.getsockname()[1]

def main():
    port = find_free_port()
    handler = http.server.SimpleHTTPRequestHandler
    
    # Allow socket address reuse
    socketserver.TCPServer.allow_reuse_address = True
    httpd = socketserver.TCPServer(('', port), handler)
    server_thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    server_thread.start()

    print(f"[JSMARS Test Runner] Starting test server on http://127.0.0.1:{port}")
    
    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()
            
            test_url = f"http://127.0.0.1:{port}/tests/index.html"
            page.goto(test_url)
            
            # Wait for Mocha suite to complete execution
            page.wait_for_function("() => window.__mochaDone === true", timeout=30000)
            
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
            
            browser.close()
            
            print("==================================================")
            print(f"JSMARS Mocha Test Suite Results:")
            print(f"  Passed:   {stats['passes']}")
            print(f"  Failed:   {stats['failures']}")
            print(f"  Summary:  {stats['statsText'].replace(chr(10), ' | ')}")
            print("==================================================")
            
            if failures_detail:
                print("FAILING TESTS:")
                for fd in failures_detail:
                    clean_title = fd['title'].encode('ascii', errors='replace').decode('ascii')
                    clean_err = fd['error'].encode('ascii', errors='replace').decode('ascii')
                    print(f"  - {clean_title}")
                    print(f"    Error: {clean_err}")
                print("==================================================")
                sys.exit(1)
            else:
                print("SUCCESS: 100% of all unit tests passed with 0 failures!")
                sys.exit(0)
    finally:
        httpd.shutdown()

if __name__ == '__main__':
    main()
