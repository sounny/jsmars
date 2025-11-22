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

        // Bindings
        this.onClick = this.onClick.bind(this);
        this.onMouseMove = this.onMouseMove.bind(this);
    }

    activate() {
        if (this.isActive) return;
        this.isActive = true;
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
    }

    reset() {
        this.center = null;
        this.layerGroup.clearLayers();
        if (this.previewLine) {
            this.previewLine.remove();
            this.previewLine = null;
        }
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

    generateProfile(center, radius) {
        this.layerGroup.clearLayers();
        L.circleMarker(center, { radius: 5, color: '#0ff' }).addTo(this.layerGroup);

        const profiles = [];

        for (let i = 0; i < this.numLines; i++) {
            const angleDeg = (360 / this.numLines) * i;
            const angleRad = (angleDeg * Math.PI) / 180;

            // Calculate end point using simple approximation or proper geodesy
            // Leaflet has tools, but we can just use geometry for short distances or find a point.
            // L.GeometryUtil (plugin) or just simulate.
            // Given we are on a sphere...
            // Simple way: Project to pixels, move, unproject?
            // Better: use Destination point formula.
            // lat2 = asin(sin(lat1)*cos(d/R) + cos(lat1)*sin(d/R)*cos(brng))
            // lon2 = lon1 + atan2(sin(brng)*sin(d/R)*cos(lat1), cos(d/R)-sin(lat1)*sin(lat2))

            const R = 3396190; // Mars radius in meters
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

            // Generate Data
            const dataPoints = [];
            const steps = 50; // Number of samples per line
            for (let s = 0; s <= steps; s++) {
                const dist = (radius / steps) * s;
                // Interpolate pos
                const ratio = s / steps;
                const sampleLat = center.lat + (endLat - center.lat) * ratio;
                const sampleLng = center.lng + (endLng - center.lng) * ratio;

                // Mock Elevation
                // Noise function based on coords
                const noise = Math.sin(sampleLat * 10) * Math.cos(sampleLng * 10) * 500;
                const base = Math.sin(sampleLat * 0.5) * 3000; // Global trend
                const crater = Math.random() > 0.9 ? -1000 : 0; // Random craters

                const elev = base + noise + crater;
                dataPoints.push({ dist, elev });
            }

            profiles.push({
                angle: angleDeg,
                color: this.getColor(i),
                data: dataPoints
            });
        }

        // Dispatch Data
        const event = new CustomEvent('jmars-profile-generated', { detail: { profiles } });
        document.dispatchEvent(event);
    }

    getColor(index) {
        const colors = ['#e6194b', '#3cb44b', '#ffe119', '#4363d8', '#f58231', '#911eb4', '#46f0f0', '#f032e6'];
        return colors[index % colors.length];
    }
}
