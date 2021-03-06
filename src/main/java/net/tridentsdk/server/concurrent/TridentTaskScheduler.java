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

package net.tridentsdk.server.concurrent;

import com.google.common.collect.ForwardingCollection;
import com.google.common.collect.ImmutableList;
import net.tridentsdk.concurrent.*;
import net.tridentsdk.plugin.Plugin;

import javax.annotation.concurrent.ThreadSafe;
import java.util.Collection;
import java.util.Iterator;
import java.util.Queue;
import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Future;

/**
 * TridentTaskScheduler is a scheduling utility that is used to reflect ScheduledTasks at a given offset of the current
 * epoch of the server
 *
 * <p>The scheduler is designed to stage-heavy/run-light philosophy: most overhead in the
 * scheduler is to the run staging, which adds the ScheduledTask to the queue, and constructs the wrapper which assigns
 * the ScheduledTask executor and constructs the logic runnables. In contrast, running the wrapper would perform the
 * pre-constructed logic and mark the ScheduledTask then move on. This ensures that the ScheduledTask will be delayed
 * preferable when it is scheduled, instead of when it will be planned to run.</p>
 *
 * <p>Logic of ScheduledTask types:
 * <ul>
 * <li>Run    - Call as soon as ticked, then cancelled</li>
 * <li>Later  - an AtomicLong is incremented each tick when the long reaches the delay, the ScheduledTask is
 * called and cancelled</li>
 * <li>Repeat - an AtomicLong is incremented each tick, when the long reaches the interval, the ScheduledTask is
 * called, then the long is set to 0 and continues.</li>
 * </ul>
 *
 * The difference between sync and async ScheduledTasks is sync runs on the plugin thread
 * that is from the plugin scheduling the ScheduledTask. This is why a plugin object is required for ScheduledTask
 * scheduling. Async runs on one of the other 2 ScheduledTask execution concurrent (because there are 3 concurrent in the
 * scheduler).</p>
 *
 * <p>The benchmarks and testing units for the TridentTaskScheduler can be found at:
 * http://git.io/nifjcg.</p>
 *
 * <p>Insertion logic places the ScheduledTask wrapped by the implementation of {@link
 * net.tridentsdk.concurrent.ScheduledTask} to perform the run logic and scheduling decisions plus automatic
 * ScheduledTask cancellation. Then, the overriden runnable with the ScheduledTask to be run is {@link
 * net.tridentsdk.concurrent.ScheduledRunnable#markSchedule(net.tridentsdk.concurrent.ScheduledTask)}ed to indicate the
 * ScheduledTask delegate is available.</p>
 *
 * <p>Thread safety is ensured a single iteration thread, the tick thread. Tasks added first put in the task list,
 * then the task is marked. The execution has a higher priority over the access to the task scheduling period. Also,
 * most tasks will be allowed to complete before any change is needed. Task execution occurs in a single thread,
 * the tick method adds to an executor which does not share the state of the task implementation.</p>
 *
 * <p>The scheduler is high performance due to lock-free execution. The internal task list is a
 * {@link java.util.concurrent.ConcurrentLinkedQueue}, iterated in the tick method which schedules a runnable assigned
 * to the task during construction. The most overhead occurs when the runnable is scheduled, and when the logic for
 * the scheduling method is decided during the task wrapper's construction.</p>
 *
 * @author The TridentSDK Team
 */
@ThreadSafe
public class TridentTaskScheduler extends ForwardingCollection<ScheduledTask> implements Scheduler {
    private final Queue<ScheduledTaskImpl> taskList = new ConcurrentLinkedQueue<>();
    private final SelectableThreadPool taskExecutor = ThreadsHandler.configure("Scheduler");

    private TridentTaskScheduler() {
    }

    /**
     * Creates a new scheduler
     *
     * @return the new scheduler
     */
    public static TridentTaskScheduler create() {
        return new TridentTaskScheduler();
    }

    public void tick() {
        Iterator<ScheduledTaskImpl> iterator = taskList.iterator();
        for (; iterator.hasNext(); ) {
            iterator.next().run();
        }
    }

    private ScheduledTaskImpl doAdd(ScheduledTaskImpl wrap) {
        // Does not necessarily need to be atomic, as long as changes are visible
        // taskList is thread-safe
        // markSchedule sets volatile field
        while (true) {
            boolean added = taskList.add(wrap);
            if (added) {
                wrap.runnable().markSchedule(wrap);
                return wrap;
            }
        }
    }

