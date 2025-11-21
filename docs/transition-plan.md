# Transition Plan: From JMARS Desktop to JSMARS Web

This plan describes how to adapt core JMARS concepts, data sources, and workflows into a browser-native experience.

## Guiding Principles
- **Service Parity First**: Reuse existing WMS/XYZ endpoints.
- **Progressive Enhancement**: Ship vertical slices (data → rendering → interaction).
- **Client-side Resilience**: Handle network errors gracefully.
- **Modular Growth**: Keep architecture pluggable (ES modules, no-build).

## Concept Mapping

| JMARS Desktop Concept | JSMARS Web Implementation | Notes |
|-----------------------|---------------------------|-------|
| **MapServer / MapSource** | `jmars-config.js` Services | Configurable base URLs, CRS, auth. |
| **Layer Manager** | `LayerManager` UI Component | Drag-and-drop ordering, opacity sliders, grouping. |
| **Main / Panner Views** | Main Map / Panner Widget | Linked viewports. Panner toggleable via UI. |
| **M / P / 3D Toggles** | Overlay Toggles | Checkboxes for Main/Panner visibility per layer. |
| **Grid Layer** | Leaflet Graticule Plugin | Configurable spacing/color via focus panel. |
| **Shape Layer** | `JMARSVectors` (Leaflet.Draw) | FeatureGroup for user shapes. Import/Export support. |
| **ROI (Region of Interest)** | GeoJSON Features | Extended properties (name, style) in feature table. |
| **Save Session** | JSON Export | Serialize state (layers, view, shapes) to client-side JSON. |
| **Load Session** | JSON Import | File reader API to restore state. |

## Technical Steps

### 1. Service Configuration
- Define service descriptor schema.
- Add helpers to fetch capabilities and validate versions.

### 2. Map Rendering
- Wrap Leaflet tile layers for WMS.
- Implement request scheduler/debouncing.

### 3. Layer Management
- **Ordering**: Implement drag-and-drop logic.
- **Active Layer**: Track which layer receives draw events.

### 4. Vector Overlays & ROIs
- **Storage**: In-memory `FeatureGroup` initially, then `localStorage`.
- **IO**: Client-side parsers for Shapefile/CSV.

### 5. Interaction Model
- **Tools**: Toolbar for Select, Info, Measure, Draw.
- **Panels**: Context panels for layer settings (Grid config, etc.).

### 6. Multi-body & Coordinates
- **Projections**: Handle 0-360 vs -180-180 logic (`src/util/geo.js`).
- **Switching**: Reset map state when body changes.

## Risk Mitigation
- **Service Instability**: Fallback endpoints, clear error UI.
- **Projection Mismatches**: Validate CRS compatibility early.
- **Performance**: Limit default ROI counts, use simplification for complex polygons.
- **Data Persistence**: Warn users that `localStorage` is ephemeral; encourage JSON export.
