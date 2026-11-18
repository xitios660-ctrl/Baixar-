package com.guhflix.shell

/** A broken renderer gets one automatic retry per user attempt. Old page callbacks are ignored. */
class CinemaStartupPolicy {
 enum class Failure { IGNORE, RETRY_COMPATIBLE, SHOW_RECOVERY }
 var generation=0; private set
 var ready=false; private set
 private var automaticRetries=0
 private var failed=false
 fun begin(manual:Boolean=false):Int {if(manual)automaticRetries=0;ready=false;failed=false;return ++generation}
 fun markReady(ticket:Int):Boolean {if(ticket!=generation||failed)return false;ready=true;return true}
 fun fail(ticket:Int):Failure {
  if(ticket!=generation)return Failure.IGNORE
  ready=false;failed=true
  return if(automaticRetries++==0)Failure.RETRY_COMPATIBLE else Failure.SHOW_RECOVERY
 }
}
