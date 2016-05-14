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

