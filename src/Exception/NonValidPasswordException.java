package Exception;

/**
 * Eccezione sollevata quando la password è vuota o sbagliata
 */
public class NonValidPasswordException extends Exception{

    public NonValidPasswordException(){super();}

    public NonValidPasswordException(String s){super(s);}
}