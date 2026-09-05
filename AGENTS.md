# JMARS JS Port – Agent Briefing

This file explains the project goals, architecture, and rules for contributors (agents).

---

## 1. Project Overview
- **Goal:** Build a JavaScript, browser-based client inspired by JMARS.
- **Live URL:** https://jsmars.sounny.com/
- **Stack:** Vanilla JS (ES Modules), Leaflet, CSS. **No Build Step.**
- **License:** GPLv3 Compatible.

## 📚 Reference Material
The original Java-based JMARS application is located in the `jmars/` folder at the root of this repository.
- **Upstream Source**: ASU Mars Space Flight Facility Subversion repository at `https://oss.mars.asu.edu/svn/jmars/default/trunk/`.
- **Sync Cadence**: Check every **3 months** for upstream updates or new release tags (`python scripts/sync_jmars_svn.py --check`). Run `python scripts/sync_jmars_svn.py` to synchronize any new upstream files.
- **Usage**: Use this to compare features, UI/UX flows, and data visualization styles.
- **Goal**: We aim for feature parity where possible, but adapted for modern web patterns.

> [!WARNING]
> **DO NOT MODIFY** the contents of the `jmars/` folder directly. It is strictly for reference purposes only and should only be updated via the upstream sync script. Any manual changes to the reference application will invalidate it as a source of truth.

## 2. Architecture & Patterns

### 2.1. Event-Driven Design
- Use `CustomEvent` to communicate between modules.
- **Examples:** `jmars:layers-updated`, `jmars:body-changed`, `jmars:shape-created`.
- Avoid tight coupling between the Map and UI components.

### 2.2. State Management
- Introduce a lightweight store (plain object) to track:
  - Active Body
  - Active Layers (and order)
  - Session Data (Bookmarks, ROIs)
- This simplifies session serialization (Save/Load).

### 2.3. UI Components
- Keep components in `/src/ui/` small and self-contained.
- Components should accept a container element and subscribe to relevant events.
- **Accessibility:** Use ARIA labels, semantic HTML, and ensure keyboard navigability.

### 2.4. Configuration
- `jmars-config.js` holds defaults (Grid spacing, Scalebar units, Bodies).
- Allow overrides via query parameters or loaded sessions.

## 3. Coding Conventions
- **Modules:** Use standard ES modules (`import`/`export`).
- **Formatting:** Clean, readable code. Add comments for complex logic.
- **Error Handling:** Fail gracefully. Show UI feedback for network errors.
- **Dependencies:** Minimize external deps. Use CDNs for libraries like `Leaflet.Draw`.

## 4. Testing & Verification
- **Manual:** Verify all UI changes in the browser.
- **Automated:**
  - **Unit Tests:** For utility functions (`src/util/`), use a browser-compatible runner if added.
  - **E2E Tests:** Playwright tests should cover:
    - Layer ordering
    - Shape editing
    - Session Save/Load
    - Measurement tools

## 5. Documentation
- **Update Docs:** When adding features, update `docs/jsmars-roadmap.md` and `docs/user-guide.md`.
- **Release Notes:** Add an entry to `docs/release-notes.md` for every milestone.

---

## 6. Directory Structure
```text
/AGENTS.md            # This file
/index.html           # Entry point
/src/
  jmars-config.js     # Config
  jmars-map.js        # Core map logic
  jmars-state.js      # (New) State management
  layers/             # Layer definitions
  ui/                 # UI Components (LayerManager, Panner, etc.)
  util/               # Helpers (Geo, IO, Formats)
/docs/
  jsmars-roadmap.md   # Development plan
  transition-plan.md  # JMARS -> Web mapping
  user-guide.md       # User instructions
  release-notes.md    # Changelog
```

---

## 7. Review Follow-up Notes (2026-08-27)

The following issues were identified in a whole-project review. Treat the P1
items as a stabilization milestone before expanding the feature set. Avoid
patching only the individual call sites: the underlying concern is that map,
application-state, and DOM-event state can become inconsistent.

### P1 — Session restore order loses saved state

