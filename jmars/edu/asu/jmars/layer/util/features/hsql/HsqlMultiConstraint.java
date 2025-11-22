package edu.asu.jmars.layer.util.features.hsql;

import java.util.Iterator;
import java.util.List;
import java.util.ArrayList;

public class HsqlMultiConstraint implements Cloneable, HsqlConstraint, Iterable<HsqlConstraint> {
	List<HsqlConstraint> constraints = new ArrayList<HsqlConstraint>();
	private boolean conjunctive = true;
	
	public HsqlMultiConstraint(){
		this(true);
	}
	
	public HsqlMultiConstraint(boolean conjunctive){
		this.conjunctive = conjunctive;
	}
	
	public void add(HsqlConstraint c){
		constraints.add(c);
	}
	
	public void remove(HsqlConstraint c){
		constraints.remove(c);
	}
	
	public boolean isConjunctive(){
		return conjunctive;
	}
	
	public Iterator<HsqlConstraint> iterator(){
		return constraints.iterator();
	}
	
	public boolean isEmpty(){
		return constraints.isEmpty();
	}

	public String getPreparedSqlSnippet() {
		StringBuffer sbuf = new StringBuffer();
		for(HsqlConstraint c: constraints){
			if (sbuf.length() > 0)
				sbuf.append(conjunctive?" AND ":" OR ");
			
			sbuf.append(c.getPreparedSqlSnippet());
		}
		return sbuf.length() > 0? "("+sbuf.toString()+")": sbuf.toString();
	}
	
	public List<Object> getPreparedSqlParams(HsqlColumnConverterFactory converterFactory) {
		List<Object> paramObjs = new ArrayList<Object>();
		
		for(HsqlConstraint c: constraints)
			paramObjs.addAll(c.getPreparedSqlParams(converterFactory));
		
		return paramObjs;
	}

	public HsqlMultiConstraint clone() throws CloneNotSupportedException {
		HsqlMultiConstraint c = (HsqlMultiConstraint)super.clone();
		c.constraints = new ArrayList<HsqlConstraint>(c.constraints);
		return c;
	}
	
	public String toString(){
		StringBuffer sbuf = new StringBuffer();
		for(HsqlConstraint c: constraints){
			if (sbuf.length() > 0)
				sbuf.append(conjunctive?" AND ":" OR ");
			sbuf.append(c.toString());
		}
		
		return sbuf.length() > 0? "("+sbuf.toString()+")": sbuf.toString();
	}
}
