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
import net.tridentsdk.base.Block;
import net.tridentsdk.base.Position;
import net.tridentsdk.base.Substance;
import net.tridentsdk.effect.sound.SoundEffect;
import net.tridentsdk.effect.sound.SoundEffectType;
import net.tridentsdk.meta.block.ChestMeta;
import net.tridentsdk.meta.block.FurnaceMeta;
import net.tridentsdk.meta.component.MetaFactory;
import net.tridentsdk.server.data.block.FurnaceMetaImpl;
import net.tridentsdk.server.inventory.TridentInventory;
import net.tridentsdk.server.netty.ClientConnection;
import net.tridentsdk.server.netty.packet.InPacket;
import net.tridentsdk.server.netty.packet.Packet;
import net.tridentsdk.server.player.PlayerConnection;
import net.tridentsdk.server.player.TridentPlayer;
import net.tridentsdk.server.util.OwnedTridentBlock;
import net.tridentsdk.util.Value;
import net.tridentsdk.util.Vector;

import static net.tridentsdk.meta.block.ByteArray.writeFirst;
import static net.tridentsdk.meta.block.ByteArray.writeSecond;

public class PacketPlayInBlockPlace extends InPacket {
    /**
     * Location of the block being placed
     */
    protected Position location;
    protected byte direction;
    /**
     * PositionWritable of the cursor, incorrect use of a Vector
     */
    protected Vector cursorPosition;

    @Override
    public int id() {
        return 0x1C;
    }

    @Override
    public Packet decode(ByteBuf buf) {
        long encodedLocation = buf.readLong();

        this.location = Position.create(null, (double) (encodedLocation >> 38),
                (double) ((encodedLocation >> 26) & 0xFFF), (double) (encodedLocation << 38 >> 38));
        this.direction = buf.readByte();

        // ignore held item
        for (int i = 0; i < buf.readableBytes() - 3; i++) {
            buf.readByte();
        }

        double x = (double) buf.readByte();
        double y = (double) buf.readByte();
        double z = (double) buf.readByte();

        this.cursorPosition = new Vector(x, y, z);
        return this;
    }

    public Position location() {
        return this.location;
    }

    public byte blockDirection() {
        return this.direction;
    }

    public Vector cursorPosition() {
        return this.cursorPosition;
    }

    @Override
    public void handleReceived(ClientConnection connection) {
        TridentPlayer player = ((PlayerConnection) connection).player();
        location.setWorld(player.world());

        if(player.heldItem() == null) {
            //TODO: add a check where this is called from so it will not be null while a player interacts with the item slots containing air while placing blocks.
            return;
        }
        
        Substance substance = player.heldItem().type();
        Vector vector = determineOffset();
        if (!substance.isBlock()) {
            // TODO eat food or pull bow or release/obtain water in a bucket, etc
            return;
        }

        if (location.y() + vector.y() > 255 || location.y() + vector.y() < 0) {
            // Illegal block position
            return;
        }

        Position position = location.block().substance().canBeReplaced() ? location : location.relative(vector);
        Block block = new OwnedTridentBlock(player, position.block());

        if (location.y() < 255 && location.block() != null && block.substance().isFunctional() && !player.isCrouching()) {
            switch (block.substance()) {
                case FURNACE:
                case BURNING_FURNACE:
                    ((FurnaceMetaImpl) block.obtainMeta(FurnaceMeta.class)).furnaceInventory().sendTo(player);
                    break;
                case CHEST:
                    ((TridentInventory) block.obtainMeta(ChestMeta.class).inventory()).sendTo(player);
                    break;
            }
            // TODO Add all functional blocks (workbench, furnace, anvil, etc)
        } else if (player.heldItem() != null && player.heldItem().type() != Substance.AIR) {
            short yaw = (short) (player.position().yaw() * 10);
            short meta = player.heldItem().damageValue();
            Value<Byte> result = Value.of((byte) 0);
            Value<Substance> substanceValue = Value.of(substance);

            boolean allow = MetaFactory.decode(block, substanceValue, new byte[]{
                    writeFirst(yaw), writeSecond(yaw), direction,
                    ((byte) cursorPosition.x()), ((byte) cursorPosition.y()), ((byte) cursorPosition.z()),
                    writeFirst(meta), writeSecond(meta)
            }, result);

            if (allow) {
                block.setSubstanceAndMeta(substanceValue.get(), result.get());

                SoundEffectType soundEffectType = substanceValue.get().placeSound();
                if (soundEffectType != null) {
                    SoundEffect sound = location.world().playSound(soundEffectType);
                    sound.setPosition(location);
                    sound.apply();
                }
            }
        }
    }

    private Vector determineOffset() {
        int x = 0;
        int y = 0;
        int z = 0;

        switch (blockDirection()) {
            case 0:
                y--;
                break;
            case 1:
                y++;
                break;
            case 2:
                z--;
                break;
            case 3:
                z++;
                break;
            case 4:
                x--;
                break;
            case 5:
                x++;
                break;
            default:
                return new Vector(0, 0, 0);
        }

        return new Vector(x, y, z);
    }
}
