package edu.asu.jmars.layer.map2.msd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import edu.asu.jmars.layer.map2.MapSource;

public class MapSourceArrTransferable implements Transferable {
	public static final String mapSrcArrMimeType = DataFlavor.javaJVMLocalObjectMimeType+";class="+MapSource.class.getName();
	public static final DataFlavor mapSrcArrDataFlavor;
	DataFlavor[] suppFlavors;
	List suppFlavorsList;
	
	static {
		DataFlavor f = null;
		try {
			f = new DataFlavor(mapSrcArrMimeType);
		}
		catch(ClassNotFoundException ex){
			ex.printStackTrace();
		}
		mapSrcArrDataFlavor = f;
	}
	
	MapSource[] srcArr;
	
	public MapSourceArrTransferable(MapSource[] srcArr){
		if (mapSrcArrDataFlavor == null)
			suppFlavors = new DataFlavor[0];
		else
			suppFlavors = new DataFlavor[]{ mapSrcArrDataFlavor };
		suppFlavorsList = Arrays.asList(suppFlavors);
		
		this.srcArr = srcArr;
	}
	
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		if (!isDataFlavorSupported(flavor))
			throw new UnsupportedFlavorException(flavor);
		
		return srcArr;
	}

	public DataFlavor[] getTransferDataFlavors() {
		return suppFlavors;
	}

	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return suppFlavorsList.contains(flavor);
	}
}

