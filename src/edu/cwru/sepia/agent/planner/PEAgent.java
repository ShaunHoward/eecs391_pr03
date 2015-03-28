package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.action.Action;
import edu.cwru.sepia.agent.Agent;
import edu.cwru.sepia.agent.planner.actions.*;
import edu.cwru.sepia.environment.model.history.History;
import edu.cwru.sepia.environment.model.state.ResourceNode.ResourceView;
import edu.cwru.sepia.environment.model.state.ResourceNode.Type;
import edu.cwru.sepia.environment.model.state.State;
import edu.cwru.sepia.environment.model.state.State.StateView;
import edu.cwru.sepia.environment.model.state.ResourceType;
import edu.cwru.sepia.environment.model.state.Template;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.environment.model.state.Unit.UnitView;
import edu.cwru.sepia.util.Direction;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;

/**
 * The Plan Execution Agent (PEA) executes a plan to gather and deposit resources in SEPIA.
 * @author Shaun Howard (smh150), Matt Swartwout (mws85) 
 */
public class PEAgent extends Agent {

	int curState = 0;
    private Stack<GameState> plan; // list of planned actions
	private int townhallId;
	private int peasantTemplateId;
	private boolean buildPeasants;
	private GameState lastState;
	private int runCount = 0;
	private static final int MAX_DEPTH = 300;
	private List<Integer> currIds = new ArrayList<>();

	private List<UnitView> peasants = new ArrayList<>();
	private List<UnitView> townhalls = new ArrayList<>();
	private boolean busy;
	private boolean retry;
	private PlannerAgent plannerAgent;

	public PEAgent(int playernum, Stack<GameState> plan2, boolean buildPeasants, PlannerAgent planner) {
		super(playernum);
		this.plan = plan2;
		this.plannerAgent = planner;
		this.buildPeasants = buildPeasants;
		this.lastState = null;
		this.retry = false;
	}

