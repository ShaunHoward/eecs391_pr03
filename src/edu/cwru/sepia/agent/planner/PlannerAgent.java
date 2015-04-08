package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.BuildPeasantAction;
import edu.cwru.sepia.agent.planner.actions.DepositAction;
import edu.cwru.sepia.agent.planner.actions.HarvestAction;
import edu.cwru.sepia.agent.planner.actions.MoveAction;
import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.Unit;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Stack;
import java.io.*;

/**
 * An agent that plans for a resource collection game in SEPIA using A* search.
 * This agent searches through possible actions of peasants for a certain number of
 * peasants. We determine the best outcome of actions based on combindations of their
 * make spans and heuristic weights. 
 * The actions possible for each peasant are the following:
 * Move from X to Y
 * Gather Gold or Wood
 * Deposit Gold or Wood
 * 
 * A peasant can also do nothing.
 * The maximum number of peasants that can play is 3. 
 * The required wood and gold criteria determine win a game is won.
 * This planner creates the action plan by linking SEPIA game data
 * with a STRIPS-like action plan. The Planner Execution Agent then
 * translates that plan back into SEPIA actions.
 * 
 * @author Shaun Howard (smh150), Matt Swartwout(mws85)
 */
public class PlannerAgent extends Agent {
	private long startTime;
	
	private static final long serialVersionUID = 1L;

	//Criteria that determines when the peasants win
	final int requiredWood;
	final int requiredGold;
	final boolean buildPeasants;
	
	//The goal state of the game
	public GameState goalState;

	private int townHallID;

	//The game plan to enact in the SEPIA engine
	private Stack<GameState> plan; 

	//The list of actions that will be tried on the A* chosen game states
	private static ArrayList<StripsAction> actions;

	//Prevents having to parse the text file representation of our plan
	PEAgent peAgent;

	public PlannerAgent(int playernum, String[] params) {
		super(playernum);

		if (params.length < 3) {
			System.err.println("You must specify the required wood and"
					+ " gold amounts and whether peasants should be built");
		}

		requiredGold = Integer.parseInt(params[0]);
		requiredWood = Integer.parseInt(params[1]);
		buildPeasants = Boolean.parseBoolean(params[2]);

		System.out.println("required wood: " + requiredWood
				+ " required gold: " + requiredGold + " build Peasants: "
				+ buildPeasants);
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView,
			History.HistoryView historyView) {

		//Initial state for the search, starting with gold and wood at 0.
		GameState initial = new GameState(0, 0);

		//Recognize all units on the game map
		for (int id : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(id);
			String typeName = unit.getTemplateView().getName();
			if (typeName.equals("TownHall"))
				townHallID = id;
			if (typeName.equals("Peasant"))
				initial.peasants.add(new Peasant(unit.getCargoAmount(), 0, 0, unit.getID()));
		}

		//Find all resource locations on the map
		for (int id : stateView.getAllResourceIds()) {
			initial.resources.add(new Resource(stateView
					.getResourceNode(id), stateView.getUnit(townHallID)));
		}

		//Goal state of the A* search, winning with required gold and wood values
		GameState goal = new GameState(requiredGold, requiredWood);
		
		//We find the best number of peasants to add to our mock game state
		for (int i = 0; i < getMaxPeasants(); i++){
			goal.peasants.add(new Peasant(0, 0, 0, 0));
		}
		
		//Track the goal globally
		goalState = goal;

		/**
		 * Obtain a plan from our A* search implementation, limit to depth 140
		 * Note that search on buildPeasants=true will take approx. 15 sec.
		 * to complete after starting the game. Since the game tree is
		 * very big for 3 peasants, this is tolerable to us.
		 */
		plan = PlannerAgent.AstarSearch(initial, goal, 140);

		//Prints the action list to a text file named "plan"
		savePlan(getActionPlan(plan));

		//Feed the plan to an execution agent to play in SEPIA
		peAgent = new PEAgent(playernum, plan);

		//Call the agent to execute
		return peAgent.initialStep(stateView, historyView);
	}

