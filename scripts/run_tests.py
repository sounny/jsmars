#!/usr/bin/env python3
"""
scripts/run_tests.py
====================
Repeatable Headless Automated Test Runner for JSMARS Mocha Unit Test Suite.

Runs the complete test suite in tests/index.html using Playwright over a
multi-threaded local HTTP server so parallel ES module imports never stall,
and fails fast on uncaught module/boot syntax or import errors.

Usage:
    python scripts/run_tests.py
"""

import http.server
import socket
import sys
import threading
from playwright.sync_api import sync_playwright


class QuietHTTPRequestHandler(http.server.SimpleHTTPRequestHandler):
    def log_message(self, format, *args):
        pass


class ReusableThreadingHTTPServer(http.server.ThreadingHTTPServer):
    allow_reuse_address = True
    daemon_threads = True


def find_free_port():
    with socket.socket(socket.AF_INET, socket.SOCK_STREAM) as s:
        s.bind(('', 0))
        return s.getsockname()[1]


def main():
    port = find_free_port()
    httpd = ReusableThreadingHTTPServer(('127.0.0.1', port), QuietHTTPRequestHandler)
    server_thread = threading.Thread(target=httpd.serve_forever, daemon=True)
    server_thread.start()

    print(f"[JSMARS Test Runner] Starting threaded test server on http://127.0.0.1:{port}")

    page_errors = []
    console_errors = []

    try:
        with sync_playwright() as p:
            browser = p.chromium.launch(headless=True)
            page = browser.new_page()

            def on_pageerror(err):
                msg = str(err)
                page_errors.append(msg)
                print(f"[PAGE ERROR] {msg}")

            def on_console(msg):
                if msg.type == "error":
                    console_errors.append(msg.text)
                    print(f"[CONSOLE ERROR] {msg.text}")

            page.on("pageerror", on_pageerror)
            page.on("console", on_console)
            page.add_init_script("""
                window.addEventListener('error', (e) => {
                    if (!window.__mochaRunner) {
                        window.__mochaBootError = e.message || String(e.error || 'Boot error');
                    }
                });
            """)

            test_url = f"http://127.0.0.1:{port}/tests/index.html"
            page.goto(test_url, timeout=90000)

            # Wait for Mocha suite to complete OR an early module/boot error
            page.wait_for_function(
                "() => window.__mochaDone === true || Boolean(window.__mochaBootError)",
                timeout=90000
            )

            boot_err = page.evaluate("() => window.__mochaBootError || null")
            if boot_err or (page_errors and not page.evaluate("() => Boolean(window.__mochaDone)")):
                print("==================================================")
                print("FATAL BOOT / MODULE ERROR BEFORE MOCHA COMPLETED:")
                if boot_err:
                    print(f"  Boot Error: {boot_err}")
                for pe in page_errors:
                    print(f"  Page Error: {pe}")
                for ce in console_errors:
                    print(f"  Console Error: {ce}")
                print("==================================================")
                browser.close()
                sys.exit(1)

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
            print("JSMARS Mocha Test Suite Results:")
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
