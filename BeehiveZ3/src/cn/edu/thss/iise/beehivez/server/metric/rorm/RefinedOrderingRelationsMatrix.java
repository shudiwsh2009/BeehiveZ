package cn.edu.thss.iise.beehivez.server.metric.rorm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpt.petri.INode;
import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Place;
import org.jbpt.petri.Transition;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Condition;
import org.jbpt.petri.unfolding.Event;
import org.jbpt.petri.unfolding.IBPNode;
import org.processmining.framework.models.petrinet.PetriNet;

import cn.edu.thss.iise.beehivez.server.metric.rorm.jbpt.conversion.PetriNetConversion;
import cn.edu.thss.iise.beehivez.server.metric.rorm.lc.LeastCommonPredecessorsAndSuccessors;

@SuppressWarnings("rawtypes")
public class RefinedOrderingRelationsMatrix {

	public static final int FORWARD = 1;
	public static final int BACKWARD = 0;

	private NetSystem _sys;
	private CompletePrefixUnfolding _cpu;
	private RefinedOrderingRelation[][] causalMatrix;
	private RefinedOrderingRelation[][] inverseCausalMatrix;
	private RefinedOrderingRelation[][] concurrentMatrix;
	private List<String> tName;

	private Set<Condition> _loopJoinConditions = new HashSet<Condition>();
	private LeastCommonPredecessorsAndSuccessors _lc;
	
	public RefinedOrderingRelationsMatrix(PetriNet pn) {
		this(PetriNetConversion.convert(pn));
	}

	public RefinedOrderingRelationsMatrix(NetSystem sys) {
		this._sys = sys;
		this._cpu = new CompletePrefixUnfolding(this._sys);
		this._lc = new LeastCommonPredecessorsAndSuccessors(this._cpu);
		this._loopJoinConditions = getLoopJoinConditions();
		getTransitionNames();
		this.causalMatrix = new RefinedOrderingRelation[this.tName.size()][this.tName
				.size()];
		this.inverseCausalMatrix = new RefinedOrderingRelation[this.tName
				.size()][this.tName.size()];
		this.concurrentMatrix = new RefinedOrderingRelation[this.tName.size()][this.tName
				.size()];
		for (int i = 0; i < this.tName.size(); ++i) {
			for (int j = 0; j < this.tName.size(); ++j) {
				this.causalMatrix[i][j] = new RefinedOrderingRelation(
						Relation.NEVER, false);
				this.inverseCausalMatrix[i][j] = new RefinedOrderingRelation(
						Relation.NEVER, false);
				this.concurrentMatrix[i][j] = new RefinedOrderingRelation(
						Relation.NEVER, false);
			}
		}
		generateCausalAndInverseCausalMatrix();
		generateConcurrentMatrix();
	}

