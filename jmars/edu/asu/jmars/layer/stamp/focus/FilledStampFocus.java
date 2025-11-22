package edu.asu.jmars.layer.stamp.focus;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.StringTokenizer;
import java.util.Timer;
import java.util.TimerTask;
import javax.swing.AbstractAction;
import javax.swing.Box;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.MouseInputAdapter;
import edu.asu.jmars.Main;
import edu.asu.jmars.layer.Layer.LView3D;
import edu.asu.jmars.layer.stamp.FilledStamp;
import edu.asu.jmars.layer.stamp.FilledStampImageType;
import edu.asu.jmars.layer.stamp.StampImage;
import edu.asu.jmars.layer.stamp.StampImageFactory;
import edu.asu.jmars.layer.stamp.StampLView;
import edu.asu.jmars.layer.stamp.StampLayer;
import edu.asu.jmars.layer.stamp.StampShape;
import edu.asu.jmars.layer.stamp.StampLayer.StampSelectionListener;
import edu.asu.jmars.layer.stamp.chart.ChartView;
import edu.asu.jmars.layer.stamp.radar.FilledStampRadarTypeFocus;
import edu.asu.jmars.swing.ValidClipboard;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.viz3d.ThreeDManager;
import edu.emory.mathcs.backport.java.util.Collections;


public class FilledStampFocus extends JPanel implements StampSelectionListener {
    protected static final DebugLog log = DebugLog.instance();
    
    final protected StampLView parent;
    final protected StampLayer stampLayer;
    
    protected JList listStamps;
    protected DefaultListModel listModel;
    
    protected JButton btnListCopy;
    protected JButton btnListImport;
    
    private JButton btnRaise;
    private JButton btnLower;
    private JButton btnTop;
    private JButton btnBottom;
    private JButton btnDelete;
    private JButton btnSort;
    private JButton btnDetails;
    private JButton btnPlay;
    
    protected boolean stampListDragging = false;
    
    private final JCheckBox onlyFillSelected;
    private JCheckBox loop;
    
    private int dragStartIndex = -1;
    private int dragCurIndex = -1;
    
    // Examine this very very carefully for race conditions
    boolean modelUpdating = false;
    
	private int startSelectionIndex=-1;
	
	private int lastRow = -1;
	
	//used for building the UI and adding components
    protected int pad = 4;
    protected Insets in = new Insets(pad,pad,pad,pad);
    
    public FilledStampFocus(final StampLView parent) {
        this.parent = parent;
        stampLayer = parent.stampLayer;
        
		parent.stampLayer.addSelectionListener(this);

        listModel = new DefaultListModel();
        listStamps = new JList(listModel);
        listStamps.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);

        listStamps.addListSelectionListener(
            new ListSelectionListener()
            {
                public void valueChanged(ListSelectionEvent e)
                {
                    if (!e.getValueIsAdjusting() &&
                        !stampListDragging)
                    {
                        enableEverything();
                        if (onlyFillSelected.isSelected())
                            redrawTriggered();
                    }
                }
            }
        );

        listStamps.addKeyListener(new ListKeyListener());
        
