package qlog;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.Disabled;
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
        List<String> lastNLines = new TailReaderImpl(65536).getLastNLines(file, null, null, 0, n).lines();

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
        List<String> lastNLines = new TailReaderImpl(65536).getLastNLines(path, null, null, 0, n).lines();

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
        List<String> lastNLines = new TailReaderImpl(65536).getLastNLines(path, null, null, 0, n).lines();

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
        List<String> lastNLines = new TailReaderImpl(65536).getLastNLines(path, null, null, 0, n).lines();

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
        List<String> actual = new TailReaderImpl(65536).getLastNLines(path, "tomorrow", null, 0, 1).lines();
        assertThat(actual).hasSize(1);
        assertThat(actual).containsExactlyElementsOf(List.of("Tomorrow, and tomorrow, and tomorrow,"));
    }

    @Test
    void multipleLinesMatchingFilterAreReturned() {
        var path = getPathToResource("macbeth.txt");
        List<String> actual = new TailReaderImpl(65536).getLastNLines(path, ",", null, 0, 3).lines();
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
                .untilAsserted(() -> assertThat(reader.getLastNLines(path, "WONTFIND", null, 0, 10).lines())
                        .isEmpty());
    }

    @Test
    void testFilterNoMatches() {
        var path = getPathToResource("macbeth.txt");
        var reader = new TailReaderImpl(65536);
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(reader.getLastNLines(path, "WONTFIND", null, 0, 10).lines())
                        .isEmpty());
    }

    @Test
    void noLinesFromEmptyFile() {
        var path = getPathToResource("empty.txt");
        var reader = new TailReaderImpl(65536);
        await().atMost(1, TimeUnit.SECONDS)
                .untilAsserted(() -> assertThat(reader.getLastNLines(path, null, null, 0, 10).lines())
                        .isEmpty());
    }

    @Test
    void skipsLinesWhenStartIsGreaterThanZero() {
        var path = getPathToResource("macbeth.txt");
        var reader = new TailReaderImpl(65536);
        var actual = reader.getLastNLines(path, null, null, 5, 1).lines();
        assertThat(actual).containsExactly("The way to dusty death. Out, out, brief candle!");
    }

    @Test
    void returnsContinuationTokenWhenThereAreMoreBytesToReadInTheFile() {
        var path = getPathToResource("macbeth.txt");
        var reader = new TailReaderImpl(16);
        var result = reader.getLastNLines(path, null, null, 0, 1);
        assertThat(result.continuationToken()).isNotEmpty();
    }

    @Test
    void noContinuationTokenWhenFinishedReadingTheFile() {
        var path = getPathToResource("smallfile");
        var reader = new TailReaderImpl(16);
        var result = reader.getLastNLines(path, null, null, 0, 1);
        assertThat(result.continuationToken()).isEmpty();
    }

    @Test
    void continuationTokenIsUsedToReadMoreLines() {
        var path = getPathToResource("macbeth.txt");
        var reader = new TailReaderImpl(16);
        var result = reader.getLastNLines(path, null, null, 0, 2);
        assertThat(result.lines())
                .as("The last two lines are read from the file.")
                .containsExactly("Signifying nothing.", "Told by an idiot, full of sound and fury,");
        var continuationToken = result.continuationToken().orElseThrow();
        var moreLines = reader.getLastNLines(path, null, continuationToken, 0, 1).lines();
        assertThat(moreLines)
                .as("The next line is read from the file (third from the end).")
                .containsExactly("And then is heard no more. It is a tale");
    }

    @Test
    void startIsIgnoredWhenContinuationTokenIsPresent() {
        var path = getPathToResource("macbeth.txt");
        var reader = new TailReaderImpl(16);
        var result = reader.getLastNLines(path, null, null, 0, 2);
        assertThat(result.lines())
                .as("The last two lines are read from the file.")
                .containsExactly("Signifying nothing.", "Told by an idiot, full of sound and fury,");
        var continuationToken = result.continuationToken().orElseThrow();
        var moreLines = reader.getLastNLines(path, null, continuationToken, 1, 1).lines();
        assertThat(moreLines)
                .as("The next line is read from the file, the start parameter is ignored.")
                .containsExactly("And then is heard no more. It is a tale");
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
