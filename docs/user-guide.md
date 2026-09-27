# JSMARS User Guide

Welcome to JSMARS, a web-based planetary GIS viewer.

## Getting Started
1. Open `index.html` in your browser.
2. The map will load centered on Mars (Lat 0, Lon 0). Use the planetary body selector at the top of the sidebar to seamlessly switch between **Mars**, the **Moon**, **Earth**, and **Europa**.

## Managing Layers
- **Add/Remove:** Use the buttons in the "Layer Manager" panel (top-right) to add layers from the available list or remove active layers.
- **Find Layers Faster:** Use the **Filter available layers** search box to narrow long layer lists by name or ID.
- **Opacity:** Use the slider below each active layer name to adjust transparency.
- **Settings:** Double-click an active layer, press **Enter/Space** when the layer item is focused, or use the **⚙ settings button** to open its settings panel with metadata, attribution, and a quick opacity control.


## Keyboard Shortcuts & Navigation
- **Zoom In / Out:** Press `+` or `-` (or use numeric keys `1` to `5` for quick zoom levels).
- **Reset View:** Press `R` to return to the global overview.
- **Go to Coordinates:** Press `Ctrl+G` to jump to any latitude/longitude coordinate.
- **Save & Load Sessions:** Press `Ctrl+S` to save your active session to JSON, or `Ctrl+O` to load a saved session.
- **Export Map:** Press `Ctrl+Shift+E` to download a high-resolution PNG export.

## Mars Solar Time & Calendar ($L_s$)
- The **Mars Solar Time** slider in the sidebar allows you to scrub through the Martian orbit ($L_s$ from 0° to 360°), Mars Sol Date (MSD), and Mars Year (MY).
- Click **Play** to animate solar progression across seasons.
- View real-time subsolar latitude, Mars-Sun distance (AU), and top-of-atmosphere solar insolation ($W/m^2$).

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
2. The globe uses the body's reference ellipsoid and drapes the imagery layer you have turned on (USGS, OpenPlanetary, NASA GIBS, or NASA Trek). **Elevation terrain is not loaded** — Olympus Mons will not stand above the surrounding plains. That is planned. The on-globe line names the imagery and the ellipsoid.
3. Drag to orbit, scroll or use **+ / −** to zoom, and **Spin** to rotate. **Grid** draws a latitude/longitude overlay. **Light** is a visual terminator (local hour), not a measured ephemeris. **Reset** returns to the body's overview.
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

## Drawing Shapes (ROIs)
1. Look for the **Shapes** section or the toolbar on the map.
2. Select a tool: Point, Line, Polygon, Circle, Rectangle.
3. Edit attributes in the shape table, change fill/stroke colors, and import/export GeoJSON, CSV, KML, or WKT (Well-Known Text) files.

## Navigation
- **Pan:** Click and drag the map.
- **Zoom:** Use the +/- buttons or your mouse wheel.
- **Coordinates:** View current Latitude/Longitude in the bottom-left corner. Click the format button to cycle between $E180$, $E360$, and $DMS$.
- **Map Viewpoints:** Use the Map Options panel to jump between Global Equirectangular, North Polar (Planum Boreum), and South Polar (Planum Australe) views.
- **North Arrow & Planetary Scale Bar:** View real-time physical scale bars calibrated to true planetary radii ($R_{\text{Mars}} = 3389.5\text{ km}$, $R_{\text{Moon}} = 1737.4\text{ km}$) with cosine latitude distortion.

## Troubleshooting
- **"Loading map data..." stuck?** The map server might be slow or down. Try refreshing the page.
- **Missing Layers?** Check your internet connection; layers are fetched dynamically from USGS/OpenPlanetary.
