package com.client.puzzleSolver.logic.computation;

import com.client.puzzleSolver.logic.computation.algorithm.IDAStar;
import com.client.puzzleSolver.logic.computation.heuristic.ManhattanDistance;
import com.client.puzzleSolver.logic.elements.Node;
import com.client.puzzleSolver.logic.elements.PuzzleSolution;
import com.client.puzzleSolver.logic.elements.State;

/**
 * Created by Almog on 21/5/2014.
 */
public class PuzzleSolverCalculator {
    final static IDAStar searchAlgorithm;
    final static ManhattanDistance heuristic;

    static {
        searchAlgorithm = new IDAStar();
        heuristic = new ManhattanDistance();
        searchAlgorithm.setHeuristic(heuristic);
    }

    public static Node solve(State startState) {
        State goalState = State.getGoalState();
        long startTime = System.currentTimeMillis();
        PuzzleSolution solution = searchAlgorithm.resolve(startState, goalState);
        long endTime = System.currentTimeMillis();
        System.out.println("Logic calculation time: " + (endTime-startTime)/1000);

        return solution.getPath().getNodes()[1];
    }

}
