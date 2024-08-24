package per.lcy.masterdessertation.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import per.lcy.masterdessertation.entity.CustomException;
import per.lcy.masterdessertation.entity.DocumentType;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
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
    public static void saveFeedback(String feedback,String fileName){
        try (FileWriter writer = new FileWriter(fileName+".txt")) {
            writer.write(feedback);
            System.out.println("Feedback has been generated, please check the current path the program is on.");
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
        }
    }
    public static String getFileName(String path){
        String regex = "[^\\\\/]+(?=\\.[^\\.]+$)";
        Pattern pattern = Pattern.compile(regex);
        Matcher matcher = pattern.matcher(path);

        if (matcher.find()) {
            return matcher.group();
        } else {
            return null;
        }
    }
    public static List<String> getReferenceFromTxt(File file) throws CustomException {
        List<String> lines = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                lines.add(line.trim());  // 将每一行添加到列表中
            }
        } catch (IOException e) {
            logger.error(e.getMessage(),e);
            throw new CustomException("Some error happen in get Reference from txt file");
        }
        return lines;

    }

    public static void main(String [] args){
        DocumentType type=getDocumentType("sdff");
    }
}

