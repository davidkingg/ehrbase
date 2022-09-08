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
Documentation   Multitenancy With Directories Tests
...             *Precondition:* tenants_operations.robot suite is executed.
...             - Tenants are created in tenants_operations.robot.
...             \n*Following operations on Folders were covered:*\n- Create\n- Update\n- Delete\n- Get
...             \n*1. (CREATE+GET Directory) Below cases covers isolation of data between tenants:*
...             \n*Positive:*
...             - 1.1 Create EHR on tenant 1\n- 1.2 Create Directory on tenant 1
...             - 1.3 Get Directory using ehr_id from tenant 1 and expect 200 code
...             - 1.4 Get Directory using ehr_id, version_uid from tenant 1 and expect 200 code
...             - 1.5 Create EHR on tenant 2\n- 1.6 Create Directory on tenant 2
...             - 1.7 Get Directory using ehr_id from tenant 2 and expect 200 code
...             - 1.8 Get Directory using ehr_id, version_uid from tenant 2 and expect 200 code
...             \n*Negative:*\n- Get Directory created in tenant2 from tenant1 (using ehr_id from tenant2)
...             - Get Directory created in tenant2 from tenant2 -> version_uid present in tenant1
...             - Get Directory created in tenant2 from tenant2 (using ehr_id From tenant1)
...             \n*2. (CREATE+UPDATE Directory) Below cases covers isolation of data between tenants:*
...             \n*Positive:*
...             - 2.1 Create EHR on tenant 1\n- 2.2 Create Directory on tenant 1
...             - 2.3 Update Directory on tenant 1 and expect 200 code
...             - 2.4 Create EHR on tenant 2\n- 2.5 Create Directory on tenant 2
...             - 2.6 Update Directory on tenant 2 and expect 200 code
...             \n*Negative:*
...             - Get Directory created and updated in tenant2 from tenant1
...             - Get Directory created and updated in tenant1 from tenant2

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/ehr_keywords.robot
Resource        ../_resources/keywords/multitenancy_keywords.robot
Resource        ../_resources/keywords/directory_keywords.robot


*** Test Cases ***
1. Positive Create And Get Directories In Tentant1 And Tenant2
    [Documentation]     Covers create and get Directory + Isolation of data between tenants.
    [Tags]      Positive
    Create New EHR With Multitenant Token       ${encoded_token_1}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable      ${ehr_id}           ${response.json()['ehr_id']['value']}
    Set Suite Variable      ${ehr_id_tnt1}      ${ehr_id}
    Create Directory With Multitenant Token     empty_directory.json     multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     201
    Get Directory Using Ehr Id Only And Get Version Uid Value From Specific Tenant
    ...     multitenancy_token=${encoded_token_1}
    Set Suite Variable      ${version_uid_tnt1}     ${version_uid}
    GET /ehr/ehr_id/directory/version_uid   format=JSON     multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     200
    ###########
    Create New EHR With Multitenant Token       ${encoded_token_2}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable      ${ehr_id}           ${response.json()['ehr_id']['value']}
    Create Directory With Multitenant Token     empty_directory.json     multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     201
    Get Directory Using Ehr Id Only And Get Version Uid Value From Specific Tenant
    ...     multitenancy_token=${encoded_token_2}
    Set Suite Variable      ${version_uid_tnt2}     ${version_uid}
    GET /ehr/ehr_id/directory/version_uid   format=JSON     multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     200

Negative Get Directory Created In Tenant2 From Tenant1 Using Ehr Id From Tenant2
    [Documentation]     *DEPENDENT OF* 1. Create And Get Directories In Tentant1 And Tenant2
    [Tags]   Negative
    GET /ehr/ehr_id/directory/version_uid   format=JSON     multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     404
    Should Be Equal As Strings      ${response.json()["message"]}     EHR with id ${ehr_id} not found

