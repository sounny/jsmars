# JSMARS User Guide

Welcome to JSMARS, a web-based planetary GIS viewer.

## Getting Started
1. Open `index.html` in your browser.
2. The map will load centered on Mars (Lat 0, Lon 0). Use the grouped planetary body selector at the top of the sidebar (or press **Ctrl+K** and type a world's name) to switch across **26 verified Solar System worlds** organized into 7 planetary systems (*Inner Planets & Moon*, *Mars Moons*, *Asteroids & Dwarf Planets*, *Jupiter System*, *Saturn System*, *Neptune System*, and *Pluto System*).

## Managing Layers
- **Add/Remove:** Use the buttons in the "Layer Manager" panel (top-right) to add layers from the available list or remove active layers.
- **Find Layers Faster:** Use the **Filter available layers** search box to narrow long layer lists by name or ID.
- **Opacity:** Use the slider below each active layer name to adjust transparency.
- **Settings:** Double-click an active layer, press **Enter/Space** when the layer item is focused, or use the **⚙ settings button** to open its settings panel with metadata, attribution, and a quick opacity control.


## Tool Categories & Organization
The sidebar organizes 23 tools into 5 structured, accessible categories:
1. **📐 Measure & Inspect**: Point investigation, distance & area measurement, radial & linear elevation profiles, reproducible statistical sampling, and lat/lon coordinate grids.
2. **🌋 Cartography & Geology**: Crater counting with CSFD isochron dating, IAU planetary nomenclature, contour lines, dynamic hillshade relief, custom GeoTIFF upload, and vector shapes.
3. **🛰️ Planetary Data & Orbital Layers**: Spacecraft landing sites (including updated NASA Artemis III October 2024 candidate regions), USGS ODE instrument footprint browser (Stamp), Keplerian orbit ground tracks, and interplanetary transfer trajectory planning.
4. **🔬 Physical Models & Simulations [Modeled]**: KRC 1D thermal conduction, MCD vertical atmospheric profiler, subsurface radar sounding, and multispectral mineral ratio proxies.
5. **🚀 Mission Planning & 3D Visualization**: Full-screen Lunar Base Site Planner (LOLA South Pole DEMs, slope hazards, blast buffers, DXF/SVG CAD site imports), 3D interactive globe and terrain viewer, Mars solar time slider, and publication cartographic map export.

## Accessibility & Keyboard Navigation
- **Tool Accordions & Sections:** Every collapsible section header is fully keyboard accessible (`role="button"`). Press **Tab** to navigate and **Enter** or **Space** to toggle any tool panel open or closed.
- **Dialogs & Modals:** The welcome modal and full-screen tools support **Escape** key dismissal and automated focus trapping.
- **Touch-Friendly Controls:** Popover buttons and mobile sheet controls feature expanded 44x44px minimum touch targets.
- **Screen Reader Notifications:** Status updates, PWA toasts, and copy-link confirmations announce politely via ARIA live regions.

## Planetary Science Tools

### KRC Mars 1D Thermal Model
1. Open the **KRC Thermal Model** section under Tools.
2. Click **📍 Pick Location** and click on any point on Mars (or type latitude/elevation).
3. Adjust Thermal Inertia ($J\cdot m^{-2}\cdot K^{-1}\cdot s^{-1/2}$), Albedo, and Dust Opacity ($\tau$).
4. Click **Run Simulation** to solve the 1D heat diffusion equation. The panel is labeled **Model** — output is a client-side Kieffer-style simulation, not TES/THEMIS measurements.
5. Toggle between **Diurnal** temperature curves ($T(t)$), **Depth** subsurface profiles ($T(z)$), and **Seasonal** curves ($T(L_s)$).
6. Export simulation tables via **Export CSV** or save charts via **PNG**.

### 3D Globe
1. Use the **2D | 3D** control at the top of the map. 2D is the flat Leaflet map. 3D replaces it with a CesiumJS globe of the active body.
2. The globe uses the body's reference ellipsoid and drapes the imagery layer you have turned on (USGS, OpenPlanetary, NASA GIBS, or NASA Trek). **Elevation terrain is not loaded** — Olympus Mons will not stand above the surrounding plains. That is planned. The bottom-right attribution pill (`ⓘ`) or the **Globe Info** (`i`) button on the right-edge icon rail (bottom pill dock on mobile) opens the full imagery, ellipsoid, and elevation provenance popover.
3. Drag to orbit, scroll or use **+ / −** on the right-edge icon rail (or mobile bottom dock) to zoom, **Reset** to return to overview, and **Spin** to rotate. **Grid** draws a latitude/longitude overlay. **Terminator** opens a dismissible solar illumination popover beside the rail with two provenance-labeled modes: **Live UTC Ephemeris** (`[Ephemeris]`, computing the true subsolar point from JPL J2000 orbits + IAU WGCCRE body pole/rotation elements, synced with the Mars $L_s$ Time Slider or snapped to real-time UTC via **Now**) and **Local Hour [Model]** (`[Model]`, anchoring local solar hour `00:00–24:00` to your currently viewed longitude while keeping the body's true seasonal solar declination). Note that terminator shading applies to the smooth reference ellipsoid only (local crater shadows in tiles remain baked spacecraft illumination). Hovering or moving the 3D globe updates the bottom `#status-bar` live.
4. Switch back to **2D** to restore the place you were looking at in 3D.
5. Drawing, measuring, crater counting, profiles, and the other map tools stay in 2D. They are greyed out while 3D is open.

### Mars Climate Database (MCD) Profiler
1. Open the **MCD Atmospheric Profiler** section under Tools.
2. Choose **1D Analytical Physics Model** (offline) or **LMD MCD v6.1 Live GCM**. Live fetch is a climate model, not a spacecraft profile; failures are labeled **Offline Fallback**.
3. Pick a location or specify coordinates, elevation, and local solar hour.
4. Click **Calculate Profile** to generate vertical profiles up to 50 km altitude.
5. Toggle between **Temp** $T(z)$, **Pressure** $P(z)$ (log scale), and **Wind** speed curves.

### Crater Counting & CSFD Isochron Dating
1. Under **Crater Counting**, click **Start Crater Counting**.
2. Resize the ghost circle cursor using the mouse scroll wheel, and click to digitize craters.
3. The integrated **CSFD Chart** plots cumulative size-frequency distribution ($N(>D)/\text{km}^2$) against Hartmann & Neukum isochron models ($10\text{ Ma} - 4.3\text{ Ga}$).
4. The system calculates model surface age and geological epoch (Amazonian / Hesperian / Noachian).

### Educational Mineral-Index Visualizer
1. Open the **Mineral Index Visualizer** section under Tools.
2. Select a preset (e.g. **BD530 Ferric Iron**, **BD1900 Hydrated Clays**, **BD1500 Water Ice**, **THEMIS Olivine**) or enter a custom formula. These are CRISM-style teaching formulas over illustrative overlays — **not** per-pixel CRISM/THEMIS cubes.
3. Choose a colormap (**Viridis**, **Magma**, **Coolwarm**, **Jet**, **Rainbow**) and adjust color stretch.
4. Click **Apply Educational Overlay**. The panel is labeled **Model**.

### Subsurface Radar Sounder (simulation)
1. Open the **Subsurface Radar Sounder** section under Tools. The panel is labeled **Model**.
2. Choose an *illustrative* ground-track region (Planum Boreum, Planum Australe, Medusae Fossae, Utopia Planitia, or Europa ice-shell presets). These are not measured SHARAD/MARSIS/REASON radargrams.
3. Configure target dielectric permittivity ($\varepsilon_r$) and loss tangent ($\tan\delta$).
4. Click **Synthesize Radargram** and toggle between the **2D B-Scope** and **1D A-Scope**. Use the PDS SHARAD/MARSIS archive links in the panel for observed data.
5. Export synthetic radargram tables via **Export Synthetic Radar CSV**.

### Interplanetary Trajectory & Astrodynamics Planner
1. Open the **Interplanetary Trajectory** section under Tools.
2. Select origin body (Earth, Mars, Venus) and destination body.
3. Click **Calculate Transfer Budget** to compute heliocentric Hohmann transfer orbits, Trans-Mars Injection ($\Delta v_1$), Mars Orbit Insertion ($\Delta v_2$), $C_3$ launch energy, and flight duration in days/months.
4. Inspect upcoming Earth-Mars synodic launch opportunities and export mission plans to CSV.

## Planetary Cartography & Overlays

### Planetary Lat/Lon Graticule Grid
- Toggle the **Lat/Lon Grid** in the sidebar to overlay adaptive planetary coordinate graticule lines.
- Customize line color, opacity, major/minor divisions, and coordinate notation formats ($0^\circ-360^\circ\text{ E}$, $\pm 180^\circ$, $0^\circ-360^\circ\text{ W}$).

### Publication Cartography & GIS World File Export
- Under **Map Export**, export high-resolution publication-ready maps with neatline borders, coordinate tick labels, scale bars, and titles.
- Click **Export World File** to download `.pgw` or `.jgw` georeferencing sidecar files for direct import into QGIS, ArcGIS, and GDAL.

## Independent Geodesic Measurement (Distance, Azimuth, Area & Perimeter)
1. Open **Measurement** under **Measure & Inspect** in the sidebar.
2. Click **Distance** to draw a multi-segment polyline (computes planet-scaled geodesic length and initial forward **Azimuth** bearing $0^\circ\text{–}360^\circ$) or **Area** to draw a polygon (computes spherical **Area** and geodesic **Perimeter**).
3. Measurements stay strictly isolated from the **Shapes** layer. Review values in the table, delete individual rows with **×**, or export to **GeoJSON** / **CSV**.

## Drawing Shapes, Geologic Patterns & Shapefile/WKT I/O
1. Open the **Shapes** section under **Cartography & Geology** (or use the map draw toolbar).
2. Select a geometry tool: **Point**, **Line**, **Polygon**, **Circle**, or **Rectangle**.
3. **Geologic Fill Patterns:** Right-click any polygon, circle, or rectangle on the map to open the **Style Editor** and apply stroke/fill colors, dash styles, and SVG geologic unit patterns (*Ejecta Blanket*, *Crater Material*, *Volcanic Plains*, *Fracture Zone*, *Layered Deposits*, *Chaos / Breccia*).
4. **Custom Attributes & Geodesic Buffers:** Click **+ Column** in the shape table toolbar to add custom attribute fields (double-click any cell to edit inline), or select a shape and click **Buffer** to generate a planet-scaled geodesic buffer polygon in kilometers.
5. **GIS Import / Export:** Import and export **GeoJSON**, **WKT (`.wkt`)**, **ESRI Shapefile triads (`.shp` + `.shx` + `.dbf`)**, **CSV**, or **KML**. Select both `.shp` and `.dbf` together when importing a shapefile to preserve all attribute columns. Shapes, patterns, and attributes are also saved inside JSMARS Session `.json` files.

## Navigation & Quick Command Palette (`Ctrl+K`)
- **Quick Command Palette (`Ctrl+K` / `⌘+K`):** Click **Search / Cmd** at the top of the map or press **Ctrl+K** anywhere to jump to IAU landmarks (typo-tolerant fuzzy search), fly directly to `lat, lon` coordinates, recall saved bookmarks, switch planetary bodies, toggle **2D | 3D**, or launch any science tool.
- **Pan:** Click and drag the map.
- **Zoom:** Use the +/- buttons or your mouse wheel.
- **Coordinates:** View current Latitude/Longitude in the status bar. Click the format button to cycle between $E180$, $E360$, and $DMS$.
- **Map Viewpoints:** Use the Map Options panel to jump between Global Equirectangular, North Polar (Planum Boreum), and South Polar (Planum Australe) views.
- **North Arrow & Planetary Scale Bar:** View real-time physical scale bars calibrated to true planetary radii ($R_{\text{Mars}} = 3389.5\text{ km}$, $R_{\text{Moon}} = 1737.4\text{ km}$, $R_{\text{Europa}} = 1560.8\text{ km}$) with cosine latitude distortion.

## Troubleshooting
- **"Loading map data..." stuck?** The map server might be slow or down. Try refreshing the page.
- **Missing Layers?** Check your internet connection; layers are fetched dynamically from USGS/OpenPlanetary.


## Lunar Base Site Planner

Open **Lunar Base Site Planner** in the sidebar and press **Open Site Planner**. Pick a south pole site, choose a thematic layer (slope hazard, solar or Earth visibility, PSR, roughness proxy, elevation), set your slope thresholds, and use the tool bar: Inspect, Corridor, Place pad, Horizon, Place CAD, Export window. Import DXF/SVG plans and export OBJ, GeoTIFF, DXF, CSV or a plan JSON. Press Esc or the close button to return to the main map. Full guide: [lunar-site-planner.md](lunar-site-planner.md).

## Mission JSMARS: Space Data Analysts (`/spacecamp/`)

Open `https://jsmars.sounny.com/spacecamp/` to launch **Mission JSMARS: Space Data Analysts** (*Atelier numérique de planification de missions et d'analyse en télédétection*), an interactive 5-round Earth Observation and Planetary GIS game built on top of the full JSMARS scientific suite while leaving the root `/` scientific viewer untouched.

- **Docked Wide-Screen Side Panel:** Co-docks a collapsible 384 px Mission Control panel on the right edge (`›` / `‹`), keeping 100% of the map and all 23 JSMARS tools open on the left.
- **Language Toggle (`EN | FR`):** Defaults to English on load with a one-click French toggle saved in a browser cookie.
- **1-Hour Event Budget & Evergreen Solo Play:** Paced for a 60-minute classroom workshop (**5 min** Intro Slides, **5 × 5 min** Trails = **25 min** core play, **5 min** Winner's Circle Podium, plus **25 min** buffer for setup, transitions, and questions), while remaining playable solo any day of the year. Claim a unique callsign (saved via cookie with `⇄` shared-PC switching, automatic collision suggestions, and live Firebase Realtime Database sync).
- **5 Progressive Rounds (5 Min Each · 600 Max Points):**
  1. **Round 1 (~5m · 60 pts) - Earth Observation:** Classify 4 real Moroccan satellite targets on 10 m ESA Copernicus Sentinel-2 imagery (Middle Atlas Cedar Forest, Ben Guerir Urban Corridor, Doukkala Irrigated Cropland, Erg Chebbi Sahara Dunes) + geodesic Measurement.
  2. **Round 2 (~5m · 90 pts) - Mars Laser Altimetry:** Switch to Mars, stack MOLA Colorized Elevation, scan Olympus Mons and Valles Marineris, and slice a MOLA topographic cross-section.
  3. **Round 3 (~5m · 120 pts) - Mars Rover Traverse Planning:** Select JSMARS payload planning tools and plot a multi-waypoint rover route from Jezero Crater Floor (`18.38°N, 77.58°E`) to the Nili Carbonate Highlands (`18.72°N, 76.95°E`) scored against live USGS 128 ppd MOLA DEM slopes.
  4. **Round 4 (~5m · 150 pts) - Artemis Lunar South Pole Siting:** Open the Lunar Base Site Planner (`85°S-90°S`) and click real NASA LRO LOLA slope, solar illumination, Earth radio visibility, and PSR water-ice rasters to engineer and score an Artemis landing site.
  5. **Round 5 (~5m · 180 pts) - Europa Ocean World Finale:** Auto-unlocks after scoring in 3 trails (or 15 minutes) to scan Conamara Chaos (`8.85°N, 86.15°E`), activate the 9 m/px Galileo SSI mosaic, and map fractured ice rafts or inspect the 3D Globe.
- **AI CapCom (`✨ AI CapCom`):** Provides live telemetry-grounded Socratic guidance out of the box, with optional local-session Gemini API key support (`gemini-3.8-flash` via `/v1beta/interactions`).
- **Live Leaderboard & Winner's Circle:** Click **`🏆 Leaderboard`** inside the side panel or open the standalone 1080p/4K projector dashboard at `https://jsmars.sounny.com/spacecamp/leaderboard/` (`Live Today` vs `All-Time` filters, Top 3 Podium, and Specialist Mission Commendations).

