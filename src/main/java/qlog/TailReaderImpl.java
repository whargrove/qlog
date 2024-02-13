package qlog;

import io.micronaut.context.annotation.Value;
import jakarta.annotation.Nullable;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import qlog.exc.TailReaderFileNotFoundException;
import qlog.exc.TailReaderIOException;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Singleton
public class TailReaderImpl implements TailReader {

    private static final Logger LOG = LoggerFactory.getLogger(TailReaderImpl.class);

    private final int bufferCapacity;

    public TailReaderImpl(@Value("${qlog.tail.buffer.capacity:65536}") int bufferCapacity) {
        this.bufferCapacity = bufferCapacity;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> getLastNLines(Path path, int lineCount, @Nullable String filter) {
        // Each chunk of the file will be read into this buffer
        var bb = ByteBuffer.allocate(this.bufferCapacity);

        // Collect encountered lines into this List to be returned when the
        // line count requirement is satisfied.
        var collectedLines = new ArrayList<String>();

        // Open a seekable channel to the file so that we can read chunks of the file starting
        // at the end. The start of each read will be determined as the byte-position end of the
        // file minus the capacity of the byte buffer. Each "step" will move the start backwards
        // through the file like a cursor and read the chunk into the byte buffer.
        // This approach requires use of a SeekableByteChannel so that we can manually control
        // position of the channel while reading the chunks.
        try (var ch = Files.newByteChannel(path, StandardOpenOption.READ)) {
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
            // or we've read all the bytes in the file.
            while (collectedLines.size() < lineCount || bytesRead < ch.size()) {
                if (bytesRead > 0) {
                    // Each time we start a new chunk, we want to dynamically size the limit
                    // of the byte buffer so that we only read the bytes necessary to complete
                    // the chunk and read the whole file. This is especially impactful when
                    // we reach the head of the file to avoid reading bytes from the file
                    // that were already read in a previous chunk.
                    //
                    // We use min(CAP, size - bytesRead) to avoid setting a larger limit
                    // that what the buffer was allocated with.
                    bb.limit(Math.min(this.bufferCapacity, (int) ch.size() - bytesRead));
                }
                // Set the position of the channel to the start of the chunk.
                ch.position(start);
                // Read the bytes from the channel starting from the position into the bytebuffer.
                bytesRead += ch.read(bb);
                for (int i = bb.position() - 1; i >= 0; i--) {
                    // Casting byte to char means this reader only supports ASCII encoded files.
                    //
                    // Supporting UTF-8 would require using a CharsetDecoder to decode bytes from
                    // the chunk into characters. However, because we are tailing the file (that is
                    // starting with the end of the file), and supporting UTF-8 requires reading the
                    // bytes in forward order to decode bytes into a complete codepoint, we would
                    // need to implement a more complex buffer management to handle boundary cases
                    // where a code unit is split between two chunks. The complexity mainly arises
                    // from needing to search the head (furthest away from the end of the file) of
                    // the byte buffer to find the beginning of a code unit and then adjusting the
                    // size of the next chunk to read any bytes that were skipped in the previous
                    // chunk.
                    var c = (char) bb.get(i);
                    if (c == '\n') {
                        if (lineBuffer.length() <= 0) {
                            // skip flushing of the lineBuffer is empty, e.g. if the last
                            // character in the file is a line-ending.
                            continue;
                        }
                        // Reverse the buffer (since we're iterating through chars backwards),
                        // and flush it to the result set. If there's a filter defined in the
                        // arguments, then apply the filter before adding to the result set.
                        var stringFromBuffer = lineBuffer.reverse().toString();
                        lineBuffer.setLength(0);
                        if (filter != null) {
                            if (stringFromBuffer.contains(filter)) {
                                collectedLines.add(stringFromBuffer);
                            }
                        } else {
                            collectedLines.addLast(stringFromBuffer);
                        }

                        if (collectedLines.size() >= lineCount) {
                            // break out of looping through this chunk if we have enough lines
                            break;
                        }
                    } else {
                        lineBuffer.append(c);
                    }
                }
                // End of chunk

                if (!lineBuffer.isEmpty() && bytesRead >= ch.size()) {
                    // Flush the lineBuffer into the result set if we're at the beginning of the file.
                    collectedLines.addLast(lineBuffer.reverse().toString());
                    lineBuffer.setLength(0);
                }

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
            if (e instanceof NoSuchFileException noSuchFileException) {
                LOG.info("File not found: {}", path, noSuchFileException);
                throw new TailReaderFileNotFoundException("File not found: " + path, noSuchFileException);
            } else {
                LOG.error("Error reading file: {}", path, e);
                throw new TailReaderIOException("Error reading file: " + path, e);
            }
        }
        return collectedLines;
    }
}
