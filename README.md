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
- 🪐 **26 Solar System Worlds Across 7 Planetary Systems:** Seamlessly switch across **Mars, Moon, Earth, Mercury, Venus, Phobos, Deimos, Ceres, Vesta, Jupiter, Io, Europa, Ganymede, Callisto, Saturn, Mimas, Enceladus, Tethys, Dione, Rhea, Titan, Iapetus, Phoebe, Triton, Pluto, and Charon** with live HTTPS/CORS spacecraft WMS/XYZ feeds from USGS Astrogeology, NASA Trek, NASA GIBS, and OpenPlanetary (strictly excluding simulated gas-giant textures).

---

## ✨ Key Capabilities

### 🪐 1. Multi-Body Planetary Maps (26 Verified Worlds)
* **Inner Planets & Moon:** **Mars** (OpenPlanetary Viking, USGS MDIM 2.1, THEMIS Day IR, MOLA relief), **Moon** (LROC WAC, LOLA relief, Apollo/Luna/Chang'e/Artemis III sites, and full **Lunar Base Site Planner** — see [docs/lunar-site-planner.md](docs/lunar-site-planner.md)), **Earth** (NASA GIBS Blue Marble), **Mercury** (MESSENGER MDIS global & color mosaics), and **Venus** (Magellan SAR FMAP, C3-MDIR colorized topography & Venera/Vega landing sites).
* **Mars Moons & Main-Belt Asteroids:** **Phobos** (Viking global mosaic), **Deimos** (Viking global mosaic), **Ceres** (Dawn Framing Camera global mosaic), and **Vesta** (Dawn Framing Camera HAMO mosaic).
* **Jupiter System:** **Jupiter** (Cassini cylindrical cloud mosaic), **Io** (Galileo/Voyager merged color), **Europa** (USGS Galileo/Voyager + NASA Trek global), **Ganymede** (Galileo/Voyager global), and **Callisto** (Galileo/Voyager global).
* **Saturn System:** **Saturn** (Cassini global), **Mimas**, **Enceladus**, **Tethys**, **Dione**, **Rhea**, **Titan** (NASA Trek Cassini ISS/VIMS color-ratio & SAR mosaics + ESA **Huygens Probe** landing site), **Iapetus**, and **Phoebe** (Cassini ISS mosaics).
* **Outer Solar System:** **Triton** (Voyager 2 global mosaic), **Pluto** (New Horizons LORRI/MVIC global mosaic), and **Charon** (New Horizons global mosaic).
* **IAU Nomenclature:** Searchable 144-landmark gazetteer across all planetary systems (*craters*, *montes*, *valles*, *planitiae*, *maria*, *lineae*, *maculae*, *sulci*).

### 🌍 2. 3D Globe & Astronomical Solar Terminator
* **2D | 3D toggle:** 2D stays the Leaflet map. 3D is a full-size [CesiumJS](https://cesium.com/platform/cesiumjs/) globe (Apache-2.0, © Cesium GS, Inc.) loaded from a pinned CDN release only on the first switch to 3D.
* **True IAU Oblate Spheroid Ellipsoids & Radius-Scaled Lighting:** All 26 worlds use IAU WGCCRE (Archinal et al., 2018) radii — including mean-equatorial oblate spheroids for small irregular moons/asteroids (Phobos, Deimos, Vesta, Mimas, Enceladus, Io) and oblate spheroids for Jupiter and Saturn, with camera zoom limits scaled to body radius.
* **Astronomical Solar Terminator (`solarEphemeris.js`):** Optional **Terminator** control computes real-time subsolar latitude/longitude and body-fixed sunlight vectors from JPL J2000 Keplerian heliocentric orbits + IAU WGCCRE pole right ascension, declination, prime-meridian rotation ($W_0 + \dot{W}d$), and lunar libration (`[Ephemeris]` Live UTC, synchronized with the Mars $L_s$ `TimeSlider` and Earth's `SkyAtmosphere`), alongside an explicitly badged **`Local Hour [Model]`** mode anchored to the viewed longitude.
* **Published imagery & terrain provenance:** The active WMS/XYZ spacecraft mosaic is draped on the reference ellipsoid. **3D elevation terrain (DEM) is not loaded** — when the terminator is active, the globe chrome explicitly notes that shading applies to macro day/night ($\cos	heta_z$) on the smooth ellipsoid while crater/mountain relief in tiles reflects baked spacecraft illumination.
* **Elevation transects (2D):** Linear and radial cross-section profiles across crater rims, volcanoes, and canyon floors.

### 🔬 3. Scientific Research & Modeling Suite
* **KRC 1D Subsurface Thermal Model:** Client-side simulation of diurnal and seasonal surface/subsurface temperature curves, regolith thermal inertia, and $\text{CO}_2$ frost condensation (not TES/THEMIS measurements).
* **MCD Atmospheric Profiler:** Explicit choice of a 1D analytical physics model (offline) or live LMD MCD v6.1 GCM, with labeled fallback if the live fetch fails.
* **Crater Counting & CSFD Isochron Dating:** Interactive crater digitization with real-time log-log CSFD plots and Hartmann & Neukum production functions to estimate surface model ages.
* **Educational mineral-index visualizer:** Approximate CRISM-style band-depth formulas (BD530, BD1500, BD1900, D2300, olivine index) as illustrative overlays — not per-pixel CRISM/THEMIS cubes. Labeled **Model** in the UI.
* **Subsurface radar sounder (simulation):** Physically-based synthetic radargrams with illustrative SHARAD/MARSIS-analog presets — not observed PDS radargrams. Archive links are in the panel.
* **Mars Time & Solar Longitude ($L_s$):** Real-time calculation of Solar Longitude ($L_s$), Mars Sol Date (MSD), Mars Year (MY), and interactive orbital time scrubbing.

### 🛰️ 4. Mission Footprints & Spacecraft Tracking
* **USGS ODE Stamp Query:** Search and visualize instrument footprints (THEMIS, CTX, HiRISE, MOC, CRISM) directly via the USGS ODE REST API.
* **Orbital Ground Tracks:** Visualize live spacecraft orbits for MRO, Mars Odyssey, MAVEN, and Mars Express.

### 📐 5. Digitization, Analysis & Export
* **Vector Drawing & Geologic Patterns:** Points, lines, polygons, circles, and rectangles with 6 planetary SVG geologic fill patterns (*Ejecta*, *Crater Material*, *Volcanic Plains*, *Fracture Zone*, *Layered Deposits*, *Chaos/Breccia*), custom attribute columns, and multi-body geodesic buffering.
* **Independent Geodesic Measurement:** Planet-calibrated distance, initial forward azimuth ($0^\circ\text{–}360^\circ$), spherical polygon area, and perimeter isolated from vector digitization.
* **GIS & Shapefile Interoperability:** Import and export binary **ESRI Shapefile triads (`.shp`/`.shx`/`.dbf`)**, **WKT (`.wkt`)**, **GeoJSON**, **KML**, and **CSV**.
* **Quick Command Palette (`Ctrl+K`):** Instant fuzzy landmark search, direct coordinate fly-to, cross-body switching, bookmark recall, and tool activation.
* **Map Export & Session Persistence:** High-resolution PNG, JPEG, georeferenced World Files (`.pgw` / `.jgw`), and portable `.json` workspaces preserving layers, viewports, measurements, craters, and styled shapes.

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
* **0 Production npm Dependencies:** Uses standard browser ES Modules and CDN libraries (Leaflet.js, and CesiumJS only after switching to 3D).
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
- [x] **Phase 7**: KRC 1D Thermal Model, Mars Time & $L_s$ Slider, 3D WebGL Terrain, MCD Atmospheric Profiler, CSFD Isochrons & educational mineral-index visualizer
- [x] **Phase 8**: Touch-First Mobile UX & Progressive Web App (PWA) 1-Click Install
- [ ] **Phase 9 (Active)**: Documentation portal (`docs/index.html`) — tool docs shipped; interactive sandboxes and JSDoc API refs remain. *Europa already ships as a fourth body; Titan and Venus are future work, not Phase 9.*

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
