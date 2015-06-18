package cn.edu.thss.iise.beehivez.server.metric.rorm.dependency;

import java.util.ArrayList;

public class Polynomial extends ArrayList<Double> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6674226442586219352L;

	public Polynomial() {
		super();
	}

	public void add(Polynomial polynomial) {
		for (int i = 0; i < this.size() && i < polynomial.size(); ++i) {
			this.set(i, this.get(i) + polynomial.get(i));
		}
		if (polynomial.size() > this.size()) {
			for (int i = this.size(); i < polynomial.size(); ++i) {
				this.add(polynomial.get(i));
			}
		}
	}

	public void sub(Polynomial polynomial) {
		for (int i = 0; i < this.size() && i < polynomial.size(); ++i) {
			this.set(i, this.get(i) - polynomial.get(i));
		}
		if (polynomial.size() > this.size()) {
			for (int i = this.size(); i < polynomial.size(); ++i) {
				this.add(-polynomial.get(i));
			}
		}
	}

	public void div(double d) {
		for (int i = 0; i < this.size(); ++i) {
			this.set(i, this.get(i) / d);
		}
	}

	public void mul(double d) {
		for (int i = 0; i < this.size(); ++i) {
			this.set(i, this.get(i) * d);
		}
	}

	public Integer[] getArray() {
		return (Integer[]) this.toArray();
	}

}
