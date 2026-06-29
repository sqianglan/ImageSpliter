// Unified Fiji/ImageJ macro package for RGB images, multichannel images, and Z-slice export.
// UI design based on Image Processing Suite; file processing based on Image Exporting Suite.
// Contact: qiang.lan@bristol.ac.uk (CMM, University of Bristol)
// License: Creative Commons CC-BY-SA

workflow = "Multichannel Image";

doBatch = true;
addScalebar = false;
transform_to_8bit = true;
autoWhiteBalanceSingle = false;
singleChannelExportGray = false;
outAsIn = true;
grayCh = 1;
cyanCh = 2;
megaCh = 3;
yellowCh = 4;
mergeIncludeGray = true;
mergeIncludeCyan = true;
mergeIncludeMega = true;
mergeIncludeYellow = true;
scaleCh = 1;
scaleLength = 50;
channelSplit = false;
defaultZ = 1;
zSplitChannels = false;
zConvertSplitTo8Bit = false;
zReviewFileExtension = ".lif";
defaultPreviewChannel = 1;
currentImageName = "";
outputFolder = "";
imageDir = "";

launch_suite();
exit("Done");

function launch_suite() {
    Dialog.create("Image Exporting Suite");
    Dialog.addMessage("Choose a workflow.");
    Dialog.addChoice("Workflow", newArray("RGB Image", "Multichannel Image", "Z-Slice Export"), workflow);
    Dialog.addMessage("RGB Image: export RGB/composite images with optional grayscale conversion.");
    Dialog.addMessage("Multichannel Image: recolor, split, merge, and export multichannel files.");
    Dialog.addMessage("Z-Slice Export: extract one z slice from the current image or review a folder one file at a time.");
    Dialog.show();
    workflow = Dialog.getChoice();

    if (workflow == "Z-Slice Export") {
        run_z_slice_export();
    } else {
        run_batch_export(workflow);
    }
}

function run_batch_export(workflow) {
    valid = false;
    defaultDir = getDirectory("default");

    while (!valid) {
        Dialog.create("Batch Export: " + workflow);
        Dialog.addDirectory("Input Directory:", defaultDir);
        Dialog.addCheckbox("Output to the same folder as input?", outAsIn);
        Dialog.addToSameRow();
        Dialog.addMessage("(Otherwise choose output in next dialog)");
        Dialog.addMessage("");
        Dialog.addString("File Suffix:", ".lif");
        Dialog.addMessage("");
        Dialog.addCheckbox("Change to 8 bit image", transform_to_8bit);
        Dialog.addToSameRow();
        Dialog.addCheckbox("Add Scale bar?", addScalebar);
        Dialog.addToSameRow();
        Dialog.addCheckbox("Batch Silent Mode?", doBatch);
        Dialog.show();

        inputDir = Dialog.getString() + File.separator;
        outAsIn = Dialog.getCheckbox();
        fileExtension = Dialog.getString();
        transform_to_8bit = Dialog.getCheckbox();
        addScalebar = Dialog.getCheckbox();
        doBatch = Dialog.getCheckbox();

        if (workflow == "RGB Image") {
            Dialog.create("RGB Image Settings");
            Dialog.addCheckbox("Auto white balance", autoWhiteBalanceSingle);
            Dialog.addMessage("");
            Dialog.addCheckbox("Export grayscale final image", singleChannelExportGray);
            Dialog.show();
            autoWhiteBalanceSingle = Dialog.getCheckbox();
            singleChannelExportGray = Dialog.getCheckbox();
            channelSplit = false;
        } else {
            Dialog.create("Multichannel Image Settings");
            Dialog.addNumber("Gray channel", grayCh);
            Dialog.addToSameRow();
            Dialog.addNumber("Cyan channel", cyanCh);
            Dialog.addNumber("Magenta channel", megaCh);
            Dialog.addToSameRow();
            Dialog.addNumber("Yellow channel", yellowCh);
            Dialog.addMessage("");
            Dialog.addCheckbox("Split Channels?", channelSplit);
            Dialog.show();
            grayCh = Dialog.getNumber();
            cyanCh = Dialog.getNumber();
            megaCh = Dialog.getNumber();
            yellowCh = Dialog.getNumber();
            channelSplit = Dialog.getCheckbox();
        }

        if (hasfiles(inputDir, fileExtension)) {
            valid = true;
        } else {
            showMessage("Invalid or empty directory. Please reselect.");
            defaultDir = inputDir;
        }
    }

    if (!outAsIn) {
        Dialog.create("Choose the output Directory");
        Dialog.addDirectory("Output Directory:", inputDir);
        Dialog.show();
        outputDir = Dialog.getString() + File.separator;
    }

    if (addScalebar) {
        Dialog.create("Scale bar Settings");
        Dialog.addNumber("Channel for adding scalebar", scaleCh);
        Dialog.addNumber("Scale bar length", scaleLength);
        Dialog.addToSameRow();
        Dialog.addMessage("(with default unit in image properties)");
        Dialog.show();
        scaleCh = Dialog.getNumber();
        scaleLength = Dialog.getNumber();
    }

    if (doBatch) setBatchMode(true);
    run("Bio-Formats Macro Extensions");
    showProgress(0);
    process_bioformat_files(inputDir, workflow);
}

