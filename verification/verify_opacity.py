from playwright.sync_api import Page, expect, sync_playwright
import time

def test_opacity_slider(page: Page):
    # Navigate to localhost
    page.goto("http://localhost:8080/public/index.html")

    # Wait for initial layer (Mars Viking)
    # The checkbox should be checked.
    viking_layer = page.locator("label").filter(has_text="Mars Viking (OpenPlanetary)")
    expect(viking_layer).to_be_visible()

    # Check if slider exists
    # It should be in the same container.
    container = page.locator(".layer-item-container").filter(has_text="Mars Viking (OpenPlanetary)")
    slider = container.locator("input[type='range']")
    expect(slider).to_be_visible()
    expect(slider).to_be_enabled()

    # Change opacity
    print("Changing opacity to 0.5")
    # For range input, we might need to use evaluate or specific interaction
    slider.evaluate("el => { el.value = 0.5; el.dispatchEvent(new Event('input')); }")

    expect(slider).to_have_value("0.5")

    # Take screenshot
    time.sleep(1)
    page.screenshot(path="verification/opacity_test.png")
    print("Screenshot saved to verification/opacity_test.png")

    # Disable layer, check slider disabled
    print("Disabling layer...")
    viking_layer.locator("input[type='checkbox']").uncheck()
    expect(slider).to_be_disabled()
    print("Slider disabled correctly.")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            test_opacity_slider(page)
        except Exception as e:
            print(f"Error: {e}")
            raise e
        finally:
            browser.close()
