package org.imagespliter.fiji;

import java.nio.file.Path;

final class SuiteSettings {
    private SuiteSettings() {
    }

    static class BatchBase {
        Path inputDirectory;
        Path outputDirectory;
        boolean outputToInput = true;
        String fileSuffix = ".lif";
        boolean transformTo8Bit = true;
        boolean addScaleBar;
        int scaleChannel = 1;
        double scaleLength = 50.0;
    }

    static final class RgbBatch extends BatchBase {
        boolean autoWhiteBalance;
        boolean exportGrayscale;
    }

    static final class MultiBatch extends BatchBase {
        int grayChannel = 1;
        int cyanChannel = 2;
        int magentaChannel = 3;
        int yellowChannel = 4;
        boolean splitChannels;
        boolean previewBeforeRun;
    }

    static final class ZExport {
        Path inputDirectory;
        Path outputDirectory;
        int previewChannel = 1;
        int previewSlice = 1;
        int grayChannel = 1;
        int cyanChannel = 2;
        int magentaChannel = 3;
        int yellowChannel = 4;
        boolean addScaleBar;
        int scaleChannel = 1;
        double scaleLength = 50.0;
        boolean splitChannels;
        boolean convertSplitTo8Bit;

        ZExport copy() {
            ZExport copy = new ZExport();
            copy.inputDirectory = inputDirectory;
            copy.outputDirectory = outputDirectory;
            copy.previewChannel = previewChannel;
            copy.previewSlice = previewSlice;
            copy.grayChannel = grayChannel;
            copy.cyanChannel = cyanChannel;
            copy.magentaChannel = magentaChannel;
            copy.yellowChannel = yellowChannel;
            copy.addScaleBar = addScaleBar;
            copy.scaleChannel = scaleChannel;
            copy.scaleLength = scaleLength;
            copy.splitChannels = splitChannels;
            copy.convertSplitTo8Bit = convertSplitTo8Bit;
            return copy;
        }
    }

    static final class ReviewEntry {
        final Path path;
        final int seriesIndex;

        ReviewEntry(Path path, int seriesIndex) {
            this.path = path;
            this.seriesIndex = seriesIndex;
        }
    }
}
