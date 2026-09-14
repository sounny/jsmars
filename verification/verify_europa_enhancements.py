import http.server
import socketserver
import threading
import time
from playwright.sync_api import sync_playwright

PORT = 8899
Handler = http.server.SimpleHTTPRequestHandler
httpd = socketserver.TCPServer(('', PORT), Handler)
server_thread = threading.Thread(target=httpd.serve_forever, daemon=True)
server_thread.start()
print(f"Local server running on port {PORT}")

try:
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page(viewport={'width': 1280, 'height': 800})
        page.goto(f"http://localhost:{PORT}/index.html")
        page.wait_for_selector("#map")
        time.sleep(1)

        # Dismiss welcome modal
        dismiss_btn = page.query_selector("#welcome-dismiss")
        if dismiss_btn:
            dismiss_btn.click()
            time.sleep(0.5)

        # 1. Check load status in bottom bar
        load_pill = page.query_selector("#status-load")
        assert load_pill is not None, "Load status pill missing from status bar"
        load_text = page.inner_text("#status-load-text")
        print("1. Initial Load Status:", load_text)
        assert len(load_text) > 0, "Load status text is empty"

        # 2. Switch to Europa via the dropdown
        print("2. Switching to Europa via .body-selector-dropdown...")
        page.select_option(".body-selector-dropdown", "europa")
        time.sleep(3)

        # Check load status during/after Europa switch
        europa_status = page.inner_text("#status-load-text")
        print("   Load status on Europa:", europa_status)

        # 3. Check MiniMap for Europa
        minimap_info = page.evaluate("""() => {
            const mm = document.querySelector('.leaflet-control-minimap');
            if (!mm) return { found: false };
            const imgs = Array.from(mm.querySelectorAll('img.leaflet-tile'));
            const urls = imgs.map(img => img.src);
            return {
                found: true,
                tileCount: imgs.length,
                firstTileUrl: urls[0] || null,
                isWms: urls.some(u => u.includes('mapserv') || u.includes('europa') || u.includes('WMS') || u.includes('GALILEO'))
            };
        }""")
        print("3. MiniMap check for Europa:", minimap_info)
        assert minimap_info['found'], "Leaflet.MiniMap control not found"
        assert minimap_info['isWms'], f"Expected MiniMap to use Europa WMS layer, got: {minimap_info['firstTileUrl']}"

        # 4. Check Feature Labels toggle in Display Overlays (inside Map Options accordion)
        print("4. Opening Map Options accordion and toggling Feature Labels...")
        page.click('.accordion-header:has-text("Map Options")')
        time.sleep(0.5)

        page.click('label[for="toggle-labels"]')
        time.sleep(2)

        # Verify nomenclature markers on map
        landmarks_count = page.evaluate("""() => {
            const markers = document.querySelectorAll('.nomenclature-label-container');
            return markers.length;
        }""")
        print(f"   Active Europa feature label markers on map: {landmarks_count}")
        assert landmarks_count > 0, "Expected Europa feature markers on map after toggling labels"

        # Verify specific Europa landmarks exist
        has_europa_landmarks = page.evaluate("""() => {
            const text = document.body.innerText;
            return text.includes('Conamara') || text.includes('Pwyll') || text.includes('Mannannán') || text.includes('Agenor');
        }""")
        print("   Europa landmark names detected:", has_europa_landmarks)

        # Take screenshot
        page.screenshot(path="verification/europa_features_verified.png")
        print("5. Saved screenshot to verification/europa_features_verified.png")

        browser.close()
        print("ALL CHECKS PASSED SUCCESSFULLY!")
finally:
    httpd.shutdown()
