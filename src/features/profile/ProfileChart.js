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
        this.canvas = document.createElement('canvas');
        this.canvas.width = 280; // Fit in sidebar
        this.canvas.height = 150;
        this.canvas.style.background = '#222';
        this.canvas.style.border = '1px solid #444';
        this.container.appendChild(this.canvas);
        this.ctx = this.canvas.getContext('2d');

        // Initial text
        this.ctx.fillStyle = '#666';
        this.ctx.font = '12px sans-serif';
        this.ctx.textAlign = 'center';
        this.ctx.fillText('No profile data', this.canvas.width / 2, this.canvas.height / 2);
    }

    draw(profiles) {
        const ctx = this.ctx;
        const w = this.canvas.width;
        const h = this.canvas.height;
        const pad = 20;

        // Clear
        ctx.clearRect(0, 0, w, h);
        ctx.fillStyle = '#222';
        ctx.fillRect(0, 0, w, h);

        if (!profiles || profiles.length === 0) return;

        // Find Min/Max
        let minElev = Infinity;
        let maxElev = -Infinity;
        let maxDist = 0;

        profiles.forEach(p => {
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

        // Scaling functions
        const scaleX = (d) => pad + (d / maxDist) * (w - 2 * pad);
        const scaleY = (e) => h - pad - ((e - minElev) / (maxElev - minElev)) * (h - 2 * pad);

        // Draw Axes
        ctx.strokeStyle = '#666';
        ctx.lineWidth = 1;
        ctx.beginPath();
        ctx.moveTo(pad, pad);
        ctx.lineTo(pad, h - pad); // Y axis
        ctx.lineTo(w - pad, h - pad); // X axis
        ctx.stroke();

        // Draw Profiles
        profiles.forEach(p => {
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
