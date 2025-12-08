export class ProfileChart {
    constructor(containerId) {
        this.container = document.getElementById(containerId);
        this.canvas = null;
        this.ctx = null;

        if (!this.container) {
            console.warn('ProfileChart container not found');
            return;
        }

        this.init();

        document.addEventListener('jmars-profile-generated', (e) => {
            this.draw(e.detail.profiles);
        });
    }

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
        this.canvas.addEventListener('mouseleave', () => this.draw(this.lastProfiles)); // Redraw clean

        // Initial text
        this.ctx.fillStyle = '#666';
        this.ctx.font = '12px sans-serif';
        this.ctx.textAlign = 'center';
        this.ctx.fillText('No profile data', this.canvas.width / 2, this.canvas.height / 2);
    }

    exportPNG() {
        const link = document.createElement('a');
        link.download = 'profile_chart.png';
        link.href = this.canvas.toDataURL();
        link.click();
    }

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
            this.ctx.fillStyle = 'rgba(0, 0, 0, 0.8)';
            this.ctx.fillRect(nearest.px + 10, nearest.py - 30, 120, 40);
            this.ctx.fillStyle = '#fff';
            this.ctx.textAlign = 'left';
            this.ctx.font = '10px monospace';
            this.ctx.fillText(`Dist: ${Math.round(nearest.dist)}m`, nearest.px + 15, nearest.py - 20);
            this.ctx.fillText(`Elev: ${Math.round(nearest.elev)}m`, nearest.px + 15, nearest.py - 8);
        }
    }

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

        // Add padding to Y
        const range = maxElev - minElev;
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

        // Draw Profiles
        filteredProfiles.forEach(p => {
            ctx.strokeStyle = p.color;
            ctx.lineWidth = 1.5;
            ctx.beginPath();
            p.data.forEach((d, i) => {
                const x = scaleX(d.dist);
                const y = scaleY(d.elev);
                if (i === 0) ctx.moveTo(x, y);
                else ctx.lineTo(x, y);
            });
            ctx.stroke();
        });

        // Labels
        ctx.fillStyle = '#fff';
        ctx.font = '10px sans-serif';
        ctx.textAlign = 'right';
        ctx.fillText(Math.round(maxElev), pad - 2, pad + 10);
        ctx.fillText(Math.round(minElev), pad - 2, h - pad);

        ctx.textAlign = 'center';
        ctx.fillText(`${(maxDist/1000).toFixed(0)} km`, w - pad, h - 5);
    }
}