    public void shutdown() {
        taskList.clear();
        taskExecutor.shutdown();
    }

    @Override
    public ScheduledTask asyncRun(Plugin plugin, ScheduledRunnable runnable) {
        return this.doAdd(new ScheduledTaskImpl(plugin, TaskType.ASYNC_RUN, runnable, -1));
    }

    @Override
    public ScheduledTask syncRun(Plugin plugin, ScheduledRunnable runnable) {
        return this.doAdd(new ScheduledTaskImpl(plugin, TaskType.SYNC_RUN, runnable, -1));
    }

    @Override
    public ScheduledTask asyncLater(Plugin plugin, ScheduledRunnable runnable, long delay) {
        return this.doAdd(new ScheduledTaskImpl(plugin, TaskType.ASYNC_LATER, runnable, delay));
    }

    @Override
    public ScheduledTask syncLater(Plugin plugin, ScheduledRunnable runnable, long delay) {
        return this.doAdd(new ScheduledTaskImpl(plugin, TaskType.SYNC_LATER, runnable, delay));
    }

    @Override
    public ScheduledTask asyncRepeat(final Plugin plugin, final ScheduledRunnable runnable, long delay,
                                     final long initialInterval) {
        // Schedule repeating ScheduledTask later
        return this.asyncLater(plugin, new ScheduledRunnable() {
            @Override
            public void run() {
                doAdd(new ScheduledTaskImpl(plugin, TaskType.ASYNC_REPEAT, runnable, initialInterval));
            }
        }, delay);
    }

    @Override
    public ScheduledTask syncRepeat(final Plugin plugin, final ScheduledRunnable runnable, long delay,
                                    final long initialInterval) {
        // Schedule repeating ScheduledTask later
        return this.syncLater(plugin, new ScheduledRunnable() {
            @Override
            public void run() {
                doAdd(new ScheduledTaskImpl(plugin, TaskType.SYNC_REPEAT, runnable, initialInterval));
            }
        }, delay);
    }

    @Override
    protected Collection<ScheduledTask> delegate() {
        return ImmutableList.copyOf(taskList);
    }

    private class ScheduledTaskImpl implements ScheduledTask {
        private final Plugin plugin;
        private final TaskType type;
        private final ScheduledRunnable runnable;

        private final SelectableThread executor;
        private final Runnable runner;

        private volatile long interval;
        private long run = 0L;

        public ScheduledTaskImpl(Plugin plugin, TaskType type, final ScheduledRunnable runnable, long step) {
            this.plugin = plugin;
            this.type = type;
            this.runnable = runnable;
            this.interval = step;

            if (!type.name().contains("REPEAT")) {
                this.runner = () -> {
                    runnable.beforeRun();
                    runnable.run();
                    runnable.afterAsyncRun();
                    cancel();
                };
            } else {
                this.runner = () -> {
                    runnable.beforeRun();
                    runnable.run();
                    runnable.afterAsyncRun();
                };
            }

            if (type.name().contains("ASYNC")) {
                this.executor = taskExecutor.selectCore();
            } else {
                this.executor = new SelectableThread() {
                    @Override public void execute(Runnable task) { TickSync.sync(task); }
                    @Override public <V> Future<V> submitTask(Callable<V> task) { return null; }
                    @Override public void interrupt() {}
                    @Override public Thread asThread() { return null; }
                };
            }
        }

        @Override
        public long interval() {
            return this.interval;
        }

        @Override
        public void setInterval(long interval) {
            this.interval = interval;
        }

        @Override
        public TaskType type() {
            return this.type;
        }

        @Override
        public ScheduledRunnable runnable() {
            return this.runnable;
        }

        @Override
        public Plugin owner() {
            return this.plugin;
        }

        @Override
        public void cancel() {
            taskList.remove(this);
        }

        @Override
        public void run() {
            switch (type) {
                case ASYNC_RUN:
                case SYNC_RUN:
                    this.executor.execute(this.runner);
                    break;

                case ASYNC_LATER:
                case SYNC_LATER:
                    // May be over if the interval set lower
                    if (++run >= interval)
                        this.executor.execute(this.runner);
                    break;

                case ASYNC_REPEAT:
                case SYNC_REPEAT:
                    // May be over if the interval set lower
                    if (++run >= interval) {
                        this.executor.execute(this.runner);
                        run = 0;
                    }
                    break;
                default:
                    throw new IllegalStateException();
            }
        }
    }
}