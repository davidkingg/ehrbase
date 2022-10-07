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
Documentation    Multitenancy Specific Keywords
Resource   ../suite_settings.robot


*** Variables ***
${encoded_token_1}    eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE2MDA3NzEwODYsImlhdCI6MTYwMDc3MDc4NiwidG50IjoiYTk1ZjAxNDktYzQ5Ni00NzMzLTg3MDMtMzljOTExNTc4MDkwIiwiYXV0aF90aW1lIjoxNjAwNzcwNzg2LCJqdGkiOiI3NThkYzZhNC0wMWQ5LTRkN2MtYjdhMy02YmNiY2ZmNTkzNTciLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODEvYXV0aC9yZWFsbXMvZWhyYmFzZSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI5NzgzNTBkNC04NmFjLTQ2MzEtYTRjNC02Njc2Mjk0YWMzZmQiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJlaHJiYXNlLXBvc3RtYW4iLCJzZXNzaW9uX3N0YXRlIjoiODI0YjhlNGYtMWIzMS00OTNiLWE2OWQtYjc4MzIzMWYwNGM3IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIiwidXNlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IkVIUmJhc2UgVXNlciIsInByZWZlcnJlZF91c2VybmFtZSI6InVzZXIiLCJnaXZlbl9uYW1lIjoiRUhSYmFzZSIsImZhbWlseV9uYW1lIjoiVXNlciIsImVtYWlsIjoidXNlckBlaHJiYXNlLm9yZyJ9.eYLuvX-BlzT0FKeJI8s0MNbeJT6WtPOEH8ruwTPMB8c
${encoded_token_2}    eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJleHAiOjE2MDA3NzEwODYsImlhdCI6MTYwMDc3MDc4NiwidG50IjoiYmM2OTZjOTMtMjRjNy00MzRjLTgyMTQtNjk2YzI1YTI0MzEwIiwiYXV0aF90aW1lIjoxNjAwNzcwNzg2LCJqdGkiOiJmMWY3MGRhYS05YTQyLTQ4MGYtOWI3Ny00NzMxMGM4NTNjNmEiLCJpc3MiOiJodHRwOi8vbG9jYWxob3N0OjgwODEvYXV0aC9yZWFsbXMvZWhyYmFzZSIsImF1ZCI6ImFjY291bnQiLCJzdWIiOiI0ZDY1YWRhZC05NGNhLTQzOTItYTk0OS1jZGEwZjRiNzhjODkiLCJ0eXAiOiJCZWFyZXIiLCJhenAiOiJlaHJiYXNlLXBvc3RtYW4iLCJzZXNzaW9uX3N0YXRlIjoiYmNkMjUyM2QtMmE1Yi00NmYyLWE3YzAtZjVmN2UyMWYzZTc5IiwiYWNyIjoiMSIsInJlYWxtX2FjY2VzcyI6eyJyb2xlcyI6WyJvZmZsaW5lX2FjY2VzcyIsInVtYV9hdXRob3JpemF0aW9uIiwidXNlciJdfSwicmVzb3VyY2VfYWNjZXNzIjp7ImFjY291bnQiOnsicm9sZXMiOlsibWFuYWdlLWFjY291bnQiLCJtYW5hZ2UtYWNjb3VudC1saW5rcyIsInZpZXctcHJvZmlsZSJdfX0sInNjb3BlIjoib3BlbmlkIGVtYWlsIHByb2ZpbGUiLCJlbWFpbF92ZXJpZmllZCI6dHJ1ZSwibmFtZSI6IkVIUmJhc2UgVXNlcjEiLCJwcmVmZXJyZWRfdXNlcm5hbWUiOiJ1c2VyMSIsImdpdmVuX25hbWUiOiJFSFJiYXNlMSIsImZhbWlseV9uYW1lIjoiVXNlckZhbWlseU5hbWUxIiwiZW1haWwiOiJ1c2VyMUBlaHJiYXNlLm9yZyJ9.vSMGcFzge1U_bEukbAjqjwxghhvCHDlT6TLPutT1rPE


