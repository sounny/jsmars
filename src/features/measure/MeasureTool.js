export class MeasureTool {
    constructor(map) {
        this.map = map;
        this.layerGroup = L.layerGroup().addTo(map);
        this.drawControl = null;
        this.activeMode = null; // 'distance' | 'area' | null
        this.isDrawing = false;

        this.measurements = [];
        this.lineCount = 0;
        this.areaCount = 0;

        // Mars Radius / Earth Radius
        // 3389.5 / 6371
        this.scaleFactor = 0.5319;

        this.onDrawCreated = this.onDrawCreated.bind(this);
        this.onDrawStop = this.onDrawStop.bind(this);
        this.onLayerClick = this.onLayerClick.bind(this);

        this.map.on(L.Draw.Event.CREATED, this.onDrawCreated);
        this.map.on(L.Draw.Event.DRAWSTOP, this.onDrawStop);
    }

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

        if (this.drawControl) {
            this.drawControl.enable();
        }
    }

    deactivate() {
        if (this.drawControl) {
            this.drawControl.disable();
            this.drawControl = null;
        }
        this.activeMode = null;
        this.isDrawing = false;
    }

    clear() {
        this.layerGroup.clearLayers();
        this.measurements = [];
        this.lineCount = 0;
        this.areaCount = 0;
        this.notifyUpdate();
    }

    onDrawCreated(e) {
        const type = e.layerType;
        const layer = e.layer;

        if ((type === 'polyline' && this.activeMode === 'distance') ||
            (type === 'polygon' && this.activeMode === 'area')) {

            this.layerGroup.addLayer(layer);

            // Generate Data
            const id = Date.now() + Math.random().toString(36).substr(2, 9);
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
        document.dispatchEvent(new CustomEvent('jmars-tool-deactivated', { detail: { tool: 'measure' } }));
    }

    onDrawStop() {
        if (this.isDrawing) {
            this.isDrawing = false;
            this.activeMode = null;
            this.drawControl = null;
            document.dispatchEvent(new CustomEvent('jmars-tool-deactivated', { detail: { tool: 'measure' } }));
        }
    }

    onLayerClick(e) {
        // Handled in onDrawCreated via layer event
    }

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
            document.dispatchEvent(new CustomEvent('jmars-measurement-highlight', { detail: { id } }));
        }
    }

    calculateDistance(layer) {
        let totalDistance = 0;
        const latlngs = layer.getLatLngs();
        for (let i = 0; i < latlngs.length - 1; i++) {
            totalDistance += latlngs[i].distanceTo(latlngs[i + 1]);
        }
        return totalDistance * this.scaleFactor;
    }

    calculateArea(layer) {
        const latlngs = layer.getLatLngs()[0];
        const area = L.GeometryUtil.geodesicArea(latlngs);
        return area * (this.scaleFactor * this.scaleFactor);
    }

    formatDistance(meters) {
        if (meters > 1000) return `${(meters / 1000).toFixed(2)} km`;
        return `${meters.toFixed(0)} m`;
    }

    formatArea(sqMeters) {
        if (sqMeters > 1000000) return `${(sqMeters / 1000000).toFixed(2)} km²`;
        return `${sqMeters.toFixed(0)} m²`;
    }

    notifyUpdate() {
        document.dispatchEvent(new CustomEvent('jmars-measurements-updated', { detail: this.measurements }));
    }

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

    downloadFile(filename, content) {
        const element = document.createElement('a');
        element.setAttribute('href', 'data:text/plain;charset=utf-8,' + encodeURIComponent(content));
        element.setAttribute('download', filename);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    }

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
