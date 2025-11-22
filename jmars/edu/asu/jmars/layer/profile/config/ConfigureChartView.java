package edu.asu.jmars.layer.profile.config;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Supplier;
import javax.swing.*;
import javax.swing.border.*;
import org.apache.commons.lang3.tuple.Pair;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.Pipeline;
import edu.asu.jmars.layer.map2.Stage;
import edu.asu.jmars.layer.map2.msd.PipelineModel;
import edu.asu.jmars.layer.map2.stages.composite.BandAggregatorSettings;
import edu.asu.jmars.layer.map2.stages.composite.CompositeStage;
import edu.asu.jmars.layer.profile.IChartEventHandler;
import edu.asu.jmars.layer.profile.IProfileModel;
import edu.asu.jmars.layer.profile.IProfileModelEventListener;
import edu.asu.jmars.layer.profile.ProfileFactory;
import edu.asu.jmars.layer.profile.ProfileLView;
import edu.asu.jmars.layer.profile.ProfileLView.ProfileLine;
import edu.asu.jmars.layer.profile.ProfileLView.SavedParams;
import edu.asu.jmars.layer.profile.manager.ProfileManager;
import edu.asu.jmars.layer.profile.manager.ProfileManagerMode;
import edu.asu.jmars.layer.profile.swing.ProfileObjectComboBoxRenderer;
import edu.asu.jmars.layer.util.NumericMapSourceDialog;
import edu.asu.jmars.swing.ImportantMessagePanel;
import edu.asu.jmars.swing.LikeDefaultButtonUI;
import edu.asu.jmars.ui.looknfeel.GUITheme;
import edu.asu.jmars.ui.looknfeel.ThemeFont;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FONTS;
import edu.asu.jmars.ui.looknfeel.ThemeFont.FontFile;
import edu.asu.jmars.ui.looknfeel.theme.component.ThemeTableHeader;

public class ConfigureChartView extends JPanel implements IProfileModelEventListener, ActionListener {

	private static final long serialVersionUID = -6389201275675258163L;
	private JTable tblProfiles;
	private JTable tblOneNumeric, tblManyNumeric;
	private JRadioButton rdbtnOneNumeric = new JRadioButton(ConfigType.ONENUMSOURCE.asString());
	private JRadioButton rdbtnManyNumeric = new JRadioButton(ConfigType.MANYNUMSOURCES.asString());	
	private JComboBox<String> profilesBox;
	private SelectedProfilesTableModel selectedProfilesTblModel;
	private SelectedNumSourcesTableModel selectedOneNumSourceTblModel, selectedManyNumSourcesTblModel;
	private DefaultComboBoxModel combomodel = null;
	private final ProfileLView profileLView;
	private final ProfileFactory controller;
	private final ProfileManager pm; 
	private Color groupBorder = ((ThemeTableHeader) GUITheme.get("tableheader")).getLineborder();
	private JPanel cards; //a panel that uses CardLayout  
	private Config configOne, configMany;
	private boolean isDefaultSource;
	private boolean isFirstChangeOfNumSource;
    private boolean isFirstClickHappened;
    private final Font smallFont = ThemeFont.getFont(FontFile.REGULAR.toString(), false).deriveFont(FONTS.ROBOTO_CHART_SMALL.fontSize());
    private Map<ConfigType, Config> chartConfigMap = new HashMap<>();
    private static  Map<String, Supplier<IChartEventHandler>> EVENT_HANDLER;
	
		
	public ConfigureChartView(ProfileLView profileLView, ProfileFactory profileFactory) {
		this.profileLView = profileLView;
		this.controller = profileFactory;	
		this.isDefaultSource = true;
		this.isFirstChangeOfNumSource = false;
		this.isFirstClickHappened = false;
		this.pm = new ProfileManager(ProfileManagerMode.SELECT_MANY);
		this.selectedProfilesTblModel = new SelectedProfilesTableModel(this.controller);
		this.selectedOneNumSourceTblModel = new SelectedNumSourcesTableModel(this.controller);
		this.selectedManyNumSourcesTblModel = new SelectedNumSourcesTableModel(this.controller);		
		this.configOne = new Config();
		this.configMany = new Config();
		this.configOne.withConfigType(ConfigType.ONENUMSOURCE);	
		this.configMany.withConfigType(ConfigType.MANYNUMSOURCES);
		this.chartConfigMap.put(ConfigType.ONENUMSOURCE, this.configOne);
		this.chartConfigMap.put(ConfigType.MANYNUMSOURCES, this.configMany);
		rdbtnOneNumeric.setSelected(true);	
		init();
		initConfigEventHandlers();
	}


