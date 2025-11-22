package edu.asu.jmars.layer.map2.msd;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.util.DebugLog;

public class PlotSourcesListTransferHandler extends TransferHandler {
	private static DebugLog log = DebugLog.instance();
	
	List flavorsList = Arrays.asList(new DataFlavor[]{
			MapSourceArrTransferable.mapSrcArrDataFlavor,
	});

	public boolean canImport(JComponent comp, DataFlavor[] transferFlavors) {
		for(int i=0; i<transferFlavors.length; i++)
			if (flavorsList.contains(transferFlavors[i]))
				return true;
		return false;
	}

	public int getSourceActions(JComponent c) {
		return TransferHandler.NONE;
	}

	public boolean importData(JComponent comp, Transferable t) {
		if (t == null){
			log.println("Transferable is null.");
			return false;
		}

		try {
			MapSource[] srcs = (MapSource[])t.getTransferData((DataFlavor)flavorsList.get(0));
			PipelineModel pipelineModel = ((PlotSourcesListModel)((JList)comp).getModel()).getBackingPipelineModel();
			for(int i=0; i<srcs.length; i++)
				pipelineModel.addSource(srcs[i]);
		}
		catch(UnsupportedFlavorException ex){
			log.println(ex.getStackTrace());
			return false;
		}
		catch(IOException ex){
			log.println(ex.getStackTrace());
			return false;
		}
		catch(ClassCastException ex){
			log.println(ex.getStackTrace());
			return false;
		}

		return true;
	}
}
