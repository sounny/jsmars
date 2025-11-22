package edu.asu.jmars.layer.util.features.hsql;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.Set;

import edu.asu.jmars.layer.util.features.Feature;
import edu.asu.jmars.layer.util.features.FeatureCollection;
import edu.asu.jmars.layer.util.features.FeatureProvider;
import edu.asu.jmars.layer.util.features.Field;
import edu.asu.jmars.layer.util.features.SingleFeatureCollection;
import edu.asu.jmars.util.DebugLog;

/**
 * HsqlFeatureCollections table
 * - id: long
 * - featureCollectionTableName: string
 * 
 * HsqlTypeMappingExceptions memory-based hash-map
 * - javaTypeName: enum-string
 * - sqlTypeName: string
 * - converterClass: java.lang.Class
 * 
 * HsqlColumnMapping table
 * - featureCollectionId: long (FK: HsqlFeatureCollections.id)
 * - colName: string
 * - colJavaTypeName: enum-string
 * 
 * <HsqlFeatureCollectionTable> table
 * - poly: polygon
 * - :
 * 
 */

public class HsqlFeatureProvider implements FeatureProvider {
	private static final DebugLog log = DebugLog.instance();
	
	private String dbUrl;
	private String user;
	private String pass;
	
	public HsqlFeatureProvider(String dbUrl, String user, String pass){
		this.dbUrl = dbUrl;
		this.user = user;
		this.pass = pass;
	}
	
	protected void finalize() throws Throwable {
		if (dbUrl != null && dbUrl.contains("hsqldb")){
			try {
				Connection c = DriverManager.getConnection(dbUrl, user, pass);
				c.createStatement().execute("shutdown");
				c.close();
			}
			catch(SQLException ex){
				log.println(ex);
			}
		}
		super.finalize();
	}

	public String getDescription() {
		return "HSQL Feature Provider";
	}

	public File[] getExistingSaveToFiles(FeatureCollection fc, String baseName) {
		return new File[0];
	}

	public String getExtension() {
		return null;
	}

	public boolean isFileBased() {
		return false;
	}

	public boolean isRepresentable(FeatureCollection fc) {
		return true;
	}
	
	public Set<String> getExistingCollections(){
		try {
			Connection c = DriverManager.getConnection(dbUrl, user, pass);
			Set<String> collectionNames = HsqlUtil.getCollectionNames(c);
			c.close();
			return Collections.unmodifiableSet(collectionNames);
		}
		catch(SQLException ex){
			throw new RuntimeException("Unable to retrieve existing collection names.", ex);
		}
	}
	
	public FeatureCollection load(String fcName){
		return load(fcName, null);
	}
	
	public FeatureCollection load(String fcName, HsqlConstraint filter) {
		FeatureCollection fc = new SingleFeatureCollection();
		for (Iterator<Feature> it = loadIterator(fcName, filter); it.hasNext(); ) {
			fc.addFeature(it.next());
		}
		return fc;
	}
	
	public Iterator<Feature> loadIterator(String fcName, HsqlConstraint filter) {
		try {
			Connection c = DriverManager.getConnection(dbUrl, user, pass);
			return HsqlUtil.getFeatures(c, fcName, filter);
		} catch(SQLException ex){
			throw new RuntimeException("Unable to load collection "+fcName+".", ex);
		}
	}
	
	public int save(FeatureCollection fc, String fcName) {
		Connection c = null;
		PreparedStatement s = null;
		try {
			c = DriverManager.getConnection(dbUrl, user, pass);
			Set<Field> schema = new LinkedHashSet<Field>(fc.getSchema());
			if (!HsqlUtil.collectionExists(c, fcName)){
				HsqlUtil.createCollection(c, fcName, schema);
			}
			else {
				HsqlUtil.updateCollectionStructure(c, fcName, schema);
				HsqlUtil.truncateTable(c, fcName);
			}
			
			HsqlUtil.addFeatures(c, fcName, (Collection<Feature>)fc.getFeatures(),
					new LinkedHashSet<Field>(fc.getSchema()));
			c.close();
		}
		catch(SQLException ex){
			throw new RuntimeException("Unable to save collection "+fcName+".", ex);
		}
		
		return fc.getFeatureCount();
	}

    @Override
    public boolean setAsDefaultFeatureCollection() {
        return false;
    }
}
