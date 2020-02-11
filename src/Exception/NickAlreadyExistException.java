package Exception;
/**
 * Eccezione sollevata quando un nickname è già presente nel database
 */
public class NickAlreadyExistException extends Exception {

    public NickAlreadyExistException(){super();}

    public NickAlreadyExistException(String s){super(s);}
}