import { jmarsState } from '../jmars-state.js';
import { EVENTS } from '../constants.js';

export class SessionManager {
    constructor(craterLayer, measureTool, bookmarksTool) {
        this.craterLayer = craterLayer;
        this.measureTool = measureTool;
        this.bookmarksTool = bookmarksTool;
    }

    saveSession() {
        const session = {
            version: '1.0',
            timestamp: new Date().toISOString(),
            state: jmarsState.state,
            craters: this.craterLayer ? this.craterLayer.getData() : [],
            measurements: this.measureTool ? this.measureTool.getData() : [],
            bookmarks: this.bookmarksTool ? this.bookmarksTool.getData() : []
        };

        const content = JSON.stringify(session, null, 2);
        this.downloadFile(`jsmars_session_${Date.now()}.json`, content);
    }

    async loadSession(file) {
        try {
            const text = await file.text();
            const session = JSON.parse(text);

            // Validate version (basic check)
            if (!session.version) {
                console.warn('Session file missing version. Trying best effort.');
            }

            // 1. Restore State
            if (session.state) {
                // Active Layers
                if (session.state.activeLayers) {
                    jmarsState.state.activeLayers = session.state.activeLayers;
                    jmarsState.emit(EVENTS.LAYERS_CHANGED, jmarsState.state.activeLayers);
                }

                // View (Lat/Lon/Zoom)
                if (session.state.view) {
                    jmarsState.set('view', session.state.view);
                    const event = new CustomEvent(EVENTS.UPDATE_VIEW, { 
                        detail: session.state.view 
                    });
                    document.dispatchEvent(event);
                }

                // Body
                if (session.state.body && session.state.body !== jmarsState.get('body')) {
                    jmarsState.set('body', session.state.body);
                    const event = new CustomEvent(EVENTS.BODY_CHANGED, { detail: { body: session.state.body } });
                    document.dispatchEvent(event);
                }
            }

            // 2. Restore Tools
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
