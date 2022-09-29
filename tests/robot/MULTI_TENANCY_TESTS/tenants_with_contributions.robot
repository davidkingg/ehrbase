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
Documentation   Multitenancy With Contribution Tests
...             - Precondition: tenants_operations.robot suite is executed.
...             - Tenants are created in tenants_operations.robot.
...             \n*Following operations on Contributions were covered:*\n- Create\n- Get
Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/contribution_keywords.robot
Resource        ../_resources/keywords/ehr_keywords.robot
Resource        ../_resources/keywords/multitenancy_keywords.robot


*** Test Cases ***
Create And Get Contributions In Tentants And Check Isolation Of Data Between Tenants
    [Documentation]     Covers create and get Contributions + Isolation of data between tenants.
    ...         \n*Case 1:*
    ...         - Create EHR on tenant 1\n- Create Contribution on tenant 1
    ...         - Get Contribution from tenant 1 and expect 200 code
    ...         - Get Contribution from tenant 2 and expect 404 code
    ...         \n*Case 2:*
    ...         - Create EHR on tenant 2\n- Create Contribution on tenant 2
    ...         - Get Contribution from tenant 2 and expect 200 code
    ...         - Get Contribution from tenant 1 and expect 404 code
    [Tags]      Positive    Negative
    Upload OPT      minimal/minimal_evaluation.opt     multitenancy_token=${encoded_token_1}
    Create New EHR With Multitenant Token   ${encoded_token_1}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable      ${ehr_id}       ${response.json()['ehr_id']['value']}
    Set Suite Variable      ${ehr_id_tnt1}  ${ehr_id}
    Create Contribution With Multitenant Token
    ...     valid_test_data_set=minimal/minimal_evaluation.contribution.json
    ...     multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     201
    Retrieve Contribution With Multitenant Token    multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     200
    Retrieve Contribution With Multitenant Token    multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     404
    #########
    Upload OPT      minimal/minimal_evaluation.opt     multitenancy_token=${encoded_token_2}
    Create New EHR With Multitenant Token   ${encoded_token_2}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable      ${ehr_id}       ${response.json()['ehr_id']['value']}
    Create Contribution With Multitenant Token
    ...     valid_test_data_set=minimal/minimal_evaluation.contribution.json
    ...     multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     201
    Retrieve Contribution With Multitenant Token    multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     200
    Set Suite Variable      ${ehr_id}       ${ehr_id_tnt1}
    Retrieve Contribution With Multitenant Token    multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     404
    Should Be Equal As Strings      ${response.json()["message"]}    Contribution with given ID does not exist
