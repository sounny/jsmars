import { JMARSWMS } from '../../jmars-wms.js';
import { jmarsState } from '../../jmars-state.js';
import { molaDem } from '../../util/mola-dem.js';
import { haversineDistance } from '../../util/geo.js';
import { applyLeafletDrawPatch } from '../../util/leaflet-draw-patch.js';
import { EVENTS } from '../../constants.js';

/**
 * @module EnhancedProfileTool
 * @description Linear elevation profile tool.
 *
 * Lets the user draw a polyline on the map, then samples
 * elevation data along the line and dispatches PROFILE_GENERATED.
 */
export class EnhancedProfileTool {
    /**
     * Create an EnhancedProfileTool.
     * @param {L.Map} map - The Leaflet map instance.
     */
    constructor(map) {
        this.map = map;
        this.isActive = false;
        this.featureGroup = L.featureGroup().addTo(map);
        this.drawControl = null;
        this.profileData = [];
        this.elevationSources = {
            mars: [
                {
                    id: molaDem.SOURCE_ID,
                    name: molaDem.SOURCE_NAME,
                    type: 'dem'
                }
            ],
            earth: [
                {
                    id: 'aster_gdem',
                    name: 'Copernicus / SRTM Terrain Relief (EOX WMS)',
                    url: 'https://tiles.maps.eox.at/wms',
                    layer: 'terrain'
                }
            ],
            moon: [
                {
                    id: 'lola_dem',
                    name: 'LOLA Topography (USGS WMS)',
                    url: 'https://planetarymaps.usgs.gov/cgi-bin/mapserv?map=/maps/earth/moon_simp_cyl.map',
                    layer: 'LOLA_bw'
                }
            ]
        };
        this.currentSourceId = molaDem.SOURCE_ID;
        
        this.onDrawCreated = this.onDrawCreated.bind(this);
        this.onDrawStop = this.onDrawStop.bind(this);
        this.hoverMarker = null;

        // Synchronize chart mouse hovering with map cursor position
        document.addEventListener(EVENTS.PROFILE_HOVER, (e) => {
            const detail = e.detail;
            if (detail?.clear) {
                if (this.hoverMarker) {
                    this.featureGroup.removeLayer(this.hoverMarker);
                    this.hoverMarker = null;
                }
            } else if (detail && Number.isFinite(detail.lat) && Number.isFinite(detail.lng)) {
                if (!this.hoverMarker) {
                    this.hoverMarker = L.circleMarker([detail.lat, detail.lng], {
                        radius: 6,
                        color: '#ffffff',
                        weight: 2,
                        fillColor: '#38bdf8',
                        fillOpacity: 0.9
                    });
                    this.featureGroup.addLayer(this.hoverMarker);
                } else {
                    this.hoverMarker.setLatLng([detail.lat, detail.lng]);
                }
            }
        });
        
        // Listen to external events if necessary
        document.addEventListener(EVENTS.BODY_CHANGED, () => this.deactivate());
    }

    /**
     * Activate the profile tool and begin polyline drawing.
     */
    activate() {
        applyLeafletDrawPatch();
        if (this.isActive) return;
        this.isActive = true;
        if (this.map) {
            this.map._exclusiveDrawOwner = 'profile';
        }
        const body = (jmarsState.get('body') || 'mars').toLowerCase();
        if (body === 'mars') {
            molaDem.ensureLoaded().catch(() => {});
        }
        this.featureGroup.clearLayers();
        this.profileData = [];

        if (L.Draw?.Polyline) {
            this.drawControl = new L.Draw.Polyline(this.map, {
                shapeOptions: {
                    color: '#00ff00',
                    weight: 3
                },
                metric: true
            });
            this.drawControl.enable();
        }

        const drawCreatedEvt = L.Draw?.Event?.CREATED || 'draw:created';
        const drawStopEvt = L.Draw?.Event?.DRAWSTOP || 'draw:drawstop';
        this.map.on(drawCreatedEvt, this.onDrawCreated);
        this.map.on(drawStopEvt, this.onDrawStop);
        this.map.getContainer().style.cursor = 'crosshair';
        document.dispatchEvent(new CustomEvent(EVENTS.TOOL_ACTIVATED, { detail: { tool: 'profile-linear' } }));
    }

    /**
     * Deactivate the profile tool and unregister draw listeners.
     */
    deactivate() {
        if (!this.isActive) return;
        this.isActive = false;

        if (this.drawControl) {
            this.drawControl.disable();
            this.drawControl = null;
        }

        if (this.map) {
            const drawCreatedEvt = L.Draw?.Event?.CREATED || 'draw:created';
            const drawStopEvt = L.Draw?.Event?.DRAWSTOP || 'draw:drawstop';
            this.map.off(drawCreatedEvt, this.onDrawCreated);
            this.map.off(drawStopEvt, this.onDrawStop);
            if (this.map._exclusiveDrawOwner === 'profile') {
                this.map._exclusiveDrawOwner = null;
            }
            this.map.getContainer().style.cursor = '';
        }
    }

