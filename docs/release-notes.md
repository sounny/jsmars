# JSMARS Release Notes

## v0.10.0 - 2D | 3D Cesium Globe
**Date:** 2026-09-27

### 3D globe
- A **2D | 3D** control on the map switches between the existing Leaflet map and a full-size CesiumJS globe. CesiumJS 1.145.0 (Apache-2.0, © Cesium GS, Inc.) is loaded from jsDelivr only on the first switch to 3D. It is not part of the initial page and is not precached by the service worker.
- Mars uses `Ellipsoid.MARS`, the Moon uses `Ellipsoid.MOON`, Earth uses WGS84, and Europa uses the IAU radii already in JSMARS. The active WMS/XYZ basemap is draped on that ellipsoid. There is no Cesium ion token and no Earth imagery on other bodies.
- Entering 3D frames the current 2D center. Returning to 2D restores the 3D look point and a zoom matched to the camera height. Switching body while in 3D follows the map's body change.
- Drawing, measuring, crater counting, profiles, stamps, shapes, and other Leaflet tools are disabled in 3D, with a note in the Tools panel. The service worker does not cache the Cesium bundle.

### Honesty
- The removed sidebar viewer was a three.js globe with a procedural texture and a synthetic regional mesh. It did not sample MOLA or LOLA elevation. This release does not add elevation terrain either. The globe is the reference ellipsoid plus published imagery. Copy that said "3D MOLA terrain" now says imagery on the ellipsoid, with elevation terrain planned.
- 2D elevation profiles still query the USGS MOLA 128 ppd Cloud-Optimized GeoTIFF.

### Not carried over from the sidebar panel
- Procedural "photorealistic" textures (replaced by real WMS/XYZ imagery).
- Synthetic terrain mesh, vertical exaggeration, and wireframe (they were not a DEM).
- Decorative limb-glow atmosphere on Mars. Earth can still use Cesium's sky atmosphere. Other bodies do not get an Earth-blue atmosphere.
- A separate "focus the thumbnail on the 2D map" control. The 3D view is the map, and returning to 2D keeps the 3D camera position.

## v0.9.1 - Science Honesty & Docs Alignment
**Date:** 2026-09-20

### Trust / provenance
- **Radar sounder:** Visible **Model** badge, source string, and explicit disclaimer that results are a physically-based simulation with illustrative presets, not observed SHARAD/MARSIS/REASON radargrams. PDS Geosciences SHARAD and MARSIS archive links are in the panel. Chart title and CSV export use "synthetic" language.
- **Mineral index visualizer:** Relabeled from hyperspectral/CRISM band math to an educational / approximate overlay. **Model** badge states that overlays are not per-pixel CRISM/THEMIS cubes, with PDS CRISM/THEMIS archive links.
- **KRC / MCD:** Strengthened simulation vs live language. KRC remains an unambiguous client-side thermal **Model**. MCD keeps analytical vs live LMD GCM choice and now shows Model / Live GCM / Offline Fallback badges (no silent fallback).

### Docs
- README body list now includes Europa (Mars / Moon / Earth / Europa). Phase 9 matches the documentation-portal roadmap; Titan/Venus are noted as future work, not as an unchecked Europa expansion.

## v0.9.0 - Europa Planetary Body Integration
**Date:** 2026-09-13

### New Features
- **Europa Planetary Body**: Added Jupiter's icy moon Europa as a fully-supported planetary body with canonical key `'europa'`.
- **USGS Global Mosaic WMS Integration**: Live global controlled mosaic powered by USGS Astrogeology MapServer WMS (`GALILEO_VOYAGER` layer from Galileo SSI and Voyager missions).
- **Physical Geodesy Constants**: Added Europa parameters to `BODIES` ($R_{\text{mean}} = 1560.8\text{ km}$, $R_{\text{eq}} = 1564.13\text{ km}$, $R_{\text{pol}} = 1557.40\text{ km}$, flattening $0.0043$).
- **Dynamic Scale Bar & Measurement**: Scale bar and distance/area measurement tools automatically calibrate to Europa's radius ratio ($1560.8 / 6371 \approx 0.2450$).
- **Europa Surface Nomenclature & Landmarks**: Added major IAU named surface features including Conamara Chaos, Pwyll Crater, Cilix Crater, Tyre impact structure, Agenor Linea, Astypalaea Linea, Thera Macula, Thrace Macula, and Tara Corona.
- **Nomenclature Filtering**: Added support and visual category styling for icy world feature classes: *Linea* (tectonic bands), *Macula* (dark spots/chaos), and *Corona* (circular volcano-tectonic depressions).
- **Default Scientific Bookmarks**: Added preset points of interest for Conamara Chaos, Pwyll Crater, Agenor Linea, Thera Macula, and Tyre structure.

