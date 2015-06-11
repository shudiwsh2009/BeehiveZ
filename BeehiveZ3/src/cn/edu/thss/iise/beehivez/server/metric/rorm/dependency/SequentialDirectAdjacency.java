package cn.edu.thss.iise.beehivez.server.metric.rorm.dependency;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Transition;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Condition;
import org.jbpt.petri.unfolding.Event;
import org.jbpt.petri.unfolding.IBPNode;
import org.semanticweb.kaon2.tt;

public class SequentialDirectAdjacency {

	private NetSystem _sys;
	private CompletePrefixUnfolding _cpu;
	private LeastCommonPredecessorsAndSuccessors _lc;
	private Set<Marking> visitedMarkings = new HashSet<Marking>();
	private Map<Event, Marking> enabledMarkingMap = new HashMap<Event, Marking>();
	private Map<Transition, Set<Transition>> sdaRelations = new HashMap<Transition, Set<Transition>>();

	public SequentialDirectAdjacency(CompletePrefixUnfolding cpu,
			LeastCommonPredecessorsAndSuccessors lc) {
		this._cpu = cpu;
		this._sys = (NetSystem) cpu.getOriginativeNetSystem();
		this._lc = lc;
		generateSDA();
		System.out.print(toString());
	}

	private void generateSDA() {
		Marking initialMarking = getInitialMarking();
		initialMarking.getPostEnabledEvents().stream()
				.forEach(e -> enabledMarkingMap.put(e, initialMarking));
		dfsMarking(initialMarking);
		return;
	}

	@SuppressWarnings("rawtypes")
	private void dfsMarking(Marking m) {
		if (visitedMarkings.contains(m)) {
			return;
		}
		visitedMarkings.add(m);
		if (m.getPreEvent() == null) {
			// initialMarking
			for (Event postEvent : m.getPostEnabledEvents()) {
				Marking newMarking = m.clone();
				newMarking.fire(postEvent);
				dfsMarking(newMarking);
			}
		} else {
			// firstly, deal with postEnabledEvents
			for (Event postEvent : m.getPostEnabledEvents()) {
				if (!postEvent.getTransition().isSilent()) {
					// if postEnabledEvent is visible, add <pre, post> to sda
					sdaRelations.putIfAbsent(m.getPreEvent().getTransition(),
							new HashSet<Transition>());
					sdaRelations.get(m.getPreEvent().getTransition()).add(
							postEvent.getTransition());
				}
				// fire it and dfs
				Marking newMarking = m.clone();
				newMarking.fire(postEvent);
				dfsMarking(newMarking);
			}
			// secondly, fire all the enabled events which is concurrent with
			// preEvent, then check postDisabledEvents
			Marking newMarking = m.clone();
			boolean canFire = true;
			Set<Integer> visited = new HashSet<Integer>();
			while (canFire) {
				canFire = false;
				if (visited.contains(newMarking.hashCode())) {
					break;
				}
				visited.add(newMarking.hashCode());
				for (Event e : newMarking.getEnabledEvents()) {
					Set<IBPNode> lcpSet = this._lc.getLcpCpuMap().get(e)
							.get(newMarking.getPreEvent());
					boolean isConcurrent = !lcpSet.stream()
							.anyMatch(
									lcp -> (lcp instanceof Condition
											|| lcp == e || lcp == newMarking
											.getPreEvent()));
					if (isConcurrent) {
						newMarking.onlyFire(e);
						// check postDisabledEvent every time
						for (Event postEvent : newMarking
								.getPostDisabledEvents()) {
							if (newMarking.isEnabled(postEvent)) {
								if (!postEvent.getTransition().isSilent()) {
									// if postDisabledEvent is visible, add
									// <pre, post> to sda
									// fire it and dfs
									sdaRelations.putIfAbsent(newMarking
											.getPreEvent().getTransition(),
											new HashSet<Transition>());
									sdaRelations.get(
											newMarking.getPreEvent()
													.getTransition()).add(
											postEvent.getTransition());
									Marking postMarking = newMarking.clone();
									postMarking.fire(postEvent);
									dfsMarking(postMarking);
								} else {
									// if postDisabledEvent is invisible
									// TODO
									Marking postMarking = newMarking.clone();
									postMarking.fire(postEvent);
									dfsMarking(postMarking);
								}
							}
						}
						canFire = true;
						break;
					}
				}
			}

		}
	}

	private Marking getInitialMarking() {
		Marking initialMarking = Marking.createMarking(this._cpu);
		initialMarking.fromMultiSet(this._cpu.getConditions(this._sys
				.getSourcePlaces().iterator().next()));
		return initialMarking;
	}

	public String toString() {
		StringBuffer sb = new StringBuffer();
		this.sdaRelations
				.keySet()
				.stream()
				.sorted((a, b) -> a.getName().compareTo(b.getName()))
				.forEach(
						t -> {
							sb.append("\"");
							sb.append(t.getName());
							sb.append("\":[");
							this.sdaRelations
									.get(t)
									.stream()
									.sorted((a, b) -> a.getName().compareTo(
											b.getName()))
									.forEach(
											tt -> sb.append("\"" + tt.getName()
													+ "\","));
							sb.append("]");
							sb.append("\r\n");
						});
		return sb.toString();
	}

}
