package edu.asu.jmars.layer.util.features.hsql;

public final class SimplePassthruConverter implements HsqlColumnConverter {
	private Class<?> clazz;
	
	public SimplePassthruConverter(Class<?> clazz){
		this.clazz = clazz;
	}
	
	public Object hsqlToJava(Object in) {
		if (in == null)
			return null;
		
		if (!clazz.isInstance(in))
			throw new IllegalArgumentException("Unexpected type "+in.getClass().getCanonicalName()+" in input. Expected type "+clazz.getName());
		
		return in;
	}

	public Object javaToHsql(Object in) {
		if (in == null)
			return null;
		
		if (!clazz.isInstance(in))
			throw new IllegalArgumentException("Unexpected type "+in.getClass().getCanonicalName()+" in input. Expected type "+clazz.getName());
		
		return in;
	}

}