- **Files:** `src/ui/SessionManager.js`, `src/jmars-map.js`
- `SessionManager.loadSession()` restores active layers and view *before*
  dispatching the body change. `JMARSMap.switchBody()` then clears/replaces
  those values with the destination body's default or cached state.
- **Required behavior:** Restore/switch the body first, then restore the
  session's active layers and view after the map has completed the body switch.
  A session saved for a non-current body must reopen with its own layers and
  viewport intact.

### P1 — Sessions save a stale viewport

- **Files:** `src/ui/SessionManager.js`, `src/jmars-map.js`, `src/jmars-state.js`
- Sessions serialize `jmarsState.state.view`, but normal Leaflet panning and
  zooming only update the URL; they do not synchronize `jmarsState.view`.
- **Required behavior:** Session output must contain the actual `map.getCenter()`
  and `map.getZoom()` values. Prefer a single, well-defined view-sync path so
  sessions, share URLs, and state agree.

### P1 — Cross-body bookmarks do not switch the map

- **File:** `src/features/bookmarks/BookmarksTool.js`
- `goTo()` only calls `jmarsState.set('body', ...)`. The map listens for
  `EVENTS.BODY_CHANGED`, so it remains on the old body and pans that map to the
  bookmark's coordinates.
- **Required behavior:** Use one body-switch API/event path, then set the
  destination view after the body transition. Add a regression test using a
  Mars-to-Moon bookmark.

### P1 — Do not insert bookmark or remote stamp data with `innerHTML`

- **Files:** `src/features/bookmarks/BookmarksTool.js`,
  `src/features/stamp/StampQueryPanel.js`
- User-controlled bookmark names and externally returned stamp product IDs are
  interpolated into HTML. This creates persistent and remote-data XSS paths.
- **Required behavior:** Build these elements with DOM APIs and assign dynamic
  values through `textContent` (and validated `dataset`/attributes where
  necessary). Add tests using markup-like strings.

### P2 — Canonicalize the active body across map, store, and UI

- **Files:** `src/jmars-map.js`, `src/jmars-state.js`, `src/ui/BodySelector.js`
- A deep link can initialize `JMARSMap.currentBody` from the URL without
  updating `jmarsState.body`, which defaults to `Mars`. UI components created
  later can therefore disagree with the displayed map.
- **Required behavior:** Use lowercase body keys (`mars`, `moon`, `earth`) as
  the single canonical value, and update map state, store state, and body-change
  event consumers through one operation.

### P2 — Either implement or remove layer `visible`

- **Files:** `src/jmars-state.js`, `src/ui/layer-manager.js`, query tools
- Layer state stores and serializes `visible`, and some query tools honor it,
  but the layer manager always renders active Leaflet layers and exposes no
  visibility control.
- **Required behavior:** Add a real visibility control and map application
  behavior, or remove the flag from serialized/state contracts. Rendering,
  exports, and data queries must agree on visibility.

### Verification expectations for this milestone

- Add browser/E2E coverage for session save/load across bodies, deep-link body
  initialization, and cross-body bookmark navigation.
- Add unit coverage for serialized current map views and for rejecting/rendering
  markup-like bookmark and stamp values safely.
- The project currently has a browser Mocha harness in `tests/index.html` but
  no `package.json` test/lint command. If introducing tooling, preserve the
  no-build-step browser architecture and document the repeatable verification
  command.

---

## 8. Mobile & PWA Follow-up Notes (2026-08-27)

JSMARS should remain a capable scientific map in a mobile browser while also
being installable as a Progressive Web App (PWA). This is an enhancement path,
not permission to replace the existing vanilla ES-module/no-build architecture.
Build the foundation first and progressively enhance; the normal web app must
remain usable when installation and service workers are unavailable.

### Delivery order

1. **Mobile interaction and responsive layout.** Make the existing map and
   tools practical on a narrow, touch-first viewport before adding offline
   caching. The map must remain the primary surface.
2. **Installability.** Add a web app manifest, app icons, appropriate mobile
   metadata, and a deliberate install affordance where the browser supports it.
3. **Offline shell.** Add a small service worker that caches the local
   application shell and immutable local assets. Do not cache third-party
   scientific imagery indiscriminately.
