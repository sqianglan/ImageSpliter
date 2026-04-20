
//@int (label = "selected Z position", value = 1) z  

// select z plan and save with color.
outputDir = getDirectory("image");

name = getTitle();
t= lastIndexOf(name,".tif");
currentImage_name = substring(name, 0, t);
run("Duplicate...", "duplicate slices=" + z);

outFile = outputDir + currentImage_name + "_z" + z + ".tif";

Stack.setChannel(4);
run("Yellow");

Stack.setChannel(1);
run("Grays");
Stack.setChannel(2);
run("Magenta");
Stack.setChannel(3);
run("Cyan");
//run("16_colors");
//saveFile(outFile);
saveAs("Tiff", outFile);
close();

close();
