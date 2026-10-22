import sys
import os
import shutil
import time
from playwright.sync_api import sync_playwright

sys.stdout.reconfigure(encoding='utf-8')

SCREENSHOT_DIR = os.path.abspath("verification/screenshots")
ARTIFACT_DIR = r"C:\Users\sounn\.gemini\antigravity\brain\e6b9ee80-8e62-4207-8bfc-a5258e4222fc"
os.makedirs(SCREENSHOT_DIR, exist_ok=True)
os.makedirs(ARTIFACT_DIR, exist_ok=True)

def run_tests():
    console_logs = []
    console_errors = []
    page_errors = []

    def on_console(msg):
        text = f"[{msg.type.upper()}] {msg.text}"
        console_logs.append(text)
        if msg.type == "error":
            console_errors.append(msg.text)
            print(f"[CONSOLE ERROR] {msg.text}")

    def on_page_error(err):
        page_errors.append(str(err))
        print(f"[PAGE ERROR] {err}")

    print("Launching Chromium...")
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-setuid-sandbox"])
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()

        page.on("console", on_console)
        page.on("pageerror", on_page_error)

        # 1. Map Overview (Mars)
        print("\nStep 1: Navigating to http://localhost:8080/ ...")
        page.goto("http://localhost:8080/", wait_until="networkidle", timeout=30000)
        page.wait_for_selector("#map", timeout=15000)

        # Check and dismiss welcome modal
        if page.is_visible("#welcome-modal"):
            print("Dismissing welcome modal...")
            page.click("#welcome-dismiss")
            page.wait_for_timeout(800)

        snap1 = os.path.join(SCREENSHOT_DIR, "01_map_overview.png")
        page.screenshot(path=snap1)
        print(f"Captured: {snap1}")

        # 2. Planetary Body Switch to Moon + Landing Sites (Artemis III candidates)
        print("\nStep 2: Switching body to Moon and displaying Landing Sites...")
        page.select_option("#body-selector-container select.body-selector-dropdown", "moon")
        page.wait_for_timeout(2000)

        # Open accordion 'Tools' if closed
        tools_acc = page.locator(".accordion-header", has_text="Tools")
        if tools_acc.count() > 0:
            tools_content = tools_acc.locator("xpath=following-sibling::div[1]")
            if not tools_content.is_visible():
                tools_acc.click()
                page.wait_for_timeout(400)

        # Toggle Landing Sites on Moon
        ls_header = page.locator(".layer-section-header", has_text="Landing Sites")
        if ls_header.count() > 0:
            ls_header.first.scroll_into_view_if_needed()
            ls_header.first.click()
            page.wait_for_timeout(300)
            ls_btn = page.locator("#landing-sites-btn")
            if ls_btn.is_visible():
                ls_btn.click()
                page.wait_for_timeout(800)

        # Verify that Moon landing sites (Apollo / Artemis) are rendered and Mars sites (Viking 1) are NOT rendered
        labels = page.locator(".landing-marker-label").all_inner_texts()
        print(f"Landing site markers on Moon ({len(labels)}): {labels[:5]}...")
        if any("Viking 1" in lbl for lbl in labels):
            raise AssertionError(f"Mars landing site 'Viking 1' erroneously rendered on Moon! Labels: {labels}")
        if not any("Apollo" in lbl or "Artemis" in lbl or "Chang'e" in lbl for lbl in labels):
            raise AssertionError(f"Expected Lunar landing sites on Moon, got: {labels}")

        snap2 = os.path.join(SCREENSHOT_DIR, "02_moon_switch.png")
        page.screenshot(path=snap2)
        print(f"Captured: {snap2}")

        # 3. Lunar Base Site Planner (Real LOLA 20m & 60m South Pole Data)
        print("\nStep 3: Opening Lunar Base Site Planner...")
        lsp_header = page.locator(".layer-section-header", has_text="Lunar Base Site Planner")
        if lsp_header.count() > 0:
            lsp_header.first.scroll_into_view_if_needed()
            lsp_header.first.click()
            page.wait_for_timeout(500)

        open_site_btn = page.locator("#lunar-site-panel-container button", has_text="Open Site Planner")
        if open_site_btn.count() > 0 and open_site_btn.is_visible():
            open_site_btn.click()
            page.wait_for_selector(".lsp-workspace", state="visible", timeout=10000)
            print("LSP Workspace opened. Waiting for LOLA manifest and regional data to load...")

            # Wait for site select dropdown to be populated (happens after _loadRegional completes)
            page.wait_for_function(
                'document.querySelector(".lsp-workspace select.lsp-select") && document.querySelector(".lsp-workspace select.lsp-select").options.length > 1',
                timeout=20000
            )
            print("Site selector options populated after regional LOLA load.")

            # Select Site01 (Connecting ridge)
            site_sel = page.locator(".lsp-workspace select.lsp-select")
            site_sel.select_option("Site01")
            print("Selected Site01 (Connecting ridge). Waiting for 20m high-resolution terrain...")
            page.wait_for_function(
                'document.querySelector(".lsp-status") && document.querySelector(".lsp-status").textContent.includes("Connecting ridge:")',
                timeout=20000
            )
            page.wait_for_timeout(1000)

            # Click the center of the polar map to populate the Inspect point readout with real LOLA metrics
            map_box = page.locator(".lsp-map").bounding_box()
            if map_box:
                page.mouse.click(map_box["x"] + map_box["width"] / 2, map_box["y"] + map_box["height"] / 2)
                page.wait_for_timeout(500)

            snap3 = os.path.join(SCREENSHOT_DIR, "03_lunar_site_planner.png")
            page.screenshot(path=snap3)
            print(f"Captured: {snap3}")

            # Close workspace
            close_btn = page.locator(".lsp-close")
            if close_btn.count() > 0:
                close_btn.click()
                page.wait_for_timeout(500)
        # Collapse Lunar Base Site Planner section
        lsp_header.first.click()

        # 4. 3D Terrain & Globe Viewer Provenance
        print("\nStep 4: Opening 3D Terrain & Globe...")
        threed_header = page.locator(".layer-section-header", has_text="3D Terrain & Globe")
        if threed_header.count() > 0:
            threed_header.first.scroll_into_view_if_needed()
            threed_header.first.click()
            page.wait_for_timeout(800)
            page.wait_for_selector("#threed-viewer-container", timeout=8000)
            page.locator("#threed-viewer-container").scroll_into_view_if_needed()
            page.wait_for_timeout(2000)

            # Verify badge presence
            badge = page.locator(".threed-provenance-badge")
            has_badge = badge.count() > 0 and badge.first.is_visible()
            print(f"ThreeD provenance badge visible: {has_badge}")
            if not has_badge:
                raise AssertionError("ThreeD provenance badge is not visible!")

            snap4 = os.path.join(SCREENSHOT_DIR, "04_3d_globe_provenance.png")
            page.screenshot(path=snap4)
            print(f"Captured: {snap4}")

            # Collapse 3D header
            threed_header.first.click()
            page.wait_for_timeout(400)

        # 5. Physical Models & Simulations Provenance
        print("\nStep 5: Testing Physical Models & Simulations...")
        radar_header = page.locator(".layer-section-header", has_text="Subsurface Radar Sounder")
        if radar_header.count() > 0:
            radar_header.first.scroll_into_view_if_needed()
            radar_header.first.click()
            page.wait_for_timeout(500)

        band_header = page.locator(".layer-section-header", has_text="Mineral Index Visualizer")
        if band_header.count() > 0:
            band_header.first.scroll_into_view_if_needed()
            band_header.first.click()
            page.wait_for_timeout(500)

        page.locator("#radar-panel-container").scroll_into_view_if_needed()
        page.wait_for_timeout(400)

        snap5 = os.path.join(SCREENSHOT_DIR, "05_physical_models_provenance.png")
        page.screenshot(path=snap5)
        print(f"Captured: {snap5}")

        radar_header.first.click()
        band_header.first.click()
        page.wait_for_timeout(300)

        # 6. Cartography & Geology Tools
        print("\nStep 6: Testing Cartography & Geology Tools...")
        crater_header = page.locator(".layer-section-header", has_text="Crater Counting")
        if crater_header.count() > 0:
            crater_header.first.scroll_into_view_if_needed()
            crater_header.first.click()
            page.wait_for_timeout(400)

        hill_header = page.locator(".layer-section-header", has_text="Hillshade")
        if hill_header.count() > 0:
            hill_header.first.scroll_into_view_if_needed()
            hill_header.first.click()
            page.wait_for_timeout(400)

        snap6 = os.path.join(SCREENSHOT_DIR, "06_cartography_tools.png")
        page.screenshot(path=snap6)
        print(f"Captured: {snap6}")

        browser.close()

    # Copy screenshots to artifact directory
    print("\nCopying screenshots to artifact directory...")
    for f in os.listdir(SCREENSHOT_DIR):
        if f.endswith(".png"):
            src = os.path.join(SCREENSHOT_DIR, f)
            dst = os.path.join(ARTIFACT_DIR, f)
            shutil.copy2(src, dst)
            print(f"Copied {f} -> {dst}")

    # Summary of errors
    print("\n================== VERIFICATION SUMMARY ==================")
    print(f"Total Page Errors (Unhandled Exceptions): {len(page_errors)}")
    print(f"Total Console Errors: {len(console_errors)}")
    if page_errors:
        print("Page Errors Details:")
        for pe in page_errors:
            print(f"  - {pe}")
    if console_errors:
        print("Console Errors Details:")
        for ce in console_errors:
            print(f"  - {ce}")
    print("==========================================================")

    if len(page_errors) > 0 or len(console_errors) > 0:
        print("TEST FAILED: Runtime or console errors occurred!")
        sys.exit(1)
    else:
        print("TEST PASSED: 0 unhandled runtime errors and 0 console errors.")

if __name__ == "__main__":
    run_tests()
