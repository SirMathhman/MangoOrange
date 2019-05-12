package com.meti.color;

import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferStrategy;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.util.concurrent.Callable;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
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

        Renderer<SwingContext> renderer = window.getRenderer();
    }


    public interface Context {
        void render();

        int getHeight();

        int getRGB(int x, int y);

        int getWidth();

        Context setRGB(int x, int y, int rgb);
    }

    public class Renderer<C extends Context> implements Callable<Renderer<C>> {
        private boolean running = true;
        private final C context;

        protected Renderer(C context) {
            this.context = context;
        }

        @Override
        public Renderer<C> call() {
            while(running){
                context.render();
            }
            return this;
        }
    }

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
            return new Renderer<>(context);
        }

        @Override
        public SwingContext getContext() {
            if (context == null) {
                context = new SwingContext(frame.getBufferStrategy());
            }
            return context;
        }

        @Override
        public void show() {
            frame.setVisible(true);
        }
    }

    private class SwingContext implements Context {
    private final BufferStrategy strategy;
        private BufferedImage image;
        private int width;
        private int height;

        protected SwingContext(BufferStrategy strategy) {
            this.strategy = strategy;
            this.width = 0;
            this.height = 0;
        }

        @Override
        public void render() {
            do {
                do {
                    renderFrame();
                } while (strategy.contentsRestored());
                strategy.show();
            } while (strategy.contentsLost());
        }

        private void renderFrame() {
            Graphics graphics = strategy.getDrawGraphics();
            graphics.drawImage(image, 0, 0, null);
            graphics.dispose();
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