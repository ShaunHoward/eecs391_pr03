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
	private int resX; // x coordinate of resource node
	private int resY; // y coordinate of resource node

	public GatherAction(int k, Integer targetId, int x, int y) {
		this.peasantCount = k;
		this.targetId = targetId;
		this.resX = x;
		this.resY = y;
	}

	@Override
	public boolean preconditionsMet(GameState s, GameState goal) {
		int i = 0;
		if (peasantCount <= s.peasants.size()
				&& s.getResourceWithId(targetId).getAmount() >= peasantCount*100) {
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
		for (PlanPeasant peasant : newState.peasants) {
			if (isValid(peasant) 
					&& i++ < peasantCount
					&& res.getAmount() >= 100) {
				int value = res.gather();
				if (value > 0) {
					peasant.setCargo(res.getType());
					peasant.setCargoAmount(value);
				} else {
					peasant.setCargoAmount(0);
					peasant.setCargo(null);
				}
			}
		}
		newState.parentAction = this;
		return newState;
	}

	public int getPeasantCount() {
		return peasantCount;
	}

	public int getTargetId() {
		return targetId;
	}

	public int getResourceX() {
		return resX;
	}

	public int getResourceY() {
		return resY;
	}

	private boolean isValid(PlanPeasant peasant) {
		return peasant.getNextTo() != null
				&& peasant.getNextTo().getId() == targetId
				&& peasant.getCargo() == null;
	}

	@Override
	public int getMakeSpan() {
		return 1;
	}

	@Override
	public String toString() {
		return "GATHER(k:" + peasantCount + ", id:" + targetId + ")";
	}
	
	/**
	 * Determines if two gather actions are equal based on their target ids and peasant count.
	 * 
	 * @return true if the two gather actions are equal
	 */
	@Override
	public boolean equals(Object o){
		if (o != null && o instanceof GatherAction){
			GatherAction a = (GatherAction)o;
			return a.targetId == this.targetId &&
					a.peasantCount == this.peasantCount;
		}
		return false;
	}
}
