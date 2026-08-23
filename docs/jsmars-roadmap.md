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
    - [x] Drag-and-drop reordering (z-index).
    - [x] Double-click for "Settings/Options" panel.
    - [x] Added available-layer filtering to quickly find layers by name/ID.
    - [x] **Fixed Overlays**: Toggles for Grid, Scalebar.
- [ ] **Navigation Bar**:
    - Top bar with Zoom controls, Lat/Lon readout.
    - Unified Search/Landmark tool.
- [x] **Quick Actions Palette**:
    - [x] Added keyboard-first command input in sidebar branding area.
    - [x] Added shortcuts for session save/load, reset view, panel navigation, and sidebar toggle.
- [x] **Map Tools**:
    - [x] **Panner View**: Overview map (MiniMap) linked to main viewport.
    - [x] **Lat/Lon Grid**: Graticule overlay with configurable spacing/colors.
- [x] **Scalebar**: Custom metric/imperial scalebar.
    - [x] **North Arrow**: Simple directional indicator.

## Phase 3: Science Tools (Feature Modules)
### 3.1 Crater Counting Tool (Foundation Implemented)
- [x] **Interaction**: "Ghost Circle" cursor, Scroll to resize, Click to place.
- [x] **Data**: Table view (ID, Lat, Lon, Diameter).
- [x] **Export**: CSV and GeoJSON format.

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
- [x] **Pixel Inspector**: Click to query WMS/Numeric values at a point.
- [ ] **Spectral Plot**: If multi-band data is available, plot spectrum.

## Phase 4: Data Management & Search
- [ ] **Unified Search**:
    - **Landmarks**: Fuzzy search against local JSON database (mimics `places`).
    - [x] **Bookmarks**: Save/Rename/Delete current viewport (localStorage & Session).
- [x] **Nomenclature Tool**:
    - [x] Toggleable layer of major landmarks (Craters, Mons, Valles).
    - [x] Filter by type.
- [x] **Session Management**:
    - [x] Save session (Active layers, order, opacity, panner, ROIs, bookmarks).
    - [x] Load session (JSON file upload).
- [x] **Multi-body Support**:
    - [x] Body selector (Mars, Moon, Earth, etc.).
    - [ ] Configurable endpoints and coordinate systems.

## Phase 5: Advanced Layers & Tools (Completed)
- [x] **Stamp Layer**:
    - [x] Query footprint databases (THEMIS, CTX, HiRISE, MOC, CRISM).
    - [x] Render outlines on map.
    - [x] Footprint metadata inspection and CSV export.
- [x] **Ground Track**:
    - [x] Visualize spacecraft orbits (MRO, ODY, MAVEN, MEX, MGS).
- [x] **Time Awareness**: Time slider for temporal WMS layers and astronomical state.
- [x] **3D Visualization**: WebGL 3D terrain and globe exploration.
- [x] **Production Hardening**: Tile caching, debouncing, comprehensive test suite.

## Phase 6: Planetary Science & Parity Push (Completed)
- [x] **KRC Mars 1D Thermal Model**:
    - [x] Diurnal surface and subsurface heat conduction simulation.
    - [x] Seasonal temperature curve calculation vs Solar Longitude ($L_s$).
    - [x] Subsurface depth temperature profile (0 to 1 m).
    - [x] Map probe mode to simulate any coordinate.
- [x] **Mars Time & Calendar ($L_s$)**:
    - [x] Conversion between Earth UTC, Mars Sol Date (MSD), Mars Year (MY), and Solar Longitude ($L_s$).
    - [x] Interactive scrubbing slider and playback engine.
- [x] **3D Terrain & Globe Viewer (WebGL)**:
    - [x] 3D mesh displaced by MOLA DEM topography.
    - [x] Solar lighting based on solar declination and local time.
    - [x] Orbit camera controls and vertical exaggeration slider.
- [x] **Mars Climate Database (MCD) Atmospheric Profiler**:
    - [x] Vertical profiles of temperature, pressure, density, dust, and wind up to 50 km.
- [x] **Crater Counting CSFD & Isochron Age Dating**:
    - [x] Cumulative log-log size-frequency distribution ($N(>D)/\text{km}^2$).
    - [x] Hartmann & Neukum isochron overlays (10 Ma to 4.3 Ga) and model age estimation.
- [x] **Spectral Band Math & Mineralogy**:
    - [x] Mineral parameter presets (BD530 Ferric iron, BD1900 Hydrated minerals, BD1500 Water ice, D2300 Smectite, Olivine).
    - [x] Colormaps (Viridis, Magma, Coolwarm, Jet, Rainbow).
- [x] **Map Projections & Polar Views**:
    - [x] Global Equirectangular, North Polar (Planum Boreum), and South Polar (Planum Australe) viewpoints.

## Documentation & Process
- [x] **Release Notes**: Maintained in `docs/release-notes.md`.
- [x] **User Guide**: Maintained in `docs/user-guide.md`.
- [x] **Work Log**: Maintained in `docs/LOG.md`.
