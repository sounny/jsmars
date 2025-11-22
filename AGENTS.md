# JMARS JS Port – Agent Briefing

This file explains the project goals, architecture, and rules for contributors (agents).

---

## 1. Project Overview
- **Goal:** Build a JavaScript, browser-based client inspired by JMARS.
- **Stack:** Vanilla JS (ES Modules), Leaflet, CSS. **No Build Step.**
- **License:** GPLv3 Compatible.

## 📚 Reference Material
The original Java-based JMARS application is located in the `jmars/` folder at the root of this repository.
- **Usage**: Use this to compare features, UI/UX flows, and data visualization styles.
- **Goal**: We aim for feature parity where possible, but adapted for modern web patterns.

> [!WARNING]
> **DO NOT MODIFY** the contents of the `jmars/` folder. It is strictly for reference purposes only. Any changes to the reference application will invalidate it as a source of truth.

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
