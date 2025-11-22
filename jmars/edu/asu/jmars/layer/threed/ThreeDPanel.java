package edu.asu.jmars.layer.threed;

import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;

import javax.imageio.ImageIO;

import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.math.FloatUtil;
import com.jogamp.opengl.math.VectorUtil;

import javax.vecmath.Vector3f;

import com.jogamp.common.nio.Buffers;
import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;
import com.jogamp.opengl.util.awt.ImageUtil;
import com.jogamp.opengl.util.texture.Texture;
import com.jogamp.opengl.util.texture.TextureData;
import com.jogamp.opengl.util.texture.TextureIO;

import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;
import org.apache.commons.lang3.ArrayUtils;
import java.util.function.Function;
import java.util.stream.Stream;

//Meteor Studio imports
import java.net.*;
import java.io.*;
import java.util.Arrays;
import java.nio.*;
import java.nio.channels.*;

/**
 * This class implements a JOGL/Open GL JPanel that is used to render a
 * BufferedImage over a 3D surface. The X/Y dimensions of the 3D surface should
 * match that of the BufferedImage or the desired effect will not be achieved.
 *
 */

/*
 * Notes: http://www.sjbaker.org/steve/omniv/opengl_lighting.html
 * 
 * glMaterial and glLight The OpenGL light model presumes that the light that
 * reaches your eye from the polygon surface arrives by four different
 * mechanisms: AMBIENT - light that comes from all directions equally and is
 * scattered in all directions equally by the polygons in your scene. This isn't
 * quite true of the real world - but it's a good first approximation for light
 * that comes pretty much uniformly from the sky and arrives onto a surface by
 * bouncing off so many other surfaces that it might as well be uniform. DIFFUSE
 * - light that comes from a particular point source (like the Sun) and hits
 * surfaces with an intensity that depends on whether they face towards the
 * light or away from it. However, once the light radiates from the surface, it
 * does so equally in all directions. It is diffuse lighting that best defines
 * the shape of 3D objects. SPECULAR - as with diffuse lighting, the light comes
 * from a point source, but with specular lighting, it is reflected more in the
 * manner of a mirror where most of the light bounces off in a particular
 * direction defined by the surface shape. Specular lighting is what produces
 * the shiny highlights and helps us to distinguish between flat, dull surfaces
 * such as plaster and shiny surfaces like polished plastics and metals.
 * EMISSION - in this case, the light is actually emitted by the polygon -
 * equally in all directions.
 *
 */

public class ThreeDPanel extends GLJPanel implements GLEventListener {

	public static boolean validateIP(final String ip) {
		String PATTERN = "^((0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)\\.){3}(0|1\\d?\\d?|2[0-4]?\\d?|25[0-5]?|[3-9]\\d?)$";

		return ip.matches(PATTERN);
	}

