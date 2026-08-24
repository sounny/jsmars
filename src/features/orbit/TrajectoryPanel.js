import { TrajectoryEngine } from './TrajectoryEngine.js';

/**
 * @class TrajectoryPanel
 * @description Astrodynamics mission design UI: Hohmann transfer budgets,
 * C3 launch energy, TMI/MOI Delta-V, and launch window opportunities.
 */
export class TrajectoryPanel {
  /**
   * @param {HTMLElement|string} containerOrId
   */
  constructor(containerOrId) {
    this.container = typeof containerOrId === 'string'
      ? document.getElementById(containerOrId)
      : containerOrId;

    this.fromBody = 'earth';
    this.toBody = 'mars';
    this.parkAlt = 300;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div class="trajectory-panel" style="padding:8px; display:flex; flex-direction:column; gap:8px;">
        <div style="display:flex; gap:6px;">
          <div style="flex:1;">
            <label style="font-size:10px; color:#94a3b8;">Origin Body</label>
            <select id="traj-from-select" class="stamp-select" style="width:100%; padding:3px; font-size:11px; background:#0f172a; color:#fff; border:1px solid #334155;">
              <option value="earth" selected>Earth</option>
              <option value="mars">Mars</option>
              <option value="venus">Venus</option>
            </select>
          </div>
          <div style="flex:1;">
            <label style="font-size:10px; color:#94a3b8;">Destination</label>
            <select id="traj-to-select" class="stamp-select" style="width:100%; padding:3px; font-size:11px; background:#0f172a; color:#fff; border:1px solid #334155;">
              <option value="mars" selected>Mars</option>
              <option value="earth">Earth</option>
              <option value="venus">Venus</option>
              <option value="jupiter">Jupiter</option>
            </select>
          </div>
        </div>

        <div style="display:flex; justify-content:space-between; align-items:center; background:#0f172a; padding:6px; border:1px solid #1e293b; border-radius:4px;">
          <span style="font-size:10px; color:#94a3b8;">Parking Altitude</span>
          <span style="font-size:11px; color:#38bdf8; font-weight:bold;">300 km (LEO / LMO)</span>
        </div>

        <button id="traj-compute-btn" class="tool-btn" style="background:#0284c7;">Calculate Transfer Budget</button>

        <div id="traj-results-container" style="background:#0b1329; border:1px solid #1e293b; border-radius:4px; padding:6px; font-size:11px; display:flex; flex-direction:column; gap:4px;"></div>

        <div style="margin-top:4px;">
          <div style="font-size:11px; font-weight:bold; color:#38bdf8; margin-bottom:4px;">Earth-Mars Launch Windows</div>
          <div id="traj-windows-container" style="max-height:120px; overflow-y:auto; background:#0f172a; border:1px solid #1e293b; border-radius:3px; padding:4px;"></div>
        </div>

        <button id="traj-export-btn" class="crater-action-btn" style="background:#1e293b; font-size:10px; margin-top:2px;">Export Mission Plan CSV</button>
      </div>
    `;

    this.fromSelect = this.container.querySelector('#traj-from-select');
    this.toSelect = this.container.querySelector('#traj-to-select');
    this.computeBtn = this.container.querySelector('#traj-compute-btn');
    this.resultsContainer = this.container.querySelector('#traj-results-container');
    this.windowsContainer = this.container.querySelector('#traj-windows-container');
    this.exportBtn = this.container.querySelector('#traj-export-btn');

    this.computeBtn.addEventListener('click', () => this.calculate());
    this.exportBtn.addEventListener('click', () => this.exportCSV());

    this.calculate();
    this.renderWindows();
  }

  calculate() {
    this.fromBody = this.fromSelect.value;
    this.toBody = this.toSelect.value;

    const sol = TrajectoryEngine.computeHohmannTransfer(this.fromBody, this.toBody, 300, 300);
    this.lastSolution = sol;

    this.resultsContainer.innerHTML = `
      <div style="display:flex; justify-content:space-between;">
        <span style="color:#94a3b8;">Flight Duration:</span>
        <span style="color:#f8fafc; font-weight:bold;">${Math.round(sol.tofDays)} days (${sol.tofMonths.toFixed(1)} mo)</span>
      </div>
      <div style="display:flex; justify-content:space-between;">
        <span style="color:#94a3b8;">Departure Injection (TMI):</span>
        <span style="color:#38bdf8; font-weight:bold;">${sol.deltaVDepartKmS.toFixed(3)} km/s</span>
      </div>
      <div style="display:flex; justify-content:space-between;">
        <span style="color:#94a3b8;">Arrival Insertion (MOI):</span>
        <span style="color:#38bdf8; font-weight:bold;">${sol.deltaVArriveKmS.toFixed(3)} km/s</span>
      </div>
      <div style="display:flex; justify-content:space-between; border-top:1px solid #1e293b; padding-top:2px;">
        <span style="color:#fbbf24; font-weight:bold;">Total Transfer Δv:</span>
        <span style="color:#fbbf24; font-weight:bold;">${sol.totalDeltaVKmS.toFixed(3)} km/s</span>
      </div>
      <div style="display:flex; justify-content:space-between;">
        <span style="color:#94a3b8;">Launch Energy (C₃):</span>
        <span style="color:#f8fafc;">${sol.c3LaunchEnergy.toFixed(2)} km²/s²</span>
      </div>
      <div style="display:flex; justify-content:space-between;">
        <span style="color:#94a3b8;">Departure Phase Angle:</span>
        <span style="color:#f8fafc;">${sol.departurePhaseAngleDeg.toFixed(1)}°</span>
      </div>
    `;
  }

  renderWindows() {
    const windows = TrajectoryEngine.getUpcomingMarsLaunchWindows(2024, 5);
    this.windows = windows;

    this.windowsContainer.innerHTML = windows.map(w => `
      <div style="display:flex; justify-content:space-between; font-size:10px; padding:3px 0; border-bottom:1px solid #1e293b;">
        <span style="color:#38bdf8; font-weight:bold;">${w.departureDate.slice(0,7)}</span>
        <span style="color:#94a3b8;">Arr: ${w.arrivalDate.slice(0,7)}</span>
        <span style="color:#f8fafc;">C₃: ${w.c3_typical.toFixed(1)}</span>
      </div>
    `).join('');
  }

  exportCSV() {
    if (!this.lastSolution) return;
    const s = this.lastSolution;
    const rows = [
      'Parameter,Value,Unit',
      `Origin,${s.fromBody},`,
      `Destination,${s.toBody},`,
      `FlightDuration,${s.tofDays.toFixed(2)},days`,
      `DeltaV_Departure,${s.deltaVDepartKmS.toFixed(4)},km/s`,
      `DeltaV_Arrival,${s.deltaVArriveKmS.toFixed(4)},km/s`,
      `Total_DeltaV,${s.totalDeltaVKmS.toFixed(4)},km/s`,
      `C3_Launch_Energy,${s.c3LaunchEnergy.toFixed(3)},km^2/s^2`,
      `Departure_Phase_Angle,${s.departurePhaseAngleDeg.toFixed(2)},deg`,
      `Synodic_Period,${s.synodicPeriodDays.toFixed(2)},days`
    ];

    const blob = new Blob([rows.join('\n')], { type: 'text/csv' });
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = `trajectory_transfer_${s.fromBody}_${s.toBody}_${Date.now()}.csv`;
    a.click();
    URL.revokeObjectURL(url);
  }
}
