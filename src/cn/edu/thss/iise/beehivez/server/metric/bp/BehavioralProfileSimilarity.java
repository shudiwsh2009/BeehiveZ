/**
 * BeehiveZ is a business process model and instance management system.
 * Copyright (C) 2011  
 * Institute of Information System and Engineering, School of Software, Tsinghua University,
 * Beijing, China
 *
 * Contact: wangwenxingbuaa@gmail.com 
 *
 * This program is a free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation with the version of 2.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package cn.edu.thss.iise.beehivez.server.metric.bp;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;
import org.processmining.mining.petrinetmining.PetriNetResult;

import cn.edu.thss.iise.beehivez.server.basicprocess.mymodel.MyPetriNet;
import cn.edu.thss.iise.beehivez.server.basicprocess.occurrencenet.ONCompleteFinitePrefix;
import cn.edu.thss.iise.beehivez.server.basicprocess.occurrencenet.ONCompleteFinitePrefixBuilder;
import cn.edu.thss.iise.beehivez.server.metric.CausalFootprintSimilarity;
import cn.edu.thss.iise.beehivez.server.metric.PetriNetSimilarity;

/**
 * @author Wenxing Wang
 * 
 * @date Mar 27, 2011
 * 
 * modified by Shuhao Wang
 * Apr 5, 2015
 */
public class BehavioralProfileSimilarity extends PetriNetSimilarity {
	
	public static final float EPSILON = 0.00001f;
	
	public String getName() {
		return "BehavioralProfileSimilarity";
	}

	public String getDesription() {
		return "similarity match based on basic process segments whitch is designed by wwx";
	}
	
	@Override
	public float similarity(PetriNet pn1, PetriNet pn2) {
		// TODO Auto-generated method stub
		return similarity((PetriNet) pn1.clone(), (PetriNet) pn2.clone(), 1);
	}
	
	private float similarity(PetriNet pn1, PetriNet pn2, int internal){
		MyPetriNet mpn1 = MyPetriNet.PromPN2MyPN(pn1);
		ONCompleteFinitePrefixBuilder builder1 = new ONCompleteFinitePrefixBuilder(mpn1);
		ONCompleteFinitePrefix cfp1 = builder1.Build();
		BehavioralRelationBuilder bp1 = new BehavioralRelationBuilder(cfp1);
		bp1.buildBehavioralRelaton();
		
		MyPetriNet mpn2 = MyPetriNet.PromPN2MyPN(pn2);
		ONCompleteFinitePrefixBuilder builder2 = new ONCompleteFinitePrefixBuilder(mpn2);
		ONCompleteFinitePrefix cfp2 = builder2.Build();
		BehavioralRelationBuilder bp2 = new BehavioralRelationBuilder(cfp2);
		bp2.buildBehavioralRelaton();
		
		Map<Map<String, String>, BehavioralRelation> hm1 = 
			new HashMap<Map<String, String>, BehavioralRelation>();
		Map<Map<String, String>, BehavioralRelation> hm2 = 
			new HashMap<Map<String, String>, BehavioralRelation>();
		
		transform(hm1, cfp1, bp1.get_relation());
		transform(hm2, cfp2, bp2.get_relation());
		
		Map<BehavioralRelation, Set<TwoVertices>> bpMap1 = reverse(hm1);
		Map<BehavioralRelation, Set<TwoVertices>> bpMap2 = reverse(hm2);
		
		return compute(bpMap1, bpMap2);
	}
	
	public float compute(Map<BehavioralRelation, Set<TwoVertices>> bp1,
			Map<BehavioralRelation, Set<TwoVertices>> bp2) {
		float sim = 0.0f;
		float es = computeExclusivenessSimilairty(bp1, bp2);
		float so = computeStrictOrderSimilarity(bp1, bp2);
		float io = computeInterleavingOrderSimilarity(bp1, bp2);
		float eso = computeExtendedStrictOrderSimilarity(bp1, bp2);
		float eio = computeExtendedInterleavingOrderSimilarity(bp1, bp2);
		int count = 0;
		if(es > 0 || Math.abs(es - 0.0f) < EPSILON) {
			sim += es;
			++count;
		}
		if(so > 0 || Math.abs(so - 0.0f) < EPSILON) {
			sim += so;
			++count;
		}
		if(io > 0 || Math.abs(io - 0.0f) < EPSILON) {
			sim += io;
			++count;
		}
		if(eso > 0 || Math.abs(eso - 0.0f) < EPSILON) {
			sim += eso;
			++count;
		}
		if(eio > 0 || Math.abs(eio - 0.0f) < EPSILON) {
			sim += eio;
			++count;
		}
		return sim / count;
	}
	
