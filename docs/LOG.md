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


### Automated 4-Hour Check Alert [2026-08-26 21:45:39 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate ionospheric dispersion delay and cumulative multi-layer TWT
?: AssertionError: expected undefined to be a number
    at n.<anonymous> (unit.js:2359:42)


### Automated 4-Hour Check Alert [2026-08-26 22:00:39 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate temperature-dependent specific heat capacity of silicate regolith
?: AssertionError: expected 579 to be close to 771.38 +/- 0.5
    at n.<anonymous> (unit.js:4574:44)


### Automated 4-Hour Check Alert [2026-08-28 18:30:54 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Venus inward transfer trajectory, gravity assist deflection, and heliocentric boost
?: AssertionError: expected 3.372 to be close to 2.723 +/- 0.3
    at n.<anonymous> (unit.js:11882:59)


### Automated 4-Hour Check Alert [2026-08-28 18:31:11 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Venus inward transfer trajectory, gravity assist deflection, and heliocentric boost
?: AssertionError: expected 5.763 to be close to 7.019 +/- 0.3
    at n.<anonymous> (unit.js:11883:57)


### Automated 4-Hour Check Alert [2026-08-28 18:46:02 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should calculate Mars Gravity Assist (MGA) turning angle, velocity boost, and aphelion pumping
?: AssertionError: expected 2.23 to be above 4.5
    at n.<anonymous> (unit.js:11935:47)
- should discriminate Crystalline Gray Hematite, Oxyhydroxide Goethite, and Red Dust in CRISM spectra
?: AssertionError: expected false to be true
    at n.<anonymous> (unit.js:11968:47)


### Automated 4-Hour Check Alert [2026-08-28 19:00:49 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Jupiter Interplanetary Cycler orbit resonance, TCI Delta-V, and encounter excesses
?: AssertionError: expected 5.883 to be close to 6.44 +/- 0.3
    at n.<anonymous> (unit.js:11986:51)


### Automated 4-Hour Check Alert [2026-08-28 19:15:51 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Mercury inward interplanetary transfer trajectory and insertion Delta-V
?: AssertionError: expected 6.6 to be close to 3.91 +/- 0.3
    at n.<anonymous> (unit.js:12030:59)


### Automated 4-Hour Check Alert [2026-08-28 19:16:11 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Mercury inward interplanetary transfer trajectory and insertion Delta-V
?: AssertionError: expected 12.584 to be close to 25.411 +/- 1
    at n.<anonymous> (unit.js:12031:57)


### Automated 4-Hour Check Alert [2026-08-28 19:31:10 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Mercury multi-gravity assist trajectory (M-V-M) and reduced MOI Delta-V
?: AssertionError: expected 293 to be close to 323.2 +/- 3
    at n.<anonymous> (unit.js:12081:49)


### Automated 4-Hour Check Alert [2026-08-28 19:31:27 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Mercury multi-gravity assist trajectory (M-V-M) and reduced MOI Delta-V
?: AssertionError: expected 6.769 to be close to 8.529 +/- 0.5
    at n.<anonymous> (unit.js:12085:51)


### Automated 4-Hour Check Alert [2026-08-28 19:46:07 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate volcanic lava tube subsurface thermal attenuation and cave microclimate stability
?: AssertionError: expected 'Isothermal Cave Interior (Ultra-Stabl?' to include 'Thermally Buffered Subsurface Cavity'
    at n.<anonymous> (unit.js:12145:47)


### Automated 4-Hour Check Alert [2026-08-28 20:30:49 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Pluto / KBO deep space interplanetary transfer trajectory and flyby mechanics
?: AssertionError: expected 46.42 to be close to 45.61 +/- 0.5
    at n.<anonymous> (unit.js:12285:47)


### Automated 4-Hour Check Alert [2026-08-28 21:45:47 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate inward Mars-to-Venus gravity assist flyby, deflection angle, and resonant orbit pumping
?: AssertionError: expected 217.5 to be close to 174.5 +/- 10
    at n.<anonymous> (unit.js:12530:47)


### Automated 4-Hour Check Alert [2026-08-28 21:46:01 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate inward Mars-to-Venus gravity assist flyby, deflection angle, and resonant orbit pumping
?: AssertionError: expected 0.511 to be close to 0.455 +/- 0.05
    at n.<anonymous> (unit.js:12534:52)


