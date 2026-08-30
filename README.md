<div align="center">

# JSMARS
### Zero-Install Planetary GIS in Your Browser & in Your Pocket

[![Live Demo](https://img.shields.io/badge/Live_Demo-jsmars.sounny.com-blue?style=for-the-badge&logo=google-chrome&logoColor=white)](https://jsmars.sounny.com)
[![PWA Ready](https://img.shields.io/badge/PWA-1--Click_Install-00f2fe?style=for-the-badge&logo=pwa&logoColor=white)](https://jsmars.sounny.com)
[![Architecture](https://img.shields.io/badge/Architecture-Zero--Build_ESM-brightgreen?style=for-the-badge&logo=javascript&logoColor=white)](docs/ARCHITECTURE.md)
[![License: GPL v3](https://img.shields.io/badge/License-GPLv3-orange.svg?style=for-the-badge)](https://www.gnu.org/licenses/gpl-3.0)
[![LinkedIn](https://img.shields.io/badge/LinkedIn-Follow_JSMARS-0077B5?style=for-the-badge&logo=linkedin&logoColor=white)](https://www.linkedin.com/company/jsmars/)

<p align="center">
  <b>A lightweight, browser-native WebGIS suite for planetary exploration, remote sensing, and scientific modeling.</b><br>
  Inspired by ASU's desktop <a href="https://jmars.asu.edu/">JMARS</a>. Built on 100% pure web standards.
</p>

</div>

---

## 🚀 Why JSMARS?

For decades, planetary GIS required downloading 2GB desktop installers, configuring complex Java (JRE) runtime environments, and remaining tethered to a desktop workstation.

**JSMARS re-imagines planetary science for the modern web:**
- ⚡ **Zero Installation:** No Java runtime, no configuration, no setup. Open the URL and start exploring in milliseconds.
- 📱 **Planetary GIS in Your Pocket:** Fully responsive, touch-first mobile UX with a collapsible bottom sheet for smartphones and tablets.
- 📲 **1-Click PWA Install:** Install JSMARS as a standalone native app on macOS, Windows, Linux, iOS, and Android with automatic background updates.
- 🛠️ **Zero-Build Architecture:** No Webpack, no Vite, 0 npm dependencies in production. Native browser ES Modules (`import`/`export`).
- 🪐 **True Multi-Body Support:** Seamlessly switch between Mars, the Moon, and Earth with live WMS/XYZ feeds from USGS Astrogeology and NASA GIBS.

---

## ✨ Key Capabilities

### 🪐 1. Multi-Body Planetary Maps
* **Mars:** OpenPlanetary Viking basemap, USGS Viking MDIM 2.1 WMS, THEMIS Daytime IR, and global MOLA shaded relief.
* **The Moon:** High-resolution LROC basemaps, LOLA digital elevation models, and **interactive Apollo (11–17), Luna, and Surveyor landing sites** with full mission metadata.
* **Earth:** NASA GIBS Blue Marble Next Generation and Blue Marble Shaded Relief.
* **IAU Nomenclature:** Searchable database of official planetary landmarks (craters, *mons*, *valles*, *planitia*, *maria*).

### 🏔️ 2. 3D Terrain & MOLA Topography
* **WebGL 3D Terrain:** Interactive 3D mesh displacement powered by MOLA DEM data.
* **Lighting & Exaggeration:** Dynamic solar angle illumination with adjustable vertical exaggeration ($1\times$ to $5\times$).
* **Elevation Transects:** Linear and radial cross-section elevation profiles across crater rims, volcanoes, and canyon floors.

### 🔬 3. Scientific Research & Modeling Suite
* **KRC 1D Subsurface Thermal Model:** Simulate diurnal and seasonal surface/subsurface temperature curves, regolith thermal inertia, and $\text{CO}_2$ frost condensation.
* **MCD Atmospheric Profiler:** Extract vertical profiles of temperature, pressure, atmospheric density, dust optical depth, and zonal/meridional winds up to 50 km.
* **Crater Counting & CSFD Isochron Dating:** Interactive crater digitization with real-time log-log CSFD plots and Hartmann & Neukum production functions to estimate surface model ages.
* **Hyperspectral Band Math:** CRISM and THEMIS spectral mineral indices (BD530, BD1500, BD1900, D2300, Olivine index) with real-time colormap stretching.
* **Mars Time & Solar Longitude ($L_s$):** Real-time calculation of Solar Longitude ($L_s$), Mars Sol Date (MSD), Mars Year (MY), and interactive orbital time scrubbing.

### 🛰️ 4. Mission Footprints & Spacecraft Tracking
* **USGS ODE Stamp Query:** Search and visualize instrument footprints (THEMIS, CTX, HiRISE, MOC, CRISM) directly via the USGS ODE REST API.
* **Orbital Ground Tracks:** Visualize live spacecraft orbits for MRO, Mars Odyssey, MAVEN, and Mars Express.

### 📐 5. Digitization, Analysis & Export
* **Vector Drawing:** Points, lines, polygons, circles, and rectangles with attribute tables and custom styling presets.
* **Geospatial Import/Export:** Drag-and-drop support for GeoJSON, KML, and CSV.
* **Map Export:** High-resolution PNG, JPEG, and georeferenced World Files (`.pgw` / `.jgw`).
* **Session Persistence:** Save and load full workspace states (active layers, digitized shapes, viewports) as portable JSON files.

---

## 🏗️ Architecture: Pure Web Standards

JSMARS adheres to a strict **"No-Build"** philosophy designed for speed, longevity, and hackability:

```
/
├── index.html              # Core application entry point
├── style.css               # Modern dark titanium design system
├── src/
│   ├── jmars-config.js     # Body definitions, WMS endpoints, catalogs
│   ├── jmars-map.js        # Core Leaflet map wrapper (EPSG:4326)
│   ├── jmars-state.js      # Singleton application state store
│   ├── core/
│   │   ├── EventBus.js     # Centralized CustomEvent dispatch
│   │   └── ToolManager.js  # Mutual-exclusion tool state machine
│   ├── features/           # Self-contained feature modules (3D, KRC, MCD, CSFD, etc.)
│   ├── ui/                 # Small, modular UI components (Sidebar, PWA, Accordion)
│   └── util/               # Planetary geodesy, MOLA DEM queries, time math
└── docs/                   # Architecture, Roadmap, User Guide, and Release Notes
```

* **0 ms Build Time:** What you write in `/src` is what executes in the browser. Zero sourcemap lag in DevTools.
* **0 Production npm Dependencies:** Uses standard browser ES Modules and lightweight CDN libraries (Leaflet.js, Three.js).
* **Decoupled EventBus:** Modules communicate entirely through typed `CustomEvent` triggers (`jmars:body-changed`, `layers-changed`, `jmars:shape-created`).

---

## ⚡ Quick Start

### Option 1: Live Web App (No Install)
Open [**https://jsmars.sounny.com**](https://jsmars.sounny.com) in any browser.

### Option 2: 1-Click PWA Install
1. Open [jsmars.sounny.com](https://jsmars.sounny.com) on your computer or phone.
2. Click or tap **"Install App"** in the sidebar.
3. JSMARS will install as a dedicated, standalone application on your Mac Dock, Windows Start Menu, or mobile Home Screen.

### Option 3: Run Locally in 5 Seconds
No `npm install`, no build scripts:
```bash
# Clone the repository
git clone https://github.com/sounny/jsmars.git
cd jsmars

# Start any standard static HTTP server
python -m http.server 8000
# or: npx serve
```
Open `http://localhost:8000` in your browser.

---

## 🗺️ Roadmap & Milestones

- [x] **Phase 1**: WMS Layer Integration & Core Leaflet Map
- [x] **Phase 2**: Layer Manager, Opacity Blending & Accordion UI
- [x] **Phase 3**: Multi-Body Support (Mars, Moon, Earth)
- [x] **Phase 4**: Crater Counting, Elevation Profiles, Measurements & IAU Nomenclature
- [x] **Phase 5**: Location Search, Bookmarks, Area Sampling & Session Persistence
- [x] **Phase 6**: USGS ODE Stamp Footprints, Shape Digitizer, Landing Sites & Map Export
- [x] **Phase 7**: KRC 1D Thermal Model, Mars Time & $L_s$ Slider, 3D WebGL Terrain, MCD Atmospheric Profiler, CSFD Isochrons & Band Math
- [x] **Phase 8**: Touch-First Mobile UX & Progressive Web App (PWA) 1-Click Install
- [ ] **Phase 9 (Upcoming)**: Outer Solar System Expansion (Europa, Titan, Venus WMS feeds)

See [docs/jsmars-roadmap.md](docs/jsmars-roadmap.md) for detailed development plans.

---

## 💼 Institutional Services, Support & Training

For university departments, research laboratories, and space mission teams, we offer dedicated professional services:
* 🎓 **Academic Curriculum & 16-Week Lab Kits:** Turnkey planetary GIS lab exercises, crater counting/CSFD dating modules, and faculty certification.
* 🛡️ **Support Retainers & SLAs:** Guaranteed 24/48-hour response times, browser compatibility assurance, and mission-critical stability.
* 🚀 **NASA / NSF Grant Subcontracting:** Co-Investigator (Co-I) software work packages for NASA ROSES/PDART/SSW proposals adhering to NASA TOPS Open Science mandates.
* ☁️ **Managed Private Cloud & SSO:** Dedicated AWS/GCP deployment with institutional authentication (Google Workspace, Entra ID, Okta) and private landing site data hosting.

Explore options or request a quote at [**jsmars.sounny.com/services.html**](https://jsmars.sounny.com/services.html) or email [**jsmars@sounny.com**](mailto:jsmars@sounny.com).

---

## 🤝 Contributing

Contributions from planetary scientists, GIS professionals, and web developers are warmly welcome!
- Check [AGENTS.md](AGENTS.md) for architectural conventions.
- Check [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md) for module lifecycle guides.
- Open an [Issue](https://github.com/sounny/jsmars/issues) or submit a Pull Request.

---

## 📄 License & Attribution

* **License:** Open source and compatible with the original JMARS **GPLv3** license.
* **Inspiration:** **[JMARS Team at Arizona State University (ASU)](https://jmars.asu.edu/)** for the desktop standard in planetary remote sensing.
* **Data Providers:** **[USGS Astrogeology Science Center](https://astrogeology.usgs.gov/)**, **[NASA GIBS](https://www.earthdata.nasa.gov/eosdis/science-system-description/eosdis-components/gibs)**, and **[OpenPlanetary](https://www.openplanetary.org/)**.

<div align="center">
  <sub>Created by <a href="https://sounny.com">Dr. M. Anwar Sounny-Slitine</a> & the JSMARS Open Source Community.</sub>
</div>
