package ru.ifmo.rain.alekperov.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * A simple implementation of the {@link ListIP} interface.
 */
public class IterativeParallelism implements ListIP {

    private final ParallelMapper mapper;

    public IterativeParallelism() {
        mapper = null;
    }

    public IterativeParallelism(ParallelMapper mapper) {
        this.mapper = mapper;
    }

    private <T> List<Stream<? extends T>> split(final int threads, final List<? extends T> values) {
        final List<Stream<? extends T>> chunks = new ArrayList<>();
        final int chunksCount = Math.min(values.size(), threads);
        final int chunkSize = values.size() / threads;
        for (int i = 0, r = 0, left = values.size() % threads; i < chunksCount; ++i) {
            final int l = r;
            r = l + chunkSize + (left-- > 0 ? 1 : 0);
            chunks.add(values.subList(l, r).stream());
        }
        return chunks;
    }

    private <T, R> R run(final int threads, final List<? extends T> values,
                         final Function<? super Stream<? extends T>, ? extends R> task,
                         final Function<? super Stream<? extends R>, ? extends R> merger) throws InterruptedException {
        final var chunks = split(threads, values);
        final List<Thread> workers = new ArrayList<>();
        final List<R> results;
        if (mapper != null) {
            results = mapper.map(task, chunks);
        } else {
            results = new ArrayList<>(Collections.nCopies(chunks.size(), null));
            for (int i = 0; i < chunks.size(); ++i) {
                final int pos = i;
                final var thread = new Thread(() -> results.set(pos, task.apply(chunks.get(pos))));
                workers.add(thread);
                thread.start();
            }
            InterruptedException ex = null;
            for (final var worker : workers) {
                try {
                    worker.join();
                } catch (final InterruptedException e) {
                    if (ex == null) {
                        ex = e;
                    } else {
                        e.addSuppressed(ex);
                        ex = e;
                    }
                }
            }
            if (ex != null) {
                throw ex;
            }
        }
        return merger.apply(results.stream());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String join(final int threads, final List<?> values) throws InterruptedException {
        return run(threads, values, s -> s.map(Object::toString).collect(Collectors.joining()), s -> s.collect(Collectors.joining()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> List<T> filter(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return run(threads, values, s -> s.filter(predicate).collect(Collectors.toList()), s -> s.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T, U> List<U> map(final int threads, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return run(threads, values, s -> s.map(f).collect(Collectors.toList()), s -> s.flatMap(Collection::stream).collect(Collectors.toList()));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T maximum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException();
        }
        return run(threads, values, s -> s.max(comparator).orElseThrow(), s -> s.max(comparator).orElseThrow());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T minimum(final int threads, final List<? extends T> values, final Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threads, values, Collections.reverseOrder(comparator));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean all(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return run(threads, values, s -> s.allMatch(predicate), s -> s.allMatch(Boolean::booleanValue));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> boolean any(final int threads, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return run(threads, values, s -> s.anyMatch(predicate), s -> s.anyMatch(Boolean::booleanValue));
    }

}
