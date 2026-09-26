// =========================================================================
// PW DHYAN • PC Web Browser Core Application Logic
// Built by Dhyan for StudyParcham Desktop Environment
// =========================================================================

import { initializeApp } from "https://www.gstatic.com/firebasejs/12.19.0/firebase-app.js";
import { 
  getFirestore, 
  collection, 
  doc, 
  setDoc, 
  updateDoc, 
  addDoc, 
  onSnapshot, 
  query, 
  where, 
  getDocs 
} from "https://www.gstatic.com/firebasejs/12.19.0/firebase-firestore.js";

// 1. Firebase Configuration (Project: pw-dhyan)
const firebaseConfig = {
  apiKey: "AIzaSyA8iX72amArcyc1pH6nBziDhxZHxMnsC4k",
  authDomain: "pw-dhyan.firebaseapp.com",
  projectId: "pw-dhyan",
  storageBucket: "pw-dhyan.firebasestorage.app",
  messagingSenderId: "133624864848",
  appId: "1:133624864848:web:e74104d318a776895b7155",
  measurementId: "G-YXZLHPMSBL"
};

const app = initializeApp(firebaseConfig);
const db = getFirestore(app);

const MASTER_PERMANENT_CODE = "240411";
const PORTAL_URL_SP = "/studyparcham/#home-view";
const PORTAL_URL_PW = "/pwthor/study";

let currentPortal = localStorage.getItem('pwdhyan_active_portal') || 'studyparcham';
let portalConfig = {
  defaultPortal: 'studyparcham',
  studyparchamMaintenance: { isActive: false, message: '' },
  pwthorMaintenance: { isActive: false, message: '' },
  blockedPortals: []
};

try {
  document.cookie = "pwdhyan_portal=" + currentPortal + "; path=/; max-age=864000";
} catch (_) {}

// 2. Synthesized Web Audio Effects (Zero External Files Needed)
class SoundFX {
  constructor() {
    this.ctx = null;
  }
  init() {
    if (!this.ctx) {
      const AudioCtx = window.AudioContext || window.webkitAudioContext;
      if (AudioCtx) this.ctx = new AudioCtx();
    }
    if (this.ctx && this.ctx.state === 'suspended') {
      this.ctx.resume();
    }
  }
  playClick() {
    try {
      this.init();
      if (!this.ctx) return;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = 'sine';
      osc.frequency.setValueAtTime(800, this.ctx.currentTime);
      osc.frequency.exponentialRampToValueAtTime(400, this.ctx.currentTime + 0.04);
      gain.gain.setValueAtTime(0.08, this.ctx.currentTime);
      gain.gain.exponentialRampToValueAtTime(0.001, this.ctx.currentTime + 0.04);
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start();
      osc.stop(this.ctx.currentTime + 0.04);
    } catch (_) {}
  }
  playUnlock() {
    try {
      this.init();
      if (!this.ctx) return;
      const now = this.ctx.currentTime;
      const notes = [523.25, 659.25, 783.99, 1046.50]; // C5, E5, G5, C6
      notes.forEach((freq, idx) => {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'triangle';
        osc.frequency.setValueAtTime(freq, now + idx * 0.07);
        gain.gain.setValueAtTime(0.12, now + idx * 0.07);
        gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.07 + 0.25);
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        osc.start(now + idx * 0.07);
        osc.stop(now + idx * 0.07 + 0.25);
      });
    } catch (_) {}
  }
  playNotif() {
    try {
      this.init();
      if (!this.ctx) return;
      const now = this.ctx.currentTime;
      [880, 1320].forEach((freq, idx) => {
        const osc = this.ctx.createOscillator();
        const gain = this.ctx.createGain();
        osc.type = 'sine';
        osc.frequency.setValueAtTime(freq, now + idx * 0.1);
        gain.gain.setValueAtTime(0.15, now + idx * 0.1);
        gain.gain.exponentialRampToValueAtTime(0.001, now + idx * 0.1 + 0.35);
        osc.connect(gain);
        gain.connect(this.ctx.destination);
        osc.start(now + idx * 0.1);
        osc.stop(now + idx * 0.1 + 0.35);
      });
    } catch (_) {}
  }
  playBuzzer() {
    try {
      this.init();
      if (!this.ctx) return;
      const now = this.ctx.currentTime;
      const osc = this.ctx.createOscillator();
      const gain = this.ctx.createGain();
      osc.type = 'sawtooth';
      osc.frequency.setValueAtTime(150, now);
      osc.frequency.setValueAtTime(120, now + 0.1);
      gain.gain.setValueAtTime(0.12, now);
      gain.gain.exponentialRampToValueAtTime(0.001, now + 0.22);
      osc.connect(gain);
      gain.connect(this.ctx.destination);
      osc.start(now);
      osc.stop(now + 0.22);
    } catch (_) {}
  }
}

const sfx = new SoundFX();

// 3. Client Device Identity
function getOrCreateDeviceId() {
  let id = localStorage.getItem('pwdhyan_pc_device_id');
  if (!id) {
    id = 'PC-' + Math.random().toString(36).substring(2, 10).toUpperCase() + '-' + Date.now().toString(36).toUpperCase();
    localStorage.setItem('pwdhyan_pc_device_id', id);
  }
  return id;
}

const deviceId = getOrCreateDeviceId();
const deviceModel = 'Windows PC (Chrome / Desktop)';

// 4. Session State Management
let currentSession = null;
let currentPin = '';

function loadSession() {
  try {
    const raw = localStorage.getItem('pwdhyan_session');
    if (!raw) return null;
    const sess = JSON.parse(raw);
    if (!sess.isInfinite && sess.expiresAt && Date.now() >= sess.expiresAt) {
      localStorage.removeItem('pwdhyan_session');
      return null;
    }
    return sess;
  } catch (e) {
    return null;
  }
}

function saveSession(sess) {
  currentSession = sess;
  localStorage.setItem('pwdhyan_session', JSON.stringify(sess));
}

function clearSession() {
  currentSession = null;
  localStorage.removeItem('pwdhyan_session');
  switchScreen('login');
  showToast('Logged out securely.');
}

// 5. Screen Switcher
function switchScreen(screenId) {
  document.querySelectorAll('.app-screen').forEach(s => s.classList.remove('active'));
  const target = document.getElementById(`screen-${screenId}`);
  if (target) {
    target.classList.add('active');
  }
}

// 6. Toast Notification Helper
function showToast(msg, icon = 'info-circle') {
  const container = document.getElementById('toast-container');
  if (!container) return;
  const t = document.createElement('div');
  t.className = 'toast-msg';
  t.innerHTML = `<i class="fas fa-${icon}"></i> <span>${msg}</span>`;
  container.appendChild(t);
  setTimeout(() => {
    if (t.parentNode) t.parentNode.removeChild(t);
  }, 3600);
}

