package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.PlanPeasant;
import edu.cwru.sepia.agent.planner.PlanState;

public class BuildPeasantAction implements PlanAction {

    /**
    Build peasant
        Preconditions: Gold >= 400 and population < 3
        Effects: Gold -400 and add new peasant
        Makespan: 1
    **/

    @Override
    public boolean preconditionsMet(PlanState s, PlanState goal) {
        return s.gold >= 400 && s.peasants.size() < 3;
    }

    @Override
    public PlanState apply(PlanState s) {
        PlanState newState = new PlanState(s);
        newState.peasants.add(new PlanPeasant());
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
