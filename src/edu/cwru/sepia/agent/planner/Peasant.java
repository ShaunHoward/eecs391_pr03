package edu.cwru.sepia.agent.planner;
import edu.cwru.sepia.environment.model.state.ResourceNode;

/**
 * A class to track each peasant in the current resource collection game in SEPIA.
 * 
 * A peasant has a location in x,y coordinates, a unique id, a cargoAmount, cargo type, and
 * may be next to a resource on the map.
 * 
 * @author Shaun Howard
 */
public class Peasant {

	//the resource this peasant may be next to
    private Resource adjacentResource;
    
    //the type of cargo the peasant may be carrying
    private ResourceNode.Type cargoType;
    
    //the amount of cargo the peasant may be carrying
    private int cargoAmount = 0;
    
    //the position of the peasant on the game grid
    public int x, y;
    
    //the unique id of the peasant
    public int id;

    /**
     * Constructs a new plan peasant from the amount of cargo it should have,
     * the position it should have in x and y, and the id it should have.
     * 
     * @param cargoAmount - the cargo amount desired to carry
     * @param x - the x coordinate on the grid
     * @param y - the y coordinate on the grid
     * @param id - the unique id of this peasant
     */
    public Peasant(int cargoAmount, int x, int y, int id) {
    	this.cargoAmount = cargoAmount;
    	this.x = x;
    	this.y = y;
    	this.id = id;
    }

    public void setCargoAmount(int amount){
    	this.cargoAmount = amount;
    }
    
    public int getCargoAmount(){
    	return this.cargoAmount;
    }
    
    /**
     * Gets the resource this peasant is adjacent to, if one exists.
     * 
     * Will return null if one does not exist.
     * 
     * @return the resource adjacent to this peasant or null if there is not one
     */
    public Resource getAdjacentResource() { return adjacentResource; }

    /**
     * Sets the resource next to this peasant. Set null if there is not adjacent resource.
     *     
     * @param adjRes - the resource next to this peasant or null if there isn't one
     */
    public void setAdjacentResource(Resource adjRes) { this.adjacentResource = adjRes; }

    public ResourceNode.Type getCargo() {
        return cargoType;
    }

    public void setCargo(ResourceNode.Type cargo) {
        this.cargoType = cargo;
    }

    /**
     * Returns a string describing this peasant.
     * The string outlines whether the peasant has an adjacent resource
     * and its cargo type.
     * @return the string describing this peasant
     */
    @Override
    public String toString() {
    	StringBuilder builder = new StringBuilder();
        builder.append("(" + (adjacentResource == null ? "TH" : adjacentResource.getId()));
        if(cargoType != null){
            builder.append("," + (cargoType.equals(ResourceNode.Type.GOLD_MINE) ? "Gold" : "Wood"));
        }
        builder.append(")");
        return builder.toString();
    }
    
    /**
     * Equals is based on the peasants coordinates and its cargo type and value.
     * @return true if two peasants are eual
     */
    @Override
    public boolean equals(Object o){
    	if (o == null || !(o instanceof Peasant)){
    		return false;
    	} else {
    		Peasant p = (Peasant)o;
    		return p.x == this.x && p.y == this.y && p.cargoAmount == this.cargoAmount && p.cargoType == this.cargoType;
    	}
    }
}
