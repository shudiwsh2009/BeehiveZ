package cn.edu.thss.iise.beehivez.server.metric.tager.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.cfs.CFSSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmAStar;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAbstr;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmGreedy;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.led.LabelEditDistance;

public class TagerVarianceBatchLocal {

	public static final String ROOT_FOLDER = "D:\\百度云同步盘\\Learn@Tsinghua\\过程数据组\\Tager\\08.性质符合实验\\";
	// public static final String ROOT_FOLDER =
	// "E:\\wangshuhao\\Documents\\Tager\\Property\\";
	public static final String REVISION = "_150412a";

	public static final String[] PROPERTY_FOLDERS = {
			ROOT_FOLDER + "01.顺序结构漂移不变性\\", ROOT_FOLDER + "02.跨度负相关性\\",
			ROOT_FOLDER + "03.无关任务递减性\\", ROOT_FOLDER + "04.循环长度负相关性\\",
			ROOT_FOLDER + "05.互斥结构漂移不变性\\", ROOT_FOLDER + "06.循环结构漂移不变性\\",
			ROOT_FOLDER + "07.并发结构漂移不变性\\",
	// ROOT_FOLDER + "08.不平衡性\\"
	};

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		TagerVarianceBatchLocal batch = new TagerVarianceBatchLocal();
		// batch.computeBatch("greedy");
		batch.computeBatch("astar");
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
		String rootFolder = ROOT_FOLDER;
		if (algo.equals("greedy")) {
			measure = new GEDAlgorithmGreedy();
			csvWriter = new BufferedWriter(new FileWriter(rootFolder
					+ "tager_variance_local_greedy.csv"));
		} else if (algo.equals("astar")) {
			measure = new GEDAlgorithmAStar();
			csvWriter = new BufferedWriter(new FileWriter(rootFolder
					+ "tager_variance_local_astar.csv"));
		} else {
			System.out.println("Unsupported algorithm");
			return;
		}
		csvWriter
				.write("simcutoff,k,weightSkippedVertex,weightSubstitutedVertex,weightSkippedEdge,variance");
		csvWriter.newLine();

		// Use another algorithm to measure similarity
		PetriNetSimilarity otherSimilarity = new CFSSimilarity();
		List<Double> otherSimList = new ArrayList<Double>();
		for (String propertyFolder : PROPERTY_FOLDERS) {
			otherSimList.addAll(generateSimMatrix(otherSimilarity,
					propertyFolder));
		}

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
						Object[] weights = new Object[12];
						weights[0] = "vweight";
						weights[1] = weightSkippedVertex;
						weights[2] = "sweight";
						weights[3] = weightSubstitutedVertex;
						weights[4] = "eweight";
						weights[5] = weightSkippedEdge;
						weights[6] = "simcutoff";
						weights[7] = simCutoff;
						weights[8] = "waiting";
						weights[9] = 10000.0;
						LabelEditDistance.scaleWeight = scaleWeight;
						measure.setWeight(weights);
						String parameters = "simcutoff=" + simCutoff + ",k="
								+ scaleWeight + ",weightSkippedVertex="
								+ weightSkippedVertex
								+ ",weightSubstitutedVertex="
								+ weightSubstitutedVertex
								+ ",weightSkippedEdge=" + weightSkippedEdge;
						System.out.println(parameters);
						List<Double> tagerSimList = new ArrayList<Double>();
						for (String propertyFolder : PROPERTY_FOLDERS) {
							tagerSimList.addAll(generateTagerSimMatrix(measure,
									propertyFolder, weightSkippedEdge,
									weightSkippedVertex));
							// tagerSimList.addAll(generateSimMatrix(otherSimilarity,
							// propertyFolder));
						}
						double variance = variance(tagerSimList, otherSimList);
						csvWriter.write("," + variance);
						// generateSimMatrix(dgTagerSimMatrix, measure,
						// dgSimpleCGs, weightSkippedEdge, weightSkippedVertex);
						// generateSimMatrix(tcTagerSimMatrix, measure,
						// tcSimpleCGs, weightSkippedEdge, weightSkippedVertex);
						// generateSimMatrix(sapTagerSimMatrix, measure,
						// sapSimpleCGs, weightSkippedEdge,
						// weightSkippedVertex);
						// csvWriter.write("," + variance(dgTagerSimMatrix,
						// dgOtherSimMatrix) + ","
						// + variance(tcTagerSimMatrix, tcOtherSimMatrix) + ","
						// + variance(sapTagerSimMatrix, sapOtherSimMatrix));
						csvWriter.newLine();
					}
				}
			}
		}

		csvWriter.close();
	}

	private double variance(List<Double> tagerSimList, List<Double> otherSimList) {
		double sum = 0.0;
		for (int i = 0; i < tagerSimList.size(); ++i) {
			if (tagerSimList.get(i) < 0 || tagerSimList.get(i) > 1
					|| otherSimList.get(i) < 0 || otherSimList.get(i) > 1) {
				continue;
			}
			sum += Math.pow(
					Math.abs(tagerSimList.get(i) - otherSimList.get(i)), 2);
		}
		return tagerSimList.isEmpty() ? 0 : sum / tagerSimList.size();
	}

	private List<Double> generateSimMatrix(PetriNetSimilarity measure,
			String propertyFolder) throws Exception {
		String modelFolder = propertyFolder + "Models\\";
		PetriNet originalModel = loadModel(modelFolder + "N1.pnml");
		List<PetriNet> compareModels = loadModels(modelFolder);
		List<Double> simList = new ArrayList<Double>();
		for (PetriNet pn : compareModels) {
			System.out.println(measure + " : " + originalModel.getName()
					+ " & " + pn.getName());
			float result = measure.similarity(originalModel, pn);
			BigDecimal sim = new BigDecimal(result);
			sim = sim.setScale(3, BigDecimal.ROUND_HALF_UP);
			simList.add(sim.doubleValue());
		}
		return simList;
	}

	private List<Double> generateTagerSimMatrix(GEDAbstr measure,
			String propertyFolder, double weightSkippedEdge,
			double weightSkippedVertex) throws Exception {
		String modelFolder = propertyFolder + "Models\\";
		PetriNet originalModel = loadModel(modelFolder + "N1.pnml");
		List<PetriNet> compareModels = loadModels(modelFolder);
		List<Double> simList = new ArrayList<Double>();
		for (PetriNet pn : compareModels) {
			float result = TagerCGSimilarity.similairity(measure,
					new TLabeledGraph(originalModel), new TLabeledGraph(pn),
					weightSkippedEdge, weightSkippedVertex);
			BigDecimal sim = new BigDecimal(result);
			sim = sim.setScale(3, BigDecimal.ROUND_HALF_UP);
			simList.add(sim.doubleValue());
		}
		return simList;
	}

	private PetriNet loadModel(String path) throws Exception {
		PnmlImport pnmlImport = new PnmlImport();
		File file = new File(path);
		FileInputStream input = new FileInputStream(file);
		PetriNet pn = pnmlImport.read(input);
		pn.setName(file.getName());
		return pn;
	}

	private List<PetriNet> loadModels(String path) throws Exception {
		List<PetriNet> models = new ArrayList<PetriNet>();
		PnmlImport pnmlImport = new PnmlImport();
		for (int i = 1;; ++i) {
			File file = new File(path + "N" + i + ".pnml");
			if (!file.exists()) {
				break;
			}
			FileInputStream input = new FileInputStream(file);
			PetriNet pn = pnmlImport.read(input);
			pn.setName(file.getName());
			models.add(pn);
			input.close();
		}
		return models;
	}

}
