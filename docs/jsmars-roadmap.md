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

## Phase 2: Core UI & Map Interface (In Progress)
- [x] **Sidebar (Layer Manager)**:
    - [x] Collapsible panel on the left.
    - [ ] Drag-and-drop reordering (z-index).
    - [ ] Double-click for "Settings/Options" panel.
    - [x] **Fixed Overlays**: Toggles for Grid, Scalebar.
- [ ] **Navigation Bar**:
    - Top bar with Zoom controls, Lat/Lon readout.
    - Unified Search/Landmark tool.
- [x] **Map Tools**:
    - [x] **Panner View**: Overview map (MiniMap) linked to main viewport.
    - [x] **Lat/Lon Grid**: Graticule overlay with configurable spacing/colors.
- [x] **Scalebar**: Custom metric/imperial scalebar.
    - [x] **North Arrow**: Simple directional indicator.

## Phase 3: Science Tools (Feature Modules)
### 3.1 Crater Counting Tool (Foundation Implemented)
- [x] **Interaction**: "Ghost Circle" cursor, Scroll to resize, Click to place.
- [x] **Data**: Table view (ID, Lat, Lon, Diameter).
- [ ] **Export**: CSV and CraterStats format.

### 3.2 Radial Profile Viewer (Foundation Implemented)
- [x] **Algorithm**: Sample elevation along N radiating lines from a center point.
- [x] **Mock Data**: Use noise function or base map pixel intensity.
- [x] **UI**: Controls for Line Count, Angular Offset, Length.
- [x] **Visualization**: Multi-line chart.

### 3.3 Custom Shapes & Geologic Patterns
- [ ] **Pattern Library**: Canvas/SVG patterns for Ejecta, Crater Material, Fracture Zone, Plain.
- [ ] **Style Manager**: Apply patterns to polygon fills.
- [ ] **Drawing**: Points, Lines, Polygons, Circles, Rectangles.
- [ ] **Measurements**: Independent distance/area tools (Azimuth, Length, Perimeter).

### 3.4 Investigate Tool (New)
- [ ] **Pixel Inspector**: Click to query WMS/Numeric values at a point.
- [ ] **Spectral Plot**: If multi-band data is available, plot spectrum.

## Phase 4: Data Management & Search
- [ ] **Unified Search**:
    - **Landmarks**: Fuzzy search against local JSON database (mimics `places`).
    - **Bookmarks**: Save/Rename/Delete current viewport (localStorage).
- [ ] **Session Management**:
    - [ ] Save session (Active layers, order, opacity, panner, ROIs, bookmarks).
    - [ ] Load session (JSON file upload).
- [ ] **Multi-body Support**:
    - [ ] Body selector (Mars, Moon, Earth, etc.).
    - [ ] Configurable endpoints and coordinate systems.

## Phase 5: Advanced Layers & Tools (Reference Parity)
- [ ] **Stamp Layer**:
    - Query footprint databases (THEMIS, CTX, HiRISE).
    - Render outlines on map.
    - Click to load full-resolution image.
- [ ] **Ground Track**:
    - Visualize spacecraft orbits (MRO, ODY, MGS).
- [ ] **Time Awareness**: Time slider for temporal WMS layers.
- [ ] **3D Visualization**: WebGL exploration (`viz3d`).
- [ ] **Production Hardening**: Tile caching, debouncing, expanded testing.

## Documentation & Process
- [ ] **Release Notes**: Maintain `docs/release-notes.md`.
- [ ] **User Guide**: Maintain `docs/user-guide.md`.
