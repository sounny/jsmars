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
        // Note: Export and Clear buttons are defined in index.html
        // SampleTable only renders the data table
        this.container.innerHTML = `
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
