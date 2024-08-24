package per.lcy.masterdessertation.utils;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import per.lcy.masterdessertation.entity.CustomException;

import java.io.File;
import java.io.IOException;

public class PdfProcessUtil {
    public static Logger logger = LoggerFactory.getLogger(PdfProcessUtil.class);

    public static String getTextFromPdf(String path, boolean sortByPosition)throws CustomException {
        File pdfFile = new File(path);
        if(!pdfFile.exists()){
            throw new CustomException("Please check your file path: "+path+" , it doesn't exist.");
        }
        // extract text from pdf
        try (PDDocument document = PDDocument.load(pdfFile)) {
            PDFTextStripper pdfTextStripper = new PDFTextStripper();
            pdfTextStripper.setSortByPosition(sortByPosition);
            return pdfTextStripper.getText(document);
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            throw new CustomException("Some errors happen in PDF IO process.");
        }
    }
}
