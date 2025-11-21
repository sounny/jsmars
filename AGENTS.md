# JMARS JS Port – Agent Briefing (Expanded)

This file explains what this project is about and how you should help.

---

## 1. Project Overview

- **Project name:** JMARS JS Port ("JSMARS")
- **Owner:** Dr. Moulay Anwar Sounny‑Slitine
- **Upstream project:** [JMARS](https://oss.mars.asu.edu/svn/jmars/) (Java Mars GIS viewer) from Arizona State University
- **Goal:** Build a JavaScript, browser‑based client inspired by JMARS that relies on the same or similar map services.
- **Audience:** Web users who need quick, reliable access to Mars map data without a desktop install.

JMARS is a large desktop GIS tool written in Java Swing. It talks to map servers (often WMS), manages many layers, supports shapes, and has 3D views. We want a clean WebGIS client that feels lighter but still supports JMARS data and workflows.

---

## 2. High‑Level Goals

The JS project should:

1. **Use JMARS (or similar) map services**
   - Use WMS or other HTTP APIs used by JMARS.
   - Fetch capabilities, list layers, and request map images.
   - Keep the service endpoints configurable so environments can swap servers.

2. **Provide a web map viewer**
   - Run fully in the browser (no native dependencies).
   - Show a Mars base map and allow pan/zoom/layer toggling.
   - Favor progressive enhancement and short load times.

3. **Mirror JMARS concepts where it helps**
   - Keep similar ideas to `MapServer`, `MapSource`, and "Layer".
   - Maintain a clean internal model for planetary bodies (start with Mars, expand later).
   - Reuse terminology so future contributors can cross‑reference the Java client.

4. **Stay license‑compatible**
   - JMARS is GPLv3; derivatives must stay GPLv3‑compatible.
   - Preserve license headers and link back to upstream data sources.

Short‑term focus: a simple Mars WMS viewer in JS that can grow into a fuller client.

---

## 3. Non‑Goals (for now)

The project will NOT try to match every JMARS feature at once. We will not target in the first phase:

- Full Swing UI feature parity.
- All layer types from the Java client.
- Full 3D visualization and JOGL‑style rendering.
- Local session persistence that mirrors the desktop client.
- Heavy bundling or complex build pipelines before the core works.

Those features can come later. The early work should focus on a strong, simple core that is easy to grow.

---

## 4. Architecture Plan

### 4.1. Overall shape

Target stack:

- **Client:** JavaScript ES modules
- **Map library:** Leaflet as a first step (can switch to OpenLayers or MapLibre later)
- **Data source:** WMS (or similar) map services used by JMARS
- **Build setup:** Start with "no build" (plain HTML + JS). Add a bundler later if needed.
- **UI approach:** Minimal, responsive layout with a strong focus on accessibility and keyboard support.

### 4.2. Planned folder layout

This is the desired layout. If the repo does not match this layout yet, move toward it.

```text
/AGENTS.md            # This file
/public/
  index.html          # Entry HTML page for the web client
/src/
  jmars-config.js     # Config: server URLs, default layer, body info
  jmars-wms.js        # WMS helper: capabilities + GetMap URL builder
  jmars-map.js        # Map view logic (Leaflet or other library)
  layers/
    index.js          # Layer model and layer registry
  util/
    geo.js            # Small geo helpers mirrored from JMARS Util
/docs/
  jsmars-roadmap.md   # Phased roadmap to grow the client
  transition-plan.md  # Deep dive on how to move from JMARS desktop concepts to JS
  LOG.md              # Running work log for agent tasks
```

### 4.3. Coding conventions

- Prefer modern ES modules and clear, descriptive names.
- Keep helpers pure and side‑effect free when possible.
- Avoid unnecessary dependencies; start with the browser and Leaflet.
- Never wrap imports in `try/catch` blocks.
- Keep configuration (service URLs, default layers) centralized in `jmars-config.js`.

### 4.4. Testing and validation

- Add lightweight unit tests as the codebase grows (e.g., Vitest or Jest once a bundler exists).
- For now, rely on manual checks in the browser and linting once a linter is configured.
- Document manual test steps in PR descriptions until automated tests exist.

### 4.5. Work practices for agents

- Read this file before making changes; update it when the ground rules evolve.
- When adding features, document the intent and usage in `/docs/`.
- Keep commits focused and well‑described; prefer smaller, reviewable changes.
- Include a short summary of manual testing in commit messages or PR notes.
- When touching map interactions, note any WMS endpoints used so they can be swapped easily.

---

## 5. Near‑term milestones

1. Create a minimal `/public/index.html` that loads a base map via Leaflet.
2. Implement `jmars-wms.js` to fetch WMS capabilities and build GetMap requests.
3. Establish a `layers/index.js` registry with at least one Mars base layer.
4. Add simple UI controls for layer toggling and coordinate readouts.
5. Capture feedback and iterate toward parity with core JMARS workflows.

---

If you create new folders, consider adding a short `README.md` to explain their purpose. Keep this document current as the project evolves.
