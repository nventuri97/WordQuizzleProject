package Exception;

/**
 * Eccezione sollevata quando un utente è già loggato
 */
public class IsLoggedException extends Exception{

    public IsLoggedException(){super();}

    public IsLoggedException(String s){super(s);}
}