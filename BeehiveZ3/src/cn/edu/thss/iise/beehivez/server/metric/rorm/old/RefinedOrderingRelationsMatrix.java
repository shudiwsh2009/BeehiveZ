package cn.edu.thss.iise.beehivez.server.metric.rorm.old;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.framework.models.ModelGraphVertex;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Place;
import org.processmining.framework.models.petrinet.Token;
import org.processmining.framework.models.petrinet.Transition;

import cn.edu.thss.iise.beehivez.server.metric.rorm.RefinedOrderingRelation;
import cn.edu.thss.iise.beehivez.server.metric.rorm.Relation;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.lc.LeastCommonPredecessorsAndSuccessors;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.CompleteFinitePrefix;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.CompleteFinitePrefixBuilder;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.Condition;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.Event;

public class RefinedOrderingRelationsMatrix {

	public static final int FORWARD = 1;
	public static final int BACKWARD = 0;

	private PetriNet _pn;
	private CompleteFinitePrefix _cfp;
	private RefinedOrderingRelation[][] followMatrix;
	private RefinedOrderingRelation[][] precedeMatrix;
	private RefinedOrderingRelation[][] parallelMatrix;
	private List<String> tName;

	private Set<Condition> _loopJoinConditions = new HashSet<Condition>();
	// used to store all the traces from a to b
	private Map<ModelGraphVertex, Map<ModelGraphVertex, Set<List<ModelGraphVertex>>>> _forwardTraces = new HashMap<ModelGraphVertex, Map<ModelGraphVertex, Set<List<ModelGraphVertex>>>>();
	private Map<ModelGraphVertex, Map<ModelGraphVertex, Set<List<ModelGraphVertex>>>> _backwardTraces = new HashMap<ModelGraphVertex, Map<ModelGraphVertex, Set<List<ModelGraphVertex>>>>();
	private List<List<ModelGraphVertex>> _allTraces = new ArrayList<List<ModelGraphVertex>>();
	private LeastCommonPredecessorsAndSuccessors _lc;

	public RefinedOrderingRelationsMatrix(PetriNet pn) {
		this._pn = pn;
		// extendPetriNet();
		buildCompleteFinitePrefix();
		this._lc = new LeastCommonPredecessorsAndSuccessors(this._cfp);
		this._loopJoinConditions = getLoopJoinConditions();
		getTransitionNames();
		this.followMatrix = new RefinedOrderingRelation[this.tName.size()][this.tName
				.size()];
		this.precedeMatrix = new RefinedOrderingRelation[this.tName.size()][this.tName
				.size()];
		this.parallelMatrix = new RefinedOrderingRelation[this.tName.size()][this.tName
				.size()];
		for (int i = 0; i < this.tName.size(); ++i) {
			for (int j = 0; j < this.tName.size(); ++j) {
				this.followMatrix[i][j] = new RefinedOrderingRelation(
						Relation.NEVER, false);
				this.precedeMatrix[i][j] = new RefinedOrderingRelation(
						Relation.NEVER, false);
				this.parallelMatrix[i][j] = new RefinedOrderingRelation(
						Relation.NEVER, false);
			}
		}
		generateFollowAndPrecedeMatrix();
		generateParallelMatrix();
	}

