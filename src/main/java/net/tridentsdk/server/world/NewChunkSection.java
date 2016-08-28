/*
 * Trident - A Multithreaded Server Alternative
 * Copyright 2014 The TridentSDK Team
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package net.tridentsdk.server.world;


import com.google.common.collect.Lists;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.tridentsdk.meta.nbt.NBTSerializable;
import net.tridentsdk.server.netty.Codec;

import java.util.List;

public class NewChunkSection implements NBTSerializable {

    /**
     * Size (dimensions) of blocks in a chunk section.
     */
    public static final int SIZE = 16 * 16 * 16; // width * depth * height
    /**
     * Length of the sky and block light nibble arrays.
     */
    public static final int LIGHT_LENGTH = 16 * 16 * 16 / 2; // size * size * size / 2 (nibble bit count)
    /**
     * Length of the block data array.
     */

    private final List<Integer> palette = Lists.newArrayList();
    private final int[] blocks;
    private NibbleArray blockLight;
    private NibbleArray skyLight;

    public NewChunkSection() {
        this.blocks = new int[SIZE];
        this.blockLight = new NibbleArray(SIZE);
        palette.add(0); // AIR
    }

    /**
     * Set a block in the chunk
     *
     * @param x    Block X
     * @param y    Block Y
     * @param z    Block Z
     * @param type The type of the block
     * @param data The data value of the block
     */
    public void setBlock(int x, int y, int z, int type, int data) {
        setBlock(index(x, y, z), type, data);
    }

    /**
     * Set a block in the chunk based on the index
     *
     * @param idx  Index
     * @param type The type of the block
     * @param data The data value of the block
     */
    public void setBlock(int idx, int type, int data) {
        int hash = type << 4 | (data & 0xF);
        int index = palette.indexOf(hash);
        if (index == -1) {
            index = palette.size();
            palette.add(hash);
        }

        blocks[idx] = index;
    }

    /**
     * Set the block light array
     *
     * @param data The value to set the block light to
     */
    public void setBlockLight(byte[] data) {
        blockLight = new NibbleArray(data);
    }

    /**
     * Set the sky light array
     *
     * @param data The value to set the sky light to
     */
    public void setSkyLight(byte[] data) {
        if (data.length != LIGHT_LENGTH) throw new IllegalArgumentException("Data length != " + LIGHT_LENGTH);
        this.skyLight = new NibbleArray(data);
    }

    private int index(int x, int y, int z) {
        return z << 8 | y << 4 | x;
    }

    /**
     * Write the blocks to a buffer.
     *
     * @param output The buffer to write to.
     * @throws Exception Throws if it failed to write.
     */
    public void writeBlocks(ByteBuf output) throws Exception {
        // Write bits per block
        int bitsPerBlock = 4;
        while (palette.size() > 1 << bitsPerBlock) {
            bitsPerBlock += 1;
        }
        long maxEntryValue = (1L << bitsPerBlock) - 1;
        output.writeByte(bitsPerBlock);

        // Write pallet (or not)
        Codec.writeVarInt32(output, palette.size());
        for (int mappedId : palette) {
            Codec.writeVarInt32(output, mappedId);
        }

        int length = (int) Math.ceil(SIZE * bitsPerBlock / 64.0);
        Codec.writeVarInt32(output, length);
        long[] data = new long[length];
        for (int index = 0; index < blocks.length; index++) {
            int value = blocks[index];
            int bitIndex = index * bitsPerBlock;
            int startIndex = bitIndex / 64;
            int endIndex = ((index + 1) * bitsPerBlock - 1) / 64;
            int startBitSubIndex = bitIndex % 64;
            data[startIndex] = data[startIndex] & ~(maxEntryValue << startBitSubIndex) | ((long) value & maxEntryValue) << startBitSubIndex;
            if (startIndex != endIndex) {
                int endBitSubIndex = 64 - startBitSubIndex;
                data[endIndex] = data[endIndex] >>> endBitSubIndex << endBitSubIndex | ((long) value & maxEntryValue) >> endBitSubIndex;
            }
        }
        for (long l : data) {
            output.writeLong(l);
        }
    }

    /**
     * Write the block light to a buffer
     *
     * @param output The buffer to write to
     */
    public void writeBlockLight(ByteBuf output) {
        output.writeBytes(blockLight.getHandle());
    }

    /**
     * Write the sky light to a buffer
     *
     * @param output The buffer to write to
     */
    public void writeSkyLight(ByteBuf output) {
        output.writeBytes(skyLight.getHandle());
    }

    /**
     * Check if sky light is present
     *
     * @return True if skylight is present
     */
    public boolean hasSkyLight() {
        return skyLight != null;
    }

    /**
     * Get expected size of this chunk section.
     *
     * @return Amount of bytes sent by this section
     */
    public int getExpectedSize() throws Exception {
        int bitsPerBlock = palette.size() > 255 ? 16 : 8;
        int bytes = 1; // bits per block
        bytes += paletteBytes(); // palette
        bytes += countBytes(bitsPerBlock == 16 ? SIZE * 2 : SIZE); // block data length
        bytes += (palette.size() > 255 ? 2 : 1) * SIZE; // block data
        bytes += LIGHT_LENGTH; // block light
        bytes += hasSkyLight() ? LIGHT_LENGTH : 0; // sky light
        return bytes;
    }

    private int paletteBytes() throws Exception {
        // Count bytes used by pallet
        int bytes = countBytes(palette.size());
        for (int mappedId : palette) {
            bytes += countBytes(mappedId);
        }
        return bytes;
    }

    private int countBytes(int value) throws Exception {
        // Count amount of bytes that would be sent if the value were sent as a VarInt
        ByteBuf buf = Unpooled.buffer();
        Codec.writeVarInt32(buf, value);
        buf.readerIndex(0);
        int bitCount = buf.readableBytes();
        buf.release();
        return bitCount;
    }


}