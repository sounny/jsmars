from playwright.sync_api import sync_playwright, expect
import time

def verify_features(page):
    print("Navigating to app...")
    page.goto("http://localhost:8000/index.html")

    # 1. Check Sidebar & Fixed Overlays
    print("Checking sidebar...")
    expect(page.locator("#controls")).to_be_visible()
    expect(page.get_by_text("Fixed Overlays")).to_be_visible()

    # 2. Check Graticule Toggle
    print("Checking Graticule...")
    grid_toggle = page.locator("#toggle-graticule")
    expect(grid_toggle).to_be_visible()

    # Toggle Grid
    grid_toggle.click()
    time.sleep(1)
    # Check if SVG paths (polylines) are added to map
    paths = page.locator(".leaflet-overlay-pane path")
    count = paths.count()
    print(f"Found {count} paths after toggling grid")
    if count == 0:
        raise Exception("Graticule paths not found")

    # 3. Check Panner Toggle
    print("Checking Panner...")
    panner_toggle = page.locator("#toggle-panner")
    expect(panner_toggle).to_be_visible()

    # Toggle Panner
    panner_toggle.click()
    time.sleep(1)

    panner_div = page.locator("#jmars-panner")
    expect(panner_div).to_be_visible()

    # 4. Check Layer Manager
    print("Checking Layer Manager...")
    expect(page.get_by_text("Active Layers")).to_be_visible()
    # Wait for layers to load/sync
    page.wait_for_selector("text=Mars Viking (OpenPlanetary)")

    page.screenshot(path="verification/new_features.png")
    print("Verification successful. Screenshot at verification/new_features.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            verify_features(page)
        except Exception as e:
            print(f"Verification failed: {e}")
            exit(1)
        finally:
            browser.close()
