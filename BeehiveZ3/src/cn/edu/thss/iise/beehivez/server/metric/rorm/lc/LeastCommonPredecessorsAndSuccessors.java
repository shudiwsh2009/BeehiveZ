package cn.edu.thss.iise.beehivez.server.metric.rorm.lc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.jbpt.petri.INode;
import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Place;
import org.jbpt.petri.Transition;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Condition;
import org.jbpt.petri.unfolding.Event;
import org.jbpt.petri.unfolding.IBPNode;

@SuppressWarnings("rawtypes")
public class LeastCommonPredecessorsAndSuccessors {

	private NetSystem _sys;
	private CompletePrefixUnfolding _cpu;
	private Set<Condition> _loopJoinConditions;
	private Map<INode, Map<INode, Boolean>> sysReachMap;
	private Map<IBPNode, Map<IBPNode, Boolean>> cpuReachMap;

	// lcp and lcs based on sysReachMap
	private Map<IBPNode, Map<IBPNode, Set<IBPNode>>> lcpSysMap = new HashMap<IBPNode, Map<IBPNode, Set<IBPNode>>>();
	private Map<IBPNode, Map<IBPNode, Set<IBPNode>>> lcsSysMap = new HashMap<IBPNode, Map<IBPNode, Set<IBPNode>>>();
	private Map<IBPNode, Map<IBPNode, Boolean>> forwardSkip = new HashMap<IBPNode, Map<IBPNode, Boolean>>();
	private Map<IBPNode, Map<IBPNode, Boolean>> backwardSkip = new HashMap<IBPNode, Map<IBPNode, Boolean>>();
	// lcp based on cpuReachmap
	private Map<IBPNode, Map<IBPNode, Set<IBPNode>>> lcpCpuMap = new HashMap<IBPNode, Map<IBPNode, Set<IBPNode>>>();

	public LeastCommonPredecessorsAndSuccessors(CompletePrefixUnfolding cpu) {
		this._cpu = cpu;
		this._sys = (NetSystem) cpu.getOriginativeNetSystem();
		this._loopJoinConditions = getLoopJoinConditions();
		initReachMap();

		List<IBPNode> vertices = new ArrayList<IBPNode>();
		for (Event e : this._cpu.getEvents()) {
			vertices.add((IBPNode) e);
		}
		for (Condition c : this._cpu.getConditions()) {
			vertices.add((IBPNode) c);
		}
		for (int i = 0; i < vertices.size(); ++i) {
			IBPNode u = vertices.get(i);
			this.lcpSysMap.put(u, new HashMap<IBPNode, Set<IBPNode>>());
			this.lcsSysMap.put(u, new HashMap<IBPNode, Set<IBPNode>>());
			this.forwardSkip.put(u, new HashMap<IBPNode, Boolean>());
			this.backwardSkip.put(u, new HashMap<IBPNode, Boolean>());
			this.lcpCpuMap.put(u, new HashMap<IBPNode, Set<IBPNode>>());
			for (int j = 0; j < vertices.size(); ++j) {
				IBPNode v = vertices.get(j);
				this.lcpSysMap.get(u).put(v, new HashSet<IBPNode>());
				this.lcsSysMap.get(u).put(v, new HashSet<IBPNode>());
				this.forwardSkip.get(u).put(v, false);
				this.backwardSkip.get(u).put(v, false);
				this.lcpCpuMap.get(u).put(v, new HashSet<IBPNode>());
			}
		}
		generateLcpAndLcsMap();
	}

	private void generateLcpAndLcsMap() {
		List<Event> events = new ArrayList<Event>(this._cpu.getEvents());
		for (int i = 0; i < events.size(); ++i) {
			Event e1 = events.get(i);
			for (int j = i + 1; j < events.size(); ++j) {
				Event e2 = events.get(j);
				boolean[] skipForward = hasSkipForward(e1, e2);
				this.forwardSkip.get(e1).put(e2, skipForward[0]);
				this.forwardSkip.get(e2).put(e1, skipForward[1]);
				boolean[] skipBackward = hasSkipBackward(e1, e2);
				this.backwardSkip.get(e1).put(e2, skipBackward[0]);
				this.backwardSkip.get(e2).put(e1, skipBackward[1]);
				if(e1.getTransition().getLabel().equals("b") && e2.getTransition().getLabel().equals("a")) {
					int a = 1;
				}
				computeLcpCpu(e1, e2);
			}
		}
	}

