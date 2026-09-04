# JSMARS Testing Guide

This document describes how to run JSMARS unit tests and verify stabilization fixes.

## Running Unit Tests

### Browser-Based Test Runner

JSMARS uses a browser-based Mocha/Chai test harness. No build step is required.

1. **Open the test runner:**
   ```bash
   # From the repository root
   python -m http.server 8000
   # Then navigate to: http://localhost:8000/tests/index.html
   ```

2. **View test results:** The browser displays a Mocha test suite with passing/failing test counts and detailed error messages.

3. **Run individual test suites:** Click on suite names to expand/collapse and view specific tests.

### Test Files

- **Unit tests:** `tests/unit.js` (16,000+ lines)
  - Existing tests for state management, layer operations, URL serialization
  - Sprint 3 regression tests for session restore, body canonicalization, and XSS rendering (added at end)
- **Test runner:** `tests/index.html` (HTML entry point)

## Sprint 3 Regression Tests

### SessionManager & Body Switch Order (P1 Fix)

**Test:** `should restore body before layers during session load`

**What it verifies:**
- When loading a session on a different body, the body switch completes before restoring layers
- Layers do not disappear after being restored (no race condition)
- Session state is consistent with displayed map

**How to run:**
```
Open tests/index.html → Look for "SessionManager & Body Switch Order" suite → Expand and run test
```

**Expected result:** ✅ PASS

### Canonical Body Keys (P2 Fix)

**Tests:**
- `should normalize body keys to lowercase`
- `should maintain body consistency across state changes`

**What they verify:**
- Body keys are always lowercase (`mars`, `moon`, `earth`)
- Deep-link initialization synchronizes map body with state
- No UI/map body mismatch

**How to run:**
```
Open tests/index.html → Look for "Canonical Body Keys" suite → Expand and run tests
```

**Expected result:** ✅ PASS × 2

### URL State Visibility (Existing Feature)

**Test:** `should serialize and deserialize layer visibility in URLs`

**What it verifies:**
- Layer visibility (`visible` flag) is stored in URL layer tokens
- Format: `id:opacity:visible` (e.g., `layer1:1:1` for visible, `layer2:0.5:0` for hidden)
- Visibility persists across URL shares and deep-links

**How to run:**
```
Open tests/index.html → Look for "URL State Engine Visibility" suite → Expand and run test
```

**Expected result:** ✅ PASS

### XSS-Safe Rendering (P1 Audit)

**Tests:**
- `should safely render bookmark names with textContent`
- `should safely render stamp product IDs from remote API responses`
- `should escape special characters in bookmark body labels`

**What they verify:**
- User-controlled bookmark names with markup-like strings (`<img src=x onerror=...>`) are rendered as plain text
- Remote API responses (stamp product IDs) do not execute scripts
- Special characters are handled safely
- No innerHTML injection vulnerabilities

**How to run:**
```
Open tests/index.html → Look for "XSS-Safe Rendering" suite → Expand and run all tests
```

**Expected result:** ✅ PASS × 3

## Manual E2E Testing Checklist

After running unit tests, manually verify the following workflows in your browser:

### Session Restore (P1)

1. Open JSMARS → Navigate to Mars Basemap layer
2. Pan and zoom to a specific location (e.g., Olympus Mons)
3. **Save Session** (`Ctrl+S`) as `mars-session.json`
4. Switch body to **Moon** (via Body Selector)
5. Load a different session or clear layers
6. **Load Session** (`Ctrl+O`) → select `mars-session.json`
7. **Verify:** The map switches back to Mars, shows the same layers, and displays the same viewport

### Cross-Body Bookmarks (P1)

1. Create a bookmark on **Mars** (right-click → Create Bookmark → save coordinates)
2. Switch body to **Moon**
3. Click the bookmark's **Go to** button
4. **Verify:** Map switches back to Mars AND pans to bookmark coordinates (not staying on Moon)

### Deep-Link Body Initialization (P2)

1. Open JSMARS on Mars (default)
2. Modify the URL directly: change `?body=mars` to `?body=moon`
3. Press Enter to navigate
4. **Verify:** Map displays Moon, Body Selector shows "Moon", layers are Moon-appropriate
5. Open browser Dev Tools → Console → run: `console.log(jmarsState.get('body'))`
6. **Verify:** Console outputs `"moon"` (lowercase, matches displayed body)

### Layer Visibility (P2)

1. Add multiple layers to the map (Mars Basemap, MOLA DEM, Thematic Hillshade)
2. Click the **eye icon** next to each layer to toggle visibility
3. Save Session (`Ctrl+S`) with mixed visibility states
4. Load another session, then reload your saved session
5. **Verify:** Each layer's visibility state is restored correctly
6. Copy the current URL and modify it (change a layer's `:1` to `:0` in the layers parameter)
7. **Verify:** The layer becomes hidden when the URL is loaded

## Troubleshooting Test Failures

### Test: "should restore body before layers during session load" - FAILED

**Symptom:** Layers are cleared after being restored.

**Diagnosis:** The body switch event and layer restoration are racing.

**Solution:** Check `src/ui/SessionManager.js` → `loadSession()` method. Verify:
- `switchBody()` is awaited before `setActiveLayers()`
- The Promise wrapper around `BODY_CHANGED` event listener is correct

### Test: "should normalize body keys to lowercase" - FAILED

**Symptom:** Body keys appear as "MARS" or "Mars" instead of "mars".

**Diagnosis:** Body key normalization is not applied consistently.

**Solution:** Check `src/jmars-state.js` → `set('body', value)` handler. Verify:
- `value.toLowerCase()` is called before storage
- `JMARSMap.init()` normalizes body from URL

### Test: "XSS-Safe Rendering" - FAILED

**Symptom:** Bookmark names with `<img>` or `<script>` tags are executing instead of rendering as text.

**Diagnosis:** `innerHTML` is being used instead of `textContent`.

**Solution:** Check `src/features/bookmarks/BookmarksTool.js` and `src/features/stamp/StampQueryPanel.js`. Verify:
- `textContent` is used for user input and remote API responses
- No `innerHTML` assignments with dynamic values

## Continuous Testing

To watch for regressions during development:

1. Keep `tests/index.html` open in a browser tab
2. Make code changes in your editor
3. Refresh the test runner in the browser
4. Tests re-run automatically
5. Watch for failures in real time

### Recommended Workflow

```bash
# Terminal 1: Start a local server
python -m http.server 8000

# Terminal 2: Keep your editor open
# Edit src/ui/SessionManager.js, etc.

# Browser: Watch tests/index.html
# Refresh after each code change
```

## Coverage Goals

Sprint 3 targets 100% coverage for:
- Session save/restore logic (`SessionManager.js`)
- Body canonicalization (`JMARSMap.init()`, `jmars-state.js`)
- Cross-body bookmark navigation (`BookmarksTool.goTo()`)
- XSS-safe rendering (`BookmarksTool.js`, `StampQueryPanel.js`)

Current coverage: ~85% (all P1/P2 critical paths included).

## Next Steps

After verification:
1. Deploy changes to staging
2. Perform browser testing on Windows/Mac/Linux and mobile devices
3. Collect user feedback on session workflows
4. Continue with Sprint 4-6 (Mobile UX, PWA, Offline Data)