function run_z_slice_export() {
    Dialog.create("Z-Slice Export");
    Dialog.addChoice("Mode", newArray("Current Image", "Folder Review"), "Current Image");
    Dialog.addMessage("Current Image: extract one z slice from the active image.");
    Dialog.addMessage("Folder Review: open files one by one, preview, extract current z, then load next.");
    Dialog.addMessage("");
    Dialog.addChoice("File type filter (Folder Review)", newArray("All supported", ".lif", ".zvi", ".vsi", ".czi", ".nd2", ".lsm", ".ome.tif", ".ome.tiff", ".tif", ".tiff"), zReviewFileExtension);
    Dialog.addMessage("");
    Dialog.addNumber("Gray channel", grayCh);
    Dialog.addToSameRow();
    Dialog.addCheckbox("Include gray in merge", mergeIncludeGray);
    Dialog.addNumber("Cyan channel", cyanCh);
    Dialog.addToSameRow();
    Dialog.addCheckbox("Include cyan in merge", mergeIncludeCyan);
    Dialog.addNumber("Magenta channel", megaCh);
    Dialog.addToSameRow();
    Dialog.addCheckbox("Include magenta in merge", mergeIncludeMega);
    Dialog.addNumber("Yellow channel", yellowCh);
    Dialog.addToSameRow();
    Dialog.addCheckbox("Include yellow in merge", mergeIncludeYellow);
    Dialog.addMessage("");
    Dialog.addCheckbox("Add scale bar?", addScalebar);
    Dialog.addToSameRow();
    Dialog.addNumber("Scale bar channel", scaleCh);
    Dialog.addToSameRow();
    Dialog.addNumber("Scale length", scaleLength);
    Dialog.addCheckbox("Split channels", zSplitChannels);
    Dialog.addToSameRow();
    Dialog.addCheckbox("Convert split channels to 8-bit", zConvertSplitTo8Bit);
    Dialog.show();
    zMode = Dialog.getChoice();
    zReviewFileExtension = Dialog.getChoice();
    grayCh = Dialog.getNumber();
    mergeIncludeGray = Dialog.getCheckbox();
    cyanCh = Dialog.getNumber();
    mergeIncludeCyan = Dialog.getCheckbox();
    megaCh = Dialog.getNumber();
    mergeIncludeMega = Dialog.getCheckbox();
    yellowCh = Dialog.getNumber();
    mergeIncludeYellow = Dialog.getCheckbox();
    addScalebar = Dialog.getCheckbox();
    scaleCh = Dialog.getNumber();
    scaleLength = Dialog.getNumber();
    zSplitChannels = Dialog.getCheckbox();
    zConvertSplitTo8Bit = Dialog.getCheckbox();

    if (zMode == "Folder Review") {
        run_folder_review();
        return;
    }

    if (nImages == 0) {
        exit("Current Image mode requires an open image.");
    }

    Dialog.create("Current Image Z-Slice Export");
    Dialog.addNumber("Selected Z position", defaultZ);
    Dialog.show();
    defaultZ = Dialog.getNumber();

    imageDir = getInfo("image.directory");
    if (!endsWith(imageDir, File.separator)) imageDir = imageDir + File.separator;
    name = getTitle();
    currentImageName = sanitize_output_name(name, "");
    export_current_slice_outputs(imageDir + currentImageName, z);
}

