# Easy Imaging Exporting Suite Plugin

This module is a Java Fiji plugin rewrite of the Easy Imaging Exporting Suite.

## What It Includes
- A real Swing UI with tabs for:
  - RGB Image batch export
  - Multichannel Image batch export
  - Z-Slice Export and folder review
- Bio-Formats-backed file opening for supported microscopy formats
- Folder review with a persistent control panel instead of IJM modal dialogs
- Current-image Z export plus split-channel, composite, and RGB outputs

## Project Layout
- `pom.xml`: Maven build file
- `src/main/java/org/imagespliter/fiji/`: plugin source
- `src/main/resources/plugins.config`: ImageJ plugin menu registration

## Build
From this folder:

```bash
mvn package
```

The resulting jar can be copied into Fiji's `plugins` folder.

## Runtime Requirements
- Fiji / ImageJ
- Bio-Formats available in Fiji
- Java 11+

## Current Limitation In This Workspace
This workspace currently does not have a usable Java runtime or Maven available, so the code was scaffolded but not compiled locally here.
