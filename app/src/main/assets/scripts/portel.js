// ==UserScript==
// @name         PW THOR Pure Liquid Glass Portal — Clean Edition (by Dhyan)
// @namespace    https://pwthor.live/
// @version      3.3
// @description  Exact Liquid Glass UI conversion for PW THOR: authentic ultra-clear frosted glass, auto-inject dhyan.p user, 32 accent themes, and complete removal of Telegram, Donate, and Contact pages without React crashes.
// @match        *://*.pwthor.live/*
// @match        *://pwthor.live/*
// @match        *://*.pwthor.site/*
// @match        *://pwthor.site/*
// @match        *://localhost/*
// @match        *://127.0.0.1/*
// @run-at       document-start
// @grant        none
// ==/UserScript==

(function () {
    'use strict';

    /* ==========================================================================
       1. ROUTE INTERCEPTOR & POPUP GUARD
       ========================================================================== */
    // If the user lands directly on or navigates to /contact, redirect to batches
    if (window.location.pathname.toLowerCase().includes('/contact')) {
        window.location.replace('/study/batches');
        return;
    }

    const nativeOpen = window.open;
    window.open = function (url, target, features) {
        const urlStr = String(url || '').toLowerCase();
        if (
            urlStr.includes('t.me') ||
            urlStr.includes('telegram') ||
            urlStr.includes('youtube.com') ||
            urlStr.includes('whatsapp') ||
            urlStr.includes('/contact')
        ) {
            return null;
        }
        return nativeOpen.call(window, url, target, features);
    };

    /* ==========================================================================
       2. 32 CURATED LIQUID GLASS THEMES
       ========================================================================== */
    const THEMES = [
        { id: 'platinum', name: 'Pure Platinum', accent: '#ffffff', accentB: '#cbd5e1', glow: 'rgba(255, 255, 255, 0.65)', bg: 'linear-gradient(135deg, #ffffff, #cbd5e1)' },
        { id: 'black', name: 'Obsidian Black', accent: '#ffffff', accentB: '#27272a', glow: 'rgba(255, 255, 255, 0.45)', bg: 'linear-gradient(135deg, #ffffff, #27272a)' },
        { id: 'oled', name: 'Pure OLED', accent: '#ffffff', accentB: '#171717', glow: 'rgba(255, 255, 255, 0.4)', bg: 'linear-gradient(135deg, #ffffff, #171717)' },
        { id: 'stealth', name: 'Stealth Carbon', accent: '#d4d4d8', accentB: '#27272a', glow: 'rgba(212, 212, 216, 0.4)', bg: 'linear-gradient(135deg, #d4d4d8, #27272a)' },
        { id: 'midnight', name: 'Midnight Dark', accent: '#38bdf8', accentB: '#0f172a', glow: 'rgba(56, 189, 248, 0.45)', bg: 'linear-gradient(135deg, #38bdf8, #0f172a)' },
        { id: 'dracula', name: 'Dracula Gothic', accent: '#bd93f9', accentB: '#282a36', glow: 'rgba(189, 147, 249, 0.45)', bg: 'linear-gradient(135deg, #bd93f9, #282a36)' },
        { id: 'charcoal', name: 'Smoky Charcoal', accent: '#94a3b8', accentB: '#1e293b', glow: 'rgba(148, 163, 184, 0.4)', bg: 'linear-gradient(135deg, #94a3b8, #1e293b)' },
        { id: 'cyan', name: 'Neon Cyan', accent: '#38bdf8', accentB: '#0284c7', glow: 'rgba(56, 189, 248, 0.45)', bg: 'linear-gradient(135deg, #38bdf8, #0284c7)' },
        { id: 'electric', name: 'Electric Blue', accent: '#3b82f6', accentB: '#1d4ed8', glow: 'rgba(59, 130, 246, 0.45)', bg: 'linear-gradient(135deg, #3b82f6, #1d4ed8)' },
        { id: 'matrix', name: 'Matrix Green', accent: '#22c55e', accentB: '#15803d', glow: 'rgba(34, 197, 94, 0.45)', bg: 'linear-gradient(135deg, #22c55e, #15803d)' },
        { id: 'cyberpunk', name: 'Cyberpunk Gold', accent: '#facc15', accentB: '#eab308', glow: 'rgba(250, 204, 21, 0.45)', bg: 'linear-gradient(135deg, #facc15, #eab308)' },
        { id: 'synthwave', name: 'Synthwave Pink', accent: '#f43f5e', accentB: '#ec4899', glow: 'rgba(244, 63, 94, 0.45)', bg: 'linear-gradient(135deg, #f43f5e, #ec4899)' },
        { id: 'plasma', name: 'Plasma Violet', accent: '#a855f7', accentB: '#7c3aed', glow: 'rgba(168, 85, 247, 0.45)', bg: 'linear-gradient(135deg, #a855f7, #7c3aed)' },
        { id: 'toxic', name: 'Toxic Lime', accent: '#a3e635', accentB: '#65a30d', glow: 'rgba(163, 230, 53, 0.45)', bg: 'linear-gradient(135deg, #a3e635, #65a30d)' },
        { id: 'cosmic', name: 'Cosmic Purple', accent: '#8b5cf6', accentB: '#6d28d9', glow: 'rgba(139, 92, 246, 0.45)', bg: 'linear-gradient(135deg, #8b5cf6, #6d28d9)' },
        { id: 'gold', name: 'Imperial Gold', accent: '#fbbf24', accentB: '#d97706', glow: 'rgba(251, 191, 36, 0.45)', bg: 'linear-gradient(135deg, #fbbf24, #d97706)' },
        { id: 'amber', name: 'Warm Amber', accent: '#f59e0b', accentB: '#b45309', glow: 'rgba(245, 158, 11, 0.45)', bg: 'linear-gradient(135deg, #f59e0b, #b45309)' },
        { id: 'orange', name: 'Blaze Orange', accent: '#ff7849', accentB: '#ea580c', glow: 'rgba(255, 120, 73, 0.45)', bg: 'linear-gradient(135deg, #ff7849, #ea580c)' },
        { id: 'bronze', name: 'Bronze Copper', accent: '#d97706', accentB: '#92400e', glow: 'rgba(217, 119, 6, 0.45)', bg: 'linear-gradient(135deg, #d97706, #92400e)' },
        { id: 'crimson', name: 'Royal Crimson', accent: '#e11d48', accentB: '#9f1239', glow: 'rgba(225, 29, 72, 0.45)', bg: 'linear-gradient(135deg, #e11d48, #9f1239)' },
        { id: 'ruby', name: 'Deep Ruby', accent: '#dc2626', accentB: '#991b1b', glow: 'rgba(220, 38, 38, 0.45)', bg: 'linear-gradient(135deg, #dc2626, #991b1b)' },
        { id: 'coral', name: 'Sunset Coral', accent: '#fb7185', accentB: '#e11d48', glow: 'rgba(251, 113, 133, 0.45)', bg: 'linear-gradient(135deg, #fb7185, #e11d48)' },
        { id: 'emerald', name: 'Emerald Forest', accent: '#10b981', accentB: '#059669', glow: 'rgba(16, 185, 129, 0.45)', bg: 'linear-gradient(135deg, #10b981, #059669)' },
        { id: 'mint', name: 'Crisp Mint', accent: '#2dd4bf', accentB: '#0d9488', glow: 'rgba(45, 212, 191, 0.45)', bg: 'linear-gradient(135deg, #2dd4bf, #0d9488)' },
        { id: 'teal', name: 'Ocean Teal', accent: '#14b8a6', accentB: '#0f766e', glow: 'rgba(20, 184, 166, 0.45)', bg: 'linear-gradient(135deg, #14b8a6, #0f766e)' },
        { id: 'rose', name: 'Velvet Rose', accent: '#f43f5e', accentB: '#be123c', glow: 'rgba(244, 63, 94, 0.45)', bg: 'linear-gradient(135deg, #f43f5e, #be123c)' },
        { id: 'pink', name: 'Bubblegum Pink', accent: '#ec4899', accentB: '#be185d', glow: 'rgba(236, 72, 153, 0.45)', bg: 'linear-gradient(135deg, #ec4899, #be185d)' },
        { id: 'sakura', name: 'Sakura Blossom', accent: '#f472b6', accentB: '#db2777', glow: 'rgba(244, 114, 182, 0.45)', bg: 'linear-gradient(135deg, #f472b6, #db2777)' },
        { id: 'lilac', name: 'Pastel Lilac', accent: '#c084fc', accentB: '#9333ea', glow: 'rgba(192, 132, 252, 0.45)', bg: 'linear-gradient(135deg, #c084fc, #9333ea)' },
        { id: 'lavender', name: 'French Lavender', accent: '#a78bfa', accentB: '#7c3aed', glow: 'rgba(167, 139, 250, 0.45)', bg: 'linear-gradient(135deg, #a78bfa, #7c3aed)' },
        { id: 'nord', name: 'Nord Arctic', accent: '#88c0d0', accentB: '#5e81ac', glow: 'rgba(136, 192, 208, 0.45)', bg: 'linear-gradient(135deg, #88c0d0, #5e81ac)' },
        { id: 'ice', name: 'Glacier Ice', accent: '#f1f5f9', accentB: '#94a3b8', glow: 'rgba(241, 245, 249, 0.55)', bg: 'linear-gradient(135deg, #f1f5f9, #94a3b8)' }
    ];

    let currentThemeId = localStorage.getItem('lq_pwthor_theme') || 'platinum';

    function applyPortalTheme(themeId) {
        const theme = THEMES.find(t => t.id === themeId) || THEMES[0];
        currentThemeId = theme.id;
        localStorage.setItem('lq_pwthor_theme', theme.id);

        const root = document.documentElement;
        root.style.setProperty('--lq-accent', theme.accent);
        root.style.setProperty('--lq-accent-b', theme.accentB);
        root.style.setProperty('--lq-accent-glow', theme.glow);

        document.querySelectorAll('.lq-portal-swatch').forEach(s => {
            s.classList.toggle('active', s.dataset.name === theme.id);
        });
    }

    /* ==========================================================================
       3. PURE LIQUID GLASS CSS SUITE & PURGE (Contact, Donate & Telegram)
       ========================================================================== */
    const style = document.createElement('style');
    style.id = 'pure-liquid-glass-pwthor-styles';
    style.innerHTML = `
        :root {
            --lq-accent: #38bdf8;
            --lq-accent-b: #0284c7;
            --lq-accent-glow: rgba(56, 189, 248, 0.45);
        }

        /* 3.1 Hard Purge: Kill Telegram, Donate, and Contact Us links */
        a[href*="t.me"],
        a[href*="telegram"],
        a[href*="/study/donate-batch"],
        a[href*="/contact"] {
            display: none !important;
            opacity: 0 !important;
            pointer-events: none !important;
            visibility: hidden !important;
            height: 0 !important;
            width: 0 !important;
        }

        /* 3.2 Ultra-Transparent Wallpaper */
        body, body.dark, html.dark body {
            background-color: transparent !important;
            background-image:
                url("https://raw.githubusercontent.com/Dhyan2404/std10/main/japan-artistic-3840x2160-25406%20(1).jpg"),
                url("https://github.com/Dhyan2404/std10/raw/main/japan-artistic-3840x2160-25406%20(1).jpg") !important;
            background-size: cover !important;
            background-position: center center !important;
            background-repeat: no-repeat !important;
            background-attachment: fixed !important;
            color: #f8fafc !important;
            min-height: 100vh !important;
        }

        @media (max-width: 768px) {
            body, body.dark, html.dark body {
                background-image: var(--phone-bg-img, url("https://i.pinimg.com/1200x/ac/48/a9/ac48a9af915de3fe560ed962a7efecdd.jpg")) !important;
                background-size: cover !important;
                background-position: center center !important;
                background-repeat: no-repeat !important;
                background-attachment: fixed !important;
                transition: background-image 0.8s ease-in-out !important;
            }
        }

        .bg-backgroud,
        .bg-background,
        main,
        .container {
            background: transparent !important;
        }

        /* 3.3 Ultra-Sheer Liquid Glass Header (blur 3px, opacity 0.14) */
        header, header.sticky {
            background: rgba(0, 0, 0, 0.14) !important;
            backdrop-filter: blur(3px) !important;
            -webkit-backdrop-filter: blur(3px) !important;
            border-bottom: 1px solid rgba(255, 255, 255, 0.12) !important;
            box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15) !important;
        }

        /* 3.4 Liquid Glass Sidebar (blur 8px, opacity 0.18) */
        aside {
            background: rgba(10, 15, 26, 0.18) !important;
            backdrop-filter: blur(8px) saturate(120%) !important;
            -webkit-backdrop-filter: blur(8px) saturate(120%) !important;
            border-right: 1px solid rgba(255, 255, 255, 0.14) !important;
            box-shadow: 10px 0 35px rgba(0, 0, 0, 0.25) !important;
        }
        aside .border-b {
            background: transparent !important;
            border-bottom: 1px solid rgba(255, 255, 255, 0.1) !important;
        }

        /* 3.5 Pure Liquid Glass Cards (blur 1px, opacity 0.10) */
        .container > .border,
        .container .bg-background.border {
            background: rgba(0, 0, 0, 0.12) !important;
            backdrop-filter: blur(2px) !important;
            -webkit-backdrop-filter: blur(2px) !important;
            border: 1px solid rgba(255, 255, 255, 0.15) !important;
            border-radius: 18px !important;
        }

        .border.rounded-xl {
            background: rgba(0, 0, 0, 0.1) !important;
            backdrop-filter: blur(1px) !important;
            -webkit-backdrop-filter: blur(1px) !important;
            border: 1px solid rgba(255, 255, 255, 0.22) !important;
            border-top: 1.2px solid rgba(255, 255, 255, 0.55) !important;
            border-radius: 18px !important;
            box-shadow: inset 0 1px 1px rgba(255, 255, 255, 0.18), 0 4px 16px rgba(0, 0, 0, 0.15) !important;
            color: #ffffff !important;
            transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1) !important;
        }
        .border.rounded-xl:hover {
            transform: translateY(-2px) scale(1.005) !important;
            background: rgba(255, 255, 255, 0.08) !important;
            border-color: var(--lq-accent) !important;
            border-top-color: rgba(255, 255, 255, 0.8) !important;
            box-shadow: 0 8px 24px rgba(0, 0, 0, 0.25), 0 0 16px var(--lq-accent-glow) !important;
        }

        /* Clean Glass Search Inputs */
        input.divshadow {
            background: rgba(0, 0, 0, 0.2) !important;
            border: 1px solid rgba(255, 255, 255, 0.2) !important;
            backdrop-filter: blur(4px) !important;
            color: #ffffff !important;
        }

        /* 3.6 Sidebar Logo Text Transformation */
        aside .border-b span.font-semibold {
            visibility: hidden;
            position: relative;
            display: inline-block;
        }
        aside .border-b span.font-semibold::after {
            content: "PW Mod";
            visibility: visible;
            position: absolute;
            left: 0;
            top: 0;
            background: linear-gradient(90deg, #ffffff, var(--lq-accent)) !important;
            -webkit-background-clip: text !important;
            -webkit-text-fill-color: transparent !important;
            font-weight: 800 !important;
            font-size: 1.15rem !important;
            letter-spacing: -0.2px !important;
            white-space: nowrap !important;
        }

        /* 3.7 Top 0 XP Badge replaced with VIP Crown Badge */
        header button[aria-haspopup="dialog"] {
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
        }
        header button[aria-haspopup="dialog"]:hover {
            transform: scale(1.05) !important;
            box-shadow: 0 0 20px var(--lq-accent-glow) !important;
            border-color: var(--lq-accent) !important;
        }
        header button[aria-haspopup="dialog"] span {
            display: none !important;
        }
        header button[aria-haspopup="dialog"]::after {
            content: "Made by Dhyan";
            color: #ffffff;
            font-weight: 800;
        }
        header button[aria-haspopup="dialog"] svg {
            color: #fbbf24 !important;
            fill: #fbbf24 !important;
        }

        /* 3.8 AUTO-INJECT dhyan.p USER IDENTITY */
        header .truncate.max-w-40 {
            visibility: hidden !important;
            position: relative !important;
            display: inline-block !important;
            width: 75px !important;
        }
        header .truncate.max-w-40::after {
            content: "dhyan.p";
            visibility: visible !important;
            position: absolute !important;
            left: 0 !important;
            top: 0 !important;
            font-weight: 700 !important;
            color: #ffffff !important;
            background: linear-gradient(90deg, #ffffff, var(--lq-accent));
            -webkit-background-clip: text;
            -webkit-text-fill-color: transparent;
        }

        /* Mask "TH" avatar circle with "DP" */
        header button .bg-muted {
            visibility: hidden !important;
            position: relative !important;
            background: linear-gradient(135deg, rgba(56, 189, 248, 0.3), rgba(2, 132, 199, 0.3)) !important;
            border: 1px solid rgba(255, 255, 255, 0.4) !important;
            box-shadow: 0 0 10px var(--lq-accent-glow) !important;
        }
        header button .bg-muted::after {
            content: "DP";
            visibility: visible !important;
            position: absolute !important;
            inset: 0 !important;
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
            font-size: 0.82rem !important;
            font-weight: 800 !important;
            color: #ffffff !important;
        }

        /* 3.9 Floating 32-Theme Switcher Button & Panel */
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
            transition: all 0.25s cubic-bezier(0.16, 1, 0.3, 1), opacity 0.35s ease;
            user-select: none;
            opacity: 0;
            pointer-events: none;
        }
        #lq-portal-theme-fab.visible {
            opacity: 1 !important;
            pointer-events: auto !important;
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

    // Inject stylesheet early
    if (document.head) {
        document.head.appendChild(style);
    } else {
        document.addEventListener("DOMContentLoaded", () => document.head.appendChild(style));
    }

    /* ==========================================================================
       4. CLIENT BRANDING & FLOATING THEME SELECTOR
       ========================================================================== */
    function injectFloatingThemeSelector() {
        if (document.getElementById('lq-portal-theme-fab')) return;

        const fab = document.createElement('div');
        fab.id = 'lq-portal-theme-fab';
        fab.title = 'Switch Liquid Glass Theme (Made by Dhyan)';
        fab.innerHTML = `🎨`;

        const panel = document.createElement('div');
        panel.id = 'lq-portal-theme-panel';
        panel.innerHTML = `
            <div style="display:flex; justify-content:space-between; align-items:center; border-bottom:1px solid rgba(255,255,255,0.15); padding-bottom:8px;">
                <span style="font-size:0.85rem; font-weight:800; color:#fff; display:flex; align-items:center; gap:6px;">
                    <span>Made by Dhyan</span>
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

        document.body.appendChild(fab);
        document.body.appendChild(panel);

        let fabTimer = null;
        function pingFabVisibility() {
            if (!fab) return;
            const isVideoActive = document.querySelector('video') || 
                                  window.location.href.includes('/player') || 
                                  window.location.href.includes('/watch') ||
                                  window.location.href.includes('lecture');
            if (isVideoActive && (!panel || !panel.classList.contains('show'))) {
                fab.classList.remove('visible');
                return;
            }
            fab.classList.add('visible');
            clearTimeout(fabTimer);
            fabTimer = setTimeout(() => {
                if (panel && !panel.classList.contains('show')) {
                    fab.classList.remove('visible');
                }
            }, 3500);
        }

        window.addEventListener('touchstart', pingFabVisibility, { passive: true });
        window.addEventListener('mousedown', pingFabVisibility, { passive: true });
        pingFabVisibility();

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

        const curThemeObj = THEMES.find(t => t.id === currentThemeId);
        if (curThemeObj) {
            document.getElementById('lq-theme-current-name').innerText = curThemeObj.name;
        }
    }

    function applyNonDestructiveUpdates() {
        if (!document.title.includes('Made by Dhyan')) {
            document.title = 'PW Mod • Made by Dhyan';
        }

        const nav = document.querySelector('aside nav');
        if (nav && !nav._dhyanCreditAdded) {
            nav._dhyanCreditAdded = true;
            const creditBox = document.createElement('div');
            creditBox.style.padding = '18px 10px 10px 10px';
            creditBox.style.marginTop = 'auto';
            creditBox.style.textAlign = 'center';
            creditBox.innerHTML = `
                <div class="lq-dhyan-credit-pill">
                    <span>👑 Liquid Glass • Made by Dhyan</span>
                </div>
            `;
            nav.appendChild(creditBox);
        }
    }

    /* ==========================================================================
       5. MOBILE ROTATING WALLPAPER ENGINE
       ========================================================================== */
    const PHONE_WALLPAPERS = [
        "https://i.pinimg.com/1200x/ac/48/a9/ac48a9af915de3fe560ed962a7efecdd.jpg",
        "https://i.pinimg.com/1200x/55/c4/d6/55c4d639e5aacc6c32f725caf13d96a3.jpg",
        "https://i.pinimg.com/736x/4b/c3/c9/4bc3c9cb5736476d52c3cb417d63cef8.jpg",
        "https://i.pinimg.com/1200x/31/21/7c/31217c3ef0a7d58c6b494a9d5e84626b.jpg"
    ];

    let currentPhoneWpIdx = 0;
    function cyclePhoneWallpaper() {
        const nextWp = PHONE_WALLPAPERS[currentPhoneWpIdx];
        document.documentElement.style.setProperty('--phone-bg-img', `url("${nextWp}")`);
        if (document.body) {
            document.body.style.setProperty('--phone-bg-img', `url("${nextWp}")`);
        }
        currentPhoneWpIdx = (currentPhoneWpIdx + 1) % PHONE_WALLPAPERS.length;
    }

    /* ==========================================================================
       6. BOOTSTRAP
       ========================================================================== */
    applyPortalTheme(currentThemeId);
    cyclePhoneWallpaper();
    setInterval(cyclePhoneWallpaper, 30000);

    function init() {
        applyNonDestructiveUpdates();
        injectFloatingThemeSelector();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }

    window.addEventListener('load', init);
    setInterval(applyNonDestructiveUpdates, 2000);
})();