// 7. PIN & Keypad Management
function updatePinDots() {
  const dots = document.querySelectorAll('.pin-dot');
  dots.forEach((dot, index) => {
    dot.classList.toggle('filled', index < currentPin.length);
    dot.classList.remove('error');
  });
}

function triggerPinError(msg) {
  sfx.playBuzzer();
  const dots = document.querySelectorAll('.pin-dot');
  dots.forEach(d => d.classList.add('error'));
  const statusMsg = document.getElementById('auth-status');
  if (statusMsg) {
    statusMsg.className = 'auth-status-msg error';
    statusMsg.innerText = msg;
  }
  const container = document.querySelector('.lock-container');
  if (container) {
    container.classList.add('shake');
    setTimeout(() => container.classList.remove('shake'), 500);
  }
  setTimeout(() => {
    currentPin = '';
    updatePinDots();
  }, 900);
}

function handleKeyInput(char) {
  sfx.playClick();
  if (currentPin.length < 6) {
    currentPin += char;
    updatePinDots();
    if (currentPin.length === 6) {
      submitPasskey(currentPin);
    }
  }
}

function handleBackspace() {
  sfx.playClick();
  if (currentPin.length > 0) {
    currentPin = currentPin.slice(0, -1);
    updatePinDots();
  }
}

function handleClear() {
  sfx.playClick();
  currentPin = '';
  updatePinDots();
}

// 8. Passkey Authentication (Master PIN & Cloud Firestore)
async function submitPasskey(pinCode) {
  const statusMsg = document.getElementById('auth-status');
  if (statusMsg) {
    statusMsg.className = 'auth-status-msg info';
    statusMsg.innerText = 'Verifying security passkey with Cloud Firestore...';
  }

  // 8.1 Master Permanent Code Check
  if (pinCode === MASTER_PERMANENT_CODE) {
    sfx.playUnlock();
    const sessionData = {
      code: pinCode,
      label: 'Permanent Master (Dhyan)',
      isInfinite: true,
      expiresAt: null,
      unlockedAt: Date.now(),
      deviceId
    };
    saveSession(sessionData);
    logAudit('SUCCESS_MASTER', pinCode);
    proceedAfterLogin();
    return;
  }

  // 8.2 Firestore Cloud Access Key Verification
  try {
    const q = query(collection(db, "access_keys"), where("code", "==", pinCode));
    const snapshot = await getDocs(q);

    if (snapshot.empty) {
      logAudit('FAILED_INVALID', pinCode);
      triggerPinError('Invalid passkey. Check PIN or contact Dhyan.');
      return;
    }

    const keyDoc = snapshot.docs[0];
    const keyData = keyDoc.data();

    // Check device lock
    if (keyData.deviceId && keyData.deviceId !== deviceId) {
      logAudit('FAILED_DEVICE_LOCKED', pinCode);
      triggerPinError('This passkey is locked to another student device.');
      return;
    }

    // Check expiry
    const now = Date.now();
    let expiresAt = null;
    if (!keyData.isInfinite) {
      const duration = keyData.durationMillis || (24 * 60 * 60 * 1000);
      const startTime = keyData.usedAt || now;
      expiresAt = startTime + duration;
      if (now >= expiresAt) {
        logAudit('FAILED_EXPIRED', pinCode);
        triggerPinError('Passkey has expired. Generate a new key.');
        return;
      }
    }

    // Mark as used if first time
    if (!keyData.isUsed) {
      await updateDoc(doc(db, "access_keys", keyDoc.id), {
        isUsed: true,
        deviceId: deviceId,
        deviceModel: deviceModel,
        usedAt: now
      });
    }

    sfx.playUnlock();
    const sessionData = {
      code: pinCode,
      label: keyData.label || 'Student Key',
      isInfinite: keyData.isInfinite || false,
      expiresAt: expiresAt,
      unlockedAt: now,
      deviceId
    };
    saveSession(sessionData);
    logAudit('SUCCESS_KEY', pinCode);
    proceedAfterLogin();
  } catch (err) {
    console.error('[Auth Error]:', err);
    triggerPinError('Cloud connection timeout. Check internet connection.');
  }
}

// 9. Proceed After Login: Gate to Notifications or Browser
function proceedAfterLogin() {
  startCloudSync();

  // If maintenance lockdown is active, lock down immediately!
  if (isMaintenanceLockdown) {
    switchScreen('maintenance');
    return;
  }

  // Mandatory Notification Access Check:
  // ONLY if Notification.permission is explicitly 'granted' can the user enter StudyParcham!
  const isGranted = ("Notification" in window) && (Notification.permission === 'granted');

  if (isGranted) {
    launchStudyParcham();
  } else {
    // Show Screen 2 (Mandatory Notification Gate)
    switchScreen('notification');
    checkNotifBlockedState();
  }
}

// 10. Notification Access Handler & Engine
let swRegistration = null;
if ('serviceWorker' in navigator) {
  navigator.serviceWorker.register('/sw.js', { scope: '/' })
    .then(reg => {
      swRegistration = reg;
      console.log('[PW DHYAN] Service Worker registered.');
    })
    .catch(err => {
      console.warn('[PW DHYAN] Service Worker registration failed:', err);
    });
}

function sendDesktopNotification({ title, message, author = 'PW DHYAN', tag, isBurst = false, burstCount = 1, burstIndex = 1 }) {
  // 1. Play Synthesized Audio Chime
  sfx.playNotif();

  // 2. Native OS Desktop Notification
  if ("Notification" in window && Notification.permission === 'granted') {
    const notifOptions = {
      body: message,
      icon: "/logo.png",
      badge: "/logo.png",
      tag: tag || `pwdhyan-${Date.now()}-${burstIndex}`,
      requireInteraction: true,
      silent: false
    };

    if (swRegistration && swRegistration.showNotification) {
      swRegistration.showNotification(title, notifOptions).catch(() => {
        try { new Notification(title, notifOptions); } catch (_) {}
      });
    } else {
      try { new Notification(title, notifOptions); } catch (_) {}
    }
  }

  // 3. Cyber-Glass On-Screen Visual HUD Overlay
  displayNotificationHUD(title, message, author, isBurst, burstIndex, burstCount);

  // 4. In-App Floating Toast
  showToast(`🔔 ${title}: ${message}`, isBurst ? 'bolt' : 'bell');
}

