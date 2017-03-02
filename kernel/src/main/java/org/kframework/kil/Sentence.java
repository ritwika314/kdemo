// Copyright (c) 2012-2016 K Team. All Rights Reserved.
package org.kframework.kil;

import org.kframework.kil.Interfaces.MutableParent;
import org.kframework.kil.visitors.Visitor;

/**
 * A rule, configuration declaration, or context.
 * Each parses as a term, this class declares common members
 * {@link #body} and {@link #requires}, which have different
 * interpretations in the subclasses.
 */
public class Sentence extends ModuleItem implements MutableParent<Term, Sentence.Children> {
    /** Label from {@code rule[}label{@code ]:} syntax or "". Currently unrelated to attributes */
    String label = "";
    Term body;
    Term requires = null;
    Term ensures = null;

    public static enum Children {
        BODY, REQUIRES, ENSURES
    }

    public Sentence(Sentence s) {
        super(s);
        this.body = s.body;
        this.label = s.label;
        this.requires = s.requires;
        this.ensures = s.ensures;
    }

    public Sentence() {
        super();
    }

    public Term getBody() {
        return body;
    }

    public void setBody(Term body) {
        this.body = body;
    }

    public Term getRequires() {
        return requires;
    }

    public void setRequires(Term requires) {
        this.requires = requires;
    }

    @Override
    public Sentence shallowCopy() {
        return new Sentence(this);
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String toString() {
        String content = "";

        if (this.label != null && !this.label.equals(""))
            content += "[" + this.label + "]: ";

        content += this.body + " ";
        if (this.requires != null) {
            content += "requires " + this.requires + " ";
        }
        if (this.ensures != null) {
            content += "ensures " + this.ensures + " ";
        }

        return content + getAttributes();
    }

    public Term getEnsures() {
        return ensures;
    }

    public void setEnsures(Term ensures) {
        this.ensures = ensures;
    }

    @Override
    protected <P, R, E extends Throwable> R accept(Visitor<P, R, E> visitor, P p) throws E {
        return visitor.complete(this, visitor.visit(this, p));
    }

    @Override
    public Term getChild(Children type) {
        switch(type) {
            case BODY:
                return getBody();
            case ENSURES:
                return getEnsures();
            case REQUIRES:
                return getRequires();
            default:
                throw new AssertionError("unreachable");
        }
    }

    @Override
    public void setChild(Term child, Children type) {
        switch(type) {
            case BODY:
                setBody(child);
                break;
            case ENSURES:
                setEnsures(child);
                break;
            case REQUIRES:
                setRequires(child);
                break;
            default:
                throw new AssertionError("unreachable");
        }
    }
}
