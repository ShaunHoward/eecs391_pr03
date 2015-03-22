package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.action.ActionType;
import edu.cwru.sepia.action.LocatedAction;
import edu.cwru.sepia.action.TargetedAction;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * This is an outline of the PEAgent. Implement the provided methods. You may
 * add your own methods and members.
 */
public class PEAgent extends Agent {

	// The plan being executed
	//private Stack<StripsAction> plan = null;
	int curState = 0;
	List<GameState> plan;

	// maps the real unit Ids to the plan's unit ids
	// when you're planning you won't know the true unit IDs that sepia assigns.
	// So you'll use placeholders (1, 2, 3).
	// this maps those placeholders to the actual unit IDs.
	private Map<Integer, Integer> peasantIdMap;
	private int townhallId;
	private int peasantTemplateId;

	public PEAgent(int playernum, List<GameState> plan) {
		super(playernum);
		peasantIdMap = new HashMap<Integer, Integer>();
		this.plan = plan;

	}

	public PEAgent(int playernum, Stack<StripsAction> plan2) {
		super(playernum);
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView,
			History.HistoryView historyView) {
		// gets the townhall ID and the peasant ID
		for (int unitId : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(unitId);
			String unitType = unit.getTemplateView().getName().toLowerCase();
			if (unitType.equals("townhall")) {
				townhallId = unitId;
			} else if (unitType.equals("peasant")) {
				peasantIdMap.put(unitId, unitId);
			}
		}

		// Gets the peasant template ID. This is used when building a new
		// peasant with the townhall
		for (Template.TemplateView templateView : stateView
				.getTemplates(playernum)) {
			if (templateView.getName().toLowerCase().equals("peasant")) {
				peasantTemplateId = templateView.getID();
				break;
			}
		}

		return middleStep(stateView, historyView);
	}

	/**
	 * This is where you will read the provided plan and execute it. If your
	 * plan is correct then when the plan is empty the scenario should end with
	 * a victory. If the scenario keeps running after you run out of actions to
	 * execute then either your plan is incorrect or your execution of the plan
	 * has a bug.
	 *
	 * You can create a SEPIA deposit action with the following method
	 * Action.createPrimitiveDeposit(int peasantId, Direction townhallDirection)
	 *
	 * You can create a SEPIA harvest action with the following method
	 * Action.createPrimitiveGather(int peasantId, Direction resourceDirection)
	 *
	 * You can create a SEPIA build action with the following method
	 * Action.createPrimitiveProduction(int townhallId, int peasantTemplateId)
	 *
	 * You can create a SEPIA move action with the following method
	 * Action.createCompoundMove(int peasantId, int x, int y)
	 *
	 * these actions are stored in a mapping between the peasant unit ID
	 * executing the action and the action you created.
	 *
	 * For the compound actions you will need to check their progress and wait
	 * until they are complete before issuing another action for that unit. If
	 * you issue an action before the compound action is complete then the
	 * peasant will stop what it was doing and begin executing the new action.
	 *
	 * To check an action's progress you can call getCurrentDurativeAction on
	 * each UnitView. If the Action is null nothing is being executed. If the
	 * action is not null then you should also call getCurrentDurativeProgress.
	 * If the value is less than 1 then the action is still in progress.
	 *
	 * Also remember to check your plan's preconditions before executing!
	 */
	@Override
	public Map<Integer, Action> middleStep(State.StateView stateView,
			History.HistoryView historyView) {
		Map<Integer, edu.cwru.sepia.action.Action> builder = new HashMap<Integer, edu.cwru.sepia.action.Action>();
		List<Integer> allUnitIds = stateView.getAllUnitIds();
		List<UnitView> peasants = new ArrayList<>();
		List<UnitView> townhalls = new ArrayList<>();
		for (int i = 0; i < allUnitIds.size(); i++) {
			int id = allUnitIds.get(i);
			UnitView unit = stateView.getUnit(id);
			String unitTypeName = unit.getTemplateView().getName();
			if (unitTypeName.equals("TownHall"))
				townhalls.add(unit);
			if (unitTypeName.equals("Peasant"))
				peasants.add(unit);
		}
		Action b = null;
		GameState state = getNextAction(stateView, townhalls.get(0));
		if (state == null) {
			return builder;
		}
		System.out.println("Executing Action:");
		printStateAction(state);
		PlanAction action = state.getFromParent();
		int peasantId = 0;
		int location = 0;
		UnitView peasant = null;
		ResourceView resource = null;
		switch (action.getName()) {
		case "Move1":
			// Move from peasant id to location
			System.out.println("Moving!");
			peasantId = action.getConstants().get(0).getValue();
			location = action.getConstants().get(2).getValue();
			peasant = stateView.getUnit(peasantId);
			resource = findClosestResource(peasant, location, stateView);
			if (resource != null) {
				b = new LocatedAction(peasantId, ActionType.COMPOUNDMOVE,
						resource.getXPosition(), resource.getYPosition());
				System.out.println("Moving to resource "
						+ resource.getXPosition() + ", "
						+ resource.getYPosition());
				builder.put(peasantId, b);
			} else {
				b = new LocatedAction(peasantId, ActionType.COMPOUNDMOVE,
						townhalls.get(0).getXPosition(), townhalls.get(0)
								.getYPosition());
				builder.put(peasantId, b);
			}
			break;
		case "Harvest1":
			// Harvest the adjacent resource (peasant should be standing next to
			// it)
			System.out.println("Harvesting!");
			peasantId = action.getConstants().get(0).getValue();
			location = action.getConstants().get(1).getValue();
			peasant = stateView.getUnit(peasantId);
			resource = findClosestResource(peasant, location, stateView);
			if (resource == null) {
				System.out.println("Issue with finding closest resource");
			}
			b = new TargetedAction(peasantId, ActionType.COMPOUNDGATHER,
					resource.getID());
			builder.put(peasantId, b);
			break;
		case "Deposit1":
			// Deposit the held resource (peasant should be next to the town
			// hall already)
			System.out.println("Depositing!");
			peasantId = action.getConstants().get(0).getValue();
			b = new TargetedAction(peasantId, ActionType.COMPOUNDDEPOSIT,
					townhalls.get(0).getID());
			builder.put(peasantId, b);
			break;
		}
		return builder;
	}

