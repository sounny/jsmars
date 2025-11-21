# Work Log

This log captures the reasoning and actions taken while expanding the agent guidance and sketching plans for turning JMARS into JSMARS.

## 2025-11-21 Session Notes
- Reviewed the existing agent brief (then named `AGNETS.md`) to understand project intent and scope.
- Renamed the briefing to `AGENTS.md` for clarity and expanded it with architecture details, coding conventions, and near-term milestones.
- Established a `/docs` directory to hold planning materials and ongoing logs.
- Authored the initial roadmap and transition plan to outline how the JS client should grow toward JMARS parity.
- Documented this session to keep future contributors aware of decisions and next steps.

## 2025-11-21 Session Notes (Update)
- Refactored layer management logic into a dedicated `src/ui/layer-manager.js` module.
- Added opacity sliders to each layer item in the UI.
- Implemented `setLayerOpacity` in `src/jmars-map.js` to handle Leaflet layer opacity updates.
- Verified functionality with new Playwright test `verification/verify_opacity.py`.

## 2025-11-22 Session Notes
- Moved the app entry point to the repository root (`/index.html`) so the homepage loads directly from the repo base.
- Reviewed the existing map/view code: layer toggling and opacity controls are in place, but there is no ordering, search, or error surface beyond console logs.
- Noted that configuration is still Mars-only and does not yet expose multiple planetary bodies or authenticated services.

### Missing JMARS features to queue up
- [ ] Layer reordering and grouping to match JMARS' stack management and composite order controls.
- [ ] ROI/shapes tooling (draw, edit, style presets) with import/export of JMARS ROI formats and GeoJSON.
- [ ] Measurement utilities (distance, area, elevation sampling) that mirror JMARS analysis tools.
- [ ] Saved sessions/workspaces so users can persist layer selections, map extents, and annotations between visits.
- [ ] Enhanced search (places, features, layers) akin to JMARS' search windows, including jump-to coordinates.
- [ ] Time-aware layers and profile plotting for instruments with temporal coverage.
- [ ] Multi-body support with distinct projections and defaults (e.g., Mars, Moon) instead of Mars-only configs.
- [ ] 3D/globe-style visualization pathway to approximate JMARS' 3D view modes.
