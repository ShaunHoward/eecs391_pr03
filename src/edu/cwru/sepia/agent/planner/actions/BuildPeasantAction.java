package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.PlanPeasant;
import edu.cwru.sepia.agent.planner.PlanResource;

public class BuildPeasantAction implements StripsAction {

    /**
    Build peasant
        Preconditions: Gold >= 400 and population < 3
        Effects: Gold -400 and add new peasant
        Makespan: 1
    **/

    @Override
    public boolean preconditionsMet(GameState s, GameState goal) {
        return s.gold >= 400 && s.peasants.size() < 3;
    }

    @Override
    public GameState apply(GameState s) {
    	GameState newState = new GameState(s);
    	int nextID = 0;
    	
    	for (PlanPeasant peasant : newState.peasants){
    		if (peasant.id > nextID) {
    			nextID = peasant.id;
    		}
    	}
    	for (PlanResource resource : newState.resources){
    		if (resource.id > nextID) {
    			nextID = resource.id;
    		}
    	}
    	nextID++;
        newState.peasants.add(new PlanPeasant(0, 0, 0, nextID));
        newState.gold -= 400;
        newState.parentAction = this;
        return newState;
    }

    @Override
    public int getMakeSpan() {
        return 1;
    }

    @Override
    public String toString() {
        return "BUILD_PEASANT()";
    }
}
