//! GUHFLIX authorization. The UI never holds the network mutex.
use serde_json::{json, Value};
use std::sync::{
    atomic::{AtomicBool, AtomicU64, Ordering},
    Arc, Mutex, OnceLock, Weak,
};
use std::time::{Duration, Instant};
const API: &str = "https://guhflix-marley-api.onrender.com";
const LEASE_MS: u64 = 105_000;
fn clock() -> u64 {
    static EPOCH: OnceLock<Instant> = OnceLock::new();
    EPOCH.get_or_init(Instant::now).elapsed().as_millis() as u64
}
#[derive(Clone, Default)]
pub struct Status {
    pub active: bool,
    pub busy: bool,
    pub message: String,
    pub username: String,
    pub playback_blocked: bool,
    pub sessions: Vec<Value>,
    pub expires: String,
}
#[derive(Debug)]
struct Error {
    status: u16,
    message: String,
}
struct Client {
    agent: ureq::Agent,
    access: String,
    refresh: String,
    installation: String,
    username: String,
    endpoint: String,
    persistent: bool,
}
pub struct Service {
    client: Mutex<Client>,
    pub status: Mutex<Status>,
    leases: Mutex<Vec<Weak<Lease>>>,
}
static SERVICE: OnceLock<Arc<Service>> = OnceLock::new();
fn session_path() -> std::path::PathBuf {
    crate::catalogo::pasta().join("guhflix-session.dpapi")
}
fn uid() -> String {
    // RandomState obtains process-local OS randomness; UUID bits/layout match the API.
    use std::hash::{BuildHasher, Hasher};
    static COUNTER: AtomicU64 = AtomicU64::new(0);
    let mut h = std::collections::hash_map::RandomState::new().build_hasher();
    h.write_u64(COUNTER.fetch_add(1, Ordering::SeqCst));
    let a = h.finish();
    h.write_u128(
        std::time::SystemTime::now()
            .duration_since(std::time::UNIX_EPOCH)
            .unwrap_or_default()
            .as_nanos(),
    );
    let b = h.finish();
    let mut bytes = [0u8; 16];
    bytes[..8].copy_from_slice(&a.to_le_bytes());
    bytes[8..].copy_from_slice(&b.to_le_bytes());
    bytes[6] = (bytes[6] & 15) | 64;
    bytes[8] = (bytes[8] & 63) | 128;
    format!("{:02x}{:02x}{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}-{:02x}{:02x}{:02x}{:02x}{:02x}{:02x}",bytes[0],bytes[1],bytes[2],bytes[3],bytes[4],bytes[5],bytes[6],bytes[7],bytes[8],bytes[9],bytes[10],bytes[11],bytes[12],bytes[13],bytes[14],bytes[15])
}
#[cfg(windows)]
fn protect(input: &[u8], encrypt: bool) -> Result<Vec<u8>, String> {
    use std::ffi::c_void;
    #[repr(C)]
    struct Blob {
        len: u32,
        data: *mut u8,
    }
    #[link(name = "crypt32")]
    unsafe extern "system" {
        fn CryptProtectData(
            data: *const Blob,
            desc: *const u16,
            entropy: *const Blob,
            reserved: *mut c_void,
            prompt: *mut c_void,
            flags: u32,
            out: *mut Blob,
        ) -> i32;
        fn CryptUnprotectData(
            data: *const Blob,
            desc: *mut *mut u16,
            entropy: *const Blob,
            reserved: *mut c_void,
            prompt: *mut c_void,
            flags: u32,
            out: *mut Blob,
        ) -> i32;
    }
    #[link(name = "kernel32")]
    unsafe extern "system" {
        fn LocalFree(p: *mut c_void) -> *mut c_void;
    }
    let src = Blob {
        len: input.len() as u32,
        data: input.as_ptr() as *mut u8,
    };
    let mut out = Blob {
        len: 0,
        data: std::ptr::null_mut(),
    };
    unsafe {
        let ok = if encrypt {
            CryptProtectData(
                &src,
                std::ptr::null(),
                std::ptr::null(),
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                1,
                &mut out,
            )
        } else {
            CryptUnprotectData(
                &src,
                std::ptr::null_mut(),
                std::ptr::null(),
                std::ptr::null_mut(),
                std::ptr::null_mut(),
                1,
                &mut out,
            )
        };
        if ok == 0 {
            return Err("Não foi possível proteger a sessão do Windows.".into());
        }
        let result = std::slice::from_raw_parts(out.data, out.len as usize).to_vec();
        LocalFree(out.data as *mut c_void);
        Ok(result)
    }
}
#[cfg(not(windows))]
fn protect(_input: &[u8], _encrypt: bool) -> Result<Vec<u8>, String> {
    Err("Persistência de sessão disponível apenas no Windows.".into())
}
fn configured_api() -> String {
    #[cfg(debug_assertions)]
    if let Ok(value) = std::env::var("GUHFLIX_TEST_API") {
        if let Some(port) = value.strip_prefix("http://127.0.0.1:") {
            if !port.is_empty() && port.chars().all(|c| c.is_ascii_digit()) {
                return value;
            }
        }
    }
    API.to_owned()
}
impl Client {
    fn new() -> Self {
        let path = crate::catalogo::pasta().join("guhflix-installation.txt");
        let installation = std::fs::read_to_string(&path)
            .ok()
            .filter(|s| s.len() == 36)
            .unwrap_or_else(|| {
                let id = uid();
                let _ = std::fs::write(path, &id);
                id
            });
        let refresh = std::fs::read(session_path())
            .ok()
            .and_then(|b| protect(&b, false).ok())
            .and_then(|b| String::from_utf8(b).ok())
            .unwrap_or_default();
        let agent = ureq::Agent::config_builder()
            .http_status_as_error(false)
            .timeout_global(Some(Duration::from_secs(8)))
            .build()
            .into();
        Self {
            agent,
            access: String::new(),
            refresh,
            installation,
            username: String::new(),
            endpoint: configured_api(),
            persistent: true,
        }
    }
    fn raw(
        &self,
        method: &str,
        path: &str,
        body: Option<Value>,
        token: &str,
    ) -> Result<Value, Error> {
        let url = format!("{}{path}", self.endpoint);
        let response = if method == "GET" {
            self.agent
                .get(&url)
                .header("Accept", "application/json")
                .header("Authorization", &format!("Bearer {token}"))
                .call()
        } else {
            self.agent
                .post(&url)
                .header("Accept", "application/json")
                .header("Authorization", &format!("Bearer {token}"))
                .header("Content-Type", "application/json")
                .send(body.unwrap_or_else(|| json!({})).to_string())
        };
        let response = response.map_err(|_| Error {
            status: 0,
            message: "Sem conexão com o servidor GUHFLIX. Tente novamente.".into(),
        })?;
        let status = response.status().as_u16();
        let data: Value = response
            .into_body()
            .with_config()
            .limit(262144)
            .read_json()
            .map_err(|_| Error {
                status: 0,
                message: "Servidor iniciando ou temporariamente indisponível. Tente novamente."
                    .into(),
            })?;
        if !(200..300).contains(&status) {
            return Err(Error {
                status,
                message: data["message"]
                    .as_str()
                    .unwrap_or("Serviço temporariamente indisponível.")
                    .to_owned(),
            });
        }
        Ok(data)
    }
    fn save(&mut self, data: &Value) -> Result<(), Error> {
        let access = data["accessToken"]
            .as_str()
            .filter(|s| !s.is_empty())
            .ok_or_else(|| Error {
                status: 0,
                message: "Resposta de acesso inválida.".into(),
            })?;
        let refresh = data["refreshToken"]
            .as_str()
            .filter(|s| !s.is_empty())
            .ok_or_else(|| Error {
                status: 0,
                message: "Resposta de sessão inválida.".into(),
            })?;
        #[cfg(windows)]
        if self.persistent {
            let encrypted = protect(refresh.as_bytes(), true).map_err(|m| Error {
                status: 0,
                message: m,
            })?;
            std::fs::write(session_path(), encrypted).map_err(|_| Error {
                status: 0,
                message: "Não foi possível salvar a sessão.".into(),
            })?;
        }
        self.access = access.to_owned();
        self.refresh = refresh.to_owned();
        self.username = data["user"]["username"].as_str().unwrap_or("").to_owned();
        Ok(())
    }
    fn renew(&mut self) -> Result<(), Error> {
        if self.refresh.is_empty() {
            return Err(Error {
                status: 401,
                message: "Entre com sua conta GUHFLIX.".into(),
            });
        }
        let data = self.raw(
            "POST",
            "/api/auth/refresh",
            Some(json!({"client":"tv","refreshToken":self.refresh})),
            "",
        );
        match data {
            Ok(data) => self.save(&data),
            Err(e) => {
                if e.status == 401 {
                    self.clear()
                }
                Err(e)
            }
        }
    }
    fn request(&mut self, method: &str, path: &str, body: Option<Value>) -> Result<Value, Error> {
        if self.access.is_empty() {
            self.renew()?
        }
        match self.raw(method, path, body.clone(), &self.access) {
            Err(e) if e.status == 401 => {
                self.access.clear();
                self.renew()?;
                self.raw(method, path, body, &self.access)
            }
            result => result,
        }
    }
    fn clear(&mut self) {
        self.access.clear();
        self.refresh.clear();
        let _ = std::fs::remove_file(session_path());
    }
}
pub fn service() -> &'static Arc<Service> {
    SERVICE.get_or_init(|| {
        let svc = Arc::new(Service {
            client: Mutex::new(Client::new()),
            status: Mutex::new(Status {
                message: "Verificando seu acesso…".into(),
                busy: true,
                ..Status::default()
            }),
            leases: Mutex::new(Vec::new()),
        });
        let runner = svc.clone();
        std::thread::spawn(move || {
            runner.validate();
            let mut last = Instant::now();
            loop {
                std::thread::sleep(Duration::from_secs(1));
                if last.elapsed() < Duration::from_secs(30) {
                    continue;
                }
                last = Instant::now();
                let leases: Vec<_> = {
                    let mut list = runner.leases.lock().unwrap();
                    list.retain(|p| p.strong_count() > 0);
                    list.iter().filter_map(Weak::upgrade).collect()
                };
                let mut playing = false;
                for lease in leases {
                    if !lease.playing.load(Ordering::SeqCst) {
                        continue;
                    }
                    playing = true;
                    let generation = lease.generation.load(Ordering::SeqCst);
                    let began = clock();
                    let mut client = runner.client.lock().unwrap();
                    if !lease.playing.load(Ordering::SeqCst)
                        || lease.generation.load(Ordering::SeqCst) != generation
                    {
                        continue;
                    }
                    let result = client.request(
                        "POST",
                        "/api/app/playback/heartbeat",
                        Some(json!({"playbackId":lease.id})),
                    );
                    if lease.generation.load(Ordering::SeqCst) != generation {
                        continue;
                    }
                    match result {
                        Ok(_) => {
                            lease.until.store(began + LEASE_MS, Ordering::SeqCst);
                        }
                        Err(error) => {
                            if error.status != 0 {
                                lease.until.store(0, Ordering::SeqCst);
                                runner.failure(error, true);
                            }
                        }
                    }
                }
                if !playing {
                    runner.validate();
                }
            }
        });
        svc
    })
}
impl Service {
    pub fn snapshot(&self) -> Status {
        self.status.lock().unwrap().clone()
    }
    fn invalidate(&self) {
        for l in self.leases.lock().unwrap().iter().filter_map(Weak::upgrade) {
            l.generation.fetch_add(1, Ordering::SeqCst);
            l.until.store(0, Ordering::SeqCst);
        }
    }
    fn failure(&self, error: Error, playback: bool) {
        let mut status = self.status.lock().unwrap();
        status.busy = false;
        status.message = error.message;
        if error.status == 401 || error.status == 403 {
            status.active = false;
        } else if playback {
            status.playback_blocked = true;
        }
        drop(status);
        if error.status == 401 || error.status == 403 {
            self.invalidate();
        }
    }
    fn validate(&self) {
        let (result, name) = {
            let mut client = self.client.lock().unwrap();
            let result = client.request("GET", "/api/app/access", None);
            (result, client.username.clone())
        };
        match result {
            Ok(data) => {
                let active = data["state"] == "active";
                let mut status = self.status.lock().unwrap();
                status.active = active;
                status.username = name;
                status.busy = false;
                status.expires = data["expiresAt"]
                    .as_str()
                    .and_then(|s| chrono::DateTime::parse_from_rfc3339(s).ok())
                    .map(|d| d.format("%d/%m/%Y").to_string())
                    .unwrap_or_default();
                status.message = if active {
                    "Assinatura ativa · até 3 reproduções simultâneas.".into()
                } else {
                    if data["state"] == "blocked" {
                        "Sua conta foi bloqueada. Contato: (11) 97073-1504".into()
                    } else {
                        "Seu acesso venceu. Renove com o atendimento: (11) 97073-1504".into()
                    }
                };
                drop(status);
                if !active {
                    self.invalidate();
                }
            }
            Err(error) => self.failure(error, false),
        }
    }
    pub fn check(self: &Arc<Self>) {
        self.status.lock().unwrap().busy = true;
        let svc = self.clone();
        std::thread::spawn(move || svc.validate());
    }
    pub fn login(self: &Arc<Self>, username: String, password: String) {
        self.status.lock().unwrap().busy = true;
        let svc = self.clone();
        std::thread::spawn(move || {
            let result = {
                let mut client = svc.client.lock().unwrap();
                let began = Instant::now();
                while client.raw("GET", "/health", None, "").is_err() {
                    if began.elapsed() > Duration::from_secs(90) {
                        svc.failure(
                            Error {
                                status: 0,
                                message:
                                    "O servidor não ficou disponível. Aguarde e tente novamente."
                                        .into(),
                            },
                            false,
                        );
                        return;
                    }
                    svc.status.lock().unwrap().message =
                        "Servidor iniciando. Aguarde; seus dados ainda não foram enviados.".into();
                    std::thread::sleep(Duration::from_secs(2));
                }
                let result=client.raw("POST","/api/auth/login",Some(json!({"username":username.trim().to_lowercase(),"password":password,"installationId":client.installation,"client":"tv","label":"GUHFLIX Windows"})),"");
                result.and_then(|data| client.save(&data))
            };
            match result {
                Ok(()) => {
                    svc.status.lock().unwrap().username = username;
                    svc.validate();
                }
                Err(error) => svc.failure(error, false),
            }
        });
    }
    pub fn logout(self: &Arc<Self>) {
        self.invalidate();
        {
            let mut status = self.status.lock().unwrap();
            status.active = false;
            status.playback_blocked = false;
            status.busy = true;
        }
        let svc = self.clone();
        std::thread::spawn(move || {
            let mut client = svc.client.lock().unwrap();
            let _ = client.request("POST", "/api/auth/logout", Some(json!({})));
            client.clear();
            let mut status = svc.status.lock().unwrap();
            *status = Status {
                message: "Entre com sua conta GUHFLIX.".into(),
                ..Status::default()
            };
        });
    }
    pub fn sessions(self: &Arc<Self>) {
        self.status.lock().unwrap().busy = true;
        let svc = self.clone();
        std::thread::spawn(move || {
            let result = svc
                .client
                .lock()
                .unwrap()
                .request("GET", "/api/app/sessions", None);
            match result {
                Ok(rows) => {
                    let mut s = svc.status.lock().unwrap();
                    s.busy = false;
                    s.sessions = rows.as_array().cloned().unwrap_or_default();
                }
                Err(e) => svc.failure(e, false),
            }
        });
    }
    pub fn revoke(self: &Arc<Self>, id: String) {
        self.status.lock().unwrap().busy = true;
        let svc = self.clone();
        std::thread::spawn(move || {
            let result = svc.client.lock().unwrap().request(
                "POST",
                &format!("/api/app/sessions/{id}/revoke"),
                Some(json!({})),
            );
            match result {
                Ok(_) => svc.sessions(),
                Err(e) => svc.failure(e, false),
            }
        });
    }
}
pub struct Lease {
    pub id: String,
    pub generation: AtomicU64,
    pub until: AtomicU64,
    pub playing: AtomicBool,
}
impl Lease {
    pub fn new() -> Arc<Self> {
        let lease = Arc::new(Self {
            id: uid(),
            generation: AtomicU64::new(0),
            until: AtomicU64::new(0),
            playing: AtomicBool::new(false),
        });
        service()
            .leases
            .lock()
            .unwrap()
            .push(Arc::downgrade(&lease));
        lease
    }
    pub fn expired(&self) -> bool {
        self.playing.load(Ordering::SeqCst) && clock() >= self.until.load(Ordering::SeqCst)
    }
    pub fn watchdog_stop(self: &Arc<Self>) {
        let svc = service();
        {
            let mut status = svc.status.lock().unwrap();
            if status.active {
                status.playback_blocked = true;
                status.message =
                    "A autorização de reprodução expirou. Confira a conexão e tente novamente."
                        .into();
            }
        }
        self.stop();
    }
    pub fn stop(self: &Arc<Self>) {
        let generation = self.generation.fetch_add(1, Ordering::SeqCst) + 1;
        self.playing.store(false, Ordering::SeqCst);
        self.until.store(0, Ordering::SeqCst);
        let lease = self.clone();
        std::thread::spawn(move || {
            let mut client = service().client.lock().unwrap();
            if lease.generation.load(Ordering::SeqCst) == generation {
                let _ = client.request(
                    "POST",
                    "/api/app/playback/stop",
                    Some(json!({"playbackId":lease.id})),
                );
            }
        });
    }
    /// Authorization and dispatch share the actor lock so cancellation cannot revoke a newer grant.
    pub fn load(
        self: &Arc<Self>,
        alive: Arc<Mutex<bool>>,
        dispatch: impl FnOnce() + Send + 'static,
    ) {
        let expected = self.generation.fetch_add(1, Ordering::SeqCst) + 1;
        let lease = self.clone();
        std::thread::spawn(move || {
            let mut client = service().client.lock().unwrap();
            if lease.generation.load(Ordering::SeqCst) != expected || !*alive.lock().unwrap() {
                return;
            }
            let began = clock();
            let result = client.request(
                "POST",
                "/api/app/playback/start",
                Some(json!({"playbackId":lease.id})),
            );
            let life = alive.lock().unwrap();
            if lease.generation.load(Ordering::SeqCst) != expected || !*life {
                let _ = client.request(
                    "POST",
                    "/api/app/playback/stop",
                    Some(json!({"playbackId":lease.id})),
                );
                return;
            }
            match result {
                Ok(_) => {
                    lease.until.store(began + LEASE_MS, Ordering::SeqCst);
                    lease.playing.store(true, Ordering::SeqCst);
                    dispatch();
                }
                Err(error) => {
                    lease.until.store(0, Ordering::SeqCst);
                    service().failure(error, true);
                }
            }
        });
    }
}
pub struct Gui {
    started: Instant,
    intro_done: bool,
    username: String,
    password: String,
    pub account: bool,
    pending_revoke: Option<String>,
}
impl Default for Gui {
    fn default() -> Self {
        Self {
            username: String::new(),
            password: String::new(),
            account: false,
            pending_revoke: None,
            started: Instant::now(),
            intro_done: false,
        }
    }
}
impl Gui {
    pub fn intro(&mut self, ui: &mut egui::Ui) -> bool {
        if self.intro_done {
            return false;
        }
        let t = self.started.elapsed().as_secs_f32() / 1.4;
        if t >= 1.0
            || ui.input(|i| {
                i.pointer.any_click()
                    || i.key_pressed(egui::Key::Escape)
                    || i.key_pressed(egui::Key::Enter)
            })
        {
            self.intro_done = true;
            return false;
        }
        let rect = ui.max_rect();
        let p = ui.painter();
        p.rect_filled(rect, 0.0, egui::Color32::from_rgb(8, 8, 8));
        let pulse = (1.0 - (t - 0.45).abs() * 1.4).clamp(0.0, 1.0);
        for i in 0..8 {
            let x = rect.right() - i as f32 * 35.0;
            p.line_segment(
                [
                    egui::pos2(x, rect.top()),
                    egui::pos2(x - 160.0, rect.bottom()),
                ],
                egui::Stroke::new(
                    3.0,
                    egui::Color32::from_rgba_unmultiplied(
                        229,
                        9,
                        20,
                        (pulse * (35 - i * 3) as f32) as u8,
                    ),
                ),
            );
        }
        let size = (rect.width() * 0.065).clamp(32.0, 80.0);
        let center = rect.center();
        p.text(
            egui::pos2(center.x - 5.0, center.y),
            egui::Align2::RIGHT_CENTER,
            "GUH",
            crate::tela::forte(size),
            egui::Color32::WHITE,
        );
        p.text(
            egui::pos2(center.x + 5.0, center.y),
            egui::Align2::LEFT_CENTER,
            "FLIX",
            crate::tela::forte(size),
            crate::tela::ACENTO,
        );
        p.line_segment(
            [
                egui::pos2(center.x - 140.0, center.y + size),
                egui::pos2(center.x - 140.0 + 280.0 * t, center.y + size),
            ],
            egui::Stroke::new(3.0, crate::tela::ACENTO),
        );
        ui.ctx().request_repaint();
        true
    }