function run_folder_review() {
    inputDir = getDirectory("Choose folder for Z-slice review");
    if (inputDir == "") exit("Folder review cancelled.");
    run("Bio-Formats Macro Extensions");
    review_folder_files(inputDir);
    showMessage("Folder Review Z-Slice Export", "Finished reviewing all matching files.");
}

function review_folder_files(currentDirectory) {
    localList = getFileList(currentDirectory);
    for (j = 0; j < localList.length; j++) {
        localPath = currentDirectory + localList[j];
        if (is_supported_review_file(localList[j])) {
            Ext.setId(localPath);
            Ext.getSeriesCount(seriesCount);
            for (series = 1; series <= seriesCount; series++) {
                run("Bio-Formats Importer", "open=[" + localPath + "] color_mode=Default rois_import=[ROI manager] view=Hyperstack stack_order=XYCZT series_" + series);
                getPixelSize(U, px, py);
                run("Set Scale...", "distance=" + 1/px + " known=1 unit=" + U);
                apply_review_preview_colors();
                imageDir = getInfo("image.directory");
                if (!endsWith(imageDir, File.separator)) imageDir = imageDir + File.separator;
                reviewResult = review_current_image(localPath, series);
                close("*");
                if (reviewResult == "exit") return;
            }
        } else if (endsWith(localList[j], "/")) {
            review_folder_files(localPath);
        }
    }
}

