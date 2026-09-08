import os
import numpy as np
from PIL import Image

def hex_to_rgb(h):
    h = h.lstrip('#')
    return np.array([int(h[i:i+2], 16) for i in (0, 2, 4)], dtype=np.float32)

def generate_three_circles_icon(target_size, bg_color='#0f172a'):
    scale = 4
    w = target_size * scale
    h = target_size * scale
    
    total_w_ratio = 340.0 / 512.0
    r_ratio = total_w_ratio * 15.0 / 70.0
    d_ratio = total_w_ratio * 20.0 / 70.0
    
    R = r_ratio * w
    D = d_ratio * w
    cy = h / 2.0
    cx_mid = w / 2.0
    
    cx_earth = cx_mid - D
    cx_moon = cx_mid
    cx_mars = cx_mid + D
    
    bg_rgb = hex_to_rgb(bg_color)
    img_rgb = np.zeros((h, w, 3), dtype=np.float32)
    img_rgb[:, :] = bg_rgb
    
    y_coords, x_coords = np.mgrid[0:h, 0:w]
    
    circles = [
        (cx_earth, cy, R, '#4facfe', '#00f2fe', 0.8),
        (cx_moon,  cy, R, '#bdc3c7', '#2c3e50', 0.8),
        (cx_mars,  cy, R, '#ff5f6d', '#ffc371', 0.8)
    ]
    
    for cx, c_y, radius, start_hex, end_hex, opac in circles:
        dist = np.sqrt((x_coords - cx)**2 + (y_coords - c_y)**2)
        edge_width = 1.0 * scale
        mask = np.clip((radius + 0.5 * edge_width - dist) / edge_width, 0.0, 1.0)
        
        x_min = cx - radius
        x_max = cx + radius
        grad_t = np.clip((x_coords - x_min) / (x_max - x_min), 0.0, 1.0)
        
        c_start = hex_to_rgb(start_hex)
        c_end = hex_to_rgb(end_hex)
        grad_rgb = c_start[None, None, :] * (1.0 - grad_t[:, :, None]) + c_end[None, None, :] * grad_t[:, :, None]
        
        src_a = mask * opac
        img_rgb = img_rgb * (1.0 - src_a[:, :, None]) + grad_rgb * src_a[:, :, None]
        
    img_final = np.clip(img_rgb, 0, 255).astype(np.uint8)
    img = Image.fromarray(img_final, mode='RGB')
    
    return img.resize((target_size, target_size), Image.Resampling.LANCZOS)

def main():
    os.makedirs('assets/icons', exist_ok=True)
    targets = {
        'assets/icons/icon-512.png': 512,
        'assets/icons/icon-192.png': 192,
        'assets/icons/icon-maskable-512.png': 512,
        'assets/icons/icon-maskable-192.png': 192,
        'assets/icons/apple-touch-icon.png': 180,
    }

    for path, sz in targets.items():
        im = generate_three_circles_icon(sz)
        im.save(path)
        print(f'Saved {path} ({sz}x{sz})')

    svg_content = """<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
  <defs>
    <linearGradient id="marsGrad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" style="stop-color:#ff5f6d;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#ffc371;stop-opacity:1" />
    </linearGradient>
    <linearGradient id="earthGrad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" style="stop-color:#4facfe;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#00f2fe;stop-opacity:1" />
    </linearGradient>
    <linearGradient id="moonGrad" x1="0%" y1="0%" x2="100%" y2="0%">
      <stop offset="0%" style="stop-color:#bdc3c7;stop-opacity:1" />
      <stop offset="100%" style="stop-color:#2c3e50;stop-opacity:1" />
    </linearGradient>
  </defs>
  <!-- Background -->
  <rect width="512" height="512" fill="#0f172a" />
  <!-- Earth -->
  <circle cx="158.86" cy="256" r="72.86" fill="url(#earthGrad)" opacity="0.8" />
  <!-- Moon -->
  <circle cx="256" cy="256" r="72.86" fill="url(#moonGrad)" opacity="0.8" />
  <!-- Mars -->
  <circle cx="353.14" cy="256" r="72.86" fill="url(#marsGrad)" opacity="0.8" />
</svg>
"""
    with open('assets/icons/icon.svg', 'w', encoding='utf-8') as f:
        f.write(svg_content)
    print('Saved assets/icons/icon.svg')

if __name__ == '__main__':
    main()
