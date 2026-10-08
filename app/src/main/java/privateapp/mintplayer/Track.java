package privateapp.mintplayer;
import org.json.*;
public class Track {
 public String uri,title,artist,album;
 public Track(String u,String t,String a,String b){uri=u;title=t;artist=a;album=b;}
 public JSONObject json() { JSONObject o=new JSONObject();try{o.put("uri",uri);o.put("title",title);o.put("artist",artist);o.put("album",album);}catch(Exception e){}return o; }
 public static Track read(JSONObject o){return new Track(o.optString("uri"),o.optString("title"),o.optString("artist"),o.optString("album"));}
}
