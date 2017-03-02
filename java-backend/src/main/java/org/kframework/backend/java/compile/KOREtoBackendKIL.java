// Copyright (c) 2014-2016 K Team. All Rights Reserved.

package org.kframework.backend.java.compile;

import org.apache.commons.lang3.tuple.Pair;
import org.kframework.attributes.Att;
import org.kframework.attributes.Location;
import org.kframework.attributes.Source;
import org.kframework.backend.java.kil.*;
import org.kframework.backend.java.kil.KItem;
import org.kframework.backend.java.symbolic.ConjunctiveFormula;
import org.kframework.builtin.KLabels;
import org.kframework.definition.Module;
import org.kframework.compile.ConfigurationInfo;
import org.kframework.kil.Attribute;
import org.kframework.kil.Attributes;
import org.kframework.kil.Cell;
import org.kframework.kore.Assoc;
import org.kframework.kore.K;
import org.kframework.kore.KApply;
import org.kframework.kore.KLabel;
import org.kframework.kore.KRewrite;
import org.kframework.kore.KToken;
import org.kframework.kore.KVariable;
import org.kframework.kore.compile.RewriteToTop;
import org.kframework.kore.convertors.KOREtoKIL;
import org.kframework.utils.BitSet;

import static org.kframework.Collections.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;


/**
 * KORE to backend KIL
 */
public class KOREtoBackendKIL extends org.kframework.kore.AbstractConstructors<org.kframework.kore.K> {

    public static final String THE_VARIABLE = "THE_VARIABLE";

    private final Module module;
    private final Definition definition;
    private final GlobalContext global;
    /**
     * Flag that controls whether the translator substitutes the variables in a {@code Rule} with fresh variables
     */
    private final boolean freshRules;

    private final KLabelConstant kSeqLabel;
    private final KLabelConstant kDotLabel;

    private final HashMap<String, Variable> variableTable = new HashMap<>();

    public KOREtoBackendKIL(Module module, Definition definition, GlobalContext global, boolean freshRules) {
        this.module = module;
        this.definition = definition;
        this.global = global;
        this.freshRules = freshRules;

        kSeqLabel = KLabelConstant.of(KLabels.KSEQ, global.getDefinition());
        kDotLabel = KLabelConstant.of(KLabels.DOTK, global.getDefinition());
    }

    @Override
    public KLabelConstant KLabel(String name) {
        return KLabelConstant.of(name, global.getDefinition());
    }

    @Override
    public Sort Sort(String name) {
        return Sort.of(name);
    }

    @Override
    public <KK extends org.kframework.kore.K> KList KList(List<KK> items) {
        return (KList) KCollection.upKind(
                KList.concatenate(items.stream().map(this::convert).collect(Collectors.toList())),
                Kind.KLIST);
    }

    @Override
    public Token KToken(String s, org.kframework.kore.Sort sort, Att att) {
        return !sort.name().equals("KBoolean") ? Token.of(Sort(sort.name()), s) : Token.of(Sort("Bool"), s);
    }

    @Override
    public KApply KApply(KLabel klabel, org.kframework.kore.KList klist, Att att) {
        throw new AssertionError("Unsupported for now because KVariable is not a KLabel. See KApply1()");
    }

