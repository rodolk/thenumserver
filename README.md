#Instructions to run this code

## Install
Download zip and unzip or
Clone with git:
git clone https://github.com/rodolk/thenumserver.git

## Install Java

It is recommended you install Java 1.8 from Oracle.
This gradle-wrapper version doesn't work with Java 11.
For Java 11, graddle wrapper must be upgraded.
More on this: https://stackoverflow.com/questions/54358107/gradle-could-not-determine-java-version-from-11-0-2 

To be able to build with javadoc you need to have library tools.jar that comes with Java 8 JDK. 

In Ubuntu, install JDK 8 with:
`sudo apt-get install openjdk-8-jdk`

Otherwise javadoc generation will fail!!!

## Gradle

The build framework provided here uses gradle to build and manage
dependencies.  The `gradlew` command will automatically download gradle for you so you shouldn't need to install anything other than java.


### Building the project from the command line

To build the project on Linux or MacOS run the command `./gradlew build` in a shell terminal.  This will build the source code in
`src/main/java`, run any tests in `src/test/java` and create an output
jar file in the `build/libs` folder.
`./gradlew clean` will clean every file generated by build.


### Running application from the command line

Build a shadow jar from your project by running `./gradlew shadowJar`.  This will create a `numserver-shadow.jar` file in the `build/libs` directory.

You can then start the application by running the command
`java -jar ./build/lib/numserver-shadow.jar`


### Testing
In `src/test/java` you'll find unit tests and integration test done with JUnit 4.
The integration test will take a few minutes to complete.


### Javadoc
`./gradlew javadoc` will generate Javadoc for this project in `build/docs/javadoc`
Open `build/docs/javadoc/index.html` with a browser.


### Troubleshooting
If you do't have tools.jar properly installed and build `./gradlew build` fails, try removing  the following lines lines from file from `build.gradle`:
javadoc {
    source = sourceSets.main.allJava
    classpath = configurations.compile
}



