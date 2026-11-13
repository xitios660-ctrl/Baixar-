import { useCallback, useEffect, useState } from 'react';
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  CircleDashed,
  Cloud,
  Database,
  ExternalLink,
  Film,
  HardDrive,
  LockKeyhole,
  RefreshCcw,
  Server,
  ShieldCheck,
  Tv,
} from 'lucide-react';

type EnvironmentStatus = {
  environment: 'homologacao';
  appdeploy: 'online';
  atlas: 'aguardando_credenciais' | 'conectado' | 'indisponivel';
  jwtSecretConfigured: boolean;
  database: string;
  collectionsReady: boolean;
  productionTrafficMigrated: boolean;
  productionAltered: boolean;
};

const milestones = [
  {
    name: 'Código original preservado',
    detail: 'Branch independente, sem alterar as instalações atuais',
    done: true,
  },
  {
    name: 'Banco de dados identificado',
    detail: 'Project 0 · Cluster0 · guhflix',
    done: true,
  },
  {
    name: 'Conexão segura com o Atlas',
    detail: 'Aguardando configurar os segredos do servidor',
    done: false,
  },
  {
    name: 'Autenticação e painel compatíveis',
    detail: 'Próxima fase: portar login, clientes e assinaturas',
    done: false,
  },
  {
    name: 'Testes reais na televisão',
    detail: 'Só trocar os endereços depois de validar a TCL',
    done: false,
  },
];

function Property({
  icon: Icon,
  label,
  value,
  tone,
  description,
}: {
  icon: typeof Database;
  label: string;
  value: string;
  tone?: 'good' | 'warn' | 'idle';
  description: string;
}) {
  return (
    <article className="property">
      <div className="property-top">
        <div className="property-icon"><Icon size={20} /></div>
        <span className={`status-icon ${tone || 'idle'}`}>
          {tone === 'good' ? <CheckCircle2 size={17} /> : <CircleDashed size={17} />}
        </span>
      </div>
      <p className="property-title">{label}</p>
      <strong>{value}</strong>
      <p className="property-description">{description}</p>
    </article>
  );
}