    /**
     * TODO: rename the method to KApply when the backend fully implements KORE
     */
    public Term KApply1(org.kframework.kore.KLabel klabel, org.kframework.kore.KList klist, Att att) {
        if (klabel.name().equals(KLabels.KREWRITE)) {
            return convertKRewrite(klabel, klist);
        }

        if (klabel.name().equals(KLabels.ML_OR)) {
            return new RuleAutomatonDisjunction(
                    klist.stream().map(k -> ((KApply) k).klist().items()).map(l -> Pair.of(convert(l.get(0)), getRuleSet((KApply) l.get(1)))).collect(Collectors.toList()),
                    global);
        }

        Term convertedKLabel = convert1(klabel);
        KList convertedKList = KList(klist.items());

        // associative operator
        if (effectivelyAssocAttributes(definition.kLabelAttributesOf(klabel.name()))) {
            // this assumes there are no KLabel variables
            BuiltinList.Builder builder = BuiltinList.builder(
                    Sort.of(module.productionsFor().get(klabel).get().head().sort().name()),
                    (KLabelConstant) convertedKLabel,
                    KLabelConstant.of(module.attributesFor().get(klabel).get().<String>get(Att.unit()).get(), global.getDefinition()),
                    global);
            // this assumes there are no KList variables in the KList
            return builder.addAll(convertedKList.getContents()).build();
        }

        Optional<String> assocKLabelForUnit = getAssocKLabelForUnit(klabel);
        if (assocKLabelForUnit.isPresent()) {
            BuiltinList.Builder builder = BuiltinList.builder(
                    //Sort.of(module.productionsFor().get(klabel).get().head().sort().name()),
                    Sort.of(stream(module.productionsFor().toStream()).filter(t -> t._1.name().equals(assocKLabelForUnit.get())).findAny().get()._2.head().sort().name()),
                    KLabelConstant.of(assocKLabelForUnit.get(), global.getDefinition()),
                    (KLabelConstant) convertedKLabel,
                    global);
            return builder.build();
        }

        if (klabel.name().equals(KLabels.KSEQ) || klabel.name().equals(KLabels.DOTK)) {
            // this assumes there are no KList variables in the KList
            return BuiltinList.kSequenceBuilder(global).addAll(convertedKList.getContents()).build();
        }

        // we've encountered a regular KApply
        BitSet[] childrenDontCareRuleMask = constructDontCareRuleMask(convertedKList);
        KItem kItem = KItem.of(convertedKLabel, convertedKList, global, childrenDontCareRuleMask == null ? null : childrenDontCareRuleMask);
        if (att.contains(Att.transition())) {
            kItem.addAttribute(Att.transition(), "");
        }
        return kItem;
    }

    private Optional<String> getAssocKLabelForUnit(KLabel klabel) {
        return definition.kLabelAttributes().entrySet().stream()
                .filter(e -> effectivelyAssocAttributes(e.getValue()) && e.getValue().getAttr(Attribute.keyOf(Att.unit())).equals(klabel.name()))
                .map(e -> e.getKey())
                .findAny();
    }

    private static boolean effectivelyAssocAttributes(Attributes attributes) {
        return attributes.containsKey(Attribute.keyOf(Att.assoc())) && !attributes.containsKey(Attribute.keyOf(Att.comm()))
                || attributes.containsKey(Attribute.keyOf(Att.bag()));
    }

    /**
     * @param convertedKList the klist of the KApply
     * @return the {@link KItem}.childrenDontCareRule mask
     */
    private BitSet[] constructDontCareRuleMask(KList convertedKList) {
        BitSet childrenDontCareRuleMask[] = null;
        if (convertedKList.stream().anyMatch(RuleAutomatonDisjunction.class::isInstance)) {
            childrenDontCareRuleMask = new BitSet[convertedKList.size()];
            for (int i = 0; i < convertedKList.size(); ++i) {
                if (convertedKList.get(i) instanceof RuleAutomatonDisjunction) {
                    BitSet the_variable = ((RuleAutomatonDisjunction) convertedKList.get(i)).getVariablesForSort(Sort.KSEQUENCE).stream()
                            .filter(p -> p.getLeft().name().equals(THE_VARIABLE)).findAny().map(Pair::getRight).orElseGet(() -> null);
                    childrenDontCareRuleMask[i] = the_variable;
                } else {
                    childrenDontCareRuleMask[i] = null;
                }
            }
        }
        return childrenDontCareRuleMask;
    }

    /**
     * Converts a KRewrite to an rule-labeled ML OR ({@link RuleAutomatonDisjunction}) on the left and
     * a {@link InnerRHSRewrite} on the right.
     *
     * @param klabel is the KRewrite
     * @param klist  contains the LHS and RHS
     * @return
     */
    private Term convertKRewrite(KLabel klabel, org.kframework.kore.KList klist) {
        K kk = klist.items().get(1);

        if (!(kk instanceof KApply))
            throw new AssertionError("k should be a KApply");

        KApply k = (KApply) kk;

        Set<KApply> orContents = getOrContents(k);

        Term[] theRHSs = new Term[this.definition.reverseRuleTable.size()];

        orContents.forEach(c -> {
            if (!c.klabel().name().equals(KLabels.ML_AND))
                throw new AssertionError("c should be an KApply AND but is " + c.klabel().name());
            K term = c.klist().items().get(0);
            Integer ruleIndex = getRuleIndex((KApply) c.klist().items().get(1));
            theRHSs[ruleIndex] = convert(term);
        });

        return KItem.of(convert1(klabel), org.kframework.backend.java.kil.KList.concatenate(convert(klist.items().get(0)), new InnerRHSRewrite(theRHSs)), global);
    }

