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

public class TagerPropertyComplianceBatch {
	
	public static final String ROOT_FOLDER = "D:\\百度云同步盘\\Learn@Tsinghua\\过程数据组\\Graduate\\07.实验数据\\02.性质符合实验\\";
//	public static final String ROOT_FOLDER = "E:\\wangshuhao\\Documents\\ExRORM\\Property\\";
	public static final String REVISION = "_150722a_sda0.0_weight";

	public static final String[] PROPERTY_FOLDERS = {
			ROOT_FOLDER + "01.顺序结构漂移不变性\\",
			ROOT_FOLDER + "02.跨度负相关性\\",
			ROOT_FOLDER + "03.无关任务递减性\\",
			ROOT_FOLDER + "04.循环长度负相关性\\",
			ROOT_FOLDER + "05.互斥结构漂移不变性\\",
			ROOT_FOLDER + "06.循环结构漂移不变性\\",
			ROOT_FOLDER + "07.并发结构漂移不变性\\",
			ROOT_FOLDER + "08.不平衡性\\"
	};
	
	public static final String[] SIM_MEASURE = {
			"TAR", "PTS", "CFS"
//			"TAR", "PTS", "SSDT", "BP", "CF", "CFS", "TAGER"
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
	
	public static RormSimilarity rormMeasure = new RormSimilarity();
	public static BehavioralProfileSimilarity bpMeasure = new BehavioralProfileSimilarity();
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		RefinedOrderingRelation.SDA_WEIGHT = 0.0;
		RefinedOrderingRelation.IMPORTANCE = true;
		TagerPropertyComplianceBatch tpc = new TagerPropertyComplianceBatch();
		for(String propertyFolder : PROPERTY_FOLDERS) {
			String property = propertyFolder.substring(propertyFolder.lastIndexOf('.') + 1
					, propertyFolder.length() - 1);
			tpc.compute(propertyFolder, property);
		}
		System.exit(0);
	}
	
	public void compute(String propertyFolder, String property) throws Exception {
		System.out.println(propertyFolder);
		String modelFolder = propertyFolder + "Models\\";
		PetriNet originalModel = loadModel(modelFolder + "N1.pnml");
		List<PetriNet> compareModels = loadModels(modelFolder);
		NetSystem originalNet = loadNet(modelFolder + "N1.pnml");
		List<NetSystem> compareNets = loadNets(modelFolder);
		BufferedWriter writer = new BufferedWriter(new FileWriter(propertyFolder 
				+ "similarity_" + property + REVISION + ".csv"));
		StringBuilder builder = new StringBuilder();
		for(String s : SIM_MEASURE) {
			builder.append(",");
			builder.append(s);
		}
		builder.append(",");
		builder.append("BP");
		builder.append(",");
		builder.append("ExRORU");
		builder.append("\r\n");
		for(int i = 0; i < compareModels.size(); ++i) {
			PetriNet pn = compareModels.get(i);
			builder.append(pn.getName());
			for(String measure : SIM_MEASURE) {
				System.out.println(measure + " : " + originalModel.getName() + " & " + pn.getName());
				float result = hmMeasure.get(measure).similarity(originalModel, pn);
				BigDecimal sim = new BigDecimal(result);
				sim = sim.setScale(3, BigDecimal.ROUND_HALF_UP);
				builder.append(",");
				builder.append(sim.toString());
			}
			NetSystem net = compareNets.get(i);
			System.out.println("BP : " + originalNet.getName() + " & " + net.getName());
			float result = bpMeasure.similarity(originalNet, net);
			BigDecimal sim = new BigDecimal(result);
			sim = sim.setScale(3, BigDecimal.ROUND_HALF_UP);
			builder.append(",");
			builder.append(sim.toString());
			
			System.out.println("ExRORU : " + originalNet.getName() + " & " + net.getName());
			result = rormMeasure.similarity(originalNet, net);
			sim = new BigDecimal(result);
			sim = sim.setScale(3, BigDecimal.ROUND_HALF_UP);
			builder.append(",");
			builder.append(sim.toString());
			builder.append("\r\n");
		}
		writer.write(builder.toString());
		writer.close();
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
		for(int i = 1; ; ++i) {
			File file = new File(path + "N" + i + ".pnml");
			if(!file.exists()) {
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
	
	private NetSystem loadNet(String path) throws Exception {
		PNMLSerializer pnmlSerializer = new PNMLSerializer();
		File file = new File(path);
		NetSystem net = pnmlSerializer.parse(path);
		net.setName(file.getName());
		return net;
	}
	
	private List<NetSystem> loadNets(String path) throws Exception {
		List<NetSystem> nets = new ArrayList<>();
		PNMLSerializer pnmlSerializer = new PNMLSerializer();
		for(int i = 1; ; ++i) {
			File file = new File(path + "N" + i + ".pnml");
			if(!file.exists()) {
				break;
			}
			NetSystem net = pnmlSerializer.parse(file.getAbsolutePath());
			net.setName(file.getName());
			nets.add(net);
		}
		return nets;
	}

}
