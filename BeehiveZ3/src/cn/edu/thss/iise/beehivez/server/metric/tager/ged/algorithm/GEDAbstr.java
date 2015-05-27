package cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.Edge;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TwoVertices;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.Vertex;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.led.LabelEditDistance;

public abstract class GEDAbstr implements GEDInterface {

	protected TLabeledGraph tg1;
	protected TLabeledGraph tg2;

	double weightSkippedVertex;
	double weightSkippedEdge;
	double weightSubstitutedVertex;
	double simcutoff;
	long waiting = Long.MAX_VALUE;

	class Mapping implements Comparable<Mapping> {

		protected double gCost;
		protected double hCost;
		protected double vertexMappingCost;
		protected double vertexMappingCount;

		protected Map<Vertex, Vertex> mappingVertexFrom1;
		protected Map<Vertex, Vertex> mappingVertexFrom2;
		protected List<Vertex> remainingVertex1;
		protected List<Vertex> remainingVertex2;
		protected Set<Vertex> addedVertices;
		protected Set<Vertex> deletedVertices;

		protected Map<Edge, Edge> mappingEdgeFrom1;
		protected Map<Edge, Edge> mappingEdgeFrom2;
		protected List<Edge> remainingEdge1;
		protected List<Edge> remainingEdge2;
		protected Set<Edge> addedEdges;
		protected Set<Edge> deletedEdges;

		public Mapping() {
			gCost = 0.0;
			hCost = 0.0;
			vertexMappingCost = 0.0;
			vertexMappingCount = 0.0;

			mappingVertexFrom1 = new HashMap<Vertex, Vertex>();
			mappingVertexFrom2 = new HashMap<Vertex, Vertex>();
			remainingVertex1 = new LinkedList<Vertex>(tg1.getVertices());
			remainingVertex2 = new LinkedList<Vertex>(tg2.getVertices());
			addedVertices = new HashSet<Vertex>();
			deletedVertices = new HashSet<Vertex>();

			mappingEdgeFrom1 = new HashMap<Edge, Edge>();
			mappingEdgeFrom2 = new HashMap<Edge, Edge>();
			remainingEdge1 = new LinkedList<Edge>(tg1.getEdges());
			remainingEdge2 = new LinkedList<Edge>(tg2.getEdges());
			addedEdges = new HashSet<Edge>();
			deletedEdges = new HashSet<Edge>();
		}

		public Mapping clone() {
			Mapping m = new Mapping();
			m.remainingVertex1.clear();
			m.remainingVertex2.clear();
			m.remainingEdge1.clear();
			m.remainingEdge2.clear();
			m.gCost = gCost;
			m.hCost = hCost;
			m.vertexMappingCost = vertexMappingCost;
			m.vertexMappingCount = vertexMappingCount;

			m.mappingVertexFrom1.putAll(mappingVertexFrom1);
			m.mappingVertexFrom2.putAll(mappingVertexFrom2);
			m.remainingVertex1.addAll(remainingVertex1);
			m.remainingVertex2.addAll(remainingVertex2);
			m.addedVertices.addAll(addedVertices);
			m.deletedVertices.addAll(deletedVertices);

			m.mappingEdgeFrom1.putAll(mappingEdgeFrom1);
			m.mappingEdgeFrom2.putAll(mappingEdgeFrom2);
			m.remainingEdge1.addAll(remainingEdge1);
			m.remainingEdge2.addAll(remainingEdge2);
			m.addedEdges.addAll(addedEdges);
			m.deletedEdges.addAll(deletedEdges);
			return m;
		}

		@Override
		public int compareTo(Mapping o) {
			// TODO Auto-generated method stub
			if (gCost + hCost < o.gCost + o.hCost) {
				return -1;
			} else if (gCost + hCost > o.gCost + o.hCost) {
				return 1;
			} else {
				return 0;
			}
		}

		public double getCost() {
			return gCost + hCost;
		}

