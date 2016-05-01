[![Build Status](https://travis-ci.org/mfuerstenau/gradle-buildconfig-plugin.svg?branch=master)](https://travis-ci.org/mfuerstenau/gradle-buildconfig-plugin)

# Build config plugin for Gradle Java and Groovy projects
## What is a build config
A build config is a generated class holding constants set by the build script. It can be accessed within the Java or Groovy application, thus providing a way to transport information about version, project name or debug flags or a lot more info that otherwise will not be available or has to to be transported via complicated workarounds.

## TLDR;
```gradle
buildscript {
  repositories {
    maven {
      url 'https://plugins.gradle.org/m2/'
    }
  }
  dependencies {
    classpath 'gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.1.2'
  }
}
/* this example is for a Java project */
apply plugin: 'java'
apply plugin: 'de.fuerstenau.buildconfig'

/* ... some other stuff like dependencies */

/* closure needs to be present for build config to be created, build config is
 * created for "main" source set that way */
buildConfig {
   /* these set the values of the two default fields VERSION and NAME */
   version = '1.0.1' // default: project.version
   appName = 'some app name' // default: project.name
   /* the package can be set */
   packageName = 'org.sample.buildconfig'  // default: project.group or "de.fuerstenau.buildconfig"
   /* the class name can also be changed */
   clsName = 'MyBuildConfig' // default: BuildConfig
   /* additional fields are possible, garbage can be put, careful, no checking
    * except compiler, simple templating really */
   buildConfigField 'boolean', 'IS_DEBUG', 'true'
}
```
and the usage of the generated class
```java
package org.sample.buildconfig;
public class Main {
   public static void main (String[] args) {
      /* the build config is accessible from the app */
      System.out.println (MyBuildConfig.NAME + ' ' + MyBuildConfig.VERSION +
              "(debug: " + MyBuildConfig.IS_DEBUG ? "enabled" : "disabled" + ')');
   }
}
```

## Basic usage
### Apply plugin
Build script snippet for use in all Gradle versions:
```gradle
buildscript {
  repositories {
    maven {
      url 'https://plugins.gradle.org/m2/'
    }
  }
  dependencies {
    classpath 'gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.1.2'
  }
}

apply plugin: 'de.fuerstenau.buildconfig'
```
Build script snippet for new, incubating, plugin mechanism introduced in Gradle 2.1:
```gradle
plugins {
  id 'de.fuerstenau.buildconfig' version '1.1.2'
}
```
### Basic configuration

If applied, the plugin provides a `buildConfig` closure to set general properties.
If no buildConfig configuration closure is present or at least an empty `buildConfig`
closure is present, a build config for the `main` source set will be created upon
 compilation (of course with default values).

A build config class by default has the name `BuildConfig` and will have to constants that are always present. These are

* `NAME`, (type: `java.lang.String`) the name of the application or lib or project (default: `project.name`),
* `VERSION` (type: `java.lang.String`), the version of the project (default: `project.version`).

As  written above the class name defaults to `BuildConfig` and the _package_ defaults to `project.group` or if not set to `de.fuerstenau.buildconfig`.

The basic properties of the `buildConfig` closure are
* `appName`, sets the value of `NAME`,
* `version`, sets the value of `VERSION`,
* `clsName`, sets the class name,
* `packageName`, sets the package name.

### How it works
There are two tasks that are created for a build config. One generates the java source code the other compiles it. The generating task is named `generateBuildConfig` and the compiling task name is named `compileBuildConfig`. The compiling task depends upon the generating task output and the configuration `compile` is made dependent on the outputs of `compileBuildConfig` task.

* configuration `compile` depends on `generateBuildConfig`,
* `generateBuildConfig` depends on `generateBuildConfig`.

**Example:** A simple `buildConfig` closure, overriding only the `version`:
```gradle
project.name = 'MyProject'
project.group = 'de.fuerstenau.myproject'
...
buildConfig {
   version = '1.0-SPECIAL'
}
```
results in a build config class:
```java
package de.fuerstenau.myproject;

public final class BuildConfig {
   private BuildConfig () {}
   public static final String NAME = "MyProject";
   public static final String VERSION = "1.0-SPECIAL";
}
```
which could be used like this:
```java
 /* Note: we are in the same package as the BuildConfig class */
package de.fuerstenau.myproject;

public class Main {
   public static void main (String[] args) {
      System.out.println (BuildConfig.NAME + ' ' + BuildConfig.VERSION);
   }
```
**Example:** A `buildConfig` closure, overriding everything:
```gradle
buildConfig {
   version = '0.0.1'
   appName = 'MySuperApp'
   clsName = 'MySuperBuildConfig'
   packageName = 'org.my.sample'
}
```
results in a build config class:
```java
package org.my.sample;

public final class MySuperBuildConfig {
   private MySuperBuildConfig () {}
   public static final String NAME = "MySuperApp";
   public static final String VERSION = "0.0.1";
}
```
## Source sets
As stated above the general `buildConfig` closure creates a build config for the `main` source set. The `sourceSets` nested closure allows to add build config classes for multiple source sets.

```gradle
buildConfig {
   sourceSets {
      main
      otherSourceSet
   }
}
```
will create build configs for both source sets. As no value is defined in the `buildConfig` closure the default values will be used.

**Note:** If the `sourceSets` closure is used the build config for `main` is **no more automatically** added. Adding a non-existing source set will fail the build. Omitting an existing source set will not create a build config for this source set.

**Example**: A `buildconfig` closure, that only creates a build config for source set `otherSourceSet`:
```gradle
buildConfig {
   version = '1.0-alpha'
   sourceSets {
      otherSourceSet
   }
}
```

Properties can be set for each source set and in general. Specific properties will override general properties:

```gradle
buildConfig {
   version = '1.0-alpha'
   sourceSets {
      main {
         clsName = 'MainConfig'
      }
      otherSourceSet {
         version = '2.0-beta'
      }
   }
}
```

The tasks created for source sets other than `main` will be named `generate<SourceSet>BuildConfig` and `compile<SourceSet>BuildConfig` (`<sourceSet>` being the name of the source set). The configuration used for dependency will be `compile<SourceSet>` (`<SourceSet>` as above).

## Additional fields
The `buildConfigField (String, String, String)` method allows for additional fields. **First parameter is the _type_** (_Java type_ to be exact), **second parameter is the name** (use uppercase these fields are defined as `static final`), and the **third is value**. Any type is allowed but the plugin uses simple templating therefore entering any garbage will result in build failure.

It can be used inside the general closure or a specific source set closure:
```gradle
buildConfig {
   version = '1.0'
   /* using Java style parentheses */
   buildConfigField ('String', 'MY_STRING', 'some string value')
   /* primitives can be used, too,
    * Note: no parentheses as Gradle is Groovy */
   buildConfigField 'int', 'MY_INT', '23'
   /* assume there is an enum "org.sample.Option" with constant "SIMPLE"
    * it can be used as long as fully qualified path is provided because there are
    * no imports */
   buildConfigField 'org.sample.Option', 'MY_ENUM', 'org.sample.Option.SIMPLE'
   sourceSets {
      main
      /* assume there is another source set */
      otherSourceSet {
         buildConfigField 'String', 'SECRET_STR', 'haha secret'
         buildConfigField 'int', 'MY_INT', '42'
      }
   }
}
```
The result will be a build config for `main` source set like
```java
/* assume there is no "project.group" defined */
package de.fuerstenau.buildconfig;
public final class BuildConfig {
   private BuildConfig () {};
   public static final String NAME = "...";  /* project.name */
   public static final String VERSION = "1.0";
   public static final String MY_STRING = "some string value";
   public static final String MY_INT = 23;
   public static final org.sample.Option MY_ENUM = org.sample.Option.SIMPLE;
}
```
and a build config for `otherSourceSet`:
```java
/* assume there is no "project.group" defined */
package de.fuerstenau.buildconfig;
public final class BuildConfig {
   private BuildConfig () {};
   ...
   /* everything from the general closure plus the one from the specific
    * source set closure and the "MY_INT" value is overridden */
   public static final org.sample.Option MY_ENUM = org.sample.Option.SIMPLE;
   public static final String MY_INT = 42;
}
```
The plugin is a bit smart here and checks for types `String` and `char` and adds `""` or `''` respectively.

## Known side effects
The configuration `compile` or any other `compile<SourceSet>` configuration must not be resolved before the `buildConfig` closure or else the build fails because a resolved configuration may not have a dependency added. Try moving the `buildConfig` closure a bit up in the build script.

## Build files
As stated above the tasks of `de.fuerstenau.gradle.buildconfig.GenerateBuildConfigTask` as created and configured by the plugin default to `${buildDir}/gen/buildconfig/<sourceSetName>/src` for sources and `${buildDir}/gen/buildconfig/<SourceSet>/classes` for the compiled generated sources (with `<SourceSet>` being the name of the source set).

## Advanced usage (not preferred)
### Manual creation of tasks and wiring
If the need arises to manually create  buildconfig task, the plugin must be added as dependency but not to be applied. That way no tasks are automatically created. 

```gradle
buildscript {
  repositories {
    maven {
      url 'https://plugins.gradle.org/m2/'
    }
  }
  dependencies {
    classpath 'gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.1.2'
  }
}
// apply plugin: 'de.fuerstenau.buildconfig' <- this would create tasks automatically, that must be omitted

task generateBuildConfig (type: de.fuerstenau.gradle.buildconfig.GenerateBuildConfigTask) {
    appName = 'SuperTrooperStarshipApp'
    version = '1.1.2'
    clsName = 'MainConfig'
    packageName = 'org.sample'
    outputDir = new File ("${buildDir}/gen/buildconfig/src/main/")
}

task compileBuildConfig(type:JavaCompile, dependsOn: generateBuildConfig) {
    classpath = files () /* empty files suffices */
    destinationDir = new File ("${buildDir}/gen/buildconfig/classes/main/")
    source = generateBuildConfig.outputDir
}

afterEvaluate {
    dependencies {
        compile compileBuildConfig.outputs.files
    }
}
```