	/**
	 * used to check if e1 skips e2 and if e2 skips e1, forwards
	 * 
	 * @param e1
	 * @param e2
	 * @return
	 */
	private boolean[] hasSkipForward(Event e1, Event e2) {
		Set<IBPNode> lcsSet = new HashSet<IBPNode>();
		boolean hasSinkSucc1 = false, hasSinkSucc2 = false;
		// boolean hasLoopSucc1 = false, hasLoopSucc2 = false;
		Place sink = this._sys.getSinkPlaces().iterator().next();
		// step e1
		Queue<IBPNode> queue = new LinkedList<IBPNode>();
		queue.offer(e1);
		while (!queue.isEmpty()) {
			IBPNode u = queue.poll();
			if (u instanceof Event
					&& ((Event) u).getTransition() == e2.getTransition()) {
				continue;
			} else if (this._loopJoinConditions.contains(u)) {
				// hasLoopSucc1 = true;
			} else if (u instanceof Condition
					&& ((Condition) u).getPlace() == sink) {
				hasSinkSucc1 = true;
				if (this.sysReachMap.get(e2.getPetriNetNode()).get(
						u.getPetriNetNode())) {
					lcsSet.add(u);
				}
			} else if (u instanceof Condition && ((Condition) u).isCutoffPost()) {
				u = ((Condition) u).getCorrespondingCondition();
				queue.offer(u);
			} else {
				if (this.sysReachMap.get(e2.getPetriNetNode()).get(
						u.getPetriNetNode())) {
					lcsSet.add(u);
					continue;
				}
				if (u instanceof Condition) {
					Set<Event> uSuccSet = ((Condition) u).getPostE();
					for (Event uSucc : uSuccSet) {
						queue.offer(uSucc);
					}
				} else if (u instanceof Event) {
					Set<Condition> uSuccSet = ((Event) u).getPostConditions();
					for (Condition uSucc : uSuccSet) {
						queue.offer(uSucc);
					}
				}
			}
		}
		// step e2
		queue.clear();
		queue.offer(e2);
		while (!queue.isEmpty()) {
			IBPNode u = queue.poll();
			if (u instanceof Event
					&& ((Event) u).getTransition() == e1.getTransition()) {
				continue;
			} else if (this._loopJoinConditions.contains(u)) {
				// hasLoopSucc2 = true;
			} else if (u instanceof Condition
					&& ((Condition) u).getPlace() == sink) {
				hasSinkSucc2 = true;
				if (this.sysReachMap.get(e1.getPetriNetNode()).get(
						u.getPetriNetNode())) {
					lcsSet.add(u);
				}
			} else if (u instanceof Condition && ((Condition) u).isCutoffPost()) {
				u = ((Condition) u).getCorrespondingCondition();
				queue.offer(u);
			} else {
				if (this.sysReachMap.get(e1.getPetriNetNode()).get(
						u.getPetriNetNode())) {
					lcsSet.add(u);
					continue;
				}
				if (u instanceof Condition) {
					Set<Event> uSuccSet = ((Condition) u).getPostE();
					for (Event uSucc : uSuccSet) {
						queue.offer(uSucc);
					}
				} else if (u instanceof Event) {
					Set<Condition> uSuccSet = ((Event) u).getPostConditions();
					for (Condition uSucc : uSuccSet) {
						queue.offer(uSucc);
					}
				}
			}
		}
		lcsSet = filterLcsSet(lcsSet);
		// analysis
		this.lcsSysMap.get(e1).get(e2).addAll(lcsSet);
		this.lcsSysMap.get(e2).get(e1).addAll(lcsSet);
		boolean e1SkipE2 = false, e2SkipE1 = false;
		for (IBPNode lcs : lcsSet) {
			// if(lcs instanceof Condition) {
			if (lcs != e1 && lcs != e2) {
				e1SkipE2 = true;
				e2SkipE1 = true;
				break;
			} else if (lcs == e1) {
				e1SkipE2 = true;
			} else if (lcs == e2) {
				e2SkipE1 = true;
			}
		}
		if (hasSinkSucc1) {
			e1SkipE2 = true;
		}
		if (hasSinkSucc2) {
			e2SkipE1 = true;
		}
		return new boolean[] { e1SkipE2, e2SkipE1 };
	}

