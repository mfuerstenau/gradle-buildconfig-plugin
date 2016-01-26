/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.fuerstenau.gradle.buildconfig

import org.slf4j.Logger
import org.slf4j.LoggerFactory


/**
 *
 * @author Nuffe
 */
abstract class SourceSetConfigBase {

    private static final Logger LOG = LoggerFactory.getLogger (
        SourceSetConfig.class.getCanonicalName ())

    String version
    String clsName
    String appName
    String packageName
    
    private final Map<String, ClassField> classFields = new LinkedHashMap<>()

    Map<String, ClassField> getBuildConfigFields ()
    {
        return classFields
    }
    
    public void buildConfigField (String type, String name, String value)
    {
        addClassField (type, name, value)
    }
   
    void addClassField (String type, String name, String value)
    {
        addClassField (classFields, new ClassFieldImpl (type, name, value))
    }   
    
    void addClassField (Map<String, ClassField> dest, ClassField cf)
    {
        ClassField alreadyPresent = dest.get (cf.getName ())

        if (alreadyPresent != null)
        {
            LOG.debug "{}: buildConfigField <{}/{}/{}> exists, replacing with <{}/{}/{}>",
            name,
            alreadyPresent.type,
            alreadyPresent.name,
            alreadyPresent.value,
            cf.type,
            cf.name,
            cf.value
        }
        dest.put (cf.name, cf)
    }
    
    @Override
    String toString ()
    {
        "version=$version, clsName=$clsName, appName=$appName, packageName=$packageName, buildConfigFields=$buildConfigFields"
    }
}