### Automated 4-Hour Check Alert [2026-08-28 22:01:02 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should discriminate anhydrous Chloride / Halite salt flats in Terra Sirenum using VNIR slope and THEMIS DCS
?: AssertionError: expected undefined to be true
    at n.<anonymous> (unit.js:9258:51)
- should calculate continuous radial low-thrust propulsion perturbation, effective gravity, and apsidal shift
?: AssertionError: expected 14.99 to be close to 28.19 +/- 2
    at n.<anonymous> (unit.js:12580:58)


### Automated 4-Hour Check Alert [2026-08-28 22:15:50 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate direct high-energy Hohmann transfer from Mars to innermost planet Mercury
?: AssertionError: expected 6.6 to be close to 5.113 +/- 0.3
    at n.<anonymous> (unit.js:12623:53)


### Automated 4-Hour Check Alert [2026-08-28 22:16:05 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate direct high-energy Hohmann transfer from Mars to innermost planet Mercury
?: AssertionError: expected 12.584 to be close to 13.914 +/- 0.5
    at n.<anonymous> (unit.js:12624:54)


### Automated 4-Hour Check Alert [2026-08-28 23:01:00 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate 3-burn bi-elliptic transfer from Mars out to high asteroid aphelion and plunge to Venus
?: AssertionError: expected 3.472 to be close to 3.793 +/- 0.2
    at n.<anonymous> (unit.js:12765:57)


### Automated 4-Hour Check Alert [2026-08-28 23:01:15 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate 3-burn bi-elliptic transfer from Mars out to high asteroid aphelion and plunge to Venus
?: AssertionError: expected 2.82 to be close to 3.854 +/- 0.2
    at n.<anonymous> (unit.js:12766:56)


### Automated 4-Hour Check Alert [2026-08-28 23:01:27 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate 3-burn bi-elliptic transfer from Mars out to high asteroid aphelion and plunge to Venus
?: AssertionError: expected 7.468 to be close to 4.488 +/- 0.3
    at n.<anonymous> (unit.js:12767:56)


### Automated 4-Hour Check Alert [2026-08-28 23:30:54 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should discriminate Low-T Serpentine (Lizardite) from Antigorite and Talc in CRISM spectra
?: AssertionError: expected undefined to be true
    at n.<anonymous> (unit.js:11282:52)
- should calculate interplanetary Hohmann transfer from Mars to gas giant Jupiter and orbit insertion
?: AssertionError: expected 4.197 to be close to 5.01 +/- 0.2
    at n.<anonymous> (unit.js:12864:54)


### Automated 4-Hour Check Alert [2026-08-28 23:31:22 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary Hohmann transfer from Mars to gas giant Jupiter and orbit insertion
?: AssertionError: expected 4.269 to be close to 5.642 +/- 0.2
    at n.<anonymous> (unit.js:12865:55)


### Automated 4-Hour Check Alert [2026-08-29 00:00:44 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary Hohmann transfer from Mars to ringed planet Saturn and orbit insertion
?: AssertionError: expected 5.564 to be close to 6.141 +/- 0.2
    at n.<anonymous> (unit.js:12954:53)


### Automated 4-Hour Check Alert [2026-08-29 00:15:55 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should calculate interplanetary Hohmann transfer from Mars to ice giant planet Uranus and orbit insertion
?: AssertionError: expected 6087.7 to be close to 5928.7 +/- 50
    at n.<anonymous> (unit.js:13002:47)
- should calculate subsurface Methane Clathrate Hydrate Stability Zone (MHSZ) depth extent and gas storage capacity
?: AssertionError: expected 4363 to be close to 3500 +/- 200
    at n.<anonymous> (unit.js:13016:48)


### Automated 4-Hour Check Alert [2026-08-29 00:16:06 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should calculate interplanetary Hohmann transfer from Mars to ice giant planet Uranus and orbit insertion
?: AssertionError: expected 6.552 to be close to 6.99 +/- 0.2
    at n.<anonymous> (unit.js:13004:53)
- should calculate subsurface Methane Clathrate Hydrate Stability Zone (MHSZ) depth extent and gas storage capacity
?: AssertionError: expected 302.3 to be close to 285 +/- 5
    at n.<anonymous> (unit.js:13018:64)


### Automated 4-Hour Check Alert [2026-08-29 00:30:52 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should calculate interplanetary Hohmann transfer from Mars to outermost ice giant Neptune and orbit insertion
?: AssertionError: expected 11466.3 to be close to 11207.2 +/- 100
    at n.<anonymous> (unit.js:13051:48)