	private void generateFollowAndPrecedeMatrix() {
		List<Transition> alVisTransitions = this._pn.getVisibleTasks();
		Place sinkPlace = (Place) this._pn.getSink();
		ArrayList<Condition> sinkConditions = (ArrayList<Condition>) sinkPlace.object;
		Condition sinkCondition = sinkConditions.get(0);
		if (sinkCondition.isCutoffPost()) {
			sinkCondition = sinkCondition.getCorrespondingCondition();
		}
		Condition sourceCondition = (Condition) this._cfp.getSource();
		for (int i = 0; i < alVisTransitions.size(); ++i) {
			Transition fromTransition = alVisTransitions.get(i);
			ArrayList<Event> fromEvents = (ArrayList<Event>) fromTransition.object;
			for (int j = 0; j < alVisTransitions.size(); ++j) {
				Transition toTransition = alVisTransitions.get(j);
				ArrayList<Event> toEvents = (ArrayList<Event>) toTransition.object;

				// if a == b && there is a trace a -> b, then a ->(S) b
				if (i == j) {
					boolean selfLoop = false;
					for (Event a : fromEvents) {
						for (Event b : toEvents) {
							if (findTrace(a, b, new HashSet<ModelGraphVertex>()) != null) {
								this.followMatrix[this.tName
										.indexOf(fromTransition.getIdentifier())][this.tName
										.indexOf(toTransition.getIdentifier())].relation = Relation.SOMETIMES;
								this.precedeMatrix[this.tName
										.indexOf(fromTransition.getIdentifier())][this.tName
										.indexOf(toTransition.getIdentifier())].relation = Relation.SOMETIMES;
								selfLoop = true;
							}
						}
					}
					if (!selfLoop) {
						this.followMatrix[this.tName.indexOf(fromTransition
								.getIdentifier())][this.tName
								.indexOf(toTransition.getIdentifier())].relation = Relation.NEVER;
						this.precedeMatrix[this.tName.indexOf(fromTransition
								.getIdentifier())][this.tName
								.indexOf(toTransition.getIdentifier())].relation = Relation.NEVER;
					}
					continue;
				}

				// used for debuging
				if (fromTransition.getIdentifier().equals("d")
						&& toTransition.getIdentifier().equals("o")) {
					System.out.println();
				}

				// a may have some shadow events which need to be checked one by
				// one
				// in the meanwhile, we only need to check one of b shadow
				// events
				int forwardCount = 0;
				boolean hasSkipOrLoopForwardTrace = false;
				for (Event a : fromEvents) {
					List<ModelGraphVertex> forwardTrace = null;
					for (Event b : toEvents) {
						// find one trace a -> b
						boolean hasFoundATrace = false;
						forwardTrace = findTrace(a, b,
								new HashSet<ModelGraphVertex>());
						if (forwardTrace != null) {
							++forwardCount;
							hasFoundATrace = true;
							break;
						}
					}
					// check if there is a trace from a->a or a->sink
					// if so, end this loop
					if (forwardTrace != null
							&& !hasSkipOrLoopForwardTrace
							&& hasSkipOrLoopTrace(forwardTrace, FORWARD,
									sinkCondition)) {
						hasSkipOrLoopForwardTrace = true;
						break;
					}
				}

				// used for debuging
				// if(fromTransition.getIdentifier().equals("T2")
				// && toTransition.getIdentifier().equals("T3")) {
				// System.out.println();
				// }

				int backwardCount = 0;
				boolean hasSkipOrLoopBackwardTrace = false;
				for (Event a : fromEvents) {
					List<ModelGraphVertex> backwardTrace = null;
					for (Event b : toEvents) {
						// find one trace a <- b
						boolean hasFoundATrace = false;
						backwardTrace = findTrace(b, a,
								new HashSet<ModelGraphVertex>());
						if (backwardTrace != null) {
							++backwardCount;
							hasFoundATrace = true;
							break;
						}
					}
					// check if there is a trace from a <- a or source <- a
					// if so, end this loop
					if (backwardTrace != null
							&& !hasSkipOrLoopBackwardTrace
							&& hasSkipOrLoopTrace(backwardTrace, BACKWARD,
									sourceCondition)) {
						hasSkipOrLoopBackwardTrace = true;
						break;
					}
				}

				// if there is no trace from a to b, a->(NEVER)->b
				// else if there is trace from part of a to b, a->(SOMETIMES)->b
				// else if there is always trace from a to b but some can be
				// skiped or looped, a->(SOMETIMES)->b
				// else if there is always trace from a to b and cannot be
				// skiped or looped, a->(ALWAYS)->b
				if (forwardCount == 0) {
					this.followMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.NEVER;
				} else if (forwardCount < fromEvents.size()) {

					this.followMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.SOMETIMES;
				} else if (hasSkipOrLoopForwardTrace) {
					this.followMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.SOMETIMES;
				} else {
					this.followMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.ALWAYS;
				}

				// analogously, a <- b has four cases
				if (backwardCount == 0) {
					this.precedeMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.NEVER;
				} else if (backwardCount < fromEvents.size()) {
					this.precedeMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.SOMETIMES;
				} else if (hasSkipOrLoopBackwardTrace) {
					this.precedeMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.SOMETIMES;
				} else {
					this.precedeMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.ALWAYS;
				}
			}
		}
	}

