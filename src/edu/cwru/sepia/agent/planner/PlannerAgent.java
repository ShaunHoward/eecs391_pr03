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

	List<GameState> plan;
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
				playernum, requiredGold, requiredWood, buildPeasants);
		Stack<PlanAction> stripsPlan = AstarSearch(initialState);

		GameState goalState = GameState.getGoalState(requiredGold, requiredWood);
		Planner planner = new Planner(stripsPlan, initialState, goalState);
		plan = planner.createPlan();
		
		if (plan == null) {
			System.err.println("No plan was found");
			System.exit(1);
			return null;
		}

		// write the plan to a text file
		savePlan(stripsPlan);

		// Instantiates the PEAgent with the specified plan.
		peAgent = new PEAgent(playernum, plan, stripsPlan);

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
	private Stack<PlanAction> AstarSearch(GameState startState) {
		Stack<PlanAction> actions = new Stack<>();
		actions.addAll(PlanAction.getActions(scenario));
		return actions;
		//startState = GameState.generateInitialState(peasantIds.get(0));
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
