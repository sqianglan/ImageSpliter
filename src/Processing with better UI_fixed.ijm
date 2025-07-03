//Process the .lif or .zvi file, and output each imagesets separately for fast and easy imaging analysis
// Produced merged image with colors or separated channels or certain z optical sections 
// Enlightened by the mQC_counter (g.ball@dundee.ac.uk, Dundee Imaging Facility (October 2019))
// and discussions in image.sc forum 
//Contact: qiang.lan@bristol.ac.uk (CMM, University of Bristol)
//license: Creative Commons CC-BY-SA

// Next version, I will add splitting channel function within specified z optical section.
// parameters
doBatch = true;  // true = do batch analysis in the background
addScalebar = false; 
transform_to_8bit = true; 
outAsIn = true;
grayCh = 1;    // channel for Gray color
cyanCh = 2;  // channel for Cyan color
megaCh = 3;  // channel for Magenta color
yellowCh = 4;  // channel for yellow color
scaleCh = 1; // channel for scale bar
scaleLength = 50; // scale bar length
channelSplit = false; // default do not split channel

// --- start Macro ---

// 1. dialog to select options & update parameters

valid = false;
defaultDir = getDirectory("default");
while (!valid){
    Dialog.create("Raw File Splitter");
    Dialog.addMessage("This plugin will split compacted series image files like Leica '.lif' file into separated .tif files, and also colorize as specified.");
    Dialog.addMessage("It will also split multi-channel image into single channel files, and add scale bar in specified channel image and make a composed RGB image within selected channels.");
    Dialog.addMessage("It should work with other file types, e.g. tif, zvi. Please contact qiang.lan@bristol.ac.uk if you encounter any issues.  ");
    Dialog.addMessage("------");
    Dialog.addDirectory("Input Directory:", defaultDir); // Default path (last time custom selected)
    Dialog.addCheckbox("Output to the same folder as input?", outAsIn);
    Dialog.addToSameRow();
    Dialog.addMessage(" (Default output is same as input, otherwise will specify in the next window)");
    Dialog.addMessage("");
    Dialog.addString("File Suffix:", ".lif"); // Default suffix
    Dialog.addMessage("");
    Dialog.addCheckbox("Change to 8 bit image", transform_to_8bit);
    Dialog.addMessage("");
    Dialog.addMessage("Color for each channel (start with 1, and type 0 if the channel does not exist or do not want to change default color) \n sometimes the image will lose original color given during imaging if not re-colored.");
    Dialog.addNumber("Gray channel", grayCh);
    Dialog.addToSameRow();
    Dialog.addNumber("Cyan channel", cyanCh);

    Dialog.addNumber("Magenta channel", megaCh);
    Dialog.addToSameRow();
    Dialog.addNumber("Yellow channel", yellowCh);
    Dialog.addMessage("");
    Dialog.addCheckbox("Split Channels?", channelSplit);
    Dialog.addToSameRow();
    Dialog.addCheckbox("Add Scale bar?", addScalebar);
    Dialog.addToSameRow();
    Dialog.addCheckbox("Batch Silent Mode?", doBatch);
    Dialog.addToSameRow();
    Dialog.addMessage("(With silent mode, no image windows will pop out)");
    Dialog.show();

//get custom input values
    inputDir = Dialog.getString() + File.separator;
    outAsIn = Dialog.getCheckbox();
    fileExtension = Dialog.getString();
    transform_to_8bit = Dialog.getCheckbox();
    grayCh = Dialog.getNumber();
    cyanCh = Dialog.getNumber();
    megaCh = Dialog.getNumber();
    yellowCh = Dialog.getNumber();
    channelSplit = Dialog.getCheckbox();
    addScalebar = Dialog.getCheckbox();
    doBatch = Dialog.getCheckbox();
    
// Check if the inputDir is valid
    // Trim trailing separator (if any) for validation
    // Check existence and non-empty
    if (hasfiles(inputDir, fileExtension)) {
        valid = true;
        //print("Directory is valid and non-empty.");
    } else {
        showMessage("Invalid or empty directory. Please reselect.");
        defaultDir = inputDir; // Remember user's last attempt
    }
}

