package cn.edu.thss.iise.beehivez.server.metric.rorm.dependency;

import org.apache.commons.math3.linear.ArrayRealVector;
import org.apache.commons.math3.linear.RealVector;

public class RealVectorTest {

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		RealVector p1 = new Polynomial(new double[]{2, 0, 4});
		RealVector p2 = new Polynomial(new double[]{4, 0, 8});
		System.out.println(p1.equals(p2));
	}

}

class Polynomial extends ArrayRealVector {
	
	public Polynomial() {
		super();
	}
	
	public Polynomial(double[] ds) {
		super(ds);
	}

	@Override
	public boolean equals(Object object) {
		if(!(object instanceof Polynomial)) {
			return false;
		}
		RealVector p = (RealVector) object;
		RealVector t = (RealVector) this.copy();
		while(t.getDimension() < p.getDimension()) {
			t = t.append(0);
		}
		while(p.getDimension() < t.getDimension()) {
			p = p.append(0);
		}
		RealVector divResult = p.ebeDivide(t);
		double div = Double.NaN;
		for(double r : divResult.toArray()) {
			if(r == Double.NaN) {
				continue;
			} else if(div == Double.NaN) {
				div = r;
			} else if(div - r >= 1e-5) {
				return false;
			}
		}
		return true;
	}
}
