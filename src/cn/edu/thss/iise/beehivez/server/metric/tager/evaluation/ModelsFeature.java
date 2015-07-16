package cn.edu.thss.iise.beehivez.server.metric.tager.evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

public class ModelsFeature {

	public static final String ROOT_FOLDER = "C:\\Users\\Shudi\\Desktop\\tager\\72个性质模型\\";

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		ModelsFeature mf = new ModelsFeature();
		mf.feature();
	}

	public void feature() throws Exception {
		String path = ROOT_FOLDER;
		List<PetriNet> models = loadFiles(path);
		int totalTransition = 0, totalPlace = 0, totalEdge = 0;
		int minTransition = Integer.MAX_VALUE, minPlace = Integer.MAX_VALUE, minEdge = Integer.MAX_VALUE;
		int maxTransition = Integer.MIN_VALUE, maxPlace = Integer.MIN_VALUE, maxEdge = Integer.MIN_VALUE;
		for(PetriNet pn : models) {
			totalTransition += pn.getTransitions().size();
			totalPlace += pn.getPlaces().size();
			totalEdge += pn.getEdges().size();
			minTransition = Math.min(minTransition, pn.getTransitions().size());
			minPlace = Math.min(minPlace, pn.getPlaces().size());
			minEdge = Math.min(minEdge, pn.getEdges().size());
			maxTransition = Math.max(maxTransition, pn.getTransitions().size());
			maxPlace = Math.max(maxPlace, pn.getPlaces().size());
			maxEdge = Math.max(maxEdge, pn.getEdges().size());
		}
		System.out.println("规模：" + models.size());
		System.out.println("平均变迁数：" + ((double) totalTransition) / models.size());
		System.out.println("平均库所数：" + ((double) totalPlace) / models.size());
		System.out.println("平均边数：" + ((double) totalEdge) / models.size());
		System.out.println("最小变迁数：" + minTransition);
		System.out.println("最小库所数：" + minPlace);
		System.out.println("最小边数：" + minEdge);
		System.out.println("最大变迁数：" + maxTransition);
		System.out.println("最大库所数：" + maxPlace);
		System.out.println("最大边数：" + maxEdge);
	}

	private List<PetriNet> loadFiles(String path) throws Exception {
		File folder = new File(path);
		List<PetriNet> models = new ArrayList<PetriNet>();
		PnmlImport pnmlImport = new PnmlImport();
		File[] files = folder.listFiles();
		for (int i = 0; i < files.length; ++i) {
			FileInputStream input = new FileInputStream(files[i]);
			PetriNet pn = pnmlImport.read(input);
			pn.setName(files[i].getName());
			models.add(pn);
			input.close();
		}
		return models;
	}

}
