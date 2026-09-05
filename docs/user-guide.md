# JSMARS User Guide

Welcome to JSMARS, a web-based planetary GIS viewer.

## Getting Started
1. Open `index.html` in your browser.
2. The map will load centered on Mars (Lat 0, Lon 0).

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
4. Click **Calculate** to solve the 1D heat diffusion equation.
5. Toggle between **Diurnal** temperature curves ($T(t)$), **Depth** subsurface profiles ($T(z)$), and **Seasonal** curves ($T(L_s)$).
6. Export simulation tables via **Export CSV** or save charts via **PNG**.

### 3D Terrain & Globe Viewer
1. Open the **3D Terrain & Globe** section under Tools.
2. Switch between **3D Terrain** (regional mesh elevated by MOLA DEM) and **3D Globe**.
3. Drag to rotate and pitch the 3D camera; scroll to zoom in and out.
4. Adjust the **Vertical Exaggeration** slider ($1\times - 20\times$) to accentuate topographic relief.
5. Use the **Sun Hour Angle** slider to simulate changing shadow and illumination angles.

### Mars Climate Database (MCD) Profiler
1. Open the **MCD Atmospheric Profiler** section under Tools.
2. Pick a location or specify coordinates, elevation, and local solar hour.
3. Click **Calculate Profile** to generate vertical profiles up to 50 km altitude.
4. Toggle between **Temp** $T(z)$, **Pressure** $P(z)$ (log scale), and **Wind** speed curves.

### Crater Counting & CSFD Isochron Dating
1. Under **Crater Counting**, click **Start Crater Counting**.
2. Resize the ghost circle cursor using the mouse scroll wheel, and click to digitize craters.
3. The integrated **CSFD Chart** plots cumulative size-frequency distribution ($N(>D)/\text{km}^2$) against Hartmann & Neukum isochron models ($10\text{ Ma} - 4.3\text{ Ga}$).
4. The system calculates model surface age and geological epoch (Amazonian / Hesperian / Noachian).

### Spectral Band Math & Mineralogy
1. Open the **Spectral Band Math** section under Tools.
2. Select a preset (e.g. **BD530 Ferric Iron**, **BD1900 Hydrated Clays**, **BD1500 Water Ice**, **THEMIS Olivine**) or enter a custom formula.
3. Choose a colormap (**Viridis**, **Magma**, **Coolwarm**, **Jet**, **Rainbow**) and adjust color stretch.
4. Click **Apply Color Stretch** to evaluate mineral indices on the active layer.

### Subsurface Radar Sounder (SHARAD / MARSIS)
1. Open the **Subsurface Radar Sounder** section under Tools.
2. Choose a sounder ground track across Planum Boreum, Planum Australe, Medusae Fossae, or Utopia Planitia.
3. Configure target dielectric permittivity ($\varepsilon_r$) and loss tangent ($\tan\delta$).
4. Toggle between the **2D B-Scope Radargram** cross-section and the **1D A-Scope Power Trace** to detect subsurface interfaces and ice stratigraphy.
5. Export radargram data tables via **Export Radargram CSV**.

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

## Session Management

### Saving and Loading Sessions
1. **Save Session:** Press `Ctrl+S` or use **File → Save Session** to capture your current view, layers, layer visibility, bookmarks, and measurements.
2. **Load Session:** Press `Ctrl+O` or use **File → Load Session** to restore a previously saved JSON session file.
3. **Session Contents:** Sessions preserve:
   - Active body (Mars, Moon, Earth)
   - All active layers with opacity and visibility state
   - Current map viewport (center coordinates and zoom level)
   - Measurements and crater counts
   - Bookmarks with navigation coordinates

### Cross-Body Sessions
- **Save on Mars, Load on Moon:** Sessions can be created on one body and loaded while another body is active. For example, save a Mars observation session, switch to the Moon, and load that session; the map switches back to Mars before restoring the saved layers and viewport.
- **Viewport Accuracy:** Saved sessions capture the exact map center and zoom level at save time, ensuring your view is restored precisely.
- **Bookmark Navigation:** Bookmarks automatically include their source body. Click a bookmark's **Go to** button to:
  1. Switch to the bookmark's body (Mars → Moon, etc.)
  2. Wait for the map to initialize the new body
  3. Pan to the bookmark's saved coordinates

### Layer Visibility
- **Toggle Visibility:** Use the eye icon next to each active layer to toggle visibility on/off.
- **Visibility Persistence:** Layer visibility state is saved with sessions and serialized in shareable URLs.
- **Visible Layers Only:** Only visible layers are included in map exports. Measurements are drawn geometries tracked independently of layer visibility.

### Shareable Links
- Deep-link URLs (containing `?body=mars&layers=...`) remember your current body, zoom level, and active layers.
- Copy the URL from your browser address bar to share your exact map view with colleagues.
- Bookmarks can also include planetary landmarks or saved coordinates for easy team sharing.

## Troubleshooting
- **"Loading map data..." stuck?** The map server might be slow or down. Try refreshing the page.
- **Missing Layers?** Check your internet connection; layers are fetched dynamically from USGS/OpenPlanetary.
- **Session restore shows wrong body?** Ensure your saved session was for the body you're trying to load on. Cross-body sessions work correctly and will switch your map automatically.
- **Bookmark goes to wrong location?** Verify the bookmark was created on the target body. Use the bookmark's body label to confirm before navigating.
