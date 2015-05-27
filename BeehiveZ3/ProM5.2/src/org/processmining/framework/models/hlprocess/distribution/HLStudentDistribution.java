package org.processmining.framework.models.hlprocess.distribution;

import java.io.IOException;
import java.io.Writer;

/**
 * Represents a student distribution.
 * 
 * @see HLStudentDistributionGui
 * 
 * @author arozinat
 * @author rmans
 */
public class HLStudentDistribution extends HLDistribution {

	// distribution attributes
	private int myDegreesFreedom;

	/**
	 * Creates a chi square distribution based on a degrees of freedom value
	 * 
	 * @param degreesFreedom
	 *            double the degrees of freedom value. Not that this value
	 *            always has to be equal or greater to 1.
	 */
	public HLStudentDistribution(int degreesFreedom) {
		myDegreesFreedom = degreesFreedom;
	}

	/**
	 * Retrieves the degrees of freedom for this distribution
	 * 
	 * @return double the degrees of freedom
	 */
	public int getDegreesFreedom() {
		return myDegreesFreedom;
	}

	/**
	 * Sest the degrees of freedom for this distribution
	 * 
	 * @param value
	 *            the degrees of freedom
	 */
	public void setDegreesFreedom(int value) {
		myDegreesFreedom = value;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.framework.models.hlprocess.distribution.HLDistribution
	 * #getDistributionType()
	 */
	public DistributionEnum getDistributionType() {
		return HLDistribution.DistributionEnum.STUDENT_DISTRIBUTION;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return "Student";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.framework.models.hlprocess.distribution.HLDistribution
	 * #equals(java.lang.Object)
	 */
	public boolean equals(Object obj) {
		return (obj instanceof HLStudentDistribution)
				&& (this.getDegreesFreedom() == ((HLStudentDistribution) obj)
						.getDegreesFreedom());
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.framework.models.hlprocess.distribution.HLDistribution
	 * #hashCode()
	 */
	public int hashCode() {
		// simple recipe for generating hashCode given by
		// Effective Java (Addison-Wesley, 2001)
		int result = 17;
		result = 37 * result + this.getDegreesFreedom();
		return result;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.framework.models.hlprocess.distribution.HLDistribution
	 * #writeDistributionToDot(java.lang.String, java.lang.String,
	 * java.lang.String, java.io.Writer)
	 */
	public void writeDistributionToDot(String boxId, String nodeId,
			String addText, Writer bw) throws IOException {
		// write the box itself
		String label = "";
		label = label + addText + "\\n";
		label = label + "Student Distribution\\n";
		label = label + "degrees freedom=" + myDegreesFreedom + "\\n";
		bw.write(boxId + " [shape=\"ellipse\", label=\"" + label + "\"];\n");
		// write the connection (if needed)
		if (!nodeId.equals("")) {
			bw.write(nodeId + " -> " + boxId + " [dir=none, style=dotted];\n");
		}
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.framework.models.hlprocess.distribution.HLDistribution
	 * #timeMultiplicationValue(double)
	 */
	public void setTimeMultiplicationValue(double value) {
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * org.processmining.framework.models.hlprocess.distribution.HLDistribution
	 * #checkValuesOfTimeParameters(java.lang.String)
	 */
	public boolean checkValuesOfTimeParameters(String info) {
		return false;
	}

}