	@Override
	public Map<Integer, Action> middleStep(State.StateView stateView,
			History.HistoryView historyView) {
		if (peAgent == null) {
			System.err.println("Planning failed. No PEAgent initialized.");
			return null;
		}
		
		startTime = System.nanoTime();
		return peAgent.middleStep(stateView, historyView);
	}

	/**
	 * The A* search for the resource collection game in SEPIA. It find the best actions to take
	 * from the initial state to the goal state. A priority queue tracks the game states and orders
	 * them by their make spans as well as heuristic and cost values. When game states have been 
	 * expanded, 
	 * 
	 * @param initial - the state to initialize search on
	 * @param goal - the goal state with the required amount of gold and wood
	 * @param maxDepth - the maximum depth to in the game state generation tree
	 * @return the strips action plan in form of game state with parents as strips-like actions
	 */
	public static Stack<GameState> AstarSearch(GameState initial, GameState goal, int maxDepth) {

		//Adds generic actions to the action list
		addBaseActions(initial, goal.peasants.size());

		PriorityQueue<GameState> open = new PriorityQueue<GameState>();
		Set<GameState> closed = new HashSet<GameState>();

		//Initialize the first state and the priority queue
		initial.setCost(0);
		initial.setDepth(0);
		initial.setTotalCost(initial.heuristic(goal));
		open.add(initial);

		while (!open.isEmpty()) {

			GameState current = open.poll();
			
			//check to skip this action if it has been done
			if (closed.contains(current)) {
				continue;
			}
					
            //Remove actions that are no longer useful to the state space search
			if (current.peasants.size() >= goal.peasants.size() && actions.size() != 5 * current.peasants.size()){
				removeCostlyActions(current.peasants.size());
			}

			//Build the least cost path when the goal or depth is met
			if (current.isGoal(goal) || current.getDepth() >= maxDepth) {
				Stack<GameState> aStarPath = buildPath(current);
				return aStarPath;
			}
			
			//The expanded state is now in the closed set
			closed.add(current);

			//Generate the children of this game state to evaluate all possible next actions
			for (GameState neighbor : current.generateChildren(goal, actions)) {

				//set up neighbor node from the current node
				neighbor.setParent(current);
				neighbor.setDepth(current.getDepth() + 1);

				//We cannot operate on game states that are closed
				if (!closed.contains(neighbor)) {

					//Calculate a new score based on the cost from start and the make span
					//of the neighbor's parent STRIPS action.
					int tentativeScore = current.getCost()
							+ neighbor.parentAction.getMakeSpan();

					//We expand the nodes with lower cost than previously visited nodes
					if (!open.contains(neighbor)
							|| tentativeScore < neighbor.getCost()) {

						//Set the tentative cost to this node
						neighbor.setCost(tentativeScore);

						//Determine the total cost, including tentative and heuristic cost
						neighbor.setTotalCost(tentativeScore
								+ neighbor.heuristic(goal));
						
                        //Add the neighbor to the open queue
						open.add(neighbor);
					}
				}
			}
		}
		
		//need to inform there is not path
		System.err.print("No available path");
		return null;
	}

	/**
	 * Removes actions that are no longer beneficial to the state space search based
	 * on the current number of peasants in the game. This is typically called when there
	 * are 3 peasants to eliminate the 1 peasant actions.
	 * 
	 * @param numPeasants - the number of peasants in the current search state 
	 */
	private static void removeCostlyActions(int numPeasants) {
		ArrayList<StripsAction> actionsCopy = new ArrayList<>(actions);
		
		//Remove actions that are no longer beneficial to the state search
		for (StripsAction action : actionsCopy){
			
			if (action instanceof BuildPeasantAction){
				actions.remove(action);
			}
			if (action instanceof MoveAction){
				MoveAction mAction = (MoveAction)action;
				if (mAction.getPeasantCount() < numPeasants - 1){
					actions.remove(action);
				}
			}
			if (action instanceof HarvestAction){
				HarvestAction gAction = (HarvestAction)action;
				if (gAction.getPeasantCount() < numPeasants - 1){
					actions.remove(action);
				}
			}
			if (action instanceof DepositAction){
				DepositAction dAction = (DepositAction)action;
				if (dAction.getPeasantCount() < numPeasants - 1){
					actions.remove(action);
				}
			}
		}
	}

