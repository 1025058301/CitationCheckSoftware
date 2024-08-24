package per.lcy.masterdessertation.utils;

import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.CoreSentence;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import per.lcy.masterdessertation.entity.CustomException;
import per.lcy.masterdessertation.entity.HarvardCitation;
import org.apache.commons.text.similarity.JaccardSimilarity;

import java.io.InputStream;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CitationProcessUtil {
    public static Logger logger = LoggerFactory.getLogger(CitationProcessUtil.class);
    public static JaccardSimilarity jaccardSimilarity = new JaccardSimilarity();

    public static String getReferencesFromText(String text) throws CustomException {
        String regexFull = "(References|REFERENCES|Bibliography|BIBLIOGRAPHY)\\s+[\\[\\(].*";
        String regexPartition = "(References|REFERENCES|Bibliography|BIBLIOGRAPHY)";
        // Correctly identify the References section
        Pattern patternFull = Pattern.compile(regexFull, Pattern.DOTALL);
        Matcher matcherFull = patternFull.matcher(text);
        // Only identify the key word
        Pattern patternPartition = Pattern.compile(regexPartition, Pattern.DOTALL);
        Matcher matcherPartition = patternPartition.matcher(text);
        if (matcherFull.find()) {
            return matcherFull.group();
        } else if (matcherPartition.find()) {
            throw new CustomException("Each reference should be preceded by [] or () to indicate the order,please check it.");
        } else {
            throw new CustomException("The referenced part of the document is not recognized, the section name for the referenced part should be REFERENCES or BIBLIOGRAPHY.");
        }
    }

    public static String[] spiltReferences(String references) throws CustomException {
        if (references == null) {
            throw new CustomException("The user references input is null.");
        }
        String spiltRegex = "(?=\\[\\d+\\])";// match [1],[2]...
        String[] referencesArray = references.split(spiltRegex);
        if (referencesArray.length <= 1) {
            throw new CustomException("The user reference section does not have any citations, or the citations do not start with the format [].");
        }
        // remove the first element: References...,ensure each element is a reference
        return Arrays.copyOfRange(referencesArray, 1, referencesArray.length);
    }

    public static HashMap<String, String> spiltStandardHarvardCitation(String standardCitation) {
        HashMap<String, String> res = new HashMap<>();
        // 匹配作者部分，发表时间，文章标题，和剩下的部分
        // Match the author section, publication date, article title, and the rest of the section
        String regex = "(.*?)(\\d{4}[^.]*)\\.([^.]+?[\\.?])(.+)";
        String authorYearRegex = "(.*?\\d+[^.\\d]*)\\.";
        String urlRegex = "(https?://doi.\\s*org[^\\s]+|https?://[^\\s]+|10\\.\\d{4,9}/[-._;()/:A-Z0-9]+)";
        //匹配 in 的会议格式，将第二部分作为发版商
        //Match the conference format of in and use the second part as the publisher
        String conferenceRegex = "^(In[\\s:].+?)(?:\\([^\\)]+\\))?\\.(.+?)(\\.|\\(p)";
        //匹配形如 journalname, volume part, pages 的期刊格式
        //Matches journal formats such as journalname, volume part, pages
        String journalRegex = "^(([^,]+),\\s*([^,]+),\\s*(pp?\\.?\\s*\\w+(-\\d+)?))";
        //匹配不同格式的期号卷号
        //Match issue number and volume number in different formats
        String volumeIssueRegex = "((Vol|Volume)\\.\\s*\\d+|\\d+\\s*\\(\\d+\\))";
        //匹配单页或多页
        //Matches a single or multiple pages
        String pagesRegex = "(p{1,2}\\.\\s*\\d+(-\\d+)?)";
        //匹配发版商
        //Match publisher
        String publishRegex = "^(.*?)(\\.|\\(p)";
        String authors = "";
        String year = "";
        String title = "";
        String remainder = "";
        String urlOrDOI = "";
        String conference = "";
        String publisher = "";
        String journalTotal = "";
        String journalName = "";
        String volumeIssue = "";
        String pages = "";
        List<String> differentPart = getMatchGroups(standardCitation, regex);
        //划分不同部分
        if (differentPart.size() >= 4) {
            authors = differentPart.get(0);
            year = differentPart.get(1);
            title = differentPart.get(2);
            remainder = differentPart.get(3);
        } else {
            logger.error("fail to extract different part from standard citation");
            return null;
        }
        String authorAndYear=getMatchGroups(standardCitation,authorYearRegex).get(0);
        // 分割得到作者字符串数组
        String[] authorsArray = authors.substring(0, authors.length() - 2).replaceAll("\\s+", "").split("\\.(,|and|&)");
        authors = convertArrayToString(authorsArray, ";");
        // 获取url链接或DOI号
        List<String> url = getMatchGroups(remainder, urlRegex);
        if (!url.isEmpty()) {
            urlOrDOI = url.get(0);
        }
        // 尝试获取会议不同部分
        List<String> conferencePart = getMatchGroups(remainder, conferenceRegex);
        if (conferencePart.size() >= 2) {
            conference = conferencePart.get(0);
            publisher = conferencePart.get(1);
        }
        // 尝试获取期刊不同部分
        List<String> journalPart = getMatchGroups(remainder, journalRegex);
        if (journalPart.size() >= 4) {
            journalTotal = journalPart.get(0);
            journalName = journalPart.get(1);
            volumeIssue = journalPart.get(2);
            pages = journalPart.get(3);
        }
        // 如果volumeIssue和pages为空，尝试从剩余文本中直接获得（会议情况，如果是期刊则已获得）
        if (volumeIssue.isEmpty() && pages.isEmpty()) {
            List<String> volumeIssuePart = getMatchGroups(remainder, volumeIssueRegex);
            if (!volumeIssuePart.isEmpty()) {
                volumeIssue = volumeIssuePart.get(0);
            }
            List<String> pagesPart = getMatchGroups(remainder, pagesRegex);
            if (!pagesPart.isEmpty()) {
                pages = pagesPart.get(0);
            }
        }
        // 如果此时发版商为空，说明大概率是book，获取tittle后紧跟着的部门作为发版商
        if (journalName.isEmpty() && publisher.isEmpty()) {
            List<String> publishPart = getMatchGroups(remainder, publishRegex);
            if (!publishPart.isEmpty()) {
                publisher = publishPart.get(0);
            }
        }
        //将信息存入hashmap中
        res.put("authors", authors);
        res.put("year", year);
        res.put("authorAndYear",authorAndYear);
        res.put("tittle", title);
        res.put("conference", conference);
        res.put("journalName", journalName);
        res.put("volumeIssue", volumeIssue);
        res.put("pages", pages);
        res.put("urlOrDOI", urlOrDOI);
        res.put("publisher", publisher);
        res.put("standardCitation", standardCitation);
        res.put("journalTotal", journalTotal);
        return res;
    }

    public static String processUserHarvardCitation(String userCitation, Map<String, String> standardCitation) {
        StringBuilder feedback = new StringBuilder();
        String authorYearRegex = "(.*?)(\\d+[^.\\d]*)\\.";
        String urlRegex = ".*?(10\\.\\d{4,9}/[-._;()/:A-Z0-9]+)";
        String journalRegex = "^([^,]+),\\s*([^,]+),\\s*(pp?\\.?\\s*\\w+(-\\d+)?)";
        String conferenceRegex = "^(In[\\s:].+?)(?:\\([^\\)]+\\))?";
        String volumeIssueRegex = "((Vol|Volume)\\.\\s*\\d+|\\d+\\s*\\(\\d+\\))";
        String pagesRegex = "(p{1,2}\\.\\s*\\d+(-\\d+)?)";
        // 判断用户传入的引用是否为空
        // Determines whether the reference passed by the user is empty
        if (userCitation == null || userCitation.isEmpty()) {
            feedback.append("This is an empty citation. Please check it");
            return feedback.toString();
        }
        // 判断用户传入的引用是否与标准引用完全吻合
        // Determines whether the reference user passed matches the standard reference exactly
        if (userCitation.trim().equals(standardCitation.get("standardCitation"))) {
            feedback.append("No errors!");
            return feedback.toString();
        }
        userCitation = preprocess(userCitation);
        // Used to record the component and order of user references
        List<String> userCitationComponent = new ArrayList<>();
        // use tools to split sentences
        List<String> inputSentences = getSpiltSentenceByStanford(userCitation);
//        SentenceDetectorME sentenceDetectorME=getSentenceDetector();
//        String[] inputSentences =sentenceDetectorME.sentDetect(userCitation);
        // 为每个句子匹配最相似的部分
        // Match the most similar parts for each sentence
        for (String part : inputSentences) {
            String bestMatch = null;
            double bestSimilarity = -1.0;
            String bestKey = null;

            for (Map.Entry<String, String> entry : standardCitation.entrySet()) {
                String key = entry.getKey();
                if (key.isEmpty() || key.equals("standardCitation") || key.equals("journalName") || key.equals("volumeIssue") || key.equals("pages")||key.equals("authors")||key.equals("year"))
                    continue;
                // 计算当前句子与标准引用中每个部分的相似度，寻找最相似的部分
                // calculate the similarity between the current sentence and each part in the standard citation to find the most similar part
                double similarity = jaccardSimilarity.apply(part, entry.getValue());
                if (similarity > bestSimilarity) {
                    bestSimilarity = similarity;
                    bestMatch = entry.getValue();
                    bestKey = entry.getKey();
                }
            }
            List<String> journalPart = getMatchGroups(part, journalRegex);
            if (journalPart.size() >= 3) {// The sentence matches the journal format 该句子与期刊格式匹配
                String standardJournalName = standardCitation.get("journalName");
                if (standardJournalName.isEmpty()) {
                    feedback.append("This article is not a journal!");
                    continue;
                }
                String standardVolumeIssue = standardCitation.get("volumeIssue");
                String standardPages = standardCitation.get("pages");
                if (!journalPart.get(0).equals(standardJournalName)) {
                    feedback.append(" Errors in journal's name, correct is:").append(standardJournalName).append(".");
                }
                if (!journalPart.get(1).equals(standardVolumeIssue)) {
                    feedback.append(" Errors in journal's volume and Issue, correct is:").append(standardVolumeIssue).append(".");
                }
                if (!journalPart.get(2).equals(standardPages)) {
                    feedback.append(" Errors in journal's pages, correct is:").append(standardPages).append(".");
                }
                userCitationComponent.add("journalName");
                userCitationComponent.add("volumeIssue");
                userCitationComponent.add("pages");
                continue;
            }
            List<String> volumeIssuePart = getMatchGroups(part, volumeIssueRegex);
            List<String> pagesPart = getMatchGroups(part, pagesRegex);
            // 该句子包含 volumeIssue 信息
            // The sentence contains volumeIssue information
            if (!volumeIssuePart.isEmpty()) {
                String standardVolumeIssue = standardCitation.get("volumeIssue");
                if (standardVolumeIssue.isEmpty()) {
                    feedback.append("There is no volumeIssue part in standard citation, please confirm that the volumeIssue:").append(volumeIssuePart.get(0)).append(" is really you want.");
                } else if (!volumeIssuePart.get(0).equals(standardVolumeIssue)) {
                    feedback.append("Errors in volume and issue, correct is:").append(standardVolumeIssue);
                }
                userCitationComponent.add("volumeIssue");
            }
            // 该句子包含 pages 信息
            // This sentence contains pages information
            if (!pagesPart.isEmpty()) {
                String standardPages = standardCitation.get("pages");
                if (standardPages.isEmpty()) {
                    feedback.append("There is no pages part in standard citation, please confirm that the pages:").append(pagesPart.get(0)).append(" is really you want.");
                } else if (!pagesPart.get(0).equals(standardPages)) {
                    feedback.append("Errors in pages, correct is:").append(standardPages);
                }
                userCitationComponent.add("pages");
            }
            List<String> conferencePart = getMatchGroups(part, conferenceRegex);
            // 该句子和会议部分匹配
            // this sentence match to conference part
            if (!conferencePart.isEmpty()) {
                String standardConference = standardCitation.get("conference");
                if (!conferencePart.get(0).equals(standardConference)) {
                    feedback.append(" Errors in conference part, correct is:").append(standardConference).append(".");
                }
                userCitationComponent.add("conference");
                continue;
            }
            // 没找到合适的匹配部分
            // No suitable matching part found
            if (bestSimilarity < 0.4) {
                feedback.append("Can not identify \"").append(part).append("\" belongs to which part. Please check it. ");
                continue;
            }
            switch (bestKey) {
                // 识别到该句子是作者与发布年份部分
                // Recognize that the sentence is the author and the year of publication part
                case "authorAndYear" -> {
                    List<String> authorAndYear = getMatchGroups(part, authorYearRegex);
                    if (authorAndYear.size() < 2) {
                        feedback.append("Can't not identify publish year. Please check whether the citation contains it. ");
                        userCitationComponent.add("authorAndYear");
                        continue;
                    } else {
                        String inputAuthors = authorAndYear.get(0);
                        String[] inputAuthorsArray = inputAuthors.substring(0, inputAuthors.length() - 2).replaceAll("\\s+", "").split("\\.(,|and|&)");
                        String[] standardAuthorsArray = bestMatch.split(";");
                        String authorFeedback=authorsPartFeedback(inputAuthorsArray, standardAuthorsArray);
                        if(!authorFeedback.isEmpty()){
                            feedback.append(authorFeedback);
                        }
                    }
                    String inputPublishYear = authorAndYear.get(1);
                    String standardPublishYear = standardCitation.get("year");
                    if (!inputPublishYear.equals(standardPublishYear)) {
                        feedback.append("Publish year is wrong. Correct publish year is:").append(standardPublishYear).append(" . ");
                    }
                    userCitationComponent.add("authorAndYear");
                }
                //识别到是标题部分
                // tittle part
                case "tittle" -> {
                    if (!part.equals(bestMatch)) {
                        feedback.append("Errors in tittle, correct tittle is:").append(bestMatch);
                    }
                    userCitationComponent.add("tittle");
                }
                //publisher part
                case "publisher" -> { //识别到是发版商部分
                    if (part.charAt(part.length() - 1) == '.') {
                        part = part.substring(0, part.length() - 1);
                    }
                    if (!part.equals(bestMatch)) {
                        feedback.append("Errors in publisher, correct publisher is:").append(bestMatch);
                    }
                    userCitationComponent.add("publisher");
                }
                // url or doi part
                case "urlOrDOI" -> { //识别到是URL或DOI
                    Pattern pattern = Pattern.compile(urlRegex);
                    Matcher matcher = pattern.matcher(bestMatch);
                    if (matcher.find()) {//如果是doi,比较整个url和进行doi的单独对比
                        if (!part.equals(matcher.group(1)) && !part.equals(bestMatch.replaceAll("\\s+", ""))) {
                            feedback.append("Errors in DOI, correct is: ").append(matcher.group());
                        }
                    } else if (!part.equals(bestMatch)) {// 如果是url，整体进行对比
                        feedback.append("Errors in URL,correct is:").append(bestMatch);
                    }
                    userCitationComponent.add("urlOrDOI");
                }
                // looks like journal or conference,but format is not match
                case "journalTotal" ->
                        feedback.append(part).append(" looks like journal part, ").append("the correct one is").append(standardCitation.get("journalTotal"));
                case "conference" ->
                        feedback.append(part).append(" looks like conference part, ").append("the correct one is").append(standardCitation.get("conference"));
            }
        }
        // 检查是否缺少组成部分
        // check whether it miss some component
        ArrayList<String> standardCitationComponent = getStandardComponent(standardCitation);
        boolean miss = false;
        for (String standardComponent : standardCitationComponent) {
            boolean find = false;
            for (String userComponent : userCitationComponent) {
                if (standardComponent.equals(userComponent)) {
                    find = true;
                    break;
                }
            }
            if (!find) {
                miss = true;
                feedback.append(" Component:").append(standardComponent).append(" is miss.");
                feedback.append(" It should be ").append(standardCitation.get(standardComponent)).append(".");
            }
        }
        if (miss) return feedback.toString();
        // 检查是否顺序不一致
        // check whether the order is wrong
        int minNum = Math.min(standardCitationComponent.size(), userCitationComponent.size());
        for (int i = 0; i < minNum; i++) {
            if (!standardCitationComponent.get(i).equals(userCitationComponent.get(i))) {
                feedback.append("The order of component of your citation is wrong. Correct order is: ").append(convertArrayToString(standardCitationComponent, ","));
                break;
            }
        }
        if(feedback.isEmpty()){
            feedback.append("No errors");
        }
        return feedback.toString();
    }


    private static List<String> getMatchGroups(String text, String regex) {
        List<String> groups = new ArrayList<>();
        Pattern pattern = Pattern.compile(regex, Pattern.CASE_INSENSITIVE);
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            for (int i = 1; i <= matcher.groupCount(); i++) {
                if (matcher.group(i) != null) {
                    groups.add(matcher.group(i).trim());
                }
            }
        }
        return groups;
    }

    // 将字符串数组转换为单个字符串
    public static String convertArrayToString(String[] array, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < array.length; i++) {
            sb.append(array[i]);
            if (i < array.length - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static String convertArrayToString(ArrayList<String> list, String delimiter) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i));
            if (i < list.size() - 1) {
                sb.append(delimiter);
            }
        }
        return sb.toString();
    }

    public static SentenceDetectorME getSentenceDetector() {
        try {
            // 使用ClassLoader加载模型文件
            InputStream modelIn = CitationProcessUtil.class.getResourceAsStream("/en-sentence.bin");
            if (modelIn == null) return null;
            SentenceModel model = new SentenceModel(modelIn);
            // 创建句子检测器
            SentenceDetectorME sentenceDetector = new SentenceDetectorME(model);
            modelIn.close();
            return sentenceDetector;
        } catch (Exception e) {
            logger.error("fail to load en model. " + e.getMessage());
            return null;
        }
    }

    public static List<String> getSpiltSentenceByStanford(String text) {
        ArrayList<String> res = new ArrayList<>();
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit");

        // 初始化Stanford CoreNLP管道
        StanfordCoreNLP pipeline = new StanfordCoreNLP(props);

        // 示例文本

        // 创建一个CoreDocument对象
        CoreDocument document = new CoreDocument(text);

        // 使用管道对文档进行注释
        pipeline.annotate(document);
        // 获取并打印分割的句子
        for (CoreSentence sentence : document.sentences()) {
            res.add(sentence.text().trim());
        }
        return res;
    }

    public static String authorsPartFeedback(String[] inputAuthorsArray, String[] standardAuthorsArray) {
        StringBuilder feedback = new StringBuilder();
        String spiltRules = "Check that you are properly separating authors with commas and connecting the last two authors with and or &.";
        // 遍历标准引用中的作者名，确认是否都在用户的输入中
        for (String standard : standardAuthorsArray) {
            boolean match = false;
            for (String input : inputAuthorsArray) {
                if (standard.equals(input)) {
                    match = true;
                    break;
                }
            }
            if (!match) feedback.append(standard).append(". is missed. ");
        }
        for (String input : inputAuthorsArray) {
            boolean match = false;
            for (String standard : standardAuthorsArray) {
                if (standard.equals(input)) {
                    match = true;
                    break;
                }
            }
            if (!match) feedback.append(input).append(". is not the author. ");
        }
        if (!feedback.isEmpty()) {
            //提醒分割规则
            feedback.append(spiltRules);
        } else if (inputAuthorsArray.length != standardAuthorsArray.length) {
            // 在作者名完全匹配的前提下，作者数量不相等，怀疑是名字重复
            feedback.append("The number of authors entered and the actual number of authors are not equal. Please check the input for duplication");
        } else {
            // 作者名匹配，数量相等，查看作者顺序是否能匹配
            for (int i = 0; i < inputAuthorsArray.length; i++) {
                if (!inputAuthorsArray[i].equals(standardAuthorsArray[i])) {
                    feedback.append("The order of authors is wrong！");
                    break;
                }
            }
        }
        return feedback.toString();
    }

    //获取标准引用中的组成成分和顺序
    public static ArrayList<String> getStandardComponent(Map<String, String> map) {
        ArrayList<String> res = new ArrayList<>();
        res.add("authorAndYear");
        res.add("tittle");
        if (!map.get("journalName").isEmpty()) res.add("journalName");
        else if (!map.get("conference").isEmpty()) res.add("conference");
        if (!map.get("volumeIssue").isEmpty()) res.add("volumeIssue");
        if (!map.get("pages").isEmpty()) res.add("pages");
        if (!map.get("publisher").isEmpty()) res.add("publisher");
        if (!map.get("urlOrDOI").isEmpty()) res.add("urlOrDOI");
        return res;
    }

    public static String preprocess(String text) {
        return text.replaceAll("doi\\.\\s*org", "doi\\.org");
    }

    public static void main(String[] args) {
        String newspaper1 = "Slapper, G., 2005. Corporate manslaughter: new issues for lawyers. The Times, 3, p.4b.";
        String ebook1 = "Burkholder, C. and Thompson, J., 2020. Fieldnotes in qualitative education and social science research. New York, NY: Routledge. https://doi. org/10.4324/9780429275821.";
        String book1 = "Boyes, W. ed., 2009. Instrumentation reference book. Butterworth-Heinemann.";
        String book2 = "Baier, C. and Katoen, J.P., 2008. Principles of model checking. MIT press.";
        String book3 = "Campbell, J., 2002. Reference and consciousness. Clarendon Press.";
        String book4 = "Borel, B., 2023. The Chicago guide to fact-checking. University of Chicago Press.";
        String conTest5 = "Brown, J., 2005, May. Evaluating surveys of transparent governance. In UNDESA (United Nations Department of Economic and Social Affairs), 6th Global forum on reinventing government: towards participatory and transparent governance. Seoul, Republic of Korea (pp. 24-27).";
        String conTest4 = "Willaert, P., Van den Bergh, J., Willems, J. and Deschoolmeester, D., 2007. The process-oriented organisation: a holistic view developing a framework for business process orientation maturity. In Business Process Management: 5th International Conference, BPM 2007, Brisbane, Australia, September 24-28, 2007. Proceedings 5 (pp. 1-15). Springer Berlin Heidelberg.";
        String conTest3 = "Doe, J., 2021, July. Analyzing big data trends. In Big Data Conference, New York. Tech Press.";
        String conTest2 = "Singh, A., Thakur, N. and Sharma, A., 2016, March. A review of supervised machine learning algorithms. In 2016 3rd international conference on computing for sustainable global development (INDIACom) (pp. 1310-1315). Ieee.";
        String conTest1 = "Alzubi, J., Nayyar, A. and Kumar, A., 2018, November. Machine learning from theory to algorithms: an overview. In Journal of physics: conference series (Vol. 1142, p. 012012). IOP Publishing.";
        String journal1 = "Ada, A.F., 2007. A Lifetime of Learning to Teach. Journal of Latinos & Education, [e-journal] 6 (2), pp.103-118. 10.1080/15348430701304658.";
        String journal3 = "Al-Benna, S., Rajgarhia, P., Ahmed, S. and Sheikh, Z., 2009. Accuracy of references in burns journals? Burns, 35(5), pp.677-680.";
        String journal4 = "Das, K. and Behera, R.N., 2017. A survey on machine learning: concept, algorithms and applications. International Journal of Innovative Research in Computer and Communication Engineering, 5(2), pp.1301-1309.";
        String journal2 = "Boon, S., Johnston, B. and Webber, S., 2007. A phenomenographic study of English faculty's conceptions of information literacy. Journal of Documentation, [e-journal] 63(2), pp.204-228. https://doi. org/10.1108/00220410710737187.";
        String journal2_change = "A phenomenographic study of English faculty's conceptions of information literacy. Journal of Documentation, [e-journal] 63(2), pp.204-228. https://doi. org/10.1108/00220410710737187.";
        List<String> total = Arrays.asList(journal1, journal2, journal3, journal4, conTest1, conTest2, conTest4, conTest5, ebook1, newspaper1, book1, book2, book3, book4);
//        for(String s:total){
//            spiltStandardHarvardCitation(s);
//        }
//        spiltStandardHarvardCitation(book2);
        String s=processUserHarvardCitation(journal2_change, spiltStandardHarvardCitation(journal2));
    }
}
