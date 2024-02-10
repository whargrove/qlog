package qlog;

import org.assertj.core.api.WithAssertions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.StandardOpenOption;
import java.util.List;

import static java.nio.file.Files.createTempFile;
import static java.nio.file.Files.delete;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TailReaderTest implements WithAssertions {

    java.nio.file.Path file;

    @BeforeEach
    void setUp() throws Exception {
        file = createTempFile("test", ".log").toAbsolutePath();
    }

    @AfterEach
    void tearDown() throws Exception {
        delete(file);
    }

    @Test
    void returnsLastNLinesInFile() {
        var lines = List.of("Tomorrow, and tomorrow, and tomorrow,",
                "Creeps in this petty pace from day to day,",
                "To the last syllable of recorded time;",
                "And all our yesterdays have lighted fools",
                "The way to dusty death. Out, out, brief candle!",
                "Life's but a walking shadow, a poor player,",
                "That struts and frets his hour upon the stage,",
                "And then is heard no more. It is a tale",
                "Told by an idiot, full of sound and fury,",
                "Signifying nothing.\n");
        try {
            Files.writeString(file, String.join("\n", lines));
        } catch (IOException e) {
            fail("Error writing to file: " + e.getMessage(), e);
        }

        var n = 3;
        List<String> lastNLines = TailReader.getLastNLines(file, n);

        assertThat(lastNLines)
                .as("The last N lines were read from the file.")
                .hasSize(n);
        var expected = List.of("Signifying nothing.",
                "Told by an idiot, full of sound and fury,",
                "And then is heard no more. It is a tale");
        assertThat(lastNLines)
                .as("The last N lines contains lines ordered newest to oldest.")
                .containsExactlyElementsOf(expected);
    }

}
