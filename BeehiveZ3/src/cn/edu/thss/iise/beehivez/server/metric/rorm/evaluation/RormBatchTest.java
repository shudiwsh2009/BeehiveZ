package cn.edu.thss.iise.beehivez.server.metric.rorm.evaluation;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.rorm.RormSimilarity;

public class RormBatchTest {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		RormBatchTest batch = new RormBatchTest();
		batch.timeBatch();
	}
	
	public void timeBatch() throws Exception {
		String path = "C:\\Users\\Shudi\\Desktop\\rorm\\test\\";
		List<PetriNet> models = loadModels(path);
		int min = 0, max = models.size() - 1;
		long total = 0;
		Random random = new Random();
		RormSimilarity measure = new RormSimilarity();
		for(int i = 0; i < 1000; ++i) {
			int a = random.nextInt(max) % (max - min + 1) + min;
			int b = random.nextInt(max) % (max - min + 1) + min;
			long start = System.currentTimeMillis();
			System.out.println(i + ": " + models.get(a).getName() 
					+ " & " + models.get(b).getName() + " " 
					+ measure.similarity(models.get(a), models.get(b)));
			long end = System.currentTimeMillis();
			total += (end - start);
		}
		System.out.println("Total time used: " + total + "ms");
		System.out.println("Average time: " + ((float)total) / 1000 + "ms");
	}
	
	private List<PetriNet> loadModels(String path) throws Exception {
		List<PetriNet> models = new ArrayList<PetriNet>();
		PnmlImport pnmlImport = new PnmlImport();
		File folder = new File(path);
		File[] files = folder.listFiles();
		for(File file : files) {
			FileInputStream input = new FileInputStream(file);
			PetriNet pn = pnmlImport.read(input);
			pn.setName(file.getName());
			models.add(pn);
			input.close();
		}
		return models;
	}

}