4. **Selective data/offline maps.** Only after quota, freshness, attribution,
   and user controls are designed, offer bounded opt-in caching for map data.

### Mobile UX requirements

- At widths around 768px and below, replace the persistent sidebar with a
  dismissible bottom sheet or off-canvas panel. It must not cover most of the
  map, and the user must be able to return to the map in one action.
- Keep primary map actions reachable by thumb: body selection, layer access,
  search, current view reset, and the currently active tool. Avoid shrinking
  desktop controls until they become difficult to tap.
- Ensure a minimum practical hit target of roughly 44×44 CSS px for map/tool
  controls, with visible focus indicators and keyboard access preserved.
- Avoid accidental map gestures: controls must stop propagation appropriately;
  touch dragging a sheet, scrolling tool content, and panning/zooming the map
  must not fight each other. Preserve Leaflet pinch-to-zoom and double-tap
  behavior where possible.
- Use responsive sizing (`dvh`/safe-area CSS where suitable) so the map is not
  hidden behind mobile browser bars or device cutouts. Respect
  `env(safe-area-inset-*)` for fixed controls.
- Move dense tables, charts, and tool configuration into full-height sheets or
  dedicated mobile views. Provide a clear close/back action and retain current
  map context when dismissed.
- Treat expensive features (3D terrain, hillshade, contours, large stamp
  searches, large exports) as opt-in on mobile. Show progress, provide cancel
  behavior where feasible, and choose safe mobile defaults for result limits
  and canvas/export dimensions.
- Test portrait and landscape Android/iOS browser layouts, a narrow 320px
  viewport, keyboard-open states, and installed standalone mode.

### PWA installability baseline

- Add a root-scoped `manifest.webmanifest` and reference it from every app page
  (`index.html`, `about.html`, and documentation pages if they are intended to
  open as app pages). Include a stable `id`, `name`, `short_name`, `start_url`,
  `scope`, `display: "standalone"`, `theme_color`, `background_color`, and PNG
  icons at 192×192 and 512×512. Include a `maskable` icon variant for Android.
- Add mobile browser metadata: theme color, an Apple touch icon, and Apple
  web-app title/status-bar settings as progressive enhancements. Do not claim
  that all browsers show the same installation UI.
- Register a service worker only in a secure context (HTTPS; localhost remains
  suitable for development), and surface registration/update failures in
  development without breaking normal map initialization.
- Add an in-app **Install JSMARS** action only after capturing
  `beforeinstallprompt`. Hide it if unavailable, after successful install, or
  in browsers that do not implement that event. On iOS, show concise
  browser-appropriate “Share → Add to Home Screen” guidance rather than a
  button that cannot work.
- Do not repeatedly interrupt users with an installation prompt. Offer it from
  a non-blocking menu/quick action after the application has demonstrated value.

### Service-worker and cache policy

- Version the cache name and precache only the owned application shell:
  HTML/CSS/JS modules, local JSON/data files, logos/icons, and other bounded
  assets required to reopen the UI. Use a cache-first strategy with an update
  path for these versioned assets.
- Never blindly precache WMS tiles, ODE/stamp responses, CDN scripts, uploads,
  or arbitrary remote imagery. They can be very large, change independently,
  have CORS/attribution restrictions, and quickly exhaust mobile storage.
- For a later optional “offline area” feature, make the user select a small
  extent, zoom range, body, layer, and size budget before download. Show the
  estimated/actual size, source attribution and timestamp, enable deletion,
  and evict with a documented LRU/age/byte policy.
- Favor network-first with a bounded cached fallback for WMS/API requests where
  policy permits; show stale/offline status with source timestamp. Never
  silently present cached scientific data as current observations.
- Respect failed CORS responses and opaque-response risks. Cache only responses
  that are explicitly safe and useful for offline use; do not turn a failed
  remote request into a permanently cached failure page.
- Add an update flow: when a new service worker is waiting, notify the user and
  provide a **Reload to update** action. Do not call `skipWaiting()` in a way
  that can silently interrupt an active drawing/session workflow.

### Architecture and quality guardrails

- Keep service-worker logic in a small root-level `sw.js`; use standard browser
  APIs and a hand-maintained/release-generated asset list rather than adding a
  build system solely for PWA support.
