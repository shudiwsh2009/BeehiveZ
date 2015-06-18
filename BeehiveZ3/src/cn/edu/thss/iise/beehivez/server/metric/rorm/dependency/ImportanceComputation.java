package cn.edu.thss.iise.beehivez.server.metric.rorm.dependency;

import org.jbpt.petri.NetSystem;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;

public class ImportanceComputation {
	
	private NetSystem _sys;
	private CompletePrefixUnfolding _cpu;
	

	public ImportanceComputation(CompletePrefixUnfolding cpu,
			LeastCommonPredecessorsAndSuccessors lc) {
		this._cpu = cpu;
		this._sys = (NetSystem) cpu.getOriginativeNetSystem();
	}
	
	
}