## v0.8.1 - Stabilization Milestone
**Date:** 2026-09-05

### Fixes
- Fixed session restore sequencing so saved body switches complete before saved layers and viewport are restored.
- Fixed stale session/share view serialization by consolidating live map view sync into one JMARSMap state/URL path.
- Fixed cross-body bookmark navigation so Moon/Earth/Mars bookmarks switch the active body before panning.
- Removed unsafe dynamic HTML insertion for bookmark and stamp result content by using DOM APIs/textContent.
- Canonicalized active body keys to lowercase across map initialization, state, deep links, and late-created UI consumers.
- Added enforceable layer visibility state with explicit layer-manager visibility checkboxes and map rendering parity.

## v0.8.0 - Planetary Geodesy, Astrodynamics & Cartography Milestone
**Date:** 2026-08-24

### New Features

#### Astrodynamics & Interplanetary Trajectories (`TrajectoryEngine.js`, `TrajectoryPanel.js`)
- Heliocentric Hohmann transfer orbit solver between Earth, Mars, Venus, and Jupiter.
- Computes Trans-Mars Injection ($\Delta v_1$), Mars Orbit Insertion ($\Delta v_2$), $C_3$ launch energy, departure phase angle, and flight duration in days/months.
- Upcoming Earth-Mars synodic launch window schedule generator and mission plan CSV export.

#### Planetary Hypsometric Tinting Engine (`ColorRampEngine.js`)
- Multi-stop scientific colormap generator (`mola_rainbow`, `viridis`, `magma`, `coolwarm`, `topographic`, `grayscale`).
- 256-step RGB Lookup Table (LUT) compilation and direct numerical array-to-RGBA image buffer colorization for real-time elevation and thermal rendering.

#### Topographic Slope & Landing Site Hazard Analysis (`ContourLayer.js`)
- Central difference numerical gradient estimator calculating terrain slope angles ($\theta^\circ$) and cardinal aspect facing directions ($\alpha^\circ$).
- Automated spatial safety categorization for lander/rover site certification ($< 5^\circ$ Safe, $5^\circ-15^\circ$ Moderate, $15^\circ-25^\circ$ Steep, $> 25^\circ$ Critical / Escarpment).

#### GIS Georeferencing World File Engine (`ExportTool.js`)
- Standard 6-line affine transformation world file generator and parser (`.pgw` / `.jgw`) enabling direct export of map views into QGIS, ArcGIS, GDAL, and WebGIS workflows.

#### GIS Vector Well-Known Text (WKT) Support (`ShapeIO.js`)
- Full WKT geometry serialization (`toWKT`) and parser (`parseWKT`) for Points, Linestrings, Polygons, and MultiPolygons.

#### Spatial Proximity Search & Planetary Geodesy (`geo.js`, `PlacesManager.js`)
- Nearest-feature spatial proximity search querying planetary landmarks within distance radius $R\text{ km}$.
- Forward destination point calculation (`computeDestinationPoint`) and analytical cross-track / along-track distance solvers.
- Multi-segment geodesic polyline length and closed polygon perimeter estimation.

---

## v0.7.0 - Subsurface Geophysics & Planetary Graticules
**Date:** 2026-08-24

### New Features
- **Subsurface Radar Sounder (SHARAD/MARSIS)**: 1D A-scope power trace and 2D B-scope radargram simulations across Planum Boreum, Planum Australe, Medusae Fossae, and Utopia Planitia with dielectric permittivity and loss tangent tuning.
- **Planetary Lat/Lon Graticule Grid Layer**: Adaptive zoom spacing ($30^\circ \to 0.05^\circ$), major/minor subdivisions, edge coordinate annotations, and multi-format coordinate labeling ($0^\circ-360^\circ\text{ E}$, $\pm 180^\circ$, $0^\circ-360^\circ\text{ W}$).
- **Comprehensive IAU Planetary Nomenclature**: Multi-body gazetteer dataset for Mars and Moon across 9 morphological classes with instant search.
- **Planetary Graphic Scale Bar Control**: Accurate planet-aware scale calculation using exact planetary radii ($R_{\text{Mars}} = 3389.5\text{ km}, R_{\text{Moon}} = 1737.4\text{ km}$) and latitude cosine distortion.
- **Topographic Transect & Linked Cursor Synchronization**: Interactive elevation transect chart with bi-directional cursor linking driving a real-time crosshair marker on the map.

---

## v0.6.0 - Planetary Science & 3D Parity Push
**Date:** 2026-08-23

### New Features

