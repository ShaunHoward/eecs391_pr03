package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;
import edu.cwru.sepia.agent.planner.PlanPeasant;
import edu.cwru.sepia.environment.model.state.ResourceNode;

public class DepositAction implements StripsAction {

    /**
    Deposit cargo at town hall
        Preconditions: next to town hall and carrying cargo
        Effects: gold/wood +100 and not carrying cargo
        Makespan: 1
    **/

    private int peasantCount;              // number of peasants to operate on

    public DepositAction(int k) {
        this.peasantCount = k;
    }

    @Override
    public boolean preconditionsMet(GameState s, GameState goal) {
        int i = 0;
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
                if(peasant.getCargo().equals(ResourceNode.Type.GOLD_MINE)) {
                	newState.gold += 100;
                }
                else{
                	newState.wood += 100;
                }
                peasant.setCargo(null);
            }
        
        newState.parentAction = (PlanAction) this;
        return newState;
    }

    public int getPeasantCount() { return peasantCount; }

    private boolean isValid(PlanPeasant peasant) {
        return peasant.getNextTo() == null && peasant.getCargo() != null;
    }

    @Override
    public int getMakeSpan() {
        return 1;
    }

    @Override
    public String toString() {
        return "DEPOSIT(k:" + peasantCount + ")";
    }
}
