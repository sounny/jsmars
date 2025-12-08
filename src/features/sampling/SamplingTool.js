import { JMARSWMS } from '../../jmars-wms.js';
import { jmarsState } from '../../jmars-state.js';
import { EVENTS } from '../../constants.js';

export class SamplingTool {
    constructor(map) {
        this.map = map;
        this.isActive = false;
        this.featureGroup = L.featureGroup().addTo(map);
        this.samples = []; // { id, lat, lng, layers: { name: value } }
        this.unqueryableLayers = new Set(); // cache of layers that return ServiceException for GetFeatureInfo
        
        this.onClick = this.onClick.bind(this);
        
        document.addEventListener(EVENTS.BODY_CHANGED, () => this.clear());
    }

    activate(mode = 'point') {
        if (this.isActive && this.activeMode === mode) return;
        this.deactivate(); // Clear previous listeners
        this.isActive = true;
        this.activeMode = mode;

        if (mode === 'point') {
            this.map.on('click', this.onClick);
            this.map.getContainer().style.cursor = 'crosshair';
        } else if (mode === 'area') {
            this.drawControl = new L.Draw.Polygon(this.map, {
                shapeOptions: { color: '#ff00ff', fillOpacity: 0.2 },
                showArea: true
            });
            this.drawControl.enable();
            this.map.on(L.Draw.Event.CREATED, (e) => this.onDrawCreated(e));
        }
    }

