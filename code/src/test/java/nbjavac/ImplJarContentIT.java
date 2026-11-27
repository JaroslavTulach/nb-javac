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
import java.nio.file.Files;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.zip.CRC32;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ImplJarContentIT {

    @Test
    public void containsOnlyHelpersForEachRuntime() throws IOException {
        checkHelpers("nbjavac.implementation.jar", true);
        checkHelpers("nbjavac.implementation21.jar", false);
    }

    private void checkHelpers(String property, boolean jdk8Profile) throws IOException {
        String jarPath = System.getProperty(property);
        assertNotNull("Failsafe must set " + property, jarPath);
        File implJar = new File(jarPath);
        assertTrue("Implementation JAR does not exist: " + implJar, implJar.isFile());

        try (JarFile jar = new JarFile(implJar)) {
            assertTrue("JDK 21 helpers are missing",
                    jar.getEntry("nbjavac/jdk21/JmodFileWrapper.class") != null
                    && jar.getEntry("nbjavac/jdk21/JDK21VMWrapper.class") != null
                    && jar.getEntry("nbjavac/jdk21/ExactConversionsSupport.class") != null);
            assertEquals("JDK 8 compatibility helpers should match the selected profile",
                    jdk8Profile, jar.getEntry("nbjavac/jdk8/StringWrapper.class") != null);

            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                String path = entry.getName();
                if (entry.isDirectory() || !path.startsWith("nbjavac/") || !path.endsWith(".class")) {
                    continue;
                }

                assertTrue("Unexpected helper in implementation JAR: " + path,
                        path.startsWith("nbjavac/jdk21/")
                        || path.startsWith("nbjavac/sun/annotation/")
                        || (jdk8Profile && path.startsWith("nbjavac/jdk8/")));
            }
        }
    }

    @Test
    public void containsAllCtSymData() throws IOException {
        checkCtSym("nbjavac.implementation.jar");
        checkCtSym("nbjavac.implementation21.jar");
    }

    private void checkCtSym(String property) throws IOException {
        String implPath = System.getProperty(property);
        String ctSymPath = System.getProperty("nbjavac.ctsym.jar");
        assertNotNull("Failsafe must set the implementation JAR path", implPath);
        assertNotNull("Failsafe must set the ct.sym JAR path", ctSymPath);

        try (JarFile impl = new JarFile(implPath); JarFile ctSym = new JarFile(ctSymPath)) {
            int ctSymEntries = 0;
            Enumeration<JarEntry> entries = ctSym.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || !entry.getName().startsWith("META-INF/ct")) {
                    continue;
                }
                JarEntry bundled = impl.getJarEntry(entry.getName());
                assertNotNull("Missing ct.sym entry: " + entry.getName(), bundled);
                assertEquals("Changed ct.sym entry: " + entry.getName(), entry.getCrc(), bundled.getCrc());
                assertEquals("Changed ct.sym entry size: " + entry.getName(), entry.getSize(), bundled.getSize());
                ctSymEntries++;
            }
            assertTrue("ct.sym JAR contains no symbol data", ctSymEntries > 0);
            int bundledEntries = 0;
            entries = impl.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && entry.getName().startsWith("META-INF/ct")) {
                    bundledEntries++;
                }
            }
            assertEquals("Unexpected bundled ct.sym entries", ctSymEntries, bundledEntries);
        }
    }

    @Test
    public void republishesRuntimeArtifacts() throws IOException {
        String runtime8 = System.getProperty("nbjavac.runtime8.jar");
        String runtime21 = System.getProperty("nbjavac.runtime21.jar");
        String impl8 = System.getProperty("nbjavac.implementation.jar");
        String impl21 = System.getProperty("nbjavac.implementation21.jar");
        assertNotNull(runtime8);
        assertNotNull(runtime21);
        assertNotNull(impl8);
        assertNotNull(impl21);
        assertTrue("JDK 21 implementation must be the runtime module JAR",
                Arrays.equals(Files.readAllBytes(new File(runtime21).toPath()),
                        Files.readAllBytes(new File(impl21).toPath())));
        try (JarFile source = new JarFile(runtime8); JarFile repackaged = new JarFile(impl8)) {
            int sourceEntries = 0;
            Enumeration<JarEntry> entries = source.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || "META-INF/MANIFEST.MF".equals(entry.getName())) {
                    continue;
                }
                JarEntry copy = repackaged.getJarEntry(entry.getName());
                assertNotNull("Missing implementation entry: " + entry.getName(), copy);
                assertEquals("Changed implementation entry: " + entry.getName(), entry.getCrc(), copy.getCrc());
                sourceEntries++;
            }
            int publishedEntries = 0;
            entries = repackaged.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.isDirectory() && !"META-INF/MANIFEST.MF".equals(entry.getName())) {
                    publishedEntries++;
                }

            }
            assertEquals("Unexpected implementation entries", sourceEntries, publishedEntries);
        }
    }

    @Test
    public void sourcesMatchEachRuntime() throws IOException {
        checkSources("nbjavac.sources8.jar", "nbjavac.generated8.dir");
        checkSources("nbjavac.sources21.jar", "nbjavac.generated21.dir");
    }

    private void checkSources(String jarProperty, String sourceProperty) throws IOException {
        String jarPath = System.getProperty(jarProperty);
        String sourcePath = System.getProperty(sourceProperty);
        assertNotNull(jarPath);
        assertNotNull(sourcePath);
        String source = "java.compiler/share/classes/javax/tools/ToolProvider.java";
        CRC32 crc = new CRC32();
        crc.update(Files.readAllBytes(new File(sourcePath, source).toPath()));
        try (JarFile jar = new JarFile(jarPath)) {
            JarEntry entry = jar.getJarEntry("src/generated/java/" + source);
            assertNotNull("Missing generated source in " + jarPath, entry);
            assertEquals("Generated source does not match " + sourceProperty, crc.getValue(), entry.getCrc());
        }
    }
}
