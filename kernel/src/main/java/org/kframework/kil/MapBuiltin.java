// Copyright (c) 2013-2016 K Team. All Rights Reserved.
package org.kframework.kil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.Visitor;

/**
 * A builtin map.
 *
 * @author AndreiS
 */
public class MapBuiltin extends DataStructureBuiltin {

    private final Map<Term, Term> elements;

    public MapBuiltin(DataStructureSort sort, Collection<Term> baseTerms, Map<Term, Term> elements) {
        super(sort, baseTerms);
        this.elements = elements;
    }

    public Map<Term, Term> elements() {
        return Collections.unmodifiableMap(elements);
    }

    @Override
    public boolean isEmpty() {
        return elements.isEmpty() && super.baseTerms.isEmpty();
    }

    @Override
    public Term shallowCopy() {
        return new MapBuiltin(dataStructureSort, baseTerms, elements);
    }

    @Override
    public DataStructureBuiltin shallowCopy(java.util.Collection<Term> baseTerms) {
        return new MapBuiltin(dataStructureSort, baseTerms, elements);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * Context.HASH_PRIME + super.hashCode();
        hash = hash * Context.HASH_PRIME + elements.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof MapBuiltin)) {
            return false;
        }

        MapBuiltin mapBuiltin = (MapBuiltin) object;
        return super.equals(mapBuiltin) && elements.equals(mapBuiltin.elements);
    }

    @Override
    protected <P, R, E extends Throwable> R accept(Visitor<P, R, E> visitor, P p) throws E {
        return visitor.complete(this, visitor.visit(this, p));
    }

    @Override
    public Term toKApp(Context context, Comparator<Term> comparator) {
        List<Term> items = new ArrayList<>();
        for (java.util.Map.Entry<Term, Term> entry : elements().entrySet()) {
            //TODO(dwightguth): compute real location including value
            Term item = KApp.of(entry.getKey().getLocation(), entry.getKey().getSource(), sort().elementLabel(),
                    entry.getKey(), entry.getValue());
            items.add(item);
        }
        for (Term base : baseTerms()) {
            items.add(base);
        }
        Collections.sort(items, comparator);
        return toKApp(items);
    }
}
