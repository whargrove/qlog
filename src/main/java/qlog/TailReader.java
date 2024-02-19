package qlog;

import jakarta.annotation.Nullable;

import java.nio.file.Path;
import java.util.List;

public interface TailReader {
    /**
     * Get the last N lines from a file.
     *
     * @param path   A path to the file to read lines from.
     * @param filter If present, a line is counted only when it matches the filter.
     * @param start  The line number to start reading from. 0 is the last line.
     * @param count  The number of lines to read out in the returned List.
     * @return A List of lines ordered with the newest entries at the head of the List.
     */
    List<String> getLastNLines(Path path, @Nullable String filter, int start, int count);
}
