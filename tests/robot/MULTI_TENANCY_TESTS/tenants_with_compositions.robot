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
...             *Precondition:* tenants_operations.robot suite is executed.
...             - Tenants are created in tenants_operations.robot.
...             \n*Following operations on Compositions were covered:*\n- Create\n- Update\n- Delete\n- Get
...             \n*1. (CREATE+GET Composition) Below cases covers isolation of data between tenants:*
...             - Create EHR on tenant 1\n- Create Composition on tenant 1
...             - Get Composition from tenant 1 and expect 200 code
...             - Get Composition created in tenant 1, from tenant 2 and expect 404 Not Found.
...             - Create EHR on tenant 2\n- Create Composition on tenant 2
...             - Get Composition from tenant 2 and expect 200 code
...             - Get Composition created in tenant 2, from tenant 1 and expect 404 Not Found.
...             \n*2. (CREATE+UPDATE Composition) Below cases covers isolation of data between tenants:*
...             - Create EHR on tenant 1\n- Create Composition on tenant 1
...             - Get Composition from tenant 1 and expect 200 code
...             - Update Composition from tenant 1 and expect 200 code
...             - Get Composition created in tenant 1, from tenant 2 and expect 404 Not Found.
...             - Create EHR on tenant 2\n- Create Composition on tenant 2
...             - Get Composition from tenant 2 and expect 200 code
...             - Update Composition from tenant 2 and expect 200 code
...             - Get Composition created in tenant 2, from tenant 1 and expect 404 Not Found.
...             \n*3. (CREATE+DELETE Composition) Below cases covers isolation of data between tenants:*
...             - Create EHR on tenant 1\n- Create Composition on tenant 1
...             - Delete Composition from tenant 1\n- Get deleted Composition from tenant 1 and expect 204 code
...             - Get Composition from tenant 2, deleted in tenant 1 and expect 404 code Not Found.
...             - Create EHR on tenant 2\n- Create Composition on tenant 2\n- Delete Composition from tenant 2
...             - Get deleted Composition from tenant 2 and expect 204 code
...             - Get Composition from tenant 1, deleted in tenant 2 and expect 404 code Not Found.
Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/ehr_keywords.robot
Resource        ../_resources/keywords/multitenancy_keywords.robot


*** Variables ***
${getCompositionPositiveCode}   200
${getCompositionNegativeCode}   404
${noCompositionFoundMsg}        No composition with given ID found


*** Test Cases ***
1. Create And Get Compositions In Tentants And Check Isolation Of Data Between Tenants
    [Documentation]     Covers create and get Composition + Isolation of data between tenants.
    [Tags]      Positive    Negative
    Upload OPT    nested/nested.opt     multitenancy_token=${encoded_token_1}
    Create And Get Composition On Specific Tenant   multitenancy_token=${encoded_token_1}
    ## Get EHR created in tentant1 from tenant2 (negative)
    get composition by composition_uid    ${version_uid}    multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     ${getCompositionNegativeCode}
    Should Be Equal As Strings      ${response.json()['message']}   ${noCompositionFoundMsg}
    Upload OPT    nested/nested.opt     multitenancy_token=${encoded_token_2}
    Create And Get Composition On Specific Tenant   multitenancy_token=${encoded_token_2}
    ## Get Compositiom created in tentant2 from tenant1 (negative)
    get composition by composition_uid    ${version_uid}    multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     ${getCompositionNegativeCode}
    Should Be Equal As Strings      ${response.json()['message']}   ${noCompositionFoundMsg}

2. Create And Update Compositions In Tentants And Check Isolation Of Data Between Tenants
    [Documentation]     Covers create and update Composition + Isolation of data between tenants.
    [Tags]      Positive    Negative
    Upload OPT    nested/nested.opt     multitenancy_token=${encoded_token_1}
    Create And Update Composition On Specific Tenant    multitenancy_token=${encoded_token_1}
    ## Get Composition created and updated in tentant1 from tenant2 (negative)
    get composition by composition_uid    ${versioned_object_uid_v2}    multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     ${getCompositionNegativeCode}
    Should Be Equal As Strings      ${response.json()['message']}   ${noCompositionFoundMsg}
    Upload OPT    nested/nested.opt     multitenancy_token=${encoded_token_2}
    Create And Update Composition On Specific Tenant    multitenancy_token=${encoded_token_2}
    ## Get Composition created and updated in tentant2 from tenant1 (negative)
    get composition by composition_uid    ${versioned_object_uid_v2}    multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     ${getCompositionNegativeCode}
    Should Be Equal As Strings      ${response.json()['message']}   ${noCompositionFoundMsg}

