package cn.edu.thss.iise.beehivez.server.metric.tager.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.basicprocess.CTree;
import cn.edu.thss.iise.beehivez.server.basicprocess.CTreeGenerator;
import cn.edu.thss.iise.beehivez.server.basicprocess.mymodel.MyPetriNet;
import cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.cfs.CFSSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.cfs.dep.NewPtsSet;
import cn.edu.thss.iise.beehivez.server.metric.cfs.dep.TTreeGenerator;
import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmAStar;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAbstr;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmGreedy;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.led.LabelEditDistance;
import cn.edu.thss.iise.beehivez.server.metric.tager.utils.FileUtils;

public class TagerVarianceBatch {

	// public static final String ROOT_FOLDER = "F:\\Demo\\";
	// public static String ROOT_FOLDER = "C:\\Users\\Shudi\\Desktop\\tager\\";
	public static final String ROOT_FOLDER = "E:\\wangshuhao\\Documents\\Tager\\";
	public static final int RANDOM_COUNT = 10;
	public static final double RANDOM_RATIO = 0.3;

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		TagerVarianceBatch batch = new TagerVarianceBatch();
		batch.computeBatch("greedy");
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
		String simFolder = rootFolder + "Similarity\\";
		FileUtils.deleteDirectory(simFolder);
		FileUtils.createFolder(simFolder);
		if (algo.equals("greedy")) {
			measure = new GEDAlgorithmGreedy();
			csvWriter = new BufferedWriter(new FileWriter(rootFolder
					+ "贪心参数取值_3组筛选企业模型_随机取0.3与CFS算方差_150422a.csv"));
		} else if (algo.equals("astar")) {
			measure = new GEDAlgorithmAStar();
			csvWriter = new BufferedWriter(new FileWriter(rootFolder
					+ "AStar参数取值_3组筛选企业模型_随机取0.3与CFS算方差_150422a.csv"));
		} else {
			System.out.println("Unsupported algorithm");
			return;
		}
		csvWriter
				.write("simcutoff,k,weightSkippedVertex,weightSubstitutedVertex,weightSkippedEdge,variance");
		csvWriter.newLine();

		File dgFolder = new File(rootFolder + "DG");
		File tcFolder = new File(rootFolder + "TC");
		File sapFolder = new File(rootFolder + "SAP");
		File[] dgFiles = dgFolder.listFiles();
		File[] tcFiles = tcFolder.listFiles();
		File[] sapFiles = sapFolder.listFiles();
		List<PetriNet> dgModels = new ArrayList<PetriNet>();
		List<PetriNet> tcModels = new ArrayList<PetriNet>();
		List<PetriNet> sapModels = new ArrayList<PetriNet>();

		// load petri nets
		dgModels = loadFiles(dgFiles);
		tcModels = loadFiles(tcFiles);
		sapModels = loadFiles(sapFiles);

		List<List<PetriNet>> randomModelsList = new ArrayList<List<PetriNet>>();
		for (int i = 0; i < RANDOM_COUNT; ++i) {
			randomModelsList.add(randomSelected(RANDOM_RATIO, dgModels,
					tcModels, sapModels));
		}
		// Use another algorithm to measure similarity
		PetriNetSimilarity otherSimilarity = new CFSSimilarity();
		List<double[][]> otherSimMatrixList = new ArrayList<double[][]>();
		for (int i = 0; i < RANDOM_COUNT; ++i) {
			otherSimMatrixList.add(generateCFSSimMatrix(
					randomModelsList.get(i), otherSimilarity, i, simFolder));
		}
		// double[][] dgOtherSimMatrix = generateCFSSimMatrix(dgModels,
		// otherSimilarity);
		// double[][] tcOtherSimMatrix = generateCFSSimMatrix(tcModels,
		// otherSimilarity);
		// double[][] sapOtherSimMatrix = generateCFSSimMatrix(sapModels,
		// otherSimilarity);

		// simpleCGs
		List<List<TLabeledGraph>> randomTGraphsList = new ArrayList<List<TLabeledGraph>>();
		for (int i = 0; i < RANDOM_COUNT; ++i) {
			randomTGraphsList.add(convert(randomModelsList.get(i)));
		}
		List<double[][]> tagerSimMatrixList = new ArrayList<double[][]>();
		for (int i = 0; i < RANDOM_COUNT; ++i) {
			tagerSimMatrixList
					.add(new double[randomTGraphsList.get(i).size()][randomTGraphsList
							.get(i).size()]);
		}
		// List<SimpleCG> dgSimpleCGs = convert(dgModels);
		// List<SimpleCG> tcSimpleCGs = convert(tcModels);
		// List<SimpleCG> sapSimpleCGs = convert(sapModels);
		// double[][] dgTagerSimMatrix = new
		// double[dgModels.size()][dgModels.size()];
		// double[][] tcTagerSimMatrix = new
		// double[tcModels.size()][tcModels.size()];
		// double[][] sapTagerSimMatrix = new
		// double[sapModels.size()][sapModels.size()];

		double[] scaleWeights = { 1.0, 1.5, 2.0, 2.5 };
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
						double variance = 0.0;
						for (int k = 0; k < RANDOM_COUNT; ++k) {
							generateSimMatrix(tagerSimMatrixList.get(k),
									measure, randomTGraphsList.get(k),
									weightSkippedEdge, weightSkippedVertex,
									parameters, k, simFolder);
							variance += variance(tagerSimMatrixList.get(k),
									otherSimMatrixList.get(k));
						}
						variance /= RANDOM_COUNT;
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

