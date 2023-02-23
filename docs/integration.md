# Integrating with the Library
First, add the `mavenCentral` repository to your Gradle buildscript:
```groovy
repositories {
    mavenCentral()
}
```
Then, you can add the `tw-graceful-shutdown` library as a dependency by declaring it in your `dependencies` block as such:
```groovy
dependencies {
    implementation 'com.transferwise.common:tw-graceful-shutdown:<VERSION>'
}
```
> Please replace `<VERSION>` with the latest [version](https://github.com/transferwise/tw-graceful-shutdown/blob/master/gradle.properties)