import { EVENTS } from '../../constants.js';
import { getPlanetaryRadius, haversineDistance } from '../../util/geo.js';
import { jmarsState } from '../../jmars-state.js';

const EARTH_RADIUS_METERS = 6371000;

/**
 * @module CraterLayer
 * @description Interactive crater counting overlay for the Leaflet map.
 *
 * When active, the user moves a "ghost circle" cursor and clicks to
 * stamp craters. Scroll-wheel adjusts the crater radius.
 * The layer group is only added to the map while the tool is active
 * and removed on deactivation to avoid stale overlays.
 */
export class CraterLayer {
    /**
     * Create a CraterLayer.
     * @param {L.Map} map - The Leaflet map instance.
     */
    constructor(map) {
        this.map = map;
        this.craters = []; // Array of { id, lat, lng, diameter, layer }
        this.isActive = false;
        this.ghostCircle = null;
        this.currentRadius = 50000; // True physical radius in meters
        this.currentBody = (jmarsState?.get?.('body') || 'mars').toLowerCase();
        this._hasCustomRadius = false;
        this.layerGroup = L.layerGroup(); // Added lazily & kept visible across tool switches

        // Bind methods
        this.onMouseMove = this.onMouseMove.bind(this);
        this.onWheel = this.onWheel.bind(this);
        this.onClick = this.onClick.bind(this);
        this.onKeyDown = this.onKeyDown.bind(this);

        document.addEventListener(EVENTS.BODY_CHANGED, (e) => {
            const nextBody = (e?.detail?.body || jmarsState?.get?.('body') || 'mars').toLowerCase();
            if (nextBody !== this.currentBody) {
                this.currentBody = nextBody;
                this._hasCustomRadius = false;
                if (this.ghostCircle) {
                    this.ghostCircle.setRadius(this.physicalToLeafletRadius(this.currentRadius));
                }
            }
        });
        this.handleRemoveRequest = this.handleRemoveRequest.bind(this);
        this.handleClearRequest = this.handleClearRequest.bind(this);

        // Listen for external requests
        document.addEventListener(EVENTS.CRATER_REMOVE, this.handleRemoveRequest);
        document.addEventListener(EVENTS.CRATER_CLEAR, this.handleClearRequest);

        // Body change listener
        document.addEventListener(EVENTS.BODY_CHANGED, () => {
            this.handleClearRequest();
            document.dispatchEvent(new CustomEvent(EVENTS.CRATER_CLEAR));
            if (this.isActive) this.deactivate();
            document.dispatchEvent(new CustomEvent(EVENTS.TOOL_DEACTIVATED, { detail: { tool: 'crater' } }));
        });
    }

    /**
     * Activate crater counting mode.
     * Adds the layer group to the map and registers interaction listeners.
     */
    /**
     * Convert true physical planetary radius in meters to Leaflet L.circle Earth-equivalent meters.
     * @param {number} physicalMeters
     * @returns {number}
     */
    physicalToLeafletRadius(physicalMeters) {
        const bodyMeters = getPlanetaryRadius(this.currentBody, 'meters') || EARTH_RADIUS_METERS;
        return physicalMeters * (EARTH_RADIUS_METERS / bodyMeters);
    }

    /**
     * Clamp and update the active crater stamping radius in true planetary meters.
     * @param {number} nextRadiusMeters
     * @param {{custom?: boolean}} [options]
     */
    setRadius(nextRadiusMeters, { custom = true } = {}) {
        const clamped = Math.max(25, Math.min(1000000, Number(nextRadiusMeters) || 50000));
        this.currentRadius = clamped;
        if (custom) {
            this._hasCustomRadius = true;
        }
        if (this.ghostCircle) {
            this.ghostCircle.setRadius(this.physicalToLeafletRadius(this.currentRadius));
        }
    }

    /**
     * Activate crater counting mode.
     * Adds the crater layer group, ghost preview circle, and interaction listeners.
     */
    activate() {
        if (this.isActive) return;
        this.isActive = true;
        this.currentBody = (jmarsState?.get?.('body') || this.currentBody || 'mars').toLowerCase();

        // Add layer group to map on activation (stays visible after deactivation)
        if (!this.map.hasLayer(this.layerGroup)) {
            this.layerGroup.addTo(this.map);
        }

        // Choose a viewport-appropriate default radius if user has not resized manually
        if (!this._hasCustomRadius && this.map && typeof this.map.getBounds === 'function') {
            try {
                const bounds = this.map.getBounds();
                const center = this.map.getCenter();
                const spanKm = haversineDistance(center.lat, bounds.getWest(), center.lat, bounds.getEast(), this.currentBody);
                if (Number.isFinite(spanKm) && spanKm > 0) {
                    const suggested = Math.round(spanKm * 1000 * 0.05);
                    this.currentRadius = Math.max(25, Math.min(250000, suggested));
                }
            } catch {
                // Keep default radius when bounds are unavailable
            }
        }

        // Create ghost circle scaled for active planetary radius
        this.ghostCircle = L.circle(this.map.getCenter(), {
            color: '#ffffff',
            fillColor: '#ffff00',
            weight: 2,
            fillOpacity: 0.1,
            radius: this.physicalToLeafletRadius(this.currentRadius),
            interactive: false
        }).addTo(this.map);

        // Add listeners (passive: false on wheel so preventDefault works)
        this.map.on('mousemove', this.onMouseMove);
        this.map.getContainer().addEventListener('wheel', this.onWheel, { passive: false });
        this.map.on('click', this.onClick);
        document.addEventListener('keydown', this.onKeyDown);

        this.map.getContainer().style.cursor = 'none'; // Hide default cursor
    }

