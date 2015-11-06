#!/bin/sh

ant -Dbase.port=${PORT:-80} -Dbase.domain=${SERVER:-droidippy.oort.se} clean release && \
jarsigner -verbose -sigalg SHA1withDSA -digestalg SHA1 -keystore zond.keystore bin/Droidippy-release-unsigned.apk mykey && \
jarsigner -verify -verbose -certs bin/Droidippy-release-unsigned.apk && \
zipalign -v 4 bin/Droidippy-release-unsigned.apk bin/Droidippy.apk