	private boolean hasSkipOrLoopTrace(List<ModelGraphVertex> trace,
			int direction, Condition _endCondition) {
		// a -> b FORWARD and a <- b BACKWARD
		ModelGraphVertex startVertex = trace.get(0);
		ModelGraphVertex endVertex = trace.get(trace.size() - 1);
		if (direction == FORWARD) {
			// self-loop
			if (startVertex.getIdentifier().equals(endVertex.getIdentifier())) {
				return true;
			}
			// check all the XOR-split conditions in this trace
			// if there is another trace from the XOR-split to a or sink without
			// b, return true
			// else return false
			for (int i = 1; i < trace.size() - 1; ++i) {
				ModelGraphVertex curVertex = trace.get(i);
				if (curVertex instanceof Condition) {
					Condition curCondition = (Condition) curVertex;
					if (curCondition.isCutoffPost()) {
						curCondition = curCondition.getCorrespondingCondition();
					}
					if (curCondition.getSuccessors().size() > 1) {
						Iterator<ModelGraphVertex> itCurSucc = curCondition
								.getSuccessors().iterator();
						while (itCurSucc.hasNext()) {
							ModelGraphVertex succ = itCurSucc.next();
							if (succ != trace.get(i + 1)) {
								// if has a loop
								// the loop must not contain endVertex
								Set<ModelGraphVertex> visited = new HashSet<ModelGraphVertex>();
								visited.add(endVertex);
								if (findTrace(succ, startVertex, visited) != null) {
									return true;
								}
								// if succ skips endVertex
								if (this._lc.getForwardSkip().get(succ)
										.get(endVertex)) {
									return true;
								}
							}
						}
					}
				}
			}
		} else if (direction == BACKWARD) {
			// self-loop
			if (startVertex.getIdentifier().equals(endVertex.getIdentifier())) {
				return true;
			}
			// check all the XOR-join conditions in this trace
			// if there is another backward trace from the XOR-join to a or
			// source without b, return true
			// else return false
			for (int i = trace.size() - 2; i > 0; --i) {
				ModelGraphVertex curVertex = trace.get(i);
				if (curVertex instanceof Condition) {
					Condition curCondition = (Condition) curVertex;
					Set<Condition> mappingConditions = new HashSet<Condition>();
					if (curCondition.getMappingConditions() != null) {
						mappingConditions.addAll(curCondition
								.getMappingConditions());
					} else {
						mappingConditions.add(curCondition);
					}
					if (mappingConditions.size() > 1) {
						for (Condition cur : mappingConditions) {
							ModelGraphVertex pred = (ModelGraphVertex) cur
									.getPredecessors().iterator().next();
							if (pred != trace.get(i - 1)) {
								// if has a loop
								// the loop must not contain startVertex
								Set<ModelGraphVertex> visited = new HashSet<ModelGraphVertex>();
								visited.add(startVertex);
								if (findTrace(endVertex, pred, visited) != null) {
									return true;
								}
								// if pred skips startVertex
								if (this._lc.getBackwardSkip().get(pred)
										.get(startVertex)) {
									return true;
								}
							}
						}
					}
				}
			}
		}
		return false;
	}

