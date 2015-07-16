package cn.edu.thss.iise.beehivez.server.metric.tager.ged.led;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class LabelEditDistance {

	public static double scaleWeight = 2.0;
	
	public static double similarity(List<String> label1, List<String> label2) {
		Set<String> intersectionSet = new HashSet<String>();
		Set<String> unionSet = new HashSet<String>();
		for(String l1 : label1) {
			if(label2.contains(l1)) {
				intersectionSet.add(l1);
			}
		}
		unionSet.addAll(label1);
		unionSet.addAll(label2);
		return ((double) intersectionSet.size()) / ((double) unionSet.size());
	}

	public static double cost(List<String> label1, List<String> label2) {
		if (label1.isEmpty() && label2.isEmpty())
			return 1.0;

		int totalNum = label1.size() + label2.size();
		double disScale = scaleWeight * Math.abs(label1.size() - label2.size())
				/ (double) totalNum;

		int commonSetNum = 0;
		for (String l1 : label1) {
			for (String l2 : label2) {
				if (l1.equals(l2)) {
					commonSetNum++;
				}
			}
		}

		Set<String> allLabelSet = new HashSet<String>();
		allLabelSet.addAll(label1);
		allLabelSet.addAll(label2);
		double disLabelSet = (allLabelSet.size() - commonSetNum)
				/ (double) (allLabelSet.size());
		return (disScale + disLabelSet) / (scaleWeight + 1);
	}

}
