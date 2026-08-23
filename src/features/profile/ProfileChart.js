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

        document.addEventListener(EVENTS.PROFILE_GENERATED, (e) => {
            this.draw(e.detail.profiles);
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
        this.canvas.width = 280; // Fit in sidebar
        this.canvas.height = 150;
        this.canvas.style.background = '#222';
        this.canvas.style.border = '1px solid #444';
        this.container.appendChild(this.canvas);
        this.ctx = this.canvas.getContext('2d');

        // Interaction
        this.canvas.addEventListener('mousemove', (e) => this.onMouseMove(e));
        this.canvas.addEventListener('mouseleave', () => {
            this.draw(this.lastProfiles);
            document.dispatchEvent(new CustomEvent('jmars:profile-hover', { detail: { clear: true } }));
        });

        // Initial text
        this.ctx.fillStyle = '#666';
        this.ctx.font = '12px sans-serif';
        this.ctx.textAlign = 'center';
        this.ctx.fillText('No profile data', this.canvas.width / 2, this.canvas.height / 2);
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
        const mouseX = e.clientX - rect.left;
        
        // Redraw base
        this.draw(this.lastProfiles);

        // Find nearest point across all profiles
        let nearest = null;
        let minDiff = Infinity;

        // We need to reverse the scaleX to find data index from pixel X
        // scaleX = (d) => pad + (d / maxDist) * (w - 2 * pad);
        // d = (x - pad) / (w - 2 * pad) * maxDist
        
        // Easier: Just iterate all points and find closest in X pixels.
        // Since we already computed scales in draw(), we should store them or recompute.
        // Storing in `this` is easiest.
        
        if (!this.scales) return;

        this.lastProfiles.forEach(p => {
            p.data.forEach(d => {
                const px = this.scales.x(d.dist);
                const diff = Math.abs(px - mouseX);
                if (diff < minDiff && diff < 10) { // 10px threshold
                    minDiff = diff;
                    nearest = { ...d, color: p.color, px: px, py: this.scales.y(d.elev) };
                }
            });
        });

        if (nearest) {
            // Highlight
            this.ctx.beginPath();
            this.ctx.arc(nearest.px, nearest.py, 4, 0, 2 * Math.PI);
            this.ctx.fillStyle = '#fff';
            this.ctx.fill();
            this.ctx.stroke();

            // Tooltip
            this.ctx.fillStyle = 'rgba(0, 0, 0, 0.85)';
            this.ctx.fillRect(nearest.px + 10, nearest.py - 30, 120, 40);
            this.ctx.fillStyle = '#fff';
            this.ctx.textAlign = 'left';
            this.ctx.font = '10px monospace';
            this.ctx.fillText(`Dist: ${Math.round(nearest.dist)}m`, nearest.px + 15, nearest.py - 20);
            this.ctx.fillText(`Elev: ${Math.round(nearest.elev)}m`, nearest.px + 15, nearest.py - 8);

            if (Number.isFinite(nearest.lat) && Number.isFinite(nearest.lng)) {
                document.dispatchEvent(new CustomEvent('jmars:profile-hover', {
                    detail: { lat: nearest.lat, lng: nearest.lng, dist: nearest.dist, elev: nearest.elev }
                }));
            }
        }
    }

    /**
     * Draw all profiles onto the canvas.
     * @param {Array<object>} profiles - Array of { color, data: [{dist, elev}] }.
     */
    draw(profiles) {
        const filteredProfiles = (profiles || [])
            .map(p => ({
                ...p,
                data: (p.data || []).filter(d => Number.isFinite(d.elev))
            }))
            .filter(p => p.data.length > 0);

        this.lastProfiles = filteredProfiles; // Store for redraw
        const ctx = this.ctx;
        const w = this.canvas.width;
        const h = this.canvas.height;
        const pad = 20;

        // Clear
        ctx.clearRect(0, 0, w, h);
        ctx.fillStyle = '#222';
        ctx.fillRect(0, 0, w, h);

        if (!filteredProfiles || filteredProfiles.length === 0) {
            this.scales = null;
            ctx.fillStyle = '#666';
            ctx.font = '12px sans-serif';
            ctx.textAlign = 'center';
            ctx.fillText('No profile data', w / 2, h / 2);
            return;
        }

        // Find Min/Max
        let minElev = Infinity;
        let maxElev = -Infinity;
        let maxDist = 0;

        filteredProfiles.forEach(p => {
            p.data.forEach(d => {
                if (d.elev < minElev) minElev = d.elev;
                if (d.elev > maxElev) maxElev = d.elev;
                if (d.dist > maxDist) maxDist = d.dist;
            });
        });

        // Add padding to Y; enforce minimum range to avoid divide-by-zero on flat terrain
        const range = Math.max(maxElev - minElev, 1);
        minElev -= range * 0.1;
        maxElev += range * 0.1;

        if (maxDist === 0) {
            maxDist = 1;
        }

        // Scaling functions
        const scaleX = (d) => pad + (d / maxDist) * (w - 2 * pad);
        const scaleY = (e) => h - pad - ((e - minElev) / (maxElev - minElev)) * (h - 2 * pad);
        
        this.scales = { x: scaleX, y: scaleY }; // Store for interaction

        // Draw Axes
        ctx.strokeStyle = '#666';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(pad, pad);
        ctx.lineTo(pad, h - pad); // Y axis
        ctx.lineTo(w - pad, h - pad); // X axis
        ctx.stroke();

        // Draw Profiles with terrain fill
        filteredProfiles.forEach(p => {
            if (p.data.length < 2) return;

            // Gradient terrain fill
            const grad = ctx.createLinearGradient(0, pad, 0, h - pad);
            grad.addColorStop(0, 'rgba(74, 222, 128, 0.35)');
            grad.addColorStop(1, 'rgba(74, 222, 128, 0.02)');

            ctx.fillStyle = grad;
            ctx.beginPath();
            ctx.moveTo(scaleX(p.data[0].dist), h - pad);
            p.data.forEach((d) => {
                ctx.lineTo(scaleX(d.dist), scaleY(d.elev));
            });
            ctx.lineTo(scaleX(p.data[p.data.length - 1].dist), h - pad);
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

        // Compute slope metrics on first profile
        if (filteredProfiles[0]?.data?.length > 1) {
            const data = filteredProfiles[0].data;
            let sumSqSlope = 0;
            let maxSlopeDeg = 0;
            for (let i = 1; i < data.length; i++) {
                const dx = data[i].dist - data[i-1].dist;
                const dz = data[i].elev - data[i-1].elev;
                if (dx > 0) {
                    const slope = Math.abs(dz / dx);
                    const slopeDeg = Math.atan(slope) * (180 / Math.PI);
                    if (slopeDeg > maxSlopeDeg) maxSlopeDeg = slopeDeg;
                    sumSqSlope += slope * slope;
                }
            }
            const rmsSlopeDeg = Math.atan(Math.sqrt(sumSqSlope / (data.length - 1))) * (180 / Math.PI);
            const relief = Math.round(maxElev - minElev);

            ctx.fillStyle = '#aaa';
            ctx.font = '9px monospace';
            ctx.textAlign = 'left';
            ctx.fillText(`Relief: ${relief}m | RMS Slope: ${rmsSlopeDeg.toFixed(1)}°`, pad + 5, pad + 12);
        }

        // Labels
        ctx.fillStyle = '#fff';
        ctx.font = '10px sans-serif';
        ctx.textAlign = 'right';
        ctx.fillText(`${Math.round(maxElev)}m`, pad - 2, pad + 10);
        ctx.fillText(`${Math.round(minElev)}m`, pad - 2, h - pad);

        ctx.textAlign = 'center';
        ctx.fillText(`${(maxDist/1000).toFixed(1)} km`, w - pad, h - 5);
    }
}
