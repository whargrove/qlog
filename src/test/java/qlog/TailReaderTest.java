package qlog;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.awaitility.Awaitility.await;

public class TailReaderTest implements WithAssertions {

    @Test
    void returnsLastNLinesInFile() {
        var file = getPathToResource("macbeth.txt");
        var n = 3;
        List<String> lastNLines = new TailReaderImpl(65536).getLastNLines(file, null, 0, n);

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
        List<String> lastNLines = new TailReaderImpl(65536).getLastNLines(path, null, 0, n);

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
        List<String> lastNLines = new TailReaderImpl(65536).getLastNLines(path, null, 0, n);

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
        List<String> lastNLines = new TailReaderImpl(65536).getLastNLines(path, null, 0, n);

        assertThat(lastNLines)
                .as("The last N lines were read from the file.")
                .hasSize(n);
        var expected = Files.lines(path).toList().reversed();
        assertThat(lastNLines)
                .as("The last N lines contains lines ordered newest to oldest as in `tail -r file`.")
                .containsExactlyElementsOf(expected);
    }

    @Test
    void onlyLinesMatchingFilterAreReturned() {
        var path = getPathToResource("macbeth.txt");
        List<String> actual = new TailReaderImpl(65536).getLastNLines(path, "tomorrow", 0, 1);
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsOf(List.of("Tomorrow, and tomorrow, and tomorrow,"));
    }

    @Test
    void multipleLinesMatchingFilterAreReturned() {
        var path = getPathToResource("macbeth.txt");
        List<String> actual = new TailReaderImpl(65536).getLastNLines(path, ",", 0, 3);
        assertThat(actual).hasSize(3);
        assertThat(actual)
                .containsExactlyElementsOf(List.of(
                        "Told by an idiot, full of sound and fury,",
                        "That struts and frets his hour upon the stage,",
                        "Life's but a walking shadow, a poor player,"));
    }

    @Test
    void testOneLineFileWithFilter() {
        var path = getPathToResource("smallfile");
        var reader = new TailReaderImpl(65536);
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(reader.getLastNLines(path, "WONTFIND", 0, 10))
                        .isEmpty());
    }

    @Test
    void testFilterNoMatches() {
        var path = getPathToResource("macbeth.txt");
        var reader = new TailReaderImpl(65536);
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(reader.getLastNLines(path, "WONTFIND", 0, 10))
                        .isEmpty());
    }

    @Test
    void noLinesFromEmptyFile() {
        var path = getPathToResource("empty.txt");
        var reader = new TailReaderImpl(65536);
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(reader.getLastNLines(path, null, 0, 10))
                        .isEmpty());
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
