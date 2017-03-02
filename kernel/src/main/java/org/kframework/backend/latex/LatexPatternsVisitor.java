// Copyright (c) 2012-2016 K Team. All Rights Reserved.
package org.kframework.backend.latex;

import org.kframework.kil.*;
import org.kframework.kil.visitors.BasicVisitor;
import org.kframework.utils.StringUtil;

import java.util.HashMap;
import java.util.Map;


public class LatexPatternsVisitor extends BasicVisitor {
    public LatexPatternsVisitor(org.kframework.kil.loader.Context context) {
        super(context);
    }

    private Map<Production, String> patterns = new HashMap<Production, String>();
    private String pattern = "";
    private int nonTerm;
    private boolean prevNonTerm;

    public Map<Production, String> getPatterns() {
        return patterns;
    }

    @Override
    public Void visit(Production p, Void _void) {
        if (p.containsAttribute("latex")) {
            pattern = p.getAttribute("latex");
        } else {
            pattern = "";
            nonTerm = 1;
            prevNonTerm = false;
            super.visit(p, _void);
        }
        patterns.put(p, pattern);
        return null;
    }

    @Override
    public Void visit(NonTerminal sort, Void _void) {
        if (prevNonTerm)
            pattern += "\\mathrel{}";
        pattern += "{#" + nonTerm++ + "}";
        prevNonTerm = true;
        return null;
    }

    @Override
    public Void visit(UserList sort, Void _void) {
        // Should be only nonterminal in a production, so prevNonTerm has no effect
        pattern += "{#" + nonTerm++ + "}";
        pattern += "\\mathpunct{\\terminalNoSpace{" + StringUtil.latexify(sort.getSeparator()) + "}}";
        pattern += "{#" + nonTerm++ + "}";
        return null;
    }

    @Override
    public Void visit(Terminal pi, Void _void) {
        String terminal = pi.getTerminal();
        if (terminal.isEmpty())
            return null;
        if (context.isSpecialTerminal(terminal)) {
            pattern += StringUtil.latexify(terminal);
        } else {
                        if (!prevNonTerm) pattern += "{}";
            pattern += "\\terminal{" + StringUtil.latexify(terminal) + "}";
        }
        prevNonTerm = false;
        return null;
    }

    @Override
    public Void visit(Rule node, Void _void) {
        return null;
    }

    @Override
    public Void visit(Configuration node, Void _void) {
        return null;
    }

    @Override
    public Void visit(org.kframework.kil.Context node, Void _void) {
        return null;
    }
}
