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
package global;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaFileObject;

public class Utils {
    private Utils() {
    }

    public static List<String> asParameters(String... params) {
        final String bootPath = System.getProperty("sun.boot.class.path"); //NOI18N
        List<String> arr = new ArrayList<>();
        if (bootPath != null) {
            arr.add("-bootclasspath");
            arr.add(bootPath);
        }
        arr.addAll(Arrays.asList(params));
        return arr;
    }

    public static void collectErrorsText(DiagnosticCollector<JavaFileObject> dc, Collection<String> diagnostics) {
        for (Diagnostic<? extends JavaFileObject> d : dc.getDiagnostics()) {
            if (d.getSource() == null) {
                continue;
            }
            diagnostics.add(d.getSource().getName() + ":" + d.getStartPosition() + "-" + d.getEndPosition() + ":" + d.getCode());
        }
    }

    public static Collection<Diagnostic<? extends JavaFileObject>> filterErrors(Collection<Diagnostic<? extends JavaFileObject>> diagnostics) {
        List<Diagnostic<? extends JavaFileObject>> arr = new ArrayList<>();
        for (Diagnostic<? extends JavaFileObject> d : diagnostics) {
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                arr.add(d);
            }
        }
        return arr;
    }
}
