public class GameTimer extends Thread{

    public GameTimer(){}

    @Override
    public void run() {
        try {
            Thread.sleep(60000);
        }catch (InterruptedException ie){
            ie.printStackTrace();
        }
    }
}
