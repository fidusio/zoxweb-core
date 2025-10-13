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
package org.zoxweb.server.util;

import org.zoxweb.server.io.IOUtil;
import org.zoxweb.server.logging.LogWrapper;
import org.zoxweb.shared.data.ApplicationConfigDAO;
import org.zoxweb.shared.data.ApplicationConfigDAO.ApplicationDefaultParam;
import org.zoxweb.shared.util.GetValue;
import org.zoxweb.shared.util.NVGenericMap;
import org.zoxweb.shared.util.SharedBase64.Base64Type;
import org.zoxweb.shared.util.SharedStringUtil;
import org.zoxweb.shared.util.SharedUtil;

import java.io.*;

public class ApplicationConfigManager {

    public static final LogWrapper log = new LogWrapper(ApplicationConfigManager.class).setEnabled(true);
    public static final ApplicationConfigManager SINGLETON = new ApplicationConfigManager();
    public static final GetValue<?>[] DEFAULT_DIR_NAMES = {ApplicationDefaultParam.CONF_DIR,
            ApplicationDefaultParam.CACHE_DIR,
            ApplicationDefaultParam.DATA_DIR,
            ApplicationDefaultParam.PUBLIC_DIR,
            ApplicationDefaultParam.SSL_DIR,
            ApplicationDefaultParam.TEMP_DIR,
            ApplicationDefaultParam.VAR_DIR
    };

    private ApplicationConfigManager() {

    }

    private ApplicationConfigDAO defaultACD = null;

    private File defaultFile = null;
    private long defaultFileLastAccess = 0;

    public synchronized ApplicationConfigDAO loadDefault()
            throws NullPointerException, IOException {
        return load();
    }

    public static String getDefaultApplicationEnvVar() {
        String envURLLocation = System.getenv(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR);

        if (envURLLocation == null) {
            envURLLocation = System.getProperty(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR);
        }

        return envURLLocation;
    }

    public static String getDefaultApplicationEnvVar(String defaultValue) {
        String envURLLocation = System.getenv(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR);

        if (envURLLocation == null) {
            envURLLocation = System.getProperty(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR);
        }

        if (envURLLocation == null) {
            System.getProperties().put(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR, defaultValue);
            return defaultValue;
        }

        return envURLLocation;
    }

    private ApplicationConfigDAO load()
            throws NullPointerException, IOException {
        if (defaultFile == null) {
            defaultFile = new File(SharedUtil.toCanonicalID('/', getDefaultApplicationEnvVar(), ApplicationDefaultParam.CONF_DIR.getValue()), ApplicationConfigDAO.DEFAULT_APPLICATION_CONF_FILENAME);
        }

        if (!defaultFile.exists()) {
            System.out.println(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR + "=" + getDefaultApplicationEnvVar());
            log.getLogger().info(defaultFile + " not found");
            throw new FileNotFoundException(defaultFile.getName());
        }

        if (defaultFile.lastModified() != defaultFileLastAccess) {
            System.out.println(ApplicationConfigDAO.DEFAULT_APPLICATION_ENV_VAR + "=" + getDefaultApplicationEnvVar());
            String jsonString = IOUtil.inputStreamToString(defaultFile.toURI().toURL().openStream(), true);
            try {
                NVGenericMap props = GSONUtil.fromJSONGenericMap(jsonString, null, Base64Type.DEFAULT);
                defaultACD = new ApplicationConfigDAO(props);//GSONUtil.create(true).fromJson(jsonString, ApplicationConfigDAO.class);
                defaultFileLastAccess = defaultFile.lastModified();
            } catch (Exception e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
                throw new IOException(e.getMessage());
            }

        }

        return defaultACD;
    }

    public ApplicationConfigDAO load(InputStream is)
            throws NullPointerException, IOException {
        String jsonString = IOUtil.inputStreamToString(is, true);
        ApplicationConfigDAO ret = GSONUtil.create(true).fromJson(jsonString, ApplicationConfigDAO.class);
        return ret;
    }

