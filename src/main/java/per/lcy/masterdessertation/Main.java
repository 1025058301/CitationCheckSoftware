package per.lcy.masterdessertation;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import per.lcy.masterdessertation.entity.CitationStyle;
import per.lcy.masterdessertation.entity.CustomException;
import per.lcy.masterdessertation.entity.HarvardCitation;
import per.lcy.masterdessertation.entity.OrganicSearchResult;
import per.lcy.masterdessertation.utils.CitationProcessUtil;
import per.lcy.masterdessertation.utils.CommonUtil;
import per.lcy.masterdessertation.utils.PdfProcessUtil;
import per.lcy.masterdessertation.utils.SearchUtil;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class Main {
    public static final Logger logger = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.print("Please input your file path.");
            return;
        }
        if (args.length > 2) {
            System.out.print("The number of file is larger than 2, please check it");
            return;
        }
        StringBuilder feedbacks = new StringBuilder();
        String userFilePath = args[0];
        String userFileName = CommonUtil.getFileName(userFilePath);
        String standardFilePath = null;
        String testFileName = null;
        if (args.length == 2) {
            standardFilePath = args[1];
        }
        if (userFileName == null) {
            System.out.print("Illegal file path, please check it. ");
            return;
        }
        try {
            if (args.length == 2) {// do the validation
                File userFile=new File(userFilePath);
                File standardFile=new File(standardFilePath);
                if(!userFile.exists()){
                    throw new CustomException("Please check your user file path: "+userFilePath+" , it doesn't exist.");
                }
                if(!standardFile.exists()){
                    throw new CustomException("Please check your standard file path: "+standardFilePath+" , it doesn't exist.");
                }
                List<String> userReferences=CommonUtil.getReferenceFromTxt(userFile);
                List<String> standardReferences=CommonUtil.getReferenceFromTxt(standardFile);
                int traceCount=-1;
                for(String userReference:userReferences){
                    if(userReference.length()==1){
                        traceCount+=1;
                        feedbacks.append(traceCount).append(System.lineSeparator());
                        continue;
                    }
                    String standardReference=standardReferences.get(traceCount);
                    String feedback = CitationProcessUtil.processUserHarvardCitation(userReference, CitationProcessUtil.spiltStandardHarvardCitation(standardReference));
                    feedbacks.append(feedback).append(System.lineSeparator());
                }


            } else {
                //get whole text of the article
                String wholeText = PdfProcessUtil.getTextFromPdf(userFilePath, false);
                //extract the references part of article
                String userReferences = CitationProcessUtil.getReferencesFromText(wholeText);
                //spilt the references
                String[] userReferencesArray = CitationProcessUtil.spiltReferences(userReferences);
                int referenceCount = 1;
                for (String userReference : userReferencesArray) {
                    String standardReference = SearchUtil.searchCitationFromQueryInfo(userReference, CitationStyle.Harvard);
                    String feedback = CitationProcessUtil.processUserHarvardCitation(userReference, CitationProcessUtil.spiltStandardHarvardCitation(standardReference));
                    feedbacks.append(referenceCount).append(".").append(feedback).append(System.lineSeparator());
                }
            }

        } catch (CustomException e) {
            feedbacks.append(e.getErrorMessage());
            logger.error(e.getMessage(), e);
        }
        CommonUtil.saveFeedback(feedbacks.toString(), userFileName);
    }
}
