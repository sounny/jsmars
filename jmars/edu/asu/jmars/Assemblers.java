package edu.asu.jmars;

public class Assemblers
 {
	private static int current = 0;
	public static String getOne()
	 {
		return  list[Math.min(current++, list.length-1)];
	 }

	private static String[] list =
	{
		"tiny little martian hands",
		"highly evolved primates",
		"domesticated amoebas",
		"genetically engineered fruit flies",
		"a flock of pigeons",
		"skilled Jedi warriors",
		"a pair of circus ninjas",
		"three blind mice",
		"four mutant turtles",
		"five rabid fish",
		"Bill Gates",
		"Tori Amos",
		"Fatboy Slim",
		"Vogons",
		"a swarm of albino fleas",
		"rabid vampires",
		"a cadre of elephants",
		"a fun makefile",
		"Saadat Anwar",
		"Eric Engle",
		"Scott Dickenshied",
		"Dale Noss",
		"sleep-deprived programmers"
	};
 }
