package per.lcy.masterdessertation.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import per.lcy.masterdessertation.entity.DocumentType;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CommonUtil {
    public static Logger logger = LoggerFactory.getLogger(CommonUtil.class);

    public static DocumentType getDocumentType(String path) {
        Pattern pattern = Pattern.compile(".*\\.(\\w+)$");
        Matcher matcher = pattern.matcher(path);
        if (!matcher.find()) {
            logger.error("The input path is not a valid file path. Input path: " + path);
            return null;
        }

        switch (matcher.group(1).toUpperCase()) {
            case "PDF":
                return DocumentType.PDF;
            case "ODT":
                return DocumentType.ODT;
            case "WORD":
                return DocumentType.WORD;
            default:
                logger.error("The passed file type is not supported,the supported types are pdf,odt,word,input type is " + matcher.group(1));
                return null;
        }
    }

    public static void main(String [] args){
        DocumentType type=getDocumentType("sdff");
    }
}