3. Create And Delete Compositions In Tentants And Check Isolation Of Data Between Tenants
    [Documentation]     Covers create and delete Composition + Isolation of data between tenants.
    [Tags]      Positive    Negative
    Upload OPT    nested/nested.opt     multitenancy_token=${encoded_token_1}
    Create And Delete Composition On Specific Tenant    multitenancy_token=${encoded_token_1}
    #Get composition from tenant2, deleted in tenant1 (negative)
    get composition by composition_uid    ${preceding_version_uid}    multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     ${getCompositionNegativeCode}
    Upload OPT    nested/nested.opt     multitenancy_token=${encoded_token_2}
    Create And Delete Composition On Specific Tenant    multitenancy_token=${encoded_token_2}
    #Get composition from tenant1, deleted in tenant2 (negative)
    get composition by composition_uid    ${preceding_version_uid}    multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     ${getCompositionNegativeCode}


*** Keywords ***
Get Composition Values And Set Them In Variables
    Set Test Variable   ${composition_uid}    ${response.json()['uid']['value']}
    Set Test Variable   ${version_uid}    ${response.json()['uid']['value']}    # full/long compo uid
    Set Test Variable   ${version_uid_v1}    ${version_uid}                  # different names for full uid
    Set Test Variable   ${preceding_version_uid}    ${version_uid}

    ${short_uid}        Remove String       ${version_uid}    ::${CREATING_SYSTEM_ID}::1
                        Set Test Variable   ${compo_uid_v1}    ${short_uid}
                        Set Test Variable   ${versioned_object_uid}    ${short_uid}

Create And Get Composition On Specific Tenant
    [Arguments]     ${multitenancy_token}
    ## For positive flow Create + Get
    Create New EHR With Multitenant Token       ${multitenancy_token}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable    ${ehr_id}        ${response.json()['ehr_id']['value']}
    commit composition  format=CANONICAL_JSON
    ...                 composition=nested.en.v1__full_without_links.json
    ...                 multitenancy_token=${multitenancy_token}
    Should Be Equal As Strings      ${response.status_code}     201
    Get Composition Values And Set Them In Variables
    get composition by composition_uid    ${version_uid}    multitenancy_token=${multitenancy_token}
    Should Be Equal As Strings   ${response.status_code}   ${getCompositionPositiveCode}

Create And Update Composition On Specific Tenant
    [Arguments]     ${multitenancy_token}
    ## For positive flow Create + Update
    Create New EHR With Multitenant Token       ${multitenancy_token}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable    ${ehr_id}        ${response.json()['ehr_id']['value']}
    commit composition  format=CANONICAL_JSON
    ...                 composition=nested.en.v1__full_without_links.json
    ...                 multitenancy_token=${multitenancy_token}
    Should Be Equal As Strings      ${response.status_code}     201
    Get Composition Values And Set Them In Variables
    update composition (JSON)
    ...     nested.en.v2__full_without_links_updated.json   file_type=json  multitenancy_token=${multitenancy_token}
    Should Be Equal As Strings      ${response.status_code}     200
    get composition by composition_uid    ${versioned_object_uid_v2}    multitenancy_token=${multitenancy_token}
    Should Be Equal As Strings      ${response.status_code}     200

Create And Delete Composition On Specific Tenant
    [Arguments]     ${multitenancy_token}
    ## For positive flow Create + Delete
    Create New EHR With Multitenant Token       ${multitenancy_token}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable    ${ehr_id}        ${response.json()['ehr_id']['value']}
    commit composition  format=CANONICAL_JSON
    ...                 composition=nested.en.v1__full_without_links.json
    ...                 multitenancy_token=${multitenancy_token}
    Should Be Equal As Strings      ${response.status_code}     201
    Get Composition Values And Set Them In Variables
    delete composition    ${preceding_version_uid}
    get composition by composition_uid    ${preceding_version_uid}    multitenancy_token=${multitenancy_token}
    Should Be Equal As Strings      ${response.status_code}     204