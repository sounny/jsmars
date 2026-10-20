#!/usr/bin/env python3
"""
Build the Lunar South Pole Site Planner data package for JSMARS.

Every value shipped comes from published NASA LOLA products; nothing is
synthesized. The browser cannot read these sources directly (no CORS on
pgda.gsfc.nasa.gov / imbrium.mit.edu), so this script downloads them once,
resamples them to compact grids, and writes same-origin GeoTIFFs plus a
manifest with full provenance (URLs, citations, resampling method).

Sources
  * Site DEMs + slope, 5 m/px  -- Barker et al. (2021), PGDA product 78
  * Avg solar visibility, Earth visibility, permanent shadow, 60 m/px
                                -- Mazarico et al. (2011), PGDA product 69
  * Regional LOLA DEM LDEM_85S_40M -- LRO LOLA GDR (PDS), imbrium.mit.edu

Requirements: numpy, tifffile (no GDAL).
Usage:  python scripts/build_lunar_south_pole_assets.py [--cache DIR] [--sites Site01,Site06]
"""
import argparse
import datetime
import hashlib
import json
import math
import os
import re
import sys
import urllib.request

import numpy as np
import tifffile

R_MOON = 1737400.0
OUT_DIR = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "assets", "lunar-south-pole")
UA = {"User-Agent": "Mozilla/5.0 (JSMARS data build; https://jsmars.sounny.com)"}

PGDA_SITE = "https://pgda.gsfc.nasa.gov/data/LOLA_5mpp/{s}/{s}_final_adj_5mpp_{k}.tif"
PGDA_ILLUM = "https://pgda.gsfc.nasa.gov/data/MoonIllumination/{p}.TIF"
LDEM_REGIONAL = "https://imbrium.mit.edu/DATA/LOLA_GDR/POLAR/IMG/LDEM_85S_40M"

# Site IDs and labels exactly as listed on https://pgda.gsfc.nasa.gov/products/78
SITES = {
    "Site01": "Connecting ridge",
    "Site04": "Shackleton rim",
    "Site06": "Nobile rim 1",
    "Site07": "Peak near Shackleton",
    "Site11": "de Gerlache rim",
    "Site23": "Malapert massif",
    "Site42": "de Gerlache-Kocher massif",
    "SL2": "de Gerlache rim (SL2)",
    "SL3": "Connecting ridge extension",
    "DM2": "Nobile rim 2",
}

CIT_BARKER = ("Barker, M.K., et al. (2021), Improved LOLA elevation maps for south pole landing sites: "
              "Error estimates and their impact on illumination conditions, Planet. Space Sci. 203, 105119, "
              "doi:10.1016/j.pss.2020.105119")
CIT_MAZARICO = ("Mazarico, E., et al. (2011), Illumination conditions of the lunar polar regions using LOLA "
                "topography, Icarus 211, 1066-1081, doi:10.1016/j.icarus.2010.10.030")
CIT_LOLA = "Smith, D.E., et al. (2010), LRO LOLA Gridded Data Records (LRO-L-LOLA-4-GDR-V1.0), NASA PDS"


# ---------------------------------------------------------------- download
def fetch(url, cache):
    os.makedirs(cache, exist_ok=True)
    path = os.path.join(cache, url.rsplit("/", 1)[1])
    if os.path.exists(path) and os.path.getsize(path) > 0:
        return path
    print("  downloading", url, flush=True)
    req = urllib.request.Request(url, headers=UA)
    with urllib.request.urlopen(req, timeout=600) as r:
        ctype = r.headers.get("Content-Type", "")
        if "text/html" in ctype:
            raise RuntimeError(f"{url} returned HTML, not data")
        tmp = path + ".part"
        with open(tmp, "wb") as f:
            while True:
                b = r.read(1 << 20)
                if not b:
                    break
                f.write(b)
    os.replace(tmp, path)
    return path


