package cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph;

import org.processmining.framework.models.ModelGraphEdge;

public class Label {
	private String label;
	private ModelGraphEdge restore;
	
	public Label(String l) {
		label = l;
	}
	
	public Label(String l, ModelGraphEdge e) {
		label = l;
		restore = e;
	}
	
	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public ModelGraphEdge getRestore() {
		return restore;
	}
	public void setRestore(ModelGraphEdge restore) {
		this.restore = restore;
	}
	
}