let notifBannerTimeout = null;
function displayNotificationHUD(title, msg, author = 'PW DHYAN', isBurst = false, burstIndex = 1, burstCount = 1) {
  const banner = document.getElementById('notification-banner') || document.getElementById('announcement-banner');
  if (!banner) return;

  const tagEl = document.getElementById('notif-banner-tag') || document.getElementById('announcement-author');
  const burstEl = document.getElementById('notif-banner-burst');
  const titleEl = document.getElementById('notif-banner-title');
  const textEl = document.getElementById('notif-banner-text') || document.getElementById('announcement-text');

  if (tagEl) tagEl.innerText = `${(author || 'PW DHYAN').toUpperCase()}`;
  if (burstEl) {
    if (isBurst && burstCount > 1) {
      burstEl.style.display = 'inline-block';
      burstEl.innerText = `⚡ BURST [${burstIndex}/${burstCount}]`;
    } else {
      burstEl.style.display = 'none';
    }
  }
  if (titleEl) titleEl.innerText = title;
  if (textEl) textEl.innerText = msg;

  banner.classList.add('show');
  if (isBurst) {
    banner.classList.add('burst-active');
  } else {
    banner.classList.remove('burst-active');
  }

  clearTimeout(notifBannerTimeout);
  notifBannerTimeout = setTimeout(() => {
    banner.classList.remove('show');
    banner.classList.remove('burst-active');
  }, 9000);
}

function hideNotificationHUD() {
  clearTimeout(notifBannerTimeout);
  const banner = document.getElementById('notification-banner') || document.getElementById('announcement-banner');
  if (banner) {
    banner.classList.remove('show');
    banner.classList.remove('burst-active');
  }
}

function triggerPushNotification(notif) {
  const isBurst = notif.isBurst || (notif.burstCount && parseInt(notif.burstCount, 10) > 1);
  const totalCount = isBurst ? Math.min(parseInt(notif.burstCount, 10) || 5, 20) : 1;

  for (let i = 0; i < totalCount; i++) {
    setTimeout(() => {
      const burstPrefix = totalCount > 1 ? `⚡ [${i + 1}/${totalCount}] ` : '';
      const finalTitle = burstPrefix + (notif.title || '🔥 PW DHYAN ALERT');
      sendDesktopNotification({
        title: finalTitle,
        message: notif.message,
        author: notif.author || 'ADMIN',
        tag: `notif-${notif.id || Date.now()}-${i}`,
        isBurst: totalCount > 1,
        burstCount: totalCount,
        burstIndex: i + 1
      });
    }, i * 380);
  }
}

const STUDY_MOTIVATIONS = [
  "Padh lo beta mauka hai, baki sab dhokha hai! 📚",
  "IIT Bombay CSE bula raha hai! Consistency is Selection! 🎯",
  "Selection chahiye ya excuses? Lecture complete karo abhi! ⚡",
  "PW DHYAN Reminder: Stay focused! New DPP & chapter lectures are live! 📖",
  "Ek aur DPP solve karo, rank 1 tumhari hogi! 🏆",
  "Aaj ki mehnat, kal ka result! Revision chalu karo! ⏰",
  "Mummy Papa ka sapna pura karna hai ya nahi? Focus! 💯",
  "PW Dhyan Study Power Mode: Stay relentless! 🚀"
];

let motivationInterval = null;
function startPeriodicMotivation() {
  if (motivationInterval) clearInterval(motivationInterval);
  // Periodic study motivation every 5 minutes (matching Android NotificationHelper.kt)
  motivationInterval = setInterval(() => {
    const browserScreen = document.getElementById('screen-browser');
    if (browserScreen && browserScreen.classList.contains('active')) {
      const quote = STUDY_MOTIVATIONS[Math.floor(Math.random() * STUDY_MOTIVATIONS.length)];
      sendDesktopNotification({
        title: "🔥 Study Motivator • PW DHYAN",
        message: quote,
        author: "STUDY MENTOR",
        tag: "periodic-reminder"
      });
    }
  }, 5 * 60 * 1000);
}

function checkNotifBlockedState() {
  const isDenied = ("Notification" in window) && (Notification.permission === 'denied');
  const blockedBox = document.getElementById('notif-blocked-box');
  const btnGrant = document.getElementById('btn-grant-notif');
  const btnCheck = document.getElementById('btn-check-notif');

  if (isDenied) {
    if (blockedBox) blockedBox.style.display = 'block';
    if (btnCheck) btnCheck.style.display = 'flex';
    if (btnGrant) btnGrant.style.display = 'none';
  } else {
    if (blockedBox) blockedBox.style.display = 'none';
    if (btnCheck) btnCheck.style.display = 'none';
    if (btnGrant) btnGrant.style.display = 'flex';
  }
}

async function requestNotificationAccess() {
  sfx.init();
  if (!("Notification" in window)) {
    showToast('Web notifications are not supported in this browser.', 'exclamation-circle');
    launchStudyParcham();
    return;
  }

  try {
    const perm = await Notification.requestPermission();
    checkNotifBlockedState();

    if (perm === 'granted') {
      sfx.playUnlock();
      sendDesktopNotification({
        title: "🎉 Desktop Notifications Active!",
        message: "You will receive live class updates, announcements, and study alerts from Dhyan.",
        author: "PW DHYAN"
      });
      showToast('Notification Access Granted! Entering Study Portal...', 'bell');
      setTimeout(() => launchStudyParcham(), 700);
    } else {
      sfx.playBuzzer();
      showToast('Notification access is MANDATORY to enter the portal!', 'exclamation-triangle');
      const container = document.querySelector('.notif-container');
      if (container) {
        container.classList.add('shake');
        setTimeout(() => container.classList.remove('shake'), 500);
      }
    }
  } catch (e) {
    console.warn('[Notification Error]:', e);
    checkNotifBlockedState();
  }
}

function checkNotificationAccessAgain() {
  sfx.init();
  sfx.playClick();
  const isGranted = ("Notification" in window) && (Notification.permission === 'granted');
  checkNotifBlockedState();

  if (isGranted) {
    sfx.playUnlock();
    showToast('Notification Access Verified! Entering Study Portal...', 'bell');
    sendDesktopNotification({
      title: "🎉 Notifications Verified!",
      message: "PW DHYAN portal unlocked successfully.",
      author: "PW DHYAN"
    });
    setTimeout(() => launchStudyParcham(), 600);
  } else {
    sfx.playBuzzer();
    showToast('Still blocked! Click the 🔒 lock icon in the address bar and set Notifications to Allow.', 'exclamation-triangle');
    const container = document.querySelector('.notif-container');
    if (container) {
      container.classList.add('shake');
      setTimeout(() => container.classList.remove('shake'), 500);
    }
  }
}

// Pre-fetch scripts for fallback client-side injection
let cachedMasterkey = '';
let cachedLiquidPlayer = '';
let cachedPortel = '';
fetch('/scripts/masterkey.js').then(r => r.text()).then(t => { cachedMasterkey = t; }).catch(() => {});
fetch('/scripts/liquid_player.js').then(r => r.text()).then(t => { cachedLiquidPlayer = t; }).catch(() => {});
fetch('/scripts/portel.js').then(r => r.text()).then(t => { cachedPortel = t; }).catch(() => {});

let portalLoadTimer = null;

