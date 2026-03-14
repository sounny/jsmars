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

## Drawing Shapes (ROIs)
1. Look for the toolbar on the left side of the map.
2. Select a tool:
   - **Polygon:** Click multiple points to define an area. Click the first point to close.
   - **Rectangle:** Click and drag to draw a box.
   - **Marker:** Click a point to place a pin.
3. Shapes are currently temporary and will disappear if you reload the page.

## Navigation
- **Pan:** Click and drag the map.
- **Zoom:** Use the +/- buttons or your mouse wheel.
- **Coordinates:** View current Latitude/Longitude in the bottom-left corner.
- **North Arrow:** Use the sidebar toggle under "Fixed Overlays" to show/hide the compass rose. Click "Reset view" inside the control to recenter on the default JMARS position.

## Troubleshooting
- **"Loading map data..." stuck?** The map server might be slow or down. Try refreshing the page.
- **Missing Layers?** Check your internet connection; layers are fetched dynamically from USGS/OpenPlanetary.
