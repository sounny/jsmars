import { JMARSWMS } from '../../jmars-wms.js';
import { jmarsState } from '../../jmars-state.js';
import { molaDem } from '../../util/mola-dem.js';
import { EVENTS } from '../../constants.js';

export class RadialProfileTool {
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
                document.dispatchEvent(new CustomEvent('jmars-profile-generated', { detail: { profiles } }));
            });
        }

        // Initial dispatch to clear chart
        document.dispatchEvent(new CustomEvent('jmars-profile-generated', { detail: { profiles } }));
    }

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

    getColor(index) {
        const colors = ['#e6194b', '#3cb44b', '#ffe119', '#4363d8', '#f58231', '#911eb4', '#46f0f0', '#f032e6'];
        return colors[index % colors.length];
    }
}
