package cn.edu.thss.iise.beehivez.server.metric.rorm.dependency;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Transition;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Condition;
import org.jbpt.petri.unfolding.Event;
import org.jbpt.petri.unfolding.IBPNode;

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

	private void dfsMarking(Marking m) {
		if (visitedMarkings.contains(m)) {
			return;
		}
		visitedMarkings.add(m);
		if (m.getPreVisEvent() == null) {
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
					sdaRelations.putIfAbsent(
							m.getPreVisEvent().getTransition(),
							new HashSet<Transition>());
					sdaRelations.get(m.getPreVisEvent().getTransition()).add(
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
			Set<Integer> visited = new HashSet<Integer>();
			dfsPostDisabledEvents(newMarking, visited);
		}
	}

	@SuppressWarnings("rawtypes")
	private void dfsPostDisabledEvents(Marking m, Set<Integer> visited) {
		// fire all the enabled events which is concurrent with preEvent,
		// check postDisabledEvents in every fire
		boolean canFire = true;
		// 1. try to fire all the enabled events which are not in a non-free
		// choice structure
		while (canFire) {
			canFire = false;
			if (visited.contains(m.hashCode())) {
				break;
			}
			visited.add(m.hashCode());
			for (Event e : m.getEnabledEvents()) {
				if (this.checkEventInNFC(e)) {
					Set<IBPNode> lcpSet = this._lc.getLcpCpuMap().get(e)
							.get(m.getPreEvent());
					boolean isConcurrent = !lcpSet.stream()
							.anyMatch(
									lcp -> (lcp instanceof Condition
											|| lcp == e || lcp == m
											.getPreEvent()));
					if (isConcurrent) {
						m.onlyFire(e);
						// check postDisabledEvent every time
						for (Event postEvent : m.getPostDisabledEvents()) {
							if (m.isEnabled(postEvent)) {
								if (!postEvent.getTransition().isSilent()) {
									// if postDisabledEvent is visible, add
									// <pre, post> to sda
									sdaRelations.putIfAbsent(m.getPreVisEvent()
											.getTransition(),
											new HashSet<Transition>());
									sdaRelations.get(
											m.getPreVisEvent().getTransition())
											.add(postEvent.getTransition());
								}
								// fire it and dfs
								Marking postMarking = m.clone();
								postMarking.fire(postEvent);
								dfsMarking(postMarking);
							}
						}
						canFire = true;
					}
				}
			}
		}
		// 2. if there is no such events, traverse to fire other enabled events
		for (Event e : m.getEnabledEvents()) {
			Set<IBPNode> lcpSet = this._lc.getLcpCpuMap().get(e)
					.get(m.getPreEvent());
			boolean isConcurrent = !lcpSet.stream().anyMatch(
					lcp -> (lcp instanceof Condition || lcp == e || lcp == m
							.getPreEvent()));
			if (isConcurrent) {
				Marking copyMarking = m.clone();
				copyMarking.onlyFire(e);
				// check postDisabledEvent every time
				for (Event postEvent : copyMarking.getPostDisabledEvents()) {
					if (copyMarking.isEnabled(postEvent)) {
						if (!postEvent.getTransition().isSilent()) {
							// if postDisabledEvent is visible, add <pre, post>
							// to sda
							sdaRelations.putIfAbsent(copyMarking
									.getPreVisEvent().getTransition(),
									new HashSet<Transition>());
							sdaRelations.get(
									copyMarking.getPreVisEvent()
											.getTransition()).add(
									postEvent.getTransition());
						}
						// fire it and dfs
						Marking postMarking = copyMarking.clone();
						postMarking.fire(postEvent);
						dfsMarking(postMarking);
					}
				}
				dfsPostDisabledEvents(copyMarking, visited);
			}
		}
	}

	/**
	 * check if a event is in a non-free choice structure
	 * 
	 * @param e
	 * @return
	 */
	private boolean checkEventInNFC(Event e) {
		Set<Condition> preConditions = e.getPreConditions();
		for (Condition c : preConditions) {
			Set<Event> postEvents = c.getPostE();
			for (Event pe : postEvents) {
				if (pe == e
						|| e.getPreConditions().equals(pe.getPreConditions())) {
					continue;
				}
				return false;
			}
		}
		return true;
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

	public Map<Transition, Set<Transition>> getSdaRelations() {
		return sdaRelations;
	}

}
