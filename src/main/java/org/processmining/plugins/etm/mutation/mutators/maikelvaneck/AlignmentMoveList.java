package org.processmining.plugins.etm.mutation.mutators.maikelvaneck;

import java.util.ArrayList;

/**
 * A list of {@link AlignmentMove}s (for a node in the tree) which is nothing
 * more than a list of {@link AlignmentMove}s and some unique/sum counters
 * 
 * @author Maikel van Eck
 */
public class AlignmentMoveList {

	private ArrayList<AlignmentMove> alignmentMoves;
	private int uniqueMoveCount;
	private int uniqueSyncMoveCount;
	private int uniqueModelMoveCount;
	private int uniqueLogMoveCount;
	private int totalMoveCount;
	private int totalSyncMoveCount;
	private int totalModelMoveCount;
	private int totalLogMoveCount;

	public AlignmentMoveList() {
		alignmentMoves = new ArrayList<AlignmentMove>();
	}

	public void add(AlignmentMove alignmentMove) {
		alignmentMoves.add(alignmentMove);
		uniqueMoveCount += 1;
		totalMoveCount += alignmentMove.traceCount;

		switch (alignmentMove.moveType) {
			case AlignmentMove.SYNC :
				uniqueSyncMoveCount += 1;
				totalSyncMoveCount += alignmentMove.traceCount;
				break;
			case AlignmentMove.MODEL :
				uniqueModelMoveCount += 1;
				totalModelMoveCount += alignmentMove.traceCount;
				break;
			case AlignmentMove.LOG :
				uniqueLogMoveCount += 1;
				totalLogMoveCount += alignmentMove.traceCount;
				break;
			case AlignmentMove.LOGP :
				uniqueLogMoveCount += 1;
				totalLogMoveCount += alignmentMove.traceCount;
				break;
			default :
				assert false;
		}

		return;
	}

	public void addAll(ArrayList<AlignmentMove> logMoves) {
		for (AlignmentMove alignmentMove : logMoves) {
			add(alignmentMove);
		}

		return;
	}

	public boolean equals(AlignmentMoveList otherAlignmentMoveList) {
		ArrayList<AlignmentMove> moves = this.getAlignmentMoves();
		ArrayList<AlignmentMove> otherMoves = otherAlignmentMoveList.getAlignmentMoves();

		return moves.containsAll(otherMoves);
	}

	public ArrayList<AlignmentMove> getAlignmentMoves() {
		return alignmentMoves;
	}

	public int getUniqueMoveCount() {
		return uniqueMoveCount;
	}

	public int getUniqueSyncMoveCount() {
		return uniqueSyncMoveCount;
	}

	public int getUniqueModelMoveCount() {
		return uniqueModelMoveCount;
	}

	public int getUniqueLogMoveCount() {
		return uniqueLogMoveCount;
	}

	public int getTotalMoveCount() {
		return totalMoveCount;
	}

	public int getTotalSyncMoveCount() {
		return totalSyncMoveCount;
	}

	public int getTotalModelMoveCount() {
		return totalModelMoveCount;
	}

	public int getTotalLogMoveCount() {
		return totalLogMoveCount;
	}

	public int size() {
		return alignmentMoves.size();
	}

	public String toString() {
		return alignmentMoves.toString();
	}
}