    private BitSet getRuleSet(KApply k) {
        BitSet theRuleSetIndices = BitSet.apply(definition.reverseRuleTable.size());
        Set<KApply> rulePs = getOrContents(k);
        rulePs.stream().map(this::getRuleIndex).forEach(theRuleSetIndices::set);
        return theRuleSetIndices;
    }

    private Integer getRuleIndex(KApply kk) {
        return definition.reverseRuleTable.get(Integer.valueOf(((KToken) kk.klist().items().get(0)).s()));
    }

    private Set<KApply> getOrContents(KApply k) {
        return k.klabel().name().equals(KLabels.ML_OR) ? k.klist().items().stream().map(KApply.class::cast).collect(Collectors.toSet()) : Collections.singleton(k);
    }

    @Override
    public <KK extends org.kframework.kore.K> Term KSequence(List<KK> items, Att att) {
        KSequence.Builder builder = KSequence.builder();
        items.stream().map(this::convert).forEach(builder::concatenate);
        return builder.build();
    }

    @Override
    public Variable KVariable(String name, Att att) {
        Sort sort = Sort.of(att.<String>getOptional(Attribute.SORT_KEY).orElse("K"));
        String key = name + sort;
        if (variableTable.containsKey(key)) {
            return variableTable.get(key);
        }

        Variable var = new Variable(
                freshRules && !name.equals(THE_VARIABLE) && !name.equals(KLabels.THIS_CONFIGURATION) ? "R_" + name : name,
                sort,
                variableTable.size());
        var.setAttributes(new KOREtoKIL().convertAttributes(att));
        variableTable.put(key, var);
        return var;

    }

    @Override
    public org.kframework.kore.KRewrite KRewrite(org.kframework.kore.K left, org.kframework.kore.K right, Att att) {
        throw new AssertionError("Should not encounter a KRewrite");
    }

    @Override
    public InjectedKLabel InjectedKLabel(org.kframework.kore.KLabel klabel, Att att) {
        return new InjectedKLabel(convert1(klabel));
    }

    private Term convert1(KLabel klabel) {
        if (klabel instanceof KVariable) {
            return KVariable(klabel.name(), ((KVariable) klabel).att().add(Attribute.SORT_KEY, "KLabel"));
        } else {
            return KLabel(klabel.name());
        }
    }

    private Term CellCollection(org.kframework.kore.KLabel klabel, org.kframework.kore.KList klist) {
        final CellCollection.Builder builder = CellCollection.builder(
                definition.configurationInfo().getCellForConcat(klabel).get(),
                definition);
        Assoc.flatten(klabel, klist.items(), module).stream().forEach(k -> {
            if (k instanceof KApply) {
                builder.put(
                        CellLabel.of(((KApply) k).klabel().name()),
                        KList(((KApply) k).klist().items()));
            } else if (k instanceof KVariable) {
                // TODO(AndreiS): ensure the ... variables do not have sort K
                // assert k.att().contains(Attribute.SORT_KEY);
                builder.concatenate(new Variable(((org.kframework.kore.KVariable) k).name(), Sort.BAG));
            } else {
                assert false : "unexpected CellCollection term " + k;
            }
        });
        return builder.build();
    }

    public Term convert(org.kframework.kore.K k) {
        if (k instanceof Term)
            return (Term) k;
        else if (k instanceof org.kframework.kore.KToken)
            return KToken(((org.kframework.kore.KToken) k).s(), ((org.kframework.kore.KToken) k).sort(), k.att());
        else if (k instanceof org.kframework.kore.KApply) {
            return KApply1(((KApply) k).klabel(), ((KApply) k).klist(), k.att());
        } else if (k instanceof org.kframework.kore.KSequence)
            return KSequence(((org.kframework.kore.KSequence) k).items(), k.att());
        else if (k instanceof org.kframework.kore.KVariable)
            return KVariable(((org.kframework.kore.KVariable) k).name(), k.att());
        else if (k instanceof org.kframework.kore.InjectedKLabel)
            return InjectedKLabel(((org.kframework.kore.InjectedKLabel) k).klabel(), k.att());
        else if (k instanceof org.kframework.kore.KRewrite) {
            return KItem.of(KLabelConstant.of(KLabels.KREWRITE, definition), KList.concatenate(convert(((KRewrite) k).left()), convert(((KRewrite) k).right())), global);
        } else
            throw new AssertionError("BUM!");
    }

