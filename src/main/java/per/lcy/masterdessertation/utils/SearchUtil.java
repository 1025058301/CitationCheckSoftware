package per.lcy.masterdessertation.utils;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import per.lcy.masterdessertation.entity.CitationStyle;
import per.lcy.masterdessertation.entity.OrganicSearchResult;

public class SearchUtil {
    public static Logger logger = LoggerFactory.getLogger(SearchUtil.class);
    public static String searchApiKey = "fdbb219f94866719954a6bd413f9110bf86ed60bcf7e28515e2eb4211136008a";
    public static String searchResultUrl = "https://serpapi.com/search.json?engine=%s&q=%s&hl=en&api_key=%s";
    public static String searchCitationUrl = "https://serpapi.com/search.json?engine=%s&q=%s&api_key=%s";
    public static String engineScholar = "google_scholar";
    public static String engineScholarCite = "google_scholar_cite";

    public static OrganicSearchResult searchResultIdFromInfo(String queryInfo) {

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String encodedQueryInfo = URLEncoder.encode(queryInfo, StandardCharsets.UTF_8);
            String fullUrl = String.format(searchResultUrl, engineScholar, encodedQueryInfo, searchApiKey);
            HttpGet request = new HttpGet(fullUrl);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                // 打印整个JSON响应
                logger.info(jsonObject.toString());
                JsonArray organicResultsArray = jsonObject.getAsJsonArray("organic_results");
                JsonObject firstResult = organicResultsArray.get(0).getAsJsonObject();
                return new OrganicSearchResult(firstResult.get("title").getAsString(), firstResult.get("result_id").getAsString());
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            return null;
        }
    }

    public static boolean searchCitationFromResultId(OrganicSearchResult result) {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            String fullUrl = String.format(searchCitationUrl, engineScholarCite, result.getResult_id(), searchApiKey);
            HttpGet request = new HttpGet(fullUrl);
            try (CloseableHttpResponse response = httpClient.execute(request)) {
                String responseBody = EntityUtils.toString(response.getEntity());
                JsonObject jsonObject = JsonParser.parseString(responseBody).getAsJsonObject();
                JsonArray citations = jsonObject.getAsJsonArray("citations");
                for (int i = 0; i < citations.size(); i++) {
                    JsonObject citation = citations.get(i).getAsJsonObject();
                    result.setCitation(citation.get("title").getAsString(), citation.get("snippet").getAsString());
                }
                return true;
            }
        } catch (Exception e) {
            logger.error(e.getMessage());
            logger.error(Arrays.toString(e.getStackTrace()));
            return false;
        }
    }

    public static String searchCitationFromQueryInfo(String queryInfo, CitationStyle citationStyle) {
        OrganicSearchResult res = searchResultIdFromInfo(queryInfo);
        if (res == null||!searchCitationFromResultId(res)) return null;
        return res.getCitation(citationStyle.name());
    }


}