	/**
	 * filter lcs set to only keep the least common successors
	 * 
	 * @param oriLcsSet
	 * @return
	 */
	private Set<IBPNode> filterLcsSet(Set<IBPNode> oriLcsSet) {
		Set<IBPNode> lcsSet = new HashSet<IBPNode>();
		for (IBPNode lcs : oriLcsSet) {
			Iterator<IBPNode> it = lcsSet.iterator();
			boolean filter = false;
			while (it.hasNext()) {
				IBPNode v = it.next();
				if (this.sysReachMap.get(v.getPetriNetNode()).get(
						lcs.getPetriNetNode())) {
					filter = true;
					break;
				}
				if (this.sysReachMap.get(lcs.getPetriNetNode()).get(
						v.getPetriNetNode())) {
					it.remove();
				}
			}
			if (filter) {
				continue;
			} else {
				lcsSet.add(lcs);
			}
		}
		return lcsSet;
	}

	/**
	 * used to check if e1 skips e2 and if e2 skips e1, backwards
	 * 
	 * @param e1
	 * @param e2
	 * @return
	 */
	private boolean[] hasSkipBackward(Event e1, Event e2) {
		Set<IBPNode> lcpSet = new HashSet<IBPNode>();
		boolean hasSourcePred1 = false, hasSourcePred2 = false;
		// boolean hasLoopPred1 = false, hasLoopPred2 = false;
		Place source = this._sys.getSourcePlaces().iterator().next();
		// step e1
		Queue<IBPNode> queue = new LinkedList<IBPNode>();
		queue.offer(e1);
		while (!queue.isEmpty()) {
			IBPNode u = queue.poll();
			if (u instanceof Event
					&& ((Event) u).getTransition() == e2.getTransition()) {
				continue;
			} else if (this._loopJoinConditions.contains(u)) {
				// hasLoopPred1 = true;
			} else if (u instanceof Condition
					&& ((Condition) u).getPlace() == source) {
				hasSourcePred1 = true;
				if (this.sysReachMap.get(u.getPetriNetNode()).get(
						e2.getPetriNetNode())) {
					lcpSet.add(u);
				}
			} else if (u instanceof Condition) {
				Set<Condition> conditions = new HashSet<Condition>();
				if (((Condition) u).getMappingConditions() != null) {
					conditions.addAll(((Condition) u).getMappingConditions());
				} else {
					conditions.add((Condition) u);
				}
				for (Condition c : conditions) {
					if (this._loopJoinConditions.contains(c)) {
						continue;
					}
					if (this.sysReachMap.get(c.getPetriNetNode()).get(
							e2.getPetriNetNode())) {
						lcpSet.add(c);
						continue;
					}
					queue.offer(c.getPreEvent());
				}
			} else {
				if (this.sysReachMap.get(u.getPetriNetNode()).get(
						e2.getPetriNetNode())) {
					lcpSet.add(u);
					continue;
				}
				Set<Condition> uPredSet = ((Event) u).getPreConditions();
				for (Condition uPred : uPredSet) {
					queue.offer(uPred);
				}
			}
		}
		// step e2
		queue.clear();
		queue.offer(e2);
		while (!queue.isEmpty()) {
			IBPNode u = queue.poll();
			if (u instanceof Event
					&& ((Event) u).getTransition() == e1.getTransition()) {
				continue;
			} else if (this._loopJoinConditions.contains(u)) {
				// hasLoopPred2 = true;
			} else if (u instanceof Condition
					&& ((Condition) u).getPlace() == source) {
				hasSourcePred2 = true;
				if (this.sysReachMap.get(u.getPetriNetNode()).get(
						e1.getPetriNetNode())) {
					lcpSet.add(u);
				}
			} else if (u instanceof Condition) {
				Set<Condition> conditions = new HashSet<Condition>();
				if (((Condition) u).getMappingConditions() != null) {
					conditions.addAll(((Condition) u).getMappingConditions());
				} else {
					conditions.add((Condition) u);
				}
				for (Condition c : conditions) {
					if (this._loopJoinConditions.contains(c)) {
						continue;
					}
					if (this.sysReachMap.get(c.getPetriNetNode()).get(
							e1.getPetriNetNode())) {
						lcpSet.add(c);
						continue;
					}
					queue.offer(c.getPreEvent());
				}
			} else {
				if (this.sysReachMap.get(u.getPetriNetNode()).get(
						e1.getPetriNetNode())) {
					lcpSet.add(u);
					continue;
				}
				Set<Condition> uPredSet = ((Event) u).getPreConditions();
				for (Condition uPred : uPredSet) {
					queue.offer(uPred);
				}
			}
		}
		lcpSet = filterLcpSet(lcpSet);
		// analysis
		this.lcpSysMap.get(e1).get(e2).addAll(lcpSet);
		this.lcpSysMap.get(e2).get(e1).addAll(lcpSet);
		boolean e1SkipE2 = false, e2SkipE1 = false;
		for (IBPNode lcp : lcpSet) {
			// if(lcp instanceof Condition) {
			if (lcp != e1 && lcp != e2) {
				e1SkipE2 = true;
				e2SkipE1 = true;
				break;
			} else if (lcp == e1) {
				e1SkipE2 = true;
			} else if (lcp == e2) {
				e2SkipE1 = true;
			}
		}
		if (hasSourcePred1) {
			e1SkipE2 = true;
		}
		if (hasSourcePred2) {
			e2SkipE1 = true;
		}
		return new boolean[] { e1SkipE2, e2SkipE1 };
	}

