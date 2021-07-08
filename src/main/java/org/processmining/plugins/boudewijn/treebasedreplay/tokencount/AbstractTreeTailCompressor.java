package org.processmining.plugins.boudewijn.treebasedreplay.tokencount;

import java.io.IOException;
import java.io.InputStream;

import nl.tue.astar.Tail;
import nl.tue.astar.impl.AbstractCompressor;
import nl.tue.storage.compressor.BitMask;


public abstract class AbstractTreeTailCompressor<T extends Tail> extends AbstractCompressor<T> {

	protected final short activities;
	protected int nodeMaskSize;
	protected final int activityBytes;

	public AbstractTreeTailCompressor(short nodes, short activities, short leafs) {
		this.activities = activities;
		this.nodeMaskSize = BitMask.getNumBytes(nodes);
		this.activityBytes = BitMask.getNumBytes(activities);
		// we use double bitmasks
	}

	public void skipHead(InputStream stream) throws IOException {
		//skip hashCode
		stream.skip(4);
		// skip enabled
		stream.skip(nodeMaskSize);
		// skip future
		stream.skip(nodeMaskSize);
		// skip parikh
		byte[] bitmask1 = readMask(stream, activities, activityBytes).getBytes();
		byte[] bitmask2 = readMask(stream, activities, activityBytes).getBytes();
		for (int i = 0; i < bitmask1.length; i++) {
			byte b1 = bitmask1[i];
			byte b2 = bitmask2[i];
			for (int j = 0; j < 8; j++) {
				int one1 = (b1 & POWER[j]);
				int one2 = (b2 & POWER[j]);
				if (one1 > 0 && one2 > 0) {
					stream.skip(2);
				}
			}
		}
	}


}