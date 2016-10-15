# Redok

A simple app to control my Argon Audio Stream 2.

It contains an incomplete Java implementation of the Frontier Silicon API,
inspired by [fsapi](https://github.com/tiwilliam/fsapi) for Python.

I called the app Redok because the official app for my Argon Audio Stream 2 is called "Undok".

# Why

Because the Undok app forces me to

1. Wait for it to find a device
2. Select a device

Even though I only have one device!

I only have one device and I don't want to spend my precious time using software that is not
customized to my needs.

# Debug build
```
./gradlew app:assembleDebug
```

# Lombok
This project uses lombok to reduce boiler plate code.

Android studio users need to do this to build the project:
* Install the Lombok plugin
 * `Preferences > Plugins > Browse repositories > search for Lombok`
* Enable annotation processing
 * `File > Other Settings > Default Preferences > Build, Execution, Deployment > Compiler > Annotation Processors > Enable annotation processing`

# Retrofit 2
The api is modeled using retrofit 2. The interface FsApi contains the http api model. Currently
it only contains a few api calls.

# UI
The UI is super simple, designed to fit my needs: Switch between Spotify / Radio and control volume.

# Contributions
Feel free to contribute.
