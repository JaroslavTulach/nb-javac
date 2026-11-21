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
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class ImplJarContentIT {

    @Test
    public void containsOnlyHelpersForSelectedProfile() throws IOException {
        String profile = System.getProperty("nbjavac.profile");
        assertNotNull("Failsafe must set the build profile", profile);
        assertTrue("Unexpected build profile: " + profile,
                "jdk8+".equals(profile) || "jdk21+".equals(profile));

        String jarPath = System.getProperty("nbjavac.implementation.jar");
        assertNotNull("Failsafe must set the implementation JAR path", jarPath);
        File implJar = new File(jarPath);
        assertTrue("Implementation JAR does not exist: " + implJar, implJar.isFile());

        boolean jdk8Profile = "jdk8+".equals(profile);
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
}
