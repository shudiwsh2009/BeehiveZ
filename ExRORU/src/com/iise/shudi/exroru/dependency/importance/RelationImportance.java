package com.iise.shudi.exroru.dependency.importance;

import org.apache.commons.math3.linear.*;
import org.jbpt.petri.INode;
import org.jbpt.petri.NetSystem;
import org.jbpt.petri.Place;
import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Condition;
import org.jbpt.petri.unfolding.Event;
import org.jbpt.petri.unfolding.IBPNode;

import java.util.*;

@SuppressWarnings("rawtypes")
public class RelationImportance {

    private NetSystem _sys;
    private CompletePrefixUnfolding _cpu;
    private Map<IBPNode, Map<IBPNode, RealVector>> edges = new HashMap<IBPNode, Map<IBPNode, RealVector>>();
    private Map<IBPNode, Map<IBPNode, Double>> importance = new HashMap<IBPNode, Map<IBPNode, Double>>();
    private Set<RealVector> equations = new HashSet<RealVector>();
    private int unknown = 0;
    private Set<Branch> activeBranches = new HashSet<Branch>();
    private Set<Branch> waitingBranchs = new HashSet<Branch>();
    private Set<Condition> loopJoinConditions = new HashSet<Condition>();
    public RelationImportance(CompletePrefixUnfolding cpu) {
        this._cpu = cpu;
        this._sys = (NetSystem) cpu.getOriginativeNetSystem();
        searchEquations();
//		System.out.println(equations);
        solveEquations();
//		System.out.println(importance);
    }

    private void searchEquations() {
        this.loopJoinConditions = getLoopJoinConditions();
        Condition sourcecCondition = this._cpu
                .getConditions(this._sys.getSourcePlaces().iterator().next())
                .iterator().next();
        Place sinkPlace = this._sys.getSinkPlaces().iterator().next();
        Branch startBranch = new Branch(sourcecCondition, new Polynomial(
                new double[]{1.0}), false, false);
        activeBranches.add(startBranch);
        while (!activeBranches.isEmpty()) {
            Branch randomBranch = activeBranches.iterator().next();
            forwardBranch(randomBranch, sinkPlace);
        }
    }

    private void forwardBranch(Branch branch, Place sinkPlace) {
        if (branch.isWaiting()) {
            return;
        }
        IBPNode cur = branch.getCur();
        if (cur instanceof Condition && loopJoinConditions.contains(cur)) {
            activeBranches.remove(branch);
        } else if (((cur instanceof Condition && ((Condition) cur)
                .getMappingConditions() != null) || (cur instanceof Event && ((Event) cur)
                .getPreConditions().size() > 1))
                && !branch.isMerged()) {
            branch.setWaiting(true);
            activeBranches.remove(branch);
            waitingBranchs.add(branch);
            mergeBranch(branch);
        } else {
            activeBranches.remove(branch);
            if (cur instanceof Condition) {
                // split in-coefficients into equal pieces for each output edge
                Condition curCondition = (Condition) cur;
                RealVector inEdges = branch.getInEdges();
                if (curCondition.getPlace() == sinkPlace && unknown > 0) {
                    // do nothing
                } else {
                    for (Event outEvent : curCondition.getPostE()) {
                        RealVector perOutEdge = inEdges.mapDivide(curCondition
                                .getPostE().size());
                        edges.putIfAbsent(curCondition,
                                new HashMap<IBPNode, RealVector>());
                        edges.get(curCondition).putIfAbsent(outEvent,
                                perOutEdge);
                        Branch newBranch = new Branch(outEvent,
                                perOutEdge.copy(), false, false);
                        activeBranches.add(newBranch);
                        forwardBranch(newBranch, sinkPlace);
                    }
                }
            } else if (cur instanceof Event) {
                // for each output condition
                // 1. if there is already a coefficient, list a equation
                // 2. otherwise, set it with a coefficient
                Event curEvent = (Event) cur;
                RealVector inEdge = branch.getInEdges();
                for (Condition outCondition : curEvent.getPostConditions()) {
                    Condition corrCondition = (outCondition.isCutoffPost() && outCondition
                            .getPostE().isEmpty()) ? outCondition
                            .getCorrespondingCondition() : outCondition;
                    if (edges.containsKey(curEvent)
                            && edges.get(curEvent).containsKey(corrCondition)) {
                        RealVector otherEdge = edges.get(curEvent).get(
                                corrCondition);
                        RealVector outEdge = inEdge.copy();
                        while (otherEdge.getDimension() < outEdge
                                .getDimension()) {
                            otherEdge = otherEdge.append(0);
                        }
                        while (outEdge.getDimension() < otherEdge
                                .getDimension()) {
                            outEdge = outEdge.append(0);
                        }
                        outEdge = outEdge.subtract(otherEdge);
                        equations.add(outEdge);
                    } else {
                        edges.putIfAbsent(curEvent,
                                new HashMap<IBPNode, RealVector>());
                        edges.get(curEvent).putIfAbsent(corrCondition,
                                inEdge.copy());
                    }
                    Branch newBranch = new Branch(outCondition, inEdge.copy(),
                            false, false);
                    forwardBranch(newBranch, sinkPlace);
                }
            }
        }
    }

