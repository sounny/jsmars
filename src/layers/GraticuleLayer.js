export class GraticuleLayer extends L.LayerGroup {
    constructor(options) {
        super();
        this.options = Object.assign({
            interval: 10,
            color: 'rgba(255, 255, 255, 0.5)',
            weight: 1,
            dashArray: '4, 4'
        }, options);

        this.draw();
    }

    draw() {
        const { interval, color, weight, dashArray } = this.options;

        // Clear existing
        this.clearLayers();

        // Longitude lines
        for (let lng = -180; lng <= 180; lng += interval) {
            const line = L.polyline([[-90, lng], [90, lng]], {
                color, weight, dashArray, interactive: false
            });
            this.addLayer(line);

            // Label (at equator?)
            if (lng % (interval * 2) === 0) { // sparse labels
               // Add label logic later if needed
            }
        }

        // Latitude lines
        for (let lat = -90; lat <= 90; lat += interval) {
            const line = L.polyline([[lat, -180], [lat, 180]], {
                color, weight, dashArray, interactive: false
            });
            this.addLayer(line);
        }
    }
}
