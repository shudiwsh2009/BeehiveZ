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
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;

public class TagerTriangleEquationBatch {

	// public static final String ROOT_FOLDER = "F:\\Demo\\";
	// public static String ROOT_FOLDER = "C:\\Users\\Shudi\\Desktop\\tager\\";
	public static final String ROOT_FOLDER = "E:\\wangshuhao\\Documents\\Tager\\";
	public static final int REPEAT_TIME = 20;
	private int multiple = 10000;

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		TagerTriangleEquationBatch tb = new TagerTriangleEquationBatch();
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
				+ "三角不等式满足率计算结果_TAGER算法_3组筛选企业模型20次平均值_150422a.csv"));
		csvWriter
				.write("DG_time1,DG_time2,DG4-2,TC_time1,TC_time2,TC4-2,SAP_tim1,SAP_time2,SAP4-2");
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
		double dgTime1 = loadFiles(dgFiles, dgTGraphs);
		double tcTime1 = loadFiles(tcFiles, tcTGraphs);
		double sapTime1 = loadFiles(sapFiles, sapTGraphs);

		// generate similarity matrix
		double[][] dgSimMatrix = new double[dgTGraphs.size()][dgTGraphs.size()];
		double[][] tcSimMatrix = new double[tcTGraphs.size()][tcTGraphs.size()];
		double[][] sapSimMatrix = new double[sapTGraphs.size()][sapTGraphs
				.size()];

		double dgTime2 = 0.0, tcTime2 = 0.0, sapTime2 = 0.0;
		for (int i = 0; i < REPEAT_TIME; ++i) {
			System.out.print((i + 1) + " ");
			dgTime2 += generateSimMatrix(dgSimMatrix, dgTGraphs);
			tcTime2 += generateSimMatrix(tcSimMatrix, tcTGraphs);
			sapTime2 += generateSimMatrix(sapSimMatrix, sapTGraphs);
		}
		csvWriter.write(dgTime1 + "," + dgTime2 / REPEAT_TIME);
		csvWriter.write("," + meet(dgSimMatrix, dgTGraphs));
		csvWriter.write("," + tcTime1 + "," + tcTime2 / REPEAT_TIME);
		csvWriter.write("," + meet(tcSimMatrix, tcTGraphs));
		csvWriter.write("," + sapTime1 + "," + sapTime2 / REPEAT_TIME);
		csvWriter.write("," + meet(sapSimMatrix, sapTGraphs));
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
			List<TLabeledGraph> tGraphs) {
		int totalCount = tGraphs.size() * (tGraphs.size() - 1) / 2;
		long totalTime = 0L;
		for (int p = 0; p < tGraphs.size(); ++p) {
			for (int q = p; q < tGraphs.size(); ++q) {
				if (p == q) {
					simMatrix[p][q] = 1.0;
				} else {
					long a = System.currentTimeMillis();
					simMatrix[p][q] = simMatrix[q][p] = TagerCGSimilarity
							.similairity(tGraphs.get(p), tGraphs.get(q));
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
