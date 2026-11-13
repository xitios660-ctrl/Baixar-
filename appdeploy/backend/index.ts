import { MongoClient } from 'mongodb';
import { router, json, secrets } from '@appdeploy/sdk';

// Staging-only. Never reuse this client against a test database or initialize a
// blank production database. Atlas database name is case-sensitive.
const DATABASE_NAME = 'guhflix';
const REQUIRED_COLLECTIONS = [
  'users',
  'sessions',
  'subscriptions',
  'payments',
  'audit_logs',
  'idempotency_keys',
  'devices',
  'web_playbacks',
  'connect_pairings',
  'connect_commands',
  'connect_links',
  'connect_state',
];

let sharedClient: Promise<MongoClient> | null = null;

async function getMongoClient(): Promise<MongoClient> {
  if (!sharedClient) {
    sharedClient = (async () => {
      const uri = await secrets.readSecret('MONGODB_URI');
      if (!uri.startsWith('mongodb://') && !uri.startsWith('mongodb+srv://')) {
        throw new Error('Invalid MongoDB connection protocol');
      }

      const client = new MongoClient(uri, {
        connectTimeoutMS: 8000,
        serverSelectionTimeoutMS: 8000,
        maxPoolSize: 3,
        retryWrites: false,
        appName: 'guhflix-appdeploy-staging',
      });

      try {
        await client.connect();
        return client;
      } catch (err) {
        await client.close().catch(() => undefined);
        throw err;
      }
    })().catch((err) => {
      sharedClient = null;
      throw err;
    });
  }
  return sharedClient;
}

type ProbeStatus = {
  environment: 'homologacao';
  appdeploy: 'online';
  atlas: 'aguardando_credenciais' | 'conectado' | 'indisponivel';
  jwtSecretConfigured: boolean;
  database: 'guhflix';
  collectionsReady: boolean;
  productionTrafficMigrated: false;
  productionAltered: false;
};

// Only performs read-only operations. No client names or other personal data
// are included in responses, even when the caller is unauthenticated.
async function probe(): Promise<ProbeStatus> {
  const names = await secrets.listSecretNames();
  const mongoConfigured = names.includes('MONGODB_URI');
  const jwtSecretConfigured = names.includes('JWT_SECRET');

  const base: ProbeStatus = {
    environment: 'homologacao',
    appdeploy: 'online',
    atlas: 'aguardando_credenciais',
    jwtSecretConfigured,
    database: DATABASE_NAME,
    collectionsReady: false,
    productionTrafficMigrated: false,
    productionAltered: false,
  };

  if (!mongoConfigured) return base;

  try {
    const client = await getMongoClient();
    const database = client.db(DATABASE_NAME);
    await database.command({ ping: 1 });
    const collections = await database.listCollections({}, { nameOnly: true }).toArray();
    const existing = new Set(collections.map((item) => item.name));

    return {
      ...base,
      atlas: 'conectado',
      collectionsReady: REQUIRED_COLLECTIONS.every((name) => existing.has(name)),
    };
  } catch {
    // Do not print exceptions: drivers may include secret hosts or credentials.
    return { ...base, atlas: 'indisponivel' };
  }
}

export const handler = router({
  'GET /api/_healthcheck': [
    async () => json({ ok: true, service: 'guhflix-appdeploy-staging' }),
  ],
  'GET /api/migration/status': [
    async () => json(await probe()),
  ],
});
