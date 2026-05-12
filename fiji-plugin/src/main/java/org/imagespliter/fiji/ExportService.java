package org.imagespliter.fiji;

import ij.IJ;
import ij.ImagePlus;
import ij.WindowManager;
import ij.plugin.ChannelSplitter;
import ij.plugin.ContrastEnhancer;
import ij.plugin.Duplicator;
import ij.plugin.RGBStackMerge;
import ij.process.ImageStatistics;
import loci.formats.FormatException;
import loci.formats.ImageReader;
import loci.plugins.BF;
import loci.plugins.in.ImporterOptions;

import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Component;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class ExportService {
    private static final String EXPORT_PREVIEW_TITLE = "Z_Export_Preview";
    private final Component owner;
    private final Consumer<String> statusSink;
    private ReviewSession reviewSession;

    ExportService(Component owner, Consumer<String> statusSink) {
        this.owner = owner;
        this.statusSink = statusSink;
    }

    void runRgbBatch(SuiteSettings.RgbBatch settings) throws Exception {
        validateBatchSettings(settings);
        List<Path> files = collectFiles(settings.inputDirectory, settings.fileSuffix);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No matching files found.");
        }
        int processed = 0;
        for (Path file : files) {
            int seriesCount = countSeries(file);
            for (int series = 0; series < seriesCount; series++) {
                statusSink.accept("RGB export: " + file.getFileName() + " series " + (series + 1));
                ImagePlus image = openSeries(file, series, false);
                Path outputBase = buildBatchOutputBase(file, settings, sanitizeOutputName(image.getTitle(), settings.fileSuffix));
                processRgbOutput(image, outputBase, settings);
                closeImage(image);
                processed++;
            }
        }
        statusSink.accept("RGB export finished. Processed " + processed + " series.");
    }

    void runMultiChannelBatch(SuiteSettings.MultiBatch settings) throws Exception {
        validateBatchSettings(settings);
        List<Path> files = collectFiles(settings.inputDirectory, settings.fileSuffix);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No matching files found.");
        }
        if (settings.previewBeforeRun) {
            previewMultiChannel(settings);
        }
        int processed = 0;
        for (Path file : files) {
            int seriesCount = countSeries(file);
            for (int series = 0; series < seriesCount; series++) {
                statusSink.accept("Multichannel export: " + file.getFileName() + " series " + (series + 1));
                ImagePlus image = openSeries(file, series, false);
                if (settings.transformTo8Bit) {
                    convertTo8Bit(image);
                }
                applyColorMapping(image, settings.grayChannel, settings.yellowChannel, settings.cyanChannel, settings.magentaChannel);
                Path outputBase = buildBatchOutputBase(file, settings, sanitizeOutputName(image.getTitle(), settings.fileSuffix));
                saveTiff(image, outputBase + ".tif");
                if (settings.splitChannels) {
                    exportSplitChannels(outputBase.toString(), image, settings.scaleChannel, settings.scaleLength, settings.addScaleBar, true);
                    exportMergedSelectedChannels(outputBase.toString(), image, settings.grayChannel, settings.cyanChannel, settings.magentaChannel, settings.yellowChannel);
                }
                closeImage(image);
                processed++;
            }
        }
        statusSink.accept("Multichannel export finished. Processed " + processed + " series.");
    }

    void previewMultiChannel(SuiteSettings.MultiBatch settings) throws Exception {
        validateBatchSettings(settings);
        List<Path> files = collectFiles(settings.inputDirectory, settings.fileSuffix);
        if (files.isEmpty()) {
            throw new IllegalArgumentException("No matching files found.");
        }
        ImagePlus image = openSeries(files.get(0), 0, true);
        applyColorMapping(image, settings.grayChannel, settings.yellowChannel, settings.cyanChannel, settings.magentaChannel);
        ImagePlus[] splitChannels = ChannelSplitter.split(new Duplicator().run(image));
        StringBuilder builder = new StringBuilder();
        builder.append("Preview file: ").append(files.get(0)).append("\n");
        builder.append("Series: 1\n");
        builder.append("Size: ").append(image.getWidth()).append(" x ").append(image.getHeight()).append("\n");
        builder.append("Z slices: ").append(image.getNSlices()).append(", Frames: ").append(image.getNFrames()).append("\n");
        builder.append("Detected channels: ").append(splitChannels.length).append("\n\n");
        builder.append("Channel statistics:\n");
        for (int index = 0; index < splitChannels.length; index++) {
            ImageStatistics stats = splitChannels[index].getStatistics();
            builder.append("Ch").append(index + 1)
                .append(" | min=").append(IJ.d2s(stats.min, 1))
                .append(", max=").append(IJ.d2s(stats.max, 1))
                .append(", mean=").append(IJ.d2s(stats.mean, 1))
                .append("\n");
            closeImage(splitChannels[index]);
        }
        builder.append("\nColor mapping: Gray=").append(settings.grayChannel)
            .append(", Cyan=").append(settings.cyanChannel)
            .append(", Magenta=").append(settings.magentaChannel)
            .append(", Yellow=").append(settings.yellowChannel);
        JOptionPane.showMessageDialog(owner, builder.toString(), "Multichannel Preview", JOptionPane.INFORMATION_MESSAGE);
        statusSink.accept("Preview loaded in Fiji. Close the preview image when finished.");
    }

    void exportCurrentSlice(Path outputDirectory, SuiteSettings.ZExport settings) throws Exception {
        if (outputDirectory == null) {
            throw new IllegalArgumentException("Choose an output directory first.");
        }
        ImagePlus image = WindowManager.getCurrentImage();
        if (image == null) {
            throw new IllegalStateException("No active image is open.");
        }
        Files.createDirectories(outputDirectory);
        applyReviewPositionAndColors(image, settings);
        String baseName = sanitizeReviewOutputName(image.getTitle());
        exportCurrentSliceOutputs(image, outputDirectory.resolve(baseName).toString(), resolveSlice(image, settings), settings);
        statusSink.accept("Current image export finished.");
    }

    void startFolderReview(Path inputDirectory, SuiteSettings.ZExport settings) throws Exception {
        if (inputDirectory == null || !Files.isDirectory(inputDirectory)) {
            throw new IllegalArgumentException("Choose a valid review folder.");
        }
        List<SuiteSettings.ReviewEntry> entries = collectReviewEntries(inputDirectory);
        if (entries.isEmpty()) {
            throw new IllegalArgumentException("No supported image files were found in the selected folder.");
        }
        stopReview();
        reviewSession = new ReviewSession(entries, settings.copy());
        openReviewEntry(0);
    }

    void applyReviewPreview(SuiteSettings.ZExport settings) {
        ReviewSession session = requireReviewSession();
        session.settings = settings.copy();
        applyReviewPositionAndColors(session.currentImage, session.settings);
        session.currentImage.updateAndDraw();
        statusSink.accept("Review preview updated.");
    }

    void previewCurrentReviewExport(SuiteSettings.ZExport settings) {
        ReviewSession session = requireReviewSession();
        session.settings = settings.copy();
        applyReviewPositionAndColors(session.currentImage, session.settings);
        closeNamedImage(EXPORT_PREVIEW_TITLE);
        ImagePlus preview = duplicateSlice(session.currentImage, resolveSlice(session.currentImage, session.settings));
        preview.setTitle(EXPORT_PREVIEW_TITLE);
        applyColorMapping(preview, session.settings.grayChannel, session.settings.yellowChannel, session.settings.cyanChannel, session.settings.magentaChannel);
        preview.show();
        statusSink.accept("Preview export image updated.");
    }

    void exportCurrentReviewAndNext(SuiteSettings.ZExport settings) throws Exception {
        ReviewSession session = requireReviewSession();
        session.settings = settings.copy();
        applyReviewPositionAndColors(session.currentImage, session.settings);
        int slice = resolveSlice(session.currentImage, session.settings);
        String outputBase = buildReviewOutputBase(session.entry, session.settings);
        exportCurrentSliceOutputs(session.currentImage, outputBase, slice, session.settings);
        openReviewEntry(session.index + 1);
    }

    void nextReviewImage(SuiteSettings.ZExport settings) throws Exception {
        ReviewSession session = requireReviewSession();
        session.settings = settings.copy();
        openReviewEntry(session.index + 1);
    }

    void stopReview() {
        if (reviewSession != null) {
            closeImage(reviewSession.currentImage);
            closeNamedImage(EXPORT_PREVIEW_TITLE);
            reviewSession = null;
            statusSink.accept("Folder review stopped.");
        }
    }

    private void processRgbOutput(ImagePlus image, Path outputBase, SuiteSettings.RgbBatch settings) throws IOException {
        if (settings.exportGrayscale) {
            ImagePlus grayscale = new Duplicator().run(image);
            IJ.run(grayscale, "RGB Color", "");
            if (grayscale.getBitDepth() != 8) {
                IJ.run(grayscale, "8-bit", "");
            }
            IJ.run(grayscale, "Grays", "");
            if (settings.autoWhiteBalance) {
                new ContrastEnhancer().stretchHistogram(grayscale, 0.35);
            }
            saveTiff(grayscale, outputBase + ".tif");
            closeImage(grayscale);
            return;
        }

        ImagePlus rgbSource = new Duplicator().run(image);
        if (settings.autoWhiteBalance) {
            new ContrastEnhancer().stretchHistogram(rgbSource, 0.35);
        }
        Path tempSource = Paths.get(outputBase + "__rgb_source__.tif");
        saveTiff(rgbSource, tempSource.toString());
        closeImage(rgbSource);
        ImagePlus reopened = IJ.openImage(tempSource.toString());
        if (reopened == null) {
            throw new IOException("Failed to reopen temporary RGB source: " + tempSource);
        }
        IJ.run(reopened, "Stack to RGB", "");
        saveTiff(reopened, outputBase + ".tif");
        closeImage(reopened);
        Files.deleteIfExists(tempSource);
    }

    private void exportCurrentSliceOutputs(ImagePlus sourceImage, String outputBase, int sliceNumber, SuiteSettings.ZExport settings) throws IOException {
        ImagePlus raw = duplicateSlice(sourceImage, sliceNumber);
        raw.setTitle("Z_Raw_Export");
        saveTiff(raw, outputBase + "_z" + sliceNumber + "_raw.tif");

        if (settings.splitChannels) {
            exportSplitChannels(outputBase + "_z" + sliceNumber, raw, settings.scaleChannel, settings.scaleLength, settings.addScaleBar, settings.convertSplitTo8Bit);
        }

        ImagePlus composite = new Duplicator().run(raw);
        applyColorMapping(composite, settings.grayChannel, settings.yellowChannel, settings.cyanChannel, settings.magentaChannel);
        saveTiff(composite, outputBase + "_z" + sliceNumber + "_composite.tif");

        Path tempComposite = Paths.get(outputBase + "__z_rgb_source__.tif");
        saveTiff(composite, tempComposite.toString());
        closeImage(composite);

        ImagePlus reopened = IJ.openImage(tempComposite.toString());
        if (reopened == null) {
            throw new IOException("Failed to reopen temporary composite source: " + tempComposite);
        }
        if (reopened.getNChannels() > 1 || reopened.getNSlices() > 1) {
            IJ.run(reopened, "Stack to RGB", "");
        } else {
            IJ.run(reopened, "RGB Color", "");
        }
        saveTiff(reopened, outputBase + "_z" + sliceNumber + "_rgb.tif");
        closeImage(reopened);
        Files.deleteIfExists(tempComposite);
        closeImage(raw);
    }

    private void exportSplitChannels(String outputBase, ImagePlus sourceImage, int scaleChannel, double scaleLength, boolean addScaleBar, boolean convertTo8Bit) throws IOException {
        ImagePlus[] split = ChannelSplitter.split(new Duplicator().run(sourceImage));
        for (int index = 0; index < split.length; index++) {
            ImagePlus channelImage = split[index];
            IJ.run(channelImage, "Grays", "");
            if (convertTo8Bit && channelImage.getBitDepth() != 8) {
                IJ.run(channelImage, "8-bit", "");
            }
            String channelPath = outputBase + "_Ch" + (index + 1) + ".tif";
            saveTiff(channelImage, channelPath);
            if (addScaleBar && scaleChannel == index + 1) {
                ImagePlus scalePreview = new Duplicator().run(channelImage);
                IJ.run(scalePreview, "Scale Bar...", scaleBarOptions(scaleLength));
                ImagePlus flattened = scalePreview.flatten();
                saveTiff(flattened, outputBase + "_Ch" + (index + 1) + "_scalebar.tif");
                closeImage(scalePreview);
                closeImage(flattened);
            }
            closeImage(channelImage);
        }
    }

    private void exportMergedSelectedChannels(String outputBase, ImagePlus sourceImage, int grayChannel, int cyanChannel, int magentaChannel, int yellowChannel) throws IOException {
        ImagePlus[] split = ChannelSplitter.split(new Duplicator().run(sourceImage));
        ImagePlus[] mergeInputs = new ImagePlus[4];
        mergeInputs[0] = selectChannel(split, grayChannel);
        mergeInputs[1] = selectChannel(split, cyanChannel);
        mergeInputs[2] = selectChannel(split, magentaChannel);
        mergeInputs[3] = selectChannel(split, yellowChannel);
        ImagePlus merged = RGBStackMerge.mergeChannels(mergeInputs, true);
        String label = "ch-" + grayChannel + "-" + cyanChannel + "-" + magentaChannel + "-" + yellowChannel;
        saveTiff(merged, outputBase + "_" + label + "_merged.tif");
        closeImage(merged);
        for (ImagePlus image : split) {
            closeImage(image);
        }
    }

    private ImagePlus selectChannel(ImagePlus[] split, int channelIndex) {
        if (channelIndex < 1 || channelIndex > split.length) {
            return null;
        }
        return new Duplicator().run(split[channelIndex - 1]);
    }

    private void applyReviewPositionAndColors(ImagePlus image, SuiteSettings.ZExport settings) {
        if (image == null) {
            throw new IllegalStateException("No review image is open.");
        }
        int channel = clamp(settings.previewChannel, 1, Math.max(1, image.getNChannels()));
        int slice = clamp(settings.previewSlice, 1, Math.max(1, image.getNSlices()));
        image.setC(channel);
        image.setZ(slice);
        applyColorMapping(image, settings.grayChannel, settings.yellowChannel, settings.cyanChannel, settings.magentaChannel);
    }

    private int resolveSlice(ImagePlus image, SuiteSettings.ZExport settings) {
        return clamp(settings.previewSlice, 1, Math.max(1, image.getNSlices()));
    }

    private void applyColorMapping(ImagePlus image, int grayChannel, int yellowChannel, int cyanChannel, int magentaChannel) {
        if (image == null || image.getNChannels() < 2) {
            return;
        }
        runColorCommand(image, grayChannel, "Grays");
        runColorCommand(image, yellowChannel, "Yellow");
        runColorCommand(image, cyanChannel, "Cyan");
        runColorCommand(image, magentaChannel, "Magenta");
        image.updateAndDraw();
    }

    private void runColorCommand(ImagePlus image, int channelIndex, String command) {
        if (channelIndex < 1 || channelIndex > image.getNChannels()) {
            return;
        }
        image.setC(channelIndex);
        IJ.run(image, command, "");
    }

    private void convertTo8Bit(ImagePlus image) {
        if (image.getBitDepth() != 8) {
            IJ.run(image, "8-bit", "");
        }
    }

    private ImagePlus duplicateSlice(ImagePlus sourceImage, int sliceNumber) {
        int frame = clamp(sourceImage.getFrame(), 1, Math.max(1, sourceImage.getNFrames()));
        int slice = clamp(sliceNumber, 1, Math.max(1, sourceImage.getNSlices()));
        Duplicator duplicator = new Duplicator();
        return duplicator.run(sourceImage, 1, Math.max(1, sourceImage.getNChannels()), slice, slice, frame, frame);
    }

    private void openReviewEntry(int index) throws Exception {
        if (reviewSession == null) {
            throw new IllegalStateException("Review session is not active.");
        }
        if (index >= reviewSession.entries.size()) {
            stopReview();
            JOptionPane.showMessageDialog(owner, "Finished reviewing all matching files.", "Folder Review", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        closeImage(reviewSession.currentImage);
        closeNamedImage(EXPORT_PREVIEW_TITLE);
        reviewSession.index = index;
        reviewSession.entry = reviewSession.entries.get(index);
        reviewSession.currentImage = openSeries(reviewSession.entry.path, reviewSession.entry.seriesIndex, true);
        applyReviewPositionAndColors(reviewSession.currentImage, reviewSession.settings);
        statusSink.accept("Reviewing " + reviewSession.entry.path.getFileName() + " series " + (reviewSession.entry.seriesIndex + 1));
    }

    private ImagePlus openSeries(Path file, int seriesIndex, boolean show) throws Exception {
        ImporterOptions options = new ImporterOptions();
        options.setId(file.toString());
        options.setColorMode(ImporterOptions.COLOR_MODE_DEFAULT);
        options.setStackOrder(ImporterOptions.ORDER_XYCZT);
        options.setQuiet(true);
        options.setSeriesOn(seriesIndex, true);
        ImagePlus[] images = BF.openImagePlus(options);
        if (images.length == 0) {
            throw new IOException("Bio-Formats did not open any image for " + file);
        }
        ImagePlus image = images[0];
        if (show) {
            image.show();
        }
        return image;
    }

    private int countSeries(Path file) throws FormatException, IOException {
        ImageReader reader = new ImageReader();
        try {
            reader.setId(file.toString());
            return reader.getSeriesCount();
        } finally {
            reader.close();
        }
    }

    private List<Path> collectFiles(Path inputDirectory, String suffix) throws IOException {
        String normalizedSuffix = suffix == null ? "" : suffix.toLowerCase(Locale.ROOT);
        try (Stream<Path> stream = Files.walk(inputDirectory)) {
            return stream.filter(Files::isRegularFile)
                .filter(path -> normalizedSuffix.isEmpty() || path.getFileName().toString().toLowerCase(Locale.ROOT).endsWith(normalizedSuffix))
                .sorted()
                .collect(Collectors.toList());
        }
    }

    private List<SuiteSettings.ReviewEntry> collectReviewEntries(Path inputDirectory) throws Exception {
        List<SuiteSettings.ReviewEntry> entries = new ArrayList<>();
        try (Stream<Path> stream = Files.walk(inputDirectory)) {
            List<Path> files = stream.filter(Files::isRegularFile)
                .filter(this::isSupportedReviewFile)
                .sorted()
                .collect(Collectors.toList());
            for (Path file : files) {
                int seriesCount = countSeries(file);
                for (int series = 0; series < seriesCount; series++) {
                    entries.add(new SuiteSettings.ReviewEntry(file, series));
                }
            }
        }
        return entries;
    }

    private boolean isSupportedReviewFile(Path path) {
        String name = path.getFileName().toString().toLowerCase(Locale.ROOT);
        return name.endsWith(".lif")
            || name.endsWith(".zvi")
            || name.endsWith(".vsi")
            || name.endsWith(".czi")
            || name.endsWith(".nd2")
            || name.endsWith(".lsm")
            || name.endsWith(".ome.tif")
            || name.endsWith(".ome.tiff")
            || name.endsWith(".tif")
            || name.endsWith(".tiff");
    }

    private Path buildBatchOutputBase(Path sourceFile, SuiteSettings.BatchBase settings, String imageName) throws IOException {
        Path baseDirectory = settings.outputToInput ? sourceFile.getParent() : settings.outputDirectory;
        if (baseDirectory == null) {
            throw new IllegalArgumentException("Choose an output directory or enable output-to-input.");
        }
        Files.createDirectories(baseDirectory);
        String folderName = sourceFile.getParent().getFileName().toString() + "_Image_Splitted_" + LocalDate.now();
        Path outputFolder = baseDirectory.resolve(folderName);
        Files.createDirectories(outputFolder);
        return outputFolder.resolve(imageName);
    }

    private String buildReviewOutputBase(SuiteSettings.ReviewEntry entry, SuiteSettings.ZExport settings) {
        Path parent = entry.path.getParent();
        String fileName = sanitizeReviewOutputName(entry.path.getFileName().toString());
        return parent.resolve(fileName + "_s" + (entry.seriesIndex + 1)).toString();
    }

    private void validateBatchSettings(SuiteSettings.BatchBase settings) {
        Objects.requireNonNull(settings, "settings");
        if (settings.inputDirectory == null || !Files.isDirectory(settings.inputDirectory)) {
            throw new IllegalArgumentException("Choose a valid input directory.");
        }
        if (!settings.outputToInput && (settings.outputDirectory == null || !Files.isDirectory(settings.outputDirectory))) {
            throw new IllegalArgumentException("Choose a valid output directory.");
        }
    }

    private String sanitizeOutputName(String name, String suffix) {
        String clean = name;
        if (suffix != null && !suffix.isBlank() && clean.toLowerCase(Locale.ROOT).endsWith(suffix.toLowerCase(Locale.ROOT))) {
            clean = clean.substring(0, clean.length() - suffix.length());
        }
        return sanitizeName(clean);
    }

    private String sanitizeReviewOutputName(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".ome.tiff")) {
            return sanitizeName(name.substring(0, name.length() - 9));
        }
        if (lower.endsWith(".ome.tif")) {
            return sanitizeName(name.substring(0, name.length() - 8));
        }
        int dot = name.lastIndexOf('.');
        return sanitizeName(dot >= 0 ? name.substring(0, dot) : name);
    }

    private String sanitizeName(String value) {
        return value.replace('-', '_')
            .replace('\\', '_')
            .replace(':', '_')
            .replace('*', '_')
            .replace('?', '_')
            .replace('"', '_')
            .replace('<', '_')
            .replace('>', '_')
            .replace('|', '_')
            .replace("/", "_stitching_");
    }

    private void saveTiff(ImagePlus image, String path) {
        IJ.saveAsTiff(image, path);
    }

    private void closeNamedImage(String title) {
        ImagePlus image = WindowManager.getImage(title);
        closeImage(image);
    }

    private void closeImage(ImagePlus image) {
        if (image == null) {
            return;
        }
        image.changes = false;
        image.close();
    }

    private String scaleBarOptions(double scaleLength) {
        return "width=" + scaleLength + " height=12 font=42 color=White background=None location=[Lower Left] bold overlay";
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private ReviewSession requireReviewSession() {
        if (reviewSession == null || reviewSession.currentImage == null) {
            throw new IllegalStateException("Folder review is not active.");
        }
        return reviewSession;
    }

    private static final class ReviewSession {
        final List<SuiteSettings.ReviewEntry> entries;
        SuiteSettings.ZExport settings;
        int index;
        SuiteSettings.ReviewEntry entry;
        ImagePlus currentImage;

        ReviewSession(List<SuiteSettings.ReviewEntry> entries, SuiteSettings.ZExport settings) {
            this.entries = entries.stream().sorted(Comparator.comparing(item -> item.path.toString())).collect(Collectors.toList());
            this.settings = settings;
        }
    }
}
