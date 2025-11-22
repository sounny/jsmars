package edu.asu.jmars.layer.threed;

import java.awt.Color;
import com.jogamp.opengl.GL;
import com.jogamp.opengl.GL2;
import com.jogamp.opengl.GLAutoDrawable;
import com.jogamp.opengl.GLCapabilities;
import com.jogamp.opengl.GLEventListener;
import com.jogamp.opengl.GLProfile;
import com.jogamp.opengl.awt.GLJPanel;
import com.jogamp.opengl.glu.GLU;
import com.jogamp.opengl.glu.GLUquadric;
import javax.vecmath.Color3f;
import javax.vecmath.Vector3f;

import com.jogamp.newt.Window;
import com.jogamp.newt.event.KeyAdapter;
import com.jogamp.newt.event.KeyEvent;
import com.jogamp.newt.event.KeyListener;
import com.jogamp.newt.event.MouseAdapter;
import com.jogamp.newt.event.MouseEvent;
import com.jogamp.newt.event.MouseListener;
import com.jogamp.newt.event.awt.AWTKeyAdapter;
import com.jogamp.newt.event.awt.AWTMouseAdapter;

import edu.asu.jmars.layer.FocusPanel;
import edu.asu.jmars.util.HVector;


public class DirectionalLightWidget extends GLJPanel implements GLEventListener {
	
    private static final long serialVersionUID = 1L;

	GLU glu;
	GL2 gl;
	int shader, program = 0;
	private float alpha = 0;  // angle about x axis
	private float beta = 0;	// angle about y axis
	private int prevMouseX;
	private int prevMouseY;
    private float ambient = 0.2f, diffused = 1f, specular = 0f; // lighting default
    private float mSpecular = 0.8f, mDiffused = 0.3f; // material properties
        
    private float radius = 20.0f;
    private float xLight = 0.0f;
    private float yLight = 0.0f;
    private float zLight = radius;
    private HVector lightVector = new HVector(xLight, yLight, zLight);
    
    FocusPanel parent = null;
    GLJPanel drawable = null;
    Color color = new Color(128, 128, 128);
    private Color3f lightColor = new Color3f(128, 128, 128);

    
    public DirectionalLightWidget(FocusPanel parent) {
    	super(DirectionalLightWidget.createGLCapabilities());
        setSize(150, 150);
        addGLEventListener(this);
        
        this.parent = parent;
        this.drawable = this;
        
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
		
       // Enable z- (depth) buffer for hidden surface removal. 
        gl.glClearDepth(1.0f);                      // Depth Buffer Setup
        gl.glEnable(GL.GL_DEPTH_TEST);
        gl.glDepthFunc(GL.GL_LEQUAL);
        
        // Enable smooth shading.
        gl.glShadeModel(gl.GL_SMOOTH);
        
        // We want a nice perspective.
        gl.glHint(gl.GL_PERSPECTIVE_CORRECTION_HINT, GL.GL_NICEST);
                
        
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
		        
	}

	public void dispose(GLAutoDrawable drawable) {
		
	}

	public void display(GLAutoDrawable drawable) {
    	final GL2 gl = drawable.getGL().getGL2();
    	// clear screen
        gl.glClear(GL.GL_COLOR_BUFFER_BIT | GL.GL_DEPTH_BUFFER_BIT);

       	setCamera(gl, glu, 30);
        
    	float SHINE_ALL_DIRECTIONS = 1f;
    	float[] lightPos = {0, 0, 0, SHINE_ALL_DIRECTIONS};
    	float[] spotLightPos = {(float)lightVector.x, (float)lightVector.y, (float)lightVector.z, 1.0f};
    	float[] lightColorAmbient = {0.1f, 0.1f, 0.1f, 1f};
    	float[] lightColorSpecular = {0.2f, 0.2f, 0.2f, 1f};
    	float[] spotLightColorSpecular = {0.5f, 0.5f, 0.5f, 1.0f};
    	float[] spotLightSpecular = {1.0f, 1.0f, 1.0f, 1.0f};
    	float[] lightDiffuse = {0.1f, 0.1f, 0.1f, 1f}; 
    	float[] spotLightDiffuse = {1f, 1f, 1f, 1f}; 
    	float[] partialDiffuse = {0.5f, 0.5f, 0.5f, 0f}; 
		
        gl.glEnable(gl.GL_LIGHTING);

        // Set light parameters.
    	gl.glLightfv(gl.GL_LIGHT1, gl.GL_POSITION, lightPos, 0);
    	gl.glLightfv(gl.GL_LIGHT1, gl.GL_AMBIENT, lightColorAmbient, 0);
        
       	// set up the spot light
    	gl.glLightfv(gl.GL_LIGHT0, gl.GL_POSITION, spotLightPos, 0);
    	gl.glLightfv(gl.GL_LIGHT0, gl.GL_SPECULAR, spotLightColorSpecular, 0);
       	gl.glLightf(gl.GL_LIGHT0, gl.GL_SPOT_CUTOFF, 135.0f);
       	gl.glLightf(gl.GL_LIGHT0, gl.GL_SPOT_EXPONENT, 200.0f);
       	gl.glLightfv(GL2.GL_LIGHT0, GL2.GL_DIFFUSE, partialDiffuse, 0);
       	
    	// Enable lighting in GL.
        gl.glEnable(gl.GL_LIGHT0);
        gl.glEnable(gl.GL_LIGHT1);
		gl.glColor4f((float)lightColor.get().getRed()/255.0f, (float)lightColor.get().getGreen()/255.0f, (float)lightColor.get().getBlue()/255.0f, 1.0f);
        
        gl.glEnable(gl.GL_COLOR_MATERIAL);

        // Set material properties.
        gl.glMaterialfv(GL.GL_FRONT, gl.GL_SPECULAR, spotLightColorSpecular, 0);
        gl.glMaterialfv(GL.GL_FRONT, gl.GL_DIFFUSE, partialDiffuse, 0);
        gl.glMaterialf(GL.GL_FRONT, gl.GL_SHININESS, 10000f);

    	gl.glEnable(gl.GL_CULL_FACE);
		gl.glCullFace(gl.GL_BACK);

        gl.glPushMatrix();

		// draw lighting sphere
        GLUquadric sphere = glu.gluNewQuadric();
        glu.gluQuadricDrawStyle(sphere, GLU.GLU_FILL);
        glu.gluQuadricNormals(sphere, GLU.GLU_SMOOTH);
        glu.gluQuadricOrientation(sphere, GLU.GLU_OUTSIDE);
        final float radius = 11.4f;
        final int slices = 360;
        final int stacks = 360;
        glu.gluSphere(sphere, radius, slices, stacks);
        
        glu.gluDeleteQuadric(sphere);
		gl.glPopMatrix();

	}
	
