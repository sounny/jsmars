from playwright.sync_api import sync_playwright, expect
import os

def verify_jmars(page):
    # Navigate to the app
    page.goto("http://localhost:3000/public/index.html")

    # Wait for map to be visible
    expect(page.locator("#map")).to_be_visible()

    # Check title
    expect(page).to_have_title("JSMARS - JMARS for the Web")

    # Check for controls
    expect(page.locator("#controls")).to_be_visible()
    expect(page.get_by_text("JSMARS")).to_be_visible()
    expect(page.get_by_text("Mars Viking (OpenPlanetary)")).to_be_visible()

    # Wait a bit for tiles to load
    page.wait_for_timeout(5000)

    # Take screenshot
    page.screenshot(path="verification/map_screenshot.png")
    print("Screenshot taken at verification/map_screenshot.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            verify_jmars(page)
        except Exception as e:
            print(f"Verification failed: {e}")
            exit(1)
        finally:
            browser.close()
