package cn.edu.thss.iise.beehivez.server.metric.rorm;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.jbpt.petri.INode;
import org.jbpt.petri.NetSystem;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Condition;
import org.jbpt.petri.unfolding.Event;
import org.jbpt.petri.unfolding.IBPNode;

public class RefinedOrderingRelationsMatrix {

	public static final int FORWARD = 1;
	public static final int BACKWARD = 0;

	private NetSystem _sys;
	private CompletePrefixUnfolding _cpu;
	private RefinedOrderingRelation[][] causalMatrix;
	private RefinedOrderingRelation[][] inverseCausalMatrix;
	private RefinedOrderingRelation[][] concurrentMatrix;
	private List<String> tName;

	private Set<Condition> _loopJoinConditions = new HashSet<Condition>();
	// used to store all the traces from a to b
	private Map<IBPNode, Map<IBPNode, Set<List<IBPNode>>>> _forwardTraces = new HashMap<IBPNode, Map<IBPNode, Set<List<IBPNode>>>>();
	
	public RefinedOrderingRelationsMatrix(NetSystem sys) {
		this._sys = sys;
		this._cpu = new CompletePrefixUnfolding(this._sys);
	}

	/**
	 * dfs to get all the XOR-join conditions which ends a loop
	 * 
	 * @return
	 */
	private Set<Condition> getLoopJoinConditions() {
		Set<Condition> loopJoinConditions = new HashSet<Condition>();
		Condition source = this._cpu.getInitialCut().iterator().next();
		Set<INode> visited = new HashSet<INode>();
		dfsLoopJoin(source, visited, loopJoinConditions);
		return loopJoinConditions;
	}

	private void dfsLoopJoin(IBPNode u, Set<INode> visited,
			Set<Condition> loopJoinConditions) {
		if (u instanceof Condition && visited.contains(u.getPetriNetNode())
				&& ((Condition) u).isCutoffPost()) {
			loopJoinConditions.add((Condition) u);
			return;
		}
		visited.add(u.getPetriNetNode());
		if (u instanceof Condition) {
			for (Event uSucc : ((Condition) u).getPostE()) {
				dfsLoopJoin(uSucc, visited, loopJoinConditions);
			}
		} else {
			for (Condition uSucc : ((Event) u).getPostConditions()) {
				dfsLoopJoin(uSucc, visited, loopJoinConditions);
			}
		}
		visited.remove(u.getPetriNetNode());
	}

}
