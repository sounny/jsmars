/**
 * @module RadarChart
 * @description Renders interactive Mars subsurface radargrams and A-scope echo power curves.
 */

export class RadarChart {
  /**
   * @param {HTMLElement|string} containerOrId
   */
  constructor(containerOrId) {
    this.container = typeof containerOrId === 'string'
      ? document.getElementById(containerOrId)
      : containerOrId;
    this.viewMode = '2d'; // '2d' (B-scan radargram) or '1d' (A-scope power curve)
    this.data = null;

    if (this.container) {
      this.init();
    }
  }

  init() {
    this.container.innerHTML = `
      <div class="radar-chart-wrap" style="display:flex; flex-direction:column; gap:6px; background:#0b1329; border:1px solid #1e293b; border-radius:4px; padding:6px;">
        <div style="display:flex; justify-content:space-between; align-items:center;">
          <span style="font-size:11px; font-weight:bold; color:#38bdf8;">SHARAD / MARSIS Radargram</span>
          <div style="display:flex; gap:4px;">
            <button id="radar-mode-2d" class="crater-action-btn" style="background:#0284c7; padding:2px 6px; font-size:10px;">2D Radargram</button>
            <button id="radar-mode-1d" class="crater-action-btn" style="background:#334155; padding:2px 6px; font-size:10px;">1D Echo Trace</button>
          </div>
        </div>
        <canvas id="radar-canvas" width="300" height="180" style="width:100%; height:180px; background:#000; border-radius:3px; display:block;"></canvas>
        <div id="radar-legend" style="display:flex; justify-content:space-between; font-size:10px; color:#94a3b8;">
          <span>Depth: 0 m (Surface)</span>
          <span id="radar-max-depth">Max Depth: 2500 m</span>
        </div>
      </div>
    `;

    this.canvas = this.container.querySelector('#radar-canvas');
    this.ctx = this.canvas.getContext('2d');
    this.maxDepthEl = this.container.querySelector('#radar-max-depth');

    this.btn2d = this.container.querySelector('#radar-mode-2d');
    this.btn1d = this.container.querySelector('#radar-mode-1d');

    this.btn2d.addEventListener('click', () => {
      this.viewMode = '2d';
      this.btn2d.style.background = '#0284c7';
      this.btn1d.style.background = '#334155';
      this.render();
    });

    this.btn1d.addEventListener('click', () => {
      this.viewMode = '1d';
      this.btn1d.style.background = '#0284c7';
      this.btn2d.style.background = '#334155';
      this.render();
    });
  }

  setData(data) {
    this.data = data;
    if (data?.depths?.length > 0) {
      const maxZ = Math.round(data.depths[data.depths.length - 1]);
      if (this.maxDepthEl) {
        this.maxDepthEl.textContent = `Max Depth: ${maxZ} m`;
      }
    }
    this.render();
  }

  render() {
    if (!this.data || !this.ctx) return;

    if (this.viewMode === '2d') {
      this._render2dRadargram();
    } else {
      this._render1dTrace();
    }
  }

  _render2dRadargram() {
    const ctx = this.ctx;
    const w = this.canvas.width;
    const h = this.canvas.height;
    ctx.clearRect(0, 0, w, h);

    const grid = this.data.grid;
    if (!grid || grid.length === 0) return;

    const numCols = grid.length;
    const numRows = grid[0].length;
    const colWidth = w / numCols;
    const rowHeight = h / numRows;

    // Draw radargram pixel heat map
    for (let c = 0; c < numCols; c++) {
      for (let r = 0; r < numRows; r++) {
        const valDb = grid[c][r]; // range roughly -85 to -10 dB
        // Normalize to 0..1
        const norm = Math.max(0, Math.min(1, (valDb + 85) / 75));

        // Grayscale / high contrast radargram colormap
        const intensity = Math.round(Math.pow(norm, 1.8) * 255);
        ctx.fillStyle = `rgb(${intensity}, ${intensity}, ${intensity})`;
        ctx.fillRect(c * colWidth, r * rowHeight, Math.ceil(colWidth), Math.ceil(rowHeight));
      }
    }

    // Draw picked horizons overlay
    if (this.data.horizons) {
      this.data.horizons.forEach((hz, idx) => {
        const rowIdx = this.data.twt.findIndex(t => t >= hz.twt);
        if (rowIdx >= 0) {
          const y = (rowIdx / numRows) * h;
          ctx.strokeStyle = idx === 0 ? '#38bdf8' : (idx === this.data.horizons.length - 1 ? '#f43f5e' : '#fbbf24');
          ctx.lineWidth = 1.5;
          ctx.setLineDash([3, 3]);
          ctx.beginPath();
          ctx.moveTo(0, y);
          ctx.lineTo(w, y);
          ctx.stroke();
          ctx.setLineDash([]);

          // Label
          ctx.fillStyle = ctx.strokeStyle;
          ctx.font = '9px sans-serif';
          ctx.fillText(hz.name, 6, Math.max(y - 3, 10));
        }
      });
    }
  }

  _render1dTrace() {
    const ctx = this.ctx;
    const w = this.canvas.width;
    const h = this.canvas.height;
    ctx.fillStyle = '#050a18';
    ctx.fillRect(0, 0, w, h);

    const twt = this.data.twt;
    const depths = this.data.depths;
    const trace = this.data.grid ? this.data.grid[0] : (this.data.powerDb || []);

    if (!trace || trace.length === 0) return;

    // Grid lines
    ctx.strokeStyle = '#1e293b';
    ctx.lineWidth = 1;
    for (let y = 30; y < h; y += 30) {
      ctx.beginPath();
      ctx.moveTo(0, y);
      ctx.lineTo(w, y);
      ctx.stroke();
    }

    // Plot power (X) vs Depth/TWT (Y)
    ctx.strokeStyle = '#38bdf8';
    ctx.lineWidth = 2;
    ctx.beginPath();

    const minDb = -85;
    const maxDb = -10;

    for (let i = 0; i < trace.length; i++) {
      const db = trace[i];
      const normX = Math.max(0, Math.min(1, (db - minDb) / (maxDb - minDb)));
      const x = 30 + normX * (w - 40);
      const y = (i / (trace.length - 1)) * (h - 20) + 10;

      if (i === 0) ctx.moveTo(x, y);
      else ctx.lineTo(x, y);
    }
    ctx.stroke();

    // Axis labels
    ctx.fillStyle = '#94a3b8';
    ctx.font = '9px monospace';
    ctx.fillText('-80 dB', 30, h - 4);
    ctx.fillText('-10 dB', w - 40, h - 4);
  }
}
