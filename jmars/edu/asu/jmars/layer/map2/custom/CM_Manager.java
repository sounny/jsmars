package edu.asu.jmars.layer.map2.custom;

import java.awt.Adjustable;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.AdjustmentEvent;
import java.awt.event.AdjustmentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.GroupLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButton;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.LayoutStyle;
import javax.swing.LayoutStyle.ComponentPlacement;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.WindowConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.TableColumn;

import org.apache.commons.lang3.math.NumberUtils;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LManager;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.map2.MapLViewFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.lmanager.SearchProvider;
import edu.asu.jmars.swing.STable;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeText;
import edu.asu.jmars.util.DebugLog;
import edu.asu.jmars.util.Util;
import edu.asu.jmars.util.stable.FilteringColumnModel;
import edu.asu.jmars.util.stable.STableViewPersister;
import edu.asu.jmars.util.stable.Sorter;
import edu.stanford.ejalbert.BrowserLauncher;

public class CM_Manager extends JDialog {
    private static final long serialVersionUID = -8626650421727172884L;
    private static CM_Manager cmManager = null;
    
    private static final int CARD_STATE_INITIAL = 0;
    private static final int CARD_STATE_EDIT_UPLOAD = 1;
    private static final int CARD_STATE_VIEW_UPLOAD = 2;
    private static final int CARD_STATE_EDIT_EXISTING = 3;
    private static final int CARD_STATE_REPROCESS = 4;
    
    private static final int TABLE_SELECTION_IN_PROGRESS = 0;
    private static final int TABLE_SELECTION_COMPLETED = 1;
    private static final int TABLE_SELECTION_EXISTING = 2;
    private static final int TABLE_SELECTION_FILE_CHOOSER = 3;
    
    private int card_state = CARD_STATE_INITIAL;
    
    public static final int TAB_UPLOAD = 0;
    public static final int TAB_EXISTING = 1;
    
    private static final String CARD1_NOTE_DEFAULT_MSG = "This information will apply to all files selected. Some fields may not be editable. "
            + "The information can be updated later by editing the existing map.";
   
    
    private int tableSelectionSource = TABLE_SELECTION_IN_PROGRESS;
        
    //upload tab
    private JButton cancelUploadButton;
    private JButton refreshUploadStatusButton;
    private JButton selectFilesButton;
    private JButton viewEditUploadButton;
    private JTabbedPane mainTp;
    private JPanel uploadTabPanel;
    private STable uploadInProgressTable;
    private STable uploadCompletedTable;
    private JPanel uploadInProgressTablePanel;
    private JPanel uploadCompletedTablePanel;
    private JScrollPane uploadTableSp;
    private JFileChooser fileChooser;
    private JPopupMenu uploadRCMenu;
    private JPopupMenu completedRCMenu;
    private JMenuItem cancelUploadMenuItem;
    private JMenuItem viewUploadMenuItem;
    private JMenuItem viewCompletedMenuItem;
    private JMenuItem clearCompletedMenuItem;
    private JSeparator uploadSeparator;
    private JLabel inProgressUploadsLbl;
    private JScrollPane uploadCompletedTableSp;
    private JButton viewCompletedButton;
    private JButton clearSelectedButton;
    private JButton clearAllCompletedButton;
    private JLabel completedUploadsLbl;
    
    private FileTableModel inProgressUploadFileTableModel;
    private FileTableModel completedUploadFileTableModel;
    private CustomMapTableModel customMapTableModel;
    private SharingTableModel sharingTableModel;
    private boolean processInProgress;
    
    CustomMap reprocessedCustomMap = null;
    
    //manage maps tab
//    private JPanel manageMapsTabPanel;
    private GroupLayout mainDialogLayout;
    
    //data structures
    //uploadFiles is used for the uploadFilesTable
    private List<CustomMap> uploadInProgressFiles = Collections.synchronizedList(new ArrayList<CustomMap>());
    private List<CustomMap> uploadCompletedFiles = Collections.synchronizedList(new ArrayList<CustomMap>());
    
    //existingMapFiles is used for the existingMapsTable
    private List<CustomMap> existingMapsFiles = Collections.synchronizedList(new ArrayList<CustomMap>());
    private ArrayList<CustomMap> allExistingMapsFiles = new ArrayList<CustomMap>();
    private ArrayList<CustomMap> ownedByMeExistingMapsFiles = null;//to be populated if necessary to shorten search time
    private ArrayList<CustomMap> sharedWithMeExistingMapsFiles = null;//to be populated if necessary to shorten search time
    private ArrayList<CustomMap> sharedByMeExistingMapsFiles = null;//to be populated if necessary to shorten search time
    private ArrayList<String> mapNames = new ArrayList<String>();//used for validation and prevention of duplicates
    private ArrayList<String> inProgressNames = new ArrayList<String>();//used for validation and prevention of duplicates
    private ArrayList<CustomMap> newlyCreatedFilesToAddToSearch = new ArrayList<CustomMap>();
    
    //shareMapFiles is used for the shareMapsTable
    private ArrayList<CustomMap> sharedMapFiles = new ArrayList<CustomMap>();
    
    //hashmap to reference stage names
    public static HashMap<String, String> stageReferenceTable;
    
    //dialogUplaodFileList is the list of UploadFile objects while in the main dialog
    private List<CustomMap> dialogUploadFileList = Collections.synchronizedList(new ArrayList<CustomMap>());
    
    //main dialog
    private JDialog mainDialog;
    
    //main dialog panels
    private JPanel uploadOptionsPanel;
    private JPanel uploadInfoPanel;
    private JPanel uploadMetadataPanel;
    private JPanel uploadOptionsButtonPanel;
    private JPanel selectedFilesPanel;
    private JPanel mainDialogActivePanel;
    
    //MainDialog
    private JLabel citationLbl;
    private JScrollPane citationSp;
    private JTextArea citationTextAreaInput;
    
    private JLabel descriptionLbl;
    private JScrollPane descriptionSp;
    private JTextArea descriptionTextAreaInput;
   
    private JLabel enterMapInfoLbl;
    private JLabel enterMapInfoLbl1;
    private JScrollPane enterMapInfoNoteSp;
    private JTextPane enterMapInfoNoteTextPane;
    private JLabel extentLbl;
    private JList<String> selectedFilesList;
    private JLabel keywordsLbl;
    private JScrollPane keywordsSp;
    private JTextArea keywordsTextAreaInput;
    private JLabel linksLbl;
    private JScrollPane linksSp;
    private JTextArea linksTextAreaInput;
    
    private JScrollPane uploadOptionVerifySp;
    private JScrollPane uploadOptionProcessSp;
    private JScrollPane uploadOptionManualSp;
    private JEditorPane uploadOptionVerifyEp;
    private JEditorPane uploadOptionProcessEp;
    private JEditorPane uploadOptionManualEp;
    
    //buttons for different cards
    //card0
    private JButton uploadOptionsContinueButton;
    private JButton uploadOptionsCancelButton;
    private JRadioButton manauallyEnterRadio;
    private JRadioButton uploadAndProcessRadio;
    private JRadioButton verifyInformationRadio;
    
    //map info
    private JButton mapInfoBackButton;
    private JButton mapInfoCancelButton;
    private JButton mapInfoClearButton;
    private JButton mapInfoNextButton;
    private JButton mapInfoUploadButton;
    private JButton mapInfoReprocessButton;
    
    //map metadata
    private JButton mapMetadataBackButton;
    private JButton mapMetadataCancelButton;
    private JButton mapMetadataClearButton;
    private JButton mapMetadataUploadButton;
    private JButton mapMetadataReprocessButton;    
    
    
    //inputs and labels for card1
    private JRadioButton regionalRadio;
    private JTextField southernLatInput;
    private JLabel southernmostLatLbl;
    private JTextField unitsInput;
    private JLabel unitsLbl;
    private JLabel westernLonLbl;
    private JRadioButton westRadio;
    private JTextField westernLonInput;
    private JRadioButton globalRadio;
    private JTextField ignoreValueInput;
    private JLabel ignoreValueLbl;
    private JLabel degreesLbl;
    private JRadioButton eastRadio;
    private JTextField easternLonInput;
    private JLabel easternLonLbl;
    private JTextField nameInput;
    private JLabel nameLbl;
    private JTextField northernLatInput;
    private JLabel northernmostLatLbl;
    private ButtonGroup extentButtonGroup;
    private ButtonGroup degreeButtonGroup;
    private ButtonGroup uploadOptionsButtonGroup;
    private JRadioButton ocentricRadio;
    private JRadioButton ographicRadio;
    private JLabel shapeTypeLbl;
    private JLabel shapeTypeHelpLbl;
    private JLabel fileTypesLbl;
    private ButtonGroup shapeTypeButtonGroup;
    //end inputs for card1
    
    
    
    private JLabel selectedFilesLbl;
    private JScrollPane selectedFilesScrollPane;
    private JLabel uploadOptionsHeaderLbl;
    
    private String degEStr = "\u00b0E";
    private String degWStr = "\u00b0W";
    private String degNStr = "\u00b0N";
    
//    private Thread processThread = null;
    private Thread uploadThread = null;
    private Thread monitorThread = null;
    private CustomMapMonitor customMapMonitor = null;
    private UploadFileRunner uploadRunner = null;
    
    private DefaultListModel<String> selectedFilesModel;
    private DefaultListModel<String> availableUsersModel;
    private DefaultListModel<String> sharedWithUsersModel;
    private DefaultListModel<String> sharedWithGroupsModel;
    private DefaultListModel<String> availableGroupsModel;
    private DefaultListModel<String> manageUsersListModel;
    private DefaultListModel<String> manageGroupsUsersModel;
    private DefaultListModel<String> manageGroupsAvailableModel;
    private boolean manageUsersDirtyFlag = false;
    private DefaultComboBoxModel<String> groupComboBoxModel;
    
    
    //End MainDialog
    
    
    //start reprocess dialog
    //Labels 
    private JLabel mapName;
    private JButton reprocessMapButton;
    private JMenuItem reprocessMenuItem;
    private boolean reprocessFlag;
    //end reprocess dialog
    
    //Existing Maps
    private JButton manageSharingButton;
    private JRadioButton sharedWithOthersRadio;
    private STable existingMapsTable;
    private JButton addLayerButton;
    private JButton deleteExistingMapButton;
    private JButton editExistingMapButton;
    private JButton shareExistingMapButton;
    private JButton refreshMapsButton;
    private JCheckBox descriptionCheckbox;
    private JPanel existingMainPanel;
    private JPanel existingMapsButtonPanel;
    private JPanel mapFilterPanel;
    private JScrollPane existingMapsTableSp;
    private JCheckBox fileNameCheckbox;
    private JButton filterButton;
    private JButton clearFilterButton;
    private JPanel filterCheckBoxPanel;
    private JLabel filterMapsLabel;
    private JPanel filterPanel;
    private JRadioButton allMapsRadio;
    private JRadioButton myMapsRadio;
    private JRadioButton sharedWithMeRadio;
    private JTextField filterTextbox;
    private JCheckBox keywordsCheckbox;
    private JCheckBox mapNameCheckbox;
    private JMenuItem editCMMenuItem;
    private JMenuItem deleteCMMenuItem;
    private JMenuItem addLayerMenuItem;
    private JMenuItem shareCMMenuItem;
    private JPopupMenu existingMapRCMenu;
    private JCheckBox ownerCheckbox;
    private JPanel existingMapsTabPanel;
    private boolean activeFilter = false;//flag to know if a filter needs to be re-applied
    
    //sharing
    private JButton removeGroupShareButton;
    private JButton saveSharingChangesButton;
    private JLabel shareUsernameLbl;
    private JPanel shareWithGroupsPanel;
    private JScrollPane sharedWithGroupsSp;
    private JButton shareWithUserButton;
    private JTextField shareWithUserInput;
    private JPanel shareWithUserSp;
    private JLabel sharedMapsLbl;
    private JScrollPane sharedMapsSp;
    private STable sharedMapsTable;
    private JPanel sharedUsersPanel;
    private JLabel sharedWithGroupsLbl;
    private JLabel sharedWithUsersLbl;
    private JList<String> sharedWithUsersList;
    private JScrollPane sharedWithUsersSp;
    private JPanel sharingManagmentButtonPanel;
    private JButton manageUserListButton;
    private JList<String> availableGroupsList;
    private JList<String> sharedWithGroupsList;
    private JButton manageGroupsButton;
    private JDialog sharingManagementDialog;
    private JButton addGroupShareButton;
    private JLabel availableGroupsLbl;
    private JScrollPane availableGroupsSp;
    private JButton cancelSharingButton;
    private JLabel availableUsersLbl;
    private JList<String> availableUsersList;
    private JScrollPane availableUsersSp;
    private JPanel groupsSelectDeselectButtonPanel;
    private JButton unshareWithUserButton;
    private JPanel usersSelectDeselectButtonPanel;
    private JList<String> manageUsersList;
    private JButton manageUsersAddButton;
    private JPanel manageUsersAddPanel;
    private JPanel manageUsersButtonPanel;
    private JDialog manageUsersDialog;
    private JButton manageUsersDoneButton;
    private JLabel manageUsersLbl;
    private JButton manageUsersRemoveButton;
    private JTextField searchUserInput;
    private JScrollPane userListSp;
    private JLabel manageUsersMsgLbl;
    private JButton manageUsersSaveButton;
    
    private JDialog manageGroupsDialog;
    private JPanel groupAddRemoveUserButtonPanel;
    private JButton groupAddUserButton;
    private JList<String> groupAvailableUserList;
    private JLabel groupAvailableUsersLbl;
    private JPanel groupAvailableUsersPanel;
    private JPanel groupButtonPanel;
    private JComboBox<String> groupComboBox;
    private JButton groupCreateGroupButton;
    private JButton groupDeleteButton;
    private JButton groupDoneButton;
    private JPanel groupListPanel;
    private JButton groupManageUserListButton;
    private JLabel groupMgmtLbl;
    private JPanel groupNewGroupPanel;
    private JButton groupRemoveUserButton;
    private JButton groupSaveButton;
    private JList<String> groupUserList;
    private JPanel groupUserListPanel;
    private JScrollPane groupUserListSp;
    private JLabel groupUsersLbl;
    
    
    private JScrollPane groupAvailableUserListSp;
    
    
    private JPanel groupComboBoxPanel;
    private JPanel groupMainListPanel;
    private JButton groupRenameButton;
    private JButton groupSaveAndCloseButton;
    private JPanel groupTopButtonPanel;
    private JLabel sharingGroupsLbl;
    private JButton sharingSaveAndCloseButton;
    private JLabel sharingUsersLbl;
    
    ArrayList<SharingGroup> allSharingGroups = null;
    ArrayList<String> allSharingUsers = null;
    //end sharing
    
