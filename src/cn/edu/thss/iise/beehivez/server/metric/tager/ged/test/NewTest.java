package cn.edu.thss.iise.beehivez.server.metric.tager.ged.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAbstr;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.algorithm.GEDAlgorithmGreedy;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph.TLabeledGraph;
import cn.edu.thss.iise.beehivez.server.metric.tager.ged.led.LabelEditDistance;

public class NewTest {

	public static double EPSILON = 0.00001;
	public static double WEIGHT_SKIPPED_VERTEX = 1.0;
	public static double WEIGHT_SUBSTITUTED_VERTEX = 2.0;
	public static double WEIGHT_SKIPPED_EDGE = 0.5;
	public static double SIM_CUTOFF = 0.0;
	public static double SCALE_WEIGHT = 2.0;

	public static void main(String[] args) {
		// TODO Auto-generated method stub
		try {
			File file1 = new File(
					"C:\\Users\\Shudi\\Desktop\\tager\\test\\M0.pnml");
			FileInputStream fInput1 = new FileInputStream(file1);
			File file2 = new File(
					"C:\\Users\\Shudi\\Desktop\\tager\\test\\M4.pnml");
			FileInputStream fInput2 = new FileInputStream(file2);
			PnmlImport pnmlImport = new PnmlImport();
			PetriNet pn1 = pnmlImport.read(fInput1);
			PetriNet pn2 = pnmlImport.read(fInput2);
			pn1.setName(file1.getName());
			pn2.setName(file2.getName());

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

//			GEDAlgorithmAbstr measure = new GEDAlgorithmAStar();
			GEDAbstr measure = new GEDAlgorithmGreedy();
			measure.setWeight(weights);
			System.out.println(measure.compute(tg1, tg2));
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

}