	private void generateCausalAndInverseCausalMatrix() {
		List<Transition> alObTransitions = new ArrayList<Transition>(
				this._sys.getObservableTransitions());
		Place sinkPlace = this._sys.getSinkPlaces().iterator().next();
		Condition sinkCondition = this._cpu.getConditions(sinkPlace).iterator()
				.next();
		if (sinkCondition.isCutoffPost()) {
			sinkCondition = sinkCondition.getCorrespondingCondition();
		}
		Place sourcePlace = this._sys.getSourcePlaces().iterator().next();
		Condition sourceCondition = this._cpu.getConditions(sourcePlace)
				.iterator().next();
		if (sourceCondition.isCutoffPost()) {
			sourceCondition = sourceCondition.getCorrespondingCondition();
		}
		for (int i = 0; i < alObTransitions.size(); ++i) {
			Transition fromTransition = alObTransitions.get(i);
			Set<Event> fromEvents = this._cpu.getEvents(fromTransition);
			for (int j = 0; j < alObTransitions.size(); ++j) {
				Transition toTransition = alObTransitions.get(j);
				Set<Event> toEvents = this._cpu.getEvents(toTransition);

				// if a == b && there is a trace a -> b, then a ->(S) b
				if (i == j) {
					boolean selfLoop = false;
					for (Event a : fromEvents) {
						for (Event b : toEvents) {
							if (findTrace(a, b, new HashSet<IBPNode>()) != null) {
								this.causalMatrix[this.tName
										.indexOf(fromTransition.getLabel())][this.tName
										.indexOf(toTransition.getLabel())].relation = Relation.SOMETIMES;
								this.inverseCausalMatrix[this.tName
										.indexOf(fromTransition.getLabel())][this.tName
										.indexOf(toTransition.getLabel())].relation = Relation.SOMETIMES;
								selfLoop = true;
							}
						}
					}
					if (!selfLoop) {
						this.causalMatrix[this.tName.indexOf(fromTransition
								.getLabel())][this.tName.indexOf(toTransition
								.getLabel())].relation = Relation.NEVER;
						this.inverseCausalMatrix[this.tName
								.indexOf(fromTransition.getLabel())][this.tName
								.indexOf(toTransition.getLabel())].relation = Relation.NEVER;
					}
					continue;
				}

//				if(fromTransition.getLabel().equals("a")
//						&& toTransition.getLabel().equals("o")) {
//					int a = 1;
//				}
				// a may have some shadow events
				// which need to be checked one by one
				// in the meanwhile
				// we only need to check one of b shadow events
				int forwardCount = 0;
				boolean hasSkipOrLoopForwardTrace = false;
				for (Event a : fromEvents) {
					List<IBPNode> forwardTrace = null;
					for (Event b : toEvents) {
						// find one trace a -> b
						forwardTrace = findTrace(a, b, new HashSet<IBPNode>());
						if (forwardTrace != null) {
							++forwardCount;
							break;
						}
					}
					// check if there is a trace from a -> a or a -> sink
					// if so, end this loop
					if (forwardTrace != null
							&& !hasSkipOrLoopForwardTrace
							&& hasSkipOrLoopTrace(forwardTrace, FORWARD,
									sinkCondition)) {
						hasSkipOrLoopForwardTrace = true;
						break;
					}
				}

				int backwardCount = 0;
				boolean hasSkipOrLoopBackwardTrace = false;
				for (Event a : fromEvents) {
					List<IBPNode> backwardTrace = null;
					for (Event b : toEvents) {
						// find one trace a <- b
						backwardTrace = findTrace(b, a, new HashSet<IBPNode>());
						if (backwardTrace != null) {
							++backwardCount;
							break;
						}
					}
					// check if there is a trace a <- a or source <- a
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
					this.causalMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.NEVER;
				} else if (forwardCount < fromEvents.size()) {

					this.causalMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.SOMETIMES;
				} else if (hasSkipOrLoopForwardTrace) {
					this.causalMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.SOMETIMES;
				} else {
					this.causalMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.ALWAYS;
				}

				// analogously, a <- b has four cases
				if (backwardCount == 0) {
					this.inverseCausalMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.NEVER;
				} else if (backwardCount < fromEvents.size()) {
					this.inverseCausalMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.SOMETIMES;
				} else if (hasSkipOrLoopBackwardTrace) {
					this.inverseCausalMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.SOMETIMES;
				} else {
					this.inverseCausalMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.ALWAYS;
				}
			}
		}
	}