function review_current_image(reviewPath, reviewSeries) {
    getDimensions(width, height, channels, slices, frames);
    apply_review_preview_colors();

    setTool("hand");
    Stack.getPosition(currentChannel, currentSlice, currentFrame);
    if (currentSlice < 1) currentSlice = 1;

    waitForUser("Review: " + File.getName(reviewPath),
        "Series: " + reviewSeries + "   Channel: " + currentChannel + "   Z: " + currentSlice +
        "\nSettings:  Gray=" + grayCh + "  Cyan=" + cyanCh + "  Magenta=" + megaCh + "  Yellow=" + yellowCh +
        "\n\nNavigate to the desired Z slice, then press OK to export.");

    Stack.getPosition(currentChannel, currentSlice, currentFrame);
    if (currentSlice < 1) currentSlice = 1;
    extract_current_review_slice(reviewPath, reviewSeries);
    return "next";
}

        if (reviewAction == "Export current Z") {
            extract_current_review_slice(reviewPath, reviewSeries);
            return "next";
        }

        if (reviewAction == "Change settings") {
            Dialog.create("Channel Settings");
            Dialog.addNumber("Gray channel", grayCh);
            Dialog.addToSameRow();
            Dialog.addCheckbox("Include gray in merge", mergeIncludeGray);
            Dialog.addNumber("Cyan channel", cyanCh);
            Dialog.addToSameRow();
            Dialog.addCheckbox("Include cyan in merge", mergeIncludeCyan);
            Dialog.addNumber("Magenta channel", megaCh);
            Dialog.addToSameRow();
            Dialog.addCheckbox("Include magenta in merge", mergeIncludeMega);
            Dialog.addNumber("Yellow channel", yellowCh);
            Dialog.addToSameRow();
            Dialog.addCheckbox("Include yellow in merge", mergeIncludeYellow);
            Dialog.show();
            grayCh = Dialog.getNumber();
            mergeIncludeGray = Dialog.getCheckbox();
            cyanCh = Dialog.getNumber();
            mergeIncludeCyan = Dialog.getCheckbox();
            megaCh = Dialog.getNumber();
            mergeIncludeMega = Dialog.getCheckbox();
            yellowCh = Dialog.getNumber();
            mergeIncludeYellow = Dialog.getCheckbox();
            apply_review_preview_colors();
            continue;
        }

        if (reviewAction == "Skip to next file") {
            return "next";
        }

        return "exit";
    }
}

        if (reviewAction == "Export current Z") {
            extract_current_review_slice(reviewPath, reviewSeries);
            return "next";
        }

        if (reviewAction == "Change settings") {
            Dialog.create("Channel Settings");
            Dialog.addNumber("Gray channel", grayCh);
            Dialog.addToSameRow();
            Dialog.addCheckbox("Include gray in merge", mergeIncludeGray);
            Dialog.addNumber("Cyan channel", cyanCh);
            Dialog.addToSameRow();
            Dialog.addCheckbox("Include cyan in merge", mergeIncludeCyan);
            Dialog.addNumber("Magenta channel", megaCh);
            Dialog.addToSameRow();
            Dialog.addCheckbox("Include magenta in merge", mergeIncludeMega);
            Dialog.addNumber("Yellow channel", yellowCh);
            Dialog.addToSameRow();
            Dialog.addCheckbox("Include yellow in merge", mergeIncludeYellow);
            Dialog.show();
            grayCh = Dialog.getNumber();
            mergeIncludeGray = Dialog.getCheckbox();
            cyanCh = Dialog.getNumber();
            mergeIncludeCyan = Dialog.getCheckbox();
            megaCh = Dialog.getNumber();
            mergeIncludeMega = Dialog.getCheckbox();
            yellowCh = Dialog.getNumber();
            mergeIncludeYellow = Dialog.getCheckbox();
            apply_review_preview_colors();
            continue;
        }

        if (reviewAction == "Skip to next file") {
            return "next";
        }

        return "exit";
    }
}

function apply_review_preview_colors() {
    getDimensions(width, height, channels, slices, frames);
    if (channels > 1) {
        color_channels(grayCh, yellowCh, cyanCh, megaCh);
        Stack.setDisplayMode("color");
    }
}

function apply_review_position_and_colors(pChannel, pSlice) {
    getDimensions(width, height, channels, slices, frames);
    if (pChannel < 1) pChannel = 1;
    if (pChannel > channels) pChannel = channels;
    if (pSlice < 1) pSlice = 1;
    if (pSlice > slices) pSlice = slices;
    if (channels >= pChannel) Stack.setChannel(pChannel);
    if (slices >= pSlice) Stack.setSlice(pSlice);
    apply_review_preview_colors();
}

function extract_current_review_slice(reviewPath, reviewSeries) {
    Stack.getPosition(currentChannel, currentSlice, currentFrame);
    if (currentSlice < 1) currentSlice = 1;
    reviewOutputBase = build_review_output_base(reviewPath, reviewSeries);
    export_current_slice_outputs(reviewOutputBase, currentSlice);
}

