package qut.pm.spm.measures;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.model.XEvent;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;

public class LogStatsCache {

	private static class Key{
		private XLog log;
		private XEventClassifier classifier;

		public Key(XLog log, XEventClassifier classifier) {
			this.log = log;
			this.classifier = classifier;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((classifier == null) ? 0 : classifier.hashCode());
			result = prime * result + ((log == null) ? 0 : log.hashCode());
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
			if (classifier == null) {
				if (other.classifier != null)
					return false;
			} else if (classifier != other.classifier)
				return false;
			if (log == null) {
				if (other.log != null)
					return false;
			} else if (log != other.log)
				return false;
			return true;
		}
				
	}
	
	protected Map<Key,LogStats> cache = new HashMap<>();
	
	public LogStats getStats(XLog log, XEventClassifier classifier) {
		Key key = new Key(log,classifier);
		LogStats existing = cache.get(key);
		if (existing != null)
			return existing;
		LogStats result = calculateStats(log,classifier);
		cache.put(key,result);
		return result;
	}

	private LogStats calculateStats(XLog log, XEventClassifier classifier) {
		Set<List<String>> uniqTraces = new HashSet<>();
		for (XTrace trace: log) {
			LinkedList<String> newTrace = new LinkedList<String>();
			for (XEvent event: trace) {
				String classId = classifier.getClassIdentity(event);
				newTrace.add(classId);
			}
			uniqTraces.add(newTrace);
		}
		LogStats result = new LogStats(log,classifier,uniqTraces.size());
		return result;
	}
	
}
