/**
 * @module KRCChart
 * @description Multi-mode 2D Canvas chart renderer for KRC Mars thermal model results.
 * Supports Diurnal curve (0-24h LTST), Subsurface Depth profile (0-1m), and Seasonal curve (Ls 0-360°).
 */
export class KRCChart {
  /**
   * @param {HTMLElement|string} container - Container element or DOM ID.
   */
  constructor(container) {
    this.container = typeof container === 'string' ? document.getElementById(container) : container;
    this.canvas = null;
    this.ctx = null;
    this.mode = 'diurnal'; // 'diurnal', 'depth', 'seasonal'
    this.lastResult = null;
    this.seasonalData = null;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div style="margin-bottom: 6px; display: flex; justify-content: space-between; align-items: center;">
        <div style="display: flex; gap: 4px;">
          <button id="krc-tab-diurnal" class="tool-btn" style="padding: 2px 6px; font-size: 10px; background: #0284c7;">Diurnal</button>
          <button id="krc-tab-depth" class="tool-btn" style="padding: 2px 6px; font-size: 10px; background: #334155;">Depth</button>
          <button id="krc-tab-seasonal" class="tool-btn" style="padding: 2px 6px; font-size: 10px; background: #334155;">Seasonal</button>
        </div>
        <button id="krc-export-png-btn" class="tool-btn" style="padding: 2px 6px; font-size: 10px;">PNG</button>
      </div>
      <canvas id="krc-canvas" width="280" height="150" style="background: #111827; border: 1px solid #374151; border-radius: 4px; display: block; width: 100%; height: 150px;"></canvas>
    `;

    this.canvas = this.container.querySelector('#krc-canvas');
    this.ctx = this.canvas.getContext('2d');

    const diurnalTab = this.container.querySelector('#krc-tab-diurnal');
    const depthTab = this.container.querySelector('#krc-tab-depth');
    const seasonalTab = this.container.querySelector('#krc-tab-seasonal');
    const exportBtn = this.container.querySelector('#krc-export-png-btn');

    const setTab = (newMode, activeBtn) => {
      this.mode = newMode;
      [diurnalTab, depthTab, seasonalTab].forEach(b => b.style.background = '#334155');
      activeBtn.style.background = '#0284c7';
      this.draw();
    };

    diurnalTab.addEventListener('click', () => setTab('diurnal', diurnalTab));
    depthTab.addEventListener('click', () => setTab('depth', depthTab));
    seasonalTab.addEventListener('click', () => setTab('seasonal', seasonalTab));
    exportBtn.addEventListener('click', () => this.exportPNG());

    this.drawEmpty();
  }

  setResult(result, seasonalData = null) {
    this.lastResult = result;
    this.seasonalData = seasonalData;
    this.draw();
  }

  exportPNG() {
    if (!this.canvas) return;
    const link = document.createElement('a');
    link.download = `krc_thermal_${this.mode}.png`;
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
    this.ctx.fillText('Run simulation to plot temperatures', w / 2, h / 2);
  }

  draw() {
    if (!this.lastResult) {
      this.drawEmpty();
      return;
    }

    if (this.mode === 'diurnal') this.drawDiurnal();
    else if (this.mode === 'depth') this.drawDepth();
    else if (this.mode === 'seasonal') this.drawSeasonal();
  }

  drawDiurnal() {
    const ctx = this.ctx;
    const w = this.canvas.width;
    const h = this.canvas.height;
    const pad = 24;
    const padR = 10;
    const padT = 16;
    const padB = 22;

    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = '#111827';
    ctx.fillRect(0, 0, w, h);

    const curve = this.lastResult.diurnalCurve;
    if (!curve || curve.length === 0) return;

    let minT = Math.min(...curve.map(d => Math.min(d.surfaceTemp, d.subsurface10cm)));
    let maxT = Math.max(...curve.map(d => Math.max(d.surfaceTemp, d.subsurface10cm)));
    minT = Math.floor(minT - 5);
    maxT = Math.ceil(maxT + 5);

    const scaleX = (hr) => pad + (hr / 24) * (w - pad - padR);
    const scaleY = (temp) => h - padB - ((temp - minT) / (maxT - minT)) * (h - padT - padB);

    // Grid lines
    ctx.strokeStyle = '#1f2937';
    ctx.lineWidth = 1;
    for (let hr = 0; hr <= 24; hr += 6) {
      const x = scaleX(hr);
      ctx.beginPath();
      ctx.moveTo(x, padT);
      ctx.lineTo(x, h - padB);
      ctx.stroke();
      ctx.fillStyle = '#9ca3af';
      ctx.font = '9px sans-serif';
      ctx.textAlign = 'center';
      ctx.fillText(`${hr}h`, x, h - 8);
    }

    // Axes
    ctx.strokeStyle = '#4b5563';
    ctx.beginPath();
    ctx.moveTo(pad, padT);
    ctx.lineTo(pad, h - padB);
    ctx.lineTo(w - padR, h - padB);
    ctx.stroke();

    // CO2 Frost line if in range
    if (minT <= 145 && maxT >= 145) {
      const yFrost = scaleY(145);
      ctx.strokeStyle = '#38bdf8';
      ctx.setLineDash([3, 3]);
      ctx.beginPath();
      ctx.moveTo(pad, yFrost);
      ctx.lineTo(w - padR, yFrost);
      ctx.stroke();
      ctx.setLineDash([]);
      ctx.fillStyle = '#38bdf8';
      ctx.font = '8px sans-serif';
      ctx.textAlign = 'right';
      ctx.fillText('CO₂ frost (145K)', w - padR - 2, yFrost - 2);
    }

    // Subsurface 10cm (Cyan)
    ctx.strokeStyle = '#38bdf8';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    curve.forEach((d, i) => {
      const x = scaleX(d.hour);
      const y = scaleY(d.subsurface10cm);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // Surface Temp (Orange/Red)
    ctx.strokeStyle = '#f97316';
    ctx.lineWidth = 2.0;
    ctx.beginPath();
    curve.forEach((d, i) => {
      const x = scaleX(d.hour);
      const y = scaleY(d.surfaceTemp);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // Labels & Legend
    ctx.fillStyle = '#e5e7eb';
    ctx.font = '9px sans-serif';
    ctx.textAlign = 'right';
    ctx.fillText(`${maxT}K`, pad - 2, padT + 8);
    ctx.fillText(`${minT}K`, pad - 2, h - padB);

    // Legend
    ctx.textAlign = 'left';
    ctx.fillStyle = '#f97316';
    ctx.fillText('— T_surf', pad + 4, padT - 4);
    ctx.fillStyle = '#38bdf8';
    ctx.fillText('— T_10cm', pad + 60, padT - 4);
  }

  drawDepth() {
    const ctx = this.ctx;
    const w = this.canvas.width;
    const h = this.canvas.height;
    const pad = 28;
    const padR = 10;
    const padT = 16;
    const padB = 22;

    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = '#111827';
    ctx.fillRect(0, 0, w, h);

    const profile = this.lastResult.depthProfile;
    if (!profile || profile.length === 0) return;

    let minT = Math.min(...profile.map(p => p.temp));
    let maxT = Math.max(...profile.map(p => p.temp));
    minT = Math.floor(minT - 2);
    maxT = Math.ceil(maxT + 2);
    const maxDepth = Math.max(...profile.map(p => p.depthCm));

    const scaleX = (temp) => pad + ((temp - minT) / (maxT - minT || 1)) * (w - pad - padR);
    const scaleY = (depth) => padT + (depth / maxDepth) * (h - padT - padB);

    // Axes (Y is depth going downward)
    ctx.strokeStyle = '#4b5563';
    ctx.beginPath();
    ctx.moveTo(pad, padT);
    ctx.lineTo(pad, h - padB);
    ctx.lineTo(w - padR, h - padB);
    ctx.stroke();

    // Depth curve (Purple)
    ctx.strokeStyle = '#a855f7';
    ctx.lineWidth = 2;
    ctx.beginPath();
    profile.forEach((p, i) => {
      const x = scaleX(p.temp);
      const y = scaleY(p.depthCm);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // Points
    ctx.fillStyle = '#c084fc';
    profile.forEach(p => {
      const x = scaleX(p.temp);
      const y = scaleY(p.depthCm);
      ctx.beginPath();
      ctx.arc(x, y, 2.5, 0, 2 * Math.PI);
      ctx.fill();
    });

    // Labels
    ctx.fillStyle = '#9ca3af';
    ctx.font = '9px sans-serif';
    ctx.textAlign = 'right';
    ctx.fillText('0cm', pad - 2, padT + 8);
    ctx.fillText(`${Math.round(maxDepth)}cm`, pad - 2, h - padB);

    ctx.textAlign = 'center';
    ctx.fillText(`${minT}K`, pad, h - 8);
    ctx.fillText(`${maxT}K`, w - padR, h - 8);

    ctx.fillStyle = '#a855f7';
    ctx.textAlign = 'left';
    ctx.fillText('Subsurface Depth Profile (0-1m)', pad + 4, padT - 4);
  }

  drawSeasonal() {
    const ctx = this.ctx;
    const w = this.canvas.width;
    const h = this.canvas.height;
    const pad = 24;
    const padR = 10;
    const padT = 16;
    const padB = 22;

    ctx.clearRect(0, 0, w, h);
    ctx.fillStyle = '#111827';
    ctx.fillRect(0, 0, w, h);

    const data = this.seasonalData;
    if (!data || data.length === 0) {
      this.drawEmpty();
      return;
    }

    let minT = Math.min(...data.map(d => d.minTemp));
    let maxT = Math.max(...data.map(d => d.maxTemp));
    minT = Math.floor(minT - 5);
    maxT = Math.ceil(maxT + 5);

    const scaleX = (Ls) => pad + (Ls / 360) * (w - pad - padR);
    const scaleY = (t) => h - padB - ((t - minT) / (maxT - minT || 1)) * (h - padT - padB);

    // Axes
    ctx.strokeStyle = '#4b5563';
    ctx.beginPath();
    ctx.moveTo(pad, padT);
    ctx.lineTo(pad, h - padB);
    ctx.lineTo(w - padR, h - padB);
    ctx.stroke();

    // Max curve (Red)
    ctx.strokeStyle = '#ef4444';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    data.forEach((d, i) => {
      const x = scaleX(d.Ls);
      const y = scaleY(d.maxTemp);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // Mean curve (Yellow)
    ctx.strokeStyle = '#eab308';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    data.forEach((d, i) => {
      const x = scaleX(d.Ls);
      const y = scaleY(d.meanTemp);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // Min curve (Blue)
    ctx.strokeStyle = '#3b82f6';
    ctx.lineWidth = 1.5;
    ctx.beginPath();
    data.forEach((d, i) => {
      const x = scaleX(d.Ls);
      const y = scaleY(d.minTemp);
      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    });
    ctx.stroke();

    // Labels
    ctx.fillStyle = '#9ca3af';
    ctx.font = '9px sans-serif';
    ctx.textAlign = 'right';
    ctx.fillText(`${maxT}K`, pad - 2, padT + 8);
    ctx.fillText(`${minT}K`, pad - 2, h - padB);

    ctx.textAlign = 'center';
    ctx.fillText('0°', pad, h - 8);
    ctx.fillText('180°', pad + (w - pad - padR) / 2, h - 8);
    ctx.fillText('360° Ls', w - padR, h - 8);

    ctx.textAlign = 'left';
    ctx.fillStyle = '#ef4444';
    ctx.fillText('— Max', pad + 4, padT - 4);
    ctx.fillStyle = '#eab308';
    ctx.fillText('— Mean', pad + 45, padT - 4);
    ctx.fillStyle = '#3b82f6';
    ctx.fillText('— Min', pad + 90, padT - 4);
  }
}
