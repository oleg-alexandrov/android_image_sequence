# android_image_sequence
An example showing how to make an android app to take a sequence of images with its camera. The idea is that the pictures should be taken in one thread, while saved to disk in a different thread. It is necessary to do it that way since after taking the picture a callback is invoked to process it, and the picture thread must do no work until the processing is done, before taking another picture. 

This code is something that looks simple but I spent a lot of time on.

Build as:

  ANDROID_HOME=$HOME/Android/Sdk ./gradlew assembleDebug;

The files build.gradle and ./app/build.gradle may need to be tweaked (they assume Android SDK version 25).

This example is released under the BSD license. 
