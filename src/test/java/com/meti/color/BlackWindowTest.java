package com.meti.color;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.util.concurrent.*;
import java.util.function.Consumer;

/**
 * @author SirMathhman
 * @version 0.0.0
 * @since 5/12/2019
 */
public class BlackWindowTest {
    public static final int WIDTH = 1024;
    public static final int HEIGHT = 768;

    public static void main(String[] args) {
        BlackWindowTest test = new BlackWindowTest();
        test.start();
    }

    public void start() {
        SwingWindow window = new SwingWindow(WIDTH, HEIGHT);
        window.show();

        Context context = window.getContext();


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
        Consumer<WindowEvent> onWindowClosing = new BlackWindowClosingConsumer(writer, renderer, writerFuture, rendererFuture, service);
        frame.addWindowListener(new FunctionalWindowAdapter().setOnWindowClosing(onWindowClosing));
    }



    public abstract class Renderer<C extends Context> implements Callable<Renderer<C>> {
        private boolean running = true;
        private final C context;

        protected Renderer(C context) {
            this.context = context;
        }

        @Override
        public Renderer<C> call() {
            while(running){
                render(context);
            }
            return this;
        }

        public abstract void render(C context);
    }

    public interface Context {
        int getHeight();

        int getRGB(int x, int y);

        int getWidth();

        Context setRGB(int x, int y, int rgb);
    }

/*
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
*/

  /*  private static class Renderer implements Callable<Renderer> {
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
    }*/

    private static class FunctionalWindowAdapter extends WindowAdapter {
        private Consumer<WindowEvent> onWindowClosing;

        @Override
        public void windowClosing(WindowEvent e) {
            if (onWindowClosing != null) {
                onWindowClosing.accept(e);
            }
        }

        public FunctionalWindowAdapter setOnWindowClosing(Consumer<WindowEvent> onWindowClosing) {
            this.onWindowClosing = onWindowClosing;
            return this;
        }
    }

    private static class BlackWindowClosingConsumer implements Consumer<WindowEvent> {
        private final FramesPerSecondWriter writer;
        private final Renderer renderer;
        private final Future<?> writerFuture;
        private final Future<Renderer> rendererFuture;
        private final ScheduledExecutorService service;

        public BlackWindowClosingConsumer(FramesPerSecondWriter writer, Renderer renderer, Future<?> writerFuture, Future<Renderer> rendererFuture, ScheduledExecutorService service) {
            this.writer = writer;
            this.renderer = renderer;
            this.writerFuture = writerFuture;
            this.rendererFuture = rendererFuture;
            this.service = service;
        }

        @Override
        public void accept(WindowEvent e12) {
            System.out.println(e12);
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
    }

    private class SwingWindow extends Window<SwingContext> {
        private final JFrame frame;
        private SwingContext context;

        public SwingWindow(int width, int height) {
            super(width, height);
            this.frame = new JFrame();
        }

        @Override
        public void setSize(int width, int height) {
            frame.setSize(width, height);
        }

        @Override
        public Renderer<SwingContext> getRenderer() {
            return new Renderer<>(context) {
                @Override
                public void render(SwingContext context) {

                }
            };
        }

        @Override
        public Context getContext() {
            if (context == null) {
                context = new FrameSwingContext();
            }
            return context;
        }

        @Override
        public void show() {
            frame.setVisible(true);
        }

        private class FrameSwingContext extends SwingContext {
            @Override
            public void addCanvas(Canvas canvas) {
                frame.add(canvas);
            }
        }
    }

    private abstract class SwingContext implements Context {
    private final BufferStrategy strategy;
        private BufferedImage image;
        private int width;
        private int height;

        protected SwingContext() {
            this.width = 0;
            this.height = 0;
        }

        @Override
        public int getHeight() {
            return height;
        }

        @Override
        public int getRGB(int x, int y) {
            int[] pixels = getPixels(checkImage());
            return pixels[getPixelIndex(x, y)];
        }

        public int[] getPixels(BufferedImage image) {
            DataBuffer dataBuffer = image.getRaster().getDataBuffer();
            if (dataBuffer instanceof DataBufferInt) {
                return ((DataBufferInt) dataBuffer).getData();
            } else {
                throw new IllegalArgumentException(image + " does not contain pixels of integer types.");
            }
        }

        public BufferedImage checkImage() {
            boolean equalWidth = image.getWidth() == width;
            boolean equalHeight = image.getHeight() == height;
            if (!equalWidth || !equalHeight) {
                return resize(width, height);
            } else {
                return image;
            }
        }

        public int getPixelIndex(int x, int y) {
            return x + y * width;
        }

        public BufferedImage resize(int width, int height) {
            return new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        }

        @Override
        public int getWidth() {
            return width;
        }

        @Override
        public Context setRGB(int x, int y, int rgb) {
            int[] pixels = getPixels(checkImage());
            pixels[getPixelIndex(x, y)] = rgb;
            return this;
        }
    }

    private abstract class Window<C extends Context> {
        public Window(int width, int height) {
            setSize(width, height);
        }

        public abstract void setSize(int width, int height);

        public abstract Renderer<C> getRenderer();

        public abstract C getContext();

        public abstract void show();
    }
}