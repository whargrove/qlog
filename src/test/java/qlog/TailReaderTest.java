package qlog;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

public class TailReaderTest implements WithAssertions {

    @Test
    void returnsLastNLinesInFile() {
        var file = getPathToResource("macbeth.txt");
        var n = 3;
        List<String> lastNLines = TailReader.getLastNLines(file, n);

        assertThat(lastNLines)
                .as("The last N lines were read from the file.")
                .hasSize(n);
        var expected = List.of("Signifying nothing.",
                "Told by an idiot, full of sound and fury,",
                "And then is heard no more. It is a tale");
        assertThat(lastNLines)
                .as("The last N lines contains lines ordered newest to oldest as in `tail -r file`.")
                .containsExactlyElementsOf(expected);
    }

    @Test
    void fileLargerThanBufferReturnsLastNLinesInFile() throws IOException {
        var path = getPathToResource("128k_access.log");
        var n = 5;
        List<String> lastNLines = TailReader.getLastNLines(path, n);

        assertThat(lastNLines)
                .as("The last N lines were read from the file.")
                .hasSize(n);
        long count = Files.lines(path).count();
        var expected = Files.lines(path).skip(count - n).toList().reversed();
        assertThat(lastNLines)
                .as("The last N lines contains lines ordered newest to oldest as in `tail -r file`.")
                .containsExactlyElementsOf(expected);
    }

    @Test
    void largeCountOfFilesCollectsLinesAcrossChunkBoundaries() throws IOException {
        var path = getPathToResource("128k_access.log");
        var n = 250;
        List<String> lastNLines = TailReader.getLastNLines(path, n);

        assertThat(lastNLines)
                .as("The last N lines were read from the file.")
                .hasSize(n);
        long count = Files.lines(path).count();
        var expected = Files.lines(path).skip(count - n).toList().reversed();
        assertThat(lastNLines)
                .as("The last N lines contains lines ordered newest to oldest as in `tail -r file`.")
                .containsExactlyElementsOf(expected);
    }

    @Test
    void readingAllOfTheLinesInAFileReturnsTheLinesInReversedOrder() throws IOException {
        var path = getPathToResource("128k_access.log");
        var n = (int) Files.lines(path).count(); // all lines
        List<String> lastNLines = TailReader.getLastNLines(path, n);

        assertThat(lastNLines)
                .as("The last N lines were read from the file.")
                .hasSize(n);
        var expected = Files.lines(path).toList().reversed();
        assertThat(lastNLines)
                .as("The last N lines contains lines ordered newest to oldest as in `tail -r file`.")
                .containsExactlyElementsOf(expected);
    }

    @SuppressWarnings("SameParameterValue")
    private Path getPathToResource(String fileName) {
        var resourceURL = getClass().getClassLoader().getResource(fileName);
        if (resourceURL == null) {
            throw new IllegalArgumentException("File not found: " + fileName);
        }
        try {
            return Paths.get(resourceURL.toURI());
        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }
    }

}
