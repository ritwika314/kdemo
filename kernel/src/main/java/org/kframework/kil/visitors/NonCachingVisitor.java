// Copyright (c) 2014-2016 K Team. All Rights Reserved.
package org.kframework.kil.visitors;

import org.kframework.kil.loader.Context;

public class NonCachingVisitor extends BasicVisitor {

    public NonCachingVisitor(Context context) {
        super(context);
    }

    @Override
    public boolean cache() {
        return false;
    }
}
