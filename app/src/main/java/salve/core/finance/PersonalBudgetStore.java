package salve.core.finance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;

/** One local budget file. Caller places it in Android's no-backup application directory. */
public final class PersonalBudgetStore {
    private static final int MAX_BYTES = 128_000;
    private final File file;
    public PersonalBudgetStore(File file) { this.file=file; }
    public PersonalBudget load() throws IOException {
        if (!file.exists()) return new PersonalBudget();
        if (!file.isFile() || file.length()>MAX_BYTES) throw new IOException("Presupuesto ilegible.");
        byte[] data;
        try (java.io.InputStream input=Files.newInputStream(file.toPath());java.io.ByteArrayOutputStream out=new java.io.ByteArrayOutputStream()) {
            byte[] buffer=new byte[4096];int n;
            while((n=input.read(buffer))!=-1){if(out.size()+n>MAX_BYTES)throw new IOException("Presupuesto demasiado grande.");out.write(buffer,0,n);}
            data=out.toByteArray();
        }
        try { return PersonalBudget.fromJson(StandardCharsets.UTF_8.newDecoder().decode(ByteBuffer.wrap(data)).toString()); }
        catch(IllegalArgumentException invalid){throw new IOException("No se pudo leer el presupuesto guardado.",invalid);}
    }
    public void save(PersonalBudget budget) throws IOException {
        byte[] bytes=budget.toJson().getBytes(StandardCharsets.UTF_8);
        if(bytes.length>MAX_BYTES)throw new IOException("Presupuesto demasiado grande.");
        File parent=file.getAbsoluteFile().getParentFile();
        if(!parent.isDirectory()&&!parent.mkdirs())throw new IOException("No se pudo preparar el almacenamiento.");
        File temporary=new File(parent,file.getName()+".pending");
        try {
            try(FileOutputStream out=new FileOutputStream(temporary)){out.write(bytes);out.getFD().sync();}
            // A failed atomic replacement keeps the previous budget; do not overwrite in place.
            Files.move(temporary.toPath(),file.toPath(),StandardCopyOption.ATOMIC_MOVE,StandardCopyOption.REPLACE_EXISTING);
        } finally {Files.deleteIfExists(temporary.toPath());}
    }
    public void clear() throws IOException {
        Files.deleteIfExists(file.toPath());
        Files.deleteIfExists(new File(file.getAbsoluteFile().getParentFile(),file.getName()+".pending").toPath());
    }
}
