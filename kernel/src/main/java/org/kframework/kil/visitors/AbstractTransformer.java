// Copyright (c) 2014-2016 K Team. All Rights Reserved.
package org.kframework.kil.visitors;

import org.kframework.kil.ASTNode;
import org.kframework.kil.AbstractVisitor;
import org.kframework.kil.Definition;
import org.kframework.kil.Module;
import org.kframework.kil.loader.Context;

/**
 * A helper class designed to encapsulate functionality shared between
 * {@link LocalTransformer}, {@link ParseForestTransformer}, and {@link CopyOnWriteTransformer}.
 *
 * This class serves to replace the Transformable interface that existed before, and implements
 * functionality specific to visitors which transform terms.
 * @author dwightguth
 *
 */
public abstract class AbstractTransformer<E extends Throwable> extends AbstractVisitor<Void, ASTNode, E> {

    public AbstractTransformer(String name, Context context) {
        super(name, context);
    }

    public AbstractTransformer(String name, Context context,
                               Definition currentDefinition, Module currentModule) {
        super(name, context, currentDefinition, currentModule);
    }

    @Override
    public ASTNode defaultReturnValue(ASTNode node, Void _void) {
        return node;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends ASTNode> T processChildTerm(T child, ASTNode childResult) {
        return (T)childResult;
    }

    @Override
    public boolean cache() {
        return false;
    }

    @Override
    public <T extends ASTNode> boolean changed(T o, T n) {
        return o != n;
    }

}