	private void generateParallelMatrix() {
		List<Transition> alVisTransitions = this._pn.getVisibleTasks();
		for (int i = 0; i < alVisTransitions.size(); ++i) {
			Transition fromTransition = alVisTransitions.get(i);
			ArrayList<Event> fromEvents = (ArrayList<Event>) fromTransition.object;
			for (int j = i + 1; j < alVisTransitions.size(); ++j) {
				Transition toTransition = alVisTransitions.get(j);
				ArrayList<Event> toEvents = (ArrayList<Event>) toTransition.object;

				// a may have some shadow events which need to be checked one by
				// one
				// when determining the relation of a||b
				Map<ModelGraphVertex, Map<ModelGraphVertex, ModelGraphVertex>> aParallelIn = new HashMap<ModelGraphVertex, Map<ModelGraphVertex, ModelGraphVertex>>();
				Map<ModelGraphVertex, Map<ModelGraphVertex, ModelGraphVertex>> bParallelIn = new HashMap<ModelGraphVertex, Map<ModelGraphVertex, ModelGraphVertex>>();
				for (Event a : fromEvents) {
					for (Event b : toEvents) {
						boolean hasFoundAParallel = false;
						Set<ModelGraphVertex> lcpSet = this._lc.getLcpMap()
								.get(a).get(b);
						for (ModelGraphVertex lcp : lcpSet) {
							if (lcp instanceof Event && lcp != a && lcp != b) {
								aParallelIn
										.put(a,
												new HashMap<ModelGraphVertex, ModelGraphVertex>());
								aParallelIn.get(a).put(b, lcp);
								hasFoundAParallel = true;
								break;
							}
						}
						if (hasFoundAParallel) {
							break;
						}
					}
				}
				for (Event b : toEvents) {
					for (Event a : fromEvents) {
						boolean hasFoundAParallel = false;
						Set<ModelGraphVertex> lcpSet = this._lc.getLcpMap()
								.get(b).get(a);
						for (ModelGraphVertex lcp : lcpSet) {
							if (lcp instanceof Event && lcp != b && lcp != a) {
								bParallelIn
										.put(b,
												new HashMap<ModelGraphVertex, ModelGraphVertex>());
								bParallelIn.get(b).put(a, lcp);
								hasFoundAParallel = true;
								break;
							}
						}
						if (hasFoundAParallel) {
							break;
						}
					}
				}

				boolean aHasSometimesParallel = false;
				boolean bHasSometimesParallel = false;
				Iterator<Map.Entry<ModelGraphVertex, Map<ModelGraphVertex, ModelGraphVertex>>> outerIter = aParallelIn
						.entrySet().iterator();
				while (outerIter.hasNext()
						&& (!aHasSometimesParallel || !bHasSometimesParallel)) {
					Map.Entry<ModelGraphVertex, Map<ModelGraphVertex, ModelGraphVertex>> outerEntry = outerIter
							.next();
					Event a = (Event) outerEntry.getKey();
					Iterator<Map.Entry<ModelGraphVertex, ModelGraphVertex>> innerIter = outerEntry
							.getValue().entrySet().iterator();
					while (innerIter.hasNext()
							&& (!aHasSometimesParallel || !bHasSometimesParallel)) {
						Map.Entry<ModelGraphVertex, ModelGraphVertex> innerEntry = innerIter
								.next();

						// used for debuging
						if (fromTransition.getIdentifier().equals("d")
								&& toTransition.getIdentifier().equals("b")) {
							System.out.println();
						}

						Event b = (Event) innerEntry.getKey();
						Event lcp = (Event) innerEntry.getValue();
						boolean[] result = hasSometimesParallel(a, b, lcp);
						aHasSometimesParallel = result[0] ? result[0]
								: aHasSometimesParallel;
						bHasSometimesParallel = result[1] ? result[1]
								: bHasSometimesParallel;
					}
				}

				if (aParallelIn.size() == 0) {
					this.parallelMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.NEVER;
				} else if (aParallelIn.size() < fromEvents.size()) {
					this.parallelMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.SOMETIMES;
				} else if (aHasSometimesParallel) {
					this.parallelMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.SOMETIMES;
				} else {
					this.parallelMatrix[this.tName.indexOf(fromTransition
							.getIdentifier())][this.tName.indexOf(toTransition
							.getIdentifier())].relation = Relation.ALWAYS;
				}

				if (bParallelIn.size() == 0) {
					this.parallelMatrix[this.tName.indexOf(toTransition
							.getIdentifier())][this.tName
							.indexOf(fromTransition.getIdentifier())].relation = Relation.NEVER;
				} else if (bParallelIn.size() < toEvents.size()) {
					this.parallelMatrix[this.tName.indexOf(toTransition
							.getIdentifier())][this.tName
							.indexOf(fromTransition.getIdentifier())].relation = Relation.SOMETIMES;
				} else if (bHasSometimesParallel) {
					this.parallelMatrix[this.tName.indexOf(toTransition
							.getIdentifier())][this.tName
							.indexOf(fromTransition.getIdentifier())].relation = Relation.SOMETIMES;
				} else {
					this.parallelMatrix[this.tName.indexOf(toTransition
							.getIdentifier())][this.tName
							.indexOf(fromTransition.getIdentifier())].relation = Relation.ALWAYS;
				}
			}
		}
	}

