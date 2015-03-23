package edu.cwru.sepia.agent.planner;
import java.util.ArrayList;
import java.util.List;

public class PlanState {

    public int gold;
    public int wood;
    public List<PlanResource> resources;
    public List<PlanPeasant> peasants;
    public PlanAction parentAction;

    public PlanState(int gold, int wood) {
        this.gold = gold;
        this.wood = wood;
        resources = new ArrayList<PlanResource>();
        peasants = new ArrayList<PlanPeasant>();
    }

    // constructor for cloning another PlanState
    public PlanState(PlanState other) {
        this(other.gold, other.wood);
        // clone each resource
        for(PlanResource resource: other.resources) {
            this.resources.add(new PlanResource(resource));
        }
        // clone each peasant and maintain reference to cloned resource if needed
        for(PlanPeasant peasant: other.peasants) {
            PlanPeasant newPeasant = new PlanPeasant();
            newPeasant.setCargo(peasant.getCargo());
            PlanResource nextTo = peasant.getNextTo();
            if(nextTo != null)
                newPeasant.setNextTo(getResourceWithId(nextTo.getId()));
            peasants.add(newPeasant);
        }
    }

    public PlanResource getResourceWithId(int id) {
        for(PlanResource resource: resources)
            if(resource.getId() == id) return resource;
        System.out.println("No resource with id " + id);
        return null;
    }

    @Override
    public String toString() {
        String str = "G:" + gold + ", W:" + wood;
        if(peasants.size() > 0)
            str += " P:" + peasants;
        if(resources.size() > 0)
            str += " R:" + resources;
        return str;
    }

}
