package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.Peasant;
import edu.cwru.sepia.agent.planner.Resource;

/**
 * Builds a peasant action for use in the resource collection game
 * of PS3 in EECS 391.
 * 
 * This action has certain preconditions that must be met
 * in order to be made. These conditions are that there are fewer than 3 peasants
 * already playing in the game (this is our global limit) and that the peasants
 * have at least 400 gold collected.
 * 
 * The effects are that there is one more peasant added to the game and
 * the peasants now have 400 less gold. The make span of this action is 1.
 * 
 * @author Shaun Howard
 */
public class BuildPeasantAction implements StripsAction {

	//the id for the new peasant
	int id = 0;
	
	/**
	 * Checks if the preconditions to build a peasant are met. 
	 * Returns true if they are met.
	 * @param currState - the current state
	 * @param goalState - the goal state to reach
	 * @return whether the current state can make a build peasant
	 * action
	 */
    @Override
    public boolean preconditionsMet(GameState currState, GameState goalState) {
        return currState.peasants.size() < 3 && currState.gold >= 400;
    }

    /**
     * Applies this action to the given game state. 
     * Returns the given game state with the effects of the
     * action accounted for.
     * @param s - the game state to apply the action to
     * @return the given game state with the effects of the build
     * peasant action accounted for
     */
    @Override
    public GameState apply(GameState s) {
    	GameState newState = new GameState(s);
    	int nextID = 0;
    	
    	//Determine the next id available for the new peasant.
    	//This is based on the peasants and resources, collectively
    	//that already exist on the map.
    	for (Peasant peasant : newState.peasants){
    		if (peasant.id > nextID) {
    			nextID = peasant.id;
    		}
    	}
    	for (Resource resource : newState.resources){
    		if (resource.id > nextID) {
    			nextID = resource.id;
    		}
    	}
    	
    	//Get the next available id
    	nextID++;
    	
    	id = nextID;
    	
    	//create a new peasant with that id
        newState.peasants.add(new Peasant(0, 0, 0, nextID));
        
        //account for making the peasant since it takes 400 gold
        newState.gold -= 400;
        
        //track the action that made the new state
        newState.parentAction = this;
        return newState;
    }

    /**
     * The make span of building a peasant is 1.
     * @return the make span of building a peasant
     */
    @Override
    public int getMakeSpan() {
        return 1;
    }

    /**
     * Returns the type of action as a string.
     * @return the type of action as a string
     */
    @Override
    public String toString() {
        return "BUILD_PEASANT(peasant id: " + id + ")";
    }
}