- Preserve session compatibility: the installed app, browser tab, shared URLs,
  and saved sessions must use the same body/layer/view contracts.
- Provide an offline indicator that distinguishes: offline app shell available,
  cached map data available, and live services unavailable.
- Keep access to the user’s locally saved bookmarks/sessions when offline, with
  graceful error feedback for features that necessarily require network access.
- Review CSP/CORS and third-party CDN dependencies before promising full
  offline operation. Long-term, consider hosting pinned runtime dependencies
  under project control if offline reliability becomes a goal.

### Required verification

- Validate the manifest and service worker in Chromium DevTools/Lighthouse and
  manually test install/uninstall, launch in standalone mode, update behavior,
  and offline reload after one successful online load.
- Test Android Chrome/Edge and iOS Safari (and at least one installed-web-app
  flow). Browser support and install UI differ by platform.
- Add automated coverage for manifest discovery, service-worker registration,
  shell fallback, install-action visibility rules, and mobile viewport layout
  regressions. Manual checks must cover map gestures and the sidebar/sheet
  interaction on touch devices.
- Document deployment requirements: HTTPS in production, correct manifest and
  service-worker MIME types, service-worker scope, cache invalidation/release
  procedure, supported browser behavior, and how users clear offline data.

---

## 9. Desktop-Parity Review Follow-up (2026-09-05)

A parity accounting against the JMARS desktop app identified remaining gaps
beyond the P1/P2 stabilization items in section 7. These are open backlog
items, not yet scheduled — pick them up after section 7's plumbing fixes are
verified. Do not start section 3.3/3.4 roadmap work until the state-plumbing
P1s are merged, since several of these tools (measurement, shapes, spectral
plots) will build on the same map/state contracts.

### A. Unimplemented roadmap items (see `docs/jsmars-roadmap.md` Phase 2–4, 9)

- **Unified top nav bar**: No persistent bar with zoom controls, a live
  lat/lon cursor readout, and a single unified search box (landmarks +
  bookmarks + coordinates). Currently search/zoom controls are scattered
  across the sidebar and Leaflet's default zoom control.
- **Custom shapes & geologic pattern library**: No canvas/SVG fill patterns
  for Ejecta, Crater Material, Fracture Zone, Plain, etc., and no style
  manager to apply them to drawn polygons (`src/features/shapes/`
  currently only wraps Leaflet.Draw for basic point/line/polygon geometry).
- **Independent measurement tools**: No azimuth, perimeter, or standalone
  distance/area tool decoupled from the profile/contour features. JMARS
  desktop treats measurement as its own tool, not a byproduct of drawing.
- **Investigate tool spectral plot**: `src/features/investigate/
  InvestigateTool.js` supports pixel/value inspection at a clicked point but
  has no spectral plot for multi-band data (see item F below — this is
  blocked on real per-pixel/multi-band raster access).
- **Landmark fuzzy search**: `src/features/search/SearchBar.js` and
  `src/ui/SearchBar.js` do partial matching but not true fuzzy/typo-tolerant
  search against the local landmark JSON (`src/data/landmarks.json`).
- **Configurable multi-CRS endpoints**: `src/jmars-config.js` hardcodes one
  WMS/XYZ endpoint set per body. There is no per-body/per-layer coordinate
  reference system override or alternate-endpoint fallback list.
- **Live in-doc tool sandboxes / API doc generation**: `docs/index.html`
  documents tools in prose but has no embedded interactive widgets (e.g. a
  live KRC/MCD/CSFD calculator) and there is no JSDoc-driven API reference
  generation for `src/` modules.

### B. Vector/GIS I/O gap

- `src/util/ShapeIO.js` supports GeoJSON and WKT round-trips only. There is
  no shapefile-grade export/import (`.shp`/`.shx`/`.dbf` triad, or even a
  zipped GeoJSON-to-shapefile conversion via a browser-side library), and no
  attribute-table editing UI for drawn features beyond name/style.

### C. Collaboration & output gap

- No multi-user/collaborative session support (sessions are single-user,
  client-side JSON only — see section 7 for even that being unreliable
  until the P1 fixes land).
