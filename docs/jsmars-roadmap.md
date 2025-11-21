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

## Phase 2: Core UI & Map Interface (Next)
- [ ] **Sidebar (Layer Manager)**:
    - Collapsible panel on the left.
    - Drag-and-drop reordering (z-index).
    - Double-click for "Settings/Options" panel.
    - **Fixed Overlays**: Toggles for Grid, Scalebar, Nomenclature.
- [ ] **Navigation Bar**:
    - Top bar with Zoom controls, Lat/Lon readout.
    - Unified Search/Landmark tool.
- [ ] **Map Tools**:
    - **Panner View**: Overview map (MiniMap) linked to main viewport.
    - **Lat/Lon Grid**: Graticule overlay with configurable spacing/colors.
    - **Scalebar**: Custom metric/imperial scalebar.

## Phase 3: Science Tools (Feature Modules)
### 3.1 Crater Counting Tool
- [ ] **Interaction**: "Ghost Circle" cursor, Scroll to resize, Click to place.
- [ ] **Data**: Table view (ID, Lat, Lon, Diameter, Color).
- [ ] **Export**: CSV and CraterStats format.

### 3.2 Radial Profile Viewer
- [ ] **Algorithm**: Sample elevation along N radiating lines from a center point.
- [ ] **Mock Data**: Use noise function or base map pixel intensity (since no DEM API).
- [ ] **UI**: Controls for Line Count, Angular Offset, Length (Relative/Absolute).
- [ ] **Visualization**: Multi-line chart (Distance vs Elevation).

### 3.3 Custom Shapes & Geologic Patterns
- [ ] **Pattern Library**: Canvas/SVG patterns for Ejecta, Crater Material, Fracture Zone, Plain.
- [ ] **Style Manager**: Apply patterns to polygon fills.
- [ ] **Drawing**: Points, Lines, Polygons, Circles, Rectangles.
- [ ] **Measurements**: Independent distance/area tools (Azimuth, Length, Perimeter).

## Phase 4: Data Management & Search
- [ ] **Unified Search**:
    - **Landmarks**: Fuzzy search against local JSON database.
    - **Bookmarks**: Save/Rename/Delete current viewport (localStorage).
- [ ] **Session Management**:
    - [ ] Save session (Active layers, order, opacity, panner, ROIs, bookmarks).
    - [ ] Load session (JSON file upload).
- [ ] **Multi-body Support**:
    - [ ] Body selector (Mars, Moon, Earth, etc.).
    - [ ] Configurable endpoints and coordinate systems.

## Phase 5: Advanced & Production
- [ ] **Time Awareness**: Time slider for temporal WMS layers.
- [ ] **3D Visualization**: WebGL exploration.
- [ ] **Production Hardening**: Tile caching, debouncing, expanded testing.

## Documentation & Process
- [ ] **Release Notes**: Maintain `docs/release-notes.md`.
- [ ] **User Guide**: Maintain `docs/user-guide.md`.
