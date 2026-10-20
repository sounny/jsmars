/**
 * @module lunar-site/PlannerCadExport
 * @description Wires CAD import and exports into the planner.
 */
import { attachCad, segmentDistance } from './PlannerCad.js';
import { attachExport } from './PlannerExport.js';

export { segmentDistance };

export function attachCadExport(P) {
  attachCad(P);
  attachExport(P);
}
