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
import net.tridentsdk.base.Position;
import net.tridentsdk.effect.particle.ParticleEffectType;
import net.tridentsdk.server.netty.Codec;
import net.tridentsdk.server.netty.packet.OutPacket;
import net.tridentsdk.util.Vector;

public class PacketPlayOutParticle extends OutPacket {
    protected ParticleEffectType particle;
    protected boolean distance;
    protected Position loc;
    protected Vector offset; // d - (d * Random#nextGaussian())
    protected float particleData;
    protected int count;
    protected int[] data;

    @Override
    public int id() {
        return 0x22;
    }

    public ParticleEffectType particle() {
        return this.particle;
    }

    public boolean isDistance() {
        return this.distance;
    }

    public Position location() {
        return this.loc;
    }

    public Vector offset() {
        return this.offset;
    }

    public float particleData() {
        return this.particleData;
    }

    public int count() {
        return this.count;
    }

    public int[] data() {
        return this.data;
    }

    @Override
    public void encode(ByteBuf buf) {
        buf.writeInt(this.particle.id());
        buf.writeBoolean(this.distance);

        buf.writeFloat((float) this.loc.x());
        buf.writeFloat((float) this.loc.y());
        buf.writeFloat((float) this.loc.z());

        buf.writeFloat((float) this.offset.x());
        buf.writeFloat((float) this.offset.y());
        buf.writeFloat((float) this.offset.z());

        buf.writeFloat(this.particleData);
        buf.writeInt(this.count);

        for (int i : this.data) {
            Codec.writeVarInt32(buf, i);
        }
    }
}
