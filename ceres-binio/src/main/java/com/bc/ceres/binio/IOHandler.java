package com.bc.ceres.binio;

import java.io.IOException;

// todo: check - need getMaxPosition()? See also "todo" in Segment.makeDataAccessible()

/**
 * Provides the mechanism to read and write byte data at a specified position
 * within a random access data file, stream or memory buffer.
 */
public interface IOHandler {

    /**
     * Reads a sequence of up to {@code data.length} bytes into the
     * given array starting from the the given position. If the position
     * is greater than the file's current size then no bytes are read.</p>
     * <p/>
     * A read operation might not fill the array, and in fact it might not
     * read any bytes at all.  Whether or not it does so, depends upon the
     * state of the file, e.g. only bytes that remain in the file starting from
     * the given position will be read.
     *
     * @param context  The I/O context.
     * @param data     The data array into which bytes are to be transferred.
     * @param position The file position at which the transfer is to begin;
     *                 must be non-negative.
     * @throws IOException If an I/O error occurs.
     */
    void read(IOContext context, byte[] data, long position) throws IOException;

    /**
     * <p> Writes a sequence of up to {@code data.length} bytes to the file,
     * starting at the given position. If the given
     * position is greater than the file's current size then the file will be
     * grown to accommodate the new bytes; the values of any bytes between the
     * previous end-of-file and the newly-written bytes are unspecified.</p>
     *
     * @param context  The I/O context.
     * @param data     The data array from which bytes are to be transferred.
     * @param position The file position at which the transfer is to begin;
     *                 must be non-negative.
     * @throws IOException If an I/O error occurs.
     */
    void write(IOContext context, byte[] data, long position) throws IOException;
}
