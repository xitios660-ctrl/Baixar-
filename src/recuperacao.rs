//! Interrompe um desenho com falha sem navegar nem repetir a falha a cada quadro.
#[derive(Default)]
pub struct Protecao {
    pendente: bool,
}

impl Protecao {
    pub fn pendente(&self) -> bool { self.pendente }

    pub fn tentar_novamente(&mut self) { self.pendente = false; }

    /// Só o usuário libera uma nova tentativa depois de uma falha.
    /// Não recebe nem altera a navegação ou o player.
    pub fn desenhar(&mut self, desenho: impl FnOnce()) -> bool {
        if self.pendente { return false; }
        if std::panic::catch_unwind(std::panic::AssertUnwindSafe(desenho)).is_err() {
            self.pendente = true;
            return false;
        }
        true
    }
}

#[cfg(test)]
mod testes {
    use super::*;

    #[test]
    fn falha_na_rolagem_preserva_catalogo_e_nao_repete_automaticamente() {
        let mut protecao = Protecao::default();
        let mut tela = "filmes";
        let mut tentativas = 0;
        assert!(!protecao.desenhar(|| {
            tentativas += 1;
            panic!("falha simulada durante a rolagem");
        }));
        assert!(protecao.pendente());
        // Mesmo que um quadro seja solicitado, não executa navegação/player
        // nem entra novamente no desenho quebrado enquanto aguarda o usuário.
        assert!(!protecao.desenhar(|| { tela = "ao vivo"; tentativas += 1; }));
        assert_eq!(tela, "filmes");
        assert_eq!(tentativas, 1);
        protecao.tentar_novamente();
        assert!(protecao.desenhar(|| { tentativas += 1; }));
        assert_eq!(tentativas, 2);
        assert_eq!(tela, "filmes");
    }

    #[test]
    fn falhas_repetidas_apos_tentativas_nao_encerram_o_processo() {
        let mut protecao = Protecao::default();
        for _ in 0..5 {
            protecao.tentar_novamente();
            assert!(!protecao.desenhar(|| panic!("nova falha")));
            assert!(protecao.pendente());
        }
    }
}