function App() {
  const [status, setStatus] = useState<EnvironmentStatus | null>(null);
  const [checking, setChecking] = useState(false);
  const [error, setError] = useState('');

  const reload = useCallback(async () => {
    setChecking(true);
    setError('');
    try {
      const { api } = await import('@appdeploy/client');
      const response = await api.get('/api/migration/status');
      setStatus(response.data as EnvironmentStatus);
    } catch {
      setStatus(null);
      setError('Não foi possível consultar o backend novo. Confira a conexão e tente novamente.');
    } finally {
      setChecking(false);
    }
  }, []);

  useEffect(() => {
    void reload();
  }, [reload]);

  const atlasConnected = status?.atlas === 'conectado';
  const fullSchema = Boolean(atlasConnected && status?.collectionsReady);

  return (
    <div className="shell">
      <aside className="sidebar">
        <a className="brand" href="#inicio" aria-label="Guhflix início">
          <span className="brand-mark">G</span>
          <span><strong>GUHFLIX</strong><small>PLATFORM</small></span>
        </a>
        <div className="sidebar-section">
          <span className="eyebrow">WORKSPACE</span>
          <a className="side-link selected" href="#inicio"><Server size={17} /> Visão geral</a>
          <a className="side-link" href="#conexoes"><Database size={17} /> Conexões</a>
          <a className="side-link" href="#etapas"><HardDrive size={17} /> Migração</a>
          <a className="side-link" href="#regras"><ShieldCheck size={17} /> Regras preservadas</a>
        </div>
        <div className="sidebar-bottom">
          <div className="sidebar-flag"><span className="pulse-dot" /> Ambiente de homologação</div>
          <p>Sem operações em clientes, cobranças ou reproduções reais.</p>
        </div>
      </aside>

      <main className="main" id="inicio">
        <header className="topbar">
          <span className="breadcrumb">Guhflix <span>/</span> Infraestrutura <span>/</span> Migração</span>
          <span className="topbar-env"><span className="pulse-dot" /> STAGING</span>
        </header>

        <section className="hero">
          <div className="hero-film" aria-hidden="true"><Film size={170} strokeWidth={0.7} /></div>
          <div className="hero-overlay">
            <span className="hero-kicker"><span /> PRÓXIMA GERAÇÃO</span>
            <h1>O futuro do <em>GUHFLIX</em><br />está sendo preparado.</h1>
            <p>Nova infraestrutura, preservando o banco de clientes, as assinaturas e o limite de três reproduções simultâneas.</p>
            <div className="hero-actions">
              <a className="primary-button" href="#conexoes">Ver diagnóstico <ArrowRight size={17} /></a>
              <a className="secondary-button" href="#etapas">Ver etapas</a>
            </div>
          </div>
        </section>

        <div className="section-head" id="conexoes">
          <div>
            <span className="eyebrow">DIAGNÓSTICO ATUAL</span>
            <h2>Infraestrutura</h2>
          </div>
          <button className="reload" type="button" disabled={checking} onClick={() => { void reload(); }}>
            <RefreshCcw size={16} className={checking ? 'spinning' : ''} />
            {checking ? 'Verificando...' : 'Atualizar diagnóstico'}
          </button>
        </div>

        {error && (
          <div className="error-banner" role="alert">
            <AlertTriangle size={19} />
            <span>{error}</span>
          </div>
        )}

        <div className="properties">
          <Property
            icon={Cloud}
            label="Ambiente AppDeploy"
            value={status ? 'Servidor online' : checking ? 'Verificando...' : 'Não verificado'}
            tone={status ? 'good' : 'idle'}
            description="Nova hospedagem, independente dos serviços suspensos no Render."
          />
          <Property
            icon={Database}
            label="MongoDB Atlas"
            value={atlasConnected ? 'Banco conectado' : status?.atlas === 'indisponivel' ? 'Conexão indisponível' : 'Aguardando conexão'}
            tone={atlasConnected ? 'good' : 'warn'}
            description={fullSchema ? 'Coleções essenciais encontradas no banco guhflix.' : 'Os dados originais não são alterados neste diagnóstico.'}
          />
          <Property
            icon={LockKeyhole}
            label="Segredo de autenticação"
            value={status?.jwtSecretConfigured ? 'Configurado' : 'Pendente'}
            tone={status?.jwtSecretConfigured ? 'good' : 'warn'}
            description="A chave JWT nunca é enviada para o navegador."
          />
        </div>

        <div className="notice" role="note">
          <AlertTriangle size={21} />
          <div>
            <strong>Os serviços antigos estão suspensos por cobrança.</strong>
            <p>O serviço de API e o painel no Render estão suspensos. Esta versão não os utiliza como proxy. A substituição completa depende de portar a API e validar o aplicativo de TV.</p>
          </div>
        </div>

        <div className="section-head" id="etapas">
          <div>
            <span className="eyebrow">ROADMAP</span>
            <h2>Etapas da migração</h2>
          </div>
          <span className="meta-caption">Sem troca de produção</span>
        </div>

        <section className="roadmap">
          {milestones.map((item, index) => {
            const isReady = item.done || (index === 2 && fullSchema && status?.jwtSecretConfigured);
            return (
              <div className="milestone" key={item.name}>
                <div className={`milestone-number ${isReady ? 'complete' : ''}`}>
                  {isReady ? <CheckCircle2 size={18} /> : String(index + 1).padStart(2, '0')}
                </div>
                <div className="milestone-description">
                  <strong>{item.name}</strong>
                  <p>{item.detail}</p>
                </div>
                <span className={`milestone-state ${isReady ? 'complete' : ''}`}>
                  {isReady ? 'Verificado' : 'Pendente'}
                </span>
              </div>
            );
          })}
        </section>

        <div className="section-head" id="regras">
          <div>
            <span className="eyebrow">PROTEÇÃO DE DADOS</span>
            <h2>O que será mantido</h2>
          </div>
        </div>

        <section className="feature-grid">
          <article><span className="feature-icon"><ShieldCheck size={23} /></span><strong>Clientes e assinaturas</strong><p>Preservar cadastros, vencimentos, pagamentos e histórico real.</p></article>
          <article><span className="feature-icon"><Tv size={23} /></span><strong>Três telas simultâneas</strong><p>Controle de reprodução no servidor com expiração de sessão.</p></article>
          <article><span className="feature-icon"><LockKeyhole size={23} /></span><strong>Login protegido</strong><p>Sem compartilhar senhas, hashes ou credenciais com o navegador.</p></article>
        </section>

        <div className="footer">
          <span>© GUHFLIX · Homologação técnica</span>
          <a href="https://github.com/xitios660-ctrl/Baixar-/tree/guhflix-appdeploy-staging/appdeploy" target="_blank" rel="noopener noreferrer">
            Código da migração <ExternalLink size={14} />
          </a>
        </div>
      </main>
    </div>
  );
}

export default App;
