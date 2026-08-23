# JSMARS (JMARS for the Web)

> **A lightweight, browser-based GIS viewer for planetary data, inspired by [JMARS](https://jmars.asu.edu/).**

## 🔭 Overview
**JSMARS** is a web-based port of the popular Java Mission-planning and Analysis for Remote Sensing (JMARS) desktop application. It aims to provide a quick, accessible way to view and analyze planetary data (Mars, Moon, etc.) directly in the browser without requiring a heavy desktop installation.

This project adheres to a **"No-Build"** philosophy: it uses standard ES Modules and Vanilla JavaScript. 

## Key Features
- **Multi-Body Maps**: View Mars, Moon, and Earth with WMS-discovered layers
- **WMS Integration**: Auto-discovers layers from USGS, OpenPlanetary, and NASA GIBS
- **Layer Management**: Toggle, reorder (drag), opacity, settings modal, filter
- **Mars Time & Calendar**: Real-time Solar Longitude ($L_s$), Mars Sol Date (MSD), Mars Year (MY), and time scrubbing
- **KRC Mars 1D Thermal Model**: Diurnal surface/subsurface thermal simulation, seasonal temperature curves, CO2 frost detection
- **3D Terrain & Globe Viewer**: Interactive WebGL 3D terrain displaced by MOLA DEM with solar lighting and vertical exaggeration
- **MCD Atmospheric Profiler**: Vertical profiles of temperature, pressure, density, dust, and wind up to 50 km
- **Crater Counting & CSFD Dating**: Interactive crater digitization with log-log CSFD plots, Hartmann & Neukum isochrons, and model ages
- **Spectral Band Math & Mineralogy**: CRISM/THEMIS mineral parameter indices (BD530, BD1900, BD1500, D2300, Olivine) with colormap stretch
- **Stamp Layer**: Browse THEMIS, CTX, HiRISE, MOC, CRISM footprints via USGS ODE REST API
- **Advanced Shapes**: Draw (point, line, polygon, circle, rectangle), edit attributes, style with presets
- **Import/Export**: GeoJSON, CSV, KML file support with drag-and-drop import
- **Elevation Profiles**: Linear and radial profiles from MOLA DEM
- **Measurements**: Distance, area with azimuth and great-circle calculations
- **Landing Sites**: Mars rovers/landers + Moon landing sites with mission info
- **Ground Tracks**: Spacecraft orbit visualization (MRO, Odyssey, MAVEN, etc.)
- **Map Projections**: Equirectangular global view, North Polar (Planum Boreum) and South Polar (Planum Australe) viewpoints
- **Map Export**: PNG, JPEG, and georeferenced world file (.pgw/.jgw)
- **Nomenclature**: IAU feature labels (craters, mons, valles, planitia)
- **Investigate & Sampling**: WMS GetFeatureInfo point queries and area sampling
- **Session Save/Load**: Full state persistence as JSON
- **Lightweight**: No Webpack, no Vite, no `npm install`. Runs on GitHub Pages.

## Reference Source
The original Java-based JMARS application source code is available in the `jmars/` directory at the repository root as the reference truth.

## Roadmap
- [x] **Phase 1**: WMS Layer Support, Basic Map
- [x] **Phase 2**: UI Polish, Layer Manager, Accordion sidebar
- [x] **Phase 3**: Multi-body support (Mars, Moon, Earth)
- [x] **Phase 4**: Crater Counting, Profiles, Measurements, Nomenclature
- [x] **Phase 5**: Search, Bookmarks, Session Saving, Sampling
- [x] **Phase 6 (v0.5.0)**: Stamp Layer, Shapes, Landing Sites, Ground Track, Export
- [x] **Phase 7 (v0.6.0)**: KRC 1D Thermal Model, Mars Time & $L_s$ Slider, 3D Terrain & Globe Viewer, MCD Atmospheric Profiler, CSFD Isochron Crater Dating, Spectral Band Math, Polar Projections

See [docs/jsmars-roadmap.md](docs/jsmars-roadmap.md) for the detailed plan.
See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for project architecture.


## 🤝 Contributing
Please read [AGENTS.md](AGENTS.md) for architectural guidelines and coding standards.
- **Main Branch**: `main`
- **Tech Stack**: Leaflet.js, Vanilla JS, CSS.

## 📄 License
This project is open source and intended to be compatible with the JMARS GPLv3 license.

## 🙏 Acknowledgements
- **[JMARS Team (ASU)](https://jmars.asu.edu/)**: For the original desktop application and inspiration.
- **[USGS Astrogeology](https://astrogeology.usgs.gov/)**: For providing WMS map services.
- **[OpenPlanetary](https://www.openplanetary.org/)**: For community resources and basemaps.
