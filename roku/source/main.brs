sub Main()
 screen = CreateObject("roSGScreen")
 port = CreateObject("roMessagePort")
 screen.SetMessagePort(port)
 screen.CreateScene("GuhflixScene")
 screen.Show()
 while true
  message = wait(0, port)
  if type(message) = "roSGScreenEvent" and message.IsScreenClosed() then return
 end while
end sub
