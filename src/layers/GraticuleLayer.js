/**
 * @module layers/GraticuleLayer
 * @description Coordinate grid overlay that draws latitude and longitude lines
 * at a configurable interval. Renders as a Leaflet LayerGroup of polylines
 * styled with a shared color, weight, and dash pattern.
 */

/**
 * @class GraticuleLayer
 * @extends L.LayerGroup
 * @description A coordinate graticule (grid) overlay. Draws meridians and
 * parallels at a fixed degree interval with configurable line styling.
 */
export class GraticuleLayer extends L.LayerGroup {
    /**
     * Creates a new GraticuleLayer and immediately draws the grid lines.
     * @param {object} [options] - Optional styling overrides.
     * @param {number} [options.interval=10] - Degree spacing between grid lines.
     * @param {string} [options.color='rgba(255, 255, 255, 0.5)'] - CSS color for the lines.
     * @param {number} [options.weight=1] - Stroke width in pixels.
     * @param {string} [options.dashArray='4, 4'] - SVG dash-array pattern string.
     */
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

    /**
     * Clears existing grid lines and redraws longitude (meridian) and latitude
     * (parallel) polylines according to the current options.
     * @returns {void}
     */
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
