// Copyright (c) 2014-2016 K Team. All Rights Reserved.
package org.kframework.backend.java.symbolic;

import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.Set;

import org.kframework.backend.java.kil.Immutable;
import org.kframework.backend.java.kil.JavaSymbolicObject;
import org.kframework.backend.java.kil.Term;
import org.kframework.kil.ASTNode;

/**
 * Eliminates unsafe sharing of mutable terms.
 *
 * @author YilongL
 *
 */
public class EliminateUnsafeSharingTransformer extends PrePostTransformer {

    private final Set<Term> visitedTerms = Collections.newSetFromMap(new IdentityHashMap<>());

    public static Term transformTerm(Term term) {
        return (Term) term.accept(new EliminateUnsafeSharingTransformer());
    }

    private EliminateUnsafeSharingTransformer() {
        preTransformer.addTransformer(new SharedMutableTermCopier());
    }

    private class SharedMutableTermCopier extends LocalTransformer {

        @Override
        public ASTNode transform(JavaSymbolicObject object) {
            throw new UnsupportedOperationException(
                    EliminateUnsafeSharingTransformer.this.getName()
                            + " should only be applied on Terms.");
        }

        @Override
        public ASTNode transform(Term term) {
            // it's immutable and can be safely shared
            if ((term instanceof Immutable) || !term.isMutable()) {
                return new DoneTransforming(term);
            }

            // it's mutable and it's shared
            if (visitedTerms.contains(term)) {
                return new DoneTransforming(DeepCloner.clone(term));
            }

            visitedTerms.add(term);
            return term;
        }
    }
}
