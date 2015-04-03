package edu.cwru.sepia.agent.planner.actions;
import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.PlanPeasant;
import edu.cwru.sepia.agent.planner.PlanResource;
import edu.cwru.sepia.environment.model.state.ResourceNode;

public class MoveAction implements StripsAction {

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

    private int peasantCount;              // number of peasants to operate on
    public Integer originId,   // id of resource node or null for town hall
                    destId;     // use nullable Integer instead of int
    private int makeSpan;       // cost of executing the action
    private boolean toTownHall;

    public MoveAction(int k, GameState s, Integer originId, Integer destId, boolean toTownhall) {
        this.peasantCount = k;
        this.originId = originId;
        this.destId = destId;
        this.toTownHall = toTownhall;
        makeSpan = s.getResourceWithId(destId == null ? originId : destId).getDistance();
    }

    @Override
    public boolean preconditionsMet(GameState s, GameState goal) {
        int i = 0;
        if(destId != null) { // do not allow move to empty resource node, and gather gold before wood
            PlanResource resource = s.getResourceWithId(destId);
            if(resource.getAmount() < peasantCount * 100 ||
               (resource.getType().equals(ResourceNode.Type.TREE) && s.gold < goal.gold) || 
               (resource.getType().equals(ResourceNode.Type.GOLD_MINE) && s.gold > goal.gold)) {
            	return false;
            }
        }
        if(s.peasants.size() >= peasantCount) {
            for(PlanPeasant peasant: s.peasants)
                if(isValid(peasant) && ++i == peasantCount) return true;
        }
        return false;
    }

    @Override
    public GameState apply(GameState s) {
        int i = 0;
        GameState newState = new GameState(s);
        for(PlanPeasant peasant: newState.peasants)
            if(isValid(peasant) && i++ < peasantCount) {
            	
                if(destId == null) peasant.setNextTo(null);
                else peasant.setNextTo(newState.getResourceWithId(destId));
            }
        newState.parentAction = this;
        return newState;
    }
    
    public boolean toTownHall(){
    	return this.toTownHall;
    }

    public int getK() { return peasantCount; }

    public Integer getDestId() { return destId; }

    public Integer getOriginId() { return originId; }

    private boolean isValid(PlanPeasant peasant) {
        if(destId == null)
            return peasant.getNextTo() != null && peasant.getNextTo().getId() == originId && peasant.getCargo() != null && peasant.getCargoAmount() > 0;
        else
            return peasant.getNextTo() == null && peasant.getCargo() == null;
    }

    @Override
    public int getMakeSpan() {
        return makeSpan;
    }

    @Override
    public String toString() {
        return "MOVE(k:" + peasantCount + ", from:" + originId + ", to:" + destId + ")";
    }
}
