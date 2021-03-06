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
package net.tridentsdk.server.bench;


import net.tridentsdk.server.world.gen.DefaultWorldGen;
import net.tridentsdk.server.world.gen.FlatWorldGen;
import net.tridentsdk.server.world.gen.SimplexOctaveGenerator;
import net.tridentsdk.world.ChunkLocation;
import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;
import org.openjdk.jmh.runner.options.TimeValue;
import org.openjdk.jmh.runner.options.VerboseMode;

import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;

@State(Scope.Thread)
public class DefaultGeneratorTest {
    
    Random rand;
    DefaultWorldGen generator;
    FlatWorldGen flatGenerator;
    SimplexOctaveGenerator noiseGenerator;
    
    ChunkLocation toGen;
    
    int x;
    int y;
    int z;
    
    @Param({"0","1","5","10","20","40"})
    int cpuTokens;
    
    public static void main (String [] args) {
        Options options = new OptionsBuilder().include(".*" + DefaultGeneratorTest.class.getSimpleName() + ".*")
                .warmupTime(TimeValue.milliseconds(5))
                .warmupIterations(2)
                .threads(2)
                .shouldDoGC(false)
                .measurementIterations(6)
                .verbosity(VerboseMode.NORMAL)
                .timeUnit(TimeUnit.NANOSECONDS)
                .mode(Mode.AverageTime)
                .forks(1)
                .build();
        try {
            new Runner(options).run();
        } catch (RunnerException e) {
            e.printStackTrace();
        }
    }
    
    @Setup(Level.Trial)
    public void setUpMembers() {
        this.rand = new Random();
        this.generator = new DefaultWorldGen(ThreadLocalRandom.current().nextLong());
        this.flatGenerator = new FlatWorldGen(ThreadLocalRandom.current().nextLong());
        this.noiseGenerator = new SimplexOctaveGenerator(8,0.5,666);
    }   
    
    @Setup(Level.Iteration)
    public void setRandoms() {
        this.toGen = ChunkLocation.create(rand.nextInt()/2, rand.nextInt()/2);
        
        this.x = rand.nextInt();
        this.y = rand.nextInt();
        this.z = rand.nextInt();
    }
    
    /*@Benchmark
    public char[][] testGen () {
        return generator.generateBlocks(toGen);
    }
    
    @Benchmark
    public char[][] testFlatGen () {
        return flatGenerator.generateBlocks(toGen);
    }*/
    
    @Benchmark
    public double testSimplex() {
        Blackhole.consumeCPU(cpuTokens);
        return noiseGenerator.noise(x, y);
    }
    
    @Benchmark
    public double testSimplex2() {
        Blackhole.consumeCPU(cpuTokens);
        return noiseGenerator.noise(x, y, z);
        
    }
}
