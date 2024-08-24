package per.lcy.masterdessertation.entity;

public class CustomException extends Exception{
    private String errorMessage;
    public CustomException(String errorMessage){
        super(errorMessage);
        this.errorMessage=errorMessage;
    }
    public String getErrorMessage(){
        return errorMessage;
    }

}
