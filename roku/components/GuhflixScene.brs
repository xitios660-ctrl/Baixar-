sub init()
 m.top.backgroundColor = "#080808"
 m.top.backgroundUri = ""
 m.nav = m.top.findNode("nav")
 m.nav.buttons = ["Para você", "Filmes", "Séries", "Buscar", "Mais", "Conta"]
 m.nav.observeField("buttonSelected", "navigation")
 m.catalog = m.top.findNode("catalog")
 m.catalog.observeField("rowItemSelected", "titleSelected")
 m.actions = m.top.findNode("actions")
 m.actions.observeField("buttonSelected", "detailAction")
 m.episodes = m.top.findNode("episodes")
 m.episodes.observeField("itemSelected", "episodeSelected")
 m.video = m.top.findNode("video")
 m.video.observeField("state", "videoState")
 m.top.findNode("introTimer").observeField("fire", "finishIntro")
 m.top.findNode("heartbeat").observeField("fire", "sendHeartbeat")
 m.top.findNode("poll").observeField("fire", "pollTv")
 m.top.findNode("retry").observeField("fire", "playSource")
 m.top.findNode("watchdog").observeField("fire", "retrySource")
 m.queue = []
 m.busy = false
 m.access = ""
 m.username = ""
 m.grant = ""
 m.activeItem = invalid
 m.sources = []
 m.sourceIndex = 0
 m.retryCycle = 0
 m.lastCommand = ""
 m.catalogPath = "/api/connect/catalog?"
 m.offset = 0
 m.total = 0
 m.wantsVideo = false
 m.retryPending = false
 m.position = 0
 m.savedPosition = 0
 m.introFinished = false
 m.top.findNode("opening").control = "start"
 m.top.findNode("introTimer").control = "start"
 send("restore", "", "GET", {})
end sub
sub status(text as String)
 m.top.findNode("status").text = text
end sub
sub finishIntro()
 m.top.findNode("intro").visible = false
 m.introFinished = true
 if m.access = "" then showLogin() else m.nav.setFocus(true)
end sub
sub send(tag as String, path as String, method as String, body as Object, extra = invalid as Dynamic)
 if tag = "poll" or tag = "heartbeat"
  for each pending in m.queue
   if pending.tag = tag then return
  end for
 end if
 job = {tag: tag, path: path, method: method, body: body, access: m.access}
 if extra <> invalid then job.Append(extra)
 m.queue.Push(job)
 pump()
end sub
sub pump()
 if m.busy or m.queue.Count() = 0 then return
 m.busy = true
 m.task = m.top.CreateChild("ApiTask")
 m.task.request = m.queue.Shift()
 m.task.observeField("state", "taskDone")
 m.task.control = "RUN"
end sub
sub taskDone(event as Object)
 if event.GetData() <> "done" and event.GetData() <> "stop" then return
 result = m.task.response
 m.top.RemoveChild(m.task)
 m.task = invalid
 m.busy = false
 if result <> invalid then handleResult(result)
 pump()
