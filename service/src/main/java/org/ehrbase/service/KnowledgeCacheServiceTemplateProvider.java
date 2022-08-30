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

import java.util.Optional;
import org.ehrbase.ehr.knowledge.I_KnowledgeCache;
import org.ehrbase.webtemplate.model.WebTemplate;
import org.ehrbase.webtemplate.templateprovider.TemplateProvider;
import org.openehr.schemas.v1.OPERATIONALTEMPLATE;

/**
 * @author Stefan Spiska
 */
public class KnowledgeCacheServiceTemplateProvider implements TemplateProvider {

    private final I_KnowledgeCache knowledgeCacheService;
    private final IntrospectService introspectService;

    public KnowledgeCacheServiceTemplateProvider(
            I_KnowledgeCache knowledgeCacheService, IntrospectService introspectService) {
        this.knowledgeCacheService = knowledgeCacheService;
        this.introspectService = introspectService;
    }

    @Override
    public Optional<OPERATIONALTEMPLATE> find(String s) {
        return knowledgeCacheService.retrieveOperationalTemplate(s);
    }

    @Override
    public Optional<WebTemplate> buildIntrospect(String templateId) {
        return Optional.ofNullable(introspectService.getQueryOptMetaData(templateId));
    }
}