	private boolean[] hasSometimesParallel(Event a, Event b, Event lcp) {
		boolean aHasSometimesParallel = false;
		boolean bHasSometimesParallel = false;
		Condition sourceCondition = (Condition) this._cfp.getSource();
		List<ModelGraphVertex> aTrace = findTrace(lcp, a,
				new HashSet<ModelGraphVertex>());
		List<ModelGraphVertex> bTrace = findTrace(lcp, b,
				new HashSet<ModelGraphVertex>());
		// find all the event after xor-split who can skips a|b
		Set<ModelGraphVertex> aSkipSplits = new HashSet<ModelGraphVertex>();
		Set<ModelGraphVertex> bSkipSplits = new HashSet<ModelGraphVertex>();
		// and all the event before xor-join who can skip lcp or loop a|b
		Set<ModelGraphVertex> aSkipJoins = new HashSet<ModelGraphVertex>();
		Set<ModelGraphVertex> bSkipJoins = new HashSet<ModelGraphVertex>();
		for (int i = 1; i < aTrace.size() - 1; ++i) {
			ModelGraphVertex curVertex = aTrace.get(i);
			if (curVertex instanceof Condition) {
				Condition curCondition = (Condition) curVertex;
				// xor-split
				if (curCondition.isCutoffPost()) {
					curCondition = curCondition.getCorrespondingCondition();
				}
				if (curCondition.getSuccessors().size() > 1) {
					Iterator<ModelGraphVertex> itCurSucc = curCondition
							.getSuccessors().iterator();
					while (itCurSucc.hasNext()) {
						ModelGraphVertex succ = itCurSucc.next();
						if (succ != aTrace.get(i + 1)
								&& this._lc.getForwardSkip().get(succ).get(a)) {
							aSkipSplits.add(succ);
						}
					}
				}
				// xor-join
				Set<Condition> mappingConditions = new HashSet<Condition>();
				if (curCondition.getMappingConditions() != null) {
					mappingConditions.addAll(curCondition
							.getMappingConditions());
				} else {
					mappingConditions.add(curCondition);
				}
				if (mappingConditions.size() > 1) {
					for (Condition cur : mappingConditions) {
						ModelGraphVertex pred = (ModelGraphVertex) cur
								.getPredecessors().iterator().next();
						if (pred != aTrace.get(i - 1)) {
							Set<ModelGraphVertex> visited = new HashSet<ModelGraphVertex>(
									aTrace);
							if (findTrace(sourceCondition, pred, visited) != null) {
								aSkipJoins.add(pred);
								continue;
							}
							visited = new HashSet<ModelGraphVertex>(
									aTrace.subList(0, aTrace.size() - 1));
							if (a == pred
									|| findTrace(a, pred, visited) != null) {
								aSkipJoins.add(pred);
								continue;
							}
						}
					}
				}
			}
		}
		for (int j = 1; j < bTrace.size() - 1; ++j) {
			ModelGraphVertex curVertex = bTrace.get(j);
			if (curVertex instanceof Condition) {
				Condition curCondition = (Condition) curVertex;
				// xor-split
				if (curCondition.isCutoffPost()) {
					curCondition = curCondition.getCorrespondingCondition();
				}
				if (curCondition.getSuccessors().size() > 1) {
					Iterator<ModelGraphVertex> itCurSucc = curCondition
							.getSuccessors().iterator();
					while (itCurSucc.hasNext()) {
						ModelGraphVertex succ = itCurSucc.next();
						if (succ != bTrace.get(j + 1)
								&& this._lc.getForwardSkip().get(succ).get(b)) {
							bSkipSplits.add(succ);
						}
					}
				}
				// xor-join
				Set<Condition> mappingConditions = new HashSet<Condition>();
				if (curCondition.getMappingConditions() != null) {
					mappingConditions.addAll(curCondition
							.getMappingConditions());
				} else {
					mappingConditions.add(curCondition);
				}
				if (mappingConditions.size() > 1) {
					for (Condition cur : mappingConditions) {
						ModelGraphVertex pred = (ModelGraphVertex) cur
								.getPredecessors().iterator().next();
						if (pred != aTrace.get(j - 1)) {
							Set<ModelGraphVertex> visited = new HashSet<ModelGraphVertex>(
									bTrace);
							if (findTrace(sourceCondition, pred, visited) != null) {
								bSkipJoins.add(pred);
								continue;
							}
							visited = new HashSet<ModelGraphVertex>(
									bTrace.subList(0, bTrace.size() - 1));
							if (b == pred
									|| findTrace(b, pred, visited) != null) {
								bSkipJoins.add(pred);
								continue;
							}
						}
					}
				}
			}
		}
		// check xor-split
		Iterator<ModelGraphVertex> itAi = aSkipSplits.iterator();
		while (itAi.hasNext()
				&& (!aHasSometimesParallel || !bHasSometimesParallel)) {
			ModelGraphVertex ai = itAi.next();
			Iterator<ModelGraphVertex> itBj = bSkipSplits.iterator();
			while (itBj.hasNext()
					&& (!aHasSometimesParallel || !bHasSometimesParallel)) {
				ModelGraphVertex bj = itBj.next();
				Set<ModelGraphVertex> _lcsSet = this._lc.getLcsMap().get(ai)
						.get(bj);
				boolean hasConditionLcs = false;
				for (ModelGraphVertex _lcs : _lcsSet) {
					if (_lcs instanceof Event
							&& this._lc.getReachMap().get(ai).get(_lcs)
							&& this._lc.getReachMap().get(bj).get(_lcs)) {
						aHasSometimesParallel = true;
						bHasSometimesParallel = true;
						return new boolean[] { true, true };
					} else if (_lcs instanceof Condition) {
						hasConditionLcs = true;
					}
				}
				if (!hasConditionLcs) {
					itAi.remove();
					itBj.remove();
				}
			}
		}
		if (!aSkipSplits.isEmpty()) {
			bHasSometimesParallel = true;
		}
		if (!bSkipSplits.isEmpty()) {
			aHasSometimesParallel = true;
		}
		// check xor-join
		itAi = aSkipJoins.iterator();
		while (itAi.hasNext()
				&& (!aHasSometimesParallel || !bHasSometimesParallel)) {
			ModelGraphVertex ai = itAi.next();
			Iterator<ModelGraphVertex> itBj = bSkipJoins.iterator();
			while (itBj.hasNext()
					&& (!aHasSometimesParallel || !bHasSometimesParallel)) {
				ModelGraphVertex bj = itBj.next();
				Set<ModelGraphVertex> _lcpSet = this._lc.getLcpMap().get(ai)
						.get(bj);
				boolean hasConditionLcp = false;
				for (ModelGraphVertex _lcp : _lcpSet) {
					if (_lcp instanceof Condition || _lcp == lcp) {
						hasConditionLcp = true;
						break;
					}
				}
				if (!hasConditionLcp) {
					itAi.remove();
					itBj.remove();
				}
			}
		}
		if (!aSkipJoins.isEmpty()) {
			aHasSometimesParallel = true;
		}
		if (!bSkipJoins.isEmpty()) {
			bHasSometimesParallel = true;
		}
		return new boolean[] { aHasSometimesParallel, bHasSometimesParallel };
	}

