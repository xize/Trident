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
import net.tridentsdk.server.netty.packet.InPacket;
import net.tridentsdk.server.netty.packet.Packet;
import net.tridentsdk.server.player.PlayerConnection;
import net.tridentsdk.server.player.TridentPlayer;

/**
 * Packet sent when only Yaw + Pitch is sent by the client
 */
public class PacketPlayInPlayerLook extends InPacket {
    /**
     * Absolute rotation on the X Axis, in degrees
     */
    protected float newYaw;
    /**
     * Absolute rotation on the Y Axis, in degrees
     */
    protected float newPitch;

    /**
     * Wether the playeris on the ground or not
     */
    protected boolean onGround;

    @Override
    public int id() {
        return 0x0E;
    }

    @Override
    public Packet decode(ByteBuf buf) {
        this.newYaw = buf.readFloat();
        this.newPitch = buf.readFloat();
        this.onGround = buf.readBoolean();

        return this;
    }

    @Override
    public void handleReceived(ClientConnection connection) {
        TridentPlayer player = ((PlayerConnection) connection).player();
        Position from = player.position();
        Position to = player.position();

        to.setYaw(this.newYaw);
        to.setPitch(this.newPitch);

        player.setPosition(to);
    }
}
