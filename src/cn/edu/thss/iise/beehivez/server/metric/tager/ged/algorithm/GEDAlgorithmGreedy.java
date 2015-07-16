package cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm;

import java.util.HashSet;
import java.util.Random;
import java.util.Set;
import java.util.Vector;

import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TwoVertices;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.Vertex;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.led.LabelEditDistance;

public class GEDAlgorithmGreedy extends GEDAbstr {

	private Set<TwoVertices> times(Set<Vertex> a, Set<Vertex> b) {
		Set<TwoVertices> result = new HashSet<TwoVertices>();
		for (Vertex ea : a) {
			for (Vertex eb : b) {
				if (LabelEditDistance.similarity(ea.getLabelsAsString(),
						eb.getLabelsAsString()) > simcutoff) {
					result.add(new TwoVertices(ea, eb));
				}
			}
		}
		return result;
	}

	Set<TwoVertices> bestMapping = null;

	@Override
	public double compute(TLabeledGraph tg1, TLabeledGraph tg2) {
		init(tg1, tg2);

		// INIT
		bestMapping = new HashSet<TwoVertices>();
		bestMapping.add(new TwoVertices(tg1.getSource(), tg2.getSource()));
		bestMapping.add(new TwoVertices(tg1.getSink(), tg2.getSink()));
		Set<Vertex> vertices1 = new HashSet<Vertex>();
		Set<Vertex> vertices2 = new HashSet<Vertex>();
		for (Vertex v : tg1.getVertices()) {
			if (v != tg1.getSource() && v != tg1.getSink()) {
				vertices1.add(v);
			}
		}
		for (Vertex v : tg2.getVertices()) {
			if (v != tg2.getSource() && v != tg2.getSink()) {
				vertices2.add(v);
			}
		}
		Set<TwoVertices> openCouples = times(vertices1, vertices2);
		double shortestEditDistance = this.editDistance(bestMapping);
		Random randomized = new Random();

		// STEP
		boolean doStep = true;
		while (doStep) {
			doStep = false;
			Vector<TwoVertices> bestCandidates = new Vector<TwoVertices>();
			double newShortestEditDistance = shortestEditDistance;

			for (TwoVertices couple : openCouples) {
				Set<TwoVertices> newMapping = new HashSet<TwoVertices>(
						bestMapping);
				newMapping.add(couple);
				double newEditDistance = this.editDistance(newMapping);

				if (newEditDistance < newShortestEditDistance) {
					bestCandidates = new Vector<TwoVertices>();
					bestCandidates.add(couple);
					newShortestEditDistance = newEditDistance;
				} else if (newEditDistance == newShortestEditDistance) {
					bestCandidates.add(couple);
				}
			}

			if (bestCandidates.size() > 0) {
				TwoVertices couple = bestCandidates.get(randomized
						.nextInt(bestCandidates.size()));

				Set<TwoVertices> newOpenCouples = new HashSet<TwoVertices>();
				for (TwoVertices p : openCouples) {
					if (p.v1 != couple.v1 && p.v2 != couple.v2) {
						newOpenCouples.add(p);
					}
				}
				openCouples = newOpenCouples;

				bestMapping.add(couple);
				shortestEditDistance = newShortestEditDistance;
				doStep = true;
			}
		}

		return shortestEditDistance;
	}

	@Override
	public Set<TwoVertices> bestMapping() {
		return bestMapping;
	}

}
