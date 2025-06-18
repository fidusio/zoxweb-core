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
package org.zoxweb.shared.data;

import org.zoxweb.shared.util.GetNVConfig;
import org.zoxweb.shared.util.NVConfig;
import org.zoxweb.shared.util.NVConfigEntity;
import org.zoxweb.shared.util.NVConfigEntityLocal;
import org.zoxweb.shared.util.NVConfigManager;
import org.zoxweb.shared.util.SharedUtil;

/**
 *
 */
@SuppressWarnings("serial")
public class RuntimeResultDAO
        extends SetNameDescriptionDAO {

    public enum ResultAttribute
            implements GetNVConfig {
        EXIT_CODE(NVConfigManager.createNVConfig("exit_code", "Process exit code", "ExitCode", false, true, int.class)),
        OUTPUT(NVConfigManager.createNVConfig("output", "Process input stream", "Output", false, true, String.class)),
        ERROR(NVConfigManager.createNVConfig("error", "Process error stream", "Error", false, true, String.class)),

        ;

        private final NVConfig nvc;

        ResultAttribute(NVConfig nvc) {
            this.nvc = nvc;
        }

        public NVConfig getNVConfig() {
            return nvc;
        }
    }

    public static final NVConfigEntity RUNTIME_RESULT_DAO = new NVConfigEntityLocal(
            "runtime_result_dao",
            null,
            "RuntimeResultDAO",
            true,
            false,
            false,
            false,
            RuntimeResultDAO.class,
            SharedUtil.extractNVConfigs(ResultAttribute.values()),
            null,
            false,
            SetNameDescriptionDAO.NVC_NAME_DESCRIPTION_DAO
    );

    /**
     * The default constructor.
     */
    public RuntimeResultDAO() {
        super(RUNTIME_RESULT_DAO);
    }

    /**
     * This constructor instantiates RuntimeResultDAO object based on given values.
     *
     * @param exitCode
     * @param outputData
     */
    public RuntimeResultDAO(int exitCode, String outputData) {
        this();
        setExitCode(exitCode);
        setOutputData(outputData);
    }

    /**
     * Gets the exit code.
     *
     * @return exit code
     */
    public int getExitCode() {
        return lookupValue(ResultAttribute.EXIT_CODE);
    }

    /**
     * Sets the exit.
     *
     * @param exitCode
     */
    public void setExitCode(int exitCode) {
        setValue(ResultAttribute.EXIT_CODE, exitCode);
    }

    /**
     * Gets the output data.
     *
     * @return output data
     */
    public String getOutputData() {
        return lookupValue(ResultAttribute.OUTPUT);
    }

    /**
     * Sets the output data.
     *
     * @param outputData
     */
    public void setOutputData(String outputData) {
        setValue(ResultAttribute.OUTPUT, outputData);
    }

    /**
     * Returns the error data.
     *
     * @return error data
     */
    public String getErrorData() {
        return lookupValue(ResultAttribute.ERROR);
    }

    /**
     * Sets the error data.
     *
     * @param errorData
     */
    public void setErrorData(String errorData) {
        setValue(ResultAttribute.ERROR, errorData);
    }

}