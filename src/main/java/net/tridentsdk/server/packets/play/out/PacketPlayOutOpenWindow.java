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
import net.tridentsdk.docs.Policy;
import net.tridentsdk.inventory.InventoryType;
import net.tridentsdk.server.netty.Codec;
import net.tridentsdk.server.netty.packet.OutPacket;

@Policy("No instantiation; Use Inventory only to correctly handle")
public class PacketPlayOutOpenWindow extends OutPacket {
    protected int windowId;
    protected InventoryType inventoryType;
    protected String windowTitle;
    protected int slots;
    protected int entityId; // only for horses, since people at Mojang are retards

    @Override
    public int id() {
        return 0x13;
    }

    public int windowId() {
        return this.windowId;
    }

    public InventoryType inventoryType() {
        return this.inventoryType;
    }

    public String windowTitle() {
        return this.windowTitle;
    }

    public int slots() {
        return this.slots;
    }

    public int entityId() {
        return this.entityId;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeByte(this.windowId);

        Codec.writeString(buf, this.inventoryType.toString());
        Codec.writeString(buf, this.windowTitle);

        buf.writeByte(this.slots);
        //buf.writeInt(this.entityId); // rip in varints
    }
}
