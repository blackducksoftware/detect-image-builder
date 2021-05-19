import java.io.File;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import com.synopsys.integration.detect.imagebuilder.DownloadFailedException;
import com.synopsys.integration.detect.imagebuilder.DetectDownloader;

class DownloaderTest {
    private String scriptsPath = "src/main/resources/scripts";
    @Test
    void testDownloadDetectDownload() {
        File downloadDest = new File(String.format("test/%s", "DETECT_FILES"));
        try {
            DetectDownloader detectDownloader = new DetectDownloader();
            detectDownloader.downloadFiles("6.8.0", downloadDest.getAbsolutePath(), true, scriptsPath);
        } catch (DownloadFailedException e) {
            Assertions.fail();
        }
    }
}
