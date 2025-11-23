export class CraterTable {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.craters = [];

        if (!this.container) {
            console.warn('CraterTable container not found');
            return;
        }

        this.render();

        // Listen for updates
        document.addEventListener('jmars-crater-added', (e) => {
            this.addCrater(e.detail);
        });
    }

    render() {
        this.container.innerHTML = `
      <div class="crater-btn-group">
        <button id="crater-export-btn" class="crater-action-btn" style="background: #333;">Export CSV</button>
        <button id="crater-clear-btn" class="crater-action-btn" style="background: #500; border-color: #700;">Clear All</button>
      </div>
      <div class="crater-table-container">
        <table class="crater-table">
          <thead>
            <tr>
              <th>ID</th>
              <th>Lat</th>
              <th>Lon</th>
              <th>Diam (km)</th>
              <th></th>
            </tr>
          </thead>
          <tbody id="crater-table-body">
          </tbody>
        </table>
      </div>
    `;
        this.tbody = this.container.querySelector('#crater-table-body');

        this.container.querySelector('#crater-export-btn').addEventListener('click', () => this.exportCSV());
        this.container.querySelector('#crater-clear-btn').addEventListener('click', () => this.clearAll());
    }

    addCrater(crater) {
        if (!this.tbody) return;
        this.craters.push(crater);

        const tr = document.createElement('tr');
        tr.id = `crater-row-${crater.id}`;
        tr.style.borderBottom = '1px solid #333';
        tr.innerHTML = `
      <td>${crater.id.toString().slice(-4)}</td>
      <td>${crater.lat.toFixed(2)}</td>
      <td>${crater.lng.toFixed(2)}</td>
      <td>${(crater.diameter / 1000).toFixed(1)}</td>
      <td style="text-align: right;">
        <button class="delete-crater-btn" data-id="${crater.id}" style="background: none; border: none; color: #f55; cursor: pointer;">&times;</button>
      </td>
    `;

        tr.querySelector('.delete-crater-btn').addEventListener('click', (e) => {
            const id = parseInt(e.target.dataset.id);
            this.removeCrater(id);
        });

        this.tbody.prepend(tr); // Add new at top
    }

    removeCrater(id) {
        // Remove from local list
        this.craters = this.craters.filter(c => c.id !== id);

        // Remove from DOM
        const row = this.tbody.querySelector(`#crater-row-${id}`);
        if (row) row.remove();

        // Dispatch removal event
        const event = new CustomEvent('jmars-crater-remove-request', { detail: { id } });
        document.dispatchEvent(event);
    }

    clearAll() {
        if (!confirm('Are you sure you want to clear all craters?')) return;

        this.craters = [];
        this.tbody.innerHTML = '';

        // Dispatch clear event
        const event = new CustomEvent('jmars-crater-clear-request');
        document.dispatchEvent(event);
    }

    exportCSV() {
        if (this.craters.length === 0) {
            alert('No craters to export.');
            return;
        }

        const headers = ['ID,Lat,Lon,Diameter_km\n'];
        const rows = this.craters.map(c =>
            `${c.id},${c.lat.toFixed(5)},${c.lng.toFixed(5)},${(c.diameter / 1000).toFixed(3)}`
        );

        const csvContent = "data:text/csv;charset=utf-8," + headers.join('') + rows.join('\n');
        const encodedUri = encodeURI(csvContent);
        const link = document.createElement("a");
        link.setAttribute("href", encodedUri);
        link.setAttribute("download", "jmars_craters.csv");
        document.body.appendChild(link);
        link.click();
        document.body.removeChild(link);
    }
}
