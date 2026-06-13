/**
 * @module NomenclatureLayer
 * @description Leaflet layer for displaying planetary nomenclature labels.
 * Loads feature names from a local JSON file and renders them as tooltips.
 */

/**
 * @class NomenclatureLayer
 * @description Renders named surface features (craters, mountains, valleys, etc.) as map labels.
 */
export class NomenclatureLayer {
    /**
     * @param {L.Map} map - The Leaflet map instance.
     */
    constructor(map) {
        this.map = map;
        this.layerGroup = L.layerGroup().addTo(map);
        this.landmarks = [];
        /** @type {Object<string, boolean>} Visibility state for each feature type. */
        this.visibleTypes = {
            'Crater': true,
            'Mons': true,
            'Valles': true,
            'Planitia': true,
            'Other': true
        };
        this.isActive = false;
    }

    /**
     * Load landmark data from the local JSON file.
     * @returns {Promise<void>}
     */
    async load() {
        try {
            const response = await fetch('./src/data/landmarks.json');
            if (!response.ok) {
                throw new Error(`Failed to fetch landmarks: ${response.statusText}`);
            }
            this.landmarks = await response.json();
            this.render();
        } catch (e) {
            console.error('Failed to load landmarks', e);
        }
    }

    toggleType(type, isVisible) {
        this.visibleTypes[type] = isVisible;
        this.render();
    }

    toggle(isActive) {
        this.isActive = isActive;
        if (isActive) {
            this.map.addLayer(this.layerGroup);
            if (this.landmarks.length === 0) this.load();
        } else {
            this.map.removeLayer(this.layerGroup);
        }
    }

    render() {
        this.layerGroup.clearLayers();
        
        this.landmarks.forEach(l => {
            const type = this.visibleTypes[l.type] !== undefined ? l.type : 'Other';
            
            if (this.visibleTypes[type]) {
                // Normalize Lon (-180 to 180)
                let lon = l.lon;
                if (lon > 180) lon -= 360;

                const icon = L.divIcon({
                    className: 'nomenclature-label',
                    html: `<span style="color: white; text-shadow: 1px 1px 2px black; white-space: nowrap;">${l.name}</span>`,
                    iconSize: [100, 20],
                    iconAnchor: [50, 10]
                });

                L.marker([l.lat, lon], { icon: icon, interactive: false }).addTo(this.layerGroup);
                
                // Optional: Add a small dot
                L.circleMarker([l.lat, lon], {
                    radius: 2,
                    color: 'white',
                    fillColor: 'white',
                    fillOpacity: 1,
                    interactive: false
                }).addTo(this.layerGroup);
            }
        });
    }
}
