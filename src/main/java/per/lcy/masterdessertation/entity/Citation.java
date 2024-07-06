package per.lcy.masterdessertation.entity;

import lombok.Getter;
import lombok.Setter;
@Getter
@Setter
public class Citation {
    private String[] authors;
    private String editor;
    private String editTime;
    private String publishTime;
    private String tittle;
    private String version;
    private String publisher;
    private String journalName;
    private String volumeIssue;
    private String pages;
    private String webUrlOrDOI;
    private String standardCitation;
    private CitationStyle citationStyle;
}
