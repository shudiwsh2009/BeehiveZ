package cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph;

public class TwoVertices {

	public Vertex v1;
	public Vertex v2;

	public TwoVertices(Vertex v1, Vertex v2) {
		this.v1 = v1;
		this.v2 = v2;
	}

	public String toString() {
		return "(" + v1.toString() + "," + v2.toString() + ")";
	}

	public boolean equals(Object pair) {
		return pair instanceof TwoVertices ? v1 == ((TwoVertices) pair).v1
				&& v2 == ((TwoVertices) pair).v2 : false;
	}
	
	public int hashCode() {
		return v1.hashCode() + v2.hashCode();
	}

}
