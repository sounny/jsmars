import http.server
import os
import random
import socket
import string
import sys
import threading
from playwright.sync_api import sync_playwright

sys.stdout.reconfigure(encoding="utf-8")

SCREENSHOT_DIR = os.path.abspath("verification/screenshots")
os.makedirs(SCREENSHOT_DIR, exist_ok=True)


def boxes_overlap(a, b, pad=0):
    if not a or not b:
        return False
    return not (
        a["x"] + a["width"] + pad <= b["x"]
        or b["x"] + b["width"] + pad <= a["x"]
        or a["y"] + a["height"] + pad <= b["y"]
        or b["y"] + b["height"] + pad <= a["y"]
    )


def start_ephemeral_server():
    class QuietHandler(http.server.SimpleHTTPRequestHandler):
        def log_message(self, fmt, *args):
            pass

    class ThreadedServer(http.server.ThreadingHTTPServer):
        allow_reuse_address = True
        daemon_threads = True

    httpd = ThreadedServer(("127.0.0.1", 0), QuietHandler)
    port = httpd.server_address[1]
    threading.Thread(target=httpd.serve_forever, daemon=True).start()
    print(f"Started dedicated HTTP server on http://127.0.0.1:{port}/")
    return httpd, f"http://127.0.0.1:{port}"


