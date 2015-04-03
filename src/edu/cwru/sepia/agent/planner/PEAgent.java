package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.ResourceType;
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
 * The Plan Execution Agent (PEA) executes a strips-like action plan for a
 * resource collection game in SEPIA.
 * 
 * @author Shaun Howard (smh150), Matt Swartwout (mws85)
 */
public class PEAgent extends Agent {

	private Stack<GameState> plan; // list of planned actions
	private int townHallID;

	// the list of ids currently in the game
	private List<Integer> currIds = new ArrayList<>();

	// The peasants of the game
	private List<UnitView> peasants = new ArrayList<>();

	// Whether a compound action is still executing in the current game state
	private boolean isBusy;

	public PEAgent(int playernum, Stack<GameState> plan) {
		super(playernum);
		this.plan = plan;
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView,
			History.HistoryView historyView) {

		// identify units and create minimal data structures needed for planning
		for (int id : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(id);
			String typeName = unit.getTemplateView().getName();
			if (typeName.equals("TownHall"))
				townHallID = id;
		}

		System.out.println("Executing plan:");

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

		// Find the next game state from the action plan
		GameState nextState = plan.peek();
		System.out.println("current gold is: " + nextState.gold);
		System.out.println("current wood is: " + nextState.wood);
		
		// The parent action of the next game state
		StripsAction pAction = nextState.parentAction;

		System.out.println("Current action is: " + pAction.toString());

		return createSepiaActions(nextState, pAction, stateView);
	}

	/**
	 * Returns a SEPIA version of the specified Strips Action.
	 * 
	 * @param action
	 *            StripsAction
	 * @return SEPIA representation of same action
	 */
	private Map<Integer, Action> createSepiaActions(GameState nextState,
			StripsAction action, State.StateView stateView) {
		Map<Integer, Action> actions = new HashMap<>();
		List<Integer> peasants = new ArrayList<Integer>();
		
		// Get the current amount of gold
		for (int id : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(id);
			String typeName = unit.getTemplateView().getName();
			if (typeName.equals("Peasant")) {
				peasants.add(id);
			}
		}
		
		// Make a new SEPIA move action with the given peasants,
		// initial positions, and next destinations
		if (action instanceof MoveAction) {
			MoveAction mAction = (MoveAction) action;
			Unit.UnitView townHallUnit = stateView.getUnit(townHallID);
			PlanResource resource = nextState.getResourceWithId(mAction
					.getOriginId() == null ? mAction.getDestId() : mAction
					.getOriginId());
			boolean done = false;
			int i = 0, j = 0;
			int originX, originY, destX, destY;

			// Have to set destination id to resources, unless we are traveling
			// to the townhall
			if (mAction.toTownHall()) {
				originX = resource.getX();
				originY = resource.getY();
				destX = townHallUnit.getXPosition();
				destY = townHallUnit.getYPosition();
				System.out.println("Moving to deposit at townhall");
			} else {
				originX = townHallUnit.getXPosition();
				originY = townHallUnit.getYPosition();
				destX = resource.getX();
				destY = resource.getY();
				System.out.println("Moving to gather resource at " + destX
						+ ", " + destY);
			}

			// get the number of peasants that should be at the destination
			for (PlanPeasant peasant : nextState.peasants) {
				if (mAction.toTownHall() && peasant.getNextTo() == null
						&& peasant.getCargoAmount() > 0) {
					i++;
					currIds.add(peasant.id);
					System.out.println("peasant moving to townhall: "
							+ peasant.id);
				}
				if (!mAction.toTownHall() && peasant.getNextTo() != null) {
					i++;
					currIds.add(peasant.id);
					System.out.println("added peasant to move: " + peasant.id);
				}
			}
			if (mAction.getPeasantCount() < i) {
				i = mAction.getPeasantCount();
			}

			// check to see if the right number of peasants are there
			for (int id : peasants) {
				System.out
						.println("Checking to see if the right number of peasants are at dest");
				Unit.UnitView peasant = stateView.getUnit(id);
				if (isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
						destX, destY)) {
					System.out.println("peasant is at resource: "
							+ peasant.getID());
				} else {
					System.out.println("peasant x and y: "
							+ peasant.getXPosition() + ", "
							+ peasant.getYPosition());
				}

				if (isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
						destX, destY)
						&& peasants.contains(peasant.getID())
						&& ++j == i) {
					done = true;
					System.out.println("We are done moving!");
				}
			}