	private double variance(double[][] tagerSimMatrix, double[][] otherSimMatrix) {
		double sum = 0.0;
		int count = 0;
		for (int i = 0; i < tagerSimMatrix.length; ++i) {
			for (int j = i + 1; j < tagerSimMatrix.length; ++j) {
				if (tagerSimMatrix[i][j] < 0 || tagerSimMatrix[i][j] > 1
						|| otherSimMatrix[i][j] < 0 || otherSimMatrix[i][j] > 1) {
					continue;
				}
				sum += Math.pow(
						Math.abs(tagerSimMatrix[i][j] - otherSimMatrix[i][j]),
						2);
				++count;
			}
		}
		return count == 0 ? 0 : Math.sqrt(sum / count);
	}

	private List<PetriNet> loadFiles(File[] files) throws Exception {
		List<PetriNet> models = new ArrayList<PetriNet>();
		PnmlImport pnmlImport = new PnmlImport();
		for (int i = 0; i < files.length; ++i) {
			FileInputStream input = new FileInputStream(files[i]);
			PetriNet pn = pnmlImport.read(input);
			pn.setName(files[i].getName());
			models.add(pn);
			input.close();
		}
		return models;
	}

	private List<PetriNet> randomSelected(double ratio,
			List<PetriNet>... models) {
		List<PetriNet> randoms = new ArrayList<PetriNet>();
		for (List<PetriNet> list : models) {
			int total = ratio >= 1 ? list.size() : (int) (list.size() * ratio);
			Collections.shuffle(list);
			randoms.addAll(list.subList(0, total));
		}
		return randoms;
	}

	private void generateSimMatrix(double[][] simMatrix, GEDAbstr measure,
			List<TLabeledGraph> tGraphs, double weightSkippedEdge,
			double weightSkippedVertex, String parameters, int k,
			String simFolder) throws IOException {
		StringBuilder builder = new StringBuilder();
		for (int p = 0; p < tGraphs.size(); ++p) {
			for (int q = p; q < tGraphs.size(); ++q) {
				if (p == q) {
					simMatrix[p][q] = 1.0;
				} else {
					simMatrix[p][q] = simMatrix[q][p] = TagerCGSimilarity
							.similairity(measure, tGraphs.get(p),
									tGraphs.get(q), weightSkippedEdge,
									weightSkippedVertex);
					builder.append(tGraphs.get(p).getName());
					builder.append(" & ");
					builder.append(tGraphs.get(q).getName());
					builder.append("  ");
					builder.append(simMatrix[p][q]);
					builder.append("\r\n");
				}
			}
		}
		if (builder.length() > 0) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(simFolder
					+ "tager-" + parameters + "-" + k + ".txt"));
			writer.write(builder.toString());
			writer.close();
		}
	}

	private double[][] generateOtherSimMatrix(List<PetriNet> models,
			PetriNetSimilarity measure) {
		double[][] simMatrix = new double[models.size()][models.size()];
		int finish = 0, total = models.size() * (1 + models.size()) / 2;
		for (int p = 0; p < models.size(); ++p) {
			for (int q = p; q < models.size(); ++q) {
				System.out.println((++finish) + "/" + total + " "
						+ models.get(p).getName() + " & "
						+ models.get(q).getName());
				if (p == q) {
					simMatrix[p][q] = 1.0;
				} else {
					simMatrix[p][q] = simMatrix[q][p] = measure.similarity(
							models.get(p), models.get(q));
				}
			}
		}
		return simMatrix;
	}

	private double[][] generateCFSSimMatrix(List<PetriNet> models,
			PetriNetSimilarity measure, int k, String simFolder)
			throws IOException {
		ArrayList<NewPtsSet> setArray = new ArrayList<NewPtsSet>();
		for (PetriNet pn : models) {
			CTreeGenerator generator = new CTreeGenerator(
					MyPetriNet.PromPN2MyPN(pn));
			CTree ctree = generator.generateCTree();
			TTreeGenerator ttg = new TTreeGenerator();
			NewPtsSet nps = ttg.generatTTree(ctree, 2, pn.getName());// 循环次数
			// nps.showSet();
			setArray.add(nps);
		}
		double[][] simMatrix = new double[models.size()][models.size()];
		StringBuilder builder = new StringBuilder();
		int finish = 0, total = models.size() * (1 + models.size()) / 2;
		for (int p = 0; p < models.size(); ++p) {
			for (int q = p; q < models.size(); ++q) {
				System.out.println((++finish) + "/" + total + " "
						+ models.get(p).getName() + " & "
						+ models.get(q).getName());
				if (p == q) {
					simMatrix[p][q] = 1.0;
				} else {
					double[][] seqM = computeSeqMatrix(setArray.get(p),
							setArray.get(q));
					simMatrix[p][q] = simMatrix[q][p] = computeSimilarityForTwoNet_Astar(
							seqM, setArray.get(p), setArray.get(q));
					builder.append(models.get(p).getName());
					builder.append(" & ");
					builder.append(models.get(q).getName());
					builder.append("  ");
					builder.append(simMatrix[p][q]);
					builder.append("\r\n");
				}
			}
		}
		if (builder.length() > 0) {
			BufferedWriter writer = new BufferedWriter(new FileWriter(simFolder
					+ "cfs-" + k + ".txt"));
			writer.write(builder.toString());
			writer.close();
		}
		return simMatrix;
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

	private List<TLabeledGraph> convert(List<PetriNet> models) {
		List<TLabeledGraph> tGraphs = new ArrayList<TLabeledGraph>();
		for (PetriNet pn : models) {
			tGraphs.add(new TLabeledGraph(pn));
		}
		return tGraphs;
	}

}
