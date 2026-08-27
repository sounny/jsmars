import { JMARSWMS } from '../../jmars-wms.js';
import { jmarsState } from '../../jmars-state.js';
import { EVENTS } from '../../constants.js';

/**
 * @module SamplingTool
 * @description Point and area sampling tool for querying WMS layer values.
 *
 * In point mode, the user clicks the map to drop sample markers.
 * In area mode, a polygon is drawn and the centroid is queried.
 * Results are dispatched via SAMPLE_UPDATED.
 */
export class SamplingTool {
    /**
     * Create a SamplingTool.
     * @param {L.Map} map - The Leaflet map instance.
     */
    constructor(map) {
        this.map = map;
        this.isActive = false;
        this.featureGroup = L.featureGroup().addTo(map);
        this.samples = []; // { id, lat, lng, layers: { name: value } }
        this.unqueryableLayers = new Set(); // cache of layers that return ServiceException for GetFeatureInfo
        
        this.onClick = this.onClick.bind(this);
        this.onDrawCreated = this.onDrawCreated.bind(this);
        
        document.addEventListener(EVENTS.BODY_CHANGED, () => this.clear());
    }

    /**
     * Activate the sampling tool in the given mode.
     * @param {'point'|'area'} [mode='point'] - Sampling mode.
     */
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
            this.map.on(L.Draw.Event.CREATED, this.onDrawCreated);
        }
    }

    /**
     * Deactivate the sampling tool and remove listeners.
     */
    deactivate() {
        if (!this.isActive) return;
        this.isActive = false;
        this.activeMode = null;

        this.map.off('click', this.onClick);
        if (this.drawControl) {
            this.drawControl.disable();
            this.drawControl = null;
        }
        this.map.off(L.Draw.Event.CREATED, this.onDrawCreated);
        this.map.getContainer().style.cursor = '';

        document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'sampling' } }));
    }

    onDrawCreated(e) {
        if (!this.isActive || this.activeMode !== 'area') return;

        const layer = e.layer;
        this.featureGroup.addLayer(layer);
        this.calculatePolygonStats(layer);
        
        this.deactivate();
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

    /**
     * Dispatch a SAMPLE_UPDATED event with current samples.
     */
    notifyUpdate() {
        document.dispatchEvent(new CustomEvent(EVENTS.SAMPLE_UPDATED, { detail: { samples: this.samples } }));
    }

    /**
     * Export all samples as a CSV file download.
     */
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

    // --- Spatial Sampling Statistics & Grid Generation ---

    /**
     * Compute comprehensive statistical aggregation over an array of sample values.
     * @param {Array<number>} values - Array of numeric values
     * @returns {{count: number, min: number, max: number, mean: number, variance: number, stdDev: number, median: number, standardError: number}}
     */
    static computeSampleStatistics(values = []) {
        const nums = values.filter(v => typeof v === 'number' && Number.isFinite(v));
        if (nums.length === 0) {
            return { count: 0, min: 0, max: 0, mean: 0, variance: 0, stdDev: 0, median: 0, standardError: 0 };
        }

        const count = nums.length;
        const sorted = [...nums].sort((a, b) => a - b);
        const min = sorted[0];
        const max = sorted[count - 1];
        const sum = nums.reduce((a, b) => a + b, 0);
        const mean = sum / count;

        const median = count % 2 === 0
            ? (sorted[count / 2 - 1] + sorted[count / 2]) / 2
            : sorted[Math.floor(count / 2)];

        const sumSqDiff = nums.reduce((a, b) => a + (b - mean) * (b - mean), 0);
        const variance = count > 1 ? sumSqDiff / (count - 1) : 0;
        const stdDev = Math.sqrt(variance);
        const standardError = count > 0 ? stdDev / Math.sqrt(count) : 0;

        return {
            count,
            min: parseFloat(min.toFixed(3)),
            max: parseFloat(max.toFixed(3)),
            mean: parseFloat(mean.toFixed(3)),
            variance: parseFloat(variance.toFixed(4)),
            stdDev: parseFloat(stdDev.toFixed(3)),
            median: parseFloat(median.toFixed(3)),
            standardError: parseFloat(standardError.toFixed(4))
        };
    }

    /**
     * Calculate Pearson linear correlation coefficient between two paired sample sets.
     * @param {Array<number>} xArray
     * @param {Array<number>} yArray
     * @returns {number} Pearson r (-1.0 to +1.0)
     */
    static computeCorrelationCoefficient(xArray = [], yArray = []) {
        const n = Math.min(xArray.length, yArray.length);
        if (n < 2) return 0;

        const meanX = xArray.slice(0, n).reduce((a, b) => a + b, 0) / n;
        const meanY = yArray.slice(0, n).reduce((a, b) => a + b, 0) / n;

        let num = 0;
        let denX = 0;
        let denY = 0;

        for (let i = 0; i < n; i++) {
            const dx = xArray[i] - meanX;
            const dy = yArray[i] - meanY;
            num += dx * dy;
            denX += dx * dx;
            denY += dy * dy;
        }

        const den = Math.sqrt(denX * denY);
        if (den === 0) return 0;

        return parseFloat((num / den).toFixed(4));
    }

    /**
     * Generate a regular grid of sample coordinates within a bounding box.
     * @param {{south: number, west: number, north: number, east: number}} bbox
     * @param {number} [stepKm=50] - Grid spacing in km
     * @param {string} [body='mars'] - Target planetary body
     * @returns {Array<[number, number]>} Array of [lat, lon] points
     */
    static generateRegularGridPoints(bbox, stepKm = 50, body = 'mars') {
        const R = (body.toLowerCase() === 'moon') ? 1737.4 : 3389.5;
        const kmPerDegLat = (Math.PI * R) / 180;
        const dLat = Math.max(0.1, stepKm / kmPerDegLat);

        const south = Math.max(-90, bbox.south);
        const north = Math.min(90, bbox.north);
        const west = bbox.west;
        const east = bbox.east;

        const points = [];

        for (let lat = south; lat <= north; lat += dLat) {
            const cosLat = Math.max(0.01, Math.cos(lat * Math.PI / 180));
            const kmPerDegLon = kmPerDegLat * cosLat;
            const dLon = Math.max(0.1, stepKm / kmPerDegLon);

            let lonSpan = east >= west ? (east - west) : (360 - west + east);
            let lonSteps = Math.ceil(lonSpan / dLon);

            for (let step = 0; step <= lonSteps; step++) {
                let lon = west + step * dLon;
                while (lon > 180) lon -= 360;
                while (lon < -180) lon += 360;
                points.push([parseFloat(lat.toFixed(4)), parseFloat(lon.toFixed(4))]);
            }
        }

        return points;
    }

    // --- Topographic Roughness & Position Indices (TRI / TPI) ---

    /**
     * Calculate Riley's Terrain Ruggedness Index (TRI) from a 3x3 neighborhood of elevations.
     * TRI = sqrt( sum( (z_i - z_0)^2 ) / 8 )
     * @param {Array<number>} elevationGrid3x3 - Array of 9 elevations ordered [top-left...bottom-right] or [center, 8 neighbors]
     * @returns {{triMeters: number, roughnessClass: string}}
     */
    static computeTerrainRuggednessIndex(elevationGrid3x3) {
        if (!Array.isArray(elevationGrid3x3) || elevationGrid3x3.length < 9) {
            return { triMeters: 0, roughnessClass: 'Level' };
        }

        // Center cell is index 4 in 3x3 row-major grid
        const z0 = elevationGrid3x3[4];
        let sumSqDiff = 0;
        let count = 0;

        for (let i = 0; i < 9; i++) {
            if (i === 4) continue;
            const diff = elevationGrid3x3[i] - z0;
            sumSqDiff += diff * diff;
            count++;
        }

        const tri = Math.sqrt(sumSqDiff / count);

        let rClass = 'Level';
        if (tri > 499) {
            rClass = 'Extremely Rugged';
        } else if (tri > 239) {
            rClass = 'Moderately Rugged';
        } else if (tri > 160) {
            rClass = 'Slightly Rugged';
        } else if (tri > 80) {
            rClass = 'Nearly Level';
        }

        return {
            triMeters: parseFloat(tri.toFixed(2)),
            roughnessClass: rClass
        };
    }

    /**
     * Calculate Topographic Position Index (TPI) comparing a center cell elevation to the neighborhood mean.
     * TPI = z_0 - mean(z_neighbors)
     * @param {number} centerElevationMeters - Center elevation z_0 in meters
     * @param {Array<number>} neighborElevationsMeters - Array of surrounding neighbor elevations
     * @returns {{tpiMeters: number, landscapePosition: string}}
     */
    static computeTopographicPositionIndex(centerElevationMeters, neighborElevationsMeters) {
        if (!Array.isArray(neighborElevationsMeters) || neighborElevationsMeters.length === 0) {
            return { tpiMeters: 0, landscapePosition: 'Flat' };
        }

        const meanN = neighborElevationsMeters.reduce((a, b) => a + b, 0) / neighborElevationsMeters.length;
        const tpi = centerElevationMeters - meanN;

        let pos = 'Flat / Mid-slope';
        if (tpi > 100) {
            pos = 'Major Ridge / Peak';
        } else if (tpi > 25) {
            pos = 'Upper Slope / Mound';
        } else if (tpi < -100) {
            pos = 'Valley / Crater Floor';
        } else if (tpi < -25) {
            pos = 'Lower Slope / Toe';
        }

        return {
            tpiMeters: parseFloat(tpi.toFixed(2)),
            landscapePosition: pos
        };
    }
}