end sub
sub handleResult(result as Object)
 if result.auth <> invalid then m.access = result.auth.accessToken
 tag = result.tag
 if tag = "logout"
  m.access = ""
  m.queue = []
  m.top.findNode("poll").control = "stop"
  showLogin()
  return
 end if
 if result.status < 200 or result.status >= 300
  if tag = "restore"
   if m.introFinished then showLogin()
  else if tag = "watch"
   status("Não foi possível iniciar o vídeo. " + message(result))
  else if tag = "heartbeat"
   closeVideo()
   status(message(result))
  else if tag <> "poll" and tag <> "stop" and tag <> "ack"
   status(message(result))
  end if
  return
 end if
 data = result.data
 if tag = "login" or tag = "restore"
  m.access = data.accessToken
  m.username = data.user.username
  m.nav.buttons = ["Para você", "Filmes", "Séries", "Buscar", "Mais", "Conta"]
  status("Sua conta: " + m.username + " · Preparando o catálogo…")
  send("home", "/api/connect/home", "GET", {})
  m.top.findNode("poll").control = "start"
 else if tag = "home"
  showRows(data.rows)
  if data.featured <> invalid then status(data.featured.title)
 else if tag = "catalog"
  m.total = data.total
  m.offset += data.items.Count()
  showRows([{title: "Seu catálogo GUHFLIX", items: data.items}])
  status(data.total.ToStr() + " títulos · Use as setas para escolher")
 else if tag = "detail"
  m.activeItem = data
  m.top.findNode("detail").visible = true
  m.top.findNode("detailTitle").text = data.title
  m.top.findNode("detailPoster").uri = safeText(data.poster)
  m.top.findNode("synopsis").text = safeText(data.synopsis)
  if data.kind = "series"
   m.actions.buttons = ["Escolher episódio", "Recomendações", "Voltar"]
  else
   m.actions.buttons = ["Assistir", "Recomendações", "Voltar"]
  end if
  if m.remotePlay <> invalid
   command = m.remotePlay
   m.remotePlay = invalid
   watchItem(data.id, command.season, command.episode)
  else
   m.actions.setFocus(true)
  end if
 else if tag = "related"
  m.top.findNode("detail").visible = false
  showRows([{title: data.label, items: data.items}])
 else if tag = "watch"
  if not m.wantsVideo or result.playbackId <> m.expectedPlaybackId
   send("stop", "/api/connect/watch/" + data.grant + "/stop", "POST", {})
   return
  end if
  m.grant = data.grant
  m.sources = data.sources
  m.sourceIndex = 0
  m.retryCycle = 0
  if result.seek <> invalid then m.savedPosition = result.seek else m.savedPosition = 0
  m.top.findNode("heartbeat").control = "start"
  playSource()
 else if tag = "pair"
  dialog = CreateObject("roSGNode", "Dialog")
  dialog.title = "Conecte seu celular ao GUHFLIX"
  dialog.message = "Código: " + data.code + chr(10) + "Abra guhflix-marley-api.onrender.com/pwa/ no celular, entre na mesma conta e escolha Conectar TV."
  dialog.buttons = ["Entendi"]
  dialog.observeField("buttonSelected", "closeDialog")
  m.top.dialog = dialog
 else if tag = "poll" and data.command <> invalid
  received = data.command
  if received.id <> m.lastCommand
   m.lastCommand = received.id
   performCommand(received.command)
  end if
  send("ack", "/api/connect/tv/commands/" + received.id + "/ack", "POST", {success: true, message: "Comando recebido pelo GUHFLIX Roku"})
 else if tag = "logout"
  m.access = ""
  m.queue = []
  m.top.findNode("poll").control = "stop"
  showLogin()
 end if
end sub
function message(result as Object) as String
 if result.data <> invalid and result.data.message <> invalid then return result.data.message
 return "Confira a conexão e tente novamente."
end function
function safeText(value as Dynamic) as String
 if value = invalid then return ""
 return value
end function
sub showLogin()
 m.catalog.visible = false
 m.top.findNode("detail").visible = false
 m.nav.buttons = ["Entrar na conta"]
 status("Use seu acesso de cliente ativo do GUHFLIX.")
 m.nav.setFocus(true)
end sub
sub keyboard(kind as String, title as String)
 m.keyboardKind = kind
 dialog = CreateObject("roSGNode", "KeyboardDialog")
 dialog.title = title
 dialog.buttons = ["Continuar", "Cancelar"]
 if kind = "password" then dialog.keyboard.textEditBox.secureMode = true
 dialog.observeField("buttonSelected", "keyboardDone")
 m.top.dialog = dialog
end sub
sub keyboardDone(event as Object)
 dialog = m.top.dialog
 if event.GetData() <> 0
  dialog.close = true
  return
 end if
 value = dialog.text
 dialog.close = true
 if m.keyboardKind = "username"
  m.loginUsername = value.Trim()
  keyboard("password", "Senha do seu acesso GUHFLIX")
 else if m.keyboardKind = "password"
  status("Conectando sua conta…")
  send("login", "/api/auth/login", "POST", {username: m.loginUsername, password: value})
 else
  escaped = CreateObject("roUrlTransfer").Escape(value)
  catalogPage("/api/connect/catalog?q=" + escaped + "&")
 end if
end sub
sub navigation(event as Object)
 index = event.GetData()
 if m.access = ""
  keyboard("username", "Usuário do seu acesso GUHFLIX")
  return
 end if
 m.top.findNode("detail").visible = false
 m.episodes.visible = false
 if index = 0
  send("home", "/api/connect/home", "GET", {})
 else if index = 1
  catalogPage("/api/connect/catalog?kind=movie&")
 else if index = 2
  catalogPage("/api/connect/catalog?kind=series&")
 else if index = 3
  keyboard("search", "Buscar filmes e séries")
 else if index = 4
  if m.offset < m.total
   send("catalog", m.catalogPath + "offset=" + m.offset.ToStr(), "GET", {})
  else
   status("Fim desta seleção. Busque outro título ou categoria.")
  end if
 else
  dialog = CreateObject("roSGNode", "Dialog")
  dialog.title = "Sua conta GUHFLIX · " + m.username
  dialog.message = "Use a mesma conta no celular para escolher o que assistir nesta Roku."
  dialog.buttons = ["Gerar código para celular", "Sair da conta", "Voltar"]
  dialog.observeField("buttonSelected", "accountAction")
  m.top.dialog = dialog
 end if
