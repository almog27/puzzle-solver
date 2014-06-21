/*
 * Copyright 2011 Julien Dramaix.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package com.client.puzzleSolver.logic.elements;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

public class State {
	public static class CellLocation {

		private int rowIndex;
		private int columnIndex;

		public CellLocation(int rowIndex, int columnIndex) {
			this.rowIndex = rowIndex;
			this.columnIndex = columnIndex;
		}

		public int getRowIndex() {
			return rowIndex;
		}

		public int getColumnIndex() {
			return columnIndex;
		}
	}

	public static final State GOAL_4_4;

    public static State createStartState(int[] locations) {
        int nbrOfCells = 16;
        byte[] goalCells = new byte[nbrOfCells];
        for (byte i = 0; i < goalCells.length; i++) {
            goalCells[i] = (byte)locations[i];
        }

        return new State(goalCells);
    }

	public static State createRandom() {
		State s = new State(getGoalState().getAllCells());
		Action old = null;

        Random r = new Random();
		for (int i = 0; i < 500; i++) {
			List<Action> actions = s.getPossibleActions();
			// pick an action randomly
			int index = r.nextInt(actions.size());
			Action a = actions.get(index);
			if (old != null && old.isInverse(a)){
				if (index == 0){
					index = 1;
				}else{
					index--;
				}
				a = actions.get(index);
			}
			s = a.applyTo(s);
			old = a;
		}

		return s;
	}
	
	public static State getGoalState(){
		return GOAL_4_4;
	}

	private static State createGoalState(){
		int nbrOfCells = 16;
		byte[] goalCells = new byte[nbrOfCells];
//		for (byte i = 1; i < goalCells.length; i++) {
//			goalCells[i - 1] = i;
//		}
//		goalCells[nbrOfCells - 1] = 0;

        for (byte i = 0; i < goalCells.length; i++) {
            goalCells[i] = i;
        }

		return new State(goalCells);
	}

    static {
		GOAL_4_4 = createGoalState();
	}

	private byte[] allCells;
	private int hashCode = -1;
	private final int dimension = 4;

	public State(byte[] cells) {
		// make a copy of the array
		allCells = new byte[cells.length];
		System.arraycopy(cells, 0, allCells, 0, cells.length);
	}

	public byte getCellValue(CellLocation cell) {
		return getCellValue(cell.rowIndex, cell.columnIndex);
	}
	
	public byte getCellValue(int rowIndex, int columnIndex){
		return allCells[rowIndex * dimension + columnIndex];
	}

	public void setCellValue(CellLocation cell, byte value) {
		allCells[cell.getRowIndex() * dimension + cell.getColumnIndex()] = value;
		reset();
	}

	public byte[] getAllCells() {
		return allCells;
	}

	private CellLocation getEmptyCellLocation() {
		for (int i = 0; i < allCells.length; i++) {
			if (allCells[i] == 0) {
				return new CellLocation(i/dimension, i % dimension);
			}

		}

		throw new RuntimeException("No Empty cell found");
	}

	@Override
	public boolean equals(Object obj) {
		if (obj instanceof State) {
			State s2 = (State)obj;
			return Arrays.equals(allCells, s2.allCells);
		}

		return false;
	}

	@Override
	public int hashCode() {
		if (hashCode == -1){
			int result = 17;
            for (byte allCell : allCells) {
                result = 31 * result + allCell;
            }
			hashCode = result;
		}
		
		return hashCode;
	}

	public List<Action> getPossibleActions() {
		List<Action> actions = new ArrayList<Action>();

		CellLocation emptyCell = getEmptyCellLocation();

		if (emptyCell.getRowIndex() > 0) {
			CellLocation upCell = new CellLocation(emptyCell.getRowIndex() - 1,
					emptyCell.getColumnIndex());
			actions.add(new Action(upCell, Move.DOWN));
		}

		if (emptyCell.getRowIndex() < dimension - 1) {
			CellLocation upCell = new CellLocation(emptyCell.getRowIndex() + 1,
					emptyCell.getColumnIndex());
			actions.add(new Action(upCell, Move.UP));
		}

		if (emptyCell.getColumnIndex() > 0) {
			CellLocation upCell = new CellLocation(emptyCell.getRowIndex(),
					emptyCell.getColumnIndex() - 1);
			actions.add(new Action(upCell, Move.RIGHT));
		}

		if (emptyCell.getColumnIndex() < dimension - 1) {
			CellLocation upCell = new CellLocation(emptyCell.getRowIndex(),
					emptyCell.getColumnIndex() + 1);
			actions.add(new Action(upCell, Move.LEFT));
		}

		return actions;
	}

	@Override
	public String toString() {
		return Arrays.toString(allCells);
	}

	private void reset(){
		hashCode = -1;
	}

	public int getDimension() {
		return dimension;
	}
}