		public void updateCost(GEDAlgorithmAStar measure) {
			double[] cost = measure.editDistance(this);
			this.gCost = cost[0];
			this.hCost = cost[1];
		}

		public void step(Vertex v1, Vertex v2) {
			step(v1, v2, 0.0);
		}

		public void step(Vertex v1, Vertex v2, double subsCost) {
			if (v1 == null) {
				remainingVertex2.remove(v2);
				addedVertices.add(v2);
				mappingVertexFrom2.put(v2, null);
			} else if (v2 == null) {
				remainingVertex1.remove(v1);
				deletedVertices.add(v1);
				mappingVertexFrom1.put(v1, null);
			} else {
				remainingVertex1.remove(v1);
				remainingVertex2.remove(v2);
				vertexMappingCost += subsCost;
				vertexMappingCount += 1.0;
				mappingVertexFrom1.put(v1, v2);
				mappingVertexFrom2.put(v2, v1);
			}

			if (v1 != null) {
				for (Edge e1 : v1.getInEdges()) {
					if (mappingVertexFrom1.containsKey(e1.getSource())) {
						if (v2 != null) {
							boolean matchedEdge = false;
							for (Edge e2 : v2.getInEdges()) {
								if (e2.getSource() == mappingVertexFrom1.get(e1
										.getSource())) {
									mappingEdgeFrom1.put(e1, e2);
									mappingEdgeFrom2.put(e2, e1);
									remainingEdge1.remove(e1);
									remainingEdge2.remove(e2);
									matchedEdge = true;
									break;
								}
							}
							if (!matchedEdge) {
								deletedEdges.add(e1);
								remainingEdge1.remove(e1);
							}
						} else {
							deletedEdges.add(e1);
							remainingEdge1.remove(e1);
						}
					}
				}
				for (Edge e1 : v1.getOutEdges()) {
					if (mappingVertexFrom1.containsKey(e1.getTarget())) {
						if (v2 != null) {
							boolean matchedEdge = false;
							for (Edge e2 : v2.getOutEdges()) {
								if (e2.getTarget() == mappingVertexFrom1.get(e1
										.getTarget())) {
									mappingEdgeFrom1.put(e1, e2);
									mappingEdgeFrom2.put(e2, e1);
									remainingEdge1.remove(e1);
									remainingEdge2.remove(e2);
									matchedEdge = true;
									break;
								}
							}
							if (!matchedEdge) {
								deletedEdges.add(e1);
								remainingEdge1.remove(e1);
							}
						} else {
							deletedEdges.add(e1);
							remainingEdge1.remove(e1);
						}
					}
				}
			}

			if (v2 != null) {
				for (Edge e2 : v2.getInEdges()) {
					if (mappingVertexFrom2.containsKey(e2.getSource())) {
						if (v1 != null) {
							boolean matchedEdge = false;
							for (Edge e1 : v1.getInEdges()) {
								if (e1.getSource() == mappingVertexFrom2.get(e2
										.getSource())) {
									// already handled
									matchedEdge = true;
									break;
								}
							}
							if (!matchedEdge) {
								addedEdges.add(e2);
								remainingEdge2.remove(e2);
							}
						} else {
							addedEdges.add(e2);
							remainingEdge2.remove(e2);
						}
					}
				}
				for (Edge e2 : v2.getOutEdges()) {
					if (mappingVertexFrom2.containsKey(e2.getTarget())) {
						if (v1 != null) {
							boolean matchedEdge = false;
							for (Edge e1 : v1.getOutEdges()) {
								if (e1.getTarget() == mappingVertexFrom2.get(e2
										.getTarget())) {
									// already handled
									matchedEdge = true;
									break;
								}
							}
							if (!matchedEdge) {
								addedEdges.add(e2);
								remainingEdge2.remove(e2);
							}
						} else {
							addedEdges.add(e2);
							remainingEdge2.remove(e2);
						}
					}
				}
			}
		}
	}

