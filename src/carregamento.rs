//! Prazo de abertura conta a partir do comando enviado ao player, não da autorização.
use std::sync::{Mutex, atomic::{AtomicU64, Ordering}};
use std::time::{Duration, Instant};

#[derive(Default)]
pub struct Carregamento {
    pedido: AtomicU64,
    inicio: Mutex<Option<(u64, Instant)>>,
    erro: Mutex<Option<String>>,
}
impl Carregamento {
    pub fn solicitar(&self) -> u64 {
        let id = self.pedido.fetch_add(1, Ordering::SeqCst) + 1;
        *self.inicio.lock().unwrap() = None;
        *self.erro.lock().unwrap() = None;
        id
    }
    pub fn despachar(&self, id: u64) -> bool { self.iniciar_em(id, Instant::now()) }
    fn iniciar_em(&self, id: u64, agora: Instant) -> bool {
        let mut inicio = self.inicio.lock().unwrap();
        if self.pedido.load(Ordering::SeqCst) != id { return false; }
        *inicio = Some((id, agora));
        true
    }
    pub fn tempo(&self) -> Option<Duration> {
        let inicio = self.inicio.lock().unwrap();
        inicio.as_ref().filter(|(id, _)| *id == self.pedido.load(Ordering::SeqCst))
            .map(|(_, t)| t.elapsed())
    }
    pub fn falha(&self, id: u64, erro: String) {
        let mut destino = self.erro.lock().unwrap();
        if self.pedido.load(Ordering::SeqCst) == id { *destino = Some(erro); }
    }
    pub fn ultimo_erro(&self) -> Option<String> { self.erro.lock().unwrap().clone() }
    pub fn id(&self) -> u64 { self.pedido.load(Ordering::SeqCst) }
}

/// Apenas categorias de erro; nunca copia URLs, cabeçalhos ou tokens do player.
pub fn resumir_log(texto: &str) -> Option<String> {
    let texto = texto.to_ascii_lowercase();
    if texto.contains("http error") || texto.contains("server returned") || texto.contains("http/1.") {
        for code in [400,401,403,404,410,429,500,502,503,504] {
            if texto.split(|c: char| !c.is_ascii_digit()).any(|s| s == code.to_string()) {
                return Some(format!("Servidor de vídeo respondeu HTTP {code}."));
            }
        }
    }
    for (sinal, resumo) in [
        ("connection timed out", "Conexão com o vídeo excedeu o prazo."),
        ("connection refused", "O servidor de vídeo recusou a conexão."),
        ("certificate verify failed", "O certificado do servidor de vídeo não foi aceito."),
        ("failed to resolve", "Não foi possível localizar o servidor de vídeo."),
        ("invalid data found", "O servidor não retornou um vídeo ou playlist reconhecido."),
        ("failed to initialize decoder", "Não foi possível iniciar o decodificador."),
    ] {
        if texto.contains(sinal) { return Some(resumo.into()); }
    }
    None
}

#[cfg(test)]
mod testes {
    use super::*;
    #[test]
    fn autorizacao_e_fila_nao_consumem_prazo_da_fonte() {
        let c = Carregamento::default();
        let id = c.solicitar();
        let selecionado = Instant::now() - Duration::from_secs(18);
        assert!(selecionado.elapsed() > Duration::from_secs(14));
        assert_eq!(c.tempo(), None);
        // A autorização pode aguardar a rede: não existe relógio de mídia ainda.
        assert_eq!(c.tempo(), None);
        assert!(c.iniciar_em(id, Instant::now() - Duration::from_secs(15)));
        assert!(c.tempo().unwrap() >= Duration::from_secs(15));
    }
    #[test]
    fn retorno_atrasado_nao_inicia_relogio_de_outro_pedido() {
        let c = Carregamento::default();
        let antigo = c.solicitar();
        let novo = c.solicitar();
        assert!(!c.despachar(antigo));
        c.falha(antigo, "erro antigo".into());
        assert_eq!(c.tempo(), None);
        assert_eq!(c.ultimo_erro(), None);
        assert!(c.despachar(novo));
        assert!(c.tempo().is_some());
        c.solicitar(); // cancelamento/stop invalida o prazo anterior
        assert_eq!(c.tempo(), None);
    }
    #[test]
    fn diagnostico_nao_copia_endereco_ou_token_do_stream() {
        let resumo = resumir_log("HTTP error 403 Forbidden https://example.invalid/v?token=SECRETO").unwrap();
        assert_eq!(resumo, "Servidor de vídeo respondeu HTTP 403.");
        assert!(!resumo.contains("SECRETO"));
        assert_eq!(resumir_log("https://example.invalid/403?token=SECRETO"), None);
        assert_eq!(resumir_log("mensagem comum sem erro"), None);
    }
}
