package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.environment.model.state.ResourceNode;

/**
 * A deposit action in the sepia game engine for the resource collection game.
 * 
 * The preconditions for this action are that the desired number of peasants are
 * adjacent to the town hall and each have cargo.
 * 
 * The effects of this action are that the peasants will have deposited (100 * the 
 * number of peasants)-worth in gold and/or wood to the town hall.
 * 
 * The make span is simply 1 for this action.
 * 
 * @author Shaun Howard, Matt Swartwout
 *
 */
public class DepositAction implements StripsAction {

	//The peasant count for this action to operate on
    private int peasantCount;

    /**
     * Constructs a new deposit action for the specified number
     * of peasants.
     * 
     * @param peasantCount - the number of peasants who need to deposit
     */
    public DepositAction(int peasantCount) {
        this.peasantCount = peasantCount;
    }

    /**
     * Check if the given game state meets the preconditions of the goal
     * if this action is applied to it.
     * This just determines if the peasants in the state are valid
     * for this action.
     * @param s - the state to operate on
     * @param goal - the state desired to reach
     */
    @Override
    public boolean preconditionsMet(GameState s, GameState goal) {
        int currNumPeas = 0;
        if(s.peasants.size() >= peasantCount) {
            for(Peasant peasant: s.peasants) {
            	//check if peasants are valid and that we have enough
                if(isValid(peasant) && ++currNumPeas == peasantCount){
                	return true;
                }
            }
        }
        return false;
    }

    /**
     * Apply this action to the given game state.
     * Checks the validity of peasants for redundancy,
     * then determines what type of resource was deposited by each peasant. 
     * This will reset the cargo of the peasants after they deposit and
     * will track the cargo amount the peasants have.
     * 
     * @param state - the state to apply this action to
     * @return the given state with the action applied to it
     */
    @Override
    public GameState apply(GameState state) {
        int i = 0;
        GameState newState = new GameState(state);
        
        //check if all peasants are valid and determine which
        //type of resource they each deposit, then
        //set their resources to empty.
        for(Peasant peasant: newState.peasants) {
            if(isValid(peasant) && i++ < peasantCount) {
                if(peasant.getCargo().equals(ResourceNode.Type.GOLD_MINE)) {
                	newState.gold += 100;
                }
                else {
                	newState.wood += 100;
                }
                peasant.setCargo(null);
                peasant.setCargoAmount(0);
            }
        }
        newState.parentAction = this;
        return newState;
    }

    /**
     * Checks if the given peasant is valid for this action.
     * Check is based on if the peasant has an adjacent resource
     * and if it has cargo.
     * @param peasant - the peasant to check for validity
     * @return whether the given peasant is valid for this action
     */
    private boolean isValid(Peasant peasant) {
        return peasant.getAdjacentResource() == null && peasant.getCargo() != null && peasant.getCargoAmount() > 0;
    }

    /**
     * The make span of this action is simply 1.
     * @return the make span of this action
     */
    @Override
    public int getMakeSpan() {
        return 1;
    }

    /**
     * The string of this action describing type and peasant count.
     * @return the string describing this action
     */
    @Override
    public String toString() {
        return "DEPOSIT(peasant count: " + peasantCount + ")";
    }
    

    public int getPeasantCount() { return peasantCount; }
    
    /**
     * Determines if two deposit actions are equal based on their peasant counts.
     * 
     * @return true if the two deposit actions are equal
     */
	@Override
	public boolean equals(Object o){
		if (o != null && o instanceof DepositAction){
			DepositAction a = (DepositAction)o;
			return a.peasantCount == this.peasantCount;
		}
		return false;
	}
}