- should calculate subsurface mud volcanism conduit ascent, flash-boiling plume, and flow runout length
?: AssertionError: expected 1.8 to be close to 18.4 +/- 3
    at n.<anonymous> (unit.js:13068:49)


### Automated 4-Hour Check Alert [2026-08-29 00:31:15 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary Hohmann transfer from Mars to outermost ice giant Neptune and orbit insertion
?: AssertionError: expected 6.944 to be close to 7.504 +/- 0.3
    at n.<anonymous> (unit.js:13053:54)


### Automated 4-Hour Check Alert [2026-08-29 00:45:55 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should calculate interplanetary Hohmann transfer from Mars to Kuiper Belt dwarf planet Pluto and orbit insertion
?: AssertionError: expected 16954.7 to be close to 16738.7 +/- 100
    at n.<anonymous> (unit.js:13098:46)
- should calculate burial diagenetic smectite illitization kinetics and geothermometry
?: AssertionError: expected 'R0 Random Mixed-Layer Illite/Smectite?' to include 'Ordered Illite/Smectite'
    at n.<anonymous> (unit.js:13114:51)


### Automated 4-Hour Check Alert [2026-08-29 00:46:16 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should calculate interplanetary Hohmann transfer from Mars to Kuiper Belt dwarf planet Pluto and orbit insertion
?: AssertionError: expected 7.116 to be close to 7.807 +/- 0.3
    at n.<anonymous> (unit.js:13100:52)
- should calculate burial diagenetic smectite illitization kinetics and geothermometry
?: AssertionError: expected 'R3 Highly Ordered Illite / Sericite (?' to include 'Ordered Illite/Smectite'
    at n.<anonymous> (unit.js:13114:51)


### Automated 4-Hour Check Alert [2026-08-29 01:00:43 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary Hohmann transfer from Mars to Kuiper Belt contact binary 486958 Arrokoth and flyby speed
?: AssertionError: expected 20213.5 to be close to 19992.5 +/- 200
    at n.<anonymous> (unit.js:13142:49)


### Automated 4-Hour Check Alert [2026-08-29 01:15:57 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate subsurface salt diapirism, dislocation creep rheology, and halite thermal inertia
?: AssertionError: expected 'Incipient Salt Pillow / Low-Relief Sw?' to include 'Mature Salt Dome / Bulging Sedimentar?'
    at n.<anonymous> (unit.js:13202:54)


### Automated 4-Hour Check Alert [2026-08-29 02:16:06 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Mercury trajectory via Venus Gravity Assist (VGA) and orbit insertion
?: AssertionError: expected 3.372 to be close to 2.06 +/- 0.4
    at n.<anonymous> (unit.js:13366:50)


### Automated 4-Hour Check Alert [2026-08-29 02:16:18 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Mercury trajectory via Venus Gravity Assist (VGA) and orbit insertion
?: AssertionError: expected 5.763 to be close to 5.163 +/- 0.4
    at n.<anonymous> (unit.js:13367:47)


### Automated 4-Hour Check Alert [2026-08-29 02:16:29 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Mercury trajectory via Venus Gravity Assist (VGA) and orbit insertion
?: AssertionError: expected 4.031 to be close to 6.572 +/- 0.8
    at n.<anonymous> (unit.js:13369:58)


### Automated 4-Hour Check Alert [2026-08-29 02:30:58 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should calculate Mars-to-Saturn Grand Tour trajectory via Jupiter Gravity Assist (JGA) and orbit insertion
?: AssertionError: expected 4798.3 to be close to 4321.3 +/- 300
    at n.<anonymous> (unit.js:13411:41)
- should calculate contact metamorphic dehydroxylation kinetics of serpentine and recrystallized olivine yield
?: AssertionError: expected 2499.9 to be close to 2964 +/- 200
    at n.<anonymous> (unit.js:13427:61)


### Automated 4-Hour Check Alert [2026-08-29 02:31:10 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Saturn Grand Tour trajectory via Jupiter Gravity Assist (JGA) and orbit insertion
?: AssertionError: expected 4.197 to be close to 5.046 +/- 0.6
    at n.<anonymous> (unit.js:13413:50)


### Automated 4-Hour Check Alert [2026-08-29 02:31:21 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate Mars-to-Saturn Grand Tour trajectory via Jupiter Gravity Assist (JGA) and orbit insertion
?: AssertionError: expected 4.269 to be close to 5.642 +/- 0.6
    at n.<anonymous> (unit.js:13414:49)


