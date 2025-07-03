# ConcurrrencyJavaImageProcessor

A modern Java application that demonstrates and benchmarks sequential versus parallel image processing techniques using JavaFX. This project showcases various Java concurrency mechanisms including ExecutorService, Fork/Join framework, parallel streams, and virtual threads.

## 🖼️ Features

- **Multiple Image Filters**: Grayscale, Blur, and Edge Detection
- **Processing Modes**: Sequential and Parallel processing with real-time performance comparison
- **Parallelization Methods**:
  - ExecutorService with configurable thread pools
  - Fork/Join framework for work-stealing parallelism
  - Parallel Streams for functional-style concurrency
  - Virtual Threads (Java 21+) for lightweight concurrency
- **Interactive GUI**: JavaFX-based interface with side-by-side image comparison
- **Performance Metrics**: Real-time processing time measurement and display
- **Image Operations**: Zoom, pan, and save processed images
- **Dark Mode**: Toggle between light and dark themes
- **Docker Support**: Containerized deployment for consistent execution

## 🚀 Quick Start

### Prerequisites
- Java 21 or higher
- Maven (or use the included Maven wrapper)

### Local Development
```bash
# Clone the repository
git clone <repository-url>
cd ConcurrrencyJavaImageProcessor

# Build and run with Maven
mvn clean javafx:run
```

### Using Docker
```bash
# Build the Docker image
docker build -t java-image-processor .

# Run the container (Linux with X11)
docker run -it --rm \
    -e DISPLAY=$DISPLAY \
    -v /tmp/.X11-unix:/tmp/.X11-unix \
    java-image-processor
```

**Note**: For Windows/Mac, you'll need an X server (Xming, VcXsrv, or XQuartz) and may need to adjust the DISPLAY variable.

## 📖 Usage

1. **Select Image**: Click "Select Image" to choose a high-resolution image from your system
2. **Choose Filter**: Select from Grayscale, Blur, or Edge Detection filters
3. **Select Processing Mode**:
   - **Sequential**: Single-threaded processing (baseline)
   - **Parallel**: Multi-threaded processing with configurable method and thread count
4. **Process**: Click "Process" to apply the selected filter and see performance results
5. **Compare**: View original and processed images side by side with zoom/pan capabilities
6. **Save**: Save the processed image to your system

## 🏗️ Architecture

- **Frontend**: JavaFX with FXML for declarative UI layout
- **Backend**: Java 21 with modern concurrency APIs
- **Build System**: Maven with JavaFX Maven Plugin
- **Containerization**: Docker with X11 forwarding for GUI support

## 📊 Performance Benchmarking

The application is designed to demonstrate the performance benefits of parallel processing:
- Compare processing times between sequential and parallel approaches
- Test different parallelization strategies on various image sizes
- Real-time performance metrics display
- Side-by-side visual comparison of results

## 🛠️ Development

### Project Structure
```
src/
├── main/
│   ├── java/gui/
│   │   ├── Controller.java      # Main application logic
│   │   ├── MainApp.java         # JavaFX application entry point
│   │   ├── ImageProcessor.java  # Functional interface for image processing
│   │   └── FilterType.java      # Enum for available filters
│   └── resources/gui/
│       ├── main_view.fxml       # UI layout
│       └── dark-theme.css       # Dark mode styling
Images/                          # Sample images for testing
```

### Key Components
- **Controller**: Manages UI interactions and coordinates image processing
- **ImageProcessor**: Functional interface for implementing different processing strategies
- **FilterType**: Enum defining available image filters
- **Parallel Implementations**: Various concurrency approaches for performance comparison

---
