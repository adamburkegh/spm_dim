package org.processmining.plugins.etm.model.narytree.replayer.fullilp;

import java.io.IOException;
import java.io.InputStream;

import org.processmining.plugins.etm.model.narytree.replayer.AbstractNAryTreeLPTailCompressor;

public class NAryTreeFullILPTailCompressor extends AbstractNAryTreeLPTailCompressor<NAryTreeFullILPTail> {

	public NAryTreeFullILPTailCompressor(int stateBytes, int variables, short activities) {
		super(stateBytes, variables, activities);
	}

	public NAryTreeFullILPTail inflate(InputStream stream) throws IOException {
		int est = readIntFromStream(stream);

		// read the marking
		NAryTreeFullILPTail m = new NAryTreeFullILPTail(est, readVarsDoubleBitmask(stream));
		return m;
	}
}
