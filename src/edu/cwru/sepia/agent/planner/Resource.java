package edu.cwru.sepia.agent.planner;
import edu.cwru.sepia.environment.model.state.ResourceNode;
import edu.cwru.sepia.environment.model.state.Unit;
import edu.cwru.sepia.util.DistanceMetrics;

/**
 * A resource represents a resource view in the sepia game engine for the
 * resource collection game.
 * 
 * @author Shaun Howard
 */
public class Resource {

	//The id of the provided resource view
    public int id;                     
    
    //the x position of the resource on the game grid
    private int x;                     
    
    //the y position of the resource on the game grid
    private int y;                     
    
    //the type of this resource (wood or gold)
    private ResourceNode.Type type;    
    
    //the amount available at this resource
    private int amount;                
    
    //the distance of this resource from the town hall
    private int distance;              

    /**
     * Constructs a resource from a resource view and the town hall on the game 
     * map.
     * 
     * @param resourceView - the resource view of the desired resource to make
     * @param townHall - the town hall unit view
     */
    public Resource(ResourceNode.ResourceView resourceView, Unit.UnitView townHall) {
        this.type = resourceView.getType();
        this.amount = resourceView.getAmountRemaining();
        this.id = resourceView.getID();
        this.x = resourceView.getXPosition();
        this.y = resourceView.getYPosition();
        
        //Calculate an admissible distance from this resource to the town hall
        //Do this because the peasants only travel to/from resources and the town hall
        this.distance = (int)Math.ceil(DistanceMetrics.euclideanDistance(
                resourceView.getXPosition(), resourceView.getYPosition(),
                townHall.getXPosition(), townHall.getYPosition()));
    }

    /**
     * Constructor that makes a new resource out of the given resource
     * with the same data.
     * 
     * @param resToCopy - the resource to make a copy of
     */
    public Resource(Resource resToCopy) {
        this.type = resToCopy.getType();
        this.amount = resToCopy.getAmount();
        this.distance = resToCopy.distance;
        this.id = resToCopy.id;
        this.x = resToCopy.x;
        this.y = resToCopy.y;
    }

    /**
     * Diminishes the amount of this resource by 100, if there
     * are at least 100 parts of this resource left.
     * 
     * @return the amount gathered (100 or 0)
     */
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

    /**
     * Returns the string of this resource with its type, id, amount, and distance to the town hall.
     * @return the string of this resource
     */
    @Override
    public String toString() {
        return (type.equals(ResourceNode.Type.GOLD_MINE) ? "G" : "W") + "(" + id + "," + amount + "," + distance + ")";
    }
}
