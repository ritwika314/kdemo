// Copyright (c) 2013-2016 K Team. All Rights Reserved.
package org.kframework.backend.java.indexing;

import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.util.Constants;

import java.io.Serializable;


/**
 * @author AndreiS
 */
public class IndexingPair implements Serializable {

    public static Index getIndex(Term term, Definition definition) {
        if (term instanceof KItem) {
            KItem kItem = (KItem) term;
            if (kItem.kLabel() instanceof KLabelConstant) {
                KLabelConstant kLabel = (KLabelConstant) kItem.kLabel();
                return new KLabelIndex(kLabel);
            } else if (kItem.kLabel() instanceof KLabelFreezer) {
                KLabelFreezer freezer = (KLabelFreezer) kItem.kLabel();
                KItem frozenKItem = (KItem) freezer.term();
                return new FreezerIndex(
                        (KLabelConstant) frozenKItem.kLabel(),
                        ((KList) frozenKItem.kList()).getContents().indexOf(Hole.HOLE));
            }
        } else {
            if (definition.builtinSorts().contains(term.sort())) {
                return new TokenIndex(term.sort());
            }
        }

        return definition.indexingData.TOP_INDEX;
    }

    public static IndexingPair getKCellIndexingPair(Term term, Definition definition) {
        if (term instanceof KSequence) {
            KSequence kSequence = (KSequence) term;

            if (kSequence.concreteSize() == 0) {
                if (kSequence.hasFrame()) {
                    return definition.indexingData.TOP_INDEXING_PAIR;
                } else {
                    return definition.indexingData.BOTTOM_INDEXING_PAIR;
                }
            }
            else if (kSequence.concreteSize() == 1) {
                if (kSequence.hasFrame()) {
                    return new IndexingPair(getIndex(kSequence.get(0), definition), definition.indexingData.TOP_INDEX);
                } else {
                    return new IndexingPair(getIndex(kSequence.get(0), definition), definition.indexingData.BOTTOM_INDEX);
                }
            }
            else {
                return new IndexingPair(
                        getIndex(kSequence.get(0), definition),
                        getIndex(kSequence.get(1), definition));
            }
        } else {
            return new IndexingPair(getIndex(term, definition), definition.indexingData.BOTTOM_INDEX);
        }
    }

    /**
     * Retrieves {@code IndexingPair} from an input stream pattern based on the
     * first and last elements.
     *
     * @param pattern
     *            the input stream pattern
     * @param definition
     *            the definition
     * @return the indexing pair
     */
    public static IndexingPair getInstreamIndexingPair(Term pattern, Definition definition) {
        throw new AssertionError("Should not reach this place anymore");
    }

    /**
     * Retrieves {@code IndexingPair} from an output stream pattern based on the
     * first and second elements.
     *
     * @param pattern
     *            the output stream pattern
     * @param definition
     *            the definition
     * @return the indexing pair
     */
    public static IndexingPair getOutstreamIndexingPair(Term pattern, Definition definition) {
        throw new AssertionError("Should not reach this place anymore");
    }

    public final Index first;
    public final Index second;

    public IndexingPair(Index first, Index second) {
        this.first = first;
        this.second = second;
    }

    public boolean isUnifiable(IndexingPair pair) {
        return first.isUnifiable(pair.first) && second.isUnifiable(pair.second);
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }

        if (!(object instanceof IndexingPair)) {
            return false;
        }

        IndexingPair pair = (IndexingPair) object;
        return first.equals(pair.first) && second.equals(pair.second);
    }

    @Override
    public int hashCode() {
        int hash = 1;
        hash = hash * Constants.HASH_PRIME + first.hashCode();
        hash = hash * Constants.HASH_PRIME + second.hashCode();
        return hash;
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ")";
    }

}