	/**
	 * Adds base actions such as:
	 * move entity from x to y
	 * gather resource
	 * deposit resource
	 * 
	 * to the global actions list
	 * 
	 * Produces these actions for from 1 and 3 peasants.
	 * 
	 * @param state - the current state in the game
	 * @param maxPeasants - the maximum number of peasants to build
	 */
	private static void addBaseActions(GameState state, int maxPeasants) {
		
		actions = new ArrayList<StripsAction>();
		
		//Create actions for moving to destinations and gathering resources
		for (Resource resource : state.resources) {
			int resId = resource.getId();
			for (int i = 1; i <= maxPeasants; i++) {
				actions.add(new MoveAction(i, state, null, resId, false));
				actions.add(new HarvestAction(i, resId, resource.getX(),
						resource.getY()));
				actions.add(new MoveAction(i, state, resId, null, true));
			}
		}
		
		//Add a deposit resource action in case we have a resource
		for (int i = 1; i <= maxPeasants; i++)
			actions.add(new DepositAction(i));
		
		//Add a new build peasant action in case we have the resources
		if (maxPeasants > 1)
			actions.add(new BuildPeasantAction());
	}

	/**
	 * Builds the shortest path through the game in order to
	 * produce a stack that the planner execution agent can
	 * enact in SEPIA. The state that is given should have been
	 * produced by the A* search in this planner. The end order
	 * of the stack will have the initial game state at the top
	 * and the goal state at the bottom.
	 * 
	 * @param state - the goal state of the game
	 * @return a stack of game states that are ordered ascending numerical
	 * according to position in time made.
	 */
	private static Stack<GameState> buildPath(GameState state) {
		Stack<GameState> path = new Stack<>();
		GameState curr = state;
		
		//build the plan backwards from goal state to initial state
		//this way the stack will be loaded in order
		while (curr.getParent() != null) {
			path.push(curr);
			curr = curr.getParent();
		}
		return path;
	}

	/**
	 * Gets the action plan from a game state plan. The actions
	 * are in the STRIPS-like form. 
	 * 
	 * @param plan - the mock game state plan
	 * @return the strips action plan to enact in the game
	 */
	private Stack<StripsAction> getActionPlan(Stack<GameState> plan) {
		Stack<StripsAction> actionPlan = new Stack<>();
		List<StripsAction> actionList = new ArrayList<>();
		
		//Only convert to strips when we have a game state plan
		if (plan != null){
			//we don't want to alter the actual plan
			Stack<GameState> planCopy = (Stack<GameState>) plan.clone();
			
			//Add all state actions to the action list in forward order
			while (!planCopy.isEmpty()) {
				actionList.add(planCopy.pop().parentAction);
			}
			
			//Add all state actions to the action stack in reverse order
			for (int i = actionList.size() - 1; i >= 0; i--) {
				actionPlan.push(actionList.get(i));
			}	
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

			Stack<StripsAction> tempPlan = (Stack<StripsAction>) stripsPlan.clone();
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

	/**
	 * Let's assume that a peasant takes one cycle of 4 actions
	 * (move-gather-move-deposit) in order to collect 100 parts of a resource.
	 * Building a new peasant takes 1 action and 300 gold.
	 * We find that 1 peasant is optimal for <= 800 resources.
	 * Then 2 peasants are optimal for <= 1200 resources. 3 will take on the remaining.
	 * These values remain the same throughout the game.
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
			History.HistoryView historyView) {
		long totalTime = System.nanoTime() - startTime;
		System.out.println("Total time was " +totalTime/1e9);
	}

	@Override
	public void savePlayerData(OutputStream outputStream) {}

	@Override
	public void loadPlayerData(InputStream inputStream) {}
}
