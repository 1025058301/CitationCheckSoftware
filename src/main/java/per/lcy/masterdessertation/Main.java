package per.lcy.masterdessertation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import per.lcy.masterdessertation.entity.OrganicSearchResult;
import per.lcy.masterdessertation.utils.CitationProcessUtil;
import per.lcy.masterdessertation.utils.PdfProcessUtil;
import per.lcy.masterdessertation.utils.SearchUtil;

import java.io.File;
import java.io.IOException;

public class Main {
    public static final Logger logger= LoggerFactory.getLogger(Main.class);
    public static void main(String [] args){
        // test
        String path="/Users/lichiyuan/Desktop/referencing-paper-draft.pdf";
        File pdfFile=new File(path);
        logger.info(String.valueOf(pdfFile.exists()));
        //get whole text of the article
        String wholeText= PdfProcessUtil.getTextFromPdf(path,false);
        //extract the references part of article
        String references= CitationProcessUtil.getReferencesFromText(wholeText);
        //spilt the references
        String[] referencesArray=CitationProcessUtil.spiltReferences(references);

        // test call api to get different style of citation from online sources
        OrganicSearchResult result= SearchUtil.searchResultIdFromInfo("crime rate");
        SearchUtil.searchCitationFromResultId(result);
    }
}
