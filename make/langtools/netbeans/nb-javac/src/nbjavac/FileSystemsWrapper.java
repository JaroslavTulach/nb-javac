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

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.util.Map;

public class FileSystemsWrapper {

    private static final Method newFS13;

    static {
        Method m = null;
        try {
            // JDK 13 API
            m = FileSystems.class.getDeclaredMethod("newFileSystem", Path.class, Map.class, ClassLoader.class);
        } catch (NoSuchMethodException ignore) {}        
        newFS13 = m;
    }

    // avoids using the URI variants due to different semantics
    public static FileSystem newFileSystem(Path path, Map<String,?> env, ClassLoader loader) throws IOException {
        if (newFS13 != null) {
            try {
                return (FileSystem) newFS13.invoke(null, path, env, loader);
            } catch (IllegalAccessException ex) {
                throw new IllegalStateException();
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause instanceof IOException) {
                    throw (IOException) cause;
                } else {
                    throw new RuntimeException(ex);
                }
            }
        } else {
            // fallback without env, usage in JavacFileManager#ArchiveContainer only sets "zipinfo-time" to "false".
            return FileSystems.newFileSystem(path, loader);
        }
    }
}
