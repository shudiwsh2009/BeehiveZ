package cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph;

import java.util.ArrayList;
import java.util.List;

public class Edge {
	
	private List<String> labels = new ArrayList<String>();
	private Vertex source = null;
	private Vertex target = null;
	private int loopSpan = 0;
	private int conflictSpan = 0;
	
	public Edge(Vertex s, Vertex t) {
		source = s;
		target = t;
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(source.toString());
		builder.append(" -> ");
		builder.append(target.toString());
		return builder.toString();
	}

	public List<String> getLabels() {
		return labels;
	}

	public void setLabels(List<String> labels) {
		this.labels = labels;
	}

	public Vertex getSource() {
		return source;
	}

	public void setSource(Vertex source) {
		this.source = source;
	}

	public Vertex getTarget() {
		return target;
	}

	public void setTarget(Vertex target) {
		this.target = target;
	}

	public int getLoopSpan() {
		return loopSpan;
	}

	public void setLoopSpan(int loopSpan) {
		this.loopSpan = loopSpan;
	}

	public int getConflictSpan() {
		return conflictSpan;
	}

	public void setConflictSpan(int conflictSpan) {
		this.conflictSpan = conflictSpan;
	}
	
}
