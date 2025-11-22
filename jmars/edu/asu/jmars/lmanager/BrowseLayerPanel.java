package edu.asu.jmars.lmanager;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.BoxLayout;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTaskPane;

import edu.asu.jmars.swing.ImportantMessagePanel;

public class BrowseLayerPanel extends JPanel {

    private JLabel selectCatLbl = null;
    private JComboBox<String> catCB = null;
    private JComboBox<String> subcatCB = null;
    private JPanel mainResultsPanel = null;
    private JPanel resultsPanel = null;
    private JScrollPane resultsSP = null;
    private ImportantMessagePanel selectSubcatMsgPnl = null;
    private JPanel subcatMsgMainPanel = null;
    private JPanel subcatListPanel = null;
    private JLabel subcatsAvailableLbl = null;
    private boolean resultsShowing = true;
    private GroupLayout mainResultsPanelGroup = null;
//    private HashMap<String,ArrayList<String>> subcatsByCat = new HashMap<String, ArrayList<String>>();
    private SearchProvider searchProvider = SearchProvider.getInstance();
    private ArrayList<SearchResultRow> resultRows = new ArrayList<SearchResultRow>();
    
    public BrowseLayerPanel() {
        selectCatLbl = new JLabel("Select Category:");
        catCB = new JComboBox<String>();
        subcatCB = new JComboBox<String>();
        mainResultsPanel = new JPanel();
        resultsSP = new JScrollPane();
        resultsSP.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        resultsSP.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        selectSubcatMsgPnl = new ImportantMessagePanel("Please select a subcategory from the list.");
        resultsPanel = new JPanel();//for topics and results
        subcatMsgMainPanel = new JPanel();//panel for showing subcat message and list of subcats
        subcatListPanel = new JPanel();
        subcatsAvailableLbl = new JLabel("Subcategories available: ");
    }
    
    public void buildGUI() {
        layoutGUI();
        populateCat();
        setupActions();
        updateResults();

    }
    
