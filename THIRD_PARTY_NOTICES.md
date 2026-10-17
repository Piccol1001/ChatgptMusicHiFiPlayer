# Third-party components

The application uses Kotlin, AndroidX Compose/Material3, Glance, WorkManager, Activity, Lifecycle, Media3, Room, DataStore, Paging, DocumentFile and Coroutines under Apache License 2.0; Coil is Apache 2.0. Test tooling includes JUnit (EPL 1.0) and Robolectric (MIT). Consult each resolved artifact's license and NOTICE before distribution. No projectM engine, AutoEq data, bundled music, fonts or third-party visualization presets are included.

Instrumentation audio fixtures are original, generated 440 Hz and 735 Hz sine tones using FFmpeg. They contain no third-party recording and are only packaged in the test APK. FFmpeg is a development tool, not an app dependency.

The [resolved release inventory](docs/DEPENDENCIES.md) lists 128 artifact records. Apache-2.0, bundled upstream notices, Protobuf BSD-3-Clause and MPL-2.0 texts are preserved in [docs/licenses](docs/licenses). The shaded Protobuf dependencies declare BSD-3-Clause. OkHttp carries the Public Suffix List under MPL-2.0; its notice points to https://publicsuffix.org/list/public_suffix_list.dat for source. No network capability is granted to the app. These notices are also bundled as APK assets. A selectable offline license screen is available in Settings; a complete legal audit remains open. No project-wide license is selected on the owner's behalf.

Media3 is pinned to 1.11.1; Glance AppWidget is 1.1.1. The reactive shaders, AIFF extractor and preset curves are original project code. projectM was investigated but is not included; its LGPL code/assets are not shipped.
