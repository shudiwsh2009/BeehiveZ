package cn.edu.thss.iise.beehivez.server.metric.tager.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarityAllOptions;
import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarityGreedy;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;

public class TagerTriangleEquationBatchComparison {

	// public static final String ROOT_FOLDER = "F:\\Demo\\";
	// public static String ROOT_FOLDER = "C:\\Users\\Shudi\\Desktop\\tager\\";
	public static final String ROOT_FOLDER = "E:\\wangshuhao\\Documents\\Tager\\";
	public static final int REPEAT_TIME = 20;
	private int multiple = 10000;

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		TagerTriangleEquationBatchComparison tb = new TagerTriangleEquationBatchComparison();
		tb.computeBatch();
	}

	/**
	 * compute the fitness of triangle equations 4_2 and 4_3
	 * 
	 * @param algo
	 * @throws Exception
	 */
	public void computeBatch() throws Exception {
		String rootFolder = ROOT_FOLDER;
		BufferedWriter csvWriter = new BufferedWriter(new FileWriter(rootFolder
				+ "三种编辑距离算法三角不等式性能对比_TAGER算法_3组原始企业模型20次平均值_150423a.csv"));
		csvWriter.write(",DG_time,DG4-2,TC_time,TC4-2,SAP_time,SAP4-2");
		csvWriter.newLine();

		File dgFolder = new File(rootFolder + "DG");
		File tcFolder = new File(rootFolder + "TC");
		File sapFolder = new File(rootFolder + "SAP");
		File[] dgFiles = dgFolder.listFiles();
		File[] tcFiles = tcFolder.listFiles();
		File[] sapFiles = sapFolder.listFiles();
		List<TLabeledGraph> dgTGraphs = new ArrayList<TLabeledGraph>();
		List<TLabeledGraph> tcTGraphs = new ArrayList<TLabeledGraph>();
		List<TLabeledGraph> sapTGraphs = new ArrayList<TLabeledGraph>();

		// load petri nets and convert to simpleCG
		loadFiles(dgFiles, dgTGraphs);
		loadFiles(tcFiles, tcTGraphs);
		loadFiles(sapFiles, sapTGraphs);

		// generate similarity matrix
		double[][] dgSimMatrix = new double[dgTGraphs.size()][dgTGraphs.size()];
		double[][] tcSimMatrix = new double[tcTGraphs.size()][tcTGraphs.size()];
		double[][] sapSimMatrix = new double[sapTGraphs.size()][sapTGraphs
				.size()];

		{
			String algo = "astar";
			double dgTime2 = 0.0, tcTime2 = 0.0, sapTime2 = 0.0;
			for (int i = 0; i < REPEAT_TIME; ++i) {
				System.out.print((i + 1) + " ");
				dgTime2 += generateSimMatrix(dgSimMatrix, dgTGraphs, algo);
				tcTime2 += generateSimMatrix(tcSimMatrix, tcTGraphs, algo);
				sapTime2 += generateSimMatrix(sapSimMatrix, sapTGraphs, algo);
			}
			csvWriter.write(algo);
			csvWriter.write("," + dgTime2 / REPEAT_TIME);
			csvWriter.write("," + meet(dgSimMatrix, dgTGraphs));
			csvWriter.write("," + tcTime2 / REPEAT_TIME);
			csvWriter.write("," + meet(tcSimMatrix, tcTGraphs));
			csvWriter.write("," + sapTime2 / REPEAT_TIME);
			csvWriter.write("," + meet(sapSimMatrix, sapTGraphs));
			csvWriter.newLine();
		}
		{
			String algo = "greedy";
			double dgTime2 = 0.0, tcTime2 = 0.0, sapTime2 = 0.0;
			for (int i = 0; i < REPEAT_TIME; ++i) {
				System.out.print((i + 1) + " ");
				dgTime2 += generateSimMatrix(dgSimMatrix, dgTGraphs, algo);
				tcTime2 += generateSimMatrix(tcSimMatrix, tcTGraphs, algo);
				sapTime2 += generateSimMatrix(sapSimMatrix, sapTGraphs, algo);
			}
			csvWriter.write(algo);
			csvWriter.write("," + dgTime2 / REPEAT_TIME);
			csvWriter.write("," + meet(dgSimMatrix, dgTGraphs));
			csvWriter.write("," + tcTime2 / REPEAT_TIME);
			csvWriter.write("," + meet(tcSimMatrix, tcTGraphs));
			csvWriter.write("," + sapTime2 / REPEAT_TIME);
			csvWriter.write("," + meet(sapSimMatrix, sapTGraphs));
			csvWriter.newLine();
		}
		{
			String algo = "alloptions";
			double dgTime2 = 0.0, tcTime2 = 0.0, sapTime2 = 0.0;
			for (int i = 0; i < REPEAT_TIME; ++i) {
				System.out.print((i + 1) + " ");
				dgTime2 += generateSimMatrix(dgSimMatrix, dgTGraphs, algo);
				tcTime2 += generateSimMatrix(tcSimMatrix, tcTGraphs, algo);
				sapTime2 += generateSimMatrix(sapSimMatrix, sapTGraphs, algo);
			}
			csvWriter.write(algo);
			csvWriter.write("," + dgTime2 / REPEAT_TIME);
			csvWriter.write("," + meet(dgSimMatrix, dgTGraphs));
			csvWriter.write("," + tcTime2 / REPEAT_TIME);
			csvWriter.write("," + meet(tcSimMatrix, tcTGraphs));
			csvWriter.write("," + sapTime2 / REPEAT_TIME);
			csvWriter.write("," + meet(sapSimMatrix, sapTGraphs));
			csvWriter.newLine();
		}
		csvWriter.close();
	}

	private double loadFiles(File[] files, List<TLabeledGraph> tGraphs)
			throws Exception {
		tGraphs.clear();
		PnmlImport pnmlImport = new PnmlImport();
		double time = 0;
		for (int i = 0; i < files.length; ++i) {
			FileInputStream input = new FileInputStream(files[i]);
			PetriNet pn = pnmlImport.read(input);
			pn.setName(files[i].getName());
			long a = System.currentTimeMillis();
			tGraphs.add(new TLabeledGraph(pn));
			long b = System.currentTimeMillis();
			time += (b - a);
			input.close();
		}
		return time / files.length;
	}

	private double meet(double[][] simMatrix, List<TLabeledGraph> tGraphs)
			throws IOException {
		if (simMatrix == null || simMatrix.length == 0
				|| Math.abs(simMatrix[0][0] - 0.0) < 0.0001) {
			return 0.0;
		}
		int sum = 0, meet4_2 = 0;
		for (int p = 0; p < simMatrix.length; ++p) {
			for (int q = p + 1; q < simMatrix.length; ++q) {
				for (int r = q + 1; r < simMatrix.length; ++r) {
					++sum;
					if (meet4_2(simMatrix[p][q], simMatrix[p][r],
							simMatrix[q][r])) {
						++meet4_2;
					}
				}
			}
		}
		return sum == 0 ? 0.0 : (double) meet4_2 / (double) sum;
	}

	private double generateSimMatrix(double[][] simMatrix,
			List<TLabeledGraph> tGraphs, String algo) {
		int totalCount = tGraphs.size() * (tGraphs.size() - 1) / 2, finish = 0;
		long totalTime = 0L;
		for (int p = 0; p < tGraphs.size(); ++p) {
			for (int q = p; q < tGraphs.size(); ++q) {
				if (p == q) {
					simMatrix[p][q] = 1.0;
				} else {
					System.out.println(algo + ": " + (++finish) + "/" + totalCount);
					long a = System.currentTimeMillis();
					if (algo.equals("astar")) {
						simMatrix[p][q] = simMatrix[q][p] = TagerCGSimilarity
								.similairity(tGraphs.get(p), tGraphs.get(q));
					} else if (algo.equals("greedy")) {
						simMatrix[p][q] = simMatrix[q][p] = TagerCGSimilarityGreedy
								.similairity(tGraphs.get(p), tGraphs.get(q));
					} else if (algo.equals("alloptions")) {
						simMatrix[p][q] = simMatrix[q][p] = TagerCGSimilarityAllOptions
								.similairity(tGraphs.get(p), tGraphs.get(q));

					}
					long b = System.currentTimeMillis();
					totalTime += (b - a);
				}
			}
		}
		return ((double) totalTime) / ((double) totalCount);
	}

	/**
	 * check equation 4_2
	 */
	public boolean meet4_2(double sim1, double sim2, double sim3) {
		int dis1 = (int) Math.round(multiple * (1 - sim1));
		int dis2 = (int) Math.round(multiple * (1 - sim2));
		int dis3 = (int) Math.round(multiple * (1 - sim3));
		if ((dis1 + dis2 >= dis3) && (dis1 + dis3 >= dis2)
				&& (dis2 + dis3 >= dis1)) {
			return true;
		} else {
			return false;
		}
	}

}
