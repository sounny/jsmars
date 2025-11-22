package edu.asu.jmars.layer.shape2.xb.data.service;

import java.util.List;

public interface IValidator<T> {
	
	public T validateIdentifier (T identifier);
	default public List<String> validateAsArray (T identifier) {
		List<String> emptylist = new java.util.ArrayList<>();
		return emptylist;
	}
}
