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
package org.chemid.descriptor;

import com.google.gson.Gson;
import org.chemid.descriptor.restapi.DescriptorContainer;
import org.chemid.descriptor.restapi.Descriptors;
import org.openscience.cdk.ChemFile;
import org.openscience.cdk.aromaticity.CDKHueckelAromaticityDetector;
import org.openscience.cdk.interfaces.IAtomContainer;
import org.openscience.cdk.interfaces.IChemFile;
import org.openscience.cdk.interfaces.IChemObject;
import org.openscience.cdk.interfaces.IMolecule;
import org.openscience.cdk.io.ISimpleChemObjectReader;
import org.openscience.cdk.io.ReaderFactory;
import org.openscience.cdk.qsar.DescriptorEngine;
import org.openscience.cdk.qsar.IDescriptor;
import org.openscience.cdk.qsar.IMolecularDescriptor;
import org.openscience.cdk.qsar.descriptors.molecular.IPMolecularLearningDescriptor;
import org.openscience.cdk.qsar.result.DoubleArrayResult;
import org.openscience.cdk.qsar.result.DoubleArrayResultType;
import org.openscience.cdk.qsar.result.DoubleResult;
import org.openscience.cdk.qsar.result.IDescriptorResult;
import org.openscience.cdk.qsar.result.IntegerArrayResult;
import org.openscience.cdk.qsar.result.IntegerArrayResultType;
import org.openscience.cdk.qsar.result.IntegerResult;
import org.openscience.cdk.smiles.SmilesGenerator;
import org.openscience.cdk.tools.manipulator.AtomContainerManipulator;
import org.openscience.cdk.tools.manipulator.ChemFileManipulator;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Vector;

/**
 * This class includes molecular descriptor methods implemented using CDK.
 */
public class MolecularDescriptorService {

    private DescriptorEngine ENGINE = new DescriptorEngine(DescriptorEngine.MOLECULAR);

