import http.server
import socket
import sys
import os
import shutil
import threading
from playwright.sync_api import sync_playwright

sys.stdout.reconfigure(encoding='utf-8')

SCREENSHOT_DIR = os.path.abspath("verification/screenshots")
ARTIFACT_DIR = os.environ.get("ARTIFACT_DIR", "").strip()
os.makedirs(SCREENSHOT_DIR, exist_ok=True)
if ARTIFACT_DIR:
    os.makedirs(ARTIFACT_DIR, exist_ok=True)


def boxes_overlap(a, b, pad=0):
    if not a or not b:
        return False
    return not (
        a["x"] + a["width"] + pad <= b["x"]
        or b["x"] + b["width"] + pad <= a["x"]
        or a["y"] + a["height"] + pad <= b["y"]
        or b["y"] + b["height"] + pad <= a["y"]
    )


def run_tests():
    console_logs = []
    console_errors = []
    page_errors = []

    def on_console(msg):
        text = f"[{msg.type.upper()}] {msg.text}"
        console_logs.append(text)
        if msg.type == "error":
            console_errors.append(msg.text)
            print(f"[CONSOLE ERROR] {msg.text}")

    def on_page_error(err):
        page_errors.append(str(err))
        print(f"[PAGE ERROR] {err}")

    local_httpd = None
    try:
        with socket.create_connection(("127.0.0.1", 8080), timeout=0.5):
            print("Detected running server on http://localhost:8080/")
    except OSError:
        class QuietHandler(http.server.SimpleHTTPRequestHandler):
            def log_message(self, fmt, *args):
                pass
        class ThreadedServer(http.server.ThreadingHTTPServer):
            allow_reuse_address = True
            daemon_threads = True
        local_httpd = ThreadedServer(("127.0.0.1", 8080), QuietHandler)
        threading.Thread(target=local_httpd.serve_forever, daemon=True).start()
        print("Started embedded ThreadingHTTPServer on http://localhost:8080/")

    print("Launching Chromium...")
    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True, args=["--no-sandbox", "--disable-setuid-sandbox"])
        context = browser.new_context(viewport={"width": 1440, "height": 900})
        page = context.new_page()

        page.on("console", on_console)
        page.on("pageerror", on_page_error)

        # 1. Map Overview (Mars) + Desktop HUD non-overlap check
        print("\nStep 1: Navigating to http://localhost:8080/ ...")
        page.goto("http://localhost:8080/", wait_until="networkidle", timeout=30000)
        page.wait_for_selector("#map", timeout=15000)

        if page.is_visible("#welcome-modal"):
            print("Dismissing welcome modal...")
            page.click("#welcome-dismiss")
            page.wait_for_timeout(600)

        hud_box = page.locator("#map-top-hud").bounding_box()
        controls_box = page.locator("#controls").bounding_box()
        status_box = page.locator("#status-bar").bounding_box()
        assert hud_box is not None, "#map-top-hud must be visible on desktop"
        assert not boxes_overlap(hud_box, controls_box), f"Desktop #map-top-hud overlaps #controls: {hud_box} vs {controls_box}"
        assert not boxes_overlap(hud_box, status_box), f"Desktop #map-top-hud overlaps #status-bar: {hud_box} vs {status_box}"

        snap1 = os.path.join(SCREENSHOT_DIR, "01_map_overview.png")
        page.screenshot(path=snap1)
        print(f"Captured: {snap1}")

        # 2. Planetary Body Switch to Moon + Landing Sites (Artemis III candidates)
        print("\nStep 2: Switching body to Moon and displaying Landing Sites...")
        page.select_option("#body-selector-container select.body-selector-dropdown", "moon")
        page.wait_for_timeout(2000)

        tools_acc = page.locator(".accordion-header", has_text="Tools")
        if tools_acc.count() > 0:
            tools_content = tools_acc.locator("xpath=following-sibling::div[1]")
            if not tools_content.is_visible():
                tools_acc.click()
                page.wait_for_timeout(400)

        ls_header = page.locator(".layer-section-header", has_text="Landing Sites")
        if ls_header.count() > 0:
            ls_header.first.scroll_into_view_if_needed()
            ls_header.first.click()
            page.wait_for_timeout(300)
            ls_btn = page.locator("#landing-sites-btn")
            if ls_btn.is_visible():
                ls_btn.click()
                page.wait_for_timeout(800)

        labels = page.locator(".landing-marker-label").all_inner_texts()
        print(f"Landing site markers on Moon ({len(labels)}): {labels[:5]}...")
        if any("Viking 1" in lbl for lbl in labels):
            raise AssertionError(f"Mars landing site 'Viking 1' erroneously rendered on Moon! Labels: {labels}")
        if not any("Apollo" in lbl or "Artemis" in lbl or "Chang'e" in lbl for lbl in labels):
            raise AssertionError(f"Expected Lunar landing sites on Moon, got: {labels}")

        snap2 = os.path.join(SCREENSHOT_DIR, "02_moon_switch.png")
        page.screenshot(path=snap2)
        print(f"Captured: {snap2}")

        # 3. Lunar Base Site Planner (Real LOLA 20m & 60m South Pole Data)
        print("\nStep 3: Opening Lunar Base Site Planner...")
        lsp_header = page.locator(".layer-section-header", has_text="Lunar Base Site Planner")
        if lsp_header.count() > 0:
            lsp_header.first.scroll_into_view_if_needed()
            lsp_header.first.click()
            page.wait_for_timeout(500)

        open_site_btn = page.locator("#lunar-site-panel-container button", has_text="Open Site Planner")
        if open_site_btn.count() > 0 and open_site_btn.is_visible():
            open_site_btn.click()
            page.wait_for_selector(".lsp-workspace", state="visible", timeout=10000)
            print("LSP Workspace opened. Waiting for LOLA manifest and regional data to load...")

            page.wait_for_function(
                'document.querySelector(".lsp-workspace select.lsp-select") && document.querySelector(".lsp-workspace select.lsp-select").options.length > 1',
                timeout=20000
            )
            print("Site selector options populated after regional LOLA load.")

            site_sel = page.locator(".lsp-workspace select.lsp-select")
            site_sel.select_option("Site01")
            print("Selected Site01 (Connecting ridge). Waiting for 20m high-resolution terrain...")
            page.wait_for_function(
                'document.querySelector(".lsp-status") && document.querySelector(".lsp-status").textContent.includes("Connecting ridge:")',
                timeout=20000
            )
            page.wait_for_timeout(1000)

            map_box = page.locator(".lsp-map").bounding_box()
            if map_box:
                page.mouse.click(map_box["x"] + map_box["width"] / 2, map_box["y"] + map_box["height"] / 2)
                page.wait_for_timeout(500)

            snap3 = os.path.join(SCREENSHOT_DIR, "03_lunar_site_planner.png")
            page.screenshot(path=snap3)
            print(f"Captured: {snap3}")

            close_btn = page.locator(".lsp-close")
            if close_btn.count() > 0:
                close_btn.click()
                page.wait_for_timeout(500)
        lsp_header.first.click()

        # 4. 2D | 3D Cesium Globe Toggle & Provenance
        print("\nStep 4: Testing 2D | 3D Cesium Globe Toggle...")
        toggle_3d = page.locator("#view-mode-3d")
        toggle_2d = page.locator("#view-mode-2d")
        if toggle_3d.count() == 0 or not toggle_3d.is_visible():
            raise AssertionError("#view-mode-3d toggle button is not visible!")
        toggle_3d.click()
        page.wait_for_selector("#cesium-container .cesium-viewer", timeout=15000)
        page.wait_for_timeout(4500)

        snap4 = os.path.join(SCREENSHOT_DIR, "04_3d_globe_provenance.png")
        page.screenshot(path=snap4)
        print(f"Captured: {snap4}")

        toggle_2d.click()
        page.wait_for_timeout(600)

        # 5. Physical Models & Simulations Provenance
        print("\nStep 5: Testing Physical Models & Simulations...")
        radar_header = page.locator(".layer-section-header", has_text="Subsurface Radar Sounder")
        if radar_header.count() > 0:
            radar_header.first.scroll_into_view_if_needed()
            radar_header.first.click()
            page.wait_for_timeout(500)

        band_header = page.locator(".layer-section-header", has_text="Mineral Index Visualizer")
        if band_header.count() > 0:
            band_header.first.scroll_into_view_if_needed()
            band_header.first.click()
            page.wait_for_timeout(500)

        page.locator("#radar-panel-container").scroll_into_view_if_needed()
        page.wait_for_timeout(400)

        snap5 = os.path.join(SCREENSHOT_DIR, "05_physical_models_provenance.png")
        page.screenshot(path=snap5)
        print(f"Captured: {snap5}")

        radar_header.first.click()
        band_header.first.click()
        page.wait_for_timeout(300)

        # 6. Cartography & Geology Tools
        print("\nStep 6: Testing Cartography & Geology Tools...")
        crater_header = page.locator(".layer-section-header", has_text="Crater Counting")
        if crater_header.count() > 0:
            crater_header.first.scroll_into_view_if_needed()
            crater_header.first.click()
            page.wait_for_timeout(400)

        hill_header = page.locator(".layer-section-header", has_text="Hillshade")
        if hill_header.count() > 0:
            hill_header.first.scroll_into_view_if_needed()
            hill_header.first.click()
            page.wait_for_timeout(400)

        snap6 = os.path.join(SCREENSHOT_DIR, "06_cartography_tools.png")
        page.screenshot(path=snap6)
        print(f"Captured: {snap6}")
        crater_header.first.click()
        hill_header.first.click()
        page.wait_for_timeout(250)

        # 7. JMARS v1.6 Parity: Quick Command Palette (Ctrl+K), Shapes Shapefile/WKT & Measurement Azimuth/Perimeter
        print("\nStep 7: Verifying JMARS v1.6 Command Palette (Ctrl+K), Shapes & Measurement...")
        page.select_option("#body-selector-container select.body-selector-dropdown", "mars")
        page.wait_for_timeout(1200)

        page.keyboard.press("Control+k")
        page.wait_for_selector("#quick-command-backdrop:not([hidden])", state="visible", timeout=5000)
        page.fill("#quick-cmd-input", "Olympus")
        page.wait_for_timeout(300)
        cmd_results_text = page.locator("#quick-cmd-results").inner_text()
        assert "Olympus Mons" in cmd_results_text, f"Expected 'Olympus Mons' in Ctrl+K results, got: {cmd_results_text}"

        snap7 = os.path.join(SCREENSHOT_DIR, "07_command_palette.png")
        page.screenshot(path=snap7)
        print(f"Captured: {snap7}")

        page.keyboard.press("Escape")
        page.wait_for_timeout(250)
        assert page.locator("#quick-command-backdrop").is_hidden(), "Command palette must close on Escape"

        shapes_header = page.locator('.layer-section[data-requires-2d="shapes"] .layer-section-header')
        shapes_header.scroll_into_view_if_needed()
        shapes_header.click()
        page.wait_for_timeout(300)

        export_opts = page.locator("#shape-export-format option").evaluate_all("els => els.map(e => e.value)")
        assert "wkt" in export_opts and "shp" in export_opts, f"Expected wkt and shp in #shape-export-format, got: {export_opts}"
        shape_toolbar_text = page.locator("#shape-table-container").inner_text()
        assert "+ Column" in shape_toolbar_text and "Buffer" in shape_toolbar_text, f"Missing + Column / Buffer in ShapeTable: {shape_toolbar_text}"

        meas_header = page.locator('.layer-section[data-requires-2d="measurement"] .layer-section-header')
        meas_header.scroll_into_view_if_needed()
        meas_header.click()
        page.wait_for_timeout(300)
        meas_text = page.locator("#measure-table-container").inner_text()
        assert "Az / Perim" in meas_text, f"Expected 'Az / Perim' column in MeasurementTable, got: {meas_text}"

        snap8 = os.path.join(SCREENSHOT_DIR, "08_shapes_and_measurement_parity.png")
        page.screenshot(path=snap8)
        print(f"Captured: {snap8}")

        # 9. v0.13.0: 26-Body JMARS System Catalog & Astronomical 3D Solar Terminator
        print("\nStep 9: Verifying 26-Body JMARS Catalog, Pluto/Titan/Phobos & 3D Ephemeris Terminator...")
        optgroups = page.locator("#body-selector-container select.body-selector-dropdown optgroup").count()
        options_vals = page.locator("#body-selector-container select.body-selector-dropdown option").evaluate_all("els => els.map(e => e.value)")
        assert optgroups == 7, f"Expected 7 planetary system <optgroup> sections, got {optgroups}"
        assert len(options_vals) == 26, f"Expected 26 verified JMARS bodies, got {len(options_vals)}: {options_vals}"
        assert "uranus" not in options_vals and "neptune" not in options_vals, "Simulated gas-giant mosaics must not be present"

        # 2D Pluto New Horizons basemap (centered on Tombaugh Regio / Sputnik Planitia)
        page.select_option("#body-selector-container select.body-selector-dropdown", "pluto")
        page.wait_for_function(
            "document.querySelectorAll('#map .leaflet-tile-loaded').length >= 8",
            timeout=20000
        )
        page.wait_for_timeout(600)
        scale_txt = page.locator("#status-scale-label").inner_text()
        elev_txt = page.locator("#status-elev-val").inner_text()
        assert "(Pluto)" in scale_txt, f"Expected '(Pluto)' in status scale bar, got: {scale_txt}"
        assert elev_txt == "N/A", f"Expected 'N/A' elevation on Pluto, got: {elev_txt}"
        snap10 = os.path.join(SCREENSHOT_DIR, "10_pluto_2d_basemap.png")
        page.screenshot(path=snap10)
        print(f"Captured: {snap10}")

        # Switch to Titan via Ctrl+K QuickActions (verifying canonical switchActiveBody sync)
        page.keyboard.press("Control+k")
        page.wait_for_selector("#quick-command-backdrop:not([hidden])", state="visible", timeout=5000)
        page.fill("#quick-cmd-input", "Switch Body: Titan")
        page.wait_for_timeout(250)
        page.keyboard.press("Enter")
        page.wait_for_function(
            "document.querySelectorAll('#map .leaflet-tile-loaded').length >= 6",
            timeout=20000
        )
        page.wait_for_timeout(600)
        selected_body = page.locator("#body-selector-container select.body-selector-dropdown").input_value()
        assert selected_body == "titan", f"Expected Ctrl+K body switch to sync BodySelector to 'titan', got '{selected_body}'"
        titan_labels = page.locator(".landing-marker-label").all_inner_texts()
        assert any("Huygens" in lbl for lbl in titan_labels), f"Expected 'Huygens Probe' landing site on Titan, got: {titan_labels}"
        snap11 = os.path.join(SCREENSHOT_DIR, "11_titan_2d_huygens.png")
        page.screenshot(path=snap11)
        print(f"Captured: {snap11}")

        # 3D Globe Astronomical Terminator on Mars (Live UTC Ephemeris vs Local Hour [Model])
        page.select_option("#body-selector-container select.body-selector-dropdown", "mars")
        page.wait_for_timeout(1500)
        page.click("#view-mode-3d")
        page.wait_for_selector("#cesium-container .cesium-viewer", state="visible", timeout=15000)
        page.wait_for_timeout(2500)

        term_btn = page.locator("button.globe-chrome-btn", has_text="Terminator")
        assert term_btn.is_visible(), "Expected 'Terminator' button in 3D globe chrome"
        term_btn.click()
        page.wait_for_timeout(800)

        solar_panel = page.locator(".globe-solar-panel")
        assert solar_panel.is_visible(), "Expected .globe-solar-panel to be visible when Terminator is enabled"
        ephem_readout = page.locator(".globe-solar-readout").inner_text()
        prov_readout = page.locator(".globe-provenance").inner_text()
        print(f"3D Ephemeris Readout: {ephem_readout}")
        assert ephem_readout.startswith("[Ephemeris]") and "IAU WGCCRE" in ephem_readout, f"Unexpected ephemeris readout: {ephem_readout}"
        assert "Terminator shades day/night on the smooth ellipsoid only" in prov_readout, f"Missing smooth-ellipsoid disclaimer: {prov_readout}"

        snap12 = os.path.join(SCREENSHOT_DIR, "12_mars_3d_ephemeris_terminator.png")
        page.screenshot(path=snap12)
        print(f"Captured: {snap12}")

        # Test Modeled Local Hour mode
        local_btn = page.locator("button.globe-solar-mode-btn", has_text="Local Hour [Model]")
        local_btn.click()
        page.wait_for_timeout(400)
        model_readout = page.locator(".globe-solar-readout").inner_text()
        assert model_readout.startswith("[Model]"), f"Expected [Model] readout in local hour mode, got: {model_readout}"
        assert page.locator(".globe-hour").is_visible(), "Expected local solar hour slider to be visible in [Model] mode"

        # Switch back to Live UTC Ephemeris and test Titan + oblate Phobos in 3D (verifying surface tiles > 0!)
        page.locator("button.globe-solar-mode-btn", has_text="Live UTC Ephemeris").click()
        page.select_option("#body-selector-container select.body-selector-dropdown", "titan")
        page.wait_for_function(
            "window.jsmarsView?._globe?.viewer?.scene?.globe?._surface?._tilesToRender?.length > 0",
            timeout=15000
        )
        page.wait_for_timeout(2500)
        snap13 = os.path.join(SCREENSHOT_DIR, "13_titan_3d_terminator.png")
        page.screenshot(path=snap13)
        print(f"Captured: {snap13}")

        page.select_option("#body-selector-container select.body-selector-dropdown", "phobos")
        page.locator("button.globe-solar-mode-btn", has_text="Local Hour [Model]").click()
        page.wait_for_function(
            "window.jsmarsView?._globe?.viewer?.scene?.globe?._surface?._tilesToRender?.length > 0",
            timeout=15000
        )
        page.wait_for_timeout(3500)
        phobos_prov = page.locator(".globe-provenance").inner_text()
        assert "Phobos oblate spheroid" in phobos_prov, f"Expected Phobos oblate spheroid provenance, got: {phobos_prov}"
        snap14 = os.path.join(SCREENSHOT_DIR, "14_phobos_3d_triaxial.png")
        page.screenshot(path=snap14)
        print(f"Captured: {snap14}")

        # Return to 2D Mars before narrow viewport check
        page.click("#view-mode-2d")
        page.select_option("#body-selector-container select.body-selector-dropdown", "mars")
        page.wait_for_timeout(800)

        # Also verify narrow desktop/tablet (800x600) HUD non-overlap
        page.set_viewport_size({"width": 800, "height": 600})
        page.wait_for_timeout(300)
        hud_800 = page.locator("#map-top-hud").bounding_box()
        ctrl_800 = page.locator("#controls").bounding_box()
        toggle_800 = page.locator("#sidebar-toggle").bounding_box()
        assert not boxes_overlap(hud_800, ctrl_800), f"800x600 #map-top-hud overlaps #controls: {hud_800} vs {ctrl_800}"
        assert not boxes_overlap(hud_800, toggle_800), f"800x600 #map-top-hud overlaps #sidebar-toggle: {hud_800} vs {toggle_800}"

        context.close()

        # 8. Mobile Viewport & Europa 3D HUD Ticket Regression (Ticket 2026-11-04_2D-3D-buttons-placement.md)
        print("\nStep 8: Testing Mobile Europa 2D/3D HUD placement (390x844 & 320x568)...")
        mobile_ctx = browser.new_context(
            viewport={"width": 390, "height": 844},
            is_mobile=True,
            has_touch=True,
            device_scale_factor=3
        )
        mpage = mobile_ctx.new_page()
        mpage.on("console", on_console)
        mpage.on("pageerror", on_page_error)

        europa_url = "http://localhost:8080/index.html?body=europa&lat=0&lon=0&z=2&layers=europa_galileo_voyager%3A1%3A1"
        mpage.goto(europa_url, wait_until="networkidle", timeout=30000)
        mpage.wait_for_selector("#map", timeout=15000)
        if mpage.is_visible("#welcome-modal"):
            mpage.click("#welcome-dismiss")
            mpage.wait_for_timeout(500)

        m_status = mpage.locator("#status-bar").bounding_box()
        m_hud_2d = mpage.locator("#map-top-hud").bounding_box()
        m_zoom_2d = mpage.locator(".leaflet-control-zoom").bounding_box()

        assert m_status is not None and m_hud_2d is not None, "Mobile status bar and HUD must be visible"
        assert m_status["x"] + m_status["width"] <= 390.5, f"Mobile #status-bar overflows 390px viewport: {m_status}"
        assert not boxes_overlap(m_hud_2d, m_status), f"Mobile 2D #map-top-hud overlaps #status-bar: {m_hud_2d} vs {m_status}"
        assert m_hud_2d["y"] >= m_status["y"] + m_status["height"] + 4, (
            f"Mobile #map-top-hud must sit cleanly below #status-bar: hud={m_hud_2d}, status={m_status}"
        )
        assert not boxes_overlap(m_hud_2d, m_zoom_2d), f"Mobile 2D #map-top-hud overlaps .leaflet-control-zoom: {m_hud_2d} vs {m_zoom_2d}"

        # Compact 320px width check in 2D mode
        mpage.set_viewport_size({"width": 320, "height": 568})
        mpage.wait_for_timeout(300)
        m_status_320 = mpage.locator("#status-bar").bounding_box()
        m_hud_320 = mpage.locator("#map-top-hud").bounding_box()
        m_zoom_320 = mpage.locator(".leaflet-control-zoom").bounding_box()
        assert not boxes_overlap(m_hud_320, m_status_320), f"320px #map-top-hud overlaps #status-bar: {m_hud_320} vs {m_status_320}"
        assert not boxes_overlap(m_hud_320, m_zoom_320), f"320px #map-top-hud overlaps .leaflet-control-zoom: {m_hud_320} vs {m_zoom_320}"

        # Restore 390x844 phone viewport and switch Europa to 3D Globe mode (exact ticket scenario)
        mpage.set_viewport_size({"width": 390, "height": 844})
        mpage.wait_for_timeout(300)
        mpage.click("#view-mode-3d")
        mpage.wait_for_selector("#cesium-container .cesium-viewer", timeout=15000)
        mpage.wait_for_timeout(4000)

        m_status_3d = mpage.locator("#status-bar").bounding_box()
        m_hud_3d = mpage.locator("#map-top-hud").bounding_box()
        assert not boxes_overlap(m_hud_3d, m_status_3d), f"Mobile 3D #map-top-hud overlaps #status-bar: {m_hud_3d} vs {m_status_3d}"
        print(f"Mobile Europa 3D verified: status_bar={m_status_3d}, map_top_hud={m_hud_3d}")

        snap9 = os.path.join(SCREENSHOT_DIR, "09_mobile_europa_3d_hud.png")
        mpage.screenshot(path=snap9)
        print(f"Captured: {snap9}")

        mobile_ctx.close()
        browser.close()
    if local_httpd:
        local_httpd.shutdown()

    if ARTIFACT_DIR:
        print("\nCopying screenshots to artifact directory...")
        for f in os.listdir(SCREENSHOT_DIR):
            if f.endswith(".png"):
                src = os.path.join(SCREENSHOT_DIR, f)
                dst = os.path.join(ARTIFACT_DIR, f)
                shutil.copy2(src, dst)
                print(f"Copied {f} -> {dst}")

    print("\n================== VERIFICATION SUMMARY ==================")
    print(f"Total Page Errors (Unhandled Exceptions): {len(page_errors)}")
    print(f"Total Console Errors: {len(console_errors)}")
    if page_errors:
        print("Page Errors Details:")
        for pe in page_errors:
            print(f"  - {pe}")
    if console_errors:
        print("Console Errors Details:")
        for ce in console_errors:
            print(f"  - {ce}")
    print("==========================================================")

    if len(page_errors) > 0 or len(console_errors) > 0:
        print("TEST FAILED: Runtime or console errors occurred!")
        sys.exit(1)
    else:
        print("TEST PASSED: 0 unhandled runtime errors and 0 console errors.")


if __name__ == "__main__":
    run_tests()
