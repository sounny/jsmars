import { jmarsState } from '../jmars-state.js';

export class SessionManager {
    constructor(craterLayer, measureTool) {
        this.craterLayer = craterLayer;
        this.measureTool = measureTool;
    }

    saveSession() {
        const session = {
            version: '1.0',
            timestamp: new Date().toISOString(),
            state: jmarsState.state,
            craters: this.craterLayer ? this.craterLayer.getData() : [],
            measurements: this.measureTool ? this.measureTool.getData() : []
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
                // We need to reset state carefully.
                // Updating individual properties to trigger listeners.
                
                // Active Layers
                if (session.state.activeLayers) {
                    // Clear current active layers first?
                    // Or just set the state. jmarsState doesn't have a "setAll" method,
                    // but we can manually update the state object and emit 'layers-changed'.
                    
                    // It's safer to use the API if possible, or extend jmarsState.
                    // Let's overwrite the array and emit.
                    jmarsState.state.activeLayers = session.state.activeLayers;
                    jmarsState.emit('layers-changed', jmarsState.state.activeLayers);
                }

                // View (Lat/Lon/Zoom)
                if (session.state.view) {
                    jmarsState.set('view', session.state.view);
                    // JMARSMap doesn't listen to 'view' changes on state directly in the current code,
                    // it only sets them on init. 
                    // We might need to manually update the map view or ensure JMARSMap listens.
                    // But wait, jmars-state.js says: 
                    // "this.emit('change', ...)"
                    
                    // Check JMARSMap again. It listens to 'jmars:body-changed'. 
                    // It DOES NOT seem to listen to view changes after init.
                    
                    // We can dispatch a custom event or handle it here if we have access to map.
                    // We don't have direct access to map here, only tools.
                    // But we can dispatch 'jmars:update-view' if we add a listener in main.
                    const event = new CustomEvent('jmars:update-view', { 
                        detail: session.state.view 
                    });
                    document.dispatchEvent(event);
                }

                // Body
                if (session.state.body && session.state.body !== jmarsState.get('body')) {
                    jmarsState.set('body', session.state.body);
                    // This should trigger body switch
                    const event = new CustomEvent('jmars:body-changed', { detail: { body: session.state.body } });
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
