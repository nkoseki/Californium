package ch.inf.vs.californium.observe;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class ObserveRelationContainer implements Iterable<ObserveRelation> {
	
	private Set<ObserveRelation> observeRelations;
	
	public ObserveRelationContainer() {
		this.observeRelations = Collections.newSetFromMap(
				new ConcurrentHashMap<ObserveRelation,Boolean>());
	}
	
	public boolean add(ObserveRelation relation) {
		if (relation == null)
			throw new NullPointerException();
		return observeRelations.add(relation);
	}
	
	public boolean remove(ObserveRelation relation) {
		if (relation == null)
			throw new NullPointerException();
		return observeRelations.remove(relation);
	}

	@Override
	public Iterator<ObserveRelation> iterator() {
		return observeRelations.iterator();
	}
}
