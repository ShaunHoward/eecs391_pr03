package edu.cwru.sepia.agent.planner;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class Condition {
	public static final Condition HOLDING = new Condition("Holding",
			new Value[] { new Value("first"), new Value("type") });
	public static final Condition AT = new Condition("At", new Value[] {
			new Value("first"), new Value("pos") });
	public static final Condition HAS = new Condition("Has", new Value[] {
			new Value("type"), new Value("amt", 0, Value.Type.ADD) });
	public static final Condition CONTAINS = new Condition("Contains",
			new Value[] { new Value("pos"), new Value("type") });
	public static final Condition HALL = new Condition("Hall",
			new Value[] { new Value("pos") });
	public static final Condition NUMPEAS = new Condition("Numpeas",
			new Value[] { new Value("amt") });
	public static final Value NOTHING = new Value("type", 10,
			Value.Type.CONSTANT);
	public static final Value GOLD = new Value("type", 11, Value.Type.CONSTANT);
	public static final Value WOOD = new Value("type", 12, Value.Type.CONSTANT);
	public static final Value TOWNHALL = new Value("pos", 13,
			Value.Type.CONSTANT);
	public static final Value GOLDMINE = new Value("pos", 14,
			Value.Type.CONSTANT);
	public static final Value FOREST = new Value("pos", 15, Value.Type.CONSTANT);
	private String name;
	private List<Value> variables;

	public Condition(String name, Value[] variables) {
		this.name = name;
		this.variables = new ArrayList<>();
		for (Value value : variables) {
			this.variables.add(new Value(value));
		}
	}

	public Condition(Condition other, List<Value> values) {
		this.name = other.name;
		this.variables = new ArrayList<>();
		for (int i = 0; i < other.variables.size(); i++) {
			Value value = new Value(other.variables.get(i));
			for (Value val : values) {
				if (value.getName().equals(val.getName())) {
					value.updateValue(val);
					break;
				}
			}
			this.variables.add(value);
		}
	}

	public String getName() {
		return name;
	}

	public List<Value> getVariables() {
		return variables;
	}

	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (o == this) {
			return true;
		}
		if (!(o instanceof Condition)) {
			return false;
		}
		Condition cond = (Condition) o;
		if (!name.equals(cond.name)) {
			return false;
		} else if (variables.size() != cond.variables.size()) {
			return false;
		}
		for (int i = 0; i < variables.size(); i++) {
			Value v1 = variables.get(i);
			Value v2 = cond.variables.get(i);
			if (!v1.equals(v2)) {
				return false;
			}
		}
		return true;
	}

	public Value getValue(String key) {
		for (Value v : variables) {
			if (v.getName().equalsIgnoreCase(key)) {
				return v;
			}
		}
		return null;
	}

	public Value getValue(int index) {
		return variables.get(index);
	}

	public boolean usesOnly(Set<String> variables) {
		for (Value var : this.variables) {
			if (!variables.contains(var.getName())) {
				return false;
			}
		}
		return true;
	}

	@Override
	public String toString() {
		String temp = name + "(";
		for (int i = 0; i < variables.size(); i++) {
			temp = temp + variables.get(i).toString();
			if (i < variables.size() - 1) {
				temp = temp + ",";
			}
		}
		return temp + ")";
	}
}