	@Override
	public Map<Integer, Action> initialStep(State.StateView stateView,
			History.HistoryView historyView) {
			
		// generate initial state
        GameState initial = new GameState(0, 0);

        // identify units and create minimal data structures needed for planning
        for(int id: stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(id);
            String typeName = unit.getTemplateView().getName();
            if(typeName.equals("TownHall")) townhallId = id;
            if(typeName.equals("Peasant")) initial.peasants.add(new PlanPeasant(unit.getCargoAmount(), 0, 0, unit.getID()));
        }

        // identify resources and create minimal data structures needed for planning
        for(int id: stateView.getAllResourceIds()) {
            initial.resources.add(new PlanResource(stateView.getResourceNode(id), stateView.getUnit(townhallId)));
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

        System.out.println("Executing plan:");
        
        return middleStep(stateView, historyView);
	}
	
//    private int getMaxPeasants() {
//        if(!buildPeasants || (requiredGold + requiredWood) <= 800){
//        	return 1;
//        }
//        if((requiredGold + requiredWood) <= 1200){
//        	return 2;
//        }
//        return 3;
//    }

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
		runCount++;
		Map<Integer, Action> actions = new HashMap<Integer, Action>();
        List<Integer> peasants = new ArrayList<Integer>();
        
//        //need to use values from stateView for initial state
//        if(plan.isEmpty()){
//    		int currentGold = stateView.getResourceAmount(playernum, ResourceType.GOLD);
//            int currentWood = stateView.getResourceAmount(playernum, ResourceType.WOOD);
//            // generate initial state
//    		GameState initial = new GameState(currentGold, currentWood);
//
//    		// identify units and create minimal data structures needed for planning
//    		for (int id : stateView.getUnitIds(playernum)) {
//    			Unit.UnitView unit = stateView.getUnit(id);
//    			String typeName = unit.getTemplateView().getName();
//    			if (typeName.equals("Peasant"))
//    				initial.peasants.add(new PlanPeasant(unit.getCargoAmount()));
//    		}
//
//    		// identify resources and create minimal data structures needed for
//    		// planning
//    		for (int id : stateView.getAllResourceIds()) {
//    			initial.resources.add(new PlanResource(stateView
//    					.getResourceNode(id), stateView.getUnit(townhallId)));
//    		}
//
//        //	GameState initialState = new GameState(lastState);
//        	plan = PlannerAgent.AstarSearch(initial, plannerAgent.goalState, 5);
//        }
        GameState nextState = plan.peek();
        StripsAction pAction = nextState.parentAction;

        for(int id: stateView.getUnitIds(playernum)) {
            Unit.UnitView unit = stateView.getUnit(id);
            String typeName = unit.getTemplateView().getName();
            if(typeName.equals("Peasant")) {
            	peasants.add(id);
                System.out.println("peasant id is: " + unit.getID());
            }
        }
        
        System.out.println("Current action is: " + pAction.toString());
        
        if(pAction instanceof MoveAction) {
            MoveAction mAction = (MoveAction) pAction;
            Unit.UnitView townHallUnit = stateView.getUnit(townhallId);
            PlanResource resource = nextState.getResourceWithId(mAction.getOriginId() == null ?
                                                                mAction.getDestId() :
                                                                mAction.getOriginId());
            boolean done = false;
            boolean toTownHall = mAction.toTownhall;
            int i = 0, j = 0;
            int originX, originY, destX, destY;
            
            //Have to set destination id to resources, unless we are traveling to the townhall
            if(toTownHall) {
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
                System.out.println("Moving to gather resource at " + destX + ", " + destY);
            }
            
            // get the number of peasants that should be at the destination
            for(PlanPeasant peasant: nextState.peasants) {
            //	System.out.println("next to peasant: " + peasant.getNextTo().toString());
            //	System.out.println("To townhall: " + toTownHall); 
                if(toTownHall && peasant.getNextTo() == null && peasant.getCargoAmount() > 0) { 
                	i++;
                	currIds.add(peasant.id);
                	System.out.println("peasant moving to townhall: " + peasant.id);
                }
                if(!toTownHall && peasant.getNextTo() != null) { 
                	i++;
                	currIds.add(peasant.id);
                System.out.println("added peasant to move: " + peasant.id);
                }
            }
            if (mAction.getK() < i) {
            	i = mAction.getK();
            }
            System.out.println("i is: " + i);
            
            // check to see if the right number of peasants are there
            for(int id: peasants) {
            	System.out.println("Checking to see if the right number of peasants are at dest");
                Unit.UnitView peasant = stateView.getUnit(id);
            	if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(), destX, destY)){
            		System.out.println("peasant is at resource: " + peasant.getID());
            	} else {
            		System.out.println("peasant x and y: "+ peasant.getXPosition() + ", " + peasant.getYPosition());
            	}
           
                if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(), destX, destY) &&
                		peasants.contains(peasant.getID()) && ++j == i) {
                	done = true;
                	System.out.println("We are done moving!");
                }
            }
            
            if(done || (!toTownHall && stateView.resourceAt(destX, destY) == null)) {
            	System.out.println("removing move action from stack");
                plan.pop();
                busy = false;
                currIds.clear();
            } else if(!busy) {
                busy = true;
                
                int k = 0;
                
                //Command each peasant to move to the desired location
                for(int id: peasants) {
                    Unit.UnitView peasant = stateView.getUnit(id);
                    
                    //When the current peasant is not executing an action...
                    if (peasant.getCurrentDurativeAction() == null) {
                    	//Have them move to the desired x and y
                    	if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
                                  originX, originY) && k++ < mAction.getK() && currIds.contains(peasant.getID())) {
                        	actions.put(id, Action.createCompoundMove(id, destX, destY));
                        	System.out.println("Added move action: " + actions.get(id).toString());
                    	}
                    }
                }
            }
        }

        if(pAction instanceof GatherAction) {
        	System.out.println("Got to gather action");
            GatherAction gAction = (GatherAction) pAction;
            
            boolean done = false;
            int i = 0;
            
            // check if each peasant at the target is carrying cargo
            for(int id: peasants) {
            	System.out.println("current peasant id for gather action: " + id);
                Unit.UnitView peasant = stateView.getUnit(id);
                if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
                              gAction.getResourceX(), gAction.getResourceY()) &&
                              peasant.getCargoAmount() > 0 &&
                              ++i == gAction.getPeasantCount()){ 
                	done = true;
                }
            }
            
        	if (stateView.resourceAt(gAction.getResourceX(), gAction.getResourceY()) == null) {
        		System.out.println("resource does not exist");
        		done = true;
        	}
            
            if(done) {
            	System.out.println("done with gather action");
                plan.pop();
                System.out.println("Stack size is now: " + plan.size());
                busy = false;
                currIds.clear();
            } else if(!busy) {
            	//busy = true;
                int j = 0;
                System.out.println("going to gather peasants");
                // order peasants to gather the target resource
                for(int id: peasants) {
                	System.out.println("current peasant id for gather action: " + id);
                    Unit.UnitView peasant = stateView.getUnit(id);
                    System.out.println("Peasant cargo amount: " + peasant.getCargoAmount());
                    
                    if (peasant.getCurrentDurativeAction() == null && peasant.getCargoAmount() <= 0 &&
                    		j++ < gAction.getPeasantCount()){
                    	
                    	System.out.println("peasant has no actions or cargo: " + peasant.getID());
                    	System.out.println("Peasant x and y: " + peasant.getXPosition() + ", " + peasant.getYPosition());
	                    if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
	                                 gAction.getResourceX(), gAction.getResourceY())){ //&& j++ < gAction.getK()){
	                        actions.put(id, Action.createCompoundGather(id, stateView.resourceAt(gAction.getResourceX(), gAction.getResourceY())));
	                        System.out.println("Added gather action: " + actions.get(id).toString());
	                    }
	                    busy = true;
                    } else {
                    	busy = false;
                    }
                }
            }
        }

        if(pAction instanceof DepositAction) {
        	boolean done = false;
            Unit.UnitView townHallUnit = stateView.getUnit(townhallId);
            DepositAction dAction = (DepositAction)pAction;
            int i = 0;
         // order peasants at the town hall to deposit resources
            for(int id: peasants) {
                Unit.UnitView peasant = stateView.getUnit(id);
                
                if (peasant.getCargoType() == null && peasant.getCargoAmount() == 0) {
                	i++;
                	System.out.println(peasant.getCargoAmount() + ", " + peasant.getCargoType());
                }
            }
            if (i >= dAction.getPeasantCount() && i == peasants.size()) {
            	done = true;
            }
            
            // check if the correct amount of gold/wood has been gathered
            if((stateView.getResourceAmount(playernum, ResourceType.GOLD) == nextState.gold &&
               stateView.getResourceAmount(playernum, ResourceType.WOOD) == nextState.wood) || done) {
            	System.out.println("Done with deposit action");
                plan.pop();
                busy = false;
                currIds.clear();
            } else if(!busy) {
                busy = true;
                i = 0;
                // order peasants at the town hall to deposit resources
                for(int id: peasants) {
                    Unit.UnitView peasant = stateView.getUnit(id);
                    
                    if(peasant.getCurrentDurativeAction() == null){
                    	System.out.println("No current action for peasant id: " + peasant.getID());
                    	System.out.println("Peasant has: " + peasant.getCargoAmount());
                    //	if(//isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
                    		//	townHallUnit.getXPosition(), townHallUnit.getYPosition()) &&
                    	//		peasant.getCargoAmount() > 0 && i++ < dAction.getPeasantCount()){
                    		System.out.println("Making deposit action for id: " + id);
                    		
                    		actions.put(id, Action.createCompoundDeposit(id, townhallId));
                    //	}
                    }
                }
            }
        }

        if(pAction instanceof BuildPeasantAction) {
            // check if the correct number of peasants are present
            if(peasants.size() == nextState.peasants.size()) {
                plan.pop();
                busy = false;
                currIds.clear();
            } else if(!busy) {
            	int id = stateView.getTemplate(playernum, "Peasant").getID();
            	currIds.add(id);
                busy = true;
                // build a peasant
                actions.put(townhallId, Action.createCompoundProduction(townhallId, id));
            }
        }
        
        lastState = nextState;
        return actions;
	}
	
	/**
	 * Returns a SEPIA version of the specified Strips Action.
	 * 
	 * @param action
	 *            StripsAction
	 * @return SEPIA representation of same action
	 */
	private Action createSepiaAction(GameState nextState, StripsAction action, State.StateView stateView) {

        for(int id: stateView.getUnitIds(playernum)) {
            
        	Unit.UnitView unit = stateView.getUnit(id);
            String typeName = unit.getTemplateView().getName();
            
            if(typeName.equals("Peasant")){
            	peasants.add(unit);
            }
        }
        
        System.out.println("Current action is: " + action.toString());
        
        if(action instanceof MoveAction) {
            MoveAction mAction = (MoveAction) action;
            Unit.UnitView townHallUnit = stateView.getUnit(townhallId);
            PlanResource resource = nextState.getResourceWithId(mAction.getOriginId() == null ?
                                                                mAction.getDestId() :
                                                                mAction.getOriginId());
            boolean done = false;
            boolean toTownHall = mAction.toTownhall;
            int i = 0, j = 0;
            int originX, originY, destX, destY;
            
            //Have to set destination id to resources, unless we are traveling to the townhall
            if(toTownHall) {
                originX = resource.getX();
                originY = resource.getY();
                destX = townHallUnit.getXPosition();
                destY = townHallUnit.getYPosition();
                System.out.println("The townhall rules all destinations!");
            } else {
            	System.out.println("townhall is not everything!");
                originX = townHallUnit.getXPosition();
                originY = townHallUnit.getYPosition();
                destX = resource.getX();
                destY = resource.getY();
            }
            
            // get the number of peasants that should be at the destination
            for(PlanPeasant peasant: nextState.peasants) {
                if(toTownHall && peasant.getNextTo() == null) i++;
                if(!toTownHall && peasant.getNextTo() != null) i++;
            }
            
            // check to see if the right number of peasants are there
            for(UnitView peasant : peasants) {
                if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(), destX, destY) &&
                   ++j == i) done = true;
            }
            

        }

        if(action instanceof GatherAction) {
        	System.out.println("Got to gather action");
            GatherAction gAction = (GatherAction) action;
            
            boolean done = false;
            int i = 0;
            
            // check if each peasant at the target is carrying cargo
            for(UnitView peasant: peasants) {
                if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
                              gAction.getResourceX(), gAction.getResourceY()) &&
                   peasant.getCargoAmount() > 0 && ++i == gAction.getPeasantCount()) done = true;
            }
            
            if(done) {
                plan.pop();
                busy = false;
            } else if(!busy) {
                busy = true;
                int j = 0;
                // order peasants to gather the target resource
                for(UnitView peasant: peasants) {
                    if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
                                  gAction.getResourceX(), gAction.getResourceY()) &&
                       j++ < gAction.getPeasantCount()){
                  //      actions.put(peasant.getID(), Action.createCompoundGather(peasant.getID(), stateView.resourceAt(gAction.getX(), gAction.getY())));
                    }
                 }
             }
        }

        if(action instanceof DepositAction) {
            Unit.UnitView townHallUnit = stateView.getUnit(townhallId);
            // check if the correct amount of gold/wood has been gathered
            if(stateView.getResourceAmount(playernum, ResourceType.GOLD) == nextState.gold &&
               stateView.getResourceAmount(playernum, ResourceType.WOOD) == nextState.wood) {
                plan.pop();
                busy = false;
            } else if(!busy) {
                busy = true;
                int i = 0;
                // order peasants at the town hall to deposit resources
                for(UnitView peasant : peasants) {
                    if(isAdjacent(peasant.getXPosition(), peasant.getYPosition(),
                                  townHallUnit.getXPosition(), townHallUnit.getYPosition()) &&
                       peasant.getCargoAmount() > 0 && i++ < ((DepositAction) action).getPeasantCount()){
                    //    actions.put(id, Action.createCompoundDeposit(id, townhallId));
                    }
                }
            }
        }

        if(action instanceof BuildPeasantAction) {
            // check if the correct number of peasants are present
            if(peasants.size() == nextState.peasants.size()) {
                plan.pop();
                busy = false;
            } else if(!busy) {
                busy = true;
                // build a peasant
            //    actions.put(townhallId, Action.createCompoundProduction(townhallId, stateView.getTemplate(playernum, "Peasant").getID()));
            }
        }
		return null;
	}
	
    public static boolean isAdjacent(int x1, int y1, int x2, int y2) {
        return Math.abs(x1 - x2) + Math.abs(y1 - y2) <= 2;
    }
	