end sub
sub catalogPage(path as String)
 m.catalogPath = path
 m.offset = 0
 send("catalog", path + "offset=0", "GET", {})
end sub
sub accountAction(event as Object)
 m.top.dialog.close = true
 if event.GetData() = 0
  send("pair", "/api/connect/pairing", "POST", {})
 else if event.GetData() = 1
  closeVideo()
  send("logout", "/api/auth/logout", "POST", {})
 end if
end sub
sub closeDialog()
 m.top.dialog.close = true
end sub
sub showRows(rows as Object)
 content = CreateObject("roSGNode", "ContentNode")
 for each row in rows
  rowNode = content.CreateChild("ContentNode")
  rowNode.title = row.title
  for each item in row.items
   node = rowNode.CreateChild("ContentNode")
   node.title = item.title
   node.hdPosterUrl = safeText(item.poster)
   node.AddFields({guhItem: item})
  end for
 end for
 m.catalog.content = content
 m.catalog.visible = true
 if m.introFinished then m.catalog.setFocus(true)
end sub
sub titleSelected(event as Object)
 selected = event.GetData()
 node = m.catalog.content.GetChild(selected[0]).GetChild(selected[1])
 send("detail", "/api/connect/catalog/" + node.guhItem.id, "GET", {})
end sub
sub detailAction(event as Object)
 index = event.GetData()
 if index = 0
  if m.activeItem.kind = "series" then chooseSeason() else watchItem(m.activeItem.id)
 else if index = 1
  send("related", "/api/connect/catalog/" + m.activeItem.id + "/related", "GET", {})
 else
  m.top.findNode("detail").visible = false
  m.catalog.setFocus(true)
 end if
end sub
sub chooseSeason()
 if m.activeItem.seasons = invalid or m.activeItem.seasons.Count() = 0
  status("Nenhum episódio disponível neste título.")
  return
 end if
 dialog = CreateObject("roSGNode", "Dialog")
 dialog.title = "Escolha a temporada"
 buttons = []
 for each season in m.activeItem.seasons
  buttons.Push("Temporada " + season.ToStr())
 end for
 buttons.Push("Voltar")
 dialog.buttons = buttons
 dialog.observeField("buttonSelected", "seasonSelected")
 m.top.dialog = dialog
end sub
sub seasonSelected(event as Object)
 index = event.GetData()
 m.top.dialog.close = true
 if index >= m.activeItem.seasons.Count() then return
 m.season = m.activeItem.seasons[index]
 m.episodeItems = []
 content = CreateObject("roSGNode", "ContentNode")
 for each episode in m.activeItem.episodes
  if episode.season = m.season
   node = content.CreateChild("ContentNode")
   node.title = "Temporada " + m.season.ToStr() + " · Episódio " + episode.episode.ToStr()
   m.episodeItems.Push(episode)
  end if
 end for
 m.episodes.content = content
 m.episodes.visible = true
 m.episodes.setFocus(true)
end sub
sub episodeSelected(event as Object)
 episode = m.episodeItems[event.GetData()]
 m.episodes.visible = false
 watchItem(m.activeItem.id, episode.season, episode.episode)
end sub
sub watchItem(id as String, season = invalid as Dynamic, episode = invalid as Dynamic)
 closeVideo()
 m.wantsVideo = true
 m.playingId = id
 m.playingSeason = season
 m.playingEpisode = episode
 body = {contentId: id, playbackId: CreateObject("roDeviceInfo").GetRandomUUID()}
 m.expectedPlaybackId = body.playbackId
 m.progressKey = "p_" + m.username + "_" + id
 if season <> invalid
  body.season = season
  body.episode = episode
  m.progressKey += "_" + season.ToStr() + "_" + episode.ToStr()
 end if
 status("Preparando o vídeo…")
 send("watch", "/api/connect/watch", "POST", body, {progressKey: m.progressKey})
