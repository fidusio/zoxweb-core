/*
 * Copyright (c) 2012-2026 XlogistX.IO Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */
package org.zoxweb.shared.api;

import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.TimeStampInterface;

import java.util.List;

@SuppressWarnings("serial")
public class APISearchResult<T>
        implements TimeStampInterface {
    private NVConfigEntity nvce;
    private String reportID;
    private long creationTS;
    private long lastUpdatedTS;
    private long lastReadTS;
    private List<T> matchIDs;

    /**
     * The default constructor.
     */
    public APISearchResult() {

    }

    /**
     * Returns the NVConfigEntity.
     *
     * @return NVConfigEntity
     */
    public NVConfigEntity getNVConfigEntity() {
        return nvce;
    }

    /**
     * Sets the NVConfigEntity.
     *
     * @param nvce
     */
    public void setNVConfigEntity(NVConfigEntity nvce) {
        this.nvce = nvce;
    }

    /**
     * @return the report ID.
     */
    public String getReportID() {
        return reportID;
    }

    /**
     * Sets the report ID.
     *
     * @param reportID
     */
    public void setReportID(String reportID) {
        this.reportID = reportID;
    }

    /**
     * Returns list of match ID's.
     *
     * @return the matching ids.
     */
    public List<T> getMatchIDs() {
        return matchIDs;
    }

    /**
     * Sets list of match ID's.
     *
     * @param ids
     */
    public void setMatchIDs(List<T> ids) {
        this.matchIDs = ids;
    }

    /**
     * Returns the size of the match ID's list.
     *
     * @return size
     */
    public int size() {
        if (matchIDs != null) {
            return matchIDs.size();
        }

        return 0;
    }

    @Override
    public long getCreationTime() {
        return creationTS;
    }

    @Override
    public void setCreationTime(long ts) {
        creationTS = ts;
    }

    @Override
    public long getLastTimeUpdated() {
        return lastUpdatedTS;
    }


    @Override
    public void setLastTimeUpdated(long ts) {
        lastUpdatedTS = ts;
    }

    @Override
    public long getLastTimeRead() {
        return lastReadTS;
    }


    @Override
    public void setLastTimeRead(long ts) {
        lastReadTS = ts;
    }
}
