package edu.asu.jmars.tool.strategy;

public interface ToolStrategy {
	
	void preMode(int newmode, int oldmode);
	void doMode(int newmode, int oldmode);
	void postMode(int newmode, int oldmode);
}
