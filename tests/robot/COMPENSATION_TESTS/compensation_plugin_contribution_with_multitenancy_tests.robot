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
Documentation   Compensation Plugin Contribution With Multitenancy Test Suite
Metadata        TOP_TEST_SUITE    COMPENSATION_TESTS

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/contribution_keywords.robot
Resource        ../_resources/keywords/multitenancy_keywords.robot


*** Variables ***
${expectedDuplicateErr}      duplicate key value violates unique constraint "tenant_tenant_id_key"


*** Test Cases ***
Create Non Default Tenants
    [Documentation]     Create 2 tenants and expect them to appear at Get All tenants.
    ...         \nCreate duplicate tenants (with the same tenantId) - must be rejected.
    [Tags]      Positive
    ${tnt1}     Create Single Tenant
    ...     encodedToken=${encoded_token_1}      tenantName=MyTenant1
    Create Tenant Error Handler
    ${tnt2}     Create Single Tenant
    ...     encodedToken=${encoded_token_2}      tenantName=MyTenant2
    Create Tenant Error Handler
    Get All Created Tenants
    Should Contain      ${response}     ${tnt1}
    Should Contain      ${response}     ${tnt2}

Create Contribution Under Tenant 1 And Rollback It From Tenant 1
    [Documentation]     Upload OPT in tenant 1, \n Create EHR in tenant 1,
    ...             \n Create Contribution in tenant 1,
    ...             \n Retrieve Contribution from tenant 1 and expect 200 status code,
    ...             \n Perform Rollback on committed contribution in tenant 1,
    ...             \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...             \n Expect status code 501, with JSON body containing uuid, type and "status":" Not Implemented"
    Upload OPT      minimal/minimal_evaluation.opt     multitenancy_token=${encoded_token_1}
    Create New EHR With Multitenant Token   ${encoded_token_1}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable      ${ehr_id}       ${response.json()['ehr_id']['value']}
    Set Suite Variable      ${ehr_id_tnt1}  ${ehr_id}
    Create Contribution With Multitenant Token
    ...     minimal/minimal_evaluation.contribution.json
    ...     multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     ${201}
    Retrieve Contribution With Multitenant Token    multitenancy_token=${encoded_token_1}
    Should Be Equal As Strings      ${response.status_code}     ${200}
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...     multitenancy_token=${encoded_token_1}
    should be equal as strings      ${response_code}    ${501}
    Should Contain  ${body}    Not Implemented
    Set Suite Variable      ${contribution_uid}

Rollback Contribution Created In Tenant 1 From Tenant 2
    [Documentation]     *Dependent of:* Create Contribution Under Tenant 1 And Rollback It From Tenant 1
    ...             \n Perform Rollback on committed contribution in tenant 1 from tenant 2,
    ...             \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...             \n Expect status code 400, with empty body
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...     multitenancy_token=${encoded_token_2}
    should be equal as strings      ${response_code}    ${400}
    should be equal as strings      ${body}             ${EMPTY}

Create Contribution Under Tenant 2 And Rollback It From Tenant 2
    [Documentation]     Upload OPT in tenant 2, \n Create EHR in tenant 2,
    ...             \n Create Contribution in tenant 2,
    ...             \n Retrieve Contribution from tenant 2 and expect 200 status code,
    ...             \n Perform Rollback on committed contribution in tenant 2,
    ...             \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...             \n Expect status code 501, with JSON body containing uuid, type and "status":" Not Implemented"
    Upload OPT      minimal/minimal_evaluation.opt     multitenancy_token=${encoded_token_2}
    Create New EHR With Multitenant Token   ${encoded_token_2}
    Retrieve EHR By Ehr_id With Multitenant Token   expected_code=200
    Set Suite Variable      ${ehr_id}       ${response.json()['ehr_id']['value']}
    Set Suite Variable      ${ehr_id_tnt2}  ${ehr_id}
    Create Contribution With Multitenant Token
    ...     minimal/minimal_evaluation.contribution.json
    ...     multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     ${201}
    Retrieve Contribution With Multitenant Token    multitenancy_token=${encoded_token_2}
    Should Be Equal As Strings      ${response.status_code}     ${200}
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...     multitenancy_token=${encoded_token_2}
    should be equal as strings      ${response_code}    ${501}
    Should Contain  ${body}    Not Implemented
    Set Suite Variable      ${contribution_uid}

Rollback Contribution Created In Tenant 2 From Tenant 1
    [Documentation]     *Dependent of:* Create Contribution Under Tenant 2 And Rollback It From Tenant 2
    ...             \n Perform Rollback on committed contribution in tenant 2 from tenant 1,
    ...             \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...             \n Expect status code 400, with empty body
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...     multitenancy_token=${encoded_token_1}
    should be equal as strings      ${response_code}    ${400}
    should be equal as strings      ${body}             ${EMPTY}