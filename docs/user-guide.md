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


## Quick Actions
- Use the **Quick Command** box below the logo to run common actions quickly.
- Supported commands include: `Save Session`, `Load Session`, `Reset View`, `Open Layer Manager`, `Open Tools`, and `Toggle Sidebar`.
- Press **Ctrl+K** (or **⌘+K** on macOS) to focus the command input from anywhere in the app.

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

## Drawing Shapes (ROIs)
1. Look for the **Shapes** section or the toolbar on the map.
2. Select a tool: Point, Line, Polygon, Circle, Rectangle.
3. Edit attributes in the shape table, change fill/stroke colors, and import/export GeoJSON, CSV, or KML files.

## Navigation
- **Pan:** Click and drag the map.
- **Zoom:** Use the +/- buttons or your mouse wheel.
- **Coordinates:** View current Latitude/Longitude in the bottom-left corner. Click the format button to cycle between $E180$, $E360$, and $DMS$.
- **Map Viewpoints:** Use the Map Options panel to jump between Global Equirectangular, North Polar (Planum Boreum), and South Polar (Planum Australe) views.
- **North Arrow:** Use the sidebar toggle under "Fixed Overlays" to show/hide the compass rose. Click "Reset view" inside the control to recenter on the default position.

## Troubleshooting
- **"Loading map data..." stuck?** The map server might be slow or down. Try refreshing the page.
- **Missing Layers?** Check your internet connection; layers are fetched dynamically from USGS/OpenPlanetary.
