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
      <div class="crater-table-container" style="max-height: 200px; overflow-y: auto; background: rgba(0,0,0,0.8); color: #fff; font-size: 12px; padding: 5px;">
        <table style="width: 100%; border-collapse: collapse;">
          <thead>
            <tr style="border-bottom: 1px solid #555; text-align: left;">
              <th>ID</th>
              <th>Lat</th>
              <th>Lon</th>
              <th>Diam (km)</th>
            </tr>
          </thead>
          <tbody id="crater-table-body">
          </tbody>
        </table>
      </div>
    `;
        this.tbody = this.container.querySelector('#crater-table-body');
    }

    addCrater(crater) {
        if (!this.tbody) return;

        const tr = document.createElement('tr');
        tr.style.borderBottom = '1px solid #333';
        tr.innerHTML = `
      <td>${crater.id.toString().slice(-4)}</td>
      <td>${crater.lat.toFixed(2)}</td>
      <td>${crater.lng.toFixed(2)}</td>
      <td>${(crater.diameter / 1000).toFixed(1)}</td>
    `;
        this.tbody.prepend(tr); // Add new at top
    }
}
