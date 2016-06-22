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

package net.tridentsdk.server.packets.play.in;

import io.netty.buffer.ByteBuf;
import net.tridentsdk.server.data.Slot;
import net.tridentsdk.server.entity.TridentDroppedItem;
import net.tridentsdk.server.netty.ClientConnection;
import net.tridentsdk.server.netty.packet.InPacket;
import net.tridentsdk.server.netty.packet.Packet;
import net.tridentsdk.server.player.PlayerConnection;
import net.tridentsdk.server.player.TridentPlayer;

/**
 * While the user is in the standard inventory (i.e., not a crafting bench) on a creative-mode server, then this packet
 * will be sent:  If an item is dropped into the quick bar If an item is picked up from the quick bar (item id is
 * -1)
 */
public class PacketPlayInPlayerCreativeAction extends InPacket {

    /**
     * Slot of the action
     */
    protected short slot;
    /**
     * Item used in the action
     */
    protected Slot item;

    @Override
    public int id() {
        return 0x06;
    }

    public Slot item() {
        return this.item;
    }

    @Override
    public Packet decode(ByteBuf buf) {
        this.slot = buf.readShort();
        this.item = new Slot(buf);

        return this;
    }

    public short slot() {
        return this.slot;
    }

    @Override
    public void handleReceived(ClientConnection connection) {
        TridentPlayer player = ((PlayerConnection) connection).player();

        if (slot <= 0) {
            TridentDroppedItem item = new TridentDroppedItem(player.headLocation(), item().item());
            item.spawn();
            item.setVelocity(player.position().toDirection().normalize().multiply(2000));
            // TODO set item type
            // TODO this can also clear the inventory

            return;
        }

        // TODO: Handle when the item is set in the Player's hand
        player.window().setSlot(slot, item.item());
    }
}