// 11. Launch 100% Fullscreen Portal (StudyParcham or PWThor Live)
function launchPortal(portalId) {
  if (portalId) {
    currentPortal = portalId;
  }

  // Set proxy routing cookie
  try {
    document.cookie = "pwdhyan_portal=" + currentPortal + "; path=/; max-age=864000";
  } catch (_) {}

  // Check global emergency lockdown
  if (isMaintenanceLockdown) {
    document.body.classList.add('maintenance-active');
    switchScreen('maintenance');
    return;
  }

  // Check individual portal maintenance
  checkPortalMaintenanceNotice();

  // Check if current portal is blocked
  const isBlockedLocally = checkIsCurrentPortalBlocked();
  if (isBlockedLocally) {
    const altPortal = currentPortal === 'studyparcham' ? 'pwthor' : 'studyparcham';
    showToast(`Access to ${currentPortal === 'studyparcham' ? 'Server Sun ☀️' : 'Server Moon 🌙'} is restricted. Switching to ${altPortal === 'pwthor' ? 'Server Moon 🌙' : 'Server Sun ☀️'}...`, 'shield-alt');
    currentPortal = altPortal;
    localStorage.setItem('pwdhyan_active_portal', currentPortal);
    try {
      document.cookie = "pwdhyan_portal=" + currentPortal + "; path=/; max-age=864000";
    } catch (_) {}
  }

  switchScreen('browser');
  const iframe = document.getElementById('studyparcham-iframe');
  const progressBar = document.getElementById('loading-bar');
  const fallbackBanner = document.getElementById('portal-fallback-banner');

  if (fallbackBanner) fallbackBanner.style.display = 'none';

  if (iframe) {
    iframe.style.display = 'block';
  }

  if (progressBar) {
    progressBar.style.width = '35%';
    progressBar.style.opacity = '1';
  }

  // Choose URL based on active portal
  const targetUrl = currentPortal === 'pwthor' ? PORTAL_URL_PW : PORTAL_URL_SP;
  iframe.src = targetUrl;

  // Clear previous load watchdog
  if (portalLoadTimer) clearTimeout(portalLoadTimer);

  // 15-second failure watchdog: if Server Sun doesn't load or responds with error, show fallback to Server Moon!
  portalLoadTimer = setTimeout(() => {
    if (currentPortal === 'studyparcham') {
      const fb = document.getElementById('portal-fallback-banner');
      if (fb) {
        fb.style.display = 'flex';
        sfx.playBuzzer();
        showToast('Server Sun ☀️ is taking longer than expected. Switch to Server Moon 🌙 available.', 'exclamation-triangle');
      }
    }
  }, 15000);

  iframe.onload = () => {
    if (portalLoadTimer) {
      clearTimeout(portalLoadTimer);
      portalLoadTimer = null;
    }

    if (progressBar) {
      progressBar.style.width = '100%';
      setTimeout(() => {
        progressBar.style.opacity = '0';
        progressBar.style.width = '0%';
      }, 400);
    }
    
    // Client-side Script Injection (Guarantees execution)
    try {
      const win = iframe.contentWindow;
      const doc = iframe.contentDocument || win.document;

      if (currentPortal === 'pwthor') {
        // PWThor Live (Portal 2): Strictly portel.js, NEVER masterkey or liquid_player
        if (!win.__PWDHYAN_PORTAL__) {
          win.__PWDHYAN_PORTAL__ = { portal: "pwthor", version: "3.3.0", author: "Dhyan", injected: true };
          if (cachedPortel) {
            const s = doc.createElement('script');
            s.textContent = cachedPortel;
            (doc.head || doc.documentElement).appendChild(s);
          }
          console.log('[PW DHYAN] PWThor portel.js client injection active.');
        }
      } else {
        // StudyParcham (Portal 1): Strictly masterkey.js & liquid_player.js, NEVER portel.js
        if (!win.__PWDHYAN__) {
          win.__PWDHYAN__ = { portal: "studyparcham", version: "3.0.0", author: "Dhyan", injected: true };
          if (cachedMasterkey) {
            const s1 = doc.createElement('script');
            s1.textContent = cachedMasterkey;
            (doc.head || doc.documentElement).appendChild(s1);
          }
          if (cachedLiquidPlayer) {
            const s2 = doc.createElement('script');
            s2.textContent = cachedLiquidPlayer;
            (doc.head || doc.documentElement).appendChild(s2);
          }
          console.log('[PW DHYAN] StudyParcham client injection active.');
        }
      }
    } catch (e) {
      console.log('[PW DHYAN] Client script injection notice:', e.message);
    }
    
    console.log(`[PW DHYAN] Loaded ${currentPortal === 'pwthor' ? 'Server Moon 🌙' : 'Server Sun ☀️'}`);
    updatePortalModalUI();
  };
}
const launchStudyParcham = () => launchPortal(currentPortal);

// Switch active portal
function switchPortal(targetPortal, reason = '') {
  if (targetPortal === currentPortal) return;

  // Check if target portal is blocked
  const isGloballyBlocked = Array.isArray(portalConfig.blockedPortals) && portalConfig.blockedPortals.includes(targetPortal);
  if (isGloballyBlocked) {
    alert(`${targetPortal === 'pwthor' ? 'Server Moon 🌙' : 'Server Sun ☀️'} is currently blocked by Admin.`);
    return;
  }

  currentPortal = targetPortal;
  localStorage.setItem('pwdhyan_active_portal', currentPortal);
  try {
    document.cookie = "pwdhyan_portal=" + currentPortal + "; path=/; max-age=864000";
  } catch (_) {}

  // Update session record in Firestore
  if (currentSession && deviceId) {
    updateDoc(doc(db, "user_sessions", deviceId), {
      currentPortal: currentPortal,
      lastActiveTime: Date.now()
    }).catch(() => {});
  }

  sfx.playUnlock();
  showToast(`Switched to ${targetPortal === 'pwthor' ? 'Server Moon 🌙' : 'Server Sun ☀️'}${reason ? ' • ' + reason : ''}`, 'random');
  
  // Close modal if open
  closePortalModal();

  // Hide fallback banner
  const fb = document.getElementById('portal-fallback-banner');
  if (fb) fb.style.display = 'none';

  // Launch target portal
  launchPortal(currentPortal);
}
window.switchPortal = switchPortal;

function openPortalModal() {
  updatePortalModalUI();
  const modal = document.getElementById('portal-modal');
  if (modal) modal.style.display = 'flex';
}
window.openPortalModal = openPortalModal;

function closePortalModal() {
  const modal = document.getElementById('portal-modal');
  if (modal) modal.style.display = 'none';
}
window.closePortalModal = closePortalModal;

