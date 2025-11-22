from playwright.sync_api import sync_playwright, expect
import time

def verify_crater_tool(page):
    print("Navigating to app...")
    page.goto("http://localhost:8000/index.html")

    # Check Button
    btn = page.locator("#crater-tool-btn")
    expect(btn).to_be_visible()
    expect(btn).to_have_text("Start Crater Counting")

    # Start Tool
    print("Activating tool...")
    btn.click()
    expect(btn).to_have_text("Stop Crater Counting")

    # Click on map to add crater
    print("Clicking map...")
    # Map center
    page.mouse.click(400, 300)
    time.sleep(1)

    # Check table
    print("Checking table...")
    table = page.locator("#crater-table-body")
    rows = table.locator("tr")
    expect(rows).to_have_count(1)

    # Check buttons
    print("Checking Export/Clear buttons...")
    expect(page.locator("#crater-export-btn")).to_be_visible()
    expect(page.locator("#crater-clear-btn")).to_be_visible()

    # Delete Crater
    print("Deleting crater...")
    delete_btn = rows.first.locator(".delete-crater-btn")
    delete_btn.click()
    time.sleep(0.5)

    expect(rows).to_have_count(0)

    # Deactivate Tool
    print("Deactivating tool...")
    btn.click()
    expect(btn).to_have_text("Start Crater Counting")

    page.screenshot(path="verification/crater_tool.png")
    print("Verification successful. Screenshot at verification/crater_tool.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        # Set viewport size to ensure map is clickable
        page.set_viewport_size({"width": 800, "height": 600})
        try:
            verify_crater_tool(page)
        except Exception as e:
            print(f"Verification failed: {e}")
            exit(1)
        finally:
            browser.close()
