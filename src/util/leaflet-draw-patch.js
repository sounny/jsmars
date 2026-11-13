/**
 * @module leaflet-draw-patch
 * @description Compatibility & UX fixes for Leaflet 1.9.x + Leaflet.draw 1.0.4:
 * 1. Fixes `L.Draw.Polygon` so clicking EITHER the first vertex OR the last vertex
 *    (with >= 3 vertices) closes and finishes the polygon cleanly on single click.
 * 2. Expands vertex finish snap radius to 18 CSS px across both mouse and hybrid-touch
 *    devices (covering the 20x20 `touchIcon` square on Windows touch/trackpad laptops)
 *    so clicking near a vertex edge finishes the shape instead of stacking duplicate vertices.
 * 3. Fixes `L.Draw.Polyline._endPoint` minified `devicePixelRatio` reference and allows
 *    closing a 3+ vertex polyline loop by clicking the starting vertex.
 * 4. Fixes `L.GeometryUtil.readableArea` undeclared `type` global assignment in strict contexts.
 */

const FINISH_SNAP_PX = 18;

/**
 * Apply Leaflet.draw 1.0.4 + Leaflet 1.9.x compatibility patches idempotently.
 * @param {object} [LRef=globalThis.L]
 * @returns {boolean} True if patches were applied or already active.
 */
