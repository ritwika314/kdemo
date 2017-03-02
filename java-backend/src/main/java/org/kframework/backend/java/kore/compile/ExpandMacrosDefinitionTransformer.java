// Copyright (c) 2015-2016 K Team. All Rights Reserved.

package org.kframework.backend.java.kore.compile;

import static org.kframework.Collections.*;

import org.kframework.Collections;
import org.kframework.definition.Definition;
import org.kframework.definition.ModuleTransformer;
import org.kframework.definition.SelectiveDefinitionTransformer;
import org.kframework.kompile.KompileOptions;
import org.kframework.main.GlobalOptions;
import org.kframework.utils.errorsystem.KExceptionManager;
import org.kframework.utils.file.FileUtil;

import java.util.function.Function;

public class ExpandMacrosDefinitionTransformer implements Function<Definition, Definition> {

    private final KExceptionManager kem;
    private final FileUtil files;
    private final GlobalOptions globalOptions;
    private final KompileOptions kompileOptions;

    public ExpandMacrosDefinitionTransformer(KExceptionManager kem, FileUtil files, GlobalOptions globalOptions, KompileOptions kompileOptions) {
        this.kem = kem;
        this.files = files;
        this.globalOptions = globalOptions;
        this.kompileOptions = kompileOptions;
    }

    @Override
    public Definition apply(Definition definition) {
        ExpandMacros macroExpander = new ExpandMacros(definition.mainModule(), kem, files, globalOptions, kompileOptions);
        ModuleTransformer expandMacros = ModuleTransformer.fromSentenceTransformer(macroExpander::expand, "expand macro rules");
        return new SelectiveDefinitionTransformer(expandMacros).apply(definition);
    }
}
