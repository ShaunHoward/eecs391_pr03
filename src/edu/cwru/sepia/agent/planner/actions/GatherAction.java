package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.PlanPeasant;
import edu.cwru.sepia.agent.planner.PlanResource;
import edu.cwru.sepia.agent.planner.PlanState;

public class GatherAction implements PlanAction {

    /**
    Gather resource
        Preconditions: next to resource node, node not empty, and not carrying cargo
        Effects: resource amount - 100 and carrying cargo
        Makespan: 1
    **/

    private int k;              // nuumber of peasants to operate on
    private Integer targetId;   // id of resource node
    private int x;              // x coordinate of resource node
    private int y;              // y coordinate of resource node

    public GatherAction(int k, Integer targetId, int x, int y) {
        this.k = k;
        this.targetId = targetId;
        this.x = x;
        this.y = y;
    }

    @Override
    public boolean preconditionsMet(PlanState s, PlanState goal) {
        int i = 0;
        if(s.peasants.size() >= k && s.getResourceWithId(targetId).getAmount() > 0) {
            for(PlanPeasant peasant: s.peasants)
                if(isValid(peasant) && ++i == k) return true;
        }
        return false;
    }

    @Override
    public PlanState apply(PlanState s) {
        int i = 0;
        PlanState newState = new PlanState(s);
        PlanResource res = newState.getResourceWithId(targetId);
        for(PlanPeasant peasant: newState.peasants)
            if(isValid(peasant) && i++ < k) {
                res.gather();
                peasant.setCargo(res.getType());
            }
        newState.parentAction = this;
        return newState;
    }

    public int getK() { return k; }

    public int getTargetId() { return targetId; }

    public int getX() { return x; }

    public int getY() { return y; }

    private boolean isValid(PlanPeasant peasant) {
        return peasant.getNextTo()!= null && peasant.getNextTo().getId() == targetId && peasant.getCargo() == null;
    }

    @Override
    public int getMakeSpan() {
        return 0;
    }

    @Override
    public String toString() {
        return "GATHER(k:" + k + ", id:" + targetId + ")";
    }
}
