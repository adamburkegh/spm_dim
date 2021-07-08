package org.processmining.plugins.etm.model.narytree.replayer.hybridilp;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import nl.tue.astar.Head;
import nl.tue.astar.impl.memefficient.StorageAwareDelegate;

import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPTailCompressor;

public class NAryTreeHybridILPTailCompressor extends AbstractNAryTreeLPTailCompressor<NAryTreeHybridILPTail> {

	private final int varBytes;
	private final int bitsPerVar;

	public NAryTreeHybridILPTailCompressor(int stateBytes, int variables, short activities, int varUpBo) {
		super(stateBytes, variables, activities);
		int bitsPerVar = 0;
		while (1 << bitsPerVar <= varUpBo) {
			bitsPerVar++;
		}
		this.bitsPerVar = bitsPerVar;
		this.varBytes = (variables * bitsPerVar + 7) / 8;
	}

	/**
	 * This deflater is used for updating cached objects, hence the size of the
	 * tail cannot vary with the values of the variables.
	 */
	public void deflate(NAryTreeHybridILPTail object, OutputStream stream) throws IOException {

		short[] vars = object.getVariables();
		byte[] toWrite = new byte[varBytes];

		if (object.isExactEstimateKnown()) {
			writeIntToByteArray(stream, object.getEstimate());

			writeTo(toWrite, vars, bitsPerVar);
		} else {
			writeIntToByteArray(stream, -object.getEstimate() - 1);
		}

		stream.write(toWrite);

	}

	public NAryTreeHybridILPTail inflate(InputStream stream) throws IOException {
		int est = readIntFromStream(stream);
		boolean exact = est >= 0;
		short[] vars = new short[variables];
		if (exact) {

			byte[] toRead = new byte[varBytes];
			stream.read(toRead);

			readInto(toRead, vars, bitsPerVar);
		} else {
			est = -est - 1;
		}

		// read the marking
		NAryTreeHybridILPTail m = new NAryTreeHybridILPTail(est, vars, exact);
		return m;
	}

	public int getMaxByteCount() {
		return 4 + varBytes;
	}

	@Override
	public <H extends Head> int inflateEstimate(StorageAwareDelegate<H, NAryTreeHybridILPTail> delegate, H head,
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

	//	public static void main(String[] args) {
	//		short[] vars = new short[] { 52, 32, 24, 22, 12, 10 };
	//
	//		short[] varsRead = new short[vars.length];
	//
	//		int bitsPerVar = getBitsPerVar(vars);
	//
	//		byte[] stream = new byte[(vars.length * bitsPerVar + 4) / 8];
	//
	//		System.out.println(Arrays.toString(vars));
	//
	//		writeTo(stream, vars, bitsPerVar);
	//		System.out.println(Arrays.toString(stream));
	//
	//		readInto(stream, varsRead, bitsPerVar);
	//
	//		System.out.println(Arrays.toString(varsRead));
	//
	//		System.out.println(Arrays.equals(vars, varsRead));
	//
	//	}
}
