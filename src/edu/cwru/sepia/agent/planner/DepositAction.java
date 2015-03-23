package edu.cwru.sepia.agent.planner;
import edu.cwru.sepia.environment.model.state.ResourceNode;

public class DepositAction implements PlanAction {

    /**
    Deposit cargo at town hall
        Preconditions: next to town hall and carrying cargo
        Effects: gold/wood +100 and not carrying cargo
        Makespan: 1
    **/

    private int k;              // number of peasants to operate on

    public DepositAction(int k) {
        this.k = k;
    }

    @Override
    public boolean isAllowedFor(PlanState s, PlanState goal) {
        int i = 0;
        if(s.peasants.size() >= k) {
            for(PlanPeasant peasant: s.peasants)
                if(isValid(peasant) && ++i == k) return true;
        }
        return false;
    }

    @Override
    public PlanState applyTo(PlanState s) {
        int i = 0;
        PlanState newState = new PlanState(s);
        for(PlanPeasant peasant: newState.peasants)
            if(isValid(peasant) && i++ < k) {
                if(peasant.getCargo().equals(ResourceNode.Type.GOLD_MINE)) newState.gold += 100;
                else newState.wood += 100;
                peasant.setCargo(null);
            }
        newState.parentAction = this;
        return newState;
    }

    public int getK() { return k; }

    private boolean isValid(PlanPeasant peasant) {
        return peasant.getNextTo() == null && peasant.getCargo() != null;
    }

    @Override
    public int getMakeSpan() {
        return 1;
    }

    @Override
    public String toString() {
        return "DEPOSIT(k:" + k + ")";
    }
}
