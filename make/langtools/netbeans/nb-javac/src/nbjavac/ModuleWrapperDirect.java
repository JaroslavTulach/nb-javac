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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Objects;
import java.util.function.BiFunction;

final class ModuleWrapperDirect extends ModuleWrapper {
    // methods for reflective access to JDK9+ API
    private static Method getModuleForClass;
    private static Method getUnnamedModuleForLoader;
    private static Method getNameForModule;
    private static Method isNamedForModule;
    private static Method addUsesForModule;
    private static Method addExportsForModule;

    /** instance of java.lang.Module */
    private final Object module;

    private ModuleWrapperDirect(Object module) {
        this.module = module;
    }


    static BiFunction<Class<?>, ClassLoader, ModuleWrapper> factory() throws ReflectiveOperationException {
        Class<?> moduleClass = Class.forName("java.lang.Module");
        Objects.requireNonNull(moduleClass);
        getModuleForClass = Class.class.getMethod("getModule");
        getUnnamedModuleForLoader = ClassLoader.class.getMethod("getUnnamedModule");
        getNameForModule = moduleClass.getMethod("getName");
        isNamedForModule = moduleClass.getMethod("isNamed");
        addUsesForModule = moduleClass.getMethod("addUses", Class.class);
        addExportsForModule = moduleClass.getMethod("addExports", String.class, moduleClass);
        return (clazz, loader) -> {
            try {
                Object module;
                if (clazz != null) {
                    module = getModuleForClass.invoke(clazz);
                } else {
                    module = getUnnamedModuleForLoader.invoke(loader);
                }
                return new ModuleWrapperDirect(module);
            } catch (ReflectiveOperationException ex) {
                throw new IllegalStateException(ex);
            }
        };
    }

    @Override
    public String getName() {
        try {
            return (String) getNameForModule.invoke(module);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public boolean isNamed() {
        try {
            return (Boolean) isNamedForModule.invoke(module);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public void addExports(String pack, ModuleWrapper to) {
        Object toModule = ((ModuleWrapperDirect)to).module;
        try {
            addExportsForModule.invoke(module, pack, toModule);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException(ex);
        }
    }

    @Override
    public <S> void addUses(Class<S> service) {
        try {
            addUsesForModule.invoke(module, service);
        } catch (IllegalAccessException | InvocationTargetException ex) {
            throw new IllegalStateException(ex);
        }
    }
}
