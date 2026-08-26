# Work Log

This log captures the reasoning and actions taken while expanding the agent guidance and sketching plans for turning JMARS into JSMARS.

## 2025-11-21 Session Notes
- Reviewed the existing agent brief (then named `AGNETS.md`) to understand project intent and scope.
- Renamed the briefing to `AGENTS.md` for clarity and expanded it with architecture details, coding conventions, and near-term milestones.
- Established a `/docs` directory to hold planning materials and ongoing logs.
- Authored the initial roadmap and transition plan to outline how the JS client should grow toward JMARS parity.
- Documented this session to keep future contributors aware of decisions and next steps.

## 2025-11-21 Session Notes (Update)
- Refactored layer management logic into a dedicated `src/ui/layer-manager.js` module.
- Added opacity sliders to each layer item in the UI.
- Implemented `setLayerOpacity` in `src/jmars-map.js` to handle Leaflet layer opacity updates.
- Verified functionality with new Playwright test `verification/verify_opacity.py`.

## 2025-11-22 Session Notes
- Moved the app entry point to the repository root (`/index.html`) so the homepage loads directly from the repo base.
- Reviewed the existing map/view code: layer toggling and opacity controls are in place, but there is no ordering, search, or error surface beyond console logs.
- Noted that configuration is still Mars-only and does not yet expose multiple planetary bodies or authenticated services.

### Missing JMARS features to queue up
- [x] Layer reordering and grouping to match JMARS' stack management and composite order controls.
- [x] ROI/shapes tooling (draw, edit, style presets) with import/export of JMARS ROI formats and GeoJSON.
- [x] Measurement utilities (distance, area, elevation sampling) that mirror JMARS analysis tools.
- [x] Saved sessions/workspaces so users can persist layer selections, map extents, and annotations between visits.
- [x] Enhanced search (places, features, layers) akin to JMARS' search windows, including jump-to coordinates.
- [x] Time-aware layers and profile plotting for instruments with temporal coverage.
- [x] Multi-body support with distinct projections and defaults (e.g., Mars, Moon) instead of Mars-only configs.
- [x] 3D/globe-style visualization pathway to approximate JMARS' 3D view modes.

## 2026-08-23 Session Notes (Phase 7 - Planetary Science & 3D Parity)
- Implemented **KRC Mars 1D Thermal Model** (`src/features/krc/`): Numerical heat diffusion solver, diurnal and seasonal temperature curve generation, depth temperature profile, map probe location mode, and CSV export.
- Implemented **Mars Time & Solar Longitude ($L_s$) Calendar** (`src/features/slider/`): Real-time conversion between Earth UTC, Mars Sol Date (MSD), Mars Year (MY), and $L_s$, with interactive scrubbing and playback engine.
- Implemented **3D Terrain & Globe Viewer** (`src/features/threed/`): WebGL 3D regional mesh with MOLA DEM elevation displacement, active solar lighting, orbit controls, and vertical exaggeration.
- Implemented **Mars Climate Database (MCD) Atmospheric Profiler** (`src/features/mcd/`): Vertical profiles of temperature, pressure, density, dust, and wind up to 50 km.
- Implemented **Crater Counting CSFD & Isochron Age Dating** (`src/features/crater-counting/`): Log-log cumulative size-frequency distribution ($N(>D)/\text{km}^2$) with Hartmann & Neukum isochrons ($10\text{ Ma} - 4.3\text{ Ga}$) and surface age estimation.
- Implemented **Spectral Band Math & Mineralogy** (`src/features/bands/`): Standard mineral indices (BD530, BD1900, BD1500, D2300, Olivine) with colormaps.
- Implemented **Map Projections & Viewpoints** (`src/features/projections/`): Equirectangular, North Polar (Planum Boreum), and South Polar (Planum Australe) views.
- Expanded automated unit test suite to 24 passing Mocha/Chai tests covering all science models.
- Set up standing recurring cron tasks (every 15 minutes for iterative development and every 4 hours for automated test verification and push).



