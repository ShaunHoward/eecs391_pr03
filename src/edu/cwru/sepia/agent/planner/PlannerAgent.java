package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.BuildPeasantAction;
import edu.cwru.sepia.agent.planner.actions.DepositAction;
import edu.cwru.sepia.agent.planner.actions.GatherAction;
import edu.cwru.sepia.agent.planner.actions.MoveAction;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;
import java.io.*;

/**
 * @author Shaun Howard (smh150), Matt Swartwout(mws85)
 */
public class PlannerAgent extends Agent {

	private static final long serialVersionUID = 1L;

	final int requiredWood;
	final int requiredGold;
	final boolean buildPeasants;

	private int townHall; // town hall id

	private Stack<GameState> plan; // list of planned actions

	private static ArrayList<StripsAction> actions;

	// Your PEAgent implementation. This prevents you from having to parse the
	// text file representation of your plan.
	PEAgent peAgent;

	public PlannerAgent(int playernum, String[] params) {
		super(playernum);

		if (params.length < 3) {
			System.err.println("You must specify the required wood and"
					+ " gold amounts and whether peasants should be built");
		}

		requiredWood = Integer.parseInt(params[0]);
		requiredGold = Integer.parseInt(params[1]);
		buildPeasants = Boolean.parseBoolean(params[2]);

		System.out.println("required wood: " + requiredWood
				+ " required gold: " + requiredGold + " build Peasants: "
				+ buildPeasants);
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView,
			History.HistoryView historyView) {

		// generate initial state
		GameState initial = new GameState(0, 0);

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
		GameState goal = new GameState(requiredGold, requiredWood);
		// add optimal number of peasants to the goal state
		for (int i = 0; i < getMaxPeasants(); i++)
			goal.peasants.add(new PlanPeasant());

		// pass initial and goal states to planner and get a plan
		plan = PlannerAgent.AstarSearch(initial, goal);

		savePlan(getActionPlan(plan));

		peAgent = new PEAgent(playernum, plan, buildPeasants);

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
	public static Stack<GameState> AstarSearch(GameState initial, GameState goal) {

		System.out.println("Planner initialized for:\n" + "\tInitial: "
				+ initial + "\n" + "\tGoal: " + goal);

		// Limit the depth at
		int depth = 220;
		System.out.println("Search depth will be limited to: " + depth);

		addBaseActions(initial, goal.peasants.size());

		PriorityQueue<GameState> open = new PriorityQueue<GameState>();
		ArrayList<GameState> closed = new ArrayList<GameState>();

		initial.setCost(0);
		initial.setTotalCost(initial.heuristic(goal));
		open.add(initial);

		while (open.size() > 0) {

			GameState current = open.poll();

			// return the least cost path if the end has been reached
			if (current.isGoal(goal) || current.getCost() >= depth) {
				System.out.println("Plan complete");
				Stack<GameState> aStarPath = buildPath(current);
				return aStarPath;
			}

			// move expanded position to the closed list
			closed.add(current);

			System.out.println("Expanding " + current.getCost() + ": "
					+ current);

			// evaluate next possible moves from current location
			for (GameState neighbor : current.generateChildren(goal, actions)) {

				neighbor.setParent(current);

				// ignore locations in the closed set
				if (!closed.contains(neighbor)) {

					int tempScore = current.getCost()
							+ neighbor.parentAction.getMakeSpan();

					// explore low cost paths
					if (!open.contains(neighbor)
							|| tempScore <= neighbor.getCost()) {
						// track the path
						neighbor.setParent(current);

						// calculate cost from parent
						neighbor.setCost(tempScore);

						// calculate heuristic cost
						neighbor.setTotalCost(tempScore
								+ neighbor.heuristic(goal));

						if (!open.contains(neighbor)) {
							open.add(neighbor);
						}
					}
				}
			}
		}
		System.out.print("No available path");
		return null;
	}

	private static void addBaseActions(GameState s, int maxPeasants) {
		actions = new ArrayList<StripsAction>();
		// move to, gather, and return from each resource node
		for (PlanResource resource : s.resources) {
			int resId = resource.getId();
			for (int i = 1; i <= maxPeasants; i++) {
				actions.add(new MoveAction(i, s, null, resId, false));
				actions.add(new GatherAction(i, resId, resource.getX(),
						resource.getY()));
				actions.add(new MoveAction(i, s, resId, null, true));
			}
		}
		// deposit cargo
		for (int i = 1; i <= maxPeasants; i++)
			actions.add(new DepositAction(i));
		// build peasant
		if (maxPeasants > 1)
			actions.add(new BuildPeasantAction());
	}

	// get valid moves from a given state
	private static List<GameState> getNeighbors(GameState s, GameState goal) {
		ArrayList<GameState> result = new ArrayList<GameState>();
		for (StripsAction a : actions)
			if (a.preconditionsMet(s, goal)) {
				result.add(a.apply(s));
			}
		return result;
	}

	// extract shortest path from a given point to the root node by tracing the
	// parents
	private static Stack<GameState> buildPath(GameState state) {
		Stack<GameState> path = new Stack<>();
		GameState curr = state;
		while (curr.getParent() != null) {
			path.push(curr);
			curr = curr.getParent();
		}
		return path;
	}

	private Stack<StripsAction> getActionPlan(Stack<GameState> plan) {
		Stack<StripsAction> actionPlan = new Stack<>();
		List<StripsAction> actionList = new ArrayList<>();
		Stack<GameState> planCopy = (Stack<GameState>) plan.clone();
		while (!planCopy.isEmpty()) {
			actionList.add(planCopy.pop().parentAction);
		}
		for (StripsAction action : actionList) {
			actionPlan.push(action);
		}
		return actionPlan;
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
	private void savePlan(Stack<StripsAction> stripsPlan) {
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

			Stack<StripsAction> tempPlan = (Stack<StripsAction>) stripsPlan
					.clone();
			int actionNumber = 1;
			while (!tempPlan.isEmpty()) {
				outputWriter.println(actionNumber + ": "
						+ tempPlan.pop().toString());
				actionNumber++;
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

	@Override
	public void terminalStep(State.StateView stateView,
			History.HistoryView historyView) {}

	@Override
	public void savePlayerData(OutputStream outputStream) {}

	@Override
	public void loadPlayerData(InputStream inputStream) {}
}