### Automated 4-Hour Check Alert [2026-08-29 02:45:56 UTC]
TEST FAILURE: Unit test suite failed (3 failures):
- should discriminate Magnesite from Siderite, phyllosilicates, and unaltered basalt in CRISM spectra
?: AssertionError: expected undefined to be true
    at n.<anonymous> (unit.js:9765:51)
- should calculate Mars-to-Uranus trajectory via Jupiter Gravity Assist (JUGA) and orbit insertion
?: AssertionError: expected 8920 to be close to 8100.3 +/- 400
    at n.<anonymous> (unit.js:13464:42)
- should calculate subsurface methane clathrate hydrate stability, dissociation kinetics, and outgassing volume
?: AssertionError: expected 942.1 to be close to 1160 +/- 100
    at n.<anonymous> (unit.js:13481:66)


### Automated 4-Hour Check Alert [2026-08-29 03:00:54 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should discriminate Low-Silica Analcime vs High-Silica Clinoptilolite Zeolites in CRISM spectra
?: AssertionError: the given combination of arguments (undefined and string) is invalid for this assertion. You can use an array, a map, an object, a set, a string, or a weakset instead of a string
    at n.<anonymous> (unit.js:13077:42)


### Automated 4-Hour Check Alert [2026-08-29 03:15:48 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to Kuiper Belt Object 48695 Arrokoth
?: AssertionError: expected 20213.5 to be close to 21922.7 +/- 800
    at n.<anonymous> (unit.js:13566:44)


### Automated 4-Hour Check Alert [2026-08-29 03:16:00 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to Kuiper Belt Object 48695 Arrokoth
?: AssertionError: expected 3.314 to be close to 2.004 +/- 0.4
    at n.<anonymous> (unit.js:13570:56)


### Automated 4-Hour Check Alert [2026-08-29 03:30:55 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should calculate interplanetary transfer from Mars to Kuiper Belt dwarf planet 136472 Makemake and orbit capture
?: AssertionError: expected 2.777 to be close to 1.502 +/- 0.4
    at n.<anonymous> (unit.js:13621:60)
- should calculate acid sulfate weathering kinetics of sulfides into jarosite and alunite duricrust
?: AssertionError: expected 0.004 to be above 0.8
    at n.<anonymous> (unit.js:13629:52)


### Automated 4-Hour Check Alert [2026-08-29 03:46:02 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to Kuiper Belt dwarf planet 136108 Haumea and orbit capture
?: AssertionError: expected 19266.8 to be close to 20893.9 +/- 800
    at n.<anonymous> (unit.js:13665:44)


### Automated 4-Hour Check Alert [2026-08-29 03:46:15 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to Kuiper Belt dwarf planet 136108 Haumea and orbit capture
?: AssertionError: expected 2.731 to be close to 1.467 +/- 0.4
    at n.<anonymous> (unit.js:13668:57)


### Automated 4-Hour Check Alert [2026-08-29 04:00:58 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to extreme trans-Neptunian dwarf planet 90377 Sedna
?: AssertionError: expected 7.414 to be close to 8.087 +/- 0.6
    at n.<anonymous> (unit.js:13718:52)


### Automated 4-Hour Check Alert [2026-08-29 04:01:13 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to extreme trans-Neptunian dwarf planet 90377 Sedna
?: AssertionError: expected 2.166 to be close to 1.04 +/- 0.4
    at n.<anonymous> (unit.js:13719:58)


### Automated 4-Hour Check Alert [2026-08-29 04:16:08 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to massive scattered disc dwarf planet 136199 Eris and orbit capture
?: AssertionError: expected 1.647 to be close to 2.45 +/- 0.5
    at n.<anonymous> (unit.js:13771:56)


### Automated 4-Hour Check Alert [2026-08-29 04:31:06 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should calculate interplanetary direct transfer from Mars to resonant scattered disc dwarf planet 225088 Gonggong
?: AssertionError: expected 2.144 to be close to 1.03 +/- 0.4
    at n.<anonymous> (unit.js:13825:60)