//	private Position getDepositPosition(UnitView peasant) {
//		Position peasPos = new Position(peasant.getXPosition(), peasant.getYPosition());
//		Position thPos = new Position(townhalls.get(0).getXPosition(), townhalls.get(0).getYPosition());
//		List<Position> adjPos = thPos.getAdjacentPositions();
//		Position depPos = new Position(0, 0);
//		double minDist = Double.MAX_VALUE;
//		for (Position adj : adjPos) {
//			double dist = peasPos.euclideanDistance(adj);
//			if (dist < minDist) {
//				minDist = dist;
//				depPos = adj;
//			}
//		}
//		System.out.println("Deposit position is: " + depPos.toString());
//		return depPos;
//	}

//	/**
//	 * Checks to see if the current action on the list has been completed. If
//	 * so, advances to the next action
//	 * 
//	 * @param stateView
//	 * @param townhall
//	 * @return the current action, or the next action, if the current action has
//	 *         been completed.
//	 */
//	private GameState getNextAction(StateView stateView, UnitView townhall) {
//		GameState state = plan.get(curState);
//		PlanAction action = state.getFromParent();
//		int peasantId = 0;
//		int location = 0;
//		UnitView peasant = null;
//		ResourceView resource = null;
//
//		switch (action.getName()) {
//		case "Move1":
//			peasantId = action.getConstants().get(0).getValue();
//			location = action.getConstants().get(2).getValue();
//			peasant = stateView.getUnit(peasantId);
//			resource = findClosestResource(peasant, location, stateView);
//			if (resource != null) {
//				// If we have reached the destination, do the next action in the
//				// list
//				if (isAdjacent(peasant, resource)) {
//					curState++;
//				}
//			} else {
//				// If we have reached the destination, do the next action in the
//				// list
//				if (isAdjacent(peasant, townhall)) {
//					curState++;
//				}
//			}
//			break;
//		case "Harvest1":
//			peasantId = action.getConstants().get(0).getValue();
//			peasant = stateView.getUnit(peasantId);
//			if (peasant.getCargoAmount() > 0) {
//				curState++;
//			}
//			break;
//		case "Deposit1":
//			peasantId = action.getConstants().get(0).getValue();
//			peasant = stateView.getUnit(peasantId);
//			if (peasant.getCargoAmount() == 0) {
//				curState++;
//			}
//			break;
//		}
//		System.out.println("Get next action executed.");
//
//		return curState == plan.size() ? null : plan.get(curState);
//	}

	private boolean isAdjacent(UnitView peasant, UnitView unitView) {
		return (Math.abs(peasant.getXPosition() - unitView.getXPosition()) <= 1 && Math
				.abs(peasant.getYPosition() - unitView.getYPosition()) <= 1);
	}