*** Keywords ***
Decode JWT And Get TNT Value
    [Documentation]     Decode JWT token provided as argument and returns tnt value.
    ...         Takes 1 argument: in_token;
    ...         \nReturns tntToken value.
    [Arguments]     ${in_token}
    &{decoded_token}    decode token        ${in_token}
                        Log To Console      \ntnt: ${decoded_token.tnt}
                        Set Test Variable   ${tntToken}     ${decoded_token.tnt}
                        Dictionary Should Contain Item    ${decoded_token}    typ   Bearer
    [Return]        ${tntToken}

Create Two Tenants
    [Documentation]     Create two tenants with default provided names.
    ...     Dependency variables: encoded_token_1, encoded_token_2 - to be set as global
    ...     Set tentant TNTs as suite variables.
    [Arguments]     ${tenantName1}=AutomationTentant1     ${tenantName2}=AutomationTentant2
    ${tnt1}     Decode JWT And Get TNT Value    ${encoded_token_1}
    ${tnt2}     Decode JWT And Get TNT Value    ${encoded_token_2}
    Set Suite Variable   ${tenantTnt1}     ${tnt1}
    Set Suite Variable   ${tenantTnt2}     ${tnt2}
    Create Session      multitenancy    ${PLUGINURL}    debug=2     verify=True
    ${headers}      Create Dictionary   Content-Type    application/json
    &{tenant1}     Create Dictionary
    ...     tenantId        ${tnt1}
    ...     tenantName      ${tenantName1}
    &{tenant2}     Create Dictionary
    ...     tenantId        ${tnt2}
    ...     tenantName      ${tenantName2}
    ${resp1}     POST On Session     multitenancy   /multi-tenant/service
    ...     expected_status=anything   json=&{tenant1}   headers=${headers}
    Should Be Equal As Strings      ${resp1.status_code}     200
    ${resp2}     POST On Session     multitenancy   /multi-tenant/service
    ...     expected_status=anything   json=&{tenant2}   headers=${headers}
    Should Be Equal As Strings      ${resp2.status_code}     200

Create Single Tenant
    [Documentation]     Create single tenant with encoded_token name provided as argument.
    ...     \nTakes 2 arguments:
    ...     - encodedToken (mandatory)\n- tenantName (optional with default value)
    ...     - tenantStructure (optional with default value. If default, tnt and tenantName are present)
    ...     \nReturns tnt value.
    [Arguments]     ${encodedToken}    ${tenantName}=MyAutomationCustomTenant   ${tenantStructure}=default
    ${tnt}     Decode JWT And Get TNT Value    ${encodedToken}
    Create Session      multitenancy    ${PLUGINURL}    debug=2     verify=True
    ${headers}      Create Dictionary   Content-Type    application/json
    IF      """${tenantStructure}""" != """default"""
        &{tenant}   Set Variable    ${tenantStructure}
    ELSE
        &{tenant}       Create Dictionary
        ...     tenantId        ${tnt}
        ...     tenantName      ${tenantName}
    END
    ${resp}     POST On Session     multitenancy   /multi-tenant/service
    ...     expected_status=anything   json=&{tenant}   headers=${headers}
    Set Suite Variable      ${response}     ${resp}
    Set Suite Variable      ${response_code}     ${resp.status_code}
    ${isCreateSucceeded}    Run Keyword And Return Status
    ...     Should Be Equal As Strings      ${resp.status_code}     200
    # TODO: Change status code after fix from Michael, and validate the error message
    IF      ${isCreateSucceeded} == ${FALSE}
        Log
        ...     Create tenant with name ${tenantName}, failed due to returned status code = ${resp.status_code}
    END
    [Return]    ${tnt}

Get All Created Tenants
    ${headers}      Create Dictionary   Content-Type    application/json
    ${resp}     GET On Session     multitenancy   /multi-tenant/service
    ...     expected_status=anything   headers=${headers}
    Should Be Equal As Strings      ${resp.status_code}     200
    Set Test Variable       ${response}     ${resp.text}

Create Tenant Error Handler
    IF      '${response_code}' == '409'
        Should Contain      ${response.text}        ${expectedDuplicateErr}
        Log     \n${response.text}    console=yes
        Return From Keyword
    ELSE IF     '${response_code}' == '201'
        Return From Keyword
    ELSE IF     '${response_code}' == '400' or '${response.status_code}' == '500'
        Log     Create Tenant returned ${response_code} code.    console=yes
        Return From Keyword
    ELSE
        Log     Create Tenant returned ${response_code} code.    console=yes
        Return From Keyword
    END