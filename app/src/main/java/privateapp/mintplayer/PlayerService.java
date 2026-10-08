package privateapp.mintplayer;
import android.app.*;import android.content.*;import android.media.*;import android.media.session.*;import android.net.Uri;import android.os.*;import java.util.*;
public class PlayerService extends Service {
 public final ArrayList<Track> queue=new ArrayList<>(); public int index=-1,repeat=0; public boolean shuffle=false,ready=false;public String error="";
 public MediaPlayer player; private MediaSession session; private AudioManager audio;private AudioFocusRequest focus; private boolean resumeOnGain=false;
 public class LocalBinder extends Binder { public PlayerService get(){return PlayerService.this;} }
 public IBinder onBind(Intent i){return new LocalBinder();}
 public void onCreate(){super.onCreate();
  ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("play","음악 재생",NotificationManager.IMPORTANCE_LOW));
  audio=(AudioManager)getSystemService(AUDIO_SERVICE);
  focus=new AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN).setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build()).setOnAudioFocusChangeListener(c->{
   if(c==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT){resumeOnGain=playing();pause();}
   else if(c==AudioManager.AUDIOFOCUS_LOSS){resumeOnGain=false;pause();}
   else if(c==AudioManager.AUDIOFOCUS_GAIN){if(player!=null)player.setVolume(1,1);if(resumeOnGain){resumeOnGain=false;play();}}
   else if(c==AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK && player!=null)player.setVolume(.2f,.2f);
  }).build();
  session=new MediaSession(this,"Mint Player");session.setCallback(new MediaSession.Callback(){public void onPlay(){play();}public void onPause(){pause();}public void onSkipToNext(){next(true);}public void onSkipToPrevious(){previous();}public void onSeekTo(long p){seek((int)p);}});session.setActive(true);
  IntentFilter f=new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY);if(Build.VERSION.SDK_INT>=33)registerReceiver(noisy,f,Context.RECEIVER_NOT_EXPORTED);else registerReceiver(noisy,f);
 }
 private final BroadcastReceiver noisy=new BroadcastReceiver(){public void onReceive(Context c,Intent i){resumeOnGain=false;pause();}};
 public int onStartCommand(Intent i,int flags,int id){if(i!=null){String a=i.getAction();if("toggle".equals(a))toggle();if("next".equals(a))next(true);if("prev".equals(a))previous();}notifyState();return START_NOT_STICKY;}
 public Track current(){return index>=0&&index<queue.size()?queue.get(index):null;}
 public boolean playing(){try{return ready&&player!=null&&player.isPlaying();}catch(Exception e){return false;}}
 public int position(){try{return ready?player.getCurrentPosition():0;}catch(Exception e){return 0;}}
 public int duration(){try{return ready?player.getDuration():0;}catch(Exception e){return 0;}}
 public void start(ArrayList<Track> tracks,int at){queue.clear();queue.addAll(tracks);load(at);}
 private void load(int at){if(at<0||at>=queue.size())return;index=at;ready=false;error="";if(player!=null)player.release();player=new MediaPlayer();
  player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());
  player.setOnPreparedListener(p->{ready=true;play();});player.setOnCompletionListener(p->{if(repeat==2){seek(0);play();}else next(false);});
  player.setOnErrorListener((p,w,x)->{error="재생할 수 없는 파일입니다. 파일 권한과 코덱을 확인하세요.";ready=false;notifyState();return true;});
  try{player.setDataSource(this,Uri.parse(current().uri));player.prepareAsync();}catch(Exception e){error="파일을 열 수 없습니다. 다시 가져와 주세요.";}notifyState();
 }
 public void play(){if(!ready)return;if(audio.requestAudioFocus(focus)!=AudioManager.AUDIOFOCUS_REQUEST_GRANTED){error="다른 앱이 오디오를 사용 중입니다.";return;}player.start();error="";notifyState();}
 public void pause(){if(playing())player.pause();notifyState();}
 public void toggle(){if(playing()){resumeOnGain=false;pause();audio.abandonAudioFocusRequest(focus);}else play();}
 public void seek(int p){if(ready){player.seekTo(Math.max(0,Math.min(p,duration())));notifyState();}}
 public void previous(){if(position()>3000)seek(0);else if(!queue.isEmpty())load((index-1+queue.size())%queue.size());}
 public void next(boolean manual){if(queue.isEmpty())return;if(shuffle&&queue.size()>1){int n=new Random().nextInt(queue.size()-1);load(n>=index?n+1:n);return;}if(index+1<queue.size())load(index+1);else if(repeat==1||manual)load(0);else {pause();audio.abandonAudioFocusRequest(focus);}}
 private PendingIntent action(String a){return PendingIntent.getService(this,a.hashCode(),new Intent(this,PlayerService.class).setAction(a),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);}
 public void notifyState(){Track t=current();if(t==null)return;
  session.setMetadata(new MediaMetadata.Builder().putString(MediaMetadata.METADATA_KEY_TITLE,t.title).putString(MediaMetadata.METADATA_KEY_ARTIST,t.artist).putLong(MediaMetadata.METADATA_KEY_DURATION,duration()).build());
  session.setPlaybackState(new PlaybackState.Builder().setActions(PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_PLAY_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS|PlaybackState.ACTION_SEEK_TO).setState(playing()?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,position(),playing()?1:0).build());
  Notification n=new Notification.Builder(this,"play").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(t.title).setContentText(t.artist).setContentIntent(PendingIntent.getActivity(this,0,new Intent(this,MainActivity.class),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT)).setVisibility(Notification.VISIBILITY_PUBLIC).setOnlyAlertOnce(true).setOngoing(playing()).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_previous,"이전",action("prev")).build()).addAction(new Notification.Action.Builder(playing()?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,playing()?"일시정지":"재생",action("toggle")).build()).addAction(new Notification.Action.Builder(android.R.drawable.ic_media_next,"다음",action("next")).build()).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).build();
  if(playing())startForeground(1,n);else {((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).notify(1,n);stopForeground(false);}
 }
 public void onDestroy(){if(player!=null)player.release();session.release();audio.abandonAudioFocusRequest(focus);unregisterReceiver(noisy);((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).cancel(1);super.onDestroy();}
}