    /**
     * This method is to read in molecules, using any supported format
     *
     * @param inputStream received chemical structure file
     * @return Vector<IMolecule> The molecules
     */
    public List<IMolecule> readMolecules(InputStream inputStream) throws IOException {
        Vector<IMolecule> mols = new Vector<IMolecule>();
        List<IAtomContainer> list;
        try {
            ISimpleChemObjectReader reader = new ReaderFactory().createReader(new InputStreamReader(
                    inputStream));
            if (reader == null)
                throw new IllegalArgumentException("Could not determine input file type");
            IChemFile content = (IChemFile) reader.read((IChemObject) new ChemFile());
            list = ChemFileManipulator.getAllAtomContainers(content);
            reader.close();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        for (IAtomContainer iAtomContainer : list) {
            IMolecule mol = (IMolecule) iAtomContainer;
            mol = (IMolecule) AtomContainerManipulator.removeHydrogens(mol);
            try {
                AtomContainerManipulator.percieveAtomTypesAndConfigureAtoms(mol);
            } catch (Exception e) {
                e.printStackTrace();
            }
            try {
                CDKHueckelAromaticityDetector.detectAromaticity(mol);
            } catch (Exception e) {
                e.printStackTrace();
            }
            if (mol.getAtomCount() == 0)
                System.err.println("molecule has no atoms");
            else
                mols.add(mol);
        }
        return mols;
    }

    /**
     * This method Calculate descriptors. Omits IPMolecularLearningDescriptor
     *
     * @param inputStream  received chemical structure file
     * @param descNamesStr comma-separated list of descriptor names (if empty, all descriptors will be calculated)
     * @return StringBuilder comma-separated descriptor values
     */
    public StringBuilder getDescriptorCSV(InputStream inputStream, String descNamesStr) throws IOException {

        List<IMolecule> mols = readMolecules(inputStream);
        System.err.println("read " + mols.size() + " compounds");
        List<IDescriptor> descriptors = ENGINE.getDescriptorInstances();
        System.err.println("found " + descriptors.size() + " descriptors");

        System.out.println(descriptors);

        Descriptors descriptors1 = new Descriptors();
        List<DescriptorContainer> containers = new ArrayList<DescriptorContainer>();

        List<String> descNames = Arrays.asList(descNamesStr.split(","));
        ArrayList<String> colNames = new ArrayList<String>();
        ArrayList<Double[]> values = new ArrayList<Double[]>();
        for (IDescriptor desc : descriptors) {

            DescriptorContainer container = new DescriptorContainer();
            container.setDescriptor_class(desc.getClass().getName());

            if (desc instanceof IPMolecularLearningDescriptor) {
                continue;
            }
            String tname = desc.getClass().getSimpleName();
            container.setDescriptor_names(desc.getDescriptorNames());

            containers.add(container);

            String[] tnamebits = tname.split("\\.");
            tname = tnamebits[tnamebits.length - 1];
            if ((descNamesStr.length() > 0) && (!descNames.contains(tname)))
                continue;
            String[] colNamesArr = desc.getDescriptorNames();
            for (int idx = 0; idx < colNamesArr.length; idx++) {
                colNamesArr[idx] = tname + "-" + colNamesArr[idx];
            }
            colNames.addAll(Arrays.asList(colNamesArr));
            List<Double[]> valuesList = computeLists(mols, (IMolecularDescriptor) desc);
            values.addAll(valuesList);
        }

        descriptors1.setContainers(containers);
        Gson gson = new Gson();
        System.out.println("Json :" + gson.toJson(descriptors1));

        int ncol = values.size();
        int nrow = mols.size();
        StringBuilder stringBuffer = new StringBuilder("");
        stringBuffer.append("Database,ID,");
        stringBuffer.append("SMILES,");
        for (int c = 0; c < ncol; c++) {
            if (c != 0) stringBuffer.append(",");
            stringBuffer.append(colNames.get(c));
        }
        stringBuffer.append("\n");
        for (int r = 0; r < nrow; r++) {
            int count = 0;
            stringBuffer.append(getId(mols.get(r))).append(",");
            String smi = getSmiles(mols.get(r));
            stringBuffer.append(smi).append(",");
            for (int c = 0; c < ncol; c++) {
                if (c != 0) stringBuffer.append(",");
                if (values.get(c)[r] == null) {
                    stringBuffer.append("NaN");
                } else {
                    stringBuffer.append("").append(values.get(c)[r]);
                }
            }
            stringBuffer.append("\n");
        }
        return stringBuffer;
    }

    /**
     * This methods compute descriptor values, convert to list
     *
     * @param mols list of molecules
     * @param desc The descriptor
     * @return List<Double[]> The descriptor values as list
     */
    public List<Double[]> computeLists(List<IMolecule> mols, IMolecularDescriptor desc) {
        System.out.println("computing descriptor " + getName(desc));
        List<Double[]> values = computeDescriptors(mols, (IMolecularDescriptor) desc);
        return values;
    }

    /**
     * This method get name for a given descriptor
     *
     * @param descriptor The descriptor
     */
    private String getName(IDescriptor descriptor) {
        return ENGINE.getDictionaryTitle(descriptor.getSpecification()).trim();
    }

    /**
     * This method compute descriptors
     *
     * @param molecules  The molecules
     * @param descriptor The descriptor
     * @return List<Double[]> The results as list
     */
    public List<Double[]> computeDescriptors(List<IMolecule> molecules, IMolecularDescriptor descriptor) {
        List<Double[]> descriptorsList = new ArrayList<Double[]>();

        for (int j = 0; j < getSize(descriptor); j++)
            descriptorsList.add(new Double[molecules.size()]);

        for (int i = 0; i < molecules.size(); i++) {
            if (molecules.get(i).getAtomCount() == 0) {
                for (int j = 0; j < getSize(descriptor); j++)
                    descriptorsList.get(j)[i] = null;
            } else {
                try {
                    IDescriptorResult res = descriptor.calculate(molecules.get(i)).getValue();
                    if (res instanceof IntegerResult)
                        descriptorsList.get(0)[i] = (double) ((IntegerResult) res).intValue();
                    else if (res instanceof DoubleResult)
                        descriptorsList.get(0)[i] = ((DoubleResult) res).doubleValue();
                    else if (res instanceof DoubleArrayResult)
                        for (int j = 0; j < getSize(descriptor); j++)
                            descriptorsList.get(j)[i] = ((DoubleArrayResult) res).get(j);
                    else if (res instanceof IntegerArrayResult)
                        for (int j = 0; j < getSize(descriptor); j++)
                            descriptorsList.get(j)[i] = (double) ((IntegerArrayResult) res).get(j);
                    else
                        throw new IllegalStateException("Unknown descriptor result value for '" + descriptor + "' : "
                                + res.getClass());
                } catch (Throwable e) {
                    System.err.println("Could not compute cdk feature " + descriptor);
                    e.printStackTrace();
                    for (int j = 0; j < getSize(descriptor); j++)
                        descriptorsList.get(j)[i] = null;
                }
            }
            for (int j = 0; j < getSize(descriptor); j++)
                if (descriptorsList.get(j)[i] != null && (descriptorsList.get(j)[i].isNaN() || descriptorsList.get(j)[i].isInfinite()))
                    descriptorsList.get(j)[i] = null;
        }

        return descriptorsList;
    }

    /**
     * This method is to get length of result for a given descriptor
     *
     * @param descriptor The descriptor
     * @return int The length
     */
    private int getSize(IMolecularDescriptor descriptor) {
        IDescriptorResult r = descriptor.getDescriptorResultType();
        if (r instanceof DoubleArrayResultType)
            return ((DoubleArrayResultType) r).length();
        else if (r instanceof IntegerArrayResultType)
            return ((IntegerArrayResultType) r).length();
        else
            return 1;
    }

    /**
     * Get SMILES code for a molecule
     *
     * @param molecule The molecule
     * @return string The SMILES code
     */
    public static String getSmiles(IMolecule molecule) {
        Map<Object, Object> props = molecule.getProperties();
        for (Object key : props.keySet()) {
            if (key.toString().equals("STRUCTURE_SMILES") || key.toString().equals("SMILES"))
                return props.get(key).toString();
        }
        SmilesGenerator g = new SmilesGenerator();
        return g.createSMILES(molecule);
    }

    /**
     * Returns the Chemical Structure identification number and the database name.
     * @param molecule : Chemical structure object.
     * @return : Database and id number (comma separated)
     */
    public static String getId(IMolecule molecule) {
        Map<Object, Object> properties = molecule.getProperties();
        String id = "";
        for (Object o : properties.keySet()) {
            if (o.toString().equals("PUBCHEM_COMPOUND_CID")) {
                id = "PUBCHEM," + properties.get(o);
            } else if (o.toString().equals("CSID")) {
                id = "ChemSpider," + properties.get(o);
            }
        }
        return id;
    }
}
