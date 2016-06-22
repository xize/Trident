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

import java.util.UUID;

public class PacketPlayOutPlayerListItem extends OutPacket {

    protected int action;
    protected PlayerListDataBuilder[] playerListData;

    @Override
    public int id() {
        return 0x2D;
    }

    public int action() {
        return this.action;
    }

    public PlayerListDataBuilder[] playerListData() {
        return this.playerListData;
    }

    @Override
    public void encode(ByteBuf buf) {
        Codec.writeVarInt32(buf, this.action);
        Codec.writeVarInt32(buf, this.playerListData.length);

        for (PlayerListDataBuilder data : this.playerListData) {
            data.write(buf);
        }
    }

    public static class PlayerListDataBuilder {
        protected UUID id;
        protected Object[] values;

        public UUID id() {
            return this.id;
        }

        public PlayerListDataBuilder id(UUID id) {
            this.id = id;

            return this;
        }

        public Object[] values() {
            return this.values;
        }

        public PlayerListDataBuilder values(Object... values) {
            this.values = values;

            return this;
        }

        public void write(ByteBuf buf) {
            buf.writeLong(this.id.getMostSignificantBits());
            buf.writeLong(this.id.getLeastSignificantBits());

            // rip in organize
            for (Object o : values) {
                if (o == null) {
                    continue;
                }

                if (o.getClass().isArray()) {
                    Object[] objects = (Object[]) o;
                    for (Object o1 : objects) {
                        encode(o1, buf);
                    }

                    continue;
                }

                encode(o, buf);
            }
        }

        private void encode(Object o, ByteBuf buf) {
            switch (o.getClass().getSimpleName()) {
                case "String":
                    Codec.writeString(buf, (String) o);
                    break;

                case "Integer":
                    Codec.writeVarInt32(buf, (Integer) o);
                    break;

                case "Boolean":
                    buf.writeBoolean((Boolean) o);
                    break;

                default:
                    // ignore bad developers
                    break;
            }
        }
    }
}
