package edu.cwru.sepia.agent.planner.actions;

import edu.cwru.sepia.agent.planner.GameState;

/**
 * An interface representing a STRIPS-line action.
 * Checks if the action meets the preconditions to reach the goal state given the current state.
 * Applies this action to the given game state.
 * Has an associated make span which depends on the actual time sepia needs to apply action.
 * 
 * @author Shaun Howard, Matt Swartwout
 */
public interface StripsAction {

    /**
     * Returns true if the provided GameState meets all of the necessary conditions for this action to successfully
     * execute.
     *
     * As an example consider a Move action that moves peasant 1 in the NORTH direction. The partial game state might
     * specify that peasant 1 is at location (3, 3). In this case the game state shows that nothing is at location (3, 2)
     * and (3, 2) is within bounds. So the method returns true.
     *
     * If the peasant were at (3, 0) this method would return false because the peasant cannot move to (3, -1).
     *
     * @param state GameState to check if action is applicable
     * @return true if apply can be called, false otherwise
     */
    public boolean preconditionsMet(GameState state, GameState goal);

    /**
     * Applies the action instance to the given GameState producing a new GameState in the process.
     *
     * As an example consider a Move action that moves peasant 1 in the NORTH direction. The partial game state
     * might specify that peasant 1 is at location (3, 3). The returned GameState should specify
     * peasant 1 at location (3, 2).
     *
     * In the process of updating the peasant state you should also update the GameState's cost and parent pointers.
     *
     * @param state State to apply action to
     * @return State resulting from successful action application.
     */
    public GameState apply(GameState state);
    
    /**
     * Gets the make span of this action in order to calculate an
     * effective heuristic.
     * 
     * @return the make span of the action, i.e the time it takes to execute
     */
    public int getMakeSpan();
}
