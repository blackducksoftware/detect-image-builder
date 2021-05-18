import static org.junit.jupiter.params.provider.Arguments.arguments;

import java.io.File;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import com.synopsys.integration.detect.imagebuilder.DownloadFailedException;
import com.synopsys.integration.detect.imagebuilder.download.DetectDownloader;
import com.synopsys.integration.detect.imagebuilder.download.Downloader;

class DownloaderTest {
    private String scriptsPath = "src/main/resources/scripts";
    @ParameterizedTest
    @MethodSource("downloaderTestInputsProvider")
    void testDownloads(Downloader downloader, String downloadDir, String version) {
        File downloadDest = new File(String.format("test/%s", downloadDir));
        try {
            downloader.downloadFiles(version, downloadDest.getAbsolutePath(), true, scriptsPath);
        } catch (DownloadFailedException e) {
            Assertions.fail();
        }
    }

    static Stream<Arguments> downloaderTestInputsProvider() {
        return Stream.of(
            arguments(new DetectDownloader(), "DETECT_FILES", "6.8.0"),
            arguments(new GradleDownloader(), "PKG_MGR_FILES/gradle", "6.8.2"),
            arguments(new MavenDownloader(), "PKG_MGR_FILES/maven", "3.6.3")
        );
    }
}
