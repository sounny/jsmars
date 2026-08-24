"""
verify_and_push.py

Automated CI/CD script for JSMARS.
1. Starts a local HTTP server and runs the full Mocha/Chai unit test suite & browser sanity checks via Playwright.
2. Verifies that the reference `jmars/` directory is pristine and untouched.
3. If all tests pass and there are no fatal errors:
   - Stages all working changes (excluding temporary scratch files)
   - Creates a structured semantic Git commit
   - Pushes to `origin main`
4. If any test or runtime error occurs:
   - Logs the failure to `docs/LOG.md` for the 15-minute self-improvement cron task to resolve.
   - Halts push to keep production branches green.
"""

import http.server
import socketserver
import threading
import time
import subprocess
import sys
import os
import datetime

DIRECTORY = os.path.abspath(os.path.join(os.path.dirname(__file__), ".."))

def log_to_work_log(message):
    log_path = os.path.join(DIRECTORY, "docs", "LOG.md")
    timestamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M:%S UTC")
    entry = f"\n\n### Automated 4-Hour Check Alert [{timestamp}]\n{message}\n"
    try:
        with open(log_path, "a", encoding="utf-8") as f:
            f.write(entry)
        print(f"Logged alert to docs/LOG.md: {message}")
    except Exception as e:
        print(f"Failed to write to docs/LOG.md: {e}")

def run_tests():
    from playwright.sync_api import sync_playwright

    class Handler(http.server.SimpleHTTPRequestHandler):
        def __init__(self, *args, **kwargs):
            super().__init__(*args, directory=DIRECTORY, **kwargs)

        def log_message(self, format, *args):
            pass

    socketserver.TCPServer.allow_reuse_address = True
    server = socketserver.TCPServer(("127.0.0.1", 0), Handler)
    port = server.server_address[1]

    server_thread = threading.Thread(target=server.serve_forever, daemon=True)
    server_thread.start()
    time.sleep(0.5)

    print(f"[TESTS] HTTP server started on port {port}")

    with sync_playwright() as p:
        browser = p.chromium.launch(headless=True)
        page = browser.new_page()

        console_errors = []
        page.on("console", lambda msg: console_errors.append(f"[{msg.type}] {msg.text}") if msg.type in ["error"] else None)
        page.on("pageerror", lambda exc: console_errors.append(f"[pageerror] {exc}"))

        # 1. Mocha Unit Test Suite
        print("[TESTS] Running unit tests in tests/index.html...")
        page.goto(f"http://127.0.0.1:{port}/tests/index.html")
        page.wait_for_selector("#mocha-stats", timeout=12000)
        page.wait_for_timeout(2000)

        passes = int(page.locator(".passes em").inner_text() or 0)
        failures = int(page.locator(".failures em").inner_text() or 0)

        if failures > 0:
            fails = page.locator(".test.fail").all()
            fail_msgs = []
            for f in fails:
                title = f.locator("h2").inner_text().encode('ascii', 'replace').decode('ascii')
                err = f.locator("pre.error").inner_text().encode('ascii', 'replace').decode('ascii') if f.locator("pre.error").count() > 0 else "Unknown error"
                fail_msgs.append(f"- {title}: {err}")
            browser.close()
            server.shutdown()
            return False, f"Unit test suite failed ({failures} failures):\n" + "\n".join(fail_msgs)

        # 2. Main Web App Validation & Interactive UI Testing
        print("[TESTS] Validating index.html app runtime & exercising UI panels...")
        page.goto(f"http://127.0.0.1:{port}/index.html")
        page.wait_for_timeout(2000)

        # Dismiss welcome modal and exercise all panels & tools via browser context
        page.evaluate("""() => {
            const btn = document.getElementById('welcome-dismiss');
            if (btn) btn.click();

            // Expand all accordion sections
            document.querySelectorAll('.layer-section-header').forEach(h => h.click());

            // Switch body to moon and back to mars
            const select = document.querySelector('select.body-selector-dropdown');
            if (select) {
                select.value = 'moon';
                select.dispatchEvent(new Event('change'));
                setTimeout(() => {
                    select.value = 'mars';
                    select.dispatchEvent(new Event('change'));
                }, 150);
            }

            // Trigger KRC simulate
            const krcBtn = document.getElementById('krc-simulate-btn');
            if (krcBtn) krcBtn.click();

            // Trigger Band Math preset
            const bmSelect = document.getElementById('bm-preset-select');
            if (bmSelect) {
                bmSelect.value = 'bd1900_h2o';
                bmSelect.dispatchEvent(new Event('change'));
            }

            // Trigger Radar preset
            const radarSelect = document.getElementById('radar-preset-select');
            if (radarSelect) {
                radarSelect.value = 'australe';
                radarSelect.dispatchEvent(new Event('change'));
            }
        }""")

        page.wait_for_timeout(1500)

        if console_errors:
            browser.close()
            server.shutdown()
            return False, "Browser console errors encountered during UI interaction:\n" + "\n".join(console_errors)

        browser.close()
        server.shutdown()

    return True, f"All {passes} unit tests passed and interactive app UI exercised cleanly with 0 console errors."

