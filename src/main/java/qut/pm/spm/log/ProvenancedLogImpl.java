package qut.pm.spm.log;

import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.ListIterator;
import java.util.Set;

import org.deckfour.xes.classification.XEventClassifier;
import org.deckfour.xes.extension.XExtension;
import org.deckfour.xes.info.XLogInfo;
import org.deckfour.xes.model.XAttribute;
import org.deckfour.xes.model.XAttributeMap;
import org.deckfour.xes.model.XLog;
import org.deckfour.xes.model.XTrace;
import org.deckfour.xes.model.XVisitor;

public class ProvenancedLogImpl implements ProvenancedLog{

	// A decorated XLog that can tell you what file it was loaded from 
	protected XLog log;
	protected String logFilePath;
	
	
	public ProvenancedLogImpl(XLog log, String logFilePath) {
		this.log = log;
		this.logFilePath = logFilePath;
	}

	@Override
	public String getLogFilePath() {
		return logFilePath;
	}

	@Override
	public int size() {
		return log.size();
	}

	@Override
	/**
	 * Pre: info already created
	 */
	public XLogInfo getInfo(XEventClassifier classifier) {
		return log.getInfo(classifier);
	}

	@Override
	public boolean accept(XVisitor arg0) {
		return log.accept(arg0);
	}

	@Override
	public List<XEventClassifier> getClassifiers() {
		return log.getClassifiers();
	}

	@Override
	public List<XAttribute> getGlobalEventAttributes() {
		return log.getGlobalEventAttributes();
	}

	@Override
	public List<XAttribute> getGlobalTraceAttributes() {
		return log.getGlobalTraceAttributes();
	}

	@Override
	public void setInfo(XEventClassifier arg0, XLogInfo arg1) {
		log.setInfo(arg0, arg1);
	}

	@Override
	public XAttributeMap getAttributes() {
		return log.getAttributes();
	}

	@Override
	public Set<XExtension> getExtensions() {
		return log.getExtensions();
	}

	@Override
	public boolean hasAttributes() {
		return log.hasAttributes();
	}

	@Override
	public void setAttributes(XAttributeMap arg0) {
		log.setAttributes(arg0);
	}

	@Override
	public boolean isEmpty() {
		return log.isEmpty();
	}

	@Override
	public boolean contains(Object o) {
		return log.contains(o);
	}

	@Override
	public Iterator<XTrace> iterator() {
		return log.iterator();
	}

	@Override
	public Object[] toArray() {
		return log.toArray();
	}

	@Override
	public <T> T[] toArray(T[] a) {
		return log.toArray(a);
	}

	@Override
	public boolean add(XTrace e) {
		return log.add(e);
	}

	@Override
	public boolean remove(Object o) {
		return log.remove(o);
	}

	@Override
	public boolean containsAll(Collection<?> c) {
		return log.containsAll(c);
	}

	@Override
	public boolean addAll(Collection<? extends XTrace> c) {
		return log.addAll(c);
	}

	@Override
	public boolean addAll(int index, Collection<? extends XTrace> c) {
		return log.addAll(index,c);
	}

	@Override
	public boolean removeAll(Collection<?> c) {
		return log.removeAll(c);
	}

	@Override
	public boolean retainAll(Collection<?> c) {
		return log.retainAll(c);
	}

	@Override
	public void clear() {
		log.clear();
	}

	@Override
	public XTrace get(int index) {
		return log.get(index);
	}

	@Override
	public XTrace set(int index, XTrace element) {
		return log.set(index, element);
	}

	@Override
	public void add(int index, XTrace element) {
		log.add(index,element);
	}

	@Override
	public XTrace remove(int index) {
		return log.remove(index);
	}

	@Override
	public int indexOf(Object o) {
		return log.indexOf(o);
	}

	@Override
	public int lastIndexOf(Object o) {
		return log.lastIndexOf(o);
	}

	@Override
	public ListIterator<XTrace> listIterator() {
		return log.listIterator();
	}

	@Override
	public ListIterator<XTrace> listIterator(int index) {
		return log.listIterator();
	}

	@Override
	public List<XTrace> subList(int fromIndex, int toIndex) {
		return log.subList(fromIndex, toIndex);
	}
	
	@Override
	public Object clone() {
		XLog nlog = (XLog)log.clone();
		ProvenancedLogImpl plog = new ProvenancedLogImpl(nlog,getLogFilePath());
		return plog;
	}
	
}