end sub
sub playSource()
 m.retryPending = false
 if m.sources.Count() = 0 or m.grant = "" then return
 m.video.control = "stop"
 content = CreateObject("roSGNode", "ContentNode")
 source = m.sources[m.sourceIndex]
 content.url = "https://guhflix-marley-api.onrender.com" + source.url
 if source.format = "hls" then content.streamFormat = "hls" else content.streamFormat = "mp4"
 if m.activeItem <> invalid then content.title = m.activeItem.title else content.title = "GUHFLIX"
 m.video.content = content
 m.video.visible = true
 m.video.setFocus(true)
 m.video.control = "play"
 m.top.findNode("watchdog").control = "start"
end sub
sub retrySource()
 if m.grant = "" or m.retryPending then return
 m.retryPending = true
 m.position = m.video.position
 m.savedPosition = m.position
 m.video.control = "stop"
 m.video.visible = false
 m.top.findNode("watchdog").control = "stop"
 m.sourceIndex++
 delay = 1
 if m.sourceIndex >= m.sources.Count()
  m.sourceIndex = 0
  if m.retryCycle < 4 then m.retryCycle++
  delay = 5 * (2 ^ (m.retryCycle - 1))
  if delay > 30 then delay = 30
 end if
 status("Esta fonte não respondeu. Nova tentativa em " + delay.ToStr() + " segundos. Voltar cancela.")
 m.top.findNode("retry").duration = delay
 m.top.findNode("retry").control = "start"
 m.top.setFocus(true)
end sub
sub videoState(event as Object)
 state = event.GetData()
 if state = "error"
  retrySource()
 else if state = "playing"
  m.top.findNode("watchdog").control = "stop"
  m.retryCycle = 0
  if m.savedPosition > 0
   m.video.seek = m.savedPosition
   m.savedPosition = 0
  end if
 else if state = "finished"
  closeVideo()
  m.catalog.setFocus(true)
 end if
end sub
sub sendHeartbeat()
 if m.grant = "" then return
 send("heartbeat", "/api/connect/watch/" + m.grant + "/heartbeat", "POST", {}, {progressKey: m.progressKey, position: m.video.position})
end sub
sub closeVideo()
 m.wantsVideo = false
 m.retryPending = false
 m.top.findNode("retry").control = "stop"
 m.top.findNode("watchdog").control = "stop"
 m.top.findNode("heartbeat").control = "stop"
 if m.grant <> ""
  send("stop", "/api/connect/watch/" + m.grant + "/stop", "POST", {}, {progressKey: m.progressKey, position: m.video.position})
 end if
 m.grant = ""
 m.sources = []
 m.video.control = "stop"
 m.video.visible = false
end sub
sub pollTv()
 if m.access = "" then return
 state = {mode: "idle", title: "", positionMs: 0, durationMs: 0}
 if m.grant <> ""
  state.mode = "loading"
  if m.video.state = "playing" then state.mode = "playing"
  if m.video.state = "paused" then state.mode = "paused"
  state.contentId = m.playingId
  if m.activeItem <> invalid then state.title = m.activeItem.title
  state.positionMs = Int(m.video.position * 1000)
  state.durationMs = Int(m.video.duration * 1000)
 end if
 send("poll", "/api/connect/tv/poll", "POST", {state: state})
end sub
sub performCommand(command as Object)
 if command.type = "play"
  m.remotePlay = command
  send("detail", "/api/connect/catalog/" + command.contentId, "GET", {})
 else if command.type = "pause"
  m.video.control = "pause"
 else if command.type = "resume"
  m.video.control = "resume"
 else if command.type = "stop"
  closeVideo()
 else if command.type = "seek"
  m.video.seek = command.positionMs / 1000
 else if command.type = "seekRelative"
  position = m.video.position + command.deltaMs / 1000
  if position < 0 then position = 0
  m.video.seek = position
 end if
end sub
function onKeyEvent(key as String, press as Boolean) as Boolean
 if not press then return false
 if key = "back"
  if m.grant <> "" or m.wantsVideo
   closeVideo()
   m.actions.setFocus(true)
   return true
  else if m.episodes.visible
   m.episodes.visible = false
   m.actions.setFocus(true)
   return true
  else if m.top.findNode("detail").visible
   m.top.findNode("detail").visible = false
   m.catalog.setFocus(true)
   return true
  end if
 else if key = "up" and m.catalog.hasFocus()
  m.nav.setFocus(true)
  return true
 else if key = "down" and m.nav.hasFocus() and m.access <> ""
  m.catalog.setFocus(true)
  return true
 end if
 return false
end function
