package cn.edu.thss.iise.beehivez.server.metric.cfs.dep;
///**
// * BeehiveZ is a business process model and instance management system.
// * Copyright (C) 2011  
// * Institute of Information System and Engineering, School of Software, Tsinghua University,
// * Beijing, China
// *
// * Contact: jintao05@gmail.com 
// *
// * This program is a free software; you can redistribute it and/or
// * modify it under the terms of the GNU General Public License
// * as published by the Free Software Foundation with the version of 2.
// *
// * This program is distributed in the hope that it will be useful,
// * but WITHOUT ANY WARRANTY; without even the implied warranty of
// * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
// * GNU General Public License for more details.
// *
// * You should have received a copy of the GNU General Public License
// * along with this program; if not, write to the Free Software
// * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
// */
//
//package cn.edu.thss.iise.beehivez.server.basicprocess;
//
//import java.io.File;
//import java.io.FileInputStream;
//import java.util.ArrayList;
//
//import cn.edu.thss.iise.beehivez.server.basicprocess.mymodel.MyPetriNet;
//import org.processmining.framework.models.petrinet.PetriNet;
//import org.processmining.importing.pnml.PnmlImport;
//
//public class NewPtsTest {
//
//	/**
//	 * @param args
//	 * @throws Exception
//	 */
//	public static void main(String[] args) throws Exception {
//		// TODO Auto-generated method stub
//
//		String path = new String("C:\\Users\\dongzihe\\Dropbox\\毕设相关\\毕设代码\\实验模型");
//		NewPtsTest npt = new NewPtsTest();
//		npt.test(path);
//	}
//
//	public void test(String filepath) throws Exception {
//		File folder = new File(filepath);
//		File[] ProcessList = folder.listFiles();
//
//		ArrayList<NewPtsSet> testSets = new ArrayList<NewPtsSet>();
//
//		for (int i = 0; i < ProcessList.length; i++) {
//			File Process = ProcessList[i];
//			if (Process.isHidden()) {
//				continue;
//			}
//
//			PnmlImport pnmlimport = new PnmlImport();
//			CTree ctree = null;
//			PetriNet petrinet = null;
//			FileInputStream pnml = null;
//			pnml = new FileInputStream(Process.getAbsolutePath());
//			petrinet = pnmlimport.read(pnml);
//
//			CTreeGenerator generator = new CTreeGenerator(
//					MyPetriNet.PromPN2MyPN(petrinet));
//			ctree = generator.generateCTree();
//			TTreeGenerator ttg = new TTreeGenerator();
//
//			NewPtsSet nps2 = ttg.generatTTree(ctree, 2, Process.getName());
//
//			testSets.add(nps2);
//
//		}
//		if (testSets.get(0).getNPSet().size() > testSets.get(1).getNPSet()
//				.size()) {
//			NewPtsSet temp1 = testSets.get(0);
//			NewPtsSet temp2 = testSets.get(1);
//			testSets.clear();
//			testSets.add(temp2);
//			testSets.add(temp1);
//		}
//
//		showResult(testSets, 1);
//	}
//
//	public void showResult(ArrayList<NewPtsSet> testSets, int a) {
//		testSets.get(a - 1).showSet();
//		testSets.get(a).showSet();
////		 System.out.println("The new PTS similarity< Ergodic> of *" +
////		 testSets.get(a-1).getFileName() + " and *" +
////		 testSets.get(a).getFileName() + " is: [" +
////		 testSets.get(a-1).setSimilarity_ergodic(testSets.get(a)) + "].");
//		System.out.println("The new PTS similarity< Greedy > of *"
//				+ testSets.get(a - 1).getFileName() + " and *"
//				+ testSets.get(a).getFileName() + " is: ["
//				+ testSets.get(a - 1).setSimilarity_greedy(testSets.get(a))
//				+ "].");
//		System.out.println("The new PTS similarity<   A*   > of *"
//				+ testSets.get(a - 1).getFileName() + " and *"
//				+ testSets.get(a).getFileName() + " is: ["
//				+ testSets.get(a - 1).setSimilarity_Astar(testSets.get(a))
//				+ "].");
//	}
//
//}