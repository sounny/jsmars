# Change Log

## [0.7.0] - 2026-08-24

### Added
- **Subsurface Radar Sounder (SHARAD / MARSIS)**: 1D A-scope power trace and 2D B-scope radargram simulations across Planum Boreum, Planum Australe, Medusae Fossae, and Utopia Planitia with dielectric permittivity and loss tangent tuning.
- **Planetary Lat/Lon Graticule Grid Layer**: Adaptive zoom spacing ($30^\circ \to 0.05^\circ$), major/minor subdivisions, edge coordinate annotations, and multi-format coordinate labeling ($0^\circ-360^\circ\text{ E}$, $\pm 180^\circ$, $0^\circ-360^\circ\text{ W}$).
- **Comprehensive IAU Planetary Nomenclature**: Multi-body gazetteer dataset for Mars and Moon across 9 morphological classes with instant search, category filtering, and rich popup info.
- **Planetary Graphic Scale Bar Control**: Accurate planet-aware scale calculation using exact planetary radii ($R_{\text{Mars}} = 3389.5\text{ km}, R_{\text{Moon}} = 1737.4\text{ km}$) and latitude cosine distortion.
- **Topographic Transect & Linked Cursor Synchronization**: Interactive elevation transect chart with bi-directional cursor linking driving a real-time crosshair marker on the map.
- **Spatial POI Bookmarks & Multi-Body Navigation**: Cross-body planetary bookmarks with pre-loaded scientific POIs and JSON import/export.
- **Test Suite & CI/CD**: Expanded to 44 automated Mocha/Chai unit tests with 100% passing verification.

## [0.6.0] - 2026-08-23

### Added
- **KRC 1D Mars Thermal Model**: 1D numerical heat conduction simulation, diurnal & seasonal temperature curves, depth profiles, CO2 frost detection, and CSV export.
- **Mars Astronomy & Time Slider**: Real-time Solar Longitude ($L_s$), Mars Sol Date (MSD), Mars Year (MY), subsolar coordinates, and time scrubbing slider.
- **3D Terrain & Globe Viewer**: Interactive WebGL 3D terrain displaced by MOLA DEM with solar lighting and vertical exaggeration.
- **Mars Climate Database (MCD) Profiler**: Vertical profiles of temperature, pressure, density, dust, and wind up to 50 km.
- **Crater Counting CSFD & Isochron Dating**: Cumulative log-log size-frequency distribution ($N(>D)/\text{km}^2$), Hartmann & Neukum isochron models (10 Ma to 4.3 Ga), and surface model age estimation.
- **Spectral Band Math & Mineralogy**: CRISM/THEMIS mineral parameter indices (BD530, BD1900, BD1500, D2300, Olivine) with colormap stretch.
- **Map Viewpoints & Projections**: Global Equirectangular, North Polar (Planum Boreum), and South Polar (Planum Australe) viewpoints.
- **Test Suite**: Expanded test suite to 23 automated Mocha unit tests.

## [0.5.0] - 2026-06-13

### Added
- **Stamp Layer**: USGS ODE REST API footprint search for THEMIS, CTX, HiRISE, MOC, CRISM with CSV export.
- **Advanced Shape Layer**: Draw Point, Line, Polygon, Circle, Rectangle with interactive attribute table and GeoJSON/CSV/KML import/export.
- **Landing Sites**: Interactive markers for Mars and Moon landing sites with mission metadata.
- **Ground Tracks**: Approximate spacecraft orbit visualization for MRO, Odyssey, MAVEN, MEX, and MGS.
- **Map Export**: PNG, JPEG, and georeferencing world file (.pgw/.jgw) export.
- **Places Manager**: Location bookmarks with coordinate string parsing.

## [0.4.1] - 2025-11-29

### Changed
- **Logging**: Cleaned up verbose debug logs from the previous release, moving them to `console.debug` to reduce console noise during normal operation.
- **Configuration**: Explicitly defined `defaultLayer` for Mars in `jmars-config.js` to ensure the Viking basemap loads reliably.

## [0.4.0] - 2025-11-29

### Added
- **Test Suite**: Added `tests/index.html` and `tests/unit.js` using Mocha/Chai to verify core logic (`JMARSState`, `JMARSWMS`).
- **Constants**: Created `src/constants.js` to centralize event names (`EVENTS.BODY_CHANGED`, etc.).

### Changed
- **Refactoring**: Updated all modules (`JMARSMap`, `JMARSState`, `LayerManager`, Tools) to use `EVENTS` constants instead of hardcoded strings.
- **Initialization**: Improved application bootstrap sequence. `JMARSMap` now correctly initializes the default body state and `LayerManager` syncs immediately, preventing blank maps on load.
- **Logging**: Reduced console noise in `JMARSVectors` by moving verbose logs to `console.debug`.
- **State Management**: Added `reset()` method to `JMARSState` for better testability.

### Fixed
- **Race Condition**: Resolved an issue where the Layer Manager might miss the initial layer configuration if initialized after the map event.