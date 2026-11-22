package com.guhflix.shell

/** Local once used software unconditionally; try GPU once, then respect recovery. */
object CinemaRenderPolicy {
 fun compatible(local:Boolean,migrated:Boolean,saved:Boolean?,legacyDefault:Boolean):Boolean =
  if(local&&!migrated) false else saved?:legacyDefault
}
