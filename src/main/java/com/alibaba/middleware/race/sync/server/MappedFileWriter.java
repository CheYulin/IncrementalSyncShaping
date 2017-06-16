package com.alibaba.middleware.race.sync.server;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

/**
 * Created by will on 16/6/2017.
 */
public class MappedFileWriter {

    private static int CHUNK_SIZE = 64 * 1024 * 1024;

    private FileChannel fileChannel;
    private long realSize = 0;
    private int currentInnerChunkIndex = 0;
    private MappedByteBuffer mappedByteBuffer = null;

    public MappedFileWriter(String fullFileName, long expectLength) throws IOException {
        fileChannel = new RandomAccessFile(fullFileName, "rw").getChannel();
        fileChannel.truncate(expectLength);
        getNextChunk();
    }

    public void write(byte[] data) throws IOException {
        write(data, 0, data.length);
    }

    public void write(byte[] data, int offset, int length) throws IOException {
        int expectEnd = currentInnerChunkIndex + length;
        if (expectEnd < CHUNK_SIZE) {
            mappedByteBuffer.put(data, offset, length);
            currentInnerChunkIndex = expectEnd;
            realSize += length;
        } else {
            int writeLength = CHUNK_SIZE - currentInnerChunkIndex;
            mappedByteBuffer.put(data, offset, writeLength);
            realSize += writeLength;
            getNextChunk();
            write(data, offset + writeLength, length - writeLength);
            currentInnerChunkIndex += (length - writeLength);
            realSize += (length - writeLength);
        }
        if (currentInnerChunkIndex == CHUNK_SIZE)
            getNextChunk();
    }

    public void close() throws IOException {
        if (mappedByteBuffer != null) {
            FileUtil.unmap(mappedByteBuffer);
        }
        fileChannel.truncate(realSize);
    }

    public void getNextChunk() throws IOException {
        if (mappedByteBuffer != null) {
            FileUtil.unmap(mappedByteBuffer);
        }
        mappedByteBuffer = fileChannel.map(FileChannel.MapMode.READ_WRITE, realSize, CHUNK_SIZE);
        mappedByteBuffer.load();
        currentInnerChunkIndex = 0;
    }
}