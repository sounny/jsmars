from playwright.sync_api import sync_playwright
import time

def run():
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        # Navigate to the local server
        page.goto("http://localhost:8080/public/index.html")

        # Wait for map to load
        time.sleep(5)

        # Click the new checkbox to enable the WMS layer
        # Using user-facing locator: label text
        wms_checkbox = page.get_by_label("Mars Viking MDIM2.1 (USGS WMS)")
        wms_checkbox.check()

        # Wait for tiles to load (give it a bit of time)
        time.sleep(5)

        # Take a screenshot
        page.screenshot(path="verification/wms_layer.png")
        browser.close()

if __name__ == "__main__":
    run()
