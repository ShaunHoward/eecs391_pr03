package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.Resource;
import edu.cwru.sepia.agent.planner.GameState;

/**
 * A gather action is an action to harvest resources in sepia with
 * a STRIPS-like plan.
 * 
 * The preconditions for this action are that the peasants are adjacent to
 * a resource node, the resource can be gathered, and the peasants are not
 * carrying any cargo yet.
 * 
 * The effects for this action are that the resource will have (100 * number 
 * of peasants) less parts, and the peasants that gathered will have 100 of the
 * resource each.
 * 
 * The make span for this action is 1.
 * 
 * @author Shaun Howard
 */
public class GatherAction implements StripsAction {

	//the number of peasants to apply the action with
	private int peasantCount;
	
	//the id of the target resource
	private Integer targetId; 
	
	//the x coordinate of the resource to gather
	private int resX;
	
	//the y coordinate of the resource to gather
	private int resY;

	/**
	 * Construct a gather action from the specified number of peasants, the designated target id, 
	 * the x and y coordinates of the destination.
	 * 
	 * @param k - the number of peasants to operate on
 	 * @param targetId - the id of the target resource
	 * @param x - the x coordinate of the target
	 * @param y - the y coordinate of the target
	 */
	public GatherAction(int k, Integer targetId, int x, int y) {
		this.peasantCount = k;
		this.targetId = targetId;
		this.resX = x;
		this.resY = y;
	}

	/**
	 * Determines if the action can be applied to the given game state
	 * in order to reach the given goal state.
	 * @param s - the state to determine applicability of this action for
	 * @param goal - the goal state we need to reach
	 * @return if this action be applied to the given state to reach the goal
	 */
	@Override
	public boolean preconditionsMet(GameState s, GameState goal) {
		int i = 0;
		
		//Make sure there are enough resources to gather and up to the
		//number of peasants for this action available to gather.
		if (peasantCount <= s.peasants.size()
				&& s.getResourceWithId(targetId).getAmount() >= peasantCount*100) {
			for (Peasant peasant : s.peasants) {
				//check that the peasants are valid for this action
				//and that there are enough
				if (isValid(peasant) && ++i == peasantCount){
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Applies this action to the given game state.
	 * This will check that the peasant is valid, there is
	 * enough resource to gather, and only the max allowed
	 * peasants for this action can gather.
	 * @param s - the state to apply the action to
	 * @return the given state with the action applied to it
	 */
	@Override
	public GameState apply(GameState s) {
		int i = 0;
		GameState newState = new GameState(s);
		Resource res = newState.getResourceWithId(targetId);
		
		//check if peasants are valid and impose the limit
		//of peasant count for gather, then 
		//add cargo if resource has value still.
		for (Peasant peasant : newState.peasants) {
			if (isValid(peasant) 
					&& i++ < peasantCount
					&& res.getAmount() >= 100) {
				
				int value = res.gather();
				
				if (value > 0) {
					peasant.setCargo(res.getType());
					peasant.setCargoAmount(value);
				} else {
					peasant.setCargoAmount(0);
					peasant.setCargo(null);
				}
			}
		}
		
		//track the parent action for the new state
		newState.parentAction = this;
		return newState;
	}

	/**
	 * Determines if the given peasant is valid for this action.
	 * The peasant is checked for an adjacent resource, and target
	 * resource id, and if it has cargo.
	 * 
	 * @param peasant - the peasant to check for validity in this action
	 * @return whether the peasant is valid for this action
	 */
	private boolean isValid(Peasant peasant) {
		return peasant.getAdjacentResource() != null
				&& peasant.getAdjacentResource().getId() == targetId
				&& peasant.getCargo() == null;
	}

	/**
	 * Make span is simply 1 for this action.
	 * @return the make span for this action
	 */
	@Override
	public int getMakeSpan() {
		return 1;
	}

	/**
	 * Returns the string describing the type of action, peasant count, and
	 * target resource id.
	 * 
	 * @return the string describing this action
	 */
	@Override
	public String toString() {
		return "GATHER(k:" + peasantCount + ", id:" + targetId + ")";
	}
	
	public int getPeasantCount() {
		return peasantCount;
	}

	public int getTargetId() {
		return targetId;
	}

	public int getResourceX() {
		return resX;
	}

	public int getResourceY() {
		return resY;
	}
	
	/**
	 * Determines if two gather actions are equal based on their target ids and peasant count.
	 * 
	 * @return true if the two gather actions are equal
	 */
	@Override
	public boolean equals(Object o){
		if (o != null && o instanceof GatherAction){
			GatherAction a = (GatherAction)o;
			return a.targetId == this.targetId &&
					a.peasantCount == this.peasantCount;
		}
		return false;
	}
}