	private boolean hasSkipOrLoopTrace(List<IBPNode> trace, int direction,
			Condition _endCondition) {
		// a -> b FORWARD and a <- b BACKWARD
		IBPNode start = trace.get(0);
		IBPNode end = trace.get(trace.size() - 1);
		if (direction == FORWARD) {
			// self-loop
			if (start.getPetriNetNode() == end.getPetriNetNode()) {
				return true;
			}
			// check all the XOR-split conditions in this trace
			// if there is another trace from the XOR-split to a or sink
			// without b, return true
			// else return false
			for (int i = 1; i < trace.size() - 1; ++i) {
				IBPNode cur = trace.get(i);
				if (cur instanceof Condition) {
					Condition curCondition = (Condition) cur;
					if (curCondition.isCutoffPost()) {
						curCondition = curCondition.getCorrespondingCondition();
					}
					if (curCondition.getPostE().size() > 1) {
						Iterator<Event> itCurSucc = curCondition.getPostE()
								.iterator();
						while (itCurSucc.hasNext()) {
							IBPNode succ = itCurSucc.next();
							if (succ != trace.get(i + 1)) {
								// if has a loop
								// the loop must not contain end
								Set<IBPNode> visited = new HashSet<IBPNode>();
								visited.add(end);
								if (findTrace(succ, start, visited) != null) {
									return true;
								}
								// if succ skips end
								if (this._lc.getForwardSkip().get(succ)
										.get(end)) {
									return true;
								}
							}
						}
					}
				}
			}
		} else if (direction == BACKWARD) {
			// self-loop
			if (start.getPetriNetNode() == end.getPetriNetNode()) {
				return true;
			}
			// check all the XOR-join conditions in this trace
			// if there is another backward trace from the XOR-join to a or
			// source without b, return true
			// else return false
			for (int i = trace.size() - 2; i > 0; --i) {
				IBPNode cur = trace.get(i);
				if (cur instanceof Condition) {
					Condition curCondition = (Condition) cur;
					Set<Condition> mappingConditions = new HashSet<Condition>();
					if (curCondition.getMappingConditions() != null) {
						mappingConditions.addAll(curCondition
								.getMappingConditions());
					} else {
						mappingConditions.add(curCondition);
					}
					if (mappingConditions.size() > 1) {
						for (Condition curC : mappingConditions) {
							IBPNode pred = curC.getPreEvent();
							if (pred != trace.get(i - 1)) {
								// if has a loop
								// the loop must not contain start
								Set<IBPNode> visited = new HashSet<IBPNode>();
								visited.add(start);
								if (findTrace(end, pred, visited) != null) {
									return true;
								}
								// if pred skips start
								if (this._lc.getBackwardSkip().get(pred)
										.get(start)) {
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

	private void generateConcurrentMatrix() {
		List<Transition> alObTransitions = new ArrayList<Transition>(
				this._sys.getObservableTransitions());
		for (int i = 0; i < alObTransitions.size(); ++i) {
			Transition fromTransition = alObTransitions.get(i);
			Set<Event> fromEvents = this._cpu.getEvents(fromTransition);
			for (int j = i + 1; j < alObTransitions.size(); ++j) {
				Transition toTransition = alObTransitions.get(j);
				Set<Event> toEvents = this._cpu.getEvents(toTransition);

				// a may have some shadow events
				// which need to be checked one by one
				// when determining the relation of a||b
				Map<IBPNode, Map<IBPNode, IBPNode>> aConcurrentIn = new HashMap<IBPNode, Map<IBPNode, IBPNode>>();
				Map<IBPNode, Map<IBPNode, IBPNode>> bConcurrentIn = new HashMap<IBPNode, Map<IBPNode, IBPNode>>();
				for (Event a : fromEvents) {
					for (Event b : toEvents) {
						boolean hasFoundAConcurrent = false;
						Set<IBPNode> lcpSet = this._lc.getLcpMap().get(a)
								.get(b);
						for (IBPNode lcp : lcpSet) {
							if (lcp instanceof Event && lcp != a && lcp != b) {
								aConcurrentIn.put(a,
										new HashMap<IBPNode, IBPNode>());
								aConcurrentIn.get(a).put(b, lcp);
								hasFoundAConcurrent = true;
								break;
							}
						}
						if (hasFoundAConcurrent) {
							break;
						}
					}
				}
				for (Event b : toEvents) {
					for (Event a : toEvents) {
						boolean hasFoundAConcurrent = false;
						Set<IBPNode> lcpSet = this._lc.getLcpMap().get(b)
								.get(a);
						for (IBPNode lcp : lcpSet) {
							if (lcp instanceof Event && lcp != b && lcp != a) {
								bConcurrentIn.put(b,
										new HashMap<IBPNode, IBPNode>());
								bConcurrentIn.get(b).put(a, lcp);
								hasFoundAConcurrent = true;
								break;
							}
						}
						if (hasFoundAConcurrent) {
							break;
						}
					}
				}

				boolean aHasSometimesConcurrent = false;
				boolean bHasSometimesConcurrent = false;
				Iterator<Map.Entry<IBPNode, Map<IBPNode, IBPNode>>> outerIter = aConcurrentIn
						.entrySet().iterator();
				while (outerIter.hasNext()
						&& (!aHasSometimesConcurrent || !bHasSometimesConcurrent)) {
					Map.Entry<IBPNode, Map<IBPNode, IBPNode>> outerEntry = outerIter
							.next();
					Event a = (Event) outerEntry.getKey();
					Iterator<Map.Entry<IBPNode, IBPNode>> innerIter = outerEntry
							.getValue().entrySet().iterator();
					while (innerIter.hasNext()
							&& (!aHasSometimesConcurrent || !bHasSometimesConcurrent)) {
						Map.Entry<IBPNode, IBPNode> innerEntry = innerIter
								.next();
						Event b = (Event) innerEntry.getKey();
						Event lcp = (Event) innerEntry.getValue();
						boolean[] result = hasSometimesConcurrent(a, b, lcp);
						aHasSometimesConcurrent = result[0] ? result[0]
								: aHasSometimesConcurrent;
						bHasSometimesConcurrent = result[1] ? result[1]
								: bHasSometimesConcurrent;
					}
				}

				if (aConcurrentIn.size() == 0) {
					this.concurrentMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.NEVER;
				} else if (aConcurrentIn.size() < fromEvents.size()) {
					this.concurrentMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.SOMETIMES;
				} else if (aHasSometimesConcurrent) {
					this.concurrentMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.SOMETIMES;
				} else {
					this.concurrentMatrix[this.tName.indexOf(fromTransition
							.getLabel())][this.tName.indexOf(toTransition
							.getLabel())].relation = Relation.ALWAYS;
				}

				if (bConcurrentIn.size() == 0) {
					this.concurrentMatrix[this.tName.indexOf(toTransition
							.getLabel())][this.tName.indexOf(fromTransition
							.getLabel())].relation = Relation.NEVER;
				} else if (bConcurrentIn.size() < toEvents.size()) {
					this.concurrentMatrix[this.tName.indexOf(toTransition
							.getLabel())][this.tName.indexOf(fromTransition
							.getLabel())].relation = Relation.SOMETIMES;
				} else if (bHasSometimesConcurrent) {
					this.concurrentMatrix[this.tName.indexOf(toTransition
							.getLabel())][this.tName.indexOf(fromTransition
							.getLabel())].relation = Relation.SOMETIMES;
				} else {
					this.concurrentMatrix[this.tName.indexOf(toTransition
							.getLabel())][this.tName.indexOf(fromTransition
							.getLabel())].relation = Relation.ALWAYS;
				}
			}
		}
	}

	private boolean[] hasSometimesConcurrent(Event a, Event b, Event lcp) {
		boolean aHasSometimesConcurrent = false;
		boolean bHasSometimesConcurrent = false;
		Place sourcePlace = this._sys.getSourcePlaces().iterator().next();
		Condition sourceCondition = this._cpu.getConditions(sourcePlace)
				.iterator().next();
		List<IBPNode> aTrace = findTrace(lcp, a, new HashSet<IBPNode>());
		List<IBPNode> bTrace = findTrace(lcp, b, new HashSet<IBPNode>());
		// find all the events after xor-split who can skip a|b
		Set<IBPNode> aSkipSplits = new HashSet<IBPNode>();
		Set<IBPNode> bSkipSplits = new HashSet<IBPNode>();
		// and all the events before xor-join who can skip lcp or loop a|b
		Set<IBPNode> aSkipJoins = new HashSet<IBPNode>();
		Set<IBPNode> bSkipJoins = new HashSet<IBPNode>();
		for (int i = 1; i < aTrace.size() - 1; ++i) {
			IBPNode cur = aTrace.get(i);
			if (cur instanceof Condition) {
				Condition curCondition = (Condition) cur;
				// xor-split
				if (curCondition.isCutoffPost()) {
					curCondition = curCondition.getCorrespondingCondition();
				}
				if (curCondition.getPostE().size() > 1) {
					Iterator<Event> itCurSucc = curCondition.getPostE()
							.iterator();
					while (itCurSucc.hasNext()) {
						IBPNode succ = itCurSucc.next();
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
					for (Condition curC : mappingConditions) {
						Event pred = curC.getPreEvent();
						if (pred != aTrace.get(i - 1)) {
							Set<IBPNode> visited = new HashSet<IBPNode>(aTrace);
							if (findTrace(sourceCondition, pred, visited) != null) {
								aSkipJoins.add(pred);
								continue;
							}
							visited = new HashSet<IBPNode>(aTrace.subList(0,
									aTrace.size() - 1));
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
			IBPNode cur = bTrace.get(j);
			if (cur instanceof Condition) {
				Condition curCondition = (Condition) cur;
				// xor-split
				if (curCondition.isCutoffPost()) {
					curCondition = curCondition.getCorrespondingCondition();
				}
				if (curCondition.getPostE().size() > 1) {
					Iterator<Event> itCurSucc = curCondition.getPostE()
							.iterator();
					while (itCurSucc.hasNext()) {
						Event succ = itCurSucc.next();
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
					for (Condition curC : mappingConditions) {
						Event pred = curC.getPreEvent();
						if (pred != bTrace.get(j - 1)) {
							Set<IBPNode> visited = new HashSet<IBPNode>(bTrace);
							if (findTrace(sourceCondition, pred, visited) != null) {
								bSkipJoins.add(pred);
								continue;
							}
							visited = new HashSet<IBPNode>(bTrace.subList(0,
									bTrace.size() - 1));
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
		Iterator<IBPNode> itAi = aSkipSplits.iterator();
		while (itAi.hasNext()
				&& (!aHasSometimesConcurrent || !bHasSometimesConcurrent)) {
			IBPNode ai = itAi.next();
			Iterator<IBPNode> itBj = bSkipSplits.iterator();
			while (itBj.hasNext()
					&& (!aHasSometimesConcurrent || !bHasSometimesConcurrent)) {
				IBPNode bj = itBj.next();
				Set<IBPNode> _lcsSet = this._lc.getLcsMap().get(ai).get(bj);
				boolean hasConditionLcs = false;
				for (IBPNode _lcs : _lcsSet) {
					if (_lcs instanceof Event
							&& this._lc.getReachMap().get(ai).get(_lcs)
							&& this._lc.getReachMap().get(bj).get(_lcs)) {
						aHasSometimesConcurrent = true;
						bHasSometimesConcurrent = true;
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
			bHasSometimesConcurrent = true;
		}
		if (!bSkipSplits.isEmpty()) {
			aHasSometimesConcurrent = true;
		}
		// check xor-join
		itAi = aSkipJoins.iterator();
		while (itAi.hasNext()
				&& (!aHasSometimesConcurrent || !bHasSometimesConcurrent)) {
			IBPNode ai = itAi.next();
			Iterator<IBPNode> itBj = bSkipJoins.iterator();
			while (itBj.hasNext()
					&& (!aHasSometimesConcurrent || !bHasSometimesConcurrent)) {
				IBPNode bj = itBj.next();
				Set<IBPNode> _lcpSet = this._lc.getLcpMap().get(ai).get(bj);
				boolean hasConditionLcp = false;
				for (IBPNode _lcp : _lcpSet) {
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
			aHasSometimesConcurrent = true;
		}
		if (!bSkipJoins.isEmpty()) {
			bHasSometimesConcurrent = true;
		}
		return new boolean[] { aHasSometimesConcurrent, bHasSometimesConcurrent };
	}

	/**
	 * find a trace from start to end return a trace without a loop xor-join if
	 * possible
	 * 
	 * @param start
	 * @param end
	 * @param visited
	 * @return
	 */
	private List<IBPNode> findTrace(IBPNode start, IBPNode end,
			Set<IBPNode> visited) {
		List<IBPNode> trace = new ArrayList<IBPNode>();
		Place sinkPlace = this._sys.getSinkPlaces().iterator().next();
		Map<List<IBPNode>, Boolean> traceMap = new HashMap<List<IBPNode>, Boolean>();
		dfsFindTrace(start, end, trace, sinkPlace, false, traceMap, visited);
		if (traceMap.size() == 0) {
			return null;
		}
		Iterator<Map.Entry<List<IBPNode>, Boolean>> it = traceMap.entrySet()
				.iterator();
		while (it.hasNext()) {
			Map.Entry<List<IBPNode>, Boolean> entry = it.next();
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
	 * @param cur
	 * @param end
	 * @param trace
	 * @param sinkPlace
	 * @param containLoop
	 * @param traceMap
	 * @param visited
	 */
	private void dfsFindTrace(IBPNode cur, IBPNode end, List<IBPNode> trace,
			Place sinkPlace, boolean containLoop,
			Map<List<IBPNode>, Boolean> traceMap, Set<IBPNode> visited) {
		if (cur == end && !trace.isEmpty()) {
			List<IBPNode> tmp = new ArrayList<IBPNode>(trace);
			tmp.add(cur);
			traceMap.put(tmp, containLoop);
			return;
		} else if (visited.contains(cur)) {
			return;
		} else if (cur instanceof Condition
				&& ((Condition) cur).getPlace() == sinkPlace) {
			return;
		} else if (this._loopJoinConditions.contains(cur)) {
			cur = ((Condition) cur).getCorrespondingCondition();
			dfsFindTrace(cur, end, trace, sinkPlace, true, traceMap, visited);
		} else if (cur instanceof Condition && ((Condition) cur).isCutoffPost()) {
			cur = ((Condition) cur).getCorrespondingCondition();
			dfsFindTrace(cur, end, trace, sinkPlace, containLoop, traceMap,
					visited);
		} else {
			trace.add(cur);
			visited.add(cur);
			if (cur instanceof Condition) {
				Set<Event> curSuccSet = ((Condition) cur).getPostE();
				for (Event curSucc : curSuccSet) {
					dfsFindTrace(curSucc, end, trace, sinkPlace, containLoop,
							traceMap, visited);
				}
			} else if (cur instanceof Event) {
				Set<Condition> curSuccSet = ((Event) cur).getPostConditions();
				for (Condition curSucc : curSuccSet) {
					dfsFindTrace(curSucc, end, trace, sinkPlace, containLoop,
							traceMap, visited);
				}
			}
			visited.remove(cur);
			trace.remove(trace.size() - 1);
		}
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

	private void getTransitionNames() {
		this.tName = new ArrayList<String>();
		Set<Transition> alTransitions = this._sys.getTransitions();
		for (Transition t : alTransitions) {
			if (t.isObservable()) {
				this.tName.add(t.getLabel());
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

		System.out.println("Causal Matrix");
		for (int i = 0; i < n; ++i) {
			for (int j = 0; j < n; ++j) {
				System.out.print(this.causalMatrix[i][j].toString() + " ");
			}
			System.out.println();
		}

		System.out.println("Inverse Causal Matrix");
		for (int i = 0; i < n; ++i) {
			for (int j = 0; j < n; ++j) {
				System.out.print(this.inverseCausalMatrix[i][j].toString() + " ");
			}
			System.out.println();
		}

		System.out.println("Concurrent Matrix");
		for (int i = 0; i < n; ++i) {
			for (int j = 0; j < n; ++j) {
				System.out.print(this.concurrentMatrix[i][j].toString() + " ");
			}
			System.out.println();
		}
	}

	public RefinedOrderingRelation[][] getCausalMatrix() {
		return causalMatrix;
	}

	public RefinedOrderingRelation[][] getInverseCausalMatrix() {
		return inverseCausalMatrix;
	}

	public RefinedOrderingRelation[][] getConcurrentMatrix() {
		return concurrentMatrix;
	}

	public List<String> gettName() {
		return tName;
	}

}
