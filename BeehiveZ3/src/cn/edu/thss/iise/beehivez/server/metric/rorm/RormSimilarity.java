/**
 * Behavioral Petri Net Similarity Algorithm based on the Matrix of Refined Ordering Relations with Uncertainty
 */
package cn.edu.thss.iise.beehivez.server.metric.rorm;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity;

/**
 * @author Wang Shuhao
 *
 */
public class RormSimilarity extends PetriNetSimilarity {

	@Override
	public float similarity(PetriNet pn1, PetriNet pn2) {
		// TODO Auto-generated method stub
		return similarity((PetriNet) pn1.clone(), (PetriNet) pn2.clone(), 1);
	}

	public float similarity(PetriNet pn1, PetriNet pn2, int internal) {
		RefinedOrderingRelationsMatrix ecfm1 = new RefinedOrderingRelationsMatrix(
				pn1);
		RefinedOrderingRelationsMatrix ecfm2 = new RefinedOrderingRelationsMatrix(
				pn2);
		return similarity(ecfm1, ecfm2);
	}

	public float similarity(RefinedOrderingRelationsMatrix matrix1,
			RefinedOrderingRelationsMatrix matrix2) {
		List<String> tName1 = matrix1.gettName();
		List<String> tName2 = matrix2.gettName();
		List<String> interNames = new ArrayList<String>();
		interNames.addAll(tName1);
		interNames.retainAll(tName2);
		Set<String> unionNames = new HashSet<String>();
		unionNames.addAll(tName1);
		unionNames.addAll(tName2);
		// intersection
		double causalInter = 0.0, inverseCausalInter = 0.0, concurrentInter = 0.0;
		for (int i = 0; i < interNames.size(); ++i) {
			int idx1i = tName1.indexOf(interNames.get(i));
			int idx2i = tName2.indexOf(interNames.get(i));
			for (int j = 0; j < interNames.size(); ++j) {
				int idx1j = tName1.indexOf(interNames.get(j));
				int idx2j = tName2.indexOf(interNames.get(j));
				causalInter += matrix1.getCausalMatrix()[idx1i][idx1j]
						.intersection(matrix2.getCausalMatrix()[idx2i][idx2j]);
				inverseCausalInter += matrix1.getInverseCausalMatrix()[idx1i][idx1j]
						.intersection(matrix2.getCausalMatrix()[idx2i][idx2j]);
				concurrentInter += matrix1.getConcurrentMatrix()[idx1i][idx1j]
						.intersection(matrix2.getCausalMatrix()[idx2i][idx2j]);
			}
		}
		// union
		double causalUnion = 0.0, inverseCausalUnion = 0.0, concurrentUnion = 0.0;
		for (int i = 0; i < tName1.size(); ++i) {
			int idx2i = tName2.indexOf(tName1.get(i));
			for (int j = 0; j < tName1.size(); ++j) {
				int idx2j = tName2.indexOf(tName1.get(j));
				if (idx2i != -1 && idx2j != -1) {
					causalUnion += matrix1.getCausalMatrix()[i][j]
							.union(matrix2.getCausalMatrix()[idx2i][idx2j]);
					inverseCausalUnion += matrix1.getInverseCausalMatrix()[i][j]
							.union(matrix2.getCausalMatrix()[idx2i][idx2j]);
					concurrentUnion += matrix1.getConcurrentMatrix()[i][j]
							.union(matrix2.getCausalMatrix()[idx2i][idx2j]);
				} else {
					causalUnion += matrix1.getCausalMatrix()[i][j].relation == Relation.NEVER ? 0
							: matrix1.getCausalMatrix()[i][j].importance;
					inverseCausalUnion += matrix1.getInverseCausalMatrix()[i][j].relation == Relation.NEVER ? 0
							: matrix1.getInverseCausalMatrix()[i][j].importance;
					concurrentUnion += matrix1.getConcurrentMatrix()[i][j].relation == Relation.NEVER ? 0
							: matrix1.getConcurrentMatrix()[i][j].importance;
				}
			}
		}
		for (int i = 0; i < tName2.size(); ++i) {
			int idx1i = tName1.indexOf(tName2.get(i));
			for (int j = 0; j < tName2.size(); ++j) {
				int idx1j = tName1.indexOf(tName2.get(j));
				if (idx1i != -1 && idx1j != -1) {
					continue;
				} else {
					causalUnion += matrix2.getCausalMatrix()[i][j].relation == Relation.NEVER ? 0
							: matrix2.getCausalMatrix()[i][j].importance;
					inverseCausalUnion += matrix2.getInverseCausalMatrix()[i][j].relation == Relation.NEVER ? 0
							: matrix2.getInverseCausalMatrix()[i][j].importance;
					concurrentUnion += matrix2.getConcurrentMatrix()[i][j].relation == Relation.NEVER ? 0
							: matrix2.getConcurrentMatrix()[i][j].importance;
				}
			}
		}
		// Jaccard
		double causalSim = causalInter / causalUnion;
		double inverseCausalSim = inverseCausalInter / inverseCausalUnion;
		double concurrentSim = concurrentInter / concurrentUnion;
		System.out.println(causalSim + " " + inverseCausalSim + " "
				+ concurrentSim);
		return (float) ((causalSim + inverseCausalSim + concurrentSim) / 3);
	}

	public float similarity(PetriNet pn1, PetriNet pn2, Map<String, String> corr) {
		// Using given map of transitions
		return 0.0f;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity#getName()
	 */
	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "RORM Simialrity";
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see
	 * cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity#getDesription
	 * ()
	 */
	@Override
	public String getDesription() {
		// TODO Auto-generated method stub
		return "Behavioral Petri Net Similarity Algorithm based on the Matrix of Refined Ordering Realtions with Uncertainty.";
	}

	public static void main(String[] args) throws Exception {
		// String filePath = "/Users/shudi/Desktop/multi_relation_1.pnml";
		// String filePath =
		// "/Users/shudi/Desktop/parallel_A_with_outer_loop.pnml";
		// String filePath = "/Users/shudi/Desktop/M15.pnml";
		PnmlImport pnmlImport = new PnmlImport();

		String filePath = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\M0.pnml";
		PetriNet pn = pnmlImport.read(new FileInputStream(new File(filePath)));
		RefinedOrderingRelationsMatrix rorm = new RefinedOrderingRelationsMatrix(
				(PetriNet) pn.clone());
		rorm.printMatrix();

		// String filepath1 =
		// "C:\\Users\\Shudi\\Desktop\\rorm\\test\\parallel_inv_1_a.pnml";
		// String filepath2 =
		// "C:\\Users\\Shudi\\Desktop\\rorm\\test\\parallel_inv_1_b.pnml";
		// PetriNet pn1 = pnmlImport
		// .read(new FileInputStream(new File(filepath1)));
		// PetriNet pn2 = pnmlImport
		// .read(new FileInputStream(new File(filepath2)));
		// PetriNetSimilarity sim = new RormSimilarity();
		// System.out.println(sim.similarity(pn1, pn2));
	}
}