- should calculate cryomagma chamber freezing, volumetric overpressure, and hydrofracture eruption threshold
?: ReferenceError: regimeClass is not defined
    at KRCEngine.computeMartianCryochamberFreezingPressurization (http://127.0.0.1:51715/src/features/krc/KRCEngine.js:8932:21)
    at n.<anonymous> (unit.js:13832:32)


### Automated 4-Hour Check Alert [2026-08-29 04:46:19 UTC]
TEST FAILURE: Unit test suite failed (2 failures):
- should calculate interplanetary direct transfer from Mars to 2:3 resonant Plutino dwarf planet 90482 Orcus
?: AssertionError: expected 22421.4 to be close to 24304.5 +/- 1200
    at n.<anonymous> (unit.js:13867:46)
- should calculate hydrothermal serpentinization of ultramafic olivine, H2 degassing, and FTT methanogenesis
?: AssertionError: expected 0.001 to be above 0.5
    at n.<anonymous> (unit.js:13878:53)


### Automated 4-Hour Check Alert [2026-08-29 04:46:38 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to 2:3 resonant Plutino dwarf planet 90482 Orcus
?: AssertionError: expected 2.92 to be close to 1.63 +/- 0.4
    at n.<anonymous> (unit.js:13870:58)


### Automated 4-Hour Check Alert [2026-08-29 05:01:02 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to classical Kuiper Belt Cubewano dwarf planet 50000 Quaoar
?: AssertionError: expected 19441.8 to be close to 21083.5 +/- 800
    at n.<anonymous> (unit.js:13920:44)


### Automated 4-Hour Check Alert [2026-08-29 05:01:17 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to classical Kuiper Belt Cubewano dwarf planet 50000 Quaoar
?: AssertionError: expected 2.953 to be close to 1.664 +/- 0.4
    at n.<anonymous> (unit.js:13923:57)


### Automated 4-Hour Check Alert [2026-08-29 05:46:18 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate hydrothermal smectite-to-illite conversion kinetics, interlayer water expulsion, and shale thermal inertia
?: AssertionError: expected +0 to be above 0.2
    at n.<anonymous> (unit.js:14083:49)


### Automated 4-Hour Check Alert [2026-08-29 08:31:07 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to Mercury-crossing asteroid (3200) Phaethon
?: AssertionError: expected 11.617 to be close to 4.025 +/- 0.5
    at n.<anonymous> (unit.js:14658:51)


### Automated 4-Hour Check Alert [2026-08-29 08:31:30 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate interplanetary direct transfer from Mars to Mercury-crossing asteroid (3200) Phaethon
?: AssertionError: expected 28.13 to be close to 31.84 +/- 3
    at n.<anonymous> (unit.js:14659:60)


### Automated 4-Hour Check Alert [2026-08-29 10:30:55 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate sub-greenschist facies hydrothermal metamorphism, porosity reduction, and crystalline metabasalt thermal inertia
?: AssertionError: expected undefined to be a number or a date
    at n.<anonymous> (unit.js:14235:58)


### Automated 4-Hour Check Alert [2026-08-29 11:16:01 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should discriminate Al-Pumpellyite vs Epidote vs Fe3+-Pumpellyite in CRISM spectra
?: AssertionError: the given combination of arguments (undefined and string) is invalid for this assertion. You can use an array, a map, an object, a set, a string, or a weakset instead of a string
    at n.<anonymous> (unit.js:14678:54)


### Automated 4-Hour Check Alert [2026-08-29 11:46:42 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate atmospheric desiccation and phase transition of mirabilite into anhydrous thenardite and thermal inertia
?: AssertionError: expected 1690.6 to be close to 1976.4 +/- 200
    at n.<anonymous> (unit.js:15307:66)


### Automated 4-Hour Check Alert [2026-08-29 12:00:49 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate hydrothermal alteration and nanotubular crystallization kinetics of weathered volcanic ash into hydrated halloysite and thermal inertia
?: AssertionError: expected 'Nanotubular Halloysite Kaolin Facies ?' to include 'Hydrated Kaolin Nanomaterial Facies'
    at n.<anonymous> (unit.js:15358:42)


### Automated 4-Hour Check Alert [2026-08-29 12:31:22 UTC]
TEST FAILURE: Unit test suite failed (1 failures):
- should calculate low-to-moderate temperature alkaline metasomatism of basalt into fibrous sepiolite and thermal inertia
?: AssertionError: expected 'Alkaline Lacustrine Sepiolite Facies ?' to include 'Fibrous Magnesium Silicate Facies'
    at n.<anonymous> (unit.js:15458:43)


### Automated 4-Hour Check Alert [2026-08-30 08:53:04 UTC]
TEST FAILURE: Browser console errors encountered during UI interaction:
[pageerror] molaDem.getElevation is not a function
[pageerror] molaDem.getElevation is not a function
