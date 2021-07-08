package org.processmining.plugins.etm.model.narytree.replayer.basisilp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.astar.Head;
import nl.tue.astar.impl.memefficient.StorageAwareDelegate;

import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPTailCompressor;

public class NAryTreeBasisILPTailCompressor extends AbstractNAryTreeLPTailCompressor<NAryTreeBasisILPTail> {

	private final int varBytes;
	private final int bitsPerVar;
	private final int bitsPerBasis;
	private final int basisBytes;
	private final int basisLength;

	public NAryTreeBasisILPTailCompressor(int stateBytes, int variables, short activities, int varUpBo, int basisLength) {
		super(stateBytes, variables, activities);
		this.basisLength = basisLength;
		int bitsPerVar = 0;
		while (1 << bitsPerVar <= varUpBo) {
			bitsPerVar++;
		}
		this.bitsPerVar = bitsPerVar;
		this.varBytes = (variables * bitsPerVar + 7) / 8;

		int bitsPerBasis = 0;
		while (1 << bitsPerBasis <= 2 * basisLength - 1) {
			bitsPerBasis++;
		}
		this.bitsPerBasis = bitsPerBasis;
		this.basisBytes = (basisLength * bitsPerBasis + 7) / 8;
	}

	/**
	 * This deflater is used for updating cached objects, hence the size of the
	 * tail cannot vary with the values of the variables.
	 */
	public void deflate(NAryTreeBasisILPTail object, OutputStream stream) throws IOException {

		short[] vars = object.getVariables();
		int[] basis = object.getBasis();
		byte[] toWriteBasis = new byte[basisBytes];
		byte[] toWriteVar = new byte[varBytes];

		if (object.isExactEstimateKnown()) {
			writeIntToByteArray(stream, object.getEstimate());

			// write the definitive basis
			writeTo(toWriteBasis, basis, bitsPerBasis, basisLength);
			// write the variables
			writeTo(toWriteVar, vars, bitsPerVar);
		} else {
			writeIntToByteArray(stream, -object.getEstimate() - 1);
			// write the preliminary basis
			writeTo(toWriteBasis, basis, bitsPerBasis, basisLength);
			// skip the variables
		}

		stream.write(toWriteBasis);
		stream.write(toWriteVar);

	}

	public NAryTreeBasisILPTail inflate(InputStream stream) throws IOException {
		int est = readIntFromStream(stream);
		boolean exact = est >= 0;

		int[] basis = new int[basisLength];
		byte[] toRead = new byte[basisBytes];
		stream.read(toRead);

		readInto(toRead, basis, bitsPerBasis, basisLength);

		short[] vars = new short[variables];
		if (exact) {

			toRead = new byte[varBytes];
			stream.read(toRead);

			readInto(toRead, vars, bitsPerVar);
		} else {
			est = -est - 1;
		}

		// read the marking
		NAryTreeBasisILPTail m = new NAryTreeBasisILPTail(est, vars, exact);
		m.setBasis(basis);
		return m;
	}

	public int getMaxByteCount() {
		return 4 + varBytes + basisBytes;
	}

	@Override
	public <H extends Head> int inflateEstimate(StorageAwareDelegate<H, NAryTreeBasisILPTail> delegate, H head,
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
