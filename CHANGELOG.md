# Change Log
All notable changes to this project will be documented in this file. This change log follows the conventions of [keepachangelog.com](http://keepachangelog.com/).

## [0.0.4] - 2020-03-28
### Fixed
- Fix NPE in deg_to_cardinal function caused by missing wind degrees property in json.

### Added
- native-image config for lein native-image plugin.

## [0.0.3] - 2020-03-27
### Added
- Tooltip with additional weather info.

### Changed
- Updated README.md and CHANGELOG.md with new tooltip info.
- Added screenshot of tray.

## [0.0.2] - 2020-03-25
### Added
- This changelog.

### Changed
- Change config file format from json to edn to be able to define colors as numbers.
- Update README.md.

### Fixed
- Fix bug in find-closest function to return closest number event for key k outside of sorted-map sm
- Fix icon-size.