	/**
	 * filter lcp set to only keep the least common predecessors
	 * 
	 * @param oriLcpSet
	 * @return
	 */
	private Set<IBPNode> filterLcpSet(Set<IBPNode> oriLcpSet) {
		Set<IBPNode> lcpSet = new HashSet<IBPNode>();
		for (IBPNode lcp : oriLcpSet) {
			Iterator<IBPNode> it = lcpSet.iterator();
			boolean filter = false;
			while (it.hasNext()) {
				IBPNode v = it.next();
				if (this.sysReachMap.get(lcp.getPetriNetNode()).get(
						v.getPetriNetNode())) {
					filter = true;
					break;
				}
				if (this.sysReachMap.get(v.getPetriNetNode()).get(
						lcp.getPetriNetNode())) {
					it.remove();
				}
			}
			if (filter) {
				continue;
			} else {
				lcpSet.add(lcp);
			}
		}
		return lcpSet;
	}

	/**
	 * compute the lcp of e1 and e2 based on cpu
	 * 
	 * @param e1
	 * @param e2
	 * @return
	 */
	private void computeLcpCpu(Event e1, Event e2) {
		Set<IBPNode> lcpSet = new HashSet<IBPNode>();
		Place source = this._sys.getSourcePlaces().iterator().next();
		// step e1
		Queue<IBPNode> queue = new LinkedList<IBPNode>();
		queue.offer(e1);
		while (!queue.isEmpty()) {
			IBPNode u = queue.poll();
			if (u instanceof Event
					&& ((Event) u).getTransition() == e2.getTransition()) {
				continue;
			} else if (this._loopJoinConditions.contains(u)) {
				// hasLoopPred1 = true;
			} else if (u instanceof Condition
					&& ((Condition) u).getPlace() == source) {
				if (this.cpuReachMap.get(u).get(e2)) {
					lcpSet.add(u);
				}
			} else if (u instanceof Condition) {
				Set<Condition> conditions = new HashSet<Condition>();
				if (((Condition) u).getMappingConditions() != null) {
					conditions.addAll(((Condition) u).getMappingConditions());
				} else {
					conditions.add((Condition) u);
				}
				for (Condition c : conditions) {
					if (this._loopJoinConditions.contains(c)) {
						continue;
					}
					if (this.cpuReachMap.get(c).get(e2)) {
						lcpSet.add(c);
						continue;
					}
					queue.offer(c.getPreEvent());
				}
			} else {
				if (this.cpuReachMap.get(u).get(e2)) {
					lcpSet.add(u);
					continue;
				}
				Set<Condition> uPredSet = ((Event) u).getPreConditions();
				for (Condition uPred : uPredSet) {
					queue.offer(uPred);
				}
			}
		}
		// step e2
		queue.clear();
		queue.offer(e2);
		while (!queue.isEmpty()) {
			IBPNode u = queue.poll();
			if (u instanceof Event
					&& ((Event) u).getTransition() == e1.getTransition()) {
				continue;
			} else if (this._loopJoinConditions.contains(u)) {
				// hasLoopPred2 = true;
			} else if (u instanceof Condition
					&& ((Condition) u).getPlace() == source) {
				if (this.cpuReachMap.get(u).get(e1)) {
					lcpSet.add(u);
				}
			} else if (u instanceof Condition) {
				Set<Condition> conditions = new HashSet<Condition>();
				if (((Condition) u).getMappingConditions() != null) {
					conditions.addAll(((Condition) u).getMappingConditions());
				} else {
					conditions.add((Condition) u);
				}
				for (Condition c : conditions) {
					if (this._loopJoinConditions.contains(c)) {
						continue;
					}
					if (this.cpuReachMap.get(c).get(e1)) {
						lcpSet.add(c);
						continue;
					}
					queue.offer(c.getPreEvent());
				}
			} else {
				if (this.cpuReachMap.get(u).get(e1)) {
					lcpSet.add(u);
					continue;
				}
				Set<Condition> uPredSet = ((Event) u).getPreConditions();
				for (Condition uPred : uPredSet) {
					queue.offer(uPred);
				}
			}
		}
		// analysis
		this.lcpCpuMap.get(e1).get(e2).addAll(lcpSet);
		this.lcpCpuMap.get(e2).get(e1).addAll(lcpSet);
	}

