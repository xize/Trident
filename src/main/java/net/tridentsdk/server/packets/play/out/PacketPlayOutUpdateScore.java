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

public class PacketPlayOutUpdateScore extends OutPacket {

    protected String itemName;
    protected UpdateType type;
    protected String scoreName;
    protected int value;

    @Override
    public int id() {
        return 0x42;
    }

    public String itemName() {
        return this.itemName;
    }

    public UpdateType updateType() {
        return this.type;
    }

    public String score() {
        return this.scoreName;
    }

    public int value() {
        return this.value;
    }

    @Override
    public void encode(ByteBuf buf) {
        Codec.writeString(buf, this.itemName);
        buf.writeByte((int) this.type.asByte());

        if (this.type.b == 1) {
            return;
        }

        Codec.writeString(buf, this.scoreName);
        Codec.writeVarInt32(buf, this.value);
    }

    public enum UpdateType {
        CREATE(0),
        UPDATE(0),
        REMOVE(1);

        protected final byte b;

        UpdateType(int i) {
            this.b = (byte) i;
        }

        public byte asByte() {
            return this.b;
        }
    }
}
