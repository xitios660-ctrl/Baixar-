//! Related titles come from the authenticated catalog API and are cached by title ID.
use serde::Deserialize;
use std::{collections::HashMap,sync::{Mutex,OnceLock}};
#[derive(Clone,Debug,Deserialize,Default)]pub struct Item {pub title:String,pub kind:String,pub letter:String,#[serde(default)]pub year:String,#[serde(default)]pub poster:Option<String>,#[serde(default)]pub reason:Option<String>}
#[derive(Clone,Debug,Deserialize,Default)]pub struct Lista {#[serde(default)]pub label:String,#[serde(default)]pub items:Vec<Item>}
fn cache()-> &'static Mutex<HashMap<(u32,bool),Option<Lista>>>{static CACHE:OnceLock<Mutex<HashMap<(u32,bool),Option<Lista>>>>=OnceLock::new();CACHE.get_or_init(||Mutex::new(HashMap::new()))}
pub fn obter(id:u32,serie:bool)->Option<Lista>{
 if id==0{return None}let key=(id,serie);let mut map=cache().lock().unwrap();if let Some(v)=map.get(&key){return v.clone()}map.insert(key,None);drop(map);
 std::thread::spawn(move||{let kind=if serie{"series"}else{"movie"};let data=crate::guhflix::catalog_json(&format!("/api/connect/catalog-related/{kind}/{id}"));let mut parsed=data.and_then(|j|serde_json::from_value::<Lista>(j).ok()).unwrap_or_default();parsed.items.retain(|i|!i.title.is_empty()&&matches!(i.kind.as_str(),"movie"|"series")&&!i.letter.is_empty());parsed.items.truncate(20);cache().lock().unwrap().insert(key,Some(parsed));});None
}
