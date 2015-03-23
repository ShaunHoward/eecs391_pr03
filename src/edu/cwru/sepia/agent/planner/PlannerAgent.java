package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.io.*;
import java.util.*;

/**
 * Created by Devin on 3/15/15.
 */
public class PlannerAgent extends Agent {

	private static final int WOODSMALL1 = 200, GOLDSMALL1 = 200,
		WOODSMALL2 = 1000, GOLDSMALL2 = 1000, WOODLARGE1 = 1000,
		GOLDLARGE1 = 1000, WOODLARGE2 = 2000, GOLDLARGE2 = 3000;
	
	final int requiredWood;
	final int requiredGold;
	final boolean buildPeasants;
	
	private Stack<PlanAction> availableActions;
	final int scenario;

	// Your PEAgent implementation. This prevents you from having to parse the
	// text file representation of your plan.
	PEAgent peAgent;

	public PlannerAgent(int playernum, String[] params) {
		super(playernum);

		if (params.length < 3) {
			System.err
					.println("You must specify the required wood and gold amounts and whether peasants should be built");
		}

		requiredWood = Integer.parseInt(params[0]);
		requiredGold = Integer.parseInt(params[1]);
		buildPeasants = Boolean.parseBoolean(params[2]);
		scenario = generateScenario();
		availableActions = new Stack<>();

		System.out.println("required wood: " + requiredWood
				+ " required gold: " + requiredGold + " build Peasants: "
				+ buildPeasants);
	}

	public int generateScenario(){
		
		if (requiredWood == WOODSMALL1 && requiredGold == GOLDSMALL1){
			return 1;
		} else  if (requiredWood <= WOODSMALL2 && requiredGold <= GOLDSMALL2){
			return 2;
		} else  if (requiredWood <= WOODLARGE1 && requiredGold <= GOLDLARGE1){
			return 3;
		} else  if (requiredWood <= WOODLARGE2 && requiredGold <= GOLDLARGE2){
			return 4;
	    } else {
	    	System.err.println("A scenario this large is not fully supported in this implementation.");
	    	System.err.println("The program may not function correctly.");
	    	return 4;
	    }
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView,
			History.HistoryView historyView) {

		List<Integer> allUnitIds = stateView.getAllUnitIds();
		List<Integer> peasantIds = new ArrayList<Integer>();

		for (int i = 0; i < allUnitIds.size(); i++) {
			int id = allUnitIds.get(i);
			UnitView unit = stateView.getUnit(id);
			String unitTypeName = unit.getTemplateView().getName();
			if (unitTypeName.equals("Peasant")) {
				peasantIds.add(id);
			}
		}
		
		GameState initialState = new GameState(stateView,
				peasantIds.get(0), requiredGold, requiredWood, buildPeasants);
		Stack<GameState> plan = AstarSearch(initialState);

		if (plan == null) {
			System.err.println("No plan was found");
			System.exit(1);
			return null;
		}

		// write the plan to a text file
		//savePlan(stripsPlan);

		// Instantiates the PEAgent with the specified plan.
		peAgent = new PEAgent(playernum, plan);

		return peAgent.initialStep(stateView, historyView);
	}

	@Override
	public Map<Integer, Action> middleStep(State.StateView stateView,
			History.HistoryView historyView) {
		if (peAgent == null) {
			System.err.println("Planning failed. No PEAgent initialized.");
			return null;
		}

		return peAgent.middleStep(stateView, historyView);
	}

	@Override
	public void terminalStep(State.StateView stateView,
			History.HistoryView historyView) {

	}

	@Override
	public void savePlayerData(OutputStream outputStream) {

	}

	@Override
	public void loadPlayerData(InputStream inputStream) {

	}

	/**
	 * Perform an A* search of the game graph. This should return your plan as a
	 * stack of actions. This is essentially the same as your first assignment.
	 * The implementations should be very similar. The difference being that
	 * your nodes are now GameState objects not MapLocation objects.
	 *
	 * @param startState
	 *            The state which is being planned from
	 * @return The plan or null if no plan is found.
	 */
	private Stack<GameState> AstarSearch(GameState startState) {
		Stack<PlanAction> possibleActions = new Stack<>();
		possibleActions.addAll(PlanAction.getActions(scenario));
		availableActions = possibleActions;
		GameState goalState = GameState.getGoalState(requiredGold, requiredWood);
		List<GameState> plan = new Stack<>();
		plan.add(startState);
		
		//fulfill all goals
		for (Condition goalCondition : goalState.getState()) {
			//determine if the current goal is to harvest gold
			GameState.isGold = goalCondition.getValue("type").getValue() == Condition.GOLD
					.getValue();
			
			//use A* to find the path to fulfill this goal condition
			List<GameState> goalPath = getPathToGoal(goalCondition,
					plan.get(plan.size() - 1));
			
			System.out.println(goalPath.toString());
			
			plan = new ArrayList<>(goalPath);
		}
		
		//convert list to stack
		Stack<GameState> actions = new Stack<>();
		actions.addAll(plan);
		return actions;
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
			states.remove(0);
			System.out.println("Current state is: " + current.toString());
			System.out.println("Still in A star loop");
		} while (!current.isGoal(goalCondition) && !states.isEmpty());
		
		// Make the path from beginning to end
		List<GameState> path = new Stack<>();
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

	/**
	 * This has been provided for you. Each strips action is converted to a
	 * string with the toString method. This means each class implementing the
	 * StripsAction interface should override toString. Your strips actions
	 * should have a form matching your included Strips definition writeup. That
	 * is <action name>(<param1>, ...). So for instance the move action might
	 * have the form of Move(peasantID, X, Y) and when grounded and written to
	 * the file Move(1, 10, 15).
	 *
	 * @param stripsPlan
	 *            Stack of Strips Actions that are written to the text file.
	 */
	private void savePlan(Stack<PlanAction> stripsPlan) {
		if (stripsPlan == null) {
			System.err.println("Cannot save null plan");
			return;
		}

		File outputDir = new File("saves");
		outputDir.mkdirs();

		File outputFile = new File(outputDir, "plan.txt");

		PrintWriter outputWriter = null;
		try {
			outputFile.createNewFile();

			outputWriter = new PrintWriter(outputFile.getAbsolutePath());

			Stack<PlanAction> tempPlan = (Stack<PlanAction>) stripsPlan.clone();
			while (!tempPlan.isEmpty()) {
				outputWriter.println(tempPlan.pop().toString());
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (outputWriter != null)
				outputWriter.close();
		}
	}
}
