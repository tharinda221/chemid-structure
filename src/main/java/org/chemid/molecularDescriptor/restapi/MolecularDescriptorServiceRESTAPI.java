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

package org.chemid.molecularDescriptor.restapi;

import org.chemid.molecularDescriptor.common.MolecularDescriptorService;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class includes RESTful API methods for molecular descriptor service.
 */
@Path("/rest/molecular")
public class MolecularDescriptorServiceRESTAPI {
    /**
     * This method returns the version number of the molecular descriptor service.
     *
     * @return API version
     */
    @GET
    @Path("version")
    @Produces(MediaType.TEXT_PLAIN)
    public String version() {
        return "Molecular Descriptor Service V 1.0";
    }

    /**
     * This method returns an CVS file which is including molecular descriptors.
     * @param uploadedInputStream chemical structures in SDF file.
     * @param fileDetail SDF file details. ex - name
     * @return comma-separated values for molecular descriptors
     */
    @POST
    @Path("descriptor")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.TEXT_PLAIN)
    public String generateMolecularDescriptorService(@FormDataParam("file") InputStream uploadedInputStream,
                                                     @FormDataParam("file") FormDataContentDisposition fileDetail) {
        MolecularDescriptorService molecularDescriptorService = new MolecularDescriptorService();
        try {
            return molecularDescriptorService.getDescriptorCSV(uploadedInputStream, "").toString();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }
}