    private boolean initFailed = false;
    public boolean checkInitFailed() {
    	return initFailed;
    }
    public CM_Manager(JFrame ownerFrame) {
        super(ownerFrame);
        initComponents();
    }
    public CM_Manager(JDialog ownerDialog) {
        super(ownerDialog);
        initComponents();
    }
    public static CM_Manager getInstance() {
        return getInstance(null);
    }
    public static CM_Manager getInstance(Component owner) {
        if (cmManager == null || cmManager.initFailed) {
            if (owner == null) {
                cmManager = new CM_Manager(Main.mainFrame);
            } else if (owner instanceof JDialog) {
                JDialog ownerDialog = (JDialog) owner;
                cmManager = new CM_Manager(ownerDialog);
            } else if (owner instanceof JFrame) {
                JFrame ownerFrame = (JFrame) owner;
                cmManager = new CM_Manager(ownerFrame);
            } else {
                cmManager = new CM_Manager(Main.mainFrame);
            }
        } else {
            if (owner != null && owner != cmManager.getParent()) {
                //because we can be called from the MapSettingsDialog or from the File menu on the mainFrame, we need to 
                //properly parent the Dialog if it already existed and is called from the other source.
                cmManager.setVisible(false);
                cmManager.dispose();
                cmManager = null;
                cmManager = getInstance(owner);
            }
        }
        return cmManager;
    }
    public void setSelectedTab(int selectedTab) {
        if (selectedTab == TAB_UPLOAD) {
            mainTp.setSelectedIndex(TAB_UPLOAD);
        } else if (selectedTab == TAB_EXISTING) {
            mainTp.setSelectedIndex(TAB_EXISTING);
        } else {
            mainTp.setSelectedIndex(TAB_UPLOAD);
        }
    }
    private void initComponents() {
        setIconImage(Util.getJMarsIcon());
        stageReferenceTable = CustomMapBackendInterface.populateReferenceTable();//always call this first. Key off of it for a bad network connection
        if (stageReferenceTable == null) {
        	//we were not able to initialize data using JMARSHttpRequest. Abort
        	initFailed = true;
        	return;
        } else {
        	initFailed = false;
        }
        uploadInProgressFiles = CustomMapBackendInterface.getInProgressCustomMapList();
        ArrayList<CustomMap> toRemove = new ArrayList<CustomMap>();
        for(CustomMap map : uploadInProgressFiles) {
            if (CustomMap.STATUS_CANCELED.equalsIgnoreCase(map.getStatus())
                || CustomMap.STATUS_COMPLETE.equalsIgnoreCase(map.getStatus()) 
                || CustomMap.STATUS_ERROR.equalsIgnoreCase(map.getStatus())) {
                uploadCompletedFiles.add(map);
                toRemove.add(map);
            } else {
                inProgressNames.add(map.getName());
            }
        }
        uploadInProgressFiles.removeAll(toRemove);
        existingMapsFiles.addAll(CustomMapBackendInterface.getExistingMapList());
        allExistingMapsFiles.addAll(existingMapsFiles);
        
        //populate mapNames for validation and prevention of dupes later
        for (CustomMap map : allExistingMapsFiles) {
            mapNames.add(map.getName());
        }
        
        
        //JDialog attributes
        setTitle("Custom Map Manager");
        setDefaultCloseOperation(WindowConstants.HIDE_ON_CLOSE);
        setPreferredSize(new Dimension(900, 575));
        setSize(new Dimension(900, 575));

        mainTp = new JTabbedPane();
        mainTp.setTabPlacement(JTabbedPane.TOP);
        mainTp.setPreferredSize(new Dimension(700, 540));
        
        //build Panels for each tab
        buildUploadPanel();
        buildExistingMapsPanel();
        buildSharingManagementDialog();
        
        //build main dialog
        buildMainDialog();
        
        mainTp.addTab("UPLOAD/PROCESSING", uploadTabPanel);
        mainTp.addTab("EXISTING MAPS", existingMainPanel);
        
        GroupLayout layout = new GroupLayout(getContentPane());
        getContentPane().setLayout(layout);
        layout.setHorizontalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(mainTp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );
        layout.setVerticalGroup(
            layout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(mainTp, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
        );   

        //setColors();
        pack();
        
        if (uploadInProgressFiles.size() > 0) {
            kickOffMonitorThread();
        }
    }
    private void buildMainDialog() {
        mainDialog = new JDialog(this, "Custom Map Upload", false);
        mainDialog.setLocationRelativeTo(this);
        Dimension d1 = new Dimension(900, 450);
        mainDialog.setPreferredSize(d1);
        mainDialog.setSize(d1);

        buildMainDialogOptionsPanel();
        
        pack();
    }
    
    private void buildMainDialogOptionsPanel() {
        initializeDialogWidgets();

        //remove borders
        uploadOptionVerifySp.setBorder(new EmptyBorder(0,0,0,0));
        uploadOptionProcessSp.setBorder(new EmptyBorder(0,0,0,0));
        uploadOptionManualSp.setBorder(new EmptyBorder(0,0,0,0));

        Dimension uploadOptionTextDimension = new Dimension(600,40);
        uploadOptionManualSp.setPreferredSize(uploadOptionTextDimension);
        uploadOptionProcessSp.setPreferredSize(uploadOptionTextDimension);
        uploadOptionVerifySp.setPreferredSize(uploadOptionTextDimension);
        
        
        uploadOptionManualSp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        uploadOptionManualSp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        uploadOptionProcessSp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        uploadOptionProcessSp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        uploadOptionVerifySp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        uploadOptionVerifySp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
        
        selectedFilesModel = new DefaultListModel<String>();
        selectedFilesList.setModel(selectedFilesModel);
        selectedFilesScrollPane.setViewportView(selectedFilesList);
        selectedFilesList.setEnabled(false);
        selectedFilesLbl.setText("Selected File(s)");

        layoutSelectedFilesPanel();


        //card0 stuff
        uploadOptionManualEp.setText("Upload the file(s), then allow me to verify the image header information\n before processing.");
        uploadOptionManualEp.setEditable(false);
        uploadOptionManualSp.setViewportView(uploadOptionManualEp);
        uploadOptionProcessEp.setText("Manually enter geospatial and other information about the image(s).");
        uploadOptionProcessEp.setEditable(false);
        uploadOptionProcessSp.setViewportView(uploadOptionProcessEp);
        uploadOptionVerifyEp.setText("Map(s) will be uploaded and processed using geospatial information found\n in the image header(s).");
        uploadOptionVerifyEp.setEditable(false);
        uploadOptionVerifySp.setViewportView(uploadOptionVerifyEp);
        uploadOptionsHeaderLbl.setText("Upload Options:");
        
        
        
        manauallyEnterRadio.setText("Manually Enter:");
        verifyInformationRadio.setText("Verify Image Information:");
        uploadAndProcessRadio.setText("Upload and Process Now:");
        

        layoutUploadOptions();
        
        
        
        //card1 stuff
        enterMapInfoLbl.setText("Enter Map Information:");
        enterMapInfoNoteTextPane.setText("Note: This information will apply to all files selected. Some fields may not be editable. The information can be updated later by editing the existing map. ");
        enterMapInfoNoteTextPane.setEditable(false);
        enterMapInfoNoteSp.setViewportView(enterMapInfoNoteTextPane);
        nameLbl.setText("Name:");
        ignoreValueLbl.setText("Ignore Value:");
        unitsLbl.setText("Units:");
        extentLbl.setText("Extent:");
        globalRadio.setText("Global");
        regionalRadio.setText("Regional");
        degreesLbl.setText("Longitude Direction:");
        eastRadio.setText("Positive East("+degEStr+")");
        westRadio.setText("Positive West("+degWStr+")");
        degreesLbl.setToolTipText("<html>The longitude direction for the coordinate system of this custom map.<br /> "
                + "Positive east means longitude values increase to the east. <br />"
                + "Positive west means longitude values increase to the west.");
        eastRadio.setToolTipText("Longitude values increase to the east");
        westRadio.setToolTipText("Longitude values increase to the west");
        easternLonLbl.setText("Easternmost Lon: ");
        westernLonLbl.setText("Westernmost Lon: ");
        northernmostLatLbl.setText("Northernmost Lat: ");
        southernmostLatLbl.setText("Southernmost Lat:");
        ocentricRadio.setText("Ocentric");
        ographicRadio.setText("Ographic");
        String shapeHelp = "<html>Most Earth datums use ographic (an ellipsoid model) while majority of other planetary bodies<br /> "
                + "use ocentric latitudes (spherical models). If you select ographic, the map will be converted to the JMARS spherical<br /> "
                + "model for you. If ocentric is selected, the points entered will assume a spherical datum was used to<br /> "
                + "calculate the coordinate points.</html>";
        ographicRadio.setToolTipText(shapeHelp);
        ocentricRadio.setToolTipText(shapeHelp);
        shapeTypeLbl.setText("Shape Type: ");
        shapeTypeLbl.setToolTipText(shapeHelp);
        shapeTypeHelpLbl.setText("Help");
        shapeTypeHelpLbl.setForeground(((ThemeText) GUITheme.get(ThemeText.getCatalogKey())).getHyperlink());
        shapeTypeHelpLbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
            	String urlString = "https://jmars.mars.asu.edu/latitudeMeasurements";
                try {                               
                    Util.launchBrowser(urlString);
                } catch (Exception e1) {
                	DebugLog.instance().println(e1.getMessage());
                	DebugLog.instance().aprintln("There was a problem opening up the url: "+urlString);
                }             
            }
        });
        shapeTypeHelpLbl.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        
        layoutUploadInfo();
        
        //card2 
        enterMapInfoLbl1.setText("Enter Map Information (optional):");
        descriptionLbl.setText("Description:");
        descriptionSp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        descriptionSp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        descriptionTextAreaInput.setColumns(20);
        descriptionTextAreaInput.setRows(5);
        descriptionSp.setViewportView(descriptionTextAreaInput);
        linksLbl.setText("Links:");
        linksLbl.setToolTipText("Enter links as a comma separated list (with or without spaces)");
        linksSp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        linksSp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        linksTextAreaInput.setColumns(20);
        linksTextAreaInput.setRows(5);
        linksSp.setViewportView(linksTextAreaInput);
        citationLbl.setText("Citation:");
        citationSp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        citationSp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        citationTextAreaInput.setColumns(20);
        citationTextAreaInput.setRows(5);
        citationSp.setViewportView(citationTextAreaInput);
        keywordsLbl.setText("Keywords:");
        keywordsLbl.setToolTipText("");
        keywordsSp.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
        keywordsSp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_NEVER);
        keywordsTextAreaInput.setColumns(20);
        keywordsTextAreaInput.setRows(5);
        keywordsSp.setViewportView(keywordsTextAreaInput);

        layoutUploadMetadata();

        layoutMainDialogPanel();
    }
    
    private void buildUploadPanel() {
        initializeFileChooser();
       
        uploadTabPanel = new JPanel();
        uploadInProgressTable = new STable();
        uploadInProgressTable.setParentDialog(this);
        uploadInProgressTable.setDescription("In Progress Custom Map");
        uploadCompletedTable = new STable();
        uploadCompletedTable.setParentDialog(this);
        uploadCompletedTable.setDescription("Completed Custom Map");
        
        selectFilesButton = new JButton(selectFilesAction);
        
        //commenting this out until we decide the proper logic across tabs.
//        getRootPane().setDefaultButton(selectFilesButton);
        
        viewEditUploadButton = new JButton(viewEditUploadAction);
        cancelUploadButton = new JButton(cancelUploadAction);
        refreshUploadStatusButton = new JButton(refreshStatusAction);
        
        inProgressUploadsLbl = new JLabel();
        uploadSeparator = new JSeparator();
        uploadCompletedTablePanel = new JPanel();
        uploadCompletedTableSp = new JScrollPane();
        completedUploadsLbl = new JLabel();
        uploadInProgressTablePanel = new JPanel();
        uploadTableSp = new JScrollPane();

        fileTypesLbl = new JLabel();
        fileTypesLbl.setText("Supported image formats");
        fileTypesLbl.setForeground(((ThemeText) GUITheme.get(ThemeText.getCatalogKey())).getHyperlink());
        fileTypesLbl.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                try {                               
                    String urlString = "https://jmars.mars.asu.edu/supportedCMImages";
                    Util.launchBrowser(urlString);
                } catch (Exception e1) {
                }             
            }
        });

        fileTypesLbl.setCursor(new java.awt.Cursor(java.awt.Cursor.HAND_CURSOR));
        
        uploadRCMenu = new JPopupMenu();
        completedRCMenu = new JPopupMenu();
        cancelUploadMenuItem = new JMenuItem(cancelUploadAction);
        viewUploadMenuItem = new JMenuItem(viewEditUploadAction);
        viewCompletedMenuItem = new JMenuItem(viewCompletedUploadAction);
        clearCompletedMenuItem = new JMenuItem(clearCompletedUploadAction);
        uploadRCMenu.add(viewUploadMenuItem);
        uploadRCMenu.add(cancelUploadMenuItem);
        completedRCMenu.add(viewCompletedMenuItem);
        completedRCMenu.add(clearCompletedMenuItem);
        
        clearAllCompletedButton = new JButton(clearAllCompletedUploadAction);
        clearSelectedButton = new JButton(clearCompletedUploadAction);
        viewCompletedButton = new JButton(viewCompletedUploadAction);
                
        inProgressUploadsLbl.setText("In progress maps:");
        completedUploadsLbl.setText("Completed/Canceled/Error maps:");
        

        inProgressUploadFileTableModel = new FileTableModel(uploadInProgressFiles);
        uploadInProgressTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        uploadInProgressTable.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        uploadInProgressTable.setUnsortedTableModel(inProgressUploadFileTableModel);
        uploadTableSp.setViewportView(uploadInProgressTable);
        uploadInProgressTable.getSelectionModel().addListSelectionListener(uploadTableSelectionListener);
        uploadInProgressTable.addMouseListener(uploadTableMouseListener);
        uploadInProgressTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        uploadInProgressTable.setAutoCreateColumnsFromModel(false);
       
        FilteringColumnModel fcm = new FilteringColumnModel();
        for (int i=0; i<inProgressUploadFileTableModel.getColumnCount(); i++) {
            String header = inProgressUploadFileTableModel.getColumnName(i);
            TableColumn tc = new TableColumn(i);
            tc.setHeaderValue(header);
            tc.setPreferredWidth(inProgressUploadFileTableModel.getWidth(header));
            tc.setMinWidth(inProgressUploadFileTableModel.getWidth(header));
            fcm.addColumn(tc);
            fcm.setVisible(tc, inProgressUploadFileTableModel.getDefaultVisibleColumns().contains(header));
        }
        uploadInProgressTable.setColumnModel(fcm);
        
        STableViewPersister uploadPersister = new STableViewPersister(uploadInProgressTable, "CustomMapInProgress");
        uploadPersister.updateTable();
        uploadPersister.connect();
        
        completedUploadFileTableModel = new FileTableModel(uploadCompletedFiles);
        uploadCompletedTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        uploadCompletedTable.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        uploadCompletedTable.setUnsortedTableModel(completedUploadFileTableModel);
        uploadCompletedTableSp.setViewportView(uploadCompletedTable);
        uploadCompletedTable.getSelectionModel().addListSelectionListener(uploadCompletedTableSelectionListener);
        uploadCompletedTable.addMouseListener(completedTableMouseListener);
        uploadCompletedTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        uploadCompletedTable.setAutoCreateColumnsFromModel(false);
       
        FilteringColumnModel cfcm = new FilteringColumnModel();
        for (int i=0; i<completedUploadFileTableModel.getColumnCount(); i++) {
            String header = completedUploadFileTableModel.getColumnName(i);
            TableColumn tc = new TableColumn(i);
            tc.setHeaderValue(header);
            tc.setPreferredWidth(completedUploadFileTableModel.getWidth(header));
            tc.setMinWidth(completedUploadFileTableModel.getWidth(header));
            cfcm.addColumn(tc);
            cfcm.setVisible(tc, completedUploadFileTableModel.getDefaultVisibleColumns().contains(header));
        }
        uploadCompletedTable.setColumnModel(cfcm);
        
        STableViewPersister completedPersister = new STableViewPersister(uploadCompletedTable, "CustomMapCompleted");
        completedPersister.updateTable();
        completedPersister.connect();
        
        

        layoutUploadPanel();
        
        //init the main button state to disabled
        toggleMainButtons(false);
        toggleCompletedButtons(false);
    }
    
    private void buildExistingMapsPanel() {
//        manageMapsTabPanel = new JPanel();
        descriptionCheckbox = new JCheckBox();
        existingMainPanel = new JPanel();
        existingMapsButtonPanel = new JPanel();
        mapFilterPanel = new JPanel();
        existingMapsTableSp = new JScrollPane();
        existingMapsTabPanel = new JPanel();
        
        fileNameCheckbox = new JCheckBox();
        filterCheckBoxPanel = new JPanel();
        filterMapsLabel = new JLabel();
        filterPanel = new JPanel();
        filterTextbox = new JTextField();
        keywordsCheckbox = new JCheckBox();
        ownerCheckbox = new JCheckBox();
        mapNameCheckbox = new JCheckBox();
        allMapsRadio = new JRadioButton();
        myMapsRadio = new JRadioButton();
        sharedWithMeRadio = new JRadioButton();
        sharedWithOthersRadio = new JRadioButton();
        
        
        manageSharingButton = new JButton(manageSharingAction);
        editExistingMapButton = new JButton(existingMapEditAction);
        deleteExistingMapButton = new JButton(existingMapDeleteAction);
        addLayerButton = new JButton(existingMapAddLayerAction);
        reprocessMapButton = new JButton(existingMapReprocessAction);
        shareExistingMapButton = new JButton(existingMapShareAction);
        filterButton = new JButton(filterMapsAction);
        clearFilterButton = new JButton(clearFilterMapsAction);
        refreshMapsButton = new JButton(refreshMapsAction);
        
        
        existingMapRCMenu = new JPopupMenu();
        editCMMenuItem = new JMenuItem(existingMapEditAction);
        deleteCMMenuItem = new JMenuItem(existingMapDeleteAction);
        addLayerMenuItem = new JMenuItem(existingMapAddLayerAction);
        shareCMMenuItem = new JMenuItem(existingMapShareAction);
        reprocessMenuItem = new JMenuItem(existingMapReprocessAction);
        existingMapRCMenu.add(addLayerMenuItem);
        existingMapRCMenu.add(editCMMenuItem);
        existingMapRCMenu.add(shareCMMenuItem);
        existingMapRCMenu.add(deleteCMMenuItem);
        existingMapRCMenu.add(reprocessMenuItem);
        
        filterMapsLabel.setText("Filter Value:");
        ownerCheckbox.setText("Owner");
        fileNameCheckbox.setText("File Name");
        descriptionCheckbox.setText("Description");
        keywordsCheckbox.setText("Keywords");
        mapNameCheckbox.setText("Map Name");
        allMapsRadio.setText("All maps");
        myMapsRadio.setText("My maps");
        sharedWithMeRadio.setText("Shared with me");
        sharedWithOthersRadio.setText("Shared with others");
        
        keywordsCheckbox.setSelected(true);
        fileNameCheckbox.setSelected(true);
        ownerCheckbox.setSelected(true);
        descriptionCheckbox.setSelected(true);
        mapNameCheckbox.setSelected(true);
        
        
        ButtonGroup mapFilterRadioButtonGroup = new ButtonGroup();
        mapFilterRadioButtonGroup.add(allMapsRadio);
        mapFilterRadioButtonGroup.add(myMapsRadio);
        mapFilterRadioButtonGroup.add(sharedWithMeRadio);
        mapFilterRadioButtonGroup.add(sharedWithOthersRadio);
        allMapsRadio.addActionListener(mapFilterRadioAction);
        myMapsRadio.addActionListener(mapFilterRadioAction);
        sharedWithMeRadio.addActionListener(mapFilterRadioAction);
        sharedWithOthersRadio.addActionListener(mapFilterRadioAction);
        allMapsRadio.setSelected(true);
        
        existingMapsTable = new STable();
        existingMapsTable.setParentDialog(this);
        existingMapsTable.setDescription("Existing Custom Map");
        
        customMapTableModel = new CustomMapTableModel(existingMapsFiles);
        existingMapsTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        existingMapsTable.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        existingMapsTable.setUnsortedTableModel(customMapTableModel);
        existingMapsTableSp.setViewportView(existingMapsTable);
        existingMapsTableSp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);
        existingMapsTableSp.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_ALWAYS);
        existingMapsTable.getSelectionModel().addListSelectionListener(existingMapsTableSelectionListener);
        existingMapsTable.addMouseListener(existingMapsTableMouseListener);
        existingMapsTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        existingMapsTable.setAutoCreateColumnsFromModel(false);
       
        TableColumn toSort = null;
        FilteringColumnModel fcm = new FilteringColumnModel();
        for (int i=0; i<customMapTableModel.getColumnCount(); i++) {
            String header = customMapTableModel.getColumnName(i);
            TableColumn tc = new TableColumn(i);
            tc.setHeaderValue(header);
            tc.setPreferredWidth(customMapTableModel.getWidth(header));
            tc.setMinWidth(customMapTableModel.getWidth(header));
            fcm.addColumn(tc);
            fcm.setVisible(tc, customMapTableModel.getDefaultVisibleColumns().contains(header));
            if (header.equalsIgnoreCase("Owner")) {
            	toSort = tc;
            }
        }
        existingMapsTable.setColumnModel(fcm);
                
        STableViewPersister existingPersist = new STableViewPersister(existingMapsTable, "ExistingCustomMaps");
        existingPersist.updateTable();
        existingPersist.connect();
        
        layoutExistingMapsTab();
        
        toggleExistingMapButtons();
        
	    Sorter sorter = existingMapsTable.getSorter();
	    sorter.setSort(toSort);
    }
    private void buildSharingManagementDialog() {
        allSharingGroups = CustomMapBackendInterface.getSharingGroupList();
        sharedMapFiles.addAll(CustomMapBackendInterface.getSharedMapsForUser(allExistingMapsFiles, allSharingGroups));
        
        sharingManagementDialog = new JDialog(this, "Sharing Management", false);
        sharingManagementDialog.setLocationRelativeTo(existingMapsTable);
        sharingManagementDialog.setDefaultCloseOperation(HIDE_ON_CLOSE);
        sharedMapsSp = new JScrollPane();
        sharedMapsLbl = new JLabel();
        sharedUsersPanel = new JPanel();
        sharedWithUsersLbl = new JLabel();
        sharedWithUsersSp = new JScrollPane();
        sharedWithUsersList = new JList<>();
        shareWithUserSp = new JPanel();
        shareUsernameLbl = new JLabel();
        shareWithUserInput = new JTextField();
        shareWithGroupsPanel = new JPanel();
        availableGroupsSp = new JScrollPane();
        sharedWithGroupsList = new JList<>();
        sharedWithGroupsSp = new JScrollPane();
        availableGroupsList = new JList<>();
        sharedWithGroupsLbl = new JLabel();
        availableGroupsLbl = new JLabel();
        sharingManagmentButtonPanel = new JPanel();
        availableUsersSp = new JScrollPane();
        availableUsersList = new JList<>();
        usersSelectDeselectButtonPanel = new JPanel();
        availableUsersLbl = new JLabel();
        groupsSelectDeselectButtonPanel = new JPanel();
        userListSp = new JScrollPane();
        manageUsersList = new JList<>();
        manageUsersAddPanel = new JPanel();
        manageUsersLbl = new JLabel();
        searchUserInput = new JTextField();
        manageUsersButtonPanel = new JPanel();
        manageUsersMsgLbl = new JLabel();
        sharingUsersLbl = new JLabel();
        sharingGroupsLbl = new JLabel();
        
        manageGroupsDialog = new JDialog(sharingManagementDialog, "Manage Groups", false);
        groupAvailableUsersPanel = new JPanel();
        groupAvailableUsersLbl = new JLabel();
        groupAvailableUserList = new JList<>();
        groupAvailableUserListSp = new JScrollPane();
        groupUserListPanel = new JPanel();
        groupUserListSp = new JScrollPane();
        groupUserList = new JList<>();
        groupUsersLbl = new JLabel();
        groupAddRemoveUserButtonPanel = new JPanel();
        groupListPanel = new JPanel();
        groupComboBox = new JComboBox<>();
        groupMgmtLbl = new JLabel();
        groupButtonPanel = new JPanel();
        groupNewGroupPanel = new JPanel();
        groupComboBoxPanel = new JPanel();
        groupMainListPanel = new JPanel();
        groupTopButtonPanel = new JPanel();
        
        sharedMapsTable = new STable();
        sharedMapsTable.setParentDialog(this);
        sharedMapsTable.setDescription("Shared Custom Map");
        
        cancelSharingButton = new JButton(doneSharingAction);
        saveSharingChangesButton = new JButton(saveSharingAction);
        sharingSaveAndCloseButton = new JButton(sharingSaveAndCloseAction);
        manageUserListButton = new JButton(manageUserListAction);
        addGroupShareButton = new JButton(addGroupShareAction);
        removeGroupShareButton = new JButton(removeGroupShareAction);
        manageGroupsButton = new JButton(manageGroupsAction);
        shareWithUserButton = new JButton(shareWithUserAction);
        unshareWithUserButton = new JButton(unshareWithUserAction);
        manageUsersRemoveButton = new JButton(manageUsersRemoveAction);
        manageUsersRemoveButton.setEnabled(false);
        manageUsersDoneButton = new JButton(manageUsersDoneAction);
        manageUsersAddButton = new JButton(manageUsersAddAction);
        manageUsersSaveButton = new JButton(manageUsersSaveAction);
        groupRemoveUserButton = new JButton(groupRemoveUserAction);
        groupDeleteButton = new JButton(groupDeleteAction);
        groupCreateGroupButton = new JButton(groupCreateAction);
        groupManageUserListButton = new JButton(groupUserListAction);
        groupAddUserButton = new JButton(groupAddUserAction);
        groupDoneButton = new JButton(groupCloseAction);
        groupSaveButton = new JButton(groupSaveAction);
        groupSaveAndCloseButton = new JButton(groupSaveAndCloseAction);
        groupRenameButton = new JButton(groupRenameAction);
        
        saveSharingChangesButton.setEnabled(false);
        sharingSaveAndCloseButton.setEnabled(false);
        groupSaveButton.setEnabled(false);
        groupSaveAndCloseButton.setEnabled(false);
        groupRenameButton.setEnabled(false);
        groupDeleteButton.setEnabled(false);
        
        groupComboBoxModel = new DefaultComboBoxModel<>(new String[]{});
        groupComboBox.setModel(groupComboBoxModel);
        groupComboBox.addItemListener(groupItemListener);
        
        manageGroupsDialog.setSize(new Dimension(513, 525));
        manageGroupsDialog.setLocationRelativeTo(sharingManagementDialog);
        manageGroupsDialog.setDefaultCloseOperation(HIDE_ON_CLOSE);

        
        groupListPanel.setBorder(BorderFactory.createEtchedBorder());
        groupMainListPanel.setBorder(BorderFactory.createEtchedBorder());
        
        
        groupListPanel.setBorder(BorderFactory.createEtchedBorder());
        groupNewGroupPanel.setBorder(BorderFactory.createEtchedBorder());

        
        sharingManagementDialog.setPreferredSize(new Dimension(810, 550));
        sharingManagementDialog.setSize(new Dimension(810, 550));
        sharedMapsSp.setPreferredSize(new Dimension(453, 133));
        shareWithGroupsPanel.setPreferredSize(new Dimension(384, 231));
        sharedUsersPanel.setPreferredSize(new Dimension(384, 231));
        
        sharingTableModel = new SharingTableModel(sharedMapFiles);
        sharedMapsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        sharedMapsTable.setBorder(BorderFactory.createEmptyBorder(1, 1, 1, 1));
        sharedMapsTable.setUnsortedTableModel(sharingTableModel);
        sharedMapsSp.setViewportView(sharedMapsTable);
        sharedMapsTable.getSelectionModel().addListSelectionListener(shareTableSelectionListener);
        sharedMapsTable.addMouseListener(shareTableMouseListener);
        sharedMapsTable.setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
        sharedMapsTable.setAutoCreateColumnsFromModel(false);
       
        FilteringColumnModel fcm = new FilteringColumnModel();
        for (int i=0; i<sharingTableModel.getColumnCount(); i++) {
            String header = sharingTableModel.getColumnName(i);
            TableColumn tc = new TableColumn(i);
            tc.setHeaderValue(header);
            tc.setPreferredWidth(sharingTableModel.getWidth(header));
            tc.setMinWidth(sharingTableModel.getWidth(header));
            fcm.addColumn(tc);
            fcm.setVisible(tc, sharingTableModel.getDefaultVisibleColumns().contains(header));
        }
        sharedMapsTable.setColumnModel(fcm);
        
        STableViewPersister sharedMapsPersister = new STableViewPersister(sharedMapsTable, "CustomSharedMaps");
        sharedMapsPersister.updateTable();
        sharedMapsPersister.connect();
        
        
        availableUsersModel = new DefaultListModel<String>();
        availableUsersList.setModel(availableUsersModel);
        availableUsersSp.setViewportView(availableUsersList);
        
        sharedWithUsersModel = new DefaultListModel<String>();
        sharedWithUsersList.setModel(sharedWithUsersModel);
        sharedWithUsersSp.setViewportView(sharedWithUsersList);
        
        availableGroupsModel = new DefaultListModel<String>();
        availableGroupsList.setModel(availableGroupsModel);
        availableGroupsSp.setViewportView(availableGroupsList);
        
        sharedWithGroupsModel = new DefaultListModel<String>();
        sharedWithGroupsList.setModel(sharedWithGroupsModel);
        sharedWithGroupsSp.setViewportView(sharedWithGroupsList);
        
        manageUsersListModel = new DefaultListModel<String>();
        manageUsersList.setModel(manageUsersListModel);
        manageUsersList.addListSelectionListener(new ListSelectionListener() {
            
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (manageUsersList.getSelectedValuesList().size() > 0) {
                    manageUsersRemoveButton.setEnabled(true);
                } else {
                    manageUsersRemoveButton.setEnabled(false);
                }
            }
        });
        userListSp.setViewportView(manageUsersList);
        
        manageGroupsUsersModel = new DefaultListModel<String>();
        groupUserList.setModel(manageGroupsUsersModel);
        groupUserListSp.setViewportView(groupUserList);
        
        manageGroupsAvailableModel = new DefaultListModel<String>();
        groupAvailableUserList.setModel(manageGroupsAvailableModel);
        groupAvailableUserListSp.setViewportView(groupAvailableUserList);
        
        
        manageUsersAddPanel.setBorder(BorderFactory.createEtchedBorder());
        
        sharedMapsLbl.setText("Shared Maps:");
        shareUsernameLbl.setText("Username:");
        availableUsersLbl.setText("Not Shared:");
        sharedWithGroupsLbl.setText("Shared:");
        availableGroupsLbl.setText("Not Shared:");
        sharedWithUsersLbl.setText("Shared:");
        manageUsersLbl.setText("JMARS Username:");
        manageUsersMsgLbl.setForeground(new Color(255, 51, 51));
        manageUsersMsgLbl.setText("Invalid user entered");
        manageUsersMsgLbl.setVisible(false);
        sharingUsersLbl.setText("Users");
        sharingGroupsLbl.setText("Groups");
        groupAvailableUsersLbl.setText("Not in group:");
        groupUsersLbl.setText("In group:");
        groupMgmtLbl.setText("Group:");
        
        
        sharedUsersPanel.setBorder(BorderFactory.createEtchedBorder());
        shareWithGroupsPanel.setBorder(BorderFactory.createEtchedBorder());
        
        layoutSharingDialog();
        
    }
    
    public void refreshForBodySwitch() {
        uploadInProgressFiles.clear();
        allExistingMapsFiles.clear();
        existingMapsFiles.clear();
        sharedMapFiles.clear();
        uploadCompletedFiles.clear();
        ownedByMeExistingMapsFiles = null;
        sharedByMeExistingMapsFiles = null;
        sharedWithMeExistingMapsFiles = null;
        mapNames.clear();
        inProgressNames.clear();
        CustomMapBackendInterface.setCustomMapServerURL();
        
        ArrayList<CustomMap> tempIPFiles = CustomMapBackendInterface.getInProgressCustomMapList();
        uploadInProgressFiles.addAll(tempIPFiles);
        ArrayList<CustomMap> toRemove = new ArrayList<CustomMap>();
        for(CustomMap map : uploadInProgressFiles) {
            if (CustomMap.STATUS_CANCELED.equalsIgnoreCase(map.getStatus())
                || CustomMap.STATUS_COMPLETE.equalsIgnoreCase(map.getStatus()) 
                || CustomMap.STATUS_ERROR.equalsIgnoreCase(map.getStatus())) {
                uploadCompletedFiles.add(map);
                toRemove.add(map);
            } else {
                inProgressNames.add(map.getName());
            }
        }
        uploadInProgressFiles.removeAll(toRemove);
        ArrayList<CustomMap> tempCMFiles = CustomMapBackendInterface.loadExistingMapList();
        existingMapsFiles.addAll(tempCMFiles);
        allExistingMapsFiles.addAll(existingMapsFiles);
        for (CustomMap map : allExistingMapsFiles) {
            mapNames.add(map.getName());
        }
        ArrayList<CustomMap> tempSMFiles = CustomMapBackendInterface.getSharedMapsForUser(allExistingMapsFiles, allSharingGroups);
        sharedMapFiles.addAll(tempSMFiles);
        
        allMapsRadio.setSelected(true);
        filterTextbox.setText("");
        keywordsCheckbox.setSelected(true);
        fileNameCheckbox.setSelected(true);
        ownerCheckbox.setSelected(true);
        descriptionCheckbox.setSelected(true);
        mapNameCheckbox.setSelected(true);
        
        refreshCompletedTable();
        refreshUploadTable();
        refreshExistingMapsTable();
        refreshSharingTable();
    }
    //setting up dialogs and cards for input/edit
    private void setupManualInputDialog() {
        //disable name input if more than one file was selected
        if (selectedFilesModel.size() > 1) {
            nameLbl.setEnabled(false);
            nameInput.setEnabled(false);
        } else {
            nameLbl.setEnabled(true);
            nameInput.setEnabled(true);
        }
    }

    private void clearAllInputs() {
        clearCard1Inputs();
        clearCard2Inputs();
        card_state = CARD_STATE_INITIAL;//reset
    }
    private void clearCard1Inputs() {
        nameInput.setText("");
        ignoreValueInput.setText("");
        unitsInput.setText("");
        northernLatInput.setText("");
        southernLatInput.setText("");
        westernLonInput.setText("");
        easternLonInput.setText("");
        regionalRadio.setSelected(true);
        eastRadio.setSelected(true);
        ocentricRadio.setSelected(true);
        enterMapInfoNoteTextPane.setText(CARD1_NOTE_DEFAULT_MSG);//just a reset to be sure
    }
    private void clearCard2Inputs() {
        citationTextAreaInput.setText("");
        linksTextAreaInput.setText("");
        keywordsTextAreaInput.setText("");
        descriptionTextAreaInput.setText("");
    }
    private void storeCard1InputValues(){
        //set the state of the radio buttons
        int extRadio = -1;
        if (globalRadio.isSelected()) {
            extRadio = CustomMap.EXTENT_INPUT_GLOBAL;
        } else if (regionalRadio.isSelected()) {
            extRadio = CustomMap.EXTENT_INPUT_REGIONAL;
        }
        int degRadio = -1;
        if (eastRadio.isSelected()) {
            degRadio = CustomMap.DEGREES_INPUT_EAST;
        } else if (westRadio.isSelected()) {
            degRadio = CustomMap.DEGREES_INPUT_WEST;
        }
        int shapeRadio = -1;
        if (ocentricRadio.isSelected()) {
            shapeRadio = CustomMap.SHAPE_INPUT_OCENTRIC;
        } else if (ographicRadio.isSelected()) {
            shapeRadio = CustomMap.SHAPE_INPUT_OGRAPHIC;
        }
        //loop through all of the UploadFile objects and set the values
        for (CustomMap file : dialogUploadFileList) {
            //if this is an edit of an existing map or in progress map, remove, then add this name
            if (tableSelectionSource == TABLE_SELECTION_EXISTING) {
                mapNames.remove(file.getName());
            }
            if (tableSelectionSource == TABLE_SELECTION_IN_PROGRESS) {
                inProgressNames.remove(file.getName());
            }
            
            //nameInput is disabled if multiple files are selected
            if (nameInput.isEnabled()) {
                String name = nameInput.getText();
                if (name.trim().length() == 0) {
                    name = file.getBasename();
                }
                file.setName(name);
            } else {
                file.setName(file.getBasename());
            }
            file.addIgnoreValue(ignoreValueInput.getText());
            file.setUnits(unitsInput.getText());
            file.setExtent(extRadio);
            file.setDegrees(degRadio);
            file.setNorthLat(northernLatInput.getText());
            file.setSouthLat(southernLatInput.getText());
            file.setWestLon(westernLonInput.getText());
            file.setEastLon(easternLonInput.getText());
            file.setShapeType(shapeRadio);
            
            if (tableSelectionSource == TABLE_SELECTION_EXISTING) {
                mapNames.add(file.getName());
            }
            if (tableSelectionSource == TABLE_SELECTION_IN_PROGRESS) {
                inProgressNames.add(file.getName());
            }
        }
    }
    private void storeCard2InputValues() {
        for (CustomMap file : dialogUploadFileList) {
            file.setCitation(citationTextAreaInput.getText());
            file.setLinks(linksTextAreaInput.getText());
            file.setKeywords(keywordsTextAreaInput.getText());
            file.setDescription(descriptionTextAreaInput.getText());
        }
    }
    private void setUploadInputs() {
        dialogUploadFileList.clear();//clear out the list that the dialog uses
        selectedFilesModel.clear();//clear out the selected file list (display only)
        CustomMap file = null;
        if (tableSelectionSource == TABLE_SELECTION_COMPLETED) {
            file = getSelectedUploadCompletedFile();
            ocentricRadio.setEnabled(false);
            ographicRadio.setEnabled(false);
        } else if (tableSelectionSource == TABLE_SELECTION_IN_PROGRESS){
            file = getSelectedUploadInProgressFile();
            ocentricRadio.setEnabled(true);
            ographicRadio.setEnabled(true);
        }
        dialogUploadFileList.add(file);//add to the list the dialog uses
        selectedFilesModel.addElement(file.getName());//add to the selected files display
        fillCard1InputsWithUploadFileValues(file);
        fillCard2InputsWithUploadFileValues(file);
    }
    private void setExistingMapInputs() {
        dialogUploadFileList.clear();//clear out the list that the dialog uses
        selectedFilesModel.clear();//clear out the selected file list (display only)
        CustomMap map = getSelectedExistingMap();
        dialogUploadFileList.add(map);//add to the list the dialog uses
        selectedFilesModel.addElement(map.getName());//add to the selected files display
        fillCard1InputsWithUploadFileValues(map);
        fillCard2InputsWithUploadFileValues(map);
        ocentricRadio.setEnabled(false);
        ographicRadio.setEnabled(false);
    }

    private void setReprocessMapEditInputs(CustomMap cm) {
        dialogUploadFileList.clear();//clear out the list that the dialog uses
        selectedFilesModel.clear();//clear out the selected file list (display only)
        dialogUploadFileList.add(cm);//add to the list the dialog uses
        selectedFilesModel.addElement(cm.getName());//add to the selected files display
        fillCard1InputsWithUploadFileValues(cm);
        fillCard2InputsWithUploadFileValues(cm);
        ocentricRadio.setEnabled(true);
        ographicRadio.setEnabled(true);
    }
    private void editModeResetUploadCard1Inputs() {
        CustomMap file = getSelectedUploadInProgressFile();
        fillCard1InputsWithUploadFileValues(file);
        ocentricRadio.setEnabled(true);
        ographicRadio.setEnabled(true);
    }
    private void editModeResetUploadCard2Inputs() {
        CustomMap file = getSelectedUploadInProgressFile();
        fillCard2InputsWithUploadFileValues(file);
    }
    private void editModeResetExistingCard1Inputs() {
        CustomMap file = getSelectedExistingMap();
        fillCard1InputsWithUploadFileValues(file);
        ocentricRadio.setEnabled(false);
        ographicRadio.setEnabled(false);
    }
    private void editModeResetExistingCard2Inputs() {
        CustomMap file = getSelectedExistingMap();
        fillCard2InputsWithUploadFileValues(file);
    }
    private void fillCard2InputsWithUploadFileValues(CustomMap file) {
        String kws = "";
        boolean first = true;
        for (String kw : file.getKeywords()) {
            if (!first) {
                kws += ",";
            }
            kws += kw;
            first = false;
        }
        keywordsTextAreaInput.setText(kws);
        citationTextAreaInput.setText(file.getCitation());
        descriptionTextAreaInput.setText(file.getDescription());
        linksTextAreaInput.setText(file.getLinks());
    }
    private void fillCard1InputsWithUploadFileValues(CustomMap file) {
        nameInput.setText(file.getName());
        ignoreValueInput.setText(file.getIgnoreValue());
        unitsInput.setText(file.getUnits());
        
        String noteMsg = file.getErrorMessage();
        if (noteMsg == null || noteMsg.trim().equals("") || noteMsg.trim().equals("null")) {
            noteMsg = "No processing notes to report.";
        }
        enterMapInfoNoteTextPane.setText("Note: "+noteMsg);
        
        file.formatCornerPoints();
        file.setupGCPsForCompare();
        
        northernLatInput.setText(file.getNorthLat());
        southernLatInput.setText(file.getSouthLat());
        westernLonInput.setText(file.getWestLon());
        easternLonInput.setText(file.getEastLon());
        
        extentButtonGroup.clearSelection();
        if (file instanceof ExistingMap) {
            //editing of an existing map, the global/regional radio buttons are irrelevant until we are able
            //to re-process a map from this dialog. Set it to regional, it is uneditable anyway. 
            regionalRadio.setSelected(true);
            eastRadio.setSelected(true);
        } else {
            int ext = file.getExtent();
            if (ext == CustomMap.EXTENT_INPUT_GLOBAL) {
                globalRadio.doClick();
            } else if (ext == CustomMap.EXTENT_INPUT_REGIONAL) {
                regionalRadio.doClick();
            } else {
                if (globalRadio.isSelected()) {
                    globalRadio.doClick();
                } else {
                    regionalRadio.doClick();
                }
            }
            int deg = file.getDegrees();
            if (deg == CustomMap.DEGREES_INPUT_WEST) {
                westRadio.doClick();
            } else {
                eastRadio.doClick();
            }
        }
        if (file.getShapeType() == CustomMap.SHAPE_INPUT_OCENTRIC) {
            ocentricRadio.setSelected(true);
        } else if (file.getShapeType() == CustomMap.SHAPE_INPUT_OGRAPHIC) {
            ographicRadio.setSelected(true);
        }
    }
    private void addFilesToUploadTable() {
        int start = uploadInProgressFiles.size();
        int end = start;
        for (CustomMap cardInput : dialogUploadFileList) {
            CustomMap file = (CustomMap) cardInput;
            if (!uploadInProgressFiles.contains(file)) {
                uploadInProgressFiles.add(file);
                inProgressNames.add(file.getName());
                end++;
            }
        }
        inProgressUploadFileTableModel.fireTableRowsInserted(start, end);
        scrollToBottom(uploadTableSp);
    }
    private void prepareCardsForEdit() {
        CustomMap file = null;
        if (tableSelectionSource == TABLE_SELECTION_COMPLETED) {
            file = getSelectedUploadCompletedFile();
        } else if (tableSelectionSource == TABLE_SELECTION_IN_PROGRESS){
            file = getSelectedUploadInProgressFile();
        } 
        if (CustomMap.STATUS_AWAITING_USER_INPUT.equals(file.getStatus())) {
            //edit upload mode
            toggleAllCardInputs(true);
            prepareCardButtonRowsForUploadEdit();
        } else {
            //view mode
            toggleAllCardInputs(false);
            prepareCardButtonRowsForUploadView();
        }
    }
    private void prepareCardButtonRowsForUploadView() {
        card_state = CARD_STATE_VIEW_UPLOAD;
        uploadInfoBackAction.setEnabled(false);
        mapInfoUploadButton.setEnabled(false);
        mapInfoClearButton.setEnabled(false);
        mapInfoCancelButton.setText("DONE");
        mapInfoUploadButton.setVisible(true);
        mapInfoReprocessButton.setVisible(false);
        
        mapMetadataUploadButton.setEnabled(false);
        mapMetadataUploadButton.setEnabled(true);
        mapMetadataClearButton.setEnabled(false);
        mapMetadataCancelButton.setText("DONE");
        mapMetadataReprocessButton.setVisible(false);
        mapMetadataUploadButton.setVisible(false);
    }
    private void prepareCardButtonRowsForUploadEdit() {
        card_state = CARD_STATE_EDIT_UPLOAD;
        uploadInfoBackAction.setEnabled(false);
        mapInfoUploadButton.setEnabled(true);
        mapInfoClearButton.setEnabled(true);
        mapInfoUploadButton.setText("PROCESS");
        mapInfoUploadButton.setVisible(true);
        mapInfoClearButton.setText("RESET");
        mapInfoCancelButton.setText("CANCEL");
        mapInfoReprocessButton.setVisible(false);
        
        mapMetadataUploadButton.setEnabled(true);
        mapMetadataClearButton.setEnabled(true);
        mapMetadataUploadButton.setText("PROCESS");
        mapMetadataClearButton.setText("RESET");
        mapMetadataCancelButton.setText("CANCEL");
        mapMetadataReprocessButton.setVisible(false);
        mapMetadataUploadButton.setVisible(true);
    }
    private void prepareCardButtonRowsForExistingMapEdit() {
        card_state = CARD_STATE_EDIT_EXISTING;
        uploadInfoBackAction.setEnabled(false);
        mapInfoUploadButton.setEnabled(true);
        mapInfoClearButton.setEnabled(true);
        mapInfoUploadButton.setText("SAVE");
        mapInfoClearButton.setText("RESET");
        mapInfoCancelButton.setText("CANCEL");
        mapInfoReprocessButton.setVisible(false);
        mapInfoUploadButton.setVisible(true);
        
        mapMetadataUploadButton.setEnabled(true);
        mapMetadataClearButton.setEnabled(true);
        mapMetadataUploadButton.setText("SAVE");
        mapMetadataClearButton.setText("RESET");
        mapMetadataCancelButton.setText("CANCEL");
        mapMetadataReprocessButton.setVisible(false);
        mapMetadataUploadButton.setVisible(true);
    }
    private void prepareCardButtonRowsForReprocessMap() {
//        card_state = CARD_STATE_EDIT_EXISTING; //Reprocess state?
    	card_state = CARD_STATE_REPROCESS;
        uploadInfoBackAction.setEnabled(false);
        mapInfoUploadButton.setVisible(false);
        mapInfoClearButton.setEnabled(true);
        
//        mapInfoUploadButton.setText("SAVE");
        mapInfoUploadButton.setVisible(false);
        mapInfoClearButton.setText("RESET");
        mapInfoCancelButton.setText("CANCEL");
        mapInfoReprocessButton.setVisible(true);
        
        mapMetadataClearButton.setEnabled(true);
        mapMetadataUploadButton.setVisible(false);
        mapMetadataClearButton.setText("RESET");
        mapMetadataCancelButton.setText("CANCEL");  
        mapMetadataReprocessButton.setVisible(true);
    }
    private void toggleAllCardInputsForExistingMapEdit() {
        nameInput.setEnabled(true);
        ignoreValueInput.setEnabled(true);
        unitsInput.setEnabled(true);
        northernLatInput.setEnabled(false);
        southernLatInput.setEnabled(false);
        westernLonInput.setEnabled(false);
        easternLonInput.setEnabled(false);
        globalRadio.setEnabled(false);
        regionalRadio.setEnabled(false);
        eastRadio.setEnabled(false);
        westRadio.setEnabled(false);
        descriptionTextAreaInput.setEnabled(true);
        linksTextAreaInput.setEnabled(true);
        citationTextAreaInput.setEnabled(true);
        keywordsTextAreaInput.setEnabled(true);
        ocentricRadio.setEnabled(false);
        ographicRadio.setEnabled(false);
    }
    private void toggleAllCardInputsForReprocessMap() {
        nameInput.setEnabled(true);
        ignoreValueInput.setEnabled(true);
        unitsInput.setEnabled(true);
        northernLatInput.setEnabled(true);
        southernLatInput.setEnabled(true);
        westernLonInput.setEnabled(true);
        easternLonInput.setEnabled(true);
        globalRadio.setEnabled(true);
        regionalRadio.setEnabled(true);
        eastRadio.setEnabled(true);
        westRadio.setEnabled(true);
        descriptionTextAreaInput.setEnabled(true);
        linksTextAreaInput.setEnabled(true);
        citationTextAreaInput.setEnabled(true);
        keywordsTextAreaInput.setEnabled(true);
        ocentricRadio.setEnabled(true);
        ographicRadio.setEnabled(true);
    }
    
    private void toggleAllCardInputs(boolean enabled) {
        nameInput.setEnabled(enabled);
        ignoreValueInput.setEnabled(enabled);
        unitsInput.setEnabled(enabled);
        northernLatInput.setEnabled(enabled);
        southernLatInput.setEnabled(enabled);
        westernLonInput.setEnabled(enabled);
        easternLonInput.setEnabled(enabled);
        globalRadio.setEnabled(enabled);
        regionalRadio.setEnabled(enabled);
        eastRadio.setEnabled(enabled);
        westRadio.setEnabled(enabled);
        descriptionTextAreaInput.setEnabled(enabled);
        linksTextAreaInput.setEnabled(enabled);
        citationTextAreaInput.setEnabled(enabled);
        keywordsTextAreaInput.setEnabled(enabled);
        ocentricRadio.setEnabled(enabled);
        ographicRadio.setEnabled(enabled);
    }
    private void resetCardInputs() {
        nameInput.setEnabled(true);
        ignoreValueInput.setEnabled(true);
        unitsInput.setEnabled(true);
        northernLatInput.setEnabled(true);
        southernLatInput.setEnabled(true);
        westernLonInput.setEnabled(true);
        easternLonInput.setEnabled(true);
        globalRadio.setEnabled(true);
        regionalRadio.setEnabled(true);
        eastRadio.setEnabled(true);
        westRadio.setEnabled(true);
        descriptionTextAreaInput.setEnabled(true);
        linksTextAreaInput.setEnabled(true);
        citationTextAreaInput.setEnabled(true);
        keywordsTextAreaInput.setEnabled(true);
        ocentricRadio.setEnabled(true);
        ographicRadio.setEnabled(true);
        
        uploadInfoBackAction.setEnabled(true);
        mapInfoUploadButton.setEnabled(true);
        mapInfoClearButton.setEnabled(true);

        
        mapInfoCancelButton.setText("CANCEL");
        mapInfoClearButton.setText("CLEAR");
        mapInfoUploadButton.setText("UPLOAD");


        mapMetadataCancelButton.setText("CANCEL");
        mapMetadataUploadButton.setText("UPLOAD");
        mapMetadataClearButton.setText("CLEAR");
        
        mapMetadataClearButton.setEnabled(true);
        mapMetadataUploadButton.setEnabled(true);
        mapInfoUploadButton.setVisible(true);
        mapMetadataUploadButton.setVisible(true);
        mapInfoReprocessButton.setVisible(false);
        mapMetadataReprocessButton.setVisible(false);
        
    }
    //Actions
    private AbstractAction selectFilesAction = new AbstractAction("START NEW UPLOAD") {
        @Override
        public void actionPerformed(ActionEvent e) {
            uploadInProgressTable.clearSelection();//necessary so that the selected file is the coming from the file chooser
            tableSelectionSource = TABLE_SELECTION_FILE_CHOOSER;
            //If the user selects approve on the file chooser
            if(fileChooser.showOpenDialog(CM_Manager.this) == JFileChooser.APPROVE_OPTION){
                //pop the dialog
                resetCardInputs();
                mainDialogLayout.replace(mainDialogActivePanel, uploadOptionsPanel);//start at the options page
                mainDialogActivePanel = uploadOptionsPanel;//state of which panel is active
                
                selectedFilesModel.clear();
                dialogUploadFileList.clear();
                
                clearAllInputs();
                File[] selFiles = fileChooser.getSelectedFiles();
                for (File f : selFiles) {
                    String name = f.getName();
                    if (name != null && name.trim().length() > 0) {
                        selectedFilesModel.addElement(name);
                    }
                    CustomMap file = new UploadFile(f.getAbsolutePath());
                    dialogUploadFileList.add(file);
                }
                if (selFiles.length == 1) {
                    nameInput.setText(selectedFilesModel.getElementAt(0));
                }
                
                //shape type, set earth to ographic by default
                if ("earth".equalsIgnoreCase(Main.getCurrentBody())) {
                    ographicRadio.setSelected(true);
                } else {
                    ocentricRadio.setSelected(true);
                }
                showMainDialog();
            }
        }
    };
    private void showMainDialog() {
        pack();
        mainDialog.setVisible(true);
        mainDialog.setLocationRelativeTo(fileChooser);
        mainDialog.requestFocus();
    }
    //upload option actions
    private AbstractAction uploadOptionsContinueAction = new AbstractAction("CONTINUE") {
        @Override
        public void actionPerformed(ActionEvent e) {
            if (uploadAndProcessRadio.isSelected()) {
                kickOffUploadAllFiles(CustomMap.PROCESSING_OPTION_UPLOAD);
                addFilesToUploadTable();
                clearAllInputs();
                mainDialog.setVisible(false);
                kickOffMonitorThread();
            } else if (manauallyEnterRadio.isSelected()) {
                //this needs to be done here to set the manual flag in the setHeaderValues call
                for (CustomMap uploadFile : dialogUploadFileList) {
                    uploadFile.setSelectedUploadProcess(CustomMap.PROCESSING_OPTION_MANUAL);
                }
                setupManualInputDialog();
                mainDialogLayout.replace(uploadOptionsPanel, uploadInfoPanel);
                mainDialogActivePanel = uploadInfoPanel;
                pack();
            } else if (verifyInformationRadio.isSelected()) {
                kickOffUploadAllFiles(CustomMap.PROCESSING_OPTION_PROMPT);
                addFilesToUploadTable();
                clearAllInputs();
                mainDialog.setVisible(false);
                kickOffMonitorThread();
            } else {
                Util.showMessageDialog("Please select an option.","Select an Option",JOptionPane.PLAIN_MESSAGE);
            }
        }
    };
    private AbstractAction uploadOptionsCancelAction = new AbstractAction("CANCEL") {
        @Override
        public void actionPerformed(ActionEvent e) {
            clearAllInputs();
            mainDialog.setVisible(false);
        }
    };
    //end uploadOption actions
    //uploadInfo actions
    private AbstractAction uploadInfoUploadAction = new AbstractAction("UPLOAD") {//Upload, process, save are possible labels
        @Override
        public void actionPerformed(ActionEvent e) {
            if (card_state == CARD_STATE_EDIT_EXISTING) {
                //editing an existing map here
                if (validateCard1Inputs()) {
                    storeCard1InputValues();
                    storeCard2InputValues();
                    CustomMap map = getSelectedExistingMap();
                    if (!CustomMapBackendInterface.saveMetadata(map)) {
                    	//ignoreValueInput.setText("");
                    	Util.showMessageDialog("The ignore value is not valid and must be numeric", "Input Error", JOptionPane.PLAIN_MESSAGE);
                    } else {
                        clearAllInputs();
                        mainDialog.setVisible(false);
                        refreshExistingMapsRow(existingMapsTable.getSelectedRow());
                        CustomMapBackendInterface.finishMapEdit(map);	
                    }
//                    clearAllInputs();
//                    mainDialog.setVisible(false);
//                    refreshExistingMapsRow(existingMapsTable.getSelectedRow());
//                    CustomMapBackendInterface.finishMapEdit(map);
                }
            } else {
                if (validateCard1Inputs()) {
                    storeCard1InputValues();
                    storeCard2InputValues();
                    if (card_state == CARD_STATE_EDIT_UPLOAD) {
                        //we are in edit/process mode here
                        processInProgressFile(getSelectedUploadInProgressFile());
                        if (processInProgress) {
                            addFilesToUploadTable();
                            clearAllInputs();
                            mainDialog.setVisible(false);
                            kickOffMonitorThread();
                        } else {
                        	Util.showMessageDialog("The ignore value is not valid and must be numeric", "Input Error", JOptionPane.PLAIN_MESSAGE);
                        }
                    } else {
                        kickOffUploadAllFiles(CustomMap.PROCESSING_OPTION_MANUAL);
                        addFilesToUploadTable();
                        clearAllInputs();
                        mainDialog.setVisible(false);
                        kickOffMonitorThread();
                    }
                } else {
                    mainDialog.requestFocus();
                }
            }
        }
    };
    private AbstractAction uploadInfoNextAction = new AbstractAction("NEXT") {
        @Override
        public void actionPerformed(ActionEvent e) {
            mainDialogLayout.replace(uploadInfoPanel, uploadMetadataPanel);
            mainDialogActivePanel = uploadMetadataPanel;
            pack();
        }
    };
    private AbstractAction uploadInfoBackAction = new AbstractAction("BACK") {
        @Override
        public void actionPerformed(ActionEvent e) {
            mainDialogLayout.replace(uploadInfoPanel, uploadOptionsPanel);
            mainDialogActivePanel = uploadOptionsPanel;
            pack();
        }
    };
    private AbstractAction uploadInfoClearAction = new AbstractAction("CLEAR") {
        @Override
        public void actionPerformed(ActionEvent e) {
        //if we are editing and resetting values   
            if (card_state == CARD_STATE_EDIT_UPLOAD) {
                editModeResetUploadCard1Inputs();
            } else if (card_state == CARD_STATE_EDIT_EXISTING) {
                editModeResetExistingCard1Inputs();
            } else {
                //clear
                clearCard1Inputs();
            }
        }
    };
    private AbstractAction uploadInfoCancelAction = new AbstractAction("CANCEL") {
        @Override
        public void actionPerformed(ActionEvent e) {
            clearAllInputs();
            mainDialog.setVisible(false);
        }
    };
    //end upload info actions
    //upload metadata actions
    private AbstractAction uploadMetadataCancelAction = new AbstractAction("CANCEL") {

        @Override
        public void actionPerformed(ActionEvent e) {
            clearAllInputs();
            mainDialog.setVisible(false);
        }
        
    };
    private AbstractAction uploadMetadataClearAction = new AbstractAction("CLEAR") {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (card_state == CARD_STATE_EDIT_UPLOAD) {
               editModeResetUploadCard2Inputs();
            } else if (card_state == CARD_STATE_EDIT_EXISTING) {
               editModeResetExistingCard2Inputs();
            } else {
                //clear
                clearCard2Inputs();
            }
        }
        
    };
    private AbstractAction uploadMetadataUploadAction = new AbstractAction("UPLOAD") {

        @Override
        public void actionPerformed(ActionEvent e) {
            if (card_state == CARD_STATE_EDIT_EXISTING) {
                //editing an existing map here
                if (validateCard1Inputs()) {
                    storeCard1InputValues();
                    storeCard2InputValues();
                    CustomMap map = getSelectedExistingMap();
                    if (!CustomMapBackendInterface.saveMetadata(map)) {
                    	Util.showMessageDialog("The ignore value is not valid and must be numeric", "Input Error", JOptionPane.PLAIN_MESSAGE);
                    } else {
						clearAllInputs();
                        mainDialog.setVisible(false);
                        refreshExistingMapsRow(existingMapsTable.getSelectedRow());
                        CustomMapBackendInterface.finishMapEdit(map);
                    }
                }
            } else {
                if (validateCard1Inputs()) {
                    storeCard1InputValues();
                    storeCard2InputValues();
                    if (card_state == CARD_STATE_EDIT_UPLOAD) {
                        //we are in edit/process mode here
                        processInProgressFile(getSelectedUploadInProgressFile());
                        if (processInProgress) {
                            addFilesToUploadTable();
                            clearAllInputs();
                            mainDialog.setVisible(false);
                            kickOffMonitorThread();
                        } else {
                        	Util.showMessageDialog("The ignore value is not valid and must be numeric", "Input Error", JOptionPane.PLAIN_MESSAGE);
                        }
                    } else {
                        kickOffUploadAllFiles(CustomMap.PROCESSING_OPTION_MANUAL);
                        addFilesToUploadTable();
                        clearAllInputs();
                        mainDialog.setVisible(false);
                        kickOffMonitorThread();
                    }
//                    addFilesToUploadTable();
//                    clearAllInputs();
//                    mainDialog.setVisible(false);
//                    kickOffMonitorThread();
                } else {
                    mainDialog.requestFocus();
                }
            }
        }
        
    };
    private AbstractAction reprocessMapAction = new AbstractAction("REPROCESS") {

        @Override
        public void actionPerformed(ActionEvent e) {
//            if (card_state == CARD_STATE_EDIT_EXISTING) {
        	if (card_state == CARD_STATE_REPROCESS) {
                //editing an existing map here/reprocess
                if (validateCard1Inputs()) {
                    storeCard1InputValues(); 
                    storeCard2InputValues();
                    //close dialog
                    mainDialog.setVisible(false);
                    //use the customMap set by uploads
                    reprocessedCustomMap.setReprocess(true);
                    if (CustomMapBackendInterface.processReprocessMap(reprocessedCustomMap, CustomMap.STATUS_REPROCESSED)) {
	                    refreshExistingMaps();                   
	                    addFilesToUploadTable();
	                    updateCompletedTableReprocess();
	                    kickOffMonitorThread();
                    } else {
                    	Util.showMessageDialog("The ignore value is not valid and must be numeric", "Input Error", JOptionPane.PLAIN_MESSAGE);
                    }
                }
            }
        }  
    };
    private AbstractAction uploadMetadataBackAction = new AbstractAction("BACK") {

        @Override
        public void actionPerformed(ActionEvent e) {
            mainDialogLayout.replace(uploadMetadataPanel, uploadInfoPanel);
            mainDialogActivePanel = uploadInfoPanel;
            pack();
        }
        
    };
    //end upload metadata actions
    //existing map actions
    private AbstractAction existingMapEditAction = new AbstractAction("EDIT") {
        @Override
        public void actionPerformed(ActionEvent e) {
            doEditExistingMap();
        }
    };
    private void doEditExistingMap() {
        clearAllInputs();
        tableSelectionSource = TABLE_SELECTION_EXISTING;
        setExistingMapInputs();
        toggleAllCardInputsForExistingMapEdit();
        prepareCardButtonRowsForExistingMapEdit();
        mainDialogLayout.replace(mainDialogActivePanel, uploadInfoPanel);
        mainDialogActivePanel = uploadInfoPanel;
        showMainDialog();
    }
    public void showSharingDialog() {
        //only make these requests to the backend if they actually open up the dialog
        if (allSharingUsers == null) {
            allSharingUsers = CustomMapBackendInterface.getUserSharingList();
        }
        if (sharedMapFiles == null || sharedMapFiles.size() == 0) {
            sharedMapFiles.addAll(CustomMapBackendInterface.getSharedMapsForUser(allExistingMapsFiles,allSharingGroups));
            refreshSharingTable();
        }
        sharedWithGroupsModel.clear();
        sharedWithUsersModel.clear();
        availableGroupsModel.clear();
        availableUsersModel.clear();
        sharedMapsTable.clearSelection();
        sharingManagementDialog.setVisible(true);
        pack();
    }
    private void saveSharingChanges() {
        int selectedRow = sharedMapsTable.getSelectedRow();
        boolean somethingChanged = false;
        for (CustomMap map : sharedMapFiles) {
            if (map.hasSharingUserChanges()) {
                if (CustomMapBackendInterface.shareMapWithUsers(map)) {
                    map.updateSharedUsers();
                }
                somethingChanged = true;
            }
            if (map.hasSharingGroupChanges()) {
                if (CustomMapBackendInterface.shareMapWithGroups(map)) {
                    map.updateSharedGroups();
                }
                somethingChanged = true;
            }
            if (somethingChanged) {
                if (sharedByMeExistingMapsFiles != null) {//could be null if they never clicked the radio button
                    if (!sharedByMeExistingMapsFiles.contains(map)) {
                        sharedByMeExistingMapsFiles.add(map);
                    }
                }
            }
            somethingChanged = false;
        }
        refreshSharingTable();
        if (selectedRow > -1) {
            sharedMapsTable.setRowSelectionInterval(selectedRow, selectedRow);
        }
        refreshExistingMapsTable();
    }
    private AbstractAction manageSharingAction = new AbstractAction("MANAGE SHARING") {
        @Override
        public void actionPerformed(ActionEvent e) {
            showSharingDialog();
        }
    };
    private AbstractAction existingMapAddLayerAction = new AbstractAction("ADD LAYER") {
        @Override
        public void actionPerformed(ActionEvent e) {
            addMapLayer();
        }
    };
    private AbstractAction existingMapDeleteAction = new AbstractAction("DELETE") {
        @Override
        public void actionPerformed(ActionEvent e) {
            deleteExistingMaps();
        }
    };
    private AbstractAction existingMapReprocessAction = new AbstractAction("REPROCESS MAP") {
		
		@Override
		public void actionPerformed(ActionEvent e) {
			reprocessedCustomMap = new UploadFile(getSelectedExistingMap());
			reprocessMap(reprocessedCustomMap);
		}
	};
    private AbstractAction existingMapShareAction = new AbstractAction("SHARE") {
        @Override
        public void actionPerformed(ActionEvent e) {
            shareExistingMaps();
        }
    };
    private AbstractAction filterMapsAction = new AbstractAction("APPLY FILTER") {
        @Override
        public void actionPerformed(ActionEvent e) {
            filterExistingMaps();
        }
    };
    private AbstractAction clearFilterMapsAction = new AbstractAction("CLEAR FILTER") {
        @Override
        public void actionPerformed(ActionEvent e) {
            clearExistingMapsFilter();
        }
    };
    private AbstractAction refreshMapsAction = new AbstractAction("REFRESH MAPS") {
        @Override
        public void actionPerformed(ActionEvent e) {
            refreshExistingMaps();
        }
    };
    private AbstractAction mapFilterRadioAction = new AbstractAction() {
        @Override
        public void actionPerformed(ActionEvent e) {
            filterExistingMapsByRadioButton();
        }
    };
    private void shareExistingMaps() {
        ArrayList<CustomMap> mapsToShare = getSelectedExistingMaps();
        for (CustomMap map : mapsToShare) {
            if (!sharedMapFiles.contains(map)) {
                sharedMapFiles.add(map);
            }
        }
        
        refreshSharingTable();
        showSharingDialog();
        //select map in sharing table if only one map was shared
        if (mapsToShare.size() == 1) {
            Sorter sorter = sharedMapsTable.getSorter();
            for (int i=0; i<sharedMapFiles.size(); i++) {
                int unsortRow = sorter.unsortRow(i);
                CustomMap map = sharingTableModel.getMap(unsortRow);
                if (map == mapsToShare.get(0)) {
                    sharedMapsTable.setRowSelectionInterval(unsortRow, unsortRow);
                    break;
                }
            }
        }
        scrollToBottom(sharedMapsSp);
    }
    //Upload tab button actions
    private AbstractAction viewEditUploadAction = new AbstractAction("VIEW/EDIT") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            clearAllInputs();
            tableSelectionSource = TABLE_SELECTION_IN_PROGRESS;
            setUploadInputs();
            prepareCardsForEdit();
            mainDialogLayout.replace(mainDialogActivePanel, uploadInfoPanel);
            mainDialogActivePanel = uploadInfoPanel;
            showMainDialog();
        }
    };
    private AbstractAction cancelUploadAction = new AbstractAction("CANCEL") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            //TODO: confirm dialog before canceling
            CustomMap file = getSelectedUploadInProgressFile();
            //do not change the status of the UploadFile here. It will be managed in the cancelUpload call
            if (file.getCustomMapId() != null ) {//should never be null
                //CustomMapBackendInterface.setCancelFlag(file);
                CustomMapBackendInterface.cancelUpload(file);
                file.setStatus(CustomMap.STATUS_CANCELED);
                file.setCancelFlag(true);
                kickOffMonitorThread();
            }
        }
    };
    private AbstractAction clearAllCompletedUploadAction = new AbstractAction("CLEAR ALL") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            clearAllCompletedButton.setEnabled(false);
            int response = Util.showConfirmDialog( 
                    "Are you sure you want to clear all completed/canceled/error maps?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                ArrayList<CustomMap> toRemove = new ArrayList<CustomMap>();
                for (CustomMap file : uploadCompletedFiles) {
                    if (CustomMap.STATUS_CANCELED.equalsIgnoreCase(file.getStatus())
                        || CustomMap.STATUS_ERROR.equalsIgnoreCase(file.getStatus())) {
                        CustomMapBackendInterface.deleteUploadFile(file);
                    }
                }
                uploadCompletedFiles.clear();
                refreshCompletedTable();
            }
            clearAllCompletedButton.setEnabled(true);
        }
    };
    // after reprocess,  if reprocessed map is successful the original image needs to be deleted from completed table
    private void updateCompletedTableReprocess() {
    	// if the map exists and the reprocess map was completed, remove original map
    	ArrayList<CustomMap> toRemove = new ArrayList<CustomMap>();
    	for (CustomMap mapFile : uploadCompletedFiles) {
    		if ((mapFile.getCustomMapId().equals(reprocessedCustomMap.getReprocessParentId()))) {//&& CustomMap.STATUS_REPROCESSED.equalsIgnoreCase(reprocessedCustomMap.getStatus())) {
    			toRemove.add(mapFile);
    		}
    	}
		uploadCompletedFiles.removeAll(toRemove);
		refreshCompletedTable();
    }
    private AbstractAction clearCompletedUploadAction = new AbstractAction("CLEAR SELECTED") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            clearSelectedButton.setEnabled(false);
            int response = Util.showConfirmDialog( 
                    "Are you sure you want to clear selected completed/canceled/error maps?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
            if (response == JOptionPane.YES_OPTION) {
                Sorter sorter = uploadCompletedTable.getSorter();
                ArrayList<CustomMap> toRemove = new ArrayList<CustomMap>();
                int[] selRows = uploadCompletedTable.getSelectedRows();
                for (int idx : selRows) {
                    int row = sorter.unsortRow(idx);
                    CustomMap file = completedUploadFileTableModel.getFile(row);
                    if (CustomMap.STATUS_COMPLETE.equalsIgnoreCase(file.getStatus())) {
                        toRemove.add(file);
                    } else {
                        if (CustomMapBackendInterface.deleteUploadFile(file)) {
                            toRemove.add(file);
                        } else {
                            String msg = "There was a problem clearing the selected uploads. \nIf this problem persists, please select Help->Report a Problem to contact support.";
                            Util.showMessageDialog(msg, "Error", JOptionPane.ERROR_MESSAGE);
                        }
                    }
                }
                uploadCompletedFiles.removeAll(toRemove);
                refreshCompletedTable();
            }
            clearSelectedButton.setEnabled(true);
        }
    };
    private AbstractAction viewCompletedUploadAction = new AbstractAction("VIEW") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            clearAllInputs();
            tableSelectionSource = TABLE_SELECTION_COMPLETED;
            setUploadInputs();
            prepareCardsForEdit();
            mainDialogLayout.replace(mainDialogActivePanel, uploadInfoPanel);
            mainDialogActivePanel = uploadInfoPanel;
            showMainDialog();
        }
    };

    private AbstractAction refreshStatusAction = new AbstractAction("REFRESH STATUS OF UPLOADS") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            kickOffMonitorThread();
        }
    };


    private AbstractAction doneSharingAction = new AbstractAction("CLOSE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (isSharingDialogDirty) {
                int confirm = Util.showConfirmDialog(
                    "You may have unsaved sharing changes. Are you sure you want to close without saving? \n"
                    + "Note: the changes will exist, and can be saved until JMARS is restarted.", "Confirm Done", JOptionPane.YES_NO_OPTION);
                if (JOptionPane.YES_OPTION == confirm) {
                    sharingManagementDialog.setVisible(false);
                }
            } else {
                sharingManagementDialog.setVisible(false);
            }
            
        }
    };
    private AbstractAction saveSharingAction = new AbstractAction("SAVE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            saveSharingChanges(); 
            saveSharingChangesButton.setEnabled(false);
            sharingSaveAndCloseButton.setEnabled(false);
            isSharingDialogDirty = false;
        }
    };
    private AbstractAction sharingSaveAndCloseAction = new AbstractAction("SAVE AND CLOSE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            saveSharingChanges();
            saveSharingChangesButton.setEnabled(false);
            sharingSaveAndCloseButton.setEnabled(false);
            isSharingDialogDirty = false;
            sharingManagementDialog.setVisible(false);
        }
    };
    private AbstractAction manageUsersRemoveAction = new AbstractAction("REMOVE SELECTED") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            manageUsersDirtyFlag = true;
            List<String> selectedValuesList = manageUsersList.getSelectedValuesList();
            for (String val : selectedValuesList) {
                manageUsersListModel.removeElement(val);
            }
            manageUsersRemoveButton.setEnabled(false);
        }
    };
    private AbstractAction manageUsersAddAction = new AbstractAction("ADD USER") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            manageUsersMsgLbl.setVisible(false);
            String user = searchUserInput.getText();
            if (user.trim().length() > 0) {
            	if (user.equalsIgnoreCase(Main.USER)) {
            		manageUsersMsgLbl.setText("You can't add your username.");
                    manageUsersMsgLbl.setVisible(true);      		
            	}
            	else if (CustomMapBackendInterface.checkValidUser(user)) {
                    manageUsersDirtyFlag = true;
                    if (!manageUsersListModel.contains(user)) {
                        manageUsersListModel.addElement(user);
                        searchUserInput.setText("");
                    }
                } else {
                    manageUsersMsgLbl.setText("Invalid user entered");
                    manageUsersMsgLbl.setVisible(true);
                }
            }
        }
    };
    private AbstractAction manageUsersSaveAction = new AbstractAction("SAVE AND CLOSE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            manageUsersDirtyFlag = false;
            ArrayList<String> toRemove = new ArrayList<String>();
            
            for(String user : allSharingUsers) {//loop through the complete list of favorite users
                if (!manageUsersListModel.contains(user)) {//if the user is not currently in the selected favorite list, flag for removal
                    toRemove.add(user);
                    if (availableUsersModel.contains(user)) {//if it is in the sharing dialog available user list remove it
                        availableUsersModel.removeElement(user);
                    }
                    if (manageGroupsAvailableModel.contains(user)) {
                        manageGroupsAvailableModel.removeElement(user);
                    }
                }
                
            }
            allSharingUsers.removeAll(toRemove);//remove all the favorite users that were not still in the list
            
            for (int x=0; x<manageUsersListModel.size(); x++) {//now let's add in the new users in the list. (Don't remove from "not shared" lists).
                if (!allSharingUsers.contains(manageUsersListModel.get(x))) {
                    String user = manageUsersListModel.get(x);
                    allSharingUsers.add(user);
                    if (sharedMapsTable.getSelectedRowCount() == 1) {
                        if (!availableUsersModel.contains(user) && !sharedWithUsersModel.contains(user)) {
                            //if it is not already in the favorite list and it is not already selected for this map, add the username to the favorite
                            //list on the sharing dialog
                            availableUsersModel.addElement(user);
                        }
                    }
                    if (manageGroupsDialog.isVisible()) {
                        //let's update manage groups dialog
                        if (groupComboBox.getSelectedIndex() > 0) {
                            if (!manageGroupsAvailableModel.contains(user) && !manageGroupsUsersModel.contains(user)) {
                                manageGroupsAvailableModel.addElement(user);
                            }
                        }
                    }
                }
            }
            
            searchUserInput.setText("");//clear out the add user input on manage users
            manageUsersMsgLbl.setText("");
            manageUsersMsgLbl.setVisible(false);
            
            //allSharingUsers should now be up to date and ready for sharing table selection action.
            //update the list of users in the database
            if (CustomMapBackendInterface.updateSharingUserList(allSharingUsers)) {
                manageUsersDialog.setVisible(false);
                manageUsersDialog.dispose();
            }
        }
    };
    private AbstractAction manageUsersDoneAction = new AbstractAction("CLOSE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            boolean continueFlag = false;
            if (manageUsersDirtyFlag) {
                int confirm = Util.showConfirmDialog( 
                    "You have unsaved changes. Are you sure you want to close without saving?", "Confirm Close", JOptionPane.YES_NO_OPTION);
                if (JOptionPane.YES_OPTION == confirm) {
                    continueFlag = true;
                }
            }
            if (continueFlag || !manageUsersDirtyFlag) {
                manageUsersMsgLbl.setVisible(false);
                searchUserInput.setText("");
                manageUsersMsgLbl.setText("");
                manageUsersMsgLbl.setVisible(false);
                manageUsersDialog.setVisible(false);
                manageUsersDialog.dispose();
            }
        }
    };
    private AbstractAction manageUserListAction = new AbstractAction("FAVORITE USERS") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            showManageUsersDialog(sharingManagementDialog);
        }
    };
    private AbstractAction groupUserListAction = new AbstractAction("FAVORITE USERS") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            showManageUsersDialog(manageGroupsDialog);
        }
    };
    
    private boolean isSharingDialogDirty = false;
    private void setSharingDialogDirty() {
        isSharingDialogDirty = true;
        saveSharingChangesButton.setEnabled(true);
        sharingSaveAndCloseButton.setEnabled(true);
    }
    private AbstractAction addGroupShareAction = new AbstractAction(">>") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            List<String> selectedGroups = availableGroupsList.getSelectedValuesList();
            DefaultListModel<String> model = (DefaultListModel<String>) availableGroupsList.getModel();
            for (int i=model.getSize()-1; i>=0; i--) {
                String group = model.getElementAt(i);
                if (selectedGroups.contains(group)) {
                    if (!sharedWithGroupsModel.contains(group)) {
                        sharedWithGroupsModel.addElement(group);
                        model.remove(i);
                        for (SharingGroup aGroup : allSharingGroups) {
                            if (aGroup.getName().trim().equalsIgnoreCase(group)) {
                                getSelectedSharingMap().addTempSharedGroup(aGroup);
                                setSharingDialogDirty();
                                break;
                            }
                        }
                    }
                }
            }
        }
    };
    
    private AbstractAction removeGroupShareAction = new AbstractAction("<<") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            List<String> selectedGroups = sharedWithGroupsList.getSelectedValuesList();
            DefaultListModel<String> model = (DefaultListModel<String>) sharedWithGroupsList.getModel();
            for (int i=model.getSize()-1; i>=0; i--) {
                String group = model.getElementAt(i);
                if (selectedGroups.contains(group)) {
                    if (!availableGroupsModel.contains(group)) {
                        availableGroupsModel.addElement(group);
                        model.remove(i);
                        getSelectedSharingMap().removeTempSelectedGroup(group);
                        setSharingDialogDirty();
                    }
                }
            }
            
        }
    };
    private AbstractAction shareWithUserAction = new AbstractAction(">>") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            List<String> selectedUsers = availableUsersList.getSelectedValuesList();
            DefaultListModel<String> model = (DefaultListModel<String>) availableUsersList.getModel();
            for (int i=model.getSize()-1; i>=0; i--) {
                String user = model.getElementAt(i);
                if (selectedUsers.contains(user)) {
                    if (!sharedWithUsersModel.contains(user)) {
                        sharedWithUsersModel.addElement(user);
                        model.remove(i);
                        getSelectedSharingMap().addTempSharedUser(user);
                        setSharingDialogDirty();
                    }
                }
            }
            
        }
    };
    private AbstractAction unshareWithUserAction = new AbstractAction("<<") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            List<String> selectedUsers = sharedWithUsersList.getSelectedValuesList();
            DefaultListModel<String> model = (DefaultListModel<String>) sharedWithUsersList.getModel();
            for (int i=model.getSize()-1; i>=0; i--) {
                String user = model.getElementAt(i);
                if (selectedUsers.contains(user)) {
                    if (!availableUsersModel.contains(user)) {
                        availableUsersModel.addElement(user);
                        model.remove(i);
                        getSelectedSharingMap().removeTempSelectedUser(user);
                        setSharingDialogDirty();
                    }
                }
            }
        }
    };
    private AbstractAction manageGroupsAction = new AbstractAction("MANAGE GROUPS") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            populateGroupComboBox();
            manageGroupsDialog.setVisible(true);
        }
    };
    private void populateGroupComboBox() {
        groupComboBoxModel.removeAllElements();
        groupComboBoxModel.addElement("");
        for(SharingGroup group : allSharingGroups) {
            groupComboBoxModel.addElement(group.getName());
        }
    }
    private void populateGroupLists() {
        String selectedGroup = (String)groupComboBox.getSelectedItem();
        ArrayList<String> groupUsers = null;
        manageGroupsUsersModel.clear();
        manageGroupsAvailableModel.clear();
        if (selectedGroup.trim().length() > 0) {
            for(SharingGroup group : allSharingGroups) {
                if (group.getName().equals(selectedGroup)) {
                    groupUsers = group.getUsers();
                    for (String user : groupUsers) {
                        manageGroupsUsersModel.addElement(user);
                    }
                    break;
                }
            }
            ArrayList<String> groupAvailUsers = new ArrayList<String>();
            if (allSharingUsers != null) {
                groupAvailUsers.addAll(allSharingUsers);
            }
            if (groupUsers != null) {
                groupAvailUsers.removeAll(groupUsers);
                for(String user : groupAvailUsers) {
                    manageGroupsAvailableModel.addElement(user);
                }
            }
            groupRenameButton.setEnabled(true);
            groupDeleteButton.setEnabled(true);
        } else {
            groupRenameButton.setEnabled(false);
            groupDeleteButton.setEnabled(false);
        }
    }
    private void storeGroupInfo() {
        if (groupListsChangedFlag) {
            if (previouslySelectedGroup != null && previouslySelectedGroup.trim().length() > 0) {
                for(SharingGroup group : allSharingGroups) {
                    if (group.getName().equals(previouslySelectedGroup)) {
                        group.clearUsers();
                        for (int i=0; i<manageGroupsUsersModel.size(); i++) {
                            group.addUser(manageGroupsUsersModel.get(i));
                        }
                        group.setDirtyFlag(true);
                        makeGroupManagementDirty();
                        break;
                    }
                }
            }
            groupListsChangedFlag = false;
        } 
    }
    
    private void changeGroupNameInComboBox(String oldName, String newName) {
        groupComboBoxModel.removeElement(oldName);
        groupComboBoxModel.addElement(newName);
        groupComboBox.setSelectedIndex(0);
    }
    
    
    private boolean groupManagementDirtyFlag = false;
    private boolean groupListsChangedFlag = false;
    private void makeGroupManagementDirty() {
        groupManagementDirtyFlag = true;
        groupSaveButton.setEnabled(true);
        groupSaveAndCloseButton.setEnabled(true);
    }
    private AbstractAction groupRemoveUserAction = new AbstractAction("<<") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            groupListsChangedFlag = true;
            
            List<String> selectedList = groupUserList.getSelectedValuesList();
            if (selectedList.size() > 0) {
                makeGroupManagementDirty();
                for (String user : selectedList) {
                    if (!manageGroupsAvailableModel.contains(user)) {
                        manageGroupsAvailableModel.addElement(user);
                    }
                    manageGroupsUsersModel.removeElement(user);
                }
                storeGroupInfo();
            }
        }
    };
    private AbstractAction groupAddUserAction = new AbstractAction(">>") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            groupListsChangedFlag = true;
            List<String> selectedList = groupAvailableUserList.getSelectedValuesList();
            if (selectedList.size() > 0) {
                makeGroupManagementDirty();
                for(String group : selectedList) {
                    if (!manageGroupsUsersModel.contains(group)) {
                        manageGroupsUsersModel.addElement(group);
                    }
                    manageGroupsAvailableModel.removeElement(group);
                }
                storeGroupInfo();
            }
        }
    };
    private AbstractAction groupDeleteAction = new AbstractAction("DELETE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            deleteGroup();
        }
    };
    private AbstractAction groupCreateAction = new AbstractAction("CREATE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            createGroup();
        }
    };
    private AbstractAction groupCloseAction = new AbstractAction("CLOSE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            if (groupManagementDirtyFlag) {
                int response = Util.showConfirmDialog( 
                    "There may be unsaved changes. Are you sure you want to close this window? \n"
                    + "Note: the changes will exist, and can be saved until JMARS is restarted.", "Confirm Done", JOptionPane.YES_NO_OPTION);
                if (response == JOptionPane.YES_OPTION) {
                    manageGroupsDialog.setVisible(false);
                }
            } else {
                manageGroupsDialog.setVisible(false);
                int selectedRow = sharedMapsTable.getSelectedRow();
                refreshSharingTable();
                if (selectedRow > -1) {
                    sharedMapsTable.setRowSelectionInterval(selectedRow, selectedRow);
                }
            }
        }
    };
    private AbstractAction groupSaveAction = new AbstractAction("SAVE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            saveGroupChanges();
            groupManagementDirtyFlag = false;
            groupListsChangedFlag = false;
            groupSaveButton.setEnabled(false);
            groupSaveAndCloseButton.setEnabled(false);
        }
    };
    private AbstractAction groupSaveAndCloseAction = new AbstractAction("SAVE AND CLOSE") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            saveGroupChanges();
            groupManagementDirtyFlag = false;
            groupListsChangedFlag = false;
            groupSaveButton.setEnabled(false);
            groupSaveAndCloseButton.setEnabled(false);
            manageGroupsDialog.setVisible(false);
        }
    };
    
    private AbstractAction groupRenameAction = new AbstractAction("RENAME") {
        
        @Override
        public void actionPerformed(ActionEvent e) {
            String newName = Util.showInputDialog("Enter a new name for this group: ", "Rename Group", JOptionPane.QUESTION_MESSAGE);
            if (newName != null && newName.length() > 0) {
                if (groupComboBoxModel.getIndexOf(newName) == -1) {
                    String selectedGroup = (String) groupComboBox.getSelectedItem();
                    for (SharingGroup group : allSharingGroups) {
                        if (group.getName().equalsIgnoreCase(selectedGroup)) {
                            group.setName(newName);
                            CustomMapBackendInterface.editSharingGroup(group);
                            changeGroupNameInComboBox(selectedGroup, group.getName());
                            break;
                        }
                    }
                } else {
                    Util.showMessageDialog("Duplicate group names are not allowed.", "Message", JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }
    };

    private void saveGroupChanges() {
        if (groupManagementDirtyFlag) {
            for (SharingGroup group : allSharingGroups) {
                if (group.isDirtyFlag()) {
                    CustomMapBackendInterface.editSharingGroup(group);
                    group.setDirtyFlag(false);
                }
                
            }
        }
    }
    private void createGroup() {
        String newGroup = Util.showInputDialog("Enter the name of the new group:", "Create New Group", JOptionPane.QUESTION_MESSAGE);
        newGroup = newGroup.trim();
        if (newGroup != null && newGroup.length() > 0) {
            if (groupComboBoxModel.getIndexOf(newGroup) != -1) {
                groupComboBox.setSelectedItem(newGroup);
            } else {
                SharingGroup group = new SharingGroup(newGroup);
                CustomMapBackendInterface.createGroup(group);
                allSharingGroups.add(group);
                groupComboBox.addItem(newGroup);
            }
        }
    }
    private void deleteGroup() {
        int response = Util.showConfirmDialog( 
                    "Are you sure you want to delete the selected group?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            SharingGroup toRemove = null;
            String groupName = (String) groupComboBox.getSelectedItem();
            for (SharingGroup group : allSharingGroups) {
                if (group.getName().equals(groupName)) {
                    CustomMapBackendInterface.deleteSharingGroup(group);
                    groupComboBox.setSelectedIndex(0);
                    groupComboBox.removeItem(groupName);
                    toRemove = group;
                    break;
                }
            }
            if (toRemove != null) {
                allSharingGroups.remove(toRemove);
                for(CustomMap map : sharedMapFiles) {
                    ArrayList<SharingGroup> sharedWith = map.getSharedWithGroups();
                    ArrayList<SharingGroup> tempSharedWith = map.getTempSharedWithGroups();
                    if (sharedWith.contains(toRemove)) {
                        sharedWith.remove(toRemove);
                    }
                    if (tempSharedWith.contains(toRemove)) {
                        tempSharedWith.remove(toRemove);
                    }
                }
            }
        }
    }
    
    private void showManageUsersDialog(JDialog parent) {
        createManageUsersDialog(parent);
        this.manageUsersListModel.clear();
        for (String user : allSharingUsers) {
            this.manageUsersListModel.addElement(user);
        }
        this.manageUsersDialog.setVisible(true);
        searchUserInput.setText("");
        manageUsersMsgLbl.setText("");
        manageUsersMsgLbl.setVisible(false);
        pack();
    }
    
    private boolean validateCard1Inputs() {
        boolean allInputValid = true;
        
        String maxLat = northernLatInput.getText();
        String minLat = southernLatInput.getText();
        String minLon = westernLonInput.getText();
        String maxLon = easternLonInput.getText();
 
        if (regionalRadio.isSelected()) {
            if ("".equals(maxLat.trim()) || "".equals(minLat.trim()) || "".equals(minLon.trim()) || "".equals(maxLon.trim())) {
                Util.showMessageDialog("Some corner point information is missing. Please fill in and try again.",
                            "Custom Map Validation", JOptionPane.ERROR_MESSAGE);
                return false;
            }
            
            try {
                double mxLat = Double.parseDouble(maxLat);
                double miLat = Double.parseDouble(minLat);
                double mxLon = Double.parseDouble(maxLon);
                double miLon = Double.parseDouble(minLon);
                
                if (Double.compare(mxLat,90) > 0 || Double.compare(mxLat,-90) < 0
                 || Double.compare(miLat,90) > 0 || Double.compare(miLat,-90) < 0
                 || Double.compare(miLon,360) > 0 || Double.compare(miLon,-360) < 0
                 || Double.compare(mxLon,360) > 0 || Double.compare(mxLon,-360) < 0
                        ) {
                    Util.showMessageDialog("Some corner point information is invalid. Please try again.",
                                "Custom Map Validation", JOptionPane.ERROR_MESSAGE);
                    return false;
                }
            
            } catch (NumberFormatException nfe) {
                Util.showMessageDialog("Some corner point information is invalid. Please try again.",
                                "Custom Map Validation", JOptionPane.ERROR_MESSAGE);
                    return false;
            }
        }
        String name = nameInput.getText();
        name = name.trim();
        if (name.length() > 80) {
            Util.showMessageDialog("Name is too long (80 characters max).",
                                "Custom Map Name", JOptionPane.ERROR_MESSAGE);
                    return false;
        }
        boolean checkNameFlag = true;
        if (tableSelectionSource == TABLE_SELECTION_EXISTING) {//edit existing
            CustomMap selMap = getSelectedExistingMap();
            if (selMap != null) {//if this is an edit of an existing map
                String oldName = selMap.getName().trim();
                if (oldName.equals(name)) {
                    checkNameFlag = false;//this name will exist in the list, do not flag it
                }
            }
        }
        if (checkNameFlag && mapNames.contains(name)) {//if this is not the same name as the selected existing map and it is in the list
            Util.showMessageDialog("You already have a map by this name. Please choose a different name.",
                            "Custom Map Name", JOptionPane.ERROR_MESSAGE);
                return false;
        }
        checkNameFlag = true;
        if (tableSelectionSource == TABLE_SELECTION_IN_PROGRESS) {//edit existing
            CustomMap selMap = getSelectedUploadInProgressFile();
            if (selMap != null) {//if this is an edit of an existing map
                String oldName = selMap.getName().trim();
                if (oldName.equals(name)) {
                    checkNameFlag = false;//this name will exist in the list, do not flag it
                }
            }
        }
        if (checkNameFlag && inProgressNames.contains(name)) {//if this is not the same name as the selected existing map and it is in the list
            Util.showMessageDialog("You already have a map in progress by this name. Please choose a different name.",
                            "Custom Map Name", JOptionPane.ERROR_MESSAGE);
                return false;
        }
        if (ignoreValueInput.getText() != null && !NumberUtils.isCreatable(ignoreValueInput.getText()) && ignoreValueInput.getText().length()>0) {
        	Util.showMessageDialog("The ignore value must be a numeric value.",
                    "Ignore Value Error", JOptionPane.ERROR_MESSAGE);
        		return false;
        }
        
        
        return allInputValid;
    }
    private void clearExistingMapsFilter() {
        activeFilter = false;
        mapNameCheckbox.setSelected(true);
        ownerCheckbox.setSelected(true);
        keywordsCheckbox.setSelected(true);
        descriptionCheckbox.setSelected(true);
        fileNameCheckbox.setSelected(true);
        
        filterTextbox.setText("");
        
        existingMapsFiles.clear();
        if (sharedWithMeRadio.isSelected()) {
            existingMapsFiles.addAll(getSharedWithMeMaps());
        } else if (myMapsRadio.isSelected()) {
            existingMapsFiles.addAll(getOwnedByMeMaps());
        } else if (sharedWithOthersRadio.isSelected()){
            existingMapsFiles.addAll(getSharedWithOthersMaps());
        } else {
            existingMapsFiles.addAll(allExistingMapsFiles);
        }
        
        refreshExistingMapsTable();
    }
    private void refreshExistingMaps() {
        CustomMapBackendInterface.refreshCapabilities();
        
        allExistingMapsFiles.clear();
        existingMapsFiles.clear();
        sharedMapFiles.clear();
        ownedByMeExistingMapsFiles = null;
        sharedByMeExistingMapsFiles = null;
        sharedWithMeExistingMapsFiles = null;
        mapNames.clear();
        
        ArrayList<CustomMap> tempCMFiles = CustomMapBackendInterface.loadExistingMapList();
        existingMapsFiles.addAll(tempCMFiles);
        allExistingMapsFiles.addAll(existingMapsFiles);
        
        for (CustomMap map : allExistingMapsFiles) {
            mapNames.add(map.getName());
        }
        
        ArrayList<CustomMap> tempSMFiles = CustomMapBackendInterface.getSharedMapsForUser(allExistingMapsFiles, allSharingGroups);
        sharedMapFiles.addAll(tempSMFiles);
        
        refreshExistingMapsTable();
        refreshSharingTable();
        filterExistingMaps();
        
        SearchProvider.getInstance().refreshCustomMapSearch(allExistingMapsFiles);
    }
    private void filterExistingMapsByRadioButton() {
        //this action will check for an active filter and if it exists, call filter. 
        //otherwise, it will reset the data to the correct set based on the selected option.
        if (activeFilter) {
            filterExistingMaps();
        } else {
            existingMapsFiles.clear();
            if (sharedWithMeRadio.isSelected()) {
                existingMapsFiles.addAll(getSharedWithMeMaps());
            } else if (myMapsRadio.isSelected()) {
                existingMapsFiles.addAll(getOwnedByMeMaps());
            } else if (sharedWithOthersRadio.isSelected()){ 
                existingMapsFiles.addAll(getSharedWithOthersMaps());
            } else {
                existingMapsFiles.addAll(allExistingMapsFiles);
            }
            refreshExistingMapsTable();
        }
    }
    private ArrayList<CustomMap> getSharedWithMeMaps() {
        if (sharedWithMeExistingMapsFiles == null) {
            populateFilterLists();
        }
        return sharedWithMeExistingMapsFiles;
    }
    private ArrayList<CustomMap> getSharedWithOthersMaps() {
        if (sharedByMeExistingMapsFiles == null) {
            populateFilterLists();
        }
        return sharedByMeExistingMapsFiles;
    }
    private void populateFilterLists() {
        sharedWithMeExistingMapsFiles = new ArrayList<CustomMap>();
        ownedByMeExistingMapsFiles = new ArrayList<CustomMap>();
        sharedByMeExistingMapsFiles = new ArrayList<CustomMap>();
        String owner = Main.USER;
        for (CustomMap map : allExistingMapsFiles) {
            if (owner.equalsIgnoreCase(map.getOwner())) {
                ownedByMeExistingMapsFiles.add(map);
            } else {
                sharedWithMeExistingMapsFiles.add(map);
            }
            
            if (map.isSharedWithOthers()) {
                sharedByMeExistingMapsFiles.add(map);
            }
        }
    }
    private ArrayList<CustomMap> getOwnedByMeMaps() {
        if (ownedByMeExistingMapsFiles == null) {
            populateFilterLists();
        }
        return ownedByMeExistingMapsFiles;
    }
    private void filterExistingMaps() {
        activeFilter = true;
        boolean useName = mapNameCheckbox.isSelected();
        boolean useOwner = ownerCheckbox.isSelected();
        boolean useKW = keywordsCheckbox.isSelected();
        boolean useDesc = descriptionCheckbox.isSelected();
        boolean useFN = fileNameCheckbox.isSelected();

        
        String filterValue = filterTextbox.getText();
        
        ArrayList<CustomMap> toSearch = null;
        if (sharedWithMeRadio.isSelected()) {
            toSearch = getSharedWithMeMaps();
        } else if (myMapsRadio.isSelected()) {
            toSearch = getOwnedByMeMaps();
        } else if (sharedWithOthersRadio.isSelected()){
            toSearch = getSharedWithOthersMaps();
        } else {
            toSearch = allExistingMapsFiles;
        }
        
        ArrayList<CustomMap> toAdd = new ArrayList<CustomMap>();
        if (filterValue.trim().length() == 0) {
            //reset, they did not filter by any value
            toAdd.addAll(toSearch);
        } else if (!useName && !useOwner && !useKW && !useDesc && ! useFN) {
            //reset, they did not filter by any type
            toAdd.addAll(toSearch);
        } else {
            //we have to pare the list down
            String[] filterVals = filterValue.split(",", 0);
            for (CustomMap map : toSearch) {
                for(String val : filterVals) {
                    val = val.toLowerCase().trim();
                    if (useName && map.getName().toLowerCase().indexOf(val) > -1) {
                        toAdd.add(map);
                        break;
                    }
                    if (useOwner && map.getOwner().toLowerCase().indexOf(val) > -1) {
                        toAdd.add(map);
                        break;
                    }
                    if (useFN && map.getBasename().toLowerCase().indexOf(val) > -1) {
                        toAdd.add(map);
                        break;
                    }
                    if (useDesc && map.getDescription().toLowerCase().indexOf(val) > -1) {
                        toAdd.add(map);
                        break;
                    }
                    if (useKW) { 
                        String[] kws = map.getKeywords();
                        for (String kw : kws) {
                            kw = kw.toLowerCase().trim();
                            if (kw.indexOf(val) > -1) {
                                toAdd.add(map);
                                break;
                            }
                        }
                    }
                }     
            }
        }
        
        existingMapsFiles.clear();
        existingMapsFiles.addAll(toAdd);
        refreshExistingMapsTable();
    }

    //listeners
    private ListSelectionListener uploadTableSelectionListener = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                if (uploadInProgressTable.getSelectedRow() != -1) {
                    toggleMainButtons(true);
                } else {
                    toggleMainButtons(false);
                }
            }
        }
    };
    private ListSelectionListener uploadCompletedTableSelectionListener = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                if (uploadCompletedTable.getSelectedRow() != -1) {
                    toggleCompletedButtons(true);
                } else {
                    toggleCompletedButtons(false);
                }
            }
        }
    };
    private ListSelectionListener existingMapsTableSelectionListener = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                toggleExistingMapButtons();
            }
        }
    };
    private String previouslySelectedGroup = "";
    private ItemListener groupItemListener = new ItemListener() {
        
        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                storeGroupInfo();
                populateGroupLists();
                previouslySelectedGroup = (String) groupComboBox.getSelectedItem();
                
            }
        }
    };
    private ListSelectionListener shareTableSelectionListener = new ListSelectionListener() {
        public void valueChanged(ListSelectionEvent e) {
            if (!e.getValueIsAdjusting()) {
                populateSharingLists();
            }
        }
    };
    private void populateSharingLists() {
        sharedWithGroupsModel.clear();
        sharedWithUsersModel.clear();
        availableGroupsModel.clear();
        availableUsersModel.clear();
        
        if (sharedMapsTable.getSelectedRow() != -1) {
            CustomMap map = getSelectedSharingMap();
            ArrayList<SharingGroup> sharedWithGroups = map.getTempSharedWithGroups();
            ArrayList<String> sharedWithUsers = map.getTempSharedWithUsers();
            ArrayList<String> groupsToRemove = new ArrayList<String>();
            ArrayList<String> usersToRemove = new ArrayList<String>();
            for (SharingGroup group : sharedWithGroups) {
                sharedWithGroupsModel.addElement(group.getName());
                groupsToRemove.add(group.getName());
            }
            for (String user : sharedWithUsers) {
                sharedWithUsersModel.addElement(user);
                usersToRemove.add(user);
            }
            for (SharingGroup group : allSharingGroups) {
                if (!groupsToRemove.contains(group.getName())) {
                    availableGroupsModel.addElement(group.getName());
                }
            }
            for (String user : allSharingUsers) {
                if (!usersToRemove.contains(user)) {
                    availableUsersModel.addElement(user);
                }
            }
            
        }
    }
    private ActionListener extentListener = new ActionListener() {
        public void actionPerformed(ActionEvent e) {
            CustomMap file = null;
            if (tableSelectionSource == TABLE_SELECTION_COMPLETED) {
                file = getSelectedUploadCompletedFile();
            } else if (tableSelectionSource == TABLE_SELECTION_IN_PROGRESS){
                file = getSelectedUploadInProgressFile();
            } else if (tableSelectionSource == TABLE_SELECTION_EXISTING) {
                file = getSelectedExistingMap();
            } else if (tableSelectionSource == TABLE_SELECTION_FILE_CHOOSER) {
                if (dialogUploadFileList != null && dialogUploadFileList.size() > 0) {
                    file = dialogUploadFileList.get(0);
                }
            }
            if (file == null) {
                throw new IllegalStateException("The correct file could not be found in the extent listener! Table selection source = "+tableSelectionSource);
            }
            if(globalRadio.isSelected()){
                //only set the values if they changed to global
                if (file.getExtent() != CustomMap.EXTENT_INPUT_GLOBAL) {
                    //set these values in case they click back on regional
                    file.setNorthLat(northernLatInput.getText());
                    file.setSouthLat(southernLatInput.getText());
                    file.setWestLon(westernLonInput.getText());
                    file.setEastLon(easternLonInput.getText());
                }
                northernLatInput.setText("90");
                southernLatInput.setText("-90");
                westernLonInput.setText("-180");
                easternLonInput.setText("180");
                file.setExtent(CustomMap.EXTENT_INPUT_GLOBAL);
                
            } else if(regionalRadio.isSelected()){
                
                northernLatInput.setText(file.getNorthLat());
                southernLatInput.setText(file.getSouthLat());
                westernLonInput.setText(file.getWestLon());
                easternLonInput.setText(file.getEastLon());
                file.setExtent(CustomMap.EXTENT_INPUT_REGIONAL);
            }
        }
    };
    private CustomMap getSelectedUploadInProgressFile() {
        CustomMap file = null;
        int row = uploadInProgressTable.getSelectedRow();
        if (row != -1) {
            int select = uploadInProgressTable.getSorter().unsortRow(row);
            file = inProgressUploadFileTableModel.getFile(select);
        } else {
            //we came in from the file chooser
            file = dialogUploadFileList.get(0);
        }
        return file;
    }
    private CustomMap getSelectedUploadCompletedFile() {
        CustomMap file = null;
        int row = uploadCompletedTable.getSelectedRow();
        if (row != -1) {
            int select = uploadCompletedTable.getSorter().unsortRow(row);
            file = completedUploadFileTableModel.getFile(select);
        }
        return file;
    }
    private CustomMap getSelectedExistingMap() {
        CustomMap map = null;
        int row = existingMapsTable.getSelectedRow();
        if (row != -1) {
            int select = existingMapsTable.getSorter().unsortRow(row);
            map = customMapTableModel.getMap(select);
        }
        return map;
    }
    private ArrayList<CustomMap> getSelectedExistingMaps() {
        Sorter sort = existingMapsTable.getSorter();
        ArrayList<CustomMap> selected = new ArrayList<CustomMap>();
        int[] idxs = existingMapsTable.getSelectedRows();
        for (int x : idxs) {
            int realIdx = sort.unsortRow(x);
            selected.add(customMapTableModel.getMap(realIdx));
        }
        return selected;
    }
    private CustomMap getSelectedSharingMap() {
        CustomMap map = null;
        int row = sharedMapsTable.getSelectedRow();
        if (row != -1) {
            int select = sharedMapsTable.getSorter().unsortRow(row);
            map = sharingTableModel.getMap(select);
        }
        return map;
    }
    private MouseListener uploadTableMouseListener = new MouseListener() {
        public void mouseReleased(MouseEvent e) {
        }
        public void mousePressed(MouseEvent e) {
        }
        public void mouseExited(MouseEvent e) {
        }
        public void mouseEntered(MouseEvent e) {
        }
        public void mouseClicked(MouseEvent e) {
            if(SwingUtilities.isRightMouseButton(e)){
                //if no file is selected, disable
                if (uploadInProgressTable.getSelectedRow() == -1) {
                    cancelUploadMenuItem.setEnabled(false);
                    viewUploadMenuItem.setEnabled(false);
                } else {
                    cancelUploadMenuItem.setEnabled(true);
                    viewUploadMenuItem.setEnabled(true);
                }
                uploadRCMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    };
    private MouseListener completedTableMouseListener = new MouseListener() {
        public void mouseReleased(MouseEvent e) {
        }
        public void mousePressed(MouseEvent e) {
        }
        public void mouseExited(MouseEvent e) {
        }
        public void mouseEntered(MouseEvent e) {
        }
        public void mouseClicked(MouseEvent e) {
            if(SwingUtilities.isRightMouseButton(e)){
                //if no file is selected, disable
                if (uploadCompletedTable.getSelectedRow() == -1) {
                    viewCompletedMenuItem.setEnabled(false);
                    clearCompletedMenuItem.setEnabled(false);
                } else if (uploadCompletedTable.getSelectedRowCount() > 1){
                    viewCompletedMenuItem.setEnabled(false);
                    clearCompletedMenuItem.setEnabled(true);
                } else {
                    viewCompletedMenuItem.setEnabled(true);
                    clearCompletedMenuItem.setEnabled(true);
                }
                completedRCMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    };
    
    private boolean areAnySelectedExistingMapsSharedWithMe() {
    	Sorter sorter = existingMapsTable.getSorter();
		int[] rows = existingMapsTable.getSelectedRows();
		for (int i=0; i<rows.length; i++) {
			int row = rows[i];
			int realRow = sorter.unsortRow(row);
			CustomMap map = existingMapsFiles.get(realRow);
			if (!map.getOwner().equalsIgnoreCase(Main.USER)) {
				return true;
			}
		}
		return false;
    }
    
    private MouseListener existingMapsTableMouseListener = new MouseListener() {
        public void mouseReleased(MouseEvent e) {
        }
        public void mousePressed(MouseEvent e) {
        }
        public void mouseExited(MouseEvent e) {
        }
        public void mouseEntered(MouseEvent e) {
        }
        public void mouseClicked(MouseEvent e) {
            if(SwingUtilities.isRightMouseButton(e)){
                //if no file is selected, disable
                if (existingMapsTable.getSelectedRow() == -1) {
                    editCMMenuItem.setEnabled(false);
                    deleteCMMenuItem.setEnabled(false);
                    addLayerMenuItem.setEnabled(false);
                    shareCMMenuItem.setEnabled(false);
                    reprocessMenuItem.setEnabled(false);
                } else {
                	boolean sharedSelected = areAnySelectedExistingMapsSharedWithMe();
                	if (sharedSelected) {
                		editCMMenuItem.setEnabled(false);
                		deleteCMMenuItem.setEnabled(false);
                        shareCMMenuItem.setEnabled(false);
                        addLayerMenuItem.setEnabled(true);
                        reprocessMenuItem.setEnabled(false);
                	} else {
		                if(existingMapsTable.getSelectedRowCount() > 1) {
		                    editCMMenuItem.setEnabled(false);
		                    reprocessMenuItem.setEnabled(false);
		                } else {
		                    editCMMenuItem.setEnabled(true);
		                    reprocessMenuItem.setEnabled(true);
		                }
		                deleteCMMenuItem.setEnabled(true);
		                addLayerMenuItem.setEnabled(true);
		                shareCMMenuItem.setEnabled(true);
                	}
                }
                existingMapRCMenu.show(e.getComponent(), e.getX(), e.getY());
            } else if (SwingUtilities.isLeftMouseButton(e)) {
                if (e.getClickCount() == 2) {
                    doEditExistingMap();
                }
            }
        }
    };
    private MouseListener shareTableMouseListener = new MouseListener() {
        public void mouseReleased(MouseEvent e) {
        }
        public void mousePressed(MouseEvent e) {
        }
        public void mouseExited(MouseEvent e) {
        }
        public void mouseEntered(MouseEvent e) {
        }
        public void mouseClicked(MouseEvent e) {
            if(SwingUtilities.isRightMouseButton(e)){
                //if no file is selected, disable
//                if (uploadTable.getSelectedRow() == -1) {
//                    cancelUploadMenuItem.setEnabled(false);
//                    viewUploadMenuItem.setEnabled(false);
//                    removeUploadMenuItem.setEnabled(false);
//                } else {
//                    cancelUploadMenuItem.setEnabled(true);
//                    viewUploadMenuItem.setEnabled(true);
//                    removeUploadMenuItem.setEnabled(true);
//                }
//                uploadRCMenu.show(e.getComponent(), e.getX(), e.getY());
            }
        }
    };

    private void kickOffUploadAllFiles(int processingOption) {
        for(CustomMap cardInput : dialogUploadFileList) {
            cardInput.setSelectedUploadProcess(processingOption);
            //prevent duplicate map names before sending to upload
            int suffix = 1;
            String cardInputName = cardInput.getName();
            while (mapNames.contains(cardInput.getName()) || inProgressNames.contains(cardInput.getName())) {
                cardInput.setName(cardInputName+"("+suffix+")");
                suffix++;
            }
        }
        kickOffUploadThread(dialogUploadFileList);
    }
    private void processInProgressFile(CustomMap file) {
        file.setStatus(CustomMap.STATUS_PROCESSING);//need to change the status so that monitor cares about it
        if (!CustomMapBackendInterface.updateHeaderValuesAndStartProcessing(file)){
        	processInProgress = false; 
        } else {
        	processInProgress = true;
        }
    }
    private void kickOffMonitorThread() {
        if (customMapMonitor == null) {
            customMapMonitor = new CustomMapMonitor();
        }
        customMapMonitor.setUploadFiles(uploadInProgressFiles);
        try {
            if (monitorThread == null || !monitorThread.isAlive()) {
                monitorThread = new Thread(customMapMonitor);
                monitorThread.start();
            }
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Thread problem...starting new thread");
            monitorThread = new Thread(customMapMonitor);
            monitorThread.start();
        }
    }
    private void kickOffUploadThread(List<CustomMap> files) {
        if (uploadRunner == null) {
            uploadRunner = new UploadFileRunner(files);
        } else {
            uploadRunner.addUploadFiles(files);
        }
        if (uploadThread == null || !uploadThread.isAlive()) {
            uploadThread = new Thread(uploadRunner);
            uploadThread.start();
        }
    }
    
    private void reprocessMap(CustomMap cm) {
    	//check if map exists
        CustomMap existingMap = getSelectedExistingMap();
        String mapFileName = existingMap.getFilename();
    	boolean mapExistsDB = existingMap.initialUploadExists();
    	boolean mapExistsNetwork = CustomMapBackendInterface.originalMapExists(existingMap, mapFileName);
    	if (!mapExistsDB || !mapExistsNetwork) {
    		//show dialog
    		Util.showMessageDialog("Original upload can't be found. This image can't be reprocessed.",
    			    "Error",
    			    JOptionPane.ERROR_MESSAGE);
    	} else {
            clearAllInputs();
            tableSelectionSource = TABLE_SELECTION_EXISTING;
            setReprocessMapEditInputs(cm);
            toggleAllCardInputsForReprocessMap();
            prepareCardButtonRowsForReprocessMap();
            mainDialogLayout.replace(mainDialogActivePanel, uploadInfoPanel);
            mainDialogActivePanel = uploadInfoPanel;
            showMainDialog();
    	}
    }
    
    private void addMapLayer() {
        for(CustomMap map : getSelectedExistingMaps()){
//        CustomMap map = getSelectedExistingMap();
            if(map != null){
                try{
                    //combine keywords in the description
                    String desc = map.getDescription();
                    String[] keywords = map.getKeywords();
                    if(keywords.length>0){
                        desc += "\n\nKeywords: ";
                        for(String word : keywords){
                            desc+=word+", ";
                        }
                        desc = desc.substring(0, desc.length()-2);
                    }
                    
                    LayerParameters lp = map.getLayerParameters();
                    if (lp == null) {
                    	lp = new LayerParameters(map.getFilename(), map.getCitation(), desc, map.getUnits(), map.getLinks());
                    }
                    //create the map layer
                    ArrayList<MapSource> plotSources = new ArrayList<MapSource>();
                    if (map.isNumeric()) {
                    	plotSources.add(map.getGraphicSource());
                    }
                    new MapLViewFactory().createLayer(map.getGraphicSource(), plotSources, lp, null);
                    //refresh the lmanager
                    LManager.getLManager().repaint();
                    //update the CusomMap last used date
                    //getting current date and time using Date class
                    Date dateobj = new Date();
                    map.setLastUsedDate(ExistingMap.dateFormat.format(dateobj));
                    //refresh the selected row of the table
                    refreshExistingMapsTable();
                }
                catch(Exception ex){
                    Util.showMessageDialog("Unable to load custom map, "+map.getName()+". Please see log for more details.",
                            "Custom Map Failure", JOptionPane.ERROR_MESSAGE);
                    ex.printStackTrace();
                }
            }
        }
    }
    private void deleteExistingMaps() {
        ArrayList<CustomMap> maps = getSelectedExistingMaps();
        int response = Util.showConfirmDialog( 
                    "Are you sure you want to delete the selected maps?", "Confirm Delete", JOptionPane.YES_NO_OPTION);
        if (response == JOptionPane.YES_OPTION) {
            boolean error = false;
            for(CustomMap map : maps) {
                boolean success = CustomMapBackendInterface.deleteCustomMap(map);
                if (success) {
                    CustomMapBackendInterface.finishDeleteCustomMap(map);
                    existingMapsFiles.remove(map);//model for existing map table 
                    allExistingMapsFiles.remove(map);//data structure for all maps
                    mapNames.remove(map.getName());
                    if (sharedWithMeExistingMapsFiles != null) {//for radio button
                        sharedWithMeExistingMapsFiles.remove(map);
                    }
                    if (ownedByMeExistingMapsFiles != null) {//for radio button
                        ownedByMeExistingMapsFiles.remove(map);
                    }
                    if (sharedByMeExistingMapsFiles != null) {//for radio button
                        sharedByMeExistingMapsFiles.remove(map);
                    }
                    if (sharedMapFiles != null) {//for sharing management dialog
                        sharedMapFiles.remove(map);
                    }
                    refreshExistingMapsTable();
                    refreshSharingTable();
                } else {
                    error = true;
                }
            }
            
            CustomMapBackendInterface.refreshCapabilities();
            
             if (error) {
                String msg = "There was a problem deleting the selected custom maps. \nSome maps may not have deleted. If this problem persists, please select Help->Report a Problem to contact support.";
                Util.showMessageDialog(msg,"Delete Error",JOptionPane.ERROR_MESSAGE);
            }
        }
    }
//    private void refreshStatusOfUploads() {
//        for (int i=0; i<uploadInProgressFiles.size(); i++) {
//                    CustomMap file = uploadInProgressFiles.get(i);
//                    file.setStatus(CustomMap.STATUS_UPDATING_STATUS);
//                    updateProgressStatus(i);
//        }
//        Runnable r = new Runnable() {
// 
//            @Override
//            public void run() {
//                CustomMapBackendInterface.checkMapProcessingStatus(uploadInProgressFiles);
//                final ArrayList<CustomMap> toRemove = new ArrayList<CustomMap>();
//                for (int i=0; i<uploadInProgressFiles.size(); i++) {
//                    CustomMap file = uploadInProgressFiles.get(i);
//                    if (CustomMap.STATUS_IN_PROGRESS.equals(file.getStatus()) && CustomMap.STAGE_HEADER_ANALYZED.equals(file.getStage())) {
//                        file.setStatus(CustomMap.STATUS_AWAITING_USER_INPUT);
//                    } else if (CustomMap.STATUS_CANCELED.equalsIgnoreCase(file.getStatus()) 
//                       || CustomMap.STATUS_ERROR.equalsIgnoreCase(file.getStatus())
//                       || CustomMap.STATUS_COMPLETE.equalsIgnoreCase(file.getStatus())) {
//                        toRemove.add(file);
//                    }
//                }
//                try {
//                    SwingUtilities.invokeAndWait(new Runnable() {
//                        @Override
//                        public void run() {
//                            addToCompleted(toRemove);
//                            refreshUploadTable();
//                        }
//                    });
//                } catch (InvocationTargetException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                } catch (InterruptedException e) {
//                    // TODO Auto-generated catch block
//                    e.printStackTrace();
//                }
//                
//            }
//        };
//        Thread t = new Thread(r);
//        t.start();
//    }
//    public void resetUploadFileList() {
//        uploadInProgressFiles = CustomMapBackendInterface.getInProgressCustomMapList();
//        if (uploadInProgressFiles.size() > 0) {
//            kickOffMonitorThread();
//        }
//    }
    public static void updateStatusOnEDThread(final int row) {
        SwingUtilities.invokeLater(new Runnable() {
            
            @Override
            public void run() {
                CM_Manager.getInstance().updateProgressStatus(row);
            }
        });
    }
    public void updateProgressStatus() {
        for(int i=1; i<inProgressUploadFileTableModel.getRowCount(); i++) {
            updateProgressStatus(i);
        }
    }
    public void addToCompleted(ArrayList<CustomMap> files) {
        this.uploadInProgressFiles.removeAll(files);
        refreshUploadTable();
        inProgressNames.clear();
        for (CustomMap map:uploadInProgressFiles) {
        	inProgressNames.add(map.getName());
        }
        this.uploadCompletedFiles.addAll(files);
        refreshCompletedTable();
        this.scrollToBottom(uploadCompletedTableSp);
        addNewlyCompletedFilesToExistingMaps(files);
    }
    private void addNewlyCompletedFilesToExistingMaps(ArrayList<CustomMap> files) {
        ArrayList<CustomMap> toAdd = new ArrayList<CustomMap>();
        for (CustomMap map : files) {
            if (CustomMap.STATUS_COMPLETE.equalsIgnoreCase(map.getStatus())) {
                CustomMap customMap = CustomMapBackendInterface.getExistingMap(map);//build the ExistingMap object from the database to be safe.
                toAdd.add(customMap);
                mapNames.add(customMap.getName());
            }
        }
        
        allExistingMapsFiles.addAll(toAdd);
        populateFilterLists();
        filterExistingMapsByRadioButton();
        this.scrollToBottom(existingMapsTableSp);
        newlyCreatedFilesToAddToSearch.addAll(toAdd);
    }
    public void addNewlyCompletedFilesToSearch() {
    	for (CustomMap customMap : newlyCreatedFilesToAddToSearch) {
    		SearchProvider.getInstance().addCustomMapSearchRow(customMap);
    	}
    	newlyCreatedFilesToAddToSearch.clear();
    }
    public void refreshUploadTable() {
        inProgressUploadFileTableModel.fireTableDataChanged();
    }
    public void updateProgressStatus(int row) { 
        inProgressUploadFileTableModel.fireTableRowsUpdated(row, row);
    }
    public void refreshCompletedTable() {
        completedUploadFileTableModel.fireTableDataChanged();
    }
    public void refreshExistingMapsTable() {
        customMapTableModel.fireTableDataChanged();
    }
    public void refreshExistingMapsRow(int row) {
        customMapTableModel.fireTableRowsUpdated(row, row);
    }
    public void refreshSharingTable() {
        sharingTableModel.fireTableDataChanged();
    }
    
    //TODO: tricky way of scrolling to the bottom. Now that we have an STable, I think there is an easier way
    private void scrollToBottom(JScrollPane scrollPane) {
        final JScrollBar verticalBar = scrollPane.getVerticalScrollBar();
        AdjustmentListener downScroller = new AdjustmentListener() {
            @Override
            public void adjustmentValueChanged(AdjustmentEvent e) {
                Adjustable adjustable = e.getAdjustable();
                adjustable.setValue(adjustable.getMaximum());
                verticalBar.removeAdjustmentListener(this);
            }
        };
        verticalBar.addAdjustmentListener(downScroller);
    }
    private void initializeFileChooser() {
        //set up the file chooser
        fileChooser = new JFileChooser(Util.getDefaultFCLocation());
        fileChooser.setDialogTitle("Select file(s) to upload as maps"); 
        fileChooser.setMultiSelectionEnabled(true);
        FileFilter filter = new FileNameExtensionFilter("Map File Types", "jpeg", "tiff", "tif", "geotiff", "png", 
                "jpg", "img", "gif", "cub", "jp2","pbm","pgm","ppm","vic","vicar","fit");
        fileChooser.setFileFilter(filter);
    }
    private void initializeDialogWidgets() {
        uploadOptionsPanel = new JPanel();
        uploadInfoPanel = new JPanel();
        uploadMetadataPanel = new JPanel();
        selectedFilesPanel = new JPanel();
        
        selectedFilesScrollPane = new JScrollPane();
        selectedFilesList = new JList<>();
        selectedFilesLbl = new JLabel();
        uploadOptionManualSp = new JScrollPane();
        uploadOptionManualEp = new JEditorPane();
        uploadOptionProcessSp = new JScrollPane();
        uploadOptionProcessEp = new JEditorPane();
        uploadOptionVerifySp = new JScrollPane();
        uploadOptionVerifyEp = new JEditorPane();
        uploadOptionsHeaderLbl = new JLabel();
        enterMapInfoLbl = new JLabel();
        enterMapInfoNoteSp = new JScrollPane();
        enterMapInfoNoteTextPane = new JTextPane();
        nameLbl = new JLabel();
        nameInput = new JTextField();
        ignoreValueLbl = new JLabel();
        ignoreValueInput = new JTextField();
        unitsLbl = new JLabel();
        unitsInput = new JTextField();
        extentLbl = new JLabel();
        globalRadio = new JRadioButton();
        regionalRadio = new JRadioButton();
        degreesLbl = new JLabel();
        eastRadio = new JRadioButton();
        westRadio = new JRadioButton();
        easternLonLbl = new JLabel();
        westernLonLbl = new JLabel();
        easternLonInput = new JTextField();
        westernLonInput = new JTextField();
        northernmostLatLbl = new JLabel();
        northernLatInput = new JTextField();
        southernmostLatLbl = new JLabel();
        southernLatInput = new JTextField();
        uploadAndProcessRadio = new JRadioButton();
        verifyInformationRadio = new JRadioButton();
        manauallyEnterRadio = new JRadioButton();
        ocentricRadio = new JRadioButton();
        ographicRadio = new JRadioButton();
        shapeTypeLbl = new JLabel();
        shapeTypeHelpLbl = new JLabel();
        
        mapInfoUploadButton = new JButton(uploadInfoUploadAction);
        mapInfoClearButton = new JButton(uploadInfoClearAction);
        mapInfoCancelButton = new JButton(uploadInfoCancelAction);
        mapInfoBackButton = new JButton(uploadInfoBackAction);
        mapInfoNextButton = new JButton(uploadInfoNextAction);
        uploadOptionsCancelButton = new JButton(uploadOptionsCancelAction);
        uploadOptionsContinueButton = new JButton(uploadOptionsContinueAction);
        mapInfoReprocessButton = new JButton(reprocessMapAction);
        
        
        //map metadata
        mapMetadataUploadButton = new JButton(uploadMetadataUploadAction);
        mapMetadataClearButton = new JButton(uploadMetadataClearAction);
        mapMetadataCancelButton = new JButton(uploadMetadataCancelAction);
        mapMetadataBackButton = new JButton(uploadMetadataBackAction);
        mapMetadataReprocessButton = new JButton(reprocessMapAction);
        
        uploadOptionsButtonPanel = new JPanel();
        
        enterMapInfoLbl1 = new JLabel();
        descriptionLbl = new JLabel();
        descriptionSp = new JScrollPane();
        descriptionTextAreaInput = new JTextArea();
        linksLbl = new JLabel();
        linksSp = new JScrollPane();
        linksTextAreaInput = new JTextArea();
        citationLbl = new JLabel();
        citationSp = new JScrollPane();
        citationTextAreaInput = new JTextArea();
        keywordsLbl = new JLabel();
        keywordsSp = new JScrollPane();
        keywordsTextAreaInput = new JTextArea();
        
        extentButtonGroup = new ButtonGroup();
        extentButtonGroup.add(globalRadio);
        extentButtonGroup.add(regionalRadio);
        globalRadio.addActionListener(extentListener);
        regionalRadio.addActionListener(extentListener);
        
        degreeButtonGroup = new ButtonGroup();
        degreeButtonGroup.add(eastRadio);
        degreeButtonGroup.add(westRadio);
        
        uploadOptionsButtonGroup = new ButtonGroup();
        uploadOptionsButtonGroup.add(manauallyEnterRadio);
        uploadOptionsButtonGroup.add(verifyInformationRadio);
        uploadOptionsButtonGroup.add(uploadAndProcessRadio);
        
        shapeTypeButtonGroup = new ButtonGroup();
        shapeTypeButtonGroup.add(ocentricRadio);
        shapeTypeButtonGroup.add(ographicRadio);
        
    }
    private void toggleMainButtons(boolean enabled) {
        viewEditUploadButton.setEnabled(enabled);
        cancelUploadButton.setEnabled(enabled);
    }
    private void toggleCompletedButtons(boolean enabled) {
        if (enabled && uploadCompletedTable.getSelectedRowCount() > 1) {
            viewCompletedButton.setEnabled(false);
            clearSelectedButton.setEnabled(enabled);
        } else {
            viewCompletedButton.setEnabled(enabled);
            clearSelectedButton.setEnabled(enabled);
        }
    }
    private void toggleExistingMapButtons() {
    	if (existingMapsTable.getSelectedRow() == -1) {
    		editExistingMapButton.setEnabled(false);
    		addLayerButton.setEnabled(false);
	        deleteExistingMapButton.setEnabled(false);
	        shareExistingMapButton.setEnabled(false);
	        reprocessMapButton.setEnabled(false);
	        
    	} else {
    		boolean sharedSelected = areAnySelectedExistingMapsSharedWithMe();
    		if (sharedSelected) {
            	editExistingMapButton.setEnabled(false);
    			shareExistingMapButton.setEnabled(false);
    			deleteExistingMapButton.setEnabled(false);
    			addLayerButton.setEnabled(true);
    			reprocessMapButton.setEnabled(false);
            } else {
		    	if (existingMapsTable.getSelectedRowCount() > 1) {
                    //if we are turning buttons on, and more than one row is selected
                    editExistingMapButton.setEnabled(false);
                    reprocessMapButton.setEnabled(false);
                } else {
		        	//here only one is selected
		        	editExistingMapButton.setEnabled(true);
		        	reprocessMapButton.setEnabled(true);
                }
	            addLayerButton.setEnabled(true);
		        deleteExistingMapButton.setEnabled(true);
		        shareExistingMapButton.setEnabled(true);
            }
        }
    }
    private void setColors() {
        
    }
 
    //Warning, if you change the layout code in any method below, you own it. Have a nice day!
    private void layoutMainDialogPanel() {
        mainDialogLayout = new GroupLayout(mainDialog.getContentPane());
        mainDialog.getContentPane().setLayout(mainDialogLayout);
        setAutoGaps(mainDialogLayout);
        
        mainDialogLayout.setHorizontalGroup(
            mainDialogLayout.createSequentialGroup()
            .addComponent(selectedFilesPanel)
            .addComponent(uploadOptionsPanel)
        );
        mainDialogLayout.setVerticalGroup(
            mainDialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(selectedFilesPanel)
                .addComponent(uploadOptionsPanel)
        );
        mainDialogLayout.setHonorsVisibility(true);
        mainDialogActivePanel = uploadOptionsPanel;
    }
    private void setAutoGaps(GroupLayout gl) {
        gl.setAutoCreateContainerGaps(true);
        gl.setAutoCreateGaps(true);
    }
    private void layoutSelectedFilesPanel() {
        GroupLayout selectedFilesPanelLayout = new GroupLayout(selectedFilesPanel);
        selectedFilesPanel.setLayout(selectedFilesPanelLayout);
        setAutoGaps(selectedFilesPanelLayout);
        
        selectedFilesPanelLayout.setHorizontalGroup(
            selectedFilesPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(selectedFilesLbl)
                .addComponent(selectedFilesScrollPane)
        );
        selectedFilesPanelLayout.setVerticalGroup(
            selectedFilesPanelLayout.createSequentialGroup()
                .addComponent(selectedFilesLbl)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(selectedFilesScrollPane)
        );
    }
    private void layoutUploadOptions() {
        GroupLayout uploadOptionsButtonPanelLayout = new GroupLayout(uploadOptionsButtonPanel);
        uploadOptionsButtonPanel.setLayout(uploadOptionsButtonPanelLayout);
        setAutoGaps(uploadOptionsButtonPanelLayout);
        
        uploadOptionsButtonPanelLayout.setHorizontalGroup(
            uploadOptionsButtonPanelLayout.createSequentialGroup()
                .addComponent(uploadOptionsContinueButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(uploadOptionsCancelButton)
        );
        uploadOptionsButtonPanelLayout.setVerticalGroup(
            uploadOptionsButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(uploadOptionsContinueButton)
                .addComponent(uploadOptionsCancelButton)
        );
        
        GroupLayout uol = new GroupLayout(uploadOptionsPanel);
        uploadOptionsPanel.setLayout(uol);
        setAutoGaps(uol);
        
        uol.setHorizontalGroup(
            uol.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(uploadOptionsHeaderLbl)
                .addGroup(uol.createParallelGroup()
                    .addComponent(manauallyEnterRadio)
                    .addGap(4)
                    .addComponent(uploadOptionManualSp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(uploadAndProcessRadio)
                    .addGap(4)
                    .addComponent(uploadOptionProcessSp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(verifyInformationRadio)
                    .addGap(4)
                    .addComponent(uploadOptionVerifySp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                .addComponent(uploadOptionsButtonPanel)
        );
        
        uol.setVerticalGroup(
            uol.createSequentialGroup()
                .addComponent(uploadOptionsHeaderLbl)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(manauallyEnterRadio)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(uploadOptionProcessSp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(uploadAndProcessRadio)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(uploadOptionVerifySp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(verifyInformationRadio)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(uploadOptionManualSp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(uploadOptionsButtonPanel)
        );

    }
    
    private void layoutExistingMapsTab() {
        GroupLayout filterCheckBoxPanelLayout = new GroupLayout(filterCheckBoxPanel);
        filterCheckBoxPanel.setLayout(filterCheckBoxPanelLayout);
        setAutoGaps(filterCheckBoxPanelLayout);
        
        filterCheckBoxPanelLayout.setHorizontalGroup(
           filterCheckBoxPanelLayout.createSequentialGroup()
                .addComponent(mapNameCheckbox)
                .addComponent(ownerCheckbox)
                .addComponent(fileNameCheckbox)
                .addComponent(keywordsCheckbox)
                .addComponent(descriptionCheckbox)
        );
        filterCheckBoxPanelLayout.setVerticalGroup(
            filterCheckBoxPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(descriptionCheckbox)
                .addComponent(fileNameCheckbox)
                .addComponent(keywordsCheckbox)
                .addComponent(ownerCheckbox)
                .addComponent(mapNameCheckbox)
        );

        GroupLayout filterPanelLayout = new GroupLayout(filterPanel);
        filterPanel.setLayout(filterPanelLayout);
        setAutoGaps(filterPanelLayout);
        
        filterPanelLayout.setHorizontalGroup(
            filterPanelLayout.createSequentialGroup()
                .addComponent(filterMapsLabel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filterTextbox)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filterButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(clearFilterButton)
        );
        filterPanelLayout.setVerticalGroup(
            filterPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(filterTextbox)
                .addComponent(filterMapsLabel)
                .addComponent(filterButton)
                .addComponent(clearFilterButton)
        );

        GroupLayout existingMapsButtonPanelLayout = new GroupLayout(existingMapsButtonPanel);
        existingMapsButtonPanel.setLayout(existingMapsButtonPanelLayout);
        setAutoGaps(existingMapsButtonPanelLayout);
        
        existingMapsButtonPanelLayout.setHorizontalGroup(
            existingMapsButtonPanelLayout.createSequentialGroup()
                .addComponent(addLayerButton)
                .addComponent(editExistingMapButton)
                .addComponent(shareExistingMapButton)
                .addComponent(deleteExistingMapButton)
                .addComponent(manageSharingButton)
                .addComponent(refreshMapsButton)
                .addComponent(reprocessMapButton)
        );
        existingMapsButtonPanelLayout.setVerticalGroup(
            existingMapsButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(shareExistingMapButton)
                .addComponent(editExistingMapButton)
                .addComponent(deleteExistingMapButton)
                .addComponent(addLayerButton)
                .addComponent(manageSharingButton)
                .addComponent(refreshMapsButton)
                .addComponent(reprocessMapButton)
        );
        
        GroupLayout mapFilterPanelLayout = new GroupLayout(mapFilterPanel);
        mapFilterPanel.setLayout(mapFilterPanelLayout);
        setAutoGaps(mapFilterPanelLayout);
        
        mapFilterPanelLayout.setHorizontalGroup(
            mapFilterPanelLayout.createSequentialGroup()
                .addComponent(allMapsRadio)
                .addComponent(myMapsRadio)
                .addComponent(sharedWithMeRadio)
                .addComponent(sharedWithOthersRadio)
        );
        mapFilterPanelLayout.setVerticalGroup(
            mapFilterPanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                .addComponent(allMapsRadio)
                .addComponent(myMapsRadio)
                .addComponent(sharedWithMeRadio)
                .addComponent(sharedWithOthersRadio)
        );

        GroupLayout existingMainPanelLayout = new GroupLayout(existingMainPanel);
        existingMainPanel.setLayout(existingMainPanelLayout);
        setAutoGaps(existingMainPanelLayout);
        
        existingMainPanelLayout.setHorizontalGroup(
            existingMainPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(mapFilterPanel)
                .addComponent(filterPanel)
                .addComponent(existingMapsTableSp)
                .addComponent(filterCheckBoxPanel)
                .addComponent(existingMapsButtonPanel)
        );
        existingMainPanelLayout.setVerticalGroup(
            existingMainPanelLayout.createSequentialGroup()
                .addComponent(mapFilterPanel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(existingMapsTableSp)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(filterCheckBoxPanel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(filterPanel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(existingMapsButtonPanel)
        );
    }
    
    
    private void layoutUploadPanel() {
        GroupLayout uploadInProgressTablePanelLayout = new GroupLayout(uploadInProgressTablePanel);
        uploadInProgressTablePanel.setLayout(uploadInProgressTablePanelLayout);
        setAutoGaps(uploadInProgressTablePanelLayout);
        
        uploadInProgressTablePanelLayout.setHorizontalGroup(
            uploadInProgressTablePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addGroup(uploadInProgressTablePanelLayout.createSequentialGroup()
                    .addComponent(selectFilesButton)
                    .addComponent(fileTypesLbl))
                .addComponent(inProgressUploadsLbl)
                .addComponent(uploadTableSp)
                .addGroup(uploadInProgressTablePanelLayout.createSequentialGroup()
                    .addComponent(viewEditUploadButton)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(cancelUploadButton)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(refreshUploadStatusButton))
        );
        uploadInProgressTablePanelLayout.setVerticalGroup(
            uploadInProgressTablePanelLayout.createSequentialGroup()
                .addGroup(uploadInProgressTablePanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(selectFilesButton)
                    .addComponent(fileTypesLbl))
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(inProgressUploadsLbl)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(uploadTableSp)
                .addGroup(uploadInProgressTablePanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(viewEditUploadButton)
                    .addComponent(cancelUploadButton)
                    .addComponent(refreshUploadStatusButton))
        );
           
  
        GroupLayout uploadCompletedTablePanelLayout = new GroupLayout(uploadCompletedTablePanel);
        uploadCompletedTablePanel.setLayout(uploadCompletedTablePanelLayout);
        setAutoGaps(uploadCompletedTablePanelLayout);
        
        uploadCompletedTablePanelLayout.setHorizontalGroup(
            uploadCompletedTablePanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(completedUploadsLbl)
                .addComponent(uploadCompletedTableSp)
                .addGroup(uploadCompletedTablePanelLayout.createSequentialGroup()
                    .addComponent(viewCompletedButton)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(clearSelectedButton)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                    .addComponent(clearAllCompletedButton))
        );
        uploadCompletedTablePanelLayout.setVerticalGroup(
            uploadCompletedTablePanelLayout.createSequentialGroup()
                .addComponent(completedUploadsLbl)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(uploadCompletedTableSp)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addGroup(uploadCompletedTablePanelLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                    .addComponent(clearAllCompletedButton)
                    .addComponent(clearSelectedButton)
                    .addComponent(viewCompletedButton))
        );

        GroupLayout uploadTabPanelLayout = new GroupLayout(uploadTabPanel);
        uploadTabPanel.setLayout(uploadTabPanelLayout);
        setAutoGaps(uploadTabPanelLayout);
        
        uploadTabPanelLayout.setHorizontalGroup(
            uploadTabPanelLayout.createParallelGroup()
                .addComponent(uploadInProgressTablePanel)
                .addComponent(uploadSeparator)
                .addComponent(uploadCompletedTablePanel)
        );
        uploadTabPanelLayout.setVerticalGroup(
            uploadTabPanelLayout.createSequentialGroup()
                .addComponent(uploadInProgressTablePanel)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(uploadSeparator)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(uploadCompletedTablePanel)
        );    

    }
    
    private void layoutUploadInfo() {
        GroupLayout uploadInfoLayout = new GroupLayout(uploadInfoPanel);
        uploadInfoPanel.setLayout(uploadInfoLayout);
        setAutoGaps(uploadInfoLayout);
        uploadInfoLayout.setHonorsVisibility(true);
        
        uploadInfoLayout.setHorizontalGroup(
            uploadInfoLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(enterMapInfoLbl)
                .addComponent(enterMapInfoNoteSp, GroupLayout.PREFERRED_SIZE, 500, GroupLayout.PREFERRED_SIZE)
                .addGroup(uploadInfoLayout.createSequentialGroup()
                    .addComponent(nameLbl)
                    .addComponent(nameInput)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(ignoreValueLbl)
                    .addComponent(ignoreValueInput)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(unitsLbl)
                    .addComponent(unitsInput))
                .addGroup(uploadInfoLayout.createSequentialGroup()
                    .addComponent(extentLbl)
                    .addComponent(globalRadio)
                    .addComponent(regionalRadio)
                    .addGap(50, 50, 200)
                    .addComponent(degreesLbl)
                    .addComponent(westRadio)
                    .addComponent(eastRadio))
                .addGroup(uploadInfoLayout.createSequentialGroup()
                    .addComponent(westernLonLbl)
                    .addComponent(westernLonInput)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(easternLonLbl)
                    .addComponent(easternLonInput))
                .addGroup(uploadInfoLayout.createSequentialGroup()
                    .addComponent(northernmostLatLbl)
                    .addComponent(northernLatInput)
                    .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                    .addComponent(southernmostLatLbl)
                    .addComponent(southernLatInput))
                .addGroup(uploadInfoLayout.createSequentialGroup()
                    .addComponent(shapeTypeLbl)
                    .addComponent(ocentricRadio) 
                    .addComponent(ographicRadio)
                    .addComponent(shapeTypeHelpLbl))
                .addGroup(uploadInfoLayout.createSequentialGroup()
                    .addComponent(mapInfoBackButton)
                    .addComponent(mapInfoNextButton)
                    .addComponent(mapInfoUploadButton)
                    .addComponent(mapInfoClearButton)
                    .addComponent(mapInfoCancelButton)
                    .addComponent(mapInfoReprocessButton))
        );
        
        uploadInfoLayout.setVerticalGroup(
                uploadInfoLayout.createSequentialGroup()
                    .addComponent(enterMapInfoLbl)
                    .addGap(1, 5, 40)
                    .addComponent(enterMapInfoNoteSp, GroupLayout.PREFERRED_SIZE, 46, GroupLayout.PREFERRED_SIZE)
                    .addGap(1, 5, 40)
                    .addGroup(uploadInfoLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(nameLbl)
                        .addComponent(nameInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(ignoreValueLbl)
                        .addComponent(ignoreValueInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(unitsLbl)
                        .addComponent(unitsInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(1, 5, 40)
                    .addGroup(uploadInfoLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(extentLbl)
                        .addComponent(globalRadio)
                        .addComponent(regionalRadio)
                        .addComponent(degreesLbl)
                        .addComponent(westRadio)
                        .addComponent(eastRadio))
                    .addGap(1, 5, 40)
                    .addGroup(uploadInfoLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(westernLonLbl)
                        .addComponent(westernLonInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(easternLonLbl)
                        .addComponent(easternLonInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(1, 5, 40)
                    .addGroup(uploadInfoLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                        .addComponent(northernmostLatLbl)
                        .addComponent(northernLatInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                        .addComponent(southernmostLatLbl)
                        .addComponent(southernLatInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
                    .addGap(1, 5, 40)
                    .addGroup(uploadInfoLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(shapeTypeLbl)
                        .addComponent(ocentricRadio)
                        .addComponent(ographicRadio)
                        .addComponent(shapeTypeHelpLbl))
                    .addGap(1, 5, 40)
                    .addGroup(uploadInfoLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                        .addComponent(mapInfoBackButton)
                        .addComponent(mapInfoNextButton)
                        .addComponent(mapInfoUploadButton)
                        .addComponent(mapInfoClearButton)
                        .addComponent(mapInfoCancelButton)
                        .addComponent(mapInfoReprocessButton))
            );
    }
    private void layoutUploadMetadata() {
        GroupLayout uploadMetadataLayout = new GroupLayout(uploadMetadataPanel);
        uploadMetadataPanel.setLayout(uploadMetadataLayout);
        setAutoGaps(uploadMetadataLayout);
        
        uploadMetadataLayout.setHorizontalGroup(
            uploadMetadataLayout.createSequentialGroup()
                .addGroup(uploadMetadataLayout.createParallelGroup(GroupLayout.Alignment.TRAILING)
                    .addComponent(descriptionLbl)
                    .addComponent(linksLbl)
                    .addComponent(citationLbl)
                    .addComponent(keywordsLbl))
                .addGap(5, 18, 50)
                .addGroup(uploadMetadataLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(enterMapInfoLbl1)
                    .addComponent(linksSp, GroupLayout.PREFERRED_SIZE, 352, 600)
                    .addComponent(descriptionSp, GroupLayout.PREFERRED_SIZE, 352, 600)
                    .addComponent(citationSp, GroupLayout.PREFERRED_SIZE, 352, 600)
                    .addComponent(keywordsSp, GroupLayout.PREFERRED_SIZE, 352, 600)
                    .addGroup(uploadMetadataLayout.createSequentialGroup()
                        .addComponent(mapMetadataBackButton)
                        .addComponent(mapMetadataUploadButton)
                        .addComponent(mapMetadataClearButton)
                        .addComponent(mapMetadataCancelButton)
                        .addComponent(mapMetadataReprocessButton)))
        );
        uploadMetadataLayout.setVerticalGroup(
                uploadMetadataLayout.createSequentialGroup()
                      .addComponent(enterMapInfoLbl1)
                      .addGroup(uploadMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                              .addComponent(descriptionLbl)
                              .addComponent(descriptionSp, GroupLayout.PREFERRED_SIZE, 50, 400))
                      .addGroup(uploadMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                              .addComponent(linksLbl)
                              .addComponent(linksSp, GroupLayout.PREFERRED_SIZE, 50, 400))
                      .addGroup(uploadMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                              .addComponent(citationLbl)
                              .addComponent(citationSp, GroupLayout.PREFERRED_SIZE, 50, 400))
                      .addGroup(uploadMetadataLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                              .addComponent(keywordsLbl)
                              .addComponent(keywordsSp, GroupLayout.PREFERRED_SIZE, 50, 400))
                      .addGap(10,10,10)
                      .addGroup(uploadMetadataLayout.createParallelGroup(GroupLayout.Alignment.BASELINE)
                              .addComponent(mapMetadataBackButton)
                              .addComponent(mapMetadataUploadButton)
                              .addComponent(mapMetadataClearButton)
                              .addComponent(mapMetadataCancelButton)
                              .addComponent(mapMetadataReprocessButton))           
        );
    }
    
    
    private void createManageUsersDialog(JDialog parent) {
        manageUsersDialog = new JDialog(parent, "Manage User List", false);
        manageUsersDialog.setMinimumSize(new Dimension(479, 300)); //originally (435, 277)
        manageUsersDialog.setDefaultCloseOperation(HIDE_ON_CLOSE);
        manageUsersDialog.setLocationRelativeTo(parent);
        manageUsersDialog.getRootPane().setDefaultButton(manageUsersAddButton);
        
        GroupLayout manageUsersDialogLayout = new GroupLayout(manageUsersDialog.getContentPane());
        manageUsersDialog.getContentPane().setLayout(manageUsersDialogLayout);
        setAutoGaps(manageUsersDialogLayout);
        manageUsersDialogLayout.setHorizontalGroup(
        	manageUsersDialogLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addGroup(manageUsersDialogLayout.createSequentialGroup()
       				.addComponent(userListSp, 200, 200, Short.MAX_VALUE)
        			.addComponent(manageUsersAddPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, Short.MAX_VALUE)           
       			.addComponent(manageUsersButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        
        manageUsersDialogLayout.setVerticalGroup(
        	manageUsersDialogLayout.createSequentialGroup()
        		.addGroup(manageUsersDialogLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
        				.addComponent(manageUsersAddPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE) 
        				.addComponent(userListSp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                .addGap(0, 0, Short.MAX_VALUE)           
        		.addComponent(manageUsersButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
    }
    private void layoutSharingDialog() {
        GroupLayout usersSelectDeselectButtonPanelLayout = new GroupLayout(usersSelectDeselectButtonPanel);
        usersSelectDeselectButtonPanel.setLayout(usersSelectDeselectButtonPanelLayout);
        usersSelectDeselectButtonPanelLayout.setHorizontalGroup(
            usersSelectDeselectButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(shareWithUserButton)
            .addComponent(unshareWithUserButton)
        );
        usersSelectDeselectButtonPanelLayout.setVerticalGroup(
            usersSelectDeselectButtonPanelLayout.createSequentialGroup()
                .addComponent(shareWithUserButton)
                .addGap(18)
                .addComponent(unshareWithUserButton)
        );
        GroupLayout groupsSelectDeselectButtonPanelLayout = new GroupLayout(groupsSelectDeselectButtonPanel);
        groupsSelectDeselectButtonPanel.setLayout(groupsSelectDeselectButtonPanelLayout);
        groupsSelectDeselectButtonPanelLayout.setHorizontalGroup(
            groupsSelectDeselectButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
            .addComponent(addGroupShareButton)
            .addComponent(removeGroupShareButton)
        );
        groupsSelectDeselectButtonPanelLayout.setVerticalGroup(
            groupsSelectDeselectButtonPanelLayout.createSequentialGroup()
                .addComponent(addGroupShareButton)
                .addGap(18)
                .addComponent(removeGroupShareButton)
        );
        GroupLayout shareWithUserSpLayout = new GroupLayout(shareWithUserSp);
        shareWithUserSp.setLayout(shareWithUserSpLayout);
        shareWithUserSpLayout.setHorizontalGroup(
            shareWithUserSpLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(shareUsernameLbl)
                    .addComponent(shareWithUserButton, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(shareWithUserInput)
        );
        shareWithUserSpLayout.setVerticalGroup(
            shareWithUserSpLayout.createSequentialGroup()
                .addComponent(shareUsernameLbl)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(shareWithUserInput, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
                .addGap(18, 18, 18)
                .addComponent(shareWithUserButton)
                .addContainerGap()
        );

        GroupLayout sharedUsersPanelLayout = new GroupLayout(sharedUsersPanel);
        sharedUsersPanel.setLayout(sharedUsersPanelLayout);
        setAutoGaps(sharedUsersPanelLayout);
        sharedUsersPanelLayout.setHorizontalGroup(
        	sharedUsersPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                	.addComponent(sharingUsersLbl)
            .addGroup(sharedUsersPanelLayout.createSequentialGroup()
                .addGroup(sharedUsersPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                			.addComponent(availableUsersLbl)
                			.addComponent(availableUsersSp, GroupLayout.PREFERRED_SIZE, 136, Short.MAX_VALUE))
                		.addGap(13)
                		.addComponent(usersSelectDeselectButtonPanel)
                        .addGroup(sharedUsersPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                            .addComponent(sharedWithUsersLbl)
                            .addComponent(sharedWithUsersSp, GroupLayout.PREFERRED_SIZE, 136, Short.MAX_VALUE)))
                	.addComponent(manageUserListButton)
        );
        sharedUsersPanelLayout.setVerticalGroup(
        	sharedUsersPanelLayout.createSequentialGroup()
        		.addComponent(sharingUsersLbl)
                .addGroup(sharedUsersPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addGroup(sharedUsersPanelLayout.createSequentialGroup()
                            .addComponent(availableUsersLbl)
                		.addComponent(availableUsersSp, GroupLayout.PREFERRED_SIZE, 136, Short.MAX_VALUE))
                	.addComponent(usersSelectDeselectButtonPanel)
                    .addGroup(sharedUsersPanelLayout.createSequentialGroup()
                		.addComponent(sharedWithUsersLbl)
                		.addComponent(sharedWithUsersSp, GroupLayout.PREFERRED_SIZE, 136, Short.MAX_VALUE)))
                .addComponent(manageUserListButton)
        );
        
        GroupLayout shareWithGroupsPanelLayout = new GroupLayout(shareWithGroupsPanel);
        shareWithGroupsPanel.setLayout(shareWithGroupsPanelLayout);
        setAutoGaps(shareWithGroupsPanelLayout);
        shareWithGroupsPanelLayout.setHorizontalGroup(
        	shareWithGroupsPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                .addComponent(sharingGroupsLbl)
            .addGroup(shareWithGroupsPanelLayout.createSequentialGroup()
                .addGroup(shareWithGroupsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                		.addComponent(availableGroupsLbl)
                		.addComponent(availableGroupsSp, GroupLayout.PREFERRED_SIZE, 136, Short.MAX_VALUE))
                	.addGap(13)
                	.addComponent(groupsSelectDeselectButtonPanel)
                        .addGroup(shareWithGroupsPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                        .addComponent(sharedWithGroupsLbl)
                        .addComponent(sharedWithGroupsSp, GroupLayout.PREFERRED_SIZE, 136, Short.MAX_VALUE)))
                .addComponent(manageGroupsButton)
        );
        shareWithGroupsPanelLayout.setVerticalGroup(
        	shareWithGroupsPanelLayout.createSequentialGroup()
        		.addComponent(sharingGroupsLbl)
                .addGroup(shareWithGroupsPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addGroup(shareWithGroupsPanelLayout.createSequentialGroup()
                            .addComponent(availableGroupsLbl)
               			.addComponent(availableGroupsSp, GroupLayout.PREFERRED_SIZE, 136, Short.MAX_VALUE))
               		.addComponent(groupsSelectDeselectButtonPanel)
                    .addGroup(shareWithGroupsPanelLayout.createSequentialGroup()
               			.addComponent(sharedWithGroupsLbl)
               			.addComponent(sharedWithGroupsSp, GroupLayout.PREFERRED_SIZE, 136, Short.MAX_VALUE)))
                .addComponent(manageGroupsButton)
        );
        GroupLayout sharingManagmentButtonPanelLayout = new GroupLayout(sharingManagmentButtonPanel);
        sharingManagmentButtonPanel.setLayout(sharingManagmentButtonPanelLayout);
        setAutoGaps(sharingManagmentButtonPanelLayout);
        sharingManagmentButtonPanelLayout.setHorizontalGroup(
        	sharingManagmentButtonPanelLayout.createSequentialGroup()
                .addComponent(sharingSaveAndCloseButton)
                .addPreferredGap(ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                .addComponent(saveSharingChangesButton)
                .addPreferredGap(ComponentPlacement.RELATED, 20, Short.MAX_VALUE)
                .addComponent(cancelSharingButton)
        );
        sharingManagmentButtonPanelLayout.setVerticalGroup(
            sharingManagmentButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(saveSharingChangesButton)
                .addComponent(cancelSharingButton)
                .addComponent(sharingSaveAndCloseButton)
        );

        GroupLayout sharingManagementDialogLayout = new GroupLayout(sharingManagementDialog.getContentPane());
        sharingManagementDialog.getContentPane().setLayout(sharingManagementDialogLayout);
        setAutoGaps(sharingManagementDialogLayout);
        sharingManagementDialogLayout.setHorizontalGroup(
            sharingManagementDialogLayout.createParallelGroup(GroupLayout.Alignment.CENTER)           
            	.addComponent(sharedMapsLbl, GroupLayout.Alignment.LEADING)
                .addComponent(sharedMapsSp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
            .addGroup(sharingManagementDialogLayout.createSequentialGroup()
                	.addComponent(sharedUsersPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                	.addComponent(shareWithGroupsPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                .addComponent(sharingManagmentButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
        );
        sharingManagementDialogLayout.setVerticalGroup(
            sharingManagementDialogLayout.createSequentialGroup()
                .addComponent(sharedMapsLbl)
                .addComponent(sharedMapsSp, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addGroup(sharingManagementDialogLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addComponent(shareWithGroupsPanel, GroupLayout.PREFERRED_SIZE, 237, Short.MAX_VALUE)
                    .addComponent(sharedUsersPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
                .addComponent(sharingManagmentButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)

        );
        GroupLayout manageUsersAddPanelLayout = new GroupLayout(manageUsersAddPanel);
        manageUsersAddPanel.setLayout(manageUsersAddPanelLayout);
        setAutoGaps(manageUsersAddPanelLayout);
        manageUsersAddPanelLayout.setHorizontalGroup(
            manageUsersAddPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(manageUsersLbl)
                .addComponent(searchUserInput, GroupLayout.PREFERRED_SIZE, 195, Short.MAX_VALUE)
                    .addComponent(manageUsersAddButton)
                .addComponent(manageUsersMsgLbl) 
        );
        manageUsersAddPanelLayout.setVerticalGroup(
            manageUsersAddPanelLayout.createSequentialGroup()
                .addComponent(manageUsersLbl)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(searchUserInput, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(manageUsersAddButton)
                .addPreferredGap(LayoutStyle.ComponentPlacement.UNRELATED)
                .addComponent(manageUsersMsgLbl)
        );
        GroupLayout manageUsersButtonPanelLayout = new GroupLayout(manageUsersButtonPanel);
        manageUsersButtonPanel.setLayout(manageUsersButtonPanelLayout);
        setAutoGaps(manageUsersButtonPanelLayout);
        manageUsersButtonPanelLayout.setHorizontalGroup(
            	manageUsersButtonPanelLayout.createSequentialGroup()
            		.addGap(0, 20, Short.MAX_VALUE)
                .addComponent(manageUsersRemoveButton)
            		.addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(manageUsersSaveButton)
            		.addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(manageUsersDoneButton)
            		.addGap(0, 20, Short.MAX_VALUE)
        );
        manageUsersButtonPanelLayout.setVerticalGroup(
            manageUsersButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(manageUsersRemoveButton)
                    .addComponent(manageUsersSaveButton)
            	.addComponent(manageUsersDoneButton)
        );
        GroupLayout groupAvailableUsersPanelLayout = new GroupLayout(groupAvailableUsersPanel);
        groupAvailableUsersPanel.setLayout(groupAvailableUsersPanelLayout);
        setAutoGaps(groupAvailableUsersPanelLayout);
        groupAvailableUsersPanelLayout.setHorizontalGroup(
            groupAvailableUsersPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                .addComponent(groupUserListSp, 150, 160, Short.MAX_VALUE)
                .addComponent(groupUsersLbl)
        );
        groupAvailableUsersPanelLayout.setVerticalGroup(
            groupAvailableUsersPanelLayout.createSequentialGroup()
                .addComponent(groupUsersLbl)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(groupUserListSp,150, 160, Short.MAX_VALUE)
        );
        GroupLayout groupUserListPanelLayout = new GroupLayout(groupUserListPanel);
        groupUserListPanel.setLayout(groupUserListPanelLayout);
        setAutoGaps(groupUserListPanelLayout);
        groupUserListPanelLayout.setHorizontalGroup(
            groupUserListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
             	.addComponent(groupAvailableUserListSp, 150, 160, Short.MAX_VALUE)
                .addComponent(groupAvailableUsersLbl)
        );
        groupUserListPanelLayout.setVerticalGroup(
            groupUserListPanelLayout.createSequentialGroup()
                .addComponent(groupAvailableUsersLbl)
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(groupAvailableUserListSp, 150, 160, Short.MAX_VALUE)
        );
        GroupLayout groupAddRemoveUserButtonPanelLayout = new GroupLayout(groupAddRemoveUserButtonPanel);
        groupAddRemoveUserButtonPanel.setLayout(groupAddRemoveUserButtonPanelLayout);
        setAutoGaps(groupAddRemoveUserButtonPanelLayout);
        groupAddRemoveUserButtonPanelLayout.setHorizontalGroup(
            groupAddRemoveUserButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
            	.addGap(10, 10, Short.MAX_VALUE)                
                    .addComponent(groupAddUserButton)
                .addComponent(groupRemoveUserButton)
                .addGap(0, 0, Short.MAX_VALUE)           
        );
        groupAddRemoveUserButtonPanelLayout.setVerticalGroup(
            groupAddRemoveUserButtonPanelLayout.createSequentialGroup()
            	.addGap(0, 0, Short.MAX_VALUE)   
                .addComponent(groupAddUserButton)
                .addGap(18, 18, 18)
                .addComponent(groupRemoveUserButton)
                .addGap(0, 0, Short.MAX_VALUE)   
        );
        GroupLayout groupListPanelLayout = new GroupLayout(groupListPanel);
        groupListPanel.setLayout(groupListPanelLayout);
        setAutoGaps(groupListPanelLayout);
        groupListPanelLayout.setHorizontalGroup(
            groupListPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)  
            	.addComponent(groupTopButtonPanel)
                .addComponent(groupComboBoxPanel)
        );
        groupListPanelLayout.setVerticalGroup(
            groupListPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addGroup(groupListPanelLayout.createSequentialGroup()
            	.addComponent(groupComboBoxPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
                .addComponent(groupTopButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE))
        );
        GroupLayout groupButtonPanelLayout = new GroupLayout(groupButtonPanel);
        groupButtonPanel.setLayout(groupButtonPanelLayout);
        setAutoGaps(groupButtonPanelLayout);
        groupButtonPanelLayout.setHorizontalGroup(
            groupButtonPanelLayout.createSequentialGroup()
            	.addGap(0, 20, Short.MAX_VALUE)
            	.addComponent(groupSaveAndCloseButton)
            	.addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(groupSaveButton)
            	.addPreferredGap(ComponentPlacement.UNRELATED)
                .addComponent(groupDoneButton)
            	.addGap(0, 20, Short.MAX_VALUE)
        );
        groupButtonPanelLayout.setVerticalGroup(
            groupButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
        		.addGap(0, 0, Short.MAX_VALUE)
                    .addComponent(groupSaveAndCloseButton)
                    .addComponent(groupSaveButton)
                .addComponent(groupDoneButton)
            	.addGap(0, 0, Short.MAX_VALUE)
        );
        GroupLayout groupTopButtonPanelLayout = new GroupLayout(groupTopButtonPanel);
        groupTopButtonPanel.setLayout(groupTopButtonPanelLayout);
        setAutoGaps(groupTopButtonPanelLayout);
        groupTopButtonPanelLayout.setHorizontalGroup(
            groupTopButtonPanelLayout.createSequentialGroup()
                .addGap(18)
                .addComponent(groupCreateGroupButton)
                .addComponent(groupDeleteButton)
                .addComponent(groupRenameButton)
        );
        groupTopButtonPanelLayout.setVerticalGroup(
            groupTopButtonPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(groupCreateGroupButton)
                    .addComponent(groupDeleteButton)
                .addComponent(groupRenameButton)
                .addGap(18)
        );
        GroupLayout groupComboBoxPanelLayout = new GroupLayout(groupComboBoxPanel);
        groupComboBoxPanel.setLayout(groupComboBoxPanelLayout);
        setAutoGaps(groupComboBoxPanelLayout);
        groupComboBoxPanelLayout.setHorizontalGroup(
            groupComboBoxPanelLayout.createSequentialGroup()
            	.addGap(78)
            	.addComponent(groupMgmtLbl)
               	.addComponent(groupComboBox)
               	.addGap(80)
        );
        groupComboBoxPanelLayout.setVerticalGroup(
            groupComboBoxPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
                    .addComponent(groupComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
                    .addComponent(groupMgmtLbl)
        );
        GroupLayout manageGroupsDialogLayout = new GroupLayout(manageGroupsDialog.getContentPane());
        manageGroupsDialog.getContentPane().setLayout(manageGroupsDialogLayout);
        setAutoGaps(manageGroupsDialogLayout);
        manageGroupsDialogLayout.setHorizontalGroup(
        	manageGroupsDialogLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
        		.addComponent(groupListPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        		.addComponent(groupMainListPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
       			.addComponent(groupButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        manageGroupsDialogLayout.setVerticalGroup(
       		manageGroupsDialogLayout.createSequentialGroup()
       			.addComponent(groupListPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
       			.addComponent(groupMainListPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
       			.addComponent(groupButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
        );
        GroupLayout groupMainListPanelLayout = new GroupLayout(groupMainListPanel);
        groupMainListPanel.setLayout(groupMainListPanelLayout);
        setAutoGaps(groupMainListPanelLayout);
        groupMainListPanelLayout.setHorizontalGroup(
        	groupMainListPanelLayout.createParallelGroup(GroupLayout.Alignment.CENTER)
            .addGroup(groupMainListPanelLayout.createSequentialGroup()
                        .addComponent(groupUserListPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(groupAddRemoveUserButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                        .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                        .addComponent(groupAvailableUsersPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                        .addGap(12, 12, 12)
                    .addComponent(groupManageUserListButton)
        		.addGap(0, 0, Short.MAX_VALUE)
        );
        groupMainListPanelLayout.setVerticalGroup(
           groupMainListPanelLayout.createSequentialGroup()
            	.addGap(0, 0, Short.MAX_VALUE)
                .addGroup(groupMainListPanelLayout.createParallelGroup(GroupLayout.Alignment.LEADING)
                    .addGap(25, 25, 25)
                    .addComponent(groupAddRemoveUserButtonPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(groupUserListPanel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)
                    .addComponent(groupAvailableUsersPanel, GroupLayout.Alignment.TRAILING, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE))
                .addPreferredGap(LayoutStyle.ComponentPlacement.RELATED)
                .addComponent(groupManageUserListButton)
        		.addGap(0, 10, Short.MAX_VALUE)
        );
    }
    
    
    
    public void addPreviousUser(String text) {
        // TODO Auto-generated method stub
        
    }
    public String[] getPreviouslyUsedUsers() {
        // TODO Auto-generated method stub
        return null;
    }
    public ArrayList<SharingGroup> getGroups() {
        // TODO Auto-generated method stub
        return null;
    }
    
    public void addGroup(SharingGroup newGroup) {
        // TODO Auto-generated method stub
        
    }
    public void refreshGroupsTable() {
        // TODO Auto-generated method stub
        
    }
    public void showErrorMessage() {
    	Util.showMessageDialog("The ignore value is not valid and must be numeric", "Input Error", JOptionPane.PLAIN_MESSAGE);	
    	processInProgress = true;
    }
}
