package edu.cwru.sepia.agent.planner;

import java.util.List;

//right now, this doesn't do anything
public class PlanTester {
	public static void main(String[] args) {
		List<PlanAction> actions;
		GameState startState, goalState;
		List<GameState> plan;
		int peasantId = 0;
		int requiredGold = 200;
		int requiredWood = 200;
		boolean buildPeasants = false;
		// Test single-peasant scenarios.
		// actions = PlanAction.getActions(1);
		// startState = State.getStartState(1);
		// goalState = State.getGoalState(1);
		// planner = new Planner(actions, startState, goalState);
		// plan = planner.createPlan();
		// Planner.printPlan(plan, System.out);
		
		//Test single peasant scenario
		System.out.println("beginning plan");
		actions = PlanAction.getActions(1);
		startState = GameState.generateInitialState(peasantId, requiredGold,
				requiredWood, buildPeasants);
		System.out.println("got initial state");
		goalState = GameState.getGoalState(requiredGold, requiredWood);
		System.out.println("got goal state");
		System.out.println("got a plan");
		System.out.println("reached the end");
	}
}