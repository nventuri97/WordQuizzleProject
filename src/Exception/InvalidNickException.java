package Exception;

/**
 * Eccezione sollevata quando il nick inserito non è presente nel database
 */
public class InvalidNickException extends Exception{

    public InvalidNickException(){super();}

    public InvalidNickException(String s){super(s);}
}