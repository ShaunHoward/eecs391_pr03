package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.Unit;

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

	private static final long serialVersionUID = -1895318461218130264L;
	
	//the strips action plan of states to execute in the game
	private Stack<GameState> plan;
	
	//id of the town hall on the map
	private int townHallID;

	//list of ids currently in the game
	private List<Integer> currIds = new ArrayList<>();

	// Whether a compound action is still executing in the current game state
	private boolean isBusy;

	public PEAgent(int playernum, Stack<GameState> plan) {
		super(playernum);
		this.plan = plan;
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView,
			History.HistoryView historyView) {

		//Determine the unit of the town hall and get its id.
		for (int id : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(id);
			String typeName = unit.getTemplateView().getName();
			if (typeName.equals("TownHall")){
				townHallID = id;
			}
		}
		
		return middleStep(stateView, historyView);
	}

	/**
	 * Reads the provided STRIPS-like action plan and executes it. If the
	 * plan is correct then when the plan is empty the scenario will end with
	 * a victory. Otherwise weird things will happen.
	 *
	 * The actions are stored in a mapping between the peasant unit ID
	 * executing the action and the action you created.
	 *
	 * For the compound actions, we check their progress and wait
	 * until they are complete before issuing another action for those units busy.
	 * We do this so that we don't issue more actions before the compound action is complete
	 * because then the peasant will stop what it was doing and begin executing the new action.
	 *
	 * This method simply peeks for the next state in the plan (stack) and then
	 * creates sepia actions from the given parent action of the next state.
	 * 
	 * @param stateView - the view of the current game state
	 * @param historyView - the view of the game history
	 * @return a map of peasant id numbers linked to planned actions
	 */
	@Override
	public Map<Integer, Action> middleStep(State.StateView stateView,
			History.HistoryView historyView) {

		// Find the next game state from the action plan
		GameState nextState = plan.peek();
		
		//Get the parent action of the next game state
		StripsAction pAction = nextState.parentAction;;

		//Create a map of ids and actions to enact in the game
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
		
		//Find the current amount of gold
		for (int id : stateView.getUnitIds(playernum)) {
			Unit.UnitView unit = stateView.getUnit(id);
			String typeName = unit.getTemplateView().getName();
			if (typeName.equals("Peasant")) {
				peasants.add(id);
			}
		}
		
		//Make a new SEPIA move action with the given peasants,
		//initial positions, and next destinations
		if (action instanceof MoveAction) {
			MoveAction moveAction = (MoveAction) action;
			Unit.UnitView townHallUnit = stateView.getUnit(townHallID);
			Resource resource = nextState.getResourceWithId(moveAction
					.getOriginId() == null ? moveAction.getDestId() : moveAction
					.getOriginId());
			boolean done = false;
			int i = 0, j = 0;
			int originX, originY, destX, destY;

			//Have to set destination id to resources, unless we are traveling
			//to the townhall
			if (moveAction.toTownHall()) {
				originX = resource.getX();
				originY = resource.getY();
				destX = townHallUnit.getXPosition();
				destY = townHallUnit.getYPosition();
			} else {
				originX = townHallUnit.getXPosition();
				originY = townHallUnit.getYPosition();
				destX = resource.getX();
				destY = resource.getY();
			}

			//Find the number of peasants that should be at the destination
			for (Peasant peasant : nextState.peasants) {
				if (moveAction.toTownHall() && peasant.getAdjacentResource() == null
						&& peasant.getCargoAmount() > 0) {
					i++;
					currIds.add(peasant.id);
				}
				if (!moveAction.toTownHall() && peasant.getAdjacentResource() != null) {
					i++;
					currIds.add(peasant.id);
				}
			}
			
			//alter the move to be for less peasants if necessary
			if (moveAction.getPeasantCount() < i) {
				i = moveAction.getPeasantCount();
			}

			//Determine if the right number of peasants are at the destination
			for (int id : peasants) {
				Unit.UnitView peasant = stateView.getUnit(id);

				//Are peasants adjacent to destination when they should be?
				if (isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
						destX, destY)
						&& peasants.contains(peasant.getID())
						&& ++j == i) {
					done = true;
				}
			}

			//Check to see if we are done moving
			//Can be done when the desired resource is all gathered
			if (done || (!moveAction.toTownHall()
					&& stateView.resourceAt(destX, destY) == null)) {
				plan.pop();
				isBusy = false;
				currIds.clear();
			} else if (!isBusy) {
				isBusy = true;

				int currPeas = 0;

				//Command each peasant to move to the desired location
				for (int id : peasants) {
					Unit.UnitView peasant = stateView.getUnit(id);

					//Have them move to the desired x and y if they should be there
					if (isAdjacent(peasant.getXPosition(),
							peasant.getYPosition(), originX, originY)
							&& currPeas++ < moveAction.getPeasantCount()
							&& currIds.contains(peasant.getID())) {
						
						//create move action for this peasant to the destination
						actions.put(id,
								Action.createCompoundMove(id, destX, destY));
					}
				}
			}
		}

		//Make a new SEPIA gather action for the given number of peasants and 
		//of the desired resource.
		if (action instanceof HarvestAction) {
			HarvestAction gatherAction = (HarvestAction) action;

			boolean done = false;
			int currPeas = 0;

			//Determine if each peasant at the target is carrying cargo
			for (int id : peasants) {
				Unit.UnitView peasant = stateView.getUnit(id);
				
				//When the desired peasants have gathered cargo we are done.
				if (isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
						gatherAction.getResourceX(), gatherAction.getResourceY())
						&& peasant.getCargoAmount() > 0
						&& ++currPeas == gatherAction.getPeasantCount()) {
					done = true;
				}
			}

			//Determine if the desired resource is already gone,
			//then we are also done.
			if (stateView.resourceAt(gatherAction.getResourceX(),
					gatherAction.getResourceY()) == null) {
				done = true;
			}

			//Remove this action from plan
			if (done) {
				plan.pop();
				isBusy = false;
				currIds.clear();
			} else if (!isBusy) {
				
				int peasAssigned = 0;
				
				//Command peasants to gather the desired resource
				for (int id : peasants) {
					Unit.UnitView peasant = stateView.getUnit(id);

					//Determine if we can get cargo with this peasant
					if (peasant.getCargoAmount() <= 0
							&& peasAssigned++ < gatherAction.getPeasantCount()) {
						
						//Is this peasant adjacent to the resource?
						if (isAdjacent(peasant.getXPosition(),
								peasant.getYPosition(), gatherAction.getResourceX(),
								gatherAction.getResourceY())) {
							
							//Create a gather action to the desired resource
							actions.put(id, Action.createCompoundGather(id,
									stateView.resourceAt(
											gatherAction.getResourceX(),
											gatherAction.getResourceY())));
						}
						isBusy = true;
					} else { //otherwise we are not busy anymore
						isBusy = false;
					}
				}
			}
		}

		//Create a SEPIA deposit action
		//At the townhall in the game
		if (action instanceof DepositAction) {
			boolean done = false;
			DepositAction depositAction = (DepositAction) action;
			
			int currPeas = 0;
			
			//Determine the peasants without cargo
			for (int id : peasants) {
				Unit.UnitView peasant = stateView.getUnit(id);

				//Check if the peasant has any cargo, else move on.
				if (peasant.getCargoType() == null
						&& peasant.getCargoAmount() == 0) {
					currPeas++;
				}
			}
			
			//Check if we have met action requirements
			if (currPeas >= depositAction.getPeasantCount() &&
					currPeas == peasants.size()) {
				done = true;
			}

			//Determine if the correct amount of resources have been gathered
			//Then we can remove this action
			if (done || (stateView.getResourceAmount(playernum, ResourceType.GOLD) == nextState.gold && stateView
					.getResourceAmount(playernum, ResourceType.WOOD) == nextState.wood)) {
				plan.pop();
				isBusy = false;
				currIds.clear();
			} else if (!isBusy) {
				isBusy = true;
				currPeas = 0;
				
				//Command peasants at the town hall to deposit resources
				for (int id : peasants) {
					Unit.UnitView peasant = stateView.getUnit(id);

					//Create a deposit action for this peasant
					actions.put(id,
							Action.createCompoundDeposit(id, townHallID));
				}
			}
		}

		//Create a sepia build peasant action
		//from the template id
		if (action instanceof BuildPeasantAction) {
			
			//Determine if we have the correct number of peasants
			//then we are done and can remove this action
			if (peasants.size() == nextState.peasants.size()) {
				plan.pop();
				isBusy = false;
				currIds.clear();
			} else if (!isBusy) {
				int id = stateView.getTemplate(playernum, "Peasant").getID();
				currIds.add(id);
				isBusy = true;
				
				//Otherwise create a new production action with the given id
				actions.put(townHallID,
						Action.createCompoundProduction(townHallID, id));
			}
		}
		return actions;
	}

	/**
	 * Checks if the two sets of given coordinates are adjacent.
	 * 
	 * @param x1 - the x of the first thing
	 * @param y1 - the y of the first thing
	 * @param x2 - the x of the second thing
	 * @param y2 - the y of the second thing
	 * @return whether the two sets of coordinates are adjacent
	 */
	public static boolean isAdjacent(int x1, int y1, int x2, int y2) {
		return Math.abs(x1 - x2) + Math.abs(y1 - y2) <= 2;
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
