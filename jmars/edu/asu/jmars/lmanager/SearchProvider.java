package edu.asu.jmars.lmanager;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Set;

import edu.asu.jmars.Main;
import edu.asu.jmars.layer.LayerParameters;
import edu.asu.jmars.layer.map2.MapServer;
import edu.asu.jmars.layer.map2.MapServerFactory;
import edu.asu.jmars.layer.map2.MapSource;
import edu.asu.jmars.layer.map2.custom.CustomMap;
import edu.asu.jmars.layer.map2.custom.CustomMapBackendInterface;
import edu.asu.jmars.util.Config;
import edu.asu.jmars.util.DebugLog;

public class SearchProvider {

    public static final int TAG_NAME = 0;
    public static final int TAG_DESC = 1;
    public static final int TAG_CITATION = 2;
    public static final int TAG_LINKS = 3;
    public static final int TAG_ALL = 4;
    
    private ArrayList<String> searchOptions = new ArrayList<String>();
    private ArrayList<LayerParameters> searchParams = new ArrayList<LayerParameters>();
    private HashMap<String, SearchResultRow> searchRowsById = new HashMap<String, SearchResultRow>();
    private HashMap<String, SearchResultRow> browseRowsById = new HashMap<String, SearchResultRow>();
    private ArrayList<SearchResultRow> favoriteRows = new ArrayList<SearchResultRow>();//used for favorites tab
    private ArrayList<SearchResultRow> customMapRows = new ArrayList<SearchResultRow>();//used for custom map tab
    private HashMap<String, ArrayList<LayerParameters>> uniqueRows = new HashMap<String,ArrayList<LayerParameters>>();
    private HashMap<String, ArrayList<String>> idsByPath = new HashMap<String, ArrayList<String>>();
    private ArrayList<String> categories = new ArrayList<String>();
    private HashMap<String, ArrayList<String>> subcatsByCat = new HashMap<String, ArrayList<String>>();
    private HashMap<String, ArrayList<String>> topicsBySubcat = new HashMap<String, ArrayList<String>>();
    private ArrayList<String> favorites = new ArrayList<String>();//list of ids used for search and browse
    private ArrayList<String> customMapFavorites = new ArrayList<String>();
    private ArrayList<String> mapSourceFavorites = new ArrayList<String>();
    private HashMap<String,String> customMapNameIdMap = new HashMap<String,String>();
    private HashMap<String,String> layerNameIdMap = new HashMap<String,String>();//for searching on name
    private HashMap<String, ArrayList<SearchResultRow>> searchResultRowByCustomMapId = new HashMap<String, ArrayList<SearchResultRow>>();
    private HashMap<String, CustomMap> customMaps = new HashMap<String, CustomMap>();
    private HashMap<MapSource, SearchResultRow> searchRowsByMapSource = new HashMap<MapSource, SearchResultRow>();
    private HashMap<String, SearchResultRow> searchRowsByMapSourceTitle = new HashMap<String, SearchResultRow>();
    private ArrayList<String> layerParamMapSources = new ArrayList<String>();
    
    private static SearchProvider instance = null;
    
    public static SearchProvider getInstance() {
        if (instance == null) {
           instance = new SearchProvider(); 
        }
        return instance;
    }
    private SearchProvider() {
        populateSearchOptions();
    }
    public String getLayerTooltipText(String layerParameterId) {
    	String tooltip = null;
    	SearchResultRow searchResultRow = searchRowsById.get(layerParameterId);
    	if (searchResultRow != null) {
    		tooltip = searchResultRow.getLayerTooltipText();
    	}
    	return tooltip;
    }
    private void loadLayers() {
        searchParams.addAll(LayerParameters.lParameters);
        String layersGroups = Config.get("layers.group");
        boolean useBasic = false;
        //none of the basic logic matters for any body except Mars
        if (Main.getCurrentBody().equalsIgnoreCase("Mars") && layersGroups.trim().toLowerCase().indexOf("basic") > -1) {
        	boolean classicMode = Config.get("classic_mode",false);
        	if (!classicMode) {
        		useBasic = true;//if not overwritten in the config
        	}
        }
        ArrayList<LayerParameters> toRemoveFromHome = new ArrayList<LayerParameters>();
        for (LayerParameters layer : searchParams) {
        	if (layer.category == null || layer.category.trim().equals("")) {
        		continue;
        	}
        	//basic layers have a layergroup of "basic" and a category of "Home"
        	//normal "Home" layers have a layergroup of all and a category of "Home"
        	if (useBasic && layer.category.equalsIgnoreCase("Home") && !layer.layergroup.equalsIgnoreCase("basic")) {
        		toRemoveFromHome.add(layer);
            	continue;//don't go through if we are in basic mode and this is a home layer with an all layergroup
            } else if (!useBasic && layer.category.equalsIgnoreCase("Home") && layer.layergroup.equalsIgnoreCase("basic")) {
            	toRemoveFromHome.add(layer);
            	continue;//don't go through if we in normal mode and this is a basic home layer
            }
            ArrayList<LayerParameters> list = uniqueRows.get(layer.id);
            if (list == null) {
                list = new ArrayList<LayerParameters>();
                uniqueRows.put(layer.id,list);
            }
            list.add(layer);
            layerNameIdMap.put(layer.name, layer.id);//for searching on name
            
            //build a full hierarchy to prevent having to loop after this
            LayerParameters lp = layer;
            String cat = lp.category;
            
            if (cat != null && cat.trim().length() > 0) {
                String subcat = (lp.subcategory == null || lp.subcategory.trim().length() == 0 ? "nosubcat" : lp.subcategory);
                String topic = lp.topic;
                if (!categories.contains(cat)) {
                    categories.add(cat);
                }
                ArrayList<String> subcats = subcatsByCat.get(cat);
                if (subcats == null) {
                    subcats = new ArrayList<String>();
                    subcatsByCat.put(cat,subcats);
                }
                if (!subcats.contains(subcat) && !"nosubcat".equals(subcat)) {
                    subcats.add(subcat);
                }
                String catSubcat = cat + "-" + subcat;
                ArrayList<String> topics = topicsBySubcat.get(catSubcat);
                if (topics == null) {
                    topics = new ArrayList<String>();
                    topicsBySubcat.put(catSubcat,topics);
                }
                if (!topics.contains(topic)) {
                    topics.add(topic);
                }
                String fullPath =  catSubcat + "-" + topic;
                ArrayList<String> ids = idsByPath.get(fullPath);
                if (ids == null) {
                    ids = new ArrayList<String>();
                    idsByPath.put(fullPath,ids);
                }
                if (lp.id != null && !ids.contains(lp.id)) {//custom map ids are null and will be handled elsewhere
                    ids.add(lp.id);
                }
            }
        }
        //need to remove extra home layers from search params
        searchParams.removeAll(toRemoveFromHome);
        
        for (ArrayList<LayerParameters> list : uniqueRows.values()) {
            String id = list.get(0).id;
            boolean favorite = false;

            if (favorites != null && favorites.contains(id)) {
                favorite = true;
            }
            
            //build SearchResultRows to prevent building them later - need one per tab
            SearchResultRow sRow = new SearchResultRow(list,favorite);
            SearchResultRow bRow = new SearchResultRow(list,favorite);
            this.searchRowsById.put(id, sRow);
            this.browseRowsById.put(id, bRow);
        }
        
        if(userLoggedIn()){
        	searchResultRowByCustomMapId.clear();
        	if (LayerParameters.customlParameters != null) {
        		searchParams.addAll(LayerParameters.customlParameters);
        	}
            ArrayList<String> customIds = new ArrayList<String>();
            ArrayList<CustomMap> existingMapList = CustomMapBackendInterface.getExistingMapList();//just getting the list, not retrieving from the database
    		for (CustomMap map : existingMapList) {
    			this.customMaps.put(map.getCustomMapId(), map);//master list for search (needed on custom map refresh)

                String id = "cm_"+map.getCustomMapId();
                    
                boolean favorite = false;
                if (customMapFavorites != null && customMapFavorites.contains(id)) {
                    favorite = true;
                }

                this.setupCustomMap(map, favorite, id);
                
                customIds.add(id);
                ArrayList<LayerParameters> customLPIds = new ArrayList<LayerParameters>();
                customLPIds.add(map.getLayerParameters());
                this.uniqueRows.put(id, customLPIds);
            }
            categories.add("Custom");
            subcatsByCat.put("Custom",new ArrayList<String>());
            ArrayList<String> cmTopics = new ArrayList<String>();
            cmTopics.add("Custom");
            topicsBySubcat.put("Custom-nosubcat",cmTopics);
            this.idsByPath.put("Custom-nosubcat-Custom",customIds);
            
        }
    }
    public void rebuildSearchRow(String customMapId) {
    	ArrayList<SearchResultRow> rows = searchResultRowByCustomMapId.get(customMapId);
    	for (SearchResultRow row : rows) {
    		row.rebuildRow();
    	}
    }
    public void removeCustomMapSearchRow(CustomMap map) {
    	ArrayList<SearchResultRow> customMapSearchRows = searchResultRowByCustomMapId.get(map.getCustomMapId());
    	for (SearchResultRow row: customMapSearchRows) {
    		row.setVisible(false);
    	}
    	searchResultRowByCustomMapId.remove(map.getCustomMapId());
    	searchParams.remove(map.getLayerParameters());
    	this.idsByPath.get("Custom-nosubcat-Custom").remove(map.getLayerParameters().id);
    	this.uniqueRows.remove(map.getLayerParameters().id);
    	//TODO: favorite needs to be checked/removed?
    }

