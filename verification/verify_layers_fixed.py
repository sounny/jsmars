
from playwright.sync_api import Page, expect, sync_playwright
import time

def test_layer_discovery(page: Page):
    # Navigate to localhost
    page.goto("http://localhost:8080/public/index.html")

    # Check if map container exists
    map_container = page.locator('#map')
    expect(map_container).to_be_visible()

    # Wait for layers to load
    # The UI renders the Title, so we look for that.
    mola_layer_text = page.get_by_text("MOLA Colorized Shaded Relief")

    print("Waiting for MOLA layer...")
    try:
        mola_layer_text.wait_for(timeout=15000)
        print("Discovered MOLA layer in the UI.")
    except Exception as e:
        print(f"Timed out waiting for MOLA layer. Current UI text: {page.locator('#layer-list').inner_text()}")
        raise e

    # Toggle MOLA layer
    # The text is in a span, the input is a sibling. We can click the label parent.
    # Or specific locator:
    page.locator("label").filter(has_text="MOLA Colorized Shaded Relief").locator("input").click()

    print("Clicked MOLA layer.")

    # Wait a bit for tiles
    time.sleep(3)

    # Check for coordinate control
    expect(page.get_by_text("Lat:")).to_be_visible()

    # Take a screenshot
    page.screenshot(path="verification/layer_discovery_fixed.png")
    print("Screenshot saved to verification/layer_discovery_fixed.png")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        try:
            test_layer_discovery(page)
        except Exception as e:
            print(f"Error: {e}")
        finally:
            browser.close()
