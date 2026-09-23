// ==UserScript==
// @name         StudyParcham Pure Liquid Glass Player
// @namespace    https://studyparcham.in/
// @version      49.1
// @description  Masterpiece Liquid Glass UI with fixed scope and syntax repairs.
// @match        *://*.studyparcham.in/*
// @match        *://*/*player*
// @include      *player.html*
// @include      *studyparcham.in*
// @include      *://*/*player*
// @include      *://localhost/*
// @include      *://127.0.0.1/*
// @include      file://*
// @run-at       document-start
// @grant        unsafeWindow
// ==/UserScript==

(function () {
    'use strict';
    const win = typeof unsafeWindow !== 'undefined' ? unsafeWindow : window;

    /* 0. ANTI-DEBUGGER & DEVTOOLS UNLOCKER */
    try {
        // Prevent anti-debugging scripts from wiping console
        console.clear = () => { };

        // Neutralize dynamic Function("debugger")() calls
        const originalFunction = window.Function;
        const hookedFunction = function (...args) {
            if (args.length > 0 && typeof args[args.length - 1] === 'string') {
                if (args[args.length - 1].includes('debugger')) {
                    return function () { };
                }
            }
            return originalFunction.apply(this, args);
        };
        hookedFunction.prototype = originalFunction.prototype;
        window.Function = hookedFunction;
        Function.prototype.constructor = hookedFunction;

        // Neutralize eval("debugger") calls
        const originalEval = window.eval;
        window.eval = function (code) {
            if (typeof code === 'string' && code.includes('debugger')) {
                code = code.replace(/\bdebugger\b/g, '');
            }
            return originalEval.call(this, code);
        };

        // Neutralize setInterval & setTimeout anti-debugger and devtools-detection loops
        const originalSetInterval = window.setInterval;
        window.setInterval = function (fn, delay, ...args) {
            if (typeof fn === 'string' && fn.includes('debugger')) return -1;
            if (typeof fn === 'function') {
                const fnStr = fn.toString();
                if (fnStr.includes('debugger') || fnStr.includes('devtoolsCheck') || fnStr.includes('studyparcham-devtools-detected')) {
                    return -1;
                }
            }
            return originalSetInterval.call(this, fn, delay, ...args);
        };

        const originalSetTimeout = window.setTimeout;
        window.setTimeout = function (fn, delay, ...args) {
            if (typeof fn === 'string' && fn.includes('debugger')) return -1;
            if (typeof fn === 'function') {
                const fnStr = fn.toString();
                if (fnStr.includes('debugger') || fnStr.includes('devtoolsCheck')) {
                    return -1;
                }
            }
            return originalSetTimeout.call(this, fn, delay, ...args);
        };

        // Suppress devtools detection events
        const originalDispatch = window.dispatchEvent;
        window.dispatchEvent = function (event) {
            if (event && (event.type === 'studyparcham-devtools-detected' || event.type?.includes('devtools'))) {
                return true;
            }
            return originalDispatch.apply(this, arguments);
        };

        // Unblock DevTools shortcuts and right-click
        window.addEventListener('keydown', (e) => {
            const key = e.key?.toLowerCase();
            if (e.key === 'F12' || (e.ctrlKey && e.shiftKey && ['i', 'j', 'c'].includes(key)) || (e.ctrlKey && ['u', 's'].includes(key))) {
                e.stopImmediatePropagation();
            }
        }, true);
        window.addEventListener('contextmenu', (e) => {
            e.stopImmediatePropagation();
        }, true);
    } catch (e) { }

    function isPlayerPageActive() {
        const href = (window.location.href || '').toLowerCase();
        return href.includes('player') ||
            href.includes('live') ||
            href.includes('videoid=') ||
            href.includes('vurl=') ||
            href.includes('lectureid=') ||
            (document.querySelector('video, #video-wrapper, .video-js') !== null);
    }

    try {
        if (isPlayerPageActive() && window.AndroidPlayerBridge && window.AndroidPlayerBridge.onLecturePlayerDetected) {
            window.AndroidPlayerBridge.onLecturePlayerDetected(true);
        }
    } catch (e) { }

    /* 1. AUTH GATEWAY PASS-THROUGH */
    const originalFetch = window.fetch;
    window.fetch = async function (resource, init) {
        const url = typeof resource === 'string' ? resource : resource?.url || '';
        if (url.includes('/api/auth')) {
            try {
                const body = init?.body ? JSON.parse(init.body) : {};
                if (body.action === 'verify_key') {
                    return new Response(JSON.stringify({ success: true, system_off: true }), {
                        status: 200, headers: { 'Content-Type': 'application/json' }
                    });
                }
            } catch (e) { }
        }
        return originalFetch.apply(this, arguments);
    };

    /* 2. REFINED LIQUID GLASS CSS SUITE */
    const style = document.createElement('style');
    style.id = 'pure-liquid-glass-styles';
    style.innerHTML = `
        :root {
            --lg-bg-color: linear-gradient(180deg, rgba(255, 255, 255, 0.16) 0%, rgba(255, 255, 255, 0.05) 50%, rgba(10, 15, 30, 0.22) 100%);
            --lg-highlight: rgba(255, 255, 255, 0.65);
            --lg-text: #ffffff;
            --lg-hover-glow: rgba(255, 255, 255, 0.4);
            --lq-accent: #38bdf8;
            --lq-accent-b: #0284c7;
            --lq-accent-glow: rgba(56, 189, 248, 0.45);
            --lq-accent-text: #0f172a;
        }
        .glass-filter { position: absolute; inset: 0; z-index: 0; backdrop-filter: blur(0px); -webkit-backdrop-filter: blur(0px); filter: none; isolation: isolate; border-radius: inherit; overflow: hidden; pointer-events: none; }
        .glass-overlay { position: absolute; inset: 0; z-index: 1; background: var(--lg-bg-color, linear-gradient(180deg, rgba(255, 255, 255, 0.16) 0%, rgba(255, 255, 255, 0.05) 50%, rgba(10, 15, 30, 0.22) 100%)); border-radius: inherit; pointer-events: none; }
        .glass-specular { position: absolute; inset: 0; z-index: 2; border-radius: inherit; overflow: hidden; box-shadow: inset 0 1.2px 1px 0 var(--lg-highlight, rgba(255, 255, 255, 0.65)), inset 0 0 10px rgba(255, 255, 255, 0.12), inset 0 -1px 1px rgba(0, 0, 0, 0.25); pointer-events: none; }
        #custom-player-hub > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular),
        #lq-top-lecture-hud > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular),
        #c-settings-menu > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular),
        #lq-top-playlist-dropdown > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular),
        #lq-next-lecture-prompt > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular),
        #custom-timeline-tray > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular),
        #custom-shortcuts-modal > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular),
        #lq-buffering > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular),
        #lq-osd-hud > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular),
        #scrubber-hover-preview > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular),
        .lq-toast > *:not(.glass-filter):not(.glass-overlay):not(.glass-specular) { position: relative; z-index: 3; }
        #controls, .controls-overlay, .settings-float, .loader-overlay, .error-box,
        #sheet-overlay, #menu-panel, #timeline-panel, #chat-panel { display: none !important; opacity: 0 !important; pointer-events: none !important; }
        #video-wrapper video { width: 100vw !important; height: 100vh !important; object-fit: contain !important; background: transparent; }
        #lq-top-lecture-hud { position: fixed; top: 18px; left: 50%; transform: translateX(-50%) translateY(-70px); z-index: 999999; max-width: min(800px, calc(100vw - 32px)); background: transparent !important; border-radius: 9999px; padding: 7px 16px; display: flex; align-items: center; gap: 10px; box-shadow: 0 6px 6px rgba(0,0,0,0.2), 0 0 20px rgba(0,0,0,0.1); user-select: none; opacity: 0; pointer-events: none; transition: transform 0.4s cubic-bezier(0.16,1,0.3,1), opacity 0.35s ease, box-shadow 0.3s ease; }
        #lq-top-lecture-hud.intro-show, #lq-top-lecture-hud.user-active, #lq-top-lecture-hud.active-dropdown { opacity: 1 !important; pointer-events: auto !important; transform: translateX(-50%) translateY(0) !important; box-shadow: 0 8px 14px rgba(0,0,0,0.3), 0 0 25px rgba(0,0,0,0.15); }
        .top-hud-chip { font-family: 'JetBrains Mono', monospace; font-size: 0.72rem; font-weight: 700; color: var(--lq-accent, #38bdf8); background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.55); border-radius: 999px; padding: 3px 10px; flex-shrink: 0; white-space: nowrap; box-shadow: inset 0 1px 1px rgba(255,255,255,0.35); }
        .top-hud-title { font-size: 0.84rem; font-weight: 700; color: #ffffff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 440px; min-width: 0; flex: 1 1 auto; letter-spacing: 0.01em; text-shadow: 0 1px 3px rgba(0,0,0,0.85); }
        .top-hud-btn { background: transparent; border: 1px solid transparent; color: #ffffff; font-size: 0.95rem; cursor: pointer; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 50%; transition: all 0.2s; outline: none; flex-shrink: 0; filter: drop-shadow(0 1px 2px rgba(0,0,0,0.7)); }
        .top-hud-btn:hover { color: var(--lq-accent, #38bdf8); background: linear-gradient(135deg, rgba(255,255,255,0.22) 0%, rgba(255,255,255,0.05) 100%); border-color: rgba(255,255,255,0.35); box-shadow: inset 0 1px 1px rgba(255,255,255,0.6), 0 0 12px var(--lq-accent-glow); transform: scale(1.15); }
        .top-hud-btn:disabled { opacity: 0.3; cursor: not-allowed; transform: none !important; background: transparent !important; border-color: transparent !important; }
        .top-hud-badge { font-size: 0.82rem; font-weight: 700; color: #ffffff; padding: 5px 12px; border-radius: 9999px; background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.28); border-top: 1.2px solid rgba(255,255,255,0.6); cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all 0.2s; white-space: nowrap; max-width: 240px; overflow: hidden; text-overflow: ellipsis; box-shadow: inset 0 1px 1px rgba(255,255,255,0.4); }
        .top-hud-badge:hover, .top-hud-badge.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 14px var(--lq-accent-glow), inset 0 1px 1px rgba(255,255,255,0.5); }
        #lq-top-playlist-dropdown { position: absolute !important; top: calc(100% + 12px); left: 50%; transform: translateX(-50%) translateY(-8px) scale(0.96); width: 410px; max-height: 520px; background: transparent !important; border-radius: 22px; padding: 14px 16px; display: none !important; flex-direction: column; gap: 10px; z-index: 9999999 !important; box-shadow: 0 10px 30px rgba(0,0,0,0.35), 0 0 20px rgba(0,0,0,0.15); opacity: 0; pointer-events: none; transition: transform 0.22s cubic-bezier(0.16,1,0.3,1), opacity 0.2s ease; }
        #lq-top-playlist-dropdown.show { display: flex !important; opacity: 1 !important; pointer-events: auto !important; transform: translateX(-50%) translateY(0) scale(1) !important; }
        .top-playlist-header { display: flex; justify-content: space-between; align-items: center; font-size: 0.88rem; font-weight: 800; color: #ffffff; border-bottom: 1px solid rgba(255,255,255,0.15); padding-bottom: 8px; text-shadow: 0 1px 3px rgba(0,0,0,0.85); }
        .playlist-stats-hub { background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.2); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 16px; padding: 10px 12px; display: flex; flex-direction: column; gap: 8px; box-shadow: inset 0 1px 1.5px rgba(255,255,255,0.35), 0 4px 15px rgba(0,0,0,0.2); }
        .stats-row-cards { display: grid; grid-template-columns: repeat(3, 1fr); gap: 6px; }
        .stat-card { background: linear-gradient(135deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.2); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 12px; padding: 6px 6px; display: flex; flex-direction: column; gap: 2px; text-align: center; box-shadow: inset 0 1px 1px rgba(255,255,255,0.3); transition: all 0.2s ease; }
        .stat-card:hover { background: linear-gradient(135deg, rgba(255,255,255,0.18) 0%, rgba(255,255,255,0.06) 100%); border-color: rgba(255,255,255,0.35); transform: translateY(-1px); }
        .stat-label { font-size: 0.65rem; text-transform: uppercase; font-weight: 800; letter-spacing: 0.4px; color: #94a3b8; display: flex; align-items: center; justify-content: center; gap: 4px; }
        .stat-val { font-size: 0.88rem; font-weight: 800; color: #ffffff; font-family: 'JetBrains Mono', monospace, sans-serif; letter-spacing: -0.02em; }
        .playlist-progress-wrapper { display: flex; flex-direction: column; gap: 4px; padding: 2px 2px 0 2px; }
        .playlist-mini-progress { width: 100%; height: 6px; background: rgba(255,255,255,0.1); border-radius: 999px; overflow: hidden; position: relative; }
        .playlist-mini-fill { height: 100%; background: linear-gradient(90deg, #10b981, var(--lq-accent, #38bdf8)); border-radius: 999px; transition: width 0.3s cubic-bezier(0.16,1,0.3,1); box-shadow: 0 0 10px rgba(56,189,248,0.4); }
        .playlist-mini-meta { display: flex; justify-content: space-between; align-items: center; font-size: 0.68rem; font-weight: 700; color: #94a3b8; font-family: 'JetBrains Mono', monospace, sans-serif; }
        .top-playlist-search-input { width: 100%; box-sizing: border-box; background: linear-gradient(135deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 12px; padding: 7px 12px; font-size: 0.8rem; color: #ffffff; outline: none; box-shadow: inset 0 1px 1px rgba(255,255,255,0.3); transition: all 0.2s; }
        .top-playlist-search-input:focus { background: linear-gradient(135deg, rgba(255,255,255,0.18) 0%, rgba(255,255,255,0.06) 100%); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 12px var(--lq-accent-glow), inset 0 1px 1px rgba(255,255,255,0.4); }
        .top-playlist-search-input::placeholder { color: #94a3b8; }
        .top-playlist-list { display: flex; flex-direction: column; gap: 6px; max-height: 240px; overflow-y: auto; padding-right: 4px; }
        .top-playlist-list::-webkit-scrollbar { width: 5px; }
        .top-playlist-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.3); border-radius: 4px; }
        .playlist-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 10px; border-radius: 12px; background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.18); border-top: 1.2px solid rgba(255,255,255,0.45); color: #ffffff; cursor: pointer; box-shadow: inset 0 1px 1px rgba(255,255,255,0.25); transition: all 0.2s cubic-bezier(0.16,1,0.3,1); user-select: none; text-shadow: 0 1px 2px rgba(0,0,0,0.8); }
        .playlist-item:hover { background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0.06) 100%); border-color: rgba(255,255,255,0.35); box-shadow: inset 0 1px 1px rgba(255,255,255,0.45), 0 4px 14px rgba(0,0,0,0.2); color: #ffffff; transform: translateX(3px); }
        .playlist-item.active { background: var(--lq-accent, #38bdf8) !important; color: var(--lq-accent-text, #0f172a) !important; font-weight: 800; border-color: var(--lq-accent, #38bdf8) !important; box-shadow: 0 4px 14px var(--lq-accent-glow), inset 0 1px 1px rgba(255,255,255,0.5); text-shadow: none !important; }
        .playlist-item.active * { color: var(--lq-accent-text, #0f172a) !important; text-shadow: none !important; }
        .playlist-item.completed { border-color: rgba(16, 185, 129, 0.35); }
        .playlist-item.completed:hover { border-color: rgba(16, 185, 129, 0.6); }

        /* Playlist View-Switcher & Subject/Chapter Selector Panel */
        #lq-playlist-lectures-view { display: flex; flex-direction: column; gap: 10px; }
        #lq-playlist-selector-panel { display: none; flex-direction: column; gap: 10px; max-height: 480px; }
        #lq-playlist-selector-panel.show { display: flex; }
        .lq-panel-label { font-size: 0.74rem; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; display: flex; align-items: center; justify-content: space-between; }
        .lq-glass-select { width: 100%; box-sizing: border-box; background: linear-gradient(135deg, rgba(255,255,255,0.14) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.55); border-radius: 12px; padding: 8px 12px; font-size: 0.82rem; font-weight: 700; color: #ffffff; outline: none; cursor: pointer; box-shadow: inset 0 1px 1px rgba(255,255,255,0.3); transition: all 0.2s; }
        .lq-glass-select:focus { border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 12px var(--lq-accent-glow); }
        .lq-glass-select option { background: #0f172a; color: #ffffff; padding: 6px; }
        .lq-chapters-list { display: flex; flex-direction: column; gap: 6px; max-height: 250px; overflow-y: auto; padding-right: 4px; }
        .lq-chapters-list::-webkit-scrollbar { width: 5px; }
        .lq-chapters-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.3); border-radius: 4px; }
        .lq-chapter-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 10px; border-radius: 12px; background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.18); border-top: 1.2px solid rgba(255,255,255,0.45); color: #ffffff; cursor: pointer; box-shadow: inset 0 1px 1px rgba(255,255,255,0.25); transition: all 0.2s cubic-bezier(0.16,1,0.3,1); user-select: none; text-shadow: 0 1px 2px rgba(0,0,0,0.8); }
        .lq-chapter-item:hover { background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0.06) 100%); border-color: rgba(255,255,255,0.35); box-shadow: inset 0 1px 1px rgba(255,255,255,0.45), 0 4px 14px rgba(0,0,0,0.2); transform: translateX(3px); }
        .lq-chapter-item.active { background: var(--lq-accent, #38bdf8) !important; color: var(--lq-accent-text, #0f172a) !important; font-weight: 800; border-color: var(--lq-accent, #38bdf8) !important; box-shadow: 0 4px 14px var(--lq-accent-glow), inset 0 1px 1px rgba(255,255,255,0.5); text-shadow: none !important; }
        .lq-chapter-item.active * { color: var(--lq-accent-text, #0f172a) !important; text-shadow: none !important; }
        .lq-panel-btn { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.55); border-radius: 10px; color: #ffffff; font-size: 0.72rem; font-weight: 700; padding: 4px 10px; cursor: pointer; transition: all 0.2s; outline: none; box-shadow: inset 0 1px 1px rgba(255,255,255,0.35); display: inline-flex; align-items: center; gap: 5px; }
        .lq-panel-btn:hover { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 10px var(--lq-accent-glow); }

        /* Selector Tabs (Chapters vs Study Material) */
        .lq-selector-tabs { display: flex; gap: 6px; background: rgba(0,0,0,0.28); padding: 3px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.14); margin-top: 4px; }
        .lq-tab-btn { flex: 1; background: transparent; border: none; color: #94a3b8; font-size: 0.74rem; font-weight: 700; padding: 6px 8px; border-radius: 9px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; gap: 5px; outline: none; }
        .lq-tab-btn:hover { color: #ffffff; background: rgba(255,255,255,0.08); }
        .lq-tab-btn.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); font-weight: 800; box-shadow: 0 2px 8px var(--lq-accent-glow); }

        /* Content Filter Pills (All, Lectures, Notes, DPP PDFs, DPP Videos) */
        .lq-content-filter-bar { display: flex; gap: 5px; overflow-x: auto; padding-bottom: 2px; scrollbar-width: none; }
        .lq-content-filter-bar::-webkit-scrollbar { display: none; }
        .lq-filter-pill { background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.18); border-top: 1.2px solid rgba(255,255,255,0.4); border-radius: 999px; color: #ffffff; font-size: 0.68rem; font-weight: 700; padding: 3px 9px; cursor: pointer; white-space: nowrap; transition: all 0.15s; outline: none; display: flex; align-items: center; gap: 3px; }
        .lq-filter-pill:hover { background: rgba(255,255,255,0.18); border-color: rgba(255,255,255,0.35); }
        .lq-filter-pill.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 10px var(--lq-accent-glow); }

        /* PDF & Action Buttons inside List Items */
        .lq-item-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
        .lq-item-action-btn { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 8px; color: #ffffff; width: 26px; height: 26px; display: inline-flex; align-items: center; justify-content: center; font-size: 0.72rem; cursor: pointer; transition: all 0.2s; text-decoration: none; outline: none; }
        .lq-item-action-btn:hover { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); transform: scale(1.1); box-shadow: 0 0 8px var(--lq-accent-glow); }
        .lq-pdf-badge { background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.4); border-radius: 6px; padding: 1px 5px; font-size: 0.65rem; font-weight: 800; font-family: monospace; }
        .lq-dpp-badge { background: rgba(59, 130, 246, 0.2); color: #60a5fa; border: 1px solid rgba(59, 130, 246, 0.4); border-radius: 6px; padding: 1px 5px; font-size: 0.65rem; font-weight: 800; font-family: monospace; }
        .lq-lec-badge { background: rgba(16, 185, 129, 0.2); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.4); border-radius: 6px; padding: 1px 5px; font-size: 0.65rem; font-weight: 800; font-family: monospace; }

        #lq-next-lecture-prompt { position: fixed; bottom: 84px; right: 28px; z-index: 999999; background: transparent !important; border-radius: 20px; padding: 14px 18px; display: flex; flex-direction: column; gap: 10px; min-width: 320px; max-width: 380px; box-shadow: 0 10px 30px rgba(0,0,0,0.4), 0 0 25px rgba(0,0,0,0.2); opacity: 0; pointer-events: none; transform: translateY(30px) scale(0.95); transition: all 0.35s cubic-bezier(0.16,1,0.3,1); }
        #lq-next-lecture-prompt.show { opacity: 1; pointer-events: auto; transform: translateY(0) scale(1); }
        .next-prompt-content { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
        .next-prompt-text { display: flex; flex-direction: column; gap: 3px; overflow: hidden; }
        .next-prompt-subtitle { font-size: 0.68rem; font-weight: 800; color: var(--lq-accent, #38bdf8); letter-spacing: 0.6px; text-transform: uppercase; }
        .next-prompt-title { font-size: 0.88rem; font-weight: 700; color: #ffffff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 170px; text-shadow: 0 1px 2px rgba(0,0,0,0.8); }
        .next-prompt-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
        .next-prompt-btn { border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 12px; cursor: pointer; font-weight: 700; transition: all 0.2s; outline: none; }
        .next-prompt-btn.play-next { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); padding: 6px 14px; font-size: 0.8rem; box-shadow: 0 0 12px var(--lq-accent-glow), inset 0 1px 1px rgba(255,255,255,0.6); }
        .next-prompt-btn.play-next:hover { transform: scale(1.05); filter: brightness(1.1); }
        .next-prompt-btn.dismiss { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); color: #ffffff; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 50%; font-size: 0.85rem; box-shadow: inset 0 1px 1px rgba(255,255,255,0.3); }
        .next-prompt-btn.dismiss:hover { background: rgba(239, 68, 68, 0.4); border-color: rgba(239, 68, 68, 0.6); transform: scale(1.1); }
        .next-prompt-progress-bar { width: 100%; height: 4px; background: rgba(255,255,255,0.1); border-radius: 999px; overflow: hidden; }
        #next-prompt-fill { height: 100%; background: var(--lq-accent, #38bdf8); border-radius: 999px; transition: width 0.1s linear; box-shadow: 0 0 8px var(--lq-accent-glow); }

        #custom-player-hub { position: fixed; bottom: 18px; left: 50%; transform: translateX(-50%) translateY(70px); z-index: 999999; width: min(1280px, calc(100vw - 32px)); background: transparent !important; border-radius: 9999px; padding: 8px 18px; display: flex; align-items: center; gap: 12px; box-shadow: 0 6px 6px rgba(0,0,0,0.2), 0 0 20px rgba(0,0,0,0.1); user-select: none; opacity: 0; pointer-events: none; transition: transform 0.4s cubic-bezier(0.16,1,0.3,1), opacity 0.35s ease, box-shadow 0.3s ease; }
        #custom-player-hub.user-active, #custom-player-hub.keep-active { opacity: 1 !important; pointer-events: auto !important; transform: translateX(-50%) translateY(0) !important; box-shadow: 0 8px 14px rgba(0,0,0,0.3), 0 0 25px rgba(0,0,0,0.15); }
        .hub-btn { background: transparent; border: 1px solid transparent; color: #ffffff; font-size: 1.1rem; cursor: pointer; width: 36px; height: 36px; display: flex; align-items: center; justify-content: center; border-radius: 50%; transition: all 0.2s cubic-bezier(0.16,1,0.3,1); outline: none; flex-shrink: 0; filter: drop-shadow(0 1px 2px rgba(0,0,0,0.7)); }
        .hub-btn:hover { color: var(--lq-accent, #38bdf8); background: linear-gradient(135deg, rgba(255,255,255,0.22) 0%, rgba(255,255,255,0.05) 100%); border-color: rgba(255,255,255,0.35); box-shadow: inset 0 1px 1px rgba(255,255,255,0.6), 0 0 12px var(--lq-accent-glow); transform: scale(1.15); }
        .hub-speed-badge { border-radius: 999px !important; width: auto !important; height: 30px !important; }
        .hub-progress-box { flex: 1; display: flex; align-items: center; gap: 12px; min-width: 0; position: relative; }
        .hub-time { font-family: 'JetBrains Mono', monospace; font-size: 0.78rem; font-weight: 700; color: #ffffff; min-width: 44px; text-shadow: 0 1px 2px rgba(0,0,0,0.8); flex-shrink: 0; }
        .scrubber-track-container { position: relative; flex: 1; height: 30px; display: flex; align-items: center; cursor: pointer; }
        .hub-track-bg { position: absolute; left: 0; right: 0; height: 5px; background: rgba(255, 255, 255, 0.15); border-radius: 9999px; overflow: hidden; pointer-events: none; }
        .hub-buffer-bar { position: absolute; left: 0; top: 0; height: 100%; width: 0%; background: rgba(255, 255, 255, 0.35); border-radius: 9999px; transition: width 0.15s ease-out; }
        .hub-fill-bar { position: absolute; left: 0; top: 0; height: 100%; width: 0%; background: var(--lq-fill-gradient, linear-gradient(90deg, var(--lq-accent-b, #0284c7), var(--lq-accent, #38bdf8))); border-radius: 9999px; box-shadow: 0 0 10px var(--lq-accent-glow); }
        .hub-slider { position: absolute; left: 0; width: 100%; height: 30px; -webkit-appearance: none; appearance: none; background: transparent; outline: none; margin: 0; cursor: pointer; z-index: 10; }
        .hub-slider::-webkit-slider-thumb { -webkit-appearance: none; appearance: none; width: 15px; height: 15px; border-radius: 50%; background: #ffffff; border: 2px solid var(--lq-accent, #38bdf8); box-shadow: 0 0 10px var(--lq-accent-glow), 0 1px 3px rgba(0,0,0,0.5); cursor: pointer; transition: transform 0.15s ease; }
        .hub-slider:hover::-webkit-slider-thumb { transform: scale(1.3); }

        #timeline-scrub-separators { position: absolute; inset: 0; pointer-events: none; z-index: 8; }
        .slide-segment-divider { position: absolute; top: 50%; transform: translate(-50%, -50%); width: 5px; height: 16px; background: rgba(255, 255, 255, 0.95); border: 1px solid rgba(0, 0, 0, 0.4); border-radius: 4px; pointer-events: auto; cursor: pointer; transition: all 0.2s cubic-bezier(0.16,1,0.3,1); box-shadow: 0 0 6px rgba(0,0,0,0.6), 0 0 4px rgba(255,255,255,0.4); }
        .slide-segment-divider:hover { width: 8px; height: 22px; background: #fbbf24; border-color: #ffffff; transform: translate(-50%, -50%) scale(1.15); box-shadow: 0 0 12px rgba(251,191,36,0.9); z-index: 12; }
        .slide-segment-divider.active-slide { background: var(--lq-accent, #38bdf8); box-shadow: 0 0 10px var(--lq-accent-glow); height: 18px; }

        #scrubber-hover-preview { position: absolute; bottom: 42px; transform: translateX(-50%); width: 140px; background: transparent !important; border-radius: 14px; padding: 6px; display: none; flex-direction: column; gap: 4px; box-shadow: 0 8px 20px rgba(0,0,0,0.5); pointer-events: none; z-index: 999999; }
        #scrubber-hover-preview img { width: 100%; height: 78px; object-fit: cover; border-radius: 10px; background: #000; border: 1px solid rgba(255,255,255,0.2); }
        .preview-meta { display: flex; justify-content: space-between; font-size: 0.68rem; font-weight: 700; color: #ffffff; text-shadow: 0 1px 2px rgba(0,0,0,0.8); font-family: 'JetBrains Mono', monospace; padding: 0 2px; }

        #custom-timeline-tray { position: fixed; bottom: 84px; left: 50%; transform: translateX(-50%) translateY(30px) scale(0.97); width: min(1000px, calc(100vw - 32px)); max-height: 480px; background: transparent !important; border-radius: 28px; padding: 18px 22px; display: none; flex-direction: column; gap: 14px; z-index: 999999; box-shadow: 0 12px 36px rgba(0,0,0,0.4), 0 0 25px rgba(0,0,0,0.15); opacity: 0; pointer-events: none; transition: transform 0.3s cubic-bezier(0.16,1,0.3,1), opacity 0.25s ease; }
        #custom-timeline-tray.active { display: flex; opacity: 1; pointer-events: auto; transform: translateX(-50%) translateY(0) scale(1); }
        .tray-grid { display: flex; gap: 14px; overflow-x: auto; padding-bottom: 8px; align-items: center; }
        .tray-grid::-webkit-scrollbar { height: 6px; }
        .tray-grid::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.3); border-radius: 4px; }
        .tray-card { flex-shrink: 0; width: 155px; border-radius: 16px; background: linear-gradient(135deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.2); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 16px; cursor: pointer; overflow: hidden; transition: all 0.25s cubic-bezier(0.16,1,0.3,1); box-shadow: inset 0 1px 1px rgba(255,255,255,0.3), 0 4px 12px rgba(0,0,0,0.2); }
        .tray-card:hover { transform: translateY(-4px) scale(1.03); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 8px 20px var(--lq-accent-glow); }
        .tray-card img { width: 100%; height: 95px; object-fit: cover; background: #000; border-bottom: 1px solid rgba(255,255,255,0.15); }
        .tray-card-footer { padding: 6px 10px; display: flex; justify-content: space-between; font-size: 0.75rem; font-weight: 700; color: #ffffff; text-shadow: 0 1px 2px rgba(0,0,0,0.8); }
        .slide-badge-num { color: var(--lq-accent, #38bdf8); }
        .slide-badge-time { font-family: 'JetBrains Mono', monospace; opacity: 0.85; }
        .slide-transition-separator { color: rgba(255,255,255,0.4); font-size: 0.8rem; flex-shrink: 0; }

        #c-settings-menu { position: absolute; bottom: 50px; right: 0; width: 320px; background: transparent !important; border-radius: 22px; padding: 14px 16px; display: none; flex-direction: column; gap: 10px; z-index: 9999999; box-shadow: 0 10px 30px rgba(0,0,0,0.35), 0 0 20px rgba(0,0,0,0.15); opacity: 0; pointer-events: none; transform: translateY(10px) scale(0.97); transition: transform 0.25s cubic-bezier(0.16,1,0.3,1), opacity 0.2s ease; }
        #c-settings-menu.show { display: flex; opacity: 1; pointer-events: auto; transform: translateY(0) scale(1); }
        .settings-drawer-pill { width: 38px; height: 4px; background: rgba(255,255,255,0.35); border-radius: 999px; margin: 0 auto 4px auto; display: none; }
        .settings-header { font-size: 0.85rem; font-weight: 800; color: #ffffff; border-bottom: 1px solid rgba(255,255,255,0.15); padding-bottom: 6px; display: flex; align-items: center; gap: 6px; text-shadow: 0 1px 2px rgba(0,0,0,0.8); }
        .settings-row { display: flex; justify-content: space-between; align-items: center; font-size: 0.78rem; font-weight: 600; color: #ffffff; text-shadow: 0 1px 2px rgba(0,0,0,0.8); }
        .settings-theme-section { display: flex; flex-direction: column; gap: 6px; padding-bottom: 6px; border-bottom: 1px solid rgba(255,255,255,0.08); }
        .lq-swatches-grid { display: grid; grid-template-columns: repeat(8, 1fr); gap: 6px; max-height: 110px; overflow-y: auto; padding: 2px 2px 4px 2px; }
        .lq-swatches-grid::-webkit-scrollbar { width: 4px; }
        .lq-swatches-grid::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.3); border-radius: 4px; }
        .lq-swatch { width: 22px; height: 22px; border-radius: 50%; cursor: pointer; border: 1.5px solid rgba(255,255,255,0.45); box-shadow: 0 1px 4px rgba(0,0,0,0.4); transition: all 0.2s cubic-bezier(0.16,1,0.3,1); position: relative; box-sizing: border-box; }
        .lq-swatch:hover { transform: scale(1.22); border-color: #ffffff; box-shadow: 0 0 8px rgba(255,255,255,0.6); z-index: 2; }
        .lq-swatch.active { border-color: #ffffff; transform: scale(1.25); box-shadow: 0 0 10px #ffffff, inset 0 0 3px rgba(255,255,255,0.8); z-index: 3; }

        /* Mobile Quick Chips */
        .mobile-chips-row { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 4px; scrollbar-width: none; }
        .mobile-chips-row::-webkit-scrollbar { display: none; }
        .mobile-chip { background: linear-gradient(135deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.22); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 10px; color: #ffffff; font-size: 0.74rem; font-family: 'JetBrains Mono', monospace; font-weight: 700; padding: 5px 10px; cursor: pointer; white-space: nowrap; transition: all 0.18s; outline: none; flex-shrink: 0; min-height: 30px; display: inline-flex; align-items: center; justify-content: center; box-shadow: inset 0 1px 1px rgba(255,255,255,0.25); }
        .mobile-chip:hover { background: rgba(255,255,255,0.2); border-color: rgba(255,255,255,0.4); transform: translateY(-1px); }
        .mobile-chip.active { background: var(--lq-accent, #38bdf8) !important; color: var(--lq-accent-text, #0f172a) !important; font-weight: 800; border-color: var(--lq-accent, #38bdf8) !important; box-shadow: 0 2px 10px var(--lq-accent-glow), inset 0 1px 1px rgba(255,255,255,0.6); }

        .settings-select { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.05) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.55); border-radius: 10px; color: #ffffff; font-size: 0.75rem; font-weight: 700; padding: 3px 8px; outline: none; cursor: pointer; box-shadow: inset 0 1px 1px rgba(255,255,255,0.35); }
        .settings-select option { background: #0f172a; color: #ffffff; }
        .settings-vol-slider { -webkit-appearance: none; appearance: none; width: 100px; height: 5px; background: rgba(255,255,255,0.2); border-radius: 999px; outline: none; cursor: pointer; }
        .settings-vol-slider::-webkit-slider-thumb { -webkit-appearance: none; appearance: none; width: 13px; height: 13px; border-radius: 50%; background: #ffffff; border: 2px solid var(--lq-accent, #38bdf8); box-shadow: 0 0 8px var(--lq-accent-glow); cursor: pointer; }
        .settings-toggle-btn { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.55); border-radius: 10px; color: #ffffff; font-size: 0.72rem; font-weight: 700; padding: 4px 12px; cursor: pointer; transition: all 0.2s; outline: none; box-shadow: inset 0 1px 1px rgba(255,255,255,0.35); min-height: 28px; }
        .settings-toggle-btn:hover, .settings-toggle-btn.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 10px var(--lq-accent-glow); }
        .settings-speed-section { display: flex; flex-direction: column; gap: 6px; padding: 6px 0; border-top: 1px solid rgba(255,255,255,0.08); border-bottom: 1px solid rgba(255,255,255,0.08); }
        .settings-custom-speed-row { display: flex; align-items: center; gap: 6px; }
        .settings-speed-input { flex: 1; background: linear-gradient(135deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 8px; padding: 3px 8px; font-size: 0.75rem; color: #ffffff; outline: none; box-shadow: inset 0 1px 1px rgba(255,255,255,0.3); font-family: 'JetBrains Mono', monospace; }
        .settings-speed-btn { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 8px; color: #ffffff; font-size: 0.72rem; font-weight: 700; padding: 3px 8px; cursor: pointer; transition: all 0.2s; outline: none; }
        .settings-speed-btn:hover, .settings-speed-btn.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); }
        .settings-pinned-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
        .pinned-label { font-size: 0.7rem; color: #94a3b8; font-weight: 700; }
        .pinned-speeds-container { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
        .pinned-speed-chip { display: inline-flex; align-items: center; gap: 3px; background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.05) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 6px; padding: 1px 6px; font-size: 0.7rem; font-family: 'JetBrains Mono', monospace; font-weight: 700; color: #ffffff; cursor: pointer; box-shadow: inset 0 1px 1px rgba(255,255,255,0.3); transition: all 0.15s; }
        .pinned-speed-chip:hover, .pinned-speed-chip.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 8px var(--lq-accent-glow); }
        .pinned-speed-remove { font-size: 0.75rem; margin-left: 2px; opacity: 0.7; }
        .pinned-speed-remove:hover { opacity: 1; color: #ef4444; }

        /* 2X Hold HUD */
        #lq-2x-hold-hud {
            position: fixed;
            top: 20px;
            left: 50%;
            transform: translateX(-50%) translateY(-25px) scale(0.9);
            z-index: 99999999;
            background: rgba(15, 23, 42, 0.88);
            backdrop-filter: blur(14px);
            -webkit-backdrop-filter: blur(14px);
            border: 1px solid var(--lq-accent, #38bdf8);
            box-shadow: 0 8px 30px rgba(0,0,0,0.5), 0 0 20px var(--lq-accent-glow);
            border-radius: 9999px;
            padding: 6px 16px;
            display: flex;
            align-items: center;
            gap: 8px;
            color: #ffffff;
            font-family: 'JetBrains Mono', monospace;
            font-weight: 800;
            font-size: 0.85rem;
            letter-spacing: 0.5px;
            opacity: 0;
            pointer-events: none;
            transition: transform 0.22s cubic-bezier(0.16,1,0.3,1), opacity 0.2s ease;
        }
        #lq-2x-hold-hud.active {
            opacity: 1;
            transform: translateX(-50%) translateY(0) scale(1);
        }

        #lq-center-play-pulse { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) scale(0.5); width: 80px; height: 80px; border-radius: 50%; background: linear-gradient(135deg, rgba(255,255,255,0.25) 0%, rgba(255,255,255,0.06) 100%); border: 1px solid rgba(255,255,255,0.4); border-top: 1.5px solid rgba(255,255,255,0.7); box-shadow: 0 10px 30px rgba(0,0,0,0.3), inset 0 1px 2px rgba(255,255,255,0.5); display: flex; align-items: center; justify-content: center; font-size: 2rem; color: #ffffff; opacity: 0; pointer-events: none; z-index: 9999999; }
        #lq-center-play-pulse.pulse-anim { animation: lq-pulse-grow 0.65s cubic-bezier(0.16,1,0.3,1) forwards; }
        @keyframes lq-pulse-grow {
            0% { opacity: 0; transform: translate(-50%, -50%) scale(0.6); }
            50% { opacity: 1; transform: translate(-50%, -50%) scale(1.1); }
            100% { opacity: 0; transform: translate(-50%, -50%) scale(1.35); }
        }

        #lq-osd-hud { position: fixed; top: 75px; left: 50%; transform: translateX(-50%) translateY(-15px) scale(0.95); z-index: 99999999; background: transparent !important; border-radius: 9999px; padding: 7px 18px; display: flex; align-items: center; gap: 8px; font-size: 0.88rem; font-weight: 800; color: #ffffff; text-shadow: 0 1px 2px rgba(0,0,0,0.8); box-shadow: 0 8px 24px rgba(0,0,0,0.35); opacity: 0; pointer-events: none; transition: all 0.25s cubic-bezier(0.16,1,0.3,1); }
        #lq-osd-hud.active { opacity: 1; transform: translateX(-50%) translateY(0) scale(1); }

        #lq-buffering { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) scale(0.92); display: flex; align-items: center; gap: 10px; padding: 10px 20px; border-radius: 9999px; font-size: 0.85rem; font-weight: 700; color: #ffffff; text-shadow: 0 1px 2px rgba(0,0,0,0.8); box-shadow: 0 10px 30px rgba(0,0,0,0.4); opacity: 0; pointer-events: none; z-index: 99999998; transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.16,1,0.3,1); }
        #lq-buffering.lq-show { opacity: 1; pointer-events: auto; transform: translate(-50%, -50%) scale(1); }
        .lq-buffering-ring { width: 22px; height: 22px; border-radius: 50%; border: 2.5px solid rgba(255, 255, 255, 0.2); border-top-color: var(--lq-accent, #38bdf8); border-right-color: var(--lq-accent, #38bdf8); animation: lq-spin 0.75s linear infinite; }
        @keyframes lq-spin { to { transform: rotate(360deg); } }

        #lq-toast-container { position: fixed; top: 24px; left: 24px; display: flex; flex-direction: column; gap: 10px; z-index: 999999999; pointer-events: none; }
        .lq-toast { position: relative; background: transparent !important; border-radius: 16px; padding: 9px 16px; color: #ffffff; font-size: 0.85rem; font-weight: 600; display: flex; align-items: center; gap: 10px; text-shadow: 0 1px 3px rgba(0,0,0,0.85); box-shadow: 0 6px 6px rgba(0,0,0,0.2), 0 0 20px rgba(0,0,0,0.1); opacity: 0; transform: translateX(-30px) scale(0.95); transition: all 0.3s cubic-bezier(0.16,1,0.3,1); pointer-events: auto; max-width: 320px; }
        .lq-toast.show { opacity: 1; transform: translateX(0) scale(1); }
        .lq-toast.toast-error i { color: #ef4444; }
        .lq-toast.toast-success i { color: #10b981; }
        .lq-toast.toast-info i { color: var(--lq-accent, #38bdf8); }

        #custom-shortcuts-modal { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) scale(0.95); width: min(520px, calc(100vw - 32px)); background: transparent !important; border-radius: 26px; padding: 18px 22px; display: none; flex-direction: column; gap: 14px; z-index: 99999999; box-shadow: 0 16px 40px rgba(0,0,0,0.4), 0 0 30px rgba(0,0,0,0.2); opacity: 0; pointer-events: none; transition: transform 0.25s cubic-bezier(0.16,1,0.3,1), opacity 0.2s ease; }
        #custom-shortcuts-modal.active { display: flex; opacity: 1; pointer-events: auto; transform: translate(-50%, -50%) scale(1); }
        .shortcuts-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; padding: 4px; }
        .shortcut-item { display: flex; align-items: center; justify-content: space-between; background: linear-gradient(135deg, rgba(255,255,255,0.09) 0%, rgba(255,255,255,0.02) 100%); padding: 7px 12px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.16); border-top: 1.2px solid rgba(255,255,255,0.4); box-shadow: inset 0 1px 1px rgba(255,255,255,0.25); }
        .shortcut-key { background: linear-gradient(135deg, rgba(255,255,255,0.18) 0%, rgba(255,255,255,0.06) 100%); border: 1px solid rgba(255,255,255,0.35); border-top: 1.2px solid rgba(255,255,255,0.65); border-radius: 8px; padding: 2px 8px; font-size: 0.75rem; font-family: monospace; font-weight: 700; color: var(--lq-accent, #38bdf8); box-shadow: inset 0 1px 1px rgba(255,255,255,0.4), 0 2px 5px rgba(0,0,0,0.2); }
        .shortcut-desc { font-size: 0.8rem; color: #ffffff; text-shadow: 0 1px 2px rgba(0,0,0,0.8); }

        /* Double-tap Seek Ripple Zone */
        .lq-seek-ripple-zone {
            position: fixed;
            top: 0;
            bottom: 0;
            width: 35vw;
            pointer-events: none;
            z-index: 9999998;
            display: flex;
            align-items: center;
            justify-content: center;
            opacity: 0;
            transition: opacity 0.2s ease, transform 0.25s cubic-bezier(0.16,1,0.3,1);
        }
        .lq-seek-ripple-zone.ripple-left {
            left: 0;
            border-top-right-radius: 40% 100%;
            border-bottom-right-radius: 40% 100%;
            background: radial-gradient(circle at left, rgba(56, 189, 248, 0.28) 0%, rgba(56, 189, 248, 0.05) 65%, transparent 100%);
        }
        .lq-seek-ripple-zone.ripple-right {
            right: 0;
            border-top-left-radius: 40% 100%;
            border-bottom-left-radius: 40% 100%;
            background: radial-gradient(circle at right, rgba(56, 189, 248, 0.28) 0%, rgba(56, 189, 248, 0.05) 65%, transparent 100%);
        }
        .lq-seek-ripple-zone.active { opacity: 1; }
        .lq-seek-ripple-zone .ripple-content {
            display: flex;
            flex-direction: column;
            align-items: center;
            gap: 4px;
            color: #ffffff;
            font-size: 1.3rem;
            font-weight: 800;
            text-shadow: 0 2px 10px rgba(0,0,0,0.85);
            transform: scale(0.8);
            transition: transform 0.22s cubic-bezier(0.16,1,0.3,1);
        }
        .lq-seek-ripple-zone.active .ripple-content { transform: scale(1.1); }

        @media (max-width: 900px), (max-height: 520px) {
            #custom-player-hub {
                bottom: 16px;
                padding: 10px 20px;
                gap: 12px;
                width: min(1200px, calc(100vw - 20px));
                border-radius: 9999px;
            }
            .hub-btn { width: 38px; height: 38px; font-size: 1.15rem; }
            #c-play-btn, #c-play { width: 42px; height: 42px; font-size: 1.25rem; color: #ffffff; }
            #c-rwd, #c-fwd { display: flex !important; width: 38px !important; height: 38px !important; font-size: 1.05rem; }
            .hub-time { font-size: 0.82rem; min-width: 42px; font-family: 'JetBrains Mono', monospace; font-weight: 700; color: #ffffff; }
            .hub-speed-badge { height: 32px !important; padding: 0 12px !important; font-size: 0.82rem !important; border-radius: 999px !important; }
            .scrubber-track-container { height: 32px; }
            .hub-track-bg { height: 6px; background: rgba(255, 255, 255, 0.2); border-radius: 999px; }
            .hub-fill-bar { height: 6px; }
            .hub-slider::-webkit-slider-thumb { width: 18px; height: 18px; background: #ffffff; border: 2.5px solid var(--lq-accent, #38bdf8); box-shadow: 0 0 10px var(--lq-accent-glow); }
            .shortcuts-grid { grid-template-columns: 1fr; }
            #lq-top-lecture-hud {
                top: 14px;
                left: 50%;
                transform: translateX(-50%) translateY(-70px);
                max-width: min(800px, calc(100vw - 20px));
                padding: 8px 18px;
                gap: 10px;
            }
            #lq-top-lecture-hud.intro-show, #lq-top-lecture-hud.user-active, #lq-top-lecture-hud.active-dropdown {
                transform: translateX(-50%) translateY(0) !important;
            }
            .top-hud-title { max-width: 280px; font-size: 0.85rem; }
            .top-hud-chip { font-size: 0.72rem; padding: 3px 10px; }
            .top-hud-btn { width: 32px; height: 32px; font-size: 0.95rem; }
            .top-hud-badge { font-size: 0.8rem; padding: 4px 12px; }
            #lq-top-playlist-dropdown {
                width: 340px;
                max-height: 60vh;
                overflow-y: auto;
            }
            #c-settings-menu {
                position: absolute !important;
                bottom: 54px !important;
                right: 0 !important;
                width: 300px !important;
                max-height: 75vh !important;
                overflow-y: auto !important;
                border-radius: 22px !important;
                padding: 14px 16px !important;
            }
            #custom-timeline-tray {
                width: min(900px, calc(100vw - 20px));
                bottom: 64px;
                padding: 12px 16px;
                max-height: 320px;
                border-radius: 22px;
            }
            .tray-card { width: 130px; }
            .tray-card img { height: 80px; }
            #lq-next-lecture-prompt {
                min-width: 280px;
                max-width: calc(100vw - 24px);
                bottom: 64px;
                right: 14px;
            }
            #lq-osd-hud { top: 54px; font-size: 0.82rem; padding: 6px 14px; }
        }
    `;
    (document.head || document.documentElement).appendChild(style);

    const GLASS_HTML = '<div class="glass-filter"></div><div class="glass-overlay"></div><div class="glass-specular"></div>';

    function ensureSvgFilter() {
        if (!document.getElementById('lg-dist-svg-container')) {
            const svgContainer = document.createElement('div');
            svgContainer.id = 'lg-dist-svg-container';
            svgContainer.innerHTML = `
                <svg style="position:absolute; width:0; height:0; pointer-events:none; display:none;" aria-hidden="true">
                    <filter id="lg-dist" x="0%" y="0%" width="100%" height="100%">
                        <feTurbulence type="fractalNoise" baseFrequency="0.008 0.008" numOctaves="2" seed="92" result="noise" />
                        <feGaussianBlur in="noise" stdDeviation="2" result="blurred" />
                        <feDisplacementMap in="SourceGraphic" in2="blurred" scale="0" xChannelSelector="R" yChannelSelector="G" />
                    </filter>
                </svg>
            `;
            (document.body || document.documentElement).appendChild(svgContainer);
        }
    }

    /* 3. TOAST & OSD HUD NOTIFICATIONS */
    let toastContainer = null;
    function showGlassToast(message, type = 'info', icon = 'fas fa-info-circle') {
        ensureSvgFilter();
        if (!toastContainer) {
            toastContainer = document.createElement('div');
            toastContainer.id = 'lq-toast-container';
            (document.body || document.documentElement).appendChild(toastContainer);
        }
        const toast = document.createElement('div');
        toast.className = `lq-toast toast-${type}`;
        toast.innerHTML = `${GLASS_HTML}<i class="${icon}"></i><span>${message}</span>`;
        toastContainer.appendChild(toast);
        requestAnimationFrame(() => toast.classList.add('show'));
        setTimeout(() => {
            toast.classList.remove('show');
            setTimeout(() => toast.remove(), 350);
        }, 3500);
    }

    let osdTimeout = null;
    function showOSD(iconHtml, text) {
        ensureSvgFilter();
        let osd = document.getElementById('lq-osd-hud');
        if (!osd) {
            osd = document.createElement('div');
            osd.id = 'lq-osd-hud';
            (document.body || document.documentElement).appendChild(osd);
        }
        osd.innerHTML = `${GLASS_HTML}${iconHtml}<span>${text}</span>`;
        osd.classList.add('active');
        if (osdTimeout) clearTimeout(osdTimeout);
        osdTimeout = setTimeout(() => osd.classList.remove('active'), 850);
    }

    /* 3.5. DYNAMIC ACCENT THEMES (32 STYLES INCL. OBSIDIAN BLACK, PURE OLED, STEALTH, CYBER, NEON, PASTEL) */
    const ACCENTS = {
        // Luxury Black & Dark Themes
        black: { a: '#ffffff', b: '#27272a', fill: 'linear-gradient(90deg, #52525b, #ffffff)', glow: 'rgba(255, 255, 255, 0.45)', text: '#09090b', name: 'Obsidian Black' },
        oled: { a: '#ffffff', b: '#171717', fill: 'linear-gradient(90deg, #3f3f46, #f4f4f5)', glow: 'rgba(255, 255, 255, 0.35)', text: '#000000', name: 'Pure OLED' },
        stealth: { a: '#d4d4d8', b: '#27272a', fill: 'linear-gradient(90deg, #3f3f46, #e4e4e7)', glow: 'rgba(212, 212, 216, 0.4)', text: '#18181b', name: 'Stealth Carbon' },
        midnight: { a: '#38bdf8', b: '#0f172a', fill: 'linear-gradient(90deg, #0284c7, #38bdf8)', glow: 'rgba(56, 189, 248, 0.5)', text: '#0f172a', name: 'Midnight Dark' },
        dracula: { a: '#bd93f9', b: '#282a36', fill: 'linear-gradient(90deg, #6272a4, #bd93f9)', glow: 'rgba(189, 147, 249, 0.5)', text: '#282a36', name: 'Dracula Gothic' },
        charcoal: { a: '#94a3b8', b: '#1e293b', fill: 'linear-gradient(90deg, #334155, #cbd5e1)', glow: 'rgba(148, 163, 184, 0.4)', text: '#0f172a', name: 'Smoky Charcoal' },

        // Vibrant Neon & Cyber Themes
        cyan: { a: '#38bdf8', b: '#0284c7', glow: 'rgba(56, 189, 248, 0.45)', text: '#0f172a', name: 'Neon Cyan' },
        electric: { a: '#3b82f6', b: '#1d4ed8', glow: 'rgba(59, 130, 246, 0.45)', text: '#ffffff', name: 'Electric Blue' },
        matrix: { a: '#22c55e', b: '#15803d', glow: 'rgba(34, 197, 94, 0.5)', text: '#052e16', name: 'Matrix Green' },
        cyberpunk: { a: '#facc15', b: '#eab308', glow: 'rgba(250, 204, 21, 0.55)', text: '#000000', name: 'Cyberpunk Gold' },
        synthwave: { a: '#f43f5e', b: '#ec4899', glow: 'rgba(244, 63, 94, 0.5)', text: '#ffffff', name: 'Synthwave Pink' },
        plasma: { a: '#a855f7', b: '#7c3aed', glow: 'rgba(168, 85, 247, 0.45)', text: '#ffffff', name: 'Plasma Violet' },
        toxic: { a: '#a3e635', b: '#65a30d', glow: 'rgba(163, 230, 53, 0.5)', text: '#1a2e05', name: 'Toxic Lime' },
        cosmic: { a: '#8b5cf6', b: '#6d28d9', glow: 'rgba(139, 92, 246, 0.45)', text: '#ffffff', name: 'Cosmic Purple' },

        // Precious Metals & Warm Tones
        gold: { a: '#fbbf24', b: '#d97706', glow: 'rgba(251, 191, 36, 0.5)', text: '#0f172a', name: 'Imperial Gold' },
        amber: { a: '#f59e0b', b: '#b45309', glow: 'rgba(245, 158, 11, 0.45)', text: '#0f172a', name: 'Warm Amber' },
        orange: { a: '#ff7849', b: '#ea580c', glow: 'rgba(255, 120, 73, 0.45)', text: '#ffffff', name: 'Blaze Orange' },
        bronze: { a: '#d97706', b: '#92400e', glow: 'rgba(217, 119, 6, 0.45)', text: '#ffffff', name: 'Bronze Copper' },
        crimson: { a: '#e11d48', b: '#9f1239', glow: 'rgba(225, 29, 72, 0.45)', text: '#ffffff', name: 'Royal Crimson' },
        ruby: { a: '#dc2626', b: '#991b1b', glow: 'rgba(220, 38, 38, 0.45)', text: '#ffffff', name: 'Deep Ruby' },
        coral: { a: '#fb7185', b: '#e11d48', glow: 'rgba(251, 113, 133, 0.45)', text: '#0f172a', name: 'Sunset Coral' },

        // Nature, Pastel & Arctic Themes
        emerald: { a: '#10b981', b: '#059669', glow: 'rgba(16, 185, 129, 0.45)', text: '#0f172a', name: 'Emerald Forest' },
        mint: { a: '#2dd4bf', b: '#0d9488', glow: 'rgba(45, 212, 191, 0.45)', text: '#0f172a', name: 'Crisp Mint' },
        teal: { a: '#14b8a6', b: '#0f766e', glow: 'rgba(20, 184, 166, 0.45)', text: '#ffffff', name: 'Ocean Teal' },
        rose: { a: '#f43f5e', b: '#be123c', glow: 'rgba(244, 63, 94, 0.45)', text: '#ffffff', name: 'Velvet Rose' },
        pink: { a: '#ec4899', b: '#be185d', glow: 'rgba(236, 72, 153, 0.45)', text: '#ffffff', name: 'Bubblegum Pink' },
        sakura: { a: '#f472b6', b: '#db2777', glow: 'rgba(244, 114, 182, 0.45)', text: '#ffffff', name: 'Sakura Blossom' },
        lilac: { a: '#c084fc', b: '#9333ea', glow: 'rgba(192, 132, 252, 0.45)', text: '#0f172a', name: 'Pastel Lilac' },
        lavender: { a: '#a78bfa', b: '#7c3aed', glow: 'rgba(167, 139, 250, 0.45)', text: '#ffffff', name: 'French Lavender' },
        nord: { a: '#88c0d0', b: '#5e81ac', glow: 'rgba(136, 192, 208, 0.45)', text: '#0f172a', name: 'Nord Arctic' },
        ice: { a: '#f1f5f9', b: '#94a3b8', glow: 'rgba(241, 245, 249, 0.55)', text: '#0f172a', name: 'Glacier Ice' },
        platinum: { a: '#ffffff', b: '#cbd5e1', glow: 'rgba(255, 255, 255, 0.65)', text: '#0f172a', name: 'Pure Platinum' }
    };

    let currentAccent = 'cyan';
    try { currentAccent = localStorage.getItem('lq_accent') || 'cyan'; } catch (e) { }

    const darkThemes = ['black', 'oled', 'stealth', 'midnight', 'dracula', 'charcoal'];
    function applyAccent(name) {
        if (!ACCENTS[name]) name = 'cyan';
        currentAccent = name;
        try { localStorage.setItem('lq_accent', name); } catch (e) { }
        const chosen = ACCENTS[name];
        const s = document.documentElement.style;
        s.setProperty('--lq-accent', chosen.a);
        s.setProperty('--lq-accent-b', chosen.b);
        s.setProperty('--lq-accent-glow', chosen.glow);
        s.setProperty('--lq-accent-text', chosen.text || '#0f172a');
        if (chosen.fill) {
            s.setProperty('--lq-fill-gradient', chosen.fill);
        } else {
            s.setProperty('--lq-fill-gradient', `linear-gradient(90deg, ${chosen.b}, ${chosen.a})`);
        }

        if (darkThemes.includes(name)) {
            s.setProperty('--lg-bg-color', 'linear-gradient(180deg, rgba(18, 18, 22, 0.88) 0%, rgba(10, 10, 14, 0.82) 50%, rgba(2, 2, 4, 0.96) 100%)');
            s.setProperty('--lg-highlight', 'rgba(255, 255, 255, 0.35)');
        } else {
            s.setProperty('--lg-bg-color', 'linear-gradient(180deg, rgba(255, 255, 255, 0.16) 0%, rgba(255, 255, 255, 0.05) 50%, rgba(10, 15, 30, 0.22) 100%)');
            s.setProperty('--lg-highlight', 'rgba(255, 255, 255, 0.65)');
        }

        const swatchesContainer = document.getElementById('lq-swatches');
        if (swatchesContainer) {
            swatchesContainer.querySelectorAll('.lq-swatch').forEach(el => {
                el.classList.toggle('active', el.dataset.name === name);
            });
        }
        const label = document.getElementById('lq-current-theme-name');
        if (label) label.innerText = chosen.name;
    }
    applyAccent(currentAccent);

    let pulseTimeout = null;
    function showPlayPausePulse(isPlaying) {
        let pulseEl = document.getElementById('lq-center-play-pulse');
        if (!pulseEl) {
            pulseEl = document.createElement('div');
            pulseEl.id = 'lq-center-play-pulse';
            (document.body || document.documentElement).appendChild(pulseEl);
        }
        pulseEl.innerHTML = isPlaying ? '<i class="fas fa-play" style="margin-left:5px;"></i>' : '<i class="fas fa-pause"></i>';
        pulseEl.classList.remove('pulse-anim');
        void pulseEl.offsetWidth;
        pulseEl.classList.add('pulse-anim');
        if (pulseTimeout) clearTimeout(pulseTimeout);
        pulseTimeout = setTimeout(() => pulseEl.classList.remove('pulse-anim'), 650);
    }

    let rippleLeftTimeout = null, rippleRightTimeout = null;
    function showSeekRipple(isForward) {
        let id = isForward ? 'lq-seek-ripple-right' : 'lq-seek-ripple-left';
        let ripple = document.getElementById(id);
        if (!ripple) {
            ripple = document.createElement('div');
            ripple.id = id;
            ripple.className = `lq-seek-ripple-zone ${isForward ? 'ripple-right' : 'ripple-left'}`;
            (document.body || document.documentElement).appendChild(ripple);
        }
        ripple.innerHTML = `<div class="ripple-content"><i class="fas ${isForward ? 'fa-forward' : 'fa-backward'}" style="color:var(--lq-accent, #38bdf8)"></i><span>${isForward ? '+10s' : '-10s'}</span></div>`;
        ripple.classList.remove('active');
        void ripple.offsetWidth;
        ripple.classList.add('active');
        if (isForward) {
            if (rippleRightTimeout) clearTimeout(rippleRightTimeout);
            rippleRightTimeout = setTimeout(() => ripple.classList.remove('active'), 550);
        } else {
            if (rippleLeftTimeout) clearTimeout(rippleLeftTimeout);
            rippleLeftTimeout = setTimeout(() => ripple.classList.remove('active'), 550);
        }
    }

    function show2xHoldHUD(show) {
        let holdEl = document.getElementById('lq-2x-hold-hud');
        if (!holdEl) {
            holdEl = document.createElement('div');
            holdEl.id = 'lq-2x-hold-hud';
            holdEl.innerHTML = `<i class="fas fa-bolt" style="color:#f59e0b"></i><span>2X SPEED ⚡</span>`;
            (document.body || document.documentElement).appendChild(holdEl);
        }
        if (show) {
            holdEl.classList.add('active');
        } else {
            holdEl.classList.remove('active');
        }
    }

    /* 4. AUDIO VOCAL CLARIFIER (Web Audio API) */
    let audioCtx = null, audioSource = null, vocalFilter = null, gainNode = null, isVocalBoostActive = false;
    function toggleVocalClarifier(videoEl) {
        try {
            if (!audioCtx) {
                const AudioContext = window.AudioContext || window.webkitAudioContext;
                audioCtx = new AudioContext();
                audioSource = audioCtx.createMediaElementSource(videoEl);
                vocalFilter = audioCtx.createBiquadFilter();
                vocalFilter.type = 'peaking';
                vocalFilter.frequency.value = 2800;
                vocalFilter.Q.value = 1.2;
                vocalFilter.gain.value = 0;
                gainNode = audioCtx.createGain();
                gainNode.gain.value = 1.0;
                audioSource.connect(vocalFilter);
                vocalFilter.connect(gainNode);
                gainNode.connect(audioCtx.destination);
            }
            if (audioCtx.state === 'suspended') audioCtx.resume();
            isVocalBoostActive = !isVocalBoostActive;
            if (isVocalBoostActive) {
                vocalFilter.gain.value = 6.5;
                gainNode.gain.value = 1.35;
                showOSD('<i class="fas fa-microphone-alt" style="color:var(--lq-accent, #38bdf8)"></i>', 'Voice Boost: ON');
                showGlassToast('Vocal Clarifier Boost enabled', 'success', 'fas fa-microphone-alt');
            } else {
                vocalFilter.gain.value = 0;
                gainNode.gain.value = 1.0;
                showOSD('<i class="fas fa-microphone-slash" style="color:#94a3b8"></i>', 'Voice Boost: OFF');
                showGlassToast('Vocal Clarifier disabled', 'info', 'fas fa-microphone-slash');
            }
            return isVocalBoostActive;
        } catch (e) {
            console.warn("Web Audio Boost notice:", e);
            showGlassToast('Vocal booster unavailable on this stream', 'error', 'fas fa-exclamation-circle');
            return false;
        }
    }

    /* 5. DIRECT PDF NOTES ENGINE */
    async function directOpenPDF() {
        const btn = document.getElementById('c-notes-btn');
        if (!btn) return;
        const oldHtml = btn.innerHTML;
        btn.innerHTML = `<i class="fas fa-spinner fa-spin text-primary"></i>`;
        try {
            const urlParams = new URLSearchParams(window.location.search);
            const batchId = urlParams.get('batchId'), subjectId = urlParams.get('subjectId');
            const scheduleId = urlParams.get('schId') || urlParams.get('lectureId') || urlParams.get('videoId');
            if (!batchId || !scheduleId) {
                showGlassToast("Missing batch parameters in URL.", "error", "fas fa-exclamation-triangle");
                return;
            }
            let res = await window.ParchamCore('pw_sch_dtl', { batchId, subjectId: subjectId || '', scheduleId });
            let item = res?.data, attachments = [];
            if (item) {
                [...(item.homeworkIds || []), ...(item.attachmentIds ? [{ attachmentIds: item.attachmentIds }] : [])].forEach(g => {
                    g.attachmentIds?.forEach(att => {
                        let link = (att.baseUrl || "") + (att.key || "");
                        attachments.push(link.startsWith('http') ? link : "https://static.pw.live/" + link);
                    });
                });
            }
            if (attachments.length > 0) {
                window.open(attachments[0], '_blank');
                showGlassToast("Lecture notes PDF opened in new tab", "success", "fas fa-file-pdf");
            } else {
                showGlassToast("No notes or attachments found for this lecture.", "info", "fas fa-file-alt");
            }
        } catch (e) {
            console.error(e);
            showGlassToast("Could not load PDF notes.", "error", "fas fa-times-circle");
        } finally {
            btn.innerHTML = oldHtml;
        }
    }

    /* 6. MOUNT LIQUID PLAYER ENGINE */
    function mountLiquidPlayer() {
        const video = document.getElementById('video') || document.querySelector('#video-wrapper video, .video-js video, video');
        if (!video || document.getElementById('custom-player-hub')) return;

        ensureSvgFilter();

        /* ALL STATE VARIABLES INITIALIZED AT TOP TO PREVENT TDZ / REFERENCE ERRORS */
        let isUserPaused = false;
        let isBuffering = false;
        let lastObservedPlaybackTime = -1;
        let lastProgressTimestamp = Date.now();
        let frozenDurationMs = 0;
        let lastRecoveryExecutionTime = 0;
        let watchdogInterval = null;

        let currentSpeed = 1.0;
        try {
            const savedSpeed = parseFloat(localStorage.getItem('lq_speed'));
            if (!isNaN(savedSpeed) && savedSpeed >= 0.25 && savedSpeed <= 6.0) currentSpeed = savedSpeed;
        } catch (e) { }

        let pinnedSpeeds = [3.25, 3.5];
        try {
            const savedPins = JSON.parse(localStorage.getItem('lq_pinned_speeds'));
            if (Array.isArray(savedPins)) {
                pinnedSpeeds = savedPins.map(n => parseFloat(n)).filter(n => !isNaN(n) && n >= 0.25 && n <= 6.0).slice(0, 3);
            }
        } catch (e) { }

        let preloadMode = 'ultra';
        try { preloadMode = localStorage.getItem('lq_preload_mode') || 'ultra'; } catch (e) { }

        let autoNextMode = 'last-slide';
        try { autoNextMode = localStorage.getItem('lq_autonext_mode') || 'last-slide'; } catch (e) { }

        let loadedSlides = [], chapterLectures = [], currentLectureIndex = -1, lastVolume = 1;
        let hasDismissedNextPrompt = false, nextPromptCountdownInterval = null;
        let idleTimer = null, mouseAwayTimer = null, isScrubbing = false;
        let lastStatsHubUpdateTime = 0;
        let hasResumedPlayback = false, lastSaveTime = 0;

        // Top Lecture HUD
        const topHud = document.createElement('div');
        topHud.id = 'lq-top-lecture-hud';
        const initialParams = new URLSearchParams(window.location.search);
        function getCleanLectureTitle() {
            const domTitle = document.getElementById('video-title')?.innerText?.trim();
            if (domTitle) return domTitle;
            const urlTitle = initialParams.get('title');
            if (urlTitle) return urlTitle.trim();
            return document.title
                .replace(/\s*-\s*StudyParcham.*/gi, '')
                .replace(/\s*\|\s*Player.*/gi, '')
                .replace(/\s*-\s*Player.*/gi, '')
                .trim() || 'Lecture 1';
        }
        const initialTitle = getCleanLectureTitle();
        let initialLecNum = 1;
        const initialNumMatch = initialTitle.match(/(?:lecture|lec|ep|episode|class)?\s*(\d+)/i);
        if (initialNumMatch && initialNumMatch[1]) initialLecNum = parseInt(initialNumMatch[1], 10);

        topHud.innerHTML = `
            ${GLASS_HTML}
            <button class="top-hud-btn" id="c-prev-lec" title="Previous Lecture"><i class="fas fa-step-backward"></i></button>
            <span class="top-hud-chip" id="c-queue-chip">Lec ${initialLecNum}</span>
            <span class="top-hud-title" id="c-lecture-title" title="${initialTitle}">${initialTitle}</span>
            <button class="top-hud-btn" id="c-next-lec" title="Next Lecture"><i class="fas fa-step-forward"></i></button>
            <button class="top-hud-btn" id="c-queue-badge" title="Chapter Playlist (Click to Toggle)" style="margin-left:2px;"><i class="fas fa-list-ul"></i></button>

            <!-- Top Playlist Dropdown -->
            <div id="lq-top-playlist-dropdown">
                ${GLASS_HTML}

                <!-- 1. Content / Lecture List View -->
                <div id="lq-playlist-lectures-view">
                    <div class="top-playlist-header">
                        <div style="display:flex; align-items:center; gap:8px;">
                            <span><i class="fas fa-list-ul text-primary me-1" style="color:var(--lq-accent, #38bdf8)"></i> <span id="top-playlist-heading">Chapter Content</span></span>
                            <span id="top-playlist-count" style="font-size:0.75rem; color:#94a3b8; font-family:monospace;"></span>
                        </div>
                        <button type="button" id="lq-open-selector-btn" class="lq-panel-btn" title="Change Subject & Chapter / Study Material">
                            <i class="fas fa-exchange-alt" style="color:var(--lq-accent, #38bdf8)"></i> <span>Change</span>
                        </button>
                    </div>

                    <!-- Content Filter Pills -->
                    <div class="lq-content-filter-bar" id="lq-content-filter-bar">
                        <button type="button" class="lq-filter-pill active" data-filter="ALL">All (<span id="count-all">0</span>)</button>
                        <button type="button" class="lq-filter-pill" data-filter="LECTURE"><i class="fas fa-play" style="font-size:0.6rem;"></i> Lecs (<span id="count-lec">0</span>)</button>
                        <button type="button" class="lq-filter-pill" data-filter="NOTES"><i class="fas fa-file-pdf" style="font-size:0.65rem;"></i> Notes (<span id="count-notes">0</span>)</button>
                        <button type="button" class="lq-filter-pill" data-filter="DPP_PDF"><i class="fas fa-file-alt" style="font-size:0.65rem;"></i> DPPs (<span id="count-dpp-pdf">0</span>)</button>
                        <button type="button" class="lq-filter-pill" data-filter="DPP_VIDEOS"><i class="fas fa-video" style="font-size:0.65rem;"></i> Sol (<span id="count-dpp-vid">0</span>)</button>
                    </div>

                    <div class="playlist-stats-hub" id="lq-playlist-stats-hub">
                        <div class="stats-row-cards">
                            <div class="stat-card" title="Total watched duration across this chapter">
                                <div class="stat-label"><i class="fas fa-check-circle" style="color:#10b981;"></i> Covered</div>
                                <div class="stat-val" id="stat-time-covered">0m</div>
                            </div>
                            <div class="stat-card" title="Total remaining duration to complete chapter">
                                <div class="stat-label"><i class="fas fa-hourglass-half" style="color:#f59e0b;"></i> Left</div>
                                <div class="stat-val" id="stat-time-left">0m</div>
                            </div>
                            <div class="stat-card" title="Total duration of all lectures in this chapter">
                                <div class="stat-label"><i class="fas fa-clock" style="color:var(--lq-accent, #38bdf8);"></i> Total</div>
                                <div class="stat-val" id="stat-time-total">0m</div>
                            </div>
                        </div>
                        <div class="playlist-progress-wrapper">
                            <div class="playlist-mini-progress"><div class="playlist-mini-fill" id="stat-progress-fill" style="width: 0%;"></div></div>
                            <div class="playlist-mini-meta">
                                <span id="stat-progress-percent">0% Completed</span>
                                <span id="stat-lectures-ratio">0 / 0 Done</span>
                            </div>
                        </div>
                    </div>
                    <input type="text" id="top-playlist-search" placeholder="Search lectures, notes, DPPs..." class="top-playlist-search-input" />
                    <div class="top-playlist-list" id="top-playlist-list-container">
                        <div style="color:#94a3b8; font-size:0.8rem; padding:10px;">Loading contents...</div>
                    </div>
                </div>

                <!-- 2. Subject & Chapter / Study Material Selector Panel -->
                <div id="lq-playlist-selector-panel">
                    <div class="top-playlist-header">
                        <div style="display:flex; align-items:center; gap:8px;">
                            <button type="button" id="lq-close-selector-btn" class="lq-panel-btn" title="Back to Content">
                                <i class="fas fa-arrow-left"></i> <span>Back</span>
                            </button>
                            <span><i class="fas fa-layer-group text-primary me-1" style="color:var(--lq-accent, #38bdf8)"></i> Select Subject & Topic</span>
                        </div>
                    </div>

                    <div>
                        <div class="lq-panel-label"><i class="fas fa-chalkboard-teacher me-1" style="color:var(--lq-accent, #38bdf8)"></i> Subject / Faculty ("By Sir")</div>
                        <select id="lq-subject-selector" class="lq-glass-select" style="margin-top:4px;">
                            <option value="">Loading subjects...</option>
                        </select>
                    </div>

                    <!-- 2 Tabs: Chapters vs Study Material -->
                    <div class="lq-selector-tabs" id="lq-selector-tabs">
                        <button type="button" class="lq-tab-btn active" id="lq-tab-units" data-type="UNITS">
                            <i class="fas fa-book-open me-1"></i> Chapters
                        </button>
                        <button type="button" class="lq-tab-btn" id="lq-tab-study" data-type="STUDY_MATERIAL">
                            <i class="fas fa-file-pdf me-1"></i> Study Material
                        </button>
                    </div>

                    <div>
                        <div class="lq-panel-label" style="margin-top:2px;">
                            <span><i class="fas fa-folder-open me-1" style="color:var(--lq-accent, #38bdf8)"></i> <span id="lq-selector-section-title">Chapters</span> (<span id="lq-chapter-count">0</span>)</span>
                        </div>
                        <input type="text" id="lq-chapter-search" placeholder="Filter..." class="top-playlist-search-input" style="margin-top:4px; margin-bottom:6px;" />
                        <div class="lq-chapters-list" id="lq-chapters-list-container">
                            <div style="color:#94a3b8; font-size:0.8rem; padding:12px; text-align:center;">Select a subject to view content</div>
                        </div>
                    </div>
                </div>

            </div>
        `;
        (document.body || document.documentElement).appendChild(topHud);

        // Center Buffering Indicator
        const bufferingEl = document.createElement('div');
        bufferingEl.id = 'lq-buffering';
        bufferingEl.innerHTML = `${GLASS_HTML}<div class="lq-buffering-ring"></div><span>Buffering...</span>`;
        (document.body || document.documentElement).appendChild(bufferingEl);

        // Next Lecture Prompt
        const nextPrompt = document.createElement('div');
        nextPrompt.id = 'lq-next-lecture-prompt';
        nextPrompt.innerHTML = `
            ${GLASS_HTML}
            <div class="next-prompt-content">
                <div class="next-prompt-text">
                    <span class="next-prompt-subtitle">UP NEXT IN <b id="next-prompt-timer">10</b>S</span>
                    <span class="next-prompt-title" id="next-prompt-title">Next Lecture</span>
                </div>
                <div class="next-prompt-actions">
                    <button class="next-prompt-btn play-next" id="c-prompt-play-next"><i class="fas fa-forward me-1"></i> Play Next</button>
                    <button class="next-prompt-btn dismiss" id="c-prompt-dismiss" title="Dismiss"><i class="fas fa-times"></i></button>
                </div>
            </div>
            <div class="next-prompt-progress-bar"><div id="next-prompt-fill"></div></div>
        `;
        (document.body || document.documentElement).appendChild(nextPrompt);

        // Bottom Liquid Glass Dock Hub
        const hub = document.createElement('div');
        hub.id = 'custom-player-hub';
        hub.innerHTML = `
            ${GLASS_HTML}
            <button class="hub-btn" id="c-play"><i class="fas fa-play"></i></button>
            <button class="hub-btn" id="c-rwd" title="-10s (←)"><i class="fas fa-undo-alt"></i></button>
            <button class="hub-btn" id="c-fwd" title="+10s (→)"><i class="fas fa-redo-alt"></i></button>

            <div class="hub-progress-box">
                <span class="hub-time" id="c-cur-time">00:00</span>
                <div class="scrubber-track-container" id="scrubber-track-container">
                    <div class="hub-track-bg">
                        <div class="hub-buffer-bar" id="c-buffer-bar"></div>
                        <div class="hub-fill-bar" id="c-fill-bar"></div>
                    </div>
                    <input type="range" class="hub-slider" id="c-seek" min="0" max="100" value="0">
                    <div id="timeline-scrub-separators"></div>

                    <div id="scrubber-hover-preview">
                        ${GLASS_HTML}
                        <img src="" id="hover-preview-img">
                        <div class="preview-meta">
                            <span id="hover-preview-slide">Slide 1</span>
                            <span id="hover-preview-time">00:00</span>
                        </div>
                    </div>
                </div>
                <span class="hub-time" id="c-dur-time">00:00</span>
            </div>

            <button class="hub-btn" id="c-slides-btn" title="Lecture Slides (T)"><i class="fas fa-images"></i></button>
            <button class="hub-btn" id="c-notes-btn" title="Download Notes (PDF)"><i class="fas fa-file-pdf"></i></button>

            <button class="hub-btn hub-speed-badge" id="c-speed-badge" title="Playback Speed (Click to Toggle Settings)" style="font-family:'JetBrains Mono', monospace; font-size:0.75rem; font-weight:800; min-width:38px; padding:0 6px; color:var(--lq-accent, #38bdf8);">1.0x</button>

            <div style="position:relative;">
                <button class="hub-btn" id="c-settings-btn" title="Settings (Speed, Quality, Volume, Audio, Accent)"><i class="fas fa-cog"></i></button>
                <div id="c-settings-menu">
                    ${GLASS_HTML}
                    <div class="settings-drawer-pill"></div>
                    <div class="settings-header"><i class="fas fa-sliders-h"></i> Mobile Player Settings</div>

                    <div class="settings-theme-section">
                        <div class="settings-row" style="margin-bottom:4px;">
                            <span><i class="fas fa-palette me-1" style="color:var(--lq-accent, #38bdf8)"></i> Theme</span>
                            <span id="lq-current-theme-name" style="font-family:'JetBrains Mono', monospace; font-size:0.75rem; font-weight:700; color:var(--lq-accent, #38bdf8);">${ACCENTS[currentAccent]?.name || 'Cyan'}</span>
                        </div>
                        <div id="lq-swatches" class="lq-swatches-grid"></div>
                    </div>

                    <div class="settings-speed-section">
                        <div class="settings-row" style="margin-bottom:2px;">
                            <span><i class="fas fa-tachometer-alt me-1" style="color:var(--lq-accent, #38bdf8)"></i> Speed</span>
                            <select class="settings-select" id="c-settings-speed">
                                <option value="0.5">0.5x</option>
                                <option value="0.75">0.75x</option>
                                <option value="1.0" selected>1.0x Normal</option>
                                <option value="1.25">1.25x</option>
                                <option value="1.5">1.5x</option>
                                <option value="1.75">1.75x</option>
                                <option value="2.0">2.0x</option>
                                <option value="2.25">2.25x</option>
                                <option value="2.5">2.5x</option>
                                <option value="2.75">2.75x</option>
                                <option value="3.0">3.0x</option>
                                <option value="3.25">3.25x</option>
                                <option value="3.5">3.5x</option>
                                <option value="3.75">3.75x</option>
                                <option value="4.0">4.0x</option>
                                <option value="4.5">4.5x</option>
                                <option value="5.0">5.0x</option>
                                <option value="5.5">5.5x</option>
                                <option value="6.0">6.0x</option>
                            </select>
                        </div>
                        <div class="mobile-chips-row" id="lq-quick-speed-chips">
                            <button type="button" class="mobile-chip" data-speed="0.75">0.75x</button>
                            <button type="button" class="mobile-chip active" data-speed="1.0">1x</button>
                            <button type="button" class="mobile-chip" data-speed="1.25">1.25x</button>
                            <button type="button" class="mobile-chip" data-speed="1.5">1.5x</button>
                            <button type="button" class="mobile-chip" data-speed="1.75">1.75x</button>
                            <button type="button" class="mobile-chip" data-speed="2.0">2x</button>
                            <button type="button" class="mobile-chip" data-speed="2.5">2.5x</button>
                            <button type="button" class="mobile-chip" data-speed="3.0">3x</button>
                        </div>
                        <div class="settings-custom-speed-row">
                            <input type="number" id="c-custom-speed-input" min="0.25" max="6" step="0.05" placeholder="Custom (0.25-6x)" class="settings-speed-input" />
                            <button type="button" id="c-custom-speed-set" class="settings-speed-btn" title="Apply Custom Speed">Set</button>
                            <button type="button" id="c-custom-speed-pin" class="settings-speed-btn" title="Pin / Unpin Speed (Max 3)"><i class="fas fa-thumbtack"></i> Pin</button>
                        </div>
                        <div class="settings-pinned-row">
                            <span class="pinned-label"><i class="fas fa-thumbtack me-1" style="font-size:0.7rem; color:var(--lq-accent, #38bdf8)"></i> Pinned (Max 3):</span>
                            <div id="c-pinned-speeds-container" class="pinned-speeds-container"></div>
                        </div>
                    </div>

                    <div style="display: flex; flex-direction: column; gap: 4px;">
                        <div class="settings-row">
                            <span><i class="fas fa-film me-1" style="color:var(--lq-accent, #38bdf8)"></i> Quality</span>
                            <select class="settings-select" id="c-settings-quality">
                                <option value="auto" selected>Auto</option>
                                <option value="1080">1080p FHD</option>
                                <option value="720">720p HD</option>
                                <option value="480">480p</option>
                                <option value="360">360p</option>
                                <option value="240">240p</option>
                            </select>
                        </div>
                        <div class="mobile-chips-row" id="lq-quick-quality-chips">
                            <button type="button" class="mobile-chip active" data-quality="auto">Auto</button>
                            <button type="button" class="mobile-chip" data-quality="1080">1080p</button>
                            <button type="button" class="mobile-chip" data-quality="720">720p</button>
                            <button type="button" class="mobile-chip" data-quality="480">480p</button>
                            <button type="button" class="mobile-chip" data-quality="360">360p</button>
                        </div>
                    </div>

                    <div class="settings-row">
                        <span id="c-vol-label" style="cursor:pointer;"><i class="fas fa-volume-up me-1" style="color:var(--lq-accent, #38bdf8)"></i> Vol</span>
                        <input type="range" class="settings-vol-slider" id="c-settings-vol" min="0" max="1" step="0.05" value="1">
                        <span id="c-vol-percent" style="font-family:monospace; font-size:0.75rem; width:34px; text-align:right;">100%</span>
                    </div>

                    <div class="settings-row">
                        <span><i class="fas fa-microphone-alt me-1" style="color:#818cf8"></i> Voice Boost</span>
                        <button class="settings-toggle-btn" id="c-settings-boost-btn">OFF</button>
                    </div>

                    <div class="settings-row">
                        <span><i class="fas fa-clone me-1" style="color:var(--lq-accent, #38bdf8)"></i> Picture-in-Pic</span>
                        <button class="settings-toggle-btn" id="c-settings-pip-btn">Pop Out</button>
                    </div>

                    <div class="settings-row">
                        <span><i class="fas fa-forward me-1" style="color:var(--lq-accent, #38bdf8)"></i> Auto Next</span>
                        <select class="settings-select" id="c-settings-autonext">
                            <option value="last-slide" selected>Last Slide</option>
                            <option value="5min">Last 5 Min</option>
                            <option value="2min">Last 2 Min</option>
                            <option value="35s">Last 35s</option>
                            <option value="off">Off (Disabled)</option>
                        </select>
                    </div>

                    <div class="settings-row">
                        <span><i class="fas fa-bolt me-1" style="color:#f59e0b"></i> Preload</span>
                        <select class="settings-select" id="c-settings-preload">
                            <option value="max">Max (35s Ahead)</option>
                            <option value="ultra" selected>Ultra (25s Ahead)</option>
                            <option value="normal">Normal (18s Ahead)</option>
                            <option value="low">Eco (10s Ahead)</option>
                        </select>
                    </div>

                    <div class="settings-row" id="c-settings-shortcuts-item" style="cursor:pointer; padding-top:4px; border-top:1px solid rgba(255,255,255,0.1);">
                        <span><i class="fas fa-keyboard me-2" style="color:#fbbf24"></i> Shortcuts</span>
                        <span style="font-size:0.75rem; color:#94a3b8; font-family:monospace;">Press ?</span>
                    </div>
                </div>
            </div>

            <button class="hub-btn" id="c-fullscreen" title="Fullscreen (F)"><i class="fas fa-expand"></i></button>
        `;
        (document.body || document.documentElement).appendChild(hub);

        // Slide Tray
        const tray = document.createElement('div');
        tray.id = 'custom-timeline-tray';
        tray.innerHTML = `
            ${GLASS_HTML}
            <div style="display:flex; justify-content:space-between; align-items:center; font-weight:800; font-size:0.95rem; color:#fff;">
                <span><i class="fas fa-images text-warning me-2" style="color:#fbbf24"></i> Lecture Slides Timeline</span>
                <span id="close-tray" style="cursor:pointer; color:#94a3b8;"><i class="fas fa-times"></i></span>
            </div>
            <div class="tray-grid" id="tray-grid">
                <div style="color:#94a3b8; font-size:0.85rem; padding: 20px;">Loading slides...</div>
            </div>
        `;
        (document.body || document.documentElement).appendChild(tray);

        // Shortcuts Modal
        const SHORTCUTS_DATA = [
            ['Play / Pause', 'Space / K'], ['Seek ±5s', '← / →'], ['Seek ±10s', 'J / L'],
            ['Volume ±10%', '↑ / ↓'], ['Mute Toggle', 'M'], ['Fullscreen', 'F'],
            ['Picture-in-Picture', 'P'], ['Voice Clarifier', 'V'], ['Slides Tray', 'T'],
            ['Playlist Menu', 'Q'], ['Auto Next Mode', 'N'], ['Speed -/+ (0.25x)', '&lt; / &gt; or [ / ]']
        ];
        const shortcutsModal = document.createElement('div');
        shortcutsModal.id = 'custom-shortcuts-modal';
        shortcutsModal.innerHTML = `
            ${GLASS_HTML}
            <div style="display:flex; justify-content:space-between; align-items:center; font-weight:800; font-size:0.95rem; color:#fff;">
                <span><i class="fas fa-keyboard text-primary me-2" style="color:#38bdf8"></i> Keyboard Shortcuts</span>
                <span id="close-shortcuts" style="cursor:pointer; color:#94a3b8;"><i class="fas fa-times"></i></span>
            </div>
            <div class="shortcuts-grid">
                ${SHORTCUTS_DATA.map(([desc, key]) => `<div class="shortcut-item"><span class="shortcut-desc">${desc}</span><span class="shortcut-key">${key}</span></div>`).join('')}
            </div>
        `;
        (document.body || document.documentElement).appendChild(shortcutsModal);

        // Core Hub Element Selectors
        const playBtn = hub.querySelector('#c-play');
        const curTimeEl = hub.querySelector('#c-cur-time');
        const durTimeEl = hub.querySelector('#c-dur-time');
        const seek = hub.querySelector('#c-seek');
        const bufferBar = hub.querySelector('#c-buffer-bar');
        const fillBar = hub.querySelector('#c-fill-bar');
        const settingsBtn = hub.querySelector('#c-settings-btn');
        const settingsMenu = hub.querySelector('#c-settings-menu');
        const settingsSpeed = hub.querySelector('#c-settings-speed');
        const speedBadge = hub.querySelector('#c-speed-badge');
        const customSpeedInput = hub.querySelector('#c-custom-speed-input');
        const customSpeedSetBtn = hub.querySelector('#c-custom-speed-set');
        const customSpeedPinBtn = hub.querySelector('#c-custom-speed-pin');
        const pinnedSpeedsContainer = hub.querySelector('#c-pinned-speeds-container');
        const quickSpeedContainer = settingsMenu.querySelector('#lq-quick-speed-chips');
        const quickQualityContainer = settingsMenu.querySelector('#lq-quick-quality-chips');
        const settingsQuality = hub.querySelector('#c-settings-quality');
        const settingsVol = hub.querySelector('#c-settings-vol');
        const volPercent = hub.querySelector('#c-vol-percent');
        const volLabel = hub.querySelector('#c-vol-label');
        const settingsBoostBtn = hub.querySelector('#c-settings-boost-btn');
        const settingsPipBtn = hub.querySelector('#c-settings-pip-btn');
        const settingsAutoNext = hub.querySelector('#c-settings-autonext');
        const settingsPreload = hub.querySelector('#c-settings-preload');
        const settingsShortcutsItem = hub.querySelector('#c-settings-shortcuts-item');
        const slidesBtn = hub.querySelector('#c-slides-btn');
        const notesBtn = hub.querySelector('#c-notes-btn');
        const closeTray = tray.querySelector('#close-tray');
        const closeShortcuts = shortcutsModal.querySelector('#close-shortcuts');

        // Setup 32 Accent Theme Swatches
        const swatchesContainer = settingsMenu.querySelector('#lq-swatches');
        if (swatchesContainer) {
            swatchesContainer.innerHTML = '';
            Object.keys(ACCENTS).forEach(name => {
                const swatch = document.createElement('div');
                swatch.className = `lq-swatch ${name === currentAccent ? 'active' : ''}`;
                swatch.dataset.name = name;
                swatch.style.background = `linear-gradient(135deg, ${ACCENTS[name].a}, ${ACCENTS[name].b})`;
                swatch.title = ACCENTS[name].name;
                swatch.onmouseenter = () => {
                    const label = document.getElementById('lq-current-theme-name');
                    if (label) label.innerText = ACCENTS[name].name;
                };
                swatch.onmouseleave = () => {
                    const label = document.getElementById('lq-current-theme-name');
                    if (label) label.innerText = ACCENTS[currentAccent]?.name || 'Cyan';
                };
                swatch.onclick = (e) => {
                    e.stopPropagation();
                    applyAccent(name);
                    const label = document.getElementById('lq-current-theme-name');
                    if (label) label.innerText = ACCENTS[name].name;
                    showOSD(`<i class="fas fa-palette" style="color:${ACCENTS[name].a}"></i>`, `${ACCENTS[name].name} Theme`);
                    showGlassToast(`${ACCENTS[name].name} theme activated`, 'info', 'fas fa-palette');
                };
                swatchesContainer.appendChild(swatch);
            });
        }

        // 5-second lecture info intro
        topHud.classList.add('intro-show');
        let hudIntroTimer = setTimeout(() => topHud.classList.remove('intro-show'), 5000);
        let hasTriggeredPlayIntro = false;
        video.addEventListener('play', () => {
            if (!hasTriggeredPlayIntro) {
                hasTriggeredPlayIntro = true;
                topHud.classList.add('intro-show');
                clearTimeout(hudIntroTimer);
                hudIntroTimer = setTimeout(() => topHud.classList.remove('intro-show'), 5000);
            }
        });

        // Top HUD Selectors
        const queueBadge = topHud.querySelector('#c-queue-badge');
        const prevLecBtn = topHud.querySelector('#c-prev-lec');
        const nextLecBtn = topHud.querySelector('#c-next-lec');
        const topPlaylistDropdown = topHud.querySelector('#lq-top-playlist-dropdown');
        const topPlaylistContainer = topHud.querySelector('#top-playlist-list-container');
        const topPlaylistCount = topHud.querySelector('#top-playlist-count');
        const topPlaylistSearch = topHud.querySelector('#top-playlist-search');
        const topPlaylistHeading = topHud.querySelector('#top-playlist-heading');

        // Subject / Chapter Selector Selectors
        const openSelectorBtn = topHud.querySelector('#lq-open-selector-btn');
        const closeSelectorBtn = topHud.querySelector('#lq-close-selector-btn');
        const playlistLecturesView = topHud.querySelector('#lq-playlist-lectures-view');
        const playlistSelectorPanel = topHud.querySelector('#lq-playlist-selector-panel');
        const subjectSelector = topHud.querySelector('#lq-subject-selector');
        const chapterSearch = topHud.querySelector('#lq-chapter-search');
        const chapterCountEl = topHud.querySelector('#lq-chapter-count');
        const chaptersContainer = topHud.querySelector('#lq-chapters-list-container');
        const tabUnitsBtn = topHud.querySelector('#lq-tab-units');
        const tabStudyBtn = topHud.querySelector('#lq-tab-study');
        const selectorSectionTitle = topHud.querySelector('#lq-selector-section-title');
        const contentFilterBar = topHud.querySelector('#lq-content-filter-bar');

        let activeTopicType = 'UNITS'; // 'UNITS' (Chapters) or 'STUDY_MATERIAL'
        let activeContentFilter = 'ALL'; // 'ALL', 'LECTURE', 'NOTES', 'DPP_PDF', 'DPP_VIDEOS'
        let allChapterContents = [];
        let selectedSubjectId = '';
        let selectedTopicId = '';
        let availableSubjects = [];
        let availableChapters = [];

        // Chapter Stats Hub Selectors
        const statCoveredEl = topHud.querySelector('#stat-time-covered');
        const statLeftEl = topHud.querySelector('#stat-time-left');
        const statTotalEl = topHud.querySelector('#stat-time-total');
        const statProgressFillEl = topHud.querySelector('#stat-progress-fill');
        const statProgressPercentEl = topHud.querySelector('#stat-progress-percent');
        const statLecturesRatioEl = topHud.querySelector('#stat-lectures-ratio');

        // Next Lecture Prompt Selectors
        const promptTimerEl = nextPrompt.querySelector('#next-prompt-timer');
        const promptTitleEl = nextPrompt.querySelector('#next-prompt-title');
        const promptFillEl = nextPrompt.querySelector('#next-prompt-fill');
        const promptPlayNextBtn = nextPrompt.querySelector('#c-prompt-play-next');
        const promptDismissBtn = nextPrompt.querySelector('#c-prompt-dismiss');

        const trackContainer = hub.querySelector('#scrubber-track-container');
        const hoverPreview = hub.querySelector('#scrubber-hover-preview');
        const previewImg = hub.querySelector('#hover-preview-img');
        const previewSlideText = hub.querySelector('#hover-preview-slide');
        const previewTimeText = hub.querySelector('#hover-preview-time');

        function isAnyOverlayOpen() {
            return (
                (tray && tray.classList.contains('active')) ||
                (shortcutsModal && shortcutsModal.classList.contains('active')) ||
                (topPlaylistDropdown && topPlaylistDropdown.classList.contains('show')) ||
                (settingsMenu && settingsMenu.classList.contains('show'))
            );
        }

        let topHudIdleTimer = null;
        let topHudMouseAwayTimer = null;

        function scheduleIdleHide(ms = 2500) {
            if (idleTimer) clearTimeout(idleTimer);
            idleTimer = setTimeout(() => {
                if (!isAnyOverlayOpen() && !isScrubbing) hub.classList.remove('user-active');
            }, ms);
        }

        function scheduleTopHudIdleHide(ms = 2500) {
            if (topHudIdleTimer) clearTimeout(topHudIdleTimer);
            topHudIdleTimer = setTimeout(() => {
                if (!topHud.classList.contains('active-dropdown')) {
                    topHud.classList.remove('user-active');
                }
            }, ms);
        }

        function handlePointerActivity(e) {
            if (isAnyOverlayOpen()) return;
            const isNearBottom = e && typeof e.clientY === 'number' ? (e.clientY >= window.innerHeight - 90) : false;
            const isNearTop = e && typeof e.clientY === 'number' ? (e.clientY <= 80) : false;

            if (isNearBottom) {
                hub.classList.add('user-active');
                if (mouseAwayTimer) { clearTimeout(mouseAwayTimer); mouseAwayTimer = null; }
                scheduleIdleHide(2500);
            } else if (hub.classList.contains('user-active') && !isScrubbing) {
                if (!mouseAwayTimer) {
                    mouseAwayTimer = setTimeout(() => {
                        if (!isAnyOverlayOpen() && !isScrubbing) hub.classList.remove('user-active');
                        mouseAwayTimer = null;
                    }, 700);
                }
            }

            if (isNearTop) {
                topHud.classList.add('user-active');
                if (topHudMouseAwayTimer) { clearTimeout(topHudMouseAwayTimer); topHudMouseAwayTimer = null; }
                scheduleTopHudIdleHide(2500);
            } else if (topHud.classList.contains('user-active') && !topHud.classList.contains('active-dropdown')) {
                if (!topHudMouseAwayTimer) {
                    topHudMouseAwayTimer = setTimeout(() => {
                        if (!topHud.classList.contains('active-dropdown')) topHud.classList.remove('user-active');
                        topHudMouseAwayTimer = null;
                    }, 700);
                }
            }
        }

        window.addEventListener('mousemove', handlePointerActivity);
        window.addEventListener('mousedown', (e) => {
            if (isAnyOverlayOpen()) return;
            if (e && typeof e.clientY === 'number') {
                if (e.clientY >= window.innerHeight - 90) {
                    hub.classList.add('user-active');
                    scheduleIdleHide(2500);
                } else if (e.clientY <= 80) {
                    topHud.classList.add('user-active');
                    scheduleTopHudIdleHide(2500);
                }
            }
        });

        // Mobile Touch & Gesture Engine (Phones / Touchscreens)
        let lastTapTime = 0;
        let lastTapPos = { x: 0, y: 0 };
        let touchStartPos = { x: 0, y: 0 };
        let touchStartTime = 0;
        let hold2xTimer = null;
        let isHolding2x = false;
        let speedBeforeHold = 1.0;
        let singleTapTimer = null;

        function cancelHold2x() {
            if (hold2xTimer) {
                clearTimeout(hold2xTimer);
                hold2xTimer = null;
            }
            if (isHolding2x) {
                isHolding2x = false;
                try {
                    video.playbackRate = speedBeforeHold;
                } catch (e) { }
                show2xHoldHUD(false);
                if (speedBadge) speedBadge.innerText = `${speedBeforeHold}x`;
            }
        }

        window.addEventListener('touchstart', (e) => {
            if (e.touches && e.touches.length === 1) {
                const touch = e.touches[0];
                touchStartTime = Date.now();
                touchStartPos = { x: touch.clientX, y: touch.clientY };

                const target = e.target;
                const isControlElement = target && target.closest && target.closest(
                    '#custom-player-hub, #lq-top-lecture-hud, #custom-timeline-tray, #custom-shortcuts-modal, #c-settings-menu, #lq-next-lecture-prompt, .lq-chapter-item, .playlist-item, button, input, select'
                );

                if (!isControlElement && !isAnyOverlayOpen()) {
                    // Start Hold-for-2X Speed Timer (fires after 360ms of holding)
                    cancelHold2x();
                    hold2xTimer = setTimeout(() => {
                        if (!video.paused) {
                            isHolding2x = true;
                            speedBeforeHold = currentSpeed || video.playbackRate || 1.0;
                            try {
                                video.playbackRate = 2.0;
                            } catch (err) { }
                            show2xHoldHUD(true);
                            if (navigator.vibrate) {
                                try { navigator.vibrate(35); } catch (err) { }
                            }
                        }
                    }, 360);
                }
            } else {
                cancelHold2x();
            }
        }, { passive: true });

        window.addEventListener('touchmove', (e) => {
            if (e.touches && e.touches.length === 1) {
                const touch = e.touches[0];
                const dx = Math.abs(touch.clientX - touchStartPos.x);
                const dy = Math.abs(touch.clientY - touchStartPos.y);
                // If finger moves more than 20px before 2x engaged, cancel hold timer
                if ((dx > 20 || dy > 20) && !isHolding2x) {
                    cancelHold2x();
                }
            }
        }, { passive: true });

        window.addEventListener('touchend', (e) => {
            if (isHolding2x) {
                cancelHold2x();
                // If user was holding for 2x, release does NOT trigger play/pause or tap seek
                if (e.cancelable) e.preventDefault();
                return;
            }
            if (hold2xTimer) {
                clearTimeout(hold2xTimer);
                hold2xTimer = null;
            }

            if (isAnyOverlayOpen()) return;

            const touch = e.changedTouches ? e.changedTouches[0] : null;
            if (!touch) return;

            const target = e.target;
            if (target && target.closest && target.closest(
                '#custom-player-hub, #lq-top-lecture-hud, #custom-timeline-tray, #custom-shortcuts-modal, #c-settings-menu, #lq-next-lecture-prompt, .lq-chapter-item, .playlist-item, button, input, select'
            )) {
                return;
            }

            const dx = Math.abs(touch.clientX - touchStartPos.x);
            const dy = Math.abs(touch.clientY - touchStartPos.y);
            const dt = Date.now() - touchStartTime;

            // Only recognize as tap if finger didn't move much and duration was short (<380ms)
            if (dx < 22 && dy < 22 && dt < 380) {
                const now = Date.now();
                const timeSinceLastTouch = now - lastTapTime;
                const distFromLastTouch = Math.hypot(touch.clientX - lastTapPos.x, touch.clientY - lastTapPos.y);
                const screenWidth = window.innerWidth || window.screen?.width || 360;
                const xRatio = touch.clientX / screenWidth;

                if (timeSinceLastTouch < 340 && distFromLastTouch < 60) {
                    // Double Tap Gesture
                    if (singleTapTimer) {
                        clearTimeout(singleTapTimer);
                        singleTapTimer = null;
                    }
                    lastTapTime = 0;

                    if (xRatio < 0.38) {
                        // Double tap LEFT: Rewind 10s
                        video.currentTime = Math.max(0, video.currentTime - 10);
                        showOSD('<i class="fas fa-undo-alt" style="color:var(--lq-accent, #38bdf8)"></i>', '-10s');
                        showSeekRipple(false);
                    } else if (xRatio > 0.62) {
                        // Double tap RIGHT: Forward 10s
                        video.currentTime = Math.min(video.duration || 0, video.currentTime + 10);
                        showOSD('<i class="fas fa-redo-alt" style="color:var(--lq-accent, #38bdf8)"></i>', '+10s');
                        showSeekRipple(true);
                    } else {
                        // Double tap CENTER: Toggle Play / Pause
                        togglePlay();
                    }
                } else {
                    // Single Tap Candidate
                    lastTapTime = now;
                    lastTapPos = { x: touch.clientX, y: touch.clientY };

                    singleTapTimer = setTimeout(() => {
                        singleTapTimer = null;
                        // Single tap behavior: Toggle HUD dock & top bar visibility
                        const isHubActive = hub.classList.contains('user-active');
                        if (isHubActive) {
                            hub.classList.remove('user-active');
                            topHud.classList.remove('user-active');
                        } else {
                            hub.classList.add('user-active');
                            topHud.classList.add('user-active');
                            scheduleIdleHide(3500);
                            scheduleTopHudIdleHide(3500);
                        }
                    }, 280);
                }
            }
        }, { passive: true });

        window.addEventListener('touchcancel', () => {
            cancelHold2x();
            if (singleTapTimer) {
                clearTimeout(singleTapTimer);
                singleTapTimer = null;
            }
        }, { passive: true });

        window.addEventListener('resize', () => {
            scheduleIdleHide(2000);
            renderSlideSeparators();
        });
        window.addEventListener('orientationchange', () => {
            setTimeout(() => {
                scheduleIdleHide(2000);
                renderSlideSeparators();
            }, 300);
        });

        [hub].forEach(el => {
            if (!el) return;
            el.addEventListener('mouseenter', () => {
                hub.classList.add('user-active');
                if (idleTimer) clearTimeout(idleTimer);
                if (mouseAwayTimer) { clearTimeout(mouseAwayTimer); mouseAwayTimer = null; }
            });
            el.addEventListener('mouseleave', () => {
                if (!isAnyOverlayOpen() && !isScrubbing) scheduleIdleHide(700);
            });
        });

        [topHud].forEach(el => {
            if (!el) return;
            el.addEventListener('mouseenter', () => {
                topHud.classList.add('user-active');
                if (topHudIdleTimer) clearTimeout(topHudIdleTimer);
                if (topHudMouseAwayTimer) { clearTimeout(topHudMouseAwayTimer); topHudMouseAwayTimer = null; }
            });
            el.addEventListener('mouseleave', () => {
                if (!topHud.classList.contains('active-dropdown')) scheduleTopHudIdleHide(700);
            });
        });

        document.addEventListener('mouseleave', () => {
            if (!isAnyOverlayOpen() && !isScrubbing) scheduleIdleHide(400);
            if (!topHud.classList.contains('active-dropdown')) scheduleTopHudIdleHide(400);
        });

        function togglePlay(explicitState) {
            const shouldPlay = (typeof explicitState === 'boolean') ? explicitState : video.paused;
            if (shouldPlay) {
                isUserPaused = false;
                video.play().catch(() => { });
                playBtn.innerHTML = `<i class="fas fa-pause"></i>`;
                showOSD('<i class="fas fa-play" style="color:var(--lq-accent, #38bdf8)"></i>', 'Play');
                showPlayPausePulse(true);
                scheduleIdleHide(2000);
            } else {
                isUserPaused = true;
                video.pause();
                playBtn.innerHTML = `<i class="fas fa-play"></i>`;
                showOSD('<i class="fas fa-pause" style="color:var(--lq-accent, #38bdf8)"></i>', 'Pause');
                showBuffering(false);
                showPlayPausePulse(false);
                hub.classList.add('user-active');
                scheduleIdleHide(2500);
            }
        }
        playBtn.onclick = (e) => {
            if (e) e.stopPropagation();
            togglePlay();
        };
        // Desktop / Click fallback: Center click toggles Play/Pause directly
        video.onclick = (e) => {
            if (e) {
                e.preventDefault();
                e.stopPropagation();
            }
            togglePlay();
        };

        const rwdBtn = hub.querySelector('#c-rwd');
        const fwdBtn = hub.querySelector('#c-fwd');
        function handleSeekBack(e) {
            if (e) { e.preventDefault(); e.stopPropagation(); }
            video.currentTime = Math.max(0, video.currentTime - 10);
            showOSD('<i class="fas fa-undo-alt" style="color:var(--lq-accent, #38bdf8)"></i>', '-10s');
            showPlayPausePulse(false);
        }
        function handleSeekFwd(e) {
            if (e) { e.preventDefault(); e.stopPropagation(); }
            video.currentTime = Math.min(video.duration || 0, video.currentTime + 10);
            showOSD('<i class="fas fa-redo-alt" style="color:var(--lq-accent, #38bdf8)"></i>', '+10s');
            showPlayPausePulse(true);
        }
        if (rwdBtn) {
            rwdBtn.addEventListener('click', handleSeekBack);
            rwdBtn.addEventListener('touchstart', handleSeekBack, { passive: false });
        }
        if (fwdBtn) {
            fwdBtn.addEventListener('click', handleSeekFwd);
            fwdBtn.addEventListener('touchstart', handleSeekFwd, { passive: false });
        }

        function formatTime(s) {
            if (isNaN(s) || s < 0) return "00:00";
            const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = Math.floor(s % 60);
            return h > 0 ? `${h}:${m < 10 ? '0' : ''}${m}:${sec < 10 ? '0' : ''}${sec}` : `${m < 10 ? '0' : ''}${m}:${sec < 10 ? '0' : ''}${sec}`;
        }

        /* 7. TOP-RIGHT PLAYLIST DROPDOWN ENGINE */
        function toggleTopPlaylistDropdown(force) {
            const isShowing = topPlaylistDropdown.classList.contains('show');
            const targetState = (typeof force === 'boolean') ? force : !isShowing;
            if (targetState) {
                renderTopPlaylist(topPlaylistSearch?.value || '');
                topPlaylistDropdown.classList.add('show');
                topHud.classList.add('active-dropdown');
                queueBadge.classList.add('active');
            } else {
                topPlaylistDropdown.classList.remove('show');
                topHud.classList.remove('active-dropdown');
                queueBadge.classList.remove('active');
                closePlaylistSelectorPanel();
            }
        }

        queueBadge.onclick = (e) => { e.stopPropagation(); toggleTopPlaylistDropdown(); };
        const queueChip = topHud.querySelector('#c-queue-chip');
        if (queueChip) {
            queueChip.style.cursor = 'pointer';
            queueChip.onclick = (e) => { e.stopPropagation(); toggleTopPlaylistDropdown(); };
        }

        if (topPlaylistSearch) {
            topPlaylistSearch.addEventListener('input', (e) => renderTopPlaylist(e.target.value));
            topPlaylistSearch.addEventListener('click', (e) => e.stopPropagation());
            topPlaylistSearch.addEventListener('keydown', (e) => e.stopPropagation());
        }

        /* 7.5. SUBJECT & CHAPTER / STUDY MATERIAL SELECTOR ENGINE ("BY WHICH SIR") */
        function openPlaylistSelectorPanel() {
            if (playlistLecturesView) playlistLecturesView.style.display = 'none';
            if (playlistSelectorPanel) playlistSelectorPanel.classList.add('show');
            loadBatchSubjects();
        }

        function closePlaylistSelectorPanel() {
            if (playlistSelectorPanel) playlistSelectorPanel.classList.remove('show');
            if (playlistLecturesView) playlistLecturesView.style.display = 'flex';
        }

        if (openSelectorBtn) {
            openSelectorBtn.onclick = (e) => {
                e.stopPropagation();
                openPlaylistSelectorPanel();
            };
        }

        if (closeSelectorBtn) {
            closeSelectorBtn.onclick = (e) => {
                e.stopPropagation();
                closePlaylistSelectorPanel();
            };
        }

        if (tabUnitsBtn && tabStudyBtn) {
            tabUnitsBtn.onclick = (e) => {
                e.stopPropagation();
                if (activeTopicType === 'UNITS') return;
                activeTopicType = 'UNITS';
                tabUnitsBtn.classList.add('active');
                tabStudyBtn.classList.remove('active');
                if (selectorSectionTitle) selectorSectionTitle.innerText = 'Chapters';
                loadSubjectChapters(selectedSubjectId);
            };
            tabStudyBtn.onclick = (e) => {
                e.stopPropagation();
                if (activeTopicType === 'STUDY_MATERIAL') return;
                activeTopicType = 'STUDY_MATERIAL';
                tabStudyBtn.classList.add('active');
                tabUnitsBtn.classList.remove('active');
                if (selectorSectionTitle) selectorSectionTitle.innerText = 'Study Material';
                loadSubjectChapters(selectedSubjectId);
            };
        }

        if (contentFilterBar) {
            contentFilterBar.querySelectorAll('.lq-filter-pill').forEach(pill => {
                pill.onclick = (e) => {
                    e.stopPropagation();
                    contentFilterBar.querySelectorAll('.lq-filter-pill').forEach(p => p.classList.remove('active'));
                    pill.classList.add('active');
                    activeContentFilter = pill.dataset.filter || 'ALL';
                    renderTopPlaylist(topPlaylistSearch?.value || '');
                };
            });
        }

        if (subjectSelector) {
            subjectSelector.onclick = (e) => e.stopPropagation();
            subjectSelector.onchange = (e) => {
                e.stopPropagation();
                const subjId = e.target.value;
                if (subjId) {
                    selectedSubjectId = subjId;
                    loadSubjectChapters(subjId);
                }
            };
        }

        if (chapterSearch) {
            chapterSearch.onclick = (e) => e.stopPropagation();
            chapterSearch.onkeydown = (e) => e.stopPropagation();
            chapterSearch.oninput = (e) => renderChaptersList(e.target.value);
        }

        async function loadBatchSubjects() {
            const urlParams = new URLSearchParams(window.location.search);
            const batchId = urlParams.get('batchId') || localStorage.getItem('batchId') || sessionStorage.getItem('batchId') || (JSON.parse(localStorage.getItem('pw_enrolled_batches') || '[]')[0]);
            if (!batchId) {
                if (subjectSelector) subjectSelector.innerHTML = '<option value="">No Batch ID found</option>';
                return;
            }

            if (availableSubjects.length === 0) {
                try {
                    const res = await window.ParchamCore('pw_btch_dtl', { batchId });
                    const batchData = res?.data || res;
                    const rawSubs = batchData?.subjects || [];
                    availableSubjects = rawSubs.map(s => {
                        let name = s.subject || s.name || 'Subject';
                        if (s.teacherIds && s.teacherIds.length > 0 && !name.toLowerCase().includes(' by ') && !name.toLowerCase().includes('sir')) {
                            const tNames = s.teacherIds.map(t => `${t.firstName || ''} ${t.lastName || ''}`.trim()).filter(Boolean);
                            if (tNames.length > 0) name += ` By ${tNames.join(', ')}`;
                        }
                        return { id: s._id || s.id, name };
                    }).filter(s => s.id);
                } catch (err) {
                    console.warn('[LiquidPlayer] Batch subjects load error:', err);
                }
            }

            if (subjectSelector) {
                if (availableSubjects.length === 0) {
                    subjectSelector.innerHTML = '<option value="">No subjects found</option>';
                    return;
                }
                subjectSelector.innerHTML = availableSubjects.map(s => `
                    <option value="${s.id}" ${s.id === selectedSubjectId ? 'selected' : ''}>${s.name}</option>
                `).join('');
                if (!selectedSubjectId && availableSubjects.length > 0) {
                    selectedSubjectId = availableSubjects[0].id;
                }
                loadSubjectChapters(selectedSubjectId);
            }
        }

        async function loadSubjectChapters(subjId) {
            if (!chaptersContainer) return;
            const urlParams = new URLSearchParams(window.location.search);
            const batchId = urlParams.get('batchId') || localStorage.getItem('batchId') || sessionStorage.getItem('batchId') || (JSON.parse(localStorage.getItem('pw_enrolled_batches') || '[]')[0]);

            const typeLabel = (activeTopicType === 'STUDY_MATERIAL') ? 'study material' : 'chapters';
            chaptersContainer.innerHTML = `<div style="color:#94a3b8; font-size:0.8rem; padding:12px; text-align:center;"><i class="fas fa-spinner fa-spin me-2" style="color:var(--lq-accent, #38bdf8)"></i> Loading ${typeLabel}...</div>`;
            if (chapterCountEl) chapterCountEl.innerText = '0';

            try {
                const topicsRes = await window.ParchamCore('pw_sub_topics', { batchId, subjectId: subjId, page: 1, tagType: activeTopicType, limit: 60 });
                const topics = (topicsRes?.data?.data) || (topicsRes?.data) || [];
                availableChapters = topics.map(t => ({
                    id: t._id || t.id,
                    name: t.name || (activeTopicType === 'STUDY_MATERIAL' ? 'Material Topic' : 'Chapter'),
                    count: t.totalVideos || t.count || 0
                })).filter(c => c.id);

                if (chapterCountEl) chapterCountEl.innerText = availableChapters.length;
                renderChaptersList(chapterSearch?.value || '');
            } catch (err) {
                console.warn('[LiquidPlayer] Topics load error:', err);
                chaptersContainer.innerHTML = `<div style="color:#ef4444; font-size:0.8rem; padding:12px; text-align:center;">Failed to load ${typeLabel}</div>`;
            }
        }

        function renderChaptersList(filter = '') {
            if (!chaptersContainer) return;
            const q = (filter || '').trim().toLowerCase();
            chaptersContainer.innerHTML = '';

            const filtered = availableChapters.filter(c => !q || c.name.toLowerCase().includes(q));
            if (filtered.length === 0) {
                const typeLabel = (activeTopicType === 'STUDY_MATERIAL') ? 'study materials' : 'chapters';
                chaptersContainer.innerHTML = `<div style="color:#94a3b8; font-size:0.8rem; padding:12px; text-align:center;">No ${typeLabel} found matching "${filter}"</div>`;
                return;
            }

            filtered.forEach((ch, idx) => {
                const isCurrent = (ch.id === selectedTopicId);
                const item = document.createElement('div');
                item.className = `lq-chapter-item ${isCurrent ? 'active' : ''}`;
                item.dataset.topicId = ch.id;
                const icon = (activeTopicType === 'STUDY_MATERIAL') ? 'fas fa-file-pdf' : 'fas fa-book-open';
                item.innerHTML = `
                    <div style="display:flex; align-items:center; gap:8px; overflow:hidden;">
                        <span style="font-size:0.75rem; opacity:0.85; font-family:monospace; min-width:22px;">#${idx + 1}</span>
                        <span style="font-size:0.82rem; font-weight:600; text-overflow:ellipsis; white-space:nowrap; overflow:hidden;" title="${ch.name}">${ch.name}</span>
                    </div>
                    <div style="display:flex; align-items:center; gap:6px; flex-shrink:0;">
                        ${ch.count > 0 ? `<span style="font-size:0.7rem; color:#94a3b8; font-family:monospace;">${ch.count} items</span>` : ''}
                        <i class="fas fa-chevron-right" style="font-size:0.7rem; opacity:0.6;"></i>
                    </div>
                `;
                item.onclick = (e) => {
                    e.stopPropagation();
                    selectChapterAndLoadLectures(selectedSubjectId, ch.id, ch.name);
                };
                chaptersContainer.appendChild(item);
            });
        }

        function extractPdfUrl(item) {
            if (!item) return '';
            if (item.fileUrl && typeof item.fileUrl === 'string') return item.fileUrl;
            if (item.url && typeof item.url === 'string' && item.url.toLowerCase().endsWith('.pdf')) return item.url;

            const atts = item.attachmentIds || item.notesDetails?.attachmentIds || item.dppPDFDetails?.attachmentIds || [];
            if (atts.length > 0 && atts[0]) {
                const a = atts[0];
                const base = a.baseUrl || 'https://static.pw.live/';
                const key = a.key || '';
                if (key) return (key.startsWith('http') ? key : (base.startsWith('http') ? base : 'https://static.pw.live/' + base) + key);
            }

            const hws = item.homeworkIds || item.videoDetails?.homeworkIds || [];
            for (const hw of hws) {
                const hAtts = hw.attachmentIds || [];
                if (hAtts.length > 0 && hAtts[0]?.key) {
                    const base = hAtts[0].baseUrl || 'https://static.pw.live/';
                    const key = hAtts[0].key;
                    return (key.startsWith('http') ? key : (base.startsWith('http') ? base : 'https://static.pw.live/' + base) + key);
                }
            }
            return '';
        }

        async function fetchTopicCategoryItems(batchId, subjectId, topicId, v2Type, v3Type) {
            try {
                let res = await window.ParchamCore('pw_v2_list', { batchId, subjectId, tagId: topicId, contentType: v2Type, page: 1 });
                let raw = res?.data?.data || res?.data || [];
                if (!raw || raw.length === 0) {
                    let resV3 = await window.ParchamCore('pw_sch_cntnt', { batchId, subjectId, contentType: v3Type, tagId: topicId, skip: 0, limit: 60 });
                    raw = (resV3?.data || []).map(w => w.data || w);
                }
                return Array.isArray(raw) ? raw : [];
            } catch (e) {
                return [];
            }
        }

        async function selectChapterAndLoadLectures(subjId, topicId, chapterName) {
            selectedSubjectId = subjId;
            selectedTopicId = topicId;
            if (topPlaylistHeading) topPlaylistHeading.innerText = chapterName || 'Chapter Content';
            showGlassToast(`Loading "${chapterName}"...`, 'info', 'fas fa-spinner fa-spin');

            const urlParams = new URLSearchParams(window.location.search);
            const batchId = urlParams.get('batchId') || localStorage.getItem('batchId') || sessionStorage.getItem('batchId') || (JSON.parse(localStorage.getItem('pw_enrolled_batches') || '[]')[0]);

            // Keep URL in sync so bookmarks or refreshes stay on selected chapter
            try {
                urlParams.set('subjectId', subjId);
                urlParams.set('tagId', topicId);
                urlParams.set('topicId', topicId);
                window.history.replaceState({}, '', `${window.location.pathname}?${urlParams.toString()}`);
            } catch (e) { }

            try {
                // Fetch Videos, Notes, DPP PDFs, and DPP Videos simultaneously!
                const [videosRes, notesRes, dppPdfsRes, dppVideosRes] = await Promise.allSettled([
                    fetchTopicCategoryItems(batchId, subjId, topicId, 'videos', 'LECTURE'),
                    fetchTopicCategoryItems(batchId, subjId, topicId, 'notes', 'NOTES'),
                    fetchTopicCategoryItems(batchId, subjId, topicId, 'DppNotes', 'DPP_PDF'),
                    fetchTopicCategoryItems(batchId, subjId, topicId, 'DppVideos', 'DPP_VIDEOS')
                ]);

                const rawVideos = videosRes.status === 'fulfilled' ? videosRes.value : [];
                const rawNotes = notesRes.status === 'fulfilled' ? notesRes.value : [];
                const rawDppPdfs = dppPdfsRes.status === 'fulfilled' ? dppPdfsRes.value : [];
                const rawDppVideos = dppVideosRes.status === 'fulfilled' ? dppVideosRes.value : [];

                const parsedLectures = rawVideos.map((item) => {
                    let d = item?.data || item || {};
                    return {
                        id: d._id || d.videoDetails?._id || item?._id || String(Math.random()),
                        schId: item?._id || d._id || '',
                        type: 'LECTURE',
                        title: d.topic || d.name || 'Lecture',
                        duration: d.videoDetails?.duration || d.duration || '1h',
                        url: d.videoDetails?.videoUrl || d.url || '',
                        pdfUrl: extractPdfUrl(d),
                        date: new Date(d.date || d.startTime || 0).getTime()
                    };
                });
                parsedLectures.sort((a, b) => a.date - b.date);
                parsedLectures.forEach((lec, idx) => lec.index = idx + 1);

                const parsedNotes = rawNotes.map((item) => {
                    let d = item?.data || item || {};
                    return {
                        id: d._id || item?._id || String(Math.random()),
                        schId: item?._id || d._id || '',
                        type: 'NOTES',
                        title: d.topic || d.name || 'Lecture Notes',
                        duration: 'PDF',
                        url: '',
                        pdfUrl: extractPdfUrl(d),
                        date: new Date(d.date || d.startTime || 0).getTime()
                    };
                });

                const parsedDppPdfs = rawDppPdfs.map((item) => {
                    let d = item?.data || item || {};
                    return {
                        id: d._id || item?._id || String(Math.random()),
                        schId: item?._id || d._id || '',
                        type: 'DPP_PDF',
                        title: d.topic || d.name || 'DPP Problem Sheet',
                        duration: 'PDF',
                        url: '',
                        pdfUrl: extractPdfUrl(d),
                        date: new Date(d.date || d.startTime || 0).getTime()
                    };
                });

                const parsedDppVideos = rawDppVideos.map((item) => {
                    let d = item?.data || item || {};
                    return {
                        id: d._id || d.videoDetails?._id || item?._id || String(Math.random()),
                        schId: item?._id || d._id || '',
                        type: 'DPP_VIDEOS',
                        title: d.topic || d.name || 'DPP Video Solution',
                        duration: d.videoDetails?.duration || d.duration || '30m',
                        url: d.videoDetails?.videoUrl || d.url || '',
                        pdfUrl: extractPdfUrl(d),
                        date: new Date(d.date || d.startTime || 0).getTime()
                    };
                });

                chapterLectures = parsedLectures;
                allChapterContents = [...parsedLectures, ...parsedNotes, ...parsedDppPdfs, ...parsedDppVideos];

                // Update filter pill badges
                const countAll = topHud.querySelector('#count-all');
                const countLec = topHud.querySelector('#count-lec');
                const countNotes = topHud.querySelector('#count-notes');
                const countDppPdf = topHud.querySelector('#count-dpp-pdf');
                const countDppVid = topHud.querySelector('#count-dpp-vid');

                if (countAll) countAll.innerText = allChapterContents.length;
                if (countLec) countLec.innerText = parsedLectures.length;
                if (countNotes) countNotes.innerText = parsedNotes.length;
                if (countDppPdf) countDppPdf.innerText = parsedDppPdfs.length;
                if (countDppVid) countDppVid.innerText = parsedDppVideos.length;

                const currentVideoId = urlParams.get('videoId') || urlParams.get('schId') || urlParams.get('lectureId');
                currentLectureIndex = chapterLectures.findIndex(l =>
                    l.id === currentVideoId ||
                    l.schId === currentVideoId ||
                    (currentVideoId && (String(l.id).includes(currentVideoId) || String(l.schId).includes(currentVideoId)))
                );
                if (currentLectureIndex === -1 && chapterLectures.length > 0) currentLectureIndex = 0;

                updateQueueBadge();
                updatePrevNextButtons();
                closePlaylistSelectorPanel();
                renderTopPlaylist(topPlaylistSearch?.value || '');
                showGlassToast(`Loaded "${chapterName}" (${allChapterContents.length} items)`, 'success', 'fas fa-check-circle');
            } catch (err) {
                console.warn('[LiquidPlayer] Failed to load topic contents:', err);
                showGlassToast('Failed to load content for this topic', 'error', 'fas fa-exclamation-triangle');
            }
        }

        /* 8. CRUNCHYROLL-STYLE NEXT LECTURE PROMPT ENGINE */
        function triggerNextLecturePrompt() {
            if (hasDismissedNextPrompt || currentLectureIndex >= chapterLectures.length - 1 || nextPrompt.classList.contains('show')) return;
            const nextLec = chapterLectures[currentLectureIndex + 1];
            if (!nextLec) return;

            promptTitleEl.innerText = nextLec.title || 'Next Lecture';
            nextPrompt.classList.add('show');
            promptTimerEl.innerText = 10;
            promptFillEl.style.width = '0%';

            let elapsedMs = 0;
            const totalMs = 10000, intervalStep = 100;
            if (nextPromptCountdownInterval) clearInterval(nextPromptCountdownInterval);
            nextPromptCountdownInterval = setInterval(() => {
                elapsedMs += intervalStep;
                promptTimerEl.innerText = Math.max(0, Math.ceil((totalMs - elapsedMs) / 1000));
                promptFillEl.style.width = `${Math.min(100, (elapsedMs / totalMs) * 100)}%`;
                if (elapsedMs >= totalMs) {
                    clearInterval(nextPromptCountdownInterval);
                    navigateToLecture(currentLectureIndex + 1);
                }
            }, intervalStep);
        }

        function dismissNextLecturePrompt() {
            hasDismissedNextPrompt = true;
            if (nextPromptCountdownInterval) clearInterval(nextPromptCountdownInterval);
            nextPrompt.classList.remove('show');
        }

        promptPlayNextBtn.onclick = () => {
            if (nextPromptCountdownInterval) clearInterval(nextPromptCountdownInterval);
            navigateToLecture(currentLectureIndex + 1);
        };
        promptDismissBtn.onclick = dismissNextLecturePrompt;

        /* BUFFERING & NON-INTRUSIVE WATCHDOG ENGINE */
        function showBuffering(show) {
            if (show) {
                if (video.paused && isUserPaused && !video.seeking) return;
                bufferingEl.classList.add('lq-show');
                isBuffering = true;
            } else {
                bufferingEl.classList.remove('lq-show');
                isBuffering = false;
            }
        }

        function triggerStreamEngineRetry() {
            try {
                if (window.player?.retryStreaming) window.player.retryStreaming();
                if (window.shakaPlayer?.retryStreaming) window.shakaPlayer.retryStreaming();
                if (window.hls?.startLoad) window.hls.startLoad();
            } catch (e) {
                console.warn("[LiquidPlayer] Stream retry error:", e);
            }
        }

        if (watchdogInterval) clearInterval(watchdogInterval);
        watchdogInterval = setInterval(() => {
            if (!video) return;

            // If user paused, video ended, or seeking: never show buffering or trigger recovery
            if (video.paused || video.ended) {
                frozenDurationMs = 0;
                lastProgressTimestamp = Date.now();
                lastObservedPlaybackTime = video.currentTime;
                if (!video.seeking && isBuffering) showBuffering(false);
                return;
            }

            if (video.seeking) {
                showBuffering(true);
                return;
            }

            const curTime = video.currentTime;
            const now = Date.now();
            const timeAdvanced = Math.abs(curTime - lastObservedPlaybackTime);

            // If frames are advancing smoothly
            if (timeAdvanced > 0.03) {
                lastObservedPlaybackTime = curTime;
                lastProgressTimestamp = now;
                frozenDurationMs = 0;
                if (isBuffering && video.readyState >= 3) showBuffering(false);
                return;
            }

            // Frames have not advanced
            frozenDurationMs = now - lastProgressTimestamp;

            if (video.readyState < 3) {
                if (!video.paused) showBuffering(true);
                if (frozenDurationMs >= 15000 && (now - lastRecoveryExecutionTime >= 10000)) {
                    lastRecoveryExecutionTime = now;
                    triggerStreamEngineRetry();
                }
                return;
            }

            // If readyState >= 3 and playback clock stalled for >= 8s without advancing: gentle play call (no seek)
            if (frozenDurationMs >= 8000 && (now - lastRecoveryExecutionTime >= 8000) && !video.paused) {
                lastRecoveryExecutionTime = now;
                video.play().catch(() => { });
            }
        }, 800);

        // Video Event Listeners
        video.addEventListener('waiting', () => {
            if (!video.paused && !isUserPaused) showBuffering(true);
        });
        video.addEventListener('stalled', () => {
            if (!video.paused && !isUserPaused) showBuffering(true);
        });
        video.addEventListener('seeking', () => showBuffering(true));
        video.addEventListener('seeked', () => {
            if (video.readyState >= 3) showBuffering(false);
        });
        video.addEventListener('loadstart', () => {
            if (!video.paused && !isUserPaused) showBuffering(true);
        });

        video.addEventListener('playing', () => {
            showBuffering(false);
            playBtn.innerHTML = `<i class="fas fa-pause"></i>`;
            lastProgressTimestamp = Date.now();
            lastObservedPlaybackTime = video.currentTime;
            if (video.playbackRate < 0.25 && currentSpeed >= 0.25) {
                try { video.playbackRate = currentSpeed; } catch (e) { }
            }
            if (speedBadge) speedBadge.innerText = `${currentSpeed}x`;
        });

        ['canplay', 'canplaythrough'].forEach(ev => video.addEventListener(ev, () => {
            if (!video.paused) showBuffering(false);
        }));

        video.addEventListener('play', () => {
            playBtn.innerHTML = `<i class="fas fa-pause"></i>`;
            lastProgressTimestamp = Date.now();
            lastObservedPlaybackTime = video.currentTime;
            if (video.playbackRate < 0.25 && currentSpeed >= 0.25) {
                try { video.playbackRate = currentSpeed; } catch (e) { }
            }
            if (speedBadge) speedBadge.innerText = `${currentSpeed}x`;
        });

        video.addEventListener('pause', () => {
            savePlaybackProgress();
            playBtn.innerHTML = `<i class="fas fa-play"></i>`;
            showBuffering(false);
        });

        video.addEventListener('error', () => {
            showBuffering(true);
            triggerStreamEngineRetry();
        });

        document.addEventListener('visibilitychange', () => {
            if (!document.hidden && !isUserPaused && video && video.paused && !video.ended && video.readyState >= 3) {
                video.play().catch(() => { });
            }
        });

        /* ZERO-BUFFER TURBO PRELOAD ENGINE */
        const PRELOAD_GOALS = {
            max: { goal: 35, label: 'Max (35s Ahead)' },
            ultra: { goal: 25, label: 'Ultra (25s Ahead)' },
            normal: { goal: 18, label: 'Normal (18s Ahead)' },
            low: { goal: 10, label: 'Eco (10s Ahead)' }
        };

        function getActiveBufferedEnd() {
            if (!video?.buffered?.length) return 0;
            const cur = video.currentTime;
            for (let i = 0; i < video.buffered.length; i++) {
                if (cur >= video.buffered.start(i) - 1 && cur <= video.buffered.end(i) + 0.5) return video.buffered.end(i);
            }
            return video.buffered.end(video.buffered.length - 1);
        }

        function updateBufferBar() {
            if (video.duration) {
                const bEnd = getActiveBufferedEnd();
                if (bEnd > 0) bufferBar.style.width = `${Math.min(100, Math.max(0, (bEnd / video.duration) * 100))}%`;
            }
        }

        function applyTurboPreload(showNotice = false) {
            const cfg = PRELOAD_GOALS[preloadMode] || PRELOAD_GOALS.ultra;
            const goalSeconds = cfg.goal;
            try {
                const p = window.player || window.shakaPlayer;
                if (p?.configure) {
                    p.configure({
                        streaming: {
                            bufferingGoal: goalSeconds,
                            rebufferingGoal: 2.0,
                            bufferBehind: 30,
                            jumpLargeGaps: true,
                            smallGapLimit: 0.5
                        },
                        abr: {
                            enabled: (settingsQuality?.value === 'auto')
                        }
                    });
                }
            } catch (e) { }

            try {
                if (window.hls?.config) {
                    window.hls.config.maxBufferLength = goalSeconds;
                    window.hls.config.maxMaxBufferLength = goalSeconds * 1.5;
                }
            } catch (e) { }

            if (showNotice) {
                showOSD('<i class="fas fa-bolt" style="color:#f59e0b"></i>', `Preload: ${cfg.label}`);
                showGlassToast(`Preload set to ${cfg.label}`, 'success', 'fas fa-bolt');
            }
        }

        function attachEngineHooks() {
            try {
                const p = window.player || window.shakaPlayer;
                if (p && typeof p.addEventListener === 'function' && !p._lqAttached) {
                    p._lqAttached = true;
                    applyTurboPreload(false);
                    p.addEventListener('error', (event) => {
                        console.warn('[LiquidPlayer] Shaka error:', event.detail);
                        if (typeof p.retryStreaming === 'function') p.retryStreaming();
                    });
                    p.addEventListener('buffering', (event) => {
                        if (!isUserPaused && !video.paused) showBuffering(event.buffering);
                    });
                }
            } catch (e) { }
        }
        attachEngineHooks();
        setTimeout(attachEngineHooks, 1000);
        setTimeout(attachEngineHooks, 3000);

        window.addEventListener('beforeunload', savePlaybackProgress);

        /* RESUME PLAYBACK ENGINE */
        function getResumeKey() {
            const p = new URLSearchParams(window.location.search);
            const vid = p.get('videoId') || p.get('schId') || p.get('lectureId');
            if (vid) return 'lq_pos_' + vid;
            const title = p.get('title') || document.title || '';
            return 'lq_pos_' + (title ? encodeURIComponent(title.replace(/[^a-zA-Z0-9]/g, '_').slice(0, 40)) : encodeURIComponent(window.location.pathname));
        }

        function savePlaybackProgress() {
            if (!video?.duration || isNaN(video.currentTime)) return;
            const now = Date.now();
            if (now - lastSaveTime < 2000) return;
            lastSaveTime = now;
            if (video.currentTime > 5 && video.currentTime < (video.duration - 15)) {
                try { localStorage.setItem(getResumeKey(), JSON.stringify({ time: video.currentTime, duration: video.duration, updatedAt: now })); } catch (e) { }
            } else if (video.currentTime >= (video.duration - 15)) {
                try { localStorage.removeItem(getResumeKey()); } catch (e) { }
            }
        }

        function attemptResumePlayback() {
            if (hasResumedPlayback || !video?.duration || isNaN(video.duration) || video.duration <= 0) return;
            try {
                const saved = localStorage.getItem(getResumeKey());
                if (saved) {
                    const data = JSON.parse(saved);
                    const resumeTime = parseFloat(data.time);
                    if (resumeTime > 5 && resumeTime < (video.duration - 15)) {
                        hasResumedPlayback = true;
                        video.currentTime = resumeTime;
                        showOSD('<i class="fas fa-history" style="color:var(--lq-accent, #38bdf8)"></i>', `Resumed at ${formatTime(resumeTime)}`);
                        showGlassToast(`Resumed playback at ${formatTime(resumeTime)}`, 'info', 'fas fa-history');
                    }
                }
            } catch (e) { }
        }


        let lastCloudWatchLogTime = 0;

        video.addEventListener('timeupdate', () => {
            if (isBuffering && Math.abs(video.currentTime - lastObservedPlaybackTime) > 0.05) showBuffering(false);
            if (video.duration) {
                savePlaybackProgress();
                updateBufferBar();
                const progressPercent = (video.currentTime / video.duration) * 100;
                seek.value = progressPercent;
                if (fillBar) fillBar.style.width = `${progressPercent}%`;
                curTimeEl.innerText = formatTime(video.currentTime);
                durTimeEl.innerText = formatTime(video.duration);
                updateActiveSlideSeparator(video.currentTime);

                const nowTime = Date.now();
                if (nowTime - lastStatsHubUpdateTime >= 1000) {
                    lastStatsHubUpdateTime = nowTime;
                    updateChapterStatsHub();
                }

                // Send progress telemetry to Android Native Bridge every 10 seconds
                if (nowTime - lastCloudWatchLogTime >= 10000) {
                    lastCloudWatchLogTime = nowTime;
                    try {
                        if (window.AndroidPlayerBridge && window.AndroidPlayerBridge.logWatchProgress) {
                            const curLec = (currentLectureIndex >= 0 && currentLectureIndex < chapterLectures.length) ? chapterLectures[currentLectureIndex] : null;
                            const urlParams = new URLSearchParams(window.location.search);
                            const curTitle = curLec?.title || urlParams.get('title') || document.title.replace(/ - StudyParcham/gi, '').trim() || 'Lecture';
                            const curSubject = selectedSubjectId || urlParams.get('subjectName') || urlParams.get('subjectId') || '';
                            const curChapter = selectedTopicId || urlParams.get('chapterName') || urlParams.get('topicId') || '';

                            window.AndroidPlayerBridge.logWatchProgress(
                                curTitle,
                                curSubject,
                                curChapter,
                                video.currentTime,
                                video.duration,
                                progressPercent
                            );
                            if (window.AndroidPlayerBridge.updateCurrentActivity) {
                                window.AndroidPlayerBridge.updateCurrentActivity(window.location.href, curTitle, curTitle);
                            }
                        }
                    } catch (e) {
                        console.warn("[Bridge] logWatchProgress error:", e);
                    }
                }

                if (!hasDismissedNextPrompt && currentLectureIndex < chapterLectures.length - 1 && autoNextMode !== 'off') {
                    const timeLeft = video.duration - video.currentTime;
                    let shouldTrigger = false;
                    if (autoNextMode === 'last-slide') {
                        const isLastSlide = loadedSlides.length > 0 && loadedSlides[loadedSlides.length - 1].seconds > 0 && video.currentTime >= loadedSlides[loadedSlides.length - 1].seconds;
                        shouldTrigger = isLastSlide || (loadedSlides.length === 0 && timeLeft <= 35 && timeLeft > 0);
                    } else if (autoNextMode === '5min') {
                        shouldTrigger = (timeLeft <= 300 && timeLeft > 0 && video.currentTime > 15);
                    } else if (autoNextMode === '2min') {
                        shouldTrigger = (timeLeft <= 120 && timeLeft > 0 && video.currentTime > 15);
                    } else if (autoNextMode === '35s') {
                        shouldTrigger = (timeLeft <= 35 && timeLeft > 0);
                    }
                    if (shouldTrigger) triggerNextLecturePrompt();
                }
        });

        video.addEventListener('pause', () => {
            try {
                if (window.AndroidPlayerBridge && window.AndroidPlayerBridge.updateCurrentActivity) {
                    const cleanTitle = document.title.replace(/ - StudyParcham/gi, '').trim() || 'PW Dhyan';
                    window.AndroidPlayerBridge.updateCurrentActivity(window.location.href, cleanTitle, "");
                }
            } catch (e) {}
        });

        video.addEventListener('ended', () => {
            try {
                if (window.AndroidPlayerBridge && window.AndroidPlayerBridge.updateCurrentActivity) {
                    const cleanTitle = document.title.replace(/ - StudyParcham/gi, '').trim() || 'PW Dhyan';
                    window.AndroidPlayerBridge.updateCurrentActivity(window.location.href, cleanTitle, "");
                }
            } catch (e) {}
        });

        video.addEventListener('progress', updateBufferBar);

        seek.addEventListener('input', (e) => {
            if (video.duration) {
                const targetSec = (e.target.value / 100) * video.duration;
                curTimeEl.innerText = formatTime(targetSec);
                if (fillBar) fillBar.style.width = `${e.target.value}%`;
                video.currentTime = targetSec;
            }
        });
        ['mousedown', 'touchstart'].forEach(ev => seek.addEventListener(ev, () => { isScrubbing = true; hub.classList.add('user-active'); }));
        ['mouseup', 'touchend'].forEach(ev => window.addEventListener(ev, () => {
            if (isScrubbing) { isScrubbing = false; scheduleIdleHide(2000); }
        }));

        // Settings Master Menu Controls
        function toggleSettingsMenu(force) {
            const willShow = (typeof force === 'boolean') ? force : !settingsMenu.classList.contains('show');
            if (willShow) {
                closeAllDrawers();
                settingsMenu.classList.add('show');
                hub.classList.add('keep-active');
            } else {
                settingsMenu.classList.remove('show');
                if (!tray.classList.contains('active') && !shortcutsModal.classList.contains('active')) {
                    hub.classList.remove('keep-active');
                    scheduleIdleHide(1500);
                }
            }
        }
        settingsBtn.onclick = (e) => { e.stopPropagation(); toggleSettingsMenu(); };

        // Extended Speed Engine
        function applySpeed(rate, save = true, showHud = true) {
            const clamped = Math.max(0.25, Math.min(6.0, Math.round((parseFloat(rate) || 1.0) * 100) / 100));
            currentSpeed = clamped;
            try { if (video) video.playbackRate = clamped; } catch (err) { }
            if (save) { try { localStorage.setItem('lq_speed', clamped.toString()); } catch (e) { } }

            if (settingsSpeed) {
                let optionExists = false;
                for (let i = 0; i < settingsSpeed.options.length; i++) {
                    if (Math.abs(parseFloat(settingsSpeed.options[i].value) - clamped) < 0.01) {
                        settingsSpeed.selectedIndex = i;
                        optionExists = true;
                        break;
                    }
                }
                if (!optionExists) {
                    const customOpt = document.createElement('option');
                    customOpt.value = clamped.toString();
                    customOpt.innerText = `${clamped}x (Custom)`;
                    settingsSpeed.appendChild(customOpt);
                    settingsSpeed.value = clamped.toString();
                }
            }

            if (quickSpeedContainer) {
                quickSpeedContainer.querySelectorAll('.mobile-chip').forEach(chip => {
                    const spd = parseFloat(chip.dataset.speed);
                    chip.classList.toggle('active', Math.abs(spd - clamped) < 0.01);
                });
            }

            if (speedBadge) speedBadge.innerText = `${clamped}x`;
            if (customSpeedInput) customSpeedInput.value = clamped;
            updatePinnedChipsUI();
            if (showHud) showOSD('<i class="fas fa-tachometer-alt" style="color:var(--lq-accent, #38bdf8)"></i>', `${clamped}x Speed`);
        }

        function renderPinnedSpeeds() {
            if (!pinnedSpeedsContainer) return;
            pinnedSpeedsContainer.innerHTML = '';
            pinnedSpeeds.forEach(spd => {
                const chip = document.createElement('div');
                chip.className = `pinned-speed-chip ${Math.abs(spd - currentSpeed) < 0.01 ? 'active' : ''}`;
                chip.dataset.speed = spd;
                chip.title = `Click to set ${spd}x speed`;
                chip.innerHTML = `
                    <span class="pinned-speed-val">${spd}x</span>
                    <span class="pinned-speed-remove" title="Unpin ${spd}x">&times;</span>
                `;
                chip.querySelector('.pinned-speed-val').onclick = (e) => { e.stopPropagation(); applySpeed(spd, true, true); };
                chip.querySelector('.pinned-speed-remove').onclick = (e) => { e.stopPropagation(); unpinSpeed(spd); };
                pinnedSpeedsContainer.appendChild(chip);
            });
            updatePinButtonState();
        }

        function updatePinnedChipsUI() {
            if (!pinnedSpeedsContainer) return;
            pinnedSpeedsContainer.querySelectorAll('.pinned-speed-chip').forEach(chip => {
                const spd = parseFloat(chip.dataset.speed);
                chip.classList.toggle('active', Math.abs(spd - currentSpeed) < 0.01);
            });
            updatePinButtonState();
        }

        function updatePinButtonState() {
            if (!customSpeedPinBtn) return;
            const isPinned = pinnedSpeeds.some(s => Math.abs(s - currentSpeed) < 0.01);
            customSpeedPinBtn.innerHTML = isPinned ? '<i class="fas fa-thumbtack"></i> Pinned' : '<i class="fas fa-thumbtack"></i> Pin';
            customSpeedPinBtn.classList.toggle('active', isPinned);
        }

        function pinSpeed(rate) {
            const num = Math.max(0.25, Math.min(6.0, Math.round(rate * 100) / 100));
            if (pinnedSpeeds.some(s => Math.abs(s - num) < 0.01)) {
                showGlassToast(`${num}x is already pinned`, 'info', 'fas fa-thumbtack');
                return;
            }
            if (pinnedSpeeds.length >= 3) {
                showGlassToast("Max 3 pinned speeds reached. Unpin one first.", "error", "fas fa-exclamation-triangle");
                return;
            }
            pinnedSpeeds.push(num);
            pinnedSpeeds.sort((a, b) => a - b);
            try { localStorage.setItem('lq_pinned_speeds', JSON.stringify(pinnedSpeeds)); } catch (e) { }
            renderPinnedSpeeds();
            showGlassToast(`Pinned ${num}x speed`, 'success', 'fas fa-thumbtack');
        }

        function unpinSpeed(rate) {
            pinnedSpeeds = pinnedSpeeds.filter(s => Math.abs(s - rate) >= 0.01);
            try { localStorage.setItem('lq_pinned_speeds', JSON.stringify(pinnedSpeeds)); } catch (e) { }
            renderPinnedSpeeds();
            showGlassToast(`Unpinned ${rate}x speed`, 'info', 'fas fa-thumbtack');
        }

        renderPinnedSpeeds();
        applySpeed(currentSpeed, false, false);

        if (quickSpeedContainer) {
            quickSpeedContainer.querySelectorAll('.mobile-chip').forEach(chip => {
                chip.onclick = (e) => {
                    e.stopPropagation();
                    const spd = parseFloat(chip.dataset.speed);
                    if (!isNaN(spd)) applySpeed(spd, true, true);
                };
            });
        }

        if (speedBadge) speedBadge.onclick = (e) => { e.stopPropagation(); toggleSettingsMenu(); };
        if (settingsSpeed) settingsSpeed.onchange = (e) => applySpeed(parseFloat(e.target.value), true, true);

        if (customSpeedSetBtn && customSpeedInput) {
            customSpeedSetBtn.onclick = (e) => {
                e.stopPropagation();
                const val = parseFloat(customSpeedInput.value);
                if (!isNaN(val) && val >= 0.25 && val <= 6.0) {
                    applySpeed(val, true, true);
                    showGlassToast(`Speed set to ${currentSpeed}x`, 'success', 'fas fa-tachometer-alt');
                } else {
                    showGlassToast("Enter a valid speed between 0.25x and 6x", "error", "fas fa-exclamation-circle");
                }
            };
            customSpeedInput.addEventListener('keydown', (e) => {
                if (e.key === 'Enter') { e.preventDefault(); e.stopPropagation(); customSpeedSetBtn.click(); }
            });
        }

        if (customSpeedPinBtn) {
            customSpeedPinBtn.onclick = (e) => {
                e.stopPropagation();
                const inputVal = customSpeedInput ? parseFloat(customSpeedInput.value) : NaN;
                const targetSpeed = (!isNaN(inputVal) && inputVal >= 0.25 && inputVal <= 6.0) ? inputVal : currentSpeed;
                if (pinnedSpeeds.some(s => Math.abs(s - targetSpeed) < 0.01)) unpinSpeed(targetSpeed);
                else pinSpeed(targetSpeed);
            };
        }

        if (settingsMenu) settingsMenu.onclick = (e) => e.stopPropagation();

        // Prevent 0x speed badge bug on stream ratechange
        video.addEventListener('ratechange', () => {
            const r = video.playbackRate;
            if (r >= 0.25) {
                if (speedBadge) speedBadge.innerText = `${r}x`;
                if (settingsSpeed) {
                    for (let i = 0; i < settingsSpeed.options.length; i++) {
                        if (Math.abs(parseFloat(settingsSpeed.options[i].value) - r) < 0.01) {
                            settingsSpeed.selectedIndex = i;
                            break;
                        }
                    }
                }
                if (quickSpeedContainer) {
                    quickSpeedContainer.querySelectorAll('.mobile-chip').forEach(chip => {
                        const spd = parseFloat(chip.dataset.speed);
                        chip.classList.toggle('active', Math.abs(spd - r) < 0.01);
                    });
                }
            }
        });

        // Quality inside Settings & Mobile Drawer
        function applyQuality(res) {
            const p = window.player || window.shakaPlayer;
            if (p?.configure) {
                if (res === 'auto') {
                    p.configure({ abr: { enabled: true } });
                } else if (p.getVariantTracks) {
                    p.configure({ abr: { enabled: false } });
                    const tracks = p.getVariantTracks();
                    const match = tracks.find(t => t.height === parseInt(res, 10));
                    if (match) p.selectVariantTrack(match, true);
                }
            }
            if (settingsQuality) settingsQuality.value = res;
            if (quickQualityContainer) {
                quickQualityContainer.querySelectorAll('.mobile-chip').forEach(chip => {
                    chip.classList.toggle('active', chip.dataset.quality === res);
                });
            }
            showOSD('<i class="fas fa-film" style="color:var(--lq-accent, #38bdf8)"></i>', res === 'auto' ? 'Auto Quality' : `${res}p`);
        }

        if (settingsQuality) {
            settingsQuality.onchange = (e) => applyQuality(e.target.value);
        }

        if (quickQualityContainer) {
            quickQualityContainer.querySelectorAll('.mobile-chip').forEach(chip => {
                chip.onclick = (e) => {
                    e.stopPropagation();
                    const q = chip.dataset.quality;
                    if (q) applyQuality(q);
                };
            });
        }

        // Volume inside Settings
        function updateVolumeDisplay(vol, isMuted) {
            if (isMuted || vol === 0) {
                volLabel.innerHTML = `<i class="fas fa-volume-mute me-1" style="color:#ef4444"></i> Mute`;
                volPercent.innerText = '0%';
                settingsVol.value = 0;
            } else {
                volLabel.innerHTML = `<i class="fas fa-volume-up me-1" style="color:var(--lq-accent, #38bdf8)"></i> Vol`;
                volPercent.innerText = `${Math.round(vol * 100)}%`;
                settingsVol.value = vol;
            }
        }

        volLabel.onclick = () => {
            if (video.muted || video.volume === 0) {
                video.muted = false;
                video.volume = lastVolume || 0.8;
                showOSD('<i class="fas fa-volume-up" style="color:var(--lq-accent, #38bdf8)"></i>', `${Math.round(video.volume * 100)}%`);
            } else {
                lastVolume = video.volume;
                video.muted = true;
                showOSD('<i class="fas fa-volume-mute" style="color:#ef4444"></i>', 'Muted');
            }
            updateVolumeDisplay(video.volume, video.muted);
        };

        settingsVol.addEventListener('input', (e) => {
            const val = parseFloat(e.target.value);
            video.volume = val;
            video.muted = (val === 0);
            if (val > 0) lastVolume = val;
            updateVolumeDisplay(val, video.muted);
            showOSD('<i class="fas fa-volume-up" style="color:var(--lq-accent, #38bdf8)"></i>', `${Math.round(val * 100)}%`);
        });

        // Vocal Clarifier inside Settings
        settingsBoostBtn.onclick = (e) => {
            e.stopPropagation();
            const isActive = toggleVocalClarifier(video);
            settingsBoostBtn.innerText = isActive ? 'ON' : 'OFF';
            settingsBoostBtn.classList.toggle('active', isActive);
        };

        // Auto Next Mode inside Settings
        const autoNextLabels = {
            'last-slide': 'Last Slide', '5min': 'Last 5 Min', '2min': 'Last 2 Min',
            '35s': 'Last 35s', 'off': 'Off (Disabled)'
        };

        function setAutoNextMode(mode, showNotification = true) {
            autoNextMode = mode;
            try { localStorage.setItem('lq_autonext_mode', autoNextMode); } catch (e) { }
            if (settingsAutoNext) settingsAutoNext.value = autoNextMode;
            if (showNotification) {
                showOSD('<i class="fas fa-forward" style="color:var(--lq-accent, #38bdf8)"></i>', `Auto Next: ${autoNextLabels[autoNextMode] || autoNextMode}`);
                showGlassToast(`Auto Next: ${autoNextLabels[autoNextMode] || autoNextMode}`, 'info', 'fas fa-forward');
            }
        }

        if (settingsAutoNext) {
            settingsAutoNext.value = autoNextMode;
            settingsAutoNext.onchange = (e) => setAutoNextMode(e.target.value);
        }

        // Turbo Preload inside Settings
        if (settingsPreload) {
            settingsPreload.value = preloadMode;
            settingsPreload.onchange = (e) => {
                preloadMode = e.target.value;
                try { localStorage.setItem('lq_preload_mode', preloadMode); } catch (e) { }
                applyTurboPreload(true);
            };
        }

        // Picture-in-Picture inside Settings
        settingsPipBtn.onclick = async (e) => {
            e.stopPropagation();
            try {
                if (document.pictureInPictureElement) {
                    await document.exitPictureInPicture();
                    settingsPipBtn.innerText = 'Pop Out';
                    settingsPipBtn.classList.remove('active');
                    showOSD('<i class="fas fa-clone"></i>', 'PiP Closed');
                } else if (document.pictureInPictureEnabled && video !== document.pictureInPictureElement) {
                    await video.requestPictureInPicture();
                    settingsPipBtn.innerText = 'Close';
                    settingsPipBtn.classList.add('active');
                    showOSD('<i class="fas fa-clone" style="color:var(--lq-accent, #38bdf8)"></i>', 'PiP Active');
                } else {
                    showGlassToast("Picture-in-Picture not supported by your browser", "error");
                }
            } catch (err) {
                console.warn(err);
                showGlassToast("Could not activate Picture-in-Picture", "error");
            }
        };

        // Shortcuts Dialog from Settings
        settingsShortcutsItem.onclick = (e) => {
            e.stopPropagation();
            settingsMenu.classList.remove('show');
            toggleShortcutsModal();
        };

        document.addEventListener('click', (e) => {
            if (!settingsBtn.contains(e.target) && !settingsMenu.contains(e.target) && (!speedBadge || !speedBadge.contains(e.target))) {
                toggleSettingsMenu(false);
            }
            if (!topHud.contains(e.target)) toggleTopPlaylistDropdown(false);
        });

        hub.querySelector('#c-fullscreen').onclick = () => {
            if (!document.fullscreenElement) {
                if (document.documentElement.requestFullscreen) document.documentElement.requestFullscreen();
                else if (video.webkitEnterFullscreen) video.webkitEnterFullscreen();
                showOSD('<i class="fas fa-expand" style="color:var(--lq-accent, #38bdf8)"></i>', 'Fullscreen');
            } else {
                if (document.exitFullscreen) document.exitFullscreen();
                showOSD('<i class="fas fa-compress"></i>', 'Exit Fullscreen');
            }
        };

        function closeAllDrawers() {
            tray.classList.remove('active');
            shortcutsModal.classList.remove('active');
            toggleTopPlaylistDropdown(false);
            if (settingsMenu) settingsMenu.classList.remove('show');
            hub.classList.remove('keep-active');
            scheduleIdleHide(1500);
        }

        function toggleSlidesTray() {
            const isActive = tray.classList.contains('active');
            closeAllDrawers();
            if (!isActive) {
                tray.classList.add('active');
                hub.classList.add('keep-active');
                loadAndRenderSlides();
            }
        }
        slidesBtn.onclick = toggleSlidesTray;
        closeTray.onclick = closeAllDrawers;

        function toggleShortcutsModal() {
            const isActive = shortcutsModal.classList.contains('active');
            closeAllDrawers();
            if (!isActive) {
                shortcutsModal.classList.add('active');
                hub.classList.add('keep-active');
            }
        }
        closeShortcuts.onclick = closeAllDrawers;

        function parseTimeToSeconds(timeStr) {
            if (!timeStr) return 0;
            const match = timeStr.match(/(\d{1,2}):(\d{2}):(\d{2})/);
            if (match) return parseInt(match[1]) * 3600 + parseInt(match[2]) * 60 + parseInt(match[3]);
            const matchMin = timeStr.match(/(\d{1,2}):(\d{2})/);
            if (matchMin) return parseInt(matchMin[1]) * 60 + parseInt(matchMin[2]);
            return 0;
        }

        /* DUAL PREVIEW ENGINE */
        trackContainer.addEventListener('mousemove', (e) => {
            if (!video.duration) return;
            const rect = trackContainer.getBoundingClientRect();
            const mouseX = Math.max(0, Math.min(e.clientX - rect.left, rect.width));
            const hoverPercent = mouseX / rect.width;
            const targetSeconds = hoverPercent * video.duration;

            const previewWidth = 140;
            const clampedX = Math.max(previewWidth / 2, Math.min(mouseX, rect.width - previewWidth / 2));
            hoverPreview.style.left = `${clampedX}px`;
            hoverPreview.style.display = 'flex';

            if (loadedSlides.length > 0) {
                let activeSlide = loadedSlides[0];
                for (let i = 0; i < loadedSlides.length; i++) {
                    if (targetSeconds >= loadedSlides[i].seconds) activeSlide = loadedSlides[i];
                    else break;
                }
                if (activeSlide.image) {
                    previewImg.src = activeSlide.image;
                    previewImg.style.display = 'block';
                } else {
                    previewImg.style.display = 'none';
                }
                previewSlideText.innerText = `Slide ${activeSlide.index}`;
            } else {
                previewImg.style.display = 'none';
                previewSlideText.innerText = 'Lecture';
            }
            previewTimeText.innerText = formatTime(targetSeconds);
        });
        trackContainer.addEventListener('mouseleave', () => hoverPreview.style.display = 'none');

        /* 9. THICK SLIDE SEPARATORS & TIMELINE ENGINE */
        function renderSlideSeparators() {
            const sepContainer = document.getElementById('timeline-scrub-separators');
            if (!sepContainer || !video.duration || !isFinite(video.duration) || loadedSlides.length === 0) return;
            sepContainer.innerHTML = "";
            loadedSlides.forEach((slide) => {
                if (slide.seconds > 0) {
                    const percent = (slide.seconds / video.duration) * 100;
                    if (percent > 0 && percent <= 100) {
                        const sep = document.createElement('div');
                        sep.className = 'slide-segment-divider';
                        sep.style.left = `${percent}%`;
                        sep.title = `Slide ${slide.index} (${slide.timeText})`;
                        sep.dataset.seconds = slide.seconds;
                        sep.dataset.index = slide.index;
                        sep.addEventListener('mouseenter', () => {
                            const rect = trackContainer.getBoundingClientRect();
                            const posX = (percent / 100) * rect.width;
                            hoverPreview.style.left = `${Math.max(70, Math.min(posX, rect.width - 70))}px`;
                            hoverPreview.style.display = 'flex';
                            previewImg.src = slide.image || '';
                            previewImg.style.display = slide.image ? 'block' : 'none';
                            previewSlideText.innerText = `Slide ${slide.index}`;
                            previewTimeText.innerText = slide.timeText;
                        });
                        sep.addEventListener('click', (e) => {
                            e.stopPropagation();
                            isUserPaused = false;
                            video.currentTime = slide.seconds;
                            if (video.paused) video.play();
                            showOSD('<i class="fas fa-images" style="color:#fbbf24"></i>', `Slide ${slide.index}`);
                        });
                        sepContainer.appendChild(sep);
                    }
                }
            });
            updateActiveSlideSeparator(video.currentTime);
        }

        function updateActiveSlideSeparator(time) {
            const sepContainer = document.getElementById('timeline-scrub-separators');
            if (!sepContainer) return;
            const seps = sepContainer.querySelectorAll('.slide-segment-divider');
            if (seps.length === 0) return;

            let activeIdx = -1;
            for (let i = 0; i < loadedSlides.length; i++) {
                if (time >= loadedSlides[i].seconds - 0.5) activeIdx = i;
            }
            seps.forEach((sep) => {
                const sIdx = parseInt(sep.dataset.index) - 1;
                sep.classList.toggle('active-slide', sIdx === activeIdx);
            });
        }

        async function loadAndRenderSlides() {
            const targetTrayGrid = document.getElementById('tray-grid');
            if (!targetTrayGrid) return;

            let nativeCards = document.querySelectorAll('#timeline-grid .slide-card, .slide-card');
            if (nativeCards.length === 0) {
                const nativeBtn = document.getElementById('btn-timeline');
                if (nativeBtn) nativeBtn.click();
                await new Promise(r => setTimeout(r, 650));
                const nativeOverlay = document.getElementById('sheet-overlay');
                if (nativeOverlay) nativeOverlay.classList.remove('active');
                nativeCards = document.querySelectorAll('#timeline-grid .slide-card, .slide-card');
            }

            loadedSlides = [];
            nativeCards.forEach((c) => {
                const img = c.querySelector('img')?.src || '';
                const rawTimeText = c.querySelector('.slide-time')?.innerText || '';
                const sec = parseTimeToSeconds(rawTimeText);
                loadedSlides.push({ image: img, timeText: formatTime(sec), seconds: sec, nativeEl: c });
            });

            loadedSlides.sort((a, b) => a.seconds - b.seconds);
            loadedSlides.forEach((s, i) => s.index = i + 1);

            if (loadedSlides.length > 0) {
                targetTrayGrid.innerHTML = "";
                renderSlideSeparators();

                loadedSlides.forEach((slide, idx) => {
                    const card = document.createElement('div');
                    card.className = 'tray-card';
                    card.innerHTML = `
                        <img src="${slide.image}">
                        <div class="tray-card-footer">
                            <span class="slide-badge-num">Slide ${slide.index}</span>
                            <span class="slide-badge-time">${slide.timeText}</span>
                        </div>
                    `;
                    card.onclick = () => {
                        isUserPaused = false;
                        video.currentTime = slide.seconds;
                        if (video.paused) video.play();
                        closeAllDrawers();
                        showOSD('<i class="fas fa-images" style="color:#fbbf24"></i>', `Slide ${slide.index}`);
                    };
                    targetTrayGrid.appendChild(card);

                    if (idx < loadedSlides.length - 1) {
                        const arrow = document.createElement('div');
                        arrow.className = 'slide-transition-separator';
                        arrow.innerHTML = `<i class="fas fa-chevron-right"></i>`;
                        targetTrayGrid.appendChild(arrow);
                    }
                });
            } else {
                targetTrayGrid.innerHTML = `<div style="color:#94a3b8; font-size:0.85rem; padding: 20px;">No slides found.</div>`;
            }
        }

        /* 10. CHAPTER PLAYLIST ENGINE */
        async function initChapterPlaylist(force) {
            const urlParams = new URLSearchParams(window.location.search);
            const batchId = urlParams.get('batchId') || localStorage.getItem('batchId') || sessionStorage.getItem('batchId');
            const subjectId = urlParams.get('subjectId') || localStorage.getItem('subjectId') || sessionStorage.getItem('subjectId');
            const currentVideoId = urlParams.get('videoId') || urlParams.get('schId') || urlParams.get('lectureId');
            const urlTitle = urlParams.get('title') || document.title || '';

            if (subjectId) selectedSubjectId = subjectId;

            if (!batchId || !subjectId) {
                if (urlTitle || currentVideoId) {
                    chapterLectures = [{
                        id: currentVideoId || 'cur',
                        schId: currentVideoId || 'cur',
                        title: urlTitle.replace(/ - StudyParcham/gi, '').trim() || 'Current Lecture',
                        duration: formatTime(video.duration) || 'Lecture',
                        url: '',
                        date: Date.now(),
                        index: 1
                    }];
                    currentLectureIndex = 0;
                    updateQueueBadge();
                    updatePrevNextButtons();
                    renderTopPlaylist(topPlaylistSearch?.value || '');
                }
                return;
            }

            try {
                let topicId = urlParams.get('tagId') || urlParams.get('topicId') || '';
                if (!topicId) {
                    const topicSlug = urlParams.get('topicSlug');
                    const topicsRes = await window.ParchamCore('pw_sub_topics', { batchId, subjectId, page: 1, tagType: 'UNITS', limit: 50 });
                    const topics = (topicsRes?.data?.data) || (topicsRes?.data) || [];
                    if (topicSlug && topics.length > 0) {
                        const matchedTopic = topics.find(t => t.name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '') === topicSlug);
                        if (matchedTopic) topicId = matchedTopic._id;
                    }
                }
                if (topicId) selectedTopicId = topicId;

                let res = await window.ParchamCore('pw_v2_list', { batchId, subjectId, tagId: topicId, contentType: 'videos', page: 1 });
                let rawList = res?.data?.data || res?.data || [];

                if (!rawList || rawList.length === 0) {
                    let resV3 = await window.ParchamCore('pw_sch_cntnt', { batchId, subjectId, contentType: 'LECTURE', tagId: topicId, skip: 0, limit: 50 });
                    rawList = (resV3?.data || []).map(w => w.data || w);
                }

                chapterLectures = rawList.map((item) => {
                    let d = item?.data || item || {};
                    return {
                        id: d._id || d.videoDetails?._id || item?._id || String(Math.random()),
                        schId: item?._id || d._id || '',
                        title: d.topic || d.name || 'Lecture',
                        duration: d.videoDetails?.duration || d.duration || '1h',
                        url: d.videoDetails?.videoUrl || d.url || '',
                        date: new Date(d.date || d.startTime || 0).getTime()
                    };
                });

                chapterLectures.sort((a, b) => a.date - b.date);
                chapterLectures.forEach((lec, idx) => lec.index = idx + 1);

                currentLectureIndex = chapterLectures.findIndex(l =>
                    l.id === currentVideoId ||
                    l.schId === currentVideoId ||
                    (currentVideoId && (String(l.id).includes(currentVideoId) || String(l.schId).includes(currentVideoId)))
                );

                if (currentLectureIndex === -1 && (urlTitle || initialTitle)) {
                    const searchTitle = (urlTitle || initialTitle).toLowerCase();
                    currentLectureIndex = chapterLectures.findIndex(l =>
                        l.title && (searchTitle.includes(l.title.toLowerCase()) || l.title.toLowerCase().includes(searchTitle))
                    );
                    if (currentLectureIndex === -1) {
                        const numMatch = searchTitle.match(/(?:lecture|lec|ep|episode|class)?\s*(\d+)/i);
                        if (numMatch && numMatch[1]) {
                            const targetNum = parseInt(numMatch[1], 10);
                            currentLectureIndex = chapterLectures.findIndex(l => {
                                const lMatch = l.title?.match(/(?:lecture|lec|ep|episode|class)?\s*(\d+)/i);
                                return lMatch && parseInt(lMatch[1], 10) === targetNum;
                            });
                        }
                    }
                }

                if (currentLectureIndex === -1 && chapterLectures.length > 0) currentLectureIndex = 0;

                updateQueueBadge();
                updatePrevNextButtons();
                renderTopPlaylist(topPlaylistSearch?.value || '');
            } catch (e) {
                console.warn("Playlist warning:", e);
                if (queueBadge) queueBadge.innerHTML = '<i class="fas fa-list-ul"></i>';
                if (urlTitle || currentVideoId) {
                    chapterLectures = [{
                        id: currentVideoId || 'cur',
                        schId: currentVideoId || 'cur',
                        title: urlTitle.replace(/ - StudyParcham/gi, '').trim() || 'Current Lecture',
                        duration: formatTime(video.duration) || 'Lecture',
                        url: '',
                        date: Date.now(),
                        index: 1
                    }];
                    currentLectureIndex = 0;
                    updateQueueBadge();
                    updatePrevNextButtons();
                    renderTopPlaylist(topPlaylistSearch?.value || '');
                }
            }
        }

        function parseDurationToSeconds(dur) {
            if (!dur) return 0;
            if (typeof dur === 'number') return (isFinite(dur) && dur > 0) ? dur : 0;
            if (typeof dur === 'object') {
                if (dur.seconds) return parseFloat(dur.seconds) || 0;
                if (dur.duration) return parseDurationToSeconds(dur.duration);
            }
            const s = String(dur).trim();
            if (!s || s.toLowerCase() === 'lecture') return 0;
            if (s.includes(':')) {
                const parts = s.split(':').map(p => parseFloat(p));
                if (parts.length === 3 && parts.every(p => !isNaN(p))) return parts[0] * 3600 + parts[1] * 60 + parts[2];
                if (parts.length === 2 && parts.every(p => !isNaN(p))) return parts[0] * 60 + parts[1];
            }
            let total = 0;
            const hMatch = s.match(/(\d+)\s*(?:h|hr|hours?)/i);
            const mMatch = s.match(/(\d+)\s*(?:m|min|minutes?)/i);
            const sMatch = s.match(/(\d+)\s*(?:s|sec|seconds?)/i);
            if (hMatch) total += parseInt(hMatch[1], 10) * 3600;
            if (mMatch) total += parseInt(mMatch[1], 10) * 60;
            if (sMatch) total += parseInt(sMatch[1], 10);
            if (total > 0) return total;
            const num = parseFloat(s);
            return (!isNaN(num) && isFinite(num) && num > 0) ? num : 0;
        }

        function formatDurationHuman(seconds) {
            if (!seconds || seconds <= 0 || isNaN(seconds)) return "0m";
            const sec = Math.round(seconds);
            const h = Math.floor(sec / 3600), m = Math.floor((sec % 3600) / 60), remS = sec % 60;
            if (h > 0) return `${h}h ${m < 10 ? '0' : ''}${m}m`;
            if (m > 0) return `${m}m`;
            return `${remS}s`;
        }

        function calculateChapterStats() {
            if (!chapterLectures || chapterLectures.length === 0) {
                return { totalSec: 0, coveredSec: 0, leftSec: 0, percent: 0, completedCount: 0, totalCount: 0 };
            }
            let totalSec = 0, coveredSec = 0, completedCount = 0;
            const totalCount = chapterLectures.length;

            chapterLectures.forEach((lec, idx) => {
                let lecDuration = parseDurationToSeconds(lec.duration);
                if (idx === currentLectureIndex && video?.duration && isFinite(video.duration) && video.duration > 0) {
                    lecDuration = video.duration;
                }
                if (lecDuration <= 0) lecDuration = 3600;
                totalSec += lecDuration;

                if (idx < currentLectureIndex) {
                    coveredSec += lecDuration;
                    completedCount++;
                } else if (idx === currentLectureIndex) {
                    const cur = (video && !isNaN(video.currentTime)) ? video.currentTime : 0;
                    const watched = Math.min(cur, lecDuration);
                    coveredSec += watched;
                    if (watched >= lecDuration * 0.9 || (video && video.ended)) completedCount++;
                } else {
                    let savedProgress = 0;
                    try {
                        const key1 = 'lq_pos_' + lec.id, key2 = 'lq_pos_' + lec.schId;
                        const saved = localStorage.getItem(key1) || localStorage.getItem(key2);
                        if (saved) {
                            const data = JSON.parse(saved);
                            if (data?.time) savedProgress = parseFloat(data.time) || 0;
                        }
                    } catch (e) { }
                    if (savedProgress > 0) {
                        const watched = Math.min(savedProgress, lecDuration);
                        coveredSec += watched;
                        if (watched >= lecDuration * 0.9) completedCount++;
                    }
                }
            });

            coveredSec = Math.min(coveredSec, totalSec);
            const leftSec = Math.max(0, totalSec - coveredSec);
            const percent = totalSec > 0 ? Math.min(100, Math.round((coveredSec / totalSec) * 100)) : 0;

            return { totalSec, coveredSec, leftSec, percent, completedCount, totalCount };
        }

        function updateChapterStatsHub() {
            const stats = calculateChapterStats();
            if (statCoveredEl) statCoveredEl.innerText = formatDurationHuman(stats.coveredSec);
            if (statLeftEl) statLeftEl.innerText = formatDurationHuman(stats.leftSec);
            if (statTotalEl) statTotalEl.innerText = formatDurationHuman(stats.totalSec);
            if (statProgressFillEl) statProgressFillEl.style.width = `${stats.percent}%`;
            if (statProgressPercentEl) statProgressPercentEl.innerText = `${stats.percent}% Completed`;
            if (statLecturesRatioEl) statLecturesRatioEl.innerText = `${stats.completedCount} / ${stats.totalCount} Done`;
        }

        function updateQueueBadge() {
            if (chapterLectures.length === 0) return;
            const validIdx = (currentLectureIndex >= 0 && currentLectureIndex < chapterLectures.length);
            const currentNum = validIdx ? (currentLectureIndex + 1) : initialLecNum;
            const total = chapterLectures.length;
            const currentLec = validIdx ? chapterLectures[currentLectureIndex] : null;
            const titleDisplay = currentLec ? currentLec.title : (initialTitle || `Lecture ${currentNum}`);

            const queueChipEl = topHud.querySelector('#c-queue-chip');
            const lectureTitleEl = topHud.querySelector('#c-lecture-title');

            if (queueChipEl) queueChipEl.innerText = `Lec ${currentNum}/${total}`;
            if (lectureTitleEl) {
                lectureTitleEl.innerText = titleDisplay;
                lectureTitleEl.title = titleDisplay;
            }

            const left = Math.max(0, total - currentNum);
            if (topPlaylistCount) topPlaylistCount.innerText = `${left} Left`;
            updateChapterStatsHub();
        }

        function updatePrevNextButtons() {
            prevLecBtn.disabled = (currentLectureIndex <= 0);
            nextLecBtn.disabled = (currentLectureIndex >= chapterLectures.length - 1 || currentLectureIndex === -1);
            prevLecBtn.onclick = () => { if (currentLectureIndex > 0) navigateToLecture(currentLectureIndex - 1); };
            nextLecBtn.onclick = () => { if (currentLectureIndex < chapterLectures.length - 1) navigateToLecture(currentLectureIndex + 1); };
        }

        function navigateToLecture(index) {
            const target = chapterLectures[index];
            if (!target) return;
            const urlParams = new URLSearchParams(window.location.search);
            urlParams.set('videoId', target.id);
            urlParams.set('schId', target.schId);
            urlParams.set('lectureId', target.id);
            urlParams.set('title', target.title);
            if (selectedSubjectId) urlParams.set('subjectId', selectedSubjectId);
            if (selectedTopicId) {
                urlParams.set('tagId', selectedTopicId);
                urlParams.set('topicId', selectedTopicId);
            }
            if (target.url) urlParams.set('vUrl', target.url);
            window.location.search = urlParams.toString();
        }

        function renderTopPlaylist(filter = '') {
            if (!topPlaylistContainer) return;
            const q = (filter || '').trim().toLowerCase();
            updateChapterStatsHub();
            topPlaylistContainer.innerHTML = "";

            const displayList = (allChapterContents.length > 0) ? allChapterContents : chapterLectures;

            if (displayList.length === 0) {
                topPlaylistContainer.innerHTML = `
                    <div style="color:#94a3b8; font-size:0.82rem; padding:12px; text-align:center;">
                        <i class="fas fa-spinner fa-spin me-2" style="color:#38bdf8"></i> Loading or single lecture...
                        <div style="margin-top:8px;">
                            <button id="lq-retry-playlist" style="background:rgba(56,189,248,0.2); border:1px solid #38bdf8; color:#fff; border-radius:8px; padding:4px 10px; font-size:0.75rem; cursor:pointer;">Retry Load</button>
                        </div>
                    </div>
                `;
                const retryBtn = topPlaylistContainer.querySelector('#lq-retry-playlist');
                if (retryBtn) retryBtn.onclick = (e) => { e.stopPropagation(); initChapterPlaylist(true); };
                return;
            }

            let matches = 0;
            displayList.forEach((item, idx) => {
                if (activeContentFilter !== 'ALL' && item.type !== activeContentFilter) return;
                if (q && !item.title.toLowerCase().includes(q)) return;
                matches++;

                const isVideo = (item.type === 'LECTURE' || item.type === 'DPP_VIDEOS' || !item.type);
                const isPdf = (item.type === 'NOTES' || item.type === 'DPP_PDF');
                const isActive = isVideo && (idx === currentLectureIndex || (chapterLectures[currentLectureIndex] && chapterLectures[currentLectureIndex].id === item.id));
                const isCompleted = isVideo && (idx < currentLectureIndex);

                let statusBadge = '';
                if (item.type === 'NOTES') {
                    statusBadge = `<span class="lq-pdf-badge"><i class="fas fa-file-pdf"></i> PDF</span>`;
                } else if (item.type === 'DPP_PDF') {
                    statusBadge = `<span class="lq-dpp-badge"><i class="fas fa-file-alt"></i> DPP</span>`;
                } else if (item.type === 'DPP_VIDEOS') {
                    statusBadge = `<span class="lq-lec-badge"><i class="fas fa-video"></i> Sol</span>`;
                } else if (isActive) {
                    statusBadge = `<i class="fas fa-play" style="font-size:0.7rem;"></i>`;
                } else if (isCompleted) {
                    statusBadge = `<i class="fas fa-check-circle" style="font-size:0.78rem; color:#10b981;"></i>`;
                } else {
                    statusBadge = `#${item.index || (idx + 1)}`;
                }

                let durationDisplay = item.duration;
                if (isActive && video?.duration && isFinite(video.duration)) {
                    durationDisplay = `${formatTime(video.currentTime)} / ${formatTime(video.duration)}`;
                }

                let actionsHtml = '';
                if (isPdf && item.pdfUrl) {
                    actionsHtml = `
                        <div class="lq-item-actions">
                            <a href="${item.pdfUrl}" target="_blank" download class="lq-item-action-btn" title="Download PDF" onclick="event.stopPropagation();">
                                <i class="fas fa-download"></i>
                            </a>
                            <a href="${item.pdfUrl}" target="_blank" class="lq-item-action-btn" title="Open PDF in new tab" onclick="event.stopPropagation();">
                                <i class="fas fa-external-link-alt"></i>
                            </a>
                        </div>
                    `;
                } else if (isVideo && item.pdfUrl) {
                    actionsHtml = `
                        <div class="lq-item-actions">
                            <a href="${item.pdfUrl}" target="_blank" download class="lq-item-action-btn" title="Download Lecture Notes PDF" onclick="event.stopPropagation();">
                                <i class="fas fa-file-pdf" style="color:#f87171;"></i>
                            </a>
                        </div>
                    `;
                }

                const row = document.createElement('div');
                row.className = `playlist-item ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`;
                row.innerHTML = `
                    <div style="display:flex; align-items:center; gap:10px; overflow:hidden; flex:1;">
                        <span style="font-size:0.8rem; opacity:0.9; font-weight:800; min-width:24px; text-align:center;">${statusBadge}</span>
                        <span style="font-size:0.82rem; text-overflow:ellipsis; white-space:nowrap; overflow:hidden; font-weight:600;" title="${item.title}">${item.title}</span>
                    </div>
                    <div style="display:flex; align-items:center; gap:6px; flex-shrink:0;">
                        ${durationDisplay ? `<span style="font-size:0.75rem; opacity:0.85; font-family:monospace; white-space:nowrap;">${durationDisplay}</span>` : ''}
                        ${actionsHtml}
                    </div>
                `;

                row.onclick = (e) => {
                    e.stopPropagation();
                    if (isPdf && item.pdfUrl) {
                        window.open(item.pdfUrl, '_blank');
                    } else if (isVideo) {
                        const targetLecIdx = chapterLectures.findIndex(l => l.id === item.id);
                        if (targetLecIdx !== -1) navigateToLecture(targetLecIdx);
                        else if (item.url) {
                            const urlParams = new URLSearchParams(window.location.search);
                            urlParams.set('videoId', item.id);
                            urlParams.set('title', item.title);
                            urlParams.set('vUrl', item.url);
                            window.location.search = urlParams.toString();
                        }
                    }
                };
                topPlaylistContainer.appendChild(row);
            });

            if (matches === 0) {
                topPlaylistContainer.innerHTML = `<div style="color:#94a3b8; font-size:0.8rem; padding:14px; text-align:center;">No items found matching "${filter}"</div>`;
            } else {
                setTimeout(() => {
                    const activeEl = topPlaylistContainer.querySelector('.playlist-item.active');
                    if (activeEl) activeEl.scrollIntoView({ behavior: 'smooth', block: 'center' });
                }, 80);
            }
        }

        notesBtn.onclick = directOpenPDF;

        // Global Keyboard Shortcuts (Guarded to prevent stacked listeners across SPA lecture changes)
        if (window._liquidKeydownHandler) {
            try { window.removeEventListener('keydown', window._liquidKeydownHandler); } catch (e) { }
        }
        window._liquidKeydownHandler = (e) => {
            if (['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName)) return;

            if (['Space', 'ArrowRight', 'ArrowLeft', 'ArrowUp', 'ArrowDown'].includes(e.code) || ['k', 'K', 'j', 'J', 'l', 'L', 'm', 'M', '<', '>', ',', '.', '[', ']'].includes(e.key)) {
                hub.classList.add('user-active');
                scheduleIdleHide(2000);
            }

            if (e.code === 'Space' || e.key === 'k' || e.key === 'K') {
                e.preventDefault(); togglePlay();
            } else if (e.key === 'ArrowRight') {
                e.preventDefault();
                video.currentTime = Math.min(video.duration || 0, video.currentTime + 5);
                showOSD('<i class="fas fa-forward" style="color:var(--lq-accent, #38bdf8)"></i>', '+5s');
            } else if (e.key === 'ArrowLeft') {
                e.preventDefault();
                video.currentTime = Math.max(0, video.currentTime - 5);
                showOSD('<i class="fas fa-backward" style="color:var(--lq-accent, #38bdf8)"></i>', '-5s');
            } else if (e.key === 'j' || e.key === 'J') {
                e.preventDefault();
                video.currentTime = Math.max(0, video.currentTime - 10);
                showOSD('<i class="fas fa-undo-alt" style="color:var(--lq-accent, #38bdf8)"></i>', '-10s');
            } else if (e.key === 'l' || e.key === 'L') {
                e.preventDefault();
                video.currentTime = Math.min(video.duration || 0, video.currentTime + 10);
                showOSD('<i class="fas fa-redo-alt" style="color:var(--lq-accent, #38bdf8)"></i>', '+10s');
            } else if (e.key === 'ArrowUp') {
                e.preventDefault();
                video.volume = Math.min(1, video.volume + 0.1);
                video.muted = false;
                updateVolumeDisplay(video.volume, false);
                showOSD('<i class="fas fa-volume-up" style="color:var(--lq-accent, #38bdf8)"></i>', `${Math.round(video.volume * 100)}%`);
            } else if (e.key === 'ArrowDown') {
                e.preventDefault();
                video.volume = Math.max(0, video.volume - 0.1);
                video.muted = (video.volume === 0);
                updateVolumeDisplay(video.volume, video.muted);
                showOSD('<i class="fas fa-volume-down" style="color:var(--lq-accent, #38bdf8)"></i>', `${Math.round(video.volume * 100)}%`);
            } else if (e.key === 'm' || e.key === 'M') {
                volLabel.click();
            } else if (e.key === 'f' || e.key === 'F') {
                hub.querySelector('#c-fullscreen').click();
            } else if (e.key === 'p' || e.key === 'P') {
                settingsPipBtn.click();
            } else if (e.key === 'v' || e.key === 'V') {
                settingsBoostBtn.click();
            } else if (e.key === 't' || e.key === 'T') {
                toggleSlidesTray();
            } else if (e.key === 'q' || e.key === 'Q') {
                toggleTopPlaylistDropdown();
            } else if (e.key === 'n' || e.key === 'N') {
                const cycleModes = ['last-slide', '5min', 'off'];
                const curIdx = cycleModes.indexOf(autoNextMode);
                setAutoNextMode(cycleModes[(curIdx + 1) % cycleModes.length]);
            } else if (e.key === '<' || e.key === ',' || e.key === '[') {
                e.preventDefault(); applySpeed(Math.max(0.25, currentSpeed - 0.25), true, true);
            } else if (e.key === '>' || e.key === '.' || e.key === ']') {
                e.preventDefault(); applySpeed(Math.min(6.0, currentSpeed + 0.25), true, true);
            } else if (e.key === '?' || e.key === 'h' || e.key === 'H') {
                toggleShortcutsModal();
            } else if (e.key === 'Escape') {
                closeAllDrawers();
                dismissNextLecturePrompt();
            }
        };
        window.addEventListener('keydown', window._liquidKeydownHandler);

        video.addEventListener('loadedmetadata', () => {
            applySpeed(currentSpeed, false, false);
            applyTurboPreload(false);
            setTimeout(loadAndRenderSlides, 600);
            setTimeout(initChapterPlaylist, 800);
            setTimeout(attemptResumePlayback, 400);
        });
        video.addEventListener('durationchange', () => {
            applySpeed(currentSpeed, false, false);
            applyTurboPreload(false);
            setTimeout(renderSlideSeparators, 300);
            attemptResumePlayback();
        });
        video.addEventListener('canplay', () => {
            applySpeed(currentSpeed, false, false);
            applyTurboPreload(false);
            setTimeout(renderSlideSeparators, 300);
            attemptResumePlayback();
        });

        setTimeout(loadAndRenderSlides, 800);
        setTimeout(loadAndRenderSlides, 2000);
        setTimeout(initChapterPlaylist, 1000);
        setTimeout(initChapterPlaylist, 2500);
        setTimeout(attemptResumePlayback, 500);
        setTimeout(attemptResumePlayback, 1200);
        setTimeout(attemptResumePlayback, 2500);
    }

    let _liquidCurrentVideo = null;
    function tryMountLiquidPlayer() {
        const existingHub = document.getElementById('custom-player-hub');
        const video = document.getElementById('video') || document.querySelector('#video-wrapper video, .video-js video, video');
        if (!video) return;

        if (existingHub) {
            // In Single Page Apps, clicking another video lecture replaces the <video> element.
            // If the video element changed or old hub is orphaned, clean up and re-mount!
            if (_liquidCurrentVideo && _liquidCurrentVideo !== video) {
                try { existingHub.remove(); } catch (e) { }
                const oldTop = document.getElementById('lq-top-lecture-hud');
                if (oldTop) try { oldTop.remove(); } catch (e) { }
            } else {
                return;
            }
        }

        try {
            _liquidCurrentVideo = video;
            mountLiquidPlayer();
        } catch (err) {
            console.error("[LiquidPlayer] Mount error:", err);
        }
    }

    function checkAndMountPlayer() {
        if (isPlayerPageActive()) {
            try {
                if (window.AndroidPlayerBridge && window.AndroidPlayerBridge.onLecturePlayerDetected) {
                    window.AndroidPlayerBridge.onLecturePlayerDetected(true);
                }
            } catch (e) { }
            tryMountLiquidPlayer();
        }
    }

    // Intercept SPA client-side routing (pushState, replaceState, popstate, hashchange)
    try {
        const _origPush = history.pushState;
        history.pushState = function () {
            _origPush.apply(this, arguments);
            setTimeout(checkAndMountPlayer, 150);
            setTimeout(checkAndMountPlayer, 600);
        };
        const _origReplace = history.replaceState;
        history.replaceState = function () {
            _origReplace.apply(this, arguments);
            setTimeout(checkAndMountPlayer, 150);
            setTimeout(checkAndMountPlayer, 600);
        };
        window.addEventListener('popstate', checkAndMountPlayer);
        window.addEventListener('hashchange', checkAndMountPlayer);
    } catch (e) { }

    // Continuous lightweight watcher for DOM/route changes
    setInterval(checkAndMountPlayer, 800);

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        checkAndMountPlayer();
    } else {
        document.addEventListener('DOMContentLoaded', checkAndMountPlayer);
        window.addEventListener('load', checkAndMountPlayer);
        setTimeout(checkAndMountPlayer, 350);
    }
})();