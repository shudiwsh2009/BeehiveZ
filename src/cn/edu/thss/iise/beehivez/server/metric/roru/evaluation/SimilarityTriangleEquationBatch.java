package cn.edu.thss.iise.beehivez.server.metric.roru.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.io.PNMLSerializer;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import com.iise.shudi.bp.BehavioralProfileSimilarity;
import com.iise.shudi.exroru.RefinedOrderingRelation;
import com.iise.shudi.exroru.RormSimilarity;

import cn.edu.thss.iise.beehivez.server.metric.BTSSimilarity_Wang;
import cn.edu.thss.iise.beehivez.server.metric.ExtensiveTARSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.cfs.CFSSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.utils.FileUtils;

public class SimilarityTriangleEquationBatch {
	
//	public static final String ROOT_FOLDER = "F:\\Demo\\";
//	public static final String ROOT_FOLDER = "C:\\Users\\Shudi\\Desktop\\rorm\\";
	public static final String ROOT_FOLDER = "E:\\wangshuhao\\Documents\\ExRORU\\";
	public static final String MEET_FOLDER = ROOT_FOLDER + "NoMeet\\";
	private int multiple = 10000;

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		RefinedOrderingRelation.SDA_WEIGHT = 0.0;
		RefinedOrderingRelation.IMPORTANCE = true;
		SimilarityTriangleEquationBatch batch = new SimilarityTriangleEquationBatch();

		BufferedWriter csvWriter = new BufferedWriter(new FileWriter(ROOT_FOLDER + 
				"多算法三角不等式满足率_3组筛选企业模型_150718a.csv"));
		csvWriter.write(",DG_time,DG4-2,TC_time,TC4-2,SAP_time,SAP4-2");
		csvWriter.newLine();
		
		batch.computeBatch(new ExtensiveTARSimilarity(), "TAR", csvWriter);
		batch.computeBatch(new BTSSimilarity_Wang(), "PTS", csvWriter);
//		batch.computeBatch(new SSDTSimilarity(), "SSDT", csvWriter);
//		batch.computeBatch(new BehavioralProfileSimilarity(), "BP", csvWriter);
//		batch.computeBatch(new CausalFootprintSimilarity(), "CF", csvWriter);
		batch.computeBatch(new CFSSimilarity(), "CFS", csvWriter);
		batch.computeBatch(new TagerCGSimilarity(), "TAGER", csvWriter);
		batch.computeBatchRORU(csvWriter);
		
