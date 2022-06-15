package qut.pm.spm.ppt;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.processmining.models.graphbased.directed.petrinet.StochasticNet;
import org.processmining.models.graphbased.directed.petrinet.elements.Place;
import org.processmining.models.graphbased.directed.petrinet.elements.TimedTransition;
import org.processmining.models.graphbased.directed.petrinet.impl.StochasticNetImpl;
import org.processmining.models.semantics.petrinet.Marking;

import qut.pm.spm.AcceptingStochasticNet;
import qut.pm.spm.AcceptingStochasticNetImpl;

public class ProbProcessTreeConverter {

	private static final String TAU = "tau";


	public AcceptingStochasticNet convertToSNet(ProbProcessTree ppt) {
		return convertToSNet(ppt,ppt.getLabel());
	}
	
	public AcceptingStochasticNet convertToSNet(ProbProcessTree ppt, String label) {
		if (!ProbProcessTreeCheck.checkConsistent(ppt))
			throw new ProcessTreeConsistencyException("Inconsistent ppt");
		StochasticNet net = new StochasticNetImpl(label);
		Place start = net.addPlace("Start");
		Place end = net.addPlace("End");
		Marking initial = new Marking( Collections.singleton(start) );
		Set<Marking> finalMarking = Collections.singleton(new Marking(Collections.singleton(end)));
		convert(ppt,net,start,end);
		AcceptingStochasticNet result = new AcceptingStochasticNetImpl(net,initial,finalMarking);
		return result ;
	}

	private void convert(ProbProcessTree ppt, StochasticNet net, Place localStart, Place localEnd) {
		// considered a dedicated enum for node type
		// didn't want the converter inside the PPT classes themselves
		// more functional style
		// this is ugly though
		if (ppt instanceof ProbProcessTreeLeaf ) {
			convertLeaf((ProbProcessTreeLeaf) ppt,net,localStart,localEnd);
			return;
		}
		if (ppt instanceof ProbProcessTreeNode) {
			convertNode((ProbProcessTreeNode) ppt,net,localStart,localEnd);
			return;
		}
		// Silent
		convertSilent(ppt,net,localStart,localEnd);
	}
	
	private void convertLeaf(ProbProcessTreeLeaf ppt, StochasticNet net, Place localStart, Place localEnd) {
		TimedTransition t1 = net.addImmediateTransition(ppt.getActivity(),ppt.getWeight());
		net.addArc(localStart,t1);
		net.addArc(t1,localEnd);
	}

	private void convertSilent(ProbProcessTree ppt, StochasticNet net, Place localStart, Place localEnd) {
		TimedTransition t1 = net.addImmediateTransition(TAU,ppt.getWeight());
		t1.setInvisible(true);
		net.addArc(localStart,t1);
		net.addArc(t1,localEnd);
	}

	
	private void convertNode(ProbProcessTreeNode ppt, StochasticNet net, Place localStart, Place localEnd) {
		switch(ppt.getOperator()) {
		case CHOICE:
			convertChoice(ppt,net,localStart,localEnd);
			break;
		case CONCURRENCY:
			convertConc(ppt,net,localStart,localEnd);
			break;
		case PROBLOOP:
			convertLoop((PPTLoopNode) ppt,net,localStart,localEnd);
			break;
		case SEQUENCE:
			convertSeq(ppt,net,localStart,localEnd);
			break;
		default:
			break;
		}
	}

	private void convertConc(ProbProcessTreeNode ppt, StochasticNet net, Place localStart, Place localEnd) {
		TimedTransition tenter = net.addImmediateTransition(TAU + "cin",ppt.getWeight());
		tenter.setInvisible(true);
		TimedTransition texit = net.addImmediateTransition(TAU + "cexit",1.0d);
		texit.setInvisible(true);
		net.addArc(localStart,tenter);
		net.addArc(texit,localEnd);
		int i=1;
		for (ProbProcessTree child: ppt.getChildren()) {
			Place concPIn = net.addPlace("pci" + i);
			net.addArc(tenter,concPIn);
			Place concPOut = net.addPlace("pco" + i);
			net.addArc(concPOut,texit);
			convert(child,net,concPIn,concPOut);
			i++;
		}		
	}

	private void convertSeq(ProbProcessTreeNode ppt, StochasticNet net, Place localStart, Place localEnd) {
		List<ProbProcessTree> children = ppt.getChildren();
		Place prevPlace = localStart;
		for (int i=0; i<children.size(); i++) {
			ProbProcessTree child = children.get(i);
			Place nextPlace;
			if (i == children.size() -1) {
				nextPlace = localEnd;
			}else {
				nextPlace = net.addPlace("sp" + (i+1));
			}
			convert(child, net, prevPlace, nextPlace);
			prevPlace = nextPlace;
		}
	}

	private void convertChoice(ProbProcessTreeNode ppt, StochasticNet net, Place localStart, Place localEnd) {
		for (ProbProcessTree child: ppt.getChildren()) {
			convert(child,net,localStart,localEnd);
		}
	}

	private void convertLoop(PPTLoopNode ppt, StochasticNet net, Place localStart, Place localEnd) {
		TimedTransition tenter = net.addImmediateTransition(TAU + "lin",ppt.getWeight());
		tenter.setInvisible(true);
		TimedTransition texit = net.addImmediateTransition(TAU + "lexit",
														ppt.getWeight() / ppt.getLoopRepetitions());
		texit.setInvisible(true);
		Place midloop = net.addPlace("midloop");
		net.addArc(localStart,tenter);
		net.addArc(tenter,midloop);
		net.addArc(midloop,texit);
		net.addArc(texit,localEnd);
		
		ProbProcessTree loopChild = 
				ProbProcessTreeProjector.rescale(ppt.getChildren().get(0),
						(ppt.getLoopRepetitions() -1 ) / ppt.getLoopRepetitions());
		convert(loopChild, net, midloop, midloop);
	}

	
}
