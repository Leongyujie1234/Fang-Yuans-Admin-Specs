/*
 * Decompiled with CFR 0.152.
 */
package com.adminspec.spec;

import com.adminspec.spec.Spec;
import com.adminspec.spec.dio.DioSpec;
import com.adminspec.spec.guyue.LiuGuanYiSpec;
import java.util.HashMap;
import java.util.Map;

public final class SpecRegistry {
    private static final Map<String, Spec> SPECS = new HashMap<String, Spec>();

    private SpecRegistry() {
    }

    public static void register(Spec spec) {
        if (SPECS.containsKey(spec.id())) {
            throw new IllegalStateException("Duplicate spec id: " + spec.id());
        }
        SPECS.put(spec.id(), spec);
    }

    public static Spec get(String id) {
        return SPECS.get(id);
    }

    public static int size() {
        return SPECS.size();
    }

    public static Iterable<Spec> all() {
        return SPECS.values();
    }

    public static void registerDefaults() {
        LiuGuanYiSpec.register();
        DioSpec.register();
    }
}