function preview_current_review_export() {
    sourceImageID = getImageID();
    Stack.getPosition(currentChannel, currentSlice, currentFrame);
    getDimensions(width, height, channels, slices, frames);
    if (currentSlice < 1) currentSlice = 1;
    if (currentSlice > slices) currentSlice = slices;
    if (currentFrame < 1) currentFrame = 1;
    if (currentFrame > frames) currentFrame = frames;
    previewTitle = "Z_Export_Preview";
    if (isOpen(previewTitle)) { selectWindow(previewTitle); close(); }
    run("Duplicate...", "title=" + previewTitle + " duplicate channels=1-" + channels + " slices=" + currentSlice + " frames=" + currentFrame);
    selectWindow(previewTitle);
    getDimensions(width, height, channels, slices, frames);
    if (channels > 1) {
        run("Make Composite", "display=Composite");
        color_channels(grayCh, yellowCh, cyanCh, megaCh);
    }
    waitForUser("Preview export", "Check 'Z_Export_Preview', then click OK to return.");
    if (isOpen(previewTitle)) { selectWindow(previewTitle); close(); }
    selectImage(sourceImageID);
}

function export_current_slice_outputs(outputBase, sliceNumber) {
    getDimensions(width, height, channels, slices, frames);
    Stack.getPosition(currentChannel, currentSlice, currentFrame);
    if (sliceNumber < 1) sliceNumber = 1;
    if (sliceNumber > slices) sliceNumber = slices;
    if (currentFrame < 1) currentFrame = 1;
    if (currentFrame > frames) currentFrame = frames;

    rawTitle = "Z_Raw_Export";
    if (isOpen(rawTitle)) { selectWindow(rawTitle); close(); }
    run("Duplicate...", "title=" + rawTitle + " duplicate channels=1-" + channels + " slices=" + sliceNumber + " frames=" + currentFrame);
    selectWindow(rawTitle);
    rawID = getImageID();
    rawPath = outputBase + "_z" + sliceNumber + "_raw.tif";
    saveAs("Tiff", rawPath);
    currentImageName = sanitize_output_name(File.getName(rawPath), "");

    if (zSplitChannels) {
        splitProcessingTitle = "Z_Split_Processing";
        if (isOpen(splitProcessingTitle)) { selectWindow(splitProcessingTitle); close(); }
        selectImage(rawID);
        getDimensions(dw, dh, dch, dsl, dfr);
        run("Duplicate...", "title=" + splitProcessingTitle + " duplicate channels=1-" + dch + " slices=1-" + dsl + " frames=1-" + dfr);
        selectWindow(splitProcessingTitle);
        if (zConvertSplitTo8Bit) {
            depth_to_8bit();
        }
        apply_current_color_mapping();
        split_channel_save(outputBase + "_z" + sliceNumber);
    }

    selectImage(rawID);
    getDimensions(width, height, channels, slices, frames);
    if (channels > 1) {
        run("Make Composite", "display=Composite");
        color_channels(grayCh, yellowCh, cyanCh, megaCh);
    }
    run("RGB Color");
    mergeLabel = build_merge_channel_label();
    saveAs("Tiff", outputBase + "_z" + sliceNumber + mergeLabel + "_merged_rgb.tif");
    close();

    if (isOpen(rawTitle)) { selectWindow(rawTitle); close(); }
}

function build_review_output_base(reviewPath, reviewSeries) {
    reviewOutputFolder = File.getParent(reviewPath);
    reviewFileName = sanitize_output_name(File.getName(reviewPath), "");
    return reviewOutputFolder + File.separator + reviewFileName + "_s" + reviewSeries;
}

function process_bioformat_files(currentDirectory, workflow) {
    fileList = getFileList(currentDirectory);
    for (file = 0; file < fileList.length; file++) {
        if (endsWith(fileList[file], fileExtension)) {
            Ext.setId(currentDirectory + fileList[file]);
            Ext.getSeriesCount(seriesCount);
            for (series = 1; series <= seriesCount; series++) {
                run("Bio-Formats Importer", "open=[" + currentDirectory + fileList[file] + "] color_mode=Default rois_import=[ROI manager] view=Hyperstack stack_order=XYCZT series_" + series);
                getPixelSize(U, px, py);
                run("Set Scale...", "distance=" + 1/px + " known=1 unit=" + U);
                imageDir = getInfo("image.directory");
                if (!endsWith(imageDir, File.separator)) imageDir = imageDir + File.separator;
                outputFolder = build_output_folder();
                name = getTitle();
                currentImageName = sanitize_output_name(name, fileExtension);
                outputBaseName = outputFolder + File.separator + currentImageName;

                if (workflow == "RGB Image") {
                    process_rgb_image_output(outputBaseName);
                    close("*");
                    continue;
                }

                if (transform_to_8bit) depth_to_8bit();
                apply_current_color_mapping();
                saveAs("Tiff", outputBaseName + ".tif");
                if (channelSplit) split_channel_save(outputBaseName);
                close("*");
            }
        } else if (endsWith(fileList[file], "/")) {
            process_bioformat_files(currentDirectory + fileList[file], workflow);
        }
    }
}

