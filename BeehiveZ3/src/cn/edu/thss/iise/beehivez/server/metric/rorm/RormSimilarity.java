/**
 * Behavioral Petri Net Similarity Algorithm based on Shortest Synchronization Distance
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

	private float similarity(PetriNet pn1, PetriNet pn2, int internal) {
		RefinedOrderingRelationsMatrix ecfm1 = new RefinedOrderingRelationsMatrix(
				pn1);
		RefinedOrderingRelationsMatrix ecfm2 = new RefinedOrderingRelationsMatrix(
				pn2);
		List<String> tName1 = ecfm1.gettName();
		List<String> tName2 = ecfm2.gettName();
		List<String> interNames = new ArrayList<String>();
		for (String s : tName1) {
			if (tName2.contains(s)) {
				interNames.add(s);
			}
		}
		Set<String> unionNames = new HashSet<String>();
		unionNames.addAll(tName1);
		unionNames.addAll(tName2);
		int followInter = 0, precedeInter = 0, parallelInter = 0;
		for (int i = 0; i < interNames.size(); ++i) {
			String row = interNames.get(i);
			for (int j = 0; j < interNames.size(); ++j) {
				String col = interNames.get(j);
				if (ecfm1.getCausalMatrix()[tName1.indexOf(row)][tName1
						.indexOf(col)].equals(ecfm2.getCausalMatrix()[tName2
						.indexOf(row)][tName2.indexOf(col)])) {
					++followInter;
				}
				if (ecfm1.getInverseCausalMatrix()[tName1.indexOf(row)][tName1
						.indexOf(col)]
						.equals(ecfm2.getInverseCausalMatrix()[tName2
								.indexOf(row)][tName2.indexOf(col)])) {
					++precedeInter;
				}
				if (ecfm1.getConcurrentMatrix()[tName1.indexOf(row)][tName1
						.indexOf(col)]
						.equals(ecfm2.getConcurrentMatrix()[tName2.indexOf(row)][tName2
								.indexOf(col)])) {
					++parallelInter;
				}
			}
		}
		double followSim = followInter / Math.pow(unionNames.size(), 2);
		double precedeSim = precedeInter / Math.pow(unionNames.size(), 2);
		double parallelSim = parallelInter / Math.pow(unionNames.size(), 2);
		System.out.println(followSim + " " + precedeSim + " " + parallelSim);
		return (float) ((followSim + precedeSim + parallelSim) / 3);
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
		return "Extended Causal Footprint Matrix Similarity";
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
		return "Behavioral Petri Net Similarity Algorithm based on Extended Causal Footprint Matrix Similarity, which is computed on Complete Finite Prefix.";
	}

	public static void main(String[] args) throws Exception {
		// String filePath = "/Users/shudi/Desktop/multi_relation_1.pnml";
		// String filePath =
		// "/Users/shudi/Desktop/parallel_A_with_outer_loop.pnml";
		// String filePath = "/Users/shudi/Desktop/M15.pnml";
		PnmlImport pnmlImport = new PnmlImport();

		String filePath = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\M16.pnml";
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
