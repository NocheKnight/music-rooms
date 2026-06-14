package ru.music.media;

import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;

public class BroadcastSession {
    private final Set<OutputStream> subscribers = new CopyOnWriteArraySet<>();

    private final Process ffmpegProcess;
    private final Thread readerThread;

    public BroadcastSession(InputStream ffmpegStream, Process ffmpegProcess) {
        this.ffmpegProcess = ffmpegProcess;

        this.readerThread = new Thread(() -> {
            byte[] buffer = new byte[4096];
            int bytesRead;

            try {
                while ((bytesRead = ffmpegStream.read(buffer)) != -1) {
                    broadcast(buffer, bytesRead);
                }
            } catch (IOException e) {
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
        readerThread.interrupt();
        ffmpegProcess.destroy();
        subscribers.clear();
    }

    public boolean hasSubscribers() {
        return !subscribers.isEmpty();
    }
}
