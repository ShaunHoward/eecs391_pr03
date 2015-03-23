package edu.cwru.sepia.agent.planner;
import edu.cwru.sepia.environment.model.state.ResourceNode;

public class PlanPeasant {

    private PlanResource nextTo;
    private ResourceNode.Type cargo;

    public PlanPeasant() {}

    public PlanResource getNextTo() { return nextTo; }

    public void setNextTo(PlanResource nextTo) { this.nextTo = nextTo; }

    public ResourceNode.Type getCargo() {
        return cargo;
    }

    public void setCargo(ResourceNode.Type cargo) {
        this.cargo = cargo;
    }

    @Override
    public String toString() {
        String str = "(" + (nextTo == null ? "T" : nextTo.getId());
        if(cargo != null)
            str += "," + (cargo.equals(ResourceNode.Type.GOLD_MINE) ? "G" : "W");
        str += ")";
        return str;
    }
}
