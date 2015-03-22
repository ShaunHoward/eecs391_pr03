package edu.cwru.sepia.agent.planner;

import edu.cwru.sepia.environment.model.state.State;

import java.util.ArrayList;
import java.util.Arrays;
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

	private static int GoldValue;
	private static int WoodValue;
	private static int requiredWood;
	private static int requiredGold;
	public static boolean isGold;

	private GameState parent;
	private int depth;
	
	public void setDepth(int depth) {
		this.depth = depth;
	}

	private int weight;
	private boolean weightSet;
	private PlanAction fromParent;
	private List<Value> valuesFromParent;
	private List<Condition> conditions;
	private GameState initialState;
	private int numPeasants = 1;

	public int getNumPeasants() {
		return numPeasants;
	}

	public void setNumPeasants(int numPeasants) {
		this.numPeasants = numPeasants;
	}

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
	public GameState(State.StateView state, int playernum, int requiredGold,
			int requiredWood, boolean buildPeasants) {
		 this(generateInitialState(playernum, requiredGold, requiredWood, buildPeasants));
	}
	
	public GameState(List<Condition> initialState) {
		 this.parent = null;
		 this.depth = 0;
		 this.weightSet = false;
		 this.fromParent = null;
		 this.valuesFromParent = null;
		 this.conditions = initialState;
	}
	 
    public GameState(GameState parent, PlanAction action, List<Value> values, List<Condition> state, int numPeasants) {
		 this.parent = parent;
		 this.depth = parent.depth + 1;
		 this.fromParent = action;
		 this.valuesFromParent = values;
		 this.conditions = state;
		 this.numPeasants = numPeasants;
	}
    
    public GameState(GameState orig){
    	this.parent = orig.parent;
    	this.depth = orig.depth;
    	this.fromParent = orig.fromParent;
    	this.valuesFromParent = orig.valuesFromParent;
    	this.conditions = orig.conditions;
    	this.numPeasants = orig.numPeasants;
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
	public boolean isGoal(Condition goal) {
		return conditions.contains(goal);
	}

	public static GameState getGoalState(int requiredGold, int requiredWood) {
		List<Condition> conditions = new ArrayList<>();

		// Add condition Has(Gold, AMT)
		conditions.add(new Condition(Condition.HAS, Arrays.asList(new Value[] {
				new Value(Condition.GOLD), new Value("amt", requiredGold) })));
		// Add condition Has(Wood, AMT)
		conditions.add(new Condition(Condition.HAS, Arrays.asList(new Value[] {
				new Value(Condition.WOOD), new Value("amt", requiredWood) })));
		GameState newState = new GameState(conditions);
		GameState.GoldValue = requiredGold;
		GameState.WoodValue = requiredWood;
		GameState.requiredGold = requiredGold;
		GameState.requiredWood = requiredWood;
		return newState;
	}

	/**
	 * The branching factor of this search graph are much higher than the
	 * planning. Generate all of the possible successor states and their
	 * associated actions in this method.
	 *
	 * @return A list of the possible successor states and their associated
	 *         actions
	 */
	public List<GameState> generateChildren(List<PlanAction> availableActions) {
		List<PlanAction> possibleActions = generatePossibleActions(this, availableActions);
		List<GameState> children = new ArrayList<>();
		for (PlanAction action : possibleActions) {
			children.add(action.apply(this));
		}
		return children;
	}

	public List<PlanAction> generatePossibleActions(GameState state, List<PlanAction> availableActions) {
		List<Value> variables = new ArrayList<>();
		for (Condition condition : state.getConditions()) {
			variables.addAll(condition.getVariables());
		}
		List<Value> units = new ArrayList<>();
		List<Value> positions = new ArrayList<>();
		List<Value> types = new ArrayList<>();
		outer: for (Value variable : variables) {
			if (variable == null || variable.getName().isEmpty()) {
				continue;
			}
			List<Value> addList = null;
			if (variable.getName().equalsIgnoreCase("first")
					|| variable.getName().equalsIgnoreCase("second")
					|| variable.getName().equalsIgnoreCase("third")) {
				addList = units;
			} else if (variable.getName().equalsIgnoreCase("pos")
					|| variable.getName().equalsIgnoreCase("to")
					|| variable.getName().equalsIgnoreCase("from")) {
				addList = positions;
			} else if (variable.getName().equalsIgnoreCase("type")) {
				addList = types;
			} else if (variable.getName().equalsIgnoreCase("amt")) {
				continue;
			}
			if (addList == null) {
				System.out.println("Unrecognized variable name!!! "
						+ variable.getName());
				continue;
			}
			for (Value var : addList) {
				if (variable.getName().equals(var.getName())
						&& variable.equals(var)) {
					continue outer;
				}
			}
			addList.add(variable);
		}
		// List<PlanAction> validActions = new ArrayList<>();
		// while(!stripsActions.isEmpty()) {
		// List<PlanAction> possibleActions =
		// stripsActions.pop().getPossibleActions(units, positions, types);
		// inner: for (PlanAction action : possibleActions) {
		// if (action.preconditionsMet(state)) {
		// for (PlanAction existingAction : validActions) {
		// if (existingAction.equals(action)) {
		// continue inner;
		// }
		// }
		// validActions.add(action);
		// }
		// }
		// }
		List<PlanAction> validActions = new ArrayList<>();
		for (PlanAction actionTemplate : availableActions) {
			List<PlanAction> possibleActions = actionTemplate
					.getPossibleActions(units, positions, types);
			inner: for (PlanAction action : possibleActions) {
				if (action.preconditionsMet(state)) {
					for (PlanAction existingAction : validActions) {
						if (existingAction.equals(action)) {
							continue inner;
						}
					}
					validActions.add(action);
				}
			}
		}
		return validActions;
	}

	public static GameState generateInitialState(int peasantId, int requiredGold, int requiredWood, boolean buildPeasants) {
		List<Condition> conditions = new ArrayList<>();
		// Add condition Holding(Peasant1, Nothing)
		conditions.add(new Condition(Condition.HOLDING, Arrays
				.asList(new Value[] { new Value("first", peasantId),
						new Value(Condition.NOTHING) })));
		// Add condition At(peasent1, Townhall)
		conditions
				.add(new Condition(Condition.AT, Arrays.asList(new Value[] {
						new Value("first", peasantId),
						new Value(Condition.TOWNHALL) })));
		// Add condition Has(wood, 0)
		conditions
				.add(new Condition(Condition.HAS, Arrays.asList(new Value[] {
						new Value(Condition.WOOD),
						new Value("amt", Value.Type.ADD) })));
		// Add condition Has(gold, 0)
		conditions
				.add(new Condition(Condition.HAS, Arrays.asList(new Value[] {
						new Value(Condition.GOLD),
						new Value("amt", Value.Type.ADD) })));
		// Add condition Contains(Goldmine, gold)
		conditions.add(new Condition(Condition.CONTAINS, Arrays
				.asList(new Value[] { new Value(Condition.GOLDMINE),
						new Value(Condition.GOLD) })));
		// Add condition Contains(Forest, wood)
		conditions.add(new Condition(Condition.CONTAINS, Arrays
				.asList(new Value[] { new Value(Condition.FOREST),
						new Value(Condition.WOOD) })));
		// Add condition Hall(Townhall)
		conditions.add(new Condition(Condition.HALL, Arrays
				.asList(new Value[] { new Value(Condition.TOWNHALL) })));
		// Add condition Numpeas(numpeas)
		int peasants = 1;
		if (buildPeasants == true){
			peasants = 3;
		}
		conditions.add(new Condition(Condition.NUMPEAS, Arrays
				.asList(new Value[] { new Value("amt", peasants, Value.Type.ADD) })));
		GameState newState = new GameState(conditions);
		GameState.GoldValue = 0;
		GameState.WoodValue = 0;
		GameState.requiredGold = requiredGold;
		GameState.requiredWood = requiredWood;
		if (buildPeasants == true){
			newState.setNumPeasants(3);
		}
		return newState;
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
	public double heuristic() {
		int goal = isGold ? GoldValue : WoodValue;
		return (goal - getValue()) / (100 * numPeasants());
	}

	int numPeasants() {
		for (Condition c : conditions) {
			if (c.getName().equals("Numpeas")) {
				return c.getValue(0).getValue();
			}
		}
		return -1;
	}

	private int getValue() {
		int id = isGold ? 11 : 12;
		for (Condition c : conditions) {
			if (c.getName().equals("Has") && c.getValue(0).getValue() == id) {
				return c.getValue(1).getValue();
			}
		}
		return -1;
	}
	
	 public GameState getParent() {
		 return this.parent;
     }

	/**
	 *
	 * Write the function that computes the current cost to get to this node.
	 * This is combined with your heuristic to determine which actions/states
	 * are better to explore.
	 *
	 * @return The current cost to reach this goal
	 */
	public double getCost() {
		if (!weightSet) {
			weight = (int) (depth + heuristic());
			weightSet = true;
		}
		return weight;
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
		return (int) (this.getCost() - o.getCost());
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
		List<Condition> conds = new ArrayList<>(s.conditions);
		for (Condition c : conditions) {
			boolean isIn = false;
			for (Condition c2 : s.conditions) {
				if (c.equals(c2)) {
					isIn = true;
					conds.remove(c2);
					break;
				}
			}
			if (!isIn) {
				return false;
			}
		}
		return conds.isEmpty();
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
		int hash = (int) (31 * getCost() * conditions.size());
		return hash;
	}
	
	 public List<Condition> getConditions() {
		 return this.conditions;
     }
	 
	 public PlanAction getFromParent() {
		 return this.fromParent;
		 }

	public int getDepth() {
		return this.depth;
	}
	
	@Override
	public String toString() {
		if (valuesFromParent == null) {
			return "State depth: " + depth + ", cost: " + getCost();
		}
		return "State depth: " + depth + ", from " + fromParent + ", cost: " + getCost() + "\n" + valuesFromParent.toString();
	}

}
