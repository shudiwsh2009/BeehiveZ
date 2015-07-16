package cn.edu.thss.iise.beehivez.server.metric.tager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.math.BigDecimal;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAbstr;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmAStar;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmAllOptions;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmGreedy;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.Edge;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.led.LabelEditDistance;

public class TagerCGSimilarityAllOptions extends PetriNetSimilarity {

	public static double EPSILON = 0.00001;
	public static double WEIGHT_SKIPPED_VERTEX = 1.0;
	public static double WEIGHT_SUBSTITUTED_VERTEX = 2.0;
	public static double WEIGHT_SKIPPED_EDGE = 0.5;
	public static double SIM_CUTOFF = 0.0;
	public static double SCALE_WEIGHT = 2.0;

	@Override
	public float similarity(PetriNet pn1, PetriNet pn2) {
		// TODO Auto-generated method stub
		return similarity((PetriNet) pn1.clone(), (PetriNet) pn2.clone(), 1);
	}

	private float similarity(PetriNet pn1, PetriNet pn2, int internal) {
		// TODO Auto-generated method stub
		TLabeledGraph tg1 = new TLabeledGraph(pn1);
		TLabeledGraph tg2 = new TLabeledGraph(pn2);

		Object[] weights = new Object[10];
		weights[0] = "vweight";
		weights[1] = WEIGHT_SKIPPED_VERTEX;
		weights[2] = "sweight";
		weights[3] = WEIGHT_SUBSTITUTED_VERTEX;
		weights[4] = "eweight";
		weights[5] = WEIGHT_SKIPPED_EDGE;
		weights[6] = "simcutoff";
		weights[7] = SIM_CUTOFF;
		weights[8] = "waiting";
		weights[9] = 60000.0;
		LabelEditDistance.scaleWeight = SCALE_WEIGHT;

		 GEDAbstr measure = new GEDAlgorithmAllOptions();
//		GEDAbstr measure = new GEDAlgorithmAStar();
//		 GEDAbstr measure = new GEDAlgorithmGreedy();
		measure.setWeight(weights);
		float sim = TagerCGSimilarityAllOptions.similairity(measure, tg1, tg2,
				WEIGHT_SKIPPED_EDGE, WEIGHT_SKIPPED_VERTEX);
		if (Math.abs(sim - (-1)) < EPSILON) {
			measure = new GEDAlgorithmGreedy();
			measure.setWeight(weights);
			sim = TagerCGSimilarityAllOptions.similairity(measure, tg1, tg2,
					WEIGHT_SKIPPED_EDGE, WEIGHT_SKIPPED_VERTEX);
		}
		return sim;
	}

	public static float similairity(TLabeledGraph tg1, TLabeledGraph tg2) {
		GEDAbstr measure = new GEDAlgorithmAStar();
		Object[] weights = new Object[10];
		weights[0] = "vweight";
		weights[1] = WEIGHT_SKIPPED_VERTEX;
		weights[2] = "sweight";
		weights[3] = WEIGHT_SUBSTITUTED_VERTEX;
		weights[4] = "eweight";
		weights[5] = WEIGHT_SKIPPED_EDGE;
		weights[6] = "simcutoff";
		weights[7] = SIM_CUTOFF;
		weights[8] = "waiting";
		weights[9] = 0.0;
		LabelEditDistance.scaleWeight = SCALE_WEIGHT;
		measure.setWeight(weights);
		return TagerCGSimilarityAllOptions.similairity(measure, tg1, tg2,
				WEIGHT_SKIPPED_EDGE, WEIGHT_SKIPPED_VERTEX);
	}

