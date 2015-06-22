package cn.edu.thss.iise.beehivez.server.metric.rorm.dependency.importance;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.DecompositionSolver;
import org.apache.commons.math3.linear.LUDecomposition;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.linear.RealVector;
import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Place;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Condition;
import org.jbpt.petri.unfolding.Event;
import org.jbpt.petri.unfolding.IBPNode;

@SuppressWarnings("rawtypes")
public class RelationImportance {

	private NetSystem _sys;
	private CompletePrefixUnfolding _cpu;
	private Map<IBPNode, Map<IBPNode, RealVector>> edges = new HashMap<IBPNode, Map<IBPNode, RealVector>>();
	private Map<IBPNode, Map<IBPNode, Double>> importance = new HashMap<IBPNode, Map<IBPNode, Double>>();
	private Set<RealVector> equations = new HashSet<RealVector>();
	private int unknown = 0;

	public RelationImportance(CompletePrefixUnfolding cpu) {
		this._cpu = cpu;
		this._sys = (NetSystem) cpu.getOriginativeNetSystem();
		Place sourcePlace = this._sys.getSourcePlaces().iterator().next();
		Place sinkPlace = this._sys.getSinkPlaces().iterator().next();
		Condition sourceCondition = this._cpu.getConditions(sourcePlace)
				.iterator().next();
		Set<IBPNode> visited = new HashSet<IBPNode>();
		visited.add(sourceCondition);
		dfsEquation(sourceCondition, null, visited, sourcePlace, sinkPlace);
		solveEquations();
		System.out.println(importance);
	}

	private void dfsEquation(IBPNode cur, IBPNode pre, Set<IBPNode> visited,
			Place sourcePlace, Place sinkPlace) {
		if (cur instanceof Condition) {
			Condition curCondition = (Condition) cur;
			if(curCondition.isCutoffPost() && curCondition.getPostE().isEmpty()) {
				curCondition = curCondition.getCorrespondingCondition();
			}
			RealVector inEdges = new Polynomial();
			// add up all the in-edges
			// introduce unknown if not exist
			if (curCondition.getPlace() == sourcePlace) {
				inEdges = inEdges.append(1.0);
			} else {
				Set<Condition> conditions = new HashSet<Condition>();
				if (curCondition.getMappingConditions() != null) {
					curCondition.getMappingConditions().stream()
							.filter(c -> c.getPostE().isEmpty())
							.forEach(conditions::add);
				}
				conditions.add(curCondition);
				for (Condition c : conditions) {
					RealVector inEdge = new Polynomial();
					if (edges.containsKey(c.getPreEvent())
							&& edges.get(c.getPreEvent()).containsKey(
									curCondition)) {
						inEdge = edges.get(c.getPreEvent()).get(curCondition);
					} else {
						inEdge = new Polynomial();
						++unknown;
						while (inEdge.getDimension() < unknown) {
							inEdge = inEdge.append(0);
						}
						inEdge = inEdge.append(1);
					}
					while (inEdge.getDimension() < inEdges.getDimension()) {
						inEdge = inEdge.append(0);
					}
					while (inEdges.getDimension() < inEdge.getDimension()) {
						inEdges = inEdges.append(0);
					}
					inEdges = inEdges.add(inEdge);
					edges.putIfAbsent(c.getPreEvent(),
							new HashMap<IBPNode, RealVector>());
					edges.get(c.getPreEvent())
							.putIfAbsent(curCondition, inEdge);
				}
			}
			//
			if (curCondition.getPlace() == sinkPlace && unknown > 0) {
//				inEdges.addToEntry(0, -1);
//				equations.add(inEdges);
			} else {
				for (Event outEvent : curCondition.getPostE()) {
					if (edges.containsKey(curCondition)
							&& edges.get(curCondition).containsKey(outEvent)) {
						RealVector outEdge = edges.get(curCondition).get(
								outEvent);
						RealVector perOutEdge = inEdges.mapDivide(curCondition
								.getPostE().size());
						while (outEdge.getDimension() < perOutEdge
								.getDimension()) {
							outEdge = outEdge.append(0);
						}
						while (perOutEdge.getDimension() < outEdge
								.getDimension()) {
							perOutEdge = perOutEdge.append(0);
						}
						outEdge = outEdge.subtract(perOutEdge);
						equations.add(outEdge);
					} else {
						RealVector perOutEdge = inEdges.mapDivide(curCondition
								.getPostE().size());
						edges.putIfAbsent(curCondition,
								new HashMap<IBPNode, RealVector>());
						edges.get(curCondition).putIfAbsent(outEvent,
								perOutEdge);
					}
				}
			}
			for (Event succ : curCondition.getPostE()) {
				if(!visited.contains(succ)) {
					visited.add(succ);
					dfsEquation(succ, curCondition, visited, sourcePlace, sinkPlace);
				}
			}
		} else if (cur instanceof Event) {
			RealVector curEdge = edges.get(pre).get(cur);
			Event curEvent = (Event) cur;
			for (Condition inCondition : curEvent.getPreConditions()) {
				if (inCondition != pre) {
					if (edges.containsKey(inCondition)
							&& edges.get(inCondition).containsKey(curEvent)) {
						RealVector otherEdge = edges.get(inCondition).get(
								curEvent);
						RealVector oneEdge = curEdge.copy();
						while (otherEdge.getDimension() < oneEdge
								.getDimension()) {
							otherEdge = otherEdge.append(0);
						}
						while (oneEdge.getDimension() < otherEdge
								.getDimension()) {
							oneEdge = oneEdge.append(0);
						}
						oneEdge = oneEdge.subtract(otherEdge);
						equations.add(oneEdge);
					} else {
						edges.putIfAbsent(inCondition,
								new HashMap<IBPNode, RealVector>());
						edges.get(inCondition).putIfAbsent(curEvent,
								curEdge.copy());
					}
				}
			}
			for (Condition outCondition : curEvent.getPostConditions()) {
				if (outCondition.isCutoffPost()
						&& outCondition.getPostE().isEmpty()) {
					outCondition = outCondition.getCorrespondingCondition();
				}
				if (edges.containsKey(curEvent)
						&& edges.get(curEvent).containsKey(outCondition)) {
					RealVector otherEdge = edges.get(curEvent)
							.get(outCondition);
					RealVector oneEdge = curEdge.copy();
					while (otherEdge.getDimension() < oneEdge.getDimension()) {
						otherEdge = otherEdge.append(0);
					}
					while (oneEdge.getDimension() < otherEdge.getDimension()) {
						oneEdge = oneEdge.append(0);
					}
					oneEdge = oneEdge.subtract(otherEdge);
					equations.add(oneEdge);
				} else {
					edges.putIfAbsent(curEvent,
							new HashMap<IBPNode, RealVector>());
					edges.get(curEvent).putIfAbsent(outCondition,
							curEdge.copy());
				}
			}
			for (Condition succ : curEvent.getPostConditions()) {
				if (succ.isCutoffPost() && succ.getPostE().isEmpty()) {
					succ = succ.getCorrespondingCondition();
				}
				if(!visited.contains(succ)) {
					visited.add(succ);
					dfsEquation(succ, curEvent, visited, sourcePlace, sinkPlace);
				}
			}
		}
	}

