// Copyright (c) 2012-2016 K Team. All Rights Reserved.
package org.kframework.compile.checks;

import org.kframework.kil.Configuration;
import org.kframework.kil.Rewrite;
import org.kframework.kil.Rule;
import org.kframework.kil.Syntax;
import org.kframework.kil.TermCons;
import org.kframework.kil.loader.Context;
import org.kframework.kil.visitors.BasicVisitor;
import org.kframework.utils.errorsystem.KExceptionManager;

public class CheckRewrite extends BasicVisitor {

    public CheckRewrite(Context context) {
        super(context);
    }

    private boolean inConfig = false;
    private boolean inRewrite = false;
    private boolean inSideCondition = false;
    private boolean inFunction = false;
    private int rewritesNo = 0;

    @Override
    public Void visit(Syntax node, Void _void) {
        return null;
    }

    @Override
    public Void visit(Configuration node, Void _void) {
        inConfig = true;
        super.visit(node, _void);
        inConfig = false;
        return null;
    }

    @Override
    public Void visit(Rule node, Void _void) {
        rewritesNo = 0;
        this.visitNode(node.getBody());
        if (rewritesNo == 0) {
            String msg = "Rules must have at least one rewrite.";
            throw KExceptionManager.compilerError(msg, this, node);
        }

        if (node.getRequires() != null) {
            inSideCondition = true;
            this.visitNode(node.getRequires());
            inSideCondition = false;
        }
        if (node.getEnsures() != null) {
            inSideCondition = true;
            this.visitNode(node.getEnsures());
            inSideCondition = false;
        }
        return null;
    }

    @Override
    public Void visit(org.kframework.kil.Context node, Void _void) {
        this.visitNode(node.getBody());
        if (node.getRequires() != null) {
            inSideCondition = true;
            this.visitNode(node.getRequires());
            inSideCondition = false;
        }
        if (node.getEnsures() != null) {
            inSideCondition = true;
            this.visitNode(node.getEnsures());
            inSideCondition = false;
        }
        return null;
    }

    @Override
    public Void visit(TermCons node, Void _void) {
        boolean temp = inFunction;
        if (node.getProduction().containsAttribute("function")) {
            //inFunction = true;
        }
        super.visit(node, _void);
        inFunction = temp;
        return null;
    }

    @Override
    public Void visit(Rewrite node, Void _void) {
        if (inConfig) {
            String msg = "Rewrites are not allowed in configurations.";
            throw KExceptionManager.compilerError(msg, this, node);
        }
        if (inRewrite) {
            String msg = "Rewrites are not allowed to be nested.";
            throw KExceptionManager.compilerError(msg, this, node);
        }
        if (inSideCondition) {
            String msg = "Rewrites are not allowed in side conditions.";
            throw KExceptionManager.compilerError(msg, this, node);
        }
        if (inFunction) {
            String msg = "Rewrites are not allowed under functions.";
            throw KExceptionManager.compilerError(msg, this, node);
        }
        rewritesNo++;
        inRewrite = true;
        super.visit(node, _void);
        inRewrite = false;
        return null;
    }
}