//	private void printStateAction(GameState state) {
//		System.out.print(state.getFromParent().getName() + " (");
//
//		for (Value val : state.getFromParent().getConstants()) {
//			System.out.print(val.getConstantAsString());
//			if (state.getFromParent().getConstants().indexOf(val) != state
//					.getFromParent().getConstants().size() - 1) {
//				System.out.print(", ");
//			}
//		}
//
//		System.out.println(")");
//	}

//	private ResourceView findClosestResource(UnitView peasant, int location,
//			StateView currentState) {
//		List<ResourceView> resources = null;
//
//		if (location == Condition.TOWNHALL.getValue()) {
//			return null;
//		} else if (location == Condition.GOLDMINE.getValue()) {
//			resources = currentState.getResourceNodes(Type.GOLD_MINE);
//		} else if (location == Condition.FOREST.getValue()) {
//			resources = currentState.getResourceNodes(Type.TREE);
//		} else {
//			System.out
//					.println("Something went wrong when finding closest resource!");
//			System.out.println("\tPeasant: " + peasant.getID() + ", location: "
//					+ location);
//		}
//
//		double shortestDist = Double.MAX_VALUE;
//		ResourceView closestResource = null;
//
//		for (ResourceView resource : resources) {
//			int deltX = peasant.getXPosition() - resource.getXPosition();
//			int deltY = peasant.getYPosition() - resource.getYPosition();
//			double dist = Math.sqrt((deltX * deltX) + (deltY * deltY));
//
//			if (dist < shortestDist) {
//				shortestDist = dist;
//				closestResource = resource;
//			}
//		}
//
//		return closestResource;
//	}