	private void solveEquations() {
		double[] solution = new double[0];
		if (unknown > 0) {
			Iterator<RealVector> it = equations.iterator();
			while (it.hasNext()) {
				RealVector vec = it.next();
				if (vec.getNorm() == 0) {
					it.remove();
				}
//				System.out.println(vec.hashCode());
			}
			double[][] coefficients = new double[equations.size()][unknown];
			double[] constants = new double[equations.size()];
//			System.out.println(equations);
//			System.out.println(edges);
			int i = 0;
			for (RealVector equation : equations) {
				constants[i] = -equation.getEntry(0);
				for (int j = 1; j < equation.getDimension(); ++j) {
					coefficients[i][j - 1] = equation.getEntry(j);
				}
				++i;
			}
			RealMatrix coefficientsmMatrix = new Array2DRowRealMatrix(
					coefficients, false);
			DecompositionSolver solver = new LUDecomposition(
					coefficientsmMatrix).getSolver();
			RealVector constantsVector = new Polynomial(constants);
			RealVector solutionVector = solver.solve(constantsVector);
			solution = solutionVector.toArray();
		}
		for (Map.Entry<IBPNode, Map<IBPNode, RealVector>> outerEntry : edges
				.entrySet()) {
			importance.putIfAbsent(outerEntry.getKey(),
					new HashMap<IBPNode, Double>());
			for (Map.Entry<IBPNode, RealVector> innerEntry : outerEntry
					.getValue().entrySet()) {
				double[] edge = innerEntry.getValue().toArray();
				double importance = edge[0];
				for (int i = 1; i < edge.length; ++i) {
					importance += solution[i - 1] * edge[i];
				}
				this.importance.get(outerEntry.getKey()).putIfAbsent(
						innerEntry.getKey(), importance);
			}
		}
	}

	public Map<IBPNode, Map<IBPNode, Double>> getImportance() {
		return importance;
	}
}
