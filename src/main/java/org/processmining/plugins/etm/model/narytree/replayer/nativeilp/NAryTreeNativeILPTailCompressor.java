package org.processmining.plugins.etm.model.narytree.replayer.nativeilp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.astar.Head;
import nl.tue.astar.impl.memefficient.StorageAwareDelegate;

import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPTailCompressor;

public class NAryTreeNativeILPTailCompressor extends AbstractNAryTreeLPTailCompressor<NAryTreeNativeILPTail> {

	private final int varBytes;
	private final int bitsPerVar;
	private final int basisSize;
	private final int bitsPerBasis;
	private final int basisBytes;

	public NAryTreeNativeILPTailCompressor(int stateBytes, int variables, short activities, int varUpBo, int basisSize) {
		super(stateBytes, variables, activities);
		int bitsPerVar = 0;
		while (1 << bitsPerVar <= varUpBo) {
			bitsPerVar++;
		}
		this.bitsPerVar = bitsPerVar;
		this.varBytes = (variables * bitsPerVar + 7) / 8;

		//---------------------------
		int bitsPerBasis = 0;
		while (1 << bitsPerBasis <= 2 * basisSize - 1) {
			bitsPerBasis++;
		}
		this.bitsPerBasis = bitsPerBasis;
		this.basisBytes = (basisSize * bitsPerBasis + 7) / 8;
		//---------------------------

		this.basisSize = basisSize;
	}

	/**
	 * This deflater is used for updating cached objects, hence the size of the
	 * tail cannot vary with the values of the variables.
	 */
	public void deflate(NAryTreeNativeILPTail object, OutputStream stream) throws IOException {

		short[] vars = object.getVariables();
		byte[] toWrite = new byte[varBytes];

		if (object.isExactEstimateKnown()) {
			writeIntToByteArray(stream, object.getEstimate());

			writeTo(toWrite, vars, bitsPerVar);
		} else {
			writeIntToByteArray(stream, -object.getEstimate() - 1);
		}

		stream.write(toWrite);

		// Basis history (TODO: optimize for space if successful)
		BasisHistory basisHistory = object.getBasisHistory();

		if (basisHistory == null) {
			stream.write(0);
		} else {
			stream.write(basisHistory.isInitialized() ? 2 : 1);

			//---------------------------
			byte[] toWriteBasis = new byte[basisBytes];
			writeTo(toWriteBasis, basisHistory.getBasis(), bitsPerBasis, basisSize);
			stream.write(toWriteBasis);
			//---------------------------

			//			int[] basis = basisHistory.getBasis();
			//			for (int i = 0; i < basisSize; ++i)
			//				writeIntToByteArray(stream, basis[i]);
		}
	}

	public NAryTreeNativeILPTail inflate(InputStream stream) throws IOException {
		int est = readIntFromStream(stream);
		boolean exact = est >= 0;
		short[] vars = new short[variables];
		if (exact) {

			byte[] toRead = new byte[varBytes];
			stream.read(toRead);

			readInto(toRead, vars, bitsPerVar);
		} else {
			est = -est - 1;

			stream.skip(varBytes);
		}

		BasisHistory basisHistory;
		int savedBasis = stream.read();

		if (savedBasis != 0) {
			basisHistory = new BasisHistory(basisSize);

			if (savedBasis == 2)
				basisHistory.setInitialized();

			int[] basis = basisHistory.getBasis();

			//---------------------------
			byte[] toRead = new byte[basisBytes];
			stream.read(toRead);
			readInto(toRead, basis, bitsPerBasis, basisSize);
			//---------------------------

			//			for (int i = 0; i < basisSize; ++i)
			//				basis[i] = readIntFromStream(stream);
		} else {
			basisHistory = null;
		}

		// read the marking
		NAryTreeNativeILPTail m = new NAryTreeNativeILPTail(est, vars, exact, basisHistory);
		return m;
	}

	public int getMaxByteCount() {
		//		return 4 + varBytes + 1 + basisSize * 4;

		//---------------------------
		return 4 + varBytes + 1 + basisBytes;
		//---------------------------
	}

	@Override
	public <H extends Head> int inflateEstimate(StorageAwareDelegate<H, NAryTreeNativeILPTail> delegate, H head,
			InputStream stream) throws IOException {
		skipHead(stream);
		int est = readIntFromStream(stream);
		//		if (est >= 0) {
		//			return est;
		//		} else {
		//			return -(est + 1);
		//		}
		return est;
	}

	/**
	 * writes a byte[] of vars into a byte[] toWrite, where each element of vars
	 * is encoded using precisely bitsPerVar bits. If bitsPerVar == 8, the
	 * result is identical to the input
	 * 
	 * @param toWrite
	 *            the byte[] to write to. The size should be at least
	 *            Math.ceil(vars.length/8*bitsPerVar)
	 * @param vars
	 *            the byte[] to encode.
	 * @param bitsPerVar
	 *            the number of bits per element of var. For all i: vars[i] < (1
	 *            << bitsPerVar)
	 */
	private void writeTo(byte[] toWrite, int[] vars, int bitsPerVar, int offset) {
		assert toWrite.length >= Math.ceil(vars.length / 8 * bitsPerVar);
		int bit = 7;
		int bte = -1;
		for (int i = 1; i < vars.length; i++) {
			// write bits
			int var = vars[i] + offset;
			assert var >= 0;
			assert var < 1 << bitsPerVar;
			for (int bt = 0; bt < bitsPerVar; bt++) {
				bte += (bit == 7 ? 1 : 0);
				toWrite[bte] |= (1 & var) << bit;
				var = var >> 1;
				bit = (bit - 1) & 7;
			}
		}

	}

	/**
	 * reads a byte[] of vars from a byte[] toRead, where each element of vars
	 * is encoded using precisely bitsPerVar bits.
	 * 
	 * @param toRead
	 *            the byte[] to read from. The size should be at least
	 *            Math.ceil(vars.length/8*bitsPerVar)
	 * @param vars
	 *            the byte[] to fill.
	 * @param bitsPerVar
	 *            the number of bits per element of var. For all i: vars[i] < (1
	 *            << bitsPerVar)
	 */
	private void readInto(byte[] toRead, int[] vars, int bitsPerVar, int offset) {
		int bit = 7;
		int bte = -1;
		for (int i = 1; i < vars.length; i++) {
			// read bits
			int var = 0;
			for (int bt = 0; bt < bitsPerVar; bt++) {
				bte += (bit == 7 ? 1 : 0);
				int read = toRead[bte] & 0xFF;
				var |= ((read >> bit) & 1) << bt;
				bit = (bit - 1) & 7;
			}
			vars[i] = var - offset;
		}

	}

}
