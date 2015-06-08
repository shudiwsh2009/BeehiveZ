package cn.edu.thss.iise.beehivez.server.metric.rorm.dependency;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.jbpt.petri.unfolding.CompletePrefixUnfolding;
import org.jbpt.petri.unfolding.Condition;
import org.jbpt.petri.unfolding.Event;

public abstract class Marking extends HashMap<Condition, Integer> {

	private static final long serialVersionUID = -2144274745926614966L;

	// associated net
	private CompletePrefixUnfolding _cpu = null;
	private Event preEvent = null;
	private Set<Event> postEvents = new HashSet<Event>();

	public Marking() {
	}

	public Marking(CompletePrefixUnfolding cpu) {
		if (cpu == null)
			throw new IllegalArgumentException(
					"CompletePrefixUnfolding object expected but was NULL!");
		this._cpu = cpu;
	}

	public Integer put(Condition c, Integer tokens) {
		if (c == null) {
			return 0;
		}
		if (!this._cpu.getConditions().contains(c)) {
			throw new IllegalArgumentException(
					"Proposed condition is not part of the associated net!");
		}
		Integer result = null;
		if (tokens == null)
			result = super.remove(c);
		else {
			if (tokens <= 0) {
				result = super.remove(c);
			} else {
				result = super.put(c, tokens);
			}
		}
		return result == null ? 0 : result;
	}

	public CompletePrefixUnfolding getCPU() {
		return this._cpu;
	}

	public boolean isMarked(Condition condition) {
		return this.get(condition) > 0;
	}

	public Collection<Condition> toMultiSet() {
		Collection<Condition> result = new ArrayList<Condition>();
		for (Map.Entry<Condition, Integer> entry : this.entrySet()) {
			for (int i = 0; i < entry.getValue(); i++) {
				result.add(entry.getKey());
			}
		}
		return result;
	}

	public void fromMultiSet(Collection<Condition> conditions) {
		this.clear();
		for (Condition c : conditions) {
			if (!this._cpu.getConditions().contains(c)) {
				continue;
			}
			Integer tokens = this.get(c);
			if (tokens == null) {
				this.put(c, 1);
			} else {
				this.put(c, tokens + 1);
			}
		}
	}

	public Integer remove(Condition condition) {
		return super.remove(condition);
	}

	public Integer get(Condition condition) {
		Integer i = super.get(condition);
		return i == null ? 0 : i;
	}

	public void clear() {
		super.clear();
	}

	public boolean isEmpty() {
		return super.isEmpty();
	}

	public Integer remove(Object condition) {
		return super.remove(condition);
	}

	public Integer get(Object c) {
		if (!(c instanceof Condition))
			return 0;
		Integer i = super.get(c);
		return i == null ? 0 : i;
	}

	public int size() {
		return super.size();
	}

	public Set<Map.Entry<Condition, Integer>> entrySet() {
		return super.entrySet();
	}
	
	@Override
	public boolean equals(Object o) {
		if (o == null) {
			return false;
		}
		if (!(o instanceof Marking)) {
			return false;
		}
		Marking that = (Marking) o;
		if (this.size() != that.size()) {
			return false;
		}

		for (Map.Entry<Condition, Integer> i : this.entrySet()) {
			Integer value = that.get(i.getKey());
			if (value == null) {
				return false;
			}
			if (!i.getValue().equals(value)) {
				return false;
			}
		}

		return true;
	}

	@Override
	public int hashCode() {
		int result = 0;

		result -= this._cpu.hashCode();

		for (Condition c : this._cpu.getConditions())
			result += 17 * c.hashCode() * this.get(c);

		return result;
	}

	public static Marking createMarking(CompletePrefixUnfolding cpu) {
		Marking m = null;
		try {
			m = (Marking) Marking.class.newInstance();
			m.setCompletePrefixUnfolding(cpu);
			return m;
		} catch (IllegalAccessException exception) {
			return m;
		} catch (InstantiationException exception) {
			return m;
		}
	}

	public void setCompletePrefixUnfolding(CompletePrefixUnfolding cpu) {
		this.clear();
		this._cpu = cpu;
	}

	public boolean isEnabled(Event e) {
		if (!this._cpu.getEvents().contains(e)) {
			return false;
		}

		for (Condition c : e.getPreConditions()) {
			if (this.get(c) == 0) {
				return false;
			}
		}
		return true;
	}

	public boolean fire(Event e) {
		if (!this.isEnabled(e)) {
			return false;
		}

		this.preEvent = e;
		for (Condition c : e.getPreConditions()) {
			this.put(c, this.get(c) - 1);
		}
		Set<Event> newEvents = new HashSet<Event>();
		for (Condition c : e.getPostConditions()) {
			if (c.isCutoffPost()) {
				c = c.getCorrespondingCondition();
			}
			this.put(c, this.get(c) + 1);
			newEvents.addAll(c.getPostE());
		}
		this.postEvents.clear();
		for(Event newE : newEvents) {
			if(this.isEnabled(newE)) {
				this.postEvents.add(newE);
			}
		}

		return true;
	}
	
	public Marking clone() {
		Marking cloneMarking = (Marking) super.clone();
		cloneMarking._cpu = this._cpu;
		cloneMarking.preEvent = this.preEvent;
		cloneMarking.postEvents = new HashSet<Event>(this.postEvents);
		return cloneMarking;
	}

}