	/**
	 * find a trace from start to end return a trace without a loop xor-join if
	 * possible
	 * 
	 * @param start
	 * @param end
	 * @return
	 */
	private List<ModelGraphVertex> findTrace(ModelGraphVertex start,
			ModelGraphVertex end, Set<ModelGraphVertex> visited) {
		List<ModelGraphVertex> trace = new ArrayList<ModelGraphVertex>();
		ModelGraphVertex sink = this._pn.getSink();
		Map<List<ModelGraphVertex>, Boolean> traceMap = new HashMap<List<ModelGraphVertex>, Boolean>();
		dfsFindTrace(start, end, trace, sink, false, traceMap, visited);
		if (traceMap.size() == 0) {
			return null;
		}
		Iterator<Map.Entry<List<ModelGraphVertex>, Boolean>> it = traceMap
				.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<List<ModelGraphVertex>, Boolean> entry = it.next();
			if (entry.getValue() == false) {
				return entry.getKey();
			}
		}
		return traceMap.keySet().iterator().next();
	}

	/**
	 * dfs to find all the traces from start to end while marking if the trace
	 * contains a loop xor-join
	 * 
	 * @param curVertex
	 * @param end
	 * @param trace
	 * @param sink
	 * @param containLoop
	 * @param traceMap
	 * @param visited
	 */
	private void dfsFindTrace(ModelGraphVertex curVertex, ModelGraphVertex end,
			List<ModelGraphVertex> trace, ModelGraphVertex sink,
			boolean containLoop, Map<List<ModelGraphVertex>, Boolean> traceMap,
			Set<ModelGraphVertex> visited) {
		if (curVertex == end && !trace.isEmpty()) {
			List<ModelGraphVertex> tmp = new ArrayList<ModelGraphVertex>(trace);
			tmp.add(curVertex);
			traceMap.put(tmp, containLoop);
			return;
		} else if (visited.contains(curVertex)) {
			return;
		} else if (curVertex.getSuccessors().isEmpty()
				&& curVertex instanceof Condition
				&& ((Condition) curVertex).getOriginalPlace() == sink) {
			return;
		} else if (this._loopJoinConditions.contains(curVertex)) {
			curVertex = ((Condition) curVertex).getCorrespondingCondition();
			dfsFindTrace(curVertex, end, trace, sink, true, traceMap, visited);
		} else if (curVertex.getSuccessors().isEmpty()
				&& curVertex instanceof Condition
				&& ((Condition) curVertex).isCutoffPost()) {
			curVertex = ((Condition) curVertex).getCorrespondingCondition();
			dfsFindTrace(curVertex, end, trace, sink, containLoop, traceMap,
					visited);
		} else {
			trace.add(curVertex);
			visited.add(curVertex);
			Set<ModelGraphVertex> curSuccSet = curVertex.getSuccessors();
			for (ModelGraphVertex curSucc : curSuccSet) {
				dfsFindTrace(curSucc, end, trace, sink, containLoop, traceMap,
						visited);
			}
			visited.remove(curVertex);
			trace.remove(trace.size() - 1);
		}
	}

	/**
	 * dfs to get all the XOR-join conditions which ends a loop
	 * 
	 * @param loopJoinConditions
	 */
	private Set<Condition> getLoopJoinConditions() {
		Set<Condition> loopJoinConditions = new HashSet<Condition>();
		Condition source = (Condition) this._cfp.getSource();
		Set<String> visited = new HashSet<String>();
		dfsLoopJoin(source, visited, loopJoinConditions);
		return loopJoinConditions;
	}

	private void dfsLoopJoin(ModelGraphVertex u, Set<String> visited,
			Set<Condition> loopJoinConditions) {
		if (u instanceof Event && visited.contains(u.getIdentifier())) {
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
		if (!visitAgain) {
			visited.remove(u.getIdentifier());
		}
	}

	/**
	 * Add $INTRAN$ and $OUTTRAN$ to Petri Net
	 */
	private void extendPetriNet() {
		Place source = (Place) this._pn.getSource();
		Place sink = (Place) this._pn.getSink();
		Place newSource = new Place("$SOURCE$", this._pn);
		Place newSink = new Place("$SINK$", this._pn);
		Transition newInTransition = new Transition("$INTRAN$", this._pn);
		Transition newOutTransition = new Transition("$OUTTRAN$", this._pn);
		this._pn.addPlace(newSource);
		this._pn.addPlace(newSink);
		this._pn.addTransition(newInTransition);
		this._pn.addTransition(newOutTransition);
		this._pn.addEdge(newSource, newInTransition);
		this._pn.addEdge(newInTransition, source);
		this._pn.addEdge(sink, newOutTransition);
		this._pn.addEdge(newOutTransition, newSink);
	}

	/**
	 * Generate the cut-off unfolding of Petri Net
	 */
	private void buildCompleteFinitePrefix() {
		for (Place place : this._pn.getPlaces()) {
			place.removeAllTokens();
			if (place.inDegree() == 0) {
				place.addToken(new Token());
			}
		}
		this._cfp = CompleteFinitePrefixBuilder.build(this._pn);
//		CompleteFinitePrefixPostProcessor.postProcess(this._cfp);
	}

	private void getTransitionNames() {
		this.tName = new ArrayList<String>();
		List<Transition> alTransitions = this._pn.getTransitions();
		for (Transition t : alTransitions) {
			if (!t.isInvisibleTask()) {
				this.tName.add(t.getIdentifier());
			}
		}
	}

	public void printMatrix() {
		int n = this.tName.size();
		System.out.print("[");
		for (int i = 0; i < n - 1; ++i) {
			System.out.print("\"" + this.tName.get(i) + "\", ");
		}
		System.out.println("\"" + this.tName.get(n - 1) + "\"]");

		System.out.println("Follow Matrix");
		for (int i = 0; i < n; ++i) {
			for (int j = 0; j < n; ++j) {
				System.out.print(this.followMatrix[i][j].toString() + " ");
			}
			System.out.println();
		}

		System.out.println("Precede Matrix");
		for (int i = 0; i < n; ++i) {
			for (int j = 0; j < n; ++j) {
				System.out.print(this.precedeMatrix[i][j].toString() + " ");
			}
			System.out.println();
		}

		System.out.println("Parallel Matrix");
		for (int i = 0; i < n; ++i) {
			for (int j = 0; j < n; ++j) {
				System.out.print(this.parallelMatrix[i][j].toString() + " ");
			}
			System.out.println();
		}
	}

	private void printAllTraces() {
		// print traces
		List<ModelGraphVertex> vertexs = this._cfp.getVerticeList();
		Iterator<ModelGraphVertex> itVertex = vertexs.iterator();
		while (itVertex.hasNext()) {
			ModelGraphVertex v = itVertex.next();
			if (v instanceof Condition && ((Condition) v).isCutoffPost()) {
				itVertex.remove();
			}
		}
		for (int i = 0; i < vertexs.size(); ++i) {
			for (int j = 0; j < vertexs.size(); ++j) {
				ModelGraphVertex a = vertexs.get(i);
				ModelGraphVertex b = vertexs.get(j);
				if (this._forwardTraces.containsKey(a)
						&& this._forwardTraces.get(a).containsKey(b)) {
					System.out.println("$$ " + a.getIdentifier() + " -> "
							+ b.getIdentifier() + " $$");
					Set<List<ModelGraphVertex>> traces = this._forwardTraces
							.get(a).get(b);
					for (List<ModelGraphVertex> trace : traces) {
						for (ModelGraphVertex v : trace) {
							System.out.print(v.getIdentifier() + " -> ");
						}
						System.out.println();
					}
					System.out.println();
				}
			}
		}
	}

	public RefinedOrderingRelation[][] getFollowMatrix() {
		return followMatrix;
	}

	public RefinedOrderingRelation[][] getPrecedeMatrix() {
		return precedeMatrix;
	}

	public RefinedOrderingRelation[][] getParallelMatrix() {
		return parallelMatrix;
	}

	public List<String> gettName() {
		return tName;
	}

}