    private void setupActions() {
        catCB.addActionListener(new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                updateSubcats();
            }
        });
        subcatCB.addActionListener(new AbstractAction() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                if (subcatCB.getSelectedIndex() > 0) {
                    if (!resultsShowing) {
                        mainResultsPanelGroup.replace(subcatMsgMainPanel, resultsPanel);
                        resultsShowing = true;
                    }
                    resultRows.clear();
                    resultsPanel.removeAll();
                    resultsPanel.add(new JLabel("Working...."));
                    resultsPanel.repaint();
                    updateResults();
                } else {
                    String cat = (String)catCB.getSelectedItem();
                    ArrayList<String> subcats = searchProvider.getSubcategories(cat);
                    if (subcats != null && subcats.size() > 0) {
                        if (resultsShowing) {
                            resultsPanel.removeAll();
                            mainResultsPanelGroup.replace(resultsPanel,subcatMsgMainPanel);
                            resultsShowing = false;
                        }
                        subcatListPanel.removeAll();
                        for (String sc : subcats) {
                            subcatListPanel.add(new JLabel(sc));
                        }
                    } 
                }
                
            }
        });
    }
    private void updateSubcats() {
        subcatCB.removeAllItems();
        subcatCB.addItem("Subcategories");
        
        String cat = (String)catCB.getSelectedItem();
        ArrayList<String> subcats = searchProvider.getSubcategories(cat);
        
        if (subcats.size() > 0) {
            subcatCB.setEnabled(true);
            for (String sub : subcats) {
                subcatCB.addItem(sub);
            }
            subcatCB.setSelectedIndex(0);
        } else {
            subcatCB.setEnabled(false);
            if (!resultsShowing) {
                mainResultsPanelGroup.replace(subcatMsgMainPanel, resultsPanel);
                resultsShowing = true;
            }
            resultRows.clear();
            resultsPanel.removeAll();
            resultsPanel.add(new JLabel("Working...."));
            resultsPanel.repaint();
            updateResults();
        }
        
    }
    private void updateResults() {
        String cat = (String)catCB.getSelectedItem();
        String subcat = (String) subcatCB.getSelectedItem();
       
        resultRows.clear();   
        if ("home".equalsIgnoreCase(cat)) {
            resultRows.addAll(searchProvider.buildInitialLayerList(false, false));
        } else {
            if (subcat == null || subcat.equals("Subcategories")) {
                subcat = null;
            }
            resultRows.addAll(searchProvider.getLayersByTopic(cat, subcat));
        }

        displayResults();
        mainResultsPanel.repaint();
    }
    private void displayResults() {
        resultsPanel.removeAll();
        if (resultRows.size() == 0) {
            resultsPanel.add(new JLabel("No results to display."));
        } else {
            HashMap<String, JXTaskPane> taskPanesByTopic = new HashMap<String, JXTaskPane>();
            HashMap<String, Integer> countByTopic = new HashMap<String, Integer>();
            
            
            
            String cat = (String)catCB.getSelectedItem();
            String subcat = (String) subcatCB.getSelectedItem();
            ArrayList<String> names = new ArrayList<String>();
            
            for (SearchResultRow row : resultRows) {
                if (subcat == null || subcat.equals("Subcategories")) {
                    subcat = null;
                }
                
                String topic = row.getTopic(cat, subcat);
                JXTaskPane tp = taskPanesByTopic.get(topic);
                Integer count = countByTopic.get(topic);
                if (tp == null) {
                    tp = new JXTaskPane();
                    tp.setTitle(topic);
                    taskPanesByTopic.put(topic,tp);
                    names.add(topic);
                    count = 0;
                }
                count++;
                countByTopic.put(topic, count);
                tp.add(row);
            }
            
            boolean first = true;
            for(String name : names){
                JXTaskPane tp = taskPanesByTopic.get(name);
                resultsPanel.add(tp);
                tp.setTitle(tp.getTitle()+" ("+countByTopic.get(tp.getTitle())+")");
                if (first || "home".equalsIgnoreCase(cat)) {
                    tp.setCollapsed(false);
                    first = false;
                } else {
                    tp.setCollapsed(true);
                }
            }
        }
        resultsPanel.revalidate();
        resultsPanel.repaint();
    }
    private void layoutGUI() {
        Dimension subcatMsgDim = new Dimension(500,40);
        selectSubcatMsgPnl.setMaximumSize(subcatMsgDim);
        selectSubcatMsgPnl.setMinimumSize(subcatMsgDim);
        selectSubcatMsgPnl.setPreferredSize(subcatMsgDim);
        
        subcatListPanel.setLayout(new BoxLayout(subcatListPanel, BoxLayout.Y_AXIS));
        subcatMsgMainPanel.setLayout(new BoxLayout(subcatMsgMainPanel, BoxLayout.Y_AXIS));
        subcatMsgMainPanel.add(selectSubcatMsgPnl);
        subcatMsgMainPanel.add(subcatsAvailableLbl);
        subcatMsgMainPanel.add(subcatListPanel);
        
//        subcatListPanel.setVisible(false);
        resultsPanel.setLayout(new BoxLayout(resultsPanel, BoxLayout.Y_AXIS));
        
        mainResultsPanelGroup = new GroupLayout(mainResultsPanel);
        mainResultsPanel.setLayout(mainResultsPanelGroup);
        
//        resultsGroup.setHonorsVisibility(true);
        
        mainResultsPanelGroup.setHorizontalGroup(mainResultsPanelGroup.createParallelGroup(Alignment.LEADING)
            .addComponent(resultsPanel)
        );
        mainResultsPanelGroup.setVerticalGroup(mainResultsPanelGroup.createSequentialGroup()
            .addComponent(resultsPanel)
        );
        
        
        resultsSP.add(mainResultsPanel);
        resultsSP.setViewportView(mainResultsPanel);
        resultsSP.setPreferredSize(new Dimension(300,300));
        
        
        
        
        //main group for browse tab
        GroupLayout mainGroup = new GroupLayout(this);
        this.setLayout(mainGroup);
//        mainGroup.setAutoCreateContainerGaps(true);
        mainGroup.setAutoCreateGaps(true);
        
        mainGroup.setHorizontalGroup(mainGroup.createParallelGroup(Alignment.LEADING)
            .addComponent(selectCatLbl, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            .addComponent(catCB, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            .addComponent(subcatCB)
            .addComponent(resultsSP)
        );
        mainGroup.setVerticalGroup(mainGroup.createSequentialGroup()
            .addComponent(selectCatLbl)
            .addComponent(catCB, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
            .addComponent(subcatCB, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
            .addComponent(resultsSP)
        );
        
        mainGroup.linkSize(catCB,subcatCB);
        
        
    }
    private void populateCat() {
        ArrayList<String> cats = searchProvider.getCategories();
        for (String cat : cats) {
            catCB.addItem(cat);
        }
        catCB.setSelectedItem("Home");
    }
    
}
