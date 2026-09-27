package salve.data.sync;

import java.io.File;

/** Remote storage abstraction. A successful upload must mean the provider verified it remotely. */
public interface CloudProvider {
    boolean isConfigured();
    boolean uploadJson(String remotePath, String jsonPayload);
    boolean uploadFile(String remotePath, File file);
    byte[] download(String remotePath);
}
