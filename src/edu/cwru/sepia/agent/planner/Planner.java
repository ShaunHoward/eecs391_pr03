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

	public List<GameState> createPlan() {
		List<GameState> plan = new ArrayList<>();
		plan.add(startState);
		for (Condition goalCondition : goalState.getState()) {
			GameState.isGold = goalCondition.getValue("type").getValue() == Condition.GOLD
					.getValue();
			List<GameState> goalPath = getPathToGoal(goalCondition,
					plan.get(plan.size() - 1));
			plan.addAll(goalPath);
			// plan = new ArrayList<>(goalPath);
		}
		return plan;
	}

	private List<GameState> getPathToGoal(Condition goalCondition,
			GameState currentState) {
		// Find a goal state.
		List<GameState> states = new ArrayList<>();
		GameState current = currentState;
		do {
			states.addAll(getNextStates(current));
			// A* is being performed here, since the State.compareTo() is
			// comparing states based on their heuristic values
			Collections.sort(states);
			current = states.get(0);
			states.remove(current);
			System.out.println("Still in A star loop");
		} while (!current.isGoal(goalCondition) && !states.isEmpty());
		// Generate the list from the found state.
		List<GameState> path = new ArrayList<>();
		while (current.getParent() != null) {
			path.add(0, current);
			current = current.getParent();
		}
		return path;
	}

	private List<GameState> getNextStates(GameState currentState) {
		List<PlanAction> possibleActions = generatePossibleActions(currentState);
		List<GameState> nextStates = new ArrayList<>();
		for (PlanAction action : possibleActions) {
			nextStates.add(action.apply(currentState));
		}
		return nextStates;
	}

	public List<PlanAction> generatePossibleActions(GameState state) {
		List<Value> variables = new ArrayList<>();
		for (Condition condition : state.getState()) {
			variables.addAll(condition.getVariables());
		}
		List<Value> units = new ArrayList<>();
		List<Value> positions = new ArrayList<>();
		List<Value> types = new ArrayList<>();
		outer: for (Value variable : variables) {
			if (variable == null || variable.getName().isEmpty()) {
				continue;
			}
			List<Value> addList = null;
			if (variable.getName().equalsIgnoreCase("first")
					|| variable.getName().equalsIgnoreCase("second")
					|| variable.getName().equalsIgnoreCase("third")) {
				addList = units;
			} else if (variable.getName().equalsIgnoreCase("pos")
					|| variable.getName().equalsIgnoreCase("to")
					|| variable.getName().equalsIgnoreCase("from")) {
				addList = positions;
			} else if (variable.getName().equalsIgnoreCase("type")) {
				addList = types;
			} else if (variable.getName().equalsIgnoreCase("amt")) {
				continue;
			}
			if (addList == null) {
				System.out.println("Unrecognized variable name!!! "
						+ variable.getName());
				continue;
			}
			for (Value var : addList) {
				if (variable.getName().equals(var.getName())
						&& variable.equals(var)) {
					continue outer;
				}
			}
			addList.add(variable);
		}
		// List<PlanAction> validActions = new ArrayList<>();
		// while(!stripsActions.isEmpty()) {
		// List<PlanAction> possibleActions =
		// stripsActions.pop().getPossibleActions(units, positions, types);
		// inner: for (PlanAction action : possibleActions) {
		// if (action.preconditionsMet(state)) {
		// for (PlanAction existingAction : validActions) {
		// if (existingAction.equals(action)) {
		// continue inner;
		// }
		// }
		// validActions.add(action);
		// }
		// }
		// }
		List<PlanAction> validActions = new ArrayList<>();
		for (PlanAction actionTemplate : availableActions) {
			List<PlanAction> possibleActions = actionTemplate
					.getPossibleActions(units, positions, types);
			inner: for (PlanAction action : possibleActions) {
				if (action.preconditionsMet(state)) {
					for (PlanAction existingAction : validActions) {
						if (existingAction.equals(action)) {
							continue inner;
						}
					}
					validActions.add(action);
				}
			}
		}
		return validActions;
	}

	public static void printPlan(List<GameState> plan, PrintStream writer) {
		System.out.println("PRINTING PLAN");
		for (GameState state : plan) {
			writer.print("Step " + state.getDepth() + " - ");
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
