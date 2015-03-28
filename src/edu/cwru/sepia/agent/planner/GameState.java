package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.agent.planner.actions.StripsAction;
import edu.cwru.sepia.environment.model.state.State;

import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to represent the state of the game after applying one of
 * the avaiable actions. It will also track the A* specific information such as
 * the parent pointer and the cost and heuristic function. Remember that unlike
 * the path planning A* from the first assignment the cost of an action may be
 * more than 1. Specifically the cost of executing a compound action such as
 * move can be more than 1. You will need to account for this in your heuristic
 * and your cost function.
 *
 * The first instance is constructed from the StateView object (like in PA2).
 * Implement the methods provided and add any other methods and member variables
 * you need.
 *
 * Some useful API calls for the state view are
 *
 * state.getXExtent() and state.getYExtent() to get the map size
 *
 * I recommend storing the actions that generated the instance of the GameState
 * in this class using whatever class/structure you use to represent actions.
 */
public class GameState implements Comparable<GameState> {

	public int gold, wood;
	public List<PlanResource> resources;
	public List<PlanPeasant> peasants;
	public StripsAction parentAction;
	public int x;
	public int y;
	private int depth;
	private int cost = 0;
	private int totalCost = 0;
	private GameState parent = null;

	/**
	 * Construct a GameState from a stateview object. This is used to construct
	 * the initial search node. All other nodes should be constructed from the
	 * another constructor you create or by factory functions that you create.
	 *
	 * @param state
	 *            The current stateview at the time the plan is being created
	 * @param playernum
	 *            The player number of agent that is planning
	 * @param requiredGold
	 *            The goal amount of gold (e.g. 200 for the small scenario)
	 * @param requiredWood
	 *            The goal amount of wood (e.g. 200 for the small scenario)
	 * @param buildPeasants
	 *            True if the BuildPeasant action should be considered
	 */
	public GameState(State.StateView state, GameState previous) {
//		state.getResourceAmount(arg0, arg1)
	}

	public GameState(GameState parent, StripsAction action) {
		this.depth = parent.depth + 1;
		this.parentAction = action;
	}

	// public GameState(GameState orig){
	// this.depth = orig.depth;
	// this.parentAction = orig.parentAction;
	// this.valuesFromParent = orig.valuesFromParent;
	// this.state = orig.state;
	// }

	public GameState(int gold, int wood) {
		this.gold = gold;
		this.wood = wood;
		resources = new ArrayList<PlanResource>();
		peasants = new ArrayList<PlanPeasant>();
	}

	// constructor for cloning another PlanState
	public GameState(GameState parent) {
		this(parent.gold, parent.wood);
		// clone each resource
		for (PlanResource resource : parent.resources) {
			this.resources.add(new PlanResource(resource));
		}
		// clone each peasant and maintain reference to cloned resource if
		// needed
		for (PlanPeasant peasant : parent.peasants) {
			PlanPeasant newPeasant = new PlanPeasant(peasant.getCargoAmount());
			newPeasant.setCargo(peasant.getCargo());
			PlanResource nextTo = peasant.getNextTo();
			if (nextTo != null)
				newPeasant.setNextTo(getResourceWithId(nextTo.getId()));
			peasants.add(newPeasant);
		}
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

	public StripsAction getFromParent() {
		return this.parentAction;
	}

	public int getDepth() {
		return this.depth;
	}

	/**
	 * Unlike in the first A* assignment there are many possible goal states. As
	 * long as the wood and gold requirements are met the peasants can be at any
	 * location and the capacities of the resource locations can be anything.
	 * Use this function to check if the goal conditions are met and return true
	 * if they are.
	 *
	 * @return true if the goal conditions are met in this instance of game
	 *         state.
	 */
	public boolean isGoal(GameState goal) {
		return gold >= goal.gold && wood >= goal.wood;
	}

	/**
	 * The branching factor of this search graph are much higher than the
	 * planning. Generate all of the possible successor states and their
	 * associated actions in this method.
	 *
	 * @return A list of the possible successor states and their associated
	 *         actions
	 */
	public List<GameState> generateChildren(GameState goal,
			List<StripsAction> actions) {
		ArrayList<GameState> result = new ArrayList<GameState>();
		
		for (StripsAction a : actions) {
			if (a.preconditionsMet(this, goal)) {
				result.add(a.apply(this));
			}
		}
		
		return result;
	}

	/**
	 * Write your heuristic function here. Remember this must be admissible for
	 * the properties of A* to hold. If you can come up with an easy way of
	 * computing a consistent heuristic that is even better, but not strictly
	 * necessary.
	 *
	 * Add a description here in your submission explaining your heuristic.
	 *
	 * @return The value estimated remaining cost to reach a goal state from
	 *         this state.
	 */
	public int heuristic(GameState destination) {
		int score = 0;

		// prioritize making peasants
		score += (destination.peasants.size() - peasants.size()) * 100;

		// estimate cycles needed to gather resources
		int cyclesForGold = Math.max(destination.gold - gold, 0)
				/ (peasants.size() * 100);
		int cyclesForWood = Math.max(destination.wood - wood, 0)
				/ (peasants.size() * 100);

		// assume every resource is 30 steps away
		score += (cyclesForGold + cyclesForWood) * 60;
		
		// add weight for more deposited resources
		//score += destination.gold + destination.wood;

		return score;
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
	 *
	 * Write the function that computes the current cost to get to this node.
	 * This is combined with your heuristic to determine which actions/states
	 * are better to explore.
	 *
	 * @return The current cost to reach this goal
	 */
	public int getCost() {
		return cost;
	}

	public PlanResource getResourceWithId(int id) {
		for (PlanResource resource : resources)
			if (resource.getId() == id)
				return resource;
		System.out.println("No resource with id " + id);
		return null;
	}

	@Override
	public String toString() {
		String str = "G:" + gold + ", W:" + wood;
		if (peasants.size() > 0)
			str += " P:" + peasants;
		if (resources.size() > 0)
			str += " R:" + resources;
		return str;
	}

	/**
	 * This is necessary to use your state in the Java priority queue. See the
	 * official priority queue and Comparable interface documentation to learn
	 * how this function should work.
	 *
	 * @param o
	 *            The other game state to compare
	 * @return 1 if this state costs more than the other, 0 if equal, -1
	 *         otherwise
	 */
	@Override
	public int compareTo(GameState o) {
		return (int) (this.getTotalCost() - o.getTotalCost());
	}

	/**
	 * This will be necessary to use the GameState as a key in a Set or Map.
	 *
	 * @param o
	 *            The game state to compare
	 * @return True if this state equals the other state, false otherwise.
	 */
	@Override
	public boolean equals(Object o) {

		if (o == null || !(o instanceof State)) {
			return false;
		}

		GameState s = (GameState) o;
		// if (s.getCost() == this.getCost() &&

		if (s.gold == this.gold && s.wood == this.wood) {
			return true;
		}

		return false;
	}

	/**
	 * This is necessary to use the GameState as a key in a HashSet or HashMap.
	 * Remember that if two objects are equal they should hash to the same
	 * value.
	 *
	 * @return An integer hashcode that is equal for equal states.
	 */
	@Override
	public int hashCode() {
		// int hash = (int) (31 * getCost());
		int hash = 31 * gold;
		hash = hash * (53 * wood);
		return hash;
	}

}
