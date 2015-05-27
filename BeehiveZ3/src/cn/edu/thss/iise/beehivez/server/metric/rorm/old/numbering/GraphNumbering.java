package cn.edu.thss.iise.beehivez.server.metric.rorm.old.numbering;

import java.io.File;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.framework.models.ModelGraphVertex;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.importing.pnml.PnmlImport;

import cn.edu.thss.iise.beehivez.server.metric.rorm.old.RefinedOrderingRelationsMatrix;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.CompleteFinitePrefix;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.Condition;
import cn.edu.thss.iise.beehivez.server.metric.rorm.old.unfolding.Event;

/**
 * @author Shudi
 *
 */
public class GraphNumbering {

	private Set<Branch> activeBranches = new HashSet<Branch>();
	private Set<Branch> waitingBranches = new HashSet<Branch>();
	private Set<Condition> loopJoinConditions = new HashSet<Condition>();

	public static void main(String[] args) throws Exception {
		// TODO Auto-generated method stub
		// String filePath = "C:\\Users\\Shudi\\Desktop\\Model\\test\\M10.pnml";
		// String filePath =
		// "C:\\Users\\Shudi\\Desktop\\exCFm\\ProM\\multi_relation_1.pnml";
		String filePath = "/Users/shudi/Desktop/M8.pnml";

		PnmlImport pnmlImport = new PnmlImport();
		PetriNet pn = pnmlImport.read(new FileInputStream(new File(filePath)));

		RefinedOrderingRelationsMatrix ecfm = new RefinedOrderingRelationsMatrix(pn);
	}

	public Map<ModelGraphVertex, Integer> numberingCFP(CompleteFinitePrefix cfp) {
		getLoopJoinConditions(cfp);
		Map<ModelGraphVertex, Integer> hmNumbering = new HashMap<ModelGraphVertex, Integer>();
		Condition source = (Condition) cfp.getSource();
		Branch startBranch = new Branch(source, false, false, 1);
		activeBranches.add(startBranch);
		while (!activeBranches.isEmpty()) {
			Branch randomBranch = activeBranches.iterator().next();
			forwardBranch(randomBranch, hmNumbering);
		}
		return hmNumbering;
	}

	private void forwardBranch(Branch branch,
			Map<ModelGraphVertex, Integer> hmNumbering) {
		if (branch.isWaiting()) {
			return;
		}
		ModelGraphVertex curVertex = branch.getVertex();
		if (curVertex instanceof Condition
				&& loopJoinConditions.contains(curVertex)) {
			activeBranches.remove(branch);
			hmNumbering.put(curVertex, branch.getNumber());
		} else if (((curVertex instanceof Condition && noLoopMerge((Condition) curVertex) > 1) 
				|| (curVertex instanceof Event && curVertex.getPredecessors().size() > 1)) 
				&& !branch.isMerged()) {
			branch.setWaiting(true);
			activeBranches.remove(branch);
			waitingBranches.add(branch);
			mergeBranch(branch);
		} else {
			if (branch.isMerged() && branch.getVertex() instanceof Condition) {
				for (ModelGraphVertex v : branch.getMergeVertexs()) {
					hmNumbering.put(v, branch.getNumber());
				}
			} else {
				hmNumbering.put(branch.getVertex(), branch.getNumber());
			}
			Set<ModelGraphVertex> postVertexes = branch.getVertex()
					.getSuccessors();
			activeBranches.remove(branch);
			for (ModelGraphVertex v : postVertexes) {
				Branch newBranch = new Branch(v, false, false,
						branch.getNumber() + 1);
				activeBranches.add(newBranch);
				forwardBranch(newBranch, hmNumbering);
			}
		}
	}

	private void mergeBranch(Branch branch) {
		ModelGraphVertex curVertex = branch.getVertex();
		if (curVertex instanceof Condition) {
			Condition curCondition = (Condition) curVertex;
			Set<Branch> tmp = new HashSet<Branch>();
			for (Branch b : waitingBranches) {
				if(curCondition.getMappingConditions().contains(b.getVertex())) {
					tmp.add(b);
				}
			}
			if (tmp.size() == noLoopMerge((Condition) curVertex)) {
				int max = 0;
				List<Branch> newBranches = new ArrayList<Branch>();
				Set<ModelGraphVertex> mergeVertexs = new HashSet<ModelGraphVertex>();
				for (Branch b : tmp) {
					max = Math.max(max, b.getNumber());
					mergeVertexs.add(b.getVertex());
					if (!((Condition)b.getVertex()).isCutoffPost()
							|| !b.getVertex().getSuccessors().isEmpty()) {
						Branch newBranch = new Branch(b.getVertex(), false,
								true, 0);
						waitingBranches.remove(b);
						newBranches.add(newBranch);
					}
				}
				for (Branch b : newBranches) {
					b.setNumber(max);
					b.getMergeVertexs().addAll(mergeVertexs);
					activeBranches.add(b);
				}
			}
		} else if (curVertex instanceof Event) {
			Event curEvent = (Event) curVertex;
			int merge = curEvent.getPredecessors().size();
			Set<Branch> tmp = new HashSet<Branch>();
			for (Branch b : waitingBranches) {
//				if (b.getVertex().getIdentifier()
//						.equals(curVertex.getIdentifier())) {
//					tmp.add(b);
//				}
				if (b.getVertex() == curVertex) {
					tmp.add(b);
				}
			}
			if (tmp.size() == merge) {
				int max = 0;
				Branch newBranch = new Branch(curVertex, false, true, max);
				for (Branch b : tmp) {
					max = Math.max(max, b.getNumber());
					waitingBranches.remove(b);
				}
				newBranch.setNumber(max);
				activeBranches.add(newBranch);
			}
		}
	}

	private int noLoopMerge(Condition condition) {
		Set<Condition> mappingConditions = condition.getMappingConditions();
		if(mappingConditions == null || mappingConditions.isEmpty()) {
			return 1;
		}
		int count = 0;
		for (Condition c : mappingConditions) {
			if (!loopJoinConditions.contains(c)) {
				++count;
			}
		}
		int numOfPredecessors = condition.getOriginalPlace().getPredecessors()
				.size();
		return Math.min(count, numOfPredecessors);
	}

	private void getLoopJoinConditions(CompleteFinitePrefix cfp) {
		loopJoinConditions.clear();
		Condition source = (Condition) cfp.getSource();
		Set<String> visited = new HashSet<String>();
		dfs(source, visited);
	}

	private void dfs(ModelGraphVertex u, Set<String> visited) {
		if (u instanceof Condition && visited.contains(u.getIdentifier())
				&& ((Condition) u).isCutoffPost()) {
			loopJoinConditions.add((Condition) u);
			return;
		}
		boolean visitAgain = visited.contains(u.getIdentifier());
		visited.add(u.getIdentifier());
		Set<ModelGraphVertex> uSuccSet = u.getSuccessors();
		for (ModelGraphVertex uSucc : uSuccSet) {
			dfs(uSucc, visited);
		}
		if(!visitAgain) {
			visited.remove(u.getIdentifier());
		}
	}
}
