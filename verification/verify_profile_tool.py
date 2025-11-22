from playwright.sync_api import sync_playwright, expect
import time

def verify_profile_tool(page):
    print("Navigating to app...")
    page.goto("http://localhost:8000/index.html")

    # Check Button
    btn = page.locator("#profile-tool-btn")
    expect(btn).to_be_visible()
    expect(btn).to_have_text("Radial Profile")

    # Start Tool
    print("Activating tool...")
    btn.click()
    expect(btn).to_have_text("Stop Profile")

    # Perform Profile Interaction
    print("Drawing profile...")
    # Center
    page.mouse.click(500, 300)
    time.sleep(0.2)
    # Move
    page.mouse.move(600, 300)
    time.sleep(0.2)
    # Finish
    page.mouse.click(600, 300)
    time.sleep(0.5)

    # Check Map for Polylines (Radial lines)
    # The tool adds them to a layer group on the map.
    # Leaflet renders polylines as SVG paths in .leaflet-overlay-pane
    paths = page.locator(".leaflet-overlay-pane path")
    # We expect multiple lines. 8 lines + maybe the preview line or center marker?
    # RadialProfileTool draws 8 lines + center marker (circle).
    print(f"Found {paths.count()} paths/elements on map")
    if paths.count() < 8:
        raise Exception("Radial profile lines not found on map")

    # Check Chart
    print("Checking chart...")
    canvas = page.locator("#profile-chart-container canvas")
    expect(canvas).to_be_visible()

    # Deactivate Tool
    print("Deactivating tool...")
    btn.click()
    expect(btn).to_have_text("Radial Profile")

    page.screenshot(path="verification/profile_tool.png")
    print("Verification successful. Screenshot at verification/profile_tool.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        page.set_viewport_size({"width": 1000, "height": 800})
        try:
            verify_profile_tool(page)
        except Exception as e:
            print(f"Verification failed: {e}")
            exit(1)
        finally:
            browser.close()
