import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;

public class GameThread extends Thread {
    private Database database;
    private String gamer1;
    private String gamer2;
    private int k;
    private Gson gson;
    private FileReader reader;

    public GameThread(Database db, String nick1, String nick2){
        this.database=db;
        this.gamer1=nick1;
        this.gamer2=nick2;
        this.gson=new Gson();
        try {
            this.reader = new FileReader("dizionario.json");
        }catch(FileNotFoundException fe){
            fe.printStackTrace();
        }
    }

    @Override
    public void run(){
        //al massimo faccio tradurre 20 parole
        k=(int) (Math.random()*19)+1;
        ArrayList<String> dictionary=gson.fromJson(reader, ArrayList.class);
        ArrayList<String> kparole=new ArrayList<>(k);
        kparole=getKWord(dictionary, k);
        HashMap<String, String> traslation=new HashMap<>(k);
    }

    public ArrayList<String> getKWord(ArrayList<String> dic, int k){
        ArrayList<String> s=new ArrayList<>(k);
        int i;
        int len=dic.size();
        for(i=0;i<k;i++){
            int rand=(int) (Math.random()*(len-1));
            s.add(dic.get(rand));
        }
        return s;
    }

    public void getTranslation(HashMap<String, String> t, ArrayList<String> kparole, int k){
        for(int i=0;i<k;i++) {
            try {
                String word=kparole.get(i);
                URL site = new URL("https://api.mymemory.translated.net/get?q="+word+"&langpair=it|en");
                HttpURLConnection connection=(HttpURLConnection) site.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String translation=reader.readLine();
                t.put(word, translation);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
    }

    public void sendRequest(String request, Socket socket){
        try {
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
            writer.write(request);
            writer.newLine();
            writer.flush();
        }catch(IOException ioe){
            ioe.printStackTrace();
        }
    }

    public String receiveResponse(Socket socket){
        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            return reader.readLine();
        }catch (IOException ioe){
            ioe.printStackTrace();
        }
        return null;
    }
}
