package edu.cwru.sepia.agent.planner;

import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Set;
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

	public List<GameState> createPlan() {
		List<GameState> plan = new ArrayList<>();
		plan.add(startState);
		for (Condition goalCondition : goalState.getConditions()) {
			GameState.isGold = goalCondition.getValue("type").getValue() == Condition.GOLD
					.getValue();
			List<GameState> goalPath = getPathToGoal(goalCondition,
					plan.get(plan.size() - 1));
			//plan.addAll(goalPath);
			System.out.println(goalPath.toString());
			plan = new ArrayList<>(goalPath);
		}
		return plan;
	}

	private List<GameState> getPathToGoal(Condition goalCondition,
			GameState currentState) {
//		List<GameState> openStates = new ArrayList<>();
//		// Find a goal state.
//		PriorityQueue<GameState> openStates = new PriorityQueue<>();
//		List<GameState> possibleStates = new ArrayList<>();
//		Set<GameState> expandedStates = new HashSet<>();
		GameState current = currentState;
//		GameState cheapestState;
//		openStates.add(currentState);
//		
//		 //run A* to find optimal plan
//		while (!openStates.isEmpty()) {
//			System.out.println("In a star loop");
//			System.out.println("Current state is: " + current.toString());
//			cheapestState = openStates.poll();
//			
//			if (cheapestState.isGoal(goalCondition)) {
//				return AstarPath(cheapestState);
//			}
//			
//			expandedStates.add(cheapestState);
//			possibleStates = cheapestState.generateChildren(availableActions);
//			//openStates.addAll(possibleStates);
//			
//			for (GameState state : possibleStates) {
//				if (!expandedStates.contains(state) &&
//					!openStates.contains(state)) {
//					state.setDepth(cheapestState.getDepth() + 1);
////					state.setCost(state.getDepth() + distanceBetweenstates(state, goal));
//					openStates.add(state);
//				}
//			}
//		}
//		
//		System.err.println("No available path.");
//		return null;

		 // Find a goal state.
		List<GameState> states = new ArrayList<>();
		do {
			states.addAll(current.generateChildren(availableActions));
			// A* is being performed here, since the State.compareTo() is comparing states based on their heuristic values
			Collections.sort(states);
			current = states.get(0);
			states.remove(current);
		} while (!current.isGoal(goalCondition) && !states.isEmpty());
		// Generate the list from the found state.
		List<GameState> path = new ArrayList<>();
		while (current.getParent() != null) {
			path.add(0, current);
			current = current.getParent();
		}
		
		return path;


	}
	
	 /**
	* Returns the A* path to the given end location
	* from the beginning location of the map.
	*
	* @param end - the location to get the A* path to
	* from the beginning location
	* @return the stack of States from the beginning of the
	* map (top of stack) to the end of the map (bottom of stack)
	*/
	public static Stack<GameState> AstarPath(GameState end) {
		Stack<GameState> astarPath = new Stack<>();
		GameState curr = null;
		
		//Do not add goal to path
		if (end != null) {
			curr = end.getParent();
		}
		
		//Build path from end to beginning but disregard starting node
		while (curr != null && curr.getParent() != null) {
			astarPath.push(curr);
			curr = curr.getParent();
		}
		
		return astarPath;
	}

	public static void printPlan(List<GameState> plan, PrintStream writer) {
		System.out.println("PRINTING PLAN");
		for (GameState state : plan) {
			writer.print("Step " + state.getDepth() + " - ");
			if (state == null) {
				System.out.println("State is null..exiting");
				System.exit(1);
			}
			writer.print("Action: " + state.getFromParent().getName() + " (");
			for (Value val : state.getFromParent().getConstants()) {
				writer.print(val.getConstantAsString());
				if (state.getFromParent().getConstants().indexOf(val) != state
						.getFromParent().getConstants().size() - 1) {
					writer.print(", ");
				}
			}
			writer.println(")");
		}
	}
}
