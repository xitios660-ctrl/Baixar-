package com.gfilmes.server

import java.io.ByteArrayOutputStream
import java.net.InetAddress
import java.net.ServerSocket
import java.net.Socket
import java.security.MessageDigest
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.ThreadPoolExecutor
import java.util.concurrent.TimeUnit

/** Temporary LAN transport: public key out, already encrypted/signed files in. No activation or admin API. */
class SetupPortal(
    address: InetAddress, port: Int, private val token: String,
    private val publicKey: ByteArray,
    private val acceptFile: (String, ByteArray) -> Unit,
    private val encryptedExport: () -> ByteArray? = { null },
    private val maxBody: Int = 32 * 1024 * 1024
) : AutoCloseable {
    private val clients=java.util.concurrent.ConcurrentHashMap.newKeySet<Socket>()
    private val server = ServerSocket(port, 4, address)
    private val workers = ThreadPoolExecutor(1, 1, 10, TimeUnit.SECONDS, ArrayBlockingQueue<Runnable>(2))
    val port: Int get() = server.localPort
    init { require(token.matches(Regex("[a-f0-9]{64}"))) }
    fun start() { Thread({
        while (!server.isClosed) {
            try { val socket=server.accept(); if (!socket.inetAddress.isSiteLocalAddress && !socket.inetAddress.isLoopbackAddress) socket.close()
                else try { clients.add(socket);workers.execute { try{serve(socket)}finally{clients.remove(socket)} } } catch (_: Exception) { clients.remove(socket);socket.close() }
            } catch (_: Exception) { if (server.isClosed) break }
        }
    }, "gfilmes-setup-lan").apply { isDaemon=true; start() } }
    private fun reply(s:Socket, code:Int, bytes:ByteArray, type:String="text/plain; charset=utf-8", attachment:String?=null) {
        val extra=if(attachment!=null)"Content-Disposition: attachment; filename=\"$attachment\"\r\n" else ""
        s.getOutputStream().apply { write(("HTTP/1.1 $code Response\r\nContent-Type: $type\r\nContent-Length: ${bytes.size}\r\nConnection: close\r\nCache-Control: no-store\r\nX-Content-Type-Options: nosniff\r\nReferrer-Policy: no-referrer\r\nContent-Security-Policy: default-src 'none'; script-src 'nonce-$token'; style-src 'nonce-$token'; connect-src 'self'; form-action 'self'; frame-ancestors 'none'; base-uri 'none'\r\n$extra\r\n").toByteArray());write(bytes);flush() }
    }
    private fun serve(socket:Socket) { socket.use { s ->
        s.soTimeout=7000
        try {
            val input=s.getInputStream();var headerSize=0;val deadline=System.nanoTime()+20_000_000_000L
            fun line():String { val out=ByteArrayOutputStream();while(true){require(System.nanoTime()<deadline);val b=input.read();require(b>=0);headerSize++;require(headerSize<=8192);if(b==10)break;if(b!=13)out.write(b)};return out.toString("US-ASCII") }
            val first=line().split(' ');require(first.size==3&&first[2]=="HTTP/1.1")
            val headers=mutableMapOf<String,String>();while(true){val l=line();if(l.isEmpty())break;val i=l.indexOf(':');require(i>0);val k=l.substring(0,i).lowercase();require(!headers.containsKey(k));headers[k]=l.substring(i+1).trim()}
            require(!headers.containsKey("transfer-encoding"))
            val parts=first[1].split('/');val supplied=parts.getOrNull(1).orEmpty()
            if(!MessageDigest.isEqual(supplied.toByteArray(),token.toByteArray())){reply(s,404,"Não encontrado".toByteArray());return}
            val action=parts.getOrNull(2).orEmpty();if(parts.size>3){reply(s,404,"Não encontrado".toByteArray());return}
            if(first[0]=="GET") {
                when(action) {
                    "" -> reply(s,200,page().toByteArray(),"text/html; charset=utf-8")
                    "public-key" -> reply(s,200,publicKey,"text/plain; charset=utf-8","gfilmes-server-public.txt")
                    "backup" -> {val bytes=encryptedExport();if(bytes==null)reply(s,404,"Prepare o backup na TV primeiro".toByteArray()) else reply(s,200,bytes,"application/octet-stream","gfilmes-backup.json")}
                    else -> reply(s,404,"Não encontrado".toByteArray())
                };return
            }
            if(first[0]!="POST"||action !in listOf("grant","migration","configuration","restore")){reply(s,404,"Não encontrado".toByteArray());return}
            val length=headers["content-length"]?.toLongOrNull()?:-1
            if(length !in 1..maxBody.toLong()||(action=="grant"&&length>32768)){reply(s,413,"Arquivo excede o limite ou não possui tamanho válido".toByteArray());return}
            // Browser same-origin requests have Origin; cross-origin forms/fetches are denied.
            val origin=headers["origin"];if(origin!=null&&origin!="http://"+headers["host"]){reply(s,403,"Origem recusada".toByteArray());return}
            val bytes=ByteArray(length.toInt());var offset=0
            while(offset<bytes.size){require(System.nanoTime()<deadline);val n=input.read(bytes,offset,bytes.size-offset);require(n>0);offset+=n}
            try { acceptFile(action,bytes);reply(s,200,"Arquivo aceito. Volte ao aplicativo na TV e atualize a tela.".toByteArray()) }
            catch(_:Exception){reply(s,400,"Arquivo inválido, não autorizado ou ação não preparada na TV. Nada foi liberado.".toByteArray())}
        } catch(_:Exception) {try {reply(s,400,"Requisição inválida".toByteArray())}catch(_:Exception){}}
    } }
    private fun publicFingerprint()=MessageDigest.getInstance("SHA-256").digest(java.util.Base64.getUrlDecoder().decode(String(publicKey,Charsets.UTF_8))).joinToString(""){"%02x".format(it)}
    private fun page()="""<!doctype html><html lang="pt-BR"><meta charset="utf-8"><meta name="viewport" content="width=device-width,initial-scale=1"><title>Configuração local Gfilmes</title><style nonce="$token">body{background:#08090e;color:#fff;font:17px system-ui,sans-serif;max-width:720px;margin:auto;padding:24px}h1{color:#f3133d}a{color:#ff6678}button,select,input{font:inherit;max-width:100%;margin:8px 0;padding:12px;border-radius:8px}button{background:#e50924;color:white;border:0}code{overflow-wrap:anywhere}#status{padding:12px;background:#20212a;border-radius:8px}</style><body><h1>Gfilmes Servidor</h1><p>Configuração temporária na mesma rede Wi-Fi. Não digite senhas nesta página.</p><p><a href="/$token/public-key">1 · Baixar chave PÚBLICA do servidor</a></p><p>Impressão digital (compare com a TV): <code>${publicFingerprint()}</code></p><p>Envie a chave pública ao responsável pela preparação. Depois importe os arquivos recebidos:</p><label>Arquivo <input id="file" type="file"></label><p><select id="kind"><option value="grant">2 · Autorização pública assinada</option><option value="migration">3 · Migração cifrada e assinada</option><option value="configuration">Certificado em pacote cifrado e assinado</option><option value="restore">Recuperação cifrada (prepare na TV primeiro)</option></select><button id="send">Enviar para a TV</button></p><p id="status"></p><p><a href="/$token/backup">Baixar backup cifrado preparado na TV</a></p><p>Esta página não ativa clientes, não transmite filmes e não administra assinaturas. Feche a configuração na TV ao terminar.</p><script nonce="$token">document.getElementById('send').onclick=async()=>{const f=document.getElementById('file').files[0],s=document.getElementById('status');if(!f){s.textContent='Escolha um arquivo';return}if(f.size>33554432){s.textContent='Limite de 32 MiB';return}s.textContent='Enviando…';try{const r=await fetch('/$token/'+document.getElementById('kind').value,{method:'POST',body:f,headers:{'Content-Type':'application/octet-stream'},credentials:'omit'});s.textContent=await r.text()}catch(e){s.textContent='Não foi possível conectar. Confira se a configuração continua aberta na TV.'}};</script></body></html>"""
    override fun close(){try{server.close();clients.forEach{try{it.close()}catch(_:Exception){}};clients.clear()}finally{workers.shutdownNow()}}
}
