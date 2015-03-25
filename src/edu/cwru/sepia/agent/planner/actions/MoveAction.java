package edu.cwru.sepia.agent.planner.actions;
import edu.cwru.sepia.agent.planner.PlanPeasant;
import edu.cwru.sepia.agent.planner.PlanResource;
import edu.cwru.sepia.agent.planner.PlanState;
import edu.cwru.sepia.environment.model.state.ResourceNode;

import org.omg.DynamicAny._DynSequenceStub;

public class MoveAction implements PlanAction {

    /**
    Move to town hall
        Preconditions: next to resource node and carrying cargo
        Effects: next to town hall
        Makespan: distance between resource node and town hall

    Move to resource node
        Preconditions: next to town hall and not carrying cargo
        Effects: next to resource node
        Makespan: distance between resource node and town hall
    **/

    private int k;              // number of peasants to operate on
    public Integer originId,   // id of resource node or null for town hall
                    destId;     // use nullable Integer instead of int
    private int makeSpan;       // cost of executing the action
    public boolean toTownhall;

    public MoveAction(int k, PlanState s, Integer originId, Integer destId, boolean toTownhall) {
        this.k = k;
        this.originId = originId;
        this.destId = destId;
        this.toTownhall = toTownhall;
        makeSpan = s.getResourceWithId(destId == null ? originId : destId).getDistance();
    }

    @Override
    public boolean preconditionsMet(PlanState s, PlanState goal) {
        int i = 0;
        if(destId != null) { // do not allow move to empty resource node, and gather gold before wood
            PlanResource resource = s.getResourceWithId(destId);
            if(resource.getAmount() < k * 100 ||
               (resource.getType().equals(ResourceNode.Type.TREE) && s.gold < goal.gold) ||
               (resource.getType().equals(ResourceNode.Type.GOLD_MINE) && s.gold > goal.gold)) return false;
        }
        if(s.peasants.size() >= k) {
            for(PlanPeasant peasant: s.peasants)
                if(isValid(peasant) && ++i == k) return true;
        }
        return false;
    }

    @Override
    public PlanState apply(PlanState s) {
        int i = 0;
        PlanState newState = new PlanState(s);
        for(PlanPeasant peasant: newState.peasants)
            if(isValid(peasant) && i++ < k) {
                if(destId == null) peasant.setNextTo(null);
                else peasant.setNextTo(newState.getResourceWithId(destId));
            }
        newState.parentAction = this;
        return newState;
    }

    public int getK() { return k; }

    public Integer getDestId() { return destId; }

    public Integer getOriginId() { return originId; }

    private boolean isValid(PlanPeasant peasant) {
        if(destId == null)
            return peasant.getNextTo() != null && peasant.getNextTo().getId() == originId && peasant.getCargo() != null;
        else
            return peasant.getNextTo() == null && peasant.getCargo() == null;
    }

    @Override
    public int getMakeSpan() {
        return makeSpan;
    }

    @Override
    public String toString() {
        return "MOVE(k:" + k + ", from:" + originId + ", to:" + destId + ")";
    }
}
