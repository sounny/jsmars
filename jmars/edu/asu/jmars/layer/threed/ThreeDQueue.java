package edu.asu.jmars.layer.threed;

import java.util.ArrayList;

import com.jogamp.opengl.GL;

/**
 * A queue for actions that need to be executed on the JOGL current context. 
 *
 */
public class ThreeDQueue {
    private final ArrayList<ThreeDAction> queue= new ArrayList<ThreeDAction>(10);

    public ThreeDQueue(){}
    
    public void add(ThreeDAction action) {
       synchronized (queue) { 
    	   queue.add(action); 
       }
    }

    public void execute(GL gl) {
       // make a copy of the queue to allow thread safe iteration
       ArrayList<ThreeDAction> temp = null;
       synchronized (queue) {
          // Only make a copy, if the queue has entries
          if(queue.size() != 0) {
             temp = new ArrayList<ThreeDAction>(queue);
             queue.clear();
          }
       }

       // iterate outside of the synchronization to avoid blocking the queue
       if(temp!=null) {
          for (ThreeDAction action : temp) {
             action.execute(gl);
          }
       }
    }  
}
