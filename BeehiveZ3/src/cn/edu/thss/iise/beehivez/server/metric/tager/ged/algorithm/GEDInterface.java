package cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm;

import java.util.Set;

import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TwoVertices;

public interface GEDInterface {
	
	public double compute(TLabeledGraph tg1, TLabeledGraph tg2);

	public Set<TwoVertices> bestMapping();
	
}
