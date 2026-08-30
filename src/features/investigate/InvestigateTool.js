import { JMARSWMS } from '../../jmars-wms.js';
import { jmarsState } from '../../jmars-state.js';
import { EVENTS } from '../../constants.js';
import { molaDem } from '../../util/mola-dem.js';
import { MarsTime } from '../slider/MarsTime.js';
import { KRCEngine } from '../krc/KRCEngine.js';
import { MCDEngine } from '../mcd/MCDEngine.js';
import { formatLatLon } from '../../util/geo.js';

/**
 * @module InvestigateTool
 * @description Click-to-query tool that probes WMS layers, elevation,
 * Mars astronomical state, KRC thermal simulation, and MCD atmosphere.
 */
export class InvestigateTool {
    /**
     * Create an InvestigateTool.
     * @param {L.Map} map - The Leaflet map instance.
     */
    constructor(map) {
        this.map = map;
        this.isActive = false;
        this.popup = L.popup({ maxWidth: 320, className: 'investigate-popup' });
        
        this.onClick = this.onClick.bind(this);

        document.addEventListener(EVENTS.BODY_CHANGED, () => this.deactivate());
    }

    /**
     * Activate the investigate tool.
     * Sets cursor to 'help' and begins listening for map clicks.
     */
    activate() {
        if (this.isActive) return;
        this.isActive = true;
        this.map.getContainer().style.cursor = 'help';
        this.map.on('click', this.onClick);
        const body = (jmarsState.get('body') || 'mars').toLowerCase();
        if (body === 'mars') {
            molaDem.ensureLoaded().catch(() => {});
        }
    }

