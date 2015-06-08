package cn.edu.thss.iise.beehivez.server.metric.rorm.dependency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Event;

public class SequentialDirectAdjacency {

	private NetSystem _sys;
	private CompletePrefixUnfolding _cpu;
	private LeastCommonPredecessorsAndSuccessors _lc;
	private Set<Marking> visitedMarkings = new HashSet<Marking>();
	private Map<Event, Marking> enabledMarkingMap = new HashMap<Event, Marking>();

	public SequentialDirectAdjacency(CompletePrefixUnfolding cpu,
			LeastCommonPredecessorsAndSuccessors lc) {
		this._cpu = cpu;
		this._sys = (NetSystem) cpu.getOriginativeNetSystem();
		this._lc = lc;
	}
	
	public void generateSDA() {
		Marking initialMarking = getInitialMarking();
	}

	private Marking getInitialMarking() {
		Marking initialMarking = Marking.createMarking(this._cpu);
		initialMarking.fromMultiSet(this._cpu.getConditions(this._sys
				.getSourcePlaces().iterator().next()));
		return initialMarking;
	}

}
