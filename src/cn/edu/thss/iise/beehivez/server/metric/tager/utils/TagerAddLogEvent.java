package cn.edu.thss.iise.beehivez.server.metric.tager.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStreamWriter;

import org.processmining.framework.log.LogEvent;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Transition;
import org.processmining.framework.models.petrinet.algorithms.PnmlWriter;
import org.processmining.importing.pnml.PnmlImport;

public class TagerAddLogEvent {
	
	public static final String ROOT_FOLDER = "D:\\百度云同步盘\\Learn@Tsinghua\\过程数据组\\Tager\\08.性质符合实验\\";
	public static final String TEST_FOLDER = "D:\\百度云同步盘\\Learn@Tsinghua\\过程数据组\\Tager\\11.17个Test模型\\Models\\";

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
	
	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		TagerAddLogEvent tpc = new TagerAddLogEvent();
		for(String propertyFolder : PROPERTY_FOLDERS) {
			String modelFolder = propertyFolder + "Models\\";
			tpc.modModels(modelFolder);
		}
		tpc.modModels(TEST_FOLDER);
		System.exit(0);
	}
	
	public void modModels(String path) throws Exception {
		PnmlImport pnmlImport = new PnmlImport();
		File folder = new File(path);
		File[] files = folder.listFiles();
		for(File file : files) {
			FileInputStream input = new FileInputStream(file);
			System.out.println(file.getAbsolutePath());
			PetriNet pn = pnmlImport.read(input);
			input.close();
			pn.setName(file.getName());
			for(Transition t : pn.getTransitions()) {
				t.setLogEvent(new LogEvent(t.getIdentifier(), "auto"));
			}
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)));
			PnmlWriter.write(false, true, pn, writer);
			writer.close();
		}
	}

}
