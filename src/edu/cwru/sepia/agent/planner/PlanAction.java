package edu.cwru.sepia.agent.planner;
interface PlanAction {

    // precondition test
    public boolean isAllowedFor(PlanState s, PlanState goal);

    // returns next state by applying actions to current state
    public PlanState applyTo(PlanState s);

    // returns the makespan of this action
    public int getMakeSpan();

}
