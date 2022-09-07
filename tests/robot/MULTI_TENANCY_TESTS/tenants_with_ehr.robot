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
Documentation   Multitenancy With EHRs Tests
...             \n*Precondition:* tenants_operations.robot suite is executed. Tenants are created there.

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/ehr_keywords.robot
Resource        ../_resources/keywords/multitenancy_keywords.robot


*** Variables ***
${noEHRWithIdMsg}   No EHR with this ID can be found
${positiveCode}     200
${negativeCode}     404


*** Test Cases ***
Create EHRs In Tentants And Check Isolation Of Data In Tenants - Positive
    [Documentation]     (Positive cases) Covers creation of:
    ...     - 2 EHRs in Tentant1, Get them and check that 200 is returned
    ...     - 1 EHR in Tentant2, Get it and check that 200 is returned.
    [Tags]      Positive
    [Setup]     Upload OPT    nested/nested.opt
    ${tnt1}     Decode JWT And Get TNT Value    ${encoded_token_1}
    ${tnt2}     Decode JWT And Get TNT Value    ${encoded_token_2}
    Set Suite Variable   ${tenantTnt1}     ${tnt1}
    Set Suite Variable   ${tenantTnt2}     ${tnt2}
    ## Create 2 EHRs in Tentant1 and Get them from Tentant1
    Create New EHR With Multitenant Token       ${encoded_token_1}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=${positiveCode}
    Set Suite Variable    ${ehr_id1_tnt1}        ${response.json()['ehr_id']['value']}
    Create New EHR With Multitenant Token       ${encoded_token_1}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=${positiveCode}
    Set Suite Variable    ${ehr_id2_tnt1}        ${response.json()['ehr_id']['value']}
    ## Create 1 EHR in Tentant2 and Get it from Tentant2
    Create New EHR With Multitenant Token       ${encoded_token_2}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=${positiveCode}
    Set Suite Variable    ${ehr_id1_tnt2}        ${response.json()['ehr_id']['value']}

Create EHRs In Tentants And Check Isolation Of Data In Tenants - Negative
    [Documentation]     (Negative cases) Covers:
    ...     - Get EHR created in tentant2 being under tentant1
    ...     - Get EHR created in tentant1 being under tentant2
    [Tags]      Negative
    ## Get ehr created in tentant2 from tentant1
    Set Test Variable      ${ehr_id}     ${ehr_id1_tnt2}
    Create Session For EHR With Headers For Multitenancy With Bearer Token     ${encoded_token_1}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=${negativeCode}
    Should Be Equal As Strings      ${response.json()['message']}   ${noEHRWithIdMsg}
    ## Get ehr created in tentant1 from tentant2
    Set Test Variable      ${ehr_id}     ${ehr_id1_tnt1}
    Create Session For EHR With Headers For Multitenancy With Bearer Token     ${encoded_token_2}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=${negativeCode}
    Should Be Equal As Strings      ${response.json()['message']}   ${noEHRWithIdMsg}