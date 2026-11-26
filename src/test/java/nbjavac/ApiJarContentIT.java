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

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class ApiJarContentIT {

    @Test
    public void containsOnlyCompilerApiPackages() throws IOException {
        String apiJarPath = System.getProperty("nbjavac.api.jar");
        assertNotNull("Failsafe must set the API JAR path", apiJarPath);

        File apiJar = new File(apiJarPath);
        assertTrue("API JAR does not exist: " + apiJar, apiJar.isFile());

        try (JarFile jar = new JarFile(apiJar)) {
            int classEntries = 0;
            Enumeration<JarEntry> entries = jar.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (entry.isDirectory() || isJarMetadata(entry.getName())) {
                    continue;
                }

                String path = packagePath(entry.getName());
                if (path.endsWith(".class")) {
                    classEntries++;
                }
                assertTrue("Unexpected entry in API JAR: " + entry.getName(),
                        isAllowedApiEntry(path));
            }
            assertTrue("API JAR contains no classes", classEntries > 0);
        }
    }

    @Test
    public void publishesModuleJarWithMatchingCompiledClasses() throws IOException {
        String apiPath = System.getProperty("nbjavac.api.jar");
        String modulePath = System.getProperty("nbjavac.api.module.jar");
        String classesPath = System.getProperty("nbjavac.classes.dir");
        assertNotNull("Failsafe must set the API JAR path", apiPath);
        assertNotNull("Failsafe must set the API module JAR path", modulePath);
        assertNotNull("Failsafe must set the classes directory", classesPath);
        assertTrue("Published API must be the API module JAR",
                Arrays.equals(Files.readAllBytes(new File(modulePath).toPath()),
                        Files.readAllBytes(new File(apiPath).toPath())));

        try (JarFile jar = new JarFile(apiPath)) {
            Enumeration<JarEntry> entries = jar.entries();
            int classEntries = 0;
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().endsWith(".class")) {
                    continue;
                }
                File compiled = new File(classesPath, entry.getName());
                assertTrue("Missing compiled class: " + entry.getName(), compiled.isFile());
                CRC32 crc = new CRC32();
                crc.update(Files.readAllBytes(compiled.toPath()));
                assertEquals("API class differs from compiled class: " + entry.getName(),
                        entry.getCrc(), crc.getValue());
                classEntries++;
            }
            assertTrue("API JAR contains no classes", classEntries > 0);
        }
    }

    private static boolean isJarMetadata(String path) {
        return "META-INF/MANIFEST.MF".equals(path)
                || "README.md".equals(path)
                || "SECURITY.md".equals(path);
    }

    private static String packagePath(String path) {
        String prefix = "META-INF/versions/";
        if (!path.startsWith(prefix)) {
            return path;
        }

        int versionEnd = path.indexOf('/', prefix.length());
        return versionEnd < 0 ? path : path.substring(versionEnd + 1);
    }

    private static boolean isAllowedApiEntry(String path) {
        if (!path.endsWith(".class")) {
            return false;
        }

        return path.startsWith("javax/lang/")
                || path.startsWith("com/sun/source/tree/")
                || path.startsWith("com/sun/source/util/")
                || path.startsWith("com/sun/source/doctree/")
                || isDirectPackageClass(path, "javax/tools/")
                || path.startsWith("javax/annotation/processing/")
                || path.matches("jdk/internal/PreviewFeature[^/]*\\.class");
    }

    private static boolean isDirectPackageClass(String path, String packagePrefix) {
        return path.startsWith(packagePrefix)
                && path.indexOf('/', packagePrefix.length()) < 0;
    }
}
