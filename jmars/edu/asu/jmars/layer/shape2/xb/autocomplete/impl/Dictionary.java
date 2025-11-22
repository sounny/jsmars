package edu.asu.jmars.layer.shape2.xb.autocomplete.impl;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Deque;

public class Dictionary implements ITrie {
    private final TrieNode root;

 	public Dictionary() {
        root = new TrieNode();
     }

    @Override
    public void insert(String word) {
        TrieNode current = root;
        for(char c : word.toCharArray()) {
            current = current.getChildren().computeIfAbsent(c, e->new TrieNode());
        }
        current.setEndOfWord(true);
    }

    @Override
    public boolean search(String word) {
        TrieNode current = root;
        for(char c : word.toCharArray()){
            TrieNode node = current.getChildren().get(c);
            if(node == null) return false;
            current = node;
        }
        return current.isEndOfWord();
    }

    private List<String> traverse(TrieNode node, String word, List<String> output){
        if(node.isEndOfWord()) output.add(word);
        node.getChildren().forEach((k,v)->traverse(v, word+k, output));
        return output;
    }

    @Override
    public List<String> startsWith(String word) {
        StringBuilder result = new StringBuilder();
        TrieNode current = root;
        for(char c : word.toCharArray()){
            TrieNode node = current.getChildren().get(c);
            if(node == null) break;
            result.append(c);
            current = node;
        }
        return traverse(current, result.toString(), new ArrayList<>());
    }
    
	@Override
	public void remove(String word) {
		TrieNode current = root;
		Deque<TrieNode> stack = new ArrayDeque<>();
		for (char c : word.toCharArray()) {
			TrieNode node = current.getChildren().get(c);
			if (node == null) {
				return;
			}
			stack.push(current);
			current = node;
		}
		if (!current.isEndOfWord()) {
			return;
		}
		current.setEndOfWord(false);
		if (!current.getChildren().isEmpty()) {
			return;
		}
		while (!stack.isEmpty()) {
			TrieNode parent = stack.pop();
			char lastChar = word.charAt(stack.size());
			TrieNode child = parent.getChildren().get(lastChar);
			if (!child.isEndOfWord() && child.getChildren().isEmpty()) {
				parent.getChildren().remove(lastChar);
			} else {
				return;
			}
		}
	}

	@Override
	public boolean isLowercase() {
		return true;
	}

	@Override
	public boolean isCaseSensitive() {
		return false;
	}	
	
	public void print() {
        List<String> words = traverse(root, "", new ArrayList<>());      
    }	

}   