- No print/plate composer (a JMARS desktop feature for producing publication
  map layouts with scale bar, north arrow, legend, and title block baked into
  an exportable image/PDF). `src/features/export/ExportTool.js` currently
  exports world files and raw canvas/PNG, not a composed layout.

### D. "Parity theater" — simulated vs. live science data (flagged by user as top priority)

Several flagship science tools present physically-based **client-side
models** in a way that can be mistaken for **live server/spacecraft data**.
This is the biggest trust risk for "no reason to use the desktop app," since
a domain scientist will notice immediately if a "radargram" or "atmospheric
profile" doesn't match the authoritative source.

- **KRC (`src/features/krc/KRCEngine.js`)**: A legitimate 1D thermal
  conduction physics model (analogous to Kieffer's KRC), always run
  client-side. It does not claim to be live data anywhere in the UI
  (`KRCPanel.js` labels it as a simulation), so this one is arguably fine as
  a *model*, not fake *data* — but confirm the UI language stays unambiguous
  as other panels are touched.
- **MCD atmospheric profiler (`src/features/mcd/MCDEngine.js`,
  `MCDPanel.js`)**: Already has an honest design — it offers an explicit
  `analytical` (offline physics model) vs `lmd_live` (real LMD/CNRS/ESA MCD
  v6.1 GCM, fetched via a public CORS proxy at `api.allorigins.win`) choice,
  labels the resulting `profile.source` string accordingly, and falls back
  to the analytical model with a labeled `(Offline Fallback)` suffix if the
  live fetch fails. Remaining risk: dependence on a third-party public CORS
  proxy (`allorigins.win`) for the "live" path is fragile (rate limits,
  uptime, and it is a privacy-sensitive relay of the user's query). Longer
  term, stand up a small first-party proxy/serverless function for the LMD
  MCD endpoint instead of relying on a public relay, and surface proxy
  health/latency in the UI.
- **Radar sounder (`src/features/radar/RadarSounderEngine.js`,
  `RadarPanel.js`)**: Fully synthetic. `RadarSounderEngine.PRESETS` are
  hand-authored plausible layer/dielectric values for four named regions
  (Planum Boreum, Planum Australe, Medusae Fossae, Utopia Planitia); there is
  no fetch of real SHARAD/MARSIS radargram data anywhere. The panel button
  already says "Synthesize Radargram" (reasonably honest), but nothing in the
  UI states these are illustrative preset parameters rather than measured
  reflectors. **Required follow-up**: either (a) clearly label the panel/
  results as "physically-based simulation using illustrative parameters, not
  observed radar returns," including a visible disclaimer and a link to the
  real PDS Geosciences Node SHARAD/MARSIS archives for the same regions, or
  (b) integrate real data — e.g. fetch actual SHARAD RGRAM browse
  products/quicklook images from the PDS Geosciences Node
  (`https://pds-geosciences.wustl.edu/missions/mro/sharad.htm`) or the
  MARSIS archive for the selected ground track and display them alongside
  (not instead of) the synthetic model, clearly attributed. Do not present
  (b) as a replacement for the physics model — both have value, but they
  must never be visually or textually conflated.
- **Band math / spectral tools (`src/features/bands/BandMathEngine.js`)**:
  Currently keys off single-band mosaic imagery approximations, not real
  per-pixel, multi-band raster or spectral-cube data (e.g. CRISM cubes).
  There is no image-cube ingestion path. Required follow-up before claiming
  spectral analysis parity: either source real per-band raster tiles (WMS
  band-selectable layers or COG/cube tiles) so band math operates on actual
  DN values, or explicitly relabel the feature as an educational/approximate
  mineral-index visualizer until real per-pixel data is wired in.

### General rule for any tool producing modeled/synthetic scientific output

When adding or touching a science tool, always make the data provenance
explicit and inspectable in the UI (a visible "Model" vs "Live/Measured"
badge or source string, as MCDPanel.js already does), never silently blend
synthetic and real data in the same visualization without a legend/label
distinguishing them, and prefer graceful, clearly-labeled fallback over a
tool that fails silently or misrepresents its output as authoritative.
