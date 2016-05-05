/*
 * Copyright (c) 2015, ChemID. (http://www.chemid.org)
 *
 * ChemID licenses this file to you under the Apache License V 2.0.
 * You may not use this file except in compliance with the License.
 * You may obtain a copy of the License at http://www.apache.org/licenses/LICENSE-2.0.
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
 * CONDITIONS OF ANY KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations under the License.
 */

package org.chemid.structure.dbclient.pubchem;

import com.sun.jersey.api.client.ClientResponse;
import org.chemid.structure.beans.pubChemESearch;
import org.chemid.structure.dbclient.common.Constants;
import org.chemid.structure.dbclient.common.RestClient;
import org.chemid.structure.dbclient.common.XmlParser;
import org.w3c.dom.Document;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response.Status;

public class PubChemClient {

    private pubChemESearch pubChemESearch;
    private RestClient restClient;

    public pubChemESearch getPubChemESearchRequestParameters() {

        try {

            this.restClient = new RestClient();
            ClientResponse response = restClient.getWebResource(Constants.PubChemClient.ESEARCH_URL).
                    accept(MediaType.APPLICATION_XML).get(ClientResponse.class);

            if (response.getStatus() != Status.OK.getStatusCode()) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }
            String resp = response.getEntity(String.class);
            Document doc = XmlParser.StringToXML(resp);
            pubChemESearch = new pubChemESearch();
            pubChemESearch.setWebEnv(doc.getElementsByTagName(Constants.PubChemClient.PUBCHEM_REQUEST_WebEnv_NAME).
                    item(Constants.PubChemClient.ITEM_NUMBER).
                    getFirstChild().getNodeValue());
            pubChemESearch.setQueryKey(doc.getElementsByTagName(Constants.PubChemClient.PUBCHEM_REQUEST_QueryKey_NAME).
                    item(Constants.PubChemClient.ITEM_NUMBER).
                    getFirstChild().getNodeValue());
            return pubChemESearch;


        } catch (Exception e) {

            e.printStackTrace();

        }
        return null;
    }

    public String getDownloadURL() {

        try {
            pubChemESearch = getPubChemESearchRequestParameters();
            Document xmlPayload = XmlParser.getXMLPayload(Constants.PubChemClient.PUBCHEM_DOWNLOAD_PAYLOAD_FILENAME,
                    Constants.PubChemClient.PUBCHEM_RESOURCES);
            xmlPayload.getElementsByTagName(Constants.PubChemClient.PUBCHEM_PAYLOAD_QueryKey_NAME).
                    item(Constants.PubChemClient.ITEM_NUMBER).setTextContent(pubChemESearch.getQueryKey());
            xmlPayload.getElementsByTagName(Constants.PubChemClient.PUBCHEM_PAYLOAD_WebEnv_NAME).
                    item(Constants.PubChemClient.ITEM_NUMBER).setTextContent(pubChemESearch.getWebEnv());

            System.out.println(pubQuery(XmlParser.getStringFromDocument(xmlPayload)));

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String pubQuery(String xmlPayload) {

        try {
            this.restClient = new RestClient();
            ClientResponse response = restClient.getWebResource(Constants.PubChemClient.REQUEST_URL).
                    accept(MediaType.APPLICATION_XML).type(MediaType.APPLICATION_XML).
                    post(ClientResponse.class, xmlPayload);

            if (response.getStatus() != Status.OK.getStatusCode()) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }
            String resp = response.getEntity(String.class);

            while(resp.contains(Constants.PubChemClient.PUG_QUERY_QUEUED_STATUS_TAG_NAME) ||
                    resp.contains(Constants.PubChemClient.PUG_QUERY_RUNNING_STATUS_TAG_NAME)) {
                Thread.sleep(1000);
                if(resp.contains(Constants.PubChemClient.CHECK_QUERY_WAITING_REQUEST_ID_TAG)) {
                    resp = checkQuery(XmlParser.StringToXML(resp).getElementsByTagName(Constants.PubChemClient.
                            CHECK_QUERY_WAITING_REQUEST_ID_TAG_NAME).item(Constants.PubChemClient.ITEM_NUMBER).
                            getFirstChild().getNodeValue());
                }
            }
            return resp;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public String checkQuery(String requestID) {

        try {
            Document xmlPayload = XmlParser.getXMLPayload(Constants.PubChemClient.CHECK_QUERY_FILE_NAME,
                    Constants.PubChemClient.PUBCHEM_RESOURCES);
            xmlPayload.getElementsByTagName(Constants.PubChemClient.CHECK_QUERY_REQUEST_ID_TAG_NAME).
                    item(Constants.PubChemClient.ITEM_NUMBER).setTextContent(requestID);
            this.restClient = new RestClient();
            ClientResponse response = restClient.getWebResource(Constants.PubChemClient.REQUEST_URL).
                    accept(MediaType.APPLICATION_XML).type(MediaType.APPLICATION_XML).
                    post(ClientResponse.class, XmlParser.getStringFromDocument(xmlPayload));

            if (response.getStatus() != Status.OK.getStatusCode()) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + response.getStatus());
            }
            String resp = response.getEntity(String.class);
            return resp;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static void main(String[] args) {

        PubChemClient pubChemClient = new PubChemClient();
        pubChemClient.getDownloadURL();
    }
}
