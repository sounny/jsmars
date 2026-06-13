# jsMars Architecture

This document describes the architecture and design patterns used in jsMars,
a browser-based planetary GIS application inspired by the JMARS Java desktop app.

## Technology Stack

- **Framework**: None (vanilla JS with ES Modules)
- **Map Library**: Leaflet.js 1.9.4 (CDN)
- **Drawing**: Leaflet.Draw 1.0.4 (CDN)
- **Projection**: EPSG:4326 (Plate Carree, standard for planetary WMS)
- **Styling**: Vanilla CSS (dark theme)
- **Hosting**: GitHub Pages (static files only, no build step)
- **Data Format**: WMS, GeoJSON, CSV, KML

## Directory Structure

```text
/index.html                     # Entry point
/style.css                      # All styles
/jsmars_logo.svg                # Logo
/about.html                     # About page
/src/
  constants.js                  # Event name constants
  jmars-config.js               # Bodies, WMS endpoints, mosaics
  jmars-map.js                  # Core Leaflet map wrapper
  jmars-state.js                # Lightweight state store (singleton)
  jmars-vectors.js              # Vector layer init (Leaflet.Draw)
  jmars-wms.js                  # WMS GetCapabilities parser
  core/
    EventBus.js                 # Centralized event dispatch
    ToolManager.js              # Mutual-exclusion tool activation
    PluginRegistry.js           # Plugin registration and init
  data/
    landing-sites.json          # Mars + Moon landing site coordinates
  features/
    bookmarks/BookmarksTool.js
    crater-counting/            # CraterLayer, CraterTable
    export/ExportTool.js        # PNG/JPEG/world file export
    groundtrack/                # GroundTrackLayer, GroundTrackPanel
    investigate/InvestigateTool.js
    landing/LandingSitesLayer.js
    measure/MeasureTool.js, MeasurementTable.js
    nomenclature/NomenclatureTool.js
    places/PlacesManager.js     # Bookmark places, search
    profile/                    # RadialProfileTool, EnhancedProfileTool
    sampling/SamplingTool.js, SampleTable.js
    shapes/                     # ShapeLayer, ShapeTable, ShapeIO, StyleEditor
    stamp/                      # StampLayer, StampQueryPanel (ODE API)
  layers/
    index.js                    # Layer registry + createLeafletLayer()
    GraticuleLayer.js           # Lat/lon grid overlay
  ui/
    Accordion.js                # Collapsible sidebar sections
    BodySelector.js             # Planet/body dropdown
    CollapsibleGroup.js         # Tool section collapse
    FixedOverlays.js            # Grid, scalebar, north arrow, panner
    layer-manager.js            # Layer list with drag-reorder
    QuickActions.js             # Ctrl+K palette
    SearchBar.js                # Location search
    SessionManager.js           # JSON session save/load
    Sidebar.js                  # Sidebar collapse/expand
    StatusBar.js                # Lat/lon readout + coordinates
  util/
    geo.js                      # Haversine, azimuth, DMS, body constants
    mola-dem.js                 # MOLA DEM elevation queries
/docs/
  ARCHITECTURE.md               # This file
  jsmars-roadmap.md             # Development plan
  transition-plan.md            # Java -> Web migration notes
  release-notes.md              # Changelog
  user-guide.md                 # User instructions
/jmars/                         # REFERENCE ONLY (Java source, DO NOT MODIFY)
```

## Event-Driven Architecture

All cross-module communication uses `document.dispatchEvent(new CustomEvent(...))`.
Event names are centralized in `src/constants.js` under the `EVENTS` object.

### Key Events

| Event | Payload | Description |
|-------|---------|-------------|
| `jmars:body-changed` | `{body: 'mars'}` | User switched planetary body |
| `layers-changed` | `[{id, opacity, visible}]` | Active layer list changed |
| `jmars-layers-updated` | `[layerConfig]` | Available layers discovered |
| `jmars:shape-created` | `{id, layer, type, attributes}` | Shape drawn on map |
| `jmars:stamp-query-complete` | `{instrument, count, results}` | Stamp search finished |
| `jmars:landing-sites-toggled` | `{active: true}` | Landing sites shown/hidden |

## Adding a New Feature

1. Create a new directory under `src/features/your-feature/`
2. Create the main module (e.g., `YourFeature.js`) with `activate()` / `deactivate()` methods
3. Add event constants to `src/constants.js`
4. Add a UI section in `index.html` inside the Tools accordion
5. Import and initialize in the `<script type="module">` block in `index.html`
6. Add CSS styles at the end of `style.css`

## Data Sources

| Source | Protocol | Used For |
|--------|----------|----------|
| USGS Astrogeology | WMS | Mars/Moon base maps |
| OpenPlanetary | XYZ tiles | Mars/Moon basemaps |
| NASA GIBS | WMS | Earth imagery |
| USGS ODE REST API | HTTP/JSON | Instrument footprints (stamps) |
| USGS MOLA DEM | WMS/WCS | Elevation profiles |
| IAU Nomenclature | Static JSON | Feature names |

## State Management

`jmarsState` (singleton `JMARSState`) holds:
- `body`: Current planetary body
- `activeLayers`: Ordered list of active map layers
- `overlays`: Toggle states for grid, scalebar, etc.
- `view`: Current lat/lon/zoom

State changes emit events that UI components subscribe to.
Sessions are serialized as JSON files for save/load.
