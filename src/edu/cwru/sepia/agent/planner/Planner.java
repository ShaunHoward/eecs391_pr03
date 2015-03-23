package edu.cwru.sepia.agent.planner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Stack;

import edu.cwru.sepia.agent.planner.actions.StripsAction;

public class Planner {
	private List<PlanAction> availableActions;
	private Stack<PlanAction> stripsActions;
	private GameState startState;
	private GameState goalState;

	public Planner(List<PlanAction> availableActions, GameState startState,
			GameState goalState) {
		this.availableActions = availableActions;
		Stack<PlanAction> actions = new Stack<>();
		actions.addAll(availableActions);
		this.stripsActions = actions;
		this.startState = startState;
		this.goalState = goalState;
	}

	public Planner(Stack<PlanAction> stripsPlan, GameState initialState,
			GameState goalState2) {
		this.availableActions = new ArrayList<>(stripsPlan);
		this.stripsActions = stripsPlan;
		startState = initialState;
		goalState = goalState2;
	}


}
