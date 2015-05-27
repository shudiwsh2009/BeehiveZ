package cn.edu.thss.iise.beehivez.server.metric.tager.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAbstr;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmAStar;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmAllOptions;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmGreedy;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.led.LabelEditDistance;

public class TagerAccuracyVSThreeAlgorithms {

	// public static final String ROOT_FOLDER =
	// "C:\\Users\\Shudi\\Desktop\\tager\\";
	public static final String ROOT_FOLDER = "E:\\wangshuhao\\Documents\\Tager\\";
	public static Integer FINISH = 0;
	public static Integer TOTAL = 0;
	public static final int THREADS = 25;

	public static void main(String[] args) throws Exception {
		TagerAccuracyVSThreeAlgorithms ta = new TagerAccuracyVSThreeAlgorithms();
		ta.computeAccuracyBatch();
		System.exit(0);
	}

	public void computeAccuracyBatch() throws Exception {
		String modelPath = ROOT_FOLDER + "72个性质模型\\";
		List<TLabeledGraph> tGraphs = loadModels(modelPath);
		BufferedWriter csvWriter = new BufferedWriter(new FileWriter(
				ROOT_FOLDER + "72个性质模型（除并行63个）_150423a_Tager三种算法对比.csv"));

		int diff1 = 0, diff2 = 0;
		csvWriter.write(",AllOptions,AStar,Greedy");
		csvWriter.newLine();
		TOTAL = tGraphs.size() * (tGraphs.size() + 1) / 2;
		FINISH = 0;

		List<Pair> pairs = new ArrayList<Pair>();
		for (int i = 0; i < tGraphs.size(); ++i) {
			for (int j = i; j < tGraphs.size(); ++j) {
				pairs.add(new Pair(i, j));
			}
		}
		ExecutorService service = Executors.newFixedThreadPool(THREADS);
		List<ComputationCallable> callables = new ArrayList<ComputationCallable>();
		int partition = pairs.size() / THREADS;
		for (int i = 0; i < THREADS; ++i) {
			if (i != THREADS - 1) {
				callables.add(new ComputationCallable(i + 1, tGraphs, pairs
						.subList(i * partition, (i + 1) * partition)));
			} else {
				callables.add(new ComputationCallable(i + 1, tGraphs, pairs
						.subList(i * partition, pairs.size())));
			}
		}
		List<Future<List<Result>>> futures = new ArrayList<Future<List<Result>>>();
		for (int i = 0; i < THREADS; ++i) {
			futures.add(service.submit(callables.get(i)));
		}
		List<List<Result>> results = new ArrayList<List<Result>>();
		for (int i = 0; i < THREADS; ++i) {
			results.add(futures.get(i).get());
		}
		double timeAllOptions = 0.0, timeAStar = 0.0, timeGreedy = 0.0;
		int totalResults = 0;
		for (List<Result> result : results) {
			totalResults += result.size();
			for (Result r : result) {
				timeAllOptions += r.timeAllOptions;
				timeAStar += r.timeAStar;
				timeGreedy += r.timeGreedy;
				csvWriter.write(r.pName + "& " + r.qName + ","
						+ r.simAllOptions + "," + r.simAStar + ","
						+ r.simGreedy);
				csvWriter.newLine();
				if (r.simAllOptions != r.simAStar) {
					++diff1;
				}
				if (r.simAllOptions != r.simGreedy) {
					++diff2;
				}
			}
		}
		csvWriter.close();
		System.out.println("AllOptions与AStar不同: " + diff1);
		System.out.println("AllOptions与Greedy不同: " + diff2);
		System.out.println("AllOptions平均用时: " + timeAllOptions / totalResults);
		System.out.println("AStar平均用时: " + timeAStar / totalResults);
		System.out.println("Greedy平均用时: " + timeGreedy / totalResults);
	}

	private List<TLabeledGraph> loadModels(String path) throws Exception {
		File folder = new File(path);
		File[] files = folder.listFiles();
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

}

class ComputationCallable implements Callable<List<Result>> {

