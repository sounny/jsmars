# Transition Plan: From JMARS Desktop to JSMARS Web

This plan describes how to adapt core JMARS concepts, data sources, and workflows into a browser-native experience.

## Guiding Principles
- **Service parity first:** Reuse the same WMS/WFS/HTTP endpoints where possible to avoid reimplementing data pipelines.
- **Progressive delivery:** Ship thin vertical slices (data → rendering → interaction) instead of rebuilding whole subsystems at once.
- **Client-side resilience:** Expect intermittent network conditions; make error handling and retry strategies visible to users.
- **Modular growth:** Keep services, layers, and tools pluggable so we can swap providers or add new bodies beyond Mars.

## Concept Mapping
- **MapServer / MapSource:** Represent as configurable service definitions in `jmars-config.js`, each with base URL, supported CRS, and authentication info if needed.
- **Layers:** Normalize WMS layer metadata into a registry; include title, abstracts, attribution, default styles, and scale hints.
- **Bodies:** Model planetary bodies as configs with radii, default projections, and default center points.
- **Regions of Interest (ROIs):** Implement as GeoJSON features with metadata fields that mirror JMARS attributes (name, notes, style, visibility, group).

## Technical Steps
1. **Service configuration**
   - Define a service descriptor schema (`id`, `type`, `baseUrl`, `version`, `supportedCrs`, `defaultParams`).
   - Add helpers to fetch capabilities and validate versions (WMS 1.1.1 vs 1.3.0).
2. **Map rendering layer**
   - Wrap Leaflet tile layers for WMS GetMap requests; handle pixel ratio and projection differences.
   - Add a request scheduler to avoid flooding servers when panning/zooming quickly.
3. **Layer management**
   - Build a registry that merges capabilities info with local overrides (human-friendly names, default visibility).
   - Provide consistent layer IDs so UI components can toggle or reorder layers without relying on server titles.
4. **Vector overlays and ROIs**
   - Use Leaflet draw (or a minimal custom implementation) to create/edit shapes.
   - Persist shapes in `localStorage` initially; abstract storage so a future sync endpoint can drop in.
5. **Interaction model**
   - Mirror JMARS toolbar ideas with a lightweight control strip: selection, info, measure, and draw.
   - Provide context panels for layer info and feature inspection; keep the layout responsive.
6. **Performance and caching**
   - Enable browser caching headers where supported; add client-side memoization for capabilities.
   - Debounce coordinate readouts and network calls tied to map movements.

## Risk Mitigation
- **Service instability:** Offer fallback endpoints and surface server errors clearly.
- **Projection mismatches:** Validate CRS from capabilities and reject unsupported combinations early.
- **Data volume:** Start with lower-resolution layers and allow users to opt into high-resolution requests.
- **Licensing:** Track attributions and licensing notes per layer to maintain GPLv3 compatibility.

## Migration Milestones
- **M1:** Config-driven base map loads from a JMARS WMS endpoint.
- **M2:** Layer list with toggle/reorder and persisted selections.
- **M3:** ROI drawing/editing with local storage persistence and export/import.
- **M4:** Measurement tools and layer info panels matching JMARS expectations.
- **M5:** Hardened performance, accessibility audits, and documented extension points.

Keep this plan updated as we learn from user feedback and upstream service constraints.
