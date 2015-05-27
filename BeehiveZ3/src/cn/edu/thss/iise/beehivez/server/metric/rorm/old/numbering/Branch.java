package cn.edu.thss.iise.beehivez.server.metric.rorm.old.numbering;

import java.util.HashSet;
import java.util.Set;

import org.processmining.framework.models.ModelGraphVertex;

public class Branch {
	private ModelGraphVertex vertex;
	private boolean waiting;
	private boolean merged;
	private Set<ModelGraphVertex> mergeVertexs;
	private int number;
	
	public Branch(ModelGraphVertex v, boolean w, boolean m, int n) {
		vertex = v;
		waiting = w;
		merged = m;
		mergeVertexs = new HashSet<ModelGraphVertex>();
		number = n;
	}
	
	@Override
	public String toString() {
		return vertex.getIdentifier() + ", " + number;
	}

	public boolean isWaiting() {
		return waiting;
	}

	public void setWaiting(boolean waiting) {
		this.waiting = waiting;
	}
	
	public boolean isMerged() {
		return merged;
	}
	
	public void setMerged(boolean merged) {
		this.merged = merged;
	}

	public ModelGraphVertex getVertex() {
		return vertex;
	}
	
	public void setVertex(ModelGraphVertex vertex) {
		this.vertex = vertex;
	}
	
	public Set<ModelGraphVertex> getMergeVertexs() {
		return mergeVertexs;
	}
	
	public int getNumber() {
		return number;
	}
	
	public void setNumber(int number) {
		this.number = number;
	}
}
