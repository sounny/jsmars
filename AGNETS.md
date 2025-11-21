# JMARS JS Port – Agent Briefing

This file tells you what this project is about and how you should help.

---

## 1. Project Overview

- **Project name:** JMARS JS Port  
- **Owner:** Dr. Moulay Anwar Sounny‑Slitine  
- **Upstream project:** JMARS (Java Mars GIS viewer) from Arizona State University  
- **Goal:** Build a JavaScript, browser‑based client that follows JMARS ideas and uses the same or similar map services.

JMARS is a large desktop GIS tool written in Java Swing. It talks to map servers (often WMS), manages many layers, supports shapes, and has 3D views.  
We do not want to copy the desktop UI. We want a clean WebGIS client that feels lighter but still supports JMARS data and workflows.

You can find the svn repo for JMARS here - https://oss.mars.asu.edu/svn/jmars/

---

## 2. High‑Level Goals

The JS project should:

1. **Use JMARS (or similar) map services**  
   - Use WMS or other HTTP APIs used by JMARS.  
   - Fetch capabilities, list layers, and request map images.

2. **Provide a web map viewer**  
   - Run fully in the browser.  
   - Show a Mars base map.  
   - Allow basic map interaction: pan, zoom, layer on/off.

3. **Mirror JMARS concepts where it helps**  
   - Keep similar ideas to `MapServer`, `MapSource`, and “Layer”.  
   - Keep a clean internal model for bodies (Mars first, others later).  

4. **Stay license‑compatible**  
   - JMARS is GPLv3.  
   - Any direct port or close derivative must also follow GPLv3.  
   - Keep that in mind for file headers and repo docs.

Short term focus: a simple Mars WMS viewer in JS that can grow into a fuller client.

---

## 3. Non‑Goals (for now)

The project will NOT try to match every JMARS feature at once.

We will not target in the first phase:

- Full Swing UI feature parity.  
- All layer types from the Java client.  
- Full 3D visualization and JOGL‑style rendering.  
- Local session persistence with the same logic as the desktop client.  

Those features can come later. The early work should focus on a strong, simple core that is easy to grow.

---

## 4. Architecture Plan

### 4.1. Overall shape

Target stack:

- **Client:** JavaScript ES modules  
- **Map library:** Leaflet as a first step (can switch to OpenLayers or MapLibre later)  
- **Data source:** WMS (or similar) map services used by JMARS  
- **Build setup:** Start with “no build” (plain HTML + JS). Add bundler later if needed.

### 4.2. Planned folder layout

This is the desired layout. If the repo does not match this layout yet, move toward it.

```text
/agents.md           # This file
/public/
  index.html         # Entry HTML page for the web client
/src/
  jmars-config.js    # Config: server URLs, default layer, body info
  jmars-wms.js       # WMS helper: capabilities + GetMap URL builder
  jmars-map.js       # Map view logic (Leaflet or other library)
  layers/
    index.js         # Layer model and layer registry
  util/
    geo.js           # Small geo helpers mirrored from JMARS Util
