import { EVENTS } from '../../constants.js';
import { jmarsState } from '../../jmars-state.js';

/**
 * @module MeasureTool
 * @description Provides distance and area measurement on the Leaflet map.
 *
 * Uses Leaflet.Draw to let the user draw polylines (distance) or
 * polygons (area), then computes body-scaled metric values.
 *
 * Listeners for L.Draw.Event.CREATED and DRAWSTOP are registered
 * only while the tool is active, so they do not intercept draw
 * events intended for ShapeLayer or other tools.
 */
export class MeasureTool {
    /**
     * Create a MeasureTool instance.
     * @param {L.Map} map - The Leaflet map instance.
     */
    constructor(map) {
        this.map = map;
        this.layerGroup = L.layerGroup().addTo(map);
        this.drawControl = null;
        this.activeMode = null; // 'distance' | 'area' | null
        this.isDrawing = false;

        this.measurements = [];
        this.lineCount = 0;
        this.areaCount = 0;

        /**
         * Body radius ratios used to convert Leaflet's Earth-based
         * distance calculations to the active planetary body.
         * Key = lowercase body name, value = bodyRadius / earthRadius.
         */
        this.scaleFactors = {
            mars:  3389.5 / 6371,  // ~0.5319
            moon:  1737.4 / 6371,  // ~0.2727
            earth: 1.0
        };

        this.onDrawCreated = this.onDrawCreated.bind(this);
        this.onDrawStop = this.onDrawStop.bind(this);
        this.onLayerClick = this.onLayerClick.bind(this);

        // Body change listener
        document.addEventListener(EVENTS.BODY_CHANGED, () => {
            this.clear();
            this.deactivate();
            document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'measure' } }));
        });
    }

    /**
     * Return the scale factor for the currently active body.
     * Falls back to Mars if the body is unknown.
     * @returns {number} Ratio of body radius to Earth radius.
     */
    get scaleFactor() {
        const body = (jmarsState.get('body') || 'mars').toLowerCase();
        return this.scaleFactors[body] ?? this.scaleFactors.mars;
    }

    /**
     * Activate the measure tool in the given mode.
     * Registers L.Draw event listeners so they are only active
     * while this tool is in use.
     * @param {'distance'|'area'} mode - Measurement mode.
     */
    activate(mode) {
        if (this.isDrawing && this.activeMode === mode) {
            return; // Already active
        }

        this.deactivate(); // Clear previous mode if any

        this.activeMode = mode;
        this.isDrawing = true;

        if (mode === 'distance') {
            this.drawControl = new L.Draw.Polyline(this.map, {
                shapeOptions: {
                    color: '#00ff00',
                    weight: 3
                },
                metric: true
            });
        } else if (mode === 'area') {
            this.drawControl = new L.Draw.Polygon(this.map, {
                shapeOptions: {
                    color: '#00ff00',
                    weight: 3,
                    fillOpacity: 0.2
                },
                allowIntersection: false,
                showArea: true
            });
        }

        // Register draw listeners only while active
        this.map.on(L.Draw.Event.CREATED, this.onDrawCreated);
        this.map.on(L.Draw.Event.DRAWSTOP, this.onDrawStop);

        if (this.drawControl) {
            this.drawControl.enable();
        }
    }

    /**
     * Deactivate the measure tool and unregister draw listeners.
     */
    deactivate() {
        if (this.drawControl) {
            this.drawControl.disable();
            this.drawControl = null;
        }

        // Unregister draw listeners so other tools are not affected
        this.map.off(L.Draw.Event.CREATED, this.onDrawCreated);
        this.map.off(L.Draw.Event.DRAWSTOP, this.onDrawStop);

        this.activeMode = null;
        this.isDrawing = false;
    }

    /**
     * Clear all measurements from the map and reset counters.
     */
    clear() {
        this.layerGroup.clearLayers();
        this.measurements = [];
        this.lineCount = 0;
        this.areaCount = 0;
        this.notifyUpdate();
    }

    /**
     * Handle a completed draw event from Leaflet.Draw.
     * @param {object} e - Leaflet draw:created event.
     */
    onDrawCreated(e) {
        const type = e.layerType;
        const layer = e.layer;

        if ((type === 'polyline' && this.activeMode === 'distance') ||
            (type === 'polygon' && this.activeMode === 'area')) {

            this.layerGroup.addLayer(layer);

            // Generate unique ID using substring (not deprecated substr)
            const id = Date.now() + Math.random().toString(36).substring(2, 11);
            let name, value, valueStr, vertices;

            if (type === 'polyline') {
                this.lineCount++;
                name = `Line ${this.lineCount}`;
                value = this.calculateDistance(layer);
                valueStr = this.formatDistance(value);
                vertices = layer.getLatLngs().length;
            } else {
                this.areaCount++;
                name = `Area ${this.areaCount}`;
                value = this.calculateArea(layer);
                valueStr = this.formatArea(value);
                // Polygon latlngs are nested [[p1, p2, p3]]
                vertices = layer.getLatLngs()[0].length;
            }

            const measurement = {
                id,
                type: type === 'polyline' ? 'Line' : 'Area',
                name,
                value,
                valueStr,
                vertices,
                layer
            };

            this.measurements.push(measurement);

            // Bind Popup
            this.updatePopup(measurement);

            // Add click listener for highlighting
            layer.on('click', () => {
                this.highlight(id);
                // Also open popup
                layer.openPopup();
            });

            this.notifyUpdate();
        }

        this.deactivate();
        document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'measure' } }));
    }

    /**
     * Handle the draw-stop event (user cancelled or finished).
     */
    onDrawStop() {
        if (this.isDrawing) {
            this.isDrawing = false;
            this.activeMode = null;
            this.drawControl = null;
            document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'measure' } }));
        }
    }

    /**
     * Placeholder for layer click handling (actual logic is in onDrawCreated).
     * @param {object} _e - Leaflet click event (unused).
     */
    onLayerClick(_e) {
        // Handled in onDrawCreated via layer event
    }

    /**
     * Bind or update the popup content for a measurement.
     * @param {object} m - Measurement record.
     */
    updatePopup(m) {
        const content = `
            <div style="text-align:center">
                <b>${m.name}</b><br>
                ${m.type === 'Line' ? 'Distance' : 'Area'}: ${m.valueStr}<br>
                Vertices: ${m.vertices}
            </div>
        `;
        m.layer.bindPopup(content);
    }

    /**
     * Rename a measurement and refresh its popup.
     * @param {string} id - Measurement ID.
     * @param {string} newName - New display name.
     */
    updateName(id, newName) {
        const m = this.measurements.find(x => x.id === id);
        if (m) {
            m.name = newName;
            this.updatePopup(m);
            // If popup is open, update it
            if (m.layer.isPopupOpen()) {
                m.layer.setPopupContent(m.layer.getPopup().getContent());
            }
        }
    }

    /**
     * Visually highlight a measurement on the map and notify the table.
     * @param {string} id - Measurement ID.
     */
    highlight(id) {
        // Reset all styles
        this.measurements.forEach(m => {
            if (m.type === 'Line') {
                m.layer.setStyle({ color: '#00ff00', weight: 3 });
            } else {
                m.layer.setStyle({ color: '#00ff00', weight: 3, fillOpacity: 0.2 });
            }
        });

        // Highlight target
        const m = this.measurements.find(x => x.id === id);
        if (m) {
            m.layer.setStyle({ color: '#ffff00', weight: 5, fillOpacity: 0.4 });

            // Dispatch event to highlight table row
            document.dispatchEvent(new CustomEvent(EVENTS.MEASURE_HIGHLIGHT, { detail: { id } }));
        }
    }

    /**
     * Calculate the total distance of a polyline, scaled to the active body.
     * @param {L.Polyline} layer - The polyline layer.
     * @returns {number} Distance in meters on the active body.
     */
    calculateDistance(layer) {
        let totalDistance = 0;
        const latlngs = layer.getLatLngs();
        for (let i = 0; i < latlngs.length - 1; i++) {
            totalDistance += latlngs[i].distanceTo(latlngs[i + 1]);
        }
        return totalDistance * this.scaleFactor;
    }

    /**
     * Calculate the area of a polygon, scaled to the active body.
     * @param {L.Polygon} layer - The polygon layer.
     * @returns {number} Area in square meters on the active body.
     */
    calculateArea(layer) {
        const latlngs = layer.getLatLngs()[0];
        const area = L.GeometryUtil.geodesicArea(latlngs);
        return area * (this.scaleFactor * this.scaleFactor);
    }

    /**
     * Format a distance value for display.
     * @param {number} meters - Distance in meters.
     * @returns {string} Formatted string with units.
     */
    formatDistance(meters) {
        if (meters > 1000) return `${(meters / 1000).toFixed(2)} km`;
        return `${meters.toFixed(0)} m`;
    }

    /**
     * Format an area value for display.
     * @param {number} sqMeters - Area in square meters.
     * @returns {string} Formatted string with units.
     */
    formatArea(sqMeters) {
        if (sqMeters > 1000000) return `${(sqMeters / 1000000).toFixed(2)} km²`;
        return `${sqMeters.toFixed(0)} m²`;
    }

    /**
     * Dispatch a MEASURE_UPDATED event with current measurements.
     */
    notifyUpdate() {
        document.dispatchEvent(new CustomEvent(EVENTS.MEASURE_UPDATED, { detail: this.measurements }));
    }

    /**
     * Export all measurements as a GeoJSON file download.
     */
    exportGeoJSON() {
        if (this.measurements.length === 0) return;

        const features = this.measurements.map(m => {
            const geojson = m.layer.toGeoJSON();
            geojson.properties = {
                id: m.id,
                name: m.name,
                type: m.type,
                value: m.value,
                valueUnit: m.type === 'Line' ? 'meters' : 'square meters',
                valueFormatted: m.valueStr,
                vertices: m.vertices
            };
            return geojson;
        });

        const collection = {
            type: "FeatureCollection",
            features: features
        };

        this.downloadFile('measurements.geojson', JSON.stringify(collection, null, 2));
    }

    /**
     * Export all measurements as a CSV file download.
     */
    exportCSV() {
        if (this.measurements.length === 0) return;

        const header = ['Name', 'Type', 'Vertices', 'Value', 'Unit'];
        const rows = this.measurements.map(m => {
            const unit = m.type === 'Line' ? 'm' : 'm²';
            return [
                `"${m.name}"`,
                m.type,
                m.vertices,
                m.value.toFixed(2),
                unit
            ].join(',');
        });

        const csvContent = [header.join(','), ...rows].join('\n');
        this.downloadFile('measurements.csv', csvContent);
    }

    /**
     * Trigger a browser file download with the given content.
     * @param {string} filename - Download filename.
     * @param {string} content - File content.
     */
    downloadFile(filename, content) {
        const element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(content));
        element.setAttribute('download', filename);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    }

    /**
     * Get serializable measurement data (no Leaflet layer references).
     * @returns {Array<object>} Array of measurement records.
     */
    getData() {
        return this.measurements.map(m => ({
            id: m.id,
            type: m.type,
            name: m.name,
            value: m.value,
            valueStr: m.valueStr,
            vertices: m.vertices,
            latlngs: m.layer.getLatLngs()
        }));
    }

    /**
     * Load measurements from serialized data (e.g., session restore).
     * @param {Array<object>} data - Serialized measurement records.
     */
    loadData(data) {
        this.clear();
        if (!Array.isArray(data)) return;

        data.forEach(m => {
            let layer;
            // Reconstruct Layer
            if (m.type === 'Line') {
                layer = L.polyline(m.latlngs, { color: '#00ff00', weight: 3 });
            } else {
                // Polygon latlngs from getLatLngs() might be nested [[...]] for simple polygons in Leaflet
                // But L.polygon constructor handles it if we pass it back exactly as retrieved usually.
                // However, L.Draw.Polygon usually creates a simple polygon. 
                // Let's try passing it directly.
                layer = L.polygon(m.latlngs, { color: '#00ff00', weight: 3, fillOpacity: 0.2 });
            }

            this.layerGroup.addLayer(layer);

            const measurement = {
                ...m,
                layer: layer
            };

            this.measurements.push(measurement);

            // Re-bind Popup
            this.updatePopup(measurement);

            // Re-bind Events
            layer.on('click', () => {
                this.highlight(m.id);
                layer.openPopup();
            });
        });

        this.notifyUpdate();
    }
}
