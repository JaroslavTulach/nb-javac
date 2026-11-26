import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.classfile.ClassBuilder;
import java.lang.classfile.ClassElement;
import java.lang.classfile.ClassFile;
import java.lang.classfile.ClassModel;
import java.lang.classfile.ClassTransform;
import java.lang.classfile.attribute.ModuleHashesAttribute;
import java.util.jar.JarInputStream;
import java.util.jar.JarOutputStream;
import java.util.zip.ZipEntry;

public class StripModuleHashes {

    public static void main(String[] args) throws Exception {
        try (InputStream in = new FileInputStream(args[0]);
             OutputStream out = new FileOutputStream(args[1])) {
            out.write(in.read());//J
            out.write(in.read());//M
            out.write(in.read());//major version
            out.write(in.read());//minor version
            try (JarInputStream jis = new JarInputStream(in);
                 JarOutputStream jos = new JarOutputStream(out)) {
                ZipEntry e;
                while ((e = jis.getNextEntry()) != null) {
                    jos.putNextEntry(e);

                    if (e.getName().endsWith("/module-info.class")) {
                        ClassModel cf = ClassFile.of().parse(jis.readAllBytes());
                        jos.write(ClassFile.of().transformClass(cf, new ClassTransform() {
                            @Override
                            public void accept(ClassBuilder builder, ClassElement element) {
                                if (element instanceof ModuleHashesAttribute) {
                                    //ignore
                                } else {
                                    builder.accept(element);
                                }
                            }
                        }));
                    } else {
                        jis.transferTo(jos);
                    }
                }
            }
        }
    }

}
