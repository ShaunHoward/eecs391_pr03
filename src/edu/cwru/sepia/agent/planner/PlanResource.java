package edu.cwru.sepia.agent.planner;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.DistanceMetrics;

public class PlanResource {

    public int id;                     // ResourceView id
    private int x;                      // ResourceView x coordinate
    private int y;                      // ResourceView y coordinate
    private ResourceNode.Type type;     // gold or wood
    private int amount;                 // resources available at node
    private int distance;               // distance from town hall

    public PlanResource(ResourceNode.ResourceView node, Unit.UnitView townHall) {
        this.id = node.getID();
        this.x = node.getXPosition();
        this.y = node.getYPosition();
        this.type = node.getType();
        this.amount = node.getAmountRemaining();
        this.distance = (int)Math.ceil(DistanceMetrics.euclideanDistance(   // ceil the distance for admissibility
                node.getXPosition(), node.getYPosition(),
                townHall.getXPosition(), townHall.getYPosition()));
    }

    // clone constructor
    public PlanResource(PlanResource other) {
        this.id = other.id;
        this.x = other.x;
        this.y = other.y;
        this.type = other.getType();
        this.amount = other.getAmount();
        this.distance = other.distance;
    }

    public int gather() {
        if(this.amount >= 100) {
            this.amount -= 100;
            return 100;
        }
        return 0;
    }

    public int getId() { return id; }

    public ResourceNode.Type getType() { return type; }

    public int getAmount() { return amount; }

    public int getDistance() { return distance; }

    public int getX() { return x; }

    public int getY() { return y; }

    @Override
    public String toString() {
        return (type.equals(ResourceNode.Type.GOLD_MINE) ? "G" : "W") + "(" + id + "," + amount + "," + distance + ")";
    }
}
