package cn.edu.thss.iise.beehivez.server.metric.rorm.old.lc;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.processmining.framework.models.ModelGraphVertex;
import org.processmining.framework.models.petrinet.PetriNet;

import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.CompleteFinitePrefix;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.Condition;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.Event;

/**
 * The LCP (Least Common Predecessor) set and LCS (Least Common Successor) set
 * 
 * @author Shudi
 *
 */
public class LeastCommonPredecessorsAndSuccessors {

	private PetriNet _pn;
	private CompleteFinitePrefix _cfp;
//	private Map<ModelGraphVertex, Integer> _numbering;
	private Set<Condition> _loopJoinConditions;
	private Map<ModelGraphVertex, Map<ModelGraphVertex, Boolean>> reachMap;
	
	private Map<ModelGraphVertex, Map<ModelGraphVertex, Set<ModelGraphVertex>>> lcpMap = new HashMap<ModelGraphVertex, Map<ModelGraphVertex, Set<ModelGraphVertex>>>();
	private Map<ModelGraphVertex, Map<ModelGraphVertex, Set<ModelGraphVertex>>> lcsMap = new HashMap<ModelGraphVertex, Map<ModelGraphVertex, Set<ModelGraphVertex>>>();
	private Map<ModelGraphVertex, Map<ModelGraphVertex, Boolean>> forwardSkip = new HashMap<ModelGraphVertex, Map<ModelGraphVertex,Boolean>>();
	private Map<ModelGraphVertex, Map<ModelGraphVertex, Boolean>> backwardSkip = new HashMap<ModelGraphVertex, Map<ModelGraphVertex,Boolean>>();

	public LeastCommonPredecessorsAndSuccessors(CompleteFinitePrefix cfp) {
		this._cfp = cfp;
		this._pn = this._cfp.getPetriNet();
		this._loopJoinConditions = getLoopJoinConditions();
		this.reachMap = initReachMap();
//		GraphNumbering gn = new GraphNumbering();
//		this._numbering = gn.numberingCFP(this._cfp);
		
		List<ModelGraphVertex> vertexs = this._cfp.getVerticeList();
		for(int i = 0; i < vertexs.size(); ++i) {
			ModelGraphVertex u = vertexs.get(i);
			this.lcpMap.put(u, new HashMap<ModelGraphVertex, Set<ModelGraphVertex>>());
			this.lcsMap.put(u, new HashMap<ModelGraphVertex, Set<ModelGraphVertex>>());
			this.forwardSkip.put(u, new HashMap<ModelGraphVertex, Boolean>());
			this.backwardSkip.put(u, new HashMap<ModelGraphVertex, Boolean>());
			for(int j = 0; j < vertexs.size(); ++j) {
				ModelGraphVertex v = vertexs.get(j);
				this.lcpMap.get(u).put(v, new HashSet<ModelGraphVertex>());
				this.lcsMap.get(u).put(v, new HashSet<ModelGraphVertex>());
				this.forwardSkip.get(u).put(v, false);
				this.backwardSkip.get(u).put(v, false);
			}
		}
		generateLcpAndLcsMap();
	}
	
	private void generateLcpAndLcsMap() {
		List<ModelGraphVertex> vertexs = this._cfp.getVerticeList();
		for(int i = 0; i < vertexs.size(); ++i) {
			ModelGraphVertex v1 = vertexs.get(i);
			if(v1 instanceof Condition) {
				continue;
			}
			for(int j = i + 1; j < vertexs.size(); ++j) {
				ModelGraphVertex v2 = vertexs.get(j);
				if(v2 instanceof Condition) {
					continue;
				}
				if(v1.getIdentifier().equals("d") && v2.getIdentifier().equals("g")) {
					System.out.println();
				}
				boolean[] skipForward = hasSkipForward(v1, v2);
				this.forwardSkip.get(v1).put(v2, skipForward[0]);
				this.forwardSkip.get(v2).put(v1, skipForward[1]);
				boolean[] skipBackward = hasSkipBackward(v1, v2);
				this.backwardSkip.get(v1).put(v2, skipBackward[0]);
				this.backwardSkip.get(v2).put(v1, skipBackward[1]);
			}
		}
	}
	
