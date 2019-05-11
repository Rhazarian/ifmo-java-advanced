package ru.ifmo.rain.alekperov.crawler;

import info.kgeorgiy.java.advanced.crawler.*;

import java.io.IOException;
import java.net.MalformedURLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Phaser;
import java.util.stream.Stream;

/**
 * A simple implementation of the {@link Crawler} interface.
 */
public class WebCrawler implements Crawler {

    private final Downloader downloader;
    private final ExecutorService downloadingExecutor;
    private final ExecutorService extractingExecutor;

    private final int perHost;

    private class HostDownloadQueue {

        private final Queue<Runnable> taskQueue;
        private int submitted;

        HostDownloadQueue() {
            taskQueue = new ArrayDeque<>();
            submitted = 0;
        }

        private synchronized void executeNextTask() {
            final Runnable other = taskQueue.poll();
            if (other != null) {
                downloadingExecutor.submit(other);
            } else {
                --submitted;
            }
        }

        synchronized void submitTask(final Runnable task) {
            final Runnable wrappedTask = () -> {
                try {
                    task.run();
                } finally {
                    this.executeNextTask();
                }
            };
            if (submitted < perHost) {
                ++submitted;
                downloadingExecutor.submit(wrappedTask);
            } else {
                taskQueue.add(wrappedTask);
            }
        }

    }

    private final ConcurrentHashMap<String, HostDownloadQueue> taskQueues = new ConcurrentHashMap<>();

    /**
     * Creates a new instance which will use the provided downloader and thread limits.
     *
     * @param downloader the downloader to use.
     * @param downloaders the maximum number of threads to use to download.
     * @param extractors the maximum number of threads to use to extract links.
     * @param perHost the maximum number of threads to use to send a request to the same host.
     */
    public WebCrawler(final Downloader downloader, final int downloaders, final int extractors, final int perHost) {
        this.downloader = downloader;
        this.downloadingExecutor = Executors.newFixedThreadPool(downloaders);
        this.extractingExecutor = Executors.newFixedThreadPool(extractors);
        this.perHost = perHost;
    }

    private HostDownloadQueue getDownloadQueue(final String host) {
        return taskQueues.computeIfAbsent(host, s -> new HostDownloadQueue());
    }

    private void process(final String url, final int depth,
                         final Set<String> downloaded, final Map<String, IOException> errors,
                         final Set<String> processed, final Phaser phaser) {
        if (depth == 0 || !processed.add(url)) {
            return;
        }
        try {
            final var host = URLUtils.getHost(url);
            phaser.register();
            getDownloadQueue(host).submitTask(() -> {
                try {
                    final var doc = downloader.download(url);
                    downloaded.add(url);
                    phaser.register();
                    extractingExecutor.submit(() -> {
                        try {
                            doc.extractLinks().forEach(link -> process(link, depth - 1, downloaded, errors,
                                    processed, phaser));
                        } catch (final IOException ignored) {
                            // Cannot do anything - just skip this document.
                        } finally {
                            phaser.arrive();
                        }
                    });
                } catch (final IOException e) {
                    errors.put(url, e);
                } finally {
                    phaser.arrive();
                }
            });
        } catch (final MalformedURLException e) {
            errors.put(url, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Result download(final String url, final int depth) {
        final Set<String> downloaded = ConcurrentHashMap.newKeySet();
        final Map<String, IOException> errors = new ConcurrentHashMap<>();
        final Set<String> processed = ConcurrentHashMap.newKeySet();
        final Phaser phaser = new Phaser(1);
        process(url, depth, downloaded, errors, processed, phaser);
        phaser.arriveAndAwaitAdvance();
        return new Result(new ArrayList<>(downloaded), errors);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void close() {
        downloadingExecutor.shutdownNow();
        extractingExecutor.shutdownNow();
    }

    private static void showUsage() {
        System.out.printf("Usage: %1$s url [depth [downloaders [extractors [perHost]]]]%n", WebCrawler.class.getName());
    }

    /**
     * Creates a {@link WebCrawler} and runs it depending on the arguments provided.
     * <p>
     * The first argument is the url to start crawl from.
     * The second, the third and the fourth and the fifth arguments are parsed as integers and are used as
     * depth, downloaders, extractors and perHost limitations of the created {@link WebCrawler crawler} respectively.
     * Missing arguments are replaced by {@link Integer#MAX_VALUE}.
     * <p>
     * If there are less than one or more than five arguments, the arguments could not be parsed correctly,
     * or an error occurs during crawling an error message is printed to the standard output.
     *
     * @param args the provided arguments.
     */
    public static void main(String[] args) {
        if (args == null || args.length < 1 || args.length > 5 || Stream.of(args).anyMatch(Objects::isNull)) {
            showUsage();
            return;
        }
        final int[] params = new int[4];
        Arrays.fill(params, Integer.MAX_VALUE);
        try {
            for (int i = 1; i < args.length; i++) {
                params[i - 1] = Integer.parseInt(args[i]);
            }
        } catch (final NumberFormatException e) {
            showUsage();
            return;
        }

        try (final WebCrawler crawler = new WebCrawler(new CachingDownloader(), params[1], params[2], params[3])) {
            crawler.download(args[0], params[0]);
        } catch (final IOException e) {
            System.out.println("Could not create an instance of CachingDownloader:");
            System.out.println(e.getMessage());
        }
    }

}
