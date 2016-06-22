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
import net.tridentsdk.base.Position;
import net.tridentsdk.server.netty.ClientConnection;
import net.tridentsdk.server.netty.packet.Packet;
import net.tridentsdk.server.player.PlayerConnection;
import net.tridentsdk.server.player.TridentPlayer;

/**
 * Packet sent when player moved both x, y, z and yaw, and pitch.
 */
public class PacketPlayInPlayerCompleteMove extends PacketPlayInPlayerMove {

    @Override
    public int id() {
        return 0x0D;
    }

    @Override
    public Packet decode(ByteBuf buf) {
        double x = buf.readDouble();
        double y = buf.readDouble();
        double z = buf.readDouble();
        float newYaw = buf.readFloat();
        float newPitch = buf.readFloat();

        super.location = Position.create(null, x, y, z, newYaw, newPitch);
        super.onGround = buf.readBoolean();
        return this;
    }

    @Override
    public void handleReceived(ClientConnection connection) {
        TridentPlayer player = ((PlayerConnection) connection).player();
        super.location.setWorld(player.world());
        player.setPosition(super.location);

        if (player.isLoggingIn()){
            player.resumeLogin();
        }
    }
}