    /**
     * Clear all profile lines and data from the map.
     */
    clear() {
        this.featureGroup.clearLayers();
        this.profileData = [];
        document.dispatchEvent(new CustomEvent(EVENTS.PROFILE_GENERATED, { detail: { profiles: [] } }));
    }

    /**
     * Handle draw:created event from Leaflet.Draw.
     * @param {object} e - Leaflet draw event.
     */
    onDrawCreated(e) {
        e._handledByTool = 'profile';
        const layer = e.layer;
        this.featureGroup.addLayer(layer);

        this.calculateProfile(layer);

        this.deactivate();
        document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'profile' } }));
    }

    onDrawStop() {
        if (this.isActive) {
            this.deactivate();
            document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'profile' } }));
        }
    }

    /**
     * Parse elevation text returned from a WMS GetFeatureInfo endpoint safely.
     * Rejects XML/HTML error pages and sentinel nodata values.
     * @param {string} text
     * @returns {number|null}
     */
    static parseWmsElevationText(text) {
        if (!text || typeof text !== 'string') return null;
        if (/ServiceException|<\?xml|<html|<!DOCTYPE|ExceptionReport/i.test(text)) {
            return null;
        }
        const explicit = text.match(/(?:value_0|elevation|band\s*1|gray_index|pixel)\s*[:=]\s*(-?\d+(?:\.\d+)?)/i);
        const match = explicit || text.match(/(?:^|\s)(-?\d+(?:\.\d+)?)(?:\s|$)/);
        if (!match) return null;
        const val = parseFloat(match[1]);
        if (!Number.isFinite(val) || Math.abs(val) > 1e5 || val <= -32760) {
            return null;
        }
        return val;
    }

    /**
     * Calculate elevation profile along the drawn polyline.
     * @param {L.Polyline} layer - The drawn polyline layer.
     */
    async calculateProfile(layer) {
        const rawLatLngs = layer.getLatLngs();
        const latlngs = Array.isArray(rawLatLngs[0]) ? rawLatLngs[0] : rawLatLngs;
        if (!latlngs || latlngs.length < 2) {
            this.profileData = [];
            document.dispatchEvent(new CustomEvent(EVENTS.PROFILE_GENERATED, { detail: { profiles: [] } }));
            return;
        }

        const body = (jmarsState.get('body') || 'mars').toLowerCase();
        const segMeters = (a, b) => haversineDistance(a.lat, a.lng, b.lat, b.lng, body) * 1000;

        const samples = [];
        // Aim for a smooth curve without too many fetches.
        const targetSamples = 150;

        let totalDist = 0;
        for (let i = 0; i < latlngs.length - 1; i++) {
            totalDist += segMeters(latlngs[i], latlngs[i + 1]);
        }

        // Guard: avoid division by zero on zero-length lines
        if (totalDist === 0) {
            this.profileData = [];
            document.dispatchEvent(new CustomEvent(EVENTS.PROFILE_GENERATED, { detail: { profiles: [] } }));
            return;
        }

        document.dispatchEvent(new CustomEvent(EVENTS.PROFILE_LOADING, { detail: { body } }));
        document.getElementById('profile-chart-container')?.scrollIntoView({ behavior: 'smooth', block: 'nearest' });

        const stepSize = totalDist / targetSamples;

        let currentDist = 0;
        // Generate points
        for (let i = 0; i < latlngs.length - 1; i++) {
            const start = latlngs[i];
            const end = latlngs[i + 1];
            const segmentDist = segMeters(start, end);
            if (segmentDist <= 0) continue;

            let segmentProgress = 0;
            while (segmentProgress < segmentDist) {
                const ratio = segmentProgress / segmentDist;
                const lat = start.lat + (end.lat - start.lat) * ratio;
                const lng = start.lng + (end.lng - start.lng) * ratio;

                samples.push({
                    dist: currentDist + segmentProgress,
                    lat: lat,
                    lng: lng
                });

                segmentProgress += stepSize;
            }
            currentDist += segmentDist;
        }

        // Add last point
        const last = latlngs[latlngs.length - 1];
        samples.push({ dist: totalDist, lat: last.lat, lng: last.lng });

        const sources = this.elevationSources[body] || [];
        const hasSource = Boolean(sources.find(s => s.id === this.currentSourceId));
        const profileData = await this.populateElevations(samples);

        this.profileData = profileData;

        // Format for ProfileChart
        // ProfileChart expects array of objects with { color, data: [{dist, elev}] }
        const chartData = [{
            color: '#00ff00',
            data: profileData
        }];

        const bodyLabel = body.charAt(0).toUpperCase() + body.slice(1);
        const emptyMessage = !hasSource
            ? `No elevation DEM available for ${bodyLabel}`
            : null;

        document.dispatchEvent(new CustomEvent(EVENTS.PROFILE_GENERATED, {
            detail: { profiles: chartData, body, emptyMessage }
        }));
    }

    /**
     * Export the current profile data as a CSV file download.
     */
    exportCSV() {
        if (this.profileData.length === 0) return;
        
        const header = ['Distance_m,Elevation_m,Lat,Lon\n'];
        const rows = this.profileData.map(p => {
            const elev = Number.isFinite(p.elev) ? p.elev.toFixed(2) : '';
            return `${p.dist.toFixed(2)},${elev},${p.lat.toFixed(5)},${p.lng.toFixed(5)}`;
        });
        
        const csvContent = "data:text/csv;charset=utf-8," + encodeURIComponent(header.join('') + rows.join('\n'));
        const link = document.createElement("a");
        link.setAttribute("href", csvContent);
        link.setAttribute("download", "profile_data.csv");
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }

    setElevationSource(sourceId) {
        this.currentSourceId = sourceId;
    }

    populateSourceDropdown(selectEl) {
        if (!selectEl) return;
        const body = (jmarsState.get('body') || 'mars').toLowerCase();
        const sources = this.elevationSources[body] || [];

        selectEl.innerHTML = '';
        selectEl.disabled = sources.length === 0;
        if (sources.length === 0) {
            const opt = document.createElement('option');
            opt.value = 'none';
            opt.textContent = 'No elevation source';
            selectEl.appendChild(opt);
            this.currentSourceId = 'none';
            return;
        }

        sources.forEach((s, idx) => {
            const opt = document.createElement('option');
            opt.value = s.id;
            opt.textContent = s.name;
            selectEl.appendChild(opt);
            if (idx === 0 && !sources.find(src => src.id === this.currentSourceId)) {
                this.currentSourceId = s.id;
            }
        });

        selectEl.value = this.currentSourceId;
    }

    /**
     * Populate elevation values for an array of sample points.
     * Uses the MOLA DEM for Mars; falls back to WMS GetFeatureInfo for others.
     * @param {Array<object>} samples - Array of { dist, lat, lng }.
     * @returns {Promise<Array<object>>} Samples enriched with elev property.
     */
    async populateElevations(samples) {
        const body = (jmarsState.get('body') || 'mars').toLowerCase();
        const sources = this.elevationSources[body] || [];
        const source = sources.find(s => s.id === this.currentSourceId);

        if (body === 'mars' && source && source.id === molaDem.SOURCE_ID) {
            try {
                const elevations = await molaDem.sampleElevations(samples);
                return samples.map((s, idx) => ({
                    dist: s.dist,
                    elev: elevations[idx],
                    lat: s.lat,
                    lng: s.lng
                }));
            } catch (err) {
                console.error('MOLA DEM sampling failed', err);
                return samples.map(s => ({
                    dist: s.dist,
                    elev: null,
                    lat: s.lat,
                    lng: s.lng
                }));
            }
        }

        if (!source) {
            // No source available (e.g., moon) — return empty profile
            return samples.map(s => ({
                dist: s.dist,
                elev: null,
                lat: s.lat,
                lng: s.lng
            }));
        }

        // Limit WMS fallback requests and fetch in parallel with timeout guard
        const maxSamples = 24;
        const step = Math.max(1, Math.floor(samples.length / maxSamples));
        const reduced = samples.filter((_, idx) => idx % step === 0);

        const mapSize = this.map.getSize();
        const bounds = this.map.getBounds();
        const bbox = `${bounds.getSouth()},${bounds.getWest()},${bounds.getNorth()},${bounds.getEast()}`;

        const results = await Promise.all(reduced.map(async (s) => {
            const pt = this.map.latLngToContainerPoint([s.lat, s.lng]);
            const url = JMARSWMS.getFeatureInfoUrl(source.url, {
                layers: source.layer,
                bbox,
                width: mapSize.x,
                height: mapSize.y,
                x: pt.x,
                y: pt.y,
                crs: 'EPSG:4326',
                info_format: 'text/plain',
                version: '1.3.0'
            });

            try {
                const signal = typeof AbortSignal !== 'undefined' && AbortSignal.timeout
                    ? AbortSignal.timeout(6000)
                    : undefined;
                const resp = await fetch(url, signal ? { signal } : undefined);
                const text = await resp.text();
                const elev = EnhancedProfileTool.parseWmsElevationText(text);
                return { dist: s.dist, elev, lat: s.lat, lng: s.lng };
            } catch {
                return { dist: s.dist, elev: null, lat: s.lat, lng: s.lng };
            }
        }));

        if (results.length !== samples.length) {
            const interpolated = [];
            for (let i = 0; i < samples.length; i++) {
                const ratio = samples.length > 1 ? i / (samples.length - 1) : 0;
                const pos = ratio * Math.max(0, results.length - 1);
                const idx = Math.floor(pos);
                const t = pos - idx;
                const a = results[idx];
                const b = results[Math.min(idx + 1, results.length - 1)];
                let elev = null;
                if (Number.isFinite(a?.elev) && Number.isFinite(b?.elev)) {
                    elev = a.elev + (b.elev - a.elev) * t;
                } else if (Number.isFinite(a?.elev)) {
                    elev = a.elev;
                } else if (Number.isFinite(b?.elev)) {
                    elev = b.elev;
                }
                const s = samples[i];
                interpolated.push({
                    dist: s.dist,
                    elev,
                    lat: s.lat,
                    lng: s.lng
                });
            }
            return interpolated;
        }

        return results;
    }

}
