// Copyright (c) 2013-2016 K Team. All Rights Reserved.
package org.kframework.backend.java.symbolic;

import org.kframework.backend.java.builtins.*;
import org.kframework.backend.java.kil.*;
import org.kframework.kil.ASTNode;
import org.kframework.kil.visitors.Visitor;

/**
 * Performs transformation which includes pre-processing and post-processing.
 * <p><br>
 * Transformation on a given node is performed in three steps:
 * <li>pre-processing that node;
 * <li>applying transformation recursively on its children;
 * <li>post-processing that node.
 *
 * @author AndreiS
 */
public abstract class PrePostTransformer extends CopyOnWriteTransformer {

    protected final CombinedLocalTransformer preTransformer = new CombinedLocalTransformer();
    protected final CombinedLocalTransformer postTransformer = new CombinedLocalTransformer();

    public PrePostTransformer(TermContext context) {
        super(context);
    }

    public PrePostTransformer() { }

    @Override
    public ASTNode transform(CellCollection cellCollection) {
        ASTNode astNode = cellCollection.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof CellCollection : "preTransformer should not modify type";
        cellCollection = (CellCollection) astNode;

        Term term = (Term) super.transform(cellCollection);
        if (term instanceof CellCollection) {
            term = (Term) term.accept(postTransformer);
        }
        return term;
    }

