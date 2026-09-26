// =========================================================================
// PW DHYAN • PC Web Browser & Script Injection Server
// Developed by Dhyan for StudyParcham & PWThor Live Desktop Experience
// =========================================================================

const http = require('http');
const https = require('https');
const fs = require('fs');
const path = require('path');
const url = require('url');
const zlib = require('zlib');

const PORT = process.env.PORT || 3300;
const TARGET_HOST_SP = 'pw.studyparcham.in';
const TARGET_ORIGIN_SP = `https://${TARGET_HOST_SP}`;

const TARGET_HOST_PW = 'pwthor.live';
const TARGET_ORIGIN_PW = `https://${TARGET_HOST_PW}`;

const BASE_DIR = __dirname;

const MIME_TYPES = {
    '.html': 'text/html; charset=utf-8',
    '.css': 'text/css; charset=utf-8',
    '.js': 'application/javascript; charset=utf-8',
    '.json': 'application/json; charset=utf-8',
    '.png': 'image/png',
    '.jpg': 'image/jpeg',
    '.jpeg': 'image/jpeg',
    '.gif': 'image/gif',
    '.svg': 'image/svg+xml',
    '.ico': 'image/x-icon',
    '.woff': 'font/woff',
    '.woff2': 'font/woff2',
    '.ttf': 'font/ttf',
    '.mp4': 'video/mp4',
    '.pdf': 'application/pdf'
};

// Script assets
const masterkeyPath = path.join(BASE_DIR, 'scripts', 'masterkey.js');
const liquidPlayerPath = path.join(BASE_DIR, 'scripts', 'liquid_player.js');
const portelPath = path.join(BASE_DIR, 'scripts', 'portel.js');

console.log(`[PW DHYAN] Initializing PC Web Server...`);
console.log(`[PW DHYAN] Script Assets Check:`);
console.log(`  - Portal 1 (StudyParcham):`);
console.log(`      * masterkey.js:     ${fs.existsSync(masterkeyPath) ? '✓ Ready' : '✗ Missing'}`);
console.log(`      * liquid_player.js: ${fs.existsSync(liquidPlayerPath) ? '✓ Ready' : '✗ Missing'}`);
console.log(`  - Portal 2 (PWThor Live):`);
console.log(`      * portel.js:        ${fs.existsSync(portelPath) ? '✓ Ready' : '✗ Missing'}`);

// Helper: Serve Local Static Files
function serveStaticFile(req, res, filePath) {
    const ext = path.extname(filePath).toLowerCase();
    const contentType = MIME_TYPES[ext] || 'application/octet-stream';

    fs.readFile(filePath, (err, data) => {
        if (err) {
            res.writeHead(404, { 'Content-Type': 'text/plain; charset=utf-8' });
            res.end('404 Not Found');
            return;
        }

        const isHtml = ext === '.html';
        res.writeHead(200, {
            'Content-Type': contentType,
            'Cache-Control': isHtml ? 'no-cache, no-store, must-revalidate' : 'public, max-age=3600',
            'Access-Control-Allow-Origin': '*',
            'Service-Worker-Allowed': '/'
        });
        res.end(data);
    });
}

