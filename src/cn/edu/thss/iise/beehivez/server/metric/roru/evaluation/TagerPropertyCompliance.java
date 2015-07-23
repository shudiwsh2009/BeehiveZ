package cn.edu.thss.iise.beehivez.server.metric.roru.evaluation;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.io.PNMLSerializer;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import com.iise.shudi.bp.BehavioralProfileSimilarity;
import com.iise.shudi.exroru.RefinedOrderingRelation;
import com.iise.shudi.exroru.RormSimilarity;

import cn.edu.thss.iise.beehivez.server.metric.BTSSimilarity_Wang;
import cn.edu.thss.iise.beehivez.server.metric.CausalFootprintSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.JaccardTARSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.cfs.CFSSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.ssdt.SSDTSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.tager.TagerCGSimilarity;

public class TagerPropertyCompliance {
	
	public static final String[] SIM_MEASURE = {
		"TAR", "PTS", "SSDT", "CF", "CFS", "TAGER"
	};
	
	public static Map<String, PetriNetSimilarity> hmMeasure = new HashMap<String, PetriNetSimilarity>();
	static {
		hmMeasure.put("TAR", new JaccardTARSimilarity());
		hmMeasure.put("PTS", new BTSSimilarity_Wang());
		hmMeasure.put("SSDT", new SSDTSimilarity());
		hmMeasure.put("CF", new CausalFootprintSimilarity());
		hmMeasure.put("CFS", new CFSSimilarity());
		hmMeasure.put("TAGER", new TagerCGSimilarity());
	}

	public List<PetriNet> models;
	public List<NetSystem> nets;
	public BufferedWriter writer;
	
	public TagerPropertyCompliance() throws Exception {
		loadFile("C:\\Users\\Shudi\\Desktop\\rorm\\17个模型\\");
		writer = new BufferedWriter(new FileWriter("C:\\Users\\Shudi\\Desktop\\rorm\\17个模型_150720a_sda0.0.csv"));
		for(String measure : SIM_MEASURE) {
			writer.write("," + measure);
		}
		writer.write(",BP,ExRORU");
		writer.newLine();
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		RefinedOrderingRelation.SDA_WEIGHT = 0.0;
		RefinedOrderingRelation.IMPORTANCE = true;
		TagerPropertyCompliance compliance = new TagerPropertyCompliance();
		compliance.compute(0, 1);
		compliance.compute(0, 2);
		compliance.compute(0, 3);
		compliance.compute(0, 4);
		compliance.compute(0, 5);
		compliance.compute(0, 6);
		compliance.compute(0, 7);
		compliance.compute(0, 8);
		compliance.compute(3, 9);
		compliance.compute(3, 10);
		compliance.compute(3, 11);
		compliance.compute(3, 12);
		compliance.compute(3, 13);
		compliance.compute(3, 14);
		compliance.compute(3, 15);
		compliance.compute(3, 16);
		compliance.writer.close();
		System.exit(0);
	}
	
	public void compute(int i, int j) throws Exception {
		PetriNet pn1 = models.get(i);
		PetriNet pn2 = models.get(j);
		writer.write(pn1.getName() + " & " + pn2.getName());
		for(String measure : SIM_MEASURE) {
			float result = hmMeasure.get(measure).similarity(pn1, pn2);
			BigDecimal sim = new BigDecimal(result);
			sim = sim.setScale(3, BigDecimal.ROUND_HALF_UP);
			writer.write("," + sim.toString());
		}
		NetSystem net1 = nets.get(i);
		NetSystem net2 = nets.get(j);
		// BP
		BehavioralProfileSimilarity bp = new BehavioralProfileSimilarity();
		float result = bp.similarity(net1, net2);
		BigDecimal sim = new BigDecimal(result);
		sim = sim.setScale(3, BigDecimal.ROUND_HALF_UP);
		writer.write("," + sim.toString());
		// ExRORU
		RormSimilarity rorm = new RormSimilarity();
		result = rorm.similarity(net1, net2);
		sim = new BigDecimal(result);
		sim = sim.setScale(3, BigDecimal.ROUND_HALF_UP);
		writer.write("," + sim.toString());
		
		writer.newLine();
	}
	
	public void loadFile(String path) throws Exception {
		models = new ArrayList<PetriNet>();
		nets = new ArrayList<>();
		for(int i = 0; i <= 16; ++i) {
			String filename = path + "M" + i + ".pnml";
			
			File file = new File(filename);
			FileInputStream fInput = new FileInputStream(file);
			PnmlImport pnmlImport = new PnmlImport();
			PetriNet pn = pnmlImport.read(fInput);
			pn.setName(file.getName());
			models.add(pn);
			
			PNMLSerializer pnmlSerializer = new PNMLSerializer();
			NetSystem net = pnmlSerializer.parse(filename);
			net.setName(file.getName());
			nets.add(net);
		}
	}

}
