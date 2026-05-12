# ImageSpliter Macro: Image Exporting Suite

This repository now includes a unified ImageJ macro, [src/Image Exporting Suite.ijm](/Users/yu25465/Documents/Projects_local/Developing/ImageSpliter/src/Image Exporting Suite.ijm), for batch processing RGB images, full multichannel image stacks, and selected Z optical sections from multichannel stacks. It is designed to streamline workflows for researchers working with multi-channel, multi-series microscopy data.

The new suite is built on the polished batch exporter in `Processing with better UI_fixed.ijm` and adds a third workflow for interactively choosing a Z slice from each imported multichannel series before exporting the extracted single-Z stack and its downstream outputs. It should work with other image formats compatible with the Bio-Formats plugin. This macro has been tested with `.zvi` and `.tif` files on a MacBook Pro (M4 Pro, macOS Sequoia).

This project was inspired by discussions on image.sc, and the final script was optimized with the help of Cursor.

## Main Features
- **Batch Processing:** Process all compatible files in a selected directory (and subdirectories).
- **Unified Workflow Selector:** Choose between Single-channel image, Multi-channel image, and Z-section selection.
- **Channel Splitting:** Automatically split multi-channel images into separate grayscale TIFF files.
- **Channel Coloring:** Assign custom colors to channels (Gray, Cyan, Magenta, Yellow) for merged output.
- **Interactive Z-section Export:** Pause on each multichannel series, select one optical section, then export that extracted single-Z stack together with the usual processed outputs.
- **Scale Bar Addition:** Optionally add a scale bar to a specified channel.
- **8-bit Conversion:** Convert images to 8-bit for compatibility and reduced file size.
- **Handles Single-Channel and Multi-Channel Images:** 

## Requirements
- [Fiji (ImageJ)](https://fiji.sc/) with the Bio-Formats plugin installed (included by default in Fiji).

## Installation
1. Download or copy the macro file `Image Exporting Suite.ijm` into your Fiji/ImageJ macros directory or any folder of your choice.
2. (Optional) Set the default input directory in the macro if you want it to always start in a specific folder.

## Usage
1. Open Fiji (ImageJ).
2. Go to `Plugins > Macros > Run...` and select `Image Exporting Suite.ijm`.
3. Follow the dialogs to:
   - Select the input directory
   - Choose output options
   - Set file suffix (e.g., `.lif`, `.tif`)
   - Choose whether to convert to 8-bit
   - Select one of the three workflows
   - Assign colors to channels for multichannel and Z-section export
   - Enable/disable channel splitting and scale bar
   - In Z-section mode, review each series and choose the target Z slice before export
4. The macro will process all matching files in the selected directory (and subfolders), saving results as TIFFs.

## Output
- **Split Channels:** Each channel saved as a separate grayscale TIFF.
- **Merged Image:** Optionally, a color-merged TIFF using selected channels/colors.
- **Selected Z Raw Stack:** In Z-section mode, the extracted single-Z multichannel stack is saved before recoloring and split export.
- **Scale Bar:** If enabled, a version of the channel with a scale bar overlay.

## Notes
- The macro is robust for both single-channel and multi-channel images.
- For questions or issues, contact: qiang.lan@bristol.ac.uk

## License
Creative Commons CC-BY-SA 