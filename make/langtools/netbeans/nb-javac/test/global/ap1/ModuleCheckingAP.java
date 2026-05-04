/*
 * Copyright 2003-2004 Sun Microsystems, Inc.  All Rights Reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Sun designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Sun in the LICENSE file that accompanied this code.
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
 * Please contact Sun Microsystems, Inc., 4150 Network Circle, Santa Clara,
 * CA 95054 USA or visit www.sun.com if you need additional information or
 * have any questions.
 */

package global.ap1;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ModuleElement;
import javax.lang.model.element.QualifiedNameable;
import javax.lang.model.element.TypeElement;
import javax.lang.model.util.Elements;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import nbjavac.ModuleWrapper;
import org.junit.Assert;
import static org.junit.Assert.assertEquals;

@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("*")
public final class ModuleCheckingAP extends AbstractProcessor {

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        try (
            Writer w = processingEnv.getFiler().createResource(StandardLocation.SOURCE_OUTPUT, "", "ModuleNames.txt").openWriter();
        ) {
            assertModules(w, SuppressWarnings.class);
        } catch (ReflectiveOperationException | IOException ex) {
            ex.printStackTrace();
        }
        return false;
    }

    private void assertModules(Writer w, Class<?> clazz) throws IOException, ReflectiveOperationException {
        QualifiedNameable e1, e2;
        String moduleName;
        try {
            moduleName = moduleFor(clazz);
            Assert.assertNotNull("Module for " + clazz + " found", moduleName);
            // use real module names on JDK9+
            e1 = moduleElementFor(clazz);
            e2 = moduleElementViaWrapperFor(clazz);
        } catch (NoSuchMethodException ex) {
            // aproximations on JDK8...
            moduleName = null;
            e1 = moduleElementFor(clazz);
            e2 = moduleElementViaWrapperFor(clazz);
        }
        assertEquals("Same module via different methods for " + clazz, e1, e2);
        if (moduleName != null) {
            assertEquals("Module name via element must match runtime name", moduleName, e1.getQualifiedName().toString());
            assertEquals("ModuleWrapper name must much runtime name", moduleName, e2.getQualifiedName().toString());
        }
        // write the output
        w.append(clazz.getName() + "\n");
    }
    
    private static String moduleFor(Class<?> clazz) throws NoSuchMethodException {
        try {
            Method getModule = Class.class.getMethod("getModule");
            Object module = getModule.invoke(clazz);
            Method getName = module.getClass().getMethod("getName");
            return (String) getName.invoke(module);
        } catch (NoSuchMethodException ex) {
            throw ex;
        } catch (ClassCastException | ReflectiveOperationException ex) {
            return ex.toString();
        }
    }

    private QualifiedNameable moduleElementFor(Class<?> clazz) throws ReflectiveOperationException {
        final Elements eu = processingEnv.getElementUtils();
        TypeElement element = eu.getTypeElement(clazz.getName());
        Method getModuleOf = eu.getClass().getMethod("getModuleOf", Element.class);
        QualifiedNameable module = (QualifiedNameable) getModuleOf.invoke(eu, element);
        return module;
    }
    
    private QualifiedNameable moduleElementViaWrapperFor(Class<?> clazz) throws ReflectiveOperationException {
        final Elements eu = processingEnv.getElementUtils();
        ModuleWrapper module = ModuleWrapper.getModule(clazz);
        Method getModuleElement = eu.getClass().getMethod("getModuleElement", CharSequence.class);
        QualifiedNameable element = (QualifiedNameable) getModuleElement.invoke(eu, module.getName());
        return element;
    }
}
