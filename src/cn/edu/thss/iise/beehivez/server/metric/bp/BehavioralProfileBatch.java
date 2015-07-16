package cn.edu.thss.iise.beehivez.server.metric.bp;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

public class BehavioralProfileBatch {
	
	private List<PetriNet> models;
	
	public BehavioralProfileBatch() throws Exception {
		loadFile("C:\\Users\\Shudi\\Desktop\\tager\\test\\");
	}
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		BehavioralProfileBatch batch = new BehavioralProfileBatch();
		batch.compute(0, 1);
		batch.compute(0, 2);
		batch.compute(0, 3);
		batch.compute(0, 4);
		batch.compute(0, 5);
		batch.compute(0, 6);
		batch.compute(0, 7);
		batch.compute(0, 8);
		batch.compute(3, 9);
		batch.compute(3, 10);
		batch.compute(3, 11);
		batch.compute(3, 12);
		batch.compute(3, 13);
		batch.compute(3, 14);
		batch.compute(3, 15);
		batch.compute(3, 16);
	}
	
	public void compute(int i, int j) {
		cn.edu.thss.iise.beehivez.server.metric.BehavioralProfileSimilarity bpOld 
			= new cn.edu.thss.iise.beehivez.server.metric.BehavioralProfileSimilarity();
		cn.edu.thss.iise.beehivez.server.metric.bp.BehavioralProfileSimilarity bpNew 
			= new cn.edu.thss.iise.beehivez.server.metric.bp.BehavioralProfileSimilarity();
		float oldSim = bpOld.similarity(models.get(i), models.get(j));
		float newSim = bpNew.similarity(models.get(i), models.get(j));
		System.out.println(models.get(i).getName() + " & " + models.get(j).getName() + "  "
				+ oldSim + " " + newSim);
	}
	
	public void loadFile(String path) throws Exception {
		models = new ArrayList<PetriNet>();
		for(int i = 0; i <= 16; ++i) {
			File file = new File(path + "M" + i + ".pnml");
			FileInputStream fInput = new FileInputStream(file);
			PnmlImport pnmlImport = new PnmlImport();
			PetriNet pn = pnmlImport.read(fInput);
			pn.setName(file.getName());
			models.add(pn);
		}
	}

}
