package cn.edu.thss.iise.beehivez.server.metric.rorm;

public class RefinedOrderingRelation {

	public static double SDA_WEIGHT = 0.8;

	public Relation relation;
	public boolean adjacency;
	public double importance;

	public RefinedOrderingRelation(Relation r, boolean a, double i) {
		relation = r;
		adjacency = a;
		importance = i;
	}

	public double intersection(RefinedOrderingRelation r) {
		if (relation == Relation.NEVER && r.relation == Relation.NEVER) {
			return 0;
		} else if (relation != r.relation) {
			return 0;
		} else if (adjacency != r.adjacency) {
			return SDA_WEIGHT * Math.min(importance, r.importance);
		} else {
			return Math.min(importance, r.importance);
		}
	}

	public double union(RefinedOrderingRelation r) {
		if (relation == Relation.NEVER && r.relation == Relation.NEVER) {
			return 0;
		} else if (relation == Relation.NEVER && r.relation != Relation.NEVER) {
			return r.importance;
		} else if (relation != Relation.NEVER && r.relation == Relation.NEVER) {
			return importance;
		} else {
			return Math.max(importance, r.importance);
		}
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
