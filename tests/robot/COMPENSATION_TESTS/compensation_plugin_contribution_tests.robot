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
Documentation   Compensation Plugin Contribution Test Suite
Metadata        TOP_TEST_SUITE    COMPENSATION_TESTS

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/contribution_keywords.robot


*** Test Cases ***
Perform Rollback On Committed CONTRIBUTION With Change Type And Operation Creation
    [Documentation]     Create OPT, \n Create EHR,
    ...                 \n Commit Contribution with Change Type in it, with operation creation and expect 201 status code,
    ...                 \n Perform Rollback on committed contribution,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 501, with JSON body containing uuid, type and "status":" Not Implemented"
    Upload OPT    minimal/minimal_evaluation.opt
    create EHR
    commit CONTRIBUTION (JSON)  minimal/minimal_evaluation.contribution.json
    check response: is positive - returns version id
    retrieve CONTRIBUTION by contribution_uid (JSON)
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${501}
    Log     ${contribution_uid}
    Should Contain  ${body}    Not Implemented
    retrieve CONTRIBUTION by contribution_uid (JSON)

Perform Rollback On Committed CONTRIBUTION With Change Type And Operation Modification
    [Documentation]     Create OPT, \n Create EHR,
    ...                 \n Commit Contribution with Change Type in it, with operation modification and expect 201 status code,
    ...                 \n Perform Rollback on committed contribution,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 501, with JSON body containing uuid, type and "status":" Not Implemented"
    Upload OPT    minimal/minimal_admin.opt
    create EHR
    commit CONTRIBUTION (JSON)    minimal/minimal_admin.contribution.json
    check response: is positive - returns version id
    commit CONTRIBUTION - with preceding_version_uid (JSON)    minimal/minimal_admin.contribution.modification.complete.json
    check response: is positive - contribution has new version
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${501}
    Log     ${contribution_uid}
    Should Contain  ${body}    Not Implemented
    retrieve CONTRIBUTION by contribution_uid (JSON)

Perform Rollback On Non Existing Contribution Using Id
    [Documentation]     Create EHR, \n Commit Contribution
    ...                 \n Set hardcoded, not existing contribution_uid,
    ...                 \n Perform Rollback on not existing contribution, using not existing contribution_uid,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback,
    ...                 \n Expect status code 400, with JSON body empty.
    create EHR
    commit CONTRIBUTION (JSON)  minimal/minimal_evaluation.contribution.json
    check response: is positive - returns version id
    ${fake_contribution_uid}    Set Variable    c874e8d9-8cc2-4ce3-981f-111111112dd4
    Set Test Variable      ${contribution_uid}    ${fake_contribution_uid}
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${400}
    should be equal as strings      ${body}             ${EMPTY}