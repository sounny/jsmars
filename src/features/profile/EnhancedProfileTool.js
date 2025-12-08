import { JMARSWMS } from '../../jmars-wms.js';
import { jmarsState } from '../../jmars-state.js';
import { molaDem } from '../../util/mola-dem.js';
import { EVENTS } from '../../constants.js';

export class EnhancedProfileTool {
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
                    name: 'ASTER GDEM (GIBS shaded relief)',
                    url: 'https://gibs.earthdata.nasa.gov/wms/epsg4326/best/wms.cgi',
                    layer: 'ASTER_GDEM_Greyscale_ShadedRelief'
                }
            ],
            moon: [
                // No elevation source available yet; disabled in UI.
            ]
        };
        this.currentSourceId = molaDem.SOURCE_ID;
        
        this.onDrawCreated = this.onDrawCreated.bind(this);
        this.onDrawStop = this.onDrawStop.bind(this);
        
        // Listen to external events if necessary
        document.addEventListener(EVENTS.BODY_CHANGED, () => this.deactivate());
    }

    activate() {
        if (this.isActive) return;
        this.isActive = true;
        const body = (jmarsState.get('body') || 'mars').toLowerCase();
        if (body === 'mars') {
            molaDem.ensureLoaded().catch(() => {});
        }
        this.featureGroup.clearLayers();
        this.profileData = [];

        this.drawControl = new L.Draw.Polyline(this.map, {
            shapeOptions: {
                color: '#00ff00',
                weight: 3
            },
            metric: true
        });
        this.drawControl.enable();

        this.map.on(L.Draw.Event.CREATED, this.onDrawCreated);
        this.map.on(L.Draw.Event.DRAWSTOP, this.onDrawStop);
        this.map.getContainer().style.cursor = 'crosshair';
    }

    deactivate() {
        if (!this.isActive) return;
        this.isActive = false;

        if (this.drawControl) {
            this.drawControl.disable();
            this.drawControl = null;
        }

        this.map.off(L.Draw.Event.CREATED, this.onDrawCreated);
        this.map.off(L.Draw.Event.DRAWSTOP, this.onDrawStop);
        this.map.getContainer().style.cursor = '';
        
        // We keep the layer on map until cleared manually? 
        // Standard JMARS behavior: Layer stays. 
    }

    clear() {
        this.featureGroup.clearLayers();
        this.profileData = [];
        document.dispatchEvent(new CustomEvent('jmars-profile-generated', { detail: { profiles: [] } }));
    }

    onDrawCreated(e) {
        const layer = e.layer;
        this.featureGroup.addLayer(layer);
        
        // Calculate Profile
        this.calculateProfile(layer);
        
        // Stop drawing after one line? Or allow multiple? 
        // User request: "Multi-segment line drawing". 
        // Usually this means one continuous polyline. 
        // L.Draw.Polyline handles segments.
        // Once "Finish" is clicked, CREATED is fired.
        
        // We typically want one active profile at a time for the chart.
        this.deactivate();
        document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'profile' } }));
    }

    onDrawStop() {
        // Handled by deactivate called in onDrawCreated usually.
    }

    async calculateProfile(layer) {
        const latlngs = layer.getLatLngs();
        // Flatten if nested (Leaflet 1.0+ handles multi-polylines differently sometimes)
        // But L.Draw.Polyline usually returns simple array of LatLng objects.
        
        const samples = [];
        // Aim for a smooth curve without too many fetches.
        const targetSamples = 150;
        
        let totalDist = 0;
        for (let i = 0; i < latlngs.length - 1; i++) {
            totalDist += latlngs[i].distanceTo(latlngs[i+1]);
        }

        const stepSize = totalDist / targetSamples; 
        
        let currentDist = 0;
        // Generate points
        for (let i = 0; i < latlngs.length - 1; i++) {
            const start = latlngs[i];
            const end = latlngs[i+1];
            const segmentDist = start.distanceTo(end);
            
            let segmentProgress = 0;
            while(segmentProgress < segmentDist) {
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

        const profileData = await this.populateElevations(samples);

        this.profileData = profileData;

        // Format for ProfileChart
        // ProfileChart expects array of objects with { color, data: [{dist, elev}] }
        const chartData = [{
            color: '#00ff00',
            data: profileData
        }];

        document.dispatchEvent(new CustomEvent('jmars-profile-generated', { detail: { profiles: chartData } }));
    }

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

        // Limit requests
        const maxSamples = 60;
        const step = Math.max(1, Math.floor(samples.length / maxSamples));
        const reduced = samples.filter((_, idx) => idx % step === 0);

        const mapSize = this.map.getSize();
        const bounds = this.map.getBounds();
        const bbox = `${bounds.getSouth()},${bounds.getWest()},${bounds.getNorth()},${bounds.getEast()}`;
        const results = [];

        for (const s of reduced) {
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
                const elev = match ? parseFloat(match[0]) : null;
                results.push({
                    dist: s.dist,
                    elev: elev !== null ? elev : null,
                    lat: s.lat,
                    lng: s.lng
                });
            } catch (e) {
                results.push({
                    dist: s.dist,
                    elev: null,
                    lat: s.lat,
                    lng: s.lng
                });
            }
        }

        if (results.length !== samples.length) {
            const interpolated = [];
            for (let i = 0; i < samples.length; i++) {
                const ratio = i / (samples.length - 1);
                const pos = ratio * (results.length - 1);
                const idx = Math.floor(pos);
                const t = pos - idx;
                const a = results[idx];
                const b = results[Math.min(idx + 1, results.length - 1)];
                const elev = a.elev + (b.elev - a.elev) * t;
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