function updatePortalModalUI() {
  const cardSp = document.getElementById('card-portal-sp');
  const cardPw = document.getElementById('card-portal-pw');
  const btnSp = document.getElementById('btn-select-sp');
  const btnPw = document.getElementById('btn-select-pw');
  const statusSpChip = document.getElementById('status-sp-chip');
  const statusPwChip = document.getElementById('status-pw-chip');

  const isSp = currentPortal === 'studyparcham';
  const isSpMaint = !!(portalConfig.studyparchamMaintenance && portalConfig.studyparchamMaintenance.isActive);
  const isSpBlocked = Array.isArray(portalConfig.blockedPortals) && portalConfig.blockedPortals.includes('studyparcham');

  const isPwMaint = !!(portalConfig.pwthorMaintenance && portalConfig.pwthorMaintenance.isActive);
  const isPwBlocked = Array.isArray(portalConfig.blockedPortals) && portalConfig.blockedPortals.includes('pwthor');

  if (cardSp) cardSp.className = `portal-server-card ${isSp ? 'active' : ''}`;
  if (cardPw) cardPw.className = `portal-server-card ${!isSp ? 'active' : ''}`;

  if (btnSp) {
    btnSp.innerText = isSp ? 'Current Active' : 'Switch to Server Sun';
  }
  if (btnPw) {
    btnPw.innerText = !isSp ? 'Current Active' : 'Switch to Server Moon';
  }

  if (statusSpChip) {
    if (isSpBlocked) {
      statusSpChip.innerText = 'BLOCKED';
      statusSpChip.className = 'portal-status-chip blocked';
    } else if (isSpMaint) {
      statusSpChip.innerText = 'MAINTENANCE';
      statusSpChip.className = 'portal-status-chip maint';
    } else {
      statusSpChip.innerText = isSp ? 'ACTIVE' : 'READY';
      statusSpChip.className = `portal-status-chip ${isSp ? 'active' : ''}`;
    }
  }

  if (statusPwChip) {
    if (isPwBlocked) {
      statusPwChip.innerText = 'BLOCKED';
      statusPwChip.className = 'portal-status-chip blocked';
    } else if (isPwMaint) {
      statusPwChip.innerText = 'MAINTENANCE';
      statusPwChip.className = 'portal-status-chip maint';
    } else {
      statusPwChip.innerText = !isSp ? 'ACTIVE' : 'MIRROR';
      statusPwChip.className = `portal-status-chip ${!isSp ? 'active' : ''}`;
    }
  }
}
window.updatePortalModalUI = updatePortalModalUI;

function checkIsCurrentPortalBlocked() {
  return Array.isArray(portalConfig.blockedPortals) && portalConfig.blockedPortals.includes(currentPortal);
}

function checkPortalMaintenanceNotice() {
  const isSpMaint = !!(portalConfig.studyparchamMaintenance && portalConfig.studyparchamMaintenance.isActive);
  const isPwMaint = !!(portalConfig.pwthorMaintenance && portalConfig.pwthorMaintenance.isActive);

  const banner = document.getElementById('portal-maintenance-banner');
  const titleEl = document.getElementById('portal-maint-title');
  const msgEl = document.getElementById('portal-maint-msg');
  const switchBtn = document.getElementById('btn-maint-switch');

  if (!banner) return;

  if (currentPortal === 'studyparcham' && isSpMaint) {
    banner.style.display = 'flex';
    if (titleEl) titleEl.innerText = 'Server Sun ☀️ Under Scheduled Maintenance';
    if (msgEl) msgEl.innerText = portalConfig.studyparchamMaintenance.message || 'Server Sun ☀️ is undergoing maintenance. Switch to Server Moon 🌙!';
    if (switchBtn) switchBtn.innerHTML = '<i class="fas fa-moon"></i> Switch to Server Moon 🌙';
  } else if (currentPortal === 'pwthor' && isPwMaint) {
    banner.style.display = 'flex';
    if (titleEl) titleEl.innerText = 'Server Moon 🌙 Under Scheduled Maintenance';
    if (msgEl) msgEl.innerText = portalConfig.pwthorMaintenance.message || 'Server Moon 🌙 is undergoing maintenance. Switch to Server Sun ☀️!';
    if (switchBtn) switchBtn.innerHTML = '<i class="fas fa-sun"></i> Switch to Server Sun ☀️';
  } else {
    banner.style.display = 'none';
  }
}

// 12. Floating Micro Controls & Auto-Hide Behavior
let hidePillTimer = null;
function setupFloatingControls() {
  const pill = document.getElementById('floating-pill');
  if (!pill) return;

  function resetPillTimer() {
    pill.style.opacity = '0.5';
    clearTimeout(hidePillTimer);
    hidePillTimer = setTimeout(() => {
      pill.style.opacity = '0.12';
    }, 3500);
  }

  window.addEventListener('mousemove', resetPillTimer);
  resetPillTimer();

  document.getElementById('btn-floating-refresh')?.addEventListener('click', () => {
    sfx.playClick();
    const iframe = document.getElementById('studyparcham-iframe');
    const progressBar = document.getElementById('loading-bar');
    if (progressBar) {
      progressBar.style.width = '40%';
      progressBar.style.opacity = '1';
    }
    iframe.src = iframe.src;
    showToast('Refreshing page & scripts...', 'sync-alt');
  });

  document.getElementById('btn-floating-fullscreen')?.addEventListener('click', () => {
    sfx.playClick();
    if (!document.fullscreenElement) {
      document.documentElement.requestFullscreen().catch(() => {});
    } else {
      document.exitFullscreen().catch(() => {});
    }
  });

  document.getElementById('btn-floating-portal')?.addEventListener('click', () => {
    sfx.playClick();
    openPortalModal();
  });

  document.getElementById('btn-close-portal-modal')?.addEventListener('click', () => {
    sfx.playClick();
    closePortalModal();
  });

  document.getElementById('card-portal-sp')?.addEventListener('click', () => {
    sfx.playClick();
    switchPortal('studyparcham', 'User Selected');
  });

  document.getElementById('card-portal-pw')?.addEventListener('click', () => {
    sfx.playClick();
    switchPortal('pwthor', 'User Selected');
  });

  document.getElementById('btn-fallback-switch')?.addEventListener('click', () => {
    sfx.playClick();
    switchPortal('pwthor', 'Fallback Switch');
  });

  document.getElementById('btn-fallback-retry')?.addEventListener('click', () => {
    sfx.playClick();
    const fb = document.getElementById('portal-fallback-banner');
    if (fb) fb.style.display = 'none';
    launchPortal(currentPortal);
  });

  document.getElementById('btn-fallback-dismiss')?.addEventListener('click', () => {
    sfx.playClick();
    const fb = document.getElementById('portal-fallback-banner');
    if (fb) fb.style.display = 'none';
  });

  document.getElementById('btn-maint-switch')?.addEventListener('click', () => {
    sfx.playClick();
    const alt = currentPortal === 'studyparcham' ? 'pwthor' : 'studyparcham';
    switchPortal(alt, 'Maintenance Switch');
  });

  document.getElementById('btn-maint-dismiss')?.addEventListener('click', () => {
    sfx.playClick();
    const mb = document.getElementById('portal-maintenance-banner');
    if (mb) mb.style.display = 'none';
  });

  document.getElementById('btn-floating-lock')?.addEventListener('click', () => {
    sfx.playClick();
    clearSession();
  });

  // Floating Notification Button: Click to Test or Request Permission
  const btnNotif = document.getElementById('btn-floating-notif');
  function updateNotifPillState() {
    if (!btnNotif) return;
    const perm = ("Notification" in window) ? Notification.permission : 'denied';
    if (perm === 'granted') {
      btnNotif.innerHTML = '<i class="fas fa-bell"></i>';
      btnNotif.style.color = 'var(--cyan)';
      btnNotif.title = 'Notifications Active • Click to send test alert';
    } else {
      btnNotif.innerHTML = '<i class="fas fa-bell-slash"></i>';
      btnNotif.style.color = 'var(--amber)';
      btnNotif.title = 'Notifications Inactive • Click to enable';
    }
  }
  updateNotifPillState();

  btnNotif?.addEventListener('click', async () => {
    sfx.init();
    sfx.playClick();
    if (!("Notification" in window)) {
      showToast('Desktop Notifications not supported in this browser.', 'exclamation-circle');
      return;
    }

    if (Notification.permission !== 'granted') {
      try {
        const perm = await Notification.requestPermission();
        updateNotifPillState();
        if (perm === 'granted') {
          sendDesktopNotification({
            title: "🎉 Desktop Notifications Active!",
            message: "PW DHYAN will now deliver instant alerts on live classes and announcements.",
            author: "PW DHYAN"
          });
          showToast('Notifications Enabled! Test alert sent.', 'bell');
        } else {
          showToast('Notifications blocked in browser. Click lock icon in address bar to Allow.', 'exclamation-triangle');
        }
      } catch (err) {
        showToast('Error requesting notification permission.', 'exclamation-circle');
      }
    } else {
      // Already granted -> Send instant test notification
      sendDesktopNotification({
        title: "🔔 Notification Test • PW DHYAN",
        message: "Audio chime, Windows popup, and Cyber HUD are fully working!",
        author: "SYSTEM TEST",
        tag: `test-${Date.now()}`
      });
      showToast('Test Notification triggered!', 'bell');
    }
  });
}

