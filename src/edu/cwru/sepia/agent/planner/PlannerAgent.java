package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
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

	private String fileName; // name of plan output file

	private boolean busy; // indicates if an action is currently in progress
	private int townHall; // town hall id

	private LinkedList<PlanState> plan; // list of planned actions

	private Stack<PlanAction> availableActions;
	private static ArrayList<PlanAction> actions;
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

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView,
			History.HistoryView historyView) {

		// generate initial state
		PlanState initial = new PlanState(0, 0);

		// identify units and create minimal data structures needed for planning
		for (int id : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(id);
			String typeName = unit.getTemplateView().getName();
			if (typeName.equals("TownHall"))
				townHall = id;
			if (typeName.equals("Peasant"))
				initial.peasants.add(new PlanPeasant());
		}

		// identify resources and create minimal data structures needed for
		// planning
		for (int id : stateView.getAllResourceIds()) {
			initial.resources.add(new PlanResource(stateView
					.getResourceNode(id), stateView.getUnit(townHall)));
		}

		// generate goal state
		PlanState goal = new PlanState(requiredGold, requiredWood);
		// add optimal number of peasants to the goal state
		for (int i = 0; i < getMaxPeasants(); i++)
			goal.peasants.add(new PlanPeasant());

		// pass initial and goal states to planner and get a plan
		plan = PlannerAgent.AstarSearch(initial, goal, fileName);
		// remove initial state since we are already here
		plan.removeFirst();
		peAgent = new PEAgent(playernum, plan, requiredGold, requiredWood,
				buildPeasants);
		System.out.println("Executing plan");
		// for(PlanState s: plan)
		// System.out.println(s.parentAction + " -> " + s);

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

	// generate a plan using A* to do a forward state space search
	public static LinkedList<PlanState> AstarSearch(PlanState initial,
			PlanState goal, String fileName) {
		System.out.println("Planner initialized for:\n" + "\tInitial: "
				+ initial + "\n" + "\tGoal: " + goal);

		registerActions(initial, goal.peasants.size());

		ArrayList<PlanState> open = new ArrayList<PlanState>();
		ArrayList<PlanState> closed = new ArrayList<PlanState>();

		HashMap<PlanState, PlanState> parents = new HashMap<PlanState, PlanState>();
		HashMap<PlanState, Integer> gScore = new HashMap<PlanState, Integer>(); // cost
																				// along
																				// best
																				// known
																				// path
		HashMap<PlanState, Integer> fScore = new HashMap<PlanState, Integer>(); // total
																				// estimated
																				// cost

		open.add(initial);
		gScore.put(initial, 0);
		fScore.put(initial, getHScore(initial, goal));

		while (open.size() > 0) {
			PlanState current = getMinVal(fScore, open);
//			current.parentAction.
//			current.parentAction.d
			// return the least cost path if the end has been reached
			if (goalTest(current, goal)) {
				System.out.println("Plan complete");
				LinkedList<PlanState> result = buildPath(parents, current);
				fileName = "/home/shaun/workspace/eecs391_pr03/textPlan/test.txt";
				writeFile(result, fileName);
				return result;
			}
			// move expanded position to the closed list
			open.remove(current);
			closed.add(current);
			// System.out.println("Expanding " + fScore.get(current) + ": " +
			// current);
			// evaluate next possible moves from current location
			for (PlanState neighbor : getNeighbors(current, goal)) {
				// ignore locations in the closed set
				if (closed.contains(neighbor))
					continue;
				int tempScore = gScore.get(current)
						+ neighbor.parentAction.getMakeSpan();
				// explore low cost paths
				if (!open.contains(neighbor)
						|| tempScore <= gScore.get(neighbor)) {
					// track the path
					parents.put(neighbor, current);
					gScore.put(neighbor, tempScore);
					// calculate heuristic cost
					fScore.put(neighbor,
							gScore.get(neighbor) + getHScore(neighbor, goal));
					// System.out.println("\t" + neighbor.parentAction + ": " +
					// gScore.get(neighbor) + " + " + getHScore(neighbor, goal)
					// + " = " + fScore.get(neighbor));
					if (!open.contains(neighbor))
						open.add(neighbor);
				}
			}
		}
		System.out.print("No available path");
		return null;
	}

	private static void registerActions(PlanState s, int maxPeasants) {
		actions = new ArrayList<PlanAction>();
		// move to, gather, and return from each resource node
		for (PlanResource resource : s.resources) {
			int resId = resource.getId();
			for (int i = 1; i <= maxPeasants; i++) {
				actions.add(new MoveAction(i, s, null, resId, false));
				actions.add(new GatherAction(i, resId, resource.getX(),
						resource.getY()));
				actions.add(new MoveAction(i, s, resId, null, false));
			}
		}
		// deposit cargo
		for (int i = 1; i <= maxPeasants; i++)
			actions.add(new DepositAction(i));
		// build peasant
		if (maxPeasants > 1)
			actions.add(new BuildPeasantAction());
	}

	private static boolean goalTest(PlanState s, PlanState goal) {
		return s.gold == goal.gold && s.wood == goal.wood;
	}

	/*
	 * Get the heuristic cost of getting from state a to state b
	 */
	private static int getHScore(PlanState a, PlanState b) {
		int score = 0;
		// prioritize making peasants
		score += (b.peasants.size() - a.peasants.size()) * 100;
		// estimate cycles needed to gather resources
		int cyclesForGold = Math.max(b.gold - a.gold, 0)
				/ (a.peasants.size() * 100);
		int cyclesForWood = Math.max(b.wood - a.wood, 0)
				/ (a.peasants.size() * 100);
		// assume every resource is 30 steps away
		score += (cyclesForGold + cyclesForWood) * 60;
		return score;
	}

	// return item in list mapped to the lowest score
	private static PlanState getMinVal(HashMap<PlanState, Integer> score,
			List<PlanState> list) {
		PlanState result = null;
		int minScore = Integer.MAX_VALUE;
		for (PlanState state : list) {
			int stateScore = score.get(state);
			if (stateScore < minScore) {
				result = state;
				minScore = stateScore;
			}
		}
		return result;
	}

	// get valid moves from a given state
	private static List<PlanState> getNeighbors(PlanState s, PlanState goal) {
		ArrayList<PlanState> result = new ArrayList<PlanState>();
		for (PlanAction a : actions)
			if (a.isAllowedFor(s, goal))
				result.add(a.applyTo(s));
		return result;
	}

	// extract shortest path from a given point to the root node by tracing the
	// parents
	private static LinkedList<PlanState> buildPath(
			HashMap<PlanState, PlanState> parents, PlanState s) {
		LinkedList<PlanState> result = parents.containsKey(s) ? buildPath(
				parents, parents.get(s)) : new LinkedList<PlanState>();
		result.add(s);
		return result;
	}

	// write plan to file
	private static void writeFile(LinkedList<PlanState> plan, String fileName) {
		System.out.println("Writing plan to " + fileName);
		try {
			PrintWriter out = new PrintWriter(fileName);
			int i = 0;
			for (PlanState s : plan)
				out.println(i++ + ": " + s.parentAction);
			out.close();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int generateScenario() {

		if (requiredWood == WOODSMALL1 && requiredGold == GOLDSMALL1) {
			return 1;
		} else if (requiredWood <= WOODSMALL2 && requiredGold <= GOLDSMALL2) {
			return 2;
		} else if (requiredWood <= WOODLARGE1 && requiredGold <= GOLDLARGE1) {
			return 3;
		} else if (requiredWood <= WOODLARGE2 && requiredGold <= GOLDLARGE2) {
			return 4;
		} else {
			System.err
					.println("A scenario this large is not fully supported in this implementation.");
			System.err.println("The program may not function correctly.");
			return 4;
		}
	}

	//
	// @Override
	// public Map<Integer, Action> initialStep(State.StateView newState,
	// History.HistoryView stateHistory) {
	// // generate initial state
	// PlanState initial = new PlanState(0, 0);
	//
	// // identify units and create minimal data structures needed for planning
	// for(int id: newState.getUnitIds(playernum)) {
	// Unit.UnitView unit = newState.getUnit(id);
	// String typeName = unit.getTemplateView().getName();
	// if(typeName.equals("TownHall")) townHall = id;
	// if(typeName.equals("Peasant")) initial.peasants.add(new PlanPeasant());
	// }
	//
	// // identify resources and create minimal data structures needed for
	// planning
	// for(int id: newState.getAllResourceIds()) {
	// initial.resources.add(new PlanResource(newState.getResourceNode(id),
	// newState.getUnit(townHall)));
	// }
	//
	// // generate goal state
	// PlanState goal = new PlanState(targetGold, requiredGold);
	// // add optimal number of peasants to the goal state
	// for(int i = 0; i < getMaxPeasants(); i++)
	// goal.peasants.add(new PlanPeasant());
	//
	// // pass initial and goal states to planner and get a plan
	// plan = PlannerAgent.AstarSearch(initial, goal, fileName);
	// // remove initial state since we are already here
	// plan.removeFirst();
	// System.out.println("Executing plan");
	// //for(PlanState s: plan)
	// // System.out.println(s.parentAction + " -> " + s);
	// return middleStep(newState, stateHistory);
	// }

	/*
	 * Assuming each peasant takes one cycle of 4 actions
	 * (move-gather-move-deposit) to collect 100 of a resource, and building a
	 * new peasant takes 1 action, amount of resources gathered over x cycles
	 * ... 1 peasant: gather at 100x rate 100x 2 peasants: 4 cycles at 100x
	 * rate, then gather at 200x rate 200(x - 4.25) 3 peasants: 4 cycles at 100x
	 * rate, 2 cycles at 200x rate, then gather at 300x rate 300(x - 6.5) 100x
	 * and 200(x - 4.25) intersect at y = 850, so 1 peasant is optimal for <=
	 * 800 resources 200(x - 4.25) and 300(x - 6.5) intersect at y = 1350, so 2
	 * peasants are optimal for <= 1200 resources These values do not change
	 * between the given scenarios so they can be precomputed.
	 */
	private int getMaxPeasants() {
		if (!buildPeasants || (requiredGold + requiredWood) <= 800)
			return 1;
		if ((requiredGold + requiredWood) <= 1200)
			return 2;
		return 3;
	}

	// /*
	// @TODO: Fix translation between plan steps and game state actions
	// Pretty close but not working fully
	// */
	// @Override
	// public Map<Integer, Action> middleStep(State.StateView newState,
	// History.HistoryView stateHistory) {
	// Map<Integer, Action> actions = new HashMap<Integer, Action>();
	// List<Integer> peasants = new ArrayList<Integer>();
	// PlanState nextState = plan.peek();
	// PlanAction pAction = nextState.parentAction;
	//
	// for(int id: newState.getUnitIds(playernum)) {
	// Unit.UnitView unit = newState.getUnit(id);
	// String typeName = unit.getTemplateView().getName();
	// if(typeName.equals("Peasant")) peasants.add(id);
	// }
	//
	// if(pAction instanceof MoveAction) {
	// MoveAction mAction = (MoveAction) pAction;
	// Unit.UnitView townHallUnit = newState.getUnit(townHall);
	// PlanResource resource = nextState.getResourceWithId(mAction.getOriginId()
	// == null ?
	// mAction.getDestId() :
	// mAction.getOriginId());
	// boolean done = false;
	// boolean toTownHall = mAction.getDestId() == null;
	// int i = 0, j = 0;
	// int originX, originY, destX, destY;
	// if(toTownHall) {
	// originX = resource.getX();
	// originY = resource.getY();
	// destX = townHallUnit.getXPosition();
	// destY = townHallUnit.getYPosition();
	// } else {
	// originX = townHallUnit.getXPosition();
	// originY = townHallUnit.getYPosition();
	// destX = resource.getX();
	// destY = resource.getY();
	// }
	// // get the number of peasants that should be at the destination
	// for(PlanPeasant peasant: nextState.peasants) {
	// if(toTownHall && peasant.getNextTo() == null) i++;
	// if(!toTownHall && peasant.getNextTo() != null) i++;
	// }
	// // check to see if the right number of peasants are there
	// for(int id: peasants) {
	// Unit.UnitView peasant = newState.getUnit(id);
	// if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(), destX,
	// destY) &&
	// ++j == i) done = true;
	// }
	// if(done) {
	// plan.removeFirst();
	// busy = false;
	// } else if(!busy) {
	// busy = true;
	// int k = 0;
	// // order each peasant to move to the destination
	// for(int id: peasants) {
	// Unit.UnitView peasant = newState.getUnit(id);
	// if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
	// originX, originY) && k++ < mAction.getK())
	// actions.put(id, Action.createCompoundMove(id, destX, destY));
	// }
	// }
	// }
	//
	// if(pAction instanceof GatherAction) {
	// GatherAction gAction = (GatherAction) pAction;
	// boolean done = false;
	// int i = 0;
	// // check if each peasant at the target is carrying cargo
	// for(int id: peasants) {
	// Unit.UnitView peasant = newState.getUnit(id);
	// if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
	// gAction.getX(), gAction.getY()) &&
	// peasant.getCargoAmount() > 0 && ++i == gAction.getK()) done = true;
	// }
	// if(done) {
	// plan.removeFirst();
	// busy = false;
	// } else if(!busy){
	// busy = true;
	// int j = 0;
	// // order peasants to gather the target resource
	// for(int id: peasants) {
	// Unit.UnitView peasant = newState.getUnit(id);
	// if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
	// gAction.getX(), gAction.getY()) &&
	// j++ < gAction.getK())
	// actions.put(id, Action.createCompoundGather(id,
	// newState.resourceAt(gAction.getX(), gAction.getY())));
	// }
	// }
	// }
	//
	// if(pAction instanceof DepositAction) {
	// Unit.UnitView townHallUnit = newState.getUnit(townHall);
	// // check if the correct amount of gold/wood has been gathered
	// if(newState.getResourceAmount(playernum, ResourceType.GOLD) ==
	// nextState.gold &&
	// newState.getResourceAmount(playernum, ResourceType.WOOD) ==
	// nextState.wood) {
	// plan.removeFirst();
	// busy = false;
	// } else if(!busy) {
	// busy = true;
	// int i = 0;
	// // order peasants at the town hall to deposit resources
	// for(int id: peasants) {
	// Unit.UnitView peasant = newState.getUnit(id);
	// if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
	// townHallUnit.getXPosition(), townHallUnit.getYPosition()) &&
	// peasant.getCargoAmount() > 0 && i++ < ((DepositAction) pAction).getK())
	// actions.put(id, Action.createCompoundDeposit(id, townHall));
	// }
	// }
	// }
	//
	// if(pAction instanceof BuildPeasantAction) {
	// // check if the correct number of peasants are present
	// if(peasants.size() == nextState.peasants.size()) {
	// plan.removeFirst();
	// busy = false;
	// } else if(!busy) {
	// busy = true;
	// // build a peasant
	// actions.put(townHall, Action.createCompoundProduction(townHall,
	// newState.getTemplate(playernum, "Peasant").getID()));
	// }
	// }
	// return actions;
	// }

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

	// /**
	// * Perform an A* search of the game graph. This should return your plan as
	// a
	// * stack of actions. This is essentially the same as your first
	// assignment.
	// * The implementations should be very similar. The difference being that
	// * your nodes are now GameState objects not MapLocation objects.
	// *
	// * @param startState
	// * The state which is being planned from
	// * @return The plan or null if no plan is found.
	// */
	// private Stack<GameState> AstarSearch(GameState startState) {
	// Stack<PlanAction> possibleActions = new Stack<>();
	// possibleActions.addAll(PlanAction.getActions(scenario));
	// availableActions = possibleActions;
	// GameState goalState = GameState.getGoalState(requiredGold, requiredWood);
	// List<GameState> plan = new Stack<>();
	// plan.add(startState);
	//
	// //fulfill all goals
	// for (Condition goalCondition : goalState.getState()) {
	// //determine if the current goal is to harvest gold
	// GameState.isGold = goalCondition.getValue("type").getValue() ==
	// Condition.GOLD
	// .getValue();
	//
	// //use A* to find the path to fulfill this goal condition
	// List<GameState> goalPath = getPathToGoal(goalCondition,
	// plan.get(plan.size() - 1));
	//
	// System.out.println(goalPath.toString());
	//
	// plan = new ArrayList<>(goalPath);
	// }
	//
	// //convert list to stack
	// Stack<GameState> actions = new Stack<>();
	// actions.addAll(plan);
	// return actions;
	// }

	// private List<GameState> getPathToGoal(Condition goalCondition,
	// GameState currentState) {
	// // Find a goal state.
	// List<GameState> states = new ArrayList<>();
	// GameState current = currentState;
	// do {
	// states.addAll(getNextStates(current));
	// // A* is being performed here, since the State.compareTo() is
	// // comparing states based on their heuristic values
	// Collections.sort(states);
	// current = states.get(0);
	// states.remove(0);
	// System.out.println("Current state is: " + current.toString());
	// System.out.println("Still in A star loop");
	// } while (!current.isGoal(goalCondition) && !states.isEmpty());
	//
	// // Make the path from beginning to end
	// List<GameState> path = new Stack<>();
	// while (current.getParent() != null) {
	// path.add(0, current);
	// current = current.getParent();
	// }
	// return path;
	// }
	//
	// private List<GameState> getNextStates(GameState currentState) {
	// List<PlanAction> possibleActions = generatePossibleActions(currentState);
	// List<GameState> nextStates = new ArrayList<>();
	// for (PlanAction action : possibleActions) {
	// nextStates.add(action.apply(currentState));
	// }
	// return nextStates;
	// }
	//
	// public List<PlanAction> generatePossibleActions(GameState state) {
	// List<Value> variables = new ArrayList<>();
	// for (Condition condition : state.getState()) {
	// variables.addAll(condition.getVariables());
	// }
	// List<Value> units = new ArrayList<>();
	// List<Value> positions = new ArrayList<>();
	// List<Value> types = new ArrayList<>();
	// outer: for (Value variable : variables) {
	// if (variable == null || variable.getName().isEmpty()) {
	// continue;
	// }
	// List<Value> addList = null;
	// if (variable.getName().equalsIgnoreCase("first")
	// || variable.getName().equalsIgnoreCase("second")
	// || variable.getName().equalsIgnoreCase("third")) {
	// addList = units;
	// } else if (variable.getName().equalsIgnoreCase("pos")
	// || variable.getName().equalsIgnoreCase("to")
	// || variable.getName().equalsIgnoreCase("from")) {
	// addList = positions;
	// } else if (variable.getName().equalsIgnoreCase("type")) {
	// addList = types;
	// } else if (variable.getName().equalsIgnoreCase("amt")) {
	// continue;
	// }
	// if (addList == null) {
	// System.out.println("Unrecognized variable name!!! "
	// + variable.getName());
	// continue;
	// }
	// for (Value var : addList) {
	// if (variable.getName().equals(var.getName())
	// && variable.equals(var)) {
	// continue outer;
	// }
	// }
	// addList.add(variable);
	// }
	// // List<PlanAction> validActions = new ArrayList<>();
	// // while(!stripsActions.isEmpty()) {
	// // List<PlanAction> possibleActions =
	// // stripsActions.pop().getPossibleActions(units, positions, types);
	// // inner: for (PlanAction action : possibleActions) {
	// // if (action.preconditionsMet(state)) {
	// // for (PlanAction existingAction : validActions) {
	// // if (existingAction.equals(action)) {
	// // continue inner;
	// // }
	// // }
	// // validActions.add(action);
	// // }
	// // }
	// // }
	// List<PlanAction> validActions = new ArrayList<>();
	// for (PlanAction actionTemplate : availableActions) {
	// List<PlanAction> possibleActions = actionTemplate
	// .getPossibleActions(units, positions, types);
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
	// return validActions;
	// // }
	//
	// public static void printPlan(List<GameState> plan, PrintStream writer) {
	// System.out.println("PRINTING PLAN");
	// for (GameState state : plan) {
	// writer.print("Step " + state.getDepth() + " - ");
	// writer.print("Action: " + state.getFromParent().getName() + " (");
	// for (Value val : state.getFromParent().getConstants()) {
	// writer.print(val.getConstantAsString());
	// if (state.getFromParent().getConstants().indexOf(val) != state
	// .getFromParent().getConstants().size() - 1) {
	// writer.print(", ");
	// }
	// }
	// writer.println(")");
	// }
	// }

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
