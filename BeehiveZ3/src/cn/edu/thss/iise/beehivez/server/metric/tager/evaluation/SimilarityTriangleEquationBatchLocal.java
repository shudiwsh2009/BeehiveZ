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

import cn.edu.thss.iise.beehivez.server.metric.BTSSimilarity_Wang;
import cn.edu.thss.iise.beehivez.server.metric.CausalFootprintSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.ExtensiveTARSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.bp.BehavioralProfileSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.cfs.CFSSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.rorm.RormSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.ssdt.SSDTSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.utils.FileUtils;

public class SimilarityTriangleEquationBatchLocal {
	
//	public static final String ROOT_FOLDER = "F:\\Demo\\";
//	public static String ROOT_FOLDER = "C:\\Users\\Shudi\\Desktop\\tager\\";
	public static final String ROOT_FOLDER = "E:\\wangshuhao\\Documents\\Tager\\";
	public static final String MEET_FOLDER = ROOT_FOLDER + "NoMeet_Local\\";
	private int multiple = 10000;

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		SimilarityTriangleEquationBatchLocal batch = new SimilarityTriangleEquationBatchLocal();

		BufferedWriter csvWriter = new BufferedWriter(new FileWriter(ROOT_FOLDER + 
				"triangle_equation_other_local.csv"));
		csvWriter.write(",Local_time,Local4-2");
		csvWriter.newLine();
		
		batch.computeBatch(new ExtensiveTARSimilarity(), "TAR", csvWriter);
		batch.computeBatch(new BTSSimilarity_Wang(), "PTS", csvWriter);
		batch.computeBatch(new SSDTSimilarity(), "SSDT", csvWriter);
		batch.computeBatch(new BehavioralProfileSimilarity(), "BP", csvWriter);
//		batch.computeBatch(new CausalFootprintSimilarity(), "CF", csvWriter);
		batch.computeBatch(new CFSSimilarity(), "CFS", csvWriter);
//		batch.computeBatch(new RormSimilarity(), "RORM", csvWriter);
		batch.computeBatch(new TagerCGSimilarity(), "TAGER", csvWriter);
		
