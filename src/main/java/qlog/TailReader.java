package qlog;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

public class TailReader {

    public static List<String> getLastNLines(Path file, int lineCount) {
        var collectedLines = new ArrayList<String>();
        try (var raf = new RandomAccessFile(file.toFile(), "r")) {
            // Create an instance of a RandomAccessFile so that we can manipulate a custom cursor
            // that seeks through the file appending character into a buffer and then flushing the
            // buffer into a String and appending to a List when encountering a line-ending
            // character.

            // Iterate backwards through the file so that we only read from disk the necessary
            // chunks of the file that should be included in the result.

            // assume the last char of the file is a \n, we don't want this to count as a
            // "seen line" so we "skip" over it.
            var cur = raf.length() - 1;
            // stopping when we read enough lines to satisfy the lineCount
            var linesRead = 0;
            // the initial capacity is probably reasonable default
            var buffer = new StringBuilder();
            while ((--cur) >= 0 && linesRead < lineCount) {
                raf.seek(cur);
                char c;
                // check if we're at the beginning of the file
                if (cur > 0) {
                    // TODO This only works for ASCII
                    // TODO Read larger chunks of the file in each loop
                    //      RandomAccessFile#readByte is blocking and requires context switching
                    //      For example, we could define a buffer size, e.g. 65536 (64KB) or larger
                    //      and step backwards by the buffer size, then read the file into the
                    //      buffer, split into sequences by newline chars.
                    //      This could potentially "over read" the file (by as much as the buffer
                    //      size) but is strictly better since it would avoid many ctx switches
                    //      during the loop.
                    c = (char)raf.readByte();
                    // TODO support other line endings
                    // if this char is a line ending
                    if (c == '\n') {
                        // then increment the linesRead counter
                        linesRead++;
                        // and flush the buffer into the result
                        collectedLines.addLast(buffer.reverse().toString());
                        buffer.setLength(0);
                    } else {
                        buffer.append(c);
                    }
                }
            }
        } catch (IOException e) {
            // TODO Implement better error handling
            e.printStackTrace();
            return List.of();
        }
        return collectedLines;
    }
    
}