	private void initConfigEventHandlers() {
		final Map<String, Supplier<IChartEventHandler>> handlers = new HashMap<>();
		handlers.put(IProfileModel.NEW_MAPSOURCE_EVENT, NewMapSourceEventHandler::new);	
		handlers.put(IProfileModel.NEW_PROFILEDATA_EVENT, NewProfileEventHandler::new);
		handlers.put(IProfileModel.SELECTED_TO_PLOT_PROFILEDATA_EVENT, SelectedToPlotEventHandler::new);		
		EVENT_HANDLER = Collections.unmodifiableMap(handlers);					
	}

	private void init() {
		JPanel configpanel = createConfigureChartPanel();
		add(configpanel);
	}

	private JPanel createConfigureChartPanel() {				
		JPanel configureChartPanel = new JPanel();		
		configureChartPanel.setBorder(new EmptyBorder(5, 5, 5, 5));		
		configureChartPanel.setLayout(new BorderLayout(5, 25));
		
		JPanel radiobuttonspanel = new JPanel(new BorderLayout(5, 10));
		
		JLabel lblChartSetup = new JLabel("COMPARISON TYPE");
		lblChartSetup.setFont(smallFont);
		radiobuttonspanel.add(lblChartSetup, BorderLayout.LINE_START);
		
		FlowLayout fl = new FlowLayout(FlowLayout.LEFT, -4, 0);	
		JPanel radioPanel = new JPanel(fl);
		
		rdbtnOneNumeric.addActionListener(this);		
		radioPanel.add(rdbtnOneNumeric);
		radioPanel.add( Box.createHorizontalStrut(34));
				
		rdbtnManyNumeric.addActionListener(this);
		
		//Group the radio buttons.
        ButtonGroup group = new ButtonGroup();
        group.add(rdbtnOneNumeric);
        group.add(rdbtnManyNumeric);		
		
        radioPanel.add(rdbtnManyNumeric);
        radiobuttonspanel.add(radioPanel, BorderLayout.PAGE_END);
        
        configureChartPanel.add(radiobuttonspanel, BorderLayout.PAGE_START);
		
		JPanel card1 = new JPanel(new BorderLayout(10, 25)); 
	
		ImportantMessagePanel panelImportantMsg = new ImportantMessagePanel("Compare multiple profile lines with a single numeric source.");
		
		card1.add(panelImportantMsg, BorderLayout.NORTH);
		
		JPanel selection1Panel = new JPanel();
		selection1Panel.setLayout(new BorderLayout(10, 15));
		
		CompoundBorder border1 = BorderFactory.createCompoundBorder(new LineBorder(groupBorder), new EmptyBorder(10, 10, 10, 15)); 
		TitledBorder titledborder = BorderFactory.createTitledBorder(border1, "", TitledBorder.LEFT, TitledBorder.TOP);
		selection1Panel.setBorder(titledborder); 
		
		JPanel addProfilesPanel = new JPanel(new BorderLayout(10, 10));
		
		JButton btnConfigProfiles = new JButton("ADD PROFILES");
		btnConfigProfiles.setUI(new LikeDefaultButtonUI());
		btnConfigProfiles.addActionListener(e -> showProfileManager(e));
		addProfilesPanel.add(btnConfigProfiles, BorderLayout.NORTH);
		
		tblProfiles = new SelectedProfilesTable(this.selectedProfilesTblModel);		
		JScrollPane tableSP = new JScrollPane(tblProfiles, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel profilestablePanel = new JPanel(new GridLayout(1, 1));
	    profilestablePanel.setPreferredSize(new Dimension(300,250));	
		profilestablePanel.add(tableSP);
		addProfilesPanel.add(profilestablePanel, BorderLayout.CENTER);
		
		JPanel selectNumSourcePanel = new JPanel(new BorderLayout(10, 10));
		
		JButton btnConfigSources = new JButton("SELECT NUMERIC DATA"); //one source
		btnConfigSources.setUI(new LikeDefaultButtonUI());
		btnConfigSources.addActionListener(e -> searchMapSourceOne());
		selectNumSourcePanel.add(btnConfigSources, BorderLayout.NORTH);
			
		tblOneNumeric = new SelectedNumSourcesTable(this.selectedOneNumSourceTblModel);
		this.selectedOneNumSourceTblModel.withTable(this.tblOneNumeric);
		boolean isMultiSelect = false;
		((SelectedNumSourcesTable) tblOneNumeric).updateColumnsVisibility(isMultiSelect);
		JScrollPane tableNumSP = new JScrollPane(tblOneNumeric, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel numsourcestablePanel = new JPanel(new GridLayout(1, 1));
		numsourcestablePanel.setPreferredSize(new Dimension(300,250));	
		numsourcestablePanel.add(tableNumSP);		
		selectNumSourcePanel.add(numsourcestablePanel, BorderLayout.CENTER);	
		
		selection1Panel.add(addProfilesPanel, BorderLayout.LINE_START);
		selection1Panel.add(selectNumSourcePanel, BorderLayout.LINE_END);

		card1.add(selection1Panel, BorderLayout.CENTER);
		
		//filler
		JPanel filler1 = createFiller();
		card1.add(filler1, BorderLayout.SOUTH);		
	
		 //card2
		JPanel card2 = new JPanel(new BorderLayout(10, 25));
				
		ImportantMessagePanel panelImportantMsg2 = new ImportantMessagePanel("Compare multiple numeric sources with a single profile line.");
		
		card2.add(panelImportantMsg2, BorderLayout.NORTH);
		
		JPanel selection2Panel = new JPanel(new BorderLayout(10, 15));
		
		CompoundBorder border2 = BorderFactory.createCompoundBorder(new LineBorder(groupBorder), new EmptyBorder(10, 10, 10, 15)); 
		TitledBorder titledborder2 = BorderFactory.createTitledBorder(border2, "", TitledBorder.LEFT, TitledBorder.TOP);
		selection2Panel.setBorder(titledborder2); 
		
		//profiles combobox
		JPanel profileBoxPanel = new JPanel(new BorderLayout(10, 5));
		profileBoxPanel.setPreferredSize(new Dimension(300, 250));
		profilesBox = new JComboBox(); 
		profilesBox.addActionListener(e -> singleProfileSelected(e));
		updateCombomodel();		
		ProfileObjectComboBoxRenderer renderer= new ProfileObjectComboBoxRenderer(this.profileLView);
		profilesBox.setRenderer(renderer);
		profilesBox.setMaximumRowCount(5);
		profileBoxPanel.add(profilesBox, BorderLayout.NORTH);
		
		JPanel addSourcesPanel = new JPanel(new BorderLayout(10,10));
		JButton btnConfigSources2 = new JButton("ADD NUMERIC DATA");
		btnConfigSources2.setUI(new LikeDefaultButtonUI());
		btnConfigSources2.addActionListener(e -> searchMapSourceMany());
		addSourcesPanel.add(btnConfigSources2, BorderLayout.NORTH);
		
	
		tblManyNumeric = new SelectedNumSourcesTable(this.selectedManyNumSourcesTblModel);
		this.selectedManyNumSourcesTblModel.withTable(this.tblManyNumeric);
		isMultiSelect = true;
		((SelectedNumSourcesTable) tblManyNumeric).updateColumnsVisibility(isMultiSelect);
		JScrollPane tableNumSP2 = new JScrollPane(tblManyNumeric, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel numsourcestablePanel2 = new JPanel(new GridLayout(1, 1));
		numsourcestablePanel2.setPreferredSize(new Dimension(300,250));	
		numsourcestablePanel2.add(tableNumSP2);		
		addSourcesPanel.add(numsourcestablePanel2, BorderLayout.CENTER);
		
		selection2Panel.add(profileBoxPanel, BorderLayout.LINE_START);
		selection2Panel.add(addSourcesPanel, BorderLayout.LINE_END);	
		
		card2.add(selection2Panel, BorderLayout.CENTER);
		
		JPanel filler2 = createFiller();
		card2.add(filler2, BorderLayout.SOUTH);	

		cards = new JPanel(new CardLayout());

	    cards.add(card1, ConfigType.ONENUMSOURCE.asString());
	    cards.add(card2, ConfigType.MANYNUMSOURCES.asString());
	     
	    configureChartPanel.add(cards, BorderLayout.CENTER);		
		
	    return configureChartPanel;
	}	
	
	
	private JPanel createFiller() {
		JPanel selectionPanelzz = new JPanel();
		selectionPanelzz.setLayout(new BorderLayout(10, 15));
		
		selectionPanelzz.setBorder(BorderFactory.createEmptyBorder(10,10,10,10)); 
		
		JPanel addProfilesPanelzz = new JPanel(new BorderLayout(10, 10));
		
		JButton btnConfigProfileszz = new JButton();
		btnConfigProfileszz.setVisible(false);
		addProfilesPanelzz.add(btnConfigProfileszz, BorderLayout.NORTH);
		
		JTable tblProfileszz = new JTable();	
		JScrollPane tableSPzz = new JScrollPane(tblProfileszz, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel profilestablePanelzz = new JPanel(new GridLayout(1, 1));
	    profilestablePanelzz.setPreferredSize(new Dimension(300,180));	
		profilestablePanelzz.add(tableSPzz);
		addProfilesPanelzz.add(profilestablePanelzz, BorderLayout.CENTER);
		
		JPanel selectNumSourcePanelzz = new JPanel(new BorderLayout(10, 10));
		
		JButton btnConfigSourceszz = new JButton(); //one source
		btnConfigSourceszz.setVisible(false);
		selectNumSourcePanelzz.add(btnConfigSourceszz, BorderLayout.NORTH);
			
		JTable tblOneNumericzz = new JTable();
		JScrollPane tableNumSPzz = new JScrollPane(tblOneNumericzz, ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		JPanel numsourcestablePanelzz = new JPanel(new GridLayout(1, 1));
		numsourcestablePanelzz.setPreferredSize(new Dimension(300,180));	
		numsourcestablePanelzz.add(tableNumSPzz);		
		selectNumSourcePanelzz.add(numsourcestablePanelzz, BorderLayout.CENTER);	
		
		selectionPanelzz.add(addProfilesPanelzz, BorderLayout.LINE_START);
		selectionPanelzz.add(selectNumSourcePanelzz, BorderLayout.LINE_END);
		
		return selectionPanelzz;
	}

	private void updateCombomodel() {
		if (profilesBox != null) {
			combomodel = (DefaultComboBoxModel) profilesBox.getModel();
			Integer selectedprofile = -1;
			// remove listener while updating model; we only want Action when users selects
			ActionListener[] actionListeners = profilesBox.getActionListeners();
			for (final ActionListener listener : actionListeners) {
				profilesBox.removeActionListener(listener);
			}
			if (combomodel != null) {
				// persist current selection
				selectedprofile = (Integer) profilesBox.getSelectedItem(); // this is Profile ID
				profilesBox.removeAllItems();
				combomodel.removeAllElements();
				// get all profiles from Profile Manager
				Map<Integer, Shape> allprofiles = this.pm.getAllProfiles();
				List<Integer> idList = new ArrayList<>();
				allprofiles.forEach((id, shape) -> {
					if (shape instanceof ProfileLine) {
						idList.add(id);
					}
				});
				for (Integer id : idList) {
					combomodel.addElement(id);
				}

				// blank entry = no selection
				combomodel.addElement(null);

				profilesBox.setModel(combomodel);

				if (selectedprofile != null) {
					if (isSlectedProfileStillExist(selectedprofile)) {
						profilesBox.setSelectedItem(selectedprofile);
					} else {
					    profilesBox.setSelectedItem(null);
				    }
			    } else {
			    	profilesBox.setSelectedItem(null);
			    }
			}
			for (final ActionListener listener : actionListeners) {
				profilesBox.addActionListener(listener);
			}
		}
	}

	private boolean isSlectedProfileStillExist(Integer selectedprofile) {
		combomodel = (DefaultComboBoxModel) profilesBox.getModel();
		boolean isPresent = false;
		if (combomodel == null) return isPresent;			
		for (int cnt = 1; cnt < combomodel.getSize(); cnt++) { //skip "null" value
			if (((Integer)combomodel.getElementAt(cnt)) == selectedprofile) { // if profile is present to CB = wasn't deleted in Main
				isPresent = true;
				break;
			}
		}
		return isPresent;
	}

	private void showProfileManager(ActionEvent e) {		
		this.pm.updateRelativeToLocationOnScreen((JComponent) e.getSource());
		buildOneNumSourcesConfig();
		this.pm.manage(this.chartConfigMap.get(ConfigType.ONENUMSOURCE)); //pm needs to remember what was selected
		this.pm.setVisible(true);
	}
	
	private void hideProfileManager() {
		if (this.pm !=null && this.pm.isVisible()) {
			this.pm.setVisible(false);
		}
	}

	private void searchMapSourceOne() {
		boolean allowSelectMultipleSources = false;
		ArrayList<MapSource> userSelectedSources = NumericMapSourceDialog.getUserSelectedSources(this, allowSelectMultipleSources, true);		
		if (userSelectedSources != null && userSelectedSources.size() > 0) {
			if (isFirstClickHappened == false) {
			    isFirstChangeOfNumSource = true;
			}
			this.controller.userAddedMapSource(userSelectedSources); //this will update config through event handling
			resetFirstClickFlags();
		}
	}
	
	private void resetFirstClickFlags() {
		isFirstClickHappened = true;
		isFirstChangeOfNumSource = false;
	}
	
	private void searchMapSourceMany() {
		boolean allowSelectMultipleSources = true;
		ArrayList<MapSource> userSelectedSources = NumericMapSourceDialog.getUserSelectedSources(this, allowSelectMultipleSources, true);
		if (userSelectedSources != null && userSelectedSources.size() > 0) {
			selectedManyNumSourcesTblModel.addData(userSelectedSources, true); //accumulate sources
			this.controller.userAddedMapSource(selectedManyNumSourcesTblModel.getModelData());	
		}
	}
	
	private void singleProfileSelected(ActionEvent e) {
		Map<Integer, Shape> selectedProfiles = new HashMap<>();
		JComboBox cb = (JComboBox) e.getSource();
		Integer newSelection = (Integer) cb.getSelectedItem(); // this is Profile ID
		if (newSelection != null && this.rdbtnManyNumeric.isSelected()) {
			ProfileLine shape = (ProfileLine) this.profileLView.getProfilelineByID(newSelection);
			if (shape != null) {
				selectedProfiles.put(newSelection, shape);											
			}
		}
		this.controller.selectedProfiles(selectedProfiles, ProfileManagerMode.SELECT_ONE);	
	}
	

	@Override
	public void modelPropertyChange(PropertyChangeEvent evt) {
		String propName = evt.getPropertyName();
		Object newVal = evt.getNewValue();
		Supplier<IChartEventHandler> handler = EVENT_HANDLER.get(propName);
		if (handler != null) {
		    handler.get().handleEvent(newVal);
		}
	}
		

	private List<MapSource> getSelectedSourcesFromPipeline(Pipeline[] chartData) {
		List<MapSource> sources = new ArrayList<>();
		for (int ind = 0; ind < chartData.length; ind++) {
			sources.add(chartData[ind].getSource());
		}
		return sources;
	}
	
	private Pipeline[] getPipelineFromSelectedSources(List<MapSource> selectedsources) {
		if (selectedsources == null) return null;
		int selectedsize = selectedsources.size();
		Pipeline pipeline[] = new Pipeline[selectedsize];
		for (int i = 0; i < selectedsize; i++) {
			pipeline[i] = new Pipeline(selectedsources.get(i),
					new Stage[] { (CompositeStage) (new BandAggregatorSettings((selectedsources.size()))).createStage() });
		}
		PipelineModel ppm = new PipelineModel(pipeline);
		try {
			pipeline = Pipeline.getDeepCopy(ppm.buildPipeline());
		} catch (CloneNotSupportedException e) {
			pipeline = null;
			e.printStackTrace();
		}
		return pipeline;
	}


	@Override
	public void actionPerformed(ActionEvent evt) { //when radio buttons switch
		String radioButtonCmd = (String)evt.getActionCommand();	
		hideProfileManager();
		buildCurrentConfig();
		CardLayout cl = (CardLayout) (cards.getLayout());
		cl.show(cards, radioButtonCmd);					
	}
	
	// invoked when tabs change or radio buttons selection change
	// to capture currently selected config and do chart
	public void buildCurrentConfig() {
		// clear chart for new configuration
		this.controller.configChanged();
		if (rdbtnOneNumeric.isSelected()) {
			buildOneNumSourcesConfig();
			this.controller.createChartFromConfiguration(this.chartConfigMap.get(ConfigType.ONENUMSOURCE));
		} else if (rdbtnManyNumeric.isSelected()) {
			buildManyNumSourcesConfig();
			this.controller.createChartFromConfiguration(this.chartConfigMap.get(ConfigType.MANYNUMSOURCES));
		}
	}
	
	private void updateConfigFromSession(SavedParams savedparams) {
		// restore received Config from saved session
		ConfigType savedConfigType = savedparams.getCurrentConfigType();

		if (savedConfigType == null) { // backward compatibility
			savedConfigType = ConfigType.ONENUMSOURCE; // if no prior config, then use ONESOURCE by default
		}

		isDefaultSource = false; //when from session - don't consider default source
		resetFirstClickFlags(); //when from session - don't consider sync-ing up One and Many
		hideProfileManager();
		
		restoreConfigONESOURCE(savedparams);
		restoreConfigMANYSOURCES(savedparams);
		
		updateOneNumSourcesConfigFromSession(this.chartConfigMap.get(ConfigType.ONENUMSOURCE));
		updateManyNumSourcesConfigFromSession(this.chartConfigMap.get(ConfigType.MANYNUMSOURCES));
	
		String radioButtonCmd = savedConfigType.asString();

		if (savedConfigType == ConfigType.ONENUMSOURCE) {
			rdbtnOneNumeric.setSelected(true);
		} else if (savedConfigType == ConfigType.MANYNUMSOURCES) {
			rdbtnManyNumeric.setSelected(true);
		}
		CardLayout cl = (CardLayout) (cards.getLayout());
		cl.show(cards, radioButtonCmd);
	}

	private void restoreConfigONESOURCE(SavedParams savedparams) {
		Map<Integer, Shape> configprofiles = new HashMap<>();
		List<MapSource> configsources = new ArrayList<>();

		configprofiles.putAll(this.profileLView.restoreConfigProfilesONESOURCE(savedparams));
		configsources.addAll(this.profileLView.restoreConfigSourcesONESOURCE(savedparams));
		
		this.configOne.withProfilesToChart(configprofiles);
		this.configOne.withNumsourcesToChart(configsources);
		this.chartConfigMap.put(ConfigType.ONENUMSOURCE, this.configOne);
	}
	
	private void restoreConfigMANYSOURCES(SavedParams savedparams) {
		Map<Integer, Shape> configprofiles = new HashMap<>();
		List<MapSource> configsources = new ArrayList<>();

		configprofiles.putAll(this.profileLView.restoreConfigProfilesMANYSOURCES(savedparams));
		configsources.addAll(this.profileLView.restoreConfigSourcesMANYSOURCES(savedparams));
		
		this.configMany.withProfilesToChart(configprofiles);
		this.configMany.withNumsourcesToChart(configsources);
		this.chartConfigMap.put(ConfigType.MANYNUMSOURCES, this.configMany);
	}
	

	private void updateOneNumSourcesConfigFromSession(Config varChartConfig) {
		selectedProfilesTblModel.addData(varChartConfig.getProfilesToChart());
		selectedOneNumSourceTblModel.addData(varChartConfig.getNumsourcesToChart(), false);
		List<MapSource> varmapsources = new ArrayList<>();
		varmapsources = varChartConfig.getNumsourcesToChart();
		Pipeline[] varchartdata = null;
		Pipeline[] chartdata = null;
		varchartdata = getPipelineFromSelectedSources(varmapsources);
		if (varchartdata != null) {
			chartdata = Arrays.stream(varchartdata).toArray(Pipeline[]::new);
		}
		varChartConfig.withPipeline(chartdata);
		if (this.pm != null) {
			this.pm.manage(varChartConfig);
		}
	}
		
	
	private void updateManyNumSourcesConfigFromSession(Config varChartConfig) {
		updateCombomodel();
		Map<Integer, Shape> configprofile = new HashMap<>();
		configprofile.putAll(varChartConfig.getProfilesToChart());
		if (profilesBox != null) {  //do not fire combobox action event
			ActionListener[] actionListeners = profilesBox.getActionListeners();
			for (final ActionListener listener : actionListeners) {
				profilesBox.removeActionListener(listener);
			}
			setSelectedProfileInCombobox(configprofile);
			for (final ActionListener listener : actionListeners) {
				profilesBox.addActionListener(listener);
			}
		}
		selectedManyNumSourcesTblModel.addData(varChartConfig.getNumsourcesToChart(), true);
		List<MapSource> varmapsources = new ArrayList<>();
		varmapsources = varChartConfig.getNumsourcesToChart();
		Pipeline[] varchartdata = null;
		Pipeline[] chartdata = null;
		varchartdata = getPipelineFromSelectedSources(varmapsources);
		if (varchartdata != null) {
			chartdata = Arrays.stream(varchartdata).toArray(Pipeline[]::new);
		}
		varChartConfig.withPipeline(chartdata);
	}

	public Config getCurrentConfig() {
		Config currentConfig = null;
		if (rdbtnOneNumeric.isSelected()) {
			currentConfig = this.chartConfigMap.get(ConfigType.ONENUMSOURCE);
		} else if (rdbtnManyNumeric.isSelected()) {
			currentConfig = this.chartConfigMap.get(ConfigType.MANYNUMSOURCES);
		}	
		return currentConfig;
	}
	
	public Map<ConfigType, Config> getBothConfig() {
		Map<ConfigType, Config> currentconfigmap = new HashMap<>();
		Config currentConfig = null;
		currentConfig = getOneNumSourceConfig();
		currentconfigmap.put(ConfigType.ONENUMSOURCE, currentConfig);
		currentConfig = getManyNumSourcesConfig();
		currentconfigmap.put(ConfigType.MANYNUMSOURCES, currentConfig);
		return currentconfigmap;
	}
	
	private Config getOneNumSourceConfig() {
		Config varconfig = this.chartConfigMap.get(ConfigType.ONENUMSOURCE);
		varconfig.withConfigName("default");
		varconfig.withConfigType(ConfigType.ONENUMSOURCE);
		varconfig.withProfilesToChart(selectedProfilesTblModel.getModelData());
		List<MapSource> varmapsources = new ArrayList<>();
		varmapsources = selectedOneNumSourceTblModel.getModelData();
		varconfig.withNumsourcesToChart(varmapsources);
		Pipeline[] varchartdata = null;
		Pipeline[] chartdata = null;
		varchartdata = getPipelineFromSelectedSources(varmapsources);
		if (varchartdata != null) {
		    chartdata = Arrays.stream(varchartdata).toArray(Pipeline[]::new);
		}
		varconfig.withPipeline(chartdata);
		return varconfig;
	}
	
		
	private Config getManyNumSourcesConfig() {
		Config varconfig = this.chartConfigMap.get(ConfigType.MANYNUMSOURCES);
		varconfig.withConfigName("default");
		varconfig.withConfigType(ConfigType.MANYNUMSOURCES);
		Integer selectedProfileID = (Integer) profilesBox.getSelectedItem(); // this is Profile ID
		Map<Integer, Shape> selectedProfile = getProfileFromCombobox(selectedProfileID);
		varconfig.withProfilesToChart(selectedProfile);
		List<MapSource> varmapsources = new ArrayList<>();
		varmapsources = selectedManyNumSourcesTblModel.getModelData();
		varconfig.withNumsourcesToChart(varmapsources);
		Pipeline[] varchartdata = null;
		Pipeline[] chartdata = null;
		varchartdata = getPipelineFromSelectedSources(varmapsources);
		if (varchartdata != null) {
			chartdata = Arrays.stream(varchartdata).toArray(Pipeline[]::new);
		}
		varconfig.withPipeline(chartdata);
		return varconfig;
	}


	private void buildOneNumSourcesConfig() {
		Config thisconfig = chartConfigMap.get(ConfigType.ONENUMSOURCE);
		thisconfig.withConfigName("default");
		thisconfig.withConfigType(ConfigType.ONENUMSOURCE);
		thisconfig.withProfilesToChart(selectedProfilesTblModel.getModelData());
		List<MapSource> varmapsources = new ArrayList<>();
		varmapsources = selectedOneNumSourceTblModel.getModelData();
		thisconfig.withNumsourcesToChart(varmapsources);
		Pipeline[] varchartdata = null;
		Pipeline[] chartdata = null;
		varchartdata = getPipelineFromSelectedSources(varmapsources);
		if (varchartdata != null) {
		    chartdata = Arrays.stream(varchartdata).toArray(Pipeline[]::new);
		}
		thisconfig.withPipeline(chartdata);
		this.controller.userAddedMapSource(thisconfig.getNumsourcesToChart());	//reset pipeline
	}

	private void buildManyNumSourcesConfig() {
		 Config thisconfig = chartConfigMap.get(ConfigType.MANYNUMSOURCES);
		 thisconfig.withConfigName("default");
		 thisconfig.withConfigType(ConfigType.MANYNUMSOURCES);
		 Integer selectedProfileID = (Integer)profilesBox.getSelectedItem(); // this is Profile ID
		 Map<Integer, Shape> selectedProfile = getProfileFromCombobox(selectedProfileID);
		 thisconfig.withProfilesToChart(selectedProfile);
		 List<MapSource> varmapsources = new ArrayList<>();
		 varmapsources = selectedManyNumSourcesTblModel.getModelData();
		 thisconfig.withNumsourcesToChart(varmapsources);
		 Pipeline[] varchartdata = null;
		 Pipeline[] chartdata = null;
		 varchartdata = getPipelineFromSelectedSources(varmapsources);
		 if (varchartdata != null) {
			chartdata = Arrays.stream(varchartdata).toArray(Pipeline[]::new);
		 }
		thisconfig.withPipeline(chartdata);
		this.controller.userAddedMapSource(thisconfig.getNumsourcesToChart());  //reset pipeline
	}
			
	
	private Map<Integer, Shape> getProfileFromCombobox(Integer selectedID) {
		Map<Integer, Shape> selectedProfiles = new HashMap<>();		
		if (selectedID != null) {
			ProfileLine shape = (ProfileLine) this.profileLView.getProfilelineByID(selectedID);
			if (shape != null) {
				selectedProfiles.put(selectedID, shape);
			}
		}
		return selectedProfiles;
	}
	
	private void setSelectedProfileInCombobox(Map<Integer, Shape> profilesToChart) {
		combomodel = (DefaultComboBoxModel) profilesBox.getModel();
		if (combomodel != null) {
			Map<Integer, Shape> profile = profilesToChart;
			if (profile.isEmpty())
				return;
			Map.Entry<Integer, Shape> entry = profile.entrySet().iterator().next();
			Integer ID = entry.getKey();
			combomodel.setSelectedItem(ID);
		}
	}


	public void notifyRestoredFromSession(SavedParams savedParams) {
		updateConfigFromSession(savedParams);
	}	

	
	// config events
	private class NewMapSourceEventHandler implements IChartEventHandler {		
		@Override
		public void handleEvent(Object newVal) {
			Pair newval = (Pair) newVal;
			Pipeline[] newdata = (Pipeline[]) newval.getKey();
			if (newdata != null) {
				Pipeline[] chartData = Arrays.stream(newdata).toArray(Pipeline[]::new);
				List<MapSource> selectedsources = getSelectedSourcesFromPipeline(chartData);
				if (isDefaultSource) {
					configureBoth(selectedsources);
					isDefaultSource = false;
				} else {
					if (rdbtnOneNumeric.isSelected()) {
						selectedOneNumSourceTblModel.addData(selectedsources, false);
						if (isFirstChangeOfNumSource) { // sync up with multiple sources, if this is first change
							selectedManyNumSourcesTblModel.addData(selectedsources, false);
							isFirstChangeOfNumSource = false;
						}
					} else if (rdbtnManyNumeric.isSelected()) {
						selectedManyNumSourcesTblModel.addData(selectedsources, true);
					}
				}
			}
		}

		private void configureBoth(List<MapSource> selectedsources) {		
			selectedOneNumSourceTblModel.addData(selectedsources, false); 			
			selectedManyNumSourcesTblModel.addData(selectedsources, true);			
		}
	}
	
	
	private class SelectedToPlotEventHandler implements IChartEventHandler {
		@Override
		public void handleEvent(Object newVal) {
			Pair newval = (Pair) newVal;
			Map<Integer, Shape> selectedprofiles = new HashMap<>();
			selectedprofiles.putAll((Map<? extends Integer, ? extends Shape>) newval.getKey());
			ProfileManagerMode mode = (ProfileManagerMode) newval.getValue();
			
			updatePerMode(mode, selectedprofiles);
			
			if (rdbtnOneNumeric.isSelected()) { // many profiles = one num source
				buildOneNumSourcesConfig();
				pm.manage(chartConfigMap.get(ConfigType.ONENUMSOURCE));
			} else if (rdbtnManyNumeric.isSelected()) {
				buildManyNumSourcesConfig();
			}
			buildCurrentConfig();
		}

		private void updatePerMode(ProfileManagerMode mode, Map<Integer, Shape> selectedprofiles) {
			if (mode == ProfileManagerMode.MANAGE) {
				selectedProfilesTblModel.addData(selectedprofiles);	
				updateCombomodel();
				pm.manage();
			} else if ( mode == ProfileManagerMode.SELECT_MANY) {
				selectedProfilesTblModel.addData(selectedprofiles);
			} else if (mode == ProfileManagerMode.SELECT_ONE) {
				 updateCombomodel();
				 setSelectedProfileInCombobox(selectedprofiles);
			} else if (mode == ProfileManagerMode.CREATE_NEW_CHART) {
				ActionListener[] actionListeners = rdbtnOneNumeric.getActionListeners();
				for (final ActionListener listener : actionListeners) {
					rdbtnOneNumeric.removeActionListener(listener);
				}
				 rdbtnOneNumeric.setSelected(true);
				for (final ActionListener listener : actionListeners) {
					rdbtnOneNumeric.addActionListener(listener);
				}
				selectedProfilesTblModel.addData(selectedprofiles);	
			} else if (mode == ProfileManagerMode.ADD_TO_CHART) {
				ActionListener[] actionListeners = rdbtnOneNumeric.getActionListeners();
				for (final ActionListener listener : actionListeners) {
					rdbtnOneNumeric.removeActionListener(listener);
				}
				 rdbtnOneNumeric.setSelected(true);
				for (final ActionListener listener : actionListeners) {
					rdbtnOneNumeric.addActionListener(listener);
				}
				//get already selected profiles
				Map<Integer, Shape> currentlyselected = new HashMap<>();
				currentlyselected.putAll(selectedProfilesTblModel.getModelData());
				currentlyselected.putAll(selectedprofiles);
				selectedProfilesTblModel.addData(currentlyselected);	
			} else if (mode == ProfileManagerMode.RENAME_PROFILE) {
				updateCombomodel();
				pm.manage();
				//update already selected profiles
				Map<Integer, Shape> currentlyselected = new HashMap<>();
				currentlyselected.putAll(selectedProfilesTblModel.getModelData());
				selectedProfilesTblModel.addData(currentlyselected);					
			}
		}
	}

	
	private class NewProfileEventHandler implements IChartEventHandler {
		@Override
		public void handleEvent(Object newVal) {
			Pair newval = (Pair) newVal;
			Map<Integer, Shape> profiles = new LinkedHashMap<>();
			profiles.putAll((Map<? extends Integer, ? extends Shape>) newval.getKey());			
			updateCombomodel();
			updateSelectedProfiles();
			buildOneNumSourcesConfig(); //needed for pm
			pm.manage(chartConfigMap.get(ConfigType.ONENUMSOURCE));  // pm needs to remember what was selected
			// if this is first (or only) profile drawn, then create chart by default with one source
			if (profiles.size() == 1) {
				buildFirstDrawnProfileAsDefault(profiles);	 //select it in both One source and combobox			
			}
			buildCurrentConfig();  //also here if from session - don't build ??
		}

		private void updateSelectedProfiles() {
			//if profile was deleted in main view but is present in selected profiles - remove it
			Map<Integer, Shape> allprofiles = new HashMap<>();
			Map<Integer, Shape> newselected = new HashMap<>();
			allprofiles.putAll(pm.getAllProfiles());
			Map<Integer, Shape> currentselected = selectedProfilesTblModel.getModelData();
			newselected.putAll(currentselected);
			for (Entry<Integer, Shape> entry : currentselected.entrySet()) {
				Integer selectedID = entry.getKey();
				Shape shape = allprofiles.get(selectedID);
				if (shape == null) {
					newselected.remove(selectedID);
				}
			}
			selectedProfilesTblModel.addData(newselected);				
		}

		private void buildFirstDrawnProfileAsDefault(Map<Integer, Shape> profiles) {
			selectedProfilesTblModel.addData(profiles);
			if (profilesBox != null) {  //do not fire combobox action event
				ActionListener[] actionListeners = profilesBox.getActionListeners();
				for (final ActionListener listener : actionListeners) {
					profilesBox.removeActionListener(listener);
				}
				setSelectedProfileInCombobox(profiles);
				for (final ActionListener listener : actionListeners) {
					profilesBox.addActionListener(listener);
				}
			}
		}
	}


	public void close() {
		hideProfileManager();
		if (this.pm != null) {
		    this.pm.dispose();
		}
	}

}



