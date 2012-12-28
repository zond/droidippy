#!/bin/sh

ant -Dbase.port=${PORT:-443} -Dbase.domain=${SERVER:-droidippy.oort.se} clean release && \
jarsigner -verbose -keystore zond.keystore bin/Droidippy-release-unsigned.apk mykey && \
jarsigner -verify bin/Droidippy-release-unsigned.apk && \
zipalign -v 4 bin/Droidippy-release-unsigned.apk bin/Droidippy.apk

