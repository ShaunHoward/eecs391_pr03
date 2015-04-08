# eecs391_pr03

Project 3 for EECS 391 Spring 2015 at Case Western Reserve University.

This project provides a STRIPS-like planner and plan execution agent (PEA) in order to collect various
amounts of resources in the SEPIA game engine.

Team: Shaun Howard (smh150) and Matt Swartwout (mws85)

In order to run this code you will have to have the SEPIA jar and necessary maps for the resource collection game.

Directions for setting up a possible game are here:

http://engr.case.edu/ray_soumya/sepia/html/setup.html#eclipse-project-setup

We have tested this planner and it works on all 4 given example scenarios, the highest required 3000 gold and 2000 wood with build peasants enabled.
At the end of action plan execution the program prints the time it took to execute the plan
to the console.

We imposed a limit to building peasants. We chose the limit to be 3 peasants so the
search trees would not grow too large. Therefore, when buildPeasants = true, then 3 peasants should be built throughout the game. Otherwise less will be built if less resources are necessary to be gathered and the game doesn't need 3 to win in a much shorter amount of time.

NOTE: The details of each class are outlined in comments within each class.
Those comments are probably more than enough to suit your needs, but here is some
summarized explanation:

The forward state-space planner is fairly straightfoward. Essentially what it does
is creates a possible STRIPS-like action plan to execute in SEPIA for the purpose of
resource collection by means of peasants. The game can either build multiple peasants who can harvest resources or it can have a single peasant do so if the
amount of resources (gold and wood) desired is small enough. 

The planner is designed in a general way. One could put in any given number of peasants and it may work, but search will take a while with more peasants due
to branching factor. Hence, we limit the number of peasants to 3 for our purposes.
This also allows our plan search to complete in finite time, before the user could possibly start the game.

We use A* search to find the plan. A* is depth-limited to a depth of 140. We found this
to be a good limit for our plan when it took longer to execute, but given our limit of peasants
which is 3, the search completes before it reaches a depth of 140 in the search tree. After optimizations,
we did not need this parameter anymore, but we think it is nice to have it in case someone tries to run the game with >3 peasants.
In this case the planner will just return the plan with the best solution thus far, although it will not necesarily
be complete. A depth of 140 was chosen because after that the search could continue for a long time and not be playable
for while.

We take into consideration the make span of each
action along with the perks or detriments it will bring to the game. We thus search for actions with the minimum make span and best total cost
because we want to spend the least time executing actions possible but get the most value out of our actions.
This means we maximize actions that will lead us to our goal, which is when the current game state (or the peasant's collection) has the desired amount of gold and wood.
Our heuristic for a game state (with an associated parent action) is as follows:

The heuristic utilizes properties of this game state to determine the most probable distance to the desired goal state. Some values considered are:
	 
* the number of peasants at the goal vs the number of peasants in this state
* the number of cycles the peasants will take to gather gold
* the number of cycles the peasants will take to gather wood
* the number of steps needed to gather resources
* the amount of wood gathered at the current state

Altogether, considering these values provides a fairly accurate heuristic.
It considers when the game needs to build peasants, when the peasants
should choose one resource over another due to distance and cycles necessary
to gather the resources, and finally, the amount of wood at the current state 
helps the peasants find the goal state sooner since wood has less priority as 
gold and needs to be factored in the heuristic.

The plan, after A* finds a plan, is printed to a text file located in the default given location which is in the
top level project folder along with src named "saves". The plan is in this folder in a text file named "plan.txt".
This plan shows each action along with its peasant count and variables it uses or is applied to.
The plan actions are number from 1 (the start) to n (the final) in ascending order.

Each action has its own class with an associated make span and all actions are a type of StripsAction. They all have methods to check preconditions for a given
game state, to apply the action to a game state, and for getting the make span. The
make span of an action is typically 1 but for move actions it is the distance between the given resource and the town hall, if the start location is a resource.

Each action also has its own preconditions to be executed and the effects it produces on a given game state. We outline both for each action as follows:

MoveAction
* The preconditions for this move vary based on destination.
* The preconditions are that the given number of peasants are adjacent to a resource or town hall node. 
* The effects are that the specified number of peasants end up near either the town hall or a resource given the origin of the move action.

HarvestAction

* The preconditions for this action are that the peasants are adjacent to a resource node, the resource can be gathered, and the peasants are not carrying any cargo yet.
* The effects for this action are that the resource will have (100 * number of peasants) less value, and the peasants that gathered will have 100 of the resource each.

DepositAction

* The preconditions for this action are that the desired number of peasants are adjacent to the town hall and each have cargo.
 
* The effects of this action are that the peasants will have deposited (100 * the number of peasants)-worth in gold and/or wood to the town hall.

BuildPeasantAction

* The preconditions are that there are fewer than 3 peasants already playing in the game (this is our global limit) and that the peasants have at least 400 gold collected.
* The effects are that there is one more peasant added to the game and the peasants now have 400 less gold.

We made our our peasant and resource classes to track the peasants and resources during planning in our own state generator. These serve the purpose to make the game state track the way the game would evaluate in SEPIA. We needed to take the initial values and ids of game units from sepia and apply the possible actions to them in order to find the best set of actions. This set of actions turns into our plan of Strips actions. We translate our game states to strips actions and then to sepia actions in the PEAgent. Essentially what this does is extract the parent action from each game state and execute that action in order from initial game state + 1 to final game state. Then the game should be won by the time the last action is executed. 

Extra notes:

Our A* search actually searches properly this time. It will re-evaluate game states if a better tentative score is found during execution of search.
In this way we try to minimze the number of states searched as well as the branching factor of our graph. This helps the search complete in reasonable time
where the game can be played once it loads the GUI. We used a priority queue for the open set and compare game states based on their total cost.
The equals and hashcode methods of the game state evaluates all aspects of the game state that matter so that states are considered unique unless almost completely identical. This allows search to speed up tremendously and eliminate the repeated checking of already expanded states.

As you can see in the code, we do not utilize the Position class although it would have been much simpler, which we later realized. But what is important is that the
planner works on all the given maps and is very fast and efficient. The execution times for the action plans are very small, which is quite impressive to us. We put a lot of effort into this project and we hope you enjoy it! 