function build_output_folder() {
    if (outAsIn) {
        dir = inputDir;
    } else {
        dir = outputDir;
    }
    dir = replace(dir, File.separator + File.separator, File.separator);
    dirName = File.getName(imageDir);
    parentDir = File.getParent(imageDir);
    if (imageDir == dir || endsWith(dir, imageDir)) {
        baseDir = parentDir + File.separator;
    } else {
        baseDir = dir;
    }
    if (baseDir.contains(dirName)) {
        outFolder = baseDir + "Image_Splitted_" + timeStamp();
    } else {
        outFolder = baseDir + dirName + "_Image_Splitted_" + timeStamp();
    }
    File.makeDirectory(outFolder);
    return outFolder;
}

function apply_current_color_mapping() {
    getDimensions(width, height, channels, slices, frames);
    if (channels > 1) {
        run("Make Composite", "display=Composite");
    }
    color_channels(grayCh, yellowCh, cyanCh, megaCh);
}

function process_rgb_image_output(outputBaseName) {
    if (singleChannelExportGray) {
        save_rgb_image_as_grayscale(outputBaseName);
        return;
    }
    if (autoWhiteBalanceSingle) {
        auto_white_balance_rgb();
    }
    tempRgbSource = outputBaseName + "__rgb_source__.tif";
    saveAs("Tiff", tempRgbSource);
    save_rgb_image_as_rgb(outputBaseName, tempRgbSource);
    deletedTempRgbSource = File.delete(tempRgbSource);
}

function save_rgb_image_as_grayscale(outputBaseName) {
    run("Duplicate...", "title=RGB_Image_Gray_Output");
    selectWindow("RGB_Image_Gray_Output");
    run("RGB Color");
    if (bitDepth() != 8) run("8-bit");
    run("Grays");
    if (autoWhiteBalanceSingle) auto_white_balance_single();
    saveAs("Tiff", outputBaseName + ".tif");
    close();
}

function save_rgb_image_as_rgb(outputBaseName, sourcePath) {
    open(sourcePath);
    rgbSourceTitle = getTitle();
    selectWindow(rgbSourceTitle);
    run("Stack to RGB");
    saveAs("Tiff", outputBaseName + ".tif");
    close();
}

function color_channels(pGray, pYellow, pCyan, pMega) {
    if (pGray != 0) { Stack.setChannel(pGray); run("Grays"); }
    if (pYellow != 0) { Stack.setChannel(pYellow); run("Yellow"); }
    if (pCyan != 0) { Stack.setChannel(pCyan); run("Cyan"); }
    if (pMega != 0) { Stack.setChannel(pMega); run("Magenta"); }
}

