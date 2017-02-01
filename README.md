[![License](https://img.shields.io/badge/license-MIT-blue.svg) ](https://github.com/mfuerstenau/gradle-buildconfig-plugin/blob/master/LICENSE)
[![Build Status](https://travis-ci.org/mfuerstenau/gradle-buildconfig-plugin.svg?branch=master)](https://travis-ci.org/mfuerstenau/gradle-buildconfig-plugin)
[![Download](https://api.bintray.com/packages/mfuerstenau/maven/gradle-buildconfig-plugin/images/download.svg) ](https://bintray.com/mfuerstenau/maven/gradle-buildconfig-plugin/_latestVersion)

# BuildConfig Gradle-plugin for Java and Groovy projects
Provides a class (or multiple) containing constants (```public static final```) defined in the buildscript and available in the project. It can contain simple infos like  application _name_ or _version_ but also input from other plugins like _subversion commit number_ or _git commit hash_.

## Compatibility

* Oracle JDK and OpenJDK 1.7 compatible,
* Gradle 2.9-3.0 with Java and/or Groovy plugin tested.

## Dependency
How to add as buildscript dependency .
### Easy (Gradle 2.1+)
```gradle
plugins {
  id 'de.fuerstenau.buildconfig' version '1.1.8-SNAPSHOT'
}
```
### Classic (Gradle prior to 2.1)
A little bit more control though.
```gradle
buildscript {
  repositories {
    maven {
      url 'https://plugins.gradle.org/m2/'
    }
  }
  dependencies {
    classpath 'gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.1.8-SNAPSHOT'
  }
}
apply plugin: 'de.fuerstenau.buildconfig' // actually applies the plugin
```
### Local
You can download the ```.jar```-file from the button at the top and use as file dependency
```gradle
buildscript {
  dependencies {
    classpath files ("${projectDir}/lib/BuildConfig-1.1.8-SNAPSHOT-SNAPSHOT.jar") // insert the path to .jar-file
  }
}
apply plugin: 'de.fuerstenau.buildconfig'
```
By default a ```BuildConfig``` class ```BuildConfig``` in a package equal to the defined ```group``` or ```de.fuerstenau.buildconfig``` if no ```group``` is defined. Also the ```BuildConfig``` is by default generated for the _main_ ```SourceSet```.

## BuildConfig
A ```BuildConfig``` class (class name can be configured) has always two (2) non-optional fields that are always provided, but may not always contain useful data, depending on the availability.
* ```String NAME``` - the application name (default: ```project.name```)
* ```String VERSION``` - the application version (default: ```project.version``` or if former not set ```"unspecified"```)

A ```BuildConfig``` is generated as _Java_ code and then compiled to a ```.class```-file and added to the _classpath_ of a ```SourceSet``` and also added to the _outputs_, therefore contained in a ```.jar```-file produced from these _outputs_.

Here is an example how such a generated _Java_ class might look like:
```java
package de.fuerstenau.buildconfig;
/** DO NOT EDIT. GENERATED CODE */
public final class BuildConfig
{
   private BuildConfig () { /*. no instance */ }
   public static final String VERSION = "1.0-SNAPSHOT";
   public static final String NAME = "HelloWorld";
}
```
and usage inside the application might look like this:
```java
package some.package;
import de.fuerstenau.buildconfig.BuildConfig;

public class HelloBuildConfig
{
   public static void main (String[] args)
   {
      System.out.println (BuildConfig.NAME);
      System.out.println (BuildConfig.VERSION);
   }
}
```
Of course the generated _Java_ code won't normally show up because the compiled class will be added to the classpath and it really depends on the _IDE_ used, if the class is visible somewhere (Netbeans 8.1 show it after building reloading a project as dependency).

## Configuration
### Basic configuration
The plugin can be configured using the provided ```buildConfig { }```configuration closure. The following closure show the the basic properties and their default values:
```gradle
buildConfig {
    appName = project.name       // sets value of NAME field
    version = project.version // sets value of VERSION field,
                                 // 'unspecified' if project.version is not set
    
    clsName = 'BuildConfig'      // sets the name of the BuildConfig class
    packageName = project.group  // sets the package of the BuildConfig class,
                                 // 'de.fuerstenau.buildconfig' if project.group is not set
    charset = 'UTF-8'            // sets charset of the generated class,
                                 // 'UTF-8' if not set otherwise
}
```

### Additional fields
Additional fields can be added to the ```BuildConfig``` class easily through the ```buildConfigField (String type, String name, String value)```-method. This method can be repeated multiple time. 
* ```type``` - the of the field, which can be any _Java_ _object_ or _Java primitive type_ (eg. ```'int'``` or ```'org.sampel.SomeType'```, note: parameter is a ```String```),
* ```name``` - the name of the field, as the field will be a ```static final``` conventions says it should be an uppercasename (eg. ```'MY_FIELD'```, also a ```String```)
* and last but not least ```value``` - the value of the field, must be a valid value for the type (eg. ```'13'``` or ```' { (byte) 0xfe, (byte) 0x11 }'```, note: also a ```String```).

No ```import```-statements are generated, if you need a type that is not a Java standard type, you need to provide a fully qualified class name and this type must also be available in application. Additionally no syntax checks are performed, therefore entering any invalid or dangerous values may cause harm (remember to escape if the values come from unsafe sources).

Example:
```gradle
buildConfig {
    buildConfigField 'String', 'MY_STR_FIELD', '"my message to the app"'
    buildConfigField 'int', 'MY_INT_FIELD', '42'
    buildConfigField 'byte[]', 'MY_BYTE_ARRAY_FIELD', '{ (byte) 0xfa, (byte) 0x20, (byte) 0x22 }'
    buildConfigField 'long', 'BUILD_UNIXTIME', System.currentTimeMillis() + 'L'
    buildConfigField 'java.util.Date', 'BUILD_DATE', 'new java.util.Date(' + System.currentTimeMillis() + 'L)'
    buildConfigField 'java.time.Instant', 'BUILD_INSTANT', 'java.time.Instant.ofEpochMilli(' + System.currentTimeMillis() + 'L)'
}
```
**Note:** There is some _black magic_ included to make ```String``` and ```char``` constans more legible, instead of
```gradle
buildConfigField 'String', 'MY_STR_FIELD', '"my message to the app"'
buildConfigField 'char', 'MY_CHAR_FIELD', "'x'" // or '\'x\'' for that matter
```
one could alsow write
```gradle
buildConfigField 'String', 'MY_STR_FIELD', 'my message to the app'
buildConfigField 'char', 'MY_CHAR_FIELD', 'x'
```

### Per-SourceSet-Configuration
It's possible to configure per ```SourceSet```. Without per-SourceSet-configuration the ```BuildConfig``` is generated for the _default_ ```SourceSet``` which is ```main```. Th configuration closure provides a method ```sourceSets (Closure sourceSetsClosure)``` which can be used to accomplish this. The parameter ```sourceSetsClosure``` contains the names (case-sensitive) of ```SourceSet``` instances followed by a configuration closure which has the same properties and methods as the ```buildConfig``` configuration closure, minus a ```sourceSets```-method because we do not want endless recursion.

```gradle
buildConfig {
    sourceSets {
        main {
            // configuration of 'main' SourceSet
        }
        someOther {
            // configuration of 'someOther' SourceSet
        }
    }
}
```
There is some special behaviour though:
* Properties from the ```buildConfig``` closure are inherited and overridden if defined in a specific ```SourceSet``` configuration closure.
* If ```sourceSets``` method is used the plugin will generate a ```BuildConfig``` for the ```'main'```-```SourceSet``` only if it's explicitly configured within.

The following configuration omits a ```BuildConfig``` for ```'main'```-```SourceSet``` because it's not in the ```sourceSets```-closure, ```appName``` is inherited but overridden for ```someOther```:
```gradle
buildConfig {
    appName = 'MyAppName'
    sourceSets {
        someOther {
            appName = 'MyOtherAppName'
            // configuration of 'someOther' SourceSet
        }
        yetSomeOther {
            // configuration of 'yetSomeOther' SourceSet
        }
    }
}
```
## Advanced usage
### Manual task wiring
This is an example of manually creating the tasks and wiring them to generate and compile a build config for ```main```-```SourceSet```. This is very similar to the internal working of the plugin.

First we don't want the plugin to be applied, only to be resolved. This can be done like this
#### Prior to Gradle 3.0
```gradle
buildscript {
    repositories {
        maven {
            url 'https://plugins.gradle.org/m2/'
        }
    }
    dependencies {
        classpath 'gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.1.8-SNAPSHOT'
    }
}
plugins {
    id 'java'
}
```
**Note:** we don't apply the plugin, we just need to resolve the classpath.
#### Gradle 3.0
Since Gradle 3.0 there is a new option to resolve a plugin and make it available on the classpath, but not to apply it. That way You can omit the whole ```buildscript```-closure from above and instead use only the ```plugins```-closure.
```gradle
plugins {
    id 'java'
    id 'de.fuerstenau.buildconfig' version '1.1.8-SNAPSHOT' apply false
}
```
**Note:** The apply false at the end of the plugin declaration.

And then we create the tasks and *wire* them
```gradle
/* the generating task, creates the .java-file */
task generateBuildConfig (type: de.fuerstenau.buildconfig.GenerateBuildConfigTask) {
    /* we need to define an output dir for the generated .java-file */
    outputDir = new File ("${buildDir}/gen/buildconfig/src/main/")

    /* the task has nearly the same properties as the buildconfig closure */
    appName = 'SuperTrooperStarshipApp'
    clsName = 'MainConfig'
    packageName = 'org.sample'
    buildConfigField 'int', 'MY_INT_FIELD', '42'
}
/* the compiling task, compiles the generated .java file */
task compileBuildConfig(type:JavaCompile, dependsOn: generateBuildConfig) {
    classpath = files () // we need no extra class path
    /* where do we want our .class file */
    destinationDir = new File ("${buildDir}/gen/buildconfig/classes/main/")
    /* the input is the output of the generating task */
    source = generateBuildConfig.outputDir
}

sourceSets {
    main {
        /* last but not least we want our buildconfig to be part of the classpath */
        compileClasspath += compileBuildConfig.outputs.files
        /* also we want the .class-file to be included in the default .jar-artifact,
         * therefore we add our outputs to the sourceset's outputs, we nee to filter out
         * the dependency-cache */
        compileBuildConfig.outputs.files.findAll {
           !it.name.endsWith ('dependency-cache') // we don't want these
        }.each {
           output.dir it // add everything else
        }
    }
}
```

## Alternative Maven repository
Should there be a problem with the repository at ```plugins.gradle.org```, this can be used as alternative:
```gradle
buildscript {
  repositories {
    maven {
      url 'https://dl.bintray.com/mfuerstenau/maven'
    }
  }
  dependencies {
    classpath 'gradle.plugin.de.fuerstenau:BuildConfigPlugin:1.1.8-SNAPSHOT'
  }
}
apply plugin: 'de.fuerstenau.buildconfig' // actually applies the plugin
```
## Gradle plugin portal
The plugin is listed at [https://plugins.gradle.org/plugin/de.fuerstenau.buildconfig](https://plugins.gradle.org/plugin/de.fuerstenau.buildconfig).
