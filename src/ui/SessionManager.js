import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';

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
     */
    constructor(craterLayer, measureTool, bookmarksTool) {
        this.craterLayer = craterLayer;
        this.measureTool = measureTool;
        this.bookmarksTool = bookmarksTool;
    }

    /**
     * Save the current session to a downloadable JSON file.
     * Deep-clones state before serialization to avoid capturing
     * live object references. Captures live map viewport.
     */
    saveSession() {
        const liveState = JSON.parse(JSON.stringify(jmarsState.state));
        // Canonicalize body key to lowercase
        if (liveState.body) {
            liveState.body = liveState.body.toLowerCase();
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
     * Restores body FIRST, then restores active layers, view, and tool data.
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

            // 1. Restore Planetary Body FIRST so switchBody does not overwrite restored layers
            if (session.state && session.state.body) {
                const targetBody = session.state.body.toLowerCase();
                const currentBody = (jmarsState.get('body') || 'mars').toLowerCase();
                if (targetBody !== currentBody) {
                    jmarsState.set('body', targetBody);
                    const event = new CustomEvent(EVENTS.BODY_CHANGED, { detail: { body: targetBody } });
                    document.dispatchEvent(event);
                }
            }

            // 2. Restore Active Layers & Layer Properties after body is established
            if (session.state && session.state.activeLayers) {
                jmarsState.setActiveLayers(session.state.activeLayers);
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
