package qlog;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

public class TailReader {

    private static final int CAP = 65536; // 8KB

    // TODO Add line filter
    public static List<String> getLastNLines(Path file, int lineCount) {
        // Each chunk of the file will be read into this buffer
        var bb = ByteBuffer.allocate(CAP);

        // Collect encountered lines into this List to be returned when the
        // line count requirement is satisfied.
        var collectedLines = new ArrayList<String>();

        // Open a seekable channel to the file so that we can read chunks of the file starting
        // at the end. The start of each read will be determined as the byte-position end of the
        // file minus the capacity of the byte buffer. Each "step" will move the start backwards
        // through the file like a cursor and read the chunk into the byte buffer.
        // This approach requires use of a SeekableByteChannel so that we can manually control
        // position of the channel while reading the chunks.
        try (var ch = Files.newByteChannel(file, StandardOpenOption.READ)) {
            // The start of the chunk is the end (channel size) minus the capacity (limit) of the buffer.
            // If the file is smaller than the buffer we do not allow start to be negative so take the max of 0.
            var start = Math.max(0, ch.size() - bb.limit());
            // Create a lineBuffer to store characters read from the chunk. This buffer exists outside the scope
            // of the chunk loop since the chunk is an arbitrary boundary and may split a line. (The next chunk
            // would finish the line and flush the characters from the buffer into the collected lines.)
            var lineBuffer = new StringBuilder();
            // Keep track of how many bytes we read from the file so that we can stop from reading
            // the head of the file multiple times.
            var bytesRead = 0;
            // Chunk through the file while the collectedLines size is less than the lineCount
            // or we've reached the start of the file.
            while (collectedLines.size() < lineCount || bytesRead < ch.size()) {
                // Set the position of the channel to the start of the chunk.
                ch.position(start);
                // Read the bytes from the channel starting from the position into the bytebuffer.
                bytesRead += ch.read(bb);
                for (int i = bb.position() - 1; i > 0; i--) {
                    // TODO Read as UTF-16
                    var c = (char) bb.get(i);
                    // TODO String#valueOf allocates a String on the heap for each char
                    //      Investigate a more efficient way to check if the current char
                    //      is a line separator.
                    if (String.valueOf(c).equals(System.lineSeparator())) {
                        if (lineBuffer.length() <= 0) {
                            // skip flushing of the lineBuffer is empty, e.g. if the last
                            // character in the file is a line-ending.
                            continue;
                        }
                        // If this char is a line separator, reverse the lineBuffer (since we're
                        // iterating through chars backwards), and flush it to a String in the
                        // result list of collected lines.
                        // TODO Implement line filter
                        //      Flush the lineBuffer only if the string contains the substring
                        collectedLines.addLast(lineBuffer.reverse().toString());
                        lineBuffer.setLength(0);
                        if (collectedLines.size() >= lineCount) {
                            // break out of looping through this chunk if we have enough lines
                            break;
                        }
                    } else {
                        lineBuffer.append(c);
                    }
                }
                // End of chunk

                // Break out of chunking if we already have enough lines
                if (collectedLines.size() == lineCount) {
                    break;
                }
                // Otherwise, prepare for the next chunk by stepping start backwards by the size
                // of the buffer and clearing the buffer of its contents so that the next read will
                // fill the buffer with the next chunk of text from the file.
                start = Math.max(0, start - bb.limit());
                bb.clear();
            }
        } catch (IOException e) {
            // TODO implement better error handling
            throw new RuntimeException(e);
        }
        return collectedLines;
    }

}