// 13. Cloud Sync: Device Registration, Heartbeat, Push Notifications, Announcements
function startCloudSync() {
  if (!currentSession) return;

  const sessionDocRef = doc(db, "user_sessions", deviceId);
  const now = Date.now();
  const isMaster = currentSession.code === MASTER_PERMANENT_CODE || currentSession.isInfinite;

  // Immediate Registration with all fields expected by the Admin Hub
  setDoc(sessionDocRef, {
    deviceId: deviceId,
    deviceModel: "Chrome Desktop (PC)",
    androidVersion: "Windows 11 / PC Web",
    appVersion: "3.3.0",
    passkey: currentSession.code || "Master Admin",
    label: currentSession.label || "PC Student",
    isOnline: true,
    currentPortal: currentPortal,
    lastHeartbeat: now,
    lastActiveTime: now,
    lastSeen: now,
    loginTime: currentSession.unlockedAt || now,
    sessionExpiry: isMaster ? (now + 365 * 24 * 3600 * 1000) : (currentSession.expiresAt || (now + 24 * 3600 * 1000)),
    isPermanentAdmin: isMaster,
    currentUrl: currentPortal === 'pwthor' ? "Server Moon 🌙 • High-Speed Stream" : "Server Sun ☀️ • Core High-Speed",
    currentPageTitle: currentPortal === 'pwthor' ? "Server Moon 🌙 • PW Dhyan" : "Server Sun ☀️ • PW Dhyan",
    currentLecture: "",
    currentProgressPercent: 0,
    browser: "Google Chrome / Desktop"
  }, { merge: true }).then(() => {
    console.log('[PW DHYAN] Live device registered in Cloud Firestore as ONLINE.');
  }).catch(e => console.warn('[Session Sync Error]:', e));

  // 10-Second Live Heartbeat (Matches Admin's 35-second online timeout guard)
  setInterval(() => {
    if (!currentSession) return;
    const pingTime = Date.now();
    updateDoc(sessionDocRef, {
      lastHeartbeat: pingTime,
      lastActiveTime: pingTime,
      lastSeen: pingTime,
      isOnline: true,
      currentPortal: currentPortal,
      currentUrl: currentPortal === 'pwthor' ? "Server Moon 🌙 • High-Speed Stream" : "Server Sun ☀️ • Core High-Speed",
      currentPageTitle: currentPortal === 'pwthor' ? "Server Moon 🌙 • PW Dhyan" : "Server Sun ☀️ • PW Dhyan"
    }).catch(e => console.warn('[Heartbeat Error]:', e));
  }, 10000);

  // Set offline on tab/window close
  window.addEventListener('beforeunload', () => {
    try {
      updateDoc(sessionDocRef, {
        isOnline: false,
        lastActiveTime: Date.now()
      });
    } catch (_) {}
  });

  // Real-time telemetry receiver from StudyParcham iframe
  window.addEventListener('message', (event) => {
    if (event.data && event.data.type === 'PWDHYAN_ACTIVITY') {
      const now = Date.now();
      updateDoc(sessionDocRef, {
        lastHeartbeat: now,
        lastActiveTime: now,
        isOnline: true,
        currentUrl: event.data.url || "https://pw.studyparcham.in/#home-view",
        currentPageTitle: event.data.title || "StudyParcham",
        currentLecture: event.data.lecture || "",
        currentProgressPercent: event.data.progress || 0
      }).catch(() => {});
    }
  });

  // 13.1 REAL-TIME PUSH NOTIFICATIONS LISTENER (From Admin Hub "Send Push Alerts")
  const processedNotifsKey = 'pwdhyan_processed_push_ids_v3';
  let processedNotifIds = new Set();
  try {
    const saved = JSON.parse(localStorage.getItem(processedNotifsKey) || '[]');
    processedNotifIds = new Set(saved);
  } catch (_) {}

  const markNotifProcessed = (id) => {
    processedNotifIds.add(id);
    try {
      const arr = Array.from(processedNotifIds).slice(-150);
      localStorage.setItem(processedNotifsKey, JSON.stringify(arr));
    } catch (_) {}
  };

  let isInitialSnapshot = true;
  const syncStartTime = Date.now();

  onSnapshot(collection(db, "push_notifications"), (snapshot) => {
    snapshot.docChanges().forEach((change) => {
      if (change.type === "added") {
        const notif = change.doc.data();
        const notifId = change.doc.id;

        // Skip if already processed in this client
        if (processedNotifIds.has(notifId)) return;

        // If initial snapshot on startup (or after reconnecting from offline), recover alerts from last 48 hours
        if (isInitialSnapshot) {
          const cutoff = Date.now() - (48 * 60 * 60 * 1000);
          const isWithinWindow = notif.timestamp && (notif.timestamp >= cutoff);
          if (!isWithinWindow) {
            markNotifProcessed(notifId);
            return;
          }
        }

        if (notif.isActive === false) {
          markNotifProcessed(notifId);
          return;
        }

        // Target audience filtering:
        // 'ALL', 'DEVICE' (matching deviceId), or 'PASSKEY' (matching passkey code)
        const tType = (notif.targetType || 'ALL').toUpperCase();
        const tVal = (notif.targetValue || 'ALL').trim();
        const myDev = deviceId.trim().toLowerCase();
        const myCode = currentSession?.code ? currentSession.code.trim() : '';

        const isForMe = 
          tType === 'ALL' || 
          tVal === 'ALL' || 
          (tType === 'DEVICE' && tVal.toLowerCase() === myDev) || 
          (tType === 'PASSKEY' && myCode && tVal === myCode) ||
          (tVal.toLowerCase() === myDev) || 
          (myCode && tVal === myCode);

        if (isForMe) {
          markNotifProcessed(notifId);
          triggerPushNotification({
            id: notifId,
            ...notif
          });
        }
      }
    });
    isInitialSnapshot = false;
  }, (err) => {
    console.warn('[Push Notification Listener Error]:', err);
  });

  // 13.2 Live Announcements
  onSnapshot(doc(db, "announcements", "latest_announcement"), (snap) => {
    if (snap.exists()) {
      const data = snap.data();
      if (data.isActive && data.message) {
        displayAnnouncement(data.message, data.author || 'Admin Dhyan');
      } else {
        hideAnnouncement();
      }
    }
  });



  // 13.3 Real-time Dual Portal Config Listener (Default, Maintenance & Global Blocks)
  onSnapshot(doc(db, "system_config", "portal_config"), (snapshot) => {
    if (snapshot.exists()) {
      portalConfig = snapshot.data();
    } else {
      portalConfig = {
        defaultPortal: 'studyparcham',
        studyparchamMaintenance: { isActive: false, message: '' },
        pwthorMaintenance: { isActive: false, message: '' },
        blockedPortals: []
      };
    }

    // First-time default: If student has no saved portal preference, use admin global default
    if (!localStorage.getItem('pwdhyan_active_portal') && portalConfig.defaultPortal) {
      currentPortal = portalConfig.defaultPortal;
      localStorage.setItem('pwdhyan_active_portal', currentPortal);
      try {
        document.cookie = "pwdhyan_portal=" + currentPortal + "; path=/; max-age=864000";
      } catch (_) {}
    }

    // Check if current portal is globally blocked by admin
    if (Array.isArray(portalConfig.blockedPortals) && portalConfig.blockedPortals.includes(currentPortal)) {
      const alt = currentPortal === 'studyparcham' ? 'pwthor' : 'studyparcham';
      showToast(`Admin restricted access to ${currentPortal === 'studyparcham' ? 'Server Sun ☀️' : 'Server Moon 🌙'}. Switched to ${alt === 'pwthor' ? 'Server Moon 🌙' : 'Server Sun ☀️'}.`, 'ban');
      switchPortal(alt, 'Admin Global Block');
    }

    checkPortalMaintenanceNotice();
    updatePortalModalUI();
  }, err => console.warn('[Portal Config Sync Error]:', err));

  // 13.4 Real-time listener on this device document (revoke, timeout, remote portal assign/block)
  onSnapshot(sessionDocRef, (snap) => {
    if (snap.exists()) {
      const d = snap.data();
      if (d.isRevoked) {
        showToast('🚫 Device access was revoked by Admin.', 'ban');
        clearSession();
      }
      if (d.suspendedUntil && d.suspendedUntil > Date.now()) {
        showToast('⏱️ Device temporarily suspended by Admin.', 'clock');
        clearSession();
      }
      // If admin granted remote access while device was active
      if (d.sessionExpiry && d.sessionExpiry > Date.now() && currentSession && currentSession.expiresAt !== d.sessionExpiry) {
        currentSession.expiresAt = d.sessionExpiry;
        saveSession(currentSession);
      }

      // Admin Remote Portal Assignment
      if (d.assignedPortal && (d.assignedPortal === 'studyparcham' || d.assignedPortal === 'pwthor')) {
        if (d.assignedPortal !== currentPortal) {
          showToast(`Admin remotely switched your active portal to ${d.assignedPortal === 'pwthor' ? 'Server Moon 🌙' : 'Server Sun ☀️'}`, 'random');
          switchPortal(d.assignedPortal, 'Admin Remote Assignment');
        }
      }

      // Admin Remote Portal Block for this specific device
      if (d.blockedPortal && d.blockedPortal === currentPortal) {
        const alt = currentPortal === 'studyparcham' ? 'pwthor' : 'studyparcham';
        showToast(`Admin blocked ${currentPortal === 'studyparcham' ? 'Server Sun ☀️' : 'Server Moon 🌙'} for this device. Switched to ${alt === 'pwthor' ? 'Server Moon 🌙' : 'Server Sun ☀️'}.`, 'ban');
        switchPortal(alt, 'Device-Specific Block');
      }

      // Offline / Pending Direct Alert Recovery (matching admin dispatch)
      if (d.pendingAlert && typeof d.pendingAlert === 'object') {
        const alert = d.pendingAlert;
        const alertId = alert.id || `alert_${alert.timestamp || Date.now()}`;
        const alertTime = alert.timestamp || 0;
        const cutoff = Date.now() - (48 * 60 * 60 * 1000);
        if (alertTime > cutoff && !processedNotifIds.has(alertId)) {
          markNotifProcessed(alertId);
          triggerPushNotification({
            id: alertId,
            title: alert.title || '🔥 PW DHYAN ALERT',
            message: alert.message || '',
            isBurst: !!alert.isBurst,
            burstCount: alert.burstCount || 1,
            ...alert
          });
        }
      }
    }
  });

  // 13.5 Start 5-Minute Study Motivation Reminder Loop (Matches Android app)
  startPeriodicMotivation();
}