#### KRC Mars 1D Thermal Model
- Client-side 1D heat conduction finite difference solver based on Hugh Kieffer's KRC model.
- Diurnal surface and subsurface temperature curves ($T(t)$ from 0 to 24h LTST).
- Subsurface depth temperature profile ($T(z)$ from 0 to 1 m, 24 layers).
- Seasonal orbital temperature curves ($T(L_s)$ from 0° to 360°).
- Interactive parameter controls: Thermal Inertia, Albedo, Dust Optical Depth ($\tau$), Elevation, Latitude.
- Map click probe to simulate any Martian coordinate.
- Multi-mode canvas chart and CSV export.

#### Mars Time & Solar Longitude ($L_s$) Calendar Slider
- Real-time astronomical calculations: Earth UTC <-> Mars Sol Date (MSD) <-> Mars Year (MY) <-> Solar Longitude ($L_s$) <-> Subsolar point.
- Interactive scrubbing slider and playback engine (1x, 5x, 20x).
- Global event synchronization (`jmars:time-changed`) driving thermal, atmospheric, and 3D lighting models.

#### 3D Terrain & Globe Viewer (WebGL)
- Correction (2026-09-27): the mesh was procedural, not MOLA elevation. See v0.10.0.
- Real-time 3D planetary mesh displaced by MOLA DEM topography.
- Solar illumination angle computed from active $L_s$ and local solar time.
- Orbit camera controls (drag to rotate, wheel to zoom).
- Vertical exaggeration slider (1x to 20x) and wireframe mode.

#### Mars Climate Database (MCD) Atmospheric Profiler
- Vertical atmospheric structure from surface to 50 km altitude.
- Temperature $T(z)$, Pressure $P(z)$ (log scale), Density $\rho(z)$, and horizontal wind speed $u(z), v(z)$.
- Scale height and surface boundary conditions calculation.
- Interactive canvas profile chart and CSV export.

#### Crater Counting CSFD & Isochron Dating
- Real-time Cumulative Size-Frequency Distribution ($N(>D)/\text{km}^2$) log-log chart.
- Hartmann & Neukum isochron models (10 Ma to 4.3 Ga) and geological epoch boundaries (Noachian, Hesperian, Amazonian).
- Surface model age estimation.

#### Spectral Band Math & Mineralogy Indices
- Standard Martian remote sensing mineral indices (BD530 Ferric iron, BD1900 Hydrated minerals, BD1500 Water ice, D2300 Smectite, THEMIS Olivine ratio).
- Custom formula parser and colormaps (Viridis, Magma, Coolwarm, Jet, Rainbow, Grayscale).

#### Map Viewpoints & Polar Projections
- Switch between Global Equirectangular, North Polar (Planum Boreum), and South Polar (Planum Australe) views.
- Planetocentric vs Planetographic latitude and East 360° / East 180° / West 360° longitude formatting.

---

## v0.5.0 - Feature Parity Push (P1/P2)
**Date:** 2026-06-13

### New Features

#### Stamp Layer (Footprint Browser)
- Query USGS ODE REST API for instrument footprints (THEMIS, CTX, HiRISE, MOC, CRISM)
- Interactive footprint polygons on map with hover/click behavior
- Results table with product ID, coordinates, solar longitude
- Export search results as CSV

#### Advanced Shape Layer
- Full drawing palette: Point, Line, Polygon, Circle, Rectangle
- Interactive attribute table with inline editing
- Measurements computed per shape (distance for lines, area for polygons)
- Import: GeoJSON, CSV, KML file formats
- Export: GeoJSON, CSV, KML file formats
- Drag-and-drop file import onto map
- Enhanced style editor with color presets, dash patterns, stroke/fill opacity

