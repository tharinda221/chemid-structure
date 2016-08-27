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
package org.chemid.descriptor.restapi;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.Resource;

/**
 * The main method of this class deploys the chemical structure service on an embedded jetty server
 */
public class Application {

    /**
     * A private constructor to hide implicit public constructor
     */
    private Application() {
    }

    /**
     * This method starts an embedded instance of the jetty server and deploys the chemical structure service on it.
     *
     * @param args Arguments to main method
     * @throws Exception
     */
    public static void main(String[] args) throws Exception {
        ServletContextHandler context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        context.setContextPath("/");
        Server jettyServer = new Server(RESTAPIConstants.PORT_NO);
        jettyServer.setHandler(context);
        ServletHolder jerseyServlet = context.addServlet(org.glassfish.jersey.servlet.ServletContainer.class, "/*");
        jerseyServlet.setInitOrder(RESTAPIConstants.INIT_ORDER);
        jerseyServlet.setInitParameter("jersey.config.server.provider.classnames",
                MolecularDescriptorServiceRESTAPI.class.getCanonicalName() +  ";org.glassfish.jersey.media.multipart.MultiPartFeature");
        try {
            jettyServer.start();
            jettyServer.join();
        } finally {
            jettyServer.destroy();
        }
    }
}