	public float computeExclusivenessSimilairty(Map<BehavioralRelation, Set<TwoVertices>> bp1,
			Map<BehavioralRelation, Set<TwoVertices>> bp2) {
		Set<TwoVertices> numeratorSet = intersectionSet(bp1.get(BehavioralRelation.Exclussive), bp2.get(BehavioralRelation.Exclussive));
		Set<TwoVertices> denominatorSet = unionSet(bp1.get(BehavioralRelation.Exclussive), bp2.get(BehavioralRelation.Exclussive));
		return denominatorSet.size() == 0 ? -1 : ((float)numeratorSet.size()) / ((float)denominatorSet.size());
	}
	
	public float computeStrictOrderSimilarity(Map<BehavioralRelation, Set<TwoVertices>> bp1,
			Map<BehavioralRelation, Set<TwoVertices>> bp2) {
		Set<TwoVertices> numeratorSet = intersectionSet(bp1.get(BehavioralRelation.StrictOrder), bp2.get(BehavioralRelation.StrictOrder));
		Set<TwoVertices> denominatorSet = unionSet(bp1.get(BehavioralRelation.StrictOrder), bp2.get(BehavioralRelation.StrictOrder));
		return denominatorSet.size() == 0 ? -1 : ((float)numeratorSet.size()) / ((float)denominatorSet.size());
	}
	
	public float computeInterleavingOrderSimilarity(Map<BehavioralRelation, Set<TwoVertices>> bp1,
			Map<BehavioralRelation, Set<TwoVertices>> bp2) {
		Set<TwoVertices> numeratorSet = intersectionSet(bp1.get(BehavioralRelation.Interleaving), bp2.get(BehavioralRelation.Interleaving));
		Set<TwoVertices> denominatorSet = unionSet(bp1.get(BehavioralRelation.Interleaving), bp2.get(BehavioralRelation.Interleaving));
		return denominatorSet.size() == 0 ? -1 : ((float)numeratorSet.size()) / ((float)denominatorSet.size());
	}
	
	public float computeExtendedStrictOrderSimilarity(Map<BehavioralRelation, Set<TwoVertices>> bp1,
			Map<BehavioralRelation, Set<TwoVertices>> bp2) {
		Set<TwoVertices> numeratorSet = intersectionSet(unionSet(bp1.get(BehavioralRelation.StrictOrder), bp1.get(BehavioralRelation.ReverseOrder)), 
				unionSet(bp2.get(BehavioralRelation.StrictOrder), bp2.get(BehavioralRelation.ReverseOrder)));
		Set<TwoVertices> denominatorSet = unionSet(unionSet(bp1.get(BehavioralRelation.StrictOrder), bp1.get(BehavioralRelation.ReverseOrder)), 
				unionSet(bp2.get(BehavioralRelation.StrictOrder), bp2.get(BehavioralRelation.ReverseOrder)));
		return denominatorSet.size() == 0 ? -1 : ((float)numeratorSet.size()) / ((float)denominatorSet.size());
	}
	
	@SuppressWarnings("unchecked")
	public float computeExtendedInterleavingOrderSimilarity(Map<BehavioralRelation, Set<TwoVertices>> bp1,
			Map<BehavioralRelation, Set<TwoVertices>> bp2) {
		Set<TwoVertices> numeratorSet = intersectionSet(unionSets(bp1.get(BehavioralRelation.StrictOrder), bp1.get(BehavioralRelation.ReverseOrder), bp1.get(BehavioralRelation.Interleaving)), 
				unionSets(bp2.get(BehavioralRelation.StrictOrder), bp2.get(BehavioralRelation.ReverseOrder), bp2.get(BehavioralRelation.Interleaving)));
		Set<TwoVertices> denominatorSet = unionSet(unionSets(bp1.get(BehavioralRelation.StrictOrder), bp1.get(BehavioralRelation.ReverseOrder), bp1.get(BehavioralRelation.Interleaving)), 
				unionSets(bp2.get(BehavioralRelation.StrictOrder), bp2.get(BehavioralRelation.ReverseOrder), bp2.get(BehavioralRelation.Interleaving)));
		return denominatorSet.size() == 0 ? -1 : ((float)numeratorSet.size()) / ((float)denominatorSet.size());
	}
	