	/**
	 * dfs to get all the XOR-join conditions which ends a loop
	 * 
	 * @return
	 */
	private Set<Condition> getLoopJoinConditions() {
		Set<Condition> loopJoinConditions = new HashSet<Condition>();
		Condition source = this._cpu.getInitialCut().iterator().next();
		Set<INode> visited = new HashSet<INode>();
		dfsLoopJoin(source, visited, loopJoinConditions);
		return loopJoinConditions;
	}

	private void dfsLoopJoin(IBPNode u, Set<INode> visited,
			Set<Condition> loopJoinConditions) {
		if (u instanceof Condition && visited.contains(u.getPetriNetNode())
				&& ((Condition) u).isCutoffPost()) {
			loopJoinConditions.add((Condition) u);
			return;
		}
		visited.add(u.getPetriNetNode());
		if (u instanceof Condition) {
			for (Event uSucc : ((Condition) u).getPostE()) {
				dfsLoopJoin(uSucc, visited, loopJoinConditions);
			}
		} else {
			for (Condition uSucc : ((Event) u).getPostConditions()) {
				dfsLoopJoin(uSucc, visited, loopJoinConditions);
			}
		}
		visited.remove(u.getPetriNetNode());
	}

	/**
	 * dfs to get reachable matrix
	 * 
	 * @return
	 */
	private void initReachMap() {
		this.sysReachMap = new HashMap<INode, Map<INode,Boolean>>();
		List<INode> sysNodes = new ArrayList<INode>();
		for (Transition t : this._sys.getTransitions()) {
			sysNodes.add((INode) t);
		}
		for (Place p : this._sys.getPlaces()) {
			sysNodes.add((INode) p);
		}
		for (int i = 0; i < sysNodes.size(); ++i) {
			INode u = sysNodes.get(i);
			this.sysReachMap.put(u, new HashMap<INode, Boolean>());
			for (int j = 0; j < sysNodes.size(); ++j) {
				INode v = sysNodes.get(j);
				this.sysReachMap.get(u).put(v, false);
			}
		}

		this.cpuReachMap = new HashMap<IBPNode, Map<IBPNode,Boolean>>();
		List<IBPNode> cpuNodes = new ArrayList<IBPNode>();
		for (Event e : this._cpu.getEvents()) {
			cpuNodes.add(e);
		}
		for (Condition c : this._cpu.getConditions()) {
			cpuNodes.add(c);
		}
		for (int i = 0; i < cpuNodes.size(); ++i) {
			IBPNode u = cpuNodes.get(i);
			this.cpuReachMap.put(u, new HashMap<IBPNode, Boolean>());
			for (int j = 0; j < cpuNodes.size(); ++j) {
				IBPNode v = cpuNodes.get(j);
				this.cpuReachMap.get(u).put(v, false);
			}
		}

		Condition source = this._cpu.getInitialCut().iterator().next();
		Place sink = this._sys.getSinkPlaces().iterator().next();
		List<IBPNode> trace = new ArrayList<IBPNode>();
		dfsReachMap(source, trace, sink);
	}

