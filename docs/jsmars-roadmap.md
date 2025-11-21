# JSMARS Roadmap

A phased plan to grow the JMARS JS port from an empty shell into a featureful web client that mirrors key JMARS workflows.

## Phase 0: Foundations
- **Deliverables:** Project skeleton with `/public` entry point, `/src` modules, and documented coding conventions.
- **Map base:** Leaflet map centered on Mars with placeholder raster layer.
- **Config:** `jmars-config.js` holds WMS endpoints, projection info, and default view state.
- **Outcome:** Contributors can open `public/index.html` and see a working map container.

## Phase 1: WMS Integration
- **Capabilities:** Implement `jmars-wms.js` to fetch and cache WMS GetCapabilities.
- **Layer registry:** Define `layers/index.js` to normalize layer metadata and expose toggles.
- **Rendering:** Wire GetMap requests into Leaflet tile layers with dynamic parameter updates (bounding box, CRS, resolution).
- **Outcome:** Users can toggle at least one Mars base layer backed by live WMS responses.

## Phase 2: Interaction & UX
- **Controls:** Pan/zoom, scalebar, attribution, layer list, and opacity sliders.
- **Status:** Coordinate readout (lat/long), request status indicator, and error messaging for failed WMS calls.
- **Accessibility:** Keyboard navigation for essential controls and ARIA labels on form elements.
- **Outcome:** Core viewing experience feels polished and accessible.

## Phase 3: Vector & Annotation Support
- **Data inputs:** Import GeoJSON/KML where possible; mirror JMARS ROI/shapes semantics.
- **Editing:** Basic drawing (points, lines, polygons) with style presets matching JMARS defaults.
- **Persistence:** Temporary local storage for user shapes with hooks for future server sync.
- **Outcome:** Users can inspect WMS layers alongside lightweight vector overlays.

## Phase 4: Analysis & Extensibility
- **Measurements:** Distance/area tools, elevation sampling once DEM sources are available.
- **Search:** Layer and place search with saved queries.
- **Plugins:** Simple plugin interface so new tools can register UI panels and consume map state.
- **Outcome:** The app supports common JMARS analytical workflows and can grow without rewrites.

## Phase 5: Production Hardening
- **Performance:** Tile caching strategy, debounced requests, and lazy loading of optional tools.
- **Testing:** Automated unit tests plus integration smoke tests in CI; performance budgets.
- **Ops:** Bundle optimization (if/when we adopt a bundler), error logging, and user analytics hooks (opt-in).
- **Outcome:** Stable, observable application ready for broader use.

Use this roadmap to prioritize work and checkpoint progress. Update it as new requirements arrive.
