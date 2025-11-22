package edu.asu.jmars.layer.stamp.focus;

import java.awt.BorderLayout;
import java.awt.image.BufferedImage;
import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.URISyntaxException;
//import java.net.URL;
import java.net.URLDecoder;
import java.net.UnknownHostException;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SpringLayout;

import cookxml.cookswing.util.SpringLayoutUtilities;
import edu.asu.jmars.layer.stamp.FilledStamp;
import edu.asu.jmars.layer.stamp.FilledStampImageType;
import edu.asu.jmars.layer.stamp.StampCache;
import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampLayerSettings;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.util.JmarsHttpRequest;
import edu.asu.jmars.util.HttpRequestType;
import edu.asu.msff.Stamp;

public class DavinciFocusPanel extends JPanel {

	static int idCnt = 1;
	
	public DavinciFocusPanel(final StampLayer stampLayer) {
		BorderLayout layout=new BorderLayout();
		setLayout(layout);

		StampLayerSettings settings = stampLayer.getSettings();
		
		//Davinci Connection Panel code
		JPanel dvconnectOptionsPanel = new JPanel();
		dvconnectOptionsPanel.setLayout(new SpringLayout());
		dvconnectOptionsPanel.setBorder(BorderFactory.createTitledBorder("Connection Parameters"));
		
		//set up the host subpanel
		JLabel hostLabel = new JLabel("Host:");
		hostLabel.setToolTipText("This is the hostname used to communicate with davinci");
		JTextField hostText = new JTextField("localhost");
		
		//setup the ipaddress subpanel
		JLabel ipLabel = new JLabel("IP Address:");
		ipLabel.setToolTipText("This is the IP address used to communicate with davinci");
		JTextField ipText = new JTextField("127.0.0.1");
		
		//set up the port subpanel
		JLabel portLabel = new JLabel("Port:");
		portLabel.setToolTipText("This is the current port used to communicate with davinci");
		JTextField portText = new JTextField(""+settings.port, 20);
		dvconnectOptionsPanel.add(portLabel);
		dvconnectOptionsPanel.add(portText);

		//get the hostname of the local machine
		try {
			hostText = new JTextField(InetAddress.getLocalHost().getHostName(), 20);
			ipText= new JTextField(InetAddress.getLocalHost().getHostAddress(), 17);
		} catch (UnknownHostException e) {
			e.printStackTrace();
		}

		//add all the subpanels to the larger subpanel
		dvconnectOptionsPanel.add(hostLabel);
		dvconnectOptionsPanel.add(hostText);
		dvconnectOptionsPanel.add(ipLabel);
		dvconnectOptionsPanel.add(ipText);
		
		//Here we make the checkbox for auto rendering new data
		JCheckBox autoRender = new JCheckBox("Automatically Render Images", true);
		autoRender.setToolTipText("Enable this to automatically render new images from davinci." +
				"You may want disable this if you are dealing with many large files");
		
		dvconnectOptionsPanel.add(new JLabel(""));
		dvconnectOptionsPanel.add(autoRender);

		SpringLayoutUtilities.makeCompactGrid(dvconnectOptionsPanel, 4, 2, 30, 6, 6, 6);

		
		//Help example from davinci.asu.edu/examples/display_jmars.help
		JTextArea dvconnectHelp = new JTextArea(10,30);
		dvconnectHelp.setEditable(false);
		 
		//now we get the most recent help code from the website
        JmarsHttpRequest request = new JmarsHttpRequest("http://davinci.asu.edu/examples/display_jmars.help", HttpRequestType.GET);
		try {
			// Create a URL for the desired page
//			URL url = new URL("http://davinci.asu.edu/examples/display_jmars.help");
            boolean requestStatus = request.send();

			// Read all the text returned by the server
//			BufferedReader in = new BufferedReader(new InputStreamReader(url.openStream()));  // TODO (PW) replace with JmarsHttpRequest
            if (requestStatus) {
                BufferedReader in = new BufferedReader(new InputStreamReader(request.getResponseAsStream()));
    			String str;
    			while ((str = in.readLine()) != null) {
    				dvconnectHelp.append(str+"\n");
    			}
    			in.close();
            }
        } catch (IOException e) { 
			e.printStackTrace();
		} catch (URISyntaxException e) {
            e.printStackTrace();
        } finally {
		    request.close();
		}
		
		//set the output style and the scroll panel functionality
		dvconnectHelp.setFont(ThemeFont.getRegular());		
		JScrollPane dvconnectHelpSP = new JScrollPane(dvconnectHelp);
		dvconnectHelpSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		dvconnectHelpSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
		dvconnectHelpSP.setBorder(BorderFactory.createTitledBorder("Example Usage:"));
		
					
		//Log Panel code
		final JTextArea logText = new JTextArea("Here is the example log output");
		
		JScrollPane logSP = new JScrollPane(logText);
		logSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		logSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
				
		logSP.setBorder(BorderFactory.createTitledBorder("Log Output:"));
		
		add(dvconnectOptionsPanel,BorderLayout.NORTH);
		add(logSP, BorderLayout.CENTER);
		add(dvconnectHelpSP,BorderLayout.SOUTH);
		
		try {
			serverSocket = new ServerSocket(Integer.parseInt(portText.getText()));
			logText.append("\nSocket opened successfully on port: " + portText.getText());
			
			Runnable r = new Runnable(){
			
				public void run() {
					while (!shutdown) {
						
						if (shutdown) return;
						Socket s = null;
					try {
						s = serverSocket.accept();
						InputStream is = s.getInputStream();
						BufferedInputStream bis = new BufferedInputStream(is);
						
						String str = "";
						
						while (true) {
							int num=bis.available();
							byte b[]=new byte[num];
							bis.read(b);
							str += new String(b);
							if (str.endsWith("\n")) break;
//							break;
						}
						
						logText.append("\n"+str);
						
						String ullat = parseString(str, "ullat=");
						String ullon = parseString(str, "ullon=");

						String urlat = parseString(str, "urlat=");
						String urlon = parseString(str, "urlon=");

						String lllat = parseString(str, "lllat=");
						String lllon = parseString(str, "lllon=");

						String lrlat = parseString(str, "lrlat=");
						String lrlon = parseString(str, "lrlon=");

						String id = parseString(str, "id=");
						String ignore = parseString(str, "ignore=");
						String name = parseString(str, "name=");
						
						//
						
						double points[]=new double[8];
						
						points[0]= Double.parseDouble(lllon);
						points[1]= Double.parseDouble(lllat);
						points[2]= Double.parseDouble(lrlon);
						points[3]= Double.parseDouble(lrlat);
						points[4]= Double.parseDouble(urlon);
						points[5]= Double.parseDouble(urlat);
						points[6]= Double.parseDouble(ullon);
						points[7]= Double.parseDouble(ullat);

						String path = parseString(str, "path=");


						if (id==null || id.length()==0) {
							id=""+idCnt++;
						}
						
						if (name==null || name.length()==0) {
							name="local ("+id+")";
						} else {
							name= URLDecoder.decode(name);
						}

						Object data[]=new Object[7];
						
						data[0]=id;
						data[1]=name;
						data[2]=path;
						data[3]=""+ullat;
						data[4]=""+ullon;
						data[5]=""+lrlat;
						data[6]=""+lrlon;

						Stamp newStamp = new Stamp(id, points, data);
						
						StampShape shape = new StampShape(newStamp, stampLayer);
						
						HashMap<String,String> params = new HashMap<String, String>();
						params.put("lon0", ""+lllon);
						params.put("lon1", ""+lrlon);
						params.put("lon2", ""+urlon);
						params.put("lon3", ""+ullon);
						
						params.put("lat0", ""+lllat);
						params.put("lat1", ""+lrlat);
						params.put("lat2", ""+urlat);
						params.put("lat3", ""+ullat);
						
						params.put("map_projection_type", "CYLINDRICAL");
						
						if (ignore!=null && ignore.length()>0) {
							params.put("ignore_value", ignore);
						}
						
						// TODO: Add numeric support to davinci stamps						
						BufferedImage image = StampCache.read(path, false);
												
						
						StampImage si = new StampImage(shape, id, "davinci", name, image, params);
						
						FilledStamp fs = new FilledStampImageType(shape, si, null); 
						
						stampLayer.addStampData(shape);
						
						stampLayer.viewToUpdate.getFocusPanel().getRenderedView().addStamp(fs, null, true, true, true);				
						//
						
						java.io.OutputStream os = s.getOutputStream();
						os.write('\n');
						os.write('1');
						os.write('\n');
					
						os.flush();
						os.close();
						bis.close();
						s.close();
					} catch (Exception e) {
						e.printStackTrace();
					} finally {
						if (s!=null) {
							try {
								s.close();
							} catch (Exception e) {
								e.printStackTrace();
							}
						}
					}
					
					}
				}
			};
			
			Thread t = new Thread(r);
			t.start();
		} catch (Exception e) {
			e.printStackTrace();
			logText.append("\nSocket failed to open");
		}
		
	}
	
	boolean shutdown=false;
	
	ServerSocket serverSocket = null;
	
	protected void dispose() {
		try {
			shutdown=true;
			if (serverSocket!=null) {
				serverSocket.close();
			}
			System.out.println("socket closed");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private static String parseString(String str, String key) {
		int startloc=str.indexOf(key)+key.length();
		int endloc = str.indexOf("&",startloc);
		
		if (endloc>-1) {
			return str.substring(startloc, endloc);
		} else {
			return str.substring(startloc);
		}
	}
	
}