	public static float similairity(GEDAbstr measure, TLabeledGraph tg1,
			TLabeledGraph tg2, double weightSkippedEdge,
			double weightSkippedVertex) {
		double distance = measure.compute(tg1, tg2);
		System.out.println(distance);
		if (Math.abs(distance - (-1)) < EPSILON) {
			return -1;
		}
		int edge1 = 0, edge2 = 0;
		for (Edge e1 : tg1.getEdges()) {
			edge1 += (Math.max(1,
					Math.max(e1.getConflictSpan(), e1.getLoopSpan())));
		}
		for (Edge e2 : tg2.getEdges()) {
			edge2 += (Math.max(1,
					Math.max(e2.getConflictSpan(), e2.getLoopSpan())));
		}
		double edgeWeight = (edge1 + edge2) * weightSkippedEdge;
		int numOfVertex = tg1.getVertices().size() + tg2.getVertices().size();
		double vertexWeight = (numOfVertex - 4) * weightSkippedVertex;
		double similarity = (edgeWeight + vertexWeight - distance)
				/ (edgeWeight + vertexWeight);
		BigDecimal sim = new BigDecimal(similarity);
		sim = sim.setScale(3, BigDecimal.ROUND_HALF_UP);
		return sim.floatValue();
	}

	@Override
	public String getName() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getDesription() {
		// TODO Auto-generated method stub
		return null;
	}

	public static void singleExperiment() {
		try {
			File file1 = new File(
					"C:\\Users\\Shudi\\Desktop\\tager\\test\\N1.pnml");
			FileInputStream fInput1 = new FileInputStream(file1);
			File file2 = new File(
					"C:\\Users\\Shudi\\Desktop\\tager\\test\\N2.pnml");
			FileInputStream fInput2 = new FileInputStream(file2);
			PnmlImport pnmlImport = new PnmlImport();
			PetriNet pn1 = pnmlImport.read(fInput1);
			PetriNet pn2 = pnmlImport.read(fInput2);

			pn1.setName(file1.getName());
			pn2.setName(file2.getName());

			Object[] weights = new Object[10];
			weights[0] = "vweight";
			weights[1] = TagerCGSimilarityAllOptions.WEIGHT_SKIPPED_VERTEX;
			weights[2] = "sweight";
			weights[3] = TagerCGSimilarityAllOptions.WEIGHT_SUBSTITUTED_VERTEX;
			weights[4] = "eweight";
			weights[5] = TagerCGSimilarityAllOptions.WEIGHT_SKIPPED_EDGE;
			weights[6] = "simcutoff";
			weights[7] = TagerCGSimilarityAllOptions.SIM_CUTOFF;
			weights[8] = "waiting";
			weights[9] = 60000.0;
			LabelEditDistance.scaleWeight = TagerCGSimilarityAllOptions.SCALE_WEIGHT;
			GEDAbstr gedAllOptions = new GEDAlgorithmAllOptions();
			gedAllOptions.setWeight(weights);
			GEDAbstr gedAStar = new GEDAlgorithmAStar();
			gedAStar.setWeight(weights);
			GEDAbstr gedGreedy = new GEDAlgorithmGreedy();
			gedGreedy.setWeight(weights);

			TLabeledGraph p = new TLabeledGraph(pn1);
			TLabeledGraph q = new TLabeledGraph(pn2);
//			System.out.println("AllOptions: "
//					+ TagerCGSimilarity.similairity(gedAllOptions, p, q,
//							TagerCGSimilarity.WEIGHT_SKIPPED_EDGE,
//							TagerCGSimilarity.WEIGHT_SKIPPED_VERTEX));
			System.out.println("AStar: "
					+ TagerCGSimilarityAllOptions.similairity(gedAStar, p, q,
							TagerCGSimilarityAllOptions.WEIGHT_SKIPPED_EDGE,
							TagerCGSimilarityAllOptions.WEIGHT_SKIPPED_VERTEX));
//			System.out.println("Greedy: "
//					+ TagerCGSimilarity.similairity(gedGreedy, p, q,
//							TagerCGSimilarity.WEIGHT_SKIPPED_EDGE,
//							TagerCGSimilarity.WEIGHT_SKIPPED_VERTEX));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) {
		singleExperiment();
	}

}
