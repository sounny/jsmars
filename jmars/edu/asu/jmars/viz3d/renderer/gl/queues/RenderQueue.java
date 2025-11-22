package edu.asu.jmars.viz3d.renderer.gl.queues;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.event.EventListenerList;

import edu.asu.jmars.viz3d.renderer.gl.GLRenderable;

/**
 * Class to transfer GLRenderable requests submitted to ThreeDManager on to the renderer (Scene)
 * 
 * thread-safe
 */
public class RenderQueue extends ConcurrentLinkedQueue<GLRenderable> {

	private static final long serialVersionUID = 7276013414113712740L;
	
    private static RenderQueue instance = null;
    
    private EventListenerList listenerList = new EventListenerList();


    protected RenderQueue(){    	
    }
 
    /**
     * Singleton construction
     *
     * thread-safe
     */
    public static synchronized RenderQueue getInstance()
    {
       if(instance == null) {
    	   instance = new RenderQueue();
       }
       return instance;
    }

    /** 
     * Method to add ActionListeners to this queue
     *
     * @param l
     *
     * thread-safe
     */
    public void addActionListener(ActionListener l) {
         listenerList.add(ActionListener.class, l);
    }
    
    /**
     * Method to remove ActionListeners from this queue
     *
     * @param l
     *
     * thread-safe
     */
    public void removeActionListener(ActionListener l) {
         listenerList.remove(ActionListener.class, l);
    }

    /**
     * Notifies all listeners that have registered interest for
     * notification on this event type.
     *
     * @param e the action event to fire
     * @see EventListenerList
     */
    protected void fireActionPerformed(ActionEvent e) {
        // Guaranteed to return a non-null array
        Object[] listeners = listenerList.getListenerList();

        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i=listeners.length-2; i>=0; i-=2) {
            if (listeners[i]==ActionListener.class) {
                ((ActionListener)listeners[i+1]).actionPerformed(e);
            }
        }
    }
    
    /**
     * Method to add a GLRenderable instance to the queue for rendering by the renderer
     * 
     *  @param e GLRenderable to be rendered
     */
    @Override
    public boolean add(GLRenderable e) {
        return offer(e);
    }

    
}