    pub fn gate(&mut self, ui: &mut egui::Ui) -> bool {
        let svc = service();
        let status = svc.snapshot();
        if status.active && !status.playback_blocked {
            return false;
        }
        ui.ctx().request_repaint_after(Duration::from_millis(200));
        ui.vertical_centered(|ui| {
            ui.add_space((ui.available_height() * 0.15).max(15.));
            ui.allocate_ui(egui::vec2(260.0, 64.0), |ui| brand(ui));
            ui.add_space(18.);
            ui.heading(if status.playback_blocked {
                "Reprodução indisponível"
            } else {
                "Entre na sua conta"
            });
            ui.add_space(12.);
            ui.label(&status.message);
            ui.add_space(16.);
            ui.add_enabled_ui(!status.busy, |ui| {
                if !status.active {
                    ui.add_sized(
                        [350., 36.],
                        egui::TextEdit::singleline(&mut self.username)
                            .char_limit(40)
                            .hint_text("Usuário da assinatura"),
                    );
                    let password = ui.add_sized(
                        [350., 36.],
                        egui::TextEdit::singleline(&mut self.password)
                            .password(true)
                            .hint_text("Senha"),
                    );
                    if ui
                        .add_sized(
                            [350., 40.],
                            egui::Button::new("Entrar").fill(crate::tela::ACENTO),
                        )
                        .clicked()
                        || (password.lost_focus() && ui.input(|i| i.key_pressed(egui::Key::Enter)))
                    {
                        if !self.username.trim().is_empty() && !self.password.is_empty() {
                            svc.login(self.username.clone(), std::mem::take(&mut self.password));
                        }
                    }
                    if ui.button("Verificar acesso / renovação").clicked() {
                        svc.check();
                    }
                } else if ui.button("Voltar ao catálogo").clicked() {
                    svc.status.lock().unwrap().playback_blocked = false;
                }
                if ui.button("Gerenciar telas").clicked() {
                    self.account = true;
                    svc.sessions();
                }
                if ui.button("Sair da conta").clicked() {
                    self.password.clear();
                    svc.logout();
                }
            });
            if status.busy {
                ui.spinner();
            }
            ui.label("Até 3 reproduções simultâneas por conta · suporte: (11) 97073-1504");
        });
        self.account_window(ui.ctx());
        true
    }
    pub fn account_window(&mut self, ctx: &egui::Context) {
        let svc = service();
        let status = svc.snapshot();
        egui::Window::new("Conta GUHFLIX")
            .open(&mut self.account)
            .resizable(false)
            .show(ctx, |ui| {
                ui.label(format!("Usuário: {}", status.username));
                ui.label(&status.message);
                if !status.expires.is_empty() {
                    ui.label(format!(
                        "Válido até {} · R$ 30,00 / 30 dias",
                        status.expires
                    ));
                }
                ui.add_enabled_ui(!status.busy, |ui| {
                    if ui.button("Atualizar telas conectadas").clicked() {
                        svc.sessions();
                    }
                    for row in &status.sessions {
                        ui.horizontal(|ui| {
                            ui.label(row["label"].as_str().unwrap_or("Dispositivo"));
                            if ui.button("Encerrar sessão").clicked() {
                                if let Some(id) = row["id"].as_str() {
                                    self.pending_revoke = Some(id.to_owned());
                                }
                            }
                        });
                    }
                    if ui.button("Sair da conta").clicked() {
                        svc.logout();
                    }
                });
            });
        if let Some(id) = self.pending_revoke.clone() {
            egui::Window::new("Encerrar esta sessão?").collapsible(false).resizable(false).show(ctx,|ui|{ui.label("Todas as reproduções deste dispositivo serão encerradas e será necessário entrar novamente.");ui.horizontal(|ui|{if ui.button("Cancelar").clicked(){self.pending_revoke=None;}if ui.add(egui::Button::new("Encerrar sessão").fill(crate::tela::ACENTO)).clicked(){self.pending_revoke=None;svc.revoke(id.clone());}});});
        }
    }
}
pub fn brand(ui: &mut egui::Ui) {
    ui.horizontal(|ui| {
        ui.label(
            egui::RichText::new("GUH")
                .font(crate::tela::forte(40.0))
                .strong()
                .italics()
                .color(egui::Color32::WHITE),
        );
        ui.label(
            egui::RichText::new("FLIX")
                .font(crate::tela::forte(40.0))
                .strong()
                .italics()
                .color(crate::tela::ACENTO),
        );
    });
}
#[cfg(test)]
mod tests {
    use super::*;
    #[test]
    fn ids_match_api_contract() {
        let a = uid();
        let b = uid();
        assert_ne!(a, b);
        assert_eq!(a.len(), 36);
        assert_eq!(&a[14..15], "4");
        assert!(a.chars().all(|c| c.is_ascii_hexdigit() || c == '-'));
    }
    #[test]
    fn lease_watchdog_expires_without_network() {
        let lease = Lease {
            id: uid(),
            generation: AtomicU64::new(0),
            until: AtomicU64::new(0),
            playing: AtomicBool::new(true),
        };
        assert!(lease.expired());
        lease.until.store(clock() + 10000, Ordering::SeqCst);
        assert!(!lease.expired());
    }
    #[cfg(windows)]
    #[test]
    fn dpapi_round_trip() {
        let encrypted = protect(b"test-only-refresh", true).unwrap();
        assert_ne!(encrypted, b"test-only-refresh");
        assert_eq!(protect(&encrypted, false).unwrap(), b"test-only-refresh");
    }
}
#[cfg(test)]
mod client_tests {
    use super::*;
    use std::io::{Read, Write};
    fn fixture(responses: Vec<(u16, Value)>) -> (String, std::thread::JoinHandle<Vec<String>>) {
        fixture_gated(responses, None)
    }
    fn fixture_gated(
        responses: Vec<(u16, Value)>,
        gate: Option<(std::sync::mpsc::Sender<()>, Arc<std::sync::Barrier>)>,
    ) -> (String, std::thread::JoinHandle<Vec<String>>) {
        let socket = std::net::TcpListener::bind("127.0.0.1:0").unwrap();
        let address = format!("http://{}", socket.local_addr().unwrap());
        let handle = std::thread::spawn(move || {
            let mut paths = Vec::new();
            for (index, (status, body)) in responses.into_iter().enumerate() {
                let (mut stream, _) = socket.accept().unwrap();
                stream
                    .set_read_timeout(Some(Duration::from_secs(3)))
                    .unwrap();
                let mut bytes = Vec::new();
                let mut temp = [0u8; 1024];
                let mut end = None;
                loop {
                    let n = stream.read(&mut temp).unwrap();
                    assert!(n > 0);
                    bytes.extend_from_slice(&temp[..n]);
                    if let Some(i) = bytes.windows(4).position(|w| w == b"\r\n\r\n") {
                        let head = String::from_utf8_lossy(&bytes[..i]);
                        let len = head
                            .lines()
                            .find_map(|l| {
                                l.to_lowercase()
                                    .strip_prefix("content-length:")
                                    .map(|v| v.trim().parse::<usize>().unwrap())
                            })
                            .unwrap_or(0);
                        if bytes.len() >= i + 4 + len {
                            end = Some(i);
                            break;
                        }
                    }
                }
                let head = String::from_utf8_lossy(&bytes[..end.unwrap()]);
                paths.push(head.lines().next().unwrap().to_owned());
                if index == 0 {
                    if let Some((started, barrier)) = &gate {
                        started.send(()).unwrap();
                        barrier.wait();
                    }
                }
                let text = body.to_string();
                write!(stream,"HTTP/1.1 {status} Fixture\r\nContent-Type: application/json\r\nContent-Length: {}\r\nConnection: close\r\n\r\n{}",text.len(),text).unwrap();
            }
            paths
        });
        (address, handle)
    }
    fn client(endpoint: String) -> Client {
        Client {
            agent: ureq::Agent::config_builder()
                .http_status_as_error(false)
                .timeout_global(Some(Duration::from_secs(3)))
                .build()
                .into(),
            access: "old-access".into(),
            refresh: "test-only-refresh-token".into(),
            installation: uid(),
            username: "fixture".into(),
            endpoint,
            persistent: false,
        }
    }
    #[test]
    fn expired_access_refreshes_once_and_retries_with_new_session() {
        let (endpoint, server) = fixture(vec![
            (401, json!({"message":"expired"})),
            (
                200,
                json!({"accessToken":"new-access","refreshToken":"rotated-test-refresh","user":{"username":"fixture"}}),
            ),
            (200, json!({"state":"active"})),
        ]);
        let mut client = client(endpoint);
        let data = client.request("GET", "/api/app/access", None).unwrap();
        assert_eq!(data["state"], "active");
        assert_eq!(client.refresh, "rotated-test-refresh");
        assert_eq!(
            server.join().unwrap(),
            vec![
                "GET /api/app/access HTTP/1.1",
                "POST /api/auth/refresh HTTP/1.1",
                "GET /api/app/access HTTP/1.1"
            ]
        );
    }
    #[test]
    fn fourth_screen_is_not_retried_or_authorized() {
        let (endpoint, server) = fixture(vec![(
            409,
            json!({"error":"SCREEN_LIMIT","message":"Limite de 3 telas atingido"}),
        )]);
        let error = client(endpoint)
            .request(
                "POST",
                "/api/app/playback/start",
                Some(json!({"playbackId":uid()})),
            )
            .unwrap_err();
        assert_eq!(error.status, 409);
        assert!(error.message.contains('3'));
        assert_eq!(server.join().unwrap().len(), 1);
    }
    #[test]
    fn blocked_subscription_never_becomes_success() {
        let (endpoint, server) = fixture(vec![(
            403,
            json!({"error":"BLOCKED","message":"Acesso bloqueado"}),
        )]);
        let error = client(endpoint)
            .request(
                "POST",
                "/api/app/playback/start",
                Some(json!({"playbackId":uid()})),
            )
            .unwrap_err();
        assert_eq!(error.status, 403);
        assert_eq!(server.join().unwrap().len(), 1);
    }
    #[test]
    fn cancelled_load_cannot_start_or_revoke_newer_playback() {
        let (started, received) = std::sync::mpsc::channel();
        let barrier = Arc::new(std::sync::Barrier::new(2));
        let (endpoint, server) = fixture_gated(
            vec![
                (200, json!({"success":true})),
                (200, json!({"success":true})),
                (200, json!({"success":true})),
            ],
            Some((started, barrier.clone())),
        );
        let svc = Arc::new(Service {
            client: Mutex::new(client(endpoint)),
            status: Mutex::new(Status {
                active: true,
                ..Status::default()
            }),
            leases: Mutex::new(Vec::new()),
        });
        assert!(SERVICE.set(svc).is_ok());
        let lease = Arc::new(Lease {
            id: uid(),
            generation: AtomicU64::new(0),
            until: AtomicU64::new(0),
            playing: AtomicBool::new(false),
        });
        let alive = Arc::new(Mutex::new(true));
        let first = Arc::new(AtomicBool::new(false));
        let invoked = first.clone();
        lease.load(alive.clone(), move || {
            invoked.store(true, Ordering::SeqCst);
        });
        received.recv_timeout(Duration::from_secs(5)).unwrap();
        let (done, completed) = std::sync::mpsc::channel();
        lease.load(alive, move || {
            done.send(()).unwrap();
        });
        barrier.wait();
        completed.recv_timeout(Duration::from_secs(5)).unwrap();
        assert!(!first.load(Ordering::SeqCst));
        assert!(lease.playing.load(Ordering::SeqCst));
        assert!(!lease.expired());
        assert_eq!(
            server.join().unwrap(),
            vec![
                "POST /api/app/playback/start HTTP/1.1",
                "POST /api/app/playback/stop HTTP/1.1",
                "POST /api/app/playback/start HTTP/1.1"
            ]
        );
    }
}
#[cfg(windows)]
pub struct Instance(*mut std::ffi::c_void);
#[cfg(windows)]
impl Drop for Instance {
    fn drop(&mut self) {
        #[link(name = "kernel32")]
        unsafe extern "system" {
            fn CloseHandle(handle: *mut std::ffi::c_void) -> i32;
        }
        unsafe {
            CloseHandle(self.0);
        }
    }
}
#[cfg(windows)]
pub fn single_instance() -> Option<Instance> {
    use std::ffi::c_void;
    #[link(name = "kernel32")]
    unsafe extern "system" {
        fn CreateMutexW(attributes: *mut c_void, owner: i32, name: *const u16) -> *mut c_void;
        fn GetLastError() -> u32;
        fn CloseHandle(handle: *mut c_void) -> i32;
    }
    #[link(name = "user32")]
    unsafe extern "system" {
        fn MessageBoxW(window: *mut c_void, text: *const u16, title: *const u16, flags: u32)
            -> i32;
    }
    let name: Vec<u16> = "Local\\GUHFLIX-Windows"
        .encode_utf16()
        .chain(Some(0))
        .collect();
    unsafe {
        let handle = CreateMutexW(std::ptr::null_mut(), 0, name.as_ptr());
        let error = GetLastError();
        if handle.is_null() || error == 183 {
            if !handle.is_null() {
                CloseHandle(handle);
            }
            let text:Vec<u16>="O GUHFLIX já está aberto ou não foi possível reservar a sessão. Use a janela existente.".encode_utf16().chain(Some(0)).collect();
            let title: Vec<u16> = "GUHFLIX".encode_utf16().chain(Some(0)).collect();
            MessageBoxW(std::ptr::null_mut(), text.as_ptr(), title.as_ptr(), 0x40);
            None
        } else {
            Some(Instance(handle))
        }
    }
}
#[cfg(not(windows))]
pub struct Instance;
#[cfg(not(windows))]
pub fn single_instance() -> Option<Instance> {
    Some(Instance)
}
