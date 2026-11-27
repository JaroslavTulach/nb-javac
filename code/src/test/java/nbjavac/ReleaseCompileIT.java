/*
 * Copyright 2026 nb-javac contributors.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation, with the Classpath exception
 * as stated in LICENSE.txt.
 */
package nbjavac;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import javax.tools.Diagnostic;
import javax.tools.DiagnosticCollector;
import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * Verifies that the published implementation JAR carries a ct.sym with the
 * newest JDK API. The check runs in a separate JVM where only nb-javac provides
 * the compiler, so that the host JDK API cannot interfere.
 */
public class ReleaseCompileIT {

    // Objects.hasIdentity was added in JDK 28 as a preview API
    private static final String JDK28_API =
        "package test; public class Test { boolean f(Object o) { return java.util.Objects.hasIdentity(o); } }";

    @Test
    public void compilesAgainstJdk28Api() throws Exception {
        run("nbjavac.implementation.jar");
        run("nbjavac.implementation21.jar");
    }

    private static void run(String implProperty) throws Exception {
        String impl = System.getProperty(implProperty);
        String api = System.getProperty("nbjavac.api.jar");
        String tests = System.getProperty("nbjavac.test.classes");
        assertNotNull(impl);
        assertNotNull(api);
        assertNotNull(tests);

        String sep = File.pathSeparator;
        List<String> cmd = new ArrayList<>();
        cmd.add(new File(System.getProperty("java.home"), "bin/java").getPath());
        if (System.getProperty("java.specification.version").startsWith("1.")) {
            cmd.add("-Xbootclasspath/p:" + api + sep + impl);
        } else {
            cmd.add("--limit-modules");
            cmd.add("java.base,java.xml,jdk.zipfs,java.management,java.logging");
        }
        cmd.add("-cp");
        cmd.add(api + sep + impl + sep + tests);
        cmd.add(ReleaseCompileIT.class.getName());

        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        java.io.ByteArrayOutputStream out = new java.io.ByteArrayOutputStream();
        byte[] buf = new byte[4096];
        for (int n; (n = p.getInputStream().read(buf)) >= 0;) {
            out.write(buf, 0, n);
        }
        assertEquals(out.toString(), 0, p.waitFor());
    }

    // Runs in the child JVM
    public static void main(String... args) throws IOException {
        if (errors("28", true, JDK28_API) != 0) {
            fail("--release 28 must see the JDK 28 API");
        }
        if (errors("27", false, JDK28_API) == 0) {
            fail("--release 27 must not see the JDK 28 API");
        }
    }

    private static void fail(String msg) {
        System.err.println(msg);
        System.exit(1);
    }

    private static int errors(String release, boolean preview, String code) throws IOException {
        JavaCompiler tool = ToolProvider.getSystemJavaCompiler();
        if (!tool.getClass().getProtectionDomain().getCodeSource().getLocation().getPath().endsWith(".jar")) {
            fail("nb-javac is not the active compiler: " + tool);
        }
        DiagnosticCollector<JavaFileObject> diags = new DiagnosticCollector<>();
        List<String> options = new ArrayList<>(Arrays.asList("--release", release));
        if (preview) {
            options.add("--enable-preview");
        }
        JavaFileObject file = new SimpleJavaFileObject(URI.create("string:///test/Test.java"), JavaFileObject.Kind.SOURCE) {
            @Override
            public CharSequence getCharContent(boolean ignoreEncodingErrors) {
                return code;
            }
        };
        ((com.sun.source.util.JavacTask) tool.getTask(null, null, diags, options, null, Arrays.asList(file))).analyze();
        int count = 0;
        for (Diagnostic<?> d : diags.getDiagnostics()) {
            System.err.println(release + ": " + d);
            if (d.getKind() == Diagnostic.Kind.ERROR) {
                count++;
            }
        }
        return count;
    }
}
