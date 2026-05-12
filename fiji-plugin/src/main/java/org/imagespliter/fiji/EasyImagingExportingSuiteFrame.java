package org.imagespliter.fiji;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.nio.file.Path;

final class EasyImagingExportingSuiteFrame extends JFrame {
    private final JLabel statusLabel = new JLabel("Ready.");
    private final ExportService exportService = new ExportService(this, this::setStatus);

    EasyImagingExportingSuiteFrame() {
        super("Easy Imaging Exporting Suite");
        setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
        setMinimumSize(new Dimension(980, 720));
        setLayout(new BorderLayout());

        JTabbedPane tabs = new JTabbedPane();
        tabs.addTab("RGB Image", buildRgbPanel());
        tabs.addTab("Multichannel Image", buildMultiChannelPanel());
        tabs.addTab("Z-Slice Export", buildZExportPanel());
        add(tabs, BorderLayout.CENTER);

        JPanel statusPanel = new JPanel(new BorderLayout());
        statusPanel.setBorder(BorderFactory.createEmptyBorder(6, 10, 6, 10));
        statusPanel.add(statusLabel, BorderLayout.WEST);
        add(statusPanel, BorderLayout.SOUTH);

        pack();
        setLocationRelativeTo(null);
    }

    private JPanel buildRgbPanel() {
        JPanel panel = basePanel();
        PathField inputDir = new PathField(this, true);
        PathField outputDir = new PathField(this, true);
        JTextField suffix = new JTextField(".lif", 12);
        JCheckBox outputToInput = new JCheckBox("Output to the same folder as input", true);
        JCheckBox transformTo8Bit = new JCheckBox("Convert images to 8-bit", true);
        JCheckBox addScaleBar = new JCheckBox("Add scale bar");
        JSpinner scaleChannel = integerSpinner(1, 1, 32, 1);
        JSpinner scaleLength = decimalSpinner(50.0, 1.0, 10000.0, 1.0);
        JCheckBox autoWhiteBalance = new JCheckBox("Auto white balance");
        JCheckBox grayscaleOutput = new JCheckBox("Export grayscale final image");
        JButton runButton = new JButton("Run RGB Export");

        int row = 0;
        row = addLabeled(panel, row, "Input directory", inputDir.component);
        row = addLabeled(panel, row, "Output directory", outputDir.component);
        row = addLabeled(panel, row, "File suffix", suffix);
        row = addFullWidth(panel, row, outputToInput);
        row = addFullWidth(panel, row, transformTo8Bit);
        row = addFullWidth(panel, row, addScaleBar);
        row = addLabeled(panel, row, "Scale bar channel", scaleChannel);
        row = addLabeled(panel, row, "Scale bar length", scaleLength);
        row = addFullWidth(panel, row, autoWhiteBalance);
        row = addFullWidth(panel, row, grayscaleOutput);
        row = addButtonRow(panel, row, runButton);

        outputToInput.addActionListener(event -> outputDir.component.setEnabled(!outputToInput.isSelected()));
        outputDir.component.setEnabled(false);

        runButton.addActionListener(event -> runTask("Running RGB export", () -> {
            SuiteSettings.RgbBatch settings = new SuiteSettings.RgbBatch();
            settings.inputDirectory = inputDir.getPath();
            settings.outputDirectory = outputDir.getPath();
            settings.fileSuffix = suffix.getText().trim();
            settings.outputToInput = outputToInput.isSelected();
            settings.transformTo8Bit = transformTo8Bit.isSelected();
            settings.addScaleBar = addScaleBar.isSelected();
            settings.scaleChannel = (int) scaleChannel.getValue();
            settings.scaleLength = ((Number) scaleLength.getValue()).doubleValue();
            settings.autoWhiteBalance = autoWhiteBalance.isSelected();
            settings.exportGrayscale = grayscaleOutput.isSelected();
            exportService.runRgbBatch(settings);
        }));

        return wrapScrollable(panel);
    }

