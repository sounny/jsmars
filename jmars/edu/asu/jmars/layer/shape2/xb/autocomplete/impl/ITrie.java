package edu.asu.jmars.layer.shape2.xb.autocomplete.impl;

import java.util.List;

public interface ITrie {
    void insert(String word);
    void remove(String word);
    boolean search(String word);
    List<String> startsWith(String word);
    boolean isLowercase();
    boolean isCaseSensitive();
    void print();
}
