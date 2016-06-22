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

package net.tridentsdk.server.packets.play.out;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.server.netty.Codec;
import net.tridentsdk.server.netty.packet.OutPacket;

public class PacketPlayOutStatistics extends OutPacket {

    public static final OutPacket DEFAULT_STATISTIC = new PacketPlayOutStatistics().set("entries", null);
    protected StatisticEntry[] entries;

    @Override
    public int id() {
        return 0x07;
    }

    public StatisticEntry[] entries() {
        return this.entries;
    }

    @Override
    public void encode(ByteBuf buf) {
        if (this.entries == null) {
            Codec.writeVarInt32(buf, 0);
            return;
        }

        Codec.writeVarInt32(buf, this.entries.length);

        for (StatisticEntry entry : this.entries) {
            entry.write(buf);
        }
    }

    public static class StatisticEntry {
        protected String string;
        protected int value;

        public String string() {
            return this.string;
        }

        public void setString(String string) {
            this.string = string;
        }

        public int value() {
            return this.value;
        }

        public void setValue(int value) {
            this.value = value;
        }

        public void write(ByteBuf buf) {
            Codec.writeString(buf, this.string);
            Codec.writeVarInt32(buf, this.value);
        }
    }
}