	public void setWeight(Object[] weights) {
		weightSkippedVertex = 0.0;
		weightSkippedEdge = 0.0;
		weightSubstitutedVertex = 0.0;
		simcutoff = 0.0;
		waiting = 0;

		for (int i = 0; i < weights.length; i += 2) {
			String wname = (String) weights[i];
			Double wvalue = (Double) weights[i + 1];
			switch (wname) {
			case "vweight":
				weightSkippedVertex = wvalue;
				break;
			case "eweight":
				weightSkippedEdge = wvalue;
				break;
			case "sweight":
				weightSubstitutedVertex = wvalue;
				break;
			case "simcutoff":
				simcutoff = wvalue;
				break;
			case "waiting":
				waiting = wvalue.longValue();
				break;
			default:
				System.err
						.println("ERROR: Invalid weight identifier: " + wname);
			}
		}
	}

	protected void init(TLabeledGraph tg1, TLabeledGraph tg2) {
		this.tg1 = tg1;
		this.tg2 = tg2;
	}

	protected double computeScore(double skippedEdges, double skippedVertices,
			double substitutedVertices) {
		return weightSkippedEdge * skippedEdges + weightSkippedVertex
				* skippedVertices + 2 * weightSubstitutedVertex
				* substitutedVertices;
	}

	protected double[] editDistance(Mapping m) {
		double skippedEdges = 0.0;
		for (Edge e : m.addedEdges) {
			skippedEdges += (Math.max(1,
					Math.max(e.getConflictSpan(), e.getLoopSpan())));
		}
		for (Edge e : m.deletedEdges) {
			skippedEdges += (Math.max(1,
					Math.max(e.getConflictSpan(), e.getLoopSpan())));
		}
		for (Map.Entry<Edge, Edge> entry : m.mappingEdgeFrom1.entrySet()) {
			double e1 = Math.max(1, Math.max(entry.getKey().getConflictSpan(),
					entry.getKey().getLoopSpan()));
			double e2 = Math.max(1, Math.max(
					entry.getValue().getConflictSpan(), entry.getValue()
							.getLoopSpan()));
			skippedEdges += (Math.abs(e1 - e2));
		}
		double skippedVertices = (double) m.addedVertices.size()
				+ (double) m.deletedVertices.size();
		double substituteVertices = m.vertexMappingCost;
		double gCost = computeScore(skippedEdges, skippedVertices,
				substituteVertices);

		double remainingSkippedEdges = 0.0;
		for (Edge e1 : m.remainingEdge1) {
			remainingSkippedEdges += (Math.max(1,
					Math.max(e1.getConflictSpan(), e1.getLoopSpan())));
		}
		for (Edge e2 : m.remainingEdge2) {
			remainingSkippedEdges -= (Math.max(1,
					Math.max(e2.getConflictSpan(), e2.getLoopSpan())));
		}
		remainingSkippedEdges = Math.abs(remainingSkippedEdges);
		double remainingSkippedVertices = ((double) Math.abs(m.remainingVertex1
				.size() - m.remainingVertex2.size()));;
		double remainingSubstituteVertices = 0.0;
		double[] remainingSubstituteCost = new double[m.remainingVertex1.size()];
		int idx = 0;
		for (Vertex v1 : m.remainingVertex1) {
			double minCost = 1.0;
			for (Vertex v2 : m.remainingVertex2) {
				if (LabelEditDistance.similarity(v1.getLabelsAsString(),
						v2.getLabelsAsString()) > simcutoff) {
					double cost = LabelEditDistance.cost(
							v1.getLabelsAsString(), v2.getLabelsAsString());
					minCost = Math.min(minCost, cost);
				}
			}
			remainingSubstituteCost[idx++] = (minCost == 1.0) ? 1.0 : minCost;
		}
		Arrays.sort(remainingSubstituteCost);
		for (int i = 0; i < Math.min(m.remainingVertex1.size(),
				m.remainingVertex2.size()); ++i) {
			if(remainingSubstituteCost[i] == 1.0) {
				remainingSkippedVertices += 2;
			} else {
				remainingSubstituteVertices += remainingSubstituteCost[i];
			}
		}
		
		double hCost = computeScore(remainingSkippedEdges,
				remainingSkippedVertices, remainingSubstituteVertices);

		return new double[] { gCost, hCost };
	}