	private void dfsReachMap(IBPNode u, List<IBPNode> trace, Place sink) {
		if (this._loopJoinConditions.contains(u)) {
			generateReachMap(trace);
		} else if (u.getPetriNetNode() == sink) {
			Condition sinkCondition = (Condition) u;
			if (sinkCondition.isCutoffPost()) {
				sinkCondition = sinkCondition.getCorrespondingCondition();
			}
			trace.add(sinkCondition);
			generateReachMap(trace);
			trace.remove(trace.size() - 1);
		} else if (u instanceof Condition && ((Condition) u).isCutoffPost()) {
			u = ((Condition) u).getCorrespondingCondition();
			dfsReachMap(u, trace, sink);
		} else {
			trace.add(u);
			if (u instanceof Condition) {
				Set<Event> uSuccSet = ((Condition) u).getPostE();
				for (Event uSucc : uSuccSet) {
					dfsReachMap(uSucc, trace, sink);
				}
			} else if (u instanceof Event) {
				Set<Condition> uSuccSet = ((Event) u).getPostConditions();
				for (Condition uSucc : uSuccSet) {
					dfsReachMap(uSucc, trace, sink);
				}
			}
			trace.remove(trace.size() - 1);
		}
	}

	private void generateReachMap(List<IBPNode> trace) {
		for (int i = 0; i < trace.size(); ++i) {
			IBPNode u = trace.get(i);
			for (int j = i + 1; j < trace.size(); ++j) {
				IBPNode v = trace.get(j);
				this.sysReachMap.get(u.getPetriNetNode()).put(
						v.getPetriNetNode(), true);
				this.cpuReachMap.get(u).put(v, true);
			}
		}
	}

	public Map<IBPNode, Map<IBPNode, Boolean>> getForwardSkip() {
		return forwardSkip;
	}

	public Map<IBPNode, Map<IBPNode, Boolean>> getBackwardSkip() {
		return backwardSkip;
	}

	public Map<IBPNode, Map<IBPNode, Set<IBPNode>>> getLcpSysMap() {
		return lcpSysMap;
	}

	public Map<IBPNode, Map<IBPNode, Set<IBPNode>>> getLcsSysMap() {
		return lcsSysMap;
	}

	public Map<IBPNode, Map<IBPNode, Set<IBPNode>>> getLcpCpuMap() {
		return lcpCpuMap;
	}

	public Map<INode, Map<INode, Boolean>> getSysReachMap() {
		return sysReachMap;
	}

	public Map<IBPNode, Map<IBPNode, Boolean>> getCpuReachMap() {
		return cpuReachMap;
	}

}
