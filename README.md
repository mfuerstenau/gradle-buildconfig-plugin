# BuildConfig plugin for Gradle Java/Groovy projects

Generates a build config class that contains constants (like version or debug
flags) set by the build script and can be read from within the Java or Groovy
code.

# Requirements

The `java` or `groovy` (which implicates `java` plugin) plugin needs to be
applied prior to this plugin.

# Download

Build script snippet for use in all Gradle versions:

```gradle
buildscript {
  repositories {
    maven {
      url "https://plugins.gradle.org/m2/"
    }
  }
  dependencies {
    classpath "gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.0.6"
  }
}

apply plugin: "de.fuerstenau.buildconfig"
```

Build script snippet for new, incubating, plugin mechanism introduced in Gradle
2.1:

```gradle
plugins {
  id "de.fuerstenau.buildconfig" version "1.0.6"
}
```

# Usage

## Basics

The plugin provides a configuration closure `buildScript`. If this `buildScript`
closure is missing from the `build.gradle` or if it is empty, nothing will
happen.

A buildconfig contains a few default constants that cannot be omitted and will
have default values unless set to other values. These are

* `VERSION` (type: `String`) containing the version, if not
  set explicitly defaults to the gradle project version,

* `NAME` (type: `String`) containing the name of the project/app, if not
  set explicitly defaults to the gradle project name.

The `buildScript` closure provides properties to set the values of these
constants and configure the `BuildScript` class further.

* `appName`: value of `NAME` (default: `project.version`),
* `version`: value of `VERSION` (default: `project.name`),
* `packageName` the package of the `BuildConfig` class (default: `de.fuerstenau.buildconfig`),
* `clsName` the name of the build config class (default: `BuildConfig`).

If any of the properties is set inside the `buildConfig` closure the plugin will
create two tasks `generateBuildConfig` and `compileBuildConfig` (depends on the
former) and will make the `compile` configuration dependent on the outputs of
`compileBuildConfig` task. That happens in the background and whenever the
project is compiled the two tasks are run prior if needed.

```
buildConfig {
   appName = 'Test App'
   version = '1.0-SNAPSHOT'
   packageName = 'org.sample'
   clsName = 'MyConfig'
}
```

will create a class like 

```
package org.sample;

public final class MyConfig
{
   private MyConfig ()
   {
   }

   public final String VERSION = "1.0-SNAPSHOT";
   public final String NAME = "Test App";
}
```
Of course the class is compiled and added as dependency, but the temporary
source code is still visible in the build directory.

By default the plugin does _nothing_. Yes, that is right. Because of the
different combinations of source sets (introduced by `java`/`groovy`-plugin) a
project can hold, there would be the potential to mess things up by having a
default _build config_ included by simply applying the plugin.

`Build configs` are configured using the `buildConfig` closure, containing a
`sourceSets` closure and then for every source set an own configuration closure.
The simplest closure would look like this:

```gradle
buildConfig {
  sourceSets {
    main {
    }
  }
}
```

`main` is the default source set introduced by the `java` plugin for gradle.
If no further configuration is made, the _build config_ uses the defaults (see
[Defaults](/Defaults/)).

At the moment there are these properties to configure:

* `packageName` (package of the build config class, default: `project.group`),
* `clsName` (class of the build config class, default: `BuildConfig`),
* `appName` (name of the app, default: `project.name`),
* `version` (version of the app, default: `project.version`).

The following closure would configure the _build config_ for the `main` source
set to use _Supercool App_ as name, and using the project version (set to `1.0`
earlier) in combination with a custom suffix. Also the package for the
_build config_ class is changed to `de.fuerstenau.somepackage`.

```gradle
group = 'org.acme'
version = '1.0'

buildConfig {
  sourceSets {
    main {
      packageName = 'de.fuerstenau.somepackage'
      appName = 'Supercool App'
      version = version + '-ALPHA'
    }
  }
}
```

The result would be a class

```java
package de.fuerstenau.somepackage;

public final class BuildConfig
{
  public static final String VERSION = "1.0-ALPHA";
  public static final String NAME = "Supercool App";
}
```

which then could be referenced in the app like this

```java
import de.fuerstenau.somepackage.BuildConfig;

public class HelloWorld
{
   public static void main (String[] args)
   {
      System.out.println (BuildConfig.NAME);
      System.out.println (BuildConfig.VERSION);
   }
}
```

## Custom fields

Custom fields can be added via `buildConfigField` method which takes the type,
the name and the value of the custom field as parameter. All parameters have to
be given as strings (and escaped if necessary, single `String` and `char`
values being the exception).

```gradle
buildConfigField "type", "name", "value"
```
For example, some custom fields:

```gradle
buildConfig {
  sourceSets {
    main {
      buildConfigField "boolean", "IS_DEBUG", "false"
      buildConfigField "String", "SECRET_WORD", "Hinterland"
      buildConfigField "char", "MYCHAR", "a"
    }
  }
}
```

Any types can be referenced, the plugin will not make any imports, therefore
fully qualifierd names have to be used.

*Any garbage entered will likely lead to uncompilable code*

## Manual Wiring

It is also possible to not use the _buildConfig_ configuration closure and
create and wire the tasks manually. The plugin does this automatically for
every source set after evaluating the configuration closure.

It creates a `de.fuerstenau.gradle.buildconfig.GenerateBuildConfigTask` and
uses its outputs as inputs for a `JavaCompile` task and adds the outputs of the
latter as dependency for the `compile` configuration for the source set, and
also adds them to the source set outputs. That way the _build config_ is
included in the artifact _jar_.

Here is an example of manual wiring:

```gradle
task generateBuildConfig (type: de.fuerstenau.gradle.buildconfig.GenerateBuildConfigTask) {
    appName = "SuperTrooperStarshipApp"
    version = "1.1.2"
}

task compileBuildConfig(type:JavaCompile, dependsOn: generateBuildConfig) {
    classpath = files () // empty file collection
    destinationDir = new File("${buildDir}/generatedClasses/main")
    source = generateBuildConfig.outputDir
}

afterEvaluate {
    dependencies {
        compile compileBuildConfig.outputs.files
    }
}
```

# Defaults

The default values for a _build config_ source set configuration closure are

* `appName` defaults to the project name,
* `version` defaults to the project version,
* `packageName` defaults to the project group or `de.fuerstenau.buildconfig`
  if group was not defined.

A resulting `BuildConfig` class will have the constants `NAME` and `VERSION`
( both type `String`).
