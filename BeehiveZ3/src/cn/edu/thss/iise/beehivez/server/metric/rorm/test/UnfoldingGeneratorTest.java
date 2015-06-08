package cn.edu.thss.iise.beehivez.server.metric.rorm.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.processmining.exporting.DotPngExport;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.plugin.ProvidedObject;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.rorm.conversion.JbptConversion;

public class UnfoldingGeneratorTest {

	public static void jbptTest() throws Exception {
		String filePath1 = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\multi_relation.pnml";
		String filePath2 = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\multi_relation.png";
		// String filePath3 =
		// "C:\\Users\\Shudi\\Desktop\\rorm\\test\\multi_relation_1_cfp.pnml";
		String filePath4 = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\multi_relation_cfp.png";

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

		NetSystem ns = JbptConversion.convert(p1);
		CompletePrefixUnfolding cpu = new CompletePrefixUnfolding(ns);

		PetriNet p2 = JbptConversion.convert(cpu);

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
