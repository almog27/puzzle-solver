package com.client.puzzleSolver;

/**
 * Created by Almog on 20/4/2014.
 */
public enum CalculationState {
    ORIGINAL_IMAGE(1),
    CANNY_EDGE(2),
    HOUGH_TRANSFORM(3),
    HOUGH_TRANSFORM_DRAW(4),
    PERSPECTIVE_TRANSFORM(5),
    PERSPECTIVE_TRANSFORM_SHOW(6),
    FULL_TEMPLATE_MATCHING(7),
    FULL_CALCULATION(999);

    private int state;

    CalculationState(int state) {
        this.state = state;
    }

    public int getState() {
        return state;
    }

    public boolean isStateHigher(CalculationState comparedState) {
        return comparedState.getState() < state;
    }

    public boolean isStateHigherOrEqual(CalculationState comparedState) {
        return comparedState.getState() <= state;
    }

    public static int getHigherState(CalculationState stateA, CalculationState stateB) {
        if (stateA.isStateHigher(stateB)) {
            return 1;
        } else {
            if (stateB.isStateHigher(stateA)) {
                return -1;
            } else {
                return 0;
            }
        }
    }

    public boolean isEqual(CalculationState comparedState) {
        return state == comparedState.getState();
    }

    @Override
    public String toString() {
        for (CalculationState currentState : CalculationState.values()) {
            if (currentState.getState() == this.state) {
                String stateName = currentState.name();
                stateName = stateName.replaceAll("_", " ");
                stateName = stateName.substring(0, 1).toUpperCase() +
                        stateName.substring(1).toLowerCase();
                return stateName;
            }
        }
        return null;
    }

    public boolean isDebugMode() {
        return state != CalculationState.FULL_CALCULATION.getState();
    }
}