	/**
	 * used to check if v1 skip v2 and if v2 skip v1 backward
	 * @param v1
	 * @param v2
	 * @return
	 */
	private boolean[] hasSkipBackward(ModelGraphVertex v1, ModelGraphVertex v2) {
		Set<ModelGraphVertex> lcpSet = new HashSet<ModelGraphVertex>();
		boolean hasSourcePred1 = false, hasSourcePred2 = false;
		boolean hasLoopPred1 = false, hasLoopPred2 = false;
		ModelGraphVertex source = this._pn.getSource();
		// step v1
		Queue<ModelGraphVertex> queue = new LinkedList<ModelGraphVertex>();
		queue.offer(v1);
		while(!queue.isEmpty()) {
			ModelGraphVertex u = queue.poll();
			if(u == v2) {
				continue;
			} else if(this._loopJoinConditions.contains(u)) {
				hasLoopPred1 = true;
			} else if(u.getPredecessors().isEmpty()) {
				hasSourcePred1 = true;
				if(this.reachMap.get(u).get(v2)) {
					lcpSet.add(u);
				}
			} else if(u instanceof Condition) {
				Set<Condition> conditions = new HashSet<Condition>();
				if(((Condition) u).getMappingConditions() != null) {
					conditions.addAll(((Condition) u).getMappingConditions());
				} else {
					conditions.add((Condition) u);
				}
				for(Condition c : conditions) {
					if(this._loopJoinConditions.contains(c)) {
						continue;
					}
					if(this.reachMap.get(c).get(v2)) {
						lcpSet.add(c);
						continue;
					}
					Set<ModelGraphVertex> cPredSet = c.getPredecessors();
					queue.offer(cPredSet.iterator().next());
				}
			} else {
				if(this.reachMap.get(u).get(v2)) {
					lcpSet.add(u);
					continue;
				}
				Set<ModelGraphVertex> uPredSet = u.getPredecessors();
				for(ModelGraphVertex uPred : uPredSet) {
					queue.offer(uPred);
				}
			}
		}
		// step v2
		queue.clear();
		queue.offer(v2);
		while(!queue.isEmpty()) {
			ModelGraphVertex u = queue.poll();
			if(u == v1) {
				continue;
			} else if(this._loopJoinConditions.contains(u)) {
				hasLoopPred2 = true;
			} else if(u.getPredecessors().isEmpty()) {
				hasSourcePred2 = true;
				if(this.reachMap.get(u).get(v1)) {
					lcpSet.add(u);
				}
			} else if(u instanceof Condition) {
				Set<Condition> conditions = new HashSet<Condition>();
				if(((Condition) u).getMappingConditions() != null) {
					conditions.addAll(((Condition) u).getMappingConditions());
				} else {
					conditions.add((Condition) u);
				}
				for(Condition c : conditions) {
					if(this._loopJoinConditions.contains(c)) {
						continue;
					}
					if(this.reachMap.get(c).get(v1)) {
						lcpSet.add(c);
						continue;
					}
					Set<ModelGraphVertex> cPredSet = c.getPredecessors();
					queue.offer(cPredSet.iterator().next());
				}
			} else {
				if(this.reachMap.get(u).get(v1)) {
					lcpSet.add(u);
					continue;
				}
				Set<ModelGraphVertex> uPredSet = u.getPredecessors();
				for(ModelGraphVertex uPred : uPredSet) {
					queue.offer(uPred);
				}
			}
		}
		lcpSet = filterLcpSet(lcpSet);
		// analysis
		this.lcpMap.get(v1).get(v2).addAll(lcpSet);
		this.lcpMap.get(v2).get(v1).addAll(lcpSet);
		boolean v1SkipV2 = false, v2SkipV1 = false;
		for(ModelGraphVertex lcp : lcpSet) {
			if(lcp instanceof Condition) {
				v1SkipV2 = true;
				v2SkipV1 = true;
				break;
			} else if(lcp == v1) {
				v1SkipV2 = true;
			} else if(lcp == v2) {
				v2SkipV1 = true;
			}
		}
		if(hasSourcePred1) {
			v1SkipV2 = true;
		}
		if(hasSourcePred2) {
			v2SkipV1 = true;
		}
		return new boolean[]{v1SkipV2, v2SkipV1};
	}
	
	/**
	 * filter lcp set to only keep the least common predecessors
	 * @param oriLcsSet
	 * @return
	 */
	private Set<ModelGraphVertex> filterLcpSet(Set<ModelGraphVertex> oriLcpSet) {
		Set<ModelGraphVertex> lcpSet = new HashSet<ModelGraphVertex>();
		for(ModelGraphVertex lcp : oriLcpSet) {
			Iterator<ModelGraphVertex> it = lcpSet.iterator();
			boolean filter = false;
			while(it.hasNext()) {
				ModelGraphVertex v = it.next();
				if(this.reachMap.get(lcp).get(v)) {
					filter = true;
					break;
				}
				if(this.reachMap.get(v).get(lcp)) {
					it.remove();
				}
			}
			if(filter) {
				continue;
			} else {
				lcpSet.add(lcp);
			}
		}
		return lcpSet;
	}
	
