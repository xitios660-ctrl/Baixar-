//! Reconnection timer: bounded backoff, cancellable by leaving the selected title.
use std::time::{Duration,Instant};
#[derive(Clone,Debug,Default)]
pub struct Reconexao { ciclos:u32, proxima:Option<Instant> }
impl Reconexao {
 pub fn agendar(&mut self,agora:Instant)->Duration {let delay=Duration::from_secs((5u64<<self.ciclos.min(3)).min(30));self.ciclos=self.ciclos.saturating_add(1);self.proxima=Some(agora+delay);delay}
 pub fn pronta(&self,agora:Instant)->bool {self.proxima.is_some_and(|t|agora>=t)}
 pub fn consumir(&mut self){self.proxima=None}
 pub fn segundos(&self)->Option<u64>{self.proxima.map(|t|t.saturating_duration_since(Instant::now()).as_secs()+1)}
}
#[cfg(test)]mod tests{use super::*;
 #[test]fn atraso_cresce_sem_loop_apertado(){let now=Instant::now();let mut r=Reconexao::default();assert_eq!(r.agendar(now).as_secs(),5);assert!(!r.pronta(now));assert!(r.pronta(now+Duration::from_secs(5)));assert_eq!(r.agendar(now).as_secs(),10);for _ in 0..20{r.agendar(now);}assert!(!r.pronta(now+Duration::from_secs(29)));assert!(r.pronta(now+Duration::from_secs(30)))}
 #[test]fn consumir_cancela_o_prazo_antigo(){let now=Instant::now();let mut r=Reconexao::default();r.agendar(now);r.consumir();assert!(!r.pronta(now+Duration::from_secs(100)));assert_eq!(r.segundos(),None)}
}