# ---------------------------------------------------------------- GeoTIFF IO
def read_geotiff(path):
    """Return (array float64 with NaN nodata, (xmin, ymax, res))."""
    with tifffile.TiffFile(path) as tf:
        page = tf.pages[0]
        a = page.asarray().astype(np.float64)
        tags = page.tags
        val = lambda k: (tags[k].value if k in tags else None)
        scale = val("ModelPixelScaleTag")
        tie = val("ModelTiepointTag")
        nod = val("GDAL_NODATA")
        gkd = val("GeoKeyDirectoryTag")
        meta = val("GDAL_METADATA")
    if nod is not None:
        try:
            nv = float(str(nod).strip("\x00 "))
            a[a == nv] = np.nan
        except ValueError:
            pass
    if meta is not None:  # apply band scale/offset (e.g. AVGVISIB scale 4e-5)
        m = str(meta)
        s = re.search(r'name="SCALE"[^>]*>([^<]+)<', m)
        o = re.search(r'name="OFFSET"[^>]*>([^<]+)<', m)
        if s:
            a = a * float(s.group(1)) + (float(o.group(1)) if o else 0.0)
    res = float(scale[0])
    if abs(float(scale[1]) - res) > 1e-6:
        raise ValueError("non-square pixels unsupported")
    x0 = float(tie[3]) - float(tie[0]) * res
    y0 = float(tie[4]) + float(tie[1]) * res
    raster_type = 1
    if isinstance(gkd, dict):
        raster_type = int(getattr(gkd.get("GTRasterTypeGeoKey", 1), "value", gkd.get("GTRasterTypeGeoKey", 1)))
    elif gkd is not None:
        v = list(gkd)
        for i in range(4, len(v), 4):
            if v[i] == 1025:
                raster_type = v[i + 3]
    if raster_type == 2:  # PixelIsPoint -> shift to corner
        x0 -= res / 2
        y0 += res / 2
    return a, (x0, y0, res)


def read_pds_img(base, cache):
    lbl = open(fetch(base + ".LBL", cache), encoding="latin-1").read()
    img = fetch(base + ".IMG", cache)

    def g(k):
        m = re.search(r"\b%s\s*=\s*\"?([-\w.+]+)" % k, lbl)
        return m.group(1) if m else None

    lines, samples = int(g("LINES")), int(g("LINE_SAMPLES"))
    bits, stype = int(g("SAMPLE_BITS")), g("SAMPLE_TYPE")
    dt = {16: "i2", 32: "f4"}[bits]
    dt = ("<" if "LSB" in stype or "PC_REAL" in stype else ">") + dt
    a = np.fromfile(img, dtype=dt, count=lines * samples).reshape(lines, samples).astype(np.float64)
    a *= float(g("SCALING_FACTOR") or 1)
    res = float(g("MAP_SCALE")) * (1000.0 if "KM" in re.search(r"MAP_SCALE\s*=\s*[\d.]+\s*<([^>]+)>", lbl).group(1).upper() else 1.0)
    l0, s0 = float(g("LINE_PROJECTION_OFFSET")), float(g("SAMPLE_PROJECTION_OFFSET"))
    # PDS: x = (sample - S0 - 1) * res ; y = (1 - line + L0) * res, pixel centres, 1-based
    x0 = (1 - s0 - 1) * res - res / 2
    y0 = (1 - 1 + l0) * res + res / 2
    return a, (x0, y0, res), lbl


