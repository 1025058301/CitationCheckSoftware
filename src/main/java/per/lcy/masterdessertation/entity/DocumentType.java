package per.lcy.masterdessertation.entity;

public enum DocumentType {
    PDF("pdf",0),
    ODT("odt",1),
    WORD("word",2);
    private String type;

    private int value;

    DocumentType(String type,int value){
        this.type=type;
        this.value=value;
    }
}
