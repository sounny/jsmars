export class StyleEditor {
    constructor(map) {
        this.map = map;
        this.container = null;
        this.currentLayer = null;
        this.init();
    }

    init() {
        this.container = document.createElement('div');
        this.container.id = 'style-editor';
        this.container.style.position = 'absolute';
        this.container.style.zIndex = '2000';
        this.container.style.background = '#222';
        this.container.style.border = '1px solid #555';
        this.container.style.padding = '10px';
        this.container.style.color = '#fff';
        this.container.style.display = 'none';
        this.container.style.borderRadius = '4px';
        this.container.style.boxShadow = '0 0 10px rgba(0,0,0,0.5)';
        this.container.style.width = '200px';

        this.container.innerHTML = `
            <div style="margin-bottom: 8px; font-weight: bold; display: flex; justify-content: space-between;">
                <span>Edit Style</span>
                <span id="style-editor-close" style="cursor: pointer;">&times;</span>
            </div>
            <div class="style-field">
                <label>Stroke Color</label>
                <input type="color" id="style-stroke-color">
            </div>
            <div class="style-field">
                <label>Fill Color</label>
                <input type="color" id="style-fill-color">
            </div>
            <div class="style-field">
                <label>Weight</label>
                <input type="range" id="style-weight" min="1" max="10" step="1">
            </div>
            <div class="style-field">
                <label>Opacity</label>
                <input type="range" id="style-opacity" min="0" max="1" step="0.1">
            </div>
            <style>
                .style-field { margin-bottom: 5px; font-size: 12px; }
                .style-field label { display: block; margin-bottom: 2px; }
                .style-field input[type="color"] { width: 100%; height: 25px; border: none; }
                .style-field input[type="range"] { width: 100%; }
            </style>
        `;

        document.body.appendChild(this.container);

        // Bind events
        this.container.querySelector('#style-editor-close').addEventListener('click', () => this.close());

        const strokeColor = this.container.querySelector('#style-stroke-color');
        const fillColor = this.container.querySelector('#style-fill-color');
        const weight = this.container.querySelector('#style-weight');
        const opacity = this.container.querySelector('#style-opacity');

        strokeColor.addEventListener('input', (e) => this.updateStyle({ color: e.target.value }));
        fillColor.addEventListener('input', (e) => this.updateStyle({ fillColor: e.target.value }));
        weight.addEventListener('input', (e) => this.updateStyle({ weight: parseInt(e.target.value) }));
        opacity.addEventListener('input', (e) => this.updateStyle({ fillOpacity: parseFloat(e.target.value) }));
    }

    open(layer, point) {
        this.currentLayer = layer;

        // Load current styles
        const options = layer.options;
        this.container.querySelector('#style-stroke-color').value = options.color || '#3388ff';
        this.container.querySelector('#style-fill-color').value = options.fillColor || options.color || '#3388ff';
        this.container.querySelector('#style-weight').value = options.weight || 3;
        this.container.querySelector('#style-opacity').value = options.fillOpacity !== undefined ? options.fillOpacity : 0.2;

        // Position
        this.container.style.display = 'block';
        this.container.style.left = point.x + 10 + 'px';
        this.container.style.top = point.y + 10 + 'px';
    }

    close() {
        this.container.style.display = 'none';
        this.currentLayer = null;
    }

    updateStyle(styles) {
        if (this.currentLayer) {
            this.currentLayer.setStyle(styles);
        }
    }
}
