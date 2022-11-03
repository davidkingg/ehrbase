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
Resource        ../_resources/keywords/db_keywords.robot


*** Test Cases ***
Perform Rollback On Committed CONTRIBUTION Composition With Change Type And Operation Creation
    [Documentation]     Create OPT, \n Create EHR,
    ...                 \n Commit Contribution Composition with Change Type in it, with operation creation and expect 201 status code,
    ...                 \n Perform Rollback on committed contribution,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 200, with non empty body.
    [Tags]      Positive
    Upload OPT    minimal/minimal_evaluation.opt
    create EHR
    commit CONTRIBUTION (JSON)  minimal/minimal_evaluation.contribution.json
    check response: is positive - returns version id
    retrieve CONTRIBUTION by contribution_uid (JSON)
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${200}
    Log     ${contribution_uid}
    Should Contain     ${body}      "status":"Ok","type":"COMPOSITION","uuid":"
    retrieve CONTRIBUTION by contribution_uid (JSON)

Perform Rollback On Committed CONTRIBUTION Composition With Change Type And Operation Modification
    [Documentation]     Create OPT, \n Create EHR,
    ...                 \n Commit Contribution Composition with Change Type in it, with operation modification and expect 201 status code,
    ...                 \n Perform Rollback on committed contribution,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 200, with non empty body.
    [Tags]      Positive
    Upload OPT    minimal/minimal_admin.opt
    create EHR
    commit CONTRIBUTION (JSON)    minimal/minimal_admin.contribution.json
    check response: is positive - returns version id
    commit CONTRIBUTION - with preceding_version_uid (JSON)    minimal/minimal_admin.contribution.modification.complete.json
    check response: is positive - contribution has new version
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${200}
    Log     ${contribution_uid}
    Should Contain     ${body}      "status":"Ok","type":"COMPOSITION","uuid":"
    retrieve CONTRIBUTION by contribution_uid (JSON)

Perform Rollback On Committed CONTRIBUTION Composition With Change Type And Operation Amendment Complete
    [Documentation]     Create OPT, \n Create EHR,
    ...                 \n Commit Contribution Composition with Change Type in it, with operation amendment complete and expect 201 status code,
    ...                 \n Perform Rollback on committed contribution,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 200, with non empty body.
    [Tags]      Positive
    Upload OPT    minimal/minimal_admin.opt
    create EHR
    commit CONTRIBUTION (JSON)    minimal/minimal_admin.contribution.json
    check response: is positive - returns version id
    commit CONTRIBUTION - with preceding_version_uid (JSON)    minimal/minimal_admin.contribution.amendment.complete.json
    check response: is positive - contribution has new version
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${200}
    Log     ${contribution_uid}
    Should Contain     ${body}      "status":"Ok","type":"COMPOSITION","uuid":"
    retrieve CONTRIBUTION by contribution_uid (JSON)

Perform Rollback On Committed CONTRIBUTION Composition With Change Type And Operation Amendment Incomplete
    [Documentation]     Create OPT, \n Create EHR,
    ...                 \n Commit Contribution Composition with Change Type in it, with operation amendment incomplete and expect 201 status code,
    ...                 \n Perform Rollback on committed contribution,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 200, with non empty body.
    [Tags]      Positive
    Upload OPT    minimal/minimal_admin.opt
    create EHR
    commit CONTRIBUTION (JSON)    minimal/minimal_admin.contribution.json
    check response: is positive - returns version id
    commit CONTRIBUTION - with preceding_version_uid (JSON)    minimal/minimal_admin.contribution.amendment.incomplete.json
    check response: is positive - contribution has new version
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${200}
    Log     ${contribution_uid}
    Should Contain     ${body}      "status":"Ok","type":"COMPOSITION","uuid":"
    retrieve CONTRIBUTION by contribution_uid (JSON)

Perform Second Rollback On Committed CONTRIBUTION Composition With Change Type And Operation Modification
    [Documentation]     Create OPT, \n Create EHR,
    ...                 \n Commit Contribution Composition with Change Type in it, with operation modification and expect 201 status code,
    ...                 \n Perform Rollback on committed contribution,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 200, with non empty body from first Rollback
    ...                 \n Perform second Rollback on contribution and expect status code 409.
    ...                 \n 409 is returned, as a newer version of the composition already exists.
    [Tags]      Negative
    Upload OPT    minimal/minimal_admin.opt
    create EHR
    commit CONTRIBUTION (JSON)    minimal/minimal_admin.contribution.json
    check response: is positive - returns version id
    commit CONTRIBUTION - with preceding_version_uid (JSON)    minimal/minimal_admin.contribution.modification.complete.json
    check response: is positive - contribution has new version
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${200}
    Log     ${contribution_uid}
    Should Contain     ${body}      "status":"Ok","type":"COMPOSITION","uuid":"
    retrieve CONTRIBUTION by contribution_uid (JSON)
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${409}
    Should Contain      ${body}     Stale Version for Composition

