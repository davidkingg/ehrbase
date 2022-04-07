/*
 * Copyright (c) 2022 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

/**
 * @author luis
 */
@Configuration
@ConfigurationProperties(prefix = "terminology-server")
public class FhirTsProps {
    private String codePath = "$[\"expansion\"][\"contains\"][*][\"code\"]";
    private String systemPath = "$[\"expansion\"][\"contains\"][*][\"system\"]";
    private String displayPath = "$[\"expansion\"][\"contains\"][*][\"display\"]";
    private String tsUrl = "https://r4.ontoserver.csiro.au/fhir/";
    private String validationResultPath = "$.parameter[?(@.name='result')].valueBoolean";

    public String getValidationResultPath() {
        return validationResultPath;
    }

    public void setValidationResultPath(String validationResultPath) {
        this.validationResultPath = validationResultPath;
    }

    public String getTsUrl() {
        return tsUrl;
    }

    public void setTsUrl(String tsUrl) {
        this.tsUrl = tsUrl;
    }

    public String getCodePath() {
        return codePath;
    }

    public void setCodePath(String codePath) {
        this.codePath = codePath;
    }

    public String getSystemPath() {
        return systemPath;
    }

    public void setSystemPath(String systemPath) {
        this.systemPath = systemPath;
    }

    public String getDisplayPath() {
        return displayPath;
    }

    public void setDisplayPath(String displayPath) {
        this.displayPath = displayPath;
    }
}
