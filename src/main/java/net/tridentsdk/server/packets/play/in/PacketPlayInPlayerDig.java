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
import net.tridentsdk.base.BlockDirection;
import net.tridentsdk.base.Position;
import net.tridentsdk.base.Substance;
import net.tridentsdk.effect.particle.ParticleEffect;
import net.tridentsdk.effect.particle.ParticleEffectType;
import net.tridentsdk.effect.sound.SoundEffect;
import net.tridentsdk.effect.sound.SoundEffectType;
import net.tridentsdk.entity.types.EntityType;
import net.tridentsdk.event.Cancellable;
import net.tridentsdk.event.Event;
import net.tridentsdk.event.block.BlockBreakEvent;
import net.tridentsdk.event.player.*;
import net.tridentsdk.inventory.Item;
import net.tridentsdk.meta.block.Tile;
import net.tridentsdk.registry.Registered;
import net.tridentsdk.server.entity.TridentDroppedItem;
import net.tridentsdk.server.entity.projectile.TridentArrow;
import net.tridentsdk.server.netty.ClientConnection;
import net.tridentsdk.server.netty.packet.InPacket;
import net.tridentsdk.server.netty.packet.Packet;
import net.tridentsdk.server.packets.play.out.PacketPlayOutBlockChange;
import net.tridentsdk.server.player.PlayerConnection;
import net.tridentsdk.server.player.TridentPlayer;
import net.tridentsdk.server.world.TridentChunk;
import net.tridentsdk.server.world.TridentWorld;
import net.tridentsdk.util.TridentLogger;
import net.tridentsdk.util.Vector;
import net.tridentsdk.world.settings.GameMode;

public class PacketPlayInPlayerDig extends InPacket {
    private short status;
    private Position location;
    private short blockFace;

    @Override
    public int id() {
        return 0x13;
    }

    public short status() {
        return this.status;
    }

    public Position location() {
        return this.location;
    }

    public short blockFace() {
        return this.blockFace;
    }

    @Override
    public Packet decode(ByteBuf buf) {
        this.status = (short) buf.readByte();
        long encodedLocation = buf.readLong();

        this.location = Position.create(null, (double) (encodedLocation >> 38),
                (double) (encodedLocation << 26 >> 52), (double) (encodedLocation << 38 >> 38));
        this.blockFace = (short) buf.readByte();

        return this;
    }

    @Override
    public void handleReceived(ClientConnection connection) {
        TridentPlayer player = ((PlayerConnection) connection).player();
        DigStatus digStatus = DigStatus.getStatus(this.status);
        BlockDirection face = null;

        if (digStatus == DigStatus.DIG_START && player.gameMode() == GameMode.CREATIVE) {
            digStatus = DigStatus.DIG_FINISH;
        }

        this.location.setWorld(player.world());

        switch (this.blockFace) {
            case 0:
                face = BlockDirection.BOTTOM;
                break;

            case 1:
                face = BlockDirection.TOP;
                break;

            case 2:
                // z--
                break;

            case 3:
                // z++
                break;

            case 4:
                // x--
                break;

            case 5:
                // x++
                break;

            default:
                TridentLogger.get().error(new IllegalArgumentException("Client sent invalid BlockFace!"));
        }

        Cancellable event = null;

        Block block = location.block();
        switch (digStatus) {
            case DIG_START:
            case DIG_CANCEL:
            case DIG_FINISH:
                event = new PlayerDigEvent(player, face, this.status);

                if(digStatus == DigStatus.DIG_FINISH) {
                    BlockBreakEvent blockBreak = new BlockBreakEvent(player, block, face, player.heldItem());

                    if(blockBreak.isIgnored())
                        return;
                }

                break;

            case DROP_ITEMSTACK:
                if(player.heldItem() == null || player.heldItem().type() == Substance.AIR){
                    return;
                }

                event = new PlayerDropItemEvent(player, null); // todo: spawn item and call the event
                break;

            case DROP_ITEM:
                if(player.heldItem() == null || player.heldItem().type() == Substance.AIR){
                    return;
                }

                event = new PlayerDropItemEvent(player, null);
                break;

            case SHOOT_ARROW:
                Item item = player.heldItem();
                if (item.type().isWeapon()) {
                    if (item.type() == Substance.BOW) // bow
                        event = new PlayerShootBowEvent(player, null);
                    else event = new PlayerInteractEvent(player, block); // other weapons
                } else if (item.type().isEdible()) {
                    event = new PlayerConsumeEvent(player, item, 0.0);
                }
                // shoot bow, if player has a food item finish eating
                break;
            default:
        }

        Registered.events().fire((Event) event);

        if (event == null || event.isIgnored())
            return;

        // TODO act accordingly

        switch(digStatus){
            case SHOOT_ARROW:
                TridentArrow entity = (TridentArrow) location.world().spawn(EntityType.ARROW, location);
                entity.setVelocity(player.position().asUnitVector());
                break;

            case DIG_FINISH:
                block.ownedMeta().iterate(e -> {
                    if (e.getValue() instanceof Tile) {
                        ((TridentWorld) location.world()).tilesInternal().remove(e.getValue());
                        Position pos = block.position();
                        ((TridentChunk) pos.chunk()).tilesInternal().remove(new Vector(pos.x(), pos.y(), pos.z()));
                    }
                });

                int[] arr = {block.substance().id() + (block.meta() << 12)};

                ((TridentChunk) location().chunk()).setAt(location, Substance.AIR, (byte) 0, (byte) 255, (byte) 15);
                TridentPlayer.sendAll(new PacketPlayOutBlockChange()
                        .set("location", location).set("blockId", Substance.AIR.id()));

                ParticleEffect effect = location.world().spawnParticle(ParticleEffectType.BLOCK_CRACK);
                effect.setCount(64);
                effect.setLongDistance(false);
                effect.setPosition(location.add(new Vector(0.5, 0.5, 0.5)).asVector());
                effect.setOffset(new Vector(0.45, 0.45, 0.45));
                effect.setData(arr);
                effect.applyToEveryoneExcept(player);

                SoundEffectType soundEffectType = block.substance().breakSound();
                if(soundEffectType != null){
                    SoundEffect sound = location.world().playSound(soundEffectType);
                    sound.setPosition(location);
                    sound.applyToEveryoneExcept(player);
                }
                break;

            case DROP_ITEM:
            case DROP_ITEMSTACK:
                short count = digStatus == DigStatus.DROP_ITEM ? 1 : player.heldItem().quantity();
                Item heldItem = player.heldItem().clone();
                heldItem.setQuantity(count);
                TridentDroppedItem item = new TridentDroppedItem(player.headLocation(), heldItem);
                item.spawn();
                item.setVelocity(player.position().toDirection().normalize().multiply(2000));
                player.heldItem().setQuantity((short) (player.heldItem().quantity() - count));
                player.setHeldItem(player.heldItem());
                break;
        }

    }

    public enum DigStatus {
        DIG_START(0),
        DIG_CANCEL(1),
        DIG_FINISH(2),
        DROP_ITEMSTACK(3),
        DROP_ITEM(4),
        SHOOT_ARROW(5);

        private final short id;

        DigStatus(int id) {
            this.id = (short) id;
        }

        public static DigStatus getStatus(short id) {
            for (DigStatus status : DigStatus.values()) {
                if (status.id == id) {
                    return status;
                }
            }

            return null;
        }

        public short getId() {
            return this.id;
        }
    }
}