Perform Rollback On Committed CONTRIBUTION EHRStatus With Operation Modification
    [Documentation]     Create EHR,
    ...                 \n Commit Contribution EHR_Status with operation creation and expect 201 status code,
    ...                 \n Perform Rollback on committed contribution,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 200, non empty body.
    [Tags]      Positive
    create EHR
    Set Test Variable  ${version_id}  ${ehrstatus_uid}
    commit CONTRIBUTION - with preceding_version_uid (JSON)    minimal/status.contribution.modification.json
    check response: is positive - contribution has new version
    retrieve CONTRIBUTION by contribution_uid (JSON)
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${200}
    Should Contain     ${body}      "status":"Ok","type":"EHR_STATUS","uuid":"

Perform Rollback On Committed CONTRIBUTION Folder With Operation Creation
    [Documentation]     Create EHR,
    ...                 \n Commit Contribution Folder with operation creation and expect 201 status code,
    ...                 \n Perform Rollback on committed contribution,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 200, non empty body.
    [Tags]      Positive
    create EHR
    commit CONTRIBUTION (JSON)    minimal/folder.contribution.creation.json
    check response: is positive - returns version id
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${200}
    Log     ${contribution_uid}
    Should Contain     ${body}      "status":"Ok","type":"FOLDER","uuid":"

Perform Rollback On Committed CONTRIBUTION Folder With Operation Modification
    [Documentation]     Create EHR,
    ...                 \n Commit Contribution Folder with operation creation and expect 201 status code,
    ...                 \n Commit Contribution Folder with preceding_version_uid with operation modification and expect 201 status code,
    ...                 \n Perform Rollback on second version of committed contribution,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 200, with non empty body.
    [Tags]      Positive
    create EHR
    commit CONTRIBUTION (JSON)    minimal/folder.contribution.creation.json
    check response: is positive - returns version id
    commit CONTRIBUTION - with preceding_version_uid (JSON)    minimal/folder.contribution.modification.json
    check response: is positive - contribution has new version
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${200}
    Log     ${contribution_uid}
    Should Contain     ${body}      "status":"Ok","type":"FOLDER","uuid":"

Perform Rollback On Non Existing Contribution Using Id
    [Documentation]     Create EHR, \n Commit Contribution Composition
    ...                 \n Set hardcoded, not existing contribution_uid,
    ...                 \n Perform Rollback on not existing contribution, using not existing contribution_uid,
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback,
    ...                 \n Expect status code 500, with non body empty.
    [Tags]      Negative
    create EHR
    commit CONTRIBUTION (JSON)  minimal/minimal_evaluation.contribution.json
    check response: is positive - returns version id
    ${fake_contribution_uid}    Set Variable    c874e8d9-8cc2-4ce3-981f-111111112dd4
    Set Test Variable      ${contribution_uid}    ${fake_contribution_uid}
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${500}
    should be equal as strings      ${body}             Contribution with given ID does not exist

Perform Rollback On Committed CONTRIBUTION Composition - Contribution ID Took using SQL - Expect 501
    [Documentation]     Create OPT, \n Create EHR,
    ...                 \n Commit Contribution Composition with Change Type in it, with operation creation and expect 201 status code,
    ...                 \n Perform Rollback on committed contribution (contribution_id took using query)
    ...                 \n *ENDPOINT*: plugin/transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    ...                 \n Expect status code 501, with non empty body.
    ...                 \n *Might fail if runned using Jenkins pipeline, due to DB operations*
    [Tags]      Negative    database    not-ready
    Upload OPT    minimal/minimal_evaluation.opt
    create EHR
    commit CONTRIBUTION (JSON)  minimal/minimal_evaluation.contribution.json
    check response: is positive - returns version id
    retrieve CONTRIBUTION by contribution_uid (JSON)
    Connect With DB
    ${query}    Catenate
    ...     select id from ehr.contribution where ehr_id in
    ...     (select id from ehr.ehr where id = '${ehr_id}')
    ...     and contribution_type='ehr'
    @{output}       Query   ${query}
                    Log     ${output}[0][0]  console=yes
    Set Test Variable        ${contribution_uid}    ${output}[0][0]
    POST transaction-management/ehr/ehr_id/contribution/contribution_id/rollback
    should be equal as strings      ${response_code}    ${501}
    should not be equal as strings      ${body}    []
    [Teardown]      Disconnect From Database