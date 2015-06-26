package cn.edu.thss.iise.beehivez.server.metric.rorm.test;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Place;
import org.jbpt.petri.io.PNMLSerializer;
import org.jbpt.petri.unfolding.AbstractCondition;
import org.jbpt.petri.unfolding.AbstractEvent;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.CompletePrefixUnfoldingSetup;
import org.jbpt.petri.unfolding.order.AdequateOrderType;
import org.processmining.exporting.DotPngExport;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.plugin.ProvidedObject;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.rorm.dependency.JbptConversion;
import cn.edu.thss.iise.beehivez.server.util.PetriNetUtil;

public class UnfoldingGeneratorTest {

	public static void singlePng(String filepath) throws Exception {
		String filePath1 = filepath;
		int dotIndex = filePath1.lastIndexOf('.');
		String filePath2 = filePath1.substring(0, dotIndex) + ".png";
		String filepath3 = filePath1.substring(0, dotIndex) + "_cfp.png";
		String filepath4 = filePath1.substring(0, dotIndex) + "_cfp.pnml";

		PnmlImport pnmlImport = new PnmlImport();
		PetriNet p1 = pnmlImport.read(new FileInputStream(new File(filePath1)));

		// ori-png
		ProvidedObject po1 = new ProvidedObject("petrinet", p1);
		DotPngExport dpe1 = new DotPngExport();
		OutputStream image1 = new FileOutputStream(filePath2);
		dpe1.export(po1, image1);

		PNMLSerializer pnmlSerializer = new PNMLSerializer();
		NetSystem net = pnmlSerializer.parse(filepath);
		net.getNodes().forEach(n -> n.setName(n.getLabel()));
		net.getTransitions().stream().filter(t -> t.getLabel().startsWith("inv_")).forEach(t -> t.setLabel(""));
		preProcess(net);
		AbstractCondition.count = 0;
		AbstractEvent.count = 0;
		CompletePrefixUnfoldingSetup setup = new CompletePrefixUnfoldingSetup();
		setup.ADEQUATE_ORDER = AdequateOrderType.ESPARZA_FOR_SAFE_SYSTEMS;
		setup.SAFE_OPTIMIZATION = true;
		CompletePrefixUnfolding cpu = new CompletePrefixUnfolding(net, setup);

		// cfp
		PetriNet p2 = JbptConversion.convert(cpu);
		PetriNetUtil.export2pnml(p2, filepath4);

		// cfp-png
		ProvidedObject po2 = new ProvidedObject("petrinet", p2);
		DotPngExport dpe2 = new DotPngExport();
		OutputStream image2 = new FileOutputStream(filepath3);
		dpe2.export(po2, image2);
	}

	public static void preProcess(NetSystem net) {
		for (Place p : net.getPlaces()) {
			if (net.getIncomingEdges(p).isEmpty()) {
				net.getMarking().put(p, 1);
			}
		}
	}

	public static void batchPng(String folderPath) throws Exception {
		File folder = new File(folderPath);
		File[] files = folder.listFiles();
		for (File f : files) {
			singlePng(f.getAbsolutePath());
		}
	}

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		singlePng("C:\\Users\\Shudi\\Desktop\\rorm\\test\\DMKD07_M5.pnml");
		// batchPng("C:\\Users\\Shudi\\Desktop\\rorm\\test\\");
	}

}
