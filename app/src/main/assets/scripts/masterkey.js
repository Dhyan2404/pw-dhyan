// ==UserScript==
// @name         StudyParcham Pure Liquid Glass Portal — Made by Dhyan
// @namespace    https://studyparcham.in/
// @version      2.2
// @description  Masterpiece Pure Liquid Glass UI transformation for StudyParcham: ultra-transparent frosted glass cards, 32 customizable accent themes, zero telegram popups or external redirects, permanent "PW Mod" & "Made by Dhyan" branding, verified email and instagram support, and full admin unlock.
// @match        https://studyparcham.in/*
// @match        https://www.studyparcham.in/*
// @match        https://pw.studyparcham.in/*
// @match        http://studyparcham.in/*
// @match        http://pw.studyparcham.in/*
// @run-at       document-start
// @grant        none
// ==/UserScript==

(function () {
    'use strict';

    // Strict Domain Restriction Guard: Exclusively executes on studyparcham.in and pw.studyparcham.in
    const host = window.location.hostname.toLowerCase();
    const isAllowedHost = host === 'studyparcham.in' ||
                          host === 'www.studyparcham.in' ||
                          host === 'pw.studyparcham.in';

    if (!isAllowedHost) {
        return; // Silently exit on any other domain
    }

    /* ==========================================================================
       1. SECURITY & ANTI-REDIRECT / ANTI-POPUP GATEWAY (MADE BY DHYAN)
       ========================================================================== */

    // 1.1 Block all annoying Telegram, subscriber, and donation alerts
    const originalAlert = window.alert;
    window.alert = function (msg) {
        const str = String(msg).toLowerCase();
        if (str.includes('telegram') || str.includes('subscriber') || str.includes('community') || str.includes('donate')) {
            console.log('[Made by Dhyan] Blocked annoying alert:', msg);
            return;
        }
        return originalAlert.apply(this, arguments);
    };

    // 1.2 Prevent scripts from hijacking native window.open or forcing Telegram / spam redirects
    let nativeOpen = window.open;
    const safeWindowOpen = function (url, target, features) {
        const urlStr = String(url || '').toLowerCase();

        // Allow Dhyan's Instagram profile to open cleanly
        if (urlStr.includes('instagram.com/dhyan._patel_')) {
            return nativeOpen.call(window, url, target || '_blank', features);
        }

        // Completely block external spam / telegram redirects
        if (
            urlStr.includes('t.me') ||
            urlStr.includes('telegram') ||
            urlStr.includes('generate.html') ||
            urlStr.includes('youtube.com') ||
            urlStr.includes('whatsapp')
        ) {
            console.log('[Made by Dhyan] Blocked external/telegram popup redirect to:', url);
            return null;
        }

        // Direct bypass for player and educational content: open immediately without verification screen
        return nativeOpen.call(window, url, target, features);
    };

    Object.defineProperty(window, 'open', {
        get: () => safeWindowOpen,
        set: () => { /* Silently discard override attempts */ },
        configurable: false
    });

    // 1.3 Neutralize OneSignal push popups
    window.OneSignalDeferred = [];
    window.OneSignal = {
        push: () => {},
        init: () => {},
        isPushNotificationsSupported: () => false,
        showSlidedownPrompt: () => {},
        registerForPushNotifications: () => {}
    };

    // 1.4 Intercept and approve authentication checks permanently
    const originalFetch = window.fetch;
    window.fetch = async function (resource, init) {
        const url = typeof resource === 'string' ? resource : resource?.url || '';
        if (url.includes('/api/auth')) {
            try {
                const body = init?.body ? JSON.parse(init.body) : {};
                if (body.action === 'verify_key' || body.action === 'check_access') {
                    return new Response(JSON.stringify({ success: true, system_off: true, access: 'unlimited', author: 'Dhyan' }), {
                        status: 200,
                        headers: { 'Content-Type': 'application/json' }
                    });
                }
            } catch (e) { }
        }
        return originalFetch.apply(this, arguments);
    };

    // 1.5 Keep localStorage permanently authorized
    const farFuture = Date.now() + (365 * 24 * 60 * 60 * 1000);
    ['pw', 'nt', 'mj'].forEach(prefix => {
        try {
            localStorage.setItem(`${prefix}_key_expires`, farFuture.toString());
            if (!localStorage.getItem(`${prefix}_access_key`)) {
                localStorage.setItem(`${prefix}_access_key`, 'made_by_dhyan_vip');
            }
        } catch (e) { }
    });

    /* ==========================================================================
       2. 32 CURATED LIQUID GLASS THEMES (SYNCHRONIZED WITH PLAYER)
       ========================================================================== */
    const THEMES = [
        { id: 'platinum', name: 'Pure Platinum', accent: '#ffffff', accentB: '#cbd5e1', glow: 'rgba(255, 255, 255, 0.65)', text: '#0f172a', bg: 'linear-gradient(135deg, #ffffff, #cbd5e1)' },
        { id: 'black', name: 'Obsidian Black', accent: '#ffffff', accentB: '#27272a', glow: 'rgba(255, 255, 255, 0.45)', text: '#0f172a', bg: 'linear-gradient(135deg, #ffffff, #27272a)' },
        { id: 'oled', name: 'Pure OLED', accent: '#ffffff', accentB: '#171717', glow: 'rgba(255, 255, 255, 0.4)', text: '#0f172a', bg: 'linear-gradient(135deg, #ffffff, #171717)' },
        { id: 'stealth', name: 'Stealth Carbon', accent: '#d4d4d8', accentB: '#27272a', glow: 'rgba(212, 212, 216, 0.4)', text: '#09090b', bg: 'linear-gradient(135deg, #d4d4d8, #27272a)' },
        { id: 'midnight', name: 'Midnight Dark', accent: '#38bdf8', accentB: '#0f172a', glow: 'rgba(56, 189, 248, 0.45)', text: '#0f172a', bg: 'linear-gradient(135deg, #38bdf8, #0f172a)' },
        { id: 'dracula', name: 'Dracula Gothic', accent: '#bd93f9', accentB: '#282a36', glow: 'rgba(189, 147, 249, 0.45)', text: '#282a36', bg: 'linear-gradient(135deg, #bd93f9, #282a36)' },
        { id: 'charcoal', name: 'Smoky Charcoal', accent: '#94a3b8', accentB: '#1e293b', glow: 'rgba(148, 163, 184, 0.4)', text: '#0f172a', bg: 'linear-gradient(135deg, #94a3b8, #1e293b)' },
        { id: 'cyan', name: 'Neon Cyan', accent: '#38bdf8', accentB: '#0284c7', glow: 'rgba(56, 189, 248, 0.45)', text: '#0f172a', bg: 'linear-gradient(135deg, #38bdf8, #0284c7)' },
        { id: 'electric', name: 'Electric Blue', accent: '#3b82f6', accentB: '#1d4ed8', glow: 'rgba(59, 130, 246, 0.45)', text: '#0f172a', bg: 'linear-gradient(135deg, #3b82f6, #1d4ed8)' },
        { id: 'matrix', name: 'Matrix Green', accent: '#22c55e', accentB: '#15803d', glow: 'rgba(34, 197, 94, 0.45)', text: '#052e16', bg: 'linear-gradient(135deg, #22c55e, #15803d)' },
        { id: 'cyberpunk', name: 'Cyberpunk Gold', accent: '#facc15', accentB: '#eab308', glow: 'rgba(250, 204, 21, 0.45)', text: '#422006', bg: 'linear-gradient(135deg, #facc15, #eab308)' },
        { id: 'synthwave', name: 'Synthwave Pink', accent: '#f43f5e', accentB: '#ec4899', glow: 'rgba(244, 63, 94, 0.45)', text: '#4c0519', bg: 'linear-gradient(135deg, #f43f5e, #ec4899)' },
        { id: 'plasma', name: 'Plasma Violet', accent: '#a855f7', accentB: '#7c3aed', glow: 'rgba(168, 85, 247, 0.45)', text: '#2e1065', bg: 'linear-gradient(135deg, #a855f7, #7c3aed)' },
        { id: 'toxic', name: 'Toxic Lime', accent: '#a3e635', accentB: '#65a30d', glow: 'rgba(163, 230, 53, 0.45)', text: '#1a2e05', bg: 'linear-gradient(135deg, #a3e635, #65a30d)' },
        { id: 'cosmic', name: 'Cosmic Purple', accent: '#8b5cf6', accentB: '#6d28d9', glow: 'rgba(139, 92, 246, 0.45)', text: '#2e1065', bg: 'linear-gradient(135deg, #8b5cf6, #6d28d9)' },
        { id: 'gold', name: 'Imperial Gold', accent: '#fbbf24', accentB: '#d97706', glow: 'rgba(251, 191, 36, 0.45)', text: '#451a03', bg: 'linear-gradient(135deg, #fbbf24, #d97706)' },
        { id: 'amber', name: 'Warm Amber', accent: '#f59e0b', accentB: '#b45309', glow: 'rgba(245, 158, 11, 0.45)', text: '#451a03', bg: 'linear-gradient(135deg, #f59e0b, #b45309)' },
        { id: 'orange', name: 'Blaze Orange', accent: '#ff7849', accentB: '#ea580c', glow: 'rgba(255, 120, 73, 0.45)', text: '#431407', bg: 'linear-gradient(135deg, #ff7849, #ea580c)' },
        { id: 'bronze', name: 'Bronze Copper', accent: '#d97706', accentB: '#92400e', glow: 'rgba(217, 119, 6, 0.45)', text: '#451a03', bg: 'linear-gradient(135deg, #d97706, #92400e)' },
        { id: 'crimson', name: 'Royal Crimson', accent: '#e11d48', accentB: '#9f1239', glow: 'rgba(225, 29, 72, 0.45)', text: '#4c0519', bg: 'linear-gradient(135deg, #e11d48, #9f1239)' },
        { id: 'ruby', name: 'Deep Ruby', accent: '#dc2626', accentB: '#991b1b', glow: 'rgba(220, 38, 38, 0.45)', text: '#450a0a', bg: 'linear-gradient(135deg, #dc2626, #991b1b)' },
        { id: 'coral', name: 'Sunset Coral', accent: '#fb7185', accentB: '#e11d48', glow: 'rgba(251, 113, 133, 0.45)', text: '#4c0519', bg: 'linear-gradient(135deg, #fb7185, #e11d48)' },
        { id: 'emerald', name: 'Emerald Forest', accent: '#10b981', accentB: '#059669', glow: 'rgba(16, 185, 129, 0.45)', text: '#022c22', bg: 'linear-gradient(135deg, #10b981, #059669)' },
        { id: 'mint', name: 'Crisp Mint', accent: '#2dd4bf', accentB: '#0d9488', glow: 'rgba(45, 212, 191, 0.45)', text: '#042f2e', bg: 'linear-gradient(135deg, #2dd4bf, #0d9488)' },
        { id: 'teal', name: 'Ocean Teal', accent: '#14b8a6', accentB: '#0f766e', glow: 'rgba(20, 184, 166, 0.45)', text: '#042f2e', bg: 'linear-gradient(135deg, #14b8a6, #0f766e)' },
        { id: 'rose', name: 'Velvet Rose', accent: '#f43f5e', accentB: '#be123c', glow: 'rgba(244, 63, 94, 0.45)', text: '#4c0519', bg: 'linear-gradient(135deg, #f43f5e, #be123c)' },
        { id: 'pink', name: 'Bubblegum Pink', accent: '#ec4899', accentB: '#be185d', glow: 'rgba(236, 72, 153, 0.45)', text: '#500724', bg: 'linear-gradient(135deg, #ec4899, #be185d)' },
        { id: 'sakura', name: 'Sakura Blossom', accent: '#f472b6', accentB: '#db2777', glow: 'rgba(244, 114, 182, 0.45)', text: '#500724', bg: 'linear-gradient(135deg, #f472b6, #db2777)' },
        { id: 'lilac', name: 'Pastel Lilac', accent: '#c084fc', accentB: '#9333ea', glow: 'rgba(192, 132, 252, 0.45)', text: '#3b0764', bg: 'linear-gradient(135deg, #c084fc, #9333ea)' },
        { id: 'lavender', name: 'French Lavender', accent: '#a78bfa', accentB: '#7c3aed', glow: 'rgba(167, 139, 250, 0.45)', text: '#2e1065', bg: 'linear-gradient(135deg, #a78bfa, #7c3aed)' },
        { id: 'nord', name: 'Nord Arctic', accent: '#88c0d0', accentB: '#5e81ac', glow: 'rgba(136, 192, 208, 0.45)', text: '#2e3440', bg: 'linear-gradient(135deg, #88c0d0, #5e81ac)' },
        { id: 'ice', name: 'Glacier Ice', accent: '#f1f5f9', accentB: '#94a3b8', glow: 'rgba(241, 245, 249, 0.55)', text: '#0f172a', bg: 'linear-gradient(135deg, #f1f5f9, #94a3b8)' }
    ];

    let currentThemeId = localStorage.getItem('lq_player_theme') || localStorage.getItem('lq_portal_theme') || 'platinum';

    function applyPortalTheme(themeId) {
        const theme = THEMES.find(t => t.id === themeId) || THEMES[0];
        currentThemeId = theme.id;
        localStorage.setItem('lq_player_theme', theme.id);
        localStorage.setItem('lq_portal_theme', theme.id);

        const root = document.documentElement;
        root.style.setProperty('--lq-accent', theme.accent);
        root.style.setProperty('--lq-accent-b', theme.accentB);
        root.style.setProperty('--lq-accent-glow', theme.glow);
        root.style.setProperty('--lq-accent-text', theme.text);
        root.style.setProperty('--lq-fill-gradient', `linear-gradient(90deg, ${theme.accentB}, ${theme.accent})`);
        root.style.setProperty('--primary', theme.accent);
        root.style.setProperty('--primary-light', 'rgba(255, 255, 255, 0.08)');

        document.querySelectorAll('.lq-portal-swatch').forEach(s => {
            s.classList.toggle('active', s.dataset.name === theme.id);
        });
    }

    /* ==========================================================================
       3. PURE LIQUID GLASS CSS SUITE FOR CARDS, HEADER & PORTAL
       ========================================================================== */
    const style = document.createElement('style');
    style.id = 'pure-liquid-glass-portal-styles';
    style.innerHTML = `
        :root {
            --lg-bg-color: linear-gradient(180deg, rgba(255, 255, 255, 0.08) 0%, rgba(255, 255, 255, 0.01) 50%, rgba(10, 15, 30, 0.12) 100%);
            --lg-highlight: rgba(255, 255, 255, 0.6);
            --lg-text: #ffffff;
            --lq-accent: #38bdf8;
            --lq-accent-b: #0284c7;
            --lq-accent-glow: rgba(56, 189, 248, 0.45);
            --lq-accent-text: #0f172a;
            --bg-body: transparent !important;
            --bg-card: rgba(255, 255, 255, 0.03) !important;
            --text-dark: #f8fafc !important;
            --text-muted: #cbd5e1 !important;
            --border-color: rgba(255, 255, 255, 0.14) !important;
        }

        /* 3.1 Ultra-Transparent Artistic Wallpaper Background (100% See-Through) */
        body, body.dark-mode {
            background-color: transparent !important;
            background-image: 
                url("https://raw.githubusercontent.com/Dhyan2404/std10/main/japan-artistic-3840x2160-25406%20(1).jpg"),
                url("https://github.com/Dhyan2404/std10/raw/main/japan-artistic-3840x2160-25406%20(1).jpg") !important;
            background-size: cover !important;
            background-position: center center !important;
            background-repeat: no-repeat !important;
            background-attachment: fixed !important;
            color: #f8fafc !important;
            font-family: 'Inter', -apple-system, sans-serif !important;
            min-height: 100vh !important;
        }

        @media (max-width: 768px) {
            body, body.dark-mode {
                background-image: var(--phone-bg-img, url("https://i.pinimg.com/1200x/ac/48/a9/ac48a9af915de3fe560ed962a7efecdd.jpg")) !important;
                background-size: cover !important;
                background-position: center center !important;
                background-repeat: no-repeat !important;
                background-attachment: fixed !important;
                transition: background-image 0.8s ease-in-out !important;
            }
        }

        #app-container, .view-section, .container, .container-fluid, .row, .col, .bg-body,
        .search-container, .date-strip, .offerings-container, .live-section {
            background: transparent !important;
        }

        .breadcrumb-pw, .nav-tabs-pw {
            background: rgba(0, 0, 0, 0.12) !important;
            backdrop-filter: blur(2px) !important;
            -webkit-backdrop-filter: blur(2px) !important;
            border-bottom: 1px solid rgba(255, 255, 255, 0.12) !important;
            color: #ffffff !important;
        }
        .bg-info-box {
            background: rgba(0, 0, 0, 0.12) !important;
            backdrop-filter: blur(2px) !important;
            border: 1px solid rgba(255, 255, 255, 0.15) !important;
            border-radius: 16px !important;
            color: #ffffff !important;
        }

        /* 3.2 Ultra-Sheer Liquid Glass Header (~90-95% see-through) */
        header {
            background: rgba(0, 0, 0, 0.14) !important;
            backdrop-filter: blur(3px) !important;
            -webkit-backdrop-filter: blur(3px) !important;
            border-bottom: 1px solid rgba(255, 255, 255, 0.12) !important;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15) !important;
            transition: all 0.3s ease !important;
        }
        header .logo-area {
            display: inline-flex !important;
            align-items: center !important;
            gap: 8px !important;
            text-decoration: none !important;
            white-space: nowrap !important;
            margin-right: 12px !important;
        }
        .logo-area .logo-text,
        header .logo-text,
        .sidebar-header .logo-text {
            background: linear-gradient(90deg, #ffffff, var(--lq-accent)) !important;
            -webkit-background-clip: text !important;
            -webkit-text-fill-color: transparent !important;
            font-weight: 800 !important;
            font-size: 1.18rem !important;
            letter-spacing: -0.2px !important;
            display: inline-block !important;
            white-space: nowrap !important;
        }

        /* 3.3 Made by Dhyan - Ultra-Clear Liquid Glass Top Badge */
        #key-timer-badge {
            background: rgba(255, 255, 255, 0.08) !important;
            backdrop-filter: blur(6px) !important;
            -webkit-backdrop-filter: blur(6px) !important;
            border: 1px solid rgba(255, 255, 255, 0.25) !important;
            border-top: 1.2px solid rgba(255, 255, 255, 0.55) !important;
            color: #ffffff !important;
            border-radius: 9999px !important;
            font-size: 0.78rem !important;
            font-weight: 800 !important;
            padding: 5px 14px !important;
            box-shadow: 0 0 14px var(--lq-accent-glow), inset 0 1px 1px rgba(255, 255, 255, 0.35) !important;
            display: inline-flex !important;
            align-items: center !important;
            gap: 7px !important;
            cursor: pointer !important;
            transition: all 0.25s ease !important;
            white-space: nowrap !important;
        }
        #key-timer-badge:hover {
            transform: scale(1.05) !important;
            box-shadow: 0 0 20px var(--lq-accent-glow) !important;
            border-color: var(--lq-accent) !important;
        }
        #key-timer-badge i {
            color: #fbbf24 !important;
            font-size: 0.85rem !important;
        }

        /* 3.4 STUDY DASHBOARD (SELECT BATCH BAR) - 100% SEE-THROUGH GLASS */
        .study-header-bg {
            background: rgba(0, 0, 0, 0.12) !important;
            backdrop-filter: blur(2px) !important;
            -webkit-backdrop-filter: blur(2px) !important;
            border-bottom: 1px solid rgba(255, 255, 255, 0.15) !important;
            box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1) !important;
            color: #ffffff !important;
        }
        .batch-dropdown {
            background: transparent !important;
            color: #ffffff !important;
            border: none !important;
            text-shadow: 0 1px 4px rgba(0, 0, 0, 0.8) !important;
        }
        .batch-dropdown option {
            background: #0f172a !important;
            color: #ffffff !important;
        }

        /* 3.5 ULTRA-TRANSPARENT 100% SEE-THROUGH CARDS & OFFERING ITEMS */
        .card-custom,
        .content-card,
        .offering-item,
        .live-card-vertical,
        .list-card,
        .teacher-card,
        .resource-row,
        .notif-item,
        .post-card,
        .card:not(.border-0) {
            background: rgba(0, 0, 0, 0.1) !important;
            backdrop-filter: blur(1px) !important;
            -webkit-backdrop-filter: blur(1px) !important;
            border: 1px solid rgba(255, 255, 255, 0.22) !important;
            border-top: 1.2px solid rgba(255, 255, 255, 0.55) !important;
            border-radius: 18px !important;
            box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.18), 0 4px 16px rgba(0, 0, 0, 0.15) !important;
            color: #ffffff !important;
            transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1) !important;
            overflow: hidden !important;
        }
        .card-custom:hover,
        .content-card:hover,
        .offering-item:hover,
        .live-card-vertical:hover,
        .list-card:hover,
        .teacher-card:hover {
            transform: translateY(-2px) scale(1.005) !important;
            background: rgba(255, 255, 255, 0.08) !important;
            border-color: var(--lq-accent) !important;
            border-top-color: rgba(255, 255, 255, 0.8) !important;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25), 0 0 16px var(--lq-accent-glow) !important;
        }

        /* 3.6 Crystal Glass Offering Icons */
        .offering-icon {
            background: rgba(255, 255, 255, 0.08) !important;
            border: 1px solid rgba(255, 255, 255, 0.22) !important;
            box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.3) !important;
            backdrop-filter: blur(3px) !important;
            -webkit-backdrop-filter: blur(3px) !important;
            border-radius: 12px !important;
        }
        .icon-blue { background: rgba(56, 189, 248, 0.18) !important; color: #38bdf8 !important; border-color: rgba(56, 189, 248, 0.4) !important; }
        .icon-indigo { background: rgba(129, 140, 248, 0.18) !important; color: #818cf8 !important; border-color: rgba(129, 140, 248, 0.4) !important; }
        .icon-community { background: rgba(244, 114, 182, 0.18) !important; color: #f472b6 !important; border-color: rgba(244, 114, 182, 0.4) !important; }

        /* 3.7 Text Contrast & Sharpening on Transparent Glass */
        .offering-title,
        .title-text,
        .offering-item span,
        .breadcrumb-pw span,
        .study-header-bg div,
        .live-section .offering-title,
        .nav-tab {
            text-shadow: 0 1px 4px rgba(0, 0, 0, 0.85) !important;
            color: #ffffff !important;
        }
        #live-classes-scroll,
        #live-classes-scroll h6,
        #live-classes-scroll i {
            color: #cbd5e1 !important;
            text-shadow: 0 1px 4px rgba(0, 0, 0, 0.85) !important;
        }

        /* 3.8 Card Thumbnails & Images with Clean Glass */
        .batch-thumb, .lecture-thumb, .live-thumb-sm {
            background: transparent !important;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
            border-radius: 18px 18px 0 0 !important;
        }

        /* 3.9 Liquid Glass Search Bar */
        .search-bar {
            background: rgba(0, 0, 0, 0.14) !important;
            backdrop-filter: blur(4px) !important;
            -webkit-backdrop-filter: blur(4px) !important;
            border: 1px solid rgba(255, 255, 255, 0.18) !important;
            border-top: 1.2px solid rgba(255, 255, 255, 0.45) !important;
            border-radius: 9999px !important;
            color: #ffffff !important;
            padding: 12px 22px !important;
            box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.2) !important;
            transition: all 0.25s ease !important;
        }
        .search-bar:focus {
            border-color: var(--lq-accent) !important;
            box-shadow: 0 0 16px var(--lq-accent-glow), inset 0 1px 1px rgba(255, 255, 255, 0.35) !important;
            outline: none !important;
        }
        .search-bar::placeholder { color: #cbd5e1 !important; }

        /* 3.10 Ultra-Sheer Liquid Glass Sidebar (~80-85% see-through) */
        .sidebar {
            background: rgba(10, 15, 26, 0.18) !important;
            backdrop-filter: blur(8px) saturate(120%) !important;
            -webkit-backdrop-filter: blur(8px) saturate(120%) !important;
            border-right: 1px solid rgba(255, 255, 255, 0.14) !important;
            box-shadow: 10px 0 35px rgba(0, 0, 0, 0.25) !important;
        }
        .sidebar-header {
            border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
            background: transparent !important;
        }
        .sidebar-links a {
            color: #f1f5f9 !important;
            background: rgba(255, 255, 255, 0.02) !important;
            border-bottom: none !important;
            border-radius: 12px !important;
            margin: 5px 12px !important;
            transition: all 0.2s ease !important;
            border: 1px solid rgba(255, 255, 255, 0.05) !important;
        }
        .sidebar-links a:hover, .sidebar-links a.active {
            background: rgba(255, 255, 255, 0.12) !important;
            color: var(--lq-accent) !important;
            border-color: rgba(255, 255, 255, 0.25) !important;
            border-top-color: rgba(255, 255, 255, 0.55) !important;
            box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.35), 0 0 12px var(--lq-accent-glow) !important;
            transform: translateX(4px) !important;
        }

        /* 3.8 Ultra-Transparent Modals, Popups, and Overlays */
        .popup-content, .modal-content, .notif-panel, .bottom-sheet {
            background: rgba(10, 15, 26, 0.32) !important;
            backdrop-filter: blur(16px) saturate(130%) !important;
            -webkit-backdrop-filter: blur(16px) saturate(130%) !important;
            border: 1px solid rgba(255, 255, 255, 0.18) !important;
            border-top: 1.4px solid rgba(255, 255, 255, 0.55) !important;
            border-radius: 24px !important;
            box-shadow: 0 20px 50px rgba(0, 0, 0, 0.35) !important;
            color: #ffffff !important;
        }

        #notif-content-area,
        .list-group-item {
            background: transparent !important;
            color: #ffffff !important;
        }

        /* 3.9 Hide Distracting External Popups / Bears / Ads / Support Donate */
        #donation-modal,
        #bruno-peeking-bear,
        .onesignal-slidedown-dialog,
        #onesignal-slidedown-container,
        iframe[src*="google"],
        ins.adsbygoogle,
        .fa-hand-holding-heart {
            display: none !important;
            opacity: 0 !important;
            pointer-events: none !important;
            visibility: hidden !important;
        }

        /* 3.10 Floating Liquid Glass Theme Switcher for Dhyan */
        #lq-portal-theme-fab {
            position: fixed;
            bottom: 24px;
            right: 24px;
            z-index: 999999;
            width: 46px;
            height: 46px;
            border-radius: 50%;
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.2) 0%, rgba(255, 255, 255, 0.04) 100%);
            border: 1px solid rgba(255, 255, 255, 0.28);
            border-top: 1.2px solid rgba(255, 255, 255, 0.6);
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.4), 0 0 14px var(--lq-accent-glow);
            display: flex;
            align-items: center;
            justify-content: center;
            color: #ffffff;
            cursor: pointer;
            font-size: 1.15rem;
            backdrop-filter: blur(14px);
            -webkit-backdrop-filter: blur(14px);
            transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1);
            user-select: none;
        }
        #lq-portal-theme-fab:hover {
            transform: scale(1.15) rotate(20deg);
            border-color: var(--lq-accent);
            box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5), 0 0 20px var(--lq-accent-glow);
        }
        #lq-portal-theme-panel {
            position: fixed;
            bottom: 80px;
            right: 24px;
            z-index: 999999;
            width: 290px;
            background: rgba(15, 23, 42, 0.88);
            backdrop-filter: blur(24px);
            -webkit-backdrop-filter: blur(24px);
            border: 1px solid rgba(255, 255, 255, 0.2);
            border-top: 1.4px solid rgba(255, 255, 255, 0.6);
            border-radius: 22px;
            padding: 14px 16px;
            display: none;
            flex-direction: column;
            gap: 10px;
            box-shadow: 0 16px 40px rgba(0, 0, 0, 0.5);
        }
        #lq-portal-theme-panel.show { display: flex; animation: slideUpFade 0.2s ease-out; }
        @keyframes slideUpFade { from { opacity: 0; transform: translateY(10px); } to { opacity: 1; transform: translateY(0); } }

        .lq-portal-swatches-grid {
            display: grid; grid-template-columns: repeat(8, 1fr); gap: 6px; max-height: 110px; overflow-y: auto; padding: 2px;
        }
        .lq-portal-swatch {
            width: 22px; height: 22px; border-radius: 50%; cursor: pointer;
            border: 1.5px solid rgba(255, 255, 255, 0.45); box-shadow: 0 1px 4px rgba(0, 0, 0, 0.4);
            transition: all 0.2s;
        }
        .lq-portal-swatch:hover { transform: scale(1.25); border-color: #ffffff; }
        .lq-portal-swatch.active { border-color: #ffffff; transform: scale(1.3); box-shadow: 0 0 10px #ffffff; }

        /* 3.11 Made by Dhyan Watermark Chip */
        .lq-dhyan-credit-pill {
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.1) 0%, rgba(255, 255, 255, 0.02) 100%);
            border: 1px solid rgba(255, 255, 255, 0.2);
            border-top: 1.2px solid rgba(255, 255, 255, 0.5);
            border-radius: 999px;
            padding: 6px 14px;
            font-size: 0.74rem;
            font-weight: 700;
            color: #ffffff;
            display: inline-flex;
            align-items: center;
            gap: 6px;
            box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.25);
            text-decoration: none;
        }
    `;
    (document.head || document.documentElement).appendChild(style);

    /* ==========================================================================
       4. DOM BRANDING ENGINE — PW MOD & MADE BY DHYAN
       ========================================================================== */

    function applyDhyanBranding() {
        // 4.1 Page Title
        if (!document.title.includes('Made by Dhyan')) {
            document.title = 'PW Mod • Made by Dhyan';
        }

        // 4.2 Top Bar Badge: Lock to "Made by Dhyan" with crown
        const badge = document.getElementById('key-timer-badge');
        const timerText = document.getElementById('key-timer-text');
        if (badge && timerText) {
            badge.classList.remove('danger', 'd-none');
            badge.style.display = 'inline-flex';
            timerText.innerText = 'Made by Dhyan';
            timerText.dataset.locked = 'true';

            // Replace icon with a glowing crown
            const icon = badge.querySelector('i');
            if (icon && !icon.classList.contains('fa-crown')) {
                icon.className = 'fas fa-crown';
            }

            // Lock setters
            if (!badge._dhyanLocked) {
                badge._dhyanLocked = true;
                Object.defineProperty(timerText, 'innerText', {
                    get: () => 'Made by Dhyan',
                    set: () => {},
                    configurable: true
                });
                Object.defineProperty(timerText, 'textContent', {
                    get: () => 'Made by Dhyan',
                    set: () => {},
                    configurable: true
                });
            }
        }

        // 4.3 Header & Sidebar Logo Text: Set to "PW Mod"
        document.querySelectorAll('header .logo-text, .sidebar-header .logo-text, .logo-text').forEach(el => {
            if (el.innerText !== 'PW Mod') {
                el.innerText = 'PW Mod';
            }
        });

        // 4.4 Sidebar Branding & Clean-up (Remove "Support / Donate" completely)
        const sidebar = document.getElementById('sidebar');
        if (sidebar) {
            const linksContainer = sidebar.querySelector('.sidebar-links');
            if (linksContainer) {
                // Remove Support / Donate text node or icon
                Array.from(linksContainer.childNodes).forEach(node => {
                    if (node.nodeType === Node.TEXT_NODE && node.textContent.includes('Support / Donate')) {
                        node.remove();
                    } else if (node.nodeType === Node.ELEMENT_NODE) {
                        if (
                            node.innerText?.includes('Support / Donate') ||
                            node.classList?.contains('fa-hand-holding-heart') ||
                            node.querySelector?.('.fa-hand-holding-heart')
                        ) {
                            node.remove();
                        }
                    }
                });
            }

            if (!sidebar._dhyanBranded) {
                sidebar._dhyanBranded = true;

                // Add bottom credit badge
                if (linksContainer) {
                    const creditBox = document.createElement('div');
                    creditBox.style.padding = '18px 16px 20px 16px';
                    creditBox.style.marginTop = 'auto';
                    creditBox.style.borderTop = '1px solid rgba(255,255,255,0.1)';
                    creditBox.style.textAlign = 'center';
                    creditBox.innerHTML = `
                        <div class="lq-dhyan-credit-pill">
                            <i class="fas fa-crown text-warning"></i>
                            <span>Liquid Glass • Made by Dhyan</span>
                        </div>
                    `;
                    sidebar.appendChild(creditBox);
                }
            }
        }

        // 4.5 Clean & Custom Contact & Support View (Strictly Email & Dhyan's Instagram only)
        const contactContainer = document.querySelector('#contact-view .container');
        if (contactContainer && (!contactContainer._dhyanCustomized || contactContainer.querySelector('a[href*="youtube.com"], a[href*="t.me"]'))) {
            contactContainer._dhyanCustomized = true;
            contactContainer.innerHTML = `
                <h6 class="fw-bold text-uppercase tracking-wider mb-3 ps-2" style="font-size: 0.85rem; color: var(--lq-accent); letter-spacing: 1px;">
                    <i class="fas fa-crown text-warning me-1"></i> Contact & Support
                </h6>
                <div class="card border-0 shadow-sm rounded-4 mb-4" style="background: rgba(255, 255, 255, 0.04) !important; backdrop-filter: blur(12px) saturate(130%) !important; -webkit-backdrop-filter: blur(12px) saturate(130%) !important; border: 1px solid rgba(255, 255, 255, 0.18) !important; border-top: 1.5px solid rgba(255, 255, 255, 0.55) !important; border-radius: 20px !important; overflow: hidden;">
                    <div class="list-group list-group-flush" style="background: transparent;">
                        
                        <!-- Email Support -->
                        <a href="mailto:dhyan20190@gmail.com" class="list-group-item list-group-item-action d-flex align-items-center p-3" style="background: transparent !important; border-color: rgba(255, 255, 255, 0.08) !important; text-decoration: none; transition: all 0.25s ease;">
                            <div class="d-flex align-items-center justify-content-center rounded-circle me-3" style="width: 48px; height: 48px; background: rgba(56, 189, 248, 0.16); color: #38bdf8; font-size: 1.3rem; border: 1px solid rgba(56, 189, 248, 0.35); box-shadow: 0 0 12px rgba(56, 189, 248, 0.25);">
                                <i class="fas fa-envelope"></i>
                            </div>
                            <div class="flex-grow-1">
                                <h6 class="mb-1 fw-bold" style="color: #ffffff; font-size: 1.05rem;">Email Support</h6>
                                <small style="color: #cbd5e1; font-size: 0.88rem;">dhyan20190@gmail.com</small>
                            </div>
                            <i class="fas fa-arrow-right text-muted" style="font-size: 0.85rem;"></i>
                        </a>

                        <!-- Instagram -->
                        <a href="https://www.instagram.com/dhyan._patel_/" target="_blank" rel="noopener noreferrer" class="list-group-item list-group-item-action d-flex align-items-center p-3" style="background: transparent !important; border-color: transparent !important; text-decoration: none; transition: all 0.25s ease;">
                            <div class="d-flex align-items-center justify-content-center rounded-circle me-3" style="width: 48px; height: 48px; background: rgba(244, 63, 94, 0.16); color: #f43f5e; font-size: 1.3rem; border: 1px solid rgba(244, 63, 94, 0.35); box-shadow: 0 0 12px rgba(244, 63, 94, 0.25);">
                                <i class="fab fa-instagram"></i>
                            </div>
                            <div class="flex-grow-1">
                                <h6 class="mb-1 fw-bold" style="color: #ffffff; font-size: 1.05rem;">Instagram</h6>
                                <small style="color: #cbd5e1; font-size: 0.88rem;">@dhyan._patel_</small>
                            </div>
                            <i class="fas fa-external-link-alt text-muted" style="font-size: 0.85rem;"></i>
                        </a>

                    </div>
                </div>
            `;
        }

        // 4.6 Clean out any other Telegram links & external promos
        document.querySelectorAll('a[href*="t.me"], a[href*="telegram"]').forEach(a => {
            a.href = 'javascript:void(0)';
            a.target = '_self';
            a.onclick = (e) => {
                e.preventDefault();
                e.stopPropagation();
            };
            const h6 = a.querySelector('h6');
            if (h6) h6.innerText = 'Made by Dhyan (Verified)';
            const small = a.querySelector('small');
            if (small) small.innerText = 'All features unlocked by Dhyan';
            const icon = a.querySelector('i');
            if (icon) icon.className = 'fas fa-shield-alt text-warning';
        });

        // 4.7 Kill the Peeking Bear and Donation Popup
        const bear = document.getElementById('bruno-peeking-bear');
        if (bear) bear.remove();
        const donationModal = document.getElementById('donation-modal');
        if (donationModal) donationModal.remove();
    }

    /* ==========================================================================
       5. FLOATING THEME SELECTOR ENGINE (32 THEMES)
       ========================================================================== */
    function injectFloatingThemeSelector() {
        if (document.getElementById('lq-portal-theme-fab')) return;

        // Floating Action Button
        const fab = document.createElement('div');
        fab.id = 'lq-portal-theme-fab';
        fab.title = 'Switch Liquid Glass Theme (Made by Dhyan)';
        fab.innerHTML = '<i class="fas fa-palette"></i>';

        // Floating Palette Panel
        const panel = document.createElement('div');
        panel.id = 'lq-portal-theme-panel';
        panel.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid rgba(255,255,255,0.15); padding-bottom:8px;">
                <span style="font-size:0.85rem; font-weight:800; color:#fff; display:flex; align-items:center; gap:6px;">
                    <i class="fas fa-crown text-warning"></i> <span>Made by Dhyan</span>
                </span>
                <span style="font-size:0.75rem; font-family:monospace; color:var(--lq-accent);" id="lq-theme-current-name">Platinum</span>
            </div>
            <div style="font-size:0.72rem; color:#94a3b8; font-weight:700; text-transform:uppercase; letter-spacing:0.5px;">Theme Accents (32)</div>
            <div class="lq-portal-swatches-grid" id="lq-portal-swatches"></div>
        `;

        fab.onclick = (e) => {
            e.stopPropagation();
            panel.classList.toggle('show');
        };
        document.addEventListener('click', (e) => {
            if (!panel.contains(e.target) && e.target !== fab) {
                panel.classList.remove('show');
            }
        });

        (document.body || document.documentElement).appendChild(fab);
        (document.body || document.documentElement).appendChild(panel);

        // Render Swatches
        const swatchesGrid = panel.querySelector('#lq-portal-swatches');
        THEMES.forEach(t => {
            const swatch = document.createElement('div');
            swatch.className = `lq-portal-swatch ${t.id === currentThemeId ? 'active' : ''}`;
            swatch.dataset.name = t.id;
            swatch.title = t.name;
            swatch.style.background = t.bg;
            swatch.onclick = (e) => {
                e.stopPropagation();
                applyPortalTheme(t.id);
                document.getElementById('lq-theme-current-name').innerText = t.name;
            };
            swatchesGrid.appendChild(swatch);
        });

        // Set initial theme name
        const curThemeObj = THEMES.find(t => t.id === currentThemeId);
        if (curThemeObj) {
            document.getElementById('lq-theme-current-name').innerText = curThemeObj.name;
        }
    }

    /* ==========================================================================
       6. BOOTSTRAP & RESILIENT MUTATION OBSERVER
       ========================================================================== */
    applyPortalTheme(currentThemeId);

    function initPortalGlass() {
        applyDhyanBranding();
        injectFloatingThemeSelector();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initPortalGlass);
    } else {
        initPortalGlass();
    }

    window.addEventListener('load', initPortalGlass);

    // Continuous protection & branding guard
    const observer = new MutationObserver(() => {
        applyDhyanBranding();
    });
    observer.observe(document.documentElement, { childList: true, subtree: true });

    // Periodic safety sweep (every 2.5s)
    setInterval(applyDhyanBranding, 2500);

    /* ==========================================================================
       7. DYNAMIC PHONE WALLPAPER ROTATOR ENGINE (30s ROTATION)
       ========================================================================== */
    const PHONE_WALLPAPERS = [
        "https://i.pinimg.com/1200x/ac/48/a9/ac48a9af915de3fe560ed962a7efecdd.jpg",
        "https://i.pinimg.com/1200x/55/c4/d6/55c4d639e5aacc6c32f725caf13d96a3.jpg",
        "https://i.pinimg.com/736x/4b/c3/c9/4bc3c9cb5736476d52c3cb417d63cef8.jpg",
        "https://i.pinimg.com/1200x/31/21/7c/31217c3ef0a7d58c6b494a9d5e84626b.jpg"
    ];

    // Preload wallpapers into browser cache for instant transitions
    PHONE_WALLPAPERS.forEach(src => {
        try {
            const img = new Image();
            img.src = src;
        } catch (e) { }
    });

    let currentPhoneWpIdx = 0;
    function cyclePhoneWallpaper() {
        const nextWp = PHONE_WALLPAPERS[currentPhoneWpIdx];
        document.documentElement.style.setProperty('--phone-bg-img', `url("${nextWp}")`);
        if (document.body) {
            document.body.style.setProperty('--phone-bg-img', `url("${nextWp}")`);
        }
        currentPhoneWpIdx = (currentPhoneWpIdx + 1) % PHONE_WALLPAPERS.length;
    }

    cyclePhoneWallpaper();
    setInterval(cyclePhoneWallpaper, 30000); // Rotates every 30 seconds

})();