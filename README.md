# ImageSpliter Macro: Processing with Better UI

This is an ImageJ macro (`Processing with better UI_fixed.ijm`) for batch processing and splitting multi-dimensional microscopy image files, such as Leica `.lif`, Zeiss `.zvi`, and `.tif`. The macro is designed to streamline workflows for researchers working with multi-channel, multi-series image data.

The original purpose of this macro is to process compact Leica Project files (`.lif`) and save each image as a separate `.tif` file for downstream analysis, avoiding the need to manage large `.lif` files. It should also work with other image formats compatible with the Bio-Formats plugin. This macro has been tested with `.zvi` and `.tif` files on a MacBook Pro (M4 Pro, macOS Sequoia).

This project was inspired by discussions on image.sc, and the final script was optimized with the help of Cursor.

## Main Features
- **Batch Processing:** Process all compatible files in a selected directory (and subdirectories).
- **Channel Splitting:** Automatically split multi-channel images into separate grayscale TIFF files.
- **Channel Coloring:** Assign custom colors to channels (Gray, Cyan, Magenta, Yellow) for merged output.
- **Scale Bar Addition:** Optionally add a scale bar to a specified channel.
- **8-bit Conversion:** Convert images to 8-bit for compatibility and reduced file size.
- **Handles Single-Channel and Multi-Channel Images:** 

## Requirements
- [Fiji (ImageJ)](https://fiji.sc/) with the Bio-Formats plugin installed (included by default in Fiji).

## Installation
1. Download or copy the macro file `Processing with better UI_fixed.ijm` into your Fiji/ImageJ macros directory or any folder of your choice.
2. (Optional) Set the default input directory in the macro if you want it to always start in a specific folder.

## Usage
1. Open Fiji (ImageJ).
2. Go to `Plugins > Macros > Run...` and select `Processing with better UI_fixed.ijm`.
3. Follow the dialogs to:
   - Select the input directory
   - Choose output options
   - Set file suffix (e.g., `.lif`, `.tif`)
   - Choose whether to convert to 8-bit
   - Assign colors to channels
   - Enable/disable channel splitting and scale bar
   - Set batch/silent mode
4. The macro will process all matching files in the selected directory (and subfolders), saving results as TIFFs.

## Output
- **Split Channels:** Each channel saved as a separate grayscale TIFF.
- **Merged Image:** Optionally, a color-merged TIFF using selected channels/colors.
- **Scale Bar:** If enabled, a version of the channel with a scale bar overlay.

## Notes
- The macro is robust for both single-channel and multi-channel images.
- For questions or issues, contact: qiang.lan@bristol.ac.uk

## License
Creative Commons CC-BY-SA 