    @Override
    public ASTNode transform(Collection collection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ASTNode transform(ConstrainedTerm constrainedTerm) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ASTNode transform(KLabelConstant kLabelConstant) {
        ASTNode astNode = kLabelConstant.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof KLabelConstant : "preTransformer should not modify type";
        kLabelConstant = (KLabelConstant) astNode;
        kLabelConstant = (KLabelConstant) super.transform(kLabelConstant);
        return kLabelConstant.accept(postTransformer);
    }

    @Override
    public ASTNode transform(KLabelFreezer kLabelFreezer) {
        ASTNode astNode = kLabelFreezer.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof KLabelFreezer : "preTransformer should not modify type";
        kLabelFreezer = (KLabelFreezer) astNode;
        kLabelFreezer = (KLabelFreezer) super.transform(kLabelFreezer);
        return kLabelFreezer.accept(postTransformer);
    }

    @Override
    public ASTNode transform(Hole hole) {
        ASTNode astNode = hole.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof Hole : "preTransformer should not modify type";
        hole = (Hole) astNode;
        hole = (Hole) super.transform(hole);
        return hole.accept(postTransformer);
    }

    @Override
    public ASTNode transform(KLabelInjection kLabelInjection) {
        ASTNode astNode = kLabelInjection.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof KLabelInjection : "preTransformer should not modify type";
        kLabelInjection = (KLabelInjection) astNode;
        kLabelInjection = (KLabelInjection) super.transform(kLabelInjection);
        return kLabelInjection.accept(postTransformer);
    }

    @Override
    public ASTNode transform(KItem kItem) {
        ASTNode astNode = kItem.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof KItem : "preTransformer should not modify type";
        kItem = (KItem) astNode;
        kItem = (KItem) super.transform(kItem);
        return kItem.accept(postTransformer);
    }

    @Override
    public ASTNode transform(KItemProjection kItemProjection) {
        ASTNode astNode = kItemProjection.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof KItemProjection : "preTransformer should not modify type";
        kItemProjection = (KItemProjection) astNode;
        kItemProjection = (KItemProjection) super.transform(kItemProjection);
        return kItemProjection.accept(postTransformer);
    }

    @Override
    public ASTNode transform(Token token) {
        ASTNode astNode = token.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof Token : "preTransformer should not modify type";
        token = (Token) astNode;
        token = (Token) super.transform(token);
        return token.accept(postTransformer);
    }

    @Override
    public ASTNode transform(UninterpretedToken uninterpretedToken) {
        return transform((Token) uninterpretedToken);
    }

    @Override
    public ASTNode transform(BoolToken boolToken) {
        return transform((Token) boolToken);
    }

    @Override
    public ASTNode transform(IntToken intToken) {
        return transform((Token) intToken);
    }

    @Override
    public ASTNode transform(StringToken stringToken) {
        return transform((Token) stringToken);
    }

    @Override
    public ASTNode transform(KCollection kCollection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ASTNode transform(KLabel kLabel) {
        ASTNode astNode = kLabel.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof KLabel : "preTransformer should not modify type";
        kLabel = (KLabel) astNode;
        kLabel = (KLabel) super.transform(kLabel);
        return kLabel.accept(postTransformer);
    }

    @Override
    public ASTNode transform(KList kList) {
        ASTNode astNode = kList.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof KList : "preTransformer should not modify type";
        kList = (KList) astNode;
        // TODO(YilongL): why not apply postTransformer if term is not a KList?
        Term term = (Term) super.transform(kList);
        if (term instanceof KList) {
            // TODO(YilongL): why cast it to KList?
            term = (KList) term.accept(postTransformer);
        }
        return term;
    }

    @Override
    public ASTNode transform(KSequence kSequence) {
        ASTNode astNode = kSequence.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof KSequence : "preTransformer should not modify type";
        kSequence = (KSequence) astNode;
        Term term =  (Term) super.transform(kSequence);
        // TODO(YilongL): why not apply postTransformer if term is not a KSequence?
        if (term instanceof KSequence) {
            // TODO(YilongL): why cast it to KSequence?
            term = (KSequence) term.accept(postTransformer);
        }
        return term;
    }

    @Override
    public ASTNode transform(BuiltinList builtinList) {
        ASTNode astNode = builtinList.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof BuiltinList : "preTransformer should not modify type";
        builtinList = (BuiltinList) astNode;
        return ((JavaSymbolicObject) super.transform(builtinList)).accept(postTransformer);
    }

    @Override
    public ASTNode transform(BuiltinMap builtinMap) {
        ASTNode astNode = builtinMap.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof BuiltinMap : "preTransformer should not modify type";
        builtinMap = (BuiltinMap) astNode;
        return ((JavaSymbolicObject) super.transform(builtinMap)).accept(postTransformer);
    }

    @Override
    public ASTNode transform(BuiltinSet builtinSet) {
        ASTNode astNode = builtinSet.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof BuiltinSet : "preTransformer should not modify type";
        builtinSet = (BuiltinSet) astNode;
        return ((JavaSymbolicObject) super.transform(builtinSet)).accept(postTransformer);
    }

    @Override
    public ASTNode transform(MetaVariable metaVariable) {
        return transform((Token) metaVariable);
    }

    @Override
    public ASTNode transform(Rule rule) {
        ASTNode astNode = rule.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof Rule : "preTransformer should not modify type";
        rule = (Rule) astNode;
        rule = (Rule) super.transform(rule);
        return rule.accept(postTransformer);
    }

    @Override
    public ASTNode transform(ConjunctiveFormula conjunctiveFormula) {
        ASTNode astNode = conjunctiveFormula.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof ConjunctiveFormula : "preTransformer should not modify type";
        conjunctiveFormula = (ConjunctiveFormula) astNode;
        conjunctiveFormula = (ConjunctiveFormula) super.transform(conjunctiveFormula);
        return conjunctiveFormula.accept(postTransformer);
    }

    @Override
    public ASTNode transform(DisjunctiveFormula disjunctiveFormula) {
        ASTNode astNode = disjunctiveFormula.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof DisjunctiveFormula : "preTransformer should not modify type";
        disjunctiveFormula = (DisjunctiveFormula) astNode;
        disjunctiveFormula = (DisjunctiveFormula) super.transform(disjunctiveFormula);
        return disjunctiveFormula.accept(postTransformer);
    }

    @Override
    public ASTNode transform(Term node) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ASTNode transform(Variable variable) {
        ASTNode astNode = variable.accept(preTransformer);
        if (astNode instanceof DoneTransforming) {
            return ((DoneTransforming) astNode).getContents();
        }
        assert astNode instanceof Variable : "preTransformer should not modify type";
        variable = (Variable) astNode;
        variable = (Variable) super.transform(variable);
        return variable.accept(postTransformer);
    }

    protected static class DoneTransforming extends ASTNode {
        public DoneTransforming(ASTNode node) {
            contents = node;
        }

        @Override
        public ASTNode shallowCopy() {
            throw new UnsupportedOperationException();
        }

        public ASTNode getContents() {
            return contents;
        }

        private final ASTNode contents;

        @Override
        protected <P, R, E extends Throwable> R accept(Visitor<P, R, E> visitor, P p) throws E {
            throw new UnsupportedOperationException();
        }
    }
}