    /**
     * Deactivate crater counting mode.
     * Removes temporary stamping listeners while keeping counted craters visible on the map.
     */
    deactivate() {
        if (!this.isActive) return;
        this.isActive = false;

        // Remove ghost circle
        if (this.ghostCircle) {
            this.ghostCircle.remove();
            this.ghostCircle = null;
        }

        // Remove interaction listeners
        this.map.off('mousemove', this.onMouseMove);
        this.map.getContainer().removeEventListener('wheel', this.onWheel);
        this.map.off('click', this.onClick);
        document.removeEventListener('keydown', this.onKeyDown);

        this.map.getContainer().style.cursor = ''; // Restore cursor
    }

    /**
     * Track the mouse to update the ghost circle position.
     * @param {L.LeafletMouseEvent} e - Leaflet mouse event.
     */
    onMouseMove(e) {
        if (this.ghostCircle) {
            this.ghostCircle.setLatLng(e.latlng);
        }
    }

    /**
     * Adjust the ghost circle radius via scroll wheel.
     * @param {WheelEvent} e - Native wheel event.
     */
    onWheel(e) {
        if (!this.isActive) return;
        e.preventDefault(); // Prevent map zoom

        const delta = e.deltaY > 0 ? 0.9 : 1.1;
        this.setRadius(this.currentRadius * delta, { custom: true });
    }

    /**
     * Adjust crater stamp radius via keyboard (+/- or ArrowUp/ArrowDown) for trackpad users.
     * @param {KeyboardEvent} e
     */
    onKeyDown(e) {
        if (!this.isActive) return;
        const tag = (e.target?.tagName || '').toLowerCase();
        if (tag === 'input' || tag === 'textarea' || tag === 'select') return;
        if (e.key === '+' || e.key === '=' || e.key === 'ArrowUp') {
            e.preventDefault();
            this.setRadius(this.currentRadius * 1.15, { custom: true });
        } else if (e.key === '-' || e.key === '_' || e.key === 'ArrowDown') {
            e.preventDefault();
            this.setRadius(this.currentRadius * 0.85, { custom: true });
        }
    }

    /**
     * Place a crater at the clicked location.
     * @param {L.LeafletMouseEvent} e - Leaflet click event.
     */
    onClick(e) {
        if (!this.isActive) return;

        const crater = {
            id: crypto.randomUUID?.() || `${Date.now()}-${Math.random().toString(36).substring(2, 9)}`,
            lat: e.latlng.lat,
            lng: e.latlng.lng,
            diameter: this.currentRadius * 2
        };

        this.addCrater(crater);
    }

    /**
     * Add a crater circle to the layer group and dispatch CRATER_ADDED.
     * @param {object} crater - Crater record with id, lat, lng, diameter.
     */
    addCrater(crater) {
        if (this.map && !this.map.hasLayer(this.layerGroup)) {
            this.layerGroup.addTo(this.map);
        }
        // Draw permanent circle scaled to active planetary body angular diameter
        const circle = L.circle([crater.lat, crater.lng], {
            color: '#ffffff',
            fillColor: '#ff0000',
            weight: 2,
            fillOpacity: 0.2,
            radius: this.physicalToLeafletRadius(crater.diameter / 2)
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

    /**
     * Remove a specific crater by ID (triggered by external event).
     * @param {CustomEvent} e - Event with detail.id.
     */
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

    /**
     * Remove all craters from the layer group (triggered by external event or body change).
     */
    handleClearRequest() {
        this.craters.forEach(c => {
            if (c.layer) this.layerGroup.removeLayer(c.layer);
        });
        this.craters = [];
    }

    /**
     * Get serializable crater data (excludes Leaflet layer objects).
     * @returns {Array<object>} Array of { id, lat, lng, diameter }.
     */
    getData() {
        return this.craters.map(c => ({
            id: c.id,
            lat: c.lat,
            lng: c.lng,
            diameter: c.diameter
        }));
    }

    /**
     * Load craters from serialized data (e.g., session restore).
     * @param {Array<object>} data - Array of crater records.
     */
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
