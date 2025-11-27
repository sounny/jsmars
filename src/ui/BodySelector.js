import { JMARS_CONFIG } from '../jmars-config.js';

export class BodySelector {
    constructor(container) {
        this.container = container;
        this.element = document.createElement('div');
        this.element.className = 'jmars-body-selector';
        this.element.style.padding = '10px';
        this.element.style.borderBottom = '1px solid #444';

        this.render();
        this.container.appendChild(this.element);
    }

    render() {
        const label = document.createElement('label');
        label.textContent = 'Active Body: ';
        label.style.color = '#ccc';
        label.style.marginRight = '5px';

        const select = document.createElement('select');
        select.style.background = '#333';
        select.style.color = '#fff';
        select.style.border = '1px solid #555';
        select.style.padding = '2px 5px';

        Object.keys(JMARS_CONFIG.bodies).forEach(key => {
            const body = JMARS_CONFIG.bodies[key];
            const option = document.createElement('option');
            option.value = key;
            option.textContent = body.name;
            if (key === JMARS_CONFIG.body.toLowerCase()) {
                option.selected = true;
            }
            select.appendChild(option);
        });

        select.addEventListener('change', (e) => {
            const newBody = e.target.value;
            // Dispatch event for other components
            window.dispatchEvent(new CustomEvent('jmars:body-changed', {
                detail: { body: newBody }
            }));
        });

        this.element.appendChild(label);
        this.element.appendChild(select);
    }
}
