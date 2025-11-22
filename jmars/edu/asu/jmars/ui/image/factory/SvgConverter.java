package edu.asu.jmars.ui.image.factory;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import javax.imageio.ImageIO;
import javax.swing.Icon;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;
import org.apache.batik.transcoder.Transcoder;
import org.apache.batik.transcoder.TranscoderException;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.JPEGTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;
import org.apache.batik.transcoder.image.TIFFTranscoder;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;
import edu.asu.jmars.Main;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FONTS;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FontFile;
import edu.asu.jmars.util.DebugLog;
import jiconfont.DefaultIconCode;
import jiconfont.IconCode;
import jiconfont.IconFont;
import jiconfont.swing.IconFontSwing;


public enum SvgConverter {
		
	PNG {
		@Override
		public BufferedImage convert(ImageDescriptor desc) {
			Transcoder trc = new PNGTranscoder();
			return convertSvg(desc, trc);
		}	
	},

	JPG {
		@Override
		public BufferedImage convert(ImageDescriptor desc) {			
			Transcoder trc = new JPEGTranscoder();
			return convertSvg(desc, trc);
		}	
	},

	TIFF {
		@Override
		public BufferedImage convert(ImageDescriptor desc) {	
			Transcoder trc = new TIFFTranscoder();
			return convertSvg(desc, trc);		
		}	
	};
	
	public abstract BufferedImage convert(ImageDescriptor desc);
	private static DebugLog log = DebugLog.instance();
	private static final String DEFAULT_SVG = "<?xml version=\"1.0\" encoding=\"UTF-8\"?>" +
											"<svg width=\"12px\" height=\"12px\" viewBox=\"0 0 12 12\" version=\"1.1\" xmlns=\"http://www.w3.org/2000/svg\" xmlns:xlink=\"http://www.w3.org/1999/xlink\">" +  
    										"<title>blank</title>" +   									
    										"<g id=\"Icons\" stroke=\"none\" stroke-width=\"1\" fill=\"none\" fill-rule=\"evenodd\" fill-opacity=\"0\">" +
    										"<g id=\"Layer-Icons\" transform=\"translate(-189.000000, -719.000000)\" fill=\"#FFFFFF\">" +
    										"<rect id=\"blank\" x=\"189\" y=\"719\" width=\"12\" height=\"12\"></rect>" +
    										"</g>" +
    										"</g>" +
    										"</svg>";
		
