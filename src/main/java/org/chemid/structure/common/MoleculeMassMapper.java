/*
 * Copyright (c) 2016, ChemID. (http://www.chemid.org)
 *
 * ChemID licenses this file to you under the Apache License V 2.0.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */
package org.chemid.structure.common;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class MoleculeMassMapper {
    public static final Properties molecularProperties = new Properties();
    private static MoleculeMassMapper instance;

    private MoleculeMassMapper() {

    }

    public static MoleculeMassMapper getInstance() throws IOException {
        if (instance == null) {
            setStream();
            instance = new MoleculeMassMapper();
        }
        return instance;
    }

    public static Properties setStream() throws IOException {
        InputStream inputStream = MoleculeMassMapper.class.getResourceAsStream("/adductTypes.properties");
        if (inputStream.available() > 0) {
            molecularProperties.load(inputStream);
            inputStream.close();
            return molecularProperties;
        }
        return null;
    }

    public Double getProperty(String key){
        return Double.valueOf(molecularProperties.getProperty(key));
    }

}

