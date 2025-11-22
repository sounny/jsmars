package edu.asu.jmars.layer.shape2.xb.swing;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

public class UserTransferable implements Transferable {

	public static final DataFlavor USER_DATA_FLAVOR = new DataFlavor(String.class, "MyFieldName");
	private String myfieldname;

	public UserTransferable(String userdata) {
		this.myfieldname = userdata;
	}

	@Override
	public DataFlavor[] getTransferDataFlavors() {
		return new DataFlavor[] { USER_DATA_FLAVOR };
	}

	@Override
	public boolean isDataFlavorSupported(DataFlavor flavor) {
		return USER_DATA_FLAVOR.equals(flavor);
	}

	@Override
	public Object getTransferData(DataFlavor flavor) throws UnsupportedFlavorException, IOException {
		Object value = null;
		if (USER_DATA_FLAVOR.equals(flavor)) {
			value = myfieldname;
		} else {
			throw new UnsupportedFlavorException(flavor);
		}
		return value;
	}
}
