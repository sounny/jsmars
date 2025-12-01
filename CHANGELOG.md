# Change Log

## [0.4.1] - 2025-11-29

### Changed
- **Logging**: Cleaned up verbose debug logs from the previous release, moving them to `console.debug` to reduce console noise during normal operation.
- **Configuration**: Explicitly defined `defaultLayer` for Mars in `jmars-config.js` to ensure the Viking basemap loads reliably.

## [0.4.0] - 2025-11-29

### Added
- **Test Suite**: Added `tests/index.html` and `tests/unit.js` using Mocha/Chai to verify core logic (`JMARSState`, `JMARSWMS`).
- **Constants**: Created `src/constants.js` to centralize event names (`EVENTS.BODY_CHANGED`, etc.).

### Changed
- **Refactoring**: Updated all modules (`JMARSMap`, `JMARSState`, `LayerManager`, Tools) to use `EVENTS` constants instead of hardcoded strings.
- **Initialization**: Improved application bootstrap sequence. `JMARSMap` now correctly initializes the default body state and `LayerManager` syncs immediately, preventing blank maps on load.
- **Logging**: Reduced console noise in `JMARSVectors` by moving verbose logs to `console.debug`.
- **State Management**: Added `reset()` method to `JMARSState` for better testability.

### Fixed
- **Race Condition**: Resolved an issue where the Layer Manager might miss the initial layer configuration if initialized after the map event.