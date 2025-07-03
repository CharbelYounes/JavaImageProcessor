package gui;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Window;
import javafx.scene.image.Image;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import javafx.scene.control.Alert;
import javafx.fxml.Initializable;
import java.net.URL;
import java.util.ResourceBundle;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import java.awt.image.BufferedImage;
import javafx.embed.swing.SwingFXUtils;
import java.util.concurrent.*;
import java.util.List;
import java.util.ArrayList;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.scene.input.ScrollEvent;
import javafx.scene.Group;
import javafx.scene.control.ScrollPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.Pane;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.geometry.Bounds;
import javafx.scene.control.Spinner;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ToggleGroup;
import javafx.scene.control.TextField;
import javafx.animation.FadeTransition;
import javafx.scene.effect.Glow;
import javafx.util.Duration;

public class Controller implements Initializable {
    @FXML private Button selectImageButton;
    @FXML private ComboBox<String> filterComboBox;
    @FXML private RadioButton sequentialRadio;
    @FXML private RadioButton parallelRadio;
    @FXML private ComboBox<String> parallelMethodComboBox;
    @FXML private Spinner<Integer> threadCountSpinner;
    @FXML private Button processButton;
    @FXML private ImageView originalImageView;
    @FXML private ImageView processedImageView;
    @FXML private TextField timeLabelField;
    @FXML private ProgressBar progressBar;
    private ToggleGroup modeToggleGroup;
    @FXML private CheckBox darkModeCheckBox;
    @FXML private ScrollPane originalScrollPane;
    @FXML private ScrollPane processedScrollPane;
    @FXML private Pane originalImagePane;
    @FXML private Pane processedImagePane;
    private double originalImageScale = 1.0;
    private double processedImageScale = 1.0;
    private static final double ZOOM_FACTOR = 1.1;
    private static final double MIN_ZOOM = 0.1;
    private static final double MAX_ZOOM = 10.0;

    private File selectedImageFile;
    private Image originalImage;

    @FXML private Group originalImageGroup;
    @FXML private Group processedImageGroup;

    @FXML private Button saveImageButton;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Populate filter options
        filterComboBox.getItems().setAll("Grayscale", "Blur", "Edge Detection");
        filterComboBox.getSelectionModel().selectFirst();

        // Populate parallel method options
        parallelMethodComboBox.getItems().setAll(
            "ExecutorService",
            "ForkJoin",
            "ParallelStream",
            "VirtualThreads"
        );
        parallelMethodComboBox.getSelectionModel().selectFirst();

