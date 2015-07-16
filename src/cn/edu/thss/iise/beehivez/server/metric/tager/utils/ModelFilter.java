package cn.edu.thss.iise.beehivez.server.metric.tager.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.processmining.framework.models.ModelGraphVertex;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Place;
import org.processmining.framework.models.petrinet.Transition;
import org.processmining.framework.models.petrinet.algorithms.PnmlWriter;
import org.processmining.importing.pnml.PnmlImport;

public class ModelFilter {
	
	public static final String ROOT_FOLDER = "C:\\Users\\Shudi\\Desktop\\tager\\";
	public static final String TARGET_FOLDER = "C:\\Users\\Shudi\\Desktop\\tager\\Enterprise\\";

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		FileUtils.deleteFolder(TARGET_FOLDER);
		FileUtils.createFolder(TARGET_FOLDER);
		ModelFilter filter = new ModelFilter();
		filter.filt("DG");
		filter.filt("TC");
		filter.filt("SAP");
	}
	
	public void filt(String dataset) throws Exception {
		String target = TARGET_FOLDER + dataset + "\\";
		FileUtils.deleteFolder(target);
		FileUtils.createFolder(target);
		PnmlImport pnmlImport = new PnmlImport();
		File folder = new File(ROOT_FOLDER + dataset);
		File[] files = folder.listFiles();
		for(File file : files) {
			FileInputStream input = new FileInputStream(file);
			System.out.println(file.getAbsolutePath());
			PetriNet pn = pnmlImport.read(input);
			input.close();
			// filter
			if(!checkModel(pn)) {
				continue;
			}
//			for(Transition t : pn.getTransitions()) {
//				if(t.getIdentifier().length() <= 4 && 
//						(t.getIdentifier().startsWith("T") || t.getIdentifier().startsWith("t"))) {
//					t.setLogEvent(null);
//				} else {
//					t.setLogEvent(new LogEvent(t.getIdentifier(), "unknown:normal"));
//				}
//			}
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(
					new FileOutputStream(target + dataset + "-" + file.getName())));
			PnmlWriter.write(false, true, pn, writer);
			writer.close();
		}
	}
	
	public boolean checkModel(PetriNet pn) {
		int source = 0, sink = 0;
		for(ModelGraphVertex v : pn.getVerticeList()) {
			if(v instanceof Transition) {
				if(v.getPredecessors().isEmpty() || v.getSuccessors().isEmpty()) {
					return false;
				}
				Transition t = (Transition) v;
				if(t.getIdentifier().length() > 4 && t.isInvisibleTask()) {
					return false;
				}
			} else if(v instanceof Place) {
				if(v.getPredecessors().isEmpty()) {
					++source;
				}
				if(v.getSuccessors().isEmpty()) {
					++sink;
				}
				if(v.getPredecessors().isEmpty() && v.getSuccessors().isEmpty()) {
					return false;
				}
			}
		}
		if(source == 1 && sink == 1) {
			return true;
		}
		return false;
	}

}
