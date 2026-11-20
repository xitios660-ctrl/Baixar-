package com.guhflix.shell

/** Local edition has no authorization timer; existing online/offline editions keep bounded leases. */
object PlaybackLeasePolicy {
 @JvmStatic fun deadline(local:Boolean,offline:Boolean,elapsedMs:Long,remainingMs:Long):Long {
  if(local)return Long.MAX_VALUE
  val duration=if(offline)remainingMs.coerceIn(0,110000) else 110000L
  return elapsedMs+duration
 }
}
