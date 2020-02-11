public class GameThread extends Thread {
    private Database database;
    private String gamer1;
    private String gamer2;

    public GameThread(Database db, String nick1, String nick2){
        this.database=db;
        this.gamer1=nick1;
        this.gamer2=nick2;
    }

    @Override
    public void run(){

    }
}
