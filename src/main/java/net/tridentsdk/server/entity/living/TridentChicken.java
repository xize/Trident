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

package net.tridentsdk.server.entity.living;

import net.tridentsdk.base.Position;
import net.tridentsdk.entity.living.Chicken;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.entity.types.EntityType;
import net.tridentsdk.event.entity.EntityDamageEvent;
import net.tridentsdk.meta.nbt.ByteTag;
import net.tridentsdk.meta.nbt.CompoundTag;
import net.tridentsdk.meta.nbt.IntTag;
import net.tridentsdk.server.entity.TridentBreedable;

import java.util.UUID;

public class TridentChicken extends TridentBreedable implements Chicken {
    private volatile int layInterval;
    private volatile boolean isJockey;

    public TridentChicken(UUID id, Position spawnLocation) {
        super(id, spawnLocation);
    }

    @Override
    public void doLoad(CompoundTag tag) {
        this.layInterval = ((IntTag) tag.getTag("EggLayTime")).value();
        this.isJockey = ((ByteTag) tag.getTag("IsChickenJockey")).value() == 1;
    }

    @Override
    public boolean isJockey() {
        return isJockey;
    }

    @Override
    public int nextLayInterval() {
        return layInterval;
    }

    @Override
    public EntityDamageEvent lastDamageEvent() {
        return null;
    }

    @Override
    public Player lastPlayerDamager() {
        return null;
    }

    @Override
    public EntityType type() {
        return EntityType.CHICKEN;
    }
}
