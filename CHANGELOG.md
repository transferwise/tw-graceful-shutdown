# Changelog
All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.3.0] - 2021-05-28
### Changed
* Moved from JDK 8 to JDK 11.
* Starting to push to Maven Central again.
* Graceful shutdown strategies can override `ready` method to signal if they are ready to accept traffic.
