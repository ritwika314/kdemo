// Copyright (c) 2013-2016 K Team. All Rights Reserved.
package org.kframework.kil;

import org.kframework.kil.visitors.Visitor;

/**
 * An uninterpreted string, used to represent (subterms of) Maude which can't be parsed into valid terms.
 */
public class BackendTerm extends Term {

    public BackendTerm(BackendTerm term) {
        super(term);
        this.value = term.value;
    }

    Object value;

    public BackendTerm(Sort sort, Object value) {
        super(sort);
        this.value = value;
    }

    @Override
    public String toString() {
        return this.value.toString();
    }

    public Object getValue() {
        return value;
    }

    public void setValue(Object value) {
        this.value = value;
    }

    @Override
    public BackendTerm shallowCopy() {
        return new BackendTerm(this);
    }

    @Override
    public int hashCode() {
        return value.hashCode();
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof BackendTerm)) return false;
        BackendTerm b = (BackendTerm)o;
        return value.equals(b.value);
    }

    @Override
    protected <P, R, E extends Throwable> R accept(Visitor<P, R, E> visitor, P p) throws E {
        return visitor.complete(this, visitor.visit(this, p));
    }
}