if (!outAsIn) {
    Dialog.create("Choose the output Directory");
    Dialog.addDirectory("Input Directory:", inputDir);
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

// add second dialog for split channels
if (channelSplit) {
    Dialog.create("Merging file Settings");
    Dialog.addMessage("Specify the channels and colors to be included in the Merged image file (0 if you do not want to include it in merging) ");
    Dialog.addMessage("Each split channel will be gray scale and merged file shows specified colors");
    Dialog.addNumber("Gray channel", grayCh);
    Dialog.addNumber("Cyan channel", cyanCh);
    Dialog.addNumber("Magenta channel", megaCh);
    Dialog.addNumber("Yellow channel", yellowCh);
    Dialog.show();
    mergedGrayCh = Dialog.getNumber();
    mergedCyanCh = Dialog.getNumber();
    mergedMegaCh = Dialog.getNumber();
    mergedYellowCh = Dialog.getNumber();
}

if (doBatch) {setBatchMode(true);}

run("Bio-Formats Macro Extensions");
//Ext.setBatchMode(true); 
showProgress(0);
processBioFormatFiles(inputDir);

exit("Done");

function processBioFormatFiles(currentDirectory) {
    fileList = getFileList(currentDirectory);
    for (file = 0; file < fileList.length; file++) {
        if (endsWith(fileList[file], fileExtension)) {
            Ext.setId(currentDirectory + fileList[file]);
            Ext.getSeriesCount(seriesCount);
            for (series = 1; series <= seriesCount; series++) {
                run("Bio-Formats Importer", "open=[" + currentDirectory + fileList[file] + "] color_mode=Default rois_import=[ROI manager] view=Hyperstack stack_order=XYCZT series_"+series);
                getPixelSize(U, px, py);
                run("Set Scale...", "distance=" + 1/px + " known=1 unit=" + U); //set the scale in case it is missing after treatment
                //check output directory
                if (outAsIn) {dir = inputDir;
                }else {dir = outputDir;}
                dir = replace(dir, File.separator + File.separator,  File.separator);
                imageDir = getInfo("image.directory");
                parentDir = File.getParent(imageDir);
                dirName = File.getName(imageDir);
                if (imageDir == dir) {outputFolder = parentDir + File.separator; // if the image files in the folder directly, save the file in the parent folder
                }else {outputFolder = dir;}
                if (outputFolder.contains(dirName)){
                    outputFolder = outputFolder + "Image_Splitted_" + timeStamp();
                }else {outputFolder = outputFolder + dirName + "_Image_Splitted_" + timeStamp();
                }
                File.makeDirectory(outputFolder);
                //clean image names 
                name = getTitle();
                currentImageName = replace(name, fileExtension, "");
                currentImageName = replace(currentImageName, "-", "_");
                currentImageName = replace(currentImageName, "/", "_stitching_");
                outputBaseName = outputFolder + File.separator + currentImageName;
                // transform 12 bits to 8 bits
                if (transform_to_8bit){depth_to_8bit();}
                // color the image with dedicated color code
                color_channels(grayCh, yellowCh, cyanCh, megaCh);
                saveAs("Tiff", outputBaseName + ".tif");
                if (channelSplit){split_channel_save(outputBaseName);} //split channel only with gray scale
                close("*");
            }
        } else if (endsWith(fileList[file], "/")) {
            processBioFormatFiles(currentDirectory + fileList[file]);
        }
    }
}

// --- function definitions ---
function color_channels(grayCh, yellowCh, cyanCh, megaCh) {
    //change each channel to desired color
    if (grayCh != 0){Stack.setChannel(grayCh);
    run("Grays");}
    if (yellowCh != 0){Stack.setChannel(yellowCh);
    run("Yellow");}
    if (cyanCh != 0){Stack.setChannel(cyanCh);
    run("Cyan");}
    if (megaCh != 0){Stack.setChannel(megaCh);
    run("Magenta");}
}

function depth_to_8bit(){
    // Only split/merge if more than one channel
    getDimensions(width, height, channels, slices, frames);
    if (channels > 1) {
        run("Split Channels");
        list = getList("image.titles"); //get image list
        cmd = "";
        for (channel = 0; channel < list.length; channel++) {
            selectWindow(list[channel]);
            getStatistics(area, mean, min, max, std, histogram);
            if (bitDepth() == 16 && max <= 4095) {
                setMinAndMax(0, 4095);
                call("ij.ImagePlus.setDefault16bitRange", 12);
                run("8-bit");
            }
            if (bitDepth()!=8) {
                run("8-bit");
            }
            //merge all channels
            cmd += "c" + (channel+1) + "=[" + list[channel] + "] ";
        }
        run("Merge Channels...", cmd + "create ignore");
    } else {
        // For single channel, just ensure it's 8-bit if needed
        if (bitDepth()!=8) {
            run("8-bit");
        }
    }
}

function timeStamp() {
    // generate a time stamp string
    // requires: twoDigit()
    getDateAndTime(year, month, dayOfWeek, dayOfMonth, hour, minute, second, msec);
    timeString = toString(year) + "-" + twoDigit(month) + "-" + twoDigit(dayOfMonth);
    return timeString;
}

function split_channel_save(outputBaseName) {
    //Split channel to save each channel separated
    run("Split Channels");
    list = getList("image.titles"); //get image list
    for (channel = 0; channel < list.length; channel++) {
        selectWindow(list[channel]);
        run("Grays"); //split channel with Gray only 
        file_name_channel = outputBaseName + "_Ch" + (channel+1) + ".tif"; // fix channel index
        saveAs("Tiff", file_name_channel);
        //add scale bar
        if(channel == scaleCh-1){
            run("Duplicate...", " ");
            run("Scale Bar...", "width=" + scaleLength + " height=12 font=42 color=White background=None location=[Lower Left] bold overlay");
            run("Flatten");
            outFile2 = outputBaseName + "_Ch" + scaleCh +"_scalebar.tif"; // fix channel index
            saveAs("Tiff", outFile2);
            close();}
    }
    merge_channel(outputBaseName);
}

function merge_channel(outputBaseName){
    //Merge the selected channel to RGB color 
    list = getList("image.titles");
    //count how many channels to merge
    channelValues = newArray(mergedGrayCh, mergedCyanCh, mergedMegaCh, mergedYellowCh); //value from 0-4, 0 means not included, 1-4 means channel order
    count = 0;
    for (i = 0; i < channelValues.length; i++) {
        if (channelValues[i] != 0) count++;
    }
    nchannel = lengthOf(list);
    if (count < nchannel) {
        close("*");
        print("Not enough channels to merge in image" + imageDir + File.separator + currentImageName);
        return;
    }
    //merge only valid images with channel order not 0
    cmd = "";
    validChannel = "ch";
    for (i = 0; i < channelValues.length; i++) {  //merge channel C4 Gray (i = 0), C5 Cyan (i = 1), C6 Magenta (i = 2), C7 yellow (i = 3)
        if (channelValues[i]!=0) {
            cmd += "c" + (i+4) + "=[" + list[(channelValues[i]-1)] + "] "; //list are 0 start with channel is 1 start.
            validChannel += "-" + channelValues[i];
        }
    }
    print(cmd);
    print(validChannel);
    run("Merge Channels...", cmd + "create ignore");
    run("RGB Color");
    saveAs("Tiff", outputBaseName + "_" + validChannel + "_merged.tif");
    close();
}

function twoDigit(n) {
    // return two-digit version of number (0-padded)
    return IJ.pad(n, 2);
}

//function checkFile(directory, fileExtension){ 
//    //only check if the file with fileExtension exists in the current folder, but not the subfolder 
//    count = 0;
//    fileList = getFileList(directory); 
//    for (i = 0; i < fileList.length; i++) {
//        if (endsWith(fileList[i], fileExtension)) {
//            count++;
//            if (count > 1) {skip;}
//        }
//        return (count > 1);
//    }
//}

function hasfiles(dir, fileExtension) {
    list = getFileList(dir);
    for (i = 0; i < list.length; i++) {
        path = dir + list[i];
        if (endsWith(list[i], fileExtension)) return true;
        if (File.isDirectory(path) && hasfiles(path + File.separator, fileExtension)) return true;
    }
    return false;
}

</rewritten_file> 