### Automated 4-Hour Check Alert [2026-08-23 21:46:04 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should support composite blend mode in layer state updates
‣: AssertionError: expected [ { id: 'event_layer', …(2) }, …(1) ] to have a length of 1 but got 2
    at http://127.0.0.1:51945/tests/unit.js:45:36
    at http://127.0.0.1:51945/src/jmars-state.js:167:43
    at Array.forEach (<anonymous>)
    at JMARSState.emit (http://127.0.0.1:51945/src/jmars-state.js:167:29)
    at JMARSState.addLayer (http://127.0.0.1:51945/src/jmars-state.js:78:10)
    at n.<anonymous> (unit.js:283:20)


### Automated 4-Hour Check Alert [2026-08-23 23:45:22 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should map Solar Longitude (Ls) back to approximate Earth Date
?: AssertionError: expected 1895 to be within 2022..2024
    at n.<anonymous> (unit.js:430:45)
- should calculate surface mission sols for rovers and landers
?: AssertionError: expected 44796 to be within 0..2
    at n.<anonymous> (unit.js:438:34)


### Automated 4-Hour Check Alert [2026-08-23 23:45:42 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should map Solar Longitude (Ls) back to approximate Earth Date
?: AssertionError: expected 2021 to be within 2022..2024
    at n.<anonymous> (unit.js:430:45)

## 2026-08-24 Session Notes (4-Hour Checkpoint - All 54 Tests Passing & Green)
- **Subsurface Radar Sounding Engine (SHARAD/MARSIS)**: 1D A-scope power trace and 2D B-scope radargrams with dielectric permittivity modeling.
- **Planetary Lat/Lon Graticule Grid Layer**: Adaptive zoom spacing ($30^\circ \to 0.05^\circ$), major/minor subdivisions, and edge annotations.
- **IAU Planetary Nomenclature Gazetteer**: Comprehensive multi-body gazetteer for Mars and Moon across 9 morphological classes with instant search.
- **Planetary Scale Bar & North Arrow**: Planet-aware graphic scale calculation using exact planetary radii and latitude distortion.
- **Astrodynamics & Interplanetary Trajectories**: Hohmann transfer orbits, C3 launch energy, TMI/MOI Delta-V budgets, and synodic launch windows.
- **Color Stretch Image Processing**: Symmetric CSS filter builder and parser for raster enhancement.
- **Geodesic Navigation Suite**: Destination point calculation, cross-track distance, along-track distance, 3D Cartesian coordinates, and point-in-polygon containment.
- **Planetary Multi-Layer Probe**: Coordinate formatting, MOLA elevation, astronomical illumination, KRC thermal range, and MCD atmospheric pressure.
- **Verification Status**: 54/54 automated Mocha/Chai unit tests passing with zero errors. All changes committed and synced to GitHub `origin main`.


### Automated 4-Hour Check Alert [2026-08-24 05:30:32 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute Mars atmospheric speed of sound, viscosity, mean free path, and column mass
?: AssertionError: expected 0.000010291611372627857 to be close to 0.0000065 +/- 0.000001
    at n.<anonymous> (unit.js:192:30)


### Automated 4-Hour Check Alert [2026-08-24 05:45:36 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute Fresnel dielectric reflectivity, vertical resolution, and attenuation rate
?: AssertionError: expected 0.0032309799824388314 to be close to 0.00645 +/- 0.0005
    at n.<anonymous> (unit.js:449:29)

## 2026-08-24 Session Notes (4-Hour Checkpoint [06:00 UTC] - All 72 Tests Passing & Green)
- **Astrodynamics & Interplanetary Trajectories**: Hohmann transfer orbits, $C_3$ launch energy, TMI/MOI Delta-V budgets, synodic launch window opportunities, Vis-Viva satellite speed, escape velocity, and Areostationary synchronous orbit geometry.
- **Planetary Hypsometric Tinting & Colormap Engine**: Multi-stop colormaps (`mola_rainbow`, `viridis`, `magma`, `coolwarm`, `topographic`, `grayscale`), 256-step RGB LUTs, and array-to-RGBA direct buffer colorization.
- **Topographic Slope & Landing Site Hazard Analysis**: Central difference numerical gradient estimator for terrain slope ($\theta^\circ$), aspect facing directions ($\alpha^\circ$), and landing site safety categorization.
- **GIS Georeferencing & World File Engine**: 6-line affine transformation world file generator and parser (`.pgw` / `.jgw`) for QGIS, ArcGIS, and GDAL interoperability.
- **GIS Vector Well-Known Text (WKT) Support**: WKT geometry serialization and parser for Points, Linestrings, Polygons, and MultiPolygons.
- **Planetary Spatial Proximity Search & Navigation**: Nearest-feature gazetteer queries within distance radius $R\text{ km}$, forward destination point projection, cross-track / along-track distance, and geodesic path metrics.
- **Atmospheric Thermodynamics & Aerodynamics**: Local speed of sound in $CO_2$, dynamic viscosity via Sutherland's law, molecular mean free path, and atmospheric column mass.
- **Radar Sounding Geophysics**: Fresnel normal-incidence dielectric reflectivity, vertical range resolution, and one-way radar attenuation rate.
- **Verification Status**: 72/72 automated Mocha/Chai unit tests passing with zero errors. All changes committed and synced to GitHub `origin main`.



### Automated 4-Hour Check Alert [2026-08-24 07:15:58 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should classify complex central peak craters (7 to 100 km)
?: AssertionError: expected 'Complex' to equal 'Complex (Central Peak)'
    at n.<anonymous> (unit.js:379:33)


### Automated 4-Hour Check Alert [2026-08-24 08:24:30 UTC]
TEST FAILURE: Browser console errors encountered during UI interaction:
[error] WMS capabilities XML parse error: This page contains the following errors:error on line 5 at column 76: xmlParseEntityRef: no name
Below is a rendering of the page up to the first error.


### Automated 4-Hour Check Alert [2026-08-24 08:34:02 UTC]
TEST FAILURE: Browser console errors encountered during UI interaction:
[error] WMS capabilities XML parse error: This page contains the following errors:error on line 5 at column 76: xmlParseEntityRef: no name
Below is a rendering of the page up to the first error.


### Automated 4-Hour Check Alert [2026-08-24 09:00:35 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute minimum enclosing circle and export WKT
?: AssertionError: expected 'POLYGON ((0 0, 1 0, 1 1, 0 0))' to include 'POLYGON (((0 0, 1 0, 1 1, 0 0)))'
    at n.<anonymous> (unit.js:1128:28)


### Automated 4-Hour Check Alert [2026-08-24 10:00:33 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute Keplerian orbital period and velocity
?: AssertionError: expected 111.1 to be close to 111.9 +/- 0.5
    at n.<anonymous> (unit.js:1237:43)


### Automated 4-Hour Check Alert [2026-08-24 10:15:37 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute single-pixel Horn hillshade illumination and slope angle
?: AssertionError: expected 104 to be above 180
    at n.<anonymous> (unit.js:1271:36)


### Automated 4-Hour Check Alert [2026-08-24 12:45:26 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute seasonal sol durations and Mars Sol Date conversions
?: AssertionError: expected 44791.62022 to be close to 44796 +/- 1
    at n.<anonymous> (unit.js:1540:36)


### Automated 4-Hour Check Alert [2026-08-24 17:30:24 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate maximum radar signal penetration depth
?: AssertionError: expected 415.2 to be below 200
    at n.<anonymous> (unit.js:1664:35)


### Automated 4-Hour Check Alert [2026-08-24 18:30:25 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute solar elevation angle above surface horizon
?: AssertionError: expected 1.73 to be close to 0 +/- 0.01
    at n.<anonymous> (unit.js:1767:35)


### Automated 4-Hour Check Alert [2026-08-24 19:00:27 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute perpendicular cross-track error distance from great-circle track
?: AssertionError: expected -591.579 to be close to 591.59 +/- 2
    at n.<anonymous> (unit.js:1813:39)


### Automated 4-Hour Check Alert [2026-08-24 19:31:40 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute pressure-dependent thermal conductivity and CO2 sublimation temperature
?: AssertionError: expected 163 to be close to 147.8 +/- 1
    at n.<anonymous> (unit.js:1855:30)


### Automated 4-Hour Check Alert [2026-08-25 08:45:37 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate great-circle intersections and rhumb line distances
?: AssertionError: expected 180 to be close to 0 +/- 0.01
    at n.<anonymous> (unit.js:2172:33)


### Automated 4-Hour Check Alert [2026-08-25 09:15:29 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate convective PBL height and Deardorff velocity scale
?: AssertionError: expected 0.64 to be above 4
    at n.<anonymous> (unit.js:2209:39)


### Automated 4-Hour Check Alert [2026-08-25 09:30:31 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute exact sub-solar ground coordinates and seasonal declination
?: AssertionError: expected +0 to be above +0
    at n.<anonymous> (unit.js:2233:42)


### Automated 4-Hour Check Alert [2026-08-25 09:45:36 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute exact 8-neighbor Horn slope and compass aspect
?: AssertionError: expected 'W' to equal 'E'
    at n.<anonymous> (unit.js:2269:42)


### Automated 4-Hour Check Alert [2026-08-25 10:30:28 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate ionospheric dispersion delay and cumulative multi-layer TWT
?: AssertionError: expected 3.3607 to be close to 0.336 +/- 0.01
    at n.<anonymous> (unit.js:2358:42)


### Automated 4-Hour Check Alert [2026-08-25 11:00:32 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute upper convex hull continuum and Pearson spectral correlation
?: AssertionError: expected 0.25 to be above 0.25
    at n.<anonymous> (unit.js:2424:31)


### Automated 4-Hour Check Alert [2026-08-25 15:00:30 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute two-layer apparent thermal inertia and skin depth ratio
?: AssertionError: expected false to equal true
    at n.<anonymous> (unit.js:2818:48)


### Automated 4-Hour Check Alert [2026-08-25 16:00:42 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute Zevenbergen-Thorne profile and planform terrain curvature
?: AssertionError: expected +0 to not equal +0
    at n.<anonymous> (unit.js:2925:47)


### Automated 4-Hour Check Alert [2026-08-25 21:15:38 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute vertical subsurface range resolution and Doppler SAR sharpening
?: AssertionError: expected 14.99 to equal 15
    at n.<anonymous> (unit.js:3436:49)


### Automated 4-Hour Check Alert [2026-08-25 23:45:34 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute radar free-space spherical spreading path loss and wavelength
?: AssertionError: expected 106.43 to be above 120
    at n.<anonymous> (unit.js:3681:39)


### Automated 4-Hour Check Alert [2026-08-26 01:15:32 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Girard spherical excess polygon area and cross-track error distance
?: AssertionError: expected -591.579 to be close to 591.6 +/- 10
    at n.<anonymous> (unit.js:3833:44)


### Automated 4-Hour Check Alert [2026-08-26 01:30:37 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should invert thermal inertia from diurnal temperature amplitude and solve conductive heat flux
?: AssertionError: expected 628.7 to be below 350
    at n.<anonymous> (unit.js:3852:45)


### Automated 4-Hour Check Alert [2026-08-26 02:01:25 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute two-way signal attenuation rate and point-target radar equation received power
?: AssertionError: expected 2.9026e-20 to be above 1e-19
    at n.<anonymous> (unit.js:3894:46)


### Automated 4-Hour Check Alert [2026-08-26 03:15:35 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate H2O water ice saturation vapor pressure and saturation mixing ratio
?: AssertionError: expected 0.16295 to be below 0.01
    at n.<anonymous> (unit.js:4023:56)


### Automated 4-Hour Check Alert [2026-08-26 07:30:50 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate CRISM SINDEX2 polyhydrated sulfate and OLINDEX3 olivine indices
?: AssertionError: expected NaN to be above 0.15
    at n.<anonymous> (unit.js:3462:35)


### Automated 4-Hour Check Alert [2026-08-26 07:45:39 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should project forward Gnomonic perspective coordinates
?: AssertionError: expected undefined to equal true
    at n.<anonymous> (unit.js:3363:31)


### Automated 4-Hour Check Alert [2026-08-26 07:46:25 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should forward and inverse project coordinates under transverse Cassini-Soldner projection
?: AssertionError: expected 600.517 to be close to 591.95 +/- 2
    at n.<anonymous> (unit.js:4428:33)


### Automated 4-Hour Check Alert [2026-08-26 10:15:38 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute camera Ground Sampling Distance (GSD) and sensor FOV angles
?: AssertionError: expected 20.8333 to equal 0.25
    at n.<anonymous> (unit.js:4686:37)


### Automated 4-Hour Check Alert [2026-08-26 10:31:07 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate aerodynamic dust saltation threshold and scale height vertical gradient
?: AssertionError: expected 1.91 to be close to 0.866 +/- 0.05
    at n.<anonymous> (unit.js:4247:56)


### Automated 4-Hour Check Alert [2026-08-26 11:00:34 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate pyroxene band contrast metric and continuum curvature
?: AssertionError: expected undefined to be a number or a date
    at n.<anonymous> (unit.js:3717:38)


### Automated 4-Hour Check Alert [2026-08-26 16:00:58 UTC]
PUSH RESOLUTION: Pulled remote CNAME commit from custom domain configuration, successfully rebased and pushed 382 unit tests cleanly to origin main.




### Automated 4-Hour Check Alert [2026-08-26 17:15:53 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute crater scaling strength-to-gravity transition diameter and isochron cumulative offset
?: AssertionError: expected NaN to be close to 0.927 +/- 0.01
    at n.<anonymous> (unit.js:4512:50)


### Automated 4-Hour Check Alert [2026-08-26 17:30:35 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute wavelength-dependent dust optical depth and Brunt-V?is?l? stability
?: AssertionError: expected undefined to be true
    at n.<anonymous> (unit.js:2221:34)


### Automated 4-Hour Check Alert [2026-08-26 17:46:04 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should generate and parse 6-line GIS World File affine matrices symmetrically
?: ReferenceError: ExportTool is not defined
    at n.<anonymous> (unit.js:992:25)


### Automated 4-Hour Check Alert [2026-08-26 19:45:35 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate effective bolometric brightness temperature for sub-pixel shadowed mixtures
?: AssertionError: expected 258.99 to be close to 256.45 +/- 0.1
    at n.<anonymous> (unit.js:5220:44)


### Automated 4-Hour Check Alert [2026-08-26 20:31:47 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute subsolar coordinates and topocentric solar zenith irradiance
?: AssertionError: expected NaN to be close to 0 +/- 0.5
    at n.<anonymous> (unit.js:5320:48)


### Automated 4-Hour Check Alert [2026-08-26 20:33:06 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute subsolar coordinates and topocentric solar zenith irradiance
?: AssertionError: expected 129.62 to be close to 180 +/- 1
    at n.<anonymous> (unit.js:5326:49)


### Automated 4-Hour Check Alert [2026-08-26 21:00:57 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should compute exact intersection coordinates of two great circles
?: AssertionError: expected +0 to equal 180
    at n.<anonymous> (unit.js:5385:39)


### Automated 4-Hour Check Alert [2026-08-26 21:15:33 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should compute Mars atmospheric sound speed in CO2 gas
?: AssertionError: expected undefined to be a number
    at n.<anonymous> (unit.js:3772:40)
- should calculate CO2 dry ice frost point temperature from ambient atmospheric pressure
?: AssertionError: expected 95.96 to be close to 149.56 +/- 0.5
    at n.<anonymous> (unit.js:5395:45)
