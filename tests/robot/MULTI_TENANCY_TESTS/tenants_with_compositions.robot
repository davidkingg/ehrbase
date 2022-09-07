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
...             - Precondition: tenants_operations.robot suite is executed.
...             - Tenants are created in tenants_operations.robot.

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/ehr_keywords.robot
Resource        ../_resources/keywords/multitenancy_keywords.robot

*** Variables ***
${getCompositionPositiveCode}   200
${getCompositionNegativeCode}   404
${noCompositionFoundMsg}   No composition with given ID found



*** Test Cases ***
Create Compositions In Tentants And Check Isolation Of Data In Tenants
    [Documentation]     Covers create and get Composition + Isolation of data between tenants.
    ...         \n*Case 1:*
    ...         - Create EHR on tenant 1
    ...         - Create Composition on tenant 1
    ...         - Get Composition from tenant 1 and expect 200 code
    ...         - Get Composition created in tenant 1, from tenant 2 and expect 404 Not Found.
    ...         *Case 2:*
    ...         - Create EHR on tenant 2
    ...         - Create Composition on tenant 2
    ...         - Get Composition from tenant 2 and expect 200 code
    ...         - Get Composition created in tenant 2, from tenant 1 and expect 404 Not Found.
    [Tags]      Positive
    [Setup]     Upload OPT    nested/nested.opt
    ${tnt1}     Decode JWT And Get TNT Value    ${encoded_token_1}
    ${tnt2}     Decode JWT And Get TNT Value    ${encoded_token_2}
    Set Suite Variable   ${tenantTnt1}     ${tnt1}
    Set Suite Variable   ${tenantTnt2}     ${tnt2}
    ## Create EHR and Composition in tenant1 (positive)
    Create New EHR With Multitenant Token       ${encoded_token_1}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable    ${ehr_id}        ${response.json()['ehr_id']['value']}
    commit composition  format=CANONICAL_JSON
    ...                 composition=nested.en.v1__full_without_links.json
    ...                 multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     201
    Get Composition Values And Set Them In Variables
    get composition by composition_uid    ${version_uid}    multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings   ${response.status_code}   ${getCompositionPositiveCode}
    ## Get EHR created in tentant1 from tenant2 (negative)
    get composition by composition_uid    ${version_uid}    multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     ${getCompositionNegativeCode}
    Should Be Equal As Strings      ${response.json()['message']}   ${noCompositionFoundMsg}
    ## Create EHR and Composition in tenant2 (positive)
    Create New EHR With Multitenant Token       ${encoded_token_2}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable    ${ehr_id}        ${response.json()['ehr_id']['value']}
    commit composition  format=CANONICAL_JSON
    ...                 composition=nested.en.v1__full_without_links.json
    ...                 multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     201
    Get Composition Values And Set Them In Variables
    get composition by composition_uid    ${version_uid}    multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     ${getCompositionPositiveCode}
    ## Get EHR created in tentant2 from tenant1 (negative)
    get composition by composition_uid    ${version_uid}    multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     ${getCompositionNegativeCode}
    Should Be Equal As Strings      ${response.json()['message']}   ${noCompositionFoundMsg}


*** Keywords ***
Get Composition Values And Set Them In Variables
    Set Test Variable   ${composition_uid}    ${response.json()['uid']['value']}
    Set Test Variable   ${version_uid}    ${response.json()['uid']['value']}    # full/long compo uid
    Set Test Variable   ${version_uid_v1}    ${version_uid}                  # different namesfor full uid
    Set Test Variable   ${preceding_version_uid}    ${version_uid}          # for usage in other steps

    ${short_uid}=       Remove String       ${version_uid}    ::${CREATING_SYSTEM_ID}::1
                        Set Test Variable   ${compo_uid_v1}    ${short_uid}
                        Set Test Variable   ${versioned_object_uid}    ${short_uid}