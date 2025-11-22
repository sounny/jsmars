package edu.asu.jmars.viz3d.renderer.textures;

import edu.asu.jmars.layer.Layer.LView3D;

/**
 * 3 Key Object to map DecalSets to a specific combination of Shape Model (TriangleMesh), LView3D, and PPD
 */
public class DecalKey {
	
	private String shapeModel;
	private LView3D lview;
	private int ppd;
	
	public DecalKey(String shapeModel, LView3D lview, int ppd) {
		this.shapeModel = shapeModel;
		this.lview = lview;
		this.ppd = ppd;
	}
	
    /**
	 * @return the shapeModel
	 */
	public String getShapeModel() {
		return shapeModel;
	}

	/**
	 * @return the lview
	 */
	public LView3D getLview() {
		return lview;
	}

	/**
	 * @return the ppd
	 */
	public int getPpd() {
		return ppd;
	}

	@Override
    public boolean equals(Object o) {

//        if (o == this) return true;
        if (!(o instanceof DecalKey)) {
            return false;
        }
        DecalKey key = (DecalKey) o;
        return shapeModel == key.getShapeModel() &&
                lview == key.getLview() &&
                ppd == key.ppd;
    }

    @Override
    public int hashCode() {
        return (shapeModel.hashCode()*31+lview.hashCode())*31+Integer.valueOf(ppd).hashCode();
    }	

}
