package Exception;

/**
 * Eccezione sollevata quando si tenta di aggiungere un amico già presente nella cerchia
 */
public class FriendshipException extends Exception{
    public FriendshipException(){super();}

    public FriendshipException(String s){super(s);}
}