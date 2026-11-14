# JSMARS Release Notes


## v0.14.0 - Mission JSMARS: Space Data Analysts (`/spacecamp/`), Live Leaderboard & Unobstructed 3D Globe HUD
**Date:** 2026-11-12

### Highlights
- **Mission JSMARS: Space Data Analysts (`spacecamp/index.html`, `src/features/spacecamp/`)**:
  - Launched an evergreen, self-guided and classroom-ready planetary GIS & remote-sensing mission game at `/spacecamp/` (*Atelier numérique de planification de missions et d'analyse en télédétection*) while keeping the root `/` scientific viewer 100% untouched via shared `src/app-init.js`.
  - **Wide-Screen Docked Right Side Panel (`384px`)**: Co-docks `#map`, `#globe-view`, and `#status-bar` (`body.sc-panel-open`) with a collapsible edge tab (`#sc-edge-toggle-btn`) and zero startup blocking modal so all JSMARS layers and tools remain unobstructed.
  - **Strict English Default + Instant `EN | FR` Switch**: Defaults to English regardless of OS browser locale, with 1-click full French translation persisted via cookie (`jsmars_sc_lang`).
- **5 Progressive Scientific Rounds (`60 + 90 + 120 + 150 + 180 = 600 Max Points`)**:
  - **Round 1 (Earth EO - 60 pts)**: Added 10 m ESA Copernicus Sentinel-2 Cloudless WMS (`earth_s2_cloudless`) on Earth to classify 4 real Moroccan land covers (Middle Atlas Cedar Forest, Ben Guerir Urban Corridor, Doukkala Irrigated Cropland, Erg Chebbi Sahara Dunes) + geodesic Measurement.
  - **Round 2 (Mars Laser Altimetry - 90 pts)**: MOLA Colorized Elevation layer stacking, reticle scanning of Olympus Mons & Valles Marineris (with `SearchBar.setSpoilerFilter` anti-spoiler protection), and MOLA elevation profiling.
  - **Round 3 (Mars Rover Path & Payload Planning - 120 pts)**: Multi-waypoint rover route engineering from Jezero Crater Floor (`18.38°N, 77.58°E`) to Nili Carbonate Highlands (`18.72°N, 76.95°E`) scored against live USGS 128 ppd MOLA DEM slopes with an inline SVG elevation sparkline + payload tool selection.
  - **Round 4 (Artemis Lunar South Pole Landing Site Selection - 150 pts)**: Integrated live 4-criteria Artemis engineering scorecard inside `LunarSitePlanner` over real NASA LRO LOLA slope, solar visibility, Earth radio visibility, and PSR water-ice rasters (`jmars:lunar-site-inspected`).
  - **Round 5 (Europa Ocean World Finale - 180 pts)**: Auto-unlocks after scoring in 3 trails or 25 minutes (playable solo without an instructor), featuring corrected Conamara Chaos coordinates (`8.85°N, 86.15°E`), 9 m/px Galileo SSI mosaic activation, and vector shape / 3D Globe cartography.
- **Precision Flight Operations Avionics UI (`SpaceCampIcons.js`, `spacecamp.css`, `SpaceCampOverlay.js`)**:
  - Engineered a dark-titanium Flight Operations console (`#05080f` recessed telemetry wells, `#090d16` panel chassis, `#0e1421` task modules, `JetBrains Mono` `tabular-nums` readouts, 3-cell Operator/Telemetry/Standing strip, collapsible Sensor & GIS Spec accordion, and zero-emoji `1.75px` monoline aerospace SVG icons).
  - Calibrated for a 1-hour live workshop (`5m` Intro + `5 × 5m` Trails = `25m` + `5m` Podium + `25m` Buffer) with `5m Trail` / `25m (5×5m)` / `60m Event` projector timer presets and `15:00` fallback auto-unlock on Round 5 (`autoUnlockMinutes: 15`).
  - Upgraded Earth WMS base/relief layers (`jmars-config.js`, `EnhancedProfileTool.js`, `RadialProfileTool.js`) to CORS-enabled ESA Copernicus / EOX WMS (`https://tiles.maps.eox.at/wms`) for zero-503 tile reliability, and filtered Web-Mercator-only (`*_3857`) WMS capability entries in `JMARSWMS.parseCapabilities()` + `createLeafletLayer()` to prevent HTTP 400 SRS mismatches on `EPSG:4326` maps.
  - Added `src/util/leaflet-draw-patch.js` (`applyLeafletDrawPatch`) so polygons and polylines finish reliably when clicking either the first or last vertex (`18px` touch/mouse snap radius), paired with dedicated **Measure Area (Polygon)** and **Measure Distance (Line)** actions in Round 1.
  - Centered `.map-top-hud` and reserved a `30px` right control gutter when `body.sc-panel-open` is active so top HUD and edge-toggle controls never overlap Leaflet zoom/draw toolbars on medium or wide viewports.
  - Updated Briefing Slide 1 with verified co-creator profiles and cropped portraits for **Dr. Moulay Anwar Sounny-Slitine** (`assets/spacecamp/anwar-avatar.jpg`) and **Sara Bensalem** (`assets/spacecamp/sara-avatar.jpg`), added the Noor III Ouarzazate CSP aerial plate on Slide 2 (`assets/spacecamp/noor-ouarzazate.jpg`), and removed callsign registration prompts from lecture slides.
  - Added USGS Mars WMS layers (`mars_mola_color`, `mars_mola_themis_blend`, `mars_themis_day`, `mars_mola_bw`) in `jmars-config.js`, switched Space Camp layer toggles to `jmarsState.setActiveLayers()` so MOLA and 9 m/px Galileo mosaics mount on the 2D map immediately, and upgraded `mola-dem.js` to read Cloud-Optimized GeoTIFF overviews (`<= 512x512`) and `128x128` spatial buckets for sub-second Mars elevation profiles.
  - Hardened Service Worker (`sw.js` v1.8.6) WMS/tile cancellation fallbacks for 3D Cesium Web Workers, filtered null children in `SpaceCampOverlay` modal/drawer renders, collapsed sibling tool drawers in `openToolByTitle`, and upgraded `SpaceCampCopilot` offline diagnostics to answer planetary science questions and cross-round engineering critiques directly.
  - Hardened workshop & measurement/profiling tools (`sw.js` `v1.8.8`): fixed planetary `haversineDistance` and WMS XML/sentinel error guards in `EnhancedProfileTool` and `RadialProfileTool`; removed `+20%` Y-axis relief inflation and added live loading status in `ProfileChart`; moved `#profile-chart-container` directly beneath **Linear Profile** so both controls stay visible on laptop viewports; added `dblclick`/`Enter` completion and `jmars:draw-vertex-changed` hooks in `leaflet-draw-patch`; added `#sc-tool-coach` floating `Finish`/`Cancel` coach bar, Slide 5 active **Start Mission** button, Round 2 live MOLA cross-section readout with Earth scale comparisons + **Center on Target**, Round 3 **Count Craters** shortcut, Round 5 auto-enabled polygon drawing, and full `FR` localization across `SpaceCampOverlay` and `SpaceCampLeaderboardPage`.
  - Telemetry- & user-testing hardening (`sw.js` `v1.8.9`):
    - Upgraded core planetary tools: planetary radius angular scaling, zoom-adaptive defaults, `[25 m..1,000 km]` clamping, keyboard sizing, and persistent craters across tool switches in `CraterLayer`; live planetary-scaled distances/areas and `km²` formatting (`>= 100,000 m²`) in `leaflet-draw-patch`; smooth auto-scroll to measurement table in `MeasureTool`; full reticle/hover cleanup across tool resets and body switches in `RadialProfileTool` and `EnhancedProfileTool`; non-spoiler `EN | FR` hint in `SearchBar`; and `jmars:lunar-thematic-changed` sync in `LunarSitePlanner`.
    - Upgraded Space Camp pedagogical scaffolding & safety (`SpaceCampCookies`, `SpaceCampEngine`, `SpaceCampOverlay`): normalized leetspeak/token callsign moderation (`handleModerated`), unclaimed-points in-place nudge, Round 1 4/4 classification readiness spotlight (`#sc-r1-measure-card.is-ready-highlight`), Round 2/5 1-click wrong-body button + `WARMER`/`COLDER` delta badges + 0-pt sector hint after 2 misses, Round 3 Step 3A diagnostic **Check My 3 Tools** verification (`evaluateRound3Tools`) + Jezero traverse auto-focus + Undo Last Point (`#sc-undo-rover-btn`) + live Nili target distance readout, and Round 4 4-pill quick NASA LOLA thematic switcher.
- **Live Firebase Realtime Database Scoreboard & Winner's Circle (`database.rules.json`, `SpaceCampFirebase.js`, `SpaceCampLeaderboardPage.js`)**:
  - Real-time score and discovery feed streaming backed by Firebase RTDB (`sounny-com`) with unreadable player write secrets, `[0..600]` server-enforced bounds, unique case-insensitive callsign claiming with 3 collision-free suggestions, and shared-PC operator switching.
  - Accessible both via the in-app **Leaderboard** console tab and the standalone 1080p/4K projector & Winner's Circle Podium dashboard at `/spacecamp/leaderboard/` (`All-Time` vs `Live Today` filters + Specialist Mission Commendations).
- **Telemetry-Grounded AI CapCom (`SpaceCampCopilot.js`)**:
  - Built-in Socratic telemetry diagnostics out of the box plus optional local-session Gemini API key integration using `gemini-3.8-flash` on the official `/v1beta/interactions` REST API (`Api-Revision: 2026-05-20`).
- **Unobstructed 3D Globe Right-Rail HUD & Coordinate Clamping (`ViewModeToggle.js`, `globeViewModel.js`, `CesiumGlobe.js`, `StatusBar.js`, `PlanetaryScaleBar.js`, `style.css`)**:
  - Replaced the center-blocking 3D globe panel with a docked `54px` dark-titanium vertical icon rail on the right edge (`> 768px`) and a compact single-row bottom pill dock on mobile (`<= 768px`), keeping 100% of the planetary disk unobstructed.
  - Moved **Solar Terminator** and **3D Globe & Data Provenance** into dismissible contextual popovers paired with a bottom-right provenance summary pill (`#globe-micro-attribution`), and offset Cesium bottom-left credits past `#controls`.
  - Wired live 3D hover/camera telemetry (`jmars:globe-hover`) into `#status-bar` and clamped out-of-range EPSG:4326 latitudes to `[-90°, +90°]` (`[-89.9°, +89.9°]` in `PlanetaryScaleBar.getMetersPerPixel()`).

## v0.13.0 - Astronomical 3D Solar Ephemeris & 26-Body JMARS Solar System Catalog
**Date:** 2026-11-11

### Highlights
- **Astronomical 3D Solar Terminator (`src/util/solarEphemeris.js`, `CesiumGlobe.js`, `ViewModeToggle.js`)**:
  - Eliminated the legacy 3D `"Light"` button's hardcoded `z = -0.15` (`+8.63°N`) solar declination trap and prime-meridian (`0°E`) noon offset.
  - Implemented **IAU WGCCRE (Archinal et al., 2018)** rotational elements ($\alpha_0, \delta_0, W_0, \dot{W}$, plus lunar E1 libration bounding lunar solar declination strictly within $\pm 1.59^\circ$) coupled with **JPL J2000 Keplerian heliocentric orbits** across every supported planetary system.
  - Renamed the 3D control to **Terminator** with explicit scientific provenance: **Live UTC Ephemeris** (`[Ephemeris]`, synchronized with real-time UTC, the Mars $L_s$ `TimeSlider`, and Earth's `SkyAtmosphere` clock) and **Local Hour [Model]** (`[Model]`, anchoring local solar hour `00:00–24:00` to the camera's viewed longitude while preserving seasonal declination).
  - Appended an explicit smooth-ellipsoid disclaimer in `.globe-provenance` whenever terminator shading is active, clarifying that 3D DEM shadows are not loaded and local crater/mountain shadows in imagery remain baked spacecraft illumination.
- **Mars Subsolar Longitude & TimeSlider Sync (`MarsTime.js`, `TimeSlider.js`)**:
  - Fixed `MarsTime.computeState(date).subSolarLon` using Allison & McEwen (2000) Eq. (20) & (24) (`180 - mtc * 15 - eotDeg` including equation of center) and updated `TimeSlider.setLs()` to emit `date: MarsTime.lsToDate(newLs, currentMY)` so scrubbing $L_s$ updates the 3D seasonal terminator live.
- **26 Verified JMARS Solar System Worlds (`jmars-config.js`, `geo.js`, `globeViewModel.js`, `BodySelector.js`)**:
  - Expanded JSMARS from 4 bodies to **26 spacecraft-mapped worlds** organized into 7 semantic `<optgroup>` sections: **Inner Planets & Moon** (*Mars, Moon, Earth, Mercury, Venus*), **Mars Moons** (*Phobos, Deimos*), **Asteroids & Dwarf Planets** (*Ceres, Vesta*), **Jupiter System** (*Jupiter, Io, Europa, Ganymede, Callisto*), **Saturn System** (*Saturn, Mimas, Enceladus, Tethys, Dione, Rhea, Titan, Iapetus, Phoebe*), **Neptune System** (*Triton*), and **Pluto System** (*Pluto, Charon*).
  - Every WMS/WMTS/XYZ layer was live-verified over HTTPS with `Access-Control-Allow-Origin: *` for both 2D Leaflet and 3D WebGL. Simulated gas-giant mosaics (`Uranus Simulated Global Mosaic` / `Neptune Simulated Global Mosaic`) were audited and excluded to uphold 100% real spacecraft data integrity.
  - Added IAU IAU oblate spheroid 3D ellipsoids (`globeViewModel.js`) with radius-proportional minimum camera zoom distances for small moons/asteroids, 10,000–50,000 km gas-giant scale bar steps (`PlanetaryScaleBar.js`), 56 new IAU landmarks (`landmarks.json`), Venera 9/13/14, Vega 2 & ESA Huygens landing sites (`landing-sites.json`), 26-body deep-link URL parsing (`URLStateEngine.js`), and canonical `switchActiveBody` synchronization in `Ctrl+K` `QuickActions` (`index.html`).
- **Verification & PWA Shell (`sw.js` v1.7.0)**:
  - Expanded automated Mocha unit test suite to **1,218 passing tests (0 failures)** and desktop + mobile Playwright E2E suite (`verification/test_localhost_full.py`) covering Pluto, Titan (`Ctrl+K` switch + Huygens), Mars 3D Ephemeris/Model Terminator, and triaxial Phobos.

## v0.12.0 - JMARS Desktop Parity: Geologic Patterns, ESRI Shapefiles, Command Palette & Mobile HUD
**Date:** 2026-11-10

### Highlights
- **Geologic Fill Pattern Library (`StyleEditor.js`, `ShapeLayer.js`)**: Added 6 planetary SVG `<pattern>` fills (*Ejecta Blanket*, *Crater Material*, *Volcanic Plains*, *Fracture Zone*, *Layered Deposits*, *Chaos / Breccia*) selectable via the right-click Style Editor and preserved across GeoJSON and Session round-trips.
- **Binary ESRI Shapefile Triad & Multi-Part WKT I/O (`ShapeIO.js`)**: Added standards-compliant browser binary serialization of ESRI Shapefile triads (`.shp`, `.shx`, `.dbf` partitioned by Point/Arc/Polygon geometry class), companion `.shp` + `.dbf` multi-file import, and multi-feature `.wkt` import/export.
- **Dynamic Custom Attribute Columns & Multi-Body Geodesic Buffers (`ShapeTable.js`, `ShapeLayer.js`)**: Added `+ Column` custom schema extension, XSS-safe inline attribute editing, and planet-calibrated geodesic `Buffer` generation across Mars, Moon, Earth, and Europa.
- **Independent Geodesic Measurement & Draw Isolation (`MeasureTool.js`, `MeasurementTable.js`, `jmars-vectors.js`)**: Added initial forward geodesic **Azimuth** ($0^\circ\text{–}360^\circ$) on polylines, geodesic **Perimeter** on polygons, per-row `×` deletion, and `_exclusiveDrawOwner` draw isolation so Measurements, Profiles, and Area Sampling never leak temporary geometries into the Shapes table.
- **Global `Ctrl+K` Command & Navigation Palette (`QuickActions.js`, `KeyboardShortcuts.js`)**: Added a unified accessible modal command palette (`Ctrl+K` / `⌘+K` and top-HUD `Search / Cmd` button) combining fuzzy IAU landmark search, direct `lat, lon` fly-to, saved bookmarks, planetary body switching, 2D/3D view toggling, and tool launch.
- **Full Session Shape Persistence (`SessionManager.js`)**: Saved and restored `ShapeLayer` features, geologic fill patterns, circle radii, and custom attribute dictionaries across cross-body session loads.
- **Mobile & Desktop Top-HUD Ergonomics (`style.css`, `index.html`, `Sidebar.js`)**: Centered `#map-top-hud` (`2D | 3D` + `Search / Cmd`) over the visible map area on desktop (tracking sidebar collapse) and repositioned it cleanly below `#status-bar` on mobile viewports (`<= 768px`), resolving the mobile Europa 2D/3D status-bar overlap.
- **Verification & PWA Shell (`sw.js` v1.6.0)**: Expanded automated Mocha suite to **1,210 passing unit tests** (`scripts/run_tests.py` with fast-fail `pageerror` diagnostics) plus full desktop + mobile Playwright E2E verification (`verification/test_localhost_full.py`).

## v0.11.0 - Core Hardening, Geodesy Unification & Accessibility Milestone
**Date:** 2026-10-13

### Highlights
- **Universal Multi-Body Planetary Radius**: Centralized all planetary geodesic radii in `src/util/geo.js` with `getPlanetaryRadius(body, type)` supporting Mars, Moon, Earth, and Europa ($R_{\text{mean}}$, $R_{\text{eq}}$, $R_{\text{pol}}$). Completely eliminated hardcoded Mars fallbacks across `RadialProfileTool`, `LandingSitesLayer`, `InvestigateTool` (gravity anomalies, dipole fields), `ThreeDEngine`, `NomenclatureTool`, and `SamplingTool`.
- **Reproducible Spatial Sampling**: Replaced non-deterministic sampling with Mulberry32 seeded pseudo-random coordinate generation keyed to feature bounding boxes.
- **Service Worker Hardening (v1.5.2)**: Resolved critical navigation cache collision where opening secondary pages overwritten the cached root shell. Precached all owned application assets across `src/`, `assets/icons/`, `assets/lunar-south-pole/manifest.json`, and `vendor/`.
- **NASA Artemis III October 2024 Alignment**: Updated lunar landing candidate database to NASA's refined candidate exploration regions (including Peak near Cabeus B, Haworth, Malapert Massif, Mons Mouton, Mons Mouton Plateau, Nobile Rim 1, Nobile Rim 2, de Gerlache Rim 2, Slater Plain), updated target launch date to NET 2027+, and removed cancelled VIPER rover references.
- **Information Architecture**: Reorganized sidebar from a flat list of 23 tools into 5 structured semantic categories:
  1. Measure & Inspect
  2. Cartography & Geology
  3. Planetary Data & Orbital Layers
  4. Physical Models & Simulations [Modeled]
  5. Mission Planning & 3D Visualization
- **WCAG 2.1 AA Accessibility & Ergonomics**:
  - Full keyboard support (Enter and Space) and ARIA attributes (`role="button"`, `tabindex="0"`, `aria-expanded`, `aria-controls`) across all sidebar collapsible headers in `CollapsibleGroup.js` and `MobileSheet.js`.
  - Welcome dialog focus trapping, Escape key dismiss, and focus restoration.
  - Minimum 44x44px touch targets on info popover buttons (`.tool-info-btn`).
  - Screen reader accessible toast notifications (`role="status"` and `aria-live="polite"`).
  - PWA lifecycle check for waiting service workers and safe `controllerchange` update reloads.
- **Scientific Provenance Badges**: Explicit badges distinguishing procedural/illustrative textures in 3D viewer and multispectral ratio proxies in Band Math from raw spacecraft observations.
- **Automated Verification Harness**: Added `scripts/run_tests.py` providing a headless Playwright runner executing all unit tests with 100% pass rate.

### New Features
- **Lunar Base Site Planner** (`src/features/lunar-site/`): full-screen south polar workspace built on real LOLA data. Ten PGDA landing-site DEMs (Barker et al. 2021, 5 m slope resampled to 20 m), regional 120 m DEM (PDS GDR), and Mazarico et al. (2011) solar visibility, Earth visibility and PSR maps.
- Slope hazard classes with user thresholds, roughness proxy, distance to PSR, flat-area finder, corridor elevation profiles, landing pads with user-defined blast buffers, geometric horizon / Sun-path analysis.
- DXF and SVG site plan import with per-polygon slope / sun / PSR checks.
- **Exports:** terrain OBJ (Z-up or Y-up), elevation and max-slope GeoTIFF, site plan DXF in the same local frame, corridor CSV, plan JSON save/load (preserving slope thresholds, CAD unit/scale/rotation/origin, and layer visibility checkboxes), and one-click export window reset. Optional live read of the USGS-hosted LOLA 5 m COG.
- **Fuzzy Search, Direct Coordinates & Cross-Body Navigation**: Upgraded `SearchBar` with Levenshtein typo-tolerant landmark search, direct coordinate parsing (`18.38N, 77.58E`, `0–360°E` normalization), planetary body badges, automatic cross-body switching when selecting a landmark on another body, and keyboard/ARIA combobox navigation.
- **WMS Discovery Cancellation & Modal Shortcut Isolation**: Wired `AbortSignal` into `JMARSWMS.fetchCapabilities()` to cancel in-flight WMS requests during rapid body switches, and isolated global keyboard shortcuts while the Lunar Base Site Planner modal (`body.lsp-open`) is active.
- Data package build script `scripts/build_lunar_south_pole_assets.py` with a provenance manifest.
- Service worker (`jsmars-shell-v1.5.2`) never caches `.tif` rasters.

### Known limitations
- No longest-continuous-darkness layer (not a published raster), no boulder density, no bundled blast radii. See `docs/lunar-site-planner.md`.

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
