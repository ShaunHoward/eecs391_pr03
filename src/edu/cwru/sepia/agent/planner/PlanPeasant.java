package edu.cwru.sepia.agent.planner;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.ResourceType;

public class PlanPeasant {

    private PlanResource nextTo;
    private ResourceNode.Type cargoType;
    private int cargoAmount = 0;

    public PlanPeasant(int cargoAmount) {
    	this.cargoAmount = cargoAmount;
    }

    public void setCargoAmount(int amount){
    	this.cargoAmount = amount;
    }
    
    public int getCargoAmount(){
    	return this.cargoAmount;
    }
    public PlanResource getNextTo() { return nextTo; }

    public void setNextTo(PlanResource nextTo) { this.nextTo = nextTo; }

    public ResourceNode.Type getCargo() {
        return cargoType;
    }

    public void setCargo(ResourceNode.Type cargo) {
        this.cargoType = cargo;
    }

    @Override
    public String toString() {
        String str = "(" + (nextTo == null ? "T" : nextTo.getId());
        if(cargoType != null)
            str += "," + (cargoType.equals(ResourceNode.Type.GOLD_MINE) ? "G" : "W");
        str += ")";
        return str;
    }
}
