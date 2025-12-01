import { JMARSWMS } from '../../jmars-wms.js';
import { jmarsState } from '../../jmars-state.js';
import { JMARS_CONFIG } from '../../jmars-config.js';

export class InvestigateTool {
    constructor(map) {
        this.map = map;
        this.isActive = false;
        this.popup = L.popup({ maxWidth: 300 });
        
        this.onClick = this.onClick.bind(this);
    }

    activate() {
        this.isActive = true;
        this.map.getContainer().style.cursor = 'help';
        this.map.on('click', this.onClick);
    }

    deactivate() {
        this.isActive = false;
        this.map.getContainer().style.cursor = '';
        this.map.off('click', this.onClick);
        this.map.closePopup();
    }

    async onClick(e) {
        if (!this.isActive) return;

        const lat = e.latlng.lat;
        let lng = e.latlng.lng;
        // Normalize display Lng (0-360)
        const displayLng = lng < 0 ? lng + 360 : lng;

        // 1. Show Popup with coords immediately
        let content = `
            <div style="font-family: monospace; font-size: 12px;">
                <b>Coordinates</b><br>
                Lat: ${lat.toFixed(4)}<br>
                Lon: ${displayLng.toFixed(4)} E<br>
                <hr style="margin: 5px 0; border: 0; border-top: 1px solid #ccc;">
                <div id="investigate-loading">Querying layers...</div>
                <div id="investigate-results"></div>
            </div>
        `;

        this.popup
            .setLatLng(e.latlng)
            .setContent(content)
            .openOn(this.map);

        // 2. Query WMS Layers
        const results = await this.queryLayers(e.latlng, e.containerPoint);
        this.updatePopup(results);
    }

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
        // But JMARS_CONFIG might use a server that expects Lon,Lat (1.1.1 style) even in 1.3.0 if it's not strict.
        // USGS Astrogeology WMS 1.3.0 is usually strict (Lat,Lon).
        
        const bbox = `${bounds.getSouth()},${bounds.getWest()},${bounds.getNorth()},${bounds.getEast()}`;

        // Find Config for active layers
        // Accessing global window.jmars is a bit dirty, but we need access to availableLayers to get URL.
        // Better: JMARSMap should expose a helper, or we pass it in.
        // For now, let's assume we can access JMARS_CONFIG or the map instance if it has the data.
        
        // We can import the map instance via `window.jmars` if it's exposed, or passed in constructor.
        // In `index.html`, we exposed `window.jmars`.
        
        const availableLayers = window.jmars.availableLayers;

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

    parseFeatureInfo(html) {
        // This is highly dependent on the server output.
        // MapServer/GeoServer output simple tables.
        // We strip body tags.
        const parser = new DOMParser();
        const doc = parser.parseFromString(html, 'text/html');
        return doc.body.innerHTML;
    }

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
}