def geokeys(citation):
    keys = [  # (key, tiffTagLocation, count, value)
        (1024, 0, 1, 1),        # GTModelType = Projected
        (1025, 0, 1, 1),        # GTRasterType = PixelIsArea
        (1026, 34737, None, 0), # GTCitation
        (2048, 0, 1, 32767),    # GeographicType user-defined
        (2050, 0, 1, 32767),    # GeogGeodeticDatum user-defined
        (2054, 0, 1, 9102),     # angular unit degree
        (2056, 0, 1, 32767),    # ellipsoid user-defined
        (2057, 34736, 1, 0),    # semi-major
        (2058, 34736, 1, 1),    # semi-minor
        (3072, 0, 1, 32767),    # ProjectedCSType user-defined
        (3074, 0, 1, 32767),    # Projection user-defined
        (3075, 0, 1, 15),       # CT_PolarStereographic
        (3076, 0, 1, 9001),     # linear unit metre
        (3081, 34736, 1, 2),    # NatOriginLat
        (3082, 34736, 1, 3),    # FalseEasting
        (3083, 34736, 1, 4),    # FalseNorthing
        (3092, 34736, 1, 5),    # ScaleAtNatOrigin
        (3095, 34736, 1, 6),    # StraightVertPoleLong
    ]
    ascii_ = citation + "|"
    doubles = [R_MOON, R_MOON, -90.0, 0.0, 0.0, 1.0, 0.0]
    d = [1, 1, 0, len(keys)]
    for k, loc, cnt, val in keys:
        if k == 1026:
            d += [k, loc, len(ascii_), 0]
        else:
            d += [k, loc, cnt, val]
    return d, doubles, ascii_


def write_geotiff(path, bands, geo, dtype, nodata, description):
    x0, y0, res = geo
    data = np.stack([b.astype(dtype) for b in bands]) if len(bands) > 1 else bands[0].astype(dtype)
    gkd, dbl, asc = geokeys("Moon2000 south polar stereographic, sphere R=1737400 m; " + description)
    extratags = [
        (33550, "d", 3, (res, res, 0.0), False),
        (33922, "d", 6, (0.0, 0.0, 0.0, x0, y0, 0.0), False),
        (34735, "H", len(gkd), gkd, False),
        (34736, "d", len(dbl), dbl, False),
        (34737, "s", 0, asc, False),
        (42113, "s", 0, str(nodata), False),
    ]
    tifffile.imwrite(path, data, photometric="minisblack",
                     planarconfig="separate" if len(bands) > 1 else None,
                     tile=(256, 256), compression="zlib", predictor=True,
                     extratags=extratags, metadata=None, description=description)


