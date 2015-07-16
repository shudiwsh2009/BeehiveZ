package cn.edu.thss.iise.beehivez.server.metric.tager.ged.graph;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.processmining.framework.models.ModelGraphVertex;
import org.processmining.framework.models.fsm.FSMState;
import org.processmining.framework.models.fsm.FSMTransition;
import org.processmining.framework.models.petrinet.PetriNet;
import org.processmining.framework.models.petrinet.Place;
import org.processmining.framework.models.petrinet.StateSpace;
import org.processmining.framework.models.petrinet.Token;
import org.processmining.framework.models.petrinet.algorithms.CoverabilityGraphBuilder;

public class TLabeledGraph {
	
	private String name = "";
	private List<Vertex> vertices = new ArrayList<Vertex>();
	private List<Edge> edges = new ArrayList<Edge>();
	private Vertex source, sink;
	
	private Set<Edge> loopEdges = new HashSet<Edge>();
	
	public TLabeledGraph(PetriNet pn) {
		for(Place p : pn.getPlaces()) {
			p.removeAllTokens();
		}
		((Place) pn.getSource()).addToken(new Token());
		StateSpace space = CoverabilityGraphBuilder.build(pn);
		convert(space, pn.getName());
	}
	
	private void convert(StateSpace space, String name) {
		this.name = name;
		// vertices
		Map<ModelGraphVertex, Vertex> mapping = new HashMap<ModelGraphVertex, Vertex>();
		for(ModelGraphVertex u : space.getVerticeList()) {
			Vertex v = new Vertex();
			if(source == null) {
				@SuppressWarnings("unchecked")
				Set<ModelGraphVertex> pred = u.getPredecessors();
				if(pred.size() == 1 && pred.iterator().next().getPredecessors().isEmpty()) {
					v.getLabels().add(new Label("ROOT"));
					source = v;
				}
			}
			vertices.add(v);
			mapping.put(u, v);
		}
		// edges
		Map<ModelGraphVertex, Set<ModelGraphVertex>> visitedEdges = new HashMap<ModelGraphVertex, Set<ModelGraphVertex>>();
		for(Object o : space.getEdges()) {
			FSMTransition _transition = (FSMTransition) o;
			FSMState _tail = (FSMState) _transition.getTail();
			FSMState _head = (FSMState) _transition.getHead();
			if(!visitedEdges.containsKey(_tail)) {
				visitedEdges.put(_tail, new HashSet<ModelGraphVertex>());
			}
			if(!visitedEdges.get(_tail).contains(_head)) {
				visitedEdges.get(_tail).add(_head);
				Edge edge = new Edge(mapping.get(_tail), mapping.get(_head));
				edges.add(edge);
				mapping.get(_tail).getOutEdges().add(edge);
				mapping.get(_head).getInEdges().add(edge);
			}			
			mapping.get(_head).getLabels().add(new Label(_transition.getCondition(), _transition));
		}
		// END vertex
		sink = new Vertex();
		sink.getLabels().add(new Label("END"));
		vertices.add(sink);
		Edge lastEdge = new Edge(mapping.get(space.getSink()), sink);
		edges.add(lastEdge);
		mapping.get(space.getSink()).getOutEdges().add(lastEdge);
		sink.getInEdges().add(lastEdge);
		// loop edges
		Set<Vertex> visited = new HashSet<Vertex>();
		doFindLoopEdges(source, visited);
		// loop spans
		for(Edge e : loopEdges) {
			visited.clear();
			e.setLoopSpan(doMaxPath(e.getTarget(), e.getSource(), visited));
		}
		// confilict spans
		for(Edge e : edges) {
			visited.clear();
			e.setConflictSpan(doMaxPath(e.getSource(), e.getTarget(), visited));
		}
	}
	
	private int doMaxPath(Vertex start, Vertex end, Set<Vertex> visited) {
		if(visited.contains(start)) {
			return Integer.MIN_VALUE;
		}
		if(start == end) {
			return 0;
		}
		int max = Integer.MIN_VALUE;
		visited.add(start);
		for(Edge e : start.getOutEdges()) {
			if(loopEdges.contains(e)) {
				continue;
			}
			int dis = doMaxPath(e.getTarget(), end, visited);
			max = Math.max(max, dis);
		}
		visited.remove(start);
		return max == Integer.MIN_VALUE ? max : 1 + max;
	}
	
	private void doFindLoopEdges(Vertex u, Set<Vertex> visited) {
		if(u.getOutEdges().isEmpty()) {
			return;
		}
		for(Edge e : u.getOutEdges()) {
			if(visited.contains(e.getTarget())) {
				loopEdges.add(e);
				continue;
			}
			visited.add(e.getTarget());
			doFindLoopEdges(e.getTarget(), visited);
			visited.remove(e.getTarget());
		}
	}
	
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for(Edge e : edges) {
			builder.append(e.toString());
			builder.append("\r\n");
		}
		return builder.toString();
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public List<Vertex> getVertices() {
		return vertices;
	}

	public void setVertices(List<Vertex> vertices) {
		this.vertices = vertices;
	}

	public List<Edge> getEdges() {
		return edges;
	}

	public void setEdges(List<Edge> edges) {
		this.edges = edges;
	}

	public Vertex getSource() {
		return source;
	}

	public void setSource(Vertex source) {
		this.source = source;
	}

	public Vertex getSink() {
		return sink;
	}

	public void setSink(Vertex sink) {
		this.sink = sink;
	}
	
}
