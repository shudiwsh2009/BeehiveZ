package cn.edu.thss.iise.beehivez.server.metric.rorm;

public class RefinedOrderingRelation {

	public Relation relation;
	public boolean adjacency;

	public RefinedOrderingRelation(Relation r, boolean a) {
		relation = r;
		adjacency = a;
	}

	public boolean equals(Object o) {
		if (o instanceof RefinedOrderingRelation) {
			RefinedOrderingRelation r = (RefinedOrderingRelation) o;
			if (r.relation == relation && r.adjacency == adjacency) {
				return true;
			}
		}
		return false;
	}

	public String toString() {
		String s = "";
		switch (relation) {
		case ALWAYS:
			s = "A";
			break;
		case SOMETIMES:
			s = "S";
			break;
		case NEVER:
			s = "N";
			break;
		default:
			s = "U";
			break;
		}
		return s;

	}

}
