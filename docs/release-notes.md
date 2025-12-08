# JSMARS Release Notes

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
