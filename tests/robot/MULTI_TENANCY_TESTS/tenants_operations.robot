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


*** Test Cases ***
Create Non Default Tenants And Get All Tenants
    [Documentation]     Create 2 tenants and expect them to appear at Get All tenants.
    ...         \nCreate duplicate tenants (with the same tenantId) - must be rejected.
    [Tags]      Positive
    ${tnt1}     Create Single Tenant
    ...     encodedToken=${encoded_token_1}      tenantName=MyTenant1
    ${tnt2}     Create Single Tenant
    ...     encodedToken=${encoded_token_2}      tenantName=MyTenant2
    Get All Created Tenants
    Should Contain      ${response}     ${tnt1}
    Should Contain      ${response}     ${tnt2}