#### Landing Sites
- Interactive markers for Mars rovers/landers (Viking through Perseverance)
- Moon landing sites (Apollo, Chang'e, Chandrayaan, SLIM)
- Agency-colored markers with mission info popups

#### Ground Track Visualization
- Approximate spacecraft ground tracks (MRO, Odyssey, MAVEN, Mars Express, MGS)
- Configurable visibility per spacecraft
- Color-coded tracks with orbit labels

#### Map Export
- Export current view as PNG or JPEG (via html2canvas CDN)
- Export georeferencing world file (.pgw/.jgw) for GIS software

#### Places Manager
- Save/recall map locations
- Coordinate string parsing (lat, lon entry)
- Local storage persistence
- Import/export places as JSON

### Improvements

#### Multi-Body WMS Discovery
- WMS GetCapabilities now runs for Moon and Earth (not just Mars)
- Added USGS Moon WMS endpoint
- Body switching triggers WMS rediscovery

#### Enhanced Geo Utilities
- Mars/Moon/Earth body constants (radii, flattening)
- Haversine distance with body-specific radii
- Azimuth (initial bearing) calculation
- DMS coordinate formatting
- Spherical polygon area computation
- Great circle interpolation
- Planetocentric/planetographic latitude conversion

#### Core Architecture
- New EventBus module for centralized event dispatch
- New ToolManager for mutual-exclusion tool activation
- New PluginRegistry for feature registration
- Expanded event constants for all features

#### Documentation
- New ARCHITECTURE.md with project structure and patterns
- Updated release notes

---

## v0.4.6 - Layer Discovery Enhancements
**Date:** 2026-03-14

### Improvements
- Added a filter box to the Layer Manager "Available Layers" section to quickly narrow large layer catalogs by layer name or ID.
- Added an available-layer count indicator that shows filtered results versus total discoverable layers.

---

## v0.4.5 - Quick Actions & Workflow Boost
**Date:** 2026-03-14

### Improvements
- Added a new Quick Command palette in the sidebar for fast actions (save/load session, reset view, open panels, toggle sidebar).
- Added keyboard shortcut support (`Ctrl+K` / `⌘+K`) to focus the command input for faster navigation.
- Added command suggestions and inline status feedback behavior to improve accessibility and usability.

---

## v0.4.4 - Layer Manager Accessibility
**Date:** 2026-03-14

### Improvements
- Added a dedicated layer settings (⚙) button to each active layer card in the Layer Manager.
- Enabled keyboard access to open layer settings from focused layer cards using Enter/Space.
- Improved modal accessibility by wiring `aria-labelledby` and returning focus to the previously focused element when closing settings.

---

## v0.4.3 - Layer Settings Panel
**Date:** 2025-12-13

### Improvements
- Added a double-click layer settings panel with metadata, opacity control, and quick removal.

---

## v0.4.2 - MOLA Profiles Fixes
**Date:** 2025-12-06

### Improvements
- Switched profile tools to sample the USGS MOLA 128ppd DEM directly, replacing the hillshade-based queries.
- Stabilized profile chart rendering when samples contain gaps or null elevation values.
- Wired the linear profile control and synchronized profile tool buttons to stop/start cleanly.

---

## v0.4.1 - Polish
**Date:** 2025-11-29

### Improvements
- **Logging**: Reduced console noise by moving verbose logs to debug level.
- **Configuration**: Hardened default layer selection logic.

---

## v0.4.0 - Code Quality & Stability
**Date:** 2025-11-29

### Improvements
- **Architecture**: Centralized event handling using constants to improve maintainability.
- **Testing**: Added a browser-based unit test suite (`tests/index.html`) covering core state and WMS logic.
- **Performance**: Optimized application startup sequence to ensure reliable layer loading.

---

## v0.3.2 - Multi-body & Bookmarks
**Date:** 2025-11-28

### New Features
- **Multi-body Support:** Switch the entire map context between Mars, Earth, and Moon using the new dropdown in the header.
- **Bookmarks Tool:** Save your favorite map views (location and zoom) to quickly navigate back to them. Bookmarks are saved to your browser's local storage and included in Session files.

---

## v0.3.1 - Science Tools Expansion
**Date:** 2025-11-28

### New Features
- **Nomenclature Tool:** View and filter labels for major Martian landmarks (Craters, Montes, Valles) directly on the map.
- **Investigate Tool:** Click any point on the map to inspect coordinates and query underlying WMS layers for data values (via `GetFeatureInfo`).

---

## v0.3.0 - Session Management & Usability
**Date:** 2025-11-28

### New Features
- **Session Management:** Save your entire workspace (Active Layers, Craters, Measurements, View) to a `.json` file and load it back later to resume work.
- **Layer Reordering:** Drag and drop layers in the Layer Manager to change their draw order.
- **Enhanced Exports:** Added GeoJSON export support for Crater Counting and Measurement tools.

### Improvements
- Added "Session" section to the sidebar.
- Improved data handling for tools to support state persistence.

---

## v0.2.1 - Navigation Overlay
**Date:** 2025-11-22

### New Features
- **North Arrow Control:** Added a compass-inspired control with a one-click "Reset view" action and a sidebar toggle.

---

## v0.2.0 - Vector Support & UI Polish
**Date:** 2025-11-21

### New Features
- **Loading Indicator:** A visual "Loading map data..." overlay appears during tile fetching.
- **Vector Drawing:** Added `Leaflet.Draw` toolbar. Users can now draw Polygons, Rectangles, Lines, and Markers.
- **Architecture:** Integrated `JMARSVectors` module.

### Fixes
- Improved error handling for WMS capabilities fetching.

---

## v0.1.0 - Foundations
**Date:** 2025-11-21

### Initial Release
- Basic Map View (Leaflet).
- WMS Layer Support (USGS Mars).
- Layer Manager (Toggle, Opacity).
- Coordinate Readout.
