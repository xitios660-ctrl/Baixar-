package com.guhflix.shell

/** Backoff is capped; cancelling or leaving the title removes the scheduled work. */
class RetryPolicy {
 private var cycles=0
 fun nextDelayMs():Long {val delay=(5000L shl cycles.coerceAtMost(3)).coerceAtMost(30000L);cycles=(cycles+1).coerceAtMost(100);return delay}
 fun reset(){cycles=0}
}
