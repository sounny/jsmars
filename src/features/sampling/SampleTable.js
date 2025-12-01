export class SampleTable {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.samples = [];
        
        if (!this.container) return;
        this.render();

        document.addEventListener('jmars-samples-updated', (e) => {
            this.samples = e.detail.samples;
            this.updateTable();
        });
    }

    render() {
        this.container.innerHTML = `
            <div style="margin-bottom: 5px;">
                <button id="sample-export-btn" class="tool-btn" style="font-size: 11px; width: auto; padding: 4px 8px;">Export CSV</button>
                <button id="sample-clear-btn" class="tool-btn" style="font-size: 11px; width: auto; padding: 4px 8px; background: #500;">Clear</button>
            </div>
            <div class="crater-table-container" style="height: 150px;">
                <table class="crater-table">
                    <thead>
                        <tr>
                            <th>ID</th>
                            <th>Lat</th>
                            <th>Lon</th>
                            <th>Values</th>
                        </tr>
                    </thead>
                    <tbody id="sample-table-body"></tbody>
                </table>
            </div>
        `;
        
        // Bind buttons later in integration or dispatch events?
        // Since SamplingTool handles logic, these buttons should call SamplingTool methods.
        // But SampleTable doesn't have reference to Tool.
        // Solution: Dispatch request events.
        
        this.container.querySelector('#sample-export-btn').addEventListener('click', () => {
            document.dispatchEvent(new CustomEvent('jmars-sample-export-request'));
        });
        
        this.container.querySelector('#sample-clear-btn').addEventListener('click', () => {
            document.dispatchEvent(new CustomEvent('jmars-sample-clear-request'));
        });
    }

    updateTable() {
        const tbody = this.container.querySelector('#sample-table-body');
        if (!tbody) return;
        tbody.innerHTML = '';
        
        this.samples.forEach(s => {
            const tr = document.createElement('tr');
            const valSummary = s.values.length > 0 ? s.values[0].value : '-'; // Show first value summary
            const fullSummary = s.values.map(v => `${v.name}: ${v.value}`).join('\n') || '-';

            const idCell = document.createElement('td');
            idCell.textContent = s.id;
            tr.appendChild(idCell);

            const latCell = document.createElement('td');
            latCell.textContent = s.lat.toFixed(2);
            tr.appendChild(latCell);

            const lngCell = document.createElement('td');
            lngCell.textContent = s.lng.toFixed(2);
            tr.appendChild(lngCell);

            const valuesCell = document.createElement('td');
            valuesCell.textContent = valSummary;
            valuesCell.title = fullSummary;
            tr.appendChild(valuesCell);

            tbody.appendChild(tr);
        });
    }
}