    /**
     * Deactivate the investigate tool.
     */
    deactivate() {
        if (!this.isActive) return;
        this.isActive = false;
        this.map.getContainer().style.cursor = '';
        this.map.off('click', this.onClick);
        this.map.closePopup();
        document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'investigate' } }));
    }

    /**
     * Handle map click: show popup with coordinates, then query WMS layers.
     * @param {L.LeafletMouseEvent} e - Leaflet click event.
     */
    async onClick(e) {
        if (!this.isActive) return;

        const lat = e.latlng.lat;
        let lng = e.latlng.lng;
        // Normalize display Lng (0-360)
        const displayLng360 = ((lng % 360) + 360) % 360;
        const body = (jmarsState.get('body') || 'mars').toLowerCase();

        // 1. Show Popup with initial template
        let content = `
            <div style="font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; font-size: 11px; line-height: 1.4; color: #eee; background: #1a1a1a; padding: 4px; border-radius: 4px;">
                <div style="font-weight: 700; color: #38bdf8; margin-bottom: 4px; border-bottom: 1px solid #333; padding-bottom: 2px;">
                    📍 Planetary Probe
                </div>
                <div style="display: grid; grid-template-columns: auto 1fr; gap: 2px 6px;">
                    <span style="color:#aaa;">Lat / Lon:</span>
                    <span><b>${lat.toFixed(4)}°</b>, <b>${displayLng360.toFixed(4)}°E</b></span>
                    <span style="color:#aaa;">Elevation:</span>
                    <span id="investigate-elevation" style="color:#4ade80;">Sampling...</span>
                </div>
                <div id="investigate-planet-diagnostics" style="margin-top: 6px; border-top: 1px solid #333; padding-top: 4px;"></div>
                <hr style="margin: 6px 0; border: 0; border-top: 1px solid #333;">
                <div id="investigate-loading" style="color: #999; font-style: italic;">Querying active map layers...</div>
                <div id="investigate-results"></div>
            </div>
        `;

        this.popup
            .setLatLng(e.latlng)
            .setContent(content)
            .openOn(this.map);

        // Load elevation and planetary diagnostics
        this.loadDiagnostics(lat, displayLng360, body);

        // 2. Query WMS Layers
        const results = await this.queryLayers(e.latlng, e.containerPoint);
        this.updatePopup(results);
    }

    /**
     * Load elevation and planetary state for the clicked point.
     */
    async loadDiagnostics(lat, lng360, body) {
        const elevationEl = document.getElementById('investigate-elevation');
        const diagEl = document.getElementById('investigate-planet-diagnostics');
        if (!elevationEl || !diagEl) return;

        if (body !== 'mars') {
            elevationEl.textContent = 'N/A (Mars only)';
            return;
        }

        try {
            const values = await molaDem.sampleElevations([{ lat, lng: lng360 }]);
            const elev = values[0];
            const elevMeters = Number.isFinite(elev) ? Math.round(elev) : 0;
            elevationEl.textContent = Number.isFinite(elev) ? `${elevMeters} m (${(elevMeters/1000).toFixed(2)} km)` : 'No data';

            // Astronomical state
            const marsState = MarsTime.computeState(new Date());
            const mtc = marsState.mtc ?? marsState.MTC ?? 12;
            const ltst = MarsTime.computeLTST(marsState.Ls, mtc, lng360);
            const ltstHours = Math.floor(ltst);
            const ltstMins = Math.floor((ltst - ltstHours) * 60);
            const ltstStr = `${String(ltstHours).padStart(2, '0')}:${String(ltstMins).padStart(2, '0')}`;
            const zenith = MarsTime.getSolarZenith(lat, marsState.subSolarLat, ltst);

            // Thermal Simulation
            const krc = KRCEngine.simulateDiurnal({
                lat,
                Ls: marsState.Ls,
                elevation: elevMeters / 1000,
                thermalInertia: 280,
                albedo: 0.22
            });

            // MCD Atmospheric Profile
            const mcd = MCDEngine.computeProfile({
                lat,
                lon: lng360,
                elevation: elevMeters / 1000,
                Ls: marsState.Ls,
                localHour: ltst
            });

            diagEl.innerHTML = `
                <div style="display: grid; grid-template-columns: auto 1fr; gap: 2px 6px; font-size: 10px;">
                    <span style="color:#aaa;">Local Time:</span>
                    <span><b>${ltstStr} LTST</b> (${zenith.isDay ? '☀️ Day' : '🌙 Night'})</span>
                    <span style="color:#aaa;">Est. Temp:</span>
                    <span style="color:#fb923c;"><b>${krc.summary.meanTemp.toFixed(1)} K</b> [${krc.summary.minTemp.toFixed(0)} to ${krc.summary.maxTemp.toFixed(0)} K]</span>
                    <span style="color:#aaa;">Atmosphere:</span>
                    <span style="color:#38bdf8;">${mcd.surface.pressurePa.toFixed(0)} Pa (${mcd.surface.temperatureK.toFixed(0)} K)</span>
                </div>
            `;
        } catch (err) {
            console.warn('Investigate diagnostics failed', err);
            elevationEl.textContent = 'Error';
        }
    }

    /**
     * Query all visible WMS layers at the clicked point.
     * @param {L.LatLng} latlng - Clicked coordinates.
     * @param {L.Point} containerPoint - Pixel position in the map container.
     * @returns {Promise<Array<object>>} Array of { name, value } results.
     */
    async queryLayers(latlng, containerPoint) {
        // Get active WMS layers from State
        // We need the URL and layer names.
        // The jmarsState stores { id, opacity }. We need to map back to config.
        
        const activeState = jmarsState.get('activeLayers'); // [Bottom...Top]
        // Query top-most visible WMS layer first? Or all?
        // Let's try all visible WMS layers.
        
        const results = [];
        const size = this.map.getSize();
        const bounds = this.map.getBounds();
        // Leaflet bounds: SouthWest, NorthEast.
        // WMS 1.3.0 BBOX depends on CRS axis order. EPSG:4326 is usually Lat,Lon.
        // USGS Astrogeology WMS 1.3.0 is usually strict (Lat,Lon).
        
        const bbox = `${bounds.getSouth()},${bounds.getWest()},${bounds.getNorth()},${bounds.getEast()}`;

        // Find Config for active layers
        // Accessing global window.jmars is a bit dirty, but we need access to availableLayers to get URL.
        // Better: JMARSMap should expose a helper, or we pass it in.
        // For now, let's assume we can access JMARS_CONFIG or the map instance if it has the data.
        
        // We can import the map instance via `window.jmars` if it's exposed, or passed in constructor.
        // In `index.html`, we exposed `window.jmars`.
        
        const availableLayers = window.jmars ? window.jmars.availableLayers : [];
        if (availableLayers.length === 0) return [];

        for (let i = activeState.length - 1; i >= 0; i--) {
            const layerState = activeState[i];
            if (!layerState.visible) continue;

            const config = availableLayers.find(l => l.id === layerState.id);
            if (!config || config.type !== 'wms') continue;

            try {
                const url = JMARSWMS.getFeatureInfoUrl(config.url, {
                    layers: config.options.layers,
                    bbox: bbox,
                    width: size.x,
                    height: size.y,
                    x: containerPoint.x,
                    y: containerPoint.y,
                    crs: 'EPSG:4326',
                    version: '1.3.0',
                    info_format: 'text/html' // USGS supports text/html usually
                });

                // We need a proxy to avoid CORS?
                // Most USGS servers allow CORS.
                
                // Note: fetching text/html might return a full page. 
                // We might display it in an iframe or parse it.
                
                // Let's try fetching.
                const response = await fetch(url);
                if (response.ok) {
                    const text = await response.text();
                    // Simple cleanup of HTML
                    const cleanText = this.parseFeatureInfo(text);
                    if (cleanText) {
                        results.push({ name: config.name, value: cleanText });
                    }
                }
            } catch (e) {
                console.warn(`Failed to query layer ${config.name}`, e);
            }
        }
        
        return results;
    }

    /**
     * Parse WMS GetFeatureInfo HTML response into displayable content.
     * @param {string} html - Raw HTML response.
     * @returns {string} Cleaned inner HTML.
     */
    parseFeatureInfo(html) {
        // This is highly dependent on the server output.
        // MapServer/GeoServer output simple tables.
        // We strip body tags.
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        return doc.body.innerHTML;
    }

    /**
     * Update the investigate popup with query results.
     * @param {Array<object>} results - Array of { name, value }.
     */
    updatePopup(results) {
        const loadingEl = document.getElementById('investigate-loading');
        const resultsEl = document.getElementById('investigate-results');
        
        if (loadingEl) loadingEl.style.display = 'none';
        
        if (!resultsEl) return; // Popup closed

        if (results.length === 0) {
            resultsEl.innerHTML = '<em>No data found.</em>';
            return;
        }

        let html = '';
        results.forEach(res => {
            html += `
                <div class="investigate-layer-result">
                    <strong>${res.name}</strong>
                    <div style="font-size: 11px; overflow: auto; max-height: 100px; background: #eee; color: #000; padding: 2px;">
                        ${res.value}
                    </div>
                </div>
            `;
        });
        resultsEl.innerHTML = html;
    }

    /**
     * Compute and format scientific probe diagnostics for any planetary coordinate.
     * @param {object} params
     * @param {number} params.lat - Latitude (planetocentric)
     * @param {number} params.lng - Longitude (degrees)
     * @param {string} [params.body='mars'] - Planetary body
     * @param {number} [params.elevationMeters=0] - Elevation in meters
     * @param {number} [params.Ls=0] - Solar longitude
     * @param {number} [params.MTC=12] - Mars Coordinated Time
     * @returns {object} Full probe diagnostic data
     */
    static formatProbeDiagnostics({ lat, lng, body = 'mars', elevationMeters = 0, Ls = 0, MTC = 12 }) {
        const lng360E = ((lng % 360) + 360) % 360;
        const lng180 = lng360E > 180 ? lng360E - 360 : lng360E;
        const lng360W = (360 - lng360E) % 360;

        const isMars = body.toLowerCase() === 'mars';
        const ltst = isMars ? MarsTime.computeLTST(Ls, MTC, lng360E) : (lng360E / 15.0);
        const ltstHours = Math.floor(ltst);
        const ltstMins = Math.floor((ltst - ltstHours) * 60);
        const ltstStr = `${String(ltstHours).padStart(2, '0')}:${String(ltstMins).padStart(2, '0')}`;

        let krcSummary = null;
        let mcdSummary = null;

        if (isMars) {
            const krc = KRCEngine.simulateDiurnal({
                lat,
                Ls,
                elevation: elevationMeters / 1000,
                thermalInertia: 280,
                albedo: 0.22
            });
            krcSummary = krc.summary;

            const mcd = MCDEngine.computeProfile({
                lat,
                lon: lng360E,
                elevation: elevationMeters / 1000,
                Ls,
                localHour: ltst
            });
            mcdSummary = mcd.surface;
        }

        return {
            body,
            lat,
            lng360E,
            lng180,
            lng360W,
            elevationMeters,
            elevationKm: elevationMeters / 1000,
            ltst,
            ltstStr,
            krc: krcSummary,
            mcd: mcdSummary
        };
    }

    // --- Planetary Geophysics & Regolith Classification ---

    /**
     * Compute isothermal barometric atmospheric pressure at probed elevation.
     * @param {number} elevationMeters - Elevation relative to Mars areoid datum (m)
     * @param {number} [p0=610] - Datum surface pressure in Pa
     * @param {number} [scaleHeightM=11100] - Mars atmospheric scale height in meters
     * @returns {number} Pressure in Pascals (Pa)
     */
    static computeBarometricPressure(elevationMeters, p0 = 610, scaleHeightM = 11100) {
        const p = p0 * Math.exp(-elevationMeters / Math.max(1, scaleHeightM));
        return parseFloat(p.toFixed(1));
    }

    /**
     * Compute local gravitational acceleration at elevation z.
     * @param {number} elevationMeters - Elevation in meters
     * @param {string} [body='mars'] - Planetary body name
     * @returns {number} Gravitational acceleration (m/s^2)
     */
    static computeLocalGravity(elevationMeters, body = 'mars') {
        const bodyLow = body.toLowerCase();
        const g0 = bodyLow === 'moon' ? 1.62 : bodyLow === 'earth' ? 9.80665 : 3.72076;
        const R = (bodyLow === 'moon' ? 1737.4 : bodyLow === 'earth' ? 6371.0 : 3389.5) * 1000;
        const rRatio = R / (R + elevationMeters);
        const g = g0 * (rRatio * rRatio);
        return parseFloat(g.toFixed(4));
    }

    /**
     * Classify regolith material from thermal inertia and visual albedo.
     * @param {number} thermalInertia - Thermal inertia (J m^-2 K^-1 s^-1/2)
     * @param {number} albedo - Visual Bond/Bolometric albedo (0..1)
     * @returns {{regime: string, description: string, dustCover: string}}
     */
    static classifyThermalRegime(thermalInertia, albedo = 0.2) {
        const ti = Math.max(0, thermalInertia);
        let regime, description, dustCover;

        if (ti < 120) {
            regime = 'High Dust Mantle';
            description = 'Thick airfall dust deposits / low thermal conductivity fine particles';
            dustCover = 'Heavy (Tharsis / Arabia Terra type)';
        } else if (ti < 250) {
            regime = 'Fine-to-Medium Sand';
            description = 'Active or semi-stabilized eolian sand sheets and dune fields';
            dustCover = 'Moderate to Low';
        } else if (ti < 450) {
            regime = 'Duricrust / Coarse Sand / Pebbles';
            description = 'Indurated soil crusts, cementation, coarse lag gravels';
            dustCover = 'Low / Scoured';
        } else if (ti < 800) {
            regime = 'Rocky Regolith / Fractured Bedrock';
            description = 'High rock fraction, crater ejecta blankets, blocky lava flows';
            dustCover = 'Minimal / High Rock Abundance';
        } else {
            regime = 'Solid Bedrock / Massive Ice';
            description = 'Exposed basaltic basement or polar water ice cap ice sheet';
            dustCover = 'None / Clean Exposed Substrate';
        }

        return { regime, description, dustCover, thermalInertia: ti, albedo };
    }

    // --- Latitude-Dependent Planetary Gravity & Thermal Properties ---

    /**
     * Compute theoretical surface gravity as a function of latitude on an oblate planet (Somigliana-like formula).
     * g(phi) = g_e * (1 + beta * sin^2(phi))
     * @param {number} latDeg - Planetocentric latitude in degrees (-90 to +90)
     * @param {string} [body='mars'] - Planetary body
     * @returns {number} Surface gravitational acceleration in m/s^2
     */
    static computeTheoreticalGravityByLatitude(latDeg, body = 'mars') {
        const bKey = body.toLowerCase();
        const phiRad = latDeg * Math.PI / 180.0;
        const sin2 = Math.sin(phiRad) * Math.sin(phiRad);

        let g_e = 3.7112;
        let beta = 0.0053;

        if (bKey === 'moon') {
            g_e = 1.622;
            beta = 0.0002;
        } else if (bKey === 'earth') {
            g_e = 9.780327;
            beta = 0.0053024;
        }

        const g = g_e * (1.0 + beta * sin2);
        return parseFloat(g.toFixed(4));
    }

    /**
     * Calculate volumetric heat capacity C_vol = rho * c_p.
     * @param {number} [densityKgM3=1500] - Regolith mass density in kg/m^3
     * @param {number} [specificHeat=800] - Specific heat capacity in J/(kg K)
     * @returns {number} Volumetric heat capacity in J / (m^3 K)
     */
    static computeVolumetricHeatCapacity(densityKgM3 = 1500, specificHeat = 800) {
        return Math.max(1, densityKgM3 * specificHeat);
    }

    /**
     * Calculate isothermal atmospheric temperature from scale height and planetary gravity.
     * T = (g * H * M) / R_univ
     * @param {number} [scaleHeightM=11100] - Atmospheric scale height in meters
     * @param {number} [gSurface=3.72076] - Surface gravitational acceleration in m/s^2
     * @param {number} [molarMassKgMol=0.04401] - Molar mass of atmosphere in kg/mol (CO2 = 0.04401)
     * @returns {number} Mean isothermal atmospheric temperature in Kelvin
     */
    static computeHydrostaticColumnPressure(scaleHeightM = 11100, gSurface = 3.72076, molarMassKgMol = 0.04401) {
        const R_univ = 8.314462618; // J / (mol K)
        const T = (gSurface * scaleHeightM * molarMassKgMol) / R_univ;
        return parseFloat(T.toFixed(1));
    }

    // --- Geothermal Heat Flow, Atmospheric Scale Height & Planck Radiance ---

    /**
     * Compute lithospheric geothermal temperature gradient and subsurface temperature at depth.
     * dT/dz = q / k
     * @param {number} [surfaceHeatFlowMwM2=30.0] - Surface geothermal heat flux in mW/m^2 (typical Mars ~ 30 mW/m^2)
     * @param {number} [thermalConductivityW_MK=2.0] - Crustal thermal conductivity in W/(m K) (basalt ~ 2.0 W/m K)
     * @param {number} [surfaceTempK=210.0] - Mean annual surface temperature in Kelvin
     * @param {number} [depthKm=1.0] - Target subsurface depth in km
     * @returns {{gradientKPerKm: number, tempAtDepthK: number}}
     */
    static computeGeothermalGradient(surfaceHeatFlowMwM2 = 30.0, thermalConductivityW_MK = 2.0, surfaceTempK = 210.0, depthKm = 1.0) {
        const q_W = surfaceHeatFlowMwM2 * 1e-3; // W/m^2
        const k = Math.max(0.01, thermalConductivityW_MK);
        const gradientKPerM = q_W / k;
        const gradientKPerKm = gradientKPerM * 1000.0;

        const tempAtDepth = surfaceTempK + gradientKPerKm * Math.max(0, depthKm);

        return {
            gradientKPerKm: parseFloat(gradientKPerKm.toFixed(2)),
            tempAtDepthK: parseFloat(tempAtDepth.toFixed(2))
        };
    }

    /**
     * Calculate atmospheric scale height H = (R_univ * T) / (M * g).
     * @param {number} temperatureK - Mean atmospheric temperature in Kelvin
     * @param {string} [body='mars'] - Target planetary body
     * @returns {number} Scale height in km
     */
    static computeScaleHeight(temperatureK, body = 'mars') {
        const R_univ = 8.314462618; // J/(mol K)
        const isMars = body.toLowerCase() === 'mars';
        const g = isMars ? 3.72076 : (body.toLowerCase() === 'moon' ? 1.62 : 9.80665);
        const molarMass = isMars ? 0.04401 : 0.02897; // kg/mol (CO2 vs Earth N2/O2)

        const H_meters = (R_univ * Math.max(1, temperatureK)) / (molarMass * g);
        return parseFloat((H_meters / 1000.0).toFixed(2));
    }

    /**
     * Compute Planck blackbody spectral radiance B(lambda, T).
     * B = (2 * h * c^2) / (lambda^5 * (exp(h*c / (lambda*k_B*T)) - 1))
     * @param {number} wavelengthMicrons - Wavelength in micrometers (µm)
     * @param {number} temperatureK - Temperature in Kelvin
     * @returns {number} Spectral radiance in W / (m^2 sr µm)
     */
    static computeBlackbodySpectralRadiance(wavelengthMicrons, temperatureK) {
        const h = 6.62607015e-34; // J s
        const c = 299792458; // m/s
        const kB = 1.380649e-23; // J/K

        const lambdaM = Math.max(1e-9, wavelengthMicrons * 1e-6);
        const T = Math.max(1, temperatureK);

        const c1 = 2.0 * h * c * c;
        const c2 = (h * c) / (kB * T);

        const expTerm = Math.exp(c2 / lambdaM) - 1.0;
        if (expTerm <= 0 || !Number.isFinite(expTerm)) return 0;

        const radianceW_M3_Sr = c1 / (Math.pow(lambdaM, 5) * expTerm);
        // Convert to per micrometer: 1 m = 1e6 µm
        const radianceW_M2_Sr_Um = radianceW_M3_Sr * 1e-6;

        return parseFloat(radianceW_M2_Sr_Um.toExponential(4));
    }

    // --- Crustal Magnetic Field, Multi-Layer Geotherm & Transmittance Solvers ---

    /**
     * Calculate dipole crustal/planetary magnetic field vector components (Br, Btheta, |B|) in nanoTesla.
     * @param {number} magneticLatitudeDeg - Magnetic latitude (-90 to +90)
     * @param {number} altitudeKm - Altitude above planetary surface in km
     * @param {number} [dipoleMomentAm2=1e20] - Magnetic dipole moment in A m^2 (crustal remanence anomaly)
     * @param {string} [body='mars'] - Planetary body
     * @returns {{Br_nT: number, Btheta_nT: number, Btotal_nT: number, inclinationDeg: number}}
     */
    static computeDipoleMagneticField(magneticLatitudeDeg, altitudeKm, dipoleMomentAm2 = 1e20, body = 'mars') {
        const R = (body.toLowerCase() === 'moon' ? 1737.4 : 3389.5) * 1000.0; // meters
        const r = R + Math.max(0, altitudeKm * 1000.0);
        const mu0 = 4.0 * Math.PI * 1e-7; // T m / A

        const latRad = magneticLatitudeDeg * Math.PI / 180.0;
        const sinLat = Math.sin(latRad);
        const cosLat = Math.cos(latRad);

        const factor = (mu0 * dipoleMomentAm2) / (4.0 * Math.PI * Math.pow(r, 3)); // Tesla

        const Br_T = factor * 2.0 * sinLat;
        const Btheta_T = -factor * cosLat;
        const Btotal_T = factor * Math.sqrt(1.0 + 3.0 * sinLat * sinLat);

        // Convert Tesla to nanoTesla (1 T = 1e9 nT)
        const Br_nT = Br_T * 1e9;
        const Btheta_nT = Btheta_T * 1e9;
        const Btotal_nT = Btotal_T * 1e9;

        // Magnetic inclination I = atan2(Br, -Btheta)
        const incDeg = Math.atan2(Br_nT, -Btheta_nT) * 180.0 / Math.PI;

        return {
            Br_nT: parseFloat(Br_nT.toFixed(2)),
            Btheta_nT: parseFloat(Btheta_nT.toFixed(2)),
            Btotal_nT: parseFloat(Btotal_nT.toFixed(2)),
            inclinationDeg: parseFloat(incDeg.toFixed(2))
        };
    }

    /**
     * Compute steady-state lithospheric geotherm across multi-layer stratigraphy.
     * @param {Array<{thicknessKm: number, thermalConductivityW_MK: number, name: string}>} layers
     * @param {number} [surfaceHeatFlowMwM2=30.0] - Geothermal heat flux (mW/m^2)
     * @param {number} [surfaceTempK=210.0] - Surface temperature (K)
     * @returns {{totalCrustThicknessKm: number, tempAtBaseK: number, layerBoundaries: Array<object>}}
     */
    static computeMultiLayerGeotherm(layers = [], surfaceHeatFlowMwM2 = 30.0, surfaceTempK = 210.0) {
        const q_W = surfaceHeatFlowMwM2 * 1e-3; // W/m^2
        let currentZ = 0;
        let currentT = surfaceTempK;
        const boundaries = [{ depthKm: 0, tempK: currentT, layer: 'Surface' }];

        layers.forEach((l, idx) => {
            const dz = l.thicknessKm || 1.0;
            const k = Math.max(0.01, l.thermalConductivityW_MK || 2.0);
            const dT = (q_W / k) * (dz * 1000.0);

            currentZ += dz;
            currentT += dT;

            boundaries.push({
                layerIndex: idx + 1,
                name: l.name || `Layer ${idx + 1}`,
                depthKm: parseFloat(currentZ.toFixed(2)),
                tempK: parseFloat(currentT.toFixed(2)),
                deltaTK: parseFloat(dT.toFixed(2))
            });
        });

        return {
            totalCrustThicknessKm: parseFloat(currentZ.toFixed(2)),
            tempAtBaseK: parseFloat(currentT.toFixed(2)),
            layerBoundaries: boundaries
        };
    }

    /**
     * Calculate direct atmospheric optical transmittance via Beer-Lambert extinction law.
     * T = exp(-tau / cos(theta_z))
     * @param {number} opticalDepthTau - Atmospheric column optical depth (dust/gas tau)
     * @param {number} solarZenithAngleDeg - Solar zenith angle in degrees (0 = overhead, 90 = horizon)
     * @returns {{transmittance: number, airmass: number, directFluxFractionPercent: number}}
     */
    static computeAtmosphericTransmittance(opticalDepthTau = 0.5, solarZenithAngleDeg = 45) {
        const zRad = Math.min(88.0, Math.max(0, solarZenithAngleDeg)) * Math.PI / 180.0;
        const airmass = 1.0 / Math.cos(zRad);
        const tau = Math.max(0, opticalDepthTau);

        const transmittance = Math.exp(-tau * airmass);

        return {
            transmittance: parseFloat(transmittance.toFixed(4)),
            airmass: parseFloat(airmass.toFixed(3)),
            directFluxFractionPercent: parseFloat((transmittance * 100.0).toFixed(2))
        };
    }

    // --- Bouguer Gravity Anomaly, Airy Isostasy & Thermal Diffusivity Solvers ---

    /**
     * Calculate Complete Bouguer Gravity Anomaly in milliGals (mGal).
     * Delta_g_B = (g_obs - g_theor + delta_g_FA - 2*pi*G*rho_c*h) * 1e5
     * @param {number} observedGravityMs2 - Measured gravity in m/s^2
     * @param {number} theoreticalGravityMs2 - Normal reference ellipsoid gravity in m/s^2
     * @param {number} elevationMeters - Surface elevation above datum in meters
     * @param {number} [crustDensityKgM3=2900] - Crustal rock density (Mars basalt ~ 2900 kg/m^3)
     * @param {string} [body='mars'] - Planetary body
     * @returns {{freeAirCorrectionMGal: number, bouguerPlateCorrectionMGal: number, bouguerAnomalyMGal: number}}
     */
    static computeBouguerGravityAnomaly(observedGravityMs2, theoreticalGravityMs2, elevationMeters, crustDensityKgM3 = 2900, body = 'mars') {
        const G = 6.67430e-11; // m^3 / (kg s^2)
        const R = (body.toLowerCase() === 'moon' ? 1737.4 : 3389.5) * 1000.0;
        const g0 = theoreticalGravityMs2;
        const h = elevationMeters;

        // Free-air gradient dg/dz = (2 * g0 / R) in s^-2
        const freeAirGrad = (2.0 * g0) / R;
        const deltaFA_ms2 = freeAirGrad * h;
        const deltaFA_mGal = deltaFA_ms2 * 1e5;

        // Bouguer slab correction: 2 * pi * G * rho * h
        const deltaB_ms2 = 2.0 * Math.PI * G * crustDensityKgM3 * h;
        const deltaB_mGal = deltaB_ms2 * 1e5;

        const anomaly_ms2 = (observedGravityMs2 - theoreticalGravityMs2) + deltaFA_ms2 - deltaB_ms2;
        const anomaly_mGal = anomaly_ms2 * 1e5;

        return {
            freeAirCorrectionMGal: parseFloat(deltaFA_mGal.toFixed(2)),
            bouguerPlateCorrectionMGal: parseFloat(deltaB_mGal.toFixed(2)),
            bouguerAnomalyMGal: parseFloat(anomaly_mGal.toFixed(2))
        };
    }

    /**
     * Calculate Airy-Heiskanen local isostatic crustal compensation root thickness.
     * t_root = h * (rho_c / (rho_m - rho_c))
     * @param {number} topographyHeightKm - Topographic elevation above base datum in km
     * @param {number} [crustDensity=2900] - Crustal density in kg/m^3
     * @param {number} [mantleDensity=3500] - Upper mantle density in kg/m^3
     * @returns {{crustalRootThicknessKm: number, totalCrustalColumnKm: number}}
     */
    static computeAiryIsostaticCrustalRoot(topographyHeightKm, crustDensity = 2900, mantleDensity = 3500) {
        const h = Math.max(0, topographyHeightKm);
        const rhoC = crustDensity;
        const rhoM = mantleDensity;
        const deltaRho = Math.max(10, rhoM - rhoC);

        const rootKm = h * (rhoC / deltaRho);
        const referenceCrustThicknessKm = 50.0; // Mean Martian crustal thickness ~ 50 km
        const totalCrustKm = referenceCrustThicknessKm + h + rootKm;

        return {
            crustalRootThicknessKm: parseFloat(rootKm.toFixed(2)),
            totalCrustalColumnKm: parseFloat(totalCrustKm.toFixed(2))
        };
    }

    /**
     * Calculate thermal diffusivity kappa = k / (rho * c_p).
     * @param {number} thermalConductivityW_MK - Bulk thermal conductivity in W/(m K)
     * @param {number} [densityKgM3=1500] - Regolith/rock mass density in kg/m^3
     * @param {number} [specificHeat=800] - Specific heat capacity in J/(kg K)
     * @returns {number} Thermal diffusivity in m^2 / s
     */
    static computeThermalDiffusivity(thermalConductivityW_MK, densityKgM3 = 1500, specificHeat = 800) {
        const cVol = densityKgM3 * specificHeat;
        const kappa = Math.max(0, thermalConductivityW_MK) / Math.max(1, cVol);
        return parseFloat(kappa.toExponential(4));
    }

    // --- Lithospheric Flexure, Free-Air Anomaly & Regolith Bulk Density Solvers ---

    /**
     * Calculate Turcotte & Schubert (2002) lithospheric flexural rigidity and central deflection.
     * D = (E * Te^3) / (12 * (1 - nu^2)), alpha = [ 4 * D / (delta_rho * g) ]^(1/4)
     * @param {number} [loadRadiusKm=150] - Volcanic shield radius in km (e.g. Olympus Mons ~ 150 km)
     * @param {number} [loadHeightKm=10] - Volcano load height in km
     * @param {number} [elasticThicknessTeKm=50] - Effective elastic thickness Te in km
     * @param {number} [youngsModulusGPa=100] - Young's modulus E in GPa (crustal basalt ~ 100 GPa)
     * @returns {{flexuralRigidityNm: number, flexuralParameterKm: number, maxDeflectionKm: number}}
     */
    static computeLithosphericFlexure(loadRadiusKm = 150, loadHeightKm = 10, elasticThicknessTeKm = 50, youngsModulusGPa = 100) {
        const E = youngsModulusGPa * 1e9; // Pa
        const nu = 0.25; // Poisson ratio for rock
        const TeM = elasticThicknessTeKm * 1000.0;
        const g = 3.72076;
        const rhoLoad = 2900.0; // Basalt load
        const deltaRho = 3500.0 - 2900.0; // Mantle - crust density contrast (600 kg/m^3)

        // Rigidity D = E * Te^3 / (12 * (1 - nu^2))
        const D = (E * Math.pow(TeM, 3)) / (12.0 * (1.0 - nu * nu));

        // Flexural parameter alpha = (4 * D / (delta_rho * g))^(1/4)
        const alphaM = Math.pow((4.0 * D) / (deltaRho * g), 0.25);
        const alphaKm = alphaM / 1000.0;

        // Central deflection w0 ~ (rho_load * h_load) / delta_rho * (loadRadius / alpha)^2 / 8
        const rRatio = Math.max(0.1, loadRadiusKm / alphaKm);
        const w0Km = (rhoLoad * loadHeightKm / deltaRho) * (rRatio * rRatio * 0.125);

        return {
            flexuralRigidityNm: parseFloat(D.toExponential(4)),
            flexuralParameterKm: parseFloat(alphaKm.toFixed(2)),
            maxDeflectionKm: parseFloat(w0Km.toFixed(2))
        };
    }

    /**
     * Calculate pure Free-Air Gravity Anomaly without Bouguer plate subtraction.
     * Delta_g_FA = (g_obs - g_theor + 2 * g0 / R * h) * 1e5 mGal
     * @param {number} observedGravityMs2 - Measured gravity in m/s^2
     * @param {number} theoreticalGravityMs2 - Reference ellipsoid gravity in m/s^2
     * @param {number} elevationMeters - Elevation above datum in meters
     * @param {string} [body='mars'] - Planetary body
     * @returns {number} Free-air gravity anomaly in mGal
     */
    static computeFreeAirGravityAnomaly(observedGravityMs2, theoreticalGravityMs2, elevationMeters, body = 'mars') {
        const R = (body.toLowerCase() === 'moon' ? 1737.4 : 3389.5) * 1000.0;
        const freeAirGrad = (2.0 * theoreticalGravityMs2) / R;
        const deltaFA_ms2 = freeAirGrad * elevationMeters;

        const anomaly_ms2 = (observedGravityMs2 - theoreticalGravityMs2) + deltaFA_ms2;
        const anomaly_mGal = anomaly_ms2 * 1e5;

        return parseFloat(anomaly_mGal.toFixed(2));
    }

    /**
     * Calculate bulk density of porous regolith given porosity and solid grain density.
     * rho_bulk = (1 - phi) * rho_grain + phi * rho_pore
     * @param {number} [porosityFraction=0.40] - Volumetric porosity phi (0.0 to 1.0)
     * @param {number} [grainDensityKgM3=2900] - Basaltic grain density in kg/m^3
     * @param {number} [poreDensityKgM3=0] - Pore filler density (0 for vacuum/gas, 920 for ice)
     * @returns {number} Bulk density in kg/m^3
     */
    static computeBulkRegolithDensity(porosityFraction = 0.40, grainDensityKgM3 = 2900, poreDensityKgM3 = 0) {
        const phi = Math.max(0, Math.min(1.0, porosityFraction));
        const rhoBulk = (1.0 - phi) * grainDensityKgM3 + phi * poreDensityKgM3;
        return parseFloat(rhoBulk.toFixed(1));
    }

    // --- Grain Size Inversion, Axisymmetric Flexural Moat & Pratt Isostasy Solvers ---

    /**
     * Invert effective regolith grain size (in mm and microns) from thermal inertia (Piqueux & Christensen 2011).
     * d_grain = 0.05 * (I / 100)^2.2 mm
     * @param {number} thermalInertia - Thermal inertia (SI units: J m^-2 K^-1 s^-1/2)
     * @param {number} [pressurePa=610.0] - Ambient surface pressure in Pa
     * @returns {{grainSizeMm: number, grainSizeMicrons: number, WentworthClass: string}}
     */
    static computeEffectiveGrainSizeFromThermalInertia(thermalInertia, pressurePa = 610.0) {
        const I = Math.max(20, thermalInertia);
        const pRatio = Math.pow(610.0 / Math.max(10, pressurePa), 0.2);
        const dMm = 0.05 * Math.pow(I / 100.0, 2.2) * pRatio;
        const dMicrons = dMm * 1000.0;

        let wClass = 'Very Fine Silt / Airborne Dust (<10 µm)';
        if (dMicrons > 2000) wClass = 'Granules / Pebbles / Duricrust (>2 mm)';
        else if (dMicrons > 500) wClass = 'Coarse Sand (500-2000 µm)';
        else if (dMicrons > 125) wClass = 'Medium / Fine Sand (125-500 µm)';
        else if (dMicrons > 63) wClass = 'Very Fine Sand (63-125 µm)';
        else if (dMicrons > 10) wClass = 'Coarse Silt (10-63 µm)';

        return {
            grainSizeMm: parseFloat(dMm.toFixed(3)),
            grainSizeMicrons: parseFloat(dMicrons.toFixed(1)),
            WentworthClass: wClass
        };
    }

    /**
     * Calculate 1D axisymmetric lithospheric flexural deflection profile w(r) for volcanic loading.
     * w(r) = w0 * exp(-r / alpha) * cos(r / alpha)
     * @param {number} distanceRadiusKm - Radial distance from volcanic load center in km (r)
     * @param {number} [centralDeflectionKm=5.0] - Maximum central downward deflection w0 in km
     * @param {number} [flexuralParameterKm=180.0] - Flexural wavelength parameter alpha in km
     * @returns {{deflectionKm: number, deflectionMeters: number, isBulgeForebulge: boolean}}
     */
    static computeAxisymmetricFlexuralProfile(distanceRadiusKm, centralDeflectionKm = 5.0, flexuralParameterKm = 180.0) {
        const r = Math.max(0, distanceRadiusKm);
        const alpha = Math.max(10, flexuralParameterKm);
        const arg = r / alpha;

        const w = centralDeflectionKm * Math.exp(-arg) * Math.cos(arg);

        return {
            deflectionKm: parseFloat(w.toFixed(3)),
            deflectionMeters: parseFloat((w * 1000.0).toFixed(1)),
            isBulgeForebulge: w < 0 // Negative deflection = flexural forebulge / peripheral uplift
        };
    }

    /**
     * Calculate Pratt-Hayford isostatic crustal column density variation.
     * rho(h) = rho0 * (D / (D + h))
     * @param {number} topographyHeightKm - Topographic elevation above datum in km
     * @param {number} [compensationDepthKm=100.0] - Depth of isostatic compensation in km (D)
     * @param {number} [referenceDensityKgM3=2900.0] - Reference crustal density
     * @returns {{prattDensityKgM3: number, densityDeficitKgM3: number}}
     */
    static computePrattIsostaticDensity(topographyHeightKm, compensationDepthKm = 100.0, referenceDensityKgM3 = 2900.0) {
        const h = topographyHeightKm;
        const D = Math.max(10, compensationDepthKm);
        const rhoPratt = referenceDensityKgM3 * (D / (D + h));
        const deficit = referenceDensityKgM3 - rhoPratt;

        return {
            prattDensityKgM3: parseFloat(rhoPratt.toFixed(1)),
            densityDeficitKgM3: parseFloat(deficit.toFixed(1))
        };
    }

    // --- Line-Load Flexure Profile, Seismic Wave Velocity & Bouguer Attraction Solvers ---

    /**
     * Calculate 2D line-load lithospheric flexural deflection profile w(x).
     * w(x) = w0 * exp(-x / alpha) * (cos(x / alpha) + sin(x / alpha))
     * @param {number} distanceKm - Perpendicular distance from volcanic rift/ridge load in km
     * @param {number} [centralDeflectionKm=4.0] - Maximum central deflection w0 in km
     * @param {number} [flexuralParameterKm=150.0] - Flexural parameter alpha in km
     * @returns {{deflectionKm: number, deflectionMeters: number, isForebulge: boolean}}
     */
    static computeLineLoadFlexureProfile(distanceKm, centralDeflectionKm = 4.0, flexuralParameterKm = 150.0) {
        const x = Math.max(0, distanceKm);
        const alpha = Math.max(10, flexuralParameterKm);
        const arg = x / alpha;

        const w = centralDeflectionKm * Math.exp(-arg) * (Math.cos(arg) + Math.sin(arg));

        return {
            deflectionKm: parseFloat(w.toFixed(3)),
            deflectionMeters: parseFloat((w * 1000.0).toFixed(1)),
            isForebulge: w < 0
        };
    }

    /**
     * Calculate crustal seismic compressional (P-wave) and shear (S-wave) velocities.
     * Vp = sqrt( (K + 4/3*G) / rho ),  Vs = sqrt( G / rho )
     * @param {number} [bulkModulusGPa=50.0] - Elastic Bulk modulus K in GPa
     * @param {number} [shearModulusGPa=30.0] - Elastic Shear modulus G in GPa
     * @param {number} [densityKgM3=2900.0] - Rock density in kg/m^3
     * @returns {{vP_KmS: number, vS_KmS: number, vpVsRatio: number, poissonRatio: number}}
     */
    static computeSeismicPWaveVelocity(bulkModulusGPa = 50.0, shearModulusGPa = 30.0, densityKgM3 = 2900.0) {
        const K_Pa = bulkModulusGPa * 1e9;
        const G_Pa = shearModulusGPa * 1e9;
        const rho = Math.max(100, densityKgM3);

        const vP_m = Math.sqrt((K_Pa + (4.0 / 3.0) * G_Pa) / rho);
        const vS_m = Math.sqrt(G_Pa / rho);

        const vP_km = vP_m / 1000.0;
        const vS_km = vS_m / 1000.0;
        const ratio = vP_km / vS_km;

        // Poisson ratio nu = (Vp^2 - 2*Vs^2) / (2*(Vp^2 - Vs^2))
        const nu = (Math.pow(ratio, 2) - 2.0) / (2.0 * (Math.pow(ratio, 2) - 1.0));

        return {
            vP_KmS: parseFloat(vP_km.toFixed(2)),
            vS_KmS: parseFloat(vS_km.toFixed(2)),
            vpVsRatio: parseFloat(ratio.toFixed(3)),
            poissonRatio: parseFloat(nu.toFixed(3))
        };
    }

    /**
     * Calculate infinite slab Bouguer gravitational attraction in mGal.
     * delta_g = 2 * pi * G * rho * h * 1e5 mGal
     * @param {number} elevationMeters - Slab thickness/elevation in meters
     * @param {number} [densityKgM3=2900.0] - Slab density in kg/m^3
     * @returns {{bouguerAttractionMGal: number, attractionPerMeterMGal: number}}
     */
    static computeInfiniteSlabBouguerAttraction(elevationMeters, densityKgM3 = 2900.0) {
        const G = 6.67430e-11;
        const rho = Math.max(0, densityKgM3);
        const h = elevationMeters;

        const deltaG_ms2 = 2.0 * Math.PI * G * rho * h;
        const deltaG_mGal = deltaG_ms2 * 1e5;
        const perMeter_mGal = 2.0 * Math.PI * G * rho * 1e5;

        return {
            bouguerAttractionMGal: parseFloat(deltaG_mGal.toFixed(2)),
            attractionPerMeterMGal: parseFloat(perMeter_mGal.toFixed(4))
        };
    }

    // --- Airy Crustal Root, Flexural Rigidity & Buried Point Mass Gravity Solvers ---

    /**
     * Calculate Airy-Heiskanen isostatic crustal compensation root thickness in meters and km.
     * t_root = h * (rho_c / (rho_m - rho_c))
     * @param {number} topographyMeters - Surface elevation / topography height in meters
     * @param {number} [crustDensityKgM3=2900.0] - Crustal density in kg/m^3
     * @param {number} [mantleDensityKgM3=3500.0] - Upper mantle density in kg/m^3
     * @returns {{rootThicknessMeters: number, rootThicknessKm: number, totalCrustalColumnKm: number}}
     */
    static computeAiryRootThickness(topographyMeters, crustDensityKgM3 = 2900.0, mantleDensityKgM3 = 3500.0) {
        const h = Math.max(0, topographyMeters);
        const rhoC = crustDensityKgM3;
        const rhoM = mantleDensityKgM3;
        const deltaRho = Math.max(10.0, rhoM - rhoC);

        const rootM = h * (rhoC / deltaRho);
        const rootKm = rootM / 1000.0;
        const totalColKm = 50.0 + (h / 1000.0) + rootKm; // 50 km reference crust

        return {
            rootThicknessMeters: parseFloat(rootM.toFixed(1)),
            rootThicknessKm: parseFloat(rootKm.toFixed(3)),
            totalCrustalColumnKm: parseFloat(totalColKm.toFixed(3))
        };
    }

    /**
     * Calculate lithospheric flexural rigidity D.
     * D = (E * Te^3) / (12 * (1 - nu^2))
     * @param {number} elasticThicknessKm - Effective elastic thickness Te in km
     * @param {number} [youngsModulusGPa=100.0] - Young's modulus E in GPa
     * @param {number} [poissonRatio=0.25] - Poisson's ratio nu
     * @returns {{flexuralRigidityNm: number, log10Rigidity: number}}
     */
    static computeFlexuralRigidityD(elasticThicknessKm, youngsModulusGPa = 100.0, poissonRatio = 0.25) {
        const E = youngsModulusGPa * 1e9; // Pa
        const nu = poissonRatio;
        const TeM = Math.max(0.1, elasticThicknessKm) * 1000.0;

        const D = (E * Math.pow(TeM, 3)) / (12.0 * (1.0 - nu * nu));

        return {
            flexuralRigidityNm: parseFloat(D.toExponential(4)),
            log10Rigidity: parseFloat(Math.log10(D).toFixed(2))
        };
    }

    /**
     * Calculate vertical gravity anomaly delta_g_z from a buried spherical point mass in mGal.
     * delta_g_z = (G * M * z) / (x^2 + z^2)^(3/2) * 1e5 mGal
     * @param {number} massKg - Buried excess mass in kg
     * @param {number} depthMeters - Depth to mass center in meters (z)
     * @param {number} [offsetDistanceMeters=0] - Horizontal offset distance x in meters
     * @returns {{gravityAnomalyMGal: number, peakAnomalyMGal: number}}
     */
    static computePointMassGravityAnomaly(massKg, depthMeters, offsetDistanceMeters = 0) {
        const G = 6.67430e-11;
        const M = Math.max(0, massKg);
        const z = Math.max(1.0, depthMeters);
        const x = offsetDistanceMeters;

        const r2 = x * x + z * z;
        const r3 = Math.pow(r2, 1.5);

        const gz_ms2 = (G * M * z) / r3;
        const gz_mGal = gz_ms2 * 1e5;

        // Peak anomaly directly above mass (x = 0)
        const peak_ms2 = (G * M) / (z * z);
        const peak_mGal = peak_ms2 * 1e5;

        return {
            gravityAnomalyMGal: parseFloat(gz_mGal.toFixed(3)),
            peakAnomalyMGal: parseFloat(peak_mGal.toFixed(3))
        };
    }

    // --- Infinite Slab Bouguer, Crustal Density Contrast & Apparent Thermal Conductivity Solvers ---

    /**
     * Calculate gravitational attraction of an infinite Bouguer slab in mGal.
     * delta_g = 2 * pi * G * rho * h * 1e5 mGal
     * @param {number} thicknessMeters - Slab thickness/topography height in meters
     * @param {number} [densityKgM3=2900.0] - Crustal density in kg/m^3
     * @returns {{bouguerAttractionMGal: number, attractionMs2: number}}
     */
    static computeInfiniteSlabBouguerGravity(thicknessMeters, densityKgM3 = 2900.0) {
        const G = 6.67430e-11;
        const h = thicknessMeters;
        const rho = Math.max(100.0, densityKgM3);

        const deltaG_ms2 = 2.0 * Math.PI * G * rho * h;
        const deltaG_mGal = deltaG_ms2 * 1e5;

        return {
            bouguerAttractionMGal: parseFloat(deltaG_mGal.toFixed(3)),
            attractionMs2: parseFloat(deltaG_ms2.toExponential(4))
        };
    }

    /**
     * Invert bulk crustal density from observed Bouguer gravity attraction and topographic relief.
     * rho = (delta_g * 1e-5) / (2 * pi * G * h)
     * @param {number} gravityAnomalyMGal - Gravity anomaly in mGal
     * @param {number} topographyHeightMeters - Topography height in meters
     * @returns {{inferredDensityKgM3: number, densityGramsCm3: number}}
     */
    static invertCrustalDensityContrast(gravityAnomalyMGal, topographyHeightMeters) {
        const G = 6.67430e-11;
        const deltaG_ms2 = gravityAnomalyMGal * 1e-5;
        const h = Math.max(1.0, topographyHeightMeters);

        const rho = deltaG_ms2 / (2.0 * Math.PI * G * h);

        return {
            inferredDensityKgM3: parseFloat(rho.toFixed(1)),
            densityGramsCm3: parseFloat((rho / 1000.0).toFixed(3))
        };
    }

    /**
     * Calculate apparent bulk thermal conductivity from thermal inertia.
     * k = I^2 / (rho * c_p)
     * @param {number} thermalInertia - Thermal inertia in tiu (J m^-2 K^-1 s^-1/2)
     * @param {number} [bulkDensityKgM3=1500.0] - Bulk density in kg/m^3
     * @param {number} [specificHeatJ_KgK=800.0] - Specific heat capacity in J/(kg K)
     * @returns {{thermalConductivityW_MK: number, volumetricHeatCapacityJ_M3K: number}}
     */
    static computeApparentThermalConductivity(thermalInertia, bulkDensityKgM3 = 1500.0, specificHeatJ_KgK = 800.0) {
        const I = Math.max(1.0, thermalInertia);
        const rho = Math.max(100.0, bulkDensityKgM3);
        const cp = Math.max(100.0, specificHeatJ_KgK);

        const cVol = rho * cp;
        const k = (I * I) / cVol;

        return {
            thermalConductivityW_MK: parseFloat(k.toFixed(5)),
            volumetricHeatCapacityJ_M3K: parseFloat(cVol.toFixed(1))
        };
    }

    // --- Free-Air Gradient, Airy Isostatic Moho Depth & Apparent Permittivity Solvers ---

    /**
     * Calculate exact free-air gravity elevation correction in mGal.
     * delta_g_FA = (2 * g0 / R) * h * 1e5
     * @param {number} elevationMeters - Surface elevation in meters
     * @param {number} [surfaceGravityMs2=3.72076] - Planetary datum surface gravity
     * @param {number} [planetaryRadiusKm=3389.5] - Mean planetary radius in km
     * @returns {{freeAirCorrectionMGal: number, freeAirGradientMGalPerKm: number}}
     */
    static computeFreeAirGravityGradient(elevationMeters, surfaceGravityMs2 = 3.72076, planetaryRadiusKm = 3389.5) {
        const R_m = planetaryRadiusKm * 1000.0;
        const grad_ms2 = (2.0 * surfaceGravityMs2) / R_m;
        const grad_mGal_km = grad_ms2 * 1000.0 * 1e5;
        const deltaFA_mGal = grad_ms2 * elevationMeters * 1e5;

        return {
            freeAirCorrectionMGal: parseFloat(deltaFA_mGal.toFixed(3)),
            freeAirGradientMGalPerKm: parseFloat(grad_mGal_km.toFixed(3))
        };
    }

    /**
     * Calculate Airy-Heiskanen isostatic crust-mantle Moho boundary depth.
     * z_moho = z_ref + h * (rho_c / (rho_m - rho_c))
     * @param {number} surfaceElevationKm - Surface topography height in km
     * @param {number} [crustDensityKgM3=2900.0] - Crustal density
     * @param {number} [mantleDensityKgM3=3500.0] - Mantle density
     * @param {number} [referenceMohoKm=50.0] - Reference zero-elevation Moho depth
     * @returns {{crustalRootKm: number, totalMohoDepthKm: number}}
     */
    static computeAiryIsostaticMohoDepth(surfaceElevationKm, crustDensityKgM3 = 2900.0, mantleDensityKgM3 = 3500.0, referenceMohoKm = 50.0) {
        const h = Math.max(0, surfaceElevationKm);
        const deltaRho = Math.max(10.0, mantleDensityKgM3 - crustDensityKgM3);
        const rootKm = h * (crustDensityKgM3 / deltaRho);
        const totalMoho = referenceMohoKm + rootKm;

        return {
            crustalRootKm: parseFloat(rootKm.toFixed(2)),
            totalMohoDepthKm: parseFloat(totalMoho.toFixed(2))
        };
    }

    /**
     * Calculate SHARAD radar apparent subsurface relative dielectric permittivity (epsilon_r).
     * epsilon_r = ( (c * delta_t) / (2 * d) )^2
     * @param {number} subsurfaceDepthMeters - Inverted geological interface depth in meters
     * @param {number} twoWayTravelTimeMicrosec - Observed two-way travel time delay in microseconds
     * @returns {{relativePermittivityEpsR: number, propagationVelocityMPerMicrosec: number, materialEstimate: string}}
     */
    static computeApparentDielectricPermittivity(subsurfaceDepthMeters, twoWayTravelTimeMicrosec) {
        const d = Math.max(1.0, subsurfaceDepthMeters);
        const dt = Math.max(1e-4, twoWayTravelTimeMicrosec);
        const c = 299.792458; // m / µs

        const v = (2.0 * d) / dt; // m / µs
        const epsR = Math.pow(c / v, 2);

        let mat = 'Basaltic Regolith / Rock (eps ~ 4 - 8)';
        if (epsR <= 3.3) {
            mat = 'Clean Pure Water Ice (eps ~ 3.15)';
        } else if (epsR <= 3.8) {
            mat = 'Dusty / Ash-Rich Ice / Dry Porous Sand (eps ~ 3.5)';
        }

        return {
            relativePermittivityEpsR: parseFloat(epsR.toFixed(3)),
            propagationVelocityMPerMicrosec: parseFloat(v.toFixed(2)),
            materialEstimate: mat
        };
    }
}











