import {createPrivateKey, randomUUID, sign} from 'node:crypto';

/**
 * Signs an already-authorized customer's offline access.
 * The caller MUST verify the logged-in TV session and paid expiry first.
 * Never expose the private PEM to the client or to application logs.
 */
export function issueOfflinePermit(
  privatePem: string,
  userId: string,
  installationId: string,
  issuedAtSeconds: number,
  paidUntilSeconds: number,
): {token: string; expiresAt: number} {
  if (!/^[a-f0-9]{24}$/.test(userId) ||
      !/^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$/i.test(installationId)) {
    throw new Error('Invalid customer or installation');
  }
  if (!Number.isSafeInteger(issuedAtSeconds) || issuedAtSeconds < 1_000_000_000 ||
      !Number.isSafeInteger(paidUntilSeconds) || paidUntilSeconds <= issuedAtSeconds) {
    throw new Error('Subscription is not active');
  }
  const key = createPrivateKey(privatePem);
  if (key.asymmetricKeyType !== 'rsa' ||
      (key.asymmetricKeyDetails?.modulusLength ?? 0) < 2048) {
    throw new Error('RSA 2048+ signing key required');
  }
  const expiresAt = Math.min(paidUntilSeconds, issuedAtSeconds + 30 * 86400);
  const payload = [
    '1', userId, installationId.toLowerCase(),
    String(issuedAtSeconds), String(issuedAtSeconds), String(expiresAt), randomUUID(),
  ].join('|');
  const unsigned = 'GFL1.' + Buffer.from(payload, 'utf8').toString('base64url');
  const signature = sign('RSA-SHA256', Buffer.from(unsigned, 'ascii'), key);
  return {token: unsigned + '.' + signature.toString('base64url'), expiresAt};
}
