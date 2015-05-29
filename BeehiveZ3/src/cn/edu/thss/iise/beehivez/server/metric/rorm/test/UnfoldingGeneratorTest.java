package cn.edu.thss.iise.beehivez.server.metric.rorm.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.util.Set;

import org.jbpt.petri.IFlow;
import org.jbpt.petri.IMarking;
import org.jbpt.petri.INode;
import org.jbpt.petri.IPlace;
import org.jbpt.petri.ITransition;
import org.jbpt.petri.NetSystem;
import org.jbpt.petri.unfolding.AbstractBPNode;
import org.jbpt.petri.unfolding.AbstractEvent;
import org.jbpt.petri.unfolding.BPNode;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Condition;
import org.jbpt.petri.unfolding.Event;
import org.jbpt.petri.unfolding.IBPNode;
import org.jbpt.petri.unfolding.ICondition;
import org.jbpt.petri.unfolding.IEvent;
import org.processmining.exporting.DotPngExport;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Place;
import org.processmining.framework.models.petrinet.Token;
import org.processmining.framework.plugin.ProvidedObject;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.rorm.jbpt.conversion.PetriNetConversion;
import cn.edu.thss.iise.beehivez.server.metric.rorm.lc.LeastCommonPredecessorsAndSuccessors;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.CompleteFinitePrefix;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.CompleteFinitePrefixBuilder;
import cn.edu.thss.iise.beehivez.server.util.PetriNetUtil;

public class UnfoldingGeneratorTest {

	public static void singleTest() throws Exception {
		String filePath1 = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\DMKD07-N7.pnml";
		String filePath2 = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\DMKD07-N7.png";
		String filePath3 = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\DMKD07-N7_cfp.pnml";
		String filePath4 = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\DMKD07-N7_cfp.png";

		// String filePath1 =
		// "/Users/shudi/Desktop/parallel_A_with_outer_loop.pnml";
		// String filePath2 =
		// "/Users/shudi/Desktop/parallel_A_with_outer_loop.png";
		// String filePath3 =
		// "/Users/shudi/Desktop/parallel_A_with_outer_loop_cfp.png";

		PnmlImport pnmlImport = new PnmlImport();
		PetriNet p1 = pnmlImport.read(new FileInputStream(new File(filePath1)));

		// ori
		ProvidedObject po1 = new ProvidedObject("petrinet", p1);
		DotPngExport dpe1 = new DotPngExport();
		OutputStream image1 = new FileOutputStream(filePath2);
		dpe1.export(po1, image1);

		for (Place place : p1.getPlaces()) {
			place.removeAllTokens();
			if (place.inDegree() == 0) {
				place.addToken(new Token());
			}
		}
		CompleteFinitePrefix p2 = CompleteFinitePrefixBuilder.build(p1);

		// cfp
		PetriNetUtil.export2pnml(p2, filePath3);
		ProvidedObject po2 = new ProvidedObject("petrinet", p2);
		DotPngExport dpe2 = new DotPngExport();
		OutputStream image2 = new FileOutputStream(filePath4);
		dpe2.export(po2, image2);
	}

	public static void jbptTest() throws Exception {
		String filePath1 = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\M4.pnml";
		String filePath2 = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\M4.png";
		// String filePath3 =
		// "C:\\Users\\Shudi\\Desktop\\rorm\\test\\multi_relation_1_cfp.pnml";
		String filePath4 = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\M4_cfp.png";

		// String filePath1 =
		// "/Users/shudi/Desktop/parallel_A_with_outer_loop.pnml";
		// String filePath2 =
		// "/Users/shudi/Desktop/parallel_A_with_outer_loop.png";
		// String filePath3 =
		// "/Users/shudi/Desktop/parallel_A_with_outer_loop_cfp.png";

		PnmlImport pnmlImport = new PnmlImport();
		PetriNet p1 = pnmlImport.read(new FileInputStream(new File(filePath1)));

		// ori
		ProvidedObject po1 = new ProvidedObject("petrinet", p1);
		DotPngExport dpe1 = new DotPngExport();
		OutputStream image1 = new FileOutputStream(filePath2);
		dpe1.export(po1, image1);

		NetSystem ns = PetriNetConversion.convert(p1);
		CompletePrefixUnfolding cpu = new CompletePrefixUnfolding(ns);

		PetriNet p2 = PetriNetConversion.convert(cpu);

		// cfp
//		 PetriNetUtil.export2pnml(p2, filePath3);
		ProvidedObject po2 = new ProvidedObject("petrinet", p2);
		DotPngExport dpe2 = new DotPngExport();
		OutputStream image2 = new FileOutputStream(filePath4);
		dpe2.export(po2, image2);
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		jbptTest();
	}

}