	protected double editDistance(Set<TwoVertices> m) {
		Set<Vertex> verticesFrom1Used = new HashSet<Vertex>();
		Set<Vertex> verticesFrom2Used = new HashSet<Vertex>();

		Map<Vertex, Vertex> mappingVertexFrom1 = new HashMap<Vertex, Vertex>();
		Map<Vertex, Vertex> mappingVertexFrom2 = new HashMap<Vertex, Vertex>();

		double substitutedVertices = 0.0;
		for (TwoVertices pair : m) {
			substitutedVertices += LabelEditDistance.cost(
					pair.v1.getLabelsAsString(), pair.v2.getLabelsAsString());
			verticesFrom1Used.add(pair.v1);
			verticesFrom2Used.add(pair.v2);
			mappingVertexFrom1.put(pair.v1, pair.v2);
			mappingVertexFrom2.put(pair.v2, pair.v1);
		}

		Set<Edge> edgesIn1 = new HashSet<Edge>(tg1.getEdges());
		Set<Edge> edgesIn2 = new HashSet<Edge>(tg2.getEdges());
		Map<Edge, Edge> mappingEdgeFrom1 = new HashMap<Edge, Edge>();
		Map<Edge, Edge> mappingEdgeFrom2 = new HashMap<Edge, Edge>();
		for (Edge e1 : edgesIn1) {
			if (mappingVertexFrom1.containsKey(e1.getSource())
					&& mappingVertexFrom1.containsKey(e1.getTarget())) {
				for (Edge e2 : edgesIn2) {
					if (e2.getSource() == mappingVertexFrom1
							.get(e1.getSource())
							&& e2.getTarget() == mappingVertexFrom1.get(e1
									.getTarget())) {
						mappingEdgeFrom1.put(e1, e2);
						mappingEdgeFrom2.put(e2, e1);
					}
				}
			}
		}
		edgesIn1.removeAll(mappingEdgeFrom1.keySet());
		edgesIn2.removeAll(mappingEdgeFrom2.keySet());
		double skippedEdges = 0.0;
		for (Map.Entry<Edge, Edge> entry : mappingEdgeFrom1.entrySet()) {
			double e1 = Math.max(1, Math.max(entry.getKey().getConflictSpan(),
					entry.getKey().getLoopSpan()));
			double e2 = Math.max(1, Math.max(
					entry.getValue().getConflictSpan(), entry.getValue()
							.getLoopSpan()));
			skippedEdges += (Math.abs(e1 - e2));
		}
		for (Edge e1 : edgesIn1) {
			skippedEdges += Math.max(1,
					Math.max(e1.getConflictSpan(), e1.getLoopSpan()));
		}
		for (Edge e2 : edgesIn2) {
			skippedEdges += Math.max(1,
					Math.max(e2.getConflictSpan(), e2.getLoopSpan()));
		}

		Set<Vertex> skippedVerticesIn1 = new HashSet<Vertex>(tg1.getVertices());
		skippedVerticesIn1.removeAll(verticesFrom1Used);
		Set<Vertex> skippedVerticesIn2 = new HashSet<Vertex>(tg2.getVertices());
		skippedVerticesIn2.removeAll(verticesFrom2Used);
		double skippedVertices = (double) (skippedVerticesIn1.size() + skippedVerticesIn2
				.size());

		return computeScore(skippedEdges, skippedVertices, substitutedVertices);
	}
}