			if (done
					|| (!mAction.toTownHall() && stateView.resourceAt(destX,
							destY) == null)) {
				System.out.println("removing move action from stack");
				plan.pop();
				isBusy = false;
				currIds.clear();
			} else if (!isBusy) {
				isBusy = true;

				int k = 0;

				// Command each peasant to move to the desired location
				for (int id : peasants) {
					Unit.UnitView peasant = stateView.getUnit(id);

					// When the current peasant is not executing an action...
					if (peasant.getCurrentDurativeAction() == null) {
						// Have them move to the desired x and y
						if (isAdjacent(peasant.getXPosition(),
								peasant.getYPosition(), originX, originY)
								&& k++ < mAction.getPeasantCount()
								&& currIds.contains(peasant.getID())) {
							actions.put(id,
									Action.createCompoundMove(id, destX, destY));
							System.out.println("Added move action: "
									+ actions.get(id).toString());
						}
					}
				}
			}
		}

		if (action instanceof GatherAction) {
			System.out.println("Got to gather action");
			GatherAction gAction = (GatherAction) action;

			boolean done = false;
			int i = 0;

			// check if each peasant at the target is carrying cargo
			for (int id : peasants) {
				System.out.println("current peasant id for gather action: "
						+ id);
				Unit.UnitView peasant = stateView.getUnit(id);
				if (isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
						gAction.getResourceX(), gAction.getResourceY())
						&& peasant.getCargoAmount() > 0
						&& ++i == gAction.getPeasantCount()) {
					done = true;
				}
			}

			if (stateView.resourceAt(gAction.getResourceX(),
					gAction.getResourceY()) == null) {
				System.out.println("resource does not exist");
				done = true;
			}

			if (done) {
				System.out.println("done with gather action");
				plan.pop();
				System.out.println("Stack size is now: " + plan.size());
				isBusy = false;
				currIds.clear();
			} else if (!isBusy) {
				// busy = true;
				int j = 0;
				System.out.println("going to gather peasants");
				// order peasants to gather the target resource
				for (int id : peasants) {
					System.out.println("current peasant id for gather action: "
							+ id);
					Unit.UnitView peasant = stateView.getUnit(id);
					System.out.println("Peasant cargo amount: "
							+ peasant.getCargoAmount());

					if (peasant.getCurrentDurativeAction() == null
							&& peasant.getCargoAmount() <= 0
							&& j++ < gAction.getPeasantCount()) {

						System.out.println("peasant has no actions or cargo: "
								+ peasant.getID());
						System.out.println("Peasant x and y: "
								+ peasant.getXPosition() + ", "
								+ peasant.getYPosition());
						if (isAdjacent(peasant.getXPosition(),
								peasant.getYPosition(), gAction.getResourceX(),
								gAction.getResourceY())) { // && j++ <
															// gAction.getK()){
							actions.put(id, Action.createCompoundGather(id,
									stateView.resourceAt(
											gAction.getResourceX(),
											gAction.getResourceY())));
							System.out.println("Added gather action: "
									+ actions.get(id).toString());
						}
						isBusy = true;
					} else {
						isBusy = false;
					}
				}
			}
		}

		if (action instanceof DepositAction) {
			boolean done = false;
			Unit.UnitView townHallUnit = stateView.getUnit(townHallID);
			DepositAction dAction = (DepositAction) action;
			int i = 0;
			// order peasants at the town hall to deposit resources
			for (int id : peasants) {
				Unit.UnitView peasant = stateView.getUnit(id);

				if (peasant.getCargoType() == null
						&& peasant.getCargoAmount() == 0) {
					i++;
					System.out.println(peasant.getCargoAmount() + ", "
							+ peasant.getCargoType());
				}
			}
			if (i >= dAction.getPeasantCount() && i == peasants.size()) {
				done = true;
			}

			// check if the correct amount of gold/wood has been gathered
			if ((stateView.getResourceAmount(playernum, ResourceType.GOLD) == nextState.gold && stateView
					.getResourceAmount(playernum, ResourceType.WOOD) == nextState.wood)
					|| done) {
				System.out.println("Done with deposit action");
				plan.pop();
				isBusy = false;
				currIds.clear();
			} else if (!isBusy) {
				isBusy = true;
				i = 0;
				// order peasants at the town hall to deposit resources
				for (int id : peasants) {
					Unit.UnitView peasant = stateView.getUnit(id);

					if (peasant.getCurrentDurativeAction() == null) {
						System.out.println("No current action for peasant id: "
								+ peasant.getID());
						System.out.println("Peasant has: "
								+ peasant.getCargoAmount());
						// if(//isAdjacent(peasant.getXPosition(),
						// peasant.getYPosition(),
						// townHallUnit.getXPosition(),
						// townHallUnit.getYPosition()) &&
						// peasant.getCargoAmount() > 0 && i++ <
						// dAction.getPeasantCount()){
						System.out.println("Making deposit action for id: "
								+ id);

						actions.put(id,
								Action.createCompoundDeposit(id, townHallID));
						// }
					}
				}
			}
		}

		if (action instanceof BuildPeasantAction) {
			// check if the correct number of peasants are present
			if (peasants.size() == nextState.peasants.size()) {
				plan.pop();
				isBusy = false;
				currIds.clear();
			} else if (!isBusy) {
				int id = stateView.getTemplate(playernum, "Peasant").getID();
				currIds.add(id);
				isBusy = true;
				// build a peasant
				actions.put(townHallID,
						Action.createCompoundProduction(townHallID, id));
			}
		}
		return actions;
	}

	public static boolean isAdjacent(int x1, int y1, int x2, int y2) {
		return Math.abs(x1 - x2) + Math.abs(y1 - y2) <= 2;
	}

	// private Position getDepositPosition(UnitView peasant) {
	// Position peasPos = new Position(peasant.getXPosition(),
	// peasant.getYPosition());
	// Position thPos = new Position(townhalls.get(0).getXPosition(),
	// townhalls.get(0).getYPosition());
	// List<Position> adjPos = thPos.getAdjacentPositions();
	// Position depPos = new Position(0, 0);
	// double minDist = Double.MAX_VALUE;
	// for (Position adj : adjPos) {
	// double dist = peasPos.euclideanDistance(adj);
	// if (dist < minDist) {
	// minDist = dist;
	// depPos = adj;
	// }
	// }
	// System.out.println("Deposit position is: " + depPos.toString());
	// return depPos;
	// }

	// /**
	// * Checks to see if the current action on the list has been completed. If
	// * so, advances to the next action
	// *
	// * @param stateView
	// * @param townhall
	// * @return the current action, or the next action, if the current action
	// has
	// * been completed.
	// */
	// private GameState getNextAction(StateView stateView, UnitView townhall) {
	// GameState state = plan.get(curState);
	// PlanAction action = state.getFromParent();
	// int peasantId = 0;
	// int location = 0;
	// UnitView peasant = null;
	// ResourceView resource = null;
	//
	// switch (action.getName()) {
	// case "Move1":
	// peasantId = action.getConstants().get(0).getValue();
	// location = action.getConstants().get(2).getValue();
	// peasant = stateView.getUnit(peasantId);
	// resource = findClosestResource(peasant, location, stateView);
	// if (resource != null) {
	// // If we have reached the destination, do the next action in the
	// // list
	// if (isAdjacent(peasant, resource)) {
	// curState++;
	// }
	// } else {
	// // If we have reached the destination, do the next action in the
	// // list
	// if (isAdjacent(peasant, townhall)) {
	// curState++;
	// }
	// }
	// break;
	// case "Harvest1":
	// peasantId = action.getConstants().get(0).getValue();
	// peasant = stateView.getUnit(peasantId);
	// if (peasant.getCargoAmount() > 0) {
	// curState++;
	// }
	// break;
	// case "Deposit1":
	// peasantId = action.getConstants().get(0).getValue();
	// peasant = stateView.getUnit(peasantId);
	// if (peasant.getCargoAmount() == 0) {
	// curState++;
	// }
	// break;
	// }
	// System.out.println("Get next action executed.");
	//
	// return curState == plan.size() ? null : plan.get(curState);
	// }

	private boolean isAdjacent(UnitView peasant, UnitView unitView) {
		return (Math.abs(peasant.getXPosition() - unitView.getXPosition()) <= 1 && Math
				.abs(peasant.getYPosition() - unitView.getYPosition()) <= 1);
	}

	// private void printStateAction(GameState state) {
	// System.out.print(state.getFromParent().getName() + " (");
	//
	// for (Value val : state.getFromParent().getConstants()) {
	// System.out.print(val.getConstantAsString());
	// if (state.getFromParent().getConstants().indexOf(val) != state
	// .getFromParent().getConstants().size() - 1) {
	// System.out.print(", ");
	// }
	// }
	//
	// System.out.println(")");
	// }

	// private ResourceView findClosestResource(UnitView peasant, int location,
	// StateView currentState) {
	// List<ResourceView> resources = null;
	//
	// if (location == Condition.TOWNHALL.getValue()) {
	// return null;
	// } else if (location == Condition.GOLDMINE.getValue()) {
	// resources = currentState.getResourceNodes(Type.GOLD_MINE);
	// } else if (location == Condition.FOREST.getValue()) {
	// resources = currentState.getResourceNodes(Type.TREE);
	// } else {
	// System.out
	// .println("Something went wrong when finding closest resource!");
	// System.out.println("\tPeasant: " + peasant.getID() + ", location: "
	// + location);
	// }
	//
	// double shortestDist = Double.MAX_VALUE;
	// ResourceView closestResource = null;
	//
	// for (ResourceView resource : resources) {
	// int deltX = peasant.getXPosition() - resource.getXPosition();
	// int deltY = peasant.getYPosition() - resource.getYPosition();
	// double dist = Math.sqrt((deltX * deltX) + (deltY * deltY));
	//
	// if (dist < shortestDist) {
	// shortestDist = dist;
	// closestResource = resource;
	// }
	// }
	//
	// return closestResource;
	// }

	// /**
	// * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc) This
	// * converts the difference between the current position and the desired
	// * position to a direction.
	// *
	// * @param xDiff
	// * Integer equal to 1, 0 or -1
	// * @param yDiff
	// * Integer equal to 1, 0 or -1
	// * @return A Direction instance (e.g. SOUTHWEST) or null in the case of
	// * error
	// */
	// private Direction getNextDirection(int xDiff, int yDiff) {
	// System.out.printf("xDiff: %d, yDiff %d", xDiff, yDiff);
	// // figure out the direction the footman needs to move in
	// if (xDiff == 1 && yDiff == 1) {
	// return Direction.SOUTHEAST;
	// } else if (xDiff == 1 && yDiff == 0) {
	// return Direction.EAST;
	// } else if (xDiff == 1 && yDiff == -1) {
	// return Direction.NORTHEAST;
	// } else if (xDiff == 0 && yDiff == 1) {
	// return Direction.SOUTH;
	// } else if (xDiff == 0 && yDiff == -1) {
	// return Direction.NORTH;
	// } else if (xDiff == -1 && yDiff == 1) {
	// return Direction.SOUTHWEST;
	// } else if (xDiff == -1 && yDiff == 0) {
	// return Direction.WEST;
	// } else if (xDiff == -1 && yDiff == -1) {
	// return Direction.NORTHWEST;
	// }
	// System.err.println("Invalid path. Could not determine direction");
	// return null;
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
}
