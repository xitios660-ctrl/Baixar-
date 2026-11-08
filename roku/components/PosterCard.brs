sub contentChanged()
 c = m.top.itemContent
 if c = invalid then return
 m.top.findNode("art").uri = c.hdPosterUrl
 m.top.findNode("caption").text = c.title
end sub
sub focusChanged()
 m.top.findNode("border").opacity = m.top.focusPercent
 m.top.scale = [1 + 0.045 * m.top.focusPercent, 1 + 0.045 * m.top.focusPercent]
end sub