	public void sendMesh(Mesh m) {
		String[] ipaddresses = new String[] {};
		if (settings != null && settings.ipaddress != null) {
			ipaddresses = settings.ipaddress.split(",");
		}
		if (ipaddresses.length == 0) {
			return;
		}
		try {

			ByteBuffer bb = ByteBuffer.allocate(m.vertices.length * 4 + 8); // 4 bytes per vertex float + 4 bytes for
																			// width + 4 bytes for height
			bb.order(ByteOrder.LITTLE_ENDIAN);

			// Insert the width and height
			bb.putFloat(elevation.getWidth());
			bb.putFloat(elevation.getHeight());

			for (int i = 0; i < m.vertices.length; i++) {
				bb.putFloat(m.vertices[i]);
			}

			for (int ip_index = 0; ip_index < ipaddresses.length; ip_index++) {
				String ipaddress = ipaddresses[ip_index].trim();
				if (ipaddress.length() > 0) {
					log.println("Send vertices to " + ipaddress);

					new Thread() {
						public void run() {
							try {
								Socket clientSocket = new Socket(ipaddress, 6789);
								DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());
								outToServer.write(bb.array(), 0, bb.array().length);
								clientSocket.close();
								log.println("Vertices sent");
							} catch (Exception e) {
								System.err.println(e);
							}
						}
					}.start();
				}
			}
		} catch (Exception e) {
			System.err.println(e);
		}
	}

	public void sendTexture(byte[] byteArray) {
		String[] ipaddresses = new String[] {};
		if (settings != null && settings.ipaddress != null) {
			ipaddresses = settings.ipaddress.split(",");
		}


		for (int ip_index = 0; ip_index < ipaddresses.length; ip_index++) {
			String ipaddress = ipaddresses[ip_index];
			if (ipaddress.length() > 0) {
				try {
					log.println("Send texture to " + ipaddress);
					Socket clientSocket = new Socket(ipaddress, 6801);
					DataOutputStream outToServer = new DataOutputStream(clientSocket.getOutputStream());

					outToServer.write(byteArray, 0, byteArray.length);
					clientSocket.close();
					log.println("Texture sent");
				} catch (Exception e) {
					System.err.println(e);
				}
			}
		}
	}

	/**
	 * @return  Returns the depth image for the source elevation used
	 * in the 3D layer display
	 */
	public BufferedImage getDepthImage(){
		return getDepthImage(elevation);
	}
	
	/**
	 * Creates a buffered image from an "Elevation" object 
	 * @param elevation  The elevation object to use
	 * @return  A grayscale buffered image stretched from the min and max 
	 * values on the Elevation object
	 */
	public BufferedImage getDepthImage(Elevation elevation) {
		final float[][] elevations = elevation.getPixelArrayFloat();

		final double USHORT_MAX = Math.pow(2, 16) - 1;
		final float zMin = elevation.getMinAltitude();
		final float zRange = elevation.getMaxAltitude() - zMin;

		final BufferedImage image = new BufferedImage(elevation.getWidth(), elevation.getHeight(),
				BufferedImage.TYPE_USHORT_GRAY);
		final Function<Float, Integer> convertToGrayScale = z -> new Double((z - zMin) / zRange * USHORT_MAX)
				.intValue();
		final Function<Float[], Stream<Integer>> rasterizeRow = row -> Arrays.stream(row).map(convertToGrayScale);

		int[] pixels = Arrays.stream(elevations).map(ArrayUtils::toObject).flatMap(rasterizeRow)
				.mapToInt(Integer::intValue).toArray();

		image.getRaster().setPixels(0, 0, image.getWidth(), image.getHeight(), pixels);
		ImageUtil.flipImageVertically(image);

		return image;
	}

	public BufferedImage getTextureImage() {
		return decalImage;
	}

	private static final long serialVersionUID = 1L;
	private static DebugLog log = DebugLog.instance();
	private static String version = null;
	private static String gluVersion = null;

	GLU glu;
	GL2 gl;
	private Elevation elevation = null;
	private float ZOOM_INC = 0.05f;
	private int prevMouseX;
	private int prevMouseY;
	private Texture decalTexture;
	private BufferedImage decalImage = null;
	private float lightOffset = 50000f;
	private float ambient = 0.1f, diffused = 0.1f, specular = 0f; // lighting default
	private float mSpecular = 0.8f, mDiffused = 0.1f; // material properties
	private FloatBuffer vBuf = null, // vertex buffer
			tBuf = null, // texture buffer
			nBuf = null; // vertex normal buffer
	private IntBuffer triBuf = null; // triangle index buffer

	private Mesh meshData = null; // triangle mesh from the 3D data

	private float exaggeration = 1.0f; // exaggeration factor
	private float avgElevation = 0f;

	private StartupParameters settings = null;

	private ThreeDCanvas parent = null;

	private final int VERTEX_DATA = 0;
	private final int TEXTURE_DATA = 1;
	private final int INDEX_DATA = 2;
	private final int NORMAL_DATA = 3;
	private int[] bufferObjs = new int[4];
	private boolean VBO = true;
	private ThreeDQueue queue = new ThreeDQueue();
	private boolean reInit = false;

	static enum Direction {
		VERT, HORZ, NONE
	};

	// JNN: added some variables
	boolean isWPressed = false;
	boolean isAPressed = false;
	boolean isSPressed = false;
	boolean isDPressed = false;

	public static String getVersion() {
		return version;
	}

	public static String getGLUVersion() {
		return gluVersion;
	}

	public ThreeDPanel(Elevation elevation, BufferedImage image, float scale, ThreeDCanvas parent) {
		super(ThreeDPanel.createGLCapabilities());
		setSize(image.getWidth(), image.getHeight());
		addGLEventListener(this);
		this.settings = new StartupParameters();
		this.elevation = elevation;
		decalImage = image;
		this.exaggeration = scale;
		this.parent = parent;

		settings.xOffset = -elevation.getWidth() / 2.0f;
		settings.yOffset = -elevation.getHeight() / 2.0f;
	}

	public ThreeDPanel(Elevation elevation, BufferedImage image, StartupParameters settings, ThreeDCanvas parent) {
		super(ThreeDPanel.createGLCapabilities());
		setSize(image.getWidth(), image.getHeight());
		addGLEventListener(this);
		this.elevation = elevation;
		decalImage = image;
		this.setSettings(settings);
		this.exaggeration = settings.scaleOffset;
		this.parent = parent;

	}

	private static GLCapabilities createGLCapabilities() {
		GLCapabilities capabilities = new GLCapabilities(GLProfile.getDefault());
		capabilities.setRedBits(8);
		capabilities.setBlueBits(8);
		capabilities.setGreenBits(8);
		capabilities.setAlphaBits(8);

		return capabilities;
	}

	public void init(GLAutoDrawable drawable) {
		glu = new GLU();
		gl = drawable.getGL().getGL2();
//        drawable.setGL(new DebugGL2(gl));
		drawable.setGL(gl);

		// Check for VBO support.

		// Check version.
		version = gl.glGetString(GL.GL_VERSION);
		gluVersion = glu.gluGetString(GLU.GLU_VERSION);
		log.println("GL version: " + version);
		log.println("GLU version: " + gluVersion);

		String tempVersion = version.substring(0, 3);
		float versionNum = new Float(tempVersion).floatValue();
		boolean versionValid = (versionNum >= 1.5f) ? true : false;
		log.println("Valid GL version:" + tempVersion + "  -> " + versionValid);

		// Check if extensions are available.
		boolean extValid = gl.isExtensionAvailable("GL_ARB_vertex_buffer_object");
		log.println("VBO extension: " + extValid);
		boolean texNPOT = gl.isExtensionAvailable("GL_ARB_texture_non_power_of_two");
		log.println("Texture NPOT extension: " + texNPOT);

		// Check for VBO functions.
		boolean funcsValid = gl.isFunctionAvailable("glGenBuffers") && gl.isFunctionAvailable("glBindBuffer")
				&& gl.isFunctionAvailable("glBufferData") && gl.isFunctionAvailable("glDeleteBuffers");
		log.println("Needed JOGL Functions Available: " + funcsValid);

		if (!extValid || !funcsValid) {
			// VBOs are not supported.
			log.println("VBOs are not supported.");
			VBO = false;
		}

		// Enable z- (depth) buffer for hidden surface removal.
		gl.glClearDepth(1.0f); // Depth Buffer Setup
		gl.glEnable(GL.GL_DEPTH_TEST);
		gl.glDepthFunc(GL.GL_LEQUAL);

		// Enable smooth shading.
		gl.glShadeModel(gl.GL_SMOOTH);

		// We want a nice perspective.
//        gl.glHint(gl.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);

		if (VBO) {
			gl.glGenBuffers(4, bufferObjs, 0);
		}

		if (Float.compare(settings.xOffset, 0.0f) == 0 && Float.compare(settings.yOffset, 0.0f) == 0) {
			settings.xOffset = -elevation.getWidth() / 2.0f; // coords for the virtual origin
			settings.yOffset = -elevation.getHeight() / 2.0f; // used to center the image
		}
		if (decalImage != null) {
			this.loadDecalImage();
		}
		this.meshData = this.loadMeshFromElevation(gl);

		this.drawMesh(gl);

		MouseListener simpleMouse = new SimpleMouseAdapter();
		KeyListener simpleKeys = new SimpleKeyAdapter();

		if (drawable instanceof Window) {
			Window window = (Window) drawable;
			window.addMouseListener(simpleMouse);
			window.addKeyListener(simpleKeys);
		} else if (GLProfile.isAWTAvailable() && drawable instanceof java.awt.Component) {
			java.awt.Component comp = (java.awt.Component) drawable;
			new AWTMouseAdapter(simpleMouse, drawable).addTo(comp);
			new AWTKeyAdapter(simpleKeys, drawable).addTo(comp);
		}
		int errCode = GL2.GL_NO_ERROR;
		if ((errCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
			String errString = glu.gluErrorString(errCode);
			System.err.println("OpenGL Error: " + errString);
			log.println("OpenGL Error: " + errString);
		}
	}

	public void dispose(GLAutoDrawable drawable) {
		// can't really do final cleanup here since the GLJPanel calls this method
		// when the enclosing window is re-sized
	}

	public void display(GLAutoDrawable drawable) {
//		update(); //JNN: added private method
		gl = drawable.getGL().getGL2();
		drawable.setGL(gl);

		if (reInit) { // this block rebuilds the scene
			if (decalTexture != null) {
				decalTexture.destroy(gl);
			}
			this.loadDecalImage();
			this.meshData = this.loadMeshFromElevation(gl);
			// JNN: shouldn't need to be reset unless reset button was pressed in
			// ThreeDFocus
			// reset sets xOffset and yOffset to zero
			// probability of user placing origin exactly at this point are slim due to
			// values being float
			if (Float.compare(settings.xOffset, 0f) == 0 && Float.compare(settings.yOffset, 0f) == 0) {
				settings.xOffset = -elevation.getWidth() / 2.0f;
				settings.yOffset = -elevation.getHeight() / 2.0f;
			}

			this.drawMesh(gl);
			reInit = false;
		}

		gl.glClearColor((float) settings.backgroundColor.getRed() / 255.0f,
				(float) settings.backgroundColor.getGreen() / 255.0f,
				(float) settings.backgroundColor.getBlue() / 255.0f, 1f);

		// clear screen
		gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

		gl.glPushMatrix();

		// JNN: tag this is the camera
		setCamera(gl, glu, 1000, settings.transX, settings.transY, settings.transZ);

		gl.glRotatef(-settings.beta, 0.0f, 1.0f, 0.0f); // rotate around y-axis
		gl.glRotatef(-settings.alpha, 1.0f, 0.0f, 0.0f); // rotate around x-axis
		gl.glRotatef(settings.gamma, 0.0f, 0.0f, 1.0f); // rotate around z-axis

		gl.glScaled(settings.zoomFactor, settings.zoomFactor, settings.zoomFactor);

		// JNN:
		gl.glTranslatef(settings.xOffset, settings.yOffset, -avgElevation /* 0.0f */); // translate back to our virtual
																						// origin

		if (settings.backplaneBoolean) {
			gl.glEnable(gl.GL_CULL_FACE);
			gl.glCullFace(gl.GL_BACK);
			gl.glFrontFace(gl.GL_CCW);
		} else {
			gl.glDisable(GL2.GL_CULL_FACE);
		}

		// Prepare light parameters.
		if (settings.directionalLightBoolean) {
			float[] lightPos = { settings.directionalLightDirection.x * lightOffset,
					settings.directionalLightDirection.y * lightOffset,
					settings.directionalLightDirection.z * lightOffset, 0f };
			float[] lightColorAmbient = { ambient, ambient, ambient, 1f };
			float[] lightDiffuse = { diffused, diffused, diffused, 1f };
			float[] lightColor = { (float) settings.directionalLightColor.getRed() / 255.0f,
					(float) settings.directionalLightColor.getGreen() / 255.0f,
					(float) settings.directionalLightColor.getBlue() / 255.0f, 1f };
			float[] lightColorSpecular = { 1f, 1f, 1f, 1f };
			float[] ambientLight = { 0.1f, 0.1f, 0.1f, 1f };

			// Set light parameters.
			gl.glEnable(GL2.GL_LIGHTING);
			gl.glLightModelfv(GL2.GL_LIGHT_MODEL_AMBIENT, ambientLight, 0);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_POSITION, lightPos, 0);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_AMBIENT, ambientLight, 0);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_DIFFUSE, lightColor, 0);
			gl.glLightfv(GL2.GL_LIGHT1, GL2.GL_SPECULAR, lightColorSpecular, 0);
			gl.glEnable(gl.GL_LIGHT1);

			gl.glEnable(GL2.GL_COLOR_MATERIAL);

			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_SPECULAR,
					new float[] { (float) settings.directionalLightColor.getRed() / 255.0f,
							(float) settings.directionalLightColor.getGreen() / 255.0f,
							(float) settings.directionalLightColor.getBlue() / 255.0f, 1f },
					0);
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_DIFFUSE, new float[] { mDiffused, mDiffused, mDiffused, 1f }, 0);
			gl.glMaterialfv(GL2.GL_FRONT, GL2.GL_AMBIENT, ambientLight, 0);
		} else {
			gl.glDisable(GL2.GL_LIGHTING);
			gl.glDisable(GL2.GL_COLOR_MATERIAL);
		}

