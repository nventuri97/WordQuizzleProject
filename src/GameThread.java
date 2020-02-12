import com.google.gson.Gson;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class GameThread extends Thread {
    private Database database;
    private String gamer1;
    private String gamer2;
    private int k;
    private Gson gson;
    private FileReader reader;
    private Socket sock1, sock2;

    public GameThread(Database db, String nick1, String nick2){
        this.database=db;
        this.gamer1=nick1;
        this.gamer2=nick2;
        this.gson=new Gson();
        try {
            this.reader = new FileReader("./src/dizionario.json");
        }catch(FileNotFoundException fe){
            fe.printStackTrace();
        }
        sock1=db.getSocket(gamer1);
        sock2=db.getSocket(gamer2);
    }

    @Override
    public void run(){
        //al massimo faccio tradurre 20 parole
        k=(int) (Math.random()*11)+1;
        ArrayList<String> dictionary=gson.fromJson(reader, ArrayList.class);
        ArrayList<String> kparole=new ArrayList<>(k);
        kparole=getKWord(dictionary, k);
        HashMap<String, String> traslation=new HashMap<>(k);
        if(getTranslation(traslation,kparole,k)){
            User us1=database.getUser(gamer1);
            User us2=database.getUser(gamer2);
            int pt1=game(us1, sock1, traslation, k);
            int pt2=game(us2, sock2, traslation, k);
            if(pt1>pt2){
                sendMessage("You won "+pt1+" to "+pt2+" Receive 3 bonus point",sock1);
                sendMessage("You lose "+pt1+" to "+pt2,sock2);
                pt1+=3;
            } else if(pt2>pt1){
                sendMessage("You won "+pt2+" to "+pt1+" Receive 3 bonus point",sock2);
                sendMessage("You lose "+pt2+" to "+pt1,sock1);
                pt2+=3;
            } else {
                sendMessage("You drew "+pt2+" to "+pt1,sock1);
                sendMessage("You drew "+pt2+" to "+pt1,sock2);
            }
            us1.addPunteggio(pt1);
            us2.addPunteggio(pt2);
        }
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

    public boolean getTranslation(HashMap<String, String> t, ArrayList<String> kparole, int k){
        for(int i=0;i<k;i++) {
            try {
                String word=kparole.get(i);
                URL site = new URL("https://api.mymemory.translated.net/get?q="+word+"&langpair=it|en");
                HttpURLConnection connection=(HttpURLConnection) site.openConnection();
                BufferedReader reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String translation=reader.readLine();
                if(connection.getResponseCode()!=200){
                    sendMessage("Something is gone wrong, we are sorry", sock1);
                    sendMessage("Something is gone wrong, we are sorry", sock2);
                    return false;
                }else
                    t.put(word, translation);
            }catch(Exception e){
                e.printStackTrace();
            }
        }
        return true;
    }

    public void sendMessage(String request, Socket socket){
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

    public int game(User user, Socket sock, HashMap<String, String> translation, int k){
        int punti=user.getPunteggio();
        String original,transl;
        for(Map.Entry<String,String> entry: translation.entrySet()){
            original=entry.getKey();
            sendMessage(original, sock);
            transl=receiveResponse(sock);
            if(transl.equals(entry.getValue()))
                punti++;
            else
                punti--;
        }
        return punti;
    }
}