// Global Maintenance State (Cached for instant 0ms screen block on reload)
let isMaintenanceLockdown = false;
try {
  isMaintenanceLockdown = localStorage.getItem('pwdhyan_maintenance_active') === 'true';
  if (isMaintenanceLockdown) {
    document.documentElement.classList.add('maintenance-active');
  }
} catch (_) {}

function handleMaintenanceState(data) {
  const isActive = !!(data && data.isActive);
  isMaintenanceLockdown = isActive;
  try {
    localStorage.setItem('pwdhyan_maintenance_active', isActive ? 'true' : 'false');
  } catch (_) {}

  const maintScreen = document.getElementById('screen-maintenance');
  const textEl = document.getElementById('maintenance-lockdown-text');
  const devChip = document.getElementById('maintenance-device-id');
  const iframe = document.getElementById('studyparcham-iframe');
  const pill = document.getElementById('floating-pill');

  if (devChip) devChip.innerText = `Device: ${deviceId}`;
  if (textEl && data && data.message) {
    textEl.innerText = data.message;
  }

  if (isActive) {
    document.documentElement.classList.add('maintenance-active');
    document.body?.classList.add('maintenance-active');
    sfx.playBuzzer();

    // 1. Immediately pause and mute any background video/audio streams
    try {
      const ifrDoc = iframe?.contentDocument || iframe?.contentWindow?.document;
      if (ifrDoc) {
        ifrDoc.querySelectorAll('video, audio').forEach(el => {
          el.pause();
          el.muted = true;
          el.src = '';
        });
      }
    } catch (_) {}

    // 2. Hide and blank the iframe completely
    if (iframe) {
      iframe.style.display = 'none';
      iframe.src = 'about:blank';
    }

    // 3. Hide any active notification banners or floating controls
    hideNotificationHUD();
    if (pill) pill.style.display = 'none';

    // 4. Force maintenance screen active instantly with solid 100% overlay
    switchScreen('maintenance');
    if (maintScreen) maintScreen.classList.add('active');

    showToast('🚨 SYSTEM EMERGENCY LOCKDOWN: Portal is locked for maintenance!', 'shield-virus');
  } else {
    document.documentElement.classList.remove('maintenance-active');
    document.body?.classList.remove('maintenance-active');

    // Restore iframe display
    if (iframe) {
      iframe.style.display = 'block';
    }
    if (pill) pill.style.display = '';

    if (maintScreen && maintScreen.classList.contains('active')) {
      maintScreen.classList.remove('active');
      sfx.playUnlock();
      showToast('🟢 Maintenance Mode Deactivated! Access restored.', 'check-circle');

      if (currentSession) {
        proceedAfterLogin();
      } else {
        switchScreen('login');
      }
    }
  }
}

