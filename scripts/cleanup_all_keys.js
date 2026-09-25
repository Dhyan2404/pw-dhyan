/**
 * Automated Cloud Firestore Passkey Purge Worker
 * Purges all accumulated passkeys in access_keys collection to keep Firestore within free quota limits.
 */
const https = require('https');

const apiKey = "AIzaSyA8iX72amArcyc1pH6nBziDhxZHxMnsC4k";
const projectId = "pw-dhyan";

function httpRequest(options, postData) {
  return new Promise((resolve, reject) => {
    const req = https.request(options, res => {
      let data = '';
      res.on('data', chunk => data += chunk);
      res.on('end', () => {
        try {
          const parsed = JSON.parse(data);
          resolve({ status: res.statusCode, body: parsed });
        } catch (e) {
          resolve({ status: res.statusCode, body: data });
        }
      });
    });
    req.on('error', reject);
    if (postData) req.write(postData);
    req.end();
  });
}

async function listBatch(pageSize = 300) {
  const url = `/v1/projects/${projectId}/databases/(default)/documents/access_keys?pageSize=${pageSize}&key=${apiKey}`;
  return httpRequest({
    hostname: 'firestore.googleapis.com',
    path: url,
    method: 'GET'
  });
}

async function deleteBatch(docNames) {
  const writes = docNames.map(name => ({ delete: name }));
  const postData = JSON.stringify({ writes });
  return httpRequest({
    hostname: 'firestore.googleapis.com',
    path: `/v1/projects/${projectId}/databases/(default)/documents:commit?key=${apiKey}`,
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Content-Length': Buffer.byteLength(postData)
    }
  }, postData);
}

let totalPurged = 0;

async function runCleanupCycle() {
  console.log(`[Worker] Checking access_keys collection in Cloud Firestore (${projectId})...`);
  try {
    const res = await listBatch(300);
    if (res.status === 429) {
      console.log(`[Quota Notice] Daily quota is currently exhausted on Firebase Spark free plan. The quota window will reset at midnight PST. Retrying in 60s...`);
      return false;
    }

    if (!res.body || !res.body.documents || res.body.documents.length === 0) {
      console.log(`[Success] No more passkeys found! Collection is clean. Total purged: ${totalPurged}`);
      return true; // All done
    }

    const docNames = res.body.documents.map(d => d.name);
    console.log(`[Found] Fetched ${docNames.length} passkeys. Committing batch delete...`);
    const delRes = await deleteBatch(docNames);
    if (delRes.status === 200) {
      totalPurged += docNames.length;
      console.log(`[Deleted] Purged ${docNames.length} passkeys. (Total wiped so far: ${totalPurged})`);
      // Immediately run next batch
      return 'CONTINUE';
    } else {
      console.warn(`[Delete Response ${delRes.status}]:`, delRes.body);
      return false;
    }
  } catch (err) {
    console.error(`[Error in cleanup cycle]:`, err.message);
    return false;
  }
}

async function startWorker() {
  console.log("==================================================");
  console.log("   PW DHYAN PASSKEY AUTO-PURGE & QUOTA WORKER     ");
  console.log("==================================================");

  while (true) {
    const res = await runCleanupCycle();
    if (res === true) {
      console.log(`[Finished] All passkeys cleared. Sleeping for 5 minutes before checking again...`);
      await new Promise(r => setTimeout(r, 300000));
    } else if (res === 'CONTINUE') {
      await new Promise(r => setTimeout(r, 500));
    } else {
      await new Promise(r => setTimeout(r, 60000));
    }
  }
}

startWorker();
