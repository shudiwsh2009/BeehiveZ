package cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph;

import java.util.ArrayList;
import java.util.List;

public class Vertex {
	
	private List<Label> labels = new ArrayList<Label>();
	private List<Edge> inEdges = new ArrayList<Edge>();
	private List<Edge> outEdges = new ArrayList<Edge>();
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("[");
		for(Label l : labels) {
			builder.append(l.getLabel());
			builder.append(",");
		}
		builder.deleteCharAt(builder.length() - 1);
		builder.append("]");
		return builder.toString();
	}
	
	public List<Vertex> getPredecessors() {
		List<Vertex> pred = new ArrayList<Vertex>();
		for(Edge e : inEdges) {
			pred.add(e.getSource());
		}
		return pred;
	}
	
	public List<Vertex> getSuccessors() {
		List<Vertex> succ = new ArrayList<Vertex>();
		for(Edge e : outEdges) {
			succ.add(e.getTarget());
		}
		return succ;
	}
	
	public List<String> getLabelsAsString() {
		List<String> labels = new ArrayList<String>();
		for(Label l : this.labels) {
			labels.add(l.getLabel());
		}
		return labels;
	}
	
	public List<Label> getLabels() {
		return labels;
	}

	public void setLabels(List<Label> labels) {
		this.labels = labels;
	}

	public List<Edge> getInEdges() {
		return inEdges;
	}

	public void setInEdges(List<Edge> inEdges) {
		this.inEdges = inEdges;
	}

	public List<Edge> getOutEdges() {
		return outEdges;
	}

	public void setOutEdges(List<Edge> outEdges) {
		this.outEdges = outEdges;
	}
	
}