	private int id = 0;
	private List<TLabeledGraph> tGraphs = new ArrayList<TLabeledGraph>();
	private List<Pair> pairs = new ArrayList<Pair>();

	ComputationCallable(int i, List<TLabeledGraph> t, List<Pair> p) {
		id = i;
		tGraphs = t;
		pairs = p;
	}

	@Override
	public List<Result> call() throws Exception {
		Object[] weights = new Object[10];
		weights[0] = "vweight";
		weights[1] = TagerCGSimilarity.WEIGHT_SKIPPED_VERTEX;
		weights[2] = "sweight";
		weights[3] = TagerCGSimilarity.WEIGHT_SUBSTITUTED_VERTEX;
		weights[4] = "eweight";
		weights[5] = TagerCGSimilarity.WEIGHT_SKIPPED_EDGE;
		weights[6] = "simcutoff";
		weights[7] = TagerCGSimilarity.SIM_CUTOFF;
		weights[8] = "waiting";
		weights[9] = 60000.0;
		LabelEditDistance.scaleWeight = TagerCGSimilarity.SCALE_WEIGHT;
		GEDAbstr gedAllOptions = new GEDAlgorithmAllOptions();
		gedAllOptions.setWeight(weights);
		GEDAbstr gedAStar = new GEDAlgorithmAStar();
		gedAStar.setWeight(weights);
		GEDAbstr gedGreedy = new GEDAlgorithmGreedy();
		gedGreedy.setWeight(weights);

		int finish = 0;
		List<Result> result = new ArrayList<Result>();
		for (Pair pair : pairs) {
			TLabeledGraph p = tGraphs.get(pair.v1);
			TLabeledGraph q = tGraphs.get(pair.v2);
			System.out.println("THREAD " + id + " " + (++finish) + "/"
					+ pairs.size() + ": " + p.getName() + " & " + q.getName());
			double timeAllOptions = 0.0, timeAStar = 0.0, timeGreedy = 0.0;
			long start = System.currentTimeMillis();
			float simAllOptions = TagerCGSimilarity.similairity(gedAllOptions,
					p, q, TagerCGSimilarity.WEIGHT_SKIPPED_EDGE,
					TagerCGSimilarity.WEIGHT_SKIPPED_VERTEX);
			timeAllOptions = System.currentTimeMillis() - start;
			start = System.currentTimeMillis();
			float simAStar = TagerCGSimilarity.similairity(gedAStar, p, q,
					TagerCGSimilarity.WEIGHT_SKIPPED_EDGE,
					TagerCGSimilarity.WEIGHT_SKIPPED_VERTEX);
			timeAStar = System.currentTimeMillis() - start;
			start = System.currentTimeMillis();
			float simGreedy = TagerCGSimilarity.similairity(gedGreedy, p, q,
					TagerCGSimilarity.WEIGHT_SKIPPED_EDGE,
					TagerCGSimilarity.WEIGHT_SKIPPED_VERTEX);
			timeGreedy = System.currentTimeMillis() - start;
			result.add(new Result(p.getName(), q.getName(), simAllOptions,
					simAStar, simGreedy, timeAllOptions, timeAStar, timeGreedy));
		}
		return result;
	}

}

class Pair {
	int v1;
	int v2;

	public Pair(int i, int j) {
		v1 = i;
		v2 = j;
	}

	public String toString() {
		return v1 + " & " + v2;
	}
}

class Result {
	String pName;
	String qName;
	float simAllOptions;
	float simAStar;
	float simGreedy;
	double timeAllOptions;
	double timeAStar;
	double timeGreedy;

	public Result(String p, String q, float sim1, float sim2, float sim3,
			double time1, double time2, double time3) {
		pName = p;
		qName = q;
		simAllOptions = sim1;
		simAStar = sim2;
		simGreedy = sim3;
		timeAllOptions = time1;
		timeAStar = time2;
		timeGreedy = time3;
	}
}