    private JPanel buildMultiChannelPanel() {
        JPanel panel = basePanel();
        PathField inputDir = new PathField(this, true);
        PathField outputDir = new PathField(this, true);
        JTextField suffix = new JTextField(".lif", 12);
        JCheckBox outputToInput = new JCheckBox("Output to the same folder as input", true);
        JCheckBox transformTo8Bit = new JCheckBox("Convert images to 8-bit", true);
        JCheckBox addScaleBar = new JCheckBox("Add scale bar");
        JSpinner scaleChannel = integerSpinner(1, 1, 32, 1);
        JSpinner scaleLength = decimalSpinner(50.0, 1.0, 10000.0, 1.0);
        JSpinner grayChannel = integerSpinner(1, 0, 32, 1);
        JSpinner cyanChannel = integerSpinner(2, 0, 32, 1);
        JSpinner magentaChannel = integerSpinner(3, 0, 32, 1);
        JSpinner yellowChannel = integerSpinner(4, 0, 32, 1);
        JCheckBox splitChannels = new JCheckBox("Split channels");
        JCheckBox previewBeforeRun = new JCheckBox("Preview color mapping before processing");
        JButton previewButton = new JButton("Open Preview");
        JButton runButton = new JButton("Run Multichannel Export");

        int row = 0;
        row = addLabeled(panel, row, "Input directory", inputDir.component);
        row = addLabeled(panel, row, "Output directory", outputDir.component);
        row = addLabeled(panel, row, "File suffix", suffix);
        row = addFullWidth(panel, row, outputToInput);
        row = addFullWidth(panel, row, transformTo8Bit);
        row = addFullWidth(panel, row, addScaleBar);
        row = addLabeled(panel, row, "Scale bar channel", scaleChannel);
        row = addLabeled(panel, row, "Scale bar length", scaleLength);
        row = addLabeled(panel, row, "Gray channel", grayChannel);
        row = addLabeled(panel, row, "Cyan channel", cyanChannel);
        row = addLabeled(panel, row, "Magenta channel", magentaChannel);
        row = addLabeled(panel, row, "Yellow channel", yellowChannel);
        row = addFullWidth(panel, row, splitChannels);
        row = addFullWidth(panel, row, previewBeforeRun);
        row = addButtonRow(panel, row, previewButton, runButton);

        outputToInput.addActionListener(event -> outputDir.component.setEnabled(!outputToInput.isSelected()));
        outputDir.component.setEnabled(false);

        previewButton.addActionListener(event -> runTask("Opening multichannel preview", () -> exportService.previewMultiChannel(buildMultiSettings(
            inputDir, outputDir, suffix, outputToInput, transformTo8Bit, addScaleBar, scaleChannel, scaleLength,
            grayChannel, cyanChannel, magentaChannel, yellowChannel, splitChannels, previewBeforeRun
        ))));

        runButton.addActionListener(event -> runTask("Running multichannel export", () -> exportService.runMultiChannelBatch(buildMultiSettings(
            inputDir, outputDir, suffix, outputToInput, transformTo8Bit, addScaleBar, scaleChannel, scaleLength,
            grayChannel, cyanChannel, magentaChannel, yellowChannel, splitChannels, previewBeforeRun
        ))));

        return wrapScrollable(panel);
    }

