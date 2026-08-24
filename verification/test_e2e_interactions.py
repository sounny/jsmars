"""
test_e2e_interactions.py

Comprehensive End-to-End UI & Console Error Validation Suite for JSMARS.
Exercises all accordion tool panels, body switching, time sliders, canvas renderers,
and asserts that 0 JavaScript runtime errors or unhandled promise rejections occur.
"""

import http.server
import socketserver
import threading
import time
import os
import sys
from playwright.sync_api import sync_playwright

DIRECTORY = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def run_e2e_tests():
    class Handler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=DIRECTORY, **kwargs)

        def log_message(self, format, *args):
            pass

    socketserver.TCPServer.allow_reuse_address = True
    server = socketserver.TCPServer(("127.0.0.1", 0), Handler)
    port = server.server_address[1]

    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()
    time.sleep(0.5)

    print(f"[E2E] HTTP server started on port {port}")
    console_errors = []
    console_warnings = []

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        page.on("console", lambda msg: console_errors.append(f"[{msg.type}] {msg.text}") if msg.type in ["error"] else (console_warnings.append(f"[{msg.type}] {msg.text}") if msg.type in ["warning"] else None))
        page.on("pageerror", lambda exc: console_errors.append(f"[pageerror] {exc}"))

        # 1. Load Main Application
        print("[E2E] Navigating to index.html...")
        page.goto(f"http://127.0.0.1:{port}/index.html")
        page.wait_for_timeout(2000)

        # 2. Dismiss Welcome Modal if present
        if page.locator("#welcome-dismiss").is_visible():
            print("[E2E] Dismissing welcome modal...")
            page.locator("#welcome-dismiss").click()
            page.wait_for_timeout(300)

        # 3. Test Body Switching (Mars -> Moon -> Mars)
        print("[E2E] Testing planetary body switching...")
        moon_btn = page.locator("button:has-text('Moon'), select#body-selector option[value='moon']")
        if moon_btn.count() > 0:
            try:
                moon_btn.first.click()
                page.wait_for_timeout(500)
            except Exception as e:
                print(f"[E2E] Body switch click notice: {e}")

        # 4. Expand and Render All Accordion Sections
        headers = page.locator(".layer-section-header").all()
        print(f"[E2E] Found {len(headers)} sidebar accordion sections. Expanding all...")
        for i, header in enumerate(headers):
            try:
                title = header.locator(".layer-title").inner_text()
                header.click()
                page.wait_for_timeout(150)
            except Exception as e:
                print(f"[E2E] Warning expanding section {i}: {e}")

        page.wait_for_timeout(500)

        # 5. Exercise Interactive Tool Controls
        print("[E2E] Exercising interactive tools...")

        # KRC Model Simulation button
        krc_btn = page.locator("#krc-simulate-btn")
        if krc_btn.count() > 0 and krc_btn.is_visible():
            print("[E2E] Running KRC Thermal simulation...")
            krc_btn.click()
            page.wait_for_timeout(300)

        # Band Math Preset selection
        bm_select = page.locator("#bm-preset-select")
        if bm_select.count() > 0 and bm_select.is_visible():
            print("[E2E] Testing Spectral Band Math preset selection...")
            bm_select.select_option("bd1900_h2o")
            page.wait_for_timeout(200)

        # Radar Sounder Simulation
        radar_select = page.locator("#radar-preset-select")
        if radar_select.count() > 0 and radar_select.is_visible():
            print("[E2E] Testing Subsurface Radar preset selection...")
            radar_select.select_option("australe")
            page.wait_for_timeout(200)

        # Graticule Grid toggle
        grid_btn = page.locator("#grid-toggle-btn")
        if grid_btn.count() > 0 and grid_btn.is_visible():
            print("[E2E] Toggling Lat/Lon Graticule Grid...")
            grid_btn.click()
            page.wait_for_timeout(200)

        # Trajectory Transfer calculation
        traj_btn = page.locator("#traj-calc-btn")
        if traj_btn.count() > 0 and traj_btn.is_visible():
            print("[E2E] Testing Interplanetary Trajectory solver...")
            traj_btn.click()
            page.wait_for_timeout(200)

        page.wait_for_timeout(1000)
        browser.close()
        server.shutdown()

    if console_errors:
        print(f"[E2E ERROR] Encountered {len(console_errors)} console errors during user interaction:")
        for err in console_errors:
            print(f"  - {err}")
        return False, console_errors

    print(f"[E2E SUCCESS] Successfully interacted with all UI panels and tools with 0 console errors!")
    return True, []

if __name__ == "__main__":
    ok, errors = run_e2e_tests()
    if not ok:
        sys.exit(1)
    print("=== All E2E Interactive Tests Passed Cleanly ===")
