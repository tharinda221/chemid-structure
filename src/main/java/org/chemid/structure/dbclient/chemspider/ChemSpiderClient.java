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
package org.chemid.structure.dbclient.chemspider;

import com.chemspider.www.MassSpecAPIStub;
import com.chemspider.www.MassSpecAPIStub.*;
import com.chemspider.www.SearchStub;
import com.chemspider.www.SearchStub.AsyncSimpleSearch;
import com.chemspider.www.SearchStub.GetAsyncSearchResultResponse;
import com.chemspider.www.SearchStub.GetAsyncSearchStatusResponse;

import org.apache.axis2.transaction.TransactionConfiguration;
import org.apache.axis2.transport.http.HTTPConstants;
import org.chemid.structure.dbclient.common.Constants;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.ChemObject;
import org.openscience.cdk.exception.CDKException;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.io.MDLV2000Reader;
import org.openscience.cdk.io.SDFWriter;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.StringReader;
import java.lang.String;
import java.rmi.RemoteException;
import java.util.List;
import java.util.Vector;
import java.util.zip.GZIPInputStream;

public class ChemSpiderClient {

    protected String token = Constants.ChemSpiderConstants.TOKEN;
    protected IAtomContainer[] candidates = null;
    protected boolean verbose;
    private Integer CONNECTION_TIMEOUT = Constants.ChemSpiderConstants.CONNECTION_TIMEOUT;
    private Integer SO_TIME_OUT = Constants.ChemSpiderConstants.SO_TIME_OUT;

    public ChemSpiderClient(String token, boolean verbose) {
        this.token = token;
        this.verbose = verbose;
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
        } catch (Exception e) {
        }
        return Output;
    }

    public Vector<String> getChemspiderByMass(Double mass, Double error) throws RemoteException {
        MassSpecAPIStub stub = new MassSpecAPIStub();
        stub._getServiceClient().getOptions().setProperty(HTTPConstants.CHUNKED, false);
        stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, SO_TIME_OUT);
        stub._getServiceClient().getOptions().setCallTransportCleanup(true);

        SearchByMassAsync sbma = new SearchByMassAsync();
        sbma.setMass(mass);
        sbma.setRange(error);
        sbma.setToken(this.token);
        SearchByMassAsyncResponse sbmar = stub.searchByMassAsync(sbma);

        SearchStub thisSearchStub = new SearchStub();
        thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.CHUNKED, false);
        thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, SO_TIME_OUT);
        SearchStub.GetAsyncSearchResult GetAsyncSearchResultInput =
                new SearchStub.GetAsyncSearchResult();

        GetAsyncSearchResultInput.setRid(sbmar.getSearchByMassAsyncResult());
        GetAsyncSearchResultInput.setToken(token);
        GetAsyncSearchResultResponse thisGetAsyncSearchResultResponse = thisSearchStub.getAsyncSearchResult(GetAsyncSearchResultInput);
        int[] Output = thisGetAsyncSearchResultResponse.getGetAsyncSearchResultResult().get_int();

        Vector<String> csids = getChemSpiderByCsids(Output);
        thisSearchStub.cleanup();
        stub._getServiceClient().cleanupTransport();
        stub.cleanup();
        return csids;
    }

    public Vector<String> getChemSpiderByCsids(int[] _csids) throws RemoteException {
        Vector<Integer> uniqueCsidArray = new Vector<Integer>();
        for (int i = 0; i < _csids.length; i++) {
            if (!uniqueCsidArray.contains(_csids[i]))
                uniqueCsidArray.add(_csids[i]);
        }

        MassSpecAPIStub stub = new MassSpecAPIStub();
        stub._getServiceClient().getOptions().setProperty(HTTPConstants.CHUNKED, false);
        stub._getServiceClient().getOptions().setProperty(HTTPConstants.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
        stub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, SO_TIME_OUT);
        Vector<String> csids = new Vector<String>();

        if (this.verbose) System.out.println("Downloading compounds from ChemSpider");
        if (uniqueCsidArray.size() == 1) {
            this.candidates = new IAtomContainer[1];
            GetRecordMol getRecorMol = new GetRecordMol();
            getRecorMol.setCsid(String.valueOf(uniqueCsidArray.get(0)));
            getRecorMol.setToken(this.token);
            GetRecordMolResponse grmr = stub.getRecordMol(getRecorMol);
            try {
                Vector<IAtomContainer> cons = this.getAtomContainerFromString(grmr.getGetRecordMolResult());
                csids.add(String.valueOf(0));
                this.candidates[0] = cons.get(0);

            } catch (CDKException e) {
                e.printStackTrace();
            }
        } else {
            AsyncSimpleSearch ass = new AsyncSimpleSearch();
            String query = "";
            if (uniqueCsidArray.size() != 0) query += uniqueCsidArray.get(0);
            for (int i = 1; i < uniqueCsidArray.size(); i++)
                query += "," + uniqueCsidArray.get(i);
            ass.setQuery(query);
            ass.setToken(this.token);
            SearchStub thisSearchStub = new SearchStub();
            thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.CHUNKED, false);
            thisSearchStub._getServiceClient().getOptions().
                    setProperty(HTTPConstants.CONNECTION_TIMEOUT, CONNECTION_TIMEOUT);
            thisSearchStub._getServiceClient().getOptions().setProperty(HTTPConstants.SO_TIMEOUT, SO_TIME_OUT);

            csids = this.downloadCompressedSDF(thisSearchStub.asyncSimpleSearch(ass).getAsyncSimpleSearchResult(), stub);
        }
        stub._getServiceClient().cleanupTransport();
        stub.cleanup();
        return csids;
    }

    protected Vector<String> downloadCompressedSDF(String rid, MassSpecAPIStub stub) {
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

        Vector<String> csids = new Vector<String>();
        GZIPInputStream gin = null;
        if (dh != null) {
            try {
                gin = new GZIPInputStream(dh.getInputStream());
                SDFWriter wr = new SDFWriter(new FileOutputStream("structures.sdf"));
                MDLV2000Reader reader1 = new MDLV2000Reader(gin);
                ChemFile files = reader1.read(new ChemFile());
                List<IAtomContainer> objs = ChemFileManipulator.getAllAtomContainers(files);
                for (IChemObject o : objs) {
                    wr.write(o);
                }
                wr.close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (CDKException e) {
                e.printStackTrace();
            } catch (Exception e) {
                System.err.println("Problem retrieving ChemSpider webservices");
            }
        }
        return csids;
    }

    public synchronized IAtomContainer getMol(String id)
            throws RemoteException, CDKException {
        int intID = Integer.parseInt(id);
        return this.candidates[intID];
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

    public String getChemSpiderToken() {
        return this.token;
    }

    public String getCandidateID(String index) {
        int intIndex = Integer.parseInt(index);
        return (String) this.candidates[intIndex].getProperty("CSID");
    }
}