    deactivate() {
        if (!this.isActive) return;
        this.isActive = false;
        this.activeMode = null;

        this.map.off('click', this.onClick);
        if (this.drawControl) {
            this.drawControl.disable();
            this.drawControl = null;
        }
        this.map.off(L.Draw.Event.CREATED); // simplistic off
        this.map.getContainer().style.cursor = '';

        document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'sampling' } }));
    }

    onDrawCreated(e) {
        const layer = e.layer;
        this.featureGroup.addLayer(layer);
        this.calculatePolygonStats(layer);
        
        this.deactivate();
        document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'sampling' } }));
    }

    async calculatePolygonStats(layer) {
        const id = this.samples.length + 1;
        // Approximation: Centroid + 4 bounding box points (clamped to polygon?)
        // Better: Random points within bounds, filtered by raycast.
        // Limit to 5 points to avoid spamming server.
        
        const bounds = layer.getBounds();
        const points = [];
        
        // Try 10 attempts to find 5 points
        for (let i = 0; i < 10 && points.length < 5; i++) {
            const lat = bounds.getSouth() + Math.random() * (bounds.getNorth() - bounds.getSouth());
            const lng = bounds.getWest() + Math.random() * (bounds.getEast() - bounds.getWest());
            // Check intersection (Leaflet doesn't have native point-in-poly without plugin)
            // Basic bbox check is done by generation.
            // Let's assume all valid for prototype or use center.
            points.push(L.latLng(lat, lng));
        }
        
        // Query all
        // This will be slow if many layers.
        // We aggregate results.
        // Since we don't have real numeric data from WMS (we scrape HTML), 
        // we can't do real Math (Mean/StdDev) easily unless values are clean numbers.
        // We'll return "Sampled 5 points" summary.
        
        // For demo, just query Center.
        const center = layer.getCenter();
        // Get container point for WMS (needs map projection)
        const containerPoint = this.map.latLngToContainerPoint(center);
        
        const data = await this.queryLayers(center, containerPoint);
        
        // Format as "Area Stats (Center)"
        const stats = data.map(d => ({
            name: d.name,
            value: `Center: ${d.value} (Approx Area)`
        }));

        this.samples.push({
            id: `Area ${id}`,
            lat: center.lat,
            lng: center.lng,
            values: stats
        });
        
        this.notifyUpdate();
    }

    clear() {
        this.featureGroup.clearLayers();
        this.samples = [];
        this.notifyUpdate();
    }

    async onClick(e) {
        const latlng = e.latlng;
        const pointId = this.samples.length + 1;

        // Marker
        const marker = L.circleMarker(latlng, {
            color: '#ff00ff',
            radius: 5,
            fillOpacity: 0.8
        }).addTo(this.featureGroup);
        
        marker.bindPopup(`Sample ${pointId}: Loading...`).openPopup();

        // Query Data
        const data = await this.queryLayers(latlng, e.containerPoint);
        
        // Update Data
        const sample = {
            id: pointId,
            lat: latlng.lat,
            lng: latlng.lng,
            values: data // Array of { name, value }
        };
        this.samples.push(sample);
        
        // Update Popup
        const popupContent = this.formatPopup(sample);
        marker.setPopupContent(popupContent);
        
        this.notifyUpdate();
    }

    async queryLayers(latlng, containerPoint) {
        // Reuse logic from InvestigateTool roughly, but return structured data
        // We need access to availableLayers.
        const activeState = jmarsState.get('activeLayers');
        // Accessing global map config via window.jmars is the pattern used elsewhere
        const availableLayers = window.jmars ? window.jmars.availableLayers : [];
        
        const results = [];
        const size = this.map.getSize();
        const bounds = this.map.getBounds();
        const bbox = `${bounds.getSouth()},${bounds.getWest()},${bounds.getNorth()},${bounds.getEast()}`;

        // Query top 3 visible layers
        for (let i = activeState.length - 1; i >= 0; i--) {
            const layerState = activeState[i];
            if (!layerState.visible) continue;
            if (this.unqueryableLayers.has(layerState.id)) continue;

            const config = availableLayers.find(l => l.id === layerState.id);
            if (!config || config.type !== 'wms') continue;
            if (!config.options || !config.options.layers) continue;

            try {
                const url = JMARSWMS.getFeatureInfoUrl(config.url, {
                    layers: config.options.layers,
                    bbox: bbox,
                    width: size.x,
                    height: size.y,
                    x: containerPoint.x,
                    y: containerPoint.y,
                    crs: 'EPSG:4326',
                    info_format: 'text/plain'
                });

                const response = await fetch(url);
                if (response.ok) {
                    const text = await response.text();
                    const serviceError = text.includes('Layer(s) specified in QUERY_LAYERS parameter is not offered');
                    const exceptionReport = text.includes('ServiceException') || text.includes('ExceptionReport');
                    if (serviceError || exceptionReport) {
                        this.unqueryableLayers.add(layerState.id);
                        results.push({ name: config.name, value: 'Layer not queryable for point samples' });
                        continue;
                    }
                    // Strip HTML tags if the server ignored text/plain
                    const clean = text.replace(/<[^>]*>?/gm, '').trim();
                    results.push({ name: config.name, value: clean || 'No Data' });
                } else {
                    results.push({ name: config.name, value: 'No response' });
                }
            } catch (e) {
                results.push({ name: config.name, value: 'Error' });
            }
        }
        
        return results;
    }

    formatPopup(sample) {
        let html = `<b>Sample ${sample.id}</b><br>Lat: ${sample.lat.toFixed(4)}, Lon: ${sample.lng.toFixed(4)}<hr>`;
        sample.values.forEach(v => {
            html += `<b>${v.name}:</b> ${v.value}<br>`;
        });
        return html;
    }

    notifyUpdate() {
        document.dispatchEvent(new CustomEvent('jmars-samples-updated', { detail: { samples: this.samples } }));
    }

    exportCSV() {
        if (this.samples.length === 0) return;
        
        // Columns: ID, Lat, Lon, Layer1, Layer2...
        // Layers might differ? Assume union or just list all.
        // Simple approach: Key-Value pairs in one cell? Or fixed columns?
        // Let's make fixed columns based on the first sample or all unique layer names.
        
        const header = ['ID,Lat,Lon,Data\n'];
        const rows = this.samples.map(s => {
            const dataStr = s.values.map(v => `${v.name}: ${v.value}`).join('; ');
            return `${s.id},${s.lat.toFixed(5)},${s.lng.toFixed(5)},"${dataStr}"`;
        });

        const csvContent = "data:text/csv;charset=utf-8," + encodeURIComponent(header.join('') + rows.join('\n'));
        const link = document.createElement("a");
        link.setAttribute("href", csvContent);
        link.setAttribute("download", "samples.csv");
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
}