	/**
	 * Checks to see if the current action on the list has been completed. If
	 * so, advances to the next action
	 * 
	 * @param stateView
	 * @param townhall
	 * @return the current action, or the next action, if the current action has
	 *         been completed.
	 */
	private GameState getNextAction(StateView stateView, UnitView townhall) {
		GameState state = plan.get(curState);
		PlanAction action = state.getFromParent();
		int peasantId = 0;
		int location = 0;
		UnitView peasant = null;
		ResourceView resource = null;
		switch (action.getName()) {
		case "Move1":
			peasantId = action.getConstants().get(0).getValue();
			location = action.getConstants().get(2).getValue();
			peasant = stateView.getUnit(peasantId);
			resource = findClosestResource(peasant, location, stateView);
			if (resource != null) {
				if (isAdjacent(peasant, resource)) { // If we have reached the
														// destination, do the
														// next action in the
														// list
					curState++;
				}
			} else {
				if (isAdjacent(peasant, townhall)) { // If we have reached the
														// destination, do the
														// next action in the
														// list
					curState++;
				}
			}
			break;
		case "Harvest1":
			peasantId = action.getConstants().get(0).getValue();
			peasant = stateView.getUnit(peasantId);
			if (peasant.getCargoAmount() > 0) {
				curState++;
			}
			break;
		case "Deposit1":
			peasantId = action.getConstants().get(0).getValue();
			peasant = stateView.getUnit(peasantId);
			if (peasant.getCargoAmount() == 0) {
				curState++;
			}
			break;
		}
		return curState == plan.size() ? null : plan.get(curState);
	}

	private boolean isAdjacent(UnitView peasant, UnitView unitView) {
		return (Math.abs(peasant.getXPosition() - unitView.getXPosition()) <= 1 && Math
				.abs(peasant.getYPosition() - unitView.getYPosition()) <= 1);
	}

	private boolean isAdjacent(UnitView peasant, ResourceView resource) {
		return (Math.abs(peasant.getXPosition() - resource.getXPosition()) <= 1 && Math
				.abs(peasant.getYPosition() - resource.getYPosition()) <= 1);
	}

	private void printStateAction(GameState state) {
		System.out.print(state.getFromParent().getName() + " (");
		for (Value val : state.getFromParent().getConstants()) {
			System.out.print(val.getConstantAsString());
			if (state.getFromParent().getConstants().indexOf(val) != state
					.getFromParent().getConstants().size() - 1) {
				System.out.print(", ");
			}
		}
		System.out.println(")");
	}

	private ResourceView findClosestResource(UnitView peasant, int location,
			StateView currentState) {
		List<ResourceView> resources = null;
		if (location == Condition.TOWNHALL.getValue()) {
			return null;
		} else if (location == Condition.GOLDMINE.getValue()) {
			resources = currentState.getResourceNodes(Type.GOLD_MINE);
		} else if (location == Condition.FOREST.getValue()) {
			resources = currentState.getResourceNodes(Type.TREE);
		} else {
			System.out
					.println("Something went wrong when finding closest resource!");
			System.out.println("\tPeasant: " + peasant.getID() + ", location: "
					+ location);
		}
		double shortestDist = Double.MAX_VALUE;
		ResourceView closestResource = null;
		for (ResourceView resource : resources) {
			int deltX = peasant.getXPosition() - resource.getXPosition();
			int deltY = peasant.getYPosition() - resource.getYPosition();
			double dist = Math.sqrt((deltX * deltX) + (deltY * deltY));
			if (dist < shortestDist) {
				shortestDist = dist;
				closestResource = resource;
			}
		}
		return closestResource;
	}

	/**
	 * Returns a SEPIA version of the specified Strips Action.
	 * 
	 * @param action
	 *            StripsAction
	 * @return SEPIA representation of same action
	 */
	private Action createSepiaAction(StripsAction action) {
		return null;
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
}
