package org.chemid.descriptor.restapi;

import org.chemid.descriptor.MolecularDescriptorService;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;

/**
 * Created by menaka on 7/15/16.
 */
public class Runner {
    public static void main(String[] args) throws FileNotFoundException {
        FileInputStream fileInputStream = new FileInputStream(new File("/home/menaka/ChemId/Data/Data/pubchem.sdf"));
        MolecularDescriptorService molecularDescriptorService = new MolecularDescriptorService();
        try {
            String s = molecularDescriptorService.getDescriptorCSV(fileInputStream, "").toString();
            System.out.println(s);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}
