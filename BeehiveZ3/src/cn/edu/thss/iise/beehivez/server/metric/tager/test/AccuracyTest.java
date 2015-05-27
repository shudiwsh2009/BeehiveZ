package cn.edu.thss.iise.beehivez.server.metric.tager.test;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class AccuracyTest {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		AccuracyTest at = new AccuracyTest();
		at.accuracy();
	}

	public void accuracy() throws Exception {
		BufferedReader reader = new BufferedReader(
				new FileReader(
						"C:\\Users\\Shudi\\Desktop\\tager\\72个性质模型_150418a_Tager三种算法对比.csv"));
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				"C:\\Users\\Shudi\\Desktop\\tager\\accuracy.csv"));
		String line = reader.readLine();
		writer.write(",AllOptions,AStar,Greedy");
		writer.newLine();
		while((line = reader.readLine()) != null) {
			String[] p = line.split(",");
			Float simAllOptions = Float.parseFloat(p[1]);
			Float simAStar = Float.parseFloat(p[2]);
			Float simGreedy = Float.parseFloat(p[3]);
			if(!simAllOptions.equals(simAStar) || !simAllOptions.equals(simGreedy)) {
				writer.write(line);
				writer.newLine();
			}
		}
		reader.close();
		writer.close();
	}

}
