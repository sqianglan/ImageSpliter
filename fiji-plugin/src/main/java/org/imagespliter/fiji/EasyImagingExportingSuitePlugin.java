package org.imagespliter.fiji;

import ij.plugin.PlugIn;

import javax.swing.SwingUtilities;

public class EasyImagingExportingSuitePlugin implements PlugIn {
    private static EasyImagingExportingSuiteFrame frame;

    @Override
    public void run(String arg) {
        SwingUtilities.invokeLater(() -> {
            if (frame == null) {
                frame = new EasyImagingExportingSuiteFrame();
            }
            frame.setVisible(true);
            frame.toFront();
            frame.requestFocus();
        });
    }
}
