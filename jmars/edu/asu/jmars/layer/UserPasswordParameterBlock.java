package edu.asu.jmars.layer;

import edu.asu.jmars.Main;
import edu.asu.jmars.util.Base64Codec;

/**
 * This class will encode and decode the userid and password in order to
 * serialize and store the object to a file for session restoration. Please
 * do not make these variables public - only use the access methods.
 */
public class UserPasswordParameterBlock extends DialogParameterBlock
{
	public final String defaultUser = Main.USER;
	public final String defaultPassword = Main.PASS;

	private String		userId;
	private String		password;

	public void setUserId(String u) {
		userId = Base64Codec.getInstance().encode(u);
	}

	public void setPassword(String p) {

		password = Base64Codec.getInstance().encode(p);

	}

	public String getUserId() {

		return Base64Codec.getInstance().decode(userId);

	}

	public String getPassword() {

		return Base64Codec.getInstance().decode(password);

	}
}