//	/**
//	 * Primitive actions take a direction (e.g. NORTH, NORTHEAST, etc) This
//	 * converts the difference between the current position and the desired
//	 * position to a direction.
//	 *
//	 * @param xDiff
//	 *            Integer equal to 1, 0 or -1
//	 * @param yDiff
//	 *            Integer equal to 1, 0 or -1
//	 * @return A Direction instance (e.g. SOUTHWEST) or null in the case of
//	 *         error
//	 */
//	private Direction getNextDirection(int xDiff, int yDiff) {
//		System.out.printf("xDiff: %d, yDiff %d", xDiff, yDiff);
//		// figure out the direction the footman needs to move in
//		if (xDiff == 1 && yDiff == 1) {
//			return Direction.SOUTHEAST;
//		} else if (xDiff == 1 && yDiff == 0) {
//			return Direction.EAST;
//		} else if (xDiff == 1 && yDiff == -1) {
//			return Direction.NORTHEAST;
//		} else if (xDiff == 0 && yDiff == 1) {
//			return Direction.SOUTH;
//		} else if (xDiff == 0 && yDiff == -1) {
//			return Direction.NORTH;
//		} else if (xDiff == -1 && yDiff == 1) {
//			return Direction.SOUTHWEST;
//		} else if (xDiff == -1 && yDiff == 0) {
//			return Direction.WEST;
//		} else if (xDiff == -1 && yDiff == -1) {
//			return Direction.NORTHWEST;
//		}
//		System.err.println("Invalid path. Could not determine direction");
//		return null;
//	}

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
