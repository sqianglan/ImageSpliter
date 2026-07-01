# ImageSpliter Macro: Image Exporting Suite

This repository contains a unified Fiji/ImageJ macro, [src/Image Exporting Suite.ijm](/Users/yu25465/Documents/Projects_local/Developing/ImageSpliter/src/Image%20Exporting%20Suite.ijm), for three microscopy export workflows:

- RGB image batch export
- Multichannel image batch export
- Interactive Z-slice export from the current image or a reviewed folder

The macro is designed for Bio-Formats-compatible microscopy files and supports multi-series datasets.

## Requirements
- [Fiji (ImageJ)](https://fiji.sc/)
- Bio-Formats plugin (included with standard Fiji)

## Installation
1. Open Fiji.
2. Run [src/Image Exporting Suite.ijm](/Users/yu25465/Documents/Projects_local/Developing/ImageSpliter/src/Image%20Exporting%20Suite.ijm) from `Plugins > Macros > Run...`.

## Workflows
### RGB Image
Batch export RGB/composite images from a folder.

Options:
- Output to source folder or a separate output folder
- File suffix filter
- Export grayscale final image
- Add scale bar
- Batch silent mode

Output:
- One TIFF per input image

### Multichannel Image
Batch export multichannel images from a folder with channel recoloring.

Options:
- Output to source folder or a separate output folder
- File suffix filter
- Gray / Cyan / Magenta / Yellow channel assignment
- Convert to 8-bit image
- Split channels
- Add scale bar
- Batch silent mode

Output:
- A color-mapped TIFF for each image
- Optional per-channel grayscale TIFFs
- Optional merged TIFF assembled from the selected channels

### Z-Slice Export
Extract a single Z slice from either:
- the current open image
- a folder of files reviewed one by one

Both modes now use the same review/export pipeline.

Options:
- File type filter for folder review
- Gray / Cyan / Magenta / Yellow channel assignment
- Per-channel merge inclusion flags
- Add scale bar
- Split channels
- Convert split channels to 8-bit

Review behavior:
- The image opens in LUT-colored preview mode
- Brightness/Contrast opens during review
- You browse to the desired Z plane, then press OK to export
- Current-image mode prompts for an output directory if the active image has no `image.directory`

Output:
- Raw extracted single-Z multichannel TIFF: `_z#_raw.tif`
- Optional split channel TIFFs: `_z#_Ch#.tif`
- Color export TIFF: `_z#_ch-..._merged_rgb.tif`

## General Behavior
- Recurses through subfolders during batch processing
- Handles Bio-Formats multi-series files
- Builds timestamped output folders for batch export
- Uses the image calibration when adding scale bars

## Notes
- Channel numbering is 1-based, matching Fiji/ImageJ channel indices.
- The macro has been developed around Fiji on macOS and Bio-Formats-supported microscopy formats such as `.lif`, `.zvi`, `.czi`, `.nd2`, `.lsm`, `.ome.tif`, `.tif`, and `.tiff`.
- Contact: qiang.lan@bristol.ac.uk

## License
Creative Commons CC-BY-SA