from playwright.sync_api import sync_playwright, expect
import time

def verify_shapes(page):
    page.on("console", lambda msg: print(f"BROWSER LOG: {msg.text}"))

    print("Navigating to app...")
    page.goto("http://localhost:8000/index.html")

    # 1. Select CircleMarker Tool
    print("Selecting CircleMarker Tool...")
    btn = page.locator(".leaflet-draw-draw-circlemarker")
    expect(btn).to_be_visible()
    btn.click()
    time.sleep(1)

    # 2. Draw CircleMarker
    print("Placing CircleMarker...")
    page.mouse.click(400, 400)
    time.sleep(1)

    # 3. Right Click to Open Style Editor
    print("Opening Style Editor (Right Click)...")
    page.mouse.click(400, 400, button="right")
    time.sleep(1)

    editor = page.locator("#style-editor")

    if not editor.is_visible():
        print("DEBUG: Editor hidden. checking layers...")
        count = page.evaluate("() => window.jmars.vectors.featureGroup.getLayers().length")
        print(f"DEBUG: FeatureGroup has {count} layers")

    expect(editor).to_be_visible()

    # 4. Change Style
    print("Changing Style...")
    color_input = editor.locator("#style-fill-color")
    color_input.fill("#00ff00")
    color_input.evaluate("el => el.dispatchEvent(new Event('input'))")

    page.screenshot(path="verification/shapes_tool.png")
    print("Verification successful.")

if __name__ == "__main__":
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()
        page.set_viewport_size({"width": 1000, "height": 800})
        try:
            verify_shapes(page)
        except Exception as e:
            print(f"Verification failed: {e}")
            exit(1)
        finally:
            browser.close()