def run_spacecamp_suite():
    httpd, base_url = start_ephemeral_server()
    page_errors = []
    console_errors = []

    def attach_listeners(p, tag=""):
        def on_console(msg):
            if msg.type == "error":
                txt = msg.text
                # Ignore benign external tile 404/CORS hiccups if any, log everything clearly
                console_errors.append(f"{tag}{txt}")
                print(f"[CONSOLE ERROR {tag}] {txt}")

        def on_page_err(err):
            page_errors.append(f"{tag}{err}")
            print(f"[PAGE ERROR {tag}] {err}")

        p.on("console", on_console)
        p.on("pageerror", on_page_err)

    rand_tag = "".join(random.choices(string.ascii_uppercase + string.digits, k=5))
    primary_handle = f"SC_{rand_tag}"

    try:
        with sync_playwright() as pw:
            browser = pw.chromium.launch(headless=True, args=["--no-sandbox", "--disable-setuid-sandbox"])

            # =========================================================================
            # 1. Browser Mocha Unit Test Suite (/tests/index.html)
            # =========================================================================
            print("\n[1/8] Running Mocha Browser Unit Suite (/tests/index.html)...")
            unit_ctx = browser.new_context(viewport={"width": 1280, "height": 800})
            unit_page = unit_ctx.new_page()
            attach_listeners(unit_page, "UNIT:")
            unit_page.goto(f"{base_url}/tests/index.html", wait_until="networkidle", timeout=45000)
            unit_page.wait_for_function("window.__mochaDone === true", timeout=60000)
            stats = unit_page.evaluate("window.__mochaStats")
            print(f"Mocha Unit Results: {stats['passes']}/{stats['total']} passed, {stats['failures']} failed.")
            if stats["failures"] > 0:
                failed_titles = unit_page.evaluate(
                    """() => Array.from(document.querySelectorAll('.test.fail')).map(el => el.innerText)"""
                )
                for ft in failed_titles:
                    print("FAILED UNIT TEST:\n", ft)
                raise AssertionError(f"Mocha unit tests had {stats['failures']} failure(s)!")
            unit_ctx.close()

            # =========================================================================
            # 2. Wide-Screen Docked Layout & Strict English Default (even on fr-FR OS)
            # =========================================================================
            print("\n[2/8] Verifying /spacecamp/ wide-screen side panel & strict English default (with fr-FR OS locale)...")
            ctx1 = browser.new_context(viewport={"width": 1600, "height": 900}, locale="fr-FR")
            page = ctx1.new_page()
            attach_listeners(page, "MAIN:")

            page.goto(f"{base_url}/spacecamp/", wait_until="domcontentloaded", timeout=30000)
            page.wait_for_selector("#sc-side-panel", state="visible", timeout=20000)
            page.wait_for_function("Boolean(window.spaceCamp && window.spaceCamp.initialized)", timeout=25000)
            page.wait_for_timeout(400)

            # Must strictly default to English despite fr-FR browser locale, and start at 0 / 600 PTS
            main_title = page.locator(".sc-main-title").inner_text().strip()
            main_sub = page.locator(".sc-main-subtitle").inner_text().strip()
            assert main_title == "Mission JSMARS: Space Data Analysts", f"Unexpected title: {main_title}"
            assert "Digital Workshop in Mission Planning" in main_sub, f"Expected English default subtitle, got: {main_sub}"
            assert "is-active" in (page.locator("#sc-lang-en-btn").get_attribute("class") or ""), "EN button must be active by default"
            assert "0 / 600 PTS" in page.locator(".sc-score-pill").inner_text(), (
                f"Fresh player must start at 0 / 600 PTS, got: {page.locator('.sc-score-pill').inner_text()}"
            )

            # No forced blocking modal on startup
            modal_hidden = page.locator(".sc-modal-backdrop").evaluate("el => el.hidden")
            assert modal_hidden is True, "Space Camp must not block wide-screen map with a startup modal!"

            # Co-docked map & right side panel geometry (1600 = 1216 map + 384 panel)
            panel_box = page.locator("#sc-side-panel").bounding_box()
            map_box = page.locator("#map").bounding_box()
            zoom_box = page.locator(".leaflet-control-zoom").bounding_box()
            hud_box = page.locator(".map-top-hud").bounding_box()
            edge_box = page.locator("#sc-edge-toggle-btn").bounding_box()
            top_right_controls = page.locator(".leaflet-top.leaflet-right .leaflet-control")
            assert panel_box is not None and map_box is not None and hud_box is not None and edge_box is not None
            assert abs(panel_box["width"] - 384) <= 2, f"Expected 384px side panel, got {panel_box}"
            assert abs(map_box["width"] - 1216) <= 2, f"Expected 1216px co-docked #map on 1600px screen, got {map_box}"
            assert not boxes_overlap(map_box, panel_box), f"#map overlaps #sc-side-panel: {map_box} vs {panel_box}"
            assert not boxes_overlap(zoom_box, panel_box), f"Zoom control overlaps #sc-side-panel: {zoom_box} vs {panel_box}"
            assert not boxes_overlap(hud_box, zoom_box), f"Top HUD overlaps zoom control at 1600px: {hud_box} vs {zoom_box}"
            for ci in range(top_right_controls.count()):
                cbox = top_right_controls.nth(ci).bounding_box()
                if cbox:
                    assert not boxes_overlap(edge_box, cbox), f"Edge toggle overlaps right toolbar #{ci} at 1600px: {edge_box} vs {cbox}"

            # Also verify zero HUD / toolbar / edge-toggle overlap at medium 1024x768 viewport
            page.set_viewport_size({"width": 1024, "height": 768})
            page.wait_for_timeout(350)
            hud_1024 = page.locator(".map-top-hud").bounding_box()
            zoom_1024 = page.locator(".leaflet-control-zoom").bounding_box()
            sb_toggle_1024 = page.locator("#sidebar-toggle").bounding_box()
            edge_1024 = page.locator("#sc-edge-toggle-btn").bounding_box()
            assert not boxes_overlap(hud_1024, zoom_1024), f"Top HUD overlaps zoom at 1024x768: {hud_1024} vs {zoom_1024}"
            if sb_toggle_1024:
                assert not boxes_overlap(hud_1024, sb_toggle_1024), f"Top HUD overlaps sidebar toggle at 1024x768: {hud_1024} vs {sb_toggle_1024}"
            for ci in range(top_right_controls.count()):
                cbox = top_right_controls.nth(ci).bounding_box()
                if cbox:
                    assert not boxes_overlap(edge_1024, cbox), f"Edge toggle overlaps right toolbar #{ci} at 1024x768: {edge_1024} vs {cbox}"
            page.screenshot(path=os.path.join(SCREENSHOT_DIR, "sc_medium_hud_clearance.png"))
            page.set_viewport_size({"width": 1600, "height": 900})
            page.wait_for_timeout(350)

            # Test collapsible edge tab
            page.click("#sc-edge-toggle-btn")
            page.wait_for_timeout(400)
            expanded_map_box = page.locator("#map").bounding_box()
            assert abs(expanded_map_box["width"] - 1600) <= 2, f"Collapsed panel should give #map full 1600px width, got {expanded_map_box}"
            page.click("#sc-edge-toggle-btn")
            page.wait_for_timeout(400)

            # =========================================================================
            # 3. French (FR) Language Switch, Briefing Co-Creators, Slide 2 Image & Zero Callsign on Slides
            # =========================================================================
            print("\n[3/8] Testing EN | FR language switch, Slide 1 Co-Creators, Slide 2 Noor image & clean slides...")
            page.click("#sc-lang-fr-btn")
            page.wait_for_timeout(250)
            fr_title = page.locator(".sc-main-title").inner_text().strip()
            fr_sub = page.locator(".sc-main-subtitle").inner_text().strip()
            assert fr_title == "Mission JSMARS: Space Data Analysts", f"Unexpected FR title: {fr_title}"
            assert fr_sub == "Atelier numérique de planification de missions et d'analyse en télédétection", f"Unexpected FR subtitle: {fr_sub}"
            assert "Classement" in page.locator("#sc-open-leaderboard-btn").inner_text()

            page.click("#sc-lang-en-btn")
            page.wait_for_timeout(200)

            # Open Briefing Slides modal (before claiming callsign!) and verify:
            # 1) Zero callsign claim form on slides
            # 2) Both Dr. Moulay Anwar Sounny-Slitine & Sara Bensalem on Slide 1
            # 3) Noor Ouarzazate image loaded on Slide 2
            page.click("#sc-open-briefing-btn")
            page.wait_for_timeout(250)
            assert page.locator(".sc-modal-backdrop").evaluate("el => !el.hidden") is True
            modal_text_s1 = page.locator(".sc-modal").inner_text()
            assert "Dr. Moulay Anwar Sounny-Slitine" in modal_text_s1
            assert "Sara Bensalem" in modal_text_s1
            assert "Assistant Professor of Practice" not in modal_text_s1
            assert page.locator(".sc-modal #sc-modal-join-card").count() == 0, "Slides modal must NOT show Claim Callsign card!"
            assert "Callsign" not in modal_text_s1 and "REGISTER OPERATOR CALLSIGN" not in modal_text_s1
            page.screenshot(path=os.path.join(SCREENSHOT_DIR, "sc_slide1_cocreators.png"))

            # Navigate to Slide 2 and verify Noor Ouarzazate image renders
            page.locator(".sc-slide-nav .sc-btn").last.click()
            page.wait_for_timeout(300)
            slide2_img_ok = page.locator(".sc-modal img.sc-slide-image").evaluate("img => img.complete && img.naturalWidth > 200")
            assert slide2_img_ok is True, "Slide 2 Noor Ouarzazate image failed to load!"
            assert page.locator(".sc-modal #sc-modal-join-card").count() == 0
            page.screenshot(path=os.path.join(SCREENSHOT_DIR, "sc_slide2_noor.png"))

            page.keyboard.press("Escape")
            page.wait_for_timeout(200)

            # =========================================================================
            # 4. Live Firebase Callsign Claim, XSS Block, Collision Suggestions & Reload
            # =========================================================================
            print(f"\n[4/8] Testing live Firebase handle claim (@{primary_handle}), XSS block & collision suggestions...")
            page.fill("#sc-handle-input", "<img src=x onerror=1>")
            page.click("#sc-handle-claim-btn")
            page.wait_for_timeout(200)
            assert "3-20" in page.locator("#sc-handle-status").inner_text()

            page.fill("#sc-handle-input", primary_handle)
            page.click("#sc-handle-claim-btn")
            page.wait_for_selector(".sc-callsign-badge", state="visible", timeout=15000)
            badge_txt = page.locator(".sc-callsign-badge").inner_text()
            assert f"@{primary_handle}" in badge_txt, f"Expected @{primary_handle}, got {badge_txt}"
            print(f"Claimed live Firebase callsign: @{primary_handle}")

            # Second browser context tries claiming the SAME handle (case-insensitive)
            ctx2 = browser.new_context(viewport={"width": 1440, "height": 900})
            page2 = ctx2.new_page()
            attach_listeners(page2, "CTX2:")
            page2.goto(f"{base_url}/spacecamp/", wait_until="domcontentloaded", timeout=30000)
            page2.wait_for_selector("#sc-handle-input", state="visible", timeout=15000)
            page2.fill("#sc-handle-input", primary_handle.lower())
            page2.click("#sc-handle-claim-btn")
            page2.wait_for_selector("#sc-handle-suggestions .sc-suggestion-chip", state="visible", timeout=15000)
            chips = page2.locator("#sc-handle-suggestions .sc-suggestion-chip")
            chip_count = chips.count()
            assert chip_count == 3, f"Expected 3 collision-free suggestions, got {chip_count}"
            chosen_sug = chips.first.inner_text().strip()
            print(f"Duplicate collision caught cleanly! Offered 3 suggestions; clicking {chosen_sug}...")
            chips.first.click()
            page2.wait_for_selector(".sc-callsign-badge", state="visible", timeout=15000)
            assert chosen_sug in page2.locator(".sc-callsign-badge").inner_text()
            ctx2.close()

            # Reload primary page -> cookie restores @primary_handle automatically
            page.reload(wait_until="domcontentloaded")
            page.wait_for_function("Boolean(window.spaceCamp && window.spaceCamp.initialized)", timeout=25000)
            page.wait_for_selector(".sc-callsign-badge", state="visible", timeout=15000)
            assert f"@{primary_handle}" in page.locator(".sc-callsign-badge").inner_text()

            # =========================================================================
            # 5. Play Through All 5 Progressive Rounds (600 Max Points Architecture)
            # =========================================================================
            print("\n[5/8] Playing through all 5 progressive Space Camp rounds...")

            # --- ROUND 1: Earth Observation Land Cover Classification (60 pts) ---
            r1_cards = page.locator("#sc-side-panel .sc-drawer-body .sc-step-card")
            assert r1_cards.count() == 5, f"Expected 4 EO site cards + 1 Measurement card in Round 1, got {r1_cards.count()}"
            expected_classes = [
                "Forest / Woodland",
                "Urban / Built-Up",
                "Irrigated Cropland",
                "Hyper-Arid Desert Dunes",
            ]
            for idx, label in enumerate(expected_classes):
                r1_cards.nth(idx).locator(".sc-class-chip", has_text=label).click()
                page.wait_for_timeout(150)

            # Launch REAL Measure Area (Polygon) tool from Round 1 card and close polygon by clicking LAST point!
            r1_cards.nth(4).locator("button", has_text="Measure Area (Polygon)").click()
            page.wait_for_timeout(400)
            poly_pts = [(520, 340), (660, 340), (660, 470), (520, 470)]
            for px, py in poly_pts:
                page.mouse.click(px, py)
                page.wait_for_timeout(120)
            # Click the LAST point (520, 470) to finish polygon!
            page.mouse.click(520, 470)
            page.wait_for_timeout(400)
            assert "60 / 600 PTS" in page.locator(".sc-score-pill").inner_text(), (
                f"Expected 60 pts after Round 1 polygon last-point click, got {page.locator('.sc-score-pill').inner_text()}"
            )
            page.screenshot(path=os.path.join(SCREENSHOT_DIR, "sc_01_round1_earth_eo.png"))
            print("Round 1 verified: 60/60 pts!")

            # Verify SearchBar hides Olympus Mons spoiler before Round 2 solve
            olympus_hidden = page.evaluate(
                """() => {
                    const inp = document.querySelector('#search-bar-container input');
                    if (!inp) return false;
                    inp.value = 'Olympus';
                    inp.dispatchEvent(new Event('focus'));
                    const txt = document.querySelector('#search-bar-container .modern-search-results')?.innerText || '';
                    inp.value = '';
                    return !txt.includes('Olympus Mons');
                }"""
            )
            assert olympus_hidden is True, "SearchBar spoiler filter must hide 'Olympus Mons' before scanning!"

            # --- ROUND 2: Mars Laser Altimetry & Giant Tectonics (90 pts) ---
            page.locator(".sc-round-tab").nth(1).click()
            page.wait_for_timeout(300)
            page.locator("#sc-side-panel .sc-drawer-body button", has_text="Switch to Mars + Enable MOLA Elevation").click()
            page.wait_for_timeout(1200)

            # Verify scanner reticle is visible inside #map
            assert "is-visible" in (page.locator("#map .sc-reticle").get_attribute("class") or "")

            # Center on Olympus Mons (18.65, -133.8) and scan
            page.evaluate("window.spaceCamp.map.setView([18.65, -133.8], 5, { animate: false })")
            page.wait_for_timeout(200)
            page.locator("#sc-side-panel .sc-drawer-body .sc-step-card").nth(0).locator("button", has_text="Scan Reticle Target").click()
            page.wait_for_timeout(250)

            # Center on Valles Marineris (-13.9, -59.2) and scan
            page.evaluate("window.spaceCamp.map.setView([-13.9, -59.2], 5, { animate: false })")
            page.wait_for_timeout(200)
            page.locator("#sc-side-panel .sc-drawer-body .sc-step-card").nth(1).locator("button", has_text="Scan Reticle Target").click()
            page.wait_for_timeout(250)

            # Trigger MOLA Elevation Profile completion on Mars (+30 pts)
            page.evaluate("document.dispatchEvent(new CustomEvent('jmars:profile-generated', { detail: { points: 32 } }))")
            page.wait_for_timeout(300)
            assert "150 / 600 PTS" in page.locator(".sc-score-pill").inner_text(), (
                f"Expected 150 pts after Round 2, got {page.locator('.sc-score-pill').inner_text()}"
            )
            print("Round 2 verified: 90/90 pts (Cumulative 150/600)!")

            # --- ROUND 3: Mars Rover Path & Payload Planning at Jezero (120 pts) ---
            page.locator(".sc-round-tab").nth(2).click()
            page.wait_for_timeout(300)

            # Check the first 3 checkboxes (the 3 real Mars rover tools)
            checkboxes = page.locator("#sc-side-panel .sc-check-list input[type='checkbox']")
            for i in range(3):
                if not checkboxes.nth(i).is_checked():
                    checkboxes.nth(i).check()

            # Plot low-slope rover traverse through Neretva Vallis to Nili Highlands
            page.locator("#sc-side-panel button", has_text="Center Map on Jezero Traverse Zone").click()
            page.wait_for_timeout(700)
            page.click("#sc-draw-rover-btn")
            page.wait_for_timeout(200)
            page.evaluate(
                """() => {
                    const ov = window.spaceCamp;
                    ov._onMapClickRover({ latlng: { lat: 18.46, lng: 77.45 } });
                    ov._onMapClickRover({ latlng: { lat: 18.52, lng: 77.33 } });
                    ov._onMapClickRover({ latlng: { lat: 18.63, lng: 77.14 } });
                    ov._onMapClickRover({ latlng: { lat: 18.72, lng: 76.95 } });
                }"""
            )
            page.wait_for_timeout(200)
            page.click("#sc-score-rover-btn")
            page.wait_for_function(
                "window.spaceCamp.engine.state.r3BestRoutePts > 0",
                timeout=15000,
            )

            # Bonus Step 3C: Toggle Landing Sites on Mars (+15 pts)
            page.locator("#sc-side-panel button", has_text="Toggle Landing Sites").click()
            page.wait_for_timeout(500)

            r3_pts = page.evaluate("window.spaceCamp.engine.getRoundScores().r3")
            r5_unlocked = page.evaluate("window.spaceCamp.engine.isRound5Unlocked()")
            assert r3_pts >= 90, f"Expected strong Round 3 rover score (>=90/120), got {r3_pts}"
            assert r5_unlocked is True, "Round 5 must auto-unlock immediately once 3 rounds have points!"
            page.screenshot(path=os.path.join(SCREENSHOT_DIR, "sc_02_round3_mars_rover.png"))
            print(f"Round 3 verified: {r3_pts}/120 pts — Round 5 auto-unlocked!")

            # --- ROUND 4: Artemis Lunar South Pole Landing Site Selection (150 pts) ---
            page.locator(".sc-round-tab").nth(3).click()
            page.wait_for_timeout(300)
            page.click("#sc-open-lunar-planner-btn")
            page.wait_for_selector(".lsp-workspace", state="visible", timeout=15000)
            page.wait_for_function(
                'document.querySelector(".lsp-workspace select.lsp-select") && document.querySelector(".lsp-workspace select.lsp-select").options.length > 1',
                timeout=25000,
            )
            page.locator(".lsp-workspace select.lsp-select").select_option("Site01")
            page.wait_for_function(
                'document.querySelector(".lsp-status") && document.querySelector(".lsp-status").textContent.includes("Connecting ridge:")',
                timeout=25000,
            )
            page.wait_for_timeout(600)

            lsp_map_box = page.locator(".lsp-map").bounding_box()
            assert lsp_map_box is not None
            page.mouse.click(lsp_map_box["x"] + lsp_map_box["width"] * 0.5, lsp_map_box["y"] + lsp_map_box["height"] * 0.5)
            page.wait_for_selector("#sc-commit-exit-lunar-btn", state="visible", timeout=10000)
            page.screenshot(path=os.path.join(SCREENSHOT_DIR, "sc_03_round4_lunar_lola.png"))
            page.click("#sc-commit-exit-lunar-btn")
            page.wait_for_timeout(500)
            assert page.locator(".lsp-workspace").is_visible() is False, "Save & Return to Trails should close .lsp-workspace"
            r4_pts = page.evaluate("window.spaceCamp.engine.getRoundScores().r4")
            assert r4_pts > 0, f"Expected positive NASA LOLA landing site score, got {r4_pts}"
            print(f"Round 4 verified: {r4_pts}/150 pts from real NASA LRO LOLA rasters!")

            # --- ROUND 5: Europa Ocean World Finale (180 pts) ---
            page.locator(".sc-round-tab").nth(4).click()
            page.wait_for_timeout(300)
            page.locator("#sc-side-panel button", has_text="Enable 9 m/px Galileo Mosaic").click()
            page.wait_for_timeout(1200)
            page.locator("#sc-side-panel button", has_text="Scan Reticle Target").click()
            page.wait_for_timeout(300)
            page.evaluate("document.dispatchEvent(new CustomEvent('jmars:shape-created', { detail: { type: 'Polygon' } }))")
            page.wait_for_timeout(500)

            scores_final = page.evaluate("window.spaceCamp.engine.getRoundScores()")
            assert scores_final["r5"] == 180, f"Expected 180/180 in Round 5, got {scores_final}"
            assert scores_final["total"] >= 450, f"Expected >= 450/600 total points, got {scores_final}"
            print(f"Round 5 verified: 180/180 pts! Final 5-Round Player Score = {scores_final['total']}/600 pts.")

            # =========================================================================
            # 6. AI CapCom Mode & #gemini_key URL Hash Scrubbing
            # =========================================================================
            print("\n[6/8] Verifying AI CapCom panel & #gemini_key hash scrubbing...")
            page.click("#sc-mode-ai-btn")
            page.wait_for_timeout(250)
            page.locator("#sc-side-panel button", has_text="Analyze Current View").click()
            page.wait_for_selector(".sc-chat-msg.is-ai", state="visible", timeout=8000)
            ai_reply = page.locator(".sc-chat-msg.is-ai").first.inner_text()
            assert "EUROPA" in ai_reply.upper() or "CONAMARA" in ai_reply.upper(), f"Unexpected CapCom reply: {ai_reply}"

            # Open Gemini Key drawer UI and close
            page.locator("#sc-side-panel button", has_text="Set Gemini Key").click()
            page.wait_for_selector("#sc-gemini-key-input", state="visible", timeout=5000)

            # Verify URL #gemini_key=... hash ingestion scrubs the hash from window.location
            hash_ctx = browser.new_context(viewport={"width": 1280, "height": 800})
            hash_page = hash_ctx.new_page()
            hash_page.goto(f"{base_url}/spacecamp/#gemini_key=AIzaSyTestDummyLengthKey123456", wait_until="domcontentloaded")
            hash_page.wait_for_selector("#sc-side-panel", state="visible", timeout=15000)
            scrubbed_hash = hash_page.evaluate("window.location.hash")
            assert "gemini_key" not in scrubbed_hash, f"Expected #gemini_key to be scrubbed from URL bar, got {scrubbed_hash}"
            hash_ctx.close()

            # =========================================================================
            # 7. In-App Leaderboard Modal + Standalone /spacecamp/leaderboard/ Podium
            # =========================================================================
            print("\n[7/8] Verifying In-App Leaderboard button & standalone /spacecamp/leaderboard/ Podium...")
            # Wait briefly for Firebase syncProgress PUT/POST to settle
            page.wait_for_timeout(1500)
            page.click("#sc-open-leaderboard-btn")
            page.wait_for_selector(".sc-modal.is-wide", state="visible", timeout=10000)
            page.wait_for_function(
                f"document.querySelector('.sc-modal')?.innerText.includes('@{primary_handle}')",
                timeout=15000,
            )
            lb_modal_text = page.locator(".sc-modal").inner_text()
            assert f"{scores_final['total']}" in lb_modal_text, f"Expected live score {scores_final['total']} in modal:\n{lb_modal_text}"
            page.screenshot(path=os.path.join(SCREENSHOT_DIR, "sc_04_inapp_leaderboard_modal.png"))
            page.keyboard.press("Escape")

            # Standalone 1080p Projector & Winner's Circle Podium Page
            proj_ctx = browser.new_context(viewport={"width": 1920, "height": 1080}, locale="fr-FR")
            proj_page = proj_ctx.new_page()
            attach_listeners(proj_page, "PROJ:")
            proj_page.goto(f"{base_url}/spacecamp/leaderboard/", wait_until="domcontentloaded", timeout=30000)
            proj_page.wait_for_selector("#sc-proj-podium-btn", state="visible", timeout=15000)
            proj_page.wait_for_function(
                f"document.body.innerText.includes('@{primary_handle}')",
                timeout=15000,
            )
            proj_text_en = proj_page.locator("body").inner_text()
            assert "Mission JSMARS: Space Data Analysts" in proj_text_en
            assert "Atelier numérique de planification de missions et d'analyse en télédétection" in proj_text_en
            proj_page.screenshot(path=os.path.join(SCREENSHOT_DIR, "sc_05_projector_race_standings.png"))

            # Switch to Winner's Circle Podium view
            proj_page.click("#sc-proj-podium-btn")
            proj_page.wait_for_timeout(400)
            podium_text = proj_page.locator("body").inner_text()
            assert "WINNER’S CIRCLE" in podium_text or "WINNER'S CIRCLE" in podium_text
            assert "SPECIALIST MISSION COMMENDATIONS" in podium_text
            assert f"@{primary_handle}" in podium_text
            proj_page.screenshot(path=os.path.join(SCREENSHOT_DIR, "sc_06_projector_winners_podium.png"))

            # Test FR toggle on projector page
            proj_page.locator(".sc-lang-opt", has_text="FR").click()
            proj_page.wait_for_timeout(300)
            assert "PODIUM DES LAURÉATS" in proj_page.locator("body").inner_text()
            proj_ctx.close()

            # =========================================================================
            # 8. Mobile Responsive Check (390x844)
            # =========================================================================
            print("\n[8/8] Verifying mobile responsive layout (390x844)...")
            mob_ctx = browser.new_context(viewport={"width": 390, "height": 844}, is_mobile=True, has_touch=True)
            mob_page = mob_ctx.new_page()
            attach_listeners(mob_page, "MOB:")
            mob_page.goto(f"{base_url}/spacecamp/", wait_until="domcontentloaded", timeout=30000)
            mob_page.wait_for_selector("#sc-side-panel", state="visible", timeout=15000)
            mob_page.wait_for_timeout(800)
            mob_panel = mob_page.locator("#sc-side-panel").bounding_box()
            assert mob_panel is not None and mob_panel["width"] <= 390.5, f"Mobile panel overflows 390px: {mob_panel}"
            mob_page.click("#sc-edge-toggle-btn")
            mob_page.wait_for_timeout(400)
            mob_ctx.close()

            ctx1.close()
            browser.close()
    finally:
        if httpd:
            httpd.shutdown()

    print("\n================== SPACE CAMP E2E SUMMARY ==================")
    print(f"Page Errors: {len(page_errors)}")
    print(f"Console Errors: {len(console_errors)}")
    print("============================================================")
    if page_errors or console_errors:
        sys.exit(1)
    print("ALL SPACE CAMP UNIT + E2E VERIFICATIONS PASSED!")


if __name__ == "__main__":
    run_spacecamp_suite()
