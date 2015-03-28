package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.PlanPeasant;
import edu.cwru.sepia.agent.planner.PlanResource;
import edu.cwru.sepia.agent.planner.GameState;

public class GatherAction implements StripsAction {

	/**
	 * Gather resource Preconditions: next to resource node, node not empty, and
	 * not carrying cargo Effects: resource amount - 100 and carrying cargo
	 * Makespan: 1
	 **/

	private int peasantCount; // nuumber of peasants to operate on
	private Integer targetId; // id of resource node
	private int x; // x coordinate of resource node
	private int y; // y coordinate of resource node

	public GatherAction(int k, Integer targetId, int x, int y) {
		this.peasantCount = k;
		this.targetId = targetId;
		this.x = x;
		this.y = y;
	}

	@Override
	public boolean preconditionsMet(GameState s, GameState goal) {
		int i = 0;
		if (peasantCount <= s.peasants.size()
				&& s.getResourceWithId(targetId).getAmount() > 0) {
			for (PlanPeasant peasant : s.peasants) {
				if (isValid(peasant) && ++i == peasantCount){
					return true;
				}
			}
		}
		return false;
	}

	@Override
	public GameState apply(GameState s) {
		int i = 0;
		GameState newState = new GameState(s);
		PlanResource res = newState.getResourceWithId(targetId);
		// if (res.getAmount() >= 100){
		for (PlanPeasant peasant : newState.peasants) {
			if (isValid(peasant) && i++ < peasantCount
					&& res.getAmount() >= 100) {
				int value = res.gather();
				if (value > 0) {
					peasant.setCargo(res.getType());
					peasant.setCargoAmount(value);
				} else {
					peasant.setCargo(null);
				}
			}
		}
		// }
		newState.parentAction = this;
		return newState;
	}

	public int getK() {
		return peasantCount;
	}

	public int getTargetId() {
		return targetId;
	}

	public int getX() {
		return x;
	}

	public int getY() {
		return y;
	}

	private boolean isValid(PlanPeasant peasant) {
		return peasant.getNextTo() != null
				&& peasant.getNextTo().getId() == targetId
				&& peasant.getCargo() == null;
	}

	@Override
	public int getMakeSpan() {
		return -peasantCount;
	}

	@Override
	public String toString() {
		return "GATHER(k:" + peasantCount + ", id:" + targetId + ")";
	}
}
