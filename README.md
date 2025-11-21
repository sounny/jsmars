# JSMARS (JMARS for the Web)

> **A lightweight, browser-based GIS viewer for planetary data, inspired by [JMARS](https://jmars.asu.edu/).**

## 🔭 Overview
**JSMARS** is a web-based port of the popular Java Mission-planning and Analysis for Remote Sensing (JMARS) desktop application. It aims to provide a quick, accessible way to view and analyze planetary data (Mars, Moon, etc.) directly in the browser without requiring a heavy desktop installation.

This project adheres to a **"No-Build"** philosophy: it uses standard ES Modules and Vanilla JavaScript. 

## ✨ Key Features
- **Planetary Maps**: View high-resolution Mars basemaps (Viking MDIM 2.1).
- **WMS Integration**: Dynamically fetches layers from USGS and OpenPlanetary WMS servers.
- **Layer Management**: Toggle layers, adjust opacity, and reorder (coming soon).
- **Vector Tools**: Draw shapes (Polygons, Rectangles, Lines, Markers) using the integrated toolbar.
- **Lightweight**: No Webpack, no Vite, no `npm install` required to run.


## 🗺️ Roadmap
We are actively developing JSMARS to match core JMARS desktop features:
- [x] **Phase 1**: WMS Layer Support & Basic Map
- [x] **Phase 2**: UI Polish (Loading indicators, Layer Manager)
- [ ] **Phase 3**: Advanced Vector Support (Import/Export Shapes, Measurements)
- [ ] **Phase 4**: Search, Bookmarks, and Session Saving
- [ ] **Phase 5**: Multi-body support (Moon, Earth, etc.)

See [docs/jsmars-roadmap.md](docs/jsmars-roadmap.md) for the detailed plan.

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
