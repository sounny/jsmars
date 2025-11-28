export class MeasurementTable {
    constructor(containerId, measureTool) {
        this.container = document.getElementById(containerId);
        this.measureTool = measureTool;
        this.selectedId = null;

        if (!this.container) {
            console.error('MeasurementTable container not found');
            return;
        }

        this.render();

        // Listeners
        document.addEventListener('jmars-measurements-updated', (e) => {
            this.renderRows(e.detail);
        });

        document.addEventListener('jmars-measurement-highlight', (e) => {
            this.highlightRow(e.detail.id);
        });
    }

    render() {
        this.container.innerHTML = `
            <table class="crater-table" style="width:100%; font-size:12px; color:#eee; border-collapse:collapse;">
                <thead>
                    <tr style="border-bottom:1px solid #555; text-align:left;">
                        <th style="padding:4px;">Name</th>
                        <th style="padding:4px;">Type</th>
                        <th style="padding:4px;">Verts</th>
                        <th style="padding:4px;">Value</th>
                    </tr>
                </thead>
                <tbody id="measure-table-body">
                </tbody>
            </table>
            <div id="measure-empty-msg" style="padding:10px; text-align:center; color:#888; font-style:italic;">
                No measurements yet.
            </div>
        `;
        this.tbody = this.container.querySelector('#measure-table-body');
        this.emptyMsg = this.container.querySelector('#measure-empty-msg');
    }

    renderRows(measurements) {
        this.tbody.innerHTML = '';

        if (measurements.length === 0) {
            this.emptyMsg.style.display = 'block';
            return;
        }
        this.emptyMsg.style.display = 'none';

        measurements.forEach(m => {
            const tr = document.createElement('tr');
            tr.id = `measure-row-${m.id}`;
            tr.style.borderBottom = '1px solid #333';
            tr.style.cursor = 'pointer';

            // Name Cell (Editable)
            const nameTd = document.createElement('td');
            nameTd.style.padding = '4px';
            const nameInput = document.createElement('input');
            nameInput.type = 'text';
            nameInput.value = m.name;
            nameInput.style.background = 'transparent';
            nameInput.style.border = 'none';
            nameInput.style.color = '#eee';
            nameInput.style.width = '100%';
            nameInput.style.fontFamily = 'inherit';
            nameInput.style.fontSize = 'inherit';

            // Handle name change
            nameInput.addEventListener('change', (e) => {
                this.measureTool.updateName(m.id, e.target.value);
            });
            nameInput.addEventListener('click', (e) => e.stopPropagation()); // Prevent row click

            nameTd.appendChild(nameInput);

            // Other Cells
            const typeTd = document.createElement('td');
            typeTd.textContent = m.type;
            typeTd.style.padding = '4px';

            const vertsTd = document.createElement('td');
            vertsTd.textContent = m.vertices;
            vertsTd.style.padding = '4px';

            const valueTd = document.createElement('td');
            valueTd.textContent = m.valueStr;
            valueTd.style.padding = '4px';

            tr.appendChild(nameTd);
            tr.appendChild(typeTd);
            tr.appendChild(vertsTd);
            tr.appendChild(valueTd);

            // Row Click
            tr.addEventListener('click', () => {
                this.measureTool.highlight(m.id);
                // We don't need to call highlightRow here because measureTool dispatches an event back to us
                // But for immediate feedback we could.
                // Let's rely on the event loop for consistency.
            });

            this.tbody.appendChild(tr);
        });

        // Re-highlight if selection persists
        if (this.selectedId) {
            this.highlightRow(this.selectedId);
        }
    }

    highlightRow(id) {
        // Remove previous highlight
        if (this.selectedId) {
            const prevRow = document.getElementById(`measure-row-${this.selectedId}`);
            if (prevRow) prevRow.style.background = 'transparent';
        }

        this.selectedId = id;
        const row = document.getElementById(`measure-row-${id}`);
        if (row) {
            row.style.background = '#444';
            row.scrollIntoView({ behavior: 'smooth', block: 'nearest' });
        }
    }
}
