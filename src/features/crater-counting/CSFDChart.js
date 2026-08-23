import { CSFDEngine } from './CSFDEngine.js';

/**
 * @module CSFDChart
 * @description Log-Log Cumulative Crater Size-Frequency Distribution (CSFD) chart
 * with Hartmann-Neukum isochron overlays and geological epoch boundaries.
 */
export class CSFDChart {
  /**
   * @param {HTMLElement|string} container - Container element or ID
   */
  constructor(container) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.canvas = null;
    this.ctx = null;
    this.lastCSFD = null;
    this.showIsochrons = true;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div style="margin-bottom: 6px; display: flex; justify-content: space-between; align-items: center;">
        <label style="font-size: 10px; color: #94a3b8; display: flex; align-items: center; gap: 4px; cursor: pointer;">
          <input type="checkbox" id="csfd-isochron-toggle" checked style="accent-color: #f97316;"> Isochrons
        </label>
        <button id="csfd-export-png-btn" class="tool-btn" style="padding: 2px 6px; font-size: 10px;">PNG</button>
      </div>
      <canvas id="csfd-canvas" width="280" height="170" style="background: #111827; border: 1px solid #374151; border-radius: 4px; display: block; width: 100%; height: 170px;"></canvas>
    `;

    this.canvas = this.container.querySelector('#csfd-canvas');
    this.ctx = this.canvas.getContext('2d');

    const toggle = this.container.querySelector('#csfd-isochron-toggle');
    toggle.addEventListener('change', (e) => {
      this.showIsochrons = e.target.checked;
      this.draw();
    });

    const exportBtn = this.container.querySelector('#csfd-export-png-btn');
    exportBtn.addEventListener('click', () => this.exportPNG());

    this.drawEmpty();
  }

  setCSFD(csfdResult) {
    this.lastCSFD = csfdResult;
    this.draw();
  }

  exportPNG() {
    if (!this.canvas) return;
    const link = document.createElement('a');
    link.download = 'crater_csfd_dating.png';
    link.href = this.canvas.toDataURL();
    link.click();
  }

  drawEmpty() {
    if (!this.ctx) return;
    const w = this.canvas.width;
    const h = this.canvas.height;
    this.ctx.clearRect(0, 0, w, h);
    this.ctx.fillStyle = '#111827';
    this.ctx.fillRect(0, 0, w, h);
    this.ctx.fillStyle = '#6b7280';
    this.ctx.font = '11px sans-serif';
    this.ctx.textAlign = 'center';
    this.ctx.fillText('Count craters to generate CSFD & age', w / 2, h / 2);
  }

  draw() {
    if (!this.lastCSFD || !this.lastCSFD.bins || this.lastCSFD.bins.length === 0) {
      this.drawEmpty();
      return;
    }

    const ctx = this.ctx;
    const w = this.canvas.width;
    const h = this.canvas.height;
    const padL = 36;
    const padR = 10;
    const padT = 16;
    const padB = 24;

    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = '#111827';
    ctx.fillRect(0, 0, w, h);

    // Log-log domain:
    // D from 0.1 km to 100 km (log10: -1 to 2)
    // N(>D)/10^6 km^2 from 10^-2 to 10^5 (log10: -2 to 5)
    const logMinD = -1.0;
    const logMaxD = 2.0;
    const logMinN = -1.0;
    const logMaxN = 5.0;

    const scaleX = (dKm) => {
      const logD = Math.log10(Math.max(0.01, dKm));
      return padL + ((logD - logMinD) / (logMaxD - logMinD)) * (w - padL - padR);
    };

    const scaleY = (nVal) => {
      const logN = Math.log10(Math.max(1e-4, nVal));
      return h - padB - ((logN - logMinN) / (logMaxN - logMinN)) * (h - padT - padB);
    };

    // Grid lines for orders of magnitude
    ctx.strokeStyle = '#1f2937';
    ctx.lineWidth = 1;

    for (let logD = logMinD; logD <= logMaxD; logD++) {
      const x = scaleX(Math.pow(10, logD));
      ctx.beginPath();
      ctx.moveTo(x, padT);
      ctx.lineTo(x, h - padB);
      ctx.stroke();

      ctx.fillStyle = '#9ca3af';
      ctx.font = '8px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(logD === 0 ? '1km' : `10^${logD}`, x, h - 8);
    }

    for (let logN = 0; logN <= logMaxN; logN += 2) {
      const y = scaleY(Math.pow(10, logN));
      ctx.beginPath();
      ctx.moveTo(padL, y);
      ctx.lineTo(w - padR, y);
      ctx.stroke();

      ctx.fillStyle = '#9ca3af';
      ctx.font = '8px sans-serif';
      ctx.textAlign = 'right';
      ctx.fillText(`10^${logN}`, padL - 2, y + 3);
    }

    // Axes
    ctx.strokeStyle = '#4b5563';
    ctx.beginPath();
    ctx.moveTo(padL, padT);
    ctx.lineTo(padL, h - padB);
    ctx.lineTo(w - padR, h - padB);
    ctx.stroke();

    // Draw Reference Isochrons (4.0 Ga, 3.5 Ga, 1.0 Ga, 100 Ma)
    if (this.showIsochrons) {
      const isochronAges = [
        { age: 4.0, color: '#ef4444', label: '4.0 Ga' },
        { age: 3.5, color: '#f59e0b', label: '3.5 Ga' },
        { age: 1.0, color: '#10b981', label: '1.0 Ga' },
        { age: 0.1, color: '#3b82f6', label: '100 Ma' }
      ];

      isochronAges.forEach(iso => {
        const curve = CSFDEngine.getIsochronCurve(iso.age);
        ctx.strokeStyle = iso.color;
        ctx.lineWidth = 1;
        ctx.setLineDash([2, 2]);
        ctx.beginPath();
        curve.forEach((pt, i) => {
          const x = scaleX(pt.diameterKm);
          const y = scaleY(pt.cumulativeN);
          if (i === 0) ctx.moveTo(x, y);
          else ctx.lineTo(x, y);
        });
        ctx.stroke();
        ctx.setLineDash([]);
      });
    }

    // Draw Measured CSFD Points & Step Curve (Orange)
    const bins = this.lastCSFD.bins.filter(b => b.cumulativeDensity > 0);
    if (bins.length > 0) {
      ctx.strokeStyle = '#f97316';
      ctx.lineWidth = 2;
      ctx.beginPath();
      bins.forEach((b, i) => {
        const x = scaleX(b.diameterKm);
        const y = scaleY(b.cumulativeDensity);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.stroke();

      // Draw point markers
      ctx.fillStyle = '#fb923c';
      bins.forEach(b => {
        const x = scaleX(b.diameterKm);
        const y = scaleY(b.cumulativeDensity);
        ctx.beginPath();
        ctx.arc(x, y, 3, 0, 2 * Math.PI);
        ctx.fill();
        ctx.strokeStyle = '#ffffff';
        ctx.lineWidth = 1;
        ctx.stroke();
      });
    }

    // Title & Age Badge
    ctx.fillStyle = '#f97316';
    ctx.font = '9px sans-serif';
    ctx.textAlign = 'left';
    ctx.fillText(`CSFD: ~${this.lastCSFD.estimatedAgeGa} Ga (${this.lastCSFD.epoch})`, padL + 4, padT - 4);
  }
}