export function applyLeafletDrawPatch(LRef = globalThis.L) {
  if (!LRef || !LRef.Draw || !LRef.Draw.Polyline || !LRef.Draw.Polygon) {
    return false;
  }
  if (LRef.Draw.__jsmarsPatched) {
    return true;
  }

  // 1. Fix L.GeometryUtil.readableArea undeclared `type` identifier
  if (LRef.GeometryUtil && typeof LRef.GeometryUtil.readableArea === 'function') {
    const defaultPrecision = { km: 2, ha: 2, m: 0, mi: 2, ac: 2, yd: 0, ft: 0, nm: 2 };
    LRef.GeometryUtil.readableArea = function readableAreaPatched(area, isMetric, precision) {
      const prec = LRef.Util.extend({}, defaultPrecision, precision);
      let areaStr;
      if (isMetric) {
        let units = ['ha', 'm'];
        const metricType = typeof isMetric;
        if (metricType === 'string') {
          units = [isMetric];
        } else if (metricType !== 'boolean') {
          units = isMetric;
        }
        if (area >= 1e6 && units.indexOf('km') !== -1) {
          areaStr = `${LRef.GeometryUtil.formattedNumber(area * 1e-6, prec.km)} km²`;
        } else if (area >= 1e4 && units.indexOf('ha') !== -1) {
          areaStr = `${LRef.GeometryUtil.formattedNumber(area * 1e-4, prec.ha)} ha`;
        } else {
          areaStr = `${LRef.GeometryUtil.formattedNumber(area, prec.m)} m²`;
        }
      } else {
        const sqYards = area / 0.836127;
        if (sqYards >= 3097600) {
          areaStr = `${LRef.GeometryUtil.formattedNumber(sqYards / 3097600, prec.mi)} mi²`;
        } else if (sqYards >= 4840) {
          areaStr = `${LRef.GeometryUtil.formattedNumber(sqYards / 4840, prec.ac)} acres`;
        } else {
          areaStr = `${LRef.GeometryUtil.formattedNumber(sqYards, prec.yd)} yd²`;
        }
      }
      return areaStr;
    };
  }

  // Helper: pixel distance on map container between a marker and a LatLng
  function markerPxDistance(map, marker, latlng) {
    if (!map || !marker || !latlng) return Infinity;
    const p1 = map.latLngToContainerPoint(marker.getLatLng());
    const p2 = map.latLngToContainerPoint(latlng);
    return p1.distanceTo(p2);
  }

  // 2. Patch L.Draw.Polyline._calculateFinishDistance & _endPoint
  LRef.Draw.Polyline.prototype._calculateFinishDistance = function calculateFinishDistancePatched(potentialLatLng) {
    const markers = this._markers || [];
    const count = markers.length;
    if (count === 0 || !this._map || !potentialLatLng) return Infinity;

    if (this.type === LRef.Draw.Polygon.TYPE) {
      if (count < 3) return Infinity;
      const dFirst = markerPxDistance(this._map, markers[0], potentialLatLng);
      const dLast = markerPxDistance(this._map, markers[count - 1], potentialLatLng);
      return Math.min(dFirst, dLast);
    }

    if (this.type === LRef.Draw.Polyline.TYPE) {
      if (count < 2) return Infinity;
      const dLast = markerPxDistance(this._map, markers[count - 1], potentialLatLng);
      const dFirst = count >= 3 ? markerPxDistance(this._map, markers[0], potentialLatLng) : Infinity;
      return Math.min(dLast, dFirst);
    }

    return Infinity;
  };

  LRef.Draw.Polyline.prototype._endPoint = function endPointPatched(clientX, clientY, e) {
    if (this._mouseDownOrigin) {
      const dragDistance = LRef.point(clientX, clientY).distanceTo(this._mouseDownOrigin);
      const dpr = (typeof window !== 'undefined' && window.devicePixelRatio) || 1;
      const markers = this._markers || [];
      const count = markers.length;

      if (this.options.maxPoints > 1 && this.options.maxPoints === count + 1) {
        this.addVertex(e.latlng);
        this._finishShape();
      } else if (Math.abs(dragDistance) < 9 * dpr) {
        if (this.type === LRef.Draw.Polygon.TYPE && count >= 3) {
          const dFirst = markerPxDistance(this._map, markers[0], e.latlng);
          const dLast = markerPxDistance(this._map, markers[count - 1], e.latlng);
          if (Math.min(dFirst, dLast) <= FINISH_SNAP_PX) {
            this._finishShape();
          } else {
            this.addVertex(e.latlng);
          }
        } else if (this.type === LRef.Draw.Polyline.TYPE && count >= 2) {
          const dLast = markerPxDistance(this._map, markers[count - 1], e.latlng);
          const dFirst = count >= 3 ? markerPxDistance(this._map, markers[0], e.latlng) : Infinity;
          if (dLast <= FINISH_SNAP_PX) {
            this._finishShape();
          } else if (dFirst <= FINISH_SNAP_PX) {
            // User clicked back on the starting corner to close a perimeter loop
            this.addVertex(markers[0].getLatLng());
            this._finishShape();
          } else {
            this.addVertex(e.latlng);
          }
        } else {
          this.addVertex(e.latlng);
        }
      }

      this._enableNewMarkers();
    }
    this._mouseDownOrigin = null;
  };

  // Guard Polyline._updateFinishHandler so clicking OR double-clicking marker[-1], or clicking marker[0] on 3+ vertex lines, finishes cleanly
  LRef.Draw.Polyline.prototype._closeLoopAndFinish = function closeLoopAndFinish() {
    if (!this._enabled || !this._markers || this._markers.length < 3) return;
    const firstLatLng = this._markers[0].getLatLng();
    const lastLatLng = this._markers[this._markers.length - 1].getLatLng();
    if (!lastLatLng.equals(firstLatLng)) {
      this.addVertex(firstLatLng);
    }
    this._finishShape();
  };

  LRef.Draw.Polyline.prototype._updateFinishHandler = function updatePolylineFinishHandlerPatched() {
    const markerCount = this._markers.length;
    if (markerCount > 1) {
      this._markers[markerCount - 1].on('click', this._finishShape, this);
      this._markers[markerCount - 1].on('dblclick', this._finishShape, this);
    }
    if (markerCount > 2) {
      this._markers[markerCount - 2].off('click', this._finishShape, this);
      this._markers[markerCount - 2].off('dblclick', this._finishShape, this);
      if (markerCount === 3) {
        this._markers[0].on('click', this._closeLoopAndFinish, this);
      }
    }
  };

  const origPolylineCleanUp = LRef.Draw.Polyline.prototype._cleanUpShape;
  LRef.Draw.Polyline.prototype._cleanUpShape = function cleanUpPolylinePatched() {
    if (this._markers && this._markers.length > 0) {
      this._markers[0].off('click', this._closeLoopAndFinish, this);
      const last = this._markers[this._markers.length - 1];
      if (last) {
        last.off('click', this._finishShape, this);
        last.off('dblclick', this._finishShape, this);
      }
    }
    return origPolylineCleanUp.call(this);
  };

  // Prevent map doubleClickZoom bounce when finishing a polyline or polygon by double-clicking
  if (typeof LRef.Draw.Polyline.prototype._finishShape === 'function') {
    const origPolylineFinish = LRef.Draw.Polyline.prototype._finishShape;
    LRef.Draw.Polyline.prototype._finishShape = function finishShapePatched(...args) {
      const mapRef = this._map;
      const hadDblClickZoom = Boolean(mapRef?.doubleClickZoom?.enabled?.());
      if (hadDblClickZoom) {
        mapRef.doubleClickZoom.disable();
        setTimeout(() => {
          if (mapRef && mapRef._loaded && mapRef.doubleClickZoom && !mapRef.doubleClickZoom.enabled()) {
            mapRef.doubleClickZoom.enable();
          }
        }, 280);
      }
      return origPolylineFinish.apply(this, args);
    };
  }

  // Enable Enter key to finish Polyline (>=2 pts) / Polygon (>=3 pts) & emit live vertex events
  const origPolylineEnable = LRef.Draw.Polyline.prototype.enable;
  const origPolylineDisable = LRef.Draw.Polyline.prototype.disable;
  const origPolylineAddVertex = LRef.Draw.Polyline.prototype.addVertex;
  const origPolylineDeleteLast = LRef.Draw.Polyline.prototype.deleteLastVertex;

  LRef.Draw.Polyline.prototype.enable = function enablePolylinePatched(...args) {
    const res = origPolylineEnable.apply(this, args);
    if (this._map) {
      this._map._activeJsmarsDrawer = this;
    }
    if (!this._jsmarsEnterHandler && typeof document !== 'undefined') {
      this._jsmarsEnterHandler = (e) => {
        if (e.key !== 'Enter') return;
        const tag = e.target?.tagName;
        if (tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT') return;
        const minPts = this.type === LRef.Draw.Polygon.TYPE ? 3 : 2;
        if (this._enabled && (this._markers?.length || 0) >= minPts) {
          e.preventDefault();
          this._finishShape();
        }
      };
      document.addEventListener('keydown', this._jsmarsEnterHandler);
    }
    if (typeof document !== 'undefined') {
      document.dispatchEvent(new CustomEvent('jmars:draw-vertex-changed'));
    }
    return res;
  };

  LRef.Draw.Polyline.prototype.disable = function disablePolylinePatched(...args) {
    if (this._jsmarsEnterHandler && typeof document !== 'undefined') {
      document.removeEventListener('keydown', this._jsmarsEnterHandler);
      this._jsmarsEnterHandler = null;
    }
    if (this._map && this._map._activeJsmarsDrawer === this) {
      this._map._activeJsmarsDrawer = null;
    }
    const res = origPolylineDisable.apply(this, args);
    if (typeof document !== 'undefined') {
      document.dispatchEvent(new CustomEvent('jmars:draw-vertex-changed'));
    }
    return res;
  };

  if (typeof origPolylineAddVertex === 'function') {
    LRef.Draw.Polyline.prototype.addVertex = function addVertexPatched(...args) {
      const res = origPolylineAddVertex.apply(this, args);
      if (typeof document !== 'undefined') {
        document.dispatchEvent(new CustomEvent('jmars:draw-vertex-changed'));
      }
      return res;
    };
  }

  if (typeof origPolylineDeleteLast === 'function') {
    LRef.Draw.Polyline.prototype.deleteLastVertex = function deleteLastPatched(...args) {
      const res = origPolylineDeleteLast.apply(this, args);
      if (typeof document !== 'undefined') {
        document.dispatchEvent(new CustomEvent('jmars:draw-vertex-changed'));
      }
      return res;
    };
  }

  // 3. Patch L.Draw.Polygon._updateFinishHandler & _cleanUpShape so clicking EITHER
  //    the first vertex OR the last vertex (when >= 3 points) finishes the polygon!
  LRef.Draw.Polygon.prototype._updateFinishHandler = function updatePolygonFinishHandlerPatched() {
    const markerCount = this._markers.length;
    if (markerCount === 1) {
      this._markers[0].on('click', this._finishShape, this);
    }
    if (markerCount > 2) {
      this._markers[markerCount - 1].on('click', this._finishShape, this);
      this._markers[markerCount - 1].on('dblclick', this._finishShape, this);
      if (markerCount > 3) {
        this._markers[markerCount - 2].off('click', this._finishShape, this);
        this._markers[markerCount - 2].off('dblclick', this._finishShape, this);
      }
    }
  };

  LRef.Draw.Polygon.prototype._cleanUpShape = function cleanUpPolygonPatched() {
    const markerCount = this._markers.length;
    if (markerCount > 0) {
      this._markers[0].off('click', this._finishShape, this);
      if (markerCount > 2) {
        this._markers[markerCount - 1].off('click', this._finishShape, this);
        this._markers[markerCount - 1].off('dblclick', this._finishShape, this);
      }
    }
  };

  // Update tooltip copy so users know first/last point, double-click, or Enter closes/finishes
  if (LRef.drawLocal?.draw?.handlers?.polyline?.tooltip) {
    LRef.drawLocal.draw.handlers.polyline.tooltip.end = 'Click last point, double-click, or press Enter to finish.';
  }
  if (LRef.drawLocal?.draw?.handlers?.polygon?.tooltip) {
    LRef.drawLocal.draw.handlers.polygon.tooltip.end = 'Click first/last point, double-click, or press Enter to close.';
  }

  LRef.Draw.__jsmarsPatched = true;
  return true;
}
