
import java.io.PrintStream;
import java.util.Properties;

import be.libis.lias.pdftool.CopyOptions;
import com.lexicalscope.jewel.cli.CliFactory;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author KrisD
 */
public class Main {
    
    static Properties prop = System.getProperties();

    public static void main(String[] args) {
        PrintStream out = System.err;

        String class_path = prop.getProperty("java.class.path", null);

        out.println("\nUsage: java -cp " + class_path + " <command> ...");
        out.println("\ncommand: CopyPdf  - Copy a PDF file and perform some actions\n");
        out.println(CliFactory.createCli(CopyOptions.class).getHelpMessage());
        out.println();
    }

}
