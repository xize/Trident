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

package net.tridentsdk.server.inventory;

import com.google.common.collect.Sets;
import net.tridentsdk.base.Substance;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.inventory.Inventory;
import net.tridentsdk.inventory.InventoryType;
import net.tridentsdk.inventory.Item;
import net.tridentsdk.registry.Registered;
import net.tridentsdk.server.data.Slot;
import net.tridentsdk.server.packets.play.out.PacketPlayOutCloseWindow;
import net.tridentsdk.server.packets.play.out.PacketPlayOutOpenWindow;
import net.tridentsdk.server.packets.play.out.PacketPlayOutSetSlot;
import net.tridentsdk.server.player.TridentPlayer;
import net.tridentsdk.util.WeakEntity;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReferenceArray;

/**
 * An inventory inventory, wherever and whatever is holding it or having it open
 *
 * @author The TridentSDK Team
 */
@ThreadSafe
public class TridentInventory implements Inventory {
    /**
     * Counter for inventory ids, initial value is 2 to avoid confusion with a inventory and a player inventory
     */
    private static final AtomicInteger counter = new AtomicInteger(2);

    private final int id;
    private final String name;
    private final int length;
    private final InventoryType type;
    private final Set<WeakEntity<Player>> users = Sets.newConcurrentHashSet();
    private final AtomicReferenceArray<Item> contents;

    /**
     * Builds a new inventory inventory
     *
     * @param name   the title of the inventory
     * @param length the amount of slots in the inventory (should be multiple of 9)
     */
    private TridentInventory(String name, int length, InventoryType type, int id) {
        this.name = name != null ? name : type.defaultName();
        this.length = length;
        this.id = id;
        this.contents = new AtomicReferenceArray<>(length);
        for (int i = 0; i < contents.length(); i++) {
            contents.set(i, new Item(Substance.AIR));
        }

        this.type = type;
    }

    private TridentInventory(String name, int length, InventoryType type) {
        this(name, length, type, counter.addAndGet(1));
    }

    /**
     * Builds a new inventory inventory
     *
     * @param length the amount of slots in the inventory (should be multiple of 9)
     */
    public TridentInventory(int length) {
        this("", length, InventoryType.CHEST, counter.addAndGet(1));
    }

    public TridentInventory(int length, int id) {
        this("", length, InventoryType.CHEST, id);
    }

    public static TridentInventory create(String name, int length, InventoryType type) {
        TridentInventory window = new TridentInventory(name, length, type);
        Registered.inventories().register(window);
        return window;
    }

    @Override
    public int id() {
        return this.id;
    }

    @Override
    public Item[] items() {
        Item[] array = new Item[contents.length()];
        for (int i = 0; i < contents.length(); i++) {
            array[i] = contents.get(i);
        }

        return array;
    }

    @Override
    public int length() {
        return this.length;
    }

    //@Override
    public int itemLength() {
        int counter = 0;
        for (Item item : items()) {
            if (item != null)
                counter++;
        }

        return counter;
    }

    @Override
    public String name() {
        return this.name;
    }

    @Override
    public Item itemAt(int slot) {
        return items()[slot];
    }

    @Override
    public void setSlot(int index, Item value) {
        if(value != null && value.quantity() == 0) {
            value = null;
        }

        contents.set(index, value);

        PacketPlayOutSetSlot setSlot = new PacketPlayOutSetSlot();
        setSlot.set("windowId", id()).set("slot", (short) index).set("item", new Slot(value));

        for (WeakEntity<Player> player : WeakEntity.iterate(users)) {
            ((TridentPlayer) player.obtain()).connection().sendPacket(setSlot);
        }
    }

    @Override
    public boolean putItem(Item item) {
        int freeSlot = -1;
        if(contents.length() == 45){
            for(int i = 36; i < contents.length() && item.quantity() > 0; i++){
                if(contents.get(i) == null || contents.get(i).type() == Substance.AIR){
                    if(freeSlot == -1){
                        freeSlot = i;
                    }
                }else if(contents.get(i).isSimilarIgnoreQuantity(item)){
                    int available = contents.get(i).type().maxStackSize() - contents.get(i).quantity();
                    if(available > item.quantity()){
                        contents.get(i).setQuantity((short) (contents.get(i).quantity() + item.quantity()));
                    }else{
                        contents.get(i).setQuantity((short) (contents.get(i).quantity() + available));
                    }
                    item.setQuantity((short) (item.quantity() - available));
                }
            }

            if(item.quantity() == 0){
                return true;
            }

            for(int i = 0; i < 36 && item.quantity() > 0; i++){
                if(contents.get(i) == null || contents.get(i).type() == Substance.AIR){
                    if(i >= 9 && freeSlot == -1){
                        freeSlot = i;
                    }
                }else if(contents.get(i).isSimilarIgnoreQuantity(item)){
                    int available = contents.get(i).type().maxStackSize() - contents.get(i).quantity();
                    if(available > item.quantity()){
                        contents.get(i).setQuantity((short) (contents.get(i).quantity() + item.quantity()));
                    }else{
                        contents.get(i).setQuantity((short) (contents.get(i).quantity() + available));
                    }
                    item.setQuantity((short) (item.quantity() - available));
                }
            }
        }else{
            for(int i = 0; i < contents.length() && item.quantity() > 0; i++){
                if(contents.get(i) == null || contents.get(i).type() == Substance.AIR){
                    if(freeSlot == -1){
                        freeSlot = i;
                    }
                }else if(contents.get(i).isSimilarIgnoreQuantity(item)){
                    int available = contents.get(i).type().maxStackSize() - contents.get(i).quantity();
                    if(available > item.quantity()){
                        contents.get(i).setQuantity((short) (contents.get(i).quantity() + item.quantity()));
                    }else{
                        contents.get(i).setQuantity((short) (contents.get(i).quantity() + available));
                    }
                    item.setQuantity((short) (item.quantity() - available));
                }
            }
        }

        if(item.quantity() <= 0){
            return true;
        }else if(freeSlot != -1){
            contents.set(freeSlot, item.clone());
            item.setQuantity((short) 0);
            return true;
        }

        return false;
    }

    public void sendTo(TridentPlayer player) {
        PacketPlayOutOpenWindow window = new PacketPlayOutOpenWindow();
        window.set("windowId", id())
                .set("inventoryType", type)
                .set("windowTitle", name())
                .set("slots", length())
                .set("entityId", -1);
        player.connection().sendPacket(window);

        // TODO Send a single 0x14 packet with window items
        for (int i = 0; i < length(); i++) {
            PacketPlayOutSetSlot setSlot = new PacketPlayOutSetSlot();
            setSlot.set("windowId", id()).set("slot", (short) i).set("item", new Slot(items()[i]));
            player.connection().sendPacket(setSlot);
        }
        users.add(WeakEntity.of(player));
    }

    public void close(Player player, boolean force) {
        if (force) {
            ((TridentPlayer) player).connection().sendPacket(new PacketPlayOutCloseWindow().set("windowId", id));
        }

        users.remove(WeakEntity.searchFor(player));
    }
}