def check_jmars_integrity():
    # Verify no files in jmars/ are modified
    res = subprocess.run(["git", "status", "--porcelain", "jmars/"], cwd=DIRECTORY, capture_output=True, text=True)
    if res.stdout.strip():
        return False, f"Forbidden modification detected in reference directory `jmars/`:\n{res.stdout}"
    return True, "Reference directory `jmars/` is pristine."

def commit_and_push():
    print("[GIT] Checking git status...")
    status_res = subprocess.run(["git", "status", "--porcelain"], cwd=DIRECTORY, capture_output=True, text=True)
    changes = status_res.stdout.strip()
    if not changes:
        print("[GIT] Working tree is clean. Nothing to commit.")
        return True, "Working tree clean, nothing to commit."

    print("[GIT] Staging changes...")
    subprocess.run(["git", "add", "."], cwd=DIRECTORY, check=True)

    # Check if there are staged changes
    diff_res = subprocess.run(["git", "diff", "--cached", "--quiet"], cwd=DIRECTORY)
    if diff_res.returncode == 0:
        print("[GIT] No staged changes to commit.")
        return True, "No staged changes to commit."

    timestamp = datetime.datetime.now(datetime.timezone.utc).strftime("%Y-%m-%d %H:%M UTC")
    commit_msg = f"feat(parity): Automated sync & parity progression [{timestamp}]\n\n- Verified all automated Mocha/Chai tests pass\n- Reference jmars/ integrity verified\n- Automated 4-hour CI sync"
    
    print(f"[GIT] Committing: {commit_msg.splitlines()[0]}")
    commit_proc = subprocess.run(["git", "commit", "-m", commit_msg], cwd=DIRECTORY, capture_output=True, text=True)
    if commit_proc.returncode != 0:
        return False, f"Git commit failed:\n{commit_proc.stderr or commit_proc.stdout}"

    print("[GIT] Pushing to origin main...")
    push_proc = subprocess.run(["git", "push", "origin", "main"], cwd=DIRECTORY, capture_output=True, text=True)
    if push_proc.returncode != 0:
        return False, f"Git push failed:\n{push_proc.stderr or push_proc.stdout}"

    print("[GIT] Successfully pushed to origin main!")
    return True, "Successfully committed and pushed to origin main."

def main():
    print("=== JSMARS 4-Hour Automated Verification & Push ===")
    
    # 1. Check jmars/ reference integrity
    jmars_ok, jmars_msg = check_jmars_integrity()
    if not jmars_ok:
        log_to_work_log(f"CRITICAL: {jmars_msg}")
        print(f"[ERROR] {jmars_msg}")
        sys.exit(1)

    # 2. Run automated test suite
    test_ok, test_msg = run_tests()
    if not test_ok:
        log_to_work_log(f"TEST FAILURE: {test_msg}")
        print(f"[ERROR] {test_msg}")
        sys.exit(1)
    print(f"[SUCCESS] {test_msg}")

    # 3. Commit and push
    push_ok, push_msg = commit_and_push()
    if not push_ok:
        log_to_work_log(f"PUSH ERROR: {push_msg}")
        print(f"[ERROR] {push_msg}")
        sys.exit(1)

    print(f"[SUCCESS] {push_msg}")
    print("=== Verification & Push Complete ===")

if __name__ == "__main__":
    main()
