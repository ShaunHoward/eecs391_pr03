package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.PlanState;

public interface PlanAction {

    // precondition test
    public boolean preconditionsMet(PlanState s, PlanState goal);

    // returns next state by applying actions to current state
    public PlanState apply(PlanState s);

    // returns the makespan of this action
    public int getMakeSpan();

}
