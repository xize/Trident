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

package net.tridentsdk.server;

import net.tridentsdk.*;
import net.tridentsdk.config.Config;
import net.tridentsdk.entity.living.Player;
import net.tridentsdk.plugin.Plugin;
import net.tridentsdk.registry.Registered;
import net.tridentsdk.server.command.TridentConsole;
import net.tridentsdk.server.concurrent.ConcurrentTaskExecutor;
import net.tridentsdk.server.concurrent.MainThread;
import net.tridentsdk.server.concurrent.ThreadsHandler;
import net.tridentsdk.server.concurrent.TridentTaskScheduler;
import net.tridentsdk.server.netty.protocol.Protocol;
import net.tridentsdk.server.player.TridentPlayer;
import net.tridentsdk.server.service.Statuses;
import net.tridentsdk.server.world.TridentWorld;
import net.tridentsdk.server.world.TridentWorldLoader;
import net.tridentsdk.util.TridentLogger;
import net.tridentsdk.world.World;

import javax.annotation.concurrent.ThreadSafe;
import java.io.IOException;
import java.net.InetAddress;

/**
 * The access base to internal workings of the server
 *
 * @author The TridentSDK Team
 */
@ThreadSafe
public final class TridentServer implements Server {
    // TODO this is temporary for testing
    public static TridentWorld WORLD;
    private final MainThread mainThread;

    private final Config config;
    private final Protocol protocol;
    private final TridentLogger logger;

    private final TridentConsole console;

    final TridentWorldLoader rootWorldLoader;
    private volatile PingInfo pingInfo;

    private TridentServer(Config config) {
        this.config = config;
        this.protocol = new Protocol();
        this.logger = TridentLogger.get(getClass());
        this.mainThread = new MainThread(20);
        this.rootWorldLoader = new TridentWorldLoader();
        this.console = new TridentConsole();
        this.pingInfo = new PingInfo();
    }

    /**
     * Creates the server access base, distributing information to the fields available
     *
     * @param config the configuration to use for option lookup
     */
    public static TridentServer createServer(Config config) {
        TridentServer server = new TridentServer(config);
        Trident.setServer(server);
        server.mainThread.start();
        return server;
        // We CANNOT let the "this" instance escape during creation, else we lose thread-safety
    }

    /**
     * Gets the instance of the server
     *
     * @return the server singleton
     */
    public static TridentServer instance() {
        return (TridentServer) Trident.instance();
    }

    /**
     * Get the protocol base of the server
     *
     * @return the access to server protocol
     */
    public Protocol protocol() {
        return this.protocol;
    }

    public int compressionThreshold() {
        return this.config.getInt("compression-threshold", Defaults.COMPRESSION_THRESHOLD);
    }

    public MainThread mainThread() {
        return mainThread;
    }

    @Override
    public Console console() {
        return console;
    }

    /**
     * Gets the port the server currently runs on
     *
     * @return the port occupied by the server
     */
    @Override
    public int port() {
        return this.config.getInt("port", 25565);
    }

    @Override
    public Config config() {
        return this.config;
    }

    /**
     * Performs the shutdown procedure on the server, ending with the exit of the JVM
     */
    @Override
    public void shutdown() {
        try {
            TridentLogger.get().log("Saving files...");
            try {
                ((Statuses) Registered.statuses()).saveAll();
            } catch (IOException e) {
                e.printStackTrace();
            }

            TridentLogger.get().log("Kicking players...");
            for (Player player : TridentPlayer.players()) {
                ((TridentPlayer) player).kickPlayer("Server shutting down");
                ((TridentPlayer) player).connection().logout();
            }

            TridentLogger.get().log("Saving worlds...");
            for (World world : rootWorldLoader.worlds())
                ((TridentWorld) world).save();

            TridentLogger.get().log("Shutting down scheduler...");
            ((TridentTaskScheduler) Registered.tasks()).shutdown();

            TridentLogger.get().log("Shutting down server process...");
            ThreadsHandler.shutdownAll();

            //TODO: Cleanup stuff...
            TridentLogger.get().log("Shutting down plugins...");
            for (Plugin plugin : Registered.plugins())
                Registered.plugins().disable(plugin);

            TridentLogger.get().log("Shutting down thread pools...");
            ConcurrentTaskExecutor.executors().forEach(ConcurrentTaskExecutor::shutdown);

            TridentLogger.get().log("Shutting down server connections...");
            TridentStart.close();
        } catch (Exception e) {
            TridentLogger.get().error("Server failed to shutdown correctly");
            TridentLogger.get().error(e);
            return;
        }

        TridentLogger.get().log("Server shutdown successfully.");
    }

    @Override
    public InetAddress ip() {
        return null;
    }

    @Override
    public String version() {
        // TODO: Make this more eloquent
        return "0.3-alpha-DP";
    }

    @Override
    public PingInfo info() {
        return pingInfo;
    }

    @Override
    public TridentLogger logger() {
        return logger;
    }
}
