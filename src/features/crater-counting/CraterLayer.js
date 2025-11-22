export class CraterLayer {
    constructor(map) {
        this.map = map;
        this.craters = []; // Array of { id, lat, lng, diameter }
        this.isActive = false;
        this.ghostCircle = null;
        this.currentRadius = 50000; // Meters
        this.layerGroup = L.layerGroup().addTo(map);

        // Bind methods
        this.onMouseMove = this.onMouseMove.bind(this);
        this.onWheel = this.onWheel.bind(this);
        this.onClick = this.onClick.bind(this);
    }

    activate() {
        if (this.isActive) return;
        this.isActive = true;

        // Create ghost circle
        this.ghostCircle = L.circle(this.map.getCenter(), {
            color: '#ff0',
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
        this.craters.push(crater);

        // Draw permanent circle
        const circle = L.circle([crater.lat, crater.lng], {
            color: '#f00',
            weight: 2,
            fillOpacity: 0.2,
            radius: crater.diameter / 2
        }).addTo(this.layerGroup);

        // Store reference for removal if needed
        crater.layer = circle;

        // Dispatch event for table update
        const event = new CustomEvent('jmars-crater-added', { detail: crater });
        document.dispatchEvent(event);
    }
}
