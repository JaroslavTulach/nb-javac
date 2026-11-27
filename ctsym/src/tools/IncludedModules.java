import java.io.IOException;
import java.lang.module.ModuleDescriptor;
import java.lang.module.ModuleDescriptor.Requires;
import java.lang.module.ModuleFinder;
import java.lang.module.ModuleReference;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Computes the modules that belong into {@code ct.sym} the same way as the JDK
 * build does: the {@code DOCS_MODULES} roots from {@code docs-modules.conf},
 * everything they transitively require, plus {@code jdk.unsupported}.
 * Run it on the JDK whose modules are being described.
 *
 * <pre>java IncludedModules.java &lt;docs-modules.conf&gt; &lt;output-file&gt;</pre>
 */
public class IncludedModules {
    public static void main(String[] args) throws IOException {
        String conf = Files.readString(Path.of(args[0]));
        Matcher roots = Pattern.compile("DOCS_MODULES=((?:\\s*\\\\\\s*[\\w.]+)+)").matcher(conf);
        if (!roots.find()) {
            throw new IllegalStateException("No DOCS_MODULES in " + args[0]);
        }
        Map<String, ModuleReference> system = new java.util.HashMap<>();
        ModuleFinder.ofSystem().findAll().forEach(r -> system.put(r.descriptor().name(), r));

        TreeSet<String> result = new TreeSet<>();
        Deque<String> pending = new ArrayDeque<>();
        pending.add("java.base");
        pending.add("jdk.unsupported");
        for (String root : roots.group(1).split("[\\s\\\\]+")) {
            if (!root.isEmpty()) {
                pending.add(root);
            }
        }
        while (!pending.isEmpty()) {
            String name = pending.remove();
            if (!result.add(name)) {
                continue;
            }
            ModuleReference ref = system.get(name);
            if (ref == null) {
                throw new IllegalStateException("Module " + name + " is not part of this JDK");
            }
            for (Requires r : ref.descriptor().requires()) {
                if (r.modifiers().contains(Requires.Modifier.TRANSITIVE)) {
                    pending.add(r.name());
                }
            }
        }
        Files.writeString(Path.of(args[1]), String.join(" ", result) + System.lineSeparator());
    }
}
