import { JMARSWMS } from '../../jmars-wms.js';
import { jmarsState } from '../../jmars-state.js';
import { molaDem } from '../../util/mola-dem.js';
import { EVENTS } from '../../constants.js';

/**
 * @module RadialProfileTool
 * @description Radial elevation profile tool.
 *
 * The user clicks a center point and an edge point to define a radius.
 * The tool then generates N radiating lines and samples elevation
 * along each, dispatching PROFILE_GENERATED for the chart.
 */
export class RadialProfileTool {
    /**
     * Create a RadialProfileTool.
     * @param {L.Map} map - The Leaflet map instance.
     */
    constructor(map) {
        this.map = map;
        this.isActive = false;
        this.center = null;
        this.lines = []; // Array of L.Polyline
        this.previewLine = null; // Line showing radius
        this.layerGroup = L.layerGroup().addTo(map);

        // Config
        this.numLines = 8; // Number of radiating lines
        this.stepSize = 1000; // Meters between samples
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
                    name: 'ASTER GDEM (GIBS shaded relief)',
                    url: 'https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi',
                    layer: 'ASTER_GDEM_Greyscale_ShadedRelief'
                }
            ],
            moon: [
                // no source yet
            ]
        };
        this.currentSourceId = molaDem.SOURCE_ID;

        // Bindings
        this.onClick = this.onClick.bind(this);
        this.onMouseMove = this.onMouseMove.bind(this);
    }

    /**
     * Activate the radial profile tool and listen for clicks.
     */
    activate() {
        if (this.isActive) return;
        this.isActive = true;
        const body = (jmarsState.get('body') || 'mars').toLowerCase();
        if (body === 'mars') {
            molaDem.ensureLoaded().catch(() => {});
        }
        this.reset();

        this.map.on('click', this.onClick);
        this.map.on('mousemove', this.onMouseMove);
        this.map.getContainer().style.cursor = 'crosshair';
    }

    /**
     * Deactivate the radial profile tool.
     */
    deactivate() {
        if (!this.isActive) return;
        this.isActive = false;

        this.map.off('click', this.onClick);
        this.map.off('mousemove', this.onMouseMove);
        this.map.getContainer().style.cursor = '';
        this.reset();
        document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'profile' } }));
    }

    reset() {
        this.center = null;
        this.layerGroup.clearLayers();
        if (this.previewLine) {
            this.previewLine.remove();
            this.previewLine = null;
        }
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

    onClick(e) {
        if (!this.center) {
            // Set Center
            this.center = e.latlng;

            // Draw Center marker
            L.circleMarker(this.center, { radius: 5, color: '#0ff' }).addTo(this.layerGroup);
        } else {
            // Set Radius and Finish
            const endPoint = e.latlng;
            const radius = this.map.distance(this.center, endPoint);
            this.generateProfile(this.center, radius);
            if (this.previewLine) {
                this.previewLine.remove();
                this.previewLine = null;
            }

            // Reset state to allow new profile? Or keep it?
            // Let's keep it until user clicks "Stop" or "Clear"
            // Actually, usually you want to do one profile then stop.
            // But we can just let them click again to restart?
            // For now, let's just finish the interaction part.
            this.center = null; // Reset for next one?
            // No, let's leave the lines on screen.
            this.map.off('click', this.onClick);
            this.map.off('mousemove', this.onMouseMove);
            this.map.getContainer().style.cursor = '';
            this.isActive = false;
            document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'profile' } }));

            // Notify UI we are done (optional)
        }
    }

    onMouseMove(e) {
        if (this.center) {
            // Draw preview radius line
            if (!this.previewLine) {
                this.previewLine = L.polyline([this.center, e.latlng], { color: '#0ff', dashArray: '5, 5' }).addTo(this.map);
            } else {
                this.previewLine.setLatLngs([this.center, e.latlng]);
            }
        }
    }

    setElevationSource(sourceId) {
        this.currentSourceId = sourceId;
    }

    /**
     * Generate radial elevation profiles from a center point.
     * @param {L.LatLng} center - Center point.
     * @param {number} radius - Radius in meters.
     */
    generateProfile(center, radius) {
        this.layerGroup.clearLayers();
        L.circleMarker(center, { radius: 5, color: '#0ff' }).addTo(this.layerGroup);

        const profiles = [];

        for (let i = 0; i < this.numLines; i++) {
            const angleDeg = (360 / this.numLines) * i;
            const angleRad = (angleDeg * Math.PI) / 180;
            const body = (jmarsState.get('body') || 'mars').toLowerCase();
            const R = body === 'earth' ? 6371000 : 3396190; // approximate radii in meters

            // Calculate end point using simple approximation or proper geodesy
            // Leaflet has tools, but we can just use geometry for short distances or find a point.
            // L.GeometryUtil (plugin) or just simulate.
            // Given we are on a sphere...
            // Simple way: Project to pixels, move, unproject?
            // Better: use Destination point formula.
            // lat2 = asin(sin(lat1)*cos(d/R) + cos(lat1)*sin(d/R)*cos(brng))
            // lon2 = lon1 + atan2(sin(brng)*sin(d/R)*cos(lat1), cos(d/R)-sin(lat1)*sin(lat2))

            const d = radius;
            const lat1 = center.lat * Math.PI / 180;
            const lon1 = center.lng * Math.PI / 180;

            const lat2 = Math.asin(Math.sin(lat1) * Math.cos(d / R) + Math.cos(lat1) * Math.sin(d / R) * Math.cos(angleRad));
            const lon2 = lon1 + Math.atan2(Math.sin(angleRad) * Math.sin(d / R) * Math.cos(lat1), Math.cos(d / R) - Math.sin(lat1) * Math.sin(lat2));

            const endLat = lat2 * 180 / Math.PI;
            const endLng = lon2 * 180 / Math.PI;

            const linePoints = [center, L.latLng(endLat, endLng)];

            // Draw line
            L.polyline(linePoints, { color: this.getColor(i), weight: 2 }).addTo(this.layerGroup);

            // Generate Data (async)
            profiles.push({
                angle: angleDeg,
                color: this.getColor(i),
                data: []
            });
            this.sampleElevations(linePoints).then((dataPoints) => {
                const target = profiles.find(p => p.angle === angleDeg);
                if (target) target.data = dataPoints;
                document.dispatchEvent(new CustomEvent(EVENTS.PROFILE_GENERATED, { detail: { profiles } }));
            });
        }

        // Initial dispatch to clear chart
        document.dispatchEvent(new CustomEvent(EVENTS.PROFILE_GENERATED, { detail: { profiles } }));
    }

    /**
     * Sample elevation values along a two-point line segment.
     * @param {Array<L.LatLng>} linePoints - [start, end] coordinates.
     * @returns {Promise<Array<object>>} Array of { dist, elev }.
     */
    sampleElevations(linePoints) {
        const [start, end] = linePoints;
        const samples = [];
        const steps = 50;
        const totalDist = this.map.distance(start, end);
        for (let s = 0; s <= steps; s++) {
            const ratio = s / steps;
            const lat = start.lat + (end.lat - start.lat) * ratio;
            const lng = start.lng + (end.lng - start.lng) * ratio;
            samples.push({ dist: ratio * totalDist, lat, lng });
        }
        return this.populateElevations(samples);
    }

    async populateElevations(samples) {
        const body = (jmarsState.get('body') || 'mars').toLowerCase();
        const sources = this.elevationSources[body] || [];
        const source = sources.find(s => s.id === this.currentSourceId);

        if (body === 'mars' && source && source.id === molaDem.SOURCE_ID) {
            try {
                const elevations = await molaDem.sampleElevations(samples);
                return samples.map((s, idx) => ({ dist: s.dist, elev: elevations[idx] }));
            } catch (err) {
                console.error('MOLA DEM sampling failed (radial)', err);
                return samples.map(s => ({ dist: s.dist, elev: null }));
            }
        }

        if (!source) {
            return samples.map(s => ({ dist: s.dist, elev: null }));
        }

        const mapSize = this.map.getSize();
        const bounds = this.map.getBounds();
        const bbox = `${bounds.getSouth()},${bounds.getWest()},${bounds.getNorth()},${bounds.getEast()}`;
        const dataPoints = [];

        for (const s of samples) {
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
                const resp = await fetch(url);
                const text = await resp.text();
                const match = text.match(/-?\d+\.?\d*/);
                dataPoints.push({ dist: s.dist, elev: match ? parseFloat(match[0]) : null });
            } catch {
                dataPoints.push({ dist: s.dist, elev: null });
            }
        }

        return dataPoints;
    }

    /**
     * Return a distinct color for a radial line by index.
     * @param {number} index - Line index.
     * @returns {string} CSS color string.
     */
    getColor(index) {
        const colors = ['#e6194b', '#3cb44b', '#ffe119', '#4363d8', '#f58231', '#911eb4', '#46f0f0', '#f032e6'];
        return colors[index % colors.length];
    }

    // --- Crater Morphometry & Radial Profile Analytics ---

    /**
     * Compute average radial profile and standard deviation across multi-azimuth spokes.
     * @param {Array<{data: Array<{dist: number, elev: number}>}>} profiles
     * @returns {Array<{dist: number, meanElev: number, minElev: number, maxElev: number, stdElev: number}>}
     */
    static computeAverageProfile(profiles = []) {
        const validProfiles = profiles.filter(p => Array.isArray(p.data) && p.data.length > 0);
        if (validProfiles.length === 0) return [];

        const numSteps = validProfiles[0].data.length;
        const avg = [];

        for (let s = 0; s < numSteps; s++) {
            const dist = validProfiles[0].data[s].dist;
            const elevs = validProfiles
                .map(p => p.data[s]?.elev)
                .filter(v => typeof v === 'number' && Number.isFinite(v));

            if (elevs.length === 0) {
                avg.push({ dist, meanElev: 0, minElev: 0, maxElev: 0, stdElev: 0 });
                continue;
            }

            const minElev = Math.min(...elevs);
            const maxElev = Math.max(...elevs);
            const sum = elevs.reduce((a, b) => a + b, 0);
            const meanElev = sum / elevs.length;

            const sumSqDiff = elevs.reduce((a, b) => a + (b - meanElev) * (b - meanElev), 0);
            const stdElev = Math.sqrt(sumSqDiff / elevs.length);

            avg.push({
                dist: parseFloat(dist.toFixed(1)),
                meanElev: parseFloat(meanElev.toFixed(1)),
                minElev: parseFloat(minElev.toFixed(1)),
                maxElev: parseFloat(maxElev.toFixed(1)),
                stdElev: parseFloat(stdElev.toFixed(2))
            });
        }

        return avg;
    }

    /**
     * Detect crater rim crest, floor elevation, and apparent depth from a radial profile.
     * @param {Array<{dist: number, meanElev: number}>} avgProfile
     * @returns {{rimRadiusM: number, rimElevM: number, floorElevM: number, apparentDepthM: number, depthToDiameterRatio: number}}
     */
    static detectCraterRimAndDepth(avgProfile = []) {
        if (!avgProfile || avgProfile.length < 3) {
            return { rimRadiusM: 0, rimElevM: 0, floorElevM: 0, apparentDepthM: 0, depthToDiameterRatio: 0 };
        }

        let maxElev = -Infinity;
        let rimIdx = 0;

        for (let i = 0; i < avgProfile.length; i++) {
            if (avgProfile[i].meanElev > maxElev) {
                maxElev = avgProfile[i].meanElev;
                rimIdx = i;
            }
        }

        const rimRadiusM = avgProfile[rimIdx].dist;
        const rimElevM = maxElev;

        // Floor is lowest point inside rim radius
        let minElev = Infinity;
        for (let i = 0; i <= rimIdx; i++) {
            if (avgProfile[i].meanElev < minElev) {
                minElev = avgProfile[i].meanElev;
            }
        }

        const floorElevM = minElev;
        const apparentDepthM = Math.max(0, rimElevM - floorElevM);
        const diameterM = rimRadiusM * 2.0;
        const depthToDiameterRatio = diameterM > 0 ? apparentDepthM / diameterM : 0;

        return {
            rimRadiusM: parseFloat(rimRadiusM.toFixed(1)),
            rimElevM: parseFloat(rimElevM.toFixed(1)),
            floorElevM: parseFloat(floorElevM.toFixed(1)),
            apparentDepthM: parseFloat(apparentDepthM.toFixed(1)),
            depthToDiameterRatio: parseFloat(depthToDiameterRatio.toFixed(3))
        };
    }

    /**
     * Calculate 3D circular cavity volume deficit beneath the crater rim crest.
     * @param {Array<{dist: number, meanElev: number}>} avgProfile
     * @param {number} rimRadiusM
     * @returns {{cavityVolumeM3: number, cavityVolumeKm3: number}}
     */
    static computeCavityVolume(avgProfile = [], rimRadiusM = 0) {
        if (!avgProfile || avgProfile.length < 2) {
            return { cavityVolumeM3: 0, cavityVolumeKm3: 0 };
        }

        const rim = this.detectCraterRimAndDepth(avgProfile);
        const rMax = rimRadiusM > 0 ? rimRadiusM : rim.rimRadiusM;
        const zRim = rim.rimElevM;

        let totalVolM3 = 0;

        for (let i = 0; i < avgProfile.length - 1; i++) {
            const r1 = avgProfile[i].dist;
            const r2 = avgProfile[i + 1].dist;
            if (r1 >= rMax) break;

            const clampedR2 = Math.min(r2, rMax);
            const dr = clampedR2 - r1;
            const rMid = (r1 + clampedR2) / 2.0;

            const z1 = avgProfile[i].meanElev;
            const z2 = avgProfile[i + 1].meanElev;
            const zMid = (z1 + z2) / 2.0;

            const depthDeficit = Math.max(0, zRim - zMid);
            // Annular shell volume: dV = 2 * pi * r * depth * dr
            const dV = 2 * Math.PI * rMid * depthDeficit * dr;
            totalVolM3 += dV;
        }

        return {
            cavityVolumeM3: totalVolM3,
            cavityVolumeKm3: totalVolM3 / 1e9
        };
    }
}

