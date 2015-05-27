package cn.edu.thss.iise.beehivez.server.metric.tager.test;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class VarianceTest {

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		String path1 = "C:\\Users\\Shudi\\Desktop\\tager\\variance\\cfs-";
		String path2 = "C:\\Users\\Shudi\\Desktop\\tager\\variance\\tager-ledcutoff=0.8,k=0.5,weightSkippedVertex=0.1,weightSubstitutedVertex=0.5,weightSkippedEdge=0.2-";
		VarianceTest test = new VarianceTest();
		double var = 0.0;
		for(int i = 0; i < 10; ++i) {
			String p1 = path1 + i + ".txt";
			String p2 = path2 + i + ".txt";
			List<Double> sim1 = test.read(p1);
			List<Double> sim2 = test.read(p2);
			double tmp = test.compare(sim1, sim2);
			System.out.println(tmp);
			var += tmp;
		}
		System.out.println(var / 10);
	}
	
	public double compare(List<Double> sim1, List<Double> sim2) {
		double var = 0.0;
		for(int i = 0; i < sim1.size(); ++i) {
			if(sim1.get(i) > 1 || sim2.get(i) > 1) {
				System.out.print(i + " ");
			}
			var += Math.pow(sim1.get(i) - sim2.get(i), 2);
		}
		return Math.sqrt(var / sim1.size());
	}
	
	public List<Double> read(String path) throws Exception {
		List<Double> simList = new ArrayList<Double>();
		BufferedReader reader = new BufferedReader(new FileReader(path));
		String line;
		while((line = reader.readLine()) != null) {
			String[] s = line.split(" ");
			simList.add(Double.parseDouble(s[s.length - 1]));
		}
		return simList;
	}

}
