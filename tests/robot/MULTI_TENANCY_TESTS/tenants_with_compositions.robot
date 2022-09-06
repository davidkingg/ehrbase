# Copyright (c) 2022 Vladislav Ploaia (Vitagroup - CDR Core Team)
#
# This file is part of Project EHRbase
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
# http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.


*** Settings ***
Documentation   Multitenancy With Compositions Tests
...             Precondition: tenants_operations.robot suite is executed. Tenants are created there.

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/ehr_keywords.robot
Resource        ../_resources/keywords/multitenancy_keywords.robot


*** Test Cases ***
Create Compositions In Tentants And Check Isolation Of Data In Tenants - Positive
    [Documentation]     (Positive cases)
    [Tags]      Positive
    [Setup]     Upload OPT    nested/nested.opt
    ${tnt1}     Decode JWT And Get TNT Value    ${encoded_token_1}
    ${tnt2}     Decode JWT And Get TNT Value    ${encoded_token_2}
    Set Suite Variable   ${tenantTnt1}     ${tnt1}
    Set Suite Variable   ${tenantTnt2}     ${tnt2}
    Create New EHR With Multitenant Token       ${encoded_token_1}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable    ${ehr_id}        ${response.json()['ehr_id']['value']}
    #commit composition (JSON)
    #...     nested.en.v1__full_without_links.json     multitenancy_token=${encoded_token_1}
    commit composition  format=CANONICAL_JSON
    ...                 composition=nested.en.v1__full_without_links.json
    ...                 multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response}     201
    #get composition by composition_uid    ${version_uid}
    #check composition exists