//        gl.glEnable(GL2.GL_COLOR_MATERIAL);

		if (decalTexture != null) {
			decalTexture.enable(gl);
			decalTexture.bind(gl);

			if (VBO) {
				gl.glDrawElements(GL2.GL_TRIANGLES, meshData.tris.length, GL2.GL_UNSIGNED_INT, 0);
			} else {
				gl.glDrawElements(GL2.GL_TRIANGLES, meshData.tris.length, GL2.GL_UNSIGNED_INT, triBuf);
			}
			decalTexture.disable(gl);
		}
		// execute any jobs that require execution on this GL context
		queue.execute(drawable.getGL());

		gl.glPopMatrix();

		int errCode = GL2.GL_NO_ERROR;
		if ((errCode = gl.glGetError()) != GL2.GL_NO_ERROR) {
			String errString = glu.gluErrorString(errCode);
			System.err.println("OpenGL Error: " + errString);
			log.println("OpenGL Error: " + errString);
		}
	}

	// JNN: added update loop
	private void update() {

		float speed = 5f;
		if (isWPressed) {
			settings.xOffset -= speed * (float) Math.sin(settings.gamma * Math.PI / 180);
			settings.yOffset -= speed * (float) Math.cos(settings.gamma * Math.PI / 180);
		}
		if (isAPressed) {
			settings.yOffset -= speed * (float) Math.sin(settings.gamma * Math.PI / 180);
			settings.xOffset += speed * (float) Math.cos(settings.gamma * Math.PI / 180);
		}
		if (isSPressed) {
			settings.xOffset += speed * (float) Math.sin(settings.gamma * Math.PI / 180);
			settings.yOffset += speed * (float) Math.cos(settings.gamma * Math.PI / 180);
		}
		if (isDPressed) {
			settings.yOffset += speed * (float) Math.sin(settings.gamma * Math.PI / 180);
			settings.xOffset -= speed * (float) Math.cos(settings.gamma * Math.PI / 180);
		}
	}

	public void reshape(GLAutoDrawable drawable, int x, int y, int w, int h) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glViewport(0, 0, w, h);
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();
		glu.gluOrtho2D(0.0, (double) w, 0.0, (double) h);
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();

	}

	private void drawMesh(GL2 gl) {
		sendMesh(meshData); // Meteor added code

		gl.glEnableClientState(GL2.GL_VERTEX_ARRAY);
		gl.glEnableClientState(GL2.GL_INDEX_ARRAY);
		gl.glEnableClientState(GL2.GL_NORMAL_ARRAY);
		gl.glEnableClientState(GL2.GL_TEXTURE_COORD_ARRAY);

		if (VBO) {
			vBuf = FloatBuffer.wrap(meshData.vertices);
			tBuf = FloatBuffer.wrap(meshData.texture);
			nBuf = FloatBuffer.wrap(meshData.norms);
			triBuf = IntBuffer.wrap(meshData.tris);
			gl.glBindBuffer(GL2.GL_ARRAY_BUFFER, bufferObjs[VERTEX_DATA]);
			// Copy data to the server into the VBO.
			gl.glBufferData(GL2.GL_ARRAY_BUFFER, meshData.vertices.length * (Float.SIZE / Byte.SIZE), vBuf,
					GL2.GL_STATIC_DRAW);
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, 0);
			// Colors.
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferObjs[NORMAL_DATA]);
			gl.glBufferData(GL.GL_ARRAY_BUFFER, meshData.norms.length * (Float.SIZE / Byte.SIZE), nBuf,
					GL.GL_STATIC_DRAW);
			gl.glNormalPointer(GL2.GL_FLOAT, 0, 0);
			// Copy data to the server into the VBO.
			gl.glBindBuffer(GL.GL_ARRAY_BUFFER, bufferObjs[TEXTURE_DATA]);
			gl.glBufferData(GL.GL_ARRAY_BUFFER, meshData.texture.length * (Float.SIZE / Byte.SIZE), tBuf,
					GL.GL_STATIC_DRAW);
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, 0);

			gl.glBindBuffer(GL.GL_ELEMENT_ARRAY_BUFFER, bufferObjs[INDEX_DATA]);
			gl.glBufferData(GL.GL_ELEMENT_ARRAY_BUFFER, meshData.tris.length * (Integer.SIZE / Byte.SIZE), triBuf,
					GL.GL_STATIC_DRAW);
		} else { // use vertex arrays in the client
			vBuf = Buffers.newDirectFloatBuffer(meshData.vertices);
			tBuf = Buffers.newDirectFloatBuffer(meshData.texture);
			nBuf = Buffers.newDirectFloatBuffer(meshData.norms);
			triBuf = Buffers.newDirectIntBuffer(meshData.tris);
			gl.glVertexPointer(3, GL2.GL_FLOAT, 0, vBuf);
			gl.glNormalPointer(GL2.GL_FLOAT, 0, nBuf);
			gl.glTexCoordPointer(2, GL2.GL_FLOAT, 0, tBuf);
		}
	}

	private Mesh loadMeshFromElevation(GL2 gl) {

		// TODO (maybe...) all these float arrays should be interleaved into a single
		// array
		// and multiple index arrays used

		float[][] elevations = elevation.getPixelArrayFloat();

		int stride = 3;
		int width = elevation.getWidth();
		int height = elevation.getHeight();
		int[] tris = new int[((width - 1) * (height - 1) * 6)];
		float[] surfaceNorms = new float[((width - 1) * (height - 1) * 6)];
		float[] texture = new float[width * height * 2];
		float[] vertices = new float[width * height * 3];
		float[] normals = new float[width * height * 3];
		float maxLen = 0f;

		// calculate the vertices in 3 space coords going thru the elevation array row
		// by row
		int vertexIdx = 0;
		int tIdx = 0;
		int triIdx = 0;
		avgElevation = 0f;

		for (int y = 0; y < height; y++) {
			for (int x = 0; x < width; x++) {

				vertices[vertexIdx++] = x; // X
				if (FloatUtil.abs(x) > maxLen)
					maxLen = FloatUtil.abs(x);
				vertices[vertexIdx++] = height - y - 1; // Y
				if (FloatUtil.abs(height - y - 1) > maxLen)
					maxLen = FloatUtil.abs(height - y - 1);
				vertices[vertexIdx++] = elevations[height - y - 1][x] * this.exaggeration * -1; // Z // JNN: tag
				if (FloatUtil.abs(elevations[height - y - 1][x] * this.exaggeration * -1) > maxLen)
					maxLen = FloatUtil.abs(elevations[height - y - 1][x] * this.exaggeration * -1);
				texture[tIdx++] = x / (float) decalImage.getWidth();
				texture[tIdx++] = ((float) decalImage.getHeight() - y - 1) / (float) decalImage.getHeight();
				// initialize the normal array while we are looping through the vertices
				normals[triIdx++] = 0f;
				avgElevation += vertices[vertexIdx - 1];
			}
		}
		avgElevation /= ((float) height * (float) width);

		triIdx = 0;
		for (int y = 0; y < height - 1; y++) {
			for (int x = 0; x < width - 1; x++) {
				// calculate 2 triangles for each 4 points in x/y plane
				// first triangle
				tris[triIdx++] = vertexLoc(x, height - y - 2, width); // 0,1
				tris[triIdx++] = vertexLoc(x, height - y - 1, width); // 0,0
				tris[triIdx++] = vertexLoc(x + 1, height - y - 2, width); // 1,1

				// calculate the triangle surface normal vector
				float[] normVec = Vec3dMath.normalFromPoints(vertices, tris[triIdx - 3] * stride, vertices,
						tris[triIdx - 2] * stride, vertices, tris[triIdx - 1] * stride);

				surfaceNorms[triIdx - 3] = normVec[0];
				surfaceNorms[triIdx - 2] = normVec[1];
				surfaceNorms[triIdx - 1] = normVec[2];

				// second triangle
				tris[triIdx++] = vertexLoc(x + 1, height - y - 2, width); // 1,1
				tris[triIdx++] = vertexLoc(x, height - y - 1, width); // 0,0
				tris[triIdx++] = vertexLoc(x + 1, height - y - 1, width); // 1,0

				// calculate the triangle surface normal vector
				float[] normVec2 = Vec3dMath.normalFromPoints(vertices, tris[triIdx - 3] * stride, vertices,
						tris[triIdx - 2] * stride, vertices, tris[triIdx - 1] * stride);

				surfaceNorms[triIdx - 3] = normVec2[0];
				surfaceNorms[triIdx - 2] = normVec2[1];
				surfaceNorms[triIdx - 1] = normVec2[2];

			}
		}

		// calculate vertex normals from the surface normals
		int snLoc = 0;
		for (int v = 0; v < tris.length; v++) {

			if (v % stride == 0) {
				snLoc = v;
			}
			int ptLoc = 0;

			ptLoc = tris[v] * stride; // get the index into the normal vector array

			normals[ptLoc] = normals[ptLoc] + surfaceNorms[snLoc];
			normals[ptLoc + 1] = normals[ptLoc + 1] + surfaceNorms[snLoc + 1];
			normals[ptLoc + 2] = normals[ptLoc + 2] + surfaceNorms[snLoc + 2];
		}

		// normalize the array of normal vectors
		for (int n = 0; n < normals.length; n += stride) {
			float[] temp = new float[] { normals[n], normals[n + 1], normals[n + 2] };
			float[] norm = normalizeVec3(new float[3], temp);
			normals[n] = norm[0];
			normals[n + 1] = norm[1];
			normals[n + 2] = norm[2];
		}
		return new Mesh(vertices, tris, texture, normals, surfaceNorms, maxLen);
	}

	/**
	 * Method to write the current 3D layer topography to an STL file for 3D
	 * printing. A base will be added to the topography to create a closed 3D volume
	 * as required by STL format specifications. Shading, color, and image data will
	 * not be included in the STL file.
	 *
	 * @param filePath A platform specific path to where the STL file will be
	 *                 written
	 * @param name     Name of the STL output file
	 * @throws IllegalArgumentException
	 * @throws IOException
	 *
	 */
	public void saveBinarySTL(String filePath, String name, float baseThickness) throws IllegalArgumentException, IOException {

		float[] vertices = this.meshData.vertices;
		int[] indices = this.meshData.tris;
		float[] normals = this.meshData.facetNorms;
		float maxVal = this.meshData.maxLen;

		if (filePath == null || name == null) {
			throw new IllegalArgumentException("Cannot write 3D print file (.stl) with null input parameters.");
		}
		byte[] hdrName = name.getBytes(StandardCharsets.UTF_8);
		if (hdrName.length > 80) {
			throw new IllegalArgumentException("Name of Binary STL file cannot be greater than 80 BYTES in length");
		}
		float absMaxCoord = maxVal + 0.001f; // need to make sure that all values are > 0.0

		// calculate the base first to get the size
		// calculate the min Z of the base
		float minZ = elevation.getMinAltitude() * this.exaggeration;
		int h = elevation.getHeight();
		int w = elevation.getWidth();
		float basePad = baseThickness;
		if (h > w) {
			basePad *= h;
		} else {
			basePad *= w;
		}
		minZ = -minZ - basePad;
		// start building the base mesh
		ArrayList<Float> verts = new ArrayList<>();
		ArrayList<Float> norms = new ArrayList<>();

		// create the flat base
		float[] v1 = new float[] { 0f, 0f, minZ };
		float[] v2 = new float[] { w - 1, h - 1, minZ };
		float[] v3 = new float[] { w - 1, 0f, minZ };

		verts.add(v1[0]);
		verts.add(v1[1]);
		verts.add(v1[2]);
		verts.add(v2[0]);
		verts.add(v2[1]);
		verts.add(v2[2]);
		verts.add(v3[0]);
		verts.add(v3[1]);
		verts.add(v3[2]);
		float[] norm = normalizeVec3(new float[3], Vec3dMath.normalFromPoints(v1, 0, v2, 0, v3, 0));
		norms.add(norm[0]);
		norms.add(norm[1]);
		norms.add(norm[2]);

		float[] v4 = new float[] { 0f, 0f, minZ };
		float[] v5 = new float[] { 0f, h - 1, minZ };
		float[] v6 = new float[] { w - 1, h - 1, minZ };

		verts.add(v4[0]);
		verts.add(v4[1]);
		verts.add(v4[2]);
		verts.add(v5[0]);
		verts.add(v5[1]);
		verts.add(v5[2]);
		verts.add(v6[0]);
		verts.add(v6[1]);
		verts.add(v6[2]);
		float[] norm1 = normalizeVec3(new float[3], Vec3dMath.normalFromPoints(v4, 0, v5, 0, v6, 0));
		norms.add(norm1[0]);
		norms.add(norm1[1]);
		norms.add(norm1[2]);

		// create each side
		createSideMesh(elevation.getLeftSide(exaggeration * -1), verts, norms, minZ);
		createSideMesh(elevation.getRightSide(exaggeration * -1), verts, norms, minZ);
		createSideMesh(elevation.getUpperSide(exaggeration * -1), verts, norms, minZ);
		createSideMesh(elevation.getLowerSide(exaggeration * -1), verts, norms, minZ);

		float[] v = new float[verts.size()];
		float[] n = new float[norms.size()];
		for (int i = 0; i < verts.size(); i++) {
			v[i] = verts.get(i);
		}
		for (int i = 0; i < norms.size(); i++) {
			n[i] = norms.get(i);
		}

		// calculate how big our ByteBuffer needs to be
		// 80 bytes for the header
		// 4 bytes for the number of facets
		// number of facets (indices.length / 3) * the size of a facet (50 bytes) see
		// http://www.fabbers.com/tech/STL_Format
		int size = 84 + (indices.length / 3 * 50) + (v.length / 9 * 50);

		ByteBuffer buf = ByteBuffer.allocate(size);
		buf.order(ByteOrder.LITTLE_ENDIAN);

		// write out the header
		for (int j = 0; j < 80; j++) {
			if (j < hdrName.length) {
				buf.put(hdrName[j]);
			} else {
				buf.put((byte) 32);
			}
		}

		// write the number of facets
		buf.putInt((indices.length / 3) + (v.length / 9));

		// write out the normal and vertices for each facet in the original mesh
		for (int i = 0; i < indices.length; i += 3) {
			// normal first
			buf.putFloat(normals[i]);
			buf.putFloat(normals[i + 1]);
			buf.putFloat(normals[i + 2]);

			// facet vertices in CCW order
			// first vertex
			buf.putFloat(vertices[indices[i] * 3] + absMaxCoord);
			buf.putFloat(vertices[indices[i] * 3 + 1] + absMaxCoord);
			buf.putFloat(vertices[indices[i] * 3 + 2] + absMaxCoord);
			// second vertex
			buf.putFloat(vertices[indices[i + 1] * 3] + absMaxCoord);
			buf.putFloat(vertices[indices[i + 1] * 3 + 1] + absMaxCoord);
			buf.putFloat(vertices[indices[i + 1] * 3 + 2] + absMaxCoord);
			// third vertex
			buf.putFloat(vertices[indices[i + 2] * 3] + absMaxCoord);
			buf.putFloat(vertices[indices[i + 2] * 3 + 1] + absMaxCoord);
			buf.putFloat(vertices[indices[i + 2] * 3 + 2] + absMaxCoord);
			buf.putShort((short) 0);
		}

		// write out the normals and vertices for each base facet
		int normCnt = 0;
		for (int i = 0; i < v.length; i += 9) {
			// normal first
			buf.putFloat(n[normCnt++]);
			buf.putFloat(n[normCnt++]);
			buf.putFloat(n[normCnt++]);

			// facet vertices in CCW order
			// first vertex
			buf.putFloat(v[i] + absMaxCoord);
			buf.putFloat(v[i + 1] + absMaxCoord);
			buf.putFloat(v[i + 2] + absMaxCoord);
			// second vertex
			buf.putFloat(v[i + 3] + absMaxCoord);
			buf.putFloat(v[i + 4] + absMaxCoord);
			buf.putFloat(v[i + 5] + absMaxCoord);
			// third vertex
			buf.putFloat(v[i + 6] + absMaxCoord);
			buf.putFloat(v[i + 7] + absMaxCoord);
			buf.putFloat(v[i + 8] + absMaxCoord);
			buf.putShort((short) 0);
		}

		Files.write(Paths.get(filePath), buf.array());

	}

	private void createSideMesh(float[][] line, ArrayList<Float> verts, ArrayList<Float> norm, float z) {
		if (line == null || line.length < 9 || verts == null || norm == null) {
			return;
		}

		for (int i = 0; i < line.length - 1; i++) {
			float[] normal = normalizeVec3(new float[3], Vec3dMath.normalFromPoints(line[i], 0,
					new float[] { line[i][0], line[i][1], z }, 0, line[i + 1], 0));
			// 1st facet
			norm.add(normal[0] * -1f);
			norm.add(normal[1] * -1f);
			norm.add(normal[2] * -1f);

			verts.add(line[i + 1][0]);
			verts.add(line[i + 1][1]);
			verts.add(line[i + 1][2]);

			verts.add(line[i][0]);
			verts.add(line[i][1]);
			verts.add(z);

			verts.add(line[i][0]);
			verts.add(line[i][1]);
			verts.add(line[i][2]);

			// 2nd facet
			norm.add(normal[0] * -1f);
			norm.add(normal[1] * -1f);
			norm.add(normal[2] * -1f);

			verts.add(line[i + 1][0]);
			verts.add(line[i + 1][1]);
			verts.add(z);

			verts.add(line[i][0]);
			verts.add(line[i][1]);
			verts.add(z);

			verts.add(line[i + 1][0]);
			verts.add(line[i + 1][1]);
			verts.add(line[i + 1][2]);

		}

	}

	/**
	 * This method calculates the location of a vertex in a single dimensional array
	 * where each vertex is composed of three consecutive elements of the array. The
	 * vertex array is a row by row mapping of a 2 dimensional array to a one
	 * dimensional array...if that makes any sense
	 * 
	 * @param x
	 * @param y
	 * @param rowWidth
	 * @return
	 */
	int vertexLoc(int x, int y, int rowWidth) {
		return (x + (y * rowWidth));
	}

	boolean isPowerOf2(int i) {
		return i > 2 && ((i & -i) == i);
	}

	private void setCamera(GL2 gl, GLU glu, float distance, float lookX, float lookY, float lookZ) {
		// Change to projection matrix.
		gl.glMatrixMode(GL2.GL_PROJECTION);
		gl.glLoadIdentity();

		// Perspective.
		float widthHeightRatio = (float) getWidth() / (float) getHeight();
		glu.gluPerspective(45, widthHeightRatio, 1, 1000000);
		glu.gluLookAt(lookX, lookY, distance, lookX, lookY, lookZ, 0, 1, 0);

		// Change back to model view matrix.
		gl.glMatrixMode(GL2.GL_MODELVIEW);
		gl.glLoadIdentity();
	}

	/**
	 * @return the lightDirection
	 */
	public Vector3f getDirection() {
		return settings.directionalLightDirection;
	}

	public void setScaleString(String s) {
		if (s != null) {
			settings.zScaleString = s;
		}
	}

	public StartupParameters getSettings() {
		return settings;
	}

	public void setSettings(StartupParameters settings) {
		this.settings = settings;
		this.reInit = true;
	}

	/**
	 * @param directionalLightEnabled the directionalLightEnabled to set
	 */
	public void setDirectionalLightEnabled(boolean directionalLightEnabled) {
		settings.directionalLightBoolean = directionalLightEnabled;
		reInit = true;
	}

	/**
	 * @param directionalLightColor the directionalLightColor to set
	 */
	public void setDirectionalLightColor(Color directionalLightColor) {
		settings.directionalLightColor = directionalLightColor;
		reInit = true;
	}

	/**
	 * @return backgroundColor
	 */
	public Color getBackgroundColor() {
		return settings.backgroundColor;
	}

	/**
	 * @param backgroundColor the backgroundColor to set
	 */
	public void setBackgroundColor(Color backgroundColor) {
		settings.backgroundColor = backgroundColor;
		reInit = true;
	}

	/**
	 * @param lightDirection the lightDirection to set
	 */
	public void setDirection(Vector3f lightDirection) {
		if (lightDirection != null) {
			settings.directionalLightDirection = new Vector3f(lightDirection.x, lightDirection.y, lightDirection.z);
		}
		reInit = true;
	}

	public void setElevation(Elevation elevation) {
		this.elevation = elevation;
		this.reInit = true;
	}

	public Elevation getElevation() {
		return this.elevation;
	}

	public void setDecalImage(BufferedImage decalImage) {
		this.decalImage = decalImage;
		this.reInit = true;
	}

	public void loadDecalImage() {
		if (decalImage != null) {

			int newHeight = 0;
			int newWidth = 0;

			if (!isPowerOf2(decalImage.getWidth())) {
				// get the next higher power of two
				newWidth = (int) Math.pow(2.0, Math.ceil(Math.log(decalImage.getWidth()) / Math.log(2)));
			}

			if (!isPowerOf2(decalImage.getHeight())) {
				newHeight = (int) Math.pow(2.0, Math.ceil(Math.log(decalImage.getHeight()) / Math.log(2)));
			}

			if (newWidth > 0 || newHeight > 0) {
				BufferedImage tmp = new BufferedImage(newWidth > 0 ? newWidth : decalImage.getWidth(),
						newHeight > 0 ? newHeight : decalImage.getHeight(), decalImage.getType());

				tmp.setData(decalImage.getRaster());
				decalImage = tmp;
			}

			// get the texture
			try {

				final ByteArrayOutputStream output = new ByteArrayOutputStream() {
					@Override
					public synchronized byte[] toByteArray() {
						return this.buf;
					}
				};

				ImageIO.write(decalImage, "png", output);
				InputStream dstream = new ByteArrayInputStream(output.toByteArray());

				sendTexture(output.toByteArray()); // Meteor added code

				TextureData decalData = TextureIO.newTextureData(gl.getGLProfile(), dstream, false, "png");

				decalTexture = TextureIO.newTexture(decalData);
			} catch (IOException exc) {
				exc.printStackTrace();
				System.exit(1);
			}
		}
	}

	public void setExaggeration(float exaggeration) {
//System.err.println("setting exaggeration to: "+exaggeration);	
		this.exaggeration = exaggeration;
	}

	/**
	 * @return the backplaneEnabled
	 */
	public boolean isBackplaneEnabled() {
		return settings.backplaneBoolean;
	}

	public void refresh() {
		this.repaint();
	}

	void addToQueue(ThreeDAction action) {
		if (action != null) {
			queue.add(action);
		}
	}

	/**
	 * @param backplaneEnabled the backplaneEnabled to set
	 */
	public void setBackplaneEnabled(boolean backplaneEnabled) {
		settings.backplaneBoolean = backplaneEnabled;
	}

	/*
	 * Method to handle 3D vector normalization even when the vector magnitude is <
	 * 1.0 This version uses VectorUtil from JOGL to calculate the vector magnitude
	 *
	 * @param vout the return vector
	 * 
	 * @param vin the input vector to be normalized
	 * 
	 * @return the return vector
	 *
	 * thread-safe
	 */
	private static float[] normalizeVec3(float[] vout, float[] vin) {
		float vmag = VectorUtil.normVec3(vin);

		if (vmag > 0.0f) {
			if (vmag > 1f) {
				vout[0] = vin[0] / vmag;
				vout[1] = vin[1] / vmag;
				vout[2] = vin[2] / vmag;
			} else {
				vout = VectorUtil.scaleVec3(new float[3], vin, 1f - vmag);
			}
		} else {
			vout[0] = 0.0f;
			vout[1] = 0.0f;
			vout[2] = 0.0f;
		}

		return vout;
	}

	/*
	 * Rescale a vector of direction "vector" with length "size"
	 *
	 * @param vector
	 * 
	 * @param size
	 * 
	 * @return the resized vector
	 *
	 * thread-safe
	 */
	private static float[] setVectorLength(float[] vector, float size) {

		// normalize the vector
		float[] vectorNormalized = new float[3];
		vectorNormalized = normalizeVec3(vectorNormalized, vector);

		// scale the vector
		return VectorUtil.scaleVec3(vectorNormalized, vectorNormalized, size);

	}

	class SimpleKeyAdapter extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			int kc = e.getKeyCode();
			int kchar = e.getKeyChar();
			float speed = 3f;

			switch (kc) {
			case KeyEvent.VK_LEFT:
				settings.transX += 2f;
				break;
			case KeyEvent.VK_RIGHT:
				settings.transX -= 2f;
				break;
			case KeyEvent.VK_UP:
				settings.transY -= 2f;
				break;
			case KeyEvent.VK_DOWN:
				settings.transY += 2f;
				break;
			case KeyEvent.VK_PLUS:
				settings.gamma += 2f;
				break;
			case KeyEvent.VK_ADD:
				settings.gamma += 2f;
				break;
			case KeyEvent.VK_MINUS:
				settings.gamma -= 2f;
				break;
			case KeyEvent.VK_SUBTRACT:
				settings.gamma -= 2f;
				break;
			case KeyEvent.VK_W: // JNN Added W = up
				// to handle z rotation:
				settings.xOffset -= speed * (float) Math.sin(settings.gamma * Math.PI / 180);
				settings.yOffset -= speed * (float) Math.cos(settings.gamma * Math.PI / 180);
				break;
			case KeyEvent.VK_A: // JNN Added A = left
				// to handle z rotation
				settings.yOffset -= speed * (float) Math.sin(settings.gamma * Math.PI / 180);
				settings.xOffset += speed * (float) Math.cos(settings.gamma * Math.PI / 180);
				// settings.yOffset -= speed * (float)Math.sin(settings.gamma*Math.PI/180);
				// settings.xOffset += speed * (float)Math.cos(settings.gamma*Math.PI/180);
				break;
			case KeyEvent.VK_S: // JNN Added S = down
				// to handle z rotation
				settings.xOffset += speed * (float) Math.sin(settings.gamma * Math.PI / 180);
				settings.yOffset += speed * (float) Math.cos(settings.gamma * Math.PI / 180);
				break;
			case KeyEvent.VK_D: // JNN Added D = right
				// to handle z rotation
				settings.yOffset += speed * (float) Math.sin(settings.gamma * Math.PI / 180);
				settings.xOffset -= speed * (float) Math.cos(settings.gamma * Math.PI / 180);
				break;
			case KeyEvent.VK_F5: // update 3D view
				parent.updateElevationSource();
				parent.refresh();
				break;
			case KeyEvent.VK_Z:
				switch (kchar) {
				case 'Z':
					settings.zoomFactor += ZOOM_INC;
					break;
				case 'z':
					settings.zoomFactor -= ZOOM_INC;
					if (settings.zoomFactor < 0.0f) {
						settings.zoomFactor = 0.01f;
					}
					break;
				}
				break;
			}
			// ugly hack since VK_PLUS is not consistent across platforms/languages
			if (e.isShiftDown() && "+".equalsIgnoreCase(Character.toString((char) kchar))) {
				settings.gamma += 2f;
			}

			ThreeDPanel.this.repaint();
		}
	}

	class SimpleMouseAdapter extends MouseAdapter {

		Direction dir = Direction.NONE;
		boolean start = true;

		public void mousePressed(MouseEvent e) {
			prevMouseX = e.getX();
			prevMouseY = e.getY();
		}

		public void mouseReleased(MouseEvent e) {
			dir = Direction.NONE;
			start = true;
		}

		public void mouseDragged(MouseEvent e) {
			float xThresh = 10.0f;
			float yThresh = 10.0f;
			int x = e.getX();
			int y = e.getY();
			int width = 0, height = 0;
			Object source = e.getSource();
			if (source instanceof Window) {
				Window window = (Window) source;
				width = window.getWidth();
				height = window.getHeight();
			} else if (GLProfile.isAWTAvailable() && source instanceof java.awt.Component) {
				java.awt.Component comp = (java.awt.Component) source;
				width = comp.getWidth();
				height = comp.getHeight();
			} else {
				throw new RuntimeException("Event source neither Window nor Component: " + source);
			}

			float thetaY = 360.0f * ((float) (prevMouseX - x) / (float) width);
			float thetaX = 360.0f * ((float) (prevMouseY - y) / (float) height);
			float deltaY = y - prevMouseY;
			float deltaX = prevMouseX - x;

			if (start) {
				if (Math.abs(deltaY / (Math.abs(deltaX) > Float.MIN_NORMAL ? deltaX : 1f)) > 0.5f) {
					dir = Direction.VERT;
					start = false;
				} else {
					dir = Direction.HORZ;
					start = false;
				}
			}
			if (e.isShiftDown()) {
				if (dir == Direction.VERT && deltaY > 0.0 && deltaY > yThresh) { // zoom
					settings.zoomFactor -= ZOOM_INC;
					if (settings.zoomFactor < 0.0f) {
						settings.zoomFactor = 0.01f;
					}
				} else if (dir == Direction.VERT && deltaY <= 0.0 && Math.abs(deltaY) > yThresh) {
					settings.zoomFactor += ZOOM_INC;
				}

				if (dir == Direction.HORZ && Math.abs(deltaX) > xThresh) {
					settings.gamma += (thetaY);
				}
			} else if (e.isControlDown()) {
				// translate
				settings.transX += ((float) (prevMouseX - x));
				settings.transY += ((float) (y - prevMouseY));
			} else {
				settings.alpha += thetaX;
				settings.beta += thetaY;
			}

			prevMouseX = x;
			prevMouseY = y;

			ThreeDPanel.this.repaint();
		}

		public void mouseWheelMoved(MouseEvent e) {
			float multiplier = 1.01f;
			float[] direction = e.getRotation();
			if (e.isControlDown()) {
				multiplier = 1.1f;
			} else {
				multiplier = 1.01f;
			}

			if (direction[1] < 0) {
				settings.zoomFactor /= multiplier;
				if (settings.zoomFactor < 0.0001f) {
					settings.zoomFactor = 0.0001f;
				}
			} else {
				settings.zoomFactor *= multiplier;
				if (settings.zoomFactor > 4f) {
					settings.zoomFactor = 4f;
				}
			}
			ThreeDPanel.this.repaint();
		}
	}

	class Mesh {
		float[] vertices;
		float[] baseVerts;
		int[] tris;
		float[] texture;
		float[] norms;
		float[] facetNorms;
		float[] baseFacetNorms;
		float maxLen;

		// Copies references - no deep copy
		public Mesh(float[] vertices, int[] tris, float[] texture, float[] normals, float[] facets, float max) {

			maxLen = max;

			this.vertices = new float[vertices.length];
			System.arraycopy(vertices, 0, this.vertices, 0, vertices.length);

			this.tris = new int[tris.length];
			System.arraycopy(tris, 0, this.tris, 0, tris.length);

			this.texture = new float[texture.length];
			System.arraycopy(texture, 0, this.texture, 0, texture.length);

			this.norms = new float[normals.length];
			System.arraycopy(normals, 0, this.norms, 0, normals.length);

			this.facetNorms = new float[facets.length];
			System.arraycopy(facets, 0, this.facetNorms, 0, facets.length);

		}

		public void setBaseVerts(float[] verts) {
			this.baseVerts = new float[verts.length];
			System.arraycopy(verts, 0, this.baseVerts, 0, verts.length);
		}

		public void setBaseNorms(float[] norms) {
			this.baseFacetNorms = new float[norms.length];
			System.arraycopy(norms, 0, this.baseFacetNorms, 0, norms.length);
		}

	}
}
