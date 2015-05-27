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
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmAStar;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAbstr;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmGreedy;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.led.LabelEditDistance;
import cn.edu.thss.iise.beehivez.server.metric.tager.utils.FileUtils;

public class TagerTriangleEquationBatchMutipleParams {

	// public static final String ROOT_FOLDER = "F:\\Demo\\";
	// public static String ROOT_FOLDER = "C:\\Users\\Shudi\\Desktop\\tager\\";
	public static final String ROOT_FOLDER = "E:\\wangshuhao\\Documents\\Tager\\";
	public static final String MEET_FOLDER = ROOT_FOLDER + "NoMeet\\";
	private int multiple = 10000;

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		TagerTriangleEquationBatchMutipleParams tb = new TagerTriangleEquationBatchMutipleParams();
		tb.computeBatch("greedy");
		tb.computeBatch("astar");
	}

	/**
	 * compute the fitness of triangle equations 4_2 and 4_3
	 * 
	 * @param algo
	 * @throws Exception
	 */
	public void computeBatch(String algo) throws Exception {
		GEDAbstr measure;
		BufferedWriter csvWriter;
		String meetFolder = MEET_FOLDER + "tager\\" + algo + "\\";
		String rootFolder = ROOT_FOLDER;
		FileUtils.deleteFolder(meetFolder);
		if (algo.equals("greedy")) {
			measure = new GEDAlgorithmGreedy();
			csvWriter = new BufferedWriter(new FileWriter(rootFolder
					+ "triangle_equation_tager_greedy.csv"));
		} else if (algo.equals("astar")) {
			measure = new GEDAlgorithmAStar();
			csvWriter = new BufferedWriter(new FileWriter(rootFolder
					+ "triangle_equation_tager_astar.csv"));
		} else {
			System.out.println("Unsupported algorithm");
			return;
		}
		FileUtils.createFolder(meetFolder);
		csvWriter
				.write("simcutoff,k,weightSkippedVertex,weightSubstitutedVertex,weightSkippedEdge,"
						+ "DG_time,DG4-2,TC_time,TC4-2,SAP_time,SAP4-2");
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
		dgTGraphs = loadFiles(dgFiles);
		tcTGraphs = loadFiles(tcFiles);
		sapTGraphs = loadFiles(sapFiles);

		// generate similarity matrix
		double[][] dgSimMatrix = new double[dgTGraphs.size()][dgTGraphs.size()];
		double[][] tcSimMatrix = new double[tcTGraphs.size()][tcTGraphs.size()];
		double[][] sapSimMatrix = new double[sapTGraphs.size()][sapTGraphs
				.size()];

		double[] scaleWeights = { 0.5, 1.0, 1.5, 2.0, 2.5 };
		double[] simCutoffs = { 0.0 };
		for (double simCutoff : simCutoffs) {
			for (double scaleWeight : scaleWeights) {
				for (int i = 1; i <= 10; ++i) {
					double weightSkippedVertex = i / 10.0;
					for (int j = 1; j <= 10; ++j) {
						// set parameters
						double weightSkippedEdge = j / 10.0;
						double weightSubstitutedVertex = (i + 2 * j) / 10.0;
						csvWriter.write(simCutoff + "," + scaleWeight + ","
								+ weightSkippedVertex + ","
								+ weightSubstitutedVertex + ","
								+ weightSkippedEdge);
						Object[] weights = new Object[10];
						weights[0] = "vweight";
						weights[1] = weightSkippedVertex;
						weights[2] = "sweight";
						weights[3] = weightSubstitutedVertex;
						weights[4] = "eweight";
						weights[5] = weightSkippedEdge;
						weights[6] = "simcutoff";
						weights[7] = simCutoff;
						weights[8] = "waiting";
						weights[9] = 0.0;
						LabelEditDistance.scaleWeight = scaleWeight;
						measure.setWeight(weights);
						String parameters = "simcutoff=" + simCutoff + ",k="
								+ scaleWeight + ",weightSkippedVertex="
								+ weightSkippedVertex
								+ ",weightSubstitutedVertex="
								+ weightSubstitutedVertex
								+ ",weightSkippedEdge=" + weightSkippedEdge;
						System.out.println(parameters);
						csvWriter.write(","
								+ generateSimMatrix(dgSimMatrix, measure,
										dgTGraphs, weightSkippedEdge,
										weightSkippedVertex));
						csvWriter.write(","
								+ meet(dgSimMatrix, dgTGraphs, "DG",
										parameters, meetFolder));
						csvWriter.write(","
								+ generateSimMatrix(tcSimMatrix, measure,
										tcTGraphs, weightSkippedEdge,
										weightSkippedVertex));
						csvWriter.write(","
								+ meet(tcSimMatrix, tcTGraphs, "TC",
										parameters, meetFolder));
						csvWriter.write(","
								+ generateSimMatrix(sapSimMatrix, measure,
										sapTGraphs, weightSkippedEdge,
										weightSkippedVertex));
						csvWriter.write(","
								+ meet(sapSimMatrix, sapTGraphs, "SAP",
										parameters, meetFolder));
						csvWriter.newLine();
					}
				}
			}
		}

		csvWriter.close();
	}

	private List<TLabeledGraph> loadFiles(File[] files) throws Exception {
		List<TLabeledGraph> tGraphs = new ArrayList<TLabeledGraph>();
		PnmlImport pnmlImport = new PnmlImport();
		for (int i = 0; i < files.length; ++i) {
			FileInputStream input = new FileInputStream(files[i]);
			PetriNet pn = pnmlImport.read(input);
			pn.setName(files[i].getName());
			tGraphs.add(new TLabeledGraph(pn));
			input.close();
		}
		return tGraphs;
	}

	private double meet(double[][] simMatrix, List<TLabeledGraph> tGraphs,
			String dataset, String parameters, String meetFolder)
			throws IOException {
		if (simMatrix == null || simMatrix.length == 0
				|| Math.abs(simMatrix[0][0] - 0.0) < 0.0001) {
			return 0.0;
		}
		int sum = 0, meet4_2 = 0;
		StringBuilder builder4_2 = new StringBuilder();
		for (int p = 0; p < simMatrix.length; ++p) {
			for (int q = p + 1; q < simMatrix.length; ++q) {
				for (int r = q + 1; r < simMatrix.length; ++r) {
					++sum;
					if (meet4_2(simMatrix[p][q], simMatrix[p][r],
							simMatrix[q][r])) {
						++meet4_2;
					} else {
						if (builder4_2.length() == 0) {
							builder4_2.append("!Meet4_2\r\n");
						}
						builder4_2.append(tGraphs.get(p).getName());
						builder4_2.append(" ");
						builder4_2.append(tGraphs.get(q).getName());
						builder4_2.append(" ");
						builder4_2.append(tGraphs.get(r).getName());
						builder4_2.append("\r\n");
					}
				}
			}
		}
		if (builder4_2.length() != 0) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(
					meetFolder + parameters + "-" + dataset + ".txt"));
			writer.write(builder4_2.toString());
			writer.close();
		}
		return sum == 0 ? 0.0 : (double) meet4_2 / (double) sum;
	}

	private double generateSimMatrix(double[][] simMatrix,
			GEDAbstr measure, List<TLabeledGraph> tGraphs,
			double weightSkippedEdge, double weightSkippedVertex) {
		int totalCount = tGraphs.size() * (tGraphs.size() - 1) / 2, finish = 0;
		long totalTime = 0L;
		for (int p = 0; p < tGraphs.size(); ++p) {
			for (int q = p; q < tGraphs.size(); ++q) {
				if (p == q) {
					simMatrix[p][q] = 1.0;
				} else {
					// System.out.println((++finish) + "/" + totalCount + " "
					// + simpleCGs.get(p).getFilename() + " & " +
					// simpleCGs.get(q).getFilename());
					long a = System.currentTimeMillis();
					simMatrix[p][q] = simMatrix[q][p] = TagerCGSimilarity
							.similairity(measure, tGraphs.get(p),
									tGraphs.get(q), weightSkippedEdge,
									weightSkippedVertex);
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
