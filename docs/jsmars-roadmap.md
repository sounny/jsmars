# JSMARS Roadmap

This plan outlines how to grow the JSMARS project into a richer browser-based client that stays true to the features of the JMARS desktop program.

## Phase 0: Foundations (Completed)
- [x] **Project Skeleton**: Root `/index.html`, `/src` modules.
- [x] **Map Base**: Leaflet map centered on Mars.
- [x] **Config**: `jmars-config.js` for endpoints and defaults.

## Phase 1: WMS Integration (Completed)
- [x] **Capabilities**: `jmars-wms.js` to fetch/parse GetCapabilities.
- [x] **Layer Registry**: `layers/index.js` for metadata.
- [x] **Rendering**: Dynamic WMS tile layers.

## Phase 2: Interaction & UX (In Progress)
- [x] **Loading Indicator**: Visual feedback for network requests.
- [ ] **Layer Ordering & Grouping**:
    - [ ] Drag-and-drop reordering in the layer list.
    - [ ] "Active layer" concept for editing.
    - [ ] Fixed overlay layers (Grid, Scalebar, Nomenclature) with toggles.
- [ ] **Panner View**:
    - [ ] Overview map (MiniMap) linked to main viewport.
    - [ ] Toggle button in control panel.
- [ ] **Lat/Lon Grid**:
    - [ ] Graticule overlay (major/minor lines).
    - [ ] Configuration panel (spacing, colors, labels).
- [ ] **Scalebar Enhancements**:
    - [ ] Custom scalebar (Metric/Imperial, adjustable ticks).
    - [ ] Configuration panel.

## Phase 3: Vector & Annotation Support (Started)
- [x] **Basic Drawing**: Integrated `Leaflet.Draw` (Polygons, Lines, Markers).
- [ ] **ROI & Shape Editing**:
    - [ ] Real-time measurement readouts (distance, perimeter, area).
    - [ ] Import CSV and GeoJSON (client-side parsing).
    - [ ] Export to CSV and GeoJSON (configurable coordinate systems).
    - [ ] Feature table (ID, title, description) with selection syncing.
- [ ] **Measurements**:
    - [ ] Independent distance/area tools (Azimuth, Length, Perimeter).
    - [ ] Copy to clipboard / Export to CSV.

## Phase 4: Analysis & Data Management
- [ ] **Search & Bookmarks**:
    - [ ] Geocoder search box (NASA/USGS APIs).
    - [ ] Bookmarks panel (Save/Load to localStorage, Import/Export JSON).
- [ ] **Session Management**:
    - [ ] Save session (Active layers, order, opacity, panner, ROIs, bookmarks).
    - [ ] Load session (JSON file upload).
- [ ] **Multi-body Support**:
    - [ ] Body selector (Mars, Moon, Earth, etc.).
    - [ ] Configurable endpoints and coordinate systems (0-360 vs -180-180).

## Phase 5: Advanced Visualization & Production
- [ ] **Time Awareness**:
    - [ ] Time slider for temporal WMS layers.
    - [ ] Animation controls.
- [ ] **3D Visualization (Stretch)**:
    - [ ] Investigate CesiumJS or similar (if compatible with no-build).
- [ ] **Production Hardening**:
    - [ ] Performance optimization (Tile caching, debouncing).
    - [ ] Expanded testing (Playwright for UI, Unit tests for utils).

## Documentation & Process
- [ ] **Release Notes**: Maintain `docs/release-notes.md`.
- [ ] **User Guide**: Maintain `docs/user-guide.md`.
