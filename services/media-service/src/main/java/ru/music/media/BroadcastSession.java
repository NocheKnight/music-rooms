package ru.music.media;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.atomic.AtomicBoolean;

@Slf4j
public class BroadcastSession {
    private final Set<OutputStream> subscribers = new CopyOnWriteArraySet<>();

    private final Process ffmpegProcess;
    private final Thread readerThread;

    private final AtomicBoolean stopped = new AtomicBoolean(false);
    private volatile boolean paused = false;

    private final Runnable onFinished;

    public BroadcastSession(InputStream ffmpegStream, Process ffmpegProcess, Runnable onFinished) {
        this.ffmpegProcess = ffmpegProcess;
        this.onFinished = onFinished;

        this.readerThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            int bytesRead;

            try {
                while ((bytesRead = ffmpegStream.read(buffer)) != -1) {
                    while (paused && !stopped.get()) {
                        Thread.sleep(100);
                    }

                    if (stopped.get()) break;

                    broadcast(buffer, bytesRead);
                }
            } catch (IOException e) {
                log.error("Stream read error", e);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                stop();
            }
        });

        this.readerThread.setDaemon(true);
        this.readerThread.start();
    }

    public void subscribe(OutputStream clientStream) {
        subscribers.add(clientStream);
    }

    public void unsubscribe(OutputStream clientStream) {
        subscribers.remove(clientStream);

        if (subscribers.isEmpty()) {
            stop();
        }
    }

    private void broadcast(byte[] buffer, int length) {
        if (paused) return;

        for (OutputStream client : subscribers) {
            try {
                client.write(buffer, 0, length);
                client.flush();
            } catch (IOException e) {
                subscribers.remove(client);
            }
        }
    }

    public void stop() {
        if (!stopped.compareAndSet(false, true)) return;

        readerThread.interrupt();
        ffmpegProcess.destroy();

        try {
            ffmpegProcess.waitFor(5, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            ffmpegProcess.destroyForcibly();
            subscribers.clear();
        }
    }

    public void pause() { paused = true; }
    public void resume() { paused = false; }

    public void awaitTermination() throws InterruptedException {
        readerThread.join();
    }


    public boolean hasSubscribers() {
        return !subscribers.isEmpty();
    }
}
