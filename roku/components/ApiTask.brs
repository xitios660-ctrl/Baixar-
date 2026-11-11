sub init()
 m.top.functionName = "runRequest"
end sub
function transfer(path as String, method as String, body as Dynamic, access as String) as Object
 request = CreateObject("roUrlTransfer")
 port = CreateObject("roMessagePort")
 request.SetPort(port)
 request.SetCertificatesFile("common:/certs/ca-bundle.crt")
 request.InitClientCertificates()
 request.SetUrl("https://guhflix-marley-api.onrender.com" + path)
 request.AddHeader("Accept", "application/json")
 request.AddHeader("Content-Type", "application/json")
 if access <> "" then request.AddHeader("Authorization", "Bearer " + access)
 if method = "POST"
  started = request.AsyncPostFromString(FormatJson(body))
 else
  started = request.AsyncGetToString()
 end if
 if not started then return {status: 503, data: {message: "Não foi possível conectar ao Gfilmes."}}
 event = wait(28000, port)
 if event = invalid
  request.AsyncCancel()
  return {status: 503, data: {message: "A conexão demorou. Tente novamente."}}
 end if
 data = ParseJson(event.GetString())
 if data = invalid then data = {message: "Servidor temporariamente indisponível."}
 return {status: event.GetResponseCode(), data: data}
end function
sub runRequest()
 job = m.top.request
 registry = CreateObject("roRegistrySection", "Gfilmes")
 if job.tag = "restore"
  saved = registry.Read("refresh")
  if saved = ""
   m.top.response = {tag: "restore", status: 401, data: {message: "Entre com seu acesso de cliente."}}
   return
  end if
  result = transfer("/api/auth/refresh", "POST", {refreshToken: saved}, "")
 else
  if job.tag = "login"
   installation = registry.Read("installation")
   if installation = ""
    installation = CreateObject("roDeviceInfo").GetRandomUUID()
    registry.Write("installation", installation)
   end if
   job.body.installationId = installation
   job.body.client = "tv"
   job.body.label = "Gfilmes Roku"
  end if
  result = transfer(job.path, job.method, job.body, job.access)
  if result.status = 401 and job.tag <> "login" and job.tag <> "logout"
   saved = registry.Read("refresh")
   if saved <> ""
    renewed = transfer("/api/auth/refresh", "POST", {refreshToken: saved}, "")
    if renewed.status = 200
     registry.Write("refresh", renewed.data.refreshToken)
     registry.Flush()
     result = transfer(job.path, job.method, job.body, renewed.data.accessToken)
     result.auth = renewed.data
    end if
   end if
  end if
 end if
 if (job.tag = "login" or job.tag = "restore") and result.status = 200
  registry.Write("refresh", result.data.refreshToken)
  registry.Flush()
 end if
 if job.tag = "logout"
  registry.Delete("refresh")
  registry.Flush()
 end if
 if job.progressKey <> invalid
  if job.position <> invalid
   registry.Write(job.progressKey, job.position.ToStr())
   registry.Flush()
  else if job.tag = "watch"
   savedPosition = registry.Read(job.progressKey)
   if savedPosition <> "" then result.seek = Val(savedPosition)
  end if
 end if
 if job.tag = "watch" then result.playbackId = job.body.playbackId
 result.tag = job.tag
 m.top.response = result
end sub