		csvWriter.close();
	}
	
	public void computeBatch(PetriNetSimilarity measure, String algo,
			BufferedWriter csvWriter) throws Exception{
		String meetFolder = MEET_FOLDER + algo + "\\";
		String rootFolder = ROOT_FOLDER;
		FileUtils.deleteFolder(meetFolder);
		FileUtils.createFolder(meetFolder);
		
		File folder = new File(rootFolder + "Local");
		File[] files = folder.listFiles();
		List<PetriNet> models = new ArrayList<PetriNet>();
		models = loadFiles(files);
		double[][] simMatrix = new double[models.size()][models.size()];

		csvWriter.write(algo);
		if(algo.equals("TAGER")) {
			List<TLabeledGraph> tGraphs = convert(models);
			
			csvWriter.write("," + generateSimMatrix(simMatrix, tGraphs));
			csvWriter.write("," + meet(simMatrix, models, "Local", meetFolder));
		} else {
			csvWriter.write("," + generateSimMatrix(simMatrix, measure, models));
			csvWriter.write("," + meet(simMatrix, models, "Local", meetFolder));
		}
		csvWriter.newLine();
	}
	
	private List<PetriNet> loadFiles(File[] files) throws Exception {
		List<PetriNet> models = new ArrayList<PetriNet>();
		PnmlImport pnmlImport = new PnmlImport();
		for(int i = 0; i < files.length; ++i) {
			FileInputStream input = new FileInputStream(files[i]);
			PetriNet pn = pnmlImport.read(input);
			pn.setName(files[i].getName());
			models.add(pn);
			input.close();
		}
		return models;
	}
	
	private List<TLabeledGraph> convert(List<PetriNet> models) {
		List<TLabeledGraph> tGraphs = new ArrayList<TLabeledGraph>();
		for(PetriNet pn : models) {
			tGraphs.add(new TLabeledGraph(pn));
		}
		return tGraphs;
	}
	
	private double meet(double[][] simMatrix, List<PetriNet> models,
			String dataset, String meetFolder) throws IOException {
		if(simMatrix == null ||simMatrix.length == 0
				|| Math.abs(simMatrix[0][0] - 0.0) < 0.0001) {
			return 0.0;
		}
		int sum = 0, meet4_2 = 0;
		StringBuilder builder4_2 = new StringBuilder();
		for(int p = 0; p < simMatrix.length; ++p) {
			for(int q = p + 1; q < simMatrix.length; ++q) {
				for(int r = q + 1; r < simMatrix.length; ++r) {
					++sum;
					if(meet4_2(simMatrix[p][q], simMatrix[p][r], simMatrix[q][r])) {
						++meet4_2;
					} else {
						if(builder4_2.length() == 0) {
							builder4_2.append("!Meet4_2\r\n");
						}
						builder4_2.append(models.get(p).getName());
						builder4_2.append(" ");
						builder4_2.append(models.get(q).getName());
						builder4_2.append(" ");
						builder4_2.append(models.get(r).getName());
						builder4_2.append("\r\n");
					}
				}
			}
		}
		if(builder4_2.length() != 0) {
			BufferedWriter writer = new BufferedWriter(
					new FileWriter(meetFolder + dataset + ".txt"));
			writer.write(builder4_2.toString());
			writer.close();
		}
		return sum == 0 ? 0.0 : (double) meet4_2 / (double) sum;
	}
	
	private double generateSimMatrix(double[][] simMatrix, PetriNetSimilarity measure, List<PetriNet> models) {
		int totalCount = models.size() * (models.size() - 1) / 2, finish = 0;
		long totalTime = 0L;
		for(int p = 0; p < models.size(); ++p) {
			for(int q = p; q < models.size(); ++q) {
				if(p == q) {
					simMatrix[p][q] = 1.0;
				} else {
					System.out.println((++finish) + "/" + totalCount + " " + models.get(p).getName() + " & " + models.get(q).getName());
					Long a = System.currentTimeMillis();
					simMatrix[p][q] = simMatrix[q][p]
							= measure.similarity(models.get(p), models.get(q));
					Long b = System.currentTimeMillis();
					totalTime += (b - a);
				}
			}
		}
		return ((double) totalTime) / ((double) totalCount);
	}
	
	private double generateSimMatrix(double[][] simMatrix, List<TLabeledGraph> tGraphs) {
		int totalCount = tGraphs.size() * (tGraphs.size() - 1) / 2, finish = 0;
		long totalTime = 0L;
		for(int p = 0; p < tGraphs.size(); ++p) {
			for(int q = p; q < tGraphs.size(); ++q) {
				if(p == q) {
					simMatrix[p][q] = 1.0;
				} else {
					System.out.println((++finish) + "/" + totalCount + " " 
							+ tGraphs.get(p).getName() + " & " + tGraphs.get(q).getName());
					long a = System.currentTimeMillis();
					simMatrix[p][q] = simMatrix[q][p]
							= TagerCGSimilarity.similairity(tGraphs.get(p), tGraphs.get(q));
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

	/**
	 * check equation 4_3
	 * use -1 to represent positive infinity
	 */
	public boolean meet4_3(double sim1, double sim2, double sim3) {
		double dis1;
		if (sim1 == 0.0) {
			dis1 = -1.0;
		} else {
			dis1 = 1 / sim1 - 1.0;
		}
		double dis2;
		if (sim2 == 0.0) {
			dis2 = -1.0;
		} else {
			dis2 = 1 / sim2 - 1.0;
		}
		double dis3;
		if (sim3 == 0.0) {
			dis3 = -1.0;
		} else {
			dis3 = 1 / sim3 - 1.0;
		}
		if (((dis1 == -1.0) && (dis2 != -1.0) && (dis3 != -1.0))
				|| ((dis1 != -1.0) && (dis2 == -1.0) && (dis3 != -1.0))
				|| ((dis1 != -1.0) && (dis2 != -1.0) && (dis3 == -1.0))) {
			return false;
		} else if (((dis1 == -1.0) && (dis2 == -1.0) && (dis3 != -1.0))
				|| ((dis1 == -1.0) && (dis2 != -1.0) && (dis3 == -1.0))
				|| ((dis1 != -1.0) && (dis2 == -1.0) && (dis3 == -1.0))) {
			return true;
		} else if ((dis1 == -1.0) && (dis2 == -1.0) && (dis3 == -1.0)) {
			return true;
		} else if ((dis1 + dis2 >= dis3) && (dis1 + dis3 >= dis2)
				&& (dis2 + dis3 >= dis1)) {
			return true;
		} else {
			return false;
		}
	}

}
