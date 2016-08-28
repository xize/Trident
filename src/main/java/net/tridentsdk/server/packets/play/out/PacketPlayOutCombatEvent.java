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

public class PacketPlayOutCombatEvent extends OutPacket {

    protected short event;
    protected int entityId;

    protected short duration;

    protected int playerId;
    protected String message;

    @Override
    public int id() {
        return 0x2C;
    }

    public short event() {
        return this.event;
    }

    public int entityId() {
        return this.entityId;
    }

    public short duration() {
        return this.duration;
    }

    public int playerId() {
        return this.playerId;
    }

    public String message() {
        return this.message;
    }

    @Override
    public void encode(ByteBuf buf) {
        Codec.writeVarInt32(buf, (int) this.event);

        switch (this.event) {
            case 1:
                Codec.writeVarInt32(buf, (int) this.duration);
                buf.writeInt(this.entityId);
                break;

            case 2:
                Codec.writeVarInt32(buf, this.playerId);
                buf.writeInt(this.entityId);

                Codec.writeString(buf, this.message);
                break;
        }
    }
}