    public Rule convert(Optional<Module> module, org.kframework.definition.Rule rule) {
        K leftHandSide = RewriteToTop.toLeft(rule.body());
        org.kframework.kil.Rule oldRule = new org.kframework.kil.Rule();
        oldRule.setAttributes(new KOREtoKIL().convertAttributes(rule.att()));
        Location loc = rule.att().getOptional(Location.class).orElse(null);
        Source source = rule.att().getOptional(Source.class).orElse(null);
        oldRule.setLocation(loc);
        oldRule.setSource(source);

        if (module.isPresent()) {
            if (leftHandSide instanceof KApply && module.get().attributesFor().apply(((KApply) leftHandSide).klabel()).contains(Attribute.FUNCTION_KEY)) {
                oldRule.putAttribute(Attribute.FUNCTION_KEY, "");
            }
        }

        KLabelConstant matchLabel = KLabelConstant.of("#match", definition);
        KLabelConstant mapChoiceLabel = KLabelConstant.of("#mapChoice", definition);
        KLabelConstant setChoiceLabel = KLabelConstant.of("#setChoice", definition);
        KLabelConstant andLabel = KLabel("_andBool_");

        List<Term> requiresAndLookups = stream(Assoc.flatten(andLabel, Seq(rule.requires()), null))
                .map(this::convert)
                .collect(Collectors.toList());

        /* split requires clauses into matches and non-matches */
        List<Term> requires = Lists.newArrayList();
        ConjunctiveFormula lookups = ConjunctiveFormula.of(global);
        for (Term term : requiresAndLookups) {
            if (term instanceof KItem) {
                if (((KItem) term).kLabel().equals(matchLabel)) {
                    lookups = lookups.add(
                            ((KList) ((KItem) term).kList()).get(1),
                            ((KList) ((KItem) term).kList()).get(0));
                } else if (((KItem) term).kLabel().equals(setChoiceLabel)) {
                    lookups = lookups.add(
                            KItem.of(
                                    KLabelConstant.of(DataStructures.SET_CHOICE, definition),
                                    KList.singleton(((KList) ((KItem) term).kList()).get(1)),
                                    global),
                            ((KList) ((KItem) term).kList()).get(0));
                } else if (((KItem) term).kLabel().equals(mapChoiceLabel)) {
                    lookups = lookups.add(
                            KItem.of(
                                    KLabelConstant.of(DataStructures.MAP_CHOICE, definition),
                                    KList.singleton(((KList) ((KItem) term).kList()).get(1)),
                                    global),
                            ((KList) ((KItem) term).kList()).get(0));
                } else {
                    requires.add(term);
                }
            } else {
                requires.add(term);
            }
        }

        List<Term> ensures = stream(Assoc.flatten(andLabel, Seq(rule.ensures()), null))
                .map(this::convert)
                .collect(Collectors.toList());

        Rule backendKILRule = new Rule(
                "",
                convert(leftHandSide),
                convert(RewriteToTop.toRight(rule.body())),
                requires,
                ensures,
                Collections.emptySet(),
                Collections.emptySet(),
                lookups,
                false,
                null,
                null,
                null,
                null,
                oldRule,
                global);
        /* rename variables in function, anywhere, and pattern rules to avoid name conflicts
        with automaton variables and with each other */
        if (backendKILRule.containsAttribute(Attribute.FUNCTION_KEY)
                || backendKILRule.containsAttribute(Attribute.ANYWHERE_KEY)
                ||backendKILRule.containsAttribute(Attribute.PATTERN_KEY)
                || backendKILRule.containsAttribute(Attribute.PATTERN_FOLDING_KEY)) {
            backendKILRule = backendKILRule.renameVariables();
        }

        return backendKILRule;
    }

    public static ConfigurationInfo.Multiplicity kil2koreMultiplicity(Cell.Multiplicity multiplicity) {
        switch (multiplicity) {
        case ONE:
            return ConfigurationInfo.Multiplicity.ONE;
        case ANY:
        case SOME:
            return ConfigurationInfo.Multiplicity.STAR;
        case MAYBE:
            return ConfigurationInfo.Multiplicity.OPTIONAL;
        default:
            throw new IllegalArgumentException(multiplicity.toString());
        }
    }

}
