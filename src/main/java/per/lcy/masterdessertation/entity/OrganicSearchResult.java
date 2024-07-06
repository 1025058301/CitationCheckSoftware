package per.lcy.masterdessertation.entity;

import lombok.Getter;
import lombok.Setter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import per.lcy.masterdessertation.utils.CitationProcessUtil;

import java.util.HashMap;
import java.util.Map;

@Getter
@Setter
public class OrganicSearchResult {
    private String tittle;
    private String result_id;
    private HashMap<String, String> citation;
    public static Logger logger = LoggerFactory.getLogger(OrganicSearchResult.class);

    public OrganicSearchResult(String tittle,String result_id){
        this.tittle=tittle;
        this.result_id=result_id;
    }
    public OrganicSearchResult(){
    }

    public String getCitation(String style) {
        if (citation == null) citation = new HashMap<>();
        if (!citation.containsKey(style)) {
            logger.warn("No this style of citation: " + style);
            return null;
        }
        return citation.get(style);
    }

    public void setCitation(String style, String value) {
        if (citation == null) {
            citation = new HashMap<>();
            citation.put(style, value);
        } else {
            citation.put(style, value);
        }
    }
}
