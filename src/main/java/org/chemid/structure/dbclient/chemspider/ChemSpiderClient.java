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
package org.chemid.structure.dbclient.chemspider;

import org.apache.axis2.AxisFault;
import org.apache.axis2.transaction.TransactionConfiguration;
import org.apache.axis2.transport.http.HTTPConstants;
import org.chemid.structure.common.Constants;
import org.chemid.structure.dbclient.chemspider.generated.MassSpecAPIStub;
import org.chemid.structure.dbclient.chemspider.generated.MassSpecAPIStub.*;
import org.chemid.structure.dbclient.chemspider.generated.SearchStub;
import org.chemid.structure.dbclient.chemspider.generated.SearchStub.AsyncSimpleSearch;
import org.chemid.structure.dbclient.chemspider.generated.SearchStub.GetAsyncSearchResultResponse;
import org.chemid.structure.dbclient.chemspider.generated.SearchStub.GetAsyncSearchStatusResponse;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.lang.String;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

public class ChemSpiderClient {

    private static ChemSpiderClient client;
    protected String token = Constants.ChemSpiderConstants.TOKEN;
    protected IAtomContainer[] candidates = null;
    protected boolean verbose;
    private Integer CONNECTION_TIMEOUT = Constants.ChemSpiderConstants.CONNECTION_TIMEOUT;
    private Integer SO_TIME_OUT = Constants.ChemSpiderConstants.SO_TIME_OUT;

    private ChemSpiderClient(String token, boolean verbose) {
        this.token = token;
        this.verbose = verbose;
    }

    public static ChemSpiderClient getInstance(String token, boolean verbose) {
        if (client == null) {
            client = new ChemSpiderClient(token, verbose);
            return client;
        }
        return client;
    }

    public static String get_Search_GetAsyncSearchStatus_Results(String rid, String token) {
        String Output = null;
        try {
            final SearchStub thisSearchStub = new SearchStub();
            thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.CHUNKED, false);
            SearchStub.GetAsyncSearchStatus GetAsyncSearchStatusInput =
                    new SearchStub.GetAsyncSearchStatus();
            GetAsyncSearchStatusInput.setRid(rid);
            GetAsyncSearchStatusInput.setToken(token);
            final GetAsyncSearchStatusResponse thisGetAsyncSearchStatusResponse =
                    thisSearchStub.getAsyncSearchStatus(GetAsyncSearchStatusInput);
            Output = thisGetAsyncSearchStatusResponse.getGetAsyncSearchStatusResult().toString();
        } catch (AxisFault axisFault) {
            axisFault.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return Output;
    }

    public String getChemicalStructuresByMass(Double mass, Double error) throws RemoteException {
        MassSpecAPIStub massSpecAPIStub = new MassSpecAPIStub();
        massSpecAPIStub._getServiceClient().getOptions().setProperty(HTTPConstants.CHUNKED, false);
        massSpecAPIStub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        massSpecAPIStub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, SO_TIME_OUT);
        massSpecAPIStub._getServiceClient().getOptions().setCallTransportCleanup(true);

        SearchByMassAsync searchByMassAsync = new SearchByMassAsync();
        searchByMassAsync.setMass(mass);
        searchByMassAsync.setRange(error);
        searchByMassAsync.setToken(this.token);
        SearchByMassAsyncResponse massAsyncResponse = massSpecAPIStub.searchByMassAsync(searchByMassAsync);