    private void mergeBranch(Branch branch) {
        IBPNode cur = branch.getCur();
        if (cur instanceof Condition) {
            Condition curCondition = (Condition) cur;
            Set<Branch> tmp = new HashSet<Branch>();
            for (Branch b : waitingBranchs) {
                if (curCondition.getMappingConditions().contains(b.getCur())) {
                    tmp.add(b);
                }
            }
            if (tmp.size() == noLoopMerge(curCondition)) {
                List<Branch> newBranches = new ArrayList<Branch>();
                RealVector inEdges = new Polynomial();
                for (Branch b : tmp) {
                    RealVector otherEdges = b.getInEdges();
                    while (inEdges.getDimension() < otherEdges.getDimension()) {
                        inEdges = inEdges.append(0);
                    }
                    while (otherEdges.getDimension() < inEdges.getDimension()) {
                        otherEdges = otherEdges.append(0);
                    }
                    inEdges = inEdges.add(otherEdges);
                    if (!((Condition) b.getCur()).isCutoffPost()
                            || !((Condition) b.getCur()).getPostE().isEmpty()) {
                        Branch newBranch = new Branch(b.getCur(),
                                new Polynomial(), false, true);
                        waitingBranchs.remove(b);
                        newBranches.add(newBranch);
                    }
                }
                // add unknown number for loop XOR-join condition
                Set<Condition> mappingConditions = curCondition
                        .getMappingConditions();
                for (Condition mappingC : mappingConditions) {
                    if (loopJoinConditions.contains(mappingC)) {
                        RealVector inEdge = new Polynomial();
                        if (edges.containsKey(mappingC.getPreEvent())
                                && edges.get(mappingC.getPreEvent())
                                .containsKey(
                                        mappingC.getCorrespondingCondition())) {
                            inEdge = edges.get(mappingC.getPreEvent()).get(
                                    mappingC.getCorrespondingCondition());
                        } else {
                            inEdge = new Polynomial();
                            ++unknown;
                            while (inEdge.getDimension() < unknown) {
                                inEdge = inEdge.append(0);
                            }
                            inEdge = inEdge.append(1);
                        }
                        while (inEdge.getDimension() < inEdges.getDimension()) {
                            inEdge = inEdge.append(0);
                        }
                        while (inEdges.getDimension() < inEdge.getDimension()) {
                            inEdges = inEdges.append(0);
                        }
                        inEdges = inEdges.add(inEdge);
                        edges.putIfAbsent(mappingC.getPreEvent(),
                                new HashMap<IBPNode, RealVector>());
                        edges.get(mappingC.getPreEvent()).putIfAbsent(
                                mappingC.getCorrespondingCondition(), inEdge);
                    }
                }
                for (Branch b : newBranches) {
                    b.setInEdges(inEdges.copy());
                    activeBranches.add(b);
                }
            }
        } else if (cur instanceof Event) {
            Event curEvent = (Event) cur;
            int merge = curEvent.getPreConditions().size();
            Set<Branch> tmp = new HashSet<Branch>();
            for (Branch b : waitingBranchs) {
                if (b.getCur() == cur) {
                    tmp.add(b);
                }
            }
            if (tmp.size() == merge) {
                Branch newBranch = new Branch(curEvent, branch.getInEdges()
                        .copy(), false, true);
                waitingBranchs.removeAll(tmp);
                activeBranches.add(newBranch);
            }
        }
    }

    private void solveEquations() {
        double[] solution = new double[0];
        if (unknown > 0) {
            Iterator<RealVector> it = equations.iterator();
            while (it.hasNext()) {
                RealVector vec = it.next();
                if (vec.getNorm() == 0) {
                    it.remove();
                }
                // System.out.println(vec.hashCode());
            }
            double[][] coefficients = new double[equations.size()][unknown];
            double[] constants = new double[equations.size()];
            // System.out.println(equations);
            // System.out.println(edges);
            int i = 0;
            for (RealVector equation : equations) {
                constants[i] = -equation.getEntry(0);
                for (int j = 1; j < equation.getDimension(); ++j) {
                    coefficients[i][j - 1] = equation.getEntry(j);
                }
                ++i;
            }
            RealMatrix coefficientsmMatrix = new Array2DRowRealMatrix(
                    coefficients, false);
            DecompositionSolver solver = new LUDecomposition(
                    coefficientsmMatrix).getSolver();
            RealVector constantsVector = new Polynomial(constants);
            RealVector solutionVector = solver.solve(constantsVector);
            solution = solutionVector.toArray();
        }
        for (Map.Entry<IBPNode, Map<IBPNode, RealVector>> outerEntry : edges
                .entrySet()) {
            importance.putIfAbsent(outerEntry.getKey(),
                    new HashMap<IBPNode, Double>());
            for (Map.Entry<IBPNode, RealVector> innerEntry : outerEntry
                    .getValue().entrySet()) {
                double[] edge = innerEntry.getValue().toArray();
                double importance = edge[0];
                for (int i = 1; i < edge.length; ++i) {
                    importance += solution[i - 1] * edge[i];
                }
                this.importance.get(outerEntry.getKey()).putIfAbsent(
                        innerEntry.getKey(), importance);
            }
        }
    }

    private int noLoopMerge(Condition condition) {
        Set<Condition> mappingConditions = condition.getMappingConditions();
        if (mappingConditions == null || mappingConditions.isEmpty()) {
            return 1;
        }
        int count = 0;
        for (Condition c : mappingConditions) {
            if (!loopJoinConditions.contains(c)) {
                ++count;
            }
        }
        int numOfPredecessors = this._sys.getPreset(condition.getPlace())
                .size();
        return Math.min(count, numOfPredecessors);
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
                && ((Condition) u).isCutoffPost()
                && ((Condition) u).getPostE().isEmpty()) {
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

    public Map<IBPNode, Map<IBPNode, Double>> getImportance() {
        return importance;
    }
}
