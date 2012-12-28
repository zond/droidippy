droidippy
=========

The Droidippy Android client.

To build it you must create a file called `local.properties` containing the path to your Android SDK directory. In my case:

    sdk.dir=/Users/zond/android-sdk-macosx

`project.properties` defines the SDK version to use when building it. Right now it is 17, but it should work with both older and newer version within reasonable limits.

To build a debug version, run `ant clean debug`. To build a release, you can modify `release.sh` and run that.

Comments, bug reports and patches are welcome.

