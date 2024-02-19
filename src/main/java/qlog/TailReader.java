package qlog;

import jakarta.annotation.Nullable;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

public interface TailReader {
    /**
     * Get the last N lines from a file.
     *
     * @param path              A path to the file to read lines from.
     * @param filter            If present, a line is counted only when it matches the filter.
     * @param continuationToken A token the reader will use to continue reading at a byte position in the file.
     *                          This reader will issue a continuationToken in the result if there are more bytes
     *                          to read in the file.
     * @param start             The line number to start reading from. 0 is the last line.
     * @param count             The number of lines to read out in the returned List.
     * @return A List of lines ordered with the newest entries at the head of the List.
     */
    ReaderResult getLastNLines(Path path, @Nullable String filter, String continuationToken, int start, int count);

    record ReaderResult(List<String> lines, Optional<String> continuationToken) {
    }
}
