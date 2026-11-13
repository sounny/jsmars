import { EVENTS } from '../../constants.js';

/**
 * @module ProfileChart
 * @description Canvas-based elevation profile chart renderer.
 *
 * Listens for PROFILE_GENERATED events and draws one or more
 * elevation profiles onto a 2D canvas element.
 */
export class ProfileChart {
    /**
     * Create a ProfileChart.
     * @param {string} containerId - DOM element ID for the chart container.
     */
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.canvas = null;
        this.ctx = null;

        if (!this.container) {
            console.warn('ProfileChart container not found');
            return;
        }

        this.init();

        document.addEventListener(EVENTS.PROFILE_LOADING, () => {
            this.renderStatus('Sampling elevation...');
        });

        document.addEventListener(EVENTS.PROFILE_GENERATED, (e) => {
            this.draw(e.detail?.profiles, e.detail || {});
        });
    }

    /**
     * Build the initial chart UI (canvas, export button).
     */
    init() {
        this.container.innerHTML = '';
        
        // Controls
        const controls = document.createElement('div');
        controls.style.marginBottom = '5px';
        controls.style.textAlign = 'right';
        
        const exportBtn = document.createElement('button');
        exportBtn.className = 'tool-btn'; // Use existing style
        exportBtn.style.width = 'auto';
        exportBtn.style.padding = '2px 8px';
        exportBtn.style.fontSize = '11px';
        exportBtn.innerText = 'Export PNG';
        exportBtn.onclick = () => this.exportPNG();
        controls.appendChild(exportBtn);
        this.container.appendChild(controls);

        this.canvas = document.createElement('canvas');
        this.canvas.width = 284; // Fit cleanly inside 300px sidebar
        this.canvas.height = 164;
        this.canvas.style.width = '100%';
        this.canvas.style.display = 'block';
        this.canvas.style.background = '#222';
        this.canvas.style.border = '1px solid #444';
        this.canvas.style.borderRadius = '4px';
        this.container.appendChild(this.canvas);
        this.ctx = this.canvas.getContext('2d');

        // Interaction
        this.canvas.addEventListener('mousemove', (e) => this.onMouseMove(e));
        this.canvas.addEventListener('mouseleave', () => {
            this.draw(this.lastProfiles, this.lastMeta);
            document.dispatchEvent(new CustomEvent(EVENTS.PROFILE_HOVER, { detail: { clear: true } }));
        });

        // Initial text
        this.renderStatus('Draw a line on Mars to plot elevation');
    }

    /**
     * Render a centered status string on the chart canvas.
     * @param {string} msg
     */
    renderStatus(msg) {
        if (!this.ctx || !this.canvas) return;
        const w = this.canvas.width;
        const h = this.canvas.height;
        this.scales = null;
        this.ctx.clearRect(0, 0, w, h);
        this.ctx.fillStyle = '#222';
        this.ctx.fillRect(0, 0, w, h);
        this.ctx.fillStyle = '#94a3b8';
        this.ctx.font = '11px sans-serif';
        this.ctx.textAlign = 'center';
        this.ctx.fillText(msg, w / 2, h / 2 + 4);
    }

    /**
     * Export the current chart as a PNG image download.
     */
    exportPNG() {
        const link = document.createElement('a');
        link.download = 'profile_chart.png';
        link.href = this.canvas.toDataURL();
        link.click();
    }

    /**
     * Handle mouse-move on the canvas for interactive tooltips.
     * @param {MouseEvent} e - Native mouse event.
     */
    onMouseMove(e) {
        if (!this.lastProfiles || this.lastProfiles.length === 0) return;

        const rect = this.canvas.getBoundingClientRect();
        const scaleRatioX = this.canvas.width / Math.max(1, rect.width);
        const mouseX = (e.clientX - rect.left) * scaleRatioX;

        // Redraw base
        this.draw(this.lastProfiles, this.lastMeta);

        if (!this.scales) return;

        let nearest = null;
        let minDiff = Infinity;

        this.lastProfiles.forEach(p => {
            p.data.forEach(d => {
                const px = this.scales.x(d.dist);
                const diff = Math.abs(px - mouseX);
                if (diff < minDiff && diff < 12) {
                    minDiff = diff;
                    nearest = { ...d, color: p.color, px: px, py: this.scales.y(d.elev) };
                }
            });
        });

        if (nearest) {
            const w = this.canvas.width;
            const h = this.canvas.height;
            // Highlight
            this.ctx.beginPath();
            this.ctx.arc(nearest.px, nearest.py, 4, 0, 2 * Math.PI);
            this.ctx.fillStyle = '#fff';
            this.ctx.fill();
            this.ctx.strokeStyle = '#0f172a';
            this.ctx.lineWidth = 1.5;
            this.ctx.stroke();

            // Clamp Tooltip inside canvas bounds
            const boxW = 118;
            const boxH = 36;
            const boxX = nearest.px + 10 + boxW > w - 4 ? Math.max(4, nearest.px - boxW - 10) : nearest.px + 10;
            const boxY = Math.max(4, Math.min(h - boxH - 4, nearest.py - 28));
            const distLabel = nearest.dist >= 1000
                ? `${(nearest.dist / 1000).toFixed(2)} km`
                : `${Math.round(nearest.dist)} m`;

            this.ctx.fillStyle = 'rgba(2, 6, 23, 0.9)';
            this.ctx.fillRect(boxX, boxY, boxW, boxH);
            this.ctx.strokeStyle = '#38bdf8';
            this.ctx.lineWidth = 1;
            this.ctx.strokeRect(boxX, boxY, boxW, boxH);
            this.ctx.fillStyle = '#f8fafc';
            this.ctx.textAlign = 'left';
            this.ctx.font = '10px monospace';
            this.ctx.fillText(`Dist: ${distLabel}`, boxX + 6, boxY + 13);
            this.ctx.fillText(`Elev: ${Math.round(nearest.elev)} m`, boxX + 6, boxY + 27);

            if (Number.isFinite(nearest.lat) && Number.isFinite(nearest.lng)) {
                document.dispatchEvent(new CustomEvent(EVENTS.PROFILE_HOVER, {
                    detail: { lat: nearest.lat, lng: nearest.lng, dist: nearest.dist, elev: nearest.elev }
                }));
            }
        }
    }

    /**
     * Draw all profiles onto the canvas.
     * @param {Array<object>} profiles - Array of { color, data: [{dist, elev}] }.
     * @param {object} [meta] - Optional metadata ({ body, emptyMessage }).
     */
    draw(profiles, meta = {}) {
        const filteredProfiles = (profiles || [])
            .map(p => ({
                ...p,
                data: (p.data || []).filter(d => Number.isFinite(d.elev))
            }))
            .filter(p => p.data.length > 0);

        this.lastProfiles = filteredProfiles;
        this.lastMeta = meta || {};
        this.lastMetrics = null;
        const ctx = this.ctx;
        const w = this.canvas.width;
        const h = this.canvas.height;
        const padL = 48;
        const padR = 14;
        const padT = 22;
        const padB = 22;

        // Clear
        ctx.clearRect(0, 0, w, h);
        ctx.fillStyle = '#222';
        ctx.fillRect(0, 0, w, h);

        if (!filteredProfiles || filteredProfiles.length === 0) {
            this.renderStatus(meta?.emptyMessage || 'No profile data');
            return;
        }

        // Find unpadded data Min/Max
        let dataMinElev = Infinity;
        let dataMaxElev = -Infinity;
        let maxDist = 0;

        filteredProfiles.forEach(p => {
            p.data.forEach(d => {
                if (d.elev < dataMinElev) dataMinElev = d.elev;
                if (d.elev > dataMaxElev) dataMaxElev = d.elev;
                if (d.dist > maxDist) maxDist = d.dist;
            });
        });

        // Padded Y-axis bounds (keep relief computed from unpadded dataMinElev/dataMaxElev)
        const range = Math.max(dataMaxElev - dataMinElev, 1);
        const axisMinElev = dataMinElev - range * 0.1;
        const axisMaxElev = dataMaxElev + range * 0.1;

        if (maxDist === 0) {
            maxDist = 1;
        }

        // Scaling functions
        const scaleX = (d) => padL + (d / maxDist) * (w - padL - padR);
        const scaleY = (e) => h - padB - ((e - axisMinElev) / (axisMaxElev - axisMinElev)) * (h - padT - padB);

        this.scales = { x: scaleX, y: scaleY };

        // Draw Axes
        ctx.strokeStyle = '#64748b';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(padL, padT);
        ctx.lineTo(padL, h - padB); // Y axis
        ctx.lineTo(w - padR, h - padB); // X axis
        ctx.stroke();

        // Draw Profiles with terrain fill
        filteredProfiles.forEach(p => {
            if (p.data.length < 2) return;

            // Gradient terrain fill
            const grad = ctx.createLinearGradient(0, padT, 0, h - padB);
            grad.addColorStop(0, 'rgba(74, 222, 128, 0.35)');
            grad.addColorStop(1, 'rgba(74, 222, 128, 0.02)');

            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.moveTo(scaleX(p.data[0].dist), h - padB);
            p.data.forEach((d) => {
                ctx.lineTo(scaleX(d.dist), scaleY(d.elev));
            });
            ctx.lineTo(scaleX(p.data[p.data.length - 1].dist), h - padB);
            ctx.closePath();
            ctx.fill();

            // Profile stroke line
            ctx.strokeStyle = p.color || '#4ade80';
            ctx.lineWidth = 2;
            ctx.beginPath();
            p.data.forEach((d, i) => {
                const x = scaleX(d.dist);
                const y = scaleY(d.elev);
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            });
            ctx.stroke();
        });

        // Compute slope metrics from unpadded elevation extrema
        const relief = Math.round(dataMaxElev - dataMinElev);
        let rmsSlopeDeg = 0;
        let maxSlopeDeg = 0;
        if (filteredProfiles[0]?.data?.length > 1) {
            const data = filteredProfiles[0].data;
            let sumSqSlope = 0;
            for (let i = 1; i < data.length; i++) {
                const dx = data[i].dist - data[i - 1].dist;
                const dz = data[i].elev - data[i - 1].elev;
                if (dx > 0) {
                    const slope = Math.abs(dz / dx);
                    const slopeDeg = Math.atan(slope) * (180 / Math.PI);
                    if (slopeDeg > maxSlopeDeg) maxSlopeDeg = slopeDeg;
                    sumSqSlope += slope * slope;
                }
            }
            rmsSlopeDeg = Math.atan(Math.sqrt(sumSqSlope / (data.length - 1))) * (180 / Math.PI);

            ctx.fillStyle = '#cbd5e1';
            ctx.font = '10px monospace';
            ctx.textAlign = 'left';
            ctx.fillText(`Relief: ${relief}m | RMS: ${rmsSlopeDeg.toFixed(1)}°`, padL, 14);
        }

        this.lastMetrics = {
            reliefM: relief,
            minElevM: Math.round(dataMinElev),
            maxElevM: Math.round(dataMaxElev),
            lengthKm: maxDist / 1000,
            rmsSlopeDeg,
            maxSlopeDeg
        };

        // Unclipped Y-axis extrema & X-axis distance labels
        ctx.fillStyle = '#f8fafc';
        ctx.font = '9.5px monospace';
        ctx.textAlign = 'right';
        const maxYPos = Math.max(padT + 8, Math.min(h - padB - 12, scaleY(dataMaxElev) + 3));
        const minYPos = Math.max(maxYPos + 12, Math.min(h - padB, scaleY(dataMinElev) + 3));
        ctx.fillText(`${Math.round(dataMaxElev)}m`, padL - 5, maxYPos);
        ctx.fillText(`${Math.round(dataMinElev)}m`, padL - 5, minYPos);

        ctx.fillStyle = '#94a3b8';
        ctx.textAlign = 'left';
        ctx.fillText('0.0 km', padL, h - 6);
        ctx.textAlign = 'right';
        ctx.fillText(`${(maxDist / 1000).toFixed(1)} km`, w - padR, h - 6);
    }
}