        MouseInputAdapter mouseListener = new MouseInputAdapter()
        {
            public void mousePressed(MouseEvent e)
            {
                stampListDragging = false;

                if (SwingUtilities.isLeftMouseButton(e)) {
                    dragStartIndex = listStamps.locationToIndex( new Point(e.getX(), e.getY()) );
                }
                
				int clickedRow = listStamps.locationToIndex(e.getPoint());
												
				if (SwingUtilities.isLeftMouseButton(e)) {
					// if this was a double click, pan to the selected stamp.
					if (e.getClickCount() > 1) {
						FilledStamp fs = getFilled(clickedRow);
						if (fs != null) {
							parent.panToStamp(fs.stamp);
						}
					} else {
						FilledStamp fs = getFilled(clickedRow);
						if (fs != null) {					
							StampShape s = fs.stamp;
							
							if (e.isShiftDown()) {
								int lastClicked = startSelectionIndex;
								if (lastClicked<0) {
									lastClicked=startSelectionIndex=clickedRow;
								}
								int start = lastClicked < clickedRow ? lastClicked : clickedRow;
								int end = lastClicked > clickedRow ? lastClicked : clickedRow;
								parent.stampLayer.clearSelectedStamps();

								listStamps.addSelectionInterval(start, end);
								
								for (int i=start; i<=end; i++) {
									parent.stampLayer.addSelectedStamp(getFilled(i).stamp);
								}
							} else if (e.isControlDown()) {
								Object[] obj = listStamps.getSelectedValues();
								boolean othersSelected=false;
								for (int i=0; i<obj.length; i++) {
									FilledStamp fso = (FilledStamp)obj[i];
									if (fso==fs) continue;
									if (fso.stamp==fs.stamp) {
										othersSelected=true;
										break;
									}
								}
								if (!othersSelected) {
									parent.stampLayer.toggleSelectedStamp(s);									
								}
							} else {
								parent.stampLayer.clearSelectedStamps();
								listStamps.setSelectedIndex(clickedRow);								
								parent.stampLayer.addSelectedStamp(s);
							}
						}																				
					}
					
					return;
				} 
			} 
                
            public void mouseDragged(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e)) {
                    int min = listStamps.getMinSelectionIndex();
                    int max = listStamps.getMaxSelectionIndex();
                    
                    if (min >= 0 &&
                        min == max)
                        dragCurIndex = min;
                    else 
                        dragCurIndex = -1;
                    
                    stampListDragging = true;
                }
            }
            
            public void mouseReleased(MouseEvent e)
            {
                if (SwingUtilities.isLeftMouseButton(e) &&
                    dragStartIndex >= 0 &&
                    stampListDragging)
                {
                    int min = listStamps.getMinSelectionIndex();
                    int max = listStamps.getMaxSelectionIndex();
                    boolean redraw = false;
                    
                    if (min < 0) {
                        dragStartIndex = -1;
                        return;
                    }
                    
                    if (!onlyFillSelected.isSelected())
                    {
                        redraw = true;
                    }
                    
                    // Check that this is truly a case of dragging
                    // a single stamp to a new list location; the
                    // current list selection should be the new location
                    // and contain only one selected element.
                    if (min == max &&
                        min != dragStartIndex)
                    {
                        // Move the stamp selected at the start of the drag
                        // motion to the new selected location.
                        listModel.insertElementAt(listModel.remove(dragStartIndex),
                                                  min);
                        listStamps.setSelectedIndex(min);
                    
                        // Need to handle stamp selection change here since; the
                        // normal selection change code has been disabled during 
                        // dragging due to conflicts.
                        parent.stampLayer.clearSelectedStamps();
						listStamps.setSelectedIndex(min);
                        parent.stampLayer.addSelectedStamp(((FilledStamp)listModel.get(min)).stamp);

                        enableEverything();
                        if (redraw ||
                            onlyFillSelected.isSelected())
                            redrawTriggered();
                    }
                    else if (min == max) {
                        // Need to handle possible selection change here.  The
                        // normal selection change code has been disabled above
                        // whenever dragging has started (to prevent conflicts), 
                        // and it is possible to immediately drag a new selection 
                        // without actually dragging to a new location.
                        
                        parent.stampLayer.clearSelectedStamps();
                        listStamps.setSelectedIndex(min);
                        parent.stampLayer.addSelectedStamp(((FilledStamp)listModel.elementAt(listStamps.getSelectedIndex())).stamp);

                        enableEverything();
                        if (onlyFillSelected.isSelected())
                            redrawTriggered();
                    }
                    
                    dragStartIndex = -1;
                    dragCurIndex = -1;
                    stampListDragging = false;
                }
                else if (SwingUtilities.isLeftMouseButton(e)) {
                    dragStartIndex = -1;
                    dragCurIndex = -1;
                }
            }
        };
        listStamps.addMouseListener(mouseListener);
        listStamps.addMouseMotionListener(mouseListener);      
        
        JScrollPane pnlListStamps = new JScrollPane(listStamps, JScrollPane.  VERTICAL_SCROLLBAR_ALWAYS, JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        
        btnRaise = new JButton(
           new AbstractAction("Raise".toUpperCase())
           {
               public void actionPerformed(ActionEvent e)
               {
                   int min = listStamps.getMinSelectionIndex();
                   int max = listStamps.getMaxSelectionIndex();
                   if (min == -1  ||  min == 0)
                       return;
                   boolean redraw = false;
                   if (!onlyFillSelected.isSelected())
                   {
                       redraw = true;
                   }
                   
                   // Swap the selection range and the item before it
                   listModel.insertElementAt(listModel.remove(min-1),
                                             max);
                   listStamps.setSelectionInterval(min-1, max-1);
                   
                   clearClipAreas();
                   
                   if (redraw)
                       redrawTriggered();
               }
           }
        );
        btnRaise.setToolTipText("Move the currently selected stamp(s) UP" +
                                " in the filled-stamps list.");
        
        btnLower = new JButton(
           new AbstractAction("Lower".toUpperCase())
           {
               public void actionPerformed(ActionEvent e)
               {
                   int min = listStamps.getMinSelectionIndex();
                   int max = listStamps.getMaxSelectionIndex();
                   if (max == -1  ||  max == listModel.getSize()-1)
                       return;
                   boolean redraw = false;
                   if (!onlyFillSelected.isSelected())
                   {
                       redraw = true;
                   }
                   
                   // Swap the selection range and the item after it
                   listModel.insertElementAt(listModel.remove(max+1),
                                             min);
                   listStamps.setSelectionInterval(min+1, max+1);
                   
                   clearClipAreas();
                   
                   if (redraw)
                       redrawTriggered();
               }
           }
        );
        btnLower.setToolTipText("Move the currently selected stamp(s) DOWN" +
                                " in the filled-stamps list.");
        
        btnTop = new JButton("Top".toUpperCase());
        
        btnTop.addActionListener(new ActionListener() {		
            public void actionPerformed(ActionEvent e)
            {
                int min = listStamps.getMinSelectionIndex();
                int max = listStamps.getMaxSelectionIndex();
                if (min == -1  ||  min == 0)
                    return;
                
                modelUpdating=true;
                boolean redraw = false;
                if (!onlyFillSelected.isSelected())
                {
                    redraw = true;
                }
                
                // Move selection range to top of list.
                for (int i=min; i <= max; i++) {
                    FilledStamp fs = (FilledStamp) listModel.remove(max);
                    listModel.insertElementAt(fs, 0);
                }
                listStamps.setSelectionInterval(0, max-min);
                modelUpdating=false;
                
                clearClipAreas();
                
                if (redraw)
                    redrawTriggered();
            }
		});
        
        btnTop.setToolTipText("Move the currently selected stamp(s) to TOP" +
                              " of the filled-stamps list.");
        
        btnBottom = new JButton(
            new AbstractAction("Bottom".toUpperCase())
            {
                public void actionPerformed(ActionEvent e)
                {
                    int min = listStamps.getMinSelectionIndex();
                    int max = listStamps.getMaxSelectionIndex();
                    if (max == -1  ||  max == listModel.getSize()-1)
                        return;
                    boolean redraw = false;
                    if (!onlyFillSelected.isSelected())
                    {
                       redraw = true;
                    }
                    
                    // Move selection range to bottom of list.
                    for (int i=min; i <= max; i++)
                        listModel.insertElementAt(listModel.remove(min), listModel.getSize());
                    listStamps.setSelectionInterval(listModel.getSize() - (max-min) - 1, 
                                                    listModel.getSize()-1);
                    
                    clearClipAreas();
                    
                    if (redraw)
                        redrawTriggered();
                }
            }
        );
        btnBottom.setToolTipText("Move the currently selected stamp(s) to BOTTOM" +
                                 " of the filled-stamps list.");

        btnDetails = new JButton(
                new AbstractAction("View Details".toUpperCase())
                {
                    public void actionPerformed(ActionEvent e)
                    {
                        int[] selected = listStamps.getSelectedIndices();

                        int selIndex = selected[0];
                                                
                        FilledStamp fs = (FilledStamp)listModel.get(selIndex);

                        HashMap<String,String> params = fs.pdsi.getProjectionParams();
                        
                        ArrayList<String> displayStrings = new ArrayList<String>();
                        
                        for (String key: params.keySet()) {
                        	displayStrings.add(key + " : " + params.get(key));
                        }
                        
                        Collections.sort(displayStrings);
                        
                        JList<String> paramList = new JList<String>(displayStrings.toArray(new String[0]));
                        
                        Util.showMessageDialogObj(paramList, "Product Parameters", JOptionPane.INFORMATION_MESSAGE);                        
                    }
                }
            );
            btnDetails.setToolTipText("Display processing and projection details for the selected rendered item");

        
        loop = new JCheckBox("Loop");
        btnPlay = new JButton(
                new AbstractAction("Play".toUpperCase())
                {
                    public void actionPerformed(ActionEvent e)
                    {
                    	btnPlay.setSelected(!btnPlay.isSelected());
                    	
                    	TimerTask timerTask = new TimerTask() {	
                 			public void run() {
                 				// Wait until the layer settles before performing the next task
                 				if (stampLayer.hasActiveTasks()) return;
                 				if (btnPlay.isSelected()) {
                 					btnPlay.setText("Pause".toUpperCase());
                                    if (!onlyFillSelected.isSelected()){
                                       onlyFillSelected.setSelected(true);
                                    }

                                    stampLayer.getSettings().setRenderSelectedOnly(onlyFillSelected.isSelected());
                                    
                                    int min = listStamps.getMinSelectionIndex();
                                    
                                    if (loop.isSelected()){
	                                    if (listModel.getSize()<=min+1) {
	                                    	listStamps.setSelectionInterval(0, 0);
	                                    } else {
	                                    	listStamps.setSelectionInterval(min+1, min+1);
	                                    }
                                    }
                                    if (!loop.isSelected()){
                                    	if (listModel.getSize()<=min+1){
                                    		cancel();
                                    		btnPlay.setText("Play".toUpperCase());
                                    	} else {
                                    		listStamps.setSelectionInterval(min+1, min+1);
                                    	}
                                    }
                                           
                                    redrawTriggered();                 
                                    
                 				} else {
                 					btnPlay.setText("Play".toUpperCase());
                 					cancel();
                 				}
                			}			
                		};
                    	
                    	Timer playTimer=new Timer();
                    	playTimer.schedule(timerTask, 1000, 1000);
                    }
                }
            );
         btnPlay.setToolTipText("Display each image in sequence, slide-show style.");
            
        
        btnDelete = new JButton(
            new AbstractAction("Delete".toUpperCase())
            {
                public void actionPerformed(ActionEvent e)
                {
                    Object[] selected = listStamps.getSelectedValues();
                    for (int i=0; i<selected.length; i++)
                    {
                        listModel.removeElement(selected[i]);
                    }
                    
                    clearClipAreas();
                    
                    enableEverything();
                    
                    redrawTriggered();
                }
            }
        );
        btnDelete.setToolTipText("Remove the currently selected(s) stamp" +
                                 " from the filled-stamps list.");
        
        btnSort = new JButton(
            new AbstractAction("Left Sort".toUpperCase())
            {
                public void actionPerformed(ActionEvent e)
                {
                    // Sort the filled stamps according to the 
                    // West-to-East equator-intercept order, i.e.,
                    // by longitude of stamp's corresponding
                    // orbit track's intercept with the equator.
                    FilledStamp[] filled = new FilledStamp[listModel.size()];
                    for (int i=0; i < filled.length; i++)
                        filled[i] = (FilledStamp)listModel.get(i);
                    FilledStamp[] sorted = orbitTrackSort(filled);

                    // Check whether the stamp order has changed
                    // as a result of the sort.
                    if (!Arrays.equals(filled, sorted))
                    {
                        // Move stamps in list to match sorted order.
                        for (int i = 0; i < sorted.length; i++)
                            listModel.set(i, sorted[i]);
                        listStamps.clearSelection();    
                        selectionsChanged();
                        
                        clearClipAreas();
						enableEverything();
						redrawTriggered();
                    }
                }
            }
        );
        btnSort.setToolTipText("Sorts all filled-stamps in order of " +
                               "leftmost orbit track.");

        
        // This button seems to have been removed from the interface at
        // some point... and no one has complained.  This functionality
        // should be added back at some point, but in a more generic fashion
        // that works for any stamp on any instrument.
//        btnViewPds = new JButton(
//             new AbstractAction("View PDS Label")
//             {
//                 public void actionPerformed(ActionEvent e)
//                 {
//                     FilledStamp fs = getFilledSingle();
//                     JFrame frame = new JFrame(fs.stamp.getId());
//                     JTextArea txt = new JTextArea(fs.pdsi.getLabel(), 24, 0);
//                     frame.getContentPane().add(new JScrollPane(txt));
//                     frame.pack();
//                     frame.setVisible(true);
//                 }
//             }
//        );
                
        btnListCopy = new JButton(
    		new AbstractAction("Copy".toUpperCase()) {
    			public void actionPerformed(ActionEvent e) {
    				StringBuffer buf = new StringBuffer();
    				for (int i = 0; i < listModel.getSize(); i++) {
    					buf.append( ((FilledStamp)listModel.get(i)).stamp.getId());
    					buf.append(' ');
    				}
    				StringSelection sel = new StringSelection(buf.toString());
    				Clipboard clipboard = ValidClipboard.getValidClipboard();
    				if (clipboard == null) {
    					log.aprintln("no clipboard available");
    				} else {
    					clipboard.setContents(sel, sel);
    					Main.setStatus("Stamp list copied to clipboard");
    					log.println("stamp list copied: " + buf.toString());
    				}
    			}
    		}
        );
		btnListCopy.setToolTipText("Copy stamp IDs to the clipboard");
		
		btnListImport = new JButton(
			new AbstractAction("Import...".toUpperCase())
			{
				public void actionPerformed(ActionEvent e)
				{
					new ImportStampsDialog(FilledStampFocus.this).show();
				}
			}
		);
		btnListImport.setToolTipText("Import list of stamps to render from a file, one ID per line");
        
        onlyFillSelected = new JCheckBox("Render selections only");
        onlyFillSelected.addActionListener(
             new ActionListener()
             {
                 public void actionPerformed(ActionEvent e)
                 {
     				 stampLayer.getSettings().setRenderSelectedOnly(onlyFillSelected.isSelected());
                	 clearClipAreas();
                     redrawTriggered();
                 }
             }
        );
        
        onlyFillSelected.setSelected(stampLayer.getSettings().renderSelectedOnly());
        

        
        JCheckBox hideOutlines = new JCheckBox() {
        	{
        		setText("Hide stamp outlines");
        		addActionListener(new ActionListener() {
        			public void actionPerformed(ActionEvent e) {
        				stampLayer.getSettings().setHideOutlines(isSelected());
        				
        		        parent.drawOutlines();
        		        
        		        StampLView childView = (StampLView)parent.getChild();
        		        if (childView != null) {
            		        childView.drawOutlines();
        		        }
        			}
        		});
        	}
        };
        hideOutlines.setSelected(stampLayer.getSettings().hideOutlines());
        
        
        Box row1 = Box.createHorizontalBox();
        if (!stampLayer.getInstrument().equalsIgnoreCase("davinci")) {
        	row1.add(btnListImport);
            row1.add(Box.createHorizontalStrut(pad));
        }
        
        row1.add(btnListCopy);
        row1.add(Box.createHorizontalStrut(pad));
        row1.add(btnSort);
        
        Box row2 = Box.createHorizontalBox();
        if (!stampLayer.globalShapes()) {
        	row2.add(hideOutlines);
        }
        row2.add(Box.createHorizontalStrut(pad));
        row2.add(onlyFillSelected);
        
        if (stampLayer.globalShapes()) {
        	onlyFillSelected.setSelected(true);
        	onlyFillSelected.setEnabled(false);
        	stampLayer.getSettings().setRenderSelectedOnly(true);
        }
        
        JPanel listPanel = new JPanel(new GridBagLayout());
        listPanel.setBorder(new TitledBorder("Rendered Stamps"));
        int row = 0;
        listPanel.add(row1, new GridBagConstraints(0,row,2,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        row++;
        listPanel.add(row2, new GridBagConstraints(0,row,2,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        row++;
        listPanel.add(pnlListStamps, new GridBagConstraints(1,row,1,5,1,1,GridBagConstraints.NORTHWEST,GridBagConstraints.BOTH,in,pad,pad));
        listPanel.add(btnDelete, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        row++;
        listPanel.add(btnTop, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        row++;
        listPanel.add(btnRaise, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        row++;
        listPanel.add(btnLower, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        row++;
        listPanel.add(btnBottom, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        row++;
        listPanel.add(btnDetails, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        row++;
        listPanel.add(btnPlay, new GridBagConstraints(0,row,1,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        listPanel.add(loop, new GridBagConstraints(1,row,2,1,0,0,GridBagConstraints.NORTHWEST,GridBagConstraints.HORIZONTAL,in,pad,pad));
        row++;
        

        // Assemble everything together
        setLayout(new GridBagLayout());
        add(listPanel, new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.NORTHWEST,GridBagConstraints.BOTH,in,pad,pad));
        
        // Set proper state
        enableEverything();
    }
	
	
	public void selectionsChanged() {
        List<StampShape> selectedStamps = stampLayer.getSelectedStamps();
        
        // We want to know what was selected at the start of this process, since it
        // can and will change as we're processing
		Object[] obj=listStamps.getSelectedValues();

        Enumeration listElements=listModel.elements();
        
        modelUpdating=true;
        try {
    		while(listElements.hasMoreElements()) {
    			FilledStamp fs = (FilledStamp)listElements.nextElement();
    			if (selectedStamps.contains(fs.stamp)) {
    				boolean alreadySelected=false;
    				
    				for (int i=0; i<obj.length; i++) {
    					FilledStamp fso = (FilledStamp)obj[i];
    					if (fso.stamp==fs.stamp) {
    						alreadySelected=true;
    						break;
    					}
    				}
    				if (!alreadySelected) {
    					int idx =listModel.indexOf(fs);
    					if (idx>-1) {
    						listStamps.addSelectionInterval(idx, idx);
    					}
    				}
    			} else {
    				int idx =listModel.indexOf(fs);
    				if (idx>-1) {
    					listStamps.removeSelectionInterval(idx, idx);
    				}
    			}
    		}
        } finally {
    		modelUpdating=false;
        }
        
		redrawTriggered();
	}
	
	public void selectionsAdded(java.util.List<StampShape> newStamps) {
        Enumeration listElements=listModel.elements();

        // We want to know what was selected at the start of this process, since it
        // can and will change as we're processing
		Object[] obj=listStamps.getSelectedValues();
        
		while(listElements.hasMoreElements()) {
			FilledStamp fs = (FilledStamp)listElements.nextElement();
			if (newStamps.contains(fs.stamp)) {
				boolean alreadySelected=false;
				
				for (int i=0; i<obj.length; i++) {
					FilledStamp fso = (FilledStamp)obj[i];
					if (fso.stamp==fs.stamp) {
						alreadySelected=true;
						break;
					}
				}
				if (!alreadySelected) {
					int idx =listModel.indexOf(fs);
					if (idx>-1) {
						listStamps.addSelectionInterval(idx, idx);
					}
				}
			}
		}
	}
    

	
	protected class ListKeyListener extends KeyAdapter {

		public void keyReleased(KeyEvent e) {
			if (e.isShiftDown()==false) {
				startSelectionIndex=-1;
				lastRow=-1;
			}
		}

		public void keyPressed(KeyEvent e) {
			// Do not override the standard copy/paste functionality
			if (e.isControlDown()) {
				super.keyPressed(e);
				return;
			}

			// Override standard selection, so we can make it work the way we want it to
			e.consume();
			
			if (e.isShiftDown()) {
				handleShiftClick(e);
				return;
			}
			
			int lastClicked = listStamps.getSelectionModel().getMinSelectionIndex();
						
			if (e.isShiftDown()) {
				if (startSelectionIndex==-1) {
					startSelectionIndex=lastClicked;
				}
			}
			
			int newRow;
			if (e.getKeyCode()==KeyEvent.VK_UP) {
				newRow=lastClicked-1;
			} else if (e.getKeyCode()==KeyEvent.VK_DOWN) {
				newRow=lastClicked+1;
			} else {
				return; 
			}
			
			if (newRow>=listStamps.getModel().getSize() || newRow<0) { 
				return;
			}
			
			FilledStamp fs = (FilledStamp)listStamps.getModel().getElementAt(newRow);

			if (listStamps.getSelectedValues().length==1) {
				FilledStamp oldSelection = (FilledStamp)listStamps.getSelectedValue();
				if (oldSelection.stamp == fs.stamp) {
					listStamps.setSelectedIndex(newRow);
					return;
				}
			}
			
			modelUpdating=true;
			stampLayer.clearSelectedStamps();
            listStamps.setSelectedIndex(newRow);
            stampLayer.addSelectedStamp(fs.stamp);
//			stampLayer.toggleSelectedStamp(fs.stamp);
			modelUpdating=false;
			redrawTriggered();
		}
		
		
		private void handleShiftClick(KeyEvent e) {
			if (lastRow==-1) {
				if (e.getKeyCode()==KeyEvent.VK_UP) {
					lastRow = listStamps.getSelectionModel().getMinSelectionIndex();
				} else if (e.getKeyCode()==KeyEvent.VK_DOWN) {
					lastRow = listStamps.getSelectionModel().getMaxSelectionIndex();
				} else {
					return; 
				}
			}
			
			int newRow;
			if (e.getKeyCode()==KeyEvent.VK_UP) {
				newRow=lastRow-1;
			} else if (e.getKeyCode()==KeyEvent.VK_DOWN) {
				newRow=lastRow+1;
			} else {
				return; 
			}
			
			if (startSelectionIndex==-1) {
				startSelectionIndex=lastRow;
			}

			FilledStamp fs;
			
			// If we are reducing the number of rows selected
			if ((e.getKeyCode()==KeyEvent.VK_UP && newRow>=startSelectionIndex)
					|| (e.getKeyCode()==KeyEvent.VK_DOWN && newRow<=startSelectionIndex))
			{
				fs = (FilledStamp)listStamps.getModel().getElementAt(lastRow);
			} else {
				if (newRow>=listStamps.getModel().getSize() || newRow<0) { 
					return;
				}

				fs = (FilledStamp)listStamps.getModel().getElementAt(newRow);
			}				
					
			listStamps.setSelectionInterval(startSelectionIndex, newRow);

			if (!othersSelected(fs)) {
				stampLayer.toggleSelectedStamp(fs.stamp);									
			} 	
			lastRow=newRow;
		}
		
	}
	
	private boolean othersSelected(FilledStamp fs) {
		Object[] obj = listStamps.getSelectedValues();
		boolean othersSelected=false;
		for (int i=0; i<obj.length; i++) {
			FilledStamp fso = (FilledStamp)obj[i];
			if (fso==fs) continue;
			if (fso.stamp==fs.stamp) {
				othersSelected=true;
				break;
			}
		}
		
		return othersSelected;
	}
	
	protected void clearClipAreas() {
		List<FilledStamp> filledStamps = getFilled();
		
		for (FilledStamp fs : filledStamps) {			
			fs.pdsi.clearCurrentClip();
		}		
		
		parent.clearLastFilled();
	}
	


	/**
	 * Creates a sorted list from the specified stamps. The sort order is
	 * according to each stamp's average longitude, calculated by averaging together
	 * each of the points in the stamp's polygon.
	 * Stamps are ordered from west-to-east.
	 * 
	 * @param unsorted
	 *            List of unsorted stamps
	 */
    protected FilledStamp[] orbitTrackSort(FilledStamp[] unsorted) {

    	if (unsorted == null)
    		return null;

    	final Map<FilledStamp, Double> averageLon = new HashMap<FilledStamp, Double>();
    	for (FilledStamp s : unsorted) {
    		averageLon.put(s, s.stamp.getCenter().getX());
    	}

    	FilledStamp[] sorted = (FilledStamp[]) unsorted.clone();
    	Arrays.sort(sorted, new Comparator<FilledStamp>() {
    		public int compare(FilledStamp a, FilledStamp b) {
    			double diff = averageLon.get(a).doubleValue() - averageLon.get(b).doubleValue();
    			if (diff > 0)
    				return -1;
    			else if (diff < 0)
    				return 1;
    			else
    				return 0;
    		}
    	});

    	return sorted;
    }
            
    /**
     ** Returns list of filled stamp states for saving session settings
     **/
    public FilledStamp.State[] getStampStateList()
    {
    	List<FilledStamp> filledStamps = getFilled();
    	
        FilledStamp.State[] stateList = new FilledStamp.State[filledStamps.size()];
        
        int cnt=0;
        for (FilledStamp fs : filledStamps) {
           stateList[cnt++] = fs.getState();
        }
        
        return stateList;
    }
                    
    protected void enableEverything()
    {
        int[] selected = listStamps.getSelectedIndices();
        boolean anySelected = !listStamps.isSelectionEmpty();
        boolean rangeSelected = isContiguous(selected);
        
        btnRaise.setEnabled(rangeSelected);
        btnLower.setEnabled(rangeSelected);
        btnTop.setEnabled(rangeSelected);
        btnBottom.setEnabled(rangeSelected);
        btnDelete.setEnabled(anySelected);
        btnPlay.setEnabled(anySelected);
        loop.setEnabled(anySelected);
        btnSort.setEnabled(listModel.size() > 0);
        
        if (listModel.getSize() > 0)
            btnListCopy.setEnabled(true);
        else
            btnListCopy.setEnabled(false);

        btnDetails.setEnabled(selected.length==1);

    }
    
    private boolean isContiguous(int[] values)
    {
        return  values.length != 0
                &&  values[values.length-1] == values[0] + values.length-1;
    }

    
    public List<FilledStamp> getFilledSelections()
    {
        ArrayList<FilledStamp> filledSelected = new ArrayList<FilledStamp>();

        for (Object obj : listStamps.getSelectedValues()) {
        	filledSelected.add((FilledStamp)obj);
        }
        
        return  filledSelected;
    }
    
    protected FilledStamp getFilledSingle()
    {
        Object[] selected = listStamps.getSelectedValues();
        if (selected.length != 1)
            return  null;
        return (FilledStamp)selected[0];
    }
    
    protected FilledStamp getFilled(int n)
    {
        return  (FilledStamp) listModel.get(n);
    }
    
    public List<FilledStamp> getFilled() {
    	ArrayList<FilledStamp> filledStamps = new ArrayList<FilledStamp>();
    	    	
    	Enumeration stamps = listModel.elements();

    	while (stamps.hasMoreElements()) {
    		FilledStamp fs = (FilledStamp) stamps.nextElement();
    		filledStamps.add(fs);
    	}
  
    	return filledStamps;
    }
    
    /**
     * This will return a {@link FilledStampImageType} object by default.
     * The {@link FilledStampRadarTypeFocus} should override this method and return
     * a {@link FilledStampRadarType} object.
     ** @param state optional position offset and color map state settings
     ** used to restore FilledStamp state; may be null.
     **/
    protected FilledStamp getFilled(StampShape s, FilledStamp.State state, String type) {
    	
    	if (type==null && state!=null) {
    		type = state.getImagetype();
    	}
    	
        StampImage pdsi = StampImageFactory.load(s, stampLayer.getSettings().getInstrument(), type);
            
        if (pdsi == null)
           return  null;
        
        //return FilledStampImageType by default    
        return new FilledStampImageType(s, pdsi, state);
    }
    
    public void addStamp(StampShape s, String type)
    {
        addStamp(s, null, true, false, false, type);
    }
    
    /**
     *  @param state optional position offset and color map state settings
     *  used to restore FilledStamp state; may be null.
     *
     *  @param redraw       render/draw any image that has been selected.
     *
     */
    protected void addStamp(final StampShape s, FilledStamp.State state, boolean redraw, 
                            boolean ignoreAlreadyFilled, boolean ignoreNotLoaded, String type)
    {
    	Enumeration stamps = listModel.elements();
    
    	while (stamps.hasMoreElements()) {
    		FilledStamp stamp = (FilledStamp)stamps.nextElement();
    		if (stamp.stamp==s && stamp.pdsi.getImageType().equalsIgnoreCase(type)) {
                if (!ignoreAlreadyFilled)
                    Util.showMessageDialog("Already in filled-stamp list: " + s,
                                                  "OPERATION IGNORED",
                                                  JOptionPane.WARNING_MESSAGE);
                return;        			
    		}
    	}

    	// Do we really still need this logic?
    	if (type==null && stampLayer.getInstrument().equalsIgnoreCase("THEMIS")) {
    		if (s.getId().startsWith("I")) type="BTR"; else type="ABR";
    	}
    	
        FilledStamp fs = getFilled(s, state, type);
        
        if (fs == null ||
                fs.pdsi == null)
        {
            if (!ignoreNotLoaded)
                Util.showMessageDialog("Unable to load " + s,
                                              "PDS LOAD ERROR",
                                              JOptionPane.ERROR_MESSAGE);
            return;
        }
        
        listModel.insertElementAt(fs, 0);

        listStamps.addSelectionInterval(0, 0);
        
        if (!stampLayer.isSelected(s)) {
        	stampLayer.addSelectedStamp(s);
        }
        
        ChartView myChart = parent.myFocus.chartView;
        if (myChart!=null) {
        	myChart.mapChanged();
        }
        
        enableEverything();
        
        if (redraw) {
        	redrawTriggered();
        }
    }

    // This is called from the DavinciFocusPanel when new stamps are received from Davinci
    protected void addStamp(FilledStamp fs, FilledStamp.State state, boolean redraw, 
            boolean ignoreAlreadyFilled, boolean ignoreNotLoaded)
	{
		if (fs == null ||
		fs.pdsi == null)
		{
		if (!ignoreNotLoaded)
		Util.showMessageDialog("Unable to load " + fs.stamp,
		                              "PDS LOAD ERROR",
		                              JOptionPane.ERROR_MESSAGE);
		return;
		}
		
		String id=fs.stamp.getId();

		List<FilledStamp> stamps=getFilled();
		
		for (FilledStamp s : stamps) {
			if (s.stamp.getId().equalsIgnoreCase(id)) {
				listModel.removeElement(s);
				clearClipAreas();
			}
		}
		
		listModel.insertElementAt(fs, 0);
		
		listStamps.addSelectionInterval(0, 0);
		
		if (!stampLayer.isSelected(fs.stamp)) {
			stampLayer.addSelectedStamp(fs.stamp);
		}
		
		ChartView myChart = parent.myFocus.chartView;
		if (myChart!=null) {
		myChart.mapChanged();
		}
		
		enableEverything();
		
		if (redraw) {
		redrawTriggered();
		}
	}
    
    
    
    /**
     * Adds any stamps from file that are found in the associated
     * layer for this panel's stamp view.  File must contain
     * list of stamp IDs delimited by whitespace (includes newlines).
     */
    protected void addStamps(File file)
    {
        try {
            if (file != null)
            {
               BufferedReader reader = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
               
               // Build list of stamps from file that are present in this
               // panel's stamp view's layer.
               ArrayList<StampShape> stampList = new ArrayList<StampShape>();
               String line = reader.readLine();
               while (line != null) {
                   StringTokenizer tokenizer = new StringTokenizer(line);
                   while (tokenizer.hasMoreTokens()) {
                       String stampID = tokenizer.nextToken();
                       StampShape stamp = stampLayer.getStamp(stampID);
                       
                       if (stamp != null) {
                           log.println("adding stamp " + stampID);
                           stampList.add(stamp);
                       }
                   }
                   
                   line = reader.readLine();
               }
               
               // Add stamps to rendered list as a group (improves user
               // experience during the loading, projecting, ... phases).
               StampShape[] stampsToAdd = (StampShape[]) stampList.toArray(new StampShape[0]);
               addStamps(stampsToAdd, null, null);
               // do we need to trigger StampLView.drawStampsTogether?
            }
        }
        catch (Exception e) {
            log.aprintln(e);
        }
        stampLayer.increaseStateId(StampLayer.IMAGES_BUFFER);
    }
       

        
    /**
     ** Indicates that the user changed the drawing parameters of a
     ** single image. Default implementation simply invokes a complete
     ** redraw.
     **/
    protected void performRedrawSingle(FilledStamp changed)
    {
        redrawTriggered();
    }
    
    protected void redrawTriggered()
    {
    	if (modelUpdating) {
    		return;
    	} 
    	
        stampLayer.increaseStateId(StampLayer.IMAGES_BUFFER);

		//update the 3d view if has lview3d enabled
  		LView3D view3d = stampLayer.viewToUpdate.getLView3D();
   		if(view3d.isEnabled()){
   			if (ThreeDManager.isReady()) {
	   			ThreeDManager mgr = ThreeDManager.getInstance();
	   			//	If the 3d is already visible, update it
	   			if(view3d.isVisible()){
	   				mgr.updateDecalsForLView(stampLayer.viewToUpdate, true);
	   			}
   			}
   		}
        
        parent.redrawEverything(true);
    }
    
    public Dimension getMinimumSize()
    {
        return  getPreferredSize();
    }
        
    public void addStamps(final StampShape stampsToAdd[], FilledStamp.State states[], String types[])
		{
    		// Do some data sanitizing on the input parameters first, to keep us out of trouble later
    		// We perform this logic here to hopefully save ourselves having to do this everywhere else.
    	
    		if (states==null) {
    			states = new FilledStamp.State[stampsToAdd.length];
    		}
    		
    		if (types==null) {
    			types = new String[stampsToAdd.length];
    			
    			for (int i=0; i<stampsToAdd.length; i++) {
    				if (states[i]==null) {
    					types[i]=null;
    				} else {
    					types[i]=states[i].getImagetype();    				
    				}
    			}
    			
    		}
    	
			Enumeration stamps = listModel.elements();
						
			while (stamps.hasMoreElements()) {
				FilledStamp stamp = (FilledStamp)stamps.nextElement();
				
				for (int i=0; i<stampsToAdd.length; i++) {
					if (stamp.stamp==stampsToAdd[i] && stamp.pdsi.getImageType().equalsIgnoreCase(types[i])) {
						// Already in the stamp list, skip to the next one
						stampsToAdd[i]=null; // Don't add this one again;
						continue;        			
					}				
				}
			}
	
			
			int cnt=0;
			ArrayList<StampShape> addedStamps = new ArrayList<StampShape>();
	
			modelUpdating=true;
			for (int i=0; i<stampsToAdd.length; i++) {	
				if (stampsToAdd[i]==null) { 
					// This can happen when a user selects a group of THEMIS IR+VIS images and then 
					// chooses 'Render as BTR' or 'Render as ABR'
					continue;  
				}
				
			
				if (types[i]==null) {
					// Do we really need to handle BTR/ABR so different?
					if (stampLayer.getInstrument().equalsIgnoreCase("THEMIS")) {
						if (stampsToAdd[i].getId().startsWith("I"))  {
							types[i]="BTR"; }
						else {
							types[i]="ABR";
						}
					} else {
						types[i]=stampLayer.getInstrument().toUpperCase();
					}
				}
			
				FilledStamp fs = getFilled(stampsToAdd[i], states[i], types[i]);
				
				if (fs == null || fs.pdsi == null)
				{
	//				Util.showMessageDialog(this,
	//				                              "Unable to load " + stampsToAdd[i],
	//				                              "PDS LOAD ERROR",
	//				                              JOptionPane.ERROR_MESSAGE);
					continue;
				}
						       
				listModel.insertElementAt(fs, 0);
				cnt++;
			}			
			
			if (cnt>0) {
				listStamps.addSelectionInterval(0, cnt-1);
			}
				
			stampLayer.addSelectedStamps(addedStamps);
					
			modelUpdating=false;
			
	        ChartView myChart = parent.myFocus.chartView;
	        if (myChart!=null) {
	        	myChart.mapChanged();
	        }
			
            stampLayer.increaseStateId(StampLayer.IMAGES_BUFFER);

			enableEverything();		
		}


	
	public void dispose() {
        listModel.clear();
	}
}


