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
Documentation   Multitenancy Tests

Resource        ../_resources/keywords/composition_keywords.robot
Resource        ../_resources/keywords/multitenancy_keywords.robot

*** Variables ***
${expectedDuplicateErr}      duplicate key value violates unique constraint "tenant_tenant_id_key"

*** Test Cases ***
Create Non Default Tenants And Get All Tenants
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

Create Non Default Tenant Without Tenant Name Value
    [Documentation]     Create Tenant without Tenant Name value.
    ...     \nExpect *400* status code.
    ...     \nError message: *tenantName is required*
    [Tags]      Negative
    ${invalidTenantCase}     Create Single Tenant
    ...     encodedToken=${encoded_token_2}
    ...     tenantName=${EMPTY}    tenantStructure=default
    Create Tenant Error Handler
    Should Be Equal As Strings    ${response.status_code}   400
    Should Contain     ${response.json()["tenantName"]}    tenantName is required

Create Non Default Tenant Without Tenant Name Key
    [Documentation]     Create Tenant with missing tenantName key in json request.
    ...     \nExpect *400* status code.
    ...     \nError message: *tenantName is required*
    [Tags]      Negative
    ${tnt}      Decode JWT And Get TNT Value    ${encoded_token_2}
    &{invalidTntStructure}      Create Dictionary
    ...     tenantId        ${tnt}
    ${invalidTenantCase}        Create Single Tenant
    ...     encodedToken=${encoded_token_2}
    ...     tenantName=${EMPTY}    tenantStructure=${invalidTntStructure}
    Create Tenant Error Handler
    Should Be Equal As Strings    ${response.status_code}   400
    Should Contain     ${response.json()["tenantName"]}    tenantName is required

Create Non Default Tenant Without Tenant Name Key And Tenant Id Key
    [Documentation]     Create Tenant with missing tenantName key and tenantId key in json request.
    ...     \nExpect *400* status code.
    ...     \nError message contains: *tenantName is required* and *tenantId is required*
    [Tags]      Negative
    ${tnt}      Decode JWT And Get TNT Value    ${encoded_token_2}
    ${invalidTntStructure}      Create Dictionary
    ${invalidTenantCase}        Create Single Tenant
    ...     encodedToken=${encoded_token_2}
    ...     tenantName=${EMPTY}    tenantStructure=${invalidTntStructure}
    Create Tenant Error Handler
    Should Be Equal As Strings    ${response.status_code}   400
    Should Contain     ${response.json()["tenantName"]}     tenantName is required
    Should Contain     ${response.json()["tenantId"]}       tenantId is required

Create Non Default Tenant Duplicate Tenant
    [Documentation]     Create Tenant with the same tenantId and tenantName (Duplicate tenant).
    ...     \*Precondition:* Tenant is created with the same tenantId value.
    ...     \nExpect *409* status code.
    ...     \nError message: *duplicate key value violates unique constraint...*
    [Tags]      Negative
    ${tnt2}     Create Single Tenant
    ...     encodedToken=${encoded_token_2}     tenantName=MyTenant2
    Create Tenant Error Handler
    Should Be Equal As Strings     ${response.status_code}      409
    Should Contain      ${response.text}        ${expectedDuplicateErr}


*** Keywords ***
Create Tenant Error Handler
    IF      '${response.status_code}' == '409'
        Should Contain      ${response.text}        ${expectedDuplicateErr}
        Log     \n${response.text}    console=yes
        Return From Keyword
    ELSE IF     '${response.status_code}' == '201'
        Return From Keyword
    ELSE IF     '${response.status_code}' == '400' or '${response.status_code}' == '500'
        Log     Create Tenant returned ${response.status_code} code.    console=yes
        Return From Keyword
    ELSE
        Log     Create Tenant returned ${response.status_code} code.    console=yes
        Return From Keyword
    END