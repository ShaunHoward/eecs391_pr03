package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsAction;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to represent the state of the game after applying one of
 * the available actions. It also tracks the A* specific information such as
 * the parent action and state as well as the cost and heuristic functions. Unlike
 * the path planning A* from the first assignment, the cost of an action may be
 * more than 1. Specifically the cost of executing a compound action such as
 * move can be more than 1. This is accounted for in the heuristic
 * and the cost function.
 *
 * There are plan resources for each resource on the map and plan peasants for all
 * the peasants on the map. Important data from the state view objects is tracked in these
 * objects. The depth of the current state is also tracked for IDA* search purposes if
 * the number of peasants grows very large. 
 * 
 * The desired amount of gold and wood at this state are also set upon construction.
 * 
 * @author Shaun Howard
 */
public class GameState implements Comparable<GameState> {

	//desired gold and wood for this state
	public int gold, wood;
	
	//tracking objects for the resources and peasants on the map
	//at the current step of the game
	public List<PlanResource> resources;
	public List<PlanPeasant> peasants;
	
	//the parent action that created this action
	public StripsAction parentAction;
	
	//values set during A* search
	private int depth = 0;
	private int cost = 0;
	private int totalCost = 0;
	
	//the parent state to this state
	private GameState parent = null;

	/**
	 * Creates a brand new game state from the given amount of gold and wood, empty
	 * of peasants or resources.
	 * 
	 * @param requiredGold
	 *            The goal amount of gold (e.g. 200 for the small scenario)
	 * @param requiredWood
	 *            The goal amount of wood (e.g. 200 for the small scenario)
	 */
	public GameState(int gold, int wood) {
		this.gold = gold;
		this.wood = wood;
		resources = new ArrayList<PlanResource>();
		peasants = new ArrayList<PlanPeasant>();
	}

	/**
	 * Creates a game state deep clone from the given game state, which is now
	 * this state's parent.
	 * 
	 * @param parent - the parent of this game state, i.e. the state to clone
	 */
	public GameState(GameState parent) {
		this(parent.gold, parent.wood);
		
		//Copy each resource to the new state
		for (PlanResource resource : parent.resources) {
			this.resources.add(new PlanResource(resource));
		}
		
		//Copy each peasant to the new state
		for (PlanPeasant peasant : parent.peasants) {
			PlanPeasant newPeasant = new PlanPeasant(peasant.getCargoAmount(), peasant.x, peasant.y, peasant.id);
			newPeasant.setCargo(peasant.getCargo());
			
			//If we are next to something, it's a resource
			PlanResource nextTo = peasant.getNextTo();
			if (nextTo != null){
				newPeasant.setNextTo(getResourceWithId(nextTo.getId()));
			}
			
			peasants.add(newPeasant);
		}
		this.parent = parent;
	}

	public void setParent(GameState parent) {
		this.parent = parent;
	}

	public GameState getParent() {
		return this.parent;
	}
	
	public void setDepth(int depth){
		this.depth = depth;
	}

	public StripsAction getParentAction() {
		return this.parentAction;
	}

	public int getDepth() {
		return this.depth;
	}

	/**
	 * Determines if this state is a goal state.
	 * 
	 * Goal states are determined by the amount of resources the peasants have 
	 * gathered at that amount of time. As long as the wood and gold requirements
	 * are met the peasants can be at any location and the capacities of the resource
	 * locations can be anything.
	 *
	 * @return true if the goal conditions are met in this instance of game
	 *         state.
	 */
	public boolean isGoal(GameState goal) {
		return gold >= goal.gold && wood >= goal.wood;
	}

	/**
	 * Generates all of the possible successor states and their
	 * associated actions in this method (via GameState object).
	 *
	 * @return A list of the possible successor states and their associated
	 *         actions
	 */
	public List<GameState> generateChildren(GameState goal,
			List<StripsAction> actions) {
		ArrayList<GameState> result = new ArrayList<GameState>();
		
		//check if each possible action meets the preconditions
		//to reach the goal state
		for (StripsAction a : actions) {
			if (a.preconditionsMet(this, goal)) {
				
				//apply the action to this state in order to
				//get one action closer to the goal state
				result.add(a.apply(this));
			}
		}
		
		return result;
	}

	/**
	 * A heuristic function for A* search in the resource collection game.
	 * This should be admissible so the properties of A* hold.
	 *
	 * The heuristic utilizes properties of this game state to determine the 
	 * most probable distance to the desired goal state. Some values considered
	 * are:
	 * 
	 * the number of peasants at the goal vs the number of peasants in this state
	 * the number of cycles the peasants will take to gather gold
	 * the number of cycles the peasants will take to gather wood
	 * the number of steps needed to gather resources
	 * the amount of wood gathered at the current state
	 *
	 * Altogether, considering these values provides a fairly accurate heuristic.
	 * It considers when the game needs to build peasants, when the peasants
	 * should choose one resource over another due to distance and cycles necessary
	 * to gather the resources, and finally, the amount of wood at the current state 
	 * helps the peasants find the goal state sooner since wood has less priority as 
	 * gold and needs to be factored in the heuristic.
	 * 
	 * @return The value estimated remaining cost to reach a goal state from
	 *         this state.
	 */
	public int heuristic(GameState destination) {
		int heuristic = 0;

		//Make peasants a priority
		heuristic += (destination.peasants.size() - peasants.size()) * 100;

		//Determine the # of cycles needed to gather gold
		int goldCycles = Math.max(0,destination.gold - gold)
				/ (100 * peasants.size());
		
		//Determine the # of cycles needed to gather wood
		int woodCycles = Math.max(0, destination.wood - wood)
				/ (100 * peasants.size());

		//Choose 30 steps to be the distance from a resource
		heuristic += 60 * (goldCycles + woodCycles);
		
		//Factor in the amount of wood in order to find the goal state
		//faster.
		heuristic -= wood;

		return heuristic;
	}

	int getPeasantCount() {
		return peasants.size();
	}

	public void setCost(int cost) {
		this.cost = cost;
	}

	public void setTotalCost(int totalCost) {
		this.totalCost = totalCost;
	}

	public int getTotalCost() {
		return this.totalCost;
	}

	/**
	 * The cost of this game state is computed and set
	 * in the A* search method.
	 * 
	 * This returns the current cost of this state as computed by
	 * A*.
	 *
	 * @return The current cost to reach this goal
	 */
	public int getCost() {
		return cost;
	}

	/**
	 * Fetches the necessary plan resource with the given id
	 * from the list of resources currently present in the state.
	 * 
	 * @param id - the id of the resource to get
	 * @return the resource with the desired id
	 */
	public PlanResource getResourceWithId(int id) {
		for (PlanResource resource : resources){
			if (resource.getId() == id){
				return resource;
			}
		}
		
		System.out.println("No resource with id " + id);
		return null;
	}

	/**
	 * Simply returns the amount of gold and wood at this state 
	 * as well as the list of peasants and resources.
	 * 
	 * @return a string describing this state by gold, wood, peasants
	 * and resources
	 */
	@Override
	public String toString() {
		String output = "Gold:" + gold + ", Wood:" + wood;
		if (peasants.size() > 0)
			output += " Peas:" + peasants;
		if (resources.size() > 0)
			output += " Res:" + resources;
		return output;
	}

	/**
	 * Returns the comparison between this game state's total cost
	 * and the given game state's total cost. This will return
	 * the same values that Java's int compareTo function would.
	 *
	 * @param o
	 *            The other game state to compare
	 * @return 1 if this state costs more than the other, 0 if equal, -1
	 *         otherwise
	 */
	@Override
	public int compareTo(GameState o) {
		int thisValue = this.getTotalCost();
		int thatValue = o.getTotalCost();
		return (int) (thisValue - thatValue);
	}

	/**
	 * Determine if two game states are equal based on their
	 * desired gold value, wood value, parent actions, and the
	 * equality of their peasants.
	 *
	 * @param o - The game state to compare
	 * @return True if this state equals the other state, false otherwise.
	 */
	@Override
	public boolean equals(Object o) {

		if (o == null || !(o instanceof GameState)) {
			return false;
		}

		GameState s = (GameState) o;
	
		//Determine if the states are equal
		if (this.parentAction != null && s.parentAction != null){
			if (s.gold == this.gold && s.wood == this.wood &&
					s.parentAction.equals(this.parentAction) &&
					this.peasants.size() == s.peasants.size()){
				
				//make sure each peasant is equal between states
				//the peasants should also be in the same order in both lists
				for (int i = 0; i < peasants.size(); i++){
					
					//the peasants are not equal, so these states are not equal
					if (!this.peasants.get(i).equals(s.peasants.get(i))){
						System.out.println("States are not equal");
						return false;
					}
				}
				
				System.out.println("States are equal");
				return true;
			}	
		}
		
		//otherwise, they are not equal
		return false;
	}

	/**
	 * A hash code function based on the amount of gold and wood in this state
	 * as well as the number of peasants and the number of resources.
	 *
	 * @return An integer hash code that is equal for equal states.
	 */
	@Override
	public int hashCode() {
		int hash = 31 * gold;
		hash = hash * (53 * wood);
		hash = hash * peasants.size();
		hash += hash * resources.size();
		return hash;
	}

}
