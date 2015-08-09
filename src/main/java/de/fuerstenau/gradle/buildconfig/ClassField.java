/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package de.fuerstenau.gradle.buildconfig;

import java.util.Set;

/**
 * @author thatsokaybaby
 */
public interface ClassField
{
   String getType ();

   String getName ();

   String getValue ();

   String getDocumentation ();

   Set<String> getAnnotations ();
}
