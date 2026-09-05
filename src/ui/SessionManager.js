import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';

/**
 * How long to wait for a deferred `LAYERS_UPDATED` event (fired when
 * JMARSMap's async WMS discovery completes) before giving up on
 * re-applying restored layers. Bounds the one-shot listener added by
 * `_scheduleLayerReapplication()` so a failed/slow discovery never
 * leaves a permanent listener registered.
 */
const LAYER_REAPPLY_TIMEOUT_MS = 20000;

/**
 * @module SessionManager
 * @description Manages saving and loading jsMars sessions to/from JSON files.
 * Serializes the application state (layers, view, body), crater data,
 * measurement data, and bookmarks. On load, restores all components
 * to their saved state.
 */
export class SessionManager {
    /**
     * Create a new SessionManager.
     * @param {object|null} craterLayer - CraterCounter instance (or null)
     * @param {object|null} measureTool - MeasureTool instance (or null)
     * @param {object|null} bookmarksTool - BookmarksTool instance (or null)
     * @param {L.Map|null} map - Leaflet map instance for reading live viewport
     */
    constructor(craterLayer, measureTool, bookmarksTool, map = null) {
        this.craterLayer = craterLayer;
        this.measureTool = measureTool;
        this.bookmarksTool = bookmarksTool;
        this.map = map;
    }

    /**
     * Save the current session to a downloadable JSON file.
     * Captures live map viewport (getCenter, getZoom) instead of potentially stale jmarsState.view.
     * Deep-clones state before serialization to avoid capturing live object references.
     */
    saveSession() {
        const liveState = JSON.parse(JSON.stringify(jmarsState.state));
        // Canonicalize body key to lowercase
        if (liveState.body) {
            liveState.body = liveState.body.toLowerCase();
        }

        // Read live viewport from map if available, otherwise use jmarsState
        if (this.map) {
            const center = this.map.getCenter();
            const zoom = this.map.getZoom();
            liveState.view = {
                lat: center.lat,
                lng: center.lng,
                zoom: zoom
            };
        }

        const session = {
            version: '1.0',
            timestamp: new Date().toISOString(),
            state: liveState,
            craters: this.craterLayer ? this.craterLayer.getData() : [],
            measurements: this.measureTool ? this.measureTool.getData() : [],
            bookmarks: this.bookmarksTool ? this.bookmarksTool.getData() : []
        };

        const content = JSON.stringify(session, null, 2);
        this.downloadFile(`jsmars_session_${Date.now()}.json`, content);
    }

    /**
     * Load a session from a JSON File object.
     * Restores body, waits for map to complete body transition,
     * then restores active layers, view, and tool data.
     * @param {File} file - JSON session file chosen by the user
     * @returns {Promise<void>}
     */
    async loadSession(file) {
        try {
            const text = await file.text();
            const session = JSON.parse(text);

            // Validate version (basic check)
            if (!session.version) {
                console.warn('Session file missing version. Trying best effort.');
            }

            // 1. Restore Planetary Body FIRST and WAIT for body switch to complete
            if (session.state && session.state.body) {
                const targetBody = session.state.body.toLowerCase();
                const currentBody = (jmarsState.get('body') || 'mars').toLowerCase();
                if (targetBody !== currentBody) {
                    // Wait for the map to finish switching bodies before restoring layers/view
                    await new Promise((resolve) => {
                        const listener = () => {
                            document.removeEventListener(EVENTS.BODY_CHANGED, listener);
                            // Give map a small moment to complete layer clearing and initialization
                            setTimeout(resolve, 100);
                        };
                        document.addEventListener(EVENTS.BODY_CHANGED, listener);
                        jmarsState.set('body', targetBody);
                        const event = new CustomEvent(EVENTS.BODY_CHANGED, { detail: { body: targetBody } });
                        document.dispatchEvent(event);
                    });
                }
            }

            // 2. NOW restore Active Layers AFTER body switch is complete
            if (session.state && session.state.activeLayers) {
                const restoredLayers = session.state.activeLayers;
                jmarsState.setActiveLayers(restoredLayers);

                // Some restored layers may reference WMS-discovered configs that
                // are still being fetched asynchronously by JMARSMap.discoverLayers().
                // JMARSMap.addLayer() silently no-ops when a config isn't yet in
                // availableLayers, and LayerManager only re-syncs the map on
                // LAYERS_CHANGED (not on a later LAYERS_UPDATED), so those layers
                // could be silently dropped from the map. Re-apply the restored
                // stack once the next LAYERS_UPDATED arrives, but only if the user
                // hasn't changed the active layers in the meantime.
                this._scheduleLayerReapplication(restoredLayers);
            }

            // 3. Restore View (Lat/Lon/Zoom)
            if (session.state && session.state.view) {
                jmarsState.set('view', session.state.view);
                const event = new CustomEvent(EVENTS.UPDATE_VIEW, { 
                    detail: session.state.view 
                });
                document.dispatchEvent(event);
            }

            // 4. Restore Tools
            if (session.craters && this.craterLayer) {
                this.craterLayer.loadData(session.craters);
            }

            if (session.measurements && this.measureTool) {
                this.measureTool.loadData(session.measurements);
            }

            if (session.bookmarks && this.bookmarksTool) {
                this.bookmarksTool.loadData(session.bookmarks);
            }

            alert('Session loaded successfully!');

        } catch (e) {
            console.error(e);
            alert('Error loading session: ' + e.message);
        }
    }

    /**
     * Re-apply a restored active-layer stack once the next `LAYERS_UPDATED`
     * event fires (typically JMARSMap finishing async WMS discovery), so
     * layers whose configs weren't yet available at restore time get
     * attached to the map.
     *
     * Registers a single one-shot `LAYERS_UPDATED` listener that removes
     * itself immediately after firing once, or after a bounded timeout if
     * discovery never completes/fails - it never leaves a permanent
     * listener registered. Reapplication only happens if the current
     * active layers still match what was just restored, so it never
     * overwrites layer changes the user made while discovery was in flight.
     * @param {Array<{id:string, opacity:number, visible:boolean}>} restoredLayers
     *   The active-layer stack that was just restored from the session file.
     * @private
     */
    _scheduleLayerReapplication(restoredLayers) {
        const snapshot = JSON.stringify(restoredLayers);
        let settled = false;

        const cleanup = () => {
            if (settled) return;
            settled = true;
            document.removeEventListener(EVENTS.LAYERS_UPDATED, onLayersUpdated);
            clearTimeout(timeoutId);
        };

        const onLayersUpdated = () => {
            cleanup();
            // Only reapply if nothing changed the active layers since restore
            if (JSON.stringify(jmarsState.get('activeLayers')) === snapshot) {
                jmarsState.setActiveLayers(restoredLayers);
            }
        };

        document.addEventListener(EVENTS.LAYERS_UPDATED, onLayersUpdated);
        const timeoutId = setTimeout(cleanup, LAYER_REAPPLY_TIMEOUT_MS);
    }

    /**
     * Trigger a browser download for the given content.
     * @param {string} filename - Name for the downloaded file
     * @param {string} content - File content string
     * @private
     */
    downloadFile(filename, content) {
        const element = document.createElement('a');
        element.setAttribute('href', 'data:application/json;charset=utf-8,' + encodeURIComponent(content));
        element.setAttribute('download', filename);
        element.style.display = 'none';
        document.body.appendChild(element);
        element.click();
        document.body.removeChild(element);
    }
}
