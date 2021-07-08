package org.processmining.plugins.etm.model.narytree.replayer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.astar.Head;
import nl.tue.astar.impl.AbstractCompressor;
import nl.tue.astar.impl.memefficient.StorageAwareDelegate;
import nl.tue.astar.impl.memefficient.TailInflater;
import nl.tue.storage.compressor.BitMask;

public abstract class AbstractNAryTreeLPTailCompressor<T extends AbstractNAryTreeLPTail> extends AbstractCompressor<T>
		implements TailInflater<T> {

	protected final int variableBytes;
	protected final int variables;
	protected final int maxBytes;
	protected final int stateBytes;
	protected final short activities;
	protected final int activityBytes;

	public AbstractNAryTreeLPTailCompressor(int stateBytes, int variables, short activities) {
		this.stateBytes = stateBytes;
		this.variables = variables;
		this.activities = activities;
		this.activityBytes = BitMask.getNumBytes(activities);
		this.variableBytes = BitMask.getNumBytes(variables);
		this.maxBytes = 4 + 2 * variableBytes + variables * 2;
	}

	public void deflate(T object, OutputStream stream) throws IOException {
		writeIntToByteArray(stream, object.getEstimate());

		writeVarsDoubleBitMask(stream, object.getVariables());
	}

	public abstract T inflate(InputStream stream) throws IOException;

	protected void writeVarsDoubleBitMask(OutputStream stream, short[] vars) throws IOException {

		assert vars.length == variables;
		byte[] bitmask1 = new byte[variableBytes];
		byte[] bitmask2 = new byte[variableBytes];
		short[] greater = new short[variables];
		//iterating over all elements if possible indices is faster than 
		// first getting the relevant keys.
		int v = 0;
		for (int a = 0; a < variables; a++) {
			int i = vars[a] & 0xFFFF;
			if (i == 0) {
				continue;
			}
			int bte = a / 8;
			int bit = a % 8;
			if (i == 1) {
				bitmask1[bte] |= POWER[bit];

			} else if (i == 2) {
				bitmask2[bte] |= POWER[bit];

			} else if (i > 2) {
				bitmask1[bte] |= POWER[bit];
				bitmask2[bte] |= POWER[bit];
				greater[v++] = vars[a];
			}
		}
		stream.write(bitmask1);
		stream.write(bitmask2);
		for (int i = 0; i < v; i++) {
			writeShortToByteArray(stream, greater[i]);
		}
		//		System.out.println("written:" + Arrays.toString(bitmask1) + Arrays.toString(bitmask2));
	}

	protected short[] readVarsDoubleBitmask(InputStream stream) throws IOException {
		short[] vars = new short[variables];
		byte[] bitmask1 = readMask(stream, variables, variableBytes).getBytes();
		byte[] bitmask2 = readMask(stream, variables, variableBytes).getBytes();

		for (int i = 0; i < variableBytes; i++) {
			byte b1 = bitmask1[i];
			byte b2 = bitmask2[i];
			for (int j = 0; j < 8; j++) {
				int one1 = (b1 & (byte) POWER[j]);
				int one2 = (b2 & (byte) POWER[j]);
				if (one1 != 0 && one2 == 0) {
					assert i * 8 + j < variables;
					vars[i * 8 + j] = 1;
				} else if (one1 == 0 && one2 != 0) {
					assert i * 8 + j < variables;
					vars[i * 8 + j] = 2;
				} else if (one1 != 0 && one2 != 0) {
					assert i * 8 + j < variables;
					vars[i * 8 + j] = readShortFromStream(stream);
					assert (vars[i * 8 + j] & 0xFFFF) > 2;
				}
			}
		}
		//		System.out.println("read:   " + Arrays.toString(bitmask1) + Arrays.toString(bitmask2));

		return vars;
	}

	public int getMaxByteCount() {
		return maxBytes;
	}

	public <H extends Head> int inflateEstimate(StorageAwareDelegate<H, T> delegate, H head, InputStream stream)
			throws IOException {
		skipHead(stream);
		return readIntFromStream(stream);
	}

	public void skipHead(InputStream stream) throws IOException {
		//skip hashCode
		stream.skip(4);
		// skip state
		stream.skip(stateBytes);
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
