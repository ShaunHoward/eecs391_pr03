package edu.cwru.sepia.agent.planner.actions;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.Resource;
import edu.cwru.sepia.environment.model.state.ResourceNode;

/**
 * A move action moves the given number of peasants from the specified origin to the
 * specified destination. 
 * 
 * The preconditions for this move vary based on destination.
 * The preconditions are that the given number of peasants are adjacent to a resource
 * or town hall node. 
 * The effects are that the specified number of peasants end up near either the town hall
 * or a resource given the origin of the move action.
 * 
 * The origin and destination are represented by their ids.
 * The make span for this move is the distance from the origin 
 * and the destination.
 * 
 * @author Shaun Howard, Matt Swartwout
 */
public class MoveAction implements StripsAction {

	//the peasant count to operate on for this action
    private int peasantCount;       
    
    //ids of the unit at the origin location and the destination location
    //one is null if it is the town hall
    public Integer startId, finishId;
    
    //the cost to execute this action in sepia
    private int makeSpan;
    
    //whether this action goes to the townhall
    private boolean toTownHall;

    /**
     * Constructions a new move STRIPS-like action given the designated number of peasants to move, 
     * the game state to move them in, the origin and destination ids, and whether the peasants
     * will move to the town hall.
     * 
     * @param peasantCount - the number of peasants in this move
     * @param state - the current game state to apply the move to
     * @param startId - the origin position of the move
     * @param finishId - the destination position of the move
     * @param toTownhall - whether this move goes to a town hall
     */
    public MoveAction(int peasantCount, GameState state, Integer startId, Integer finishId, boolean toTownhall) {
        this.peasantCount = peasantCount;
        this.startId = startId;
        this.finishId = finishId;
        this.toTownHall = toTownhall;
        //set the makespan to the distance of this resource from the townhall
        makeSpan = state.getResourceWithId(finishId == null ? startId : finishId).getDistance();
    }

    @Override
    public boolean preconditionsMet(GameState s, GameState goal) {
        int currNumPeas = 0;
        
        //Disallow moves to empty resource nodes
        if(finishId != null) { 
            Resource resource = s.getResourceWithId(finishId);
           
            //Prioritize gold over wood
            if((resource.getType().equals(ResourceNode.Type.TREE) && s.gold < goal.gold) || 
               (resource.getType().equals(ResourceNode.Type.GOLD_MINE) && s.gold > goal.gold) ||
               (resource.getAmount() < peasantCount * 100)) {
            	return false;
            }
        }
        
        //see if there are enough peasants present
        if(s.peasants.size() >= peasantCount) {
            for(Peasant peasant: s.peasants)
                if(isValid(peasant) && ++currNumPeas == peasantCount) return true;
        }
        
        return false;
    }

    /**
     * Applies this action to the given game state.
     * Checks, for validity, that the state is able
     * to perform the action.
     * @param s - the state to apply the action to
     * @return the state produced by applying this action
     */
    @Override
    public GameState apply(GameState s) {
        int i = 0;
        GameState newState = new GameState(s);
        for(Peasant peasant: newState.peasants)
        	//check that peasants are valid and there are a limited number selected
            if(isValid(peasant) && i++ < peasantCount) {
            	//
                if(finishId == null) peasant.setAdjacentResource(null);
                else peasant.setAdjacentResource(newState.getResourceWithId(finishId));
            }
        newState.parentAction = this;
        return newState;
    }

    /**
     * Determine whether the given peasant is valid to move,
     * i.e. if the peasant has cargo and is going to the townhall
     * or if the peasant is moving to a resource and has no cargo.
     * @param peasant - the peasant to check for moveable validity
     * @return whether the given peasant is valid to move
     */
    private boolean isValid(Peasant peasant) {
    	//we are going to the town hall
        if(finishId == null){
            return peasant.getCargo() != null && 
            peasant.getCargoAmount() > 0 && 
            peasant.getAdjacentResource() != null &&
            peasant.getAdjacentResource().getId() == startId;
        } else {
            return peasant.getAdjacentResource() == null && peasant.getCargo() == null;
        }
    }

    /**
     * Gets the make span of this action.
     * This make span is set in the constructor.
     * @return the make span of this action
     */
    @Override
    public int getMakeSpan() {
        return makeSpan;
    }
    
    public boolean toTownHall(){
    	return this.toTownHall;
    }

    public int getPeasantCount() { return peasantCount; }

    public Integer getDestId() { return finishId; }

    public Integer getOriginId() { return startId; }

    /**
     * Returns a string with the action type, the number of peasants, and the origin and destination ids.
     * @return the string of the action
     */
    @Override
    public String toString() {
    	String townhall = "town hall";
    	if (startId == null){
    		return "MOVE(peasant count: " + peasantCount + ", from: " + townhall + ", to resource with id: " + finishId + ")";
    	} else {
    		return "MOVE(peasant count: " + peasantCount + ", from resource with id: " + startId + ", to: " + townhall + ")";
    	}    
    }
    
    /**
     * Determines whether two move actions are equal based on their origin ids, destination ids and peasant count.
     * 
     * @return true if the two move actions are equal
     */
	@Override
	public boolean equals(Object o){
		if (o != null && o instanceof MoveAction){
			MoveAction a = (MoveAction)o;
			return a.startId == this.startId &&
					a.finishId == this.finishId &&
					a.peasantCount == this.peasantCount;
		}
		return false;
	}
}
