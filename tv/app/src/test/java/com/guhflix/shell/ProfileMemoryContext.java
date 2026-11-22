package com.guhflix.shell;
import android.content.*;
import java.util.*;
/** Android preferences backed by memory for real storage-path regression tests, never production data. */
public final class ProfileMemoryContext extends ContextWrapper {
 private final Map<String,SharedPreferences> files=new HashMap<>();
 public ProfileMemoryContext(){super(null);}
 @Override public Context getApplicationContext(){return this;}
 @Override public SharedPreferences getSharedPreferences(String name,int mode){return files.computeIfAbsent(name,k->new Memory());}
 static final class Memory implements SharedPreferences {
  final Map<String,Object> values=new HashMap<>();
  public Map<String,?> getAll(){return new HashMap<>(values);}
  public String getString(String key,String def){return (String)values.getOrDefault(key,def);}
  @SuppressWarnings("unchecked") public Set<String> getStringSet(String key,Set<String> def){return (Set<String>)values.getOrDefault(key,def);}
  public int getInt(String key,int def){return (Integer)values.getOrDefault(key,def);}
  public long getLong(String key,long def){return (Long)values.getOrDefault(key,def);}
  public float getFloat(String key,float def){return (Float)values.getOrDefault(key,def);}
  public boolean getBoolean(String key,boolean def){return (Boolean)values.getOrDefault(key,def);}
  public boolean contains(String key){return values.containsKey(key);}
  public void registerOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
  public void unregisterOnSharedPreferenceChangeListener(OnSharedPreferenceChangeListener l){}
  public Editor edit(){return new Editor(){
   final Map<String,Object> pending=new HashMap<>();final Set<String> removed=new HashSet<>();boolean clear;
   public Editor putString(String k,String v){if(v==null)return remove(k);pending.put(k,v);return this;}
   public Editor putStringSet(String k,Set<String> v){if(v==null)return remove(k);pending.put(k,new HashSet<>(v));return this;}
   public Editor putInt(String k,int v){pending.put(k,v);return this;}
   public Editor putLong(String k,long v){pending.put(k,v);return this;}
   public Editor putFloat(String k,float v){pending.put(k,v);return this;}
   public Editor putBoolean(String k,boolean v){pending.put(k,v);return this;}
   public Editor remove(String k){removed.add(k);pending.remove(k);return this;}
   public Editor clear(){clear=true;return this;}
   public boolean commit(){if(clear)values.clear();for(String k:removed)values.remove(k);values.putAll(pending);return true;}
   public void apply(){commit();}
  };}
 }
}
