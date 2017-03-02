// Copyright (c) 2014-2016 K Team. All Rights Reserved.
package org.kframework.backend.java.symbolic;

import org.kframework.backend.java.indexing.IndexingAlgorithm;
import org.kframework.utils.inject.RequestScoped;
import org.kframework.utils.options.BaseEnumConverter;

import com.beust.jcommander.Parameter;

@RequestScoped
public final class JavaExecutionOptions {

    @Parameter(names="--deterministic-functions", description="Throw assertion failure during "
        + "execution in the java backend if function definitions are not deterministic.")
    public boolean deterministicFunctions = false;

    @Parameter(names="--symbolic-execution", description="Use unification rather than "
        + "pattern matching to drive rewriting in the Java backend.")
    public boolean symbolicExecution = false;

    @Parameter(names="--rule-index", converter=RuleIndexConveter.class, description="Choose a technique for indexing the rules. <rule-index> is one of [table]. (Default: table). This only has effect with '--backend java'.")
    public IndexingAlgorithm ruleIndex = IndexingAlgorithm.RULE_TABLE;

    @Parameter(names="--audit-file", description="Enforce that the rule applied at the step specified by "
            + "--apply-step is a rule at the specified file and line, or fail with an error explaining why "
            + "the rule did not apply.")
    public String auditingFile;

    @Parameter(names="--audit-line", description="Enforce that the rule applied at the step specified by "
            + "--apply-step is a rule at the specified file and line, or fail with an error explaining why "
            + "the rule did not apply.")
    public Integer auditingLine;

    @Parameter(names="--audit-step", description="Enforce that the rule applied at the specified step is a rule "
            + "tagged with the value of --apply-tag, or fail with an error explaining why the rule did not apply.")
    public Integer auditingStep;

    public static class RuleIndexConveter extends BaseEnumConverter<IndexingAlgorithm> {

        public RuleIndexConveter(String optionName) {
            super(optionName);
        }

        @Override
        public Class<IndexingAlgorithm> enumClass() {
            return IndexingAlgorithm.class;
        }
    }
}