		csvWriter.close();
	}
	
	public void computeBatch(PetriNetSimilarity measure, String algo,
			BufferedWriter csvWriter) throws Exception{
		String meetFolder = MEET_FOLDER + algo + "\\";
		String rootFolder = ROOT_FOLDER;
		FileUtils.deleteFolder(meetFolder);
		FileUtils.createFolder(meetFolder);
				
		File dgFolder = new File(rootFolder + "DG");
		File tcFolder = new File(rootFolder + "TC");
		File sapFolder = new File(rootFolder + "SAP");
		File[] dgFiles = dgFolder.listFiles();
		File[] tcFiles = tcFolder.listFiles();
		File[] sapFiles = sapFolder.listFiles();
		List<PetriNet> dgSimpleCGs = new ArrayList<PetriNet>();
		List<PetriNet> tcSimpleCGs = new ArrayList<PetriNet>();
		List<PetriNet> sapSimpleCGs = new ArrayList<PetriNet>();

		// load petri nets and convert to simpleCG
		dgSimpleCGs = loadFiles(dgFiles);
		tcSimpleCGs = loadFiles(tcFiles);
		sapSimpleCGs = loadFiles(sapFiles);
		
		// generate similarity matrix
		double[][] dgSimMatrix = new double[dgSimpleCGs.size()][dgSimpleCGs.size()];
		double[][] tcSimMatrix = new double[tcSimpleCGs.size()][tcSimpleCGs.size()];
		double[][] sapSimMatrix = new double[sapSimpleCGs.size()][sapSimpleCGs.size()];
		
		csvWriter.write(algo);
		csvWriter.write("," + generateSimMatrix(dgSimMatrix, measure, dgSimpleCGs));
		csvWriter.write("," + meet(dgSimMatrix, dgSimpleCGs, "DG", meetFolder));
		csvWriter.write("," + generateSimMatrix(tcSimMatrix, measure, tcSimpleCGs));
		csvWriter.write("," + meet(tcSimMatrix, tcSimpleCGs, "TC", meetFolder));
		csvWriter.write("," + generateSimMatrix(sapSimMatrix, measure, sapSimpleCGs));
		csvWriter.write("," + meet(sapSimMatrix, sapSimpleCGs, "SAP", meetFolder));
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
	
	public void computeBatchRORU(BufferedWriter csvWriter) throws Exception {
		String meetFolder = MEET_FOLDER + "RORU\\";
		String rootFolder = ROOT_FOLDER;
		FileUtils.deleteFolder(meetFolder);
		FileUtils.createFolder(meetFolder);
		
		File dgFolder = new File(rootFolder + "DG");
		File tcFolder = new File(rootFolder + "TC");
		File sapFolder = new File(rootFolder + "SAP");
		File[] dgFiles = dgFolder.listFiles();
		File[] tcFiles = tcFolder.listFiles();
		File[] sapFiles = sapFolder.listFiles();
		List<NetSystem> dgNets = new ArrayList<>();
		List<NetSystem> tcNets = new ArrayList<>();
		List<NetSystem> sapNets = new ArrayList<>();
		
		// load net systems
		dgNets = loadNets(dgFiles);
		tcNets = loadNets(tcFiles);
		sapNets = loadNets(sapFiles);
		
		// generate similarity matrix
		double[][] dgSimMatrix = new double[dgNets.size()][dgNets.size()];
		double[][] tcSimMatrix = new double[tcNets.size()][tcNets.size()];
		double[][] sapSimMatrix = new double[sapNets.size()][sapNets.size()];
		
		csvWriter.write("BP");
		csvWriter.write("," + generateSimMatrixBP(dgSimMatrix, dgNets));
		csvWriter.write("," + meetNets(dgSimMatrix, dgNets, "DG", meetFolder));
		csvWriter.write("," + generateSimMatrixBP(tcSimMatrix, tcNets));
		csvWriter.write("," + meetNets(tcSimMatrix, tcNets, "TC", meetFolder));
		csvWriter.write("," + generateSimMatrixBP(sapSimMatrix, sapNets));
		csvWriter.write("," + meetNets(sapSimMatrix, sapNets, "SAP", meetFolder));
		csvWriter.newLine();
		
		csvWriter.write("ExRORU");
		csvWriter.write("," + generateSimMatrixExRORU(dgSimMatrix, dgNets));
		csvWriter.write("," + meetNets(dgSimMatrix, dgNets, "DG", meetFolder));
		csvWriter.write("," + generateSimMatrixExRORU(tcSimMatrix, tcNets));
		csvWriter.write("," + meetNets(tcSimMatrix, tcNets, "TC", meetFolder));
		csvWriter.write("," + generateSimMatrixExRORU(sapSimMatrix, sapNets));
		csvWriter.write("," + meetNets(sapSimMatrix, sapNets, "SAP", meetFolder));
		csvWriter.newLine();
	}
	
	private List<NetSystem> loadNets(File[] files) throws Exception {
		List<NetSystem> nets = new ArrayList<>();
		PNMLSerializer pnmlSerializer = new PNMLSerializer();
		for(int i = 0; i < files.length; ++i) {
			NetSystem net = pnmlSerializer.parse(files[i].getAbsolutePath());
			net.setName(files[i].getName());
			nets.add(net);
		}
		return nets;
	}
	
	private double meetNets(double[][] simMatrix, List<NetSystem> nets,
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
						builder4_2.append(nets.get(p).getName());
						builder4_2.append(" ");
						builder4_2.append(nets.get(q).getName());
						builder4_2.append(" ");
						builder4_2.append(nets.get(r).getName());
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
	
	private double generateSimMatrixExRORU(double[][] simMatrix, List<NetSystem> nets) {
		int totalCount = nets.size() * (nets.size() - 1) / 2, finish = 0;
		long totalTime = 0L;
		RormSimilarity rorm = new RormSimilarity();
		for(int p = 0; p < nets.size(); ++p) {
			for(int q = p; q < nets.size(); ++q) {
				if(p == q) {
					simMatrix[p][q] = 1.0;
				} else {
					System.out.println((++finish) + "/" + totalCount + " " + nets.get(p).getName() + " & " + nets.get(q).getName());
					Long a = System.currentTimeMillis();
					double sim = rorm.similarity(nets.get(p), nets.get(q));
					simMatrix[p][q] = simMatrix[q][p] = sim;
					if(sim == Float.MIN_VALUE) {
						--totalCount;
						continue;
					}
					Long b = System.currentTimeMillis();
					totalTime += (b - a);
				}
			}
		}
		return ((double) totalTime) / ((double) totalCount);
	}
	
	private double generateSimMatrixBP(double[][] simMatrix, List<NetSystem> nets) {
		int totalCount = nets.size() * (nets.size() - 1) / 2, finish = 0;
		long totalTime = 0L;
		BehavioralProfileSimilarity bp = new BehavioralProfileSimilarity();
		for(int p = 0; p < nets.size(); ++p) {
			for(int q = p; q < nets.size(); ++q) {
				if(p == q) {
					simMatrix[p][q] = 1.0;
				} else {
					System.out.println((++finish) + "/" + totalCount + " " + nets.get(p).getName() + " & " + nets.get(q).getName());
					Long a = System.currentTimeMillis();
					double sim = bp.similarity(nets.get(p), nets.get(q));
					simMatrix[p][q] = simMatrix[q][p] = sim;
					if(sim == Float.MIN_VALUE) {
						--totalCount;
						continue;
					}
					Long b = System.currentTimeMillis();
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
		if (dis1 == 0 || dis2 == 0 || dis3 == 0) {
			return true;
		}
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
