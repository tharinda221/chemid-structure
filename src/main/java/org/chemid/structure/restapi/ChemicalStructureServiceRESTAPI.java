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

package org.chemid.structure.restapi;

import org.chemid.structure.common.Constants;
import org.chemid.structure.common.MoleculeMassMapper;
import org.chemid.structure.dbclient.chemspider.ChemSpiderClient;
import org.chemid.structure.common.Constants;
import org.chemid.structure.dbclient.pubchem.PubChemClient;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.io.IOException;

/**
 * This class includes RESTful API methods for chemical structure service.
 */
@Path("/rest/structure")
public class ChemicalStructureServiceRESTAPI {
    /**
     * This method returns the version number of the chemical structure service.
     *
     * @return API version
     */
    @GET
    @Path("version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return "Chemical Structure Service V 1.0";
    }

    @GET
    @Path("{database}/{charge}/{adduct}/{mass}/{error}/{ppm}/{format}")
    @Produces(MediaType.TEXT_PLAIN)
    public String download(@PathParam("database") String database,
                           @PathParam("charge") String charge,
                           @PathParam("adduct") String adduct,
                           @PathParam("mass") Double mass,
                           @PathParam("error") Double error,
                           @PathParam("ppm") String ppm,
                           @PathParam("format") String format) throws IOException {
        ChemicalCalculator chemicalCalculator = new ChemicalCalculator();
        if (charge.toLowerCase().equals("p")) {
            mass = mass - MoleculeMassMapper.getInstance().getProperty("P." + adduct);
        } else if (charge.toLowerCase().equals("n")) {
            mass = mass + MoleculeMassMapper.getInstance().getProperty("N." + adduct);
        } else {

        }

        if (database.toLowerCase().contains("pubchem")) {
            System.out.println("PubChem Database");
            String massRange = chemicalCalculator.getMassRange(mass,0.01);
            PubChemClient pubChemClient = new PubChemClient();
            String Url = pubChemClient.getDownloadURL(massRange);
            StringBuilder stringBuilder = pubChemClient.getSDFBuffer(Url);
            return stringBuilder.toString();

        } else if (database.toLowerCase().contains("chemspider")) {
            ChemSpiderClient client = ChemSpiderClient.getInstance(Constants.ChemSpiderConstants.TOKEN, true);
            return client.getChemicalStructuresByMass(mass, error);

        } else {
            return "ERROR: Something is Wrong. Please check the values.";
        }

    }
}
