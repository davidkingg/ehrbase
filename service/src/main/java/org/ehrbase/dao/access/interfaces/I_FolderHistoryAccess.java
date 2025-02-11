/*
 * Copyright (c) 2019 vitasystems GmbH and Hannover Medical School.
 *
 * This file is part of project EHRbase
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.ehrbase.dao.access.interfaces;

import java.util.UUID;
import org.ehrbase.dao.access.jooq.FolderHistoryAccess;

public interface I_FolderHistoryAccess extends I_Compensatable {
    /**
     * Deletes a folder history entry and it's directly associated entries in:
     * <ul>
     * <li>folder items history</li>
     * <li>folder hierarchy</li>
     * <li>object reference</li> </ul No sub-folder deletion is done.
     *
     * @param domainAccess
     * @param folderId
     * @param timestamp
     * @return indicates if the deletion was successful
     */
    static boolean deleteFlatBy(I_DomainAccess domainAccess, UUID folderId, UUID contributionId) {
        return FolderHistoryAccess.deleteFlatBy(domainAccess, folderId, contributionId);
    }
}
