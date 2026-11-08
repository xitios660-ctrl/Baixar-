sub Main(args = invalid as Dynamic)
 screen = CreateObject("roSGScreen")
 port = CreateObject("roMessagePort")
 screen.SetMessagePort(port)
 input = CreateObject("roInput")
 input.SetMessagePort(port)
 scene = screen.CreateScene("GuhflixScene")
 screen.Show()
 if args <> invalid then scene.launchInput = args
 while true
  message = wait(0, port)
  if type(message) = "roSGScreenEvent"
   if message.IsScreenClosed() then return
  else if type(message) = "roInputEvent"
   if message.IsInput() then scene.launchInput = message.GetInfo()
  end if
 end while
end sub