        // Set up thread count spinner
        threadCountSpinner.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(1, 64, 4));
        threadCountSpinner.setEditable(true);
        threadCountSpinner.setDisable(true);
        parallelMethodComboBox.setDisable(true);

        // Set up ToggleGroup for mode selection
        modeToggleGroup = new ToggleGroup();
        sequentialRadio.setToggleGroup(modeToggleGroup);
        parallelRadio.setToggleGroup(modeToggleGroup);

        // Listen for mode changes
        modeToggleGroup.selectedToggleProperty().addListener(new ChangeListener<Toggle>() {
            @Override
            public void changed(ObservableValue<? extends Toggle> obs, Toggle oldToggle, Toggle newToggle) {
                updateParallelOptions();
            }
        });
        updateParallelOptions();

        // Dark mode toggle
        darkModeCheckBox.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
            Scene scene = selectImageButton.getScene();
            if (scene != null) {
                if (isSelected) {
                    scene.getStylesheets().add(getClass().getResource("/gui/dark-theme.css").toExternalForm());
                } else {
                    scene.getStylesheets().remove(getClass().getResource("/gui/dark-theme.css").toExternalForm());
                }
            }
        });

        // Zoom for original image
        originalScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;
            if (originalImageView.getImage() == null) return;

            double oldScale = originalImageScale;
            double zoomFactor = (event.getDeltaY() > 0) ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
            double newScale = oldScale * zoomFactor;
            newScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newScale));
            if (newScale == oldScale) return;

            // Always zoom towards the center of the content
            double contentWidth = originalImageView.getParent().getBoundsInLocal().getWidth();
            double contentHeight = originalImageView.getParent().getBoundsInLocal().getHeight();
            double viewportWidth = originalScrollPane.getViewportBounds().getWidth();
            double viewportHeight = originalScrollPane.getViewportBounds().getHeight();
            double centerX = contentWidth / 2.0;
            double centerY = contentHeight / 2.0;

            originalImageScale = newScale;
            originalImageView.setScaleX(newScale);
            originalImageView.setScaleY(newScale);

            Platform.runLater(() -> {
                double newContentWidth = originalImageView.getParent().getBoundsInLocal().getWidth();
                double newContentHeight = originalImageView.getParent().getBoundsInLocal().getHeight();
                double hValue = (centerX - viewportWidth / 2) / (newContentWidth - viewportWidth);
                double vValue = (centerY - viewportHeight / 2) / (newContentHeight - viewportHeight);
                originalScrollPane.setHvalue(Math.max(0, Math.min(1, hValue)));
                originalScrollPane.setVvalue(Math.max(0, Math.min(1, vValue)));
            });
            event.consume();
        });
        // Zoom for processed image
        processedScrollPane.addEventFilter(ScrollEvent.SCROLL, event -> {
            if (event.getDeltaY() == 0) return;
            if (processedImageView.getImage() == null) return;

            double oldScale = processedImageScale;
            double zoomFactor = (event.getDeltaY() > 0) ? ZOOM_FACTOR : 1.0 / ZOOM_FACTOR;
            double newScale = oldScale * zoomFactor;
            newScale = Math.max(MIN_ZOOM, Math.min(MAX_ZOOM, newScale));
            if (newScale == oldScale) return;

            // Always zoom towards the center of the content
            double contentWidth = processedImageView.getParent().getBoundsInLocal().getWidth();
            double contentHeight = processedImageView.getParent().getBoundsInLocal().getHeight();
            double viewportWidth = processedScrollPane.getViewportBounds().getWidth();
            double viewportHeight = processedScrollPane.getViewportBounds().getHeight();
            double centerX = contentWidth / 2.0;
            double centerY = contentHeight / 2.0;

            processedImageScale = newScale;
            processedImageView.setScaleX(newScale);
            processedImageView.setScaleY(newScale);

            Platform.runLater(() -> {
                double newContentWidth = processedImageView.getParent().getBoundsInLocal().getWidth();
                double newContentHeight = processedImageView.getParent().getBoundsInLocal().getHeight();
                double hValue = (centerX - viewportWidth / 2) / (newContentWidth - viewportWidth);
                double vValue = (centerY - viewportHeight / 2) / (newContentHeight - viewportHeight);
                processedScrollPane.setHvalue(Math.max(0, Math.min(1, hValue)));
                processedScrollPane.setVvalue(Math.max(0, Math.min(1, vValue)));
            });
            event.consume();
        });

        // Double-click to reset zoom for original image
        originalImageView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (originalImageView.getImage() == null) return;
                fitImageToWindow(originalImageView, originalScrollPane, true);
            }
        });
        // Double-click to reset zoom for processed image
        processedImageView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                if (processedImageView.getImage() == null) return;
                fitImageToWindow(processedImageView, processedScrollPane, false);
            }
        });
    }

    private void updateParallelOptions() {
        boolean isParallel = parallelRadio.isSelected();
        parallelMethodComboBox.setDisable(!isParallel);
        threadCountSpinner.setDisable(!isParallel);
    }

    // Event handler stubs
    @FXML private void onSelectImage() {
        // Get the window from any UI element
        Window window = selectImageButton.getScene().getWindow();
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.bmp", "*.gif")
        );
        File file = fileChooser.showOpenDialog(window);
        if (file != null) {
            try {
                Image image = new Image(new FileInputStream(file));
                originalImageView.setImage(image);
                processedImageView.setImage(null);
                selectedImageFile = file;
                originalImage = image;
                // Fit-to-window and center for original image
                fitImageToWindow(originalImageView, originalScrollPane, true);
            } catch (FileNotFoundException e) {
                showError("Could not load image: " + e.getMessage());
            }
        }
    }

    private BufferedImage processSequential(BufferedImage input, FilterType filter) {
        switch (filter) {
            case GRAYSCALE:
                return applyGrayscale(input);
            case BLUR:
                return applyBlur(input);
            case EDGE_DETECTION:
                return applyEdgeDetection(input);
            default:
                throw new IllegalArgumentException("Unknown filter");
        }
    }

    private BufferedImage applyGrayscale(BufferedImage input) {
        BufferedImage output = new BufferedImage(input.getWidth(), input.getHeight(), BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < input.getHeight(); y++) {
            for (int x = 0; x < input.getWidth(); x++) {
                int rgb = input.getRGB(x, y);
                int a = (rgb >> 24) & 0xff;
                int r = (rgb >> 16) & 0xff;
                int g = (rgb >> 8) & 0xff;
                int b = rgb & 0xff;
                int gray = (r + g + b) / 3;
                int grb = (a << 24) | (gray << 16) | (gray << 8) | gray;
                output.setRGB(x, y, grb);
            }
        }
        return output;
    }

    private BufferedImage applyBlur(BufferedImage input) {
        // Apply blur twice for a stronger effect
        BufferedImage temp = blurOnce(input);
        return blurOnce(temp);
    }

    private BufferedImage blurOnce(BufferedImage input) {
        int w = input.getWidth();
        int h = input.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[] dx = {-1, 0, 1, -1, 0, 1, -1, 0, 1};
        int[] dy = {-1, -1, -1, 0, 0, 0, 1, 1, 1};
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int sumR = 0, sumG = 0, sumB = 0, sumA = 0;
                for (int k = 0; k < 9; k++) {
                    int rgb = input.getRGB(x + dx[k], y + dy[k]);
                    sumA += (rgb >> 24) & 0xff;
                    sumR += (rgb >> 16) & 0xff;
                    sumG += (rgb >> 8) & 0xff;
                    sumB += rgb & 0xff;
                }
                int a = sumA / 9;
                int r = sumR / 9;
                int g = sumG / 9;
                int b = sumB / 9;
                int brg = (a << 24) | (r << 16) | (g << 8) | b;
                output.setRGB(x, y, brg);
            }
        }
        return output;
    }

    private BufferedImage applyEdgeDetection(BufferedImage input) {
        int w = input.getWidth();
        int h = input.getHeight();
        BufferedImage output = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        int[][] kernel = {
            {-1, -1, -1},
            {-1, 8, -1},
            {-1, -1, -1}
        };
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int sumR = 0, sumG = 0, sumB = 0;
                for (int ky = -1; ky <= 1; ky++) {
                    for (int kx = -1; kx <= 1; kx++) {
                        int rgb = input.getRGB(x + kx, y + ky);
                        int r = (rgb >> 16) & 0xff;
                        int g = (rgb >> 8) & 0xff;
                        int b = rgb & 0xff;
                        int k = kernel[ky + 1][kx + 1];
                        sumR += r * k;
                        sumG += g * k;
                        sumB += b * k;
                    }
                }
                int r = Math.min(Math.max(sumR, 0), 255);
                int g = Math.min(Math.max(sumG, 0), 255);
                int b = Math.min(Math.max(sumB, 0), 255);
                int a = (input.getRGB(x, y) >> 24) & 0xff;
                int erg = (a << 24) | (r << 16) | (g << 8) | b;
                output.setRGB(x, y, erg);
            }
        }
        return output;
    }

    private BufferedImage processWithExecutorService(BufferedImage input, FilterType filter, int numThreads) throws InterruptedException, ExecutionException {
        int height = input.getHeight();
        int width = input.getWidth();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);
        int bandHeight = height / numThreads;
        Future<?>[] futures = new Future<?>[numThreads];
        BufferedImage blurInput = input;
        if (filter == FilterType.BLUR) {
            blurInput = blurOnce(input);
        }
        for (int t = 0; t < numThreads; t++) {
            final int yStart = t * bandHeight;
            final int yEnd = (t == numThreads - 1) ? height : (t + 1) * bandHeight;
            final BufferedImage bandInput = blurInput;
            futures[t] = executor.submit(() -> {
                for (int y = yStart; y < yEnd; y++) {
                    for (int x = 0; x < width; x++) {
                        int rgb = bandInput.getRGB(x, y);
                        int result = rgb;
                        switch (filter) {
                            case GRAYSCALE:
                                int a = (rgb >> 24) & 0xff;
                                int r = (rgb >> 16) & 0xff;
                                int g = (rgb >> 8) & 0xff;
                                int b = rgb & 0xff;
                                int gray = (r + g + b) / 3;
                                result = (a << 24) | (gray << 16) | (gray << 8) | gray;
                                break;
                            case BLUR:
                                if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                                    int sumR = 0, sumG = 0, sumB = 0, sumA = 0;
                                    for (int dy = -1; dy <= 1; dy++) {
                                        for (int dx = -1; dx <= 1; dx++) {
                                            int p = bandInput.getRGB(x + dx, y + dy);
                                            sumA += (p >> 24) & 0xff;
                                            sumR += (p >> 16) & 0xff;
                                            sumG += (p >> 8) & 0xff;
                                            sumB += p & 0xff;
                                        }
                                    }
                                    int a2 = sumA / 9;
                                    int r2 = sumR / 9;
                                    int g2 = sumG / 9;
                                    int b2 = sumB / 9;
                                    result = (a2 << 24) | (r2 << 16) | (g2 << 8) | b2;
                                }
                                break;
                            case EDGE_DETECTION:
                                if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                                    int[][] kernel = {
                                        {-1, -1, -1},
                                        {-1, 8, -1},
                                        {-1, -1, -1}
                                    };
                                    int sumR = 0, sumG = 0, sumB = 0;
                                    for (int ky = -1; ky <= 1; ky++) {
                                        for (int kx = -1; kx <= 1; kx++) {
                                            int p = bandInput.getRGB(x + kx, y + ky);
                                            int r3 = (p >> 16) & 0xff;
                                            int g3 = (p >> 8) & 0xff;
                                            int b3 = p & 0xff;
                                            int k = kernel[ky + 1][kx + 1];
                                            sumR += r3 * k;
                                            sumG += g3 * k;
                                            sumB += b3 * k;
                                        }
                                    }
                                    int r3 = Math.min(Math.max(sumR, 0), 255);
                                    int g3 = Math.min(Math.max(sumG, 0), 255);
                                    int b3 = Math.min(Math.max(sumB, 0), 255);
                                    int a3 = (bandInput.getRGB(x, y) >> 24) & 0xff;
                                    result = (a3 << 24) | (r3 << 16) | (g3 << 8) | b3;
                                }
                                break;
                        }
                        output.setRGB(x, y, result);
                    }
                }
            });
        }
        for (Future<?> f : futures) f.get();
        executor.shutdown();
        return output;
    }

    // ForkJoin implementation
    private BufferedImage processWithForkJoin(BufferedImage input, FilterType filter, int numThreads) throws InterruptedException, ExecutionException {
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        BufferedImage blurInput = input;
        if (filter == FilterType.BLUR) {
            blurInput = blurOnce(input);
        }
        ForkJoinPool pool = new ForkJoinPool(numThreads);
        try {
            pool.invoke(new ImageTask(blurInput, output, filter, 0, height));
        } finally {
            pool.shutdown();
            pool.awaitTermination(1, TimeUnit.MINUTES);
        }
        return output;
    }

    private static class ImageTask extends RecursiveAction {
        private static final int THRESHOLD = 100;
        private final BufferedImage input, output;
        private final FilterType filter;
        private final int yStart, yEnd;
        ImageTask(BufferedImage input, BufferedImage output, FilterType filter, int yStart, int yEnd) {
            this.input = input; this.output = output; this.filter = filter; this.yStart = yStart; this.yEnd = yEnd;
        }
        @Override
        protected void compute() {
            if (yEnd - yStart <= THRESHOLD) {
                for (int y = yStart; y < yEnd; y++) {
                    for (int x = 0; x < input.getWidth(); x++) {
                        int rgb = input.getRGB(x, y);
                        int result = rgb;
                        switch (filter) {
                            case GRAYSCALE:
                                int a = (rgb >> 24) & 0xff;
                                int r = (rgb >> 16) & 0xff;
                                int g = (rgb >> 8) & 0xff;
                                int b = rgb & 0xff;
                                int gray = (r + g + b) / 3;
                                result = (a << 24) | (gray << 16) | (gray << 8) | gray;
                                break;
                            case BLUR:
                                if (x > 0 && x < input.getWidth() - 1 && y > 0 && y < input.getHeight() - 1) {
                                    int sumR = 0, sumG = 0, sumB = 0, sumA = 0;
                                    for (int dy = -1; dy <= 1; dy++) {
                                        for (int dx = -1; dx <= 1; dx++) {
                                            int p = input.getRGB(x + dx, y + dy);
                                            sumA += (p >> 24) & 0xff;
                                            sumR += (p >> 16) & 0xff;
                                            sumG += (p >> 8) & 0xff;
                                            sumB += p & 0xff;
                                        }
                                    }
                                    int a2 = sumA / 9;
                                    int r2 = sumR / 9;
                                    int g2 = sumG / 9;
                                    int b2 = sumB / 9;
                                    result = (a2 << 24) | (r2 << 16) | (g2 << 8) | b2;
                                }
                                break;
                            case EDGE_DETECTION:
                                if (x > 0 && x < input.getWidth() - 1 && y > 0 && y < input.getHeight() - 1) {
                                    int[][] kernel = {
                                        {-1, -1, -1},
                                        {-1, 8, -1},
                                        {-1, -1, -1}
                                    };
                                    int sumR = 0, sumG = 0, sumB = 0;
                                    for (int ky = -1; ky <= 1; ky++) {
                                        for (int kx = -1; kx <= 1; kx++) {
                                            int p = input.getRGB(x + kx, y + ky);
                                            int r3 = (p >> 16) & 0xff;
                                            int g3 = (p >> 8) & 0xff;
                                            int b3 = p & 0xff;
                                            int k = kernel[ky + 1][kx + 1];
                                            sumR += r3 * k;
                                            sumG += g3 * k;
                                            sumB += b3 * k;
                                        }
                                    }
                                    int r3 = Math.min(Math.max(sumR, 0), 255);
                                    int g3 = Math.min(Math.max(sumG, 0), 255);
                                    int b3 = Math.min(Math.max(sumB, 0), 255);
                                    int a3 = (input.getRGB(x, y) >> 24) & 0xff;
                                    result = (a3 << 24) | (r3 << 16) | (g3 << 8) | b3;
                                }
                                break;
                        }
                        output.setRGB(x, y, result);
                    }
                }
            } else {
                int mid = (yStart + yEnd) / 2;
                invokeAll(new ImageTask(input, output, filter, yStart, mid),
                          new ImageTask(input, output, filter, mid, yEnd));
            }
        }
    }

    // Parallel Stream implementation
    private BufferedImage processWithParallelStream(BufferedImage input, FilterType filter) {
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        final BufferedImage blurInput = (filter == FilterType.BLUR) ? blurOnce(input) : input;
        java.util.stream.IntStream.range(0, height).parallel().forEach(y -> {
            for (int x = 0; x < width; x++) {
                int rgb = blurInput.getRGB(x, y);
                int result = rgb;
                switch (filter) {
                    case GRAYSCALE:
                        int a = (rgb >> 24) & 0xff;
                        int r = (rgb >> 16) & 0xff;
                        int g = (rgb >> 8) & 0xff;
                        int b = rgb & 0xff;
                        int gray = (r + g + b) / 3;
                        result = (a << 24) | (gray << 16) | (gray << 8) | gray;
                        break;
                    case BLUR:
                        if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                            int sumR = 0, sumG = 0, sumB = 0, sumA = 0;
                            for (int dy = -1; dy <= 1; dy++) {
                                for (int dx = -1; dx <= 1; dx++) {
                                    int p = blurInput.getRGB(x + dx, y + dy);
                                    sumA += (p >> 24) & 0xff;
                                    sumR += (p >> 16) & 0xff;
                                    sumG += (p >> 8) & 0xff;
                                    sumB += p & 0xff;
                                }
                            }
                            int a2 = sumA / 9;
                            int r2 = sumR / 9;
                            int g2 = sumG / 9;
                            int b2 = sumB / 9;
                            result = (a2 << 24) | (r2 << 16) | (g2 << 8) | b2;
                        }
                        break;
                    case EDGE_DETECTION:
                        if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                            int[][] kernel = {
                                {-1, -1, -1},
                                {-1, 8, -1},
                                {-1, -1, -1}
                            };
                            int sumR = 0, sumG = 0, sumB = 0;
                            for (int ky = -1; ky <= 1; ky++) {
                                for (int kx = -1; kx <= 1; kx++) {
                                    int p = blurInput.getRGB(x + kx, y + ky);
                                    int r3 = (p >> 16) & 0xff;
                                    int g3 = (p >> 8) & 0xff;
                                    int b3 = p & 0xff;
                                    int k = kernel[ky + 1][kx + 1];
                                    sumR += r3 * k;
                                    sumG += g3 * k;
                                    sumB += b3 * k;
                                }
                            }
                            int r3 = Math.min(Math.max(sumR, 0), 255);
                            int g3 = Math.min(Math.max(sumG, 0), 255);
                            int b3 = Math.min(Math.max(sumB, 0), 255);
                            int a3 = (blurInput.getRGB(x, y) >> 24) & 0xff;
                            result = (a3 << 24) | (r3 << 16) | (g3 << 8) | b3;
                        }
                        break;
                }
                output.setRGB(x, y, result);
            }
        });
        return output;
    }

    // Virtual Threads implementation (Java 21)
    private BufferedImage processWithVirtualThreads(BufferedImage input, FilterType filter, int numThreads) throws InterruptedException {
        int width = input.getWidth();
        int height = input.getHeight();
        BufferedImage output = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        int bandHeight = height / numThreads;
        List<Thread> threads = new ArrayList<>();
        BufferedImage blurInput = input;
        if (filter == FilterType.BLUR) {
            blurInput = blurOnce(input);
        }
        for (int t = 0; t < numThreads; t++) {
            final int yStart = t * bandHeight;
            final int yEnd = (t == numThreads - 1) ? height : (t + 1) * bandHeight;
            final BufferedImage bandInput = blurInput;
            Thread vt = Thread.ofVirtual().start(() -> {
                for (int y = yStart; y < yEnd; y++) {
                    for (int x = 0; x < width; x++) {
                        int rgb = bandInput.getRGB(x, y);
                        int result = rgb;
                        switch (filter) {
                            case GRAYSCALE:
                                int a = (rgb >> 24) & 0xff;
                                int r = (rgb >> 16) & 0xff;
                                int g = (rgb >> 8) & 0xff;
                                int b = rgb & 0xff;
                                int gray = (r + g + b) / 3;
                                result = (a << 24) | (gray << 16) | (gray << 8) | gray;
                                break;
                            case BLUR:
                                if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                                    int sumR = 0, sumG = 0, sumB = 0, sumA = 0;
                                    for (int dy = -1; dy <= 1; dy++) {
                                        for (int dx = -1; dx <= 1; dx++) {
                                            int p = bandInput.getRGB(x + dx, y + dy);
                                            sumA += (p >> 24) & 0xff;
                                            sumR += (p >> 16) & 0xff;
                                            sumG += (p >> 8) & 0xff;
                                            sumB += p & 0xff;
                                        }
                                    }
                                    int a2 = sumA / 9;
                                    int r2 = sumR / 9;
                                    int g2 = sumG / 9;
                                    int b2 = sumB / 9;
                                    result = (a2 << 24) | (r2 << 16) | (g2 << 8) | b2;
                                }
                                break;
                            case EDGE_DETECTION:
                                if (x > 0 && x < width - 1 && y > 0 && y < height - 1) {
                                    int[][] kernel = {
                                        {-1, -1, -1},
                                        {-1, 8, -1},
                                        {-1, -1, -1}
                                    };
                                    int sumR = 0, sumG = 0, sumB = 0;
                                    for (int ky = -1; ky <= 1; ky++) {
                                        for (int kx = -1; kx <= 1; kx++) {
                                            int p = bandInput.getRGB(x + kx, y + ky);
                                            int r3 = (p >> 16) & 0xff;
                                            int g3 = (p >> 8) & 0xff;
                                            int b3 = p & 0xff;
                                            int k = kernel[ky + 1][kx + 1];
                                            sumR += r3 * k;
                                            sumG += g3 * k;
                                            sumB += b3 * k;
                                        }
                                    }
                                    int r3 = Math.min(Math.max(sumR, 0), 255);
                                    int g3 = Math.min(Math.max(sumG, 0), 255);
                                    int b3 = Math.min(Math.max(sumB, 0), 255);
                                    int a3 = (bandInput.getRGB(x, y) >> 24) & 0xff;
                                    result = (a3 << 24) | (r3 << 16) | (g3 << 8) | b3;
                                }
                                break;
                        }
                        output.setRGB(x, y, result);
                    }
                }
            });
            threads.add(vt);
        }
        for (Thread t : threads) t.join();
        return output;
    }

    @FXML
    private void onProcess() {
        if (originalImage == null) {
            showAlert("No image selected.");
            return;
        }
        FilterType filter = FilterType.valueOf(filterComboBox.getValue().toUpperCase().replace(" ", "_"));
        boolean isSequential = sequentialRadio.isSelected();
        String method = parallelMethodComboBox.getValue();
        int numThreads = threadCountSpinner.getValue();

        // Disable UI and show progress bar
        setProcessingUI(true);
        Task<BufferedImage> task = new Task<>() {
            @Override
            protected BufferedImage call() throws Exception {
                BufferedImage input = SwingFXUtils.fromFXImage(originalImage, null);
                long start = System.nanoTime();
                BufferedImage result;
                if (isSequential) {
                    result = processSequential(input, filter);
                } else {
                    if (method == null) throw new IllegalArgumentException("Select a parallel method.");
                    switch (method) {
                        case "ExecutorService":
                            result = processWithExecutorService(input, filter, numThreads);
                            break;
                        case "ForkJoin":
                            result = processWithForkJoin(input, filter, numThreads);
                            break;
                        case "ParallelStream":
                            result = processWithParallelStream(input, filter);
                            break;
                        case "VirtualThreads":
                            result = processWithVirtualThreads(input, filter, numThreads);
                            break;
                        default:
                            throw new IllegalArgumentException("Selected parallel method not implemented yet.");
                    }
                }
                long end = System.nanoTime();
                updateMessage(String.format("Time: %.2f ms", (end - start) / 1_000_000.0));
                return result;
            }
        };
        task.setOnSucceeded(e -> {
            processedImageView.setImage(SwingFXUtils.toFXImage(task.getValue(), null));
            timeLabelField.setText(task.getMessage());
            // Animate with a quick blue glow using CSS class
            timeLabelField.getStyleClass().add("time-label-blue-glow");
            FadeTransition ft = new FadeTransition(Duration.millis(400), timeLabelField);
            ft.setFromValue(1.0);
            ft.setToValue(0.7);
            ft.setAutoReverse(true);
            ft.setCycleCount(2);
            ft.setOnFinished(ev -> timeLabelField.getStyleClass().remove("time-label-blue-glow"));
            ft.play();
            setProcessingUI(false);
            // Fit-to-window and center for processed image
            fitImageToWindow(processedImageView, processedScrollPane, false);
        });
        task.setOnFailed(e -> {
            showAlert("Processing failed: " + task.getException().getMessage());
            setProcessingUI(false);
        });
        progressBar.progressProperty().bind(task.progressProperty());
        new Thread(task).start();
    }

    private void setProcessingUI(boolean processing) {
        processButton.setDisable(processing);
        selectImageButton.setDisable(processing);
        filterComboBox.setDisable(processing);
        sequentialRadio.setDisable(processing);
        parallelRadio.setDisable(processing);
        parallelMethodComboBox.setDisable(processing || sequentialRadio.isSelected());
        threadCountSpinner.setDisable(processing || sequentialRadio.isSelected());
        progressBar.setManaged(processing);
        progressBar.setVisible(processing);
        if (!processing) {
            progressBar.progressProperty().unbind();
            progressBar.setProgress(0);
        } else {
            progressBar.setProgress(-1); // indeterminate
        }
    }

    @FXML private void onModeChanged() {
        updateParallelOptions();
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }

    // Utility method for fit-to-window and centering
    private void fitImageToWindow(ImageView imageView, ScrollPane scrollPane, boolean isOriginal) {
        Platform.runLater(() -> {
            Platform.runLater(() -> {
                if (imageView.getImage() == null) return;
                double viewportWidth = scrollPane.getViewportBounds().getWidth();
                double viewportHeight = scrollPane.getViewportBounds().getHeight();
                double imageWidth = imageView.getImage().getWidth();
                double imageHeight = imageView.getImage().getHeight();
                double scale = Math.min(viewportWidth / imageWidth, viewportHeight / imageHeight);
                scale = Math.min(1.0, scale); // Never zoom in by default
                if (isOriginal) {
                    originalImageScale = scale;
                } else {
                    processedImageScale = scale;
                }
                imageView.setScaleX(scale);
                imageView.setScaleY(scale);
                scrollPane.setHvalue(0.5);
                scrollPane.setVvalue(0.5);
            });
        });
    }

    @FXML
    private void onSaveImage() {
        if (processedImageView.getImage() == null) {
            showAlert("No processed image to save.");
            return;
        }
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Save Processed Image");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("PNG Image", "*.png"),
            new FileChooser.ExtensionFilter("JPEG Image", "*.jpg", "*.jpeg")
        );
        File file = fileChooser.showSaveDialog(saveImageButton.getScene().getWindow());
        if (file != null) {
            try {
                String ext = "png";
                String fileName = file.getName().toLowerCase();
                if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
                    ext = "jpg";
                }
                BufferedImage bImage = SwingFXUtils.fromFXImage(processedImageView.getImage(), null);
                javax.imageio.ImageIO.write(bImage, ext, file);
                showAlert("Image saved successfully.");
            } catch (Exception e) {
                showError("Failed to save image: " + e.getMessage());
            }
        }
    }
} 