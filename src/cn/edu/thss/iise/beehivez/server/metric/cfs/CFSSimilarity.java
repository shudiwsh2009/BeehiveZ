package cn.edu.thss.iise.beehivez.server.metric.cfs;

import java.io.File;
import java.io.FileInputStream;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.basicprocess.CTree;
import cn.edu.thss.iise.beehivez.server.basicprocess.CTreeGenerator;
import cn.edu.thss.iise.beehivez.server.basicprocess.mymodel.MyPetriNet;
import cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.cfs.dep.NewPtsSet;
import cn.edu.thss.iise.beehivez.server.metric.cfs.dep.TTreeGenerator;

public class CFSSimilarity extends PetriNetSimilarity {

	@Override
	public float similarity(PetriNet pn1, PetriNet pn2) {
		// TODO Auto-generated method stub
		return similarity((PetriNet) pn1.clone(), (PetriNet) pn2.clone(), 1);
	}
	
	private float similarity(PetriNet pn1, PetriNet pn2, int internal) {
		// TODO Auto-generated method stub
		NewPtsSet nps1 = computeSequenceSet(pn1);
		NewPtsSet nps2 = computeSequenceSet(pn2);
		double[][] seqM = computeSeqMatrix(nps1, nps2);
		double sim = computeSimilarityForTwoNet_Astar(seqM, nps1, nps2);
		return (float) sim;
	}

	/**
	 * 给定两个模型序列集合，返回相似度的值，A*算法
	 * 
	 * @param set1
	 * @param set2
	 * @return
	 */
	public double computeSimilarityForTwoNet_Astar(double[][] seqM,
			NewPtsSet seqSet1, NewPtsSet seqSet2) {
		if (seqSet1.getNPSet().size() <= seqSet2.getNPSet().size()) {
			return seqSet1.setSimilarity_Astar(seqM, seqSet2);
		} else {
			return seqSet2.setSimilarity_Astar(seqM, seqSet1);
		}

	}

	/**
	 * 计算两触发序列集合的矩阵，矩阵的值为触发序列的相似度
	 * 
	 * @param seqSet1
	 * @param seqSet2
	 * @return
	 */
	private double[][] computeSeqMatrix(NewPtsSet seqSet1, NewPtsSet seqSet2) {
		if (seqSet1.getNPSet().size() <= seqSet2.getNPSet().size()) {
			double[][] seqM = new double[seqSet1.getNPSet().size()][seqSet2
					.getNPSet().size()];
			for (int i = 0; i < seqSet1.getNPSet().size(); i++) {
				for (int j = 0; j < seqSet2.getNPSet().size(); j++) {
					seqM[i][j] = seqSet1.getNPSet().get(i)
							.SequenceSimilarity(seqSet2.getNPSet().get(j));
				}
				// System.out.println(i);
			}
			return seqM;
		} else {
			double[][] seqM = new double[seqSet2.getNPSet().size()][seqSet1
					.getNPSet().size()];
			for (int i = 0; i < seqSet2.getNPSet().size(); i++) {
				for (int j = 0; j < seqSet1.getNPSet().size(); j++) {
					seqM[i][j] = seqSet2.getNPSet().get(i)
							.SequenceSimilarity(seqSet1.getNPSet().get(j));
				}
			}
			return seqM;
		}
	}

	/**
	 * 给定模型，返回对应的触发序列集合
	 * 
	 * @param pn
	 * @return
	 */
	private NewPtsSet computeSequenceSet(PetriNet pn) {
		CTreeGenerator generator = new CTreeGenerator(
				MyPetriNet.PromPN2MyPN(pn));
		CTree cTree = generator.generateCTree();
		TTreeGenerator ttg = new TTreeGenerator();
		/**
		 * Input: a coverability tree cTree, loop times K Ouput: a trace tree
		 * tTree
		 */
		NewPtsSet nps = ttg.generatTTree(cTree, 2, pn.getName());// 循环次数
//		nps.showSet();
		return nps;// 循环次数
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return "Process Model Similarity Metric Based on Firing Sequences Collection";
	}

	@Override
	public String getDesription() {
		// TODO Auto-generated method stub
		return "Implemented by Dong Zihe and Huang Haowei based on \"Dong Z, Wen L, Huang H, et al. "
				+ "CFS: A Behavioral Similarity Algorithm for Process Models Based on "
				+ "Complete Firing Sequences[C]//On the Move to Meaningful Internet Systems: "
				+ "OTM 2014 Conferences. Springer Berlin Heidelberg, 2014: 202-219.\"";
	}
	
	public static void main(String[] args) throws Exception {
		String filepath1 = "C:\\Users\\Shudi\\Desktop\\Model\\test\\M3.pnml";
		String filepath2 = "C:\\Users\\Shudi\\Desktop\\Model\\test\\M16.pnml";
		PnmlImport pnmlImport = new PnmlImport();
		PetriNet pn1 = pnmlImport.read(new FileInputStream(new File(filepath1)));
		PetriNet pn2 = pnmlImport.read(new FileInputStream(new File(filepath2)));
		
		CFSSimilarity cfs = new CFSSimilarity();
		System.out.println(cfs.similarity(pn1, pn2));
	}

}
