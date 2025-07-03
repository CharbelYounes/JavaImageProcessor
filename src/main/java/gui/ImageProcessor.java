package gui;

@FunctionalInterface
public interface ImageProcessor {
    java.awt.image.BufferedImage process(java.awt.image.BufferedImage input);
} 