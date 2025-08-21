/*
 * Copyright (c) 2012-2017 ZoxWeb.com LLC.
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

import org.zoxweb.shared.data.SetNameDescriptionDAO;
import org.zoxweb.shared.util.*;
import org.zoxweb.shared.util.NVConfigEntity.ArrayType;

import java.util.List;

@SuppressWarnings("serial")
public class APIBatchResult<V extends NVEntity>
        extends SetNameDescriptionDAO {

    public enum Params
            implements GetNVConfig {

        REPORT_ID(NVConfigManager.createNVConfig("report_id", "The report ID.", "ReportID", true, true, String.class)),
        TOTAL_MATCHES(NVConfigManager.createNVConfig("total_matches", "The total number of matches.", "TotalMatches", true, true, Integer.class)),
        START_RANGE(NVConfigManager.createNVConfig("start range", "The starting index of range.", "StartRange", true, true, Integer.class)),
        END_RANGE(NVConfigManager.createNVConfig("end_range", "The ending index of range.", "EndRange", true, true, Integer.class)),
        BATCH(NVConfigManager.createNVConfigEntity("batch", "The results list.", "Batch", false, true, true, NVEntity.class, ArrayType.LIST)),

        ;

        private final NVConfig nvc;

        Params(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity NVC_API_BATCH_RESULT = new NVConfigEntityLocal("api_batch_result",
            null,
            "APIBatchResult",
            true,
            false,
            false,
            false,
            APIBatchResult.class,
            SharedUtil.extractNVConfigs(Params.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO);

    /**
     * The default constructor.
     */
    public APIBatchResult() {
        super(NVC_API_BATCH_RESULT);
    }

    /**
     * Returns the report ID.
     *
     * @return the report ID.
     */
    public String getReportID() {
        return lookupValue(Params.REPORT_ID);
    }

    /**
     * Sets the report ID.
     *
     * @param id
     */
    public void setReportID(String id) {
        setValue(Params.REPORT_ID, id);
    }

    /**
     * @return the total number of matches in APIReportResults.
     */
    public int getTotalMatches() {
        return lookupValue(Params.TOTAL_MATCHES);
    }

    /**
     * Sets the total number of matches in APIReportResults.
     *
     * @param totalMatches
     */
    public void setTotalMatches(int totalMatches) {
        setValue(Params.TOTAL_MATCHES, totalMatches);
    }

    /**
     * Returns the start range.
     *
     * @return the start range.
     */
    public int getStartRange() {
        return lookupValue(Params.START_RANGE);
    }

    /**
     * Returns the end range.
     *
     * @return the end range.
     */
    public int getEndRange() {
        return lookupValue(Params.END_RANGE);
    }

    /**
     * Sets the start and end range.
     *
     * @param startRange
     * @param endRange
     */
    public void setRange(int startRange, int endRange) {
        setValue(Params.START_RANGE, startRange);
        setValue(Params.END_RANGE, endRange);
    }

    /**
     * Returns the batch.
     *
     * @return the batch list.
     */
    public List<NVEntity> getBatch() {
        return lookupValue(Params.BATCH);
    }

    /**
     * Sets the batch.
     *
     * @param batch
     */
    public void setBatch(List<NVEntity> batch) {
        setValue(Params.BATCH, batch);
    }

    /**
     * Checks if last batch.
     *
     * @return true if last batch.
     */
    public boolean isLastBatch() {
        return (getTotalMatches() == getEndRange());
    }

}