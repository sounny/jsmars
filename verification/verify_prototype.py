
from playwright.sync_api import Page, expect, sync_playwright
import os

def test_prototype_loads(page: Page):
    # Navigate to the local file
    cwd = os.getcwd()
    file_path = os.path.join(cwd, 'public/index.html')
    page.goto(f"file://{file_path}")

    # Check if map container exists
    map_container = page.locator('#map')
    expect(map_container).to_be_visible()

    # Check if controls exist
    controls = page.locator('#controls')
    expect(controls).to_be_visible()

    # Check if layer checkboxes exist
    layer_viking = page.locator('#toggle-mars_viking')
    expect(layer_viking).to_be_visible()
    expect(layer_viking).to_be_checked()

    layer_wms = page.locator('#toggle-mars_wms_viking')
    expect(layer_wms).to_be_visible()
    expect(layer_wms).not_to_be_checked()

    # Take a screenshot
    page.screenshot(path="verification/prototype_initial.png")
    print("Screenshot saved to verification/prototype_initial.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            test_prototype_loads(page)
        except Exception as e:
            print(f"Error: {e}")
        finally:
            browser.close()