    private JPanel buildZExportPanel() {
        JPanel panel = basePanel();
        PathField reviewDir = new PathField(this, true);
        PathField currentOutputDir = new PathField(this, true);
        JSpinner previewChannel = integerSpinner(1, 1, 32, 1);
        JSpinner previewSlice = integerSpinner(1, 1, 9999, 1);
        JSpinner grayChannel = integerSpinner(1, 0, 32, 1);
        JSpinner cyanChannel = integerSpinner(2, 0, 32, 1);
        JSpinner magentaChannel = integerSpinner(3, 0, 32, 1);
        JSpinner yellowChannel = integerSpinner(4, 0, 32, 1);
        JCheckBox addScaleBar = new JCheckBox("Add scale bar");
        JSpinner scaleChannel = integerSpinner(1, 1, 32, 1);
        JSpinner scaleLength = decimalSpinner(50.0, 1.0, 10000.0, 1.0);
        JCheckBox splitChannels = new JCheckBox("Split channels");
        JCheckBox convertSplitTo8Bit = new JCheckBox("Convert split channels to 8-bit");
        JButton exportCurrent = new JButton("Export Current Image Slice");
        JButton startReview = new JButton("Start Folder Review");
        JButton applyPreview = new JButton("Update Preview");
        JButton previewExport = new JButton("Preview Export Appearance");
        JButton extractAndNext = new JButton("Extract Current Z And Next");
        JButton nextFile = new JButton("Next File");
        JButton stopReview = new JButton("Stop Review");

        int row = 0;
        row = addLabeled(panel, row, "Folder review directory", reviewDir.component);
        row = addLabeled(panel, row, "Current image output directory", currentOutputDir.component);
        row = addLabeled(panel, row, "Preview channel", previewChannel);
        row = addLabeled(panel, row, "Preview z", previewSlice);
        row = addLabeled(panel, row, "Gray channel", grayChannel);
        row = addLabeled(panel, row, "Cyan channel", cyanChannel);
        row = addLabeled(panel, row, "Magenta channel", magentaChannel);
        row = addLabeled(panel, row, "Yellow channel", yellowChannel);
        row = addFullWidth(panel, row, addScaleBar);
        row = addLabeled(panel, row, "Scale bar channel", scaleChannel);
        row = addLabeled(panel, row, "Scale bar length", scaleLength);
        row = addFullWidth(panel, row, splitChannels);
        row = addFullWidth(panel, row, convertSplitTo8Bit);
        row = addButtonRow(panel, row, exportCurrent, startReview);
        row = addButtonRow(panel, row, applyPreview, previewExport);
        row = addButtonRow(panel, row, extractAndNext, nextFile, stopReview);

        exportCurrent.addActionListener(event -> runTask("Exporting current z slice", () -> {
            SuiteSettings.ZExport settings = buildZSettings(reviewDir, currentOutputDir, previewChannel, previewSlice, grayChannel, cyanChannel, magentaChannel, yellowChannel, addScaleBar, scaleChannel, scaleLength, splitChannels, convertSplitTo8Bit);
            exportService.exportCurrentSlice(settings.outputDirectory, settings);
        }));
        startReview.addActionListener(event -> runTask("Starting folder review", () -> {
            SuiteSettings.ZExport settings = buildZSettings(reviewDir, currentOutputDir, previewChannel, previewSlice, grayChannel, cyanChannel, magentaChannel, yellowChannel, addScaleBar, scaleChannel, scaleLength, splitChannels, convertSplitTo8Bit);
            exportService.startFolderReview(settings.inputDirectory, settings);
        }));
        applyPreview.addActionListener(event -> runTask("Updating review preview", () -> exportService.applyReviewPreview(
            buildZSettings(reviewDir, currentOutputDir, previewChannel, previewSlice, grayChannel, cyanChannel, magentaChannel, yellowChannel, addScaleBar, scaleChannel, scaleLength, splitChannels, convertSplitTo8Bit)
        )));
        previewExport.addActionListener(event -> runTask("Opening export preview", () -> exportService.previewCurrentReviewExport(
            buildZSettings(reviewDir, currentOutputDir, previewChannel, previewSlice, grayChannel, cyanChannel, magentaChannel, yellowChannel, addScaleBar, scaleChannel, scaleLength, splitChannels, convertSplitTo8Bit)
        )));
        extractAndNext.addActionListener(event -> runTask("Exporting current review slice", () -> exportService.exportCurrentReviewAndNext(
            buildZSettings(reviewDir, currentOutputDir, previewChannel, previewSlice, grayChannel, cyanChannel, magentaChannel, yellowChannel, addScaleBar, scaleChannel, scaleLength, splitChannels, convertSplitTo8Bit)
        )));
        nextFile.addActionListener(event -> runTask("Moving to next review file", () -> exportService.nextReviewImage(
            buildZSettings(reviewDir, currentOutputDir, previewChannel, previewSlice, grayChannel, cyanChannel, magentaChannel, yellowChannel, addScaleBar, scaleChannel, scaleLength, splitChannels, convertSplitTo8Bit)
        )));
        stopReview.addActionListener(event -> exportService.stopReview());

        return wrapScrollable(panel);
    }

    private SuiteSettings.MultiBatch buildMultiSettings(PathField inputDir, PathField outputDir, JTextField suffix, JCheckBox outputToInput,
                                                        JCheckBox transformTo8Bit, JCheckBox addScaleBar, JSpinner scaleChannel, JSpinner scaleLength,
                                                        JSpinner grayChannel, JSpinner cyanChannel, JSpinner magentaChannel, JSpinner yellowChannel,
                                                        JCheckBox splitChannels, JCheckBox previewBeforeRun) {
        SuiteSettings.MultiBatch settings = new SuiteSettings.MultiBatch();
        settings.inputDirectory = inputDir.getPath();
        settings.outputDirectory = outputDir.getPath();
        settings.fileSuffix = suffix.getText().trim();
        settings.outputToInput = outputToInput.isSelected();
        settings.transformTo8Bit = transformTo8Bit.isSelected();
        settings.addScaleBar = addScaleBar.isSelected();
        settings.scaleChannel = (int) scaleChannel.getValue();
        settings.scaleLength = ((Number) scaleLength.getValue()).doubleValue();
        settings.grayChannel = (int) grayChannel.getValue();
        settings.cyanChannel = (int) cyanChannel.getValue();
        settings.magentaChannel = (int) magentaChannel.getValue();
        settings.yellowChannel = (int) yellowChannel.getValue();
        settings.splitChannels = splitChannels.isSelected();
        settings.previewBeforeRun = previewBeforeRun.isSelected();
        return settings;
    }

