# Lunar Base Site Planner

A JSMARS workspace for siting a base at the lunar south pole using real NASA LRO LOLA data. Open it from the sidebar section **Lunar Base Site Planner → Open Site Planner**. It opens a full-screen polar stereographic view, separate from the main map.

## What is in it

| Need | Tool | Data behind it |
|---|---|---|
| Illumination, % of time lit | Thematic layer **Avg solar visibility**, Inspect, CAD table | Mazarico et al. (2011), PGDA `AVGVISIB_85S_060M`, 60 m |
| Earth visibility (DTE comms) | **Avg Earth visibility** layer, Inspect | Mazarico et al. (2011), PGDA `AVGVISIB_85S_060M_EARTH`, 60 m |
| Permanent shadow, distance to ice access | **Permanently shadowed regions** layer; Inspect reports distance to nearest PSR | `LPSR_85S_060M`, exact Euclidean distance on 120 m grid (±0.12 km) |
| Slope hazard with your thresholds | **Slope hazard** layer with editable caution / limit degrees (defaults 10° / 20°) | Barker et al. (2021) 5 m slope, max and mean per 20 m cell |
| Surface roughness | **Roughness proxy (DEM)** | RMS residual of the 20 m DEM from a local plane. A proxy only, not boulder density |
| Level ground for the landing pad | **Flat-area finder** | Largest discs where every 20 m cell's 5 m max slope is under your threshold |
| Pad-to-habitat corridor section | **Corridor** mode, profile chart, CSV export | 20 m site DEM, 120 m regional DEM outside a site |
| Blast safety buffers | **Place pad** with your own buffer radii; reports CAD features and corridor segments inside each ring | No radii are bundled (see below) |
| Solar orientation / mast siting | **Horizon** mode: terrain horizon vs. the full range of possible Sun elevations | Site + regional DEMs to 150 km, lunar curvature included |
| Site plan import | **Site plan import (DXF / SVG)** then **Place CAD** | Your drawing |
| Terrain to Rhino / Isaac Sim | **Export**: OBJ, GeoTIFF (elevation, max slope), plan DXF in the same frame, corridor CSV, plan JSON | 20 m bundled DEM, or the live LOLA 5 m mosaic (87°S–90°S) |

Bundled sites use PGDA's own names and IDs: Connecting ridge (Site01), Shackleton rim (Site04), Nobile rim 1 (Site06), Peak near Shackleton (Site07), de Gerlache rim (Site11), Malapert massif (Site23), de Gerlache-Kocher massif (Site42), de Gerlache rim (SL2), Connecting ridge extension (SL3), Nobile rim 2 (DM2). The planner does not claim that any of these is identical to a NASA Artemis III candidate region boundary; compare against NASA's published region outlines yourself.

## Rhino / Grasshopper workflow

1. Choose a site, then **Export window** and click two corners (or skip to export the whole site).
2. Pick terrain source and mesh density, then **Terrain OBJ**. You get the `.obj` plus a `_frame.txt` describing the local frame.
3. **Site plan DXF (same frame)** writes your corridor, pads, buffers and placed CAD as 3D polylines draped on the terrain, in the same frame.
4. In Rhino: import the OBJ (units: metres, Z-up by default), then the DXF. They align without moving anything.
5. Frame: origin at window centre, +X east, +Y north (away from the pole), +Z up, true ground metres.

DXF import: export ASCII DXF from Rhino (File › Export › .dxf). Supported entities: LINE, LWPOLYLINE (with arcs), POLYLINE, CIRCLE, ARC, ELLIPSE, SPLINE (approximated), POINT and block INSERTs. `$INSUNITS` is read when present; otherwise pick the unit. Drawing +Y is aligned to local north at the anchor point.

## Isaac Sim

Use **OBJ Y-up** if your import pipeline expects Y-up. OBJ is in metres. For large windows use decimation, or export a GeoTIFF and build the mesh in your own pipeline.

## Honest limitations

- **Longest continuous dark period** is not a published LOLA raster product, so it is not included. PGDA provides it on request.
- **Boulder density** is not included. The roughness layer is a DEM proxy only.
- **Blast buffer radii** must come from your lander provider or programme. There is no single published standard, so no presets ship with the tool.
- Illumination and Earth visibility are long-term averages at the surface, not at mast height. The Horizon tool is a geometric analysis, not an illumination time series.
- Slope accuracy: LOLA 5 m slope RMS error is roughly 1.5–2.5° (Barker et al. 2021).
- Nobile rim 2 (DM2) lies partly outside the 85°S regional illumination coverage; values there show as no data.

## Data sources

- Barker, M.K., et al. (2021). Improved LOLA elevation maps for south pole landing sites. *Planet. Space Sci.* 203, 105119. doi:10.1016/j.pss.2020.105119
- Mazarico, E., et al. (2011). Illumination conditions of the lunar polar regions using LOLA topography. *Icarus* 211, 1066–1081. doi:10.1016/j.icarus.2010.10.030
- Smith, D.E., et al. (2010). LRO LOLA Gridded Data Records, NASA PDS (`LDEM_85S_40M`).
- Live 5 m terrain: LOLA 87°S 5 m mosaic (Barker et al. 2021), USGS-hosted Cloud-Optimized GeoTIFF.

## Rebuilding the bundled data

```
python scripts/build_lunar_south_pole_assets.py
```

Requires Python with `numpy` and `tifffile`. Downloads are cached in `~/.cache/jsmars-lunar`. Output goes to `assets/lunar-south-pole/` with a `manifest.json` recording sources, sha256 hashes and limitations. The service worker never caches these rasters.

## Contributing site criteria

Planners are welcome to send a pull request adding a `docs/lunar-site-criteria/<name>.md` describing their criteria, thresholds and data needs. Suggested headings: Programme context, Criteria and thresholds, Data source per criterion, What the planner should compute, Export formats needed.
