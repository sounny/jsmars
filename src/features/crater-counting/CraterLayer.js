import { EVENTS } from '../../constants.js';

export class CraterLayer {
    constructor(map) {
        this.map = map;
        this.craters = []; // Array of { id, lat, lng, diameter, layer }
        this.isActive = false;
        this.ghostCircle = null;
        this.currentRadius = 50000; // Meters
        this.layerGroup = L.layerGroup().addTo(map);

        // Bind methods
        this.onMouseMove = this.onMouseMove.bind(this);
        this.onWheel = this.onWheel.bind(this);
        this.onClick = this.onClick.bind(this);
        this.handleRemoveRequest = this.handleRemoveRequest.bind(this);
        this.handleClearRequest = this.handleClearRequest.bind(this);

        // Listen for external requests
        document.addEventListener(EVENTS.CRATER_REMOVE, this.handleRemoveRequest);
        document.addEventListener(EVENTS.CRATER_CLEAR, this.handleClearRequest);

        // Body change listener
        document.addEventListener(EVENTS.BODY_CHANGED, () => {
            this.handleClearRequest();
            if (this.isActive) this.deactivate();
            // Also update button state in UI? 
            // The UI button logic is in index.html. 
            // We can dispatch a deactivate event that index.html listens to.
            // But index.html listens to 'jmars-tool-deactivated'?
            // Let's dispatch it.
            document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'crater' } }));
        });
    }

    activate() {
        if (this.isActive) return;
        this.isActive = true;

        // Create ghost circle
        this.ghostCircle = L.circle(this.map.getCenter(), {
            color: '#ffffff',
            fillColor: '#ffff00',
            weight: 2,
            fillOpacity: 0.1,
            radius: this.currentRadius,
            interactive: false
        }).addTo(this.map);

        // Add listeners
        this.map.on('mousemove', this.onMouseMove);
        this.map.getContainer().addEventListener('wheel', this.onWheel);
        this.map.on('click', this.onClick);

        this.map.getContainer().style.cursor = 'none'; // Hide default cursor
    }

    deactivate() {
        if (!this.isActive) return;
        this.isActive = false;

        // Remove ghost circle
        if (this.ghostCircle) {
            this.ghostCircle.remove();
            this.ghostCircle = null;
        }

        // Remove listeners
        this.map.off('mousemove', this.onMouseMove);
        this.map.getContainer().removeEventListener('wheel', this.onWheel);
        this.map.off('click', this.onClick);

        this.map.getContainer().style.cursor = ''; // Restore cursor
    }

    onMouseMove(e) {
        if (this.ghostCircle) {
            this.ghostCircle.setLatLng(e.latlng);
        }
    }

    onWheel(e) {
        if (!this.isActive) return;
        e.preventDefault(); // Prevent map zoom

        const delta = e.deltaY > 0 ? 0.9 : 1.1;
        this.currentRadius *= delta;

        // Clamp radius (optional)
        if (this.currentRadius < 1000) this.currentRadius = 1000;
        if (this.currentRadius > 1000000) this.currentRadius = 1000000;

        if (this.ghostCircle) {
            this.ghostCircle.setRadius(this.currentRadius);
        }
    }

    onClick(e) {
        if (!this.isActive) return;

        const crater = {
            id: Date.now(),
            lat: e.latlng.lat,
            lng: e.latlng.lng,
            diameter: this.currentRadius * 2
        };

        this.addCrater(crater);
    }

    addCrater(crater) {
        // Draw permanent circle
        const circle = L.circle([crater.lat, crater.lng], {
            color: '#ffffff',
            fillColor: '#ff0000',
            weight: 2,
            fillOpacity: 0.2,
            radius: crater.diameter / 2
        }).addTo(this.layerGroup);

        // Store reference
        crater.layer = circle;
        this.craters.push(crater);

        // Dispatch event for table update
        const event = new CustomEvent(EVENTS.CRATER_ADDED, {
            detail: {
                id: crater.id,
                lat: crater.lat,
                lng: crater.lng,
                diameter: crater.diameter
            }
        });
        document.dispatchEvent(event);
    }

    handleRemoveRequest(e) {
        const id = e.detail.id;
        const index = this.craters.findIndex(c => c.id === id);
        if (index !== -1) {
            const crater = this.craters[index];
            if (crater.layer) {
                this.layerGroup.removeLayer(crater.layer);
            }
            this.craters.splice(index, 1);
        }
    }

    handleClearRequest(e) {
        this.craters.forEach(c => {
            if (c.layer) this.layerGroup.removeLayer(c.layer);
        });
        this.craters = [];
    }

    getData() {
        // Return serializable data (exclude Leaflet layer objects)
        return this.craters.map(c => ({
            id: c.id,
            lat: c.lat,
            lng: c.lng,
            diameter: c.diameter
        }));
    }

    loadData(data) {
        this.handleClearRequest(); // Clear existing
        if (!Array.isArray(data)) return;

        data.forEach(c => {
            // Ensure we don't duplicate IDs if they collide with new Date.now(), 
            // but for session loading we usually trust the source.
            this.addCrater(c);
        });
    }
}
