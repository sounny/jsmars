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
            const ltst = MarsTime.computeLTST(marsState.Ls, marsState.MTC, lng360);
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
}
