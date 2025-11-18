# Java Module Packaging Gradle Plugin - Changelog

## Version 1.2
* [Fixed] [#69](https://github.com/gradlex-org/java-module-packaging/issues/69) - Use an args file for 'jpackage' to not exceed command line length limits (Thanks [Oliver Kopp](https://github.com/koppor) for contributing!)

## Version 1.1
- Configuration option for `--jlink-options` 
- Configuration option for `--verbose`
- Configuration option to build packages in one step (interesting for MacOS signing)
- More options to add custom resources to a package
- Option to explicitly build the 'app-image' folder only (or in addition)
- By convention, set default target to current operating system and architecture

## Version 1.0.1
* Do not bind platform-specific assemble tasks to a lifecycle

## Versions 1.0
* Initial stable release

## Versions 0.1
* Initial features added