    private SuiteSettings.ZExport buildZSettings(PathField reviewDir, PathField currentOutputDir, JSpinner previewChannel, JSpinner previewSlice,
                                                 JSpinner grayChannel, JSpinner cyanChannel, JSpinner magentaChannel, JSpinner yellowChannel,
                                                 JCheckBox addScaleBar, JSpinner scaleChannel, JSpinner scaleLength,
                                                 JCheckBox splitChannels, JCheckBox convertSplitTo8Bit) {
        SuiteSettings.ZExport settings = new SuiteSettings.ZExport();
        settings.inputDirectory = reviewDir.getPath();
        settings.outputDirectory = currentOutputDir.getPath();
        settings.previewChannel = (int) previewChannel.getValue();
        settings.previewSlice = (int) previewSlice.getValue();
        settings.grayChannel = (int) grayChannel.getValue();
        settings.cyanChannel = (int) cyanChannel.getValue();
        settings.magentaChannel = (int) magentaChannel.getValue();
        settings.yellowChannel = (int) yellowChannel.getValue();
        settings.addScaleBar = addScaleBar.isSelected();
        settings.scaleChannel = (int) scaleChannel.getValue();
        settings.scaleLength = ((Number) scaleLength.getValue()).doubleValue();
        settings.splitChannels = splitChannels.isSelected();
        settings.convertSplitTo8Bit = convertSplitTo8Bit.isSelected();
        return settings;
    }

    private void runTask(String label, ThrowingRunnable task) {
        setStatus(label + "...");
        new Thread(() -> {
            try {
                task.run();
            } catch (Exception exception) {
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(this, exception.getMessage(), "Easy Imaging Exporting Suite", JOptionPane.ERROR_MESSAGE));
                setStatus("Failed: " + exception.getMessage());
            }
        }, "easy-imaging-suite-worker").start();
    }

    private JPanel basePanel() {
        JPanel panel = new JPanel(new GridBagLayout());
        panel.setBorder(BorderFactory.createEmptyBorder(16, 16, 16, 16));
        return panel;
    }

    private JPanel wrapScrollable(JPanel content) {
        JPanel wrapper = new JPanel(new BorderLayout());
        wrapper.add(content, BorderLayout.NORTH);
        return wrapper;
    }

    private int addLabeled(JPanel panel, int row, String label, JComponent component) {
        GridBagConstraints left = constraints(0, row);
        left.anchor = GridBagConstraints.WEST;
        panel.add(new JLabel(label), left);
        GridBagConstraints right = constraints(1, row);
        right.weightx = 1.0;
        right.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, right);
        return row + 1;
    }

    private int addFullWidth(JPanel panel, int row, JComponent component) {
        GridBagConstraints constraints = constraints(0, row);
        constraints.gridwidth = 2;
        constraints.weightx = 1.0;
        constraints.fill = GridBagConstraints.HORIZONTAL;
        panel.add(component, constraints);
        return row + 1;
    }

    private int addButtonRow(JPanel panel, int row, JComponent... components) {
        JPanel rowPanel = new JPanel();
        rowPanel.setLayout(new BoxLayout(rowPanel, BoxLayout.X_AXIS));
        for (int index = 0; index < components.length; index++) {
            if (index > 0) {
                rowPanel.add(Box.createHorizontalStrut(8));
            }
            rowPanel.add(components[index]);
        }
        return addFullWidth(panel, row, rowPanel);
    }

    private GridBagConstraints constraints(int gridx, int gridy) {
        GridBagConstraints constraints = new GridBagConstraints();
        constraints.gridx = gridx;
        constraints.gridy = gridy;
        constraints.insets = new Insets(4, 4, 4, 4);
        return constraints;
    }

    private JSpinner integerSpinner(int value, int min, int max, int step) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, step));
    }

    private JSpinner decimalSpinner(double value, double min, double max, double step) {
        return new JSpinner(new SpinnerNumberModel(value, min, max, step));
    }

    private void setStatus(String status) {
        SwingUtilities.invokeLater(() -> statusLabel.setText(status));
    }

    @FunctionalInterface
    private interface ThrowingRunnable {
        void run() throws Exception;
    }

    private static final class PathField {
        private final JPanel component = new JPanel(new BorderLayout(6, 0));
        private final JTextField textField = new JTextField();

        PathField(JFrame owner, boolean directoriesOnly) {
            JButton browseButton = new JButton("Browse");
            browseButton.addActionListener(event -> choosePath(owner, directoriesOnly));
            component.add(textField, BorderLayout.CENTER);
            component.add(browseButton, BorderLayout.EAST);
        }

        Path getPath() {
            String text = textField.getText().trim();
            return text.isEmpty() ? null : Path.of(text);
        }

        private void choosePath(JFrame owner, boolean directoriesOnly) {
            JFileChooser chooser = new JFileChooser();
            chooser.setFileSelectionMode(directoriesOnly ? JFileChooser.DIRECTORIES_ONLY : JFileChooser.FILES_ONLY);
            int result = chooser.showOpenDialog(owner);
            if (result == JFileChooser.APPROVE_OPTION) {
                textField.setText(chooser.getSelectedFile().getAbsolutePath());
            }
        }
    }
}
