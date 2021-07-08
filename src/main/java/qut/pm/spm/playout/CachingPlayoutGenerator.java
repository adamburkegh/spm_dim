package qut.pm.spm.playout;

import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.deckfour.xes.model.XLog;

import qut.pm.spm.AcceptingStochasticNet;

public class CachingPlayoutGenerator implements PlayoutGenerator{

	private static Logger LOGGER = LogManager.getLogger();
	
	private static class Key{
		public AcceptingStochasticNet net;
		public Integer targetSize;
			
		public Key(AcceptingStochasticNet net, Integer targetSize) {
			super();
			this.net = net;
			this.targetSize = targetSize;
		}
		
		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((net == null) ? 0 : net.hashCode());
			result = prime * result + ((targetSize == null) ? 0 : targetSize.hashCode());
			return result;
		}
		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			Key other = (Key) obj;
			if (net == null) {
				if (other.net != null)
					return false;
			} else if (!net.equals(other.net))
				return false;
			if (targetSize == null) {
				if (other.targetSize != null)
					return false;
			} else if (!targetSize.equals(other.targetSize))
				return false;
			return true;
		}
				
	}
	
	private PlayoutGenerator generator = new StochasticPlayoutGenerator();
	private Map<Key,XLog> cache = Collections.synchronizedMap( new WeakHashMap<>() );
	
	@Override
	public XLog buildPlayoutLog(AcceptingStochasticNet net, int targetSize) {
		LOGGER.debug("Checking cached playout log");
		Key key = new Key(net,targetSize);
		XLog result = cache.get( key );
		if (result == null) {
			result = generator.buildPlayoutLog(net,targetSize);
			cache.put(key,result);
		}else {
			LOGGER.debug("Using cached playout log");
		}
		return result;
	}

}
