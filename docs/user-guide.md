# JSMARS User Guide

Welcome to JSMARS, a web-based planetary GIS viewer.

## Getting Started
1. Open `index.html` in your browser.
2. The map will load centered on Mars (Lat 0, Lon 0).

## Managing Layers
- **Toggle:** Use the checkboxes in the "Layer Manager" panel (top-right) to show/hide layers.
- **Opacity:** Use the slider below each layer name to adjust transparency.

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

## Troubleshooting
- **"Loading map data..." stuck?** The map server might be slow or down. Try refreshing the page.
- **Missing Layers?** Check your internet connection; layers are fetched dynamically from USGS/OpenPlanetary.