    public void refreshCustomMapSearch(ArrayList<CustomMap> maps) {
    	ArrayList<CustomMap> toRemove = new ArrayList<CustomMap>();
    	toRemove.addAll(this.customMaps.values());//keep track of what old maps existed that are not in the new list
    	for (CustomMap map : maps) {
    		if (this.customMaps.containsKey(map.getCustomMapId())) {
    			CustomMap existingMap = this.customMaps.get(map.getCustomMapId());
    			map.setLayerParameters(existingMap.getLayerParameters());//transfer layer parameters from old map to new custom map object
    			toRemove.remove(existingMap);//won't need to delete this map, it existed before and after refresh
    			LayerParameters.updateCustomMapLP(map);
    			ArrayList<SearchResultRow> searchRowList = this.searchResultRowByCustomMapId.get(map.getCustomMapId());
    			for (SearchResultRow row : searchRowList) {
    				row.rebuildRow();//update information in search result row object
    			}
    		} else {
        		addCustomMapSearchRow(map);//add a new object
    		}
    	}
    	for(CustomMap remove : toRemove) {
    		removeCustomMapSearchRow(remove);//remove maps that are gone now (share removed)
    	}
    }
    public void addCustomMapSearchRow(CustomMap map) {
    	LayerParameters.createCustomMapLP(map);
    	setupCustomMap(map, false, map.getLayerParameters().id);
    	this.idsByPath.get("Custom-nosubcat-Custom").add(map.getLayerParameters().id);
    	ArrayList<LayerParameters> customLPIds = new ArrayList<LayerParameters>();
        customLPIds.add(map.getLayerParameters());
    	this.uniqueRows.put(map.getLayerParameters().id, customLPIds);
    	searchParams.add(map.getLayerParameters());
    }
    private void setupCustomMap(CustomMap map, boolean favorite, String lpId) {
    	SearchResultRow sRow = new SearchResultRow(map, favorite);
        SearchResultRow bRow = new SearchResultRow(map, favorite);
        SearchResultRow cRow = new SearchResultRow(map, favorite);
        sRow.setIsCustom(true);
        bRow.setIsCustom(true);
        cRow.setIsCustom(true);
        
        //Add the search rows to a HashMap by custom map id so that we can call a static method to update when needed
        ArrayList<SearchResultRow> searchRows = searchResultRowByCustomMapId.get(map.getCustomMapId());
        if (searchRows == null) {
        	searchRows = new ArrayList<SearchResultRow>();
        }
        searchRows.clear();
        searchRows.add(sRow);
        searchRows.add(bRow);
        searchRows.add(cRow);
        searchResultRowByCustomMapId.put(map.getCustomMapId(), searchRows);
        
        this.searchRowsById.put(lpId,sRow);
        this.browseRowsById.put(lpId,bRow);
        this.customMapRows.add(cRow);
        map.getLayerParameters().setCustomId(lpId);
        layerNameIdMap.put(map.getName(), lpId);
    }
    public String getLayerId(String layerName) {
    	return this.layerNameIdMap.get(layerName);
    }
    private static volatile boolean searchPrepared = false;
    public static synchronized boolean isSearchPrepared() {
    	return searchPrepared;
    }
    private static Thread prepareThread = null;
    public static void prepareSearch() {
        Runnable run = new Runnable() {
            
            @Override
            public void run() {
                while (!LayerParameters.isInitializationComplete() || LayerParameters.lParameters.size() < 1) {
                    try {
                        Thread.sleep(1000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                }
                SearchProvider sp = getInstance();
                try {
                    sp.reset();
                    if(userLoggedIn()){
                        sp.loadCustomMaps();
                        sp.loadFavoriteIds();
                    }
                    sp.loadLayers();
                    sp.loadMapSources();
                    if(userLoggedIn()){
                    	sp.cleanupFavorites();
                        sp.loadFavoriteLayers();
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                }
                searchPrepared = true;
            }
        };
        MapServerFactory.whenMapServersReady(new Runnable() {
			
			@Override
			public void run() {
				prepareThread = new Thread(run);
		        prepareThread.start();
			}
		});
        
        
    }
    public static boolean prepareSearchComplete() {
    	try {
    		while (prepareThread == null) {
    			Thread.sleep(500);
    		}
    		while (!searchPrepared) {
    			Thread.sleep(500);
    		}
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return true;
    }
    public static boolean userLoggedIn() {
        if (Main.USER!=null && Main.USER.length()>0) {
            return true;
        }
        return false;
    }
    private void reset() {
        searchParams = new ArrayList<LayerParameters>();
        searchRowsById = new HashMap<String, SearchResultRow>();
        browseRowsById = new HashMap<String, SearchResultRow>();
        uniqueRows = new HashMap<String,ArrayList<LayerParameters>>();
        idsByPath = new HashMap<String, ArrayList<String>>();
        categories = new ArrayList<String>();
        subcatsByCat = new HashMap<String, ArrayList<String>>();
        topicsBySubcat = new HashMap<String, ArrayList<String>>();
        layerNameIdMap = new HashMap<String, String>();
        this.favoriteRows.clear();
        favorites.clear();
        customMapFavorites.clear();
        customMapRows.clear();
        
    }

    public String getCustomMapId(String mapName) {
        return this.customMapNameIdMap.get(mapName);
    }
    private void loadCustomMaps() {
        customMapNameIdMap = CustomMapBackendInterface.getExistingNameIdMap();
    }
    private void loadFavoriteIds() {
        HashMap<String, ArrayList<String>> loadFavorites = CustomMapBackendInterface.loadFavorites();
        favorites.addAll(loadFavorites.get("layers"));
        customMapFavorites.addAll(loadFavorites.get("custom map"));
        mapSourceFavorites.addAll(loadFavorites.get("map_source"));
    }
    private void cleanupFavorites() {
    	 ArrayList<String> toRemove = new ArrayList<String>();
         for (String fave : favorites) {
         	if (!this.uniqueRows.containsKey(fave)) {
         		toRemove.add(fave);
         	}
         }
         favorites.removeAll(toRemove);//cleanup favorites that don't exist (could be because of basic mode vs classic mode)
    }
    public void addFavorite(String layerId) {
        if (layerId.startsWith("cm_")) {
            if (!customMapFavorites.contains(layerId)) {
                customMapFavorites.add(layerId);
                SearchResultRow faveRow = new SearchResultRow(uniqueRows.get(layerId), true);
                faveRow.setIsCustom(true);
                faveRow.buildRow();
                this.favoriteRows.add(faveRow);
            }
        } else {
            //add to the list of favorite ids and add a new row to the favoriteRows for the favorite tab
            if (!favorites.contains(layerId)) {
                favorites.add(layerId);
                SearchResultRow faveRow = new SearchResultRow(uniqueRows.get(layerId), true);
                faveRow.buildRow();
                this.favoriteRows.add(faveRow);
            }
        }
        
        //need to change the icon for the proper rows on browse and search tabs
        toggleFaveIconForRow(layerId, true);
    }
    public void addMapSourceFavorite(String mapSourceName) {
    	if (!mapSourceFavorites.contains(mapSourceName)) {
    		mapSourceFavorites.add(mapSourceName);
    		for (MapSource source : mapSources) {
    			if (source.getTitle().equalsIgnoreCase(mapSourceName)) {
	    			SearchResultRow faveRow = new SearchResultRow(source, true);
	                faveRow.buildRow();
	                this.favoriteRows.add(faveRow);
	                break;
    			}
    		}
    		
    	}
    }
    public void deleteFavorite(String layerId, String mapSourceName) {
    	if (mapSourceName != null) {
    		ArrayList<SearchResultRow> rowsForDelete = new ArrayList<SearchResultRow>();
            if (mapSourceFavorites.contains(mapSourceName)) {
            	mapSourceFavorites.remove(mapSourceName);
                for (SearchResultRow row : this.favoriteRows) {
                	if (row.isMapSource()) {
	                    if (row.getMapSourceTitle().equalsIgnoreCase(mapSourceName)) {
	                        rowsForDelete.add(row);
	                    }
                	}
                }
                this.favoriteRows.removeAll(rowsForDelete);
            }
    	} else if (layerId.startsWith("cm_")) {
            ArrayList<SearchResultRow> rowsForDelete = new ArrayList<SearchResultRow>();
            if (customMapFavorites.contains(layerId)) {
                customMapFavorites.remove(layerId);
                for (SearchResultRow row : this.favoriteRows) {
                    if (row.getLayerId().equals(layerId)) {
                        rowsForDelete.add(row);
                    }
                }
                this.favoriteRows.removeAll(rowsForDelete);
            }
        } else {
            //remove from the list of ids for favorites, and from the favoriteRows (rows displayed on the favorites tab)
            ArrayList<SearchResultRow> rowsForDelete = new ArrayList<SearchResultRow>();
            if (favorites.contains(layerId)) {
                favorites.remove(layerId);
                for (SearchResultRow row : this.favoriteRows) {
                    if (row.getLayerId().equals(layerId)) {
                        rowsForDelete.add(row);
                    }
                }
                this.favoriteRows.removeAll(rowsForDelete);
            }
        }
        
        //Need to change the icon for the proper rows on all tabs
    	if (mapSourceName != null) {
    		toggleMapSourceFaveIconForRow(mapSourceName, false);
    	} else {
    		toggleFaveIconForRow(layerId,false);
    	}
    }
    private void toggleMapSourceFaveIconForRow(String mapSourceName, boolean on) {            
        for(SearchResultRow row : this.favoriteRows) {
        	if (row.isMapSource()) {
	            if (row.getMapSourceTitle().equalsIgnoreCase(mapSourceName)) {
	                if ((row.isFavoriteIconOn() && !on) || (!row.isFavoriteIconOn() && on)) {
	                    row.toggleFavoriteIcon(on);//only toggle it if it needs to be (need to update 2 other tabs but not the same one twice)
	                }
	            }
        	}
        }
        
        SearchResultRow sRow = this.searchRowsByMapSourceTitle.get(mapSourceName);
        
        if ((sRow.isFavoriteIconOn() && !on) || (!sRow.isFavoriteIconOn() && on)) {
            sRow.toggleFavoriteIcon(on);//only toggle it if it needs to be (need to update 2 other tabs but not the same one twice)
        }
    }
    private void toggleFaveIconForRow(String layerId, boolean on) {            
        for(SearchResultRow row : this.favoriteRows) {
            if (row.getLayerId().equals(layerId)) {
                if ((row.isFavoriteIconOn() && !on) || (!row.isFavoriteIconOn() && on)) {
                    row.toggleFavoriteIcon(on);//only toggle it if it needs to be (need to update 2 other tabs but not the same one twice)
                }
            }
        }
        
        SearchResultRow bRow = this.browseRowsById.get(layerId);
        SearchResultRow sRow = this.searchRowsById.get(layerId);
        
        if ((bRow.isFavoriteIconOn() && !on) || (!bRow.isFavoriteIconOn() && on)) {
            bRow.toggleFavoriteIcon(on);//only toggle it if it needs to be (need to update 2 other tabs but not the same one twice)
        }
        if ((sRow.isFavoriteIconOn() && !on) || (!sRow.isFavoriteIconOn() && on)) {
            sRow.toggleFavoriteIcon(on);//only toggle it if it needs to be (need to update 2 other tabs but not the same one twice)
        }
    }
    private void loadFavoriteLayers() {
    	//this method is for creating search result rows that are displayed on the favorites tab
        for(String faveId : this.favorites) {
            if (this.uniqueRows.containsKey(faveId)) {//faves currently are not by body
                ArrayList<LayerParameters> list = this.uniqueRows.get(faveId);
                SearchResultRow fRow = new SearchResultRow(list,true);
                fRow.buildRow();
                this.favoriteRows.add(fRow);
            }
        }
        
        for(String faveId : this.customMapFavorites) {
            if (this.uniqueRows.containsKey(faveId)) {//faves currently are not by body
                ArrayList<LayerParameters> list = this.uniqueRows.get(faveId);
                SearchResultRow fRow = new SearchResultRow(list,true);
                fRow.setIsCustom(true);
                fRow.buildRow();
                this.favoriteRows.add(fRow);
            }
        }
        for (String faveId : this.mapSourceFavorites) {
        	for (MapSource source : this.mapSources) {
        		if (source.getTitle().equalsIgnoreCase(faveId)) {
        			SearchResultRow fRow = new SearchResultRow(source, true);
                    fRow.buildRow();
                    this.favoriteRows.add(fRow);
        		}
        	}
        }
    }
    public ArrayList<SearchResultRow> getFavoriteLayers() {
        return this.favoriteRows;
    }
    public ArrayList<SearchResultRow> getCustomMapLayers() {
    	return this.customMapRows;
    }
    private SearchResultRow buildRow(String id, boolean searchFlag) {
        HashMap<String, SearchResultRow> rowMap = null;
        if (searchFlag) {
            rowMap = this.searchRowsById;
        } else {
            rowMap = this.browseRowsById;
        }

        SearchResultRow row = rowMap.get(id);
        if (row == null) {
        	DebugLog.instance().println("Id: "+id+" is an invalid search result. Ignoring this layer.");
        	return null;
        }
        row.buildRow();
        return row;
    }

    public ArrayList<SearchResultRow> buildInitialLayerList(boolean search, boolean customMap) {
    	ArrayList<SearchResultRow> resultSet = new ArrayList<SearchResultRow>();
    	boolean addHome = true;
    	if (search) {
    		//default custom list
    		if (customMap) {
    			String path = "Custom-nosubcat-Custom";
    			ArrayList<String> ids = idsByPath.get(path);
    			for (String id : ids) {
    				SearchResultRow buildRow = buildRow(id,search);
    				if (buildRow != null) {
    					resultSet.add(buildRow);
    				}
                    addHome = false;//don't need to append the home category 
                }
    		}
    	}
        if (addHome) {
        	String catSubcat = "Home-nosubcat";
	        ArrayList<String> topics = topicsBySubcat.get(catSubcat);
	        if (topics != null) {
	            for (String topic : topics) { 
	                String path = catSubcat + "-" + topic;
	                ArrayList<String> ids = idsByPath.get(path);
	                for (String id : ids) {
	                	SearchResultRow buildRow = buildRow(id,search);
	                	if (buildRow != null) {
	                		resultSet.add(buildRow);
	                	}
	                }
	            }
	        }
        }

        return resultSet;
    }
    public ArrayList<SearchResultRow> getLayersByTopic(String category, String subcategory) {
        ArrayList<SearchResultRow> resultSet = new ArrayList<SearchResultRow>();

        if (category != null) {
            if (subcategory == null || subcategory.trim().length() == 0) {
                subcategory = "nosubcat";
            }
            String catSubcat = category+"-"+subcategory;
            
            ArrayList<String> topics = topicsBySubcat.get(catSubcat);
            if (topics.size() == 0) {
                ArrayList<String> ids = idsByPath.get(catSubcat);
                for (String id : ids) {
                	SearchResultRow buildRow = buildRow(id,false);
                	if (buildRow != null) {
                		resultSet.add(buildRow);
                	}
                }
            } else {
                for (String topic : topics) { 
                    String path = catSubcat + "-" + topic;
                    ArrayList<String> ids = idsByPath.get(path);
                    for (String id : ids) {
                    	SearchResultRow buildRow = buildRow(id,false);
                    	if (buildRow != null) {
                    		resultSet.add(buildRow);
                    	}
                    }
                }
            }
        } else {
            resultSet = this.buildInitialLayerList(false,false);
        }
        
        return resultSet;
    }
    
    private void populateSearchOptions() {
        searchOptions.add("name:");
        searchOptions.add("instrument:");
        searchOptions.add("imagery:");
        searchOptions.add("category:");
        searchOptions.add("subcategory:");
        searchOptions.add("topic:");
        searchOptions.add("desc:");
        searchOptions.add("citation:");
        searchOptions.add("links:");
        searchOptions.add("map source:");
        if (userLoggedIn()) {
            searchOptions.add("custom map:");
            searchOptions.add("favorite:");
        }
    }
    public ArrayList<String> getSearchOptions() {
    	if (Main.getCurrentBody().equalsIgnoreCase("mars")) {
    		return searchOptions;
    	} else {
    		ArrayList<String> options = new ArrayList<String>();
    		options.addAll(searchOptions);
    		for (ArrayList<String> subcats : subcatsByCat.values()) {
    			if (subcats.size() > 0) {
    				break;
    			}
    			options.remove("subcategory:");
    		}
    		for (ArrayList<String> topics : topicsBySubcat.values()) {
    			if (topics.size() > 0) {
    				break;
    			}
    			options.remove("topic:");
    		}
    		if (!categories.contains("Instrument")) {
    			options.remove("instrument:");
    		}
    		if (!categories.contains("Imagery")) {
    			options.remove("imagery:");
    		}
    		return options;
    	}
    }
    public ArrayList<String> getCategories() {
        return categories;
    }
    public ArrayList<String> getSubcategories(String category) {
        ArrayList<String> list = subcatsByCat.get(category);
        if (list != null) {
            Collections.sort(list);
        }
        return list;
    }
    
    public ArrayList<String> getSuggestionHome() {
        ArrayList<String> results = new ArrayList<String>();
        String catSubcat = "Home-nosubcat";
        ArrayList<String> topics = topicsBySubcat.get(catSubcat);
        if (topics != null) {
            for (String topic : topics) { 
                String path = catSubcat + "-" + topic;
                ArrayList<String> ids = idsByPath.get(path);
                for (String id : ids) {
                    results.add(" name:  "+this.uniqueRows.get(id).get(0).name);
                }
            }
        }
        return results;
    }
    public ArrayList<String> getSuggestionCategory() {
        ArrayList<String> results = new ArrayList<String>();
        for(String cat : this.categories) {
            results.add(" category:  "+cat);
        }
        return results;
    }
    
    public ArrayList<String> getSuggestionSubcategory() {
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> sorted = new ArrayList<String>();
        sorted.addAll(this.categories);
        Collections.sort(sorted);
        for(String cat : sorted) {
            results.add(" category: "+cat);
            ArrayList<String> subcats = this.subcatsByCat.get(cat);
            if (subcats.size() > 0) {
                ArrayList<String> sortedSubs = new ArrayList<String>();
                sortedSubs.addAll(subcats);
                Collections.sort(sortedSubs);
                for (String subcat : sortedSubs) {
                    results.add("    subcategory: " + subcat);
                }
            }
        }
        return results;
    }
    
    public ArrayList<String> getSuggestionTopic() {
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> sorted = new ArrayList<String>();
        sorted.addAll(this.categories);
        Collections.sort(sorted);
        for(String cat : sorted) {
            results.add(" category: "+cat);
            ArrayList<String> subcats = this.subcatsByCat.get(cat);
            if (subcats.size() > 0) {
                ArrayList<String> sortedSubs = new ArrayList<String>();
                sortedSubs.addAll(subcats);
                Collections.sort(sortedSubs);
                for (String subcat : sortedSubs) {
                    ArrayList<String> topics = this.topicsBySubcat.get(cat+"-"+subcat);
                    if (topics.size() > 0) {
                        results.add("    subcategory: " + subcat);
                        ArrayList<String> sortedTopics = new ArrayList<String>();
                        sortedTopics.addAll(topics);
                        for (String topic : sortedTopics) {
                            results.add("        topic: "+topic);
                        }
                    } 
                }
            } else {
                ArrayList<String> topics = this.topicsBySubcat.get(cat+"-nosubcat");
                if (topics.size() > 0) {
                    ArrayList<String> sortedTopics = new ArrayList<String>();
                    sortedTopics.addAll(topics);
                    for (String topic : sortedTopics) {
                        results.add("        topic: "+topic);
                    }
                }
            }
        }
        return results;
    }
    public ArrayList<String> getPartialSuggestionCat(String search) {
        ArrayList<String> results = new ArrayList<String>();
        for (String cat : this.categories) {
            if (cat.toLowerCase().startsWith(search.toLowerCase())) {
                results.add(" category: "+cat);
                ArrayList<String> subcats = this.subcatsByCat.get(cat);
                if (subcats.size() > 0) {
                    ArrayList<String> sortedSubs = new ArrayList<String>();
                    sortedSubs.addAll(subcats);
                    Collections.sort(sortedSubs);
                    for (String subcat : sortedSubs) {
                        results.add("    subcategory: " + subcat);
                    }
                }    
            }
        }
        
        return results;
    }
    public ArrayList<String> getSuggestionWithHierarchy(String category, String subcategory, String topicValue, String[] search, int tag) {
        ArrayList<String> dupes = new ArrayList<String>();
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> sorted = new ArrayList<String>();
        sorted.addAll(this.categories);
        Collections.sort(sorted);
        for(String cat : sorted) {
            if (category == null || cat.toLowerCase().contains(category.toLowerCase().trim())) {//matches category or no category sent
                boolean catAdded = false;
                ArrayList<String> subcats = this.subcatsByCat.get(cat);
                if (subcats.size() > 0) {
                    ArrayList<String> sortedSubs = new ArrayList<String>();
                    sortedSubs.addAll(subcats);
                    Collections.sort(sortedSubs);
                    for (String subcat : sortedSubs) {
                        if (subcategory == null || subcat.toLowerCase().contains(subcategory.toLowerCase().trim())) {//subcat matches or no subcat sent
                            boolean subcatAdded = false;
                            doTopicSearching(cat+"-"+subcat, topicValue, search, tag, results, dupes, cat, subcat, catAdded, subcatAdded);
                        }//end subcat matches
                    }
                } else {//case of topics not in a subcategory
                    doTopicSearching(cat+"-nosubcat", topicValue, search, tag, results, dupes, cat, null, catAdded, false);
                }
            }//end matches category
        }
        return results;
    }
    private ArrayList<MapSource> mapSources = new ArrayList<MapSource>();
    private Comparator<MapSource> byTitle = new Comparator<MapSource>() {
		public int compare(MapSource o1, MapSource o2) {
			return o1.getTitle().toLowerCase().compareTo(o2.getTitle().toLowerCase());
		}
	};

    private void loadMapSources() {
    	if(MapServerFactory.getMapServers() != null){
			for(MapServer server : MapServerFactory.getMapServers()){
				if(server != null){
					for(MapSource source : server.getMapSources()){
						if(source != null){
							if (!layerParamMapSources.contains(source.getName())) {
								mapSources.add(source);
								boolean isFave = mapSourceFavorites.contains(source.getName());
								SearchResultRow row = new SearchResultRow(source, isFave);
								searchRowsByMapSource.put(source,row);
								searchRowsByMapSourceTitle.put(source.getTitle(),row);
								layerParamMapSources.add(source.getName());
							}
							
						}
					}
				}
			}
		}
		//alphebetize the sources
		Collections.sort(mapSources, byTitle);
    }
    private void doTopicSearching(String subcatPath, String topicValue, String[] search, int tag, 
            ArrayList<String> results, ArrayList<String> dupes, String cat, String subcat, boolean catAdded, boolean subcatAdded) {
        ArrayList<String> topics = this.topicsBySubcat.get(subcatPath);
        if (topics.size() > 0) {
            ArrayList<String> sortedTopics = new ArrayList<String>();
            sortedTopics.addAll(topics);
            for (String topic : sortedTopics) {
                if (topicValue == null || topic.toLowerCase().contains(topicValue.toLowerCase().trim())) {//topic matches or no topic sent
                    boolean topicAdded = false;
                    ArrayList<String> layerIds = this.idsByPath.get(subcatPath+"-"+topic);
                    for (String id : layerIds) {
                        ArrayList<LayerParameters> lps = this.uniqueRows.get(id);
                        for (LayerParameters lp : lps) {
                            boolean allFound = true;
                            boolean atLeastOneFound = false;
                            String suffix = "";
                            for (String val : search) {
                                val = val.trim();
                                String attr = null;
                                switch(tag) {
                                    case TAG_NAME:
                                        attr = lp.name;
                                        break;
                                    case TAG_DESC:
                                        attr = lp.description;
                                        suffix = " <description match>";
                                        break;
                                    case TAG_CITATION:
                                        attr = lp.citation;
                                        suffix = " <citation match>";
                                        break;    
                                    case TAG_LINKS:
                                        attr = lp.getLinks();
                                        suffix = " <links match>";
                                        break;
                                    default : 
                                        throw new IllegalArgumentException("Invalid tag in getSuggestionWithHierarchy");
                                }
                                if (!attr.toLowerCase().contains(val.toLowerCase())) {
                                    allFound = false;
                                    break;
                                } else if (val.length() > 0){
                                    atLeastOneFound = true;
                                }
                            }
                            if (allFound) {
                                if (!catAdded) {
                                    results.add(" category: "+cat);
                                    catAdded = true;
                                }
                                if (subcat != null) {
                                    if (!subcatAdded) {
                                        results.add("    subcategory: " + subcat);
                                        subcatAdded = true;
                                    }
                                }
                                if (!topicAdded) {
                                    results.add("        topic: "+topic);
                                    topicAdded = true;
                                }
                                if (!dupes.contains(lp.name)) {
                                    results.add("            name: "+lp.name + (atLeastOneFound ? suffix : ""));
                                    dupes.add(lp.name);
                                }
                            }
                        }
                    }
                }//end topic matches
            }
        }
        
    }
    public ArrayList<String> getPartialSuggestionTopic(String category, String subcategory, String search) {
        ArrayList<String> results = new ArrayList<String>();
        ArrayList<String> sorted = new ArrayList<String>();
        sorted.addAll(this.categories);
        Collections.sort(sorted);
        for(String cat : sorted) {
            if (category == null || cat.toLowerCase().contains(category.toLowerCase().trim())) {//matches category
                boolean catAdded = false;
                ArrayList<String> subcats = this.subcatsByCat.get(cat);
                if (subcats.size() > 0) {
                    ArrayList<String> sortedSubs = new ArrayList<String>();
                    sortedSubs.addAll(subcats);
                    Collections.sort(sortedSubs);
                    for (String subcat : sortedSubs) {
                        if (subcategory == null || subcat.toLowerCase().contains(subcategory.toLowerCase().trim())) {//subcat matches
                            boolean subcatAdded = false;
                            ArrayList<String> topics = this.topicsBySubcat.get(cat+"-"+subcat);
                            if (topics.size() > 0) {
                                ArrayList<String> sortedTopics = new ArrayList<String>();
                                sortedTopics.addAll(topics);
                                for (String topic : sortedTopics) {
                                    if (topic.toLowerCase().contains(search.toLowerCase().trim())) {//topic matches
                                        if (!catAdded) {
                                            results.add(" category: "+cat);
                                            catAdded = true;
                                        }
                                        if (!subcatAdded) {
                                            results.add("    subcategory: " + subcat);
                                            subcatAdded = true;
                                        }
                                        results.add("        topic: "+topic);
                                    }//end topic matches
                                }
                            } 
                        }//end subcat matches
                    }
                } else {
                    ArrayList<String> topics = this.topicsBySubcat.get(cat+"-nosubcat");
                    if (topics.size() > 0) {
                        ArrayList<String> sortedTopics = new ArrayList<String>();
                        sortedTopics.addAll(topics);
                        for (String topic : sortedTopics) {
                            if (topic.toLowerCase().contains(search.toLowerCase().trim())) {//topic matches
                                if (!catAdded) {
                                    results.add(" category: "+cat);
                                    catAdded = true;
                                }
                                results.add("        topic: "+topic);
                            }//end topic matches
                        }
                    }
                }
            }//end matches category
        }
        return results;
    }
    public ArrayList<String> getPartialSuggestionSubcat(String category, String search) {
        ArrayList<String> results = new ArrayList<String>();
        
        for (String cat : this.categories) {
            if (category == null || cat.toLowerCase().contains(category.toLowerCase())) {
                boolean catPrinted = false;
                ArrayList<String> subcats = this.subcatsByCat.get(cat);
                if (subcats.size() > 0) {
                    ArrayList<String> sortedSubs = new ArrayList<String>();
                    sortedSubs.addAll(subcats);
                    Collections.sort(sortedSubs);
                    for (String subcat : sortedSubs) {
                        if (subcat.toLowerCase().startsWith(search.toLowerCase())) {
                            if (!catPrinted) {
                                results.add(" category: "+cat);
                                catPrinted = true;
                            }
                            results.add("    subcategory: " + subcat);
                        }
                    }
                }
            }
        }
        return results;
    }
    
    public ArrayList<String> getPartialSuggestion(String[] split, int tag) {
        ArrayList<String> results = new ArrayList<String>();
        if (tag == TAG_NAME) {
            for (String name : layerNameIdMap.keySet()) {
                boolean found = false;
                boolean containsAll = true;
                for (String one : split) {
                    if (name.toLowerCase().contains(one.trim().toLowerCase())) {
                        found = true;
                    } else {
                        containsAll = false;
                        break;
                    }
                }
                if (found && containsAll) {
                    results.add(" name: "+name);
                }
            }
        } else {
            int count = 0;
            for (ArrayList<LayerParameters> lps : this.uniqueRows.values()) {
                if (count > 300) {
                    break;
                }
                LayerParameters lp =  lps.get(0);
                String value = null;
                String suffix = "";
                String name = lp.name;
                switch(tag) {
                    case TAG_DESC:
                        value = lp.description;
                        suffix = "<description match>";
                        break;
                    case TAG_CITATION:
                        value = lp.citation;
                        suffix = "<citation match>";
                        break;    
                    case TAG_LINKS:
                        value = lp.getLinks();
                        suffix = "<links match>";
                        break;    
                }
                boolean found = false;
                boolean containsAll = true;
                for (String one : split) {
                    if (value.toLowerCase().contains(one.trim().toLowerCase())) {
                        found = true;
                    } else {
                        containsAll = false;
                        break;
                    }
                }
                if (found && containsAll) {
                    results.add(" name: "+name+" "+suffix);
                    count++;
                }
            }
        }
        return results;
    }
    public ArrayList<String> getPartialSuggestionAll(String[] split) {
        ArrayList<String> results = new ArrayList<String>();

        int count = 0;
        for (String name : layerNameIdMap.keySet()) {
            if (count > 300) {
                break;
            }
            boolean found = false;
            boolean containsAll = true;
            for (String one : split) {
                if (name.toLowerCase().contains(one.trim().toLowerCase())) {
                    found = true;
                } else {
                    containsAll = false;
                    break;
                }
            }
            if (found && containsAll) {
                results.add(" name: "+name);
                count++;
            }
        }
        if (count < 300) {
            ArrayList<Integer> searchTags = new ArrayList<Integer>();
            searchTags.add(TAG_LINKS);
            searchTags.add(TAG_CITATION);
            searchTags.add(TAG_DESC);
            for (ArrayList<LayerParameters> lps : this.uniqueRows.values()) {
                if (count > 300) {
                    break;
                }
                LayerParameters lp =  lps.get(0);
                String value = null;
                String suffix = "";
                String name = lp.name;
                for (int tag : searchTags) {
                    switch(tag) {
                        case TAG_DESC:
                            value = lp.description;
                            suffix = "<description match>";
                            break;
                        case TAG_CITATION:
                            value = lp.citation;
                            suffix = "<citation match>";
                            break;    
                        case TAG_LINKS:
                            value = lp.getLinks();
                            suffix = "<links match>";
                            break;    
                    }
                    boolean found = false;
                    boolean containsAll = true;
                    for (String one : split) {
                        if (value.toLowerCase().contains(one.trim().toLowerCase())) {
                            found = true;
                        } else {
                            containsAll = false;
                            break;
                        }
                    }
                    if (found && containsAll) {
                        results.add(" name: "+name+" "+suffix);
                        count++;
                        break;//break out of loop through these tags for this LP
                    }
                }
                
            }
        }
        return results;
    }
    public ArrayList<String> getSuggestionSubcatsAndTopicsForCat(String cat) {
        ArrayList<String> results = new ArrayList<String>();
        if (cat.trim().length() > 0) {
            results.add(" category: "+cat);
            ArrayList<String> subcats = this.subcatsByCat.get(cat);
            if (subcats != null && subcats.size() > 0) {
                ArrayList<String> sortedSubs = new ArrayList<String>();
                sortedSubs.addAll(subcats);
                Collections.sort(sortedSubs);
                for (String subcat : sortedSubs) {
                    results.add("    subcategory: " + subcat);
                }
            }
            String catSubcat = cat+"-nosubcat";
            ArrayList<String> topics = topicsBySubcat.get(catSubcat);
            if (topics != null && topics.size() > 0) {
                for (String topic : topics) {
                    results.add("    topic:  "+topic);
                }
            }
        }
        return results;
    }
    
    public ArrayList<String> getPartialSuggestionCustom(String search, boolean loadDefaultList) {
        ArrayList<String> results = new ArrayList<String>();
        if (userLoggedIn()) {
            String path = "Custom-nosubcat-Custom";
            ArrayList<String> ids = this.idsByPath.get(path);
            ArrayList<LayerParameters> lps = new ArrayList<LayerParameters>();
            for (String id : ids) {
                lps.add(this.uniqueRows.get(id).get(0));
            }
            ArrayList<String> top  = new ArrayList<String>();
            ArrayList<String> bottom = new ArrayList<String>();
            for (LayerParameters lp : lps) {
                if (loadDefaultList || search == null || search.trim().length() == 0 || lp.name.toLowerCase().contains(search.toLowerCase().trim())) {
                	if (lp.getCustomMapOwnerFlag()) {
                		top.add(" "+lp.name);
                	} else {
                		bottom.add(" "+lp.name);
                	}
                }
            }
            Collections.sort(top);
            Collections.sort(bottom);
            results.addAll(top);
            results.addAll(bottom);
        }
        return results;
    }
    
    public ArrayList<String> getSuggestionFavorite() {
    	ArrayList<String> results = new ArrayList<String>();
    	if (userLoggedIn()) {
    		for (String fave : this.favorites) {
    			ArrayList<LayerParameters> lps = this.uniqueRows.get(fave);
    			if (lps != null) {
    				LayerParameters lp = lps.get(0);
    				if (lp != null) {
    					results.add(" favorite:  "+lp.name);
    				}
    				
    			}
    			
    		}
    	}
    	return results;
    }
    public ArrayList<String> getPartialSuggestionFavorite(String search) {
        ArrayList<String> results = new ArrayList<String>();
        if (userLoggedIn()) {
            for (String fave : this.favorites) {
            	ArrayList<LayerParameters> lps = this.uniqueRows.get(fave);
            	if (lps != null) {
            		LayerParameters lp = lps.get(0);
            		if (lp != null) {
            			String name = lp.name;
                        if (search.trim().length() == 0 || name.toLowerCase().contains(search.toLowerCase().trim())) {
                            results.add(" favorite:  "+name);
                        }
            		}
            	}
                
            }
        }
        return results;
    }
    
    private ArrayList<SearchResultRow> searchMapSources(String searchText) {
    	ArrayList<SearchResultRow> results = new ArrayList<SearchResultRow>();
    	int idx = searchText.indexOf(":")+1;
    	if (idx > 0) {
	    	searchText = searchText.substring(idx);
	    	searchText = searchText.trim().toLowerCase();
    	}
    	for (MapSource mapSource : mapSources) {
    		String title = mapSource.getTitle().toLowerCase();
    		String name = mapSource.getName().toLowerCase();
    		if (title.contains(searchText) || name.contains(searchText)) {
    			SearchResultRow row = searchRowsByMapSource.get(mapSource);
    			results.add(row);
    			row.buildRow();
    		}
    	}
    	
    	return results;
    }

    public ArrayList<SearchResultRow> searchLayers(String searchText) {
    	searchText = searchText.replace("favorite:", "name:");
        searchText = searchText.replace("custom map:","name:");
        ArrayList<SearchResultRow> resultSet = new ArrayList<SearchResultRow>();
        searchText = searchText.trim();
        if (searchText.length() == 0) {
            return this.buildInitialLayerList(true,false);//put the home layers back if they search for nothing
        }
        
        HashMap<String,ArrayList<String>> params = new HashMap<String,ArrayList<String>>();
        String temp = searchText;
        
        //parse out something that might look like:
        //name: mola color category: instrument topic: bob name: themis
        ArrayList<Integer> indices = new ArrayList<Integer>();//list of all indices of search categories
        //Loop through each search category/option and list the index of each in the search string
        String[] splitVals = searchText.split(" ",0);
        int start = 0;
        for (String val : splitVals) {
            val = val.trim();
            if (searchOptions.contains(val)) {
                start = searchText.indexOf(val,start);
                indices.add(start);
                start += val.length();
            }
        }
        Collections.sort(indices);//order the list of indices
        if (indices.size() > 0) {
            if (indices.get(0) != 0) {//bob name: themis
                //our first search text is not a tag
                String value = temp.substring(0,indices.get(0));
                if (value != null) {
                    ArrayList<String> vals = params.get("all");
                    if (vals == null) {
                        vals = new ArrayList<String>();
                        params.put("all", vals);
                    }
                    vals.add(value.trim());
                }
            }
            
            for(int i=0; i<indices.size(); i++) {
                String entry;
                if (i+1 == indices.size()) {
                    //last entry
                    entry = temp.substring(indices.get(i));
                } else {
                    entry = temp.substring(indices.get(i), indices.get(i+1));
                }
                String p = entry.substring(0,entry.indexOf(":"));
                String v = entry.substring(entry.indexOf(":")+1);
                p = p.trim();
                v = v.trim();
                if (v.length() > 0) {
                    ArrayList<String> vals = params.get(p);
                    if (vals == null) {
                        vals = new ArrayList<String>();
                        params.put(p, vals);
                    }
                    vals.add(v.trim());
                }
            }
        } else{
            //no tags, just search terms
            ArrayList<String> vals = params.get("all");
            if (vals == null) {
                vals = new ArrayList<String>();
                params.put("all", vals);
            }
            vals.add(temp);
        }
       
        Set<String> keySet = params.keySet();
        if (keySet == null || keySet.size() == 0) {
        	boolean customFlag = false;
        	if ("custom map:".equalsIgnoreCase(searchText)) {
        		customFlag = true;
        	}
            return this.buildInitialLayerList(true, customFlag);
        } 

        ArrayList<String> allFoundLayerIds = new ArrayList<String>();
        ArrayList<LayerParameters> exactMatches = new ArrayList<LayerParameters>();
        ArrayList<LayerParameters> nonExactMatches = new ArrayList<LayerParameters>();
        
        for (LayerParameters layer : searchParams) {//looping once through each layer parameter
            boolean mainFlag = true;
            boolean foundFlag = false;
            boolean topFlag = true;
            boolean nameMatch = true;
            for (String key : keySet) {//for each search tag, get the array list of entries
                boolean tagFlag = true;
                for (String val : params.get(key)) {//loop through the ArrayList
                    boolean entryFlag = true;
                    val = val.trim().toLowerCase();
                    String[] split = val.split(" ", 0);
                    for (String value : split) {
                        if (value == null || value.trim().length() == 0) {
                            continue;
                        }
                        value = value.trim();
                        switch(key) {
                            case "name":
                            	if (layer.name.equalsIgnoreCase(value)) {
                            		nameMatch = true;
                            	} else {
	                                if (layer.name.toLowerCase().indexOf(value) > -1) {
	                                    foundFlag = true;
	                                    topFlag = topFlag && true;
	                                } else {
	                                    entryFlag = false;
	                                }
                            	}
                                break;
                            case "category":
                                if (layer.category.toLowerCase().indexOf(value) > -1) {
                                    foundFlag = true;
                                    topFlag = false;
                                } else {
                                    entryFlag = false;
                                }
                                break;
                            case "subcategory":
                                if (layer.subcategory.toLowerCase().indexOf(value) > -1) {
                                    foundFlag = true;
                                    topFlag = false;
                                } else {
                                    entryFlag = false;
                                }
                                break;
                            case "topic":
                                if (layer.topic.toLowerCase().indexOf(value) > -1) {
                                    foundFlag = true;
                                    topFlag = false;
                                } else {
                                    entryFlag = false;
                                }
                                break;
                            case "desc":
                            case "description":
                                if (layer.description.toLowerCase().indexOf(value) > -1) {
                                    foundFlag = true;
                                    topFlag = false;
                                } else {
                                    entryFlag = false;
                                }
                                break;
                            case "citation":
                                if (layer.citation.toLowerCase().indexOf(value) > -1) {
                                    foundFlag = true;
                                    topFlag = false;
                                } else {
                                    entryFlag = false;
                                }
                                break;    
                            case "links":
                                if (layer.getLinks().toLowerCase().indexOf(value) > -1) {
                                    foundFlag = true;
                                    topFlag = false;
                                } else {
                                    entryFlag = false;
                                }
                                break;
                            case "instrument":
                                if (layer.category.toLowerCase().equals("instrument")) {
                                    if (layer.subcategory == null || layer.subcategory.trim().length() == 0) {
                                        if (layer.topic.toLowerCase().indexOf(value) > -1) {
                                            foundFlag = true;
                                            topFlag = false;
                                        }
                                    } else if (layer.subcategory.toLowerCase().indexOf(value) > -1) {
                                        foundFlag = true;
                                        topFlag = false;
                                    }
                                    
                                }
                                if (!foundFlag) {
                                    entryFlag = false;
                                }
                                break;
                            case "imagery":
                                if (layer.category.toLowerCase().equals("imagery")) {
                                    if (layer.subcategory == null || layer.subcategory.trim().length() == 0) {
                                        if (layer.topic.toLowerCase().indexOf(value) > -1) {
                                            foundFlag = true;
                                            topFlag = false;
                                        }
                                    } else if (layer.subcategory.toLowerCase().indexOf(value) > -1) {
                                        foundFlag = true;
                                        topFlag = false;
                                    }
                                    
                                }
                                if (!foundFlag) {
                                    entryFlag = false;
                                }
                                break;
                            case "custom map":
                                if (layer.category.toLowerCase().equals("custom")) {
                                	if (layer.name.toLowerCase().indexOf(value) > -1) {
	                                    foundFlag = true;
	                                    topFlag = true;
                                	}
                                } else {
                                    entryFlag = false;
                                }
                                break;
                            case "all":
                                if (layer.name.toLowerCase().indexOf(value) > -1) {
                                    foundFlag = true;
                                    topFlag = topFlag && true;
                                    nameMatch = nameMatch && true;
                                } else if (layer.category.toLowerCase().indexOf(value) > -1 ||
                                    layer.subcategory.toLowerCase().indexOf(value) > -1 ||
                                    layer.topic.toLowerCase().indexOf(value) > -1 ||
                                    layer.description.toLowerCase().indexOf(value) > -1 ||
                                    layer.citation.toLowerCase().indexOf(value) > -1 ||
                                    layer.getLinks().toLowerCase().indexOf(value) > -1) {
                                    foundFlag = true;
                                    topFlag = false;
                                    if (layer.name.toLowerCase().indexOf(value) > -1) {
                                    	nameMatch = nameMatch && true;
                                    } else if (layer.category.equalsIgnoreCase("instrument") && layer.subcategory.equalsIgnoreCase(value)) {
                                    	nameMatch = nameMatch && true;
                                    } else {
                                    	nameMatch = false;
                                    }
                                } else {
                                    entryFlag = false;
                                    nameMatch = false;
                                }
                                break;
                        }
                    }
                    tagFlag = tagFlag && entryFlag;
                }
                mainFlag = mainFlag && tagFlag;
            
            }
            if (mainFlag && foundFlag) {
            	if (!allFoundLayerIds.contains(layer.id)) {
	            	if (nameMatch) {
	            		exactMatches.add(layer);
	            	} else {
	            		nonExactMatches.add(layer);
	            	}
	            	allFoundLayerIds.add(layer.id);
            	}
            }
   
        }
        
        for (LayerParameters lp : exactMatches) { 
        	SearchResultRow row = buildRow(lp.id,true);
            if (row != null) {
            	resultSet.add(row);
            }
        }
        Collections.sort(resultSet);
        
        ArrayList<SearchResultRow> tempList = new ArrayList<SearchResultRow>();
        for (LayerParameters lp : nonExactMatches) { 
        	SearchResultRow row = buildRow(lp.id,true);
            if (row != null) {
            	tempList.add(row);
            }
        }
        Collections.sort(tempList);
        resultSet.addAll(tempList);
        
        ArrayList<SearchResultRow> maps = new ArrayList<SearchResultRow>();
        
        //search map source proof of concept
        for (String key : keySet) {//for each search tag, get the ArrayList of entries
            for (String val : params.get(key)) {//loop through the ArrayList
                val = val.trim().toLowerCase();
                String[] split = val.split(" ", 0);
                for (String value : split) {
                    if (value == null || value.trim().length() == 0) {
                        continue;
                    }
                    value = value.trim();
                    switch(key) {
                    case "all":
                    case "name":
                    	maps.addAll(searchMapSources(searchText));
                    }
                }
            }
        }
//        Collections.sort(maps);
//        resultSet.addAll(maps);
        //commented out until we decide how we want to handle map sources.
        return resultSet;
    }
    
}