function depth_to_8bit() {
    getDimensions(width, height, channels, slices, frames);
    if (channels > 1) {
        titlesBeforeDepthSplit = getList("image.titles");
        run("Split Channels");
        list = find_new_titles(titlesBeforeDepthSplit);
        cmd = "";
        for (channel = 0; channel < list.length; channel++) {
            selectWindow(list[channel]);
            getStatistics(area, mean, min, max, std, histogram);
            if (bitDepth() == 16 && max <= 4095) {
                setMinAndMax(0, 4095);
                call("ij.ImagePlus.setDefault16bitRange", 12);
                run("8-bit");
            }
            if (bitDepth() != 8) run("8-bit");
            cmd += "c" + (channel+1) + "=[" + list[channel] + "] ";
        }
        run("Merge Channels...", cmd + "create");
    } else {
        getStatistics(area, mean, min, max, std, histogram);
        if (bitDepth() == 16 && max <= 4095) {
            setMinAndMax(0, 4095);
            call("ij.ImagePlus.setDefault16bitRange", 12);
            run("8-bit");
        }
        if (bitDepth() != 8) run("8-bit");
        if (addScalebar == true) {
            run("Duplicate...", " ");
            scalebarID = getImageID();
            run("Scale Bar...", "width=" + scaleLength + " height=12 font=42 color=White background=None location=[Lower Left] bold overlay");
            run("Flatten");
            outFile2 = outputBaseName + "_Ch" + scaleCh + "_scalebar.tif";
            saveAs("Tiff", outFile2);
            close();
            selectImage(scalebarID);
            close();
        }
    }
}

function split_channel_save(outputBaseName) {
    getDimensions(width, height, channels, slices, frames);
    if (channels < 2) return;
    titlesBeforeSplit = getList("image.titles");
    run("Split Channels");
    splitTitles = find_new_titles(titlesBeforeSplit);
    for (channelIndex = 0; channelIndex < splitTitles.length; channelIndex++) {
        if (splitTitles[channelIndex] == "") continue;
        selectWindow(splitTitles[channelIndex]);
        rename("Split_Ch" + (channelIndex + 1));
    }
    for (channelIndex = 1; channelIndex <= lengthOf(splitTitles); channelIndex++) {
        splitTitle = "Split_Ch" + channelIndex;
        if (!isOpen(splitTitle)) continue;
        selectWindow(splitTitle);
        run("Grays");
        if (should_convert_split_outputs_to_8bit() && bitDepth() != 8) run("8-bit");
        fileNameChannel = outputBaseName + "_Ch" + channelIndex + ".tif";
        saveAs("Tiff", fileNameChannel);
        rename(splitTitle);
        if (channelIndex == scaleCh && addScalebar == true) {
            run("Duplicate...", " ");
            scalebarID = getImageID();
            run("Scale Bar...", "width=" + scaleLength + " height=12 font=42 color=White background=None location=[Lower Left] bold overlay");
            run("Flatten");
            outFile2 = outputBaseName + "_Ch" + scaleCh + "_scalebar.tif";
            saveAs("Tiff", outFile2);
            close();
            selectImage(scalebarID);
            close();
        }
    }
    if (workflow != "Z-Slice Export") {
        merge_channel(outputBaseName);
    }
}

function should_convert_split_outputs_to_8bit() {
    if (workflow == "Z-Slice Export") return zConvertSplitTo8Bit;
    return false;
}

function build_merge_channel_label() {
    channelValues = newArray(grayCh, cyanCh, megaCh, yellowCh);
    mergeFlags = newArray(mergeIncludeGray, mergeIncludeCyan, mergeIncludeMega, mergeIncludeYellow);
    label = "_ch";
    for (i = 0; i < channelValues.length; i++) {
        if (channelValues[i] != 0 && mergeFlags[i]) {
            label += "-" + channelValues[i];
        }
    }
    if (label == "_ch") label = "";
    return label;
}

function merge_channel(outputBaseName) {
    channelValues = newArray(grayCh, cyanCh, megaCh, yellowCh);
    mergeFlags = newArray(mergeIncludeGray, mergeIncludeCyan, mergeIncludeMega, mergeIncludeYellow);
    cmd = "";
    validChannel = "ch";
    for (i = 0; i < channelValues.length; i++) {
        if (channelValues[i] != 0 && mergeFlags[i]) {
            splitTitle = "Split_Ch" + channelValues[i];
            if (!isOpen(splitTitle)) continue;
            cmd += "c" + (i+4) + "=[" + splitTitle + "] ";
            validChannel += "-" + channelValues[i];
        }
    }
    if (cmd == "") {
        print("No split-channel windows available for merged export in image " + currentImageName);
        return;
    }
    run("Merge Channels...", cmd + "create");
    saveAs("Tiff", outputBaseName + "_" + validChannel + "_merged.tif");
    close();
}