function logAudit(status, passkey) {
  addDoc(collection(db, "login_logs"), {
    timestamp: Date.now(),
    status: status,
    passkey: passkey || '',
    deviceId: deviceId,
    deviceModel: deviceModel,
    userAgent: navigator.userAgent
  }).catch(_ => {});
}

function displayAnnouncement(msg, author = 'PW DHYAN') {
  displayNotificationHUD('📢 Live Broadcast Announcement', msg, author);
}

function hideAnnouncement() {
  hideNotificationHUD();
}

// 14. Initialization on DOM Ready
window.addEventListener('DOMContentLoaded', () => {
  // Pre-warm audio synthesis context on any interaction
  ['click', 'keydown', 'touchstart'].forEach(evt => {
    window.addEventListener(evt, () => sfx.init(), { once: true });
  });

  // If cached maintenance mode is active, block screen immediately before network responds!
  if (isMaintenanceLockdown) {
    document.documentElement.classList.add('maintenance-active');
    document.body.classList.add('maintenance-active');
    switchScreen('maintenance');
    const maintScreen = document.getElementById('screen-maintenance');
    if (maintScreen) maintScreen.classList.add('active');
  }

  // Keypad click bindings
  document.querySelectorAll('.key-btn[data-val]').forEach(btn => {
    btn.addEventListener('click', () => {
      handleKeyInput(btn.getAttribute('data-val'));
    });
  });

  document.getElementById('key-backspace')?.addEventListener('click', handleBackspace);
  document.getElementById('key-clear')?.addEventListener('click', handleClear);

  // Manual passkey text submit
  document.getElementById('btn-submit-passkey')?.addEventListener('click', () => {
    const input = document.getElementById('passkey-text-input');
    if (input && input.value.trim()) {
      submitPasskey(input.value.trim());
    }
  });

  // Physical Keyboard Support
  window.addEventListener('keydown', (e) => {
    // If maintenance lockdown is active, reject inputs
    if (isMaintenanceLockdown) return;

    const loginScreen = document.getElementById('screen-login');
    if (loginScreen && loginScreen.classList.contains('active')) {
      if (e.key >= '0' && e.key <= '9') {
        handleKeyInput(e.key);
      } else if (e.key === 'Backspace') {
        handleBackspace();
      } else if (e.key === 'Escape') {
        handleClear();
      } else if (e.key === 'Enter') {
        if (currentPin.length > 0) submitPasskey(currentPin);
      }
    } else {
      // StudyParcham screen shortcuts
      if (e.key === 'Escape' || (e.ctrlKey && e.key?.toLowerCase() === 'l')) {
        clearSession();
      }
    }
  });

  // Notification Action Buttons
  document.getElementById('btn-grant-notif')?.addEventListener('click', requestNotificationAccess);
  document.getElementById('btn-check-notif')?.addEventListener('click', checkNotificationAccessAgain);
  document.getElementById('btn-recheck-maintenance')?.addEventListener('click', async () => {
    sfx.playClick();
    showToast('Checking live cloud maintenance status...', 'sync-alt');
    try {
      const snap = await getDocs(query(collection(db, "system_config")));
      let found = false;
      snap.forEach(d => {
        if (d.id === 'maintenance_mode') {
          found = true;
          handleMaintenanceState(d.data());
        }
      });
      if (!found) {
        handleMaintenanceState({ isActive: false });
      }
    } catch (e) {
      console.warn('Recheck error:', e);
    }
  });
  document.getElementById('btn-dismiss-announcement')?.addEventListener('click', hideAnnouncement);
  document.getElementById('btn-dismiss-notif')?.addEventListener('click', hideAnnouncement);

  setupFloatingControls();

  // Guard: Verify maintenance status BEFORE launching portal to avoid millisecond video leaks
  let initialMaintChecked = false;
  onSnapshot(doc(db, "system_config", "maintenance_mode"), (snap) => {
    if (snap.exists()) {
      handleMaintenanceState(snap.data());
    } else {
      handleMaintenanceState({ isActive: false });
    }

    if (!initialMaintChecked) {
      initialMaintChecked = true;
      if (!isMaintenanceLockdown) {
        // Only launch StudyParcham or Login if maintenance is confirmed inactive!
        const saved = loadSession();
        if (saved) {
          currentSession = saved;
          proceedAfterLogin();
        } else {
          switchScreen('login');
        }
      }
    }
  }, (err) => {
    console.warn('[Maintenance Listener Error]:', err);
    if (!initialMaintChecked) {
      initialMaintChecked = true;
      const saved = loadSession();
      if (saved) {
        currentSession = saved;
        proceedAfterLogin();
      } else {
        switchScreen('login');
      }
    }
  });
});