	public Set<TwoVertices> intersectionSet(Set<TwoVertices> set1, Set<TwoVertices> set2) {
		Set<TwoVertices> intersectionSet = new HashSet<TwoVertices>();
		for(TwoVertices v1 : set1) {
			if(set2.contains(v1)) {
				intersectionSet.add(v1);
			}
		}
		return intersectionSet;
	}
	
	public Set<TwoVertices> unionSet(Set<TwoVertices> set1, Set<TwoVertices> set2) {
		Set<TwoVertices> unionSet = new HashSet<TwoVertices>();
		unionSet.addAll(set1);
		unionSet.addAll(set2);
		return unionSet;
	}
	
	@SuppressWarnings("unchecked")
	public Set<TwoVertices> unionSets(Set<TwoVertices>... sets) {
		Set<TwoVertices> unionSet = new HashSet<TwoVertices>();
		for(Set<TwoVertices> set : sets) {
			unionSet.addAll(set);
		}
		return unionSet;
	}
	
	public Map<BehavioralRelation, Set<TwoVertices>> reverse(
			Map<Map<String, String>, BehavioralRelation> hm) {
		Map<BehavioralRelation, Set<TwoVertices>> bpMap = new HashMap<BehavioralRelation, Set<TwoVertices>>();
		bpMap.put(BehavioralRelation.StrictOrder, new HashSet<TwoVertices>());
		bpMap.put(BehavioralRelation.ReverseOrder, new HashSet<TwoVertices>());
		bpMap.put(BehavioralRelation.Interleaving, new HashSet<TwoVertices>());
		bpMap.put(BehavioralRelation.Exclussive, new HashSet<TwoVertices>());
		bpMap.put(BehavioralRelation.Undefined, new HashSet<TwoVertices>());
		for(Map<String, String> pair : hm.keySet()) {
			for(Map.Entry<String, String> entry : pair.entrySet()) {
				bpMap.get(hm.get(pair)).add(new TwoVertices(entry.getKey(), entry.getValue()));
			}
		}
		return bpMap;
	}
	
	public void transform(Map<Map<String, String>, BehavioralRelation> hm, 
			ONCompleteFinitePrefix cfp, BehavioralRelation[][] relation){
		for(int i = 0; i < cfp.getOn().getEveSet().size(); ++i){
			for(int j = 0; j < cfp.getOn().getEveSet().size(); ++j){
				HashMap<String , String> pair = new HashMap<String , String>();
				pair.put(cfp.getOn().getEveSet().get(i).getLabel(), 
						cfp.getOn().getEveSet().get(j).getLabel());
				hm.put(pair, relation[i][j]);
			}
		}
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		// TODO Auto-generated method stub
		FileInputStream fin = null;
		FileInputStream fin2 = null;
		try {
			fin = new FileInputStream("C:\\Users\\lenovo\\Documents\\experiment\\实验一F.pnml");
			fin2 = new FileInputStream("C:\\Users\\lenovo\\Documents\\experiment\\实验一E.pnml");
//			fin = new FileInputStream("C:\\Users\\winever\\Documents\\QueryModel\\Non-free Choice\\Nonfree8.pnml");
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PnmlImport pImport = new PnmlImport();
		PnmlImport pImport2 = new PnmlImport();
		PetriNetResult pnr = null;
		PetriNetResult pnr2 = null;
		try {
			pnr = (PetriNetResult) pImport.importFile(fin);
			pnr2 = (PetriNetResult) pImport2.importFile(fin2);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		try {
			fin.close();
			fin2.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		PetriNet pn = pnr.getPetriNet();
		PetriNet pn2 = pnr2.getPetriNet();
		
		long start = System.currentTimeMillis();
		System.out.println(start);
//		BehavioralProfileSimilarity bps = new BehavioralProfileSimilarity();
//		double similarity = bps.similarity(pn, pn2);
		CausalFootprintSimilarity cfp = new CausalFootprintSimilarity();
		double similarity = cfp.similarity(pn, pn2);
		long end = System.currentTimeMillis();
		System.out.println(end);
		long duration = end - start;
		System.out.println(duration);
//		System.out.println("BehaviorProfile:" + similarity);
		System.out.println("CausalFootprintSimilarity:" + similarity);
	}

}
