// Copyright (c) 2015, 2025, Oracle and/or its affiliates.

//-----------------------------------------------------------------------------
//
// This software is dual-licensed to you under the Universal Permissive License
// (UPL) 1.0 as shown at https://oss.oracle.com/licenses/upl and Apache License
// 2.0 as shown at http://www.apache.org/licenses/LICENSE-2.0. You may choose
// either license.
//
// If you elect to accept the software under the Apache License, Version 2.0,
// the following applies:
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    https://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.
//
//-----------------------------------------------------------------------------

package com.oracle.mobile.fusabase;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;

import java.io.File;
import java.io.FileReader;
import java.io.PrintWriter;
import java.io.Reader;

import jakarta.json.Json;
import jakarta.json.JsonObject;
import jakarta.json.JsonReader;
import jakarta.json.JsonValue;

/**
 * Gradle plugin that reads fusabase-config.json and generates Android string resources.
 */
public class FusabaseConfig implements Plugin<Project> {
    @Override
    public void apply(Project project) {

        File configFile = project.file("fusabase-config.json");
        if (!configFile.exists()) {
            throw new GradleException("fusabase-config.json is not found in " + project.getName()
                    + ". Please provide the config file in the app/ directory.");
        }

        try (Reader configReader = new FileReader(configFile);
             JsonReader objectReader = Json.createReader(configReader)) {

            JsonObject configJson = objectReader.readObject();

            File resourceDirectory = new File(project.getProjectDir(), "src/main/res/values");
            File outputFile = new File(resourceDirectory, "fusabase.generated.xml");

            if (!resourceDirectory.exists()) {
                resourceDirectory.mkdirs();
            }
            try (PrintWriter resourceWriter = new PrintWriter(outputFile)) {
                resourceWriter.println("<?xml version=\"1.0\" encoding=\"utf-8\"?>");
                resourceWriter.println("<resources>");
                for (String key : configJson.keySet()) {
                    JsonValue jsonValue = configJson.get(key);
                    String value;
                    if (jsonValue.getValueType() == JsonValue.ValueType.STRING) {
                        value = configJson.getString(key);
                    } else if (jsonValue.getValueType() == JsonValue.ValueType.OBJECT) {
                        for(String nestedKey : jsonValue.asJsonObject().keySet()){
                            String nestedValue = jsonValue.asJsonObject().getString(nestedKey);
                            resourceWriter.printf("    <string name=\"%s\">%s</string>%n", "fusabase_" + nestedKey, nestedValue);
                        }
                        continue;
                    } else {
                        // For other types, convert to string
                        value = jsonValue.toString();
                    }
                    resourceWriter.printf("    <string name=\"%s\">%s</string>%n", "fusabase_" + key, value);
                }
                resourceWriter.println("</resources>");
            }

        } catch (Exception e) {
            throw new GradleException("Cannot read fusabase-config.json. Please make sure that " +
                    "the file is not corrupted.");
        }
    }
}
