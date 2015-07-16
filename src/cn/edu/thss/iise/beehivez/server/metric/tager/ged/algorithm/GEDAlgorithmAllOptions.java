package cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm;

import java.util.HashSet;
import java.util.Set;

import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TwoVertices;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.Vertex;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.led.LabelEditDistance;

public class GEDAlgorithmAllOptions extends GEDAbstr {

	class UnfinishedMapping {
		public Set<Vertex> freeVertices1;
		public Set<Vertex> freeVertices2;
		public Set<TwoVertices> mapping;

		public UnfinishedMapping() {
			this.freeVertices1 = new HashSet<Vertex>();
			this.freeVertices2 = new HashSet<Vertex>();
			this.mapping = new HashSet<TwoVertices>();
		}

		public UnfinishedMapping(Set<Vertex> freeVertices1,
				Set<Vertex> freeVertices2, Set<TwoVertices> mapping) {
			this.freeVertices1 = new HashSet<Vertex>(freeVertices1);
			this.freeVertices2 = new HashSet<Vertex>(freeVertices2);
			this.mapping = new HashSet<TwoVertices>(mapping);
		}

		public UnfinishedMapping(Set<Vertex> freeVertices1,
				Set<Vertex> freeVertices2) {
			this.freeVertices1 = new HashSet<Vertex>(freeVertices1);
			this.freeVertices2 = new HashSet<Vertex>(freeVertices2);
			this.mapping = new HashSet<TwoVertices>();
		}

		public boolean equals(Object o) {
			if (o instanceof UnfinishedMapping) {
				UnfinishedMapping op = (UnfinishedMapping) o;
				return op.freeVertices1.equals(this.freeVertices1)
						&& op.freeVertices2.equals(this.freeVertices2)
						&& op.mapping.equals(this.mapping);
			} else {
				return false;
			}
		}

		public int hashCode() {
			return this.freeVertices1.hashCode()
					+ this.freeVertices2.hashCode() + this.mapping.hashCode();
		}
	}

	Set<Set<TwoVertices>> finalMappings;
	Set<TwoVertices> mappingWithMinimalDistance = null;

	@Override
	public double compute(TLabeledGraph tg1, TLabeledGraph tg2) {
		init(tg1, tg2);
		finalMappings = new HashSet<Set<TwoVertices>>();
		computeAllMappings();

		mappingWithMinimalDistance = null;
		double minimalDistance = Double.MAX_VALUE;
		for (Set<TwoVertices> mapping : finalMappings) {
			double editDistance = editDistance(mapping);
			if (editDistance < minimalDistance) {
				minimalDistance = editDistance;
				mappingWithMinimalDistance = mapping;
			}
		}

		return minimalDistance;
	}

	private void computeAllMappings() {
		Set<UnfinishedMapping> unfinishedMappings = new HashSet<UnfinishedMapping>();
		finalMappings = new HashSet<Set<TwoVertices>>();

		unfinishedMappings.add(new UnfinishedMapping(new HashSet<Vertex>(tg1
				.getVertices()), new HashSet<Vertex>(tg2.getVertices())));
		do {
			unfinishedMappings = step(unfinishedMappings);
		} while (!unfinishedMappings.isEmpty());
	}

	private Set<UnfinishedMapping> step(Set<UnfinishedMapping> ufs) {
		Set<UnfinishedMapping> newUfs = new HashSet<UnfinishedMapping>();
		for (UnfinishedMapping uf : ufs) {
			if (uf.freeVertices1.isEmpty() || uf.freeVertices2.isEmpty()) {
				finalMappings.add(uf.mapping);
			} else {
				for (Vertex v1 : uf.freeVertices1) {
					for (Vertex v2 : uf.freeVertices2) {
						if (LabelEditDistance.similarity(
								v1.getLabelsAsString(), v2.getLabelsAsString()) > simcutoff) {
							UnfinishedMapping newMapping = new UnfinishedMapping(
									uf.freeVertices1, uf.freeVertices2,
									uf.mapping);
							newMapping.freeVertices1.remove(v1);
							newMapping.freeVertices2.remove(v2);
							newMapping.mapping.add(new TwoVertices(v1, v2));
							
							newUfs.add(newMapping);
						}
					}
					UnfinishedMapping newMapping = new UnfinishedMapping(
							uf.freeVertices1, uf.freeVertices2,
							uf.mapping);
					newMapping.freeVertices1.remove(v1);
					newUfs.add(newMapping);
				}
			}
		}
		
		return newUfs;
	}

	@Override
	public Set<TwoVertices> bestMapping() {
		return mappingWithMinimalDistance;
	}

}
