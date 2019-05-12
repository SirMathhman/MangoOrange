package com.meti.accelerate;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.util.concurrent.*;

/**
 * @author SirMathhman
 * @version 0.0.0
 * @since 5/12/2019
 */
public class ImageAccelerationTest {
    public static void main(String[] args) {
        int WIDTH = 1024;
        int HEIGHT = 768;

        JFrame frame = new JFrame();
        frame.setSize(new Dimension(WIDTH, HEIGHT));
        Canvas canvas = new Canvas();
        frame.add(canvas);
        frame.setVisible(true);

        canvas.createBufferStrategy(3);
        BufferStrategy strategy = canvas.getBufferStrategy();
        Renderer renderer = new Renderer(frame, strategy);

        ScheduledExecutorService service = Executors.newScheduledThreadPool(2);
        Future<Renderer> rendererFuture = service.submit(renderer);

        FramesPerSecondWriter writer = new FramesPerSecondWriter(renderer);
        Future<?> writerFuture = service.submit(writer);
        frame.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                System.out.println("Window closed.");

                writer.stop();
                renderer.stop();

                writerFuture.cancel(true);
                rendererFuture.cancel(true);

                service.shutdownNow();
                try {
                    boolean terminated = service.awaitTermination(1000, TimeUnit.MILLISECONDS);
                    if (!terminated) {
                        throw new TimeoutException("Termination timeout timed-out.");
                    }
                } catch (Exception e1) {
                    e1.printStackTrace();
                }
                System.exit(0);
            }
        });
    }

    private static class FramesPerSecondWriter implements Runnable {
        private final Renderer renderer;
        private boolean running;

        private FramesPerSecondWriter(Renderer renderer) {
            this.renderer = renderer;
            this.running = true;
        }

        @Override
        public void run() {
            long before = System.nanoTime();
            while (running) {
                long now = System.nanoTime();
                if (now - before > Math.pow(10, 9)) {
                    long framesElapsed = renderer.readFramesElapsed();
                    System.out.println(framesElapsed);

                    before = now;
                }
            }
        }

        void stop() {
            this.running = false;
        }
    }

    private static class Renderer implements Callable<Renderer> {
        private final JFrame frame;
        private final BufferStrategy strategy;
        private long framesElapsed;

        private BufferedImage image;
        private boolean running;

        private Renderer(JFrame frame, BufferStrategy strategy) {
            this.frame = frame;
            this.strategy = strategy;
            this.framesElapsed = 0;

            this.image = scaleImage(frame.getWidth(), frame.getHeight());
            this.running = true;
        }

        private BufferedImage scaleImage(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }

        @Override
        public Renderer call() {
            while (running) {
                do {
                    do {
                        renderFrame();
                        framesElapsed++;
                    } while (strategy.contentsRestored());
                    strategy.show();
                } while (strategy.contentsLost());
            }
            return this;
        }

        private void renderFrame() {
            if (frame.getWidth() != image.getWidth()) {
                this.image = scaleImage(frame.getWidth(), image.getHeight());
            }

            if (frame.getHeight() != image.getHeight()) {
                this.image = scaleImage(image.getWidth(), frame.getHeight());
            }

            Graphics graphics = strategy.getDrawGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
        }

        long readFramesElapsed() {
            long toReturn = this.framesElapsed;
            this.framesElapsed = 0;
            return toReturn;
        }

        void stop() {
            this.running = false;
        }
    }
}