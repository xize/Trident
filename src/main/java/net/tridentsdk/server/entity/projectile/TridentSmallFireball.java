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
package net.tridentsdk.server.entity.projectile;

import net.tridentsdk.base.Position;
import net.tridentsdk.entity.living.ProjectileLauncher;
import net.tridentsdk.entity.projectile.SmallFireball;
import net.tridentsdk.entity.traits.EntityProperties;
import net.tridentsdk.entity.types.EntityType;

import java.util.UUID;

/**
 * Represents a small fireball
 *
 * @author The TridentSDK Team
 */
public class TridentSmallFireball extends TridentProjectile implements SmallFireball {
    public TridentSmallFireball(UUID uuid, Position spawnPosition, ProjectileLauncher projectileLauncher) {
        super(uuid, spawnPosition, projectileLauncher);
    }

    @Override
    public void applyProperties(EntityProperties properties) {

    }

    @Override
    protected void hit() {

    }

    @Override
    public EntityType type() {
        return EntityType.SMALL_FIREBALL;
    }
}