        SearchStub thisSearchStub = new SearchStub();
        thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.CHUNKED, false);
        thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, SO_TIME_OUT);
        SearchStub.GetAsyncSearchResult GetAsyncSearchResultInput = new SearchStub.GetAsyncSearchResult();

        GetAsyncSearchResultInput.setRid(massAsyncResponse.getSearchByMassAsyncResult());
        GetAsyncSearchResultInput.setToken(token);
        GetAsyncSearchResultResponse thisGetAsyncSearchResultResponse =
                thisSearchStub.getAsyncSearchResult(GetAsyncSearchResultInput);
        int[] Output = thisGetAsyncSearchResultResponse.getGetAsyncSearchResultResult().get_int();

        String sdf = getChemicalStructuresByCsids(Output);
        thisSearchStub.cleanup();
        massSpecAPIStub._getServiceClient().cleanupTransport();
        massSpecAPIStub.cleanup();
        return sdf;
    }

    public String getChemicalStructuresByCsids(int[] _csids) throws RemoteException {
        Vector<Integer> uniqueCsidArray = new Vector<Integer>();
        for (int _csid : _csids) {
            if (!uniqueCsidArray.contains(_csid))
                uniqueCsidArray.add(_csid);
        }
        String sdf = "";
        MassSpecAPIStub massSpecAPIStub = new MassSpecAPIStub();
        massSpecAPIStub._getServiceClient().getOptions().setProperty(HTTPConstants.CHUNKED, false);
        massSpecAPIStub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        massSpecAPIStub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, SO_TIME_OUT);
        Vector<String> csids = new Vector<String>();

        if (this.verbose) System.out.println("Downloading compounds from ChemSpider");
        if (uniqueCsidArray.size() == 1) {
            this.candidates = new IAtomContainer[1];
            GetRecordMol getRecordMol = new GetRecordMol();
            getRecordMol.setCsid(String.valueOf(uniqueCsidArray.get(0)));
            getRecordMol.setToken(this.token);
            GetRecordMolResponse grmr = massSpecAPIStub.getRecordMol(getRecordMol);
            try {
                Vector<IAtomContainer> cons = this.getAtomContainerFromString(grmr.getGetRecordMolResult());
                csids.add(String.valueOf(0));
                this.candidates[0] = cons.get(0);

            } catch (CDKException e) {
                e.printStackTrace();
            }
        } else {
            AsyncSimpleSearch asyncSimpleSearch = new AsyncSimpleSearch();
            String query = "";
            if (uniqueCsidArray.size() != 0) query += uniqueCsidArray.get(0);
            for (int i = 1; i < uniqueCsidArray.size(); i++)
                query += "," + uniqueCsidArray.get(i);
            asyncSimpleSearch.setQuery(query);
            asyncSimpleSearch.setToken(this.token);
            SearchStub thisSearchStub = new SearchStub();
            thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.CHUNKED, false);
            thisSearchStub._getServiceClient().getOptions().
                    setProperty(HTTPConstants.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
            thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, SO_TIME_OUT);

            sdf = this.downloadCompressedSDF(
                    thisSearchStub.asyncSimpleSearch(asyncSimpleSearch).getAsyncSimpleSearchResult(), massSpecAPIStub);
        }
        massSpecAPIStub._getServiceClient().cleanupTransport();
        massSpecAPIStub.cleanup();
        return sdf;
    }

    protected String downloadCompressedSDF(String rid, MassSpecAPIStub stub) {
        TransactionConfiguration tc = new TransactionConfiguration();
        tc.setTransactionTimeout(Integer.MAX_VALUE);
        stub._getServiceClient().getAxisConfiguration().setTransactionConfig(tc);
        GetCompressedRecordsSdf getCompressedRecordsSdf = new GetCompressedRecordsSdf();
        boolean status_ok = false;
        while (!status_ok) {
            String status = get_Search_GetAsyncSearchStatus_Results(rid, token);
            if (status.equals("ResultReady")) {
                status_ok = true;
            } else {
                try {

                    Thread.sleep(Constants.ChemSpiderConstants.THREAD_TIME_OUT);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }
        getCompressedRecordsSdf.setRid(rid);
        getCompressedRecordsSdf.setToken(this.token);
        getCompressedRecordsSdf.setEComp(ECompression.eGzip);
        GetCompressedRecordsSdfResponse getCompressedRecordsSdfResponse = null;
        javax.activation.DataHandler dh = null;
        try {
            getCompressedRecordsSdfResponse = stub.getCompressedRecordsSdf(getCompressedRecordsSdf);
            dh = getCompressedRecordsSdfResponse.getGetCompressedRecordsSdfResult();
        } catch (RemoteException e1) {
            e1.printStackTrace();
        } catch (Exception e) {
            System.err.println("Problem retrieving ChemSpider webservices");
        }

        GZIPInputStream gin;
        StringBuilder stringBuilder = new StringBuilder();
        if (dh != null) {
            try {
                gin = new GZIPInputStream(dh.getInputStream());
                InputStreamReader reader = new InputStreamReader(gin);
                BufferedReader in = new BufferedReader(reader);

                String read;
                while ((read = in.readLine()) != null) {
                    stringBuilder.append(read).append("\n");
                }
                in.close();

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return stringBuilder.toString();
    }

    protected Vector<IAtomContainer> getAtomContainerFromString(String sdfString) throws CDKException {
        MDLV2000Reader reader = new MDLV2000Reader(new StringReader(sdfString));

        List<IAtomContainer> containersList;
        Vector<IAtomContainer> ret = new Vector<IAtomContainer>();

        ChemFile chemFile = (ChemFile) reader.read((ChemObject) new ChemFile());
        containersList = ChemFileManipulator.getAllAtomContainers(chemFile);
        for (IAtomContainer container : containersList) {
            ret.add(container);
        }
        try {
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return ret;
    }

}