# ---------------------------------------------------------------- helpers
def block(a, f, how="mean"):
    h, w = (a.shape[0] // f) * f, (a.shape[1] // f) * f
    v = a[:h, :w].reshape(h // f, f, w // f, f)
    with np.errstate(all="ignore"):
        if how == "max":
            return np.nanmax(v, axis=(1, 3))
        return np.nanmean(v, axis=(1, 3))


def xy_to_latlon(x, y):
    rho = math.hypot(x, y)
    lat = -(90.0 - math.degrees(2 * math.atan(rho / (2 * R_MOON))))
    lon = math.degrees(math.atan2(x, y))
    return lat, lon


def crop(a, geo, bounds):
    x0, y0, res = geo
    xmin, ymin, xmax, ymax = bounds
    c0, c1 = int(math.floor((xmin - x0) / res)), int(math.ceil((xmax - x0) / res))
    r0, r1 = int(math.floor((y0 - ymax) / res)), int(math.ceil((y0 - ymin) / res))
    c0, r0 = max(c0, 0), max(r0, 0)
    c1, r1 = min(c1, a.shape[1]), min(r1, a.shape[0])
    return a[r0:r1, c0:c1], (x0 + c0 * res, y0 - r0 * res, res)


def sha(path):
    h = hashlib.sha256()
    with open(path, "rb") as f:
        for b in iter(lambda: f.read(1 << 20), b""):
            h.update(b)
    return h.hexdigest()


def enc_pct(a):  # fraction 0..1 -> uint8 half-percent steps, 255 nodata
    o = np.full(a.shape, 255, np.uint8)
    ok = np.isfinite(a)
    o[ok] = np.clip(np.round(a[ok] * 200.0), 0, 200).astype(np.uint8)
    return o


# ---------------------------------------------------------------- main
def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--cache", default=os.path.join(os.path.expanduser("~"), ".cache", "jsmars-lunar"))
    ap.add_argument("--sites", default=",".join(SITES))
    args = ap.parse_args()
    os.makedirs(OUT_DIR, exist_ok=True)
    built = datetime.datetime.now(datetime.timezone.utc).isoformat(timespec="seconds")

    # --- illumination (60 m, 85S-90S)
    illum = {}
    for key, prod in [("sun", "AVGVISIB_85S_060M_201608"), ("earth", "AVGVISIB_85S_060M_201608_EARTH"),
                      ("psr", "LPSR_85S_060M_201608")]:
        print("illumination:", prod, flush=True)
        a, geo = read_geotiff(fetch(PGDA_ILLUM.format(p=prod), args.cache))
        illum[key] = (a, geo, PGDA_ILLUM.format(p=prod))
        print("   shape", a.shape, "geo", geo, "range", np.nanmin(a), np.nanmax(a), flush=True)
    sun60, g60 = illum["sun"][0], illum["sun"][1]
    for k in ("earth", "psr"):
        if illum[k][0].shape != sun60.shape or illum[k][1] != g60:
            raise RuntimeError("illumination grids differ")
    psr_bin = np.where(np.isfinite(illum["psr"][0]), (illum["psr"][0] > 0).astype(np.float64), np.nan)

    # --- regional 120 m package
    print("regional DEM LDEM_85S_40M", flush=True)
    dem40, g40, _ = read_pds_img(LDEM_REGIONAL, args.cache)
    print("   shape", dem40.shape, "geo", g40, "range", np.nanmin(dem40), np.nanmax(dem40), flush=True)
    dem120 = block(dem40, 3)
    g120 = (g40[0], g40[1], g40[2] * 3)
    reg_dem_path = os.path.join(OUT_DIR, "regional_dem_120m.tif")
    write_geotiff(reg_dem_path, [np.round(dem120)], g120, np.int16, -32768,
                  "LOLA LDEM_85S_40M heights (m, rel. 1737.4 km sphere), 3x3 block mean to 120 m")
    sun120, earth120 = block(sun60, 2), block(illum["earth"][0], 2)
    psr120 = block(psr_bin, 2)
    g120i = (g60[0], g60[1], g60[2] * 2)
    reg_ill_path = os.path.join(OUT_DIR, "regional_illumination_120m.tif")
    write_geotiff(reg_ill_path, [enc_pct(sun120), enc_pct(earth120), enc_pct(psr120)], g120i, np.uint8, 255,
                  "bands: avg solar visibility, avg Earth visibility, PSR fraction; uint8 = fraction*200; 2x2 block mean of 60 m")

    manifest = {
        "schema": 1,
        "built": built,
        "projection": {"name": "Moon2000 south polar stereographic", "radius_m": R_MOON,
                       "lat0": -90, "lon0": 0, "formula": "x = rho*sin(lon), y = rho*cos(lon), rho = 2R*tan(pi/4 + lat/2)"},
        "encoding": {"uint8_fraction": "value/200 = fraction (0..1); 255 = nodata"},
        "regional": {
            "dem": {"file": "regional_dem_120m.tif", "geo": g120, "shape": list(dem120.shape),
                    "units": "m", "nodata": -32768, "sha256": sha(reg_dem_path),
                    "source": LDEM_REGIONAL + ".IMG", "citation": CIT_LOLA,
                    "method": "3x3 block mean of 40 m LDEM, rounded to 1 m"},
            "illumination": {"file": "regional_illumination_120m.tif", "geo": g120i, "shape": list(sun120.shape),
                             "bands": ["sun", "earth", "psr"], "sha256": sha(reg_ill_path),
                             "sources": [illum[k][2] for k in ("sun", "earth", "psr")],
                             "citation": CIT_MAZARICO,
                             "method": "2x2 block mean of 60 m products; averages over 18.6-yr nutation cycle, hourly steps"},
        },
        "sites": [],
        "limitations": [
            "Longest continuous dark period is not a published LOLA raster product and is not included.",
            "Boulder density is not included; the planner offers a DEM-derived roughness proxy only.",
            "Illumination/Earth visibility are long-term averages at the surface (Mazarico et al. 2011), not at mast height.",
        ],
    }

    for sid in [s for s in args.sites.split(",") if s]:
        print("site", sid, SITES.get(sid, sid), flush=True)
        try:
            z5, gz = read_geotiff(fetch(PGDA_SITE.format(s=sid, k="surf"), args.cache))
            s5, gs = read_geotiff(fetch(PGDA_SITE.format(s=sid, k="slp"), args.cache))
        except Exception as e:  # keep building other sites, record failure
            print("   FAILED", e, flush=True)
            continue
        if z5.shape != s5.shape or gz != gs:
            raise RuntimeError(f"{sid}: DEM/slope grid mismatch")
        z20, smax, smean = block(z5, 4), block(s5, 4, "max"), block(s5, 4)
        g20 = (gz[0], gz[1], gz[2] * 4)
        h, w = z20.shape
        bounds = (g20[0], g20[1] - h * g20[2], g20[0] + w * g20[2], g20[1])
        tpath = os.path.join(OUT_DIR, f"{sid}_terrain_20m.tif")
        def i32(a, mult):
            o = np.full(a.shape, -2147483648, np.int32)
            ok = np.isfinite(a)
            o[ok] = np.round(a[ok] * mult).astype(np.int32)
            return o
        write_geotiff(tpath, [i32(z20, 100), i32(smax, 100), i32(smean, 100)], g20, np.int32, -2147483648,
                      f"PGDA {sid} ({SITES.get(sid, sid)}): int32 bands elevation cm, max slope 0.01 deg, mean slope 0.01 deg; 4x4 blocks of 5 m")
        ic = [crop(a, g60, bounds) for a in (sun60, illum["earth"][0], psr_bin)]
        ipath = os.path.join(OUT_DIR, f"{sid}_illumination_60m.tif")
        write_geotiff(ipath, [enc_pct(c[0]) for c in ic], ic[0][1], np.uint8, 255,
                      f"{sid} crop of Mazarico 60 m products: sun, earth, psr")
        cx, cy = (bounds[0] + bounds[2]) / 2, (bounds[1] + bounds[3]) / 2
        lat, lon = xy_to_latlon(cx, cy)
        manifest["sites"].append({
            "id": sid, "name": SITES.get(sid, sid), "bounds": bounds,
            "center": {"x": cx, "y": cy, "lat": round(lat, 4), "lon": round(lon, 4)},
            "terrain": {"file": os.path.basename(tpath), "geo": g20, "shape": [h, w],
                        "bands": ["elevation_m", "slope_max_deg", "slope_mean_deg"], "scale": 0.01,
                        "nodata": -2147483648, "sha256": sha(tpath),
                        "sources": [PGDA_SITE.format(s=sid, k="surf"), PGDA_SITE.format(s=sid, k="slp")],
                        "citation": CIT_BARKER,
                        "method": "4x4 block mean (elevation, mean slope) and block max (slope) of 5 m products"},
            "illumination": {"file": os.path.basename(ipath), "geo": ic[0][1], "shape": list(ic[0][0].shape),
                             "bands": ["sun", "earth", "psr"], "sha256": sha(ipath), "citation": CIT_MAZARICO},
        })
        print(f"   {w}x{h} @20m centre {lat:.3f},{lon:.3f}", flush=True)

    with open(os.path.join(OUT_DIR, "manifest.json"), "w", encoding="utf8") as f:
        json.dump(manifest, f, indent=1)
    print("done ->", os.path.abspath(OUT_DIR))


if __name__ == "__main__":
    sys.exit(main())