// Helper: Universal Dual-Portal Reverse Proxy (StudyParcham & PWThor Live)
function handleProxyRequest(req, res, targetHost, targetOrigin, proxyPath, portalType) {
    const targetUrl = new URL(proxyPath, targetOrigin);

    const reqHeaders = { ...req.headers };
    reqHeaders['host'] = targetHost;
    reqHeaders['origin'] = targetOrigin;
    reqHeaders['referer'] = targetOrigin + '/';
    if (!reqHeaders['user-agent'] || reqHeaders['user-agent'].includes('node')) {
        reqHeaders['user-agent'] = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36';
    }
    if (portalType === 'pwthor') {
        reqHeaders['accept-encoding'] = 'gzip, deflate, br';
    } else {
        reqHeaders['accept-encoding'] = 'identity';
    }

    delete reqHeaders['connection'];
    delete reqHeaders['keep-alive'];

    const options = {
        hostname: targetHost,
        port: 443,
        path: targetUrl.pathname + targetUrl.search,
        method: req.method,
        headers: reqHeaders,
        rejectUnauthorized: false
    };

    const proxyReq = https.request(options, (proxyRes) => {
        const statusCode = proxyRes.statusCode || 200;
        const resHeaders = { ...proxyRes.headers };

        // Strip headers preventing iframe display & CSP
        delete resHeaders['x-frame-options'];
        delete resHeaders['content-security-policy'];
        delete resHeaders['content-security-policy-report-only'];

        // Add permissive CORS & frame options
        resHeaders['access-control-allow-origin'] = '*';
        resHeaders['access-control-allow-methods'] = 'GET, POST, OPTIONS, PUT, DELETE';
        resHeaders['access-control-allow-headers'] = '*';
        resHeaders['access-control-allow-credentials'] = 'true';

        // Rewrite location headers on redirects
        if (resHeaders['location']) {
            let loc = resHeaders['location'];
            if (loc.startsWith(`https://${TARGET_HOST_PW}`)) {
                loc = loc.replace(`https://${TARGET_HOST_PW}`, '/pwthor');
            } else if (loc.startsWith(`https://${TARGET_HOST_SP}`)) {
                loc = loc.replace(`https://${TARGET_HOST_SP}`, '/studyparcham');
            } else if (loc.startsWith('/') && portalType === 'pwthor' && !loc.startsWith('/pwthor')) {
                loc = '/pwthor' + loc;
            }
            resHeaders['location'] = loc;
        }

        // Rewrite cookies for localhost
        if (resHeaders['set-cookie']) {
            resHeaders['set-cookie'] = resHeaders['set-cookie'].map(c =>
                c.replace(/Domain=[^;]+/gi, '')
                 .replace(/Secure;?/gi, '')
                 .replace(/SameSite=[^;]+/gi, 'SameSite=Lax')
            );
        }

        const contentType = (resHeaders['content-type'] || '').toLowerCase();
        const isHtml = contentType.includes('text/html');

        // If not HTML (e.g. JSON data, JS files, images, video), decompress if compressed so client never receives corrupt binary
        if (!isHtml) {
            const encoding = (resHeaders['content-encoding'] || '').toLowerCase();
            if (encoding === 'gzip' || encoding === 'deflate' || encoding === 'br') {
                const chunks = [];
                proxyRes.on('data', chunk => chunks.push(chunk));
                proxyRes.on('end', () => {
                    let buf = Buffer.concat(chunks);
                    try {
                        if (encoding === 'gzip') buf = zlib.gunzipSync(buf);
                        else if (encoding === 'deflate') buf = zlib.inflateSync(buf);
                        else if (encoding === 'br') buf = zlib.brotliDecompressSync(buf);
                        delete resHeaders['content-encoding'];
                        delete resHeaders['transfer-encoding'];
                        resHeaders['content-length'] = buf.length;
                    } catch (_) {}
                    res.writeHead(statusCode, resHeaders);
                    res.end(buf);
                });
                return;
            }

            res.writeHead(statusCode, resHeaders);
            proxyRes.pipe(res);
            return;
        }

        // HTML: collect chunks and decompress (gzip, deflate, or Brotli)
        const chunks = [];
        proxyRes.on('data', chunk => chunks.push(chunk));
        proxyRes.on('end', () => {
            let buffer = Buffer.concat(chunks);
            const encoding = (resHeaders['content-encoding'] || '').toLowerCase();

            try {
                if (encoding === 'gzip') {
                    buffer = zlib.gunzipSync(buffer);
                } else if (encoding === 'deflate') {
                    buffer = zlib.inflateSync(buffer);
                } else if (encoding === 'br') {
                    buffer = zlib.brotliDecompressSync(buffer);
                }
            } catch (err) {
                console.warn('[Proxy Decompress Warning]:', err.message);
            }

            let html = buffer.toString('utf-8');
            let scriptInjection = '';

            const currentReqUrl = ((proxyPath || '') + ' ' + (req.url || '')).toLowerCase();
            const isPlayerRoute = currentReqUrl.includes('/player') ||
                                  currentReqUrl.includes('player?') ||
                                  currentReqUrl.includes('/watch') ||
                                  currentReqUrl.includes('watch?') ||
                                  currentReqUrl.includes('videoid=') ||
                                  currentReqUrl.includes('childid=') ||
                                  currentReqUrl.includes('vurl=');

            let liquidPlayerCode = '';
            if (isPlayerRoute) {
                try {
                    liquidPlayerCode = fs.readFileSync(liquidPlayerPath, 'utf-8');
                } catch (_) {}
            }

            if (portalType === 'pwthor') {
                // Portal 2: Strictly portel.js, plus liquid_player.js when on player/watch route
                let portelCode = '';
                try {
                    portelCode = fs.readFileSync(portelPath, 'utf-8');
                } catch (_) {}

                scriptInjection = `
<!-- ========================================================================= -->
<!-- [PW DHYAN PC BROWSER] PWTHOR LIQUID GLASS PORTAL 2 INJECTION (by Dhyan) -->
<!-- ========================================================================= -->
<script id="pwdhyan-injected-portal2">
    window.__PWDHYAN_PORTAL__ = { portal: "pwthor", version: "3.3.0", author: "Dhyan", injected: true, isPlayer: ${isPlayerRoute} };
    console.log("%c[PW DHYAN PORTAL 2]%c PW Thor Liquid Glass Suite Active • Developed by Dhyan ❤️ (Player: ${isPlayerRoute})", "background:#ec4899;color:#ffffff;font-weight:bold;font-size:15px;padding:4px 8px;border-radius:4px;", "background:#8b5cf6;color:#ffffff;font-weight:bold;font-size:15px;padding:4px 8px;border-radius:4px;");

    try {
        ${portelCode}
    } catch(err) {
        console.error('[PW DHYAN Portal 2 Error]:', err);
    }

    ${isPlayerRoute && liquidPlayerCode ? `
    try {
        ${liquidPlayerCode}
    } catch(err) {
        console.error('[PW DHYAN LiquidPlayer Error]:', err);
    }
    ` : ''}
</script>
<!-- ========================================================================= -->
`;
            } else {
                // Portal 1: Strictly masterkey.js, plus liquid_player.js only on player route
                let masterkeyCode = '';
                try {
                    masterkeyCode = fs.readFileSync(masterkeyPath, 'utf-8');
                } catch (_) {}

                scriptInjection = `
<!-- ========================================================================= -->
<!-- [PW DHYAN PC BROWSER] STUDYPARCHAM PORTAL 1 INJECTION (by Dhyan) -->
<!-- ========================================================================= -->
<script id="pwdhyan-injected-suite">
    window.__PWDHYAN__ = { portal: "studyparcham", version: "3.0.0", author: "Dhyan", injected: true, isPlayer: ${isPlayerRoute} };
    console.log("%c[PW DHYAN]%c Master Suite Active • Developed by Dhyan ❤️ (Player: ${isPlayerRoute})", "background:#38bdf8;color:#070a13;font-weight:bold;font-size:15px;padding:4px 8px;border-radius:4px;", "background:#10b981;color:#070a13;font-weight:bold;font-size:15px;padding:4px 8px;border-radius:4px;");

    try {
        ${masterkeyCode}
    } catch(err) {
        console.error('[PW DHYAN Masterkey Error]:', err);
    }

    ${isPlayerRoute && liquidPlayerCode ? `
    try {
        ${liquidPlayerCode}
    } catch(err) {
        console.error('[PW DHYAN LiquidPlayer Error]:', err);
    }
    ` : ''}
</script>
<!-- ========================================================================= -->
`;
            }

            // Eradicate StudyParcham lock-card screen and gatekeeper script tag
            if (html.includes('lock-card') || html.includes('Session Authentication Required') || html.includes('generate.html?platform=')) {
                console.log('[PW DHYAN] Intercepted StudyParcham lock-card screen, auto-bypassing...');
                html = `<!DOCTYPE html>
<html lang="en">
<head>
    <meta charset="UTF-8">
    <title>StudyParcham • PW DHYAN</title>
    <script>
        try {
            localStorage.setItem('pw_system_off', 'true');
            localStorage.setItem('pw_system_off_time', Date.now().toString());
            localStorage.setItem('pw_access_key', 'PW_DHYAN_VIP');
            localStorage.setItem('pw_key_expires', (Date.now() + 365*24*3600*1000).toString());
        } catch (_) {}
        window.location.replace('/studyparcham/#home-view');
    </script>
</head>
<body style="background:#070a13; color:#38bdf8; font-family:sans-serif; display:flex; align-items:center; justify-content:center; height:100vh;">
    <div style="font-weight:700; font-size:1.1rem;">Bypassing StudyParcham session lock...</div>
</body>
</html>`;
            } else {
                html = html.replace(/<script[^>]*src=["'][^"']*gatekeeper\.js[^"']*["'][^>]*><\/script>/gi, '<!-- gatekeeper bypassed by PW DHYAN -->');
            }

            // Inject right at start of <head> for @run-at document-start behavior
            if (html.includes('<head>')) {
                html = html.replace('<head>', '<head>' + scriptInjection);
            } else if (html.includes('</head>')) {
                html = html.replace('</head>', scriptInjection + '</head>');
            } else {
                html = scriptInjection + html;
            }

            delete resHeaders['content-encoding'];
            delete resHeaders['transfer-encoding'];
            const outBuf = Buffer.from(html, 'utf-8');
            resHeaders['content-length'] = outBuf.length;

            res.writeHead(statusCode, resHeaders);
            res.end(outBuf);
        });
    });

    proxyReq.on('error', (err) => {
        console.error(`[Proxy Error - ${portalType}]:`, err.message);
        res.writeHead(502, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({ error: 'Gateway Error', portal: portalType, message: err.message }));
    });

    // Forward request body (POST / PUT)
    req.pipe(proxyReq);
}

// Master HTTP Server
const server = http.createServer((req, res) => {
    const parsedUrl = url.parse(req.url, true);
    const pathname = parsedUrl.pathname;

    // Handle CORS preflight
    if (req.method === 'OPTIONS') {
        res.writeHead(204, {
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Methods': 'GET, POST, OPTIONS, PUT, DELETE',
            'Access-Control-Allow-Headers': '*'
        });
        res.end();
        return;
    }

    // API: Server Status & Metadata
    if (pathname === '/api/status') {
        res.writeHead(200, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
        res.end(JSON.stringify({
            status: 'online',
            name: 'PW DHYAN Dual Portal PC Web Browser Engine',
            version: '3.3.0',
            author: 'Dhyan',
            port: PORT,
            portals: {
                studyparcham: {
                    id: 'studyparcham',
                    name: 'StudyParcham (Portal 1)',
                    host: TARGET_HOST_SP,
                    masterkeyReady: fs.existsSync(masterkeyPath),
                    liquidPlayerReady: fs.existsSync(liquidPlayerPath)
                },
                pwthor: {
                    id: 'pwthor',
                    name: 'PWThor Live (Portal 2)',
                    host: TARGET_HOST_PW,
                    portelReady: fs.existsSync(portelPath)
                }
            },
            uptimeSeconds: Math.floor(process.uptime())
        }));
        return;
    }

    // 1. PW DHYAN Gateway App (Login & Notification Gate)
    if (pathname === '/' || pathname === '/index.html') {
        serveStaticFile(req, res, path.join(BASE_DIR, 'index.html'));
        return;
    }
    if (pathname === '/style.css') {
        serveStaticFile(req, res, path.join(BASE_DIR, 'style.css'));
        return;
    }
    if (pathname === '/app.js') {
        serveStaticFile(req, res, path.join(BASE_DIR, 'app.js'));
        return;
    }
    if (pathname === '/logo.png') {
        serveStaticFile(req, res, path.join(BASE_DIR, 'logo.png'));
        return;
    }
    if (pathname === '/sw.js') {
        serveStaticFile(req, res, path.join(BASE_DIR, 'sw.js'));
        return;
    }
    if (pathname.startsWith('/scripts/')) {
        const scriptName = path.basename(pathname);
        serveStaticFile(req, res, path.join(BASE_DIR, 'scripts', scriptName));
        return;
    }

    // 1.1 StudyParcham Gatekeeper & Auth Bypasser
    if (pathname.includes('gatekeeper.js')) {
        res.writeHead(200, {
            'Content-Type': 'application/javascript; charset=utf-8',
            'Access-Control-Allow-Origin': '*',
            'Cache-Control': 'no-cache'
        });
        res.end(`// [PW DHYAN] Gatekeeper Lock Screen Eradicated
try {
    localStorage.setItem('pw_system_off', 'true');
    localStorage.setItem('pw_system_off_time', Date.now().toString());
    localStorage.setItem('pw_access_key', 'PW_DHYAN_VIP');
    localStorage.setItem('pw_key_expires', (Date.now() + 365*24*3600*1000).toString());
} catch (_) {}
console.log('[PW DHYAN] StudyParcham Gatekeeper bypassed.');`);
        return;
    }

    if (pathname.includes('/api/auth') || pathname.endsWith('/api/auth')) {
        res.writeHead(200, {
            'Content-Type': 'application/json; charset=utf-8',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Credentials': 'true'
        });
        res.end(JSON.stringify({
            success: true,
            valid: true,
            system_off: true,
            access: 'unlimited',
            key: 'PW_DHYAN_VIP',
            expires: Date.now() + 365*24*3600*1000
        }));
        return;
    }

    if (pathname.includes('generate.html') || pathname.includes('verify.html')) {
        res.writeHead(302, { 'Location': '/studyparcham/#home-view' });
        res.end();
        return;
    }

    // 2. PWTHOR STREAMVIDEO BACKEND DATA PROXY (Solves "Failed to fetch" on Today's Class & Subjects)
    if (pathname.startsWith('/streamvideo-proxy')) {
        const subPath = req.url.replace(/^\/streamvideo-proxy/, '') || '/';
        const targetUrl = new URL(subPath, 'https://proxy.streamvideo.co.in');

        const headers = { ...req.headers };
        headers['host'] = 'proxy.streamvideo.co.in';
        headers['origin'] = 'https://pwthor.live';
        headers['referer'] = 'https://pwthor.live/study';
        headers['client-id'] = '5eb393ee95fab7468a79d189';
        headers['client-type'] = 'WEB';
        headers['client-version'] = '2.2.7';
        headers['randomid'] = '4bb61dbd-bc60-4a20-a1aa-44e7c0c8e0ec';
        if (!headers['user-agent'] || headers['user-agent'].includes('node')) {
            headers['user-agent'] = 'Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/149.0.0.0 Safari/537.36';
        }
        headers['accept-encoding'] = 'gzip, deflate, br';
        delete headers['connection'];

        const proxyReq = https.request({
            hostname: 'proxy.streamvideo.co.in',
            port: 443,
            path: targetUrl.pathname + targetUrl.search,
            method: req.method,
            headers: headers,
            rejectUnauthorized: false
        }, (proxyRes) => {
            const resHeaders = { ...proxyRes.headers };
            delete resHeaders['x-frame-options'];
            delete resHeaders['content-security-policy'];
            resHeaders['access-control-allow-origin'] = '*';
            resHeaders['access-control-allow-methods'] = 'GET, POST, OPTIONS, PUT, DELETE';
            resHeaders['access-control-allow-headers'] = '*';
            resHeaders['access-control-allow-credentials'] = 'true';

            const encoding = (resHeaders['content-encoding'] || '').toLowerCase();
            if (encoding === 'gzip' || encoding === 'deflate' || encoding === 'br') {
                const chunks = [];
                proxyRes.on('data', c => chunks.push(c));
                proxyRes.on('end', () => {
                    let buf = Buffer.concat(chunks);
                    try {
                        if (encoding === 'gzip') buf = zlib.gunzipSync(buf);
                        else if (encoding === 'deflate') buf = zlib.inflateSync(buf);
                        else if (encoding === 'br') buf = zlib.brotliDecompressSync(buf);
                        delete resHeaders['content-encoding'];
                        delete resHeaders['transfer-encoding'];
                        resHeaders['content-length'] = buf.length;
                    } catch (_) {}
                    res.writeHead(proxyRes.statusCode || 200, resHeaders);
                    res.end(buf);
                });
            } else {
                res.writeHead(proxyRes.statusCode || 200, resHeaders);
                proxyRes.pipe(res);
            }
        });

        proxyReq.on('error', (err) => {
            console.error('[StreamVideo Proxy Error]:', err.message);
            res.writeHead(502, { 'Content-Type': 'application/json', 'Access-Control-Allow-Origin': '*' });
            res.end(JSON.stringify({ error: 'StreamVideo Proxy Error', message: err.message }));
        });

        req.pipe(proxyReq);
        return;
    }

    // 3. DIRECT FETCH CONFIG INTERCEPTION (Instructs PWThor client to route penpencil queries via our proxy)
    if (pathname === '/api/direct-fetch-config' || pathname === '/pwthor/api/direct-fetch-config') {
        res.writeHead(200, {
            'Content-Type': 'application/json; charset=utf-8',
            'Access-Control-Allow-Origin': '*',
            'Access-Control-Allow-Credentials': 'true'
        });
        res.end(JSON.stringify({
            enabled: true,
            baseUrl: '/streamvideo-proxy/fetch/api.penpencil.co'
        }));
        return;
    }

    // 4. TRANSPARENT DUAL PORTAL PROXY FOR ALL OTHER REQUESTS:
    let proxyPath = req.url;
    let portalType = 'studyparcham';

    const cookieMatch = (req.headers.cookie || '').match(/pwdhyan_portal=(studyparcham|pwthor)/);
    if (cookieMatch) {
        portalType = cookieMatch[1];
    }

    if (req.url.startsWith('/pwthor') || (req.headers.referer && req.headers.referer.includes('/pwthor'))) {
        portalType = 'pwthor';
    } else if (req.url.startsWith('/studyparcham') || (req.headers.referer && req.headers.referer.includes('/studyparcham'))) {
        portalType = 'studyparcham';
    }

    if (proxyPath.startsWith('/pwthor')) {
        proxyPath = proxyPath.replace(/^\/pwthor/, '') || '/study';
        if (proxyPath === '/' || proxyPath === '') {
            proxyPath = '/study';
        }
    } else if (proxyPath.startsWith('/studyparcham') || proxyPath.startsWith('/portal')) {
        proxyPath = proxyPath.replace(/^\/(studyparcham|portal)/, '') || '/';
    }

    const targetHost = portalType === 'pwthor' ? TARGET_HOST_PW : TARGET_HOST_SP;
    const targetOrigin = portalType === 'pwthor' ? TARGET_ORIGIN_PW : TARGET_ORIGIN_SP;
    handleProxyRequest(req, res, targetHost, targetOrigin, proxyPath, portalType);
});

// Start Server with Port Availability Guard
server.listen(PORT, '0.0.0.0', () => {
    const localUrl = `http://localhost:${PORT}`;
    console.log(`\n======================================================`);
    console.log(`🚀  PW DHYAN • Dual Portal PC Web Browser Server is LIVE!`);
    console.log(`👉  URL: ${localUrl}`);
    console.log(`🌐  Portal 1: https://${TARGET_HOST_SP} (StudyParcham)`);
    console.log(`⚡  Portal 2: https://${TARGET_HOST_PW}/study (PWThor Live)`);
    console.log(`👑  Created by Dhyan`);
    console.log(`======================================================\n`);
});

server.on('error', (err) => {
    if (err.code === 'EADDRINUSE') {
        console.warn(`[Port ${PORT} in use, trying ${PORT + 1}...]`);
        server.listen(PORT + 1, '0.0.0.0');
    } else {
        console.error('[Server Error]:', err);
    }
});
