// Copyright (c) 2015-2016 K Team. All Rights Reserved.

package org.kframework.kore.compile;

import org.junit.Test;
import org.kframework.utils.KoreUtils;

import java.net.URISyntaxException;

public class MergeRulesTest {
    @Test
    public void firstTest() throws URISyntaxException {
        String filename = "/convertor-tests/kore_imp.k";
        KoreUtils utils = new KoreUtils(filename, "IMP", "IMP-SYNTAX", false);
    }
}
