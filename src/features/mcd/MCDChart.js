/**
 * @module MCDChart
 * @description 2D Canvas renderer for Mars Climate Database (MCD) vertical atmospheric profiles.
 * Supports Temperature T(z), Pressure P(z) (log scale), Density rho(z), and Wind Speed.
 */
export class MCDChart {
  /**
   * @param {HTMLElement|string} container - Container DOM element or ID
   */
  constructor(container) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.canvas = null;
    this.ctx = null;
    this.metric = 'temp'; // 'temp', 'pressure', 'density', 'wind'
    this.lastProfile = null;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div style="margin-bottom: 6px; display: flex; justify-content: space-between; align-items: center;">
        <div style="display: flex; gap: 4px;">
          <button id="mcd-tab-temp" class="tool-btn" style="padding: 2px 6px; font-size: 10px; background: #0284c7;">Temp</button>
          <button id="mcd-tab-press" class="tool-btn" style="padding: 2px 6px; font-size: 10px; background: #334155;">Pressure</button>
          <button id="mcd-tab-wind" class="tool-btn" style="padding: 2px 6px; font-size: 10px; background: #334155;">Wind</button>
        </div>
        <button id="mcd-export-png-btn" class="tool-btn" style="padding: 2px 6px; font-size: 10px;">PNG</button>
      </div>
      <canvas id="mcd-canvas" width="280" height="150" style="background: #111827; border: 1px solid #374151; border-radius: 4px; display: block; width: 100%; height: 150px;"></canvas>
    `;

    this.canvas = this.container.querySelector('#mcd-canvas');
    this.ctx = this.canvas.getContext('2d');

    const tempTab = this.container.querySelector('#mcd-tab-temp');
    const pressTab = this.container.querySelector('#mcd-tab-press');
    const windTab = this.container.querySelector('#mcd-tab-wind');
    const exportBtn = this.container.querySelector('#mcd-export-png-btn');

    const setTab = (newMetric, activeBtn) => {
      this.metric = newMetric;
      [tempTab, pressTab, windTab].forEach(b => b.style.background = '#334155');
      activeBtn.style.background = '#0284c7';
      this.draw();
    };

    tempTab.addEventListener('click', () => setTab('temp', tempTab));
    pressTab.addEventListener('click', () => setTab('pressure', pressTab));
    windTab.addEventListener('click', () => setTab('wind', windTab));
    exportBtn.addEventListener('click', () => this.exportPNG());

    this.drawEmpty();
  }

  setProfile(profile) {
    this.lastProfile = profile;
    this.draw();
  }

  exportPNG() {
    if (!this.canvas) return;
    const link = document.createElement('a');
    link.download = `mcd_profile_${this.metric}.png`;
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
    this.ctx.fillText('Calculate atmospheric profile to plot', w / 2, h / 2);
  }

  draw() {
    if (!this.lastProfile || !this.lastProfile.layers) {
      this.drawEmpty();
      return;
    }

    const ctx = this.ctx;
    const w = this.canvas.width;
    const h = this.canvas.height;
    const padL = 30;
    const padR = 10;
    const padT = 16;
    const padB = 22;

    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = '#111827';
    ctx.fillRect(0, 0, w, h);

    const layers = this.lastProfile.layers;
    const maxAlt = Math.max(...layers.map(l => l.altitudeKm));

    // Y scale: Altitude (0 at bottom, maxAlt at top)
    const scaleY = (altKm) => h - padB - (altKm / maxAlt) * (h - padT - padB);

    // Axes
    ctx.strokeStyle = '#4b5563';
    ctx.lineWidth = 1;
    ctx.beginPath();
    ctx.moveTo(padL, padT);
    ctx.lineTo(padL, h - padB);
    ctx.lineTo(w - padR, h - padB);
    ctx.stroke();

    // Y axis labels (Altitude km)
    ctx.fillStyle = '#9ca3af';
    ctx.font = '9px sans-serif';
    ctx.textAlign = 'right';
    ctx.fillText(`${maxAlt}km`, padL - 2, padT + 8);
    ctx.fillText('0km', padL - 2, h - padB);

    if (this.metric === 'temp') {
      const temps = layers.map(l => l.temperatureK);
      let minT = Math.floor(Math.min(...temps) - 5);
      let maxT = Math.ceil(Math.max(...temps) + 5);
      const scaleX = (t) => padL + ((t - minT) / (maxT - minT || 1)) * (w - padL - padR);

      // Plot Temperature Curve (Orange)
      ctx.strokeStyle = '#f97316';
      ctx.lineWidth = 2;
      ctx.beginPath();
      layers.forEach((l, i) => {
        const x = scaleX(l.temperatureK);
        const y = scaleY(l.altitudeKm);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.stroke();

      // Labels
      ctx.textAlign = 'center';
      ctx.fillText(`${minT}K`, padL + 8, h - 8);
      ctx.fillText(`${maxT}K`, w - padR - 10, h - 8);

      ctx.fillStyle = '#f97316';
      ctx.textAlign = 'left';
      ctx.fillText('Atmospheric Temp T(z)', padL + 4, padT - 4);

    } else if (this.metric === 'pressure') {
      const pressures = layers.map(l => l.pressurePa);
      const minP = Math.min(...pressures);
      const maxP = Math.max(...pressures);

      // Log10 scale for pressure
      const logMin = Math.log10(Math.max(0.01, minP));
      const logMax = Math.log10(maxP);
      const scaleX = (p) => padL + ((Math.log10(Math.max(0.01, p)) - logMin) / (logMax - logMin || 1)) * (w - padL - padR);

      // Plot Pressure Curve (Cyan)
      ctx.strokeStyle = '#38bdf8';
      ctx.lineWidth = 2;
      ctx.beginPath();
      layers.forEach((l, i) => {
        const x = scaleX(l.pressurePa);
        const y = scaleY(l.altitudeKm);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.stroke();

      // Labels
      ctx.textAlign = 'center';
      ctx.fillText(`${minP.toFixed(1)}Pa`, padL + 12, h - 8);
      ctx.fillText(`${maxP.toFixed(0)}Pa`, w - padR - 12, h - 8);

      ctx.fillStyle = '#38bdf8';
      ctx.textAlign = 'left';
      ctx.fillText('Pressure P(z) [Log Scale]', padL + 4, padT - 4);

    } else if (this.metric === 'wind') {
      const winds = layers.map(l => l.windSpeedMs);
      const maxW = Math.ceil(Math.max(...winds) + 5);
      const scaleX = (wSpeed) => padL + (wSpeed / maxW) * (w - padL - padR);

      // Plot Wind Speed Curve (Emerald)
      ctx.strokeStyle = '#10b981';
      ctx.lineWidth = 2;
      ctx.beginPath();
      layers.forEach((l, i) => {
        const x = scaleX(l.windSpeedMs);
        const y = scaleY(l.altitudeKm);
        if (i === 0) ctx.moveTo(x, y);
        else ctx.lineTo(x, y);
      });
      ctx.stroke();

      // Labels
      ctx.textAlign = 'center';
      ctx.fillText('0 m/s', padL + 8, h - 8);
      ctx.fillText(`${maxW} m/s`, w - padR - 10, h - 8);

      ctx.fillStyle = '#10b981';
      ctx.textAlign = 'left';
      ctx.fillText('Horizontal Wind Speed', padL + 4, padT - 4);
    }
  }
}