Negative Get Directory Created In Tenant2 From Tenant2 -> Version Uid Present In Tenant1
    [Documentation]     *DEPENDENT OF* 1. Create And Get Directories In Tentant1 And Tenant2
    [Tags]   Negative
    Set Suite Variable      ${version_uid}      ${version_uid_tnt1}
    GET /ehr/ehr_id/directory/version_uid   format=JSON     multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     404
    Should Be Equal As Strings      ${response.json()["message"]}     Folder with id ${version_uid} could not be found

Negative Get Directory Created In Tenant2 From Tenant2 But With Ehr From Tenant1
    [Documentation]     *DEPENDENT OF* 1. Create And Get Directories In Tentant1 And Tenant2
    [Tags]   Negative
    Set Suite Variable      ${ehr_id}       ${ehr_id_tnt1}
    GET /ehr/ehr_id/directory   format=JSON     multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     404
    Should Be Equal As Strings      ${response.json()["message"]}     EHR with id ${ehr_id} not found

2. Positive Create And Update Directories In Tentant1 And Tenant2
    [Documentation]     Covers create and update Directory + Isolation of data between tenants.
    [Tags]      Positive
    Create And Update Directory On Specific Tenant      ${encoded_token_1}
    Set Suite Variable      ${ehr_id_tnt1}      ${ehr_id}
    Set Suite Variable      ${version_uid_tnt1}      ${version_uid}
    ########
    Create And Update Directory On Specific Tenant      ${encoded_token_2}
    Set Suite Variable      ${ehr_id_tnt2}      ${ehr_id}
    Set Suite Variable      ${version_uid_tnt2}     ${version_uid}

Negative Get Directory Created And Updated In Tenant2 From Tenant1
    [Documentation]     *DEPENDENT OF* 2. Positive Create And Update Directories In Tentant1 And Tenant2
    [Tags]      Negative
    Set Suite Variable      ${ehr_id}           ${ehr_id_tnt2}
    GET /ehr/ehr_id/directory   format=JSON     multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     404
    Should Be Equal As Strings      ${response.json()["message"]}     EHR with id ${ehr_id} not found

Negative Get Directory Created And Updated In Tenant1 From Tenant2
    [Documentation]     *DEPENDENT OF* 2. Positive Create And Update Directories In Tentant1 And Tenant2
    [Tags]      Negative
    Set Suite Variable      ${ehr_id}           ${ehr_id_tnt1}
    GET /ehr/ehr_id/directory   format=JSON     multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     404
    Should Be Equal As Strings      ${response.json()["message"]}     EHR with id ${ehr_id} not found


*** Keywords ***
Get Directory Using Ehr Id Only And Get Version Uid Value From Specific Tenant
    [Arguments]     ${multitenancy_token}
    GET /ehr/ehr_id/directory   format=JSON     multitenancy_token=${multitenancy_token}
    Should Be Equal As Strings      ${response.status_code}     200
    ${version_uid_from_get_resp}    Set Variable    ${response.json()["uid"]["value"]}
    ${temp_version_uid}     Remove String   ${version_uid_from_get_resp}    ::${CREATING_SYSTEM_ID}::1
    Set Suite Variable      ${version_uid}  ${temp_version_uid}

Create And Update Directory On Specific Tenant
    [Arguments]     ${multitenancy_token}
    ${fileToCreateDirectory}    Set Variable    update/1_create_empty_directory.json
    ${fileToUpdateDirectory}    Set Variable    update/2_add_subfolders.json
    Create New EHR With Multitenant Token       ${multitenancy_token}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable      ${ehr_id}           ${response.json()['ehr_id']['value']}
    Create Directory With Multitenant Token
    ...     ${fileToCreateDirectory}
    ...     multitenancy_token=${multitenancy_token}
    Should Be Equal As Strings      ${response.status_code}     201
    Set Suite Variable      ${folder_uid}       ${response.json()['uid']['value']}
    Set Suite Variable      ${version_uid}      ${response.json()['uid']['value']}
    Set Suite Variable      ${preceding_version_uid}        ${version_uid}
    load valid dir test-data-set    ${fileToUpdateDirectory}
    PUT /ehr/ehr_id/directory   JSON    ${multitenancy_token}
    Should Be Equal As Strings      ${response.status_code}     200