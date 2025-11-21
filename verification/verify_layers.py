
from playwright.sync_api import Page, expect, sync_playwright
import os
import time

def test_layer_discovery(page: Page):
    # Navigate to the local file
    cwd = os.getcwd()
    file_path = os.path.join(cwd, 'public/index.html')
    page.goto(f"file://{file_path}")

    # Check if map container exists
    map_container = page.locator('#map')
    expect(map_container).to_be_visible()

    # Wait for layers to load (WMS discovery might take a second)
    # We look for a specific discovered layer to appear in the list
    # "MOLA Colorized Shaded Relief" is a title we saw in the XML
    mola_layer_text = page.get_by_text("MOLA Colorized Shaded Relief")

    # Increase timeout because fetch might be slow
    try:
        mola_layer_text.wait_for(timeout=10000)
        print("Discovered MOLA layer in the UI.")
    except:
        print("Timed out waiting for MOLA layer.")

    # Toggle MOLA layer
    # Find the checkbox associated with the text
    # (The text is in a label which contains the input)
    # We can click the label
    mola_layer_text.click()

    # Wait a bit for tiles to theoretically request
    time.sleep(2)

    # Check for coordinate control
    coord_control = page.locator('.coordinate-control')
    expect(coord_control).to_be_visible()

    # Move mouse to trigger coordinate update
    page.mouse.move(100, 100)
    page.mouse.move(200, 200)

    # Check text update
    expect(coord_control).not_to_have_text("Lat: 0, Lon: 0")

    # Take a screenshot
    page.screenshot(path="verification/layer_discovery.png")
    print("Screenshot saved to verification/layer_discovery.png")

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