	private static BufferedImage convertSvg(ImageDescriptor desc, Transcoder trc)
	{		
		BufferedImage img = null;
		String strSVG;			
		try (InputStream svg = Main.getResourceAsStream(desc.getImageFilePath());
				ByteArrayOutputStream resultByteStream = new ByteArrayOutputStream();
				InputStream fallbackSVG = new ByteArrayInputStream(DEFAULT_SVG.getBytes(StandardCharsets.UTF_8))) {			
			TranscoderInput input = new TranscoderInput();
			TranscoderOutput transcoderOutput = new TranscoderOutput(resultByteStream);
			if (svg == null) {
				log.aprintln("Unable to locate image " + desc.getImageFilePath() + ". Will use default svg blank_space. ");						
			}
			strSVG = (svg != null) ? updateSVG(svg, desc).toString() : updateSVG(fallbackSVG, desc).toString();
			Reader in = new StringReader(strSVG);
			input.setReader(in);
			trc.transcode(input, transcoderOutput);
			resultByteStream.flush();
			img = ImageIO.read(new ByteArrayInputStream(resultByteStream.toByteArray()));
			resultByteStream.close();
		} catch (TranscoderException e) {
			log.aprintln("Failed to convert svg " + desc.getImageFilePath());
			MyIconFont iconfont = new MyIconFont(FontFile.REGULAR.toString());
			IconFontSwing.register(iconfont);
			IconCode iconcode = new DefaultIconCode(iconfont.getFontFamily(), '\u0020');
			Icon icon = IconFontSwing.buildIcon(iconcode, FONTS.ROBOTO.fontSize(), Color.WHITE);
			img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.SCALE_DEFAULT);
		} catch (IOException e) {
			log.aprintln("Failed to convert svg " + desc.getImageFilePath());
			MyIconFont iconfont = new MyIconFont(FontFile.REGULAR.toString());
			IconFontSwing.register(iconfont);
			IconCode iconcode = new DefaultIconCode(iconfont.getFontFamily(), '\u0020');
			Icon icon = IconFontSwing.buildIcon(iconcode, FONTS.ROBOTO.fontSize(), Color.WHITE);
			img = new BufferedImage(icon.getIconWidth(), icon.getIconHeight(), BufferedImage.SCALE_DEFAULT);
		}
		return img;
	}
	
	private static StringBuffer updateSVG(InputStream svg, ImageDescriptor desc) throws IOException {
		Document doc;
		String px = "px";
		StringWriter sout = new StringWriter();

		try (PrintWriter out = new PrintWriter(sout)) {
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(svg);
			XPath xpath = XPathFactory.newInstance().newXPath();

			Optional<Color> fill = desc.getDisplayColor();
			fill.ifPresent(fillcolor -> {
				try {
					NodeList nodes = (NodeList) xpath.evaluate("//*[contains(@fill, '#')]", doc,
							XPathConstants.NODESET);
					if (nodes.getLength() > 0) {
						Node value = nodes.item(0).getAttributes().getNamedItem("fill");
						if (value != null)
							value.setNodeValue(getColorAsBrowserHex(fillcolor));
					}
				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}
			});

			Optional<Color> strokecolor = desc.getStrokeColor();
			strokecolor.ifPresent(stroke -> {
				NodeList nodes;
				try {
					nodes = (NodeList) xpath.evaluate("//*[contains(@stroke, '#')]", doc, XPathConstants.NODESET);
					if (nodes.getLength() > 0) {
						Node value = nodes.item(0).getAttributes().getNamedItem("stroke");
						if (value != null)
							value.setNodeValue(getColorAsBrowserHex(stroke));
					}
				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}
			});

			// scale if need to
			int width = desc.getWidth();
			if (width != 0) {
				NodeList nodes;
				try {
					nodes = (NodeList) xpath.evaluate("//*[@width]", doc, XPathConstants.NODESET);
					if (nodes.getLength() > 0) {
						Node value = nodes.item(0).getAttributes().getNamedItem("width");
						if (value != null) {
							value.setNodeValue(Integer.toString(width) + px);
						}
					}
				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}
			}

			int height = desc.getHeight();
			if (height != 0) {
				NodeList nodes;
				try {
					nodes = (NodeList) xpath.evaluate("//*[@height]", doc, XPathConstants.NODESET);
					if (nodes.getLength() > 0) {
						Node value = nodes.item(0).getAttributes().getNamedItem("height");
						if (value != null) {
							value.setNodeValue(Integer.toString(height) + px);
						}
					}
				} catch (XPathExpressionException e) {
					e.printStackTrace();
				}
			}
			Transformer xformer;
			xformer = TransformerFactory.newInstance().newTransformer();
			xformer.transform(new DOMSource(doc), new StreamResult(out));
		} catch (SAXException e2) {
			log.aprintln("Failed to parse svg " + desc.getImageFilePath());
			e2.printStackTrace();
		} catch (ParserConfigurationException e2) {
			log.aprintln("Failed to parse svg " + desc.getImageFilePath());
			e2.printStackTrace();
		} catch (TransformerConfigurationException e) {
			log.aprintln("Failed to transform svg to image " + desc.getImageFilePath());
			e.printStackTrace();
		} catch (TransformerFactoryConfigurationError e) {
			log.aprintln("Failed to transform svg to image " + desc.getImageFilePath());
			e.printStackTrace();
		} catch (TransformerException e) {
			log.aprintln("Failed to transform svg to image " + desc.getImageFilePath());
			e.printStackTrace();
		}
		return sout.getBuffer();
	}

	private static String getColorAsBrowserHex(Color color) {
		 String rgb = Integer.toHexString(color.getRGB());		 
	     return "#" + rgb.substring(2, rgb.length());	     
	}
	
	private static class MyIconFont implements IconFont {

		private String fontfilename;

		MyIconFont(String filename) {
			this.fontfilename = filename;
		}

		@Override
		public String getFontFamily() {
			return "Roboto JMARS";
		}

		@Override
		public InputStream getFontInputStream() {
			return Main.getResourceAsStream(ThemeFont.getFontPath() + this.fontfilename);
		}
	}	
}
