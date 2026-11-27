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

import com.sun.tools.javac.Main;
import global.ap1.ModuleCheckingAP;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.net.URL;
import junit.framework.TestCase;

public class ModuleCheckingTest extends TestCase {
    public void testModuleElementNamesMatcheModuleNames() throws IOException {
        File sourceOutput = File.createTempFile("ModuleNames", "");
        sourceOutput.delete();
        assertTrue(sourceOutput.mkdirs());

        final String version = System.getProperty("java.vm.specification.version");
        URL myself = ModuleCheckingTest.class.getProtectionDomain().getCodeSource().getLocation();
        Main.compile(Utils.asParameters(
            "-source", version,
            "-target", version,
            "-classpath", myself.toExternalForm(),
            "-processor", ModuleCheckingAP.class.getName(),
            "-s", sourceOutput.getAbsolutePath(),
            "java.lang.String"
        ).toArray(new String[0]));

        final File file = new File(sourceOutput, "ModuleNames.txt");
        assertTrue(file.canRead());
        try (BufferedReader r = new BufferedReader(new FileReader(file))) {
            assertEquals("Generated file " + file + " contains", "java.lang.SuppressWarnings", r.readLine());
        }
        file.delete();
        sourceOutput.delete();
    }
}