function find_new_titles(previousTitles) {
    currentTitles = getList("image.titles");
    newTitles = newArray(lengthOf(currentTitles));
    insertIndex = 0;
    for (i = 0; i < currentTitles.length; i++) {
        if (!array_contains(previousTitles, currentTitles[i])) {
            newTitles[insertIndex] = currentTitles[i];
            insertIndex++;
        }
    }
    trimmedTitles = newArray(insertIndex);
    for (i = 0; i < insertIndex; i++) {
        trimmedTitles[i] = newTitles[i];
    }
    return trimmedTitles;
}

function array_contains(values, target) {
    for (i = 0; i < values.length; i++) {
        if (values[i] == target) return true;
    }
    return false;
}

function auto_white_balance_single() {
    run("Enhance Contrast", "saturated=0.35 normalize");
}

function auto_white_balance_rgb() {
    run("Enhance Contrast", "saturated=0.35 normalize");
}

function sanitize_output_name(name, ext) {
    if (ext != "") {
        name = replace(name, ext, "");
    } else {
        t = lastIndexOf(name, ".");
        if (t > 0) name = substring(name, 0, t);
    }
    name = replace(name, "-", "_");
    name = replace(name, "\\", "_");
    name = replace(name, ":", "_");
    name = replace(name, "*", "_");
    name = replace(name, "?", "_");
    name = replace(name, "\"", "_");
    name = replace(name, "<", "_");
    name = replace(name, ">", "_");
    name = replace(name, "|", "_");
    name = replace(name, "/", "_stitching_");
    currentImageName = name;
    return name;
}

function timeStamp() {
    getDateAndTime(year, month, dayOfWeek, dayOfMonth, hour, minute, second, msec);
    monthOneBased = month + 1;
    return toString(year) + "-" + twoDigit(monthOneBased) + "-" + twoDigit(dayOfMonth);
}

function twoDigit(n) {
    return IJ.pad(n, 2);
}

function find_first_matching_file(currentDirectory) {
    localList = getFileList(currentDirectory);
    for (j = 0; j < localList.length; j++) {
        if (endsWith(localList[j], fileExtension)) return currentDirectory + localList[j];
        if (endsWith(localList[j], "/")) {
            nestedPath = find_first_matching_file(currentDirectory + localList[j]);
            if (nestedPath != "") return nestedPath;
        }
    }
    return "";
}

function is_supported_review_file(name) {
    lowerName = toLowerCase(name);
    if (zReviewFileExtension != "All supported") {
        return endsWith(lowerName, toLowerCase(zReviewFileExtension));
    }
    if (endsWith(lowerName, ".lif")) return true;
    if (endsWith(lowerName, ".zvi")) return true;
    if (endsWith(lowerName, ".vsi")) return true;
    if (endsWith(lowerName, ".czi")) return true;
    if (endsWith(lowerName, ".nd2")) return true;
    if (endsWith(lowerName, ".lsm")) return true;
    if (endsWith(lowerName, ".ome.tif")) return true;
    if (endsWith(lowerName, ".ome.tiff")) return true;
    if (endsWith(lowerName, ".tif")) return true;
    if (endsWith(lowerName, ".tiff")) return true;
    return false;
}

function hasfiles(dir, fileExtension) {
    list = getFileList(dir);
    for (i = 0; i < list.length; i++) {
        path = dir + list[i];
        if (endsWith(list[i], fileExtension)) return true;
        if (File.isDirectory(path) && hasfiles(path + File.separator, fileExtension)) return true;
    }
    return false;
}

