package ru.ifmo.rain.alekperov.mapper;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;

/**
 * A simple implementation of the {@link ParallelMapper} interface.
 */
public class ParallelMapperImpl implements ParallelMapper {

    private final Thread[] workers;
    private final Deque<Runnable> tasks = new ArrayDeque<>();

    /**
     * Creates a new instance which will use the provided number of threads.
     *
     * @param threads the number of threads to create and use.
     * @throws IllegalArgumentException if {@code threads} is less than 1.
     */
    public ParallelMapperImpl(final int threads) {
        if (threads < 1) {
            throw new IllegalArgumentException();
        }
        workers = new Thread[threads];
        for (int i = 0; i < threads; ++i) {
            workers[i] = new Thread(() -> {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        final Runnable task;
                        synchronized (tasks) {
                            while (tasks.isEmpty()) {
                                tasks.wait();
                            }
                            task = tasks.poll();
                        }
                        task.run();
                    }
                } catch (final InterruptedException ignored) {
                    // Finishing.
                }
            });
            workers[i].start();
        }
    }

    private static class MultitaskMonitor {
        private final int count;
        private int counter = 0;

        MultitaskMonitor(final int count) {
            this.count = count;
        }

        private synchronized void increaseCounter() {
            ++counter;
            notify();
        }

        private synchronized boolean finished() {
            return counter == count;
        }

        private synchronized void join() throws InterruptedException {
            while (!finished()) {
                wait();
            }
        }
    }

    private MultitaskMonitor addMultitask(final Runnable... multitask) {
        final var monitor = new MultitaskMonitor(multitask.length);
        synchronized (tasks) {
            for (final var task : multitask) {
                this.tasks.add(() -> {
                    task.run();
                    monitor.increaseCounter();
                });
            }
            tasks.notifyAll();
        }
        return monitor;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, R> List<R> map(final Function<? super T, ? extends R> f, final List<? extends T> args) throws InterruptedException {
        final var tasks = new Runnable[args.size()];
        final List<R> result = new ArrayList<>(Collections.nCopies(tasks.length, null));
        for (int i = 0; i < tasks.length; ++i) {
            final int pos = i;
            tasks[pos] = () -> result.set(pos, f.apply(args.get(pos)));
        }
        final var multitask = addMultitask(tasks);
        multitask.join();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        for (final var worker : workers) {
            worker.interrupt();
        }
        for (final var worker : workers) {
            try {
                worker.join();
            } catch (final InterruptedException ignored) {
                // We're finishing the work.
            }
        }
    }
}
