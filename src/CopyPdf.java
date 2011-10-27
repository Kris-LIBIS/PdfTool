
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

import be.libis.lias.pdftool.Copy;
import be.libis.lias.pdftool.CopyOptions;
import be.libis.lias.toolbox.GeneralOptionsManager;

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
/**
 *
 * @author kris
 */
public class CopyPdf {

    private static final Logger logger = Logger.getLogger(CopyPdf.class.getName());

    public static void main(String[] args) throws IOException {
      CopyOptions options = new GeneralOptionsManager<CopyOptions>().processOptions(CopyOptions.class, args, logger);

      if (options == null)
        return; // error messages are printed by GeneralOptionsHandler

      if ((!options.isWatermarkText() && !options.isWatermarkImage())
          || (options.isWatermarkText() && options.isWatermarkImage())) {
        logger.severe("Either watermark text or watermark image must be specified.");
        return;
      }

      File source = options.getSourceFile();

      if (!source.exists()) {
        logger.severe("Source file '" + source.getAbsolutePath() + "' not found.");
        return;
      }

      if (!source.canRead()) {
        logger.severe("Source file '" + source.getAbsolutePath() + "' cannot be read.");
        return;
      }

      File target = options.getTargetFile();

      if (!target.exists())
        target.createNewFile();

      if (!target.canWrite()) {
        logger.severe("Target file '" + target.getAbsolutePath() + "' cannot be written.");
        return;
      }

      new Copy(source, target, options);
    }

}