    public String concatAsDirName(ApplicationConfigDAO acd, String varName) {
        String base = getDefaultApplicationEnvVar();
        String varValue = acd.lookupValue(varName);

        if (varValue != null) {
            return SharedUtil.toCanonicalID('/', base, varValue) + "/";
        }

        return base;
    }

    public String concatWithEnvVar(String... vars) {
        String base = getDefaultApplicationEnvVar();

        if (vars != null) {
            String[] values = new String[vars.length + 1];
            int index = 0;
            values[index++] = base;

            for (String s : vars) {
                values[index++] = s;
            }

            return SharedUtil.toCanonicalID('/', (Object[]) values);
        }

        return base;
    }

    public void save(ApplicationConfigDAO acd)
            throws NullPointerException, IOException {
        //File file = new File(concatAsDirName( acd, acd.lookupValue(ApplicationDefaultParam.CONF_DIR)), ApplicationConfigDAO.DEFAULT_APPLICATION_CONF_FILENAME);
        File file = new File(SharedUtil.toCanonicalID('/', getDefaultApplicationEnvVar(), ApplicationDefaultParam.CONF_DIR.getValue()), ApplicationConfigDAO.DEFAULT_APPLICATION_CONF_FILENAME);
        String jsonString = GSONUtil.toJSONGenericMap(acd.getProperties(), true, false, true);

        FileOutputStream fos = null;

        try {
            fos = new FileOutputStream(file);
            fos.write(jsonString.getBytes());
        } finally {
            IOUtil.close(fos);
        }
    }


    public static File locateFile(String filename) {
        //log.getLogger().info(getDefaultApplicationEnvVar() + "/" + filename);
        return new File(getDefaultApplicationEnvVar(), filename);
    }

    public File locateFile(ApplicationConfigDAO acd, String varName)
            throws NullPointerException, IOException {

        if (acd == null) {
            acd = loadDefault();
        }

        String varValue = acd.lookupValue(varName);

        if (varValue == null) {
            varValue = varName;
        }


        // varValue is an absolute file
        File file = new File(varValue);
        if (file.exists()) {
            return file;
        }

        // varValue is a file in conf dir
        file = new File(concatWithEnvVar(), varValue);

        if (file.exists()) {
            return file;
        }

        for (GetValue<?> dir : DEFAULT_DIR_NAMES) {
            file = new File(concatWithEnvVar((String) dir.getValue(), varValue));

            if (file.exists()) {
                return file;
            }
        }

        return null;
    }

    public byte[] loadFile(ApplicationConfigDAO acd, String varName) throws NullPointerException, IOException {
        File file = locateFile(acd, varName);
        if (file != null) {
            return IOUtil.inputStreamToByteArray(file, true).toByteArray();
        }
        return null;
    }

    public String loadFileAsString(ApplicationConfigDAO acd, String varName) throws NullPointerException, IOException {

        byte[] content = loadFile(acd, varName);
        if (content != null)
            return SharedStringUtil.toString(content);
        return null;
    }

    public String readConfigurationContent(ApplicationConfigDAO acd, String varName)
            throws NullPointerException, IOException {
        File configFile = locateFile(acd, varName);

        if (configFile != null && configFile.isFile()) {
            InputStream is = null;

            try {
                is = new FileInputStream(configFile);
                return IOUtil.inputStreamToString(is, true);
            } finally {
                // this close is required just in case we encounter IO error
                // to prevent unclosed file descriptors
                IOUtil.close(is);
            }
        }

        return null;
    }

    public void save(ApplicationConfigDAO scd, OutputStream os)
            throws NullPointerException, IOException {
        String jsonString = GSONUtil.create(true).toJson(scd);

        try {
            os.write(jsonString.getBytes());
        } finally {
            IOUtil.close(os);
        }
    }

}