//package edu.cwru.sepia.agent.planner;
//
//import java.util.ArrayList;
//import java.util.List;
//
//class PossibleConditions {
//	public static final String[] idnames = { "first", "second", "third" };
//	private int[] ids, locations, types;
//	private List<Condition> holding, at, contains;
//	private List<List<Value>> states;
//
//	public PossibleConditions(int[] ids, int[] locations, int[] types) {
//		this.ids = ids;
//		this.locations = locations;
//		this.types = types;
//		holding = at = contains = null;
//		states = null;
//	}
//
//	public List<Condition> getHolding() {
//		if (holding == null) {
//			holding = condList("Holding", idnames[0], "type", ids, types);
//		}
//		return holding;
//	}
//
//	public List<Condition> getAt() {
//		if (at == null) {
//			at = condList("At", idnames[0], "pos", ids, locations);
//		}
//		return at;
//	}
//
//	public List<Condition> getContains() {
//		if (contains == null) {
//			contains = condList("Contains", "pos", "type", locations, types);
//		}
//		return contains;
//	}
//
//	public List<Condition> getConditions() {
//		List<Condition> conditions = new ArrayList<>();
//		conditions.addAll(getHolding());
//		conditions.addAll(getAt());
//		conditions.addAll(getContains());
//		return conditions;
//	}
//
//	public List<List<Value>> getStates() {
//		if (states == null) {
//			states = statesList();
//		}
//		return states;
//	}
//
//	private List<Condition> condList(String name, String var1name,
//			String var2name, int[] vars1, int[] vars2) {
//		List<Condition> list = new ArrayList<>();
//		Condition template = new Condition(name, new Value[] {
//				new Value(var1name), new Value(var2name) });
//		for (int var1 : vars1) {
//			for (int var2 : vars2) {
//				List<Value> vars = new ArrayList<>();
//				vars.add(new Value(var1name, var1));
//				vars.add(new Value(var2name, var2));
//				list.add(new Condition(template, vars));
//			}
//		}
//		return list;
//	}
//
//	private List<List<Value>> statesList() {
//		List<List<Value>> list = new ArrayList<>();
//		for (int id : ids) {
//			for (int location : locations) {
//				for (int type : types) {
//					List<Value> values = new ArrayList<>();
//					values.add(new Value(idnames[0], id));
//					values.add(new Value("pos", location));
//					values.add(new Value("type", type));
//					list.add(values);
//				}
//			}
//		}
//		return list;
//	}
//
//	// Move(id, from, to):
//	// P: At(id, from)
//	// A: At(id, to)
//	// D: At(id, from)
//	public static PlanAction getMoveAction() {
//		return getMoveAction(1);
//	}
//
//	public static PlanAction getMoveAction(int numpeas) {
//		List<Condition> preconditions = new ArrayList<>();
//		List<Condition> add = new ArrayList<>();
//		List<Condition> delete = new ArrayList<>();
//		for (int i = 0; i < numpeas; i++) {
//			// Add the precondition At(id, from)
//			preconditions.add(new Condition("At", new Value[] {
//					new Value(idnames[i]), new Value("from") }));
//			// Add At(id, to) to the Add list
//			add.add(new Condition("At", new Value[] { new Value(idnames[i]),
//					new Value("to") }));
//			// Add At(id, from) to the delete list
//			delete.add(new Condition("At", new Value[] { new Value(idnames[i]),
//					new Value("from") }));
//		}
//		// Move action is Move(id, from, to)
//		List<String> vars = new ArrayList<>();
//		for (int i = 0; i < numpeas; i++) {
//			vars.add(idnames[i]);
//		}
//		vars.add("from");
//		vars.add("to");
//		return new PlanAction("Move" + numpeas, vars, preconditions, add,
//				delete);
//	}
//
//	// Harvest(id, pos, type):
//	// P: Holding(id, NOTHING), At(id, pos), Contains(pos, type)
//	// A: Holding(id, type)
//	// D: Holding(id, NOTHING)
//	public static PlanAction getHarvestAction() {
//		return getHarvestAction(1);
//	}
//
//	public static PlanAction getHarvestAction(int numpeas) {
//		List<Condition> preconditions = new ArrayList<>();
//		List<Condition> add = new ArrayList<>();
//		List<Condition> delete = new ArrayList<>();
//		for (int i = 0; i < numpeas; i++) {
//			// Add the preconditions Holding(id, Nothing), At(id, pos),
//			// Contains(pos, type)
//			preconditions.add(new Condition("Holding", new Value[] {
//					new Value(idnames[i]), new Value(Condition.NOTHING) }));
//			preconditions.add(new Condition("At", new Value[] {
//					new Value(idnames[i]), new Value("pos") }));
//			// Add the add items Holding(id, type)
//			add.add(new Condition("Holding", new Value[] {
//					new Value(idnames[i]), new Value("type") }));
//			// Add the delete items Holding(id, Nothing)
//			delete.add(new Condition("Holding", new Value[] {
//					new Value(idnames[i]), new Value(Condition.NOTHING) }));
//		}
//		preconditions.add(new Condition("Contains", new Value[] {
//				new Value("pos"), new Value("type") }));
//		// Harvest action is Harvest(ids..., pos, type)
//		List<String> vars = new ArrayList<>();
//		for (int i = 0; i < numpeas; i++) {
//			vars.add(idnames[i]);
//		}
//		vars.add("pos");
//		vars.add("type");
//		return new PlanAction("Harvest" + numpeas, vars, preconditions, add,
//				delete);
//	}
//
//	// Deposit(id, type):
//	// P: Holding(id, type), At(id, TOWNHALL)
//	// A: Holding(id, NOTHING), Has(type, +amt)
//	// D: Holding(id, type)
//	public static PlanAction getDepositAction() {
//		return getDepositAction(1);
//	}
//
//	public static PlanAction getDepositAction(int numpeas) {
//		List<Condition> preconditions = new ArrayList<>();
//		List<Condition> add = new ArrayList<>();
//		List<Condition> delete = new ArrayList<>();
//		for (int i = 0; i < numpeas; i++) {
//			// Add the precondition Holding(id, type)
//			preconditions.add(new Condition("Holding", new Value[] {
//					new Value(idnames[i]), new Value("type") }));
//			// Add the precondition At(id, Townhall)
//			preconditions.add(new Condition("At", new Value[] {
//					new Value(idnames[i]), new Value(Condition.TOWNHALL) }));
//			// Add Holding(id, Nothing) to the Add list
//			add.add(new Condition("Holding", new Value[] {
//					new Value(idnames[i]), new Value(Condition.NOTHING) }));
//			// Add Holding(id, type) to the delete list
//			delete.add(new Condition("Holding", new Value[] {
//					new Value(idnames[i]), new Value("type") }));
//		}
//		// Add +Has(type, amt) to the Add list
//		add.add(new Condition("Has", new Value[] { new Value("type"),
//				new Value("amt", 100 * numpeas, Value.Type.ADD) }));
//		// Deposit action is Deposit(id, type)
//		List<String> vars = new ArrayList<>();
//		for (int i = 0; i < numpeas; i++) {
//			vars.add(idnames[i]);
//		}
//		vars.add("type");
//		return new PlanAction("Deposit" + numpeas, vars, preconditions, add,
//				delete);
//	}
//
//	public static PlanAction getBuildPeasantAction() {
//		List<Condition> preconditions = new ArrayList<>();
//		List<Condition> add = new ArrayList<>();
//		List<Condition> delete = new ArrayList<>();
//		// TODO need to somehow limit the number of peasants to 3
//		preconditions.add(new Condition("Has", new Value[] {
//				new Value(Condition.GOLD), new Value("amt", 400) }));
//		add.add(new Condition("Has", new Value[] { new Value(Condition.GOLD),
//				new Value("amt", 400, Value.Type.REMOVE) }));
//		add.add(new Condition("Numpeas", new Value[] { new Value("amt", 1,
//				Value.Type.ADD) }));
//		add.add(new Condition("Holding", new Value[] { new Value("id", 99),
//				new Value(Condition.NOTHING) }));
//		add.add(new Condition("At", new Value[] { new Value("id", 99),
//				new Value(Condition.TOWNHALL) }));
//		// BuildPeasant action is BuildPeasant()
//		return new PlanAction("BuildPeasant", new ArrayList<String>(),
//				preconditions, add, delete);
//	}
//}