	/**
	 * used to check if v1 skip v2 and if v2 skip v1
	 * @param v1
	 * @param v2
	 * @return
	 */
	private boolean[] hasSkipForward(ModelGraphVertex v1, ModelGraphVertex v2) {
		Set<ModelGraphVertex> lcsSet = new HashSet<ModelGraphVertex>();
		boolean hasSinkSucc1 = false, hasSinkSucc2 = false;
		boolean hasLoopSucc1 = false, hasLoopSucc2 = false;
		ModelGraphVertex sink = this._pn.getSink();
		// step v1
		Queue<ModelGraphVertex> queue = new LinkedList<ModelGraphVertex>();
		queue.offer(v1);
		while(!queue.isEmpty()) {
			ModelGraphVertex u = queue.poll();
			if(u == v2) {
				continue;
			} else if(this._loopJoinConditions.contains(u)) {
				hasLoopSucc1 = true;
			} else if(u.getSuccessors().isEmpty() && u instanceof Condition
					&& ((Condition) u).getOriginalPlace() == sink) {
				hasSinkSucc1 = true;
				if(this.reachMap.get(v2).get(u)) {
					lcsSet.add(u);
				}
			} else if(u.getSuccessors().isEmpty() && u instanceof Condition
					&& ((Condition) u).isCutoffPost()) {
				u = ((Condition) u).getCorrespondingCondition();
				queue.offer(u);
			} else {
				if(this.reachMap.get(v2).get(u)) {
					lcsSet.add(u);
					continue;
				}
				Set<ModelGraphVertex> uSuccSet = u.getSuccessors();
				for(ModelGraphVertex uSucc : uSuccSet) {
					queue.offer(uSucc);
				}
			}
		}
		// step v2
		queue.clear();
		queue.offer(v2);
		while(!queue.isEmpty()) {
			ModelGraphVertex u = queue.poll();
			if(u == v1) {
				continue;
			} else if(this._loopJoinConditions.contains(u)) {
				hasLoopSucc2 = true;
			} else if(u.getSuccessors().isEmpty() && u instanceof Condition
					&& ((Condition) u).getOriginalPlace() == sink) {
				hasSinkSucc2 = true;
				if(this.reachMap.get(v1).get(u)) {
					lcsSet.add(u);
				}
			} else if(u.getSuccessors().isEmpty() && u instanceof Condition
					&& ((Condition) u).isCutoffPost()) {
				u = ((Condition) u).getCorrespondingCondition();
				queue.offer(u);
			} else {
				if(this.reachMap.get(v1).get(u)) {
					lcsSet.add(u);
					continue;
				}
				Set<ModelGraphVertex> uSuccSet = u.getSuccessors();
				for(ModelGraphVertex uSucc : uSuccSet) {
					queue.offer(uSucc);
				}
			}
		}
		lcsSet = filterLcsSet(lcsSet);
		// analysis
		this.lcsMap.get(v1).get(v2).addAll(lcsSet);
		this.lcsMap.get(v2).get(v1).addAll(lcsSet);
		boolean v1SkipV2 = false, v2SkipV1 = false;
		for(ModelGraphVertex lcs : lcsSet) {
			if(lcs instanceof Condition) {
				v1SkipV2 = true;
				v2SkipV1 = true;
				break;
			} else if(lcs == v1) {
				v1SkipV2 = true;
			} else if(lcs == v2) {
				v2SkipV1 = true;
			}
		}
		if(hasSinkSucc1) {
			v1SkipV2 = true;
		}
		if(hasSinkSucc2) {
			v2SkipV1 = true;
		}
		return new boolean[]{v1SkipV2, v2SkipV1};
	}
	
	/**
	 * filter lcs set to only keep the least common successors
	 * @param oriLcsSet
	 * @return
	 */
	private Set<ModelGraphVertex> filterLcsSet(Set<ModelGraphVertex> oriLcsSet) {
		Set<ModelGraphVertex> lcsSet = new HashSet<ModelGraphVertex>();
		for(ModelGraphVertex lcs : oriLcsSet) {
			Iterator<ModelGraphVertex> it = lcsSet.iterator();
			boolean filter = false;
			while(it.hasNext()) {
				ModelGraphVertex v = it.next();
				if(this.reachMap.get(v).get(lcs)) {
					filter = true;
					break;
				}
				if(this.reachMap.get(lcs).get(v)) {
					it.remove();
				}
			}
			if(filter) {
				continue;
			} else {
				lcsSet.add(lcs);
			}
		}
		return lcsSet;
	}
	
	/**
	 * dfs to get all the XOR-join conditions which ends a loop 
	 * @param loopJoinConditions
	 */
	private Set<Condition> getLoopJoinConditions() {
		Set<Condition> loopJoinConditions = new HashSet<Condition>();
		Condition source = (Condition) this._cfp.getSource();
		Set<String> visited = new HashSet<String>();
		dfsLoopJoin(source, visited, loopJoinConditions);
		return loopJoinConditions;
	}

	private void dfsLoopJoin(ModelGraphVertex u, Set<String> visited, Set<Condition> loopJoinConditions) {
		if(u instanceof Event && visited.contains(u.getIdentifier())) {
			return;
		}
		if (u instanceof Condition && visited.contains(u.getIdentifier())
				&& ((Condition) u).isCutoffPost()) {
			loopJoinConditions.add((Condition) u);
			return;
		}
		boolean visitAgain = visited.contains(u.getIdentifier());
		visited.add(u.getIdentifier());
		Set<ModelGraphVertex> uSuccSet = u.getSuccessors();
		for (ModelGraphVertex uSucc : uSuccSet) {
			dfsLoopJoin(uSucc, visited, loopJoinConditions);
		}
		if(!visitAgain) {
			visited.remove(u.getIdentifier());
		}
	}
	
	/**
	 * dfs to get reachable matrix
	 * @return
	 */
	private Map<ModelGraphVertex, Map<ModelGraphVertex, Boolean>> initReachMap() {
		Map<ModelGraphVertex, Map<ModelGraphVertex, Boolean>> reachMap = new HashMap<ModelGraphVertex, Map<ModelGraphVertex,Boolean>>();
		List<ModelGraphVertex> vertexs =  this._cfp.getVerticeList();
		for(int i = 0; i < vertexs.size(); ++i) {
			ModelGraphVertex u = vertexs.get(i);
			reachMap.put(u, new HashMap<ModelGraphVertex, Boolean>());
			for(int j = 0; j < vertexs.size(); ++j) {
				ModelGraphVertex v = vertexs.get(j);
				reachMap.get(u).put(v, false);
			}
		}
		ModelGraphVertex source = this._cfp.getSource();
		ModelGraphVertex sink = this._pn.getSink();
		List<ModelGraphVertex> trace = new ArrayList<ModelGraphVertex>();
		dfsReachMap(source, trace, sink, reachMap);
		return reachMap;
	}
	
	private void dfsReachMap(ModelGraphVertex u, List<ModelGraphVertex> trace,
			ModelGraphVertex sink, 
			Map<ModelGraphVertex, Map<ModelGraphVertex, Boolean>> reachMap) {
		if(this._loopJoinConditions.contains(u)) {
			generateReachMap(trace, reachMap);
		} else if(u.getSuccessors().isEmpty() && u instanceof Condition
				&& ((Condition) u).getOriginalPlace() == sink) {
			Condition sinkCondition = (Condition) u;
			if(sinkCondition.isCutoffPost()) {
				sinkCondition = sinkCondition.getCorrespondingCondition();
			}
			trace.add(sinkCondition);
			generateReachMap(trace, reachMap);
			trace.remove(trace.size() - 1);
		} else if(u.getSuccessors().isEmpty() && u instanceof Condition
				&& ((Condition) u).isCutoffPost()) {
			u = ((Condition) u).getCorrespondingCondition();
			dfsReachMap(u, trace, sink, reachMap);
		} else {
			trace.add(u);
			Set<ModelGraphVertex> uSuccSet = u.getSuccessors();
			for(ModelGraphVertex uSucc : uSuccSet) {
				dfsReachMap(uSucc, trace, sink, reachMap);
			}
			trace.remove(trace.size() - 1);
		}		
	}
	
	private void generateReachMap(List<ModelGraphVertex> trace, 
			Map<ModelGraphVertex, Map<ModelGraphVertex, Boolean>> reachMap) {
		for(int i = 0; i < trace.size(); ++i) {
			ModelGraphVertex u = trace.get(i);
			for(int j = i + 1; j < trace.size(); ++j) {
				ModelGraphVertex v = trace.get(j);
				reachMap.get(u).put(v, true);
			}
		}
	}
	
	public Map<ModelGraphVertex, Map<ModelGraphVertex, Boolean>> getReachMap() {
		return reachMap;
	}

	public Map<ModelGraphVertex, Map<ModelGraphVertex, Set<ModelGraphVertex>>> getLcpMap() {
		return lcpMap;
	}

	public Map<ModelGraphVertex, Map<ModelGraphVertex, Set<ModelGraphVertex>>> getLcsMap() {
		return lcsMap;
	}

	public Map<ModelGraphVertex, Map<ModelGraphVertex, Boolean>> getForwardSkip() {
		return forwardSkip;
	}

	public Map<ModelGraphVertex, Map<ModelGraphVertex, Boolean>> getBackwardSkip() {
		return backwardSkip;
	}

}
