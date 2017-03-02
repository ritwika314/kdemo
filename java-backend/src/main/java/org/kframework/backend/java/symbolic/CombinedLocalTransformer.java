// Copyright (c) 2013-2016 K Team. All Rights Reserved.
package org.kframework.backend.java.symbolic;

import org.kframework.backend.java.builtins.*;
import org.kframework.backend.java.kil.*;
import org.kframework.kil.ASTNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Combines a list of {@code LocalTransformer}s.
 *
 * @author Traian
 */
public class CombinedLocalTransformer extends LocalTransformer {

    private final List<LocalTransformer> transformers;

    public CombinedLocalTransformer() {
        transformers = new ArrayList<>();
    }

    public CombinedLocalTransformer(LocalTransformer ... localTransformers) {
        this();
        transformers.addAll(Arrays.asList(localTransformers));
    }

    public void addTransformer(LocalTransformer t) {
        transformers.add(t);
    }

    @Override
    public String getName() {
        String name = "Combined Transformer:\n";
        for (Transformer t : transformers) {
            name += "\t" + t.getName() + "\n";
        }
        return name;
    }

    /**
     * Applies all internal transformers on the given node in order.
     */
    private ASTNode transformAll(JavaSymbolicObject node) {
        for (Transformer t : transformers) {
            ASTNode astNode = node.accept(t);
            if (!(astNode instanceof JavaSymbolicObject))
                return astNode;
            node = (JavaSymbolicObject) astNode;
        }
        return node;
    }

    @Override
    public ASTNode transform(BitVector node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(BoolToken node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(BuiltinList node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(BuiltinMap node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(BuiltinSet node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(CellCollection node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(Collection node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(ConstrainedTerm node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(Hole node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(IntToken node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(KLabelConstant node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(KLabelFreezer node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(KLabelInjection node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(KItem node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(KCollection node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(KLabel node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(KList node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(KSequence node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(MetaVariable node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(Rule node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(ConjunctiveFormula node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(DisjunctiveFormula node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(Term node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(Token node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(UninterpretedToken node) {
        return transformAll(node);
    }

    @Override
    public ASTNode transform(Variable node) {
        return transformAll(node);
    }
}
