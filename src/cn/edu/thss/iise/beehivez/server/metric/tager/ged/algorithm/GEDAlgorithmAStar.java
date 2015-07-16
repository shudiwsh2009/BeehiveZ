package cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm;

import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;

import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TwoVertices;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.Vertex;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.led.LabelEditDistance;

public class GEDAlgorithmAStar extends GEDAbstr {

	@Override
	public double compute(TLabeledGraph tg1, TLabeledGraph tg2) {
		// TODO Auto-generated method stub
		double accept_threshold = Double.POSITIVE_INFINITY;
		PriorityQueue<Mapping> open = new PriorityQueue<Mapping>();
		PriorityQueue<Mapping> fullMappings = new PriorityQueue<Mapping>();
		Mapping m;

		init(tg1, tg2);
		Mapping initMapping = new Mapping();
		initMapping.step(tg1.getSource(), tg2.getSource());
		initMapping.step(tg1.getSink(), tg2.getSink());
		initMapping.updateCost(this);
		open.add(initMapping);

		long step = 0L;
		long startTime = System.currentTimeMillis();
		while (!open.isEmpty()) {
			// Avoid overtiming
			if ((++step) % 100 == 0
					&& System.currentTimeMillis() - startTime > this.waiting) {
				return -1;
			}
			
			Mapping p = open.remove();

			if (p.getCost() > accept_threshold) {
				break;
			}
			if (p.remainingVertex1.size() == 0
					&& p.remainingVertex2.size() == 0) {
				fullMappings.add(p);
				accept_threshold = p.getCost();
				continue;
			}
			if (p.remainingVertex1.size() > 0 && p.remainingVertex2.size() > 0) {
				Vertex vk = p.remainingVertex1.get(0);
				m = p.clone();
				m.step(vk, null);
				m.updateCost(this);
				open.add(m);
				for (Vertex w : p.remainingVertex2) {
					if (LabelEditDistance.similarity(vk.getLabelsAsString(),
							w.getLabelsAsString()) > simcutoff) {
						m = p.clone();
						m.step(vk, w, LabelEditDistance.cost(
								vk.getLabelsAsString(), w.getLabelsAsString()));
						m.updateCost(this);
						open.add(m);
					}
				}
			} else if (p.remainingVertex1.size() > 0) {
				Vertex vk = p.remainingVertex1.get(0);
				p.step(vk, null);
				p.updateCost(this);
				open.add(p);
			} else {
				Vertex vk = p.remainingVertex2.get(0);
				p.step(null, vk);
				p.updateCost(this);
				open.add(p);
			}
		}

		Mapping mapping = fullMappings.remove();

		bestMapping = new HashSet<TwoVertices>();
		for (Map.Entry<Vertex, Vertex> entry : mapping.mappingVertexFrom1
				.entrySet()) {
			if (entry.getValue() != null) {
				bestMapping.add(new TwoVertices(entry.getKey(), entry
						.getValue()));
			}
		}

		return mapping.getCost();
	}

	Set<TwoVertices> bestMapping = null;

	@Override
	public Set<TwoVertices> bestMapping() {
		// TODO Auto-generated method stub
		return bestMapping;
	}

}