	public void reshape(GLAutoDrawable drawable, int x, int y, int w,
			int h) {
		GL2 gl = drawable.getGL().getGL2();
		gl.glViewport(0, 0, w, h);
        gl.glMatrixMode(GL2.GL_PROJECTION);
        gl.glLoadIdentity();
        glu.gluOrtho2D(0.0, (double) w, 0.0, (double) h);
        gl.glMatrixMode(GL2.GL_MODELVIEW);
        gl.glLoadIdentity();
	}
	
	private void setCamera(GL2 gl, GLU glu, float distance) {
        // Change to projection matrix.
        gl.glMatrixMode(gl.GL_PROJECTION);
        gl.glLoadIdentity();

        // Perspective.
        float widthHeightRatio = (float) getWidth() / (float) getHeight();
        glu.gluPerspective(45, widthHeightRatio, 1, 1000000);
        glu.gluLookAt(0, 0, distance, 0, 0, 0, 0, 1, 0);

        // Change back to model view matrix.
        gl.glMatrixMode(gl.GL_MODELVIEW);
        gl.glLoadIdentity();
    }

	public void setLightDirection(float x, float y, float z) {
		lightVector.x = x;
		lightVector.y = y;
		lightVector.z = z;
	}

	Vector3f getLightDirection() {
		return new Vector3f((float)lightVector.x, (float)lightVector.y, (float)lightVector.z);
	}
	
	public float getRadius() {
		return radius;
	}

	public void setRadius(float radius) {
		this.radius = radius;
	}

	class SimpleKeyAdapter extends KeyAdapter {
		public void keyPressed(KeyEvent e) {
			int kc = e.getKeyCode();
			int kchar = e.getKeyChar();
			switch(kc){
			case KeyEvent.VK_LEFT: beta += 1; break;
			case KeyEvent.VK_RIGHT: beta -= 1; break;
			case KeyEvent.VK_UP: alpha -= 1; break;
			case KeyEvent.VK_DOWN: alpha += 1; break;
			}
			
			DirectionalLightWidget.this.repaint();
		}
	}

  class SimpleMouseAdapter extends MouseAdapter {
      public void mousePressed(MouseEvent e) {
        prevMouseX = e.getX();
        prevMouseY = e.getY();
      }

      public void mouseReleased(MouseEvent e) {
      }

      public void mouseDragged(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int width=0, height=0;
        Object source = e.getSource();
        if(source instanceof Window) {
            Window window = (Window) source;
            width=window.getWidth();
            height=window.getHeight();
        } else if (GLProfile.isAWTAvailable() && source instanceof java.awt.Component) {
            java.awt.Component comp = (java.awt.Component) source;
            width=comp.getWidth();
            height=comp.getHeight();
        } else {
            throw new RuntimeException("Event source neither Window nor Component: "+source);
        }
        
        float thetaY = (float)Math.PI * ( (float)(x - prevMouseX)/radius/(/*lightVector.z < 0.0f ? 0.25f :*/ 16.0f));
        float thetaX = (float)Math.PI * ( (float)(y - prevMouseY)/radius/(/*lightVector.z < 0.0f ? 0.25f :*/ 16.0f));
        
        prevMouseX = x;
        prevMouseY = y;

        alpha += thetaX;
        beta += thetaY;
        
        lightVector = lightVector.rotate(lightVector.X_AXIS, thetaX);
        lightVector = lightVector.rotate(lightVector.Y_AXIS, thetaY);
        
		DirectionalLightWidget.this.repaint();
      }
  }
  
	public void setColor(Color3f color3f) {
		lightColor = color3f;	
	}
	
	public void setPosition(float x, float y) {
		// TODO Auto-generated method stub
		
	}     
}
