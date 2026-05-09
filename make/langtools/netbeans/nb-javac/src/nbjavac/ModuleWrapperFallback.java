/*
 * Copyright (c) 2020, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
package nbjavac;

import java.lang.reflect.Method;
import java.util.ServiceLoader;
import java.util.function.BiFunction;

final class ModuleWrapperFallback extends ModuleWrapper {
    private final Class<?> clazz;

    private ModuleWrapperFallback(Class<?> c) {
        this.clazz = c;
    }

    static BiFunction<Class<?>, ClassLoader, ModuleWrapper> factory() {
        return (clazz, loaer) -> {
            return new ModuleWrapperFallback(clazz);
        };
    }

    @Override
    public String getName() {
        switch (clazz.getName()) {
            case "jdk.javadoc.internal.api.JavadocTool": return "jdk.javadoc";
            case "com.sun.tools.javac.api.JavacTool": return "jdk.compiler";
            default: return "jdk.compiler"; //XXX
        }
    }

    @Override
    public boolean isNamed() {
        return false;
    }

    @Override
    public void addExports(String pack, ModuleWrapper to) {
    }

    @Override
    public <S> void addUses(Class<S> service) {
        if (this.clazz != null) {
            ensureUses(this.clazz, service);
        }
        ensureUses(ServiceLoader.class, service);
    }

    static void ensureUses(Class<?> thisClass, Class<?> clazz) {
        try {
            final Class<?> Module = Class.forName("java.lang.Module");
            final Method addUses = Module.getDeclaredMethod("addUses", Class.class);
            final Method getModule = Class.class.getDeclaredMethod("getModule");
            final Object thisClassModule = getModule.invoke(thisClass);
            addUses.invoke(thisClassModule, clazz);
        } catch (SecurityException | ReflectiveOperationException t) {
            //ignore - might log?
        }
    }
}
