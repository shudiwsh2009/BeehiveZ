package cn.edu.thss.iise.beehivez.server.metric.rorm.evaluation.unfolding;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Place;
import org.processmining.framework.models.petrinet.Token;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.rorm.conversion.JbptConversion;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.CompleteFinitePrefix;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.CompleteFinitePrefixBuilder;


public class UnfoldingEvaluation {
	
	public static void evaluation() throws Exception {
		String path = "E:\\Models\\300\\";
		File folder = new File(path);
		File[] files = folder.listFiles();
		PnmlImport pnmlImport = new PnmlImport();
		List<PetriNet> models = new ArrayList<PetriNet>();
		List<NetSystem> nets = new ArrayList<NetSystem>();
		for(File f : files) {
			PetriNet pn = pnmlImport.read(new FileInputStream(f)); 
			models.add(pn);
			nets.add(JbptConversion.convert(pn));
		}
		
		System.out.println("1");
		long totalA = 0L, totalB = 0L;
		int i = 0;
		for(PetriNet pn : models) {
			System.out.println("jintao: " + (++i) + "/" + models.size());
			for (Place place : pn.getPlaces()) {
				place.removeAllTokens();
				if (place.inDegree() == 0) {
					place.addToken(new Token());
				}
			}
			long a = System.currentTimeMillis();
			CompleteFinitePrefix cfp = CompleteFinitePrefixBuilder.build(pn);
			long b = System.currentTimeMillis();
			totalA += (b - a);
		}
		i = 0;
		for(NetSystem net : nets) {
			System.out.println("jbpt: " + (++i) + "/" + nets.size());
			long a = System.currentTimeMillis();
			CompletePrefixUnfolding cpu = new CompletePrefixUnfolding(net);
			long b = System.currentTimeMillis();
			totalB += (b - a);
		}
		System.out.println("jintao: " + (((double)totalA) / models.size()) + "ms");
		System.out.println("jbpt:" + (((double)totalB) / nets.size()) + "ms");
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		evaluation();
	}

}
