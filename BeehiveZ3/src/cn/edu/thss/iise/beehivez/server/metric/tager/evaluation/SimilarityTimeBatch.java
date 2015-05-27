package cn.edu.thss.iise.beehivez.server.metric.tager.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.BTSSimilarity_Wang;
import cn.edu.thss.iise.beehivez.server.metric.JaccardTARSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.bp.BehavioralProfileSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.cfs.CFSSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarityGreedy;

public class SimilarityTimeBatch {

//	public static final String ROOT_FOLDER = "C:\\Users\\Shudi\\Desktop\\tager\\";
	public static final String ROOT_FOLDER = "E:\\wangshuhao\\Documents\\Tager\\";
	public static final int[] RANDOM_COUNTS = { 20, 40, 60, 80, 100, 120, 140,
			160 };
	public static final String[] SIM_MEASURE = { "TAR", "PTS", "BP",
			"CFS", "TAGER", "GREEDY" };

	public static Map<String, PetriNetSimilarity> hmMeasure = new HashMap<String, PetriNetSimilarity>();
	static {
		hmMeasure.put("TAR", new JaccardTARSimilarity());
		hmMeasure.put("PTS", new BTSSimilarity_Wang());
//		hmMeasure.put("SSDT", new SSDTSimilarity());
		hmMeasure.put("BP", new BehavioralProfileSimilarity());
//		hmMeasure.put("CF", new CausalFootprintSimilarity());
		hmMeasure.put("CFS", new CFSSimilarity());
		hmMeasure.put("TAGER", new TagerCGSimilarity());
		hmMeasure.put("GREEDY", new TagerCGSimilarityGreedy());
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		BufferedWriter csvWriter = new BufferedWriter(new FileWriter(
				ROOT_FOLDER + "7类算法计算时间_3组筛选企业模型_增量模型集测时间_150422a.csv"));
		try {
			SimilarityTimeBatch batch = new SimilarityTimeBatch();
			for (int i : RANDOM_COUNTS) {
				csvWriter.write("," + i + "," + i);
			}
			csvWriter.newLine();
			batch.computeBatch(csvWriter);
			csvWriter.close();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			csvWriter.close();
		}
	}

	public void computeBatch(BufferedWriter csvWriter) throws Exception {
		String rootFolder = ROOT_FOLDER + "Enterprise\\";
		File folder = new File(rootFolder);
		File[] files = folder.listFiles();
		List<PetriNet> models = loadFiles(files);

		List<List<PetriNet>> randomModelsList = new ArrayList<List<PetriNet>>();
		for (int i : RANDOM_COUNTS) {
			randomModelsList.add(randomSelected(i, models));
		}

		for (String algo : SIM_MEASURE) {
			csvWriter.write(algo);
			for (List<PetriNet> randomModels : randomModelsList) {
				System.out.println(algo + " " + randomModels.size());
				double[] time = generateSimMatrix(hmMeasure.get(algo),
						randomModels, algo);
				csvWriter.write("," + time[0] + "," + time[1]);
			}
			csvWriter.newLine();
		}
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

	private List<PetriNet> randomSelected(int total, List<PetriNet> models) {
		Collections.shuffle(models);
		total = Math.min(total, models.size());
		return models.subList(0, total);
	}

	private double[] generateSimMatrix(PetriNetSimilarity measure,
			List<PetriNet> models, String algo) {
		int totalCount = models.size() * models.size(), finish = 0;
		long totalTime = 0L;
		for (int p = 0; p < models.size(); ++p) {
			for (int q = 0; q < models.size(); ++q) {
				System.out.println(algo + ": " + (++finish) + "/" + totalCount
						+ " " + models.get(p).getName() + " & "
						+ models.get(q).getName());
				Long a = System.currentTimeMillis();
				measure.similarity(models.get(p), models.get(q));
				Long b = System.currentTimeMillis();
				totalTime += (b - a);
			}
		}
		double avgTime = ((double) totalTime) / totalCount;
		return new double[] { totalTime, avgTime };
	}

}
