// ==UserScript==
// @name         PWThor Pure Liquid Glass Player
// @namespace    https://pwthor.live/
// @version      51.0
// @description  Masterpiece Liquid Glass UI & Course Suite for PWTHOR (pwthor.live) with subjects, chapters, lectures, and premium player.
// @match        *://*.pwthor.live/*
// @match        *://pwthor.live/*
// @match        *://*/*player*
// @match        *://*/*study*
// @include      *player.html*
// @include      *pwthor.live*
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

    /* 0. INJECT FONT AWESOME 6 (Fixes "Icon Trash" & Tailwind CSS fill conflicts) */
    try {
        if (!document.getElementById('lq-font-awesome-css')) {
            const faLink = document.createElement('link');
            faLink.id = 'lq-font-awesome-css';
            faLink.rel = 'stylesheet';
            faLink.href = 'https://cdnjs.cloudflare.com/ajax/libs/font-awesome/6.5.1/css/all.min.css';
            faLink.crossOrigin = 'anonymous';
            (document.head || document.documentElement).appendChild(faLink);
        }
    } catch (e) { }

    /* 0.1 ANTI-DEBUGGER & DEVTOOLS UNLOCKER */
    try {
        console.clear = () => {};

        const originalFunction = window.Function;
        const hookedFunction = function (...args) {
            if (args.length > 0 && typeof args[args.length - 1] === 'string') {
                if (args[args.length - 1].includes('debugger')) {
                    return function () {};
                }
            }
            return originalFunction.apply(this, args);
        };
        hookedFunction.prototype = originalFunction.prototype;
        window.Function = hookedFunction;
        Function.prototype.constructor = hookedFunction;

        const originalEval = window.eval;
        window.eval = function (code) {
            if (typeof code === 'string' && code.includes('debugger')) {
                code = code.replace(/\bdebugger\b/g, '');
            }
            return originalEval.call(this, code);
        };

        const originalSetInterval = window.setInterval;
        window.setInterval = function (fn, delay, ...args) {
            if (typeof fn === 'string' && fn.includes('debugger')) return -1;
            if (typeof fn === 'function') {
                const fnStr = fn.toString();
                if (fnStr.includes('debugger') || fnStr.includes('devtoolsCheck')) return -1;
            }
            return originalSetInterval.call(this, fn, delay, ...args);
        };

        const originalSetTimeout = window.setTimeout;
        window.setTimeout = function (fn, delay, ...args) {
            if (typeof fn === 'string' && fn.includes('debugger')) return -1;
            if (typeof fn === 'function') {
                const fnStr = fn.toString();
                if (fnStr.includes('debugger') || fnStr.includes('devtoolsCheck')) return -1;
            }
            return originalSetTimeout.call(this, fn, delay, ...args);
        };

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

    /* 0.2 PREVENT ICON DUPLICATION */
    // Font Awesome 6 provides all icons via ::before glyphs. Keep innerHTML empty to prevent duplicates.
    function applySvgIcons(root) {
        try {
            const target = root || document;
            const icons = target.querySelectorAll ? target.querySelectorAll('i[class*="fa-"]') : [];
            icons.forEach(el => {
                if (el.innerHTML !== '') el.innerHTML = '';
            });
        } catch (e) { }
    }

    /* 0.23 PWTHOR DATA STORE & EMBEDDED FALLBACK DATASETS */
    const capturedData = {
        batchSubjects: {},
        subjectTopics: {},
        topicContents: {},
        lectureDetails: {},
        headers: {}
    };

    const EMBEDDED_SUBJECTS = [
        { id: 'physics-by-rajwant-singh-sir', name: 'Physics By Rajwant Singh Sir', count: '34 Chapters', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/a66e1a97-1462-4829-9efd-7168bdd9b726.png' },
        { id: 'physical-chemistry-by-rahul-dudi-sir', name: 'Physical Chemistry By Rahul Dudi Sir', count: '19 Chapters', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/9a88cbe7-3356-4351-83eb-0770e1bc8e2f.png' },
        { id: 'inorganic-chemistry-by-kunwar-om-pandey-sir', name: 'Inorganic Chemistry By Kunwar Om Pandey Sir', count: '16 Chapters', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/66b3b9ea-9a15-4be5-aa1f-03d3b6d99b84.jpg' },
        { id: 'maths-by-sachin-jakhar-sir', name: 'Maths By Sachin Jakhar Sir', count: '34 Chapters', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/2092c715-5662-4aa1-a490-8cdccfba61c3.png' },
        { id: 'inorganic-chemistry-by-amitabh-sharma-sir', name: 'Inorganic Chemistry By Amitabh Sharma Sir', count: '15 Chapters', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/5ed575e3-0129-45c7-a070-dfb071ee298e.png' },
        { id: 'physics-by-varun-chauhan-sir', name: 'Physics by Varun Chauhan sir', count: '15 Chapters', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/a66e1a97-1462-4829-9efd-7168bdd9b726.png' },
        { id: 'physics-by-saleem-ahmad-sir', name: 'Physics By Saleem Ahmad Sir', count: '12 Chapters', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/a66e1a97-1462-4829-9efd-7168bdd9b726.png' }
    ];

    const EMBEDDED_CHAPTERS = [
        { id: 'all-contents', name: 'All Contents', totalVideos: 67, count: 67, notesCount: 311, tagType: 'UNITS', meta: '67 Videos | 44 Exercises | 311 Notes' },
        { id: 'pyq-practice-sheet-only-pdf', name: 'PYQ Practice Sheet || Only PDF', totalVideos: 0, count: 0, notesCount: 16, tagType: 'STUDY_MATERIAL', meta: '0 Videos | 0 Exercises | 16 Notes' },
        { id: 'short-notes-only-pdf', name: 'Short Notes || Only PDF', totalVideos: 0, count: 0, notesCount: 18, tagType: 'STUDY_MATERIAL', meta: '0 Videos | 0 Exercises | 18 Notes' },
        { id: 'mind-maps-only-pdf', name: 'Mind Maps || Only PDF', totalVideos: 0, count: 0, notesCount: 14, tagType: 'STUDY_MATERIAL', meta: '0 Videos | 0 Exercises | 14 Notes' },
        { id: 'pyq-blueprint-only-pdf', name: "PYQ's Blueprint || Only PDF", totalVideos: 0, count: 0, notesCount: 16, tagType: 'STUDY_MATERIAL', meta: '0 Videos | 0 Exercises | 16 Notes' },
        { id: 'bridge-course-lecture', name: 'Bridge Course Lecture', totalVideos: 1, count: 1, notesCount: 1, tagType: 'UNITS', meta: '1 Videos | 0 Exercises | 1 Notes' },
        { id: 'concise-summary-notes-only-pdf', name: 'Concise Summary Notes || Only PDF', totalVideos: 0, count: 0, notesCount: 93, tagType: 'STUDY_MATERIAL', meta: '0 Videos | 0 Exercises | 93 Notes' },
        { id: 'parakram-solution-by-rajwant-sir', name: 'Parakram Solution By Rajwant Sir', totalVideos: 6, count: 6, notesCount: 7, tagType: 'UNITS', meta: '6 Videos | 0 Exercises | 7 Notes' },
        { id: 'quick-revision-by-rajwant-sir', name: 'Quick Revision By Rajwant Sir', totalVideos: 4, count: 4, notesCount: 4, tagType: 'UNITS', meta: '4 Videos | 0 Exercises | 4 Notes' },
        { id: 'mission-11th-comeback', name: 'Mission 11th Comeback', totalVideos: 4, count: 4, notesCount: 4, tagType: 'UNITS', meta: '4 Videos | 0 Exercises | 4 Notes' },
        { id: 'extra-books-by-rajwant-sir-only-pdf', name: 'Extra Books By Rajwant Sir || Only PDF', totalVideos: 0, count: 0, notesCount: 22, tagType: 'STUDY_MATERIAL', meta: '0 Videos | 0 Exercises | 22 Notes' },
        { id: 'summary-lecture-by-rajwant-sir', name: 'Summary Lecture By Rajwant Sir', totalVideos: 2, count: 2, notesCount: 2, tagType: 'UNITS', meta: '2 Videos | 0 Exercises | 2 Notes' },
        { id: 'units-and-measurement', name: 'Units and Measurement', totalVideos: 13, count: 13, notesCount: 24, tagType: 'UNITS', meta: '13 Videos | 11 Exercises | 24 Notes' },
        { id: 'parakram-assignment-by-rajwant-sir-only-pdf', name: 'Parakram Assignment By Rajwant Sir || Only PDF', totalVideos: 0, count: 0, notesCount: 6, tagType: 'STUDY_MATERIAL', meta: '0 Videos | 0 Exercises | 6 Notes' },
        { id: 'mathematical-tools', name: 'Mathematical Tools', totalVideos: 11, count: 11, notesCount: 22, tagType: 'UNITS', meta: '11 Videos | 10 Exercises | 22 Notes' },
        { id: 'laws-of-motion', name: 'Laws of Motion', totalVideos: 21, count: 21, notesCount: 35, tagType: 'UNITS', meta: '21 Videos | 15 Exercises | 35 Notes' }
    ];

    const EMBEDDED_LAWS_OF_MOTION_LECTURES = [
        { id: 'lom-21', index: 21, type: 'LECTURE', title: 'Laws of Motion 21 : Questions on friction (Part Two)', duration: '02:11:36', date: '29 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/63299724-3716-435b-ad8c-bca7b3300470.png' },
        { id: 'lom-20', index: 20, type: 'LECTURE', title: 'Laws of Motion 20 : Questions on Friction || NO DPP', duration: '02:16:20', date: '27 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/960e0418-dcfe-4771-afd4-38becfc1e611.png' },
        { id: 'lom-19', index: 19, type: 'LECTURE', title: 'Laws of Motion 19 : Questions on friction || Rescheduled at 06:15 PM', duration: '01:47:16', date: '22 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/aee4e6fa-7a81-4fde-9886-2db5c005800e.png' },
        { id: 'lom-18', index: 18, type: 'LECTURE', title: "Laws of Motion 18 : Introduction to friction and its type || Questions on Friction", duration: '01:59:56', date: '21 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/eba9add4-6e60-4593-9409-857885d41a98.png' },
        { id: 'lom-17', index: 17, type: 'LECTURE', title: 'Laws of Motion 17 : Questions on pseudo force and introduction to friction', duration: '01:52:10', date: '19 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/40507f2d-b49f-4aa7-8153-9ef70efb1063.png' },
        { id: 'lom-16', index: 16, type: 'LECTURE', title: 'Laws of Motion 16 : Pseudo force problems and spring balance', duration: '01:45:30', date: '16 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/960e0418-dcfe-4771-afd4-38becfc1e611.png' },
        { id: 'lom-15', index: 15, type: 'LECTURE', title: 'Laws of Motion 15 : Problems on wedge constraint', duration: '02:05:12', date: '14 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/63299724-3716-435b-ad8c-bca7b3300470.png' },
        { id: 'lom-14', index: 14, type: 'LECTURE', title: 'Laws of Motion 14 : Constraint relations', duration: '02:10:45', date: '12 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/aee4e6fa-7a81-4fde-9886-2db5c005800e.png' },
        { id: 'lom-13', index: 13, type: 'LECTURE', title: 'Laws of Motion 13 : Pulley problems (Part Two)', duration: '01:58:30', date: '10 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/eba9add4-6e60-4593-9409-857885d41a98.png' },
        { id: 'lom-12', index: 12, type: 'LECTURE', title: 'Laws of Motion 12 : Pulley problems (Part One)', duration: '02:02:15', date: '8 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/40507f2d-b49f-4aa7-8153-9ef70efb1063.png' },
        { id: 'lom-11', index: 11, type: 'LECTURE', title: 'Laws of Motion 11 : Tension and normal contact force', duration: '01:50:20', date: '7 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/960e0418-dcfe-4771-afd4-38becfc1e611.png' },
        { id: 'lom-10', index: 10, type: 'LECTURE', title: "Laws of Motion 10 : Newton's third law and equilibrium", duration: '02:12:40', date: '6 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/63299724-3716-435b-ad8c-bca7b3300470.png' },
        { id: 'lom-09', index: 9, type: 'LECTURE', title: 'Laws of Motion 09 : Equilibrium problems', duration: '01:54:22', date: '5 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/aee4e6fa-7a81-4fde-9886-2db5c005800e.png' },
        { id: 'lom-08', index: 8, type: 'LECTURE', title: 'Laws of Motion 08 : Questions on Equilibrium || Spring Balance || Weighing Machine', duration: '01:56:36', date: '4 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/eba9add4-6e60-4593-9409-857885d41a98.png' },
        { id: 'lom-07', index: 7, type: 'LECTURE', title: 'Laws of Motion 07 : Questions on Equilibrium || Spring Balance || Weighing Machine || NO DPP', duration: '02:18:48', date: '1 Aug 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/5b266fc1-3b41-4140-82a2-89b7d3cca2cb.png' },
        { id: 'lom-06', index: 6, type: 'LECTURE', title: 'Laws of Motion 06 : Equilibrium Problems', duration: '02:04:10', date: '31 Jul 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/2dda89ba-dcd4-4437-a3de-6126e5c029e2.png' },
        { id: 'lom-05', index: 5, type: 'LECTURE', title: "Laws of Motion 05 : Newton's Second Law Applications", duration: '01:58:22', date: '30 Jul 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/40507f2d-b49f-4aa7-8153-9ef70efb1063.png' },
        { id: 'lom-04', index: 4, type: 'LECTURE', title: 'Laws of Motion 04 : Homework Discussion || Newton Laws of Motion || Free Body Diagram (Part Three)', duration: '02:15:04', date: '29 Jul 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/3381f7e0-8e70-4a26-bd0a-8bbe79d36bb1.png' },
        { id: 'lom-03', index: 3, type: 'LECTURE', title: 'Laws of Motion 03 : Homework Discussion || Newton Laws of Motion || Free Body Diagram (Part Two) || NO DPP', duration: '01:56:24', date: '28 Jul 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/7d7bcb9c-f3f6-483b-a9d5-7ae0d16c99f7.png' },
        { id: 'lom-02', index: 2, type: 'LECTURE', title: 'Laws of Motion 02 : Homework Discussion || Newton Laws of Motion || Free Body Diagram || NO DPP || Rescheduled at 06:30 PM', duration: '01:55:32', date: '25 Jul 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/ADMIN/940d3530-269f-48db-9d19-e50aa0be07a4.png' },
        { id: 'lom-01', index: 1, type: 'LECTURE', title: 'Laws of Motion 01 : Introduction to Force and Inertia || Newton Laws of Motion', duration: '01:48:10', date: '24 Jul 2026', image: 'https://static.pw.live/5eb393ee95fab7468a79d189/image/ed091ee4-89bb-49c4-9132-a0a87a7ac960.png' }
    ];

    const EMBEDDED_NOTES = [
        { id: 'lom-notes-21', index: 1, type: 'NOTES', title: 'Laws of Motion 21 : Class Notes PDF', duration: 'PDF', pdfUrl: 'https://static.pw.live/notes/lom_21_notes.pdf' },
        { id: 'lom-notes-20', index: 2, type: 'NOTES', title: 'Laws of Motion 20 : Class Notes PDF', duration: 'PDF', pdfUrl: 'https://static.pw.live/notes/lom_20_notes.pdf' },
        { id: 'lom-notes-19', index: 3, type: 'NOTES', title: 'Laws of Motion 19 : Class Notes PDF', duration: 'PDF', pdfUrl: 'https://static.pw.live/notes/lom_19_notes.pdf' },
        { id: 'lom-notes-18', index: 4, type: 'NOTES', title: 'Laws of Motion 18 : Class Notes PDF', duration: 'PDF', pdfUrl: 'https://static.pw.live/notes/lom_18_notes.pdf' },
        { id: 'lom-notes-concise', index: 5, type: 'NOTES', title: 'Concise Summary Notes || Only PDF', duration: 'PDF', pdfUrl: 'https://static.pw.live/notes/lom_concise.pdf' }
    ];

    const EMBEDDED_DPPS = [
        { id: 'lom-dpp-01', index: 1, type: 'DPP_PDF', title: 'Laws of Motion : DPP 01 Problem Sheet', duration: 'PDF', pdfUrl: 'https://static.pw.live/dpp/lom_dpp_01.pdf' },
        { id: 'lom-dpp-02', index: 2, type: 'DPP_PDF', title: 'Laws of Motion : DPP 02 Problem Sheet', duration: 'PDF', pdfUrl: 'https://static.pw.live/dpp/lom_dpp_02.pdf' },
        { id: 'lom-dpp-03', index: 3, type: 'DPP_PDF', title: 'Laws of Motion : DPP 03 Problem Sheet', duration: 'PDF', pdfUrl: 'https://static.pw.live/dpp/lom_dpp_03.pdf' },
        { id: 'lom-dpp-vid-01', index: 1, type: 'DPP_VIDEOS', title: 'Laws of Motion : DPP 01 Video Solution', duration: '28:15', date: '26 Jul 2026' },
        { id: 'lom-dpp-vid-02', index: 2, type: 'DPP_VIDEOS', title: 'Laws of Motion : DPP 02 Video Solution', duration: '31:40', date: '29 Jul 2026' }
    ];

    /* 0.25 PERSISTENT STORE & DATA MODEL ADAPTER */
    const PW_STORAGE = {
        saveBatchId: (id) => { try { localStorage.setItem('pwthor_batch_id', id); } catch (e) { } },
        getBatchId: () => { try { return localStorage.getItem('pwthor_batch_id') || 'batch-pwthor'; } catch (e) { return 'batch-pwthor'; } },
        saveSelectedSubject: (name) => { if (name) try { localStorage.setItem('pwthor_last_subj', name); } catch (e) { } },
        getSelectedSubject: () => { try { return localStorage.getItem('pwthor_last_subj') || ''; } catch (e) { return ''; } },
        getActiveSubjectName: () => {
            try {
                const s = localStorage.getItem('pwthor_last_subj');
                if (s) return s;
                return EMBEDDED_SUBJECTS[0] ? EMBEDDED_SUBJECTS[0].name : 'Physics By Rajwant Singh Sir';
            } catch (e) {
                return 'Physics By Rajwant Singh Sir';
            }
        },
        saveSelectedChapter: (name) => { if (name) try { localStorage.setItem('pwthor_last_chap', name); } catch (e) { } },
        getSelectedChapter: () => { try { return localStorage.getItem('pwthor_last_chap') || ''; } catch (e) { return ''; } },
        getActiveChapterName: () => {
            try {
                const c = localStorage.getItem('pwthor_last_chap');
                if (c) return c;
                return EMBEDDED_CHAPTERS[15] ? EMBEDDED_CHAPTERS[15].name : 'Laws of Motion';
            } catch (e) {
                return 'Laws of Motion';
            }
        },
        saveCurrentLectureInfo: (info) => { if (info) try { localStorage.setItem('pwthor_cur_lec', JSON.stringify(info)); } catch (e) { } },
        getCurrentLectureInfo: () => {
            try {
                const raw = JSON.parse(localStorage.getItem('pwthor_cur_lec') || '{}');
                if (raw && (raw.title || raw.name)) return raw;
                if (EMBEDDED_LAWS_OF_MOTION_LECTURES.length > 0) {
                    return EMBEDDED_LAWS_OF_MOTION_LECTURES[0];
                }
                return {};
            } catch (e) {
                return {};
            }
        },
        getActiveLectures: () => {
            try {
                if (EMBEDDED_LAWS_OF_MOTION_LECTURES.length > 0) {
                    return EMBEDDED_LAWS_OF_MOTION_LECTURES;
                }
                const cur = PW_STORAGE.getCurrentLectureInfo();
                return (cur && cur.title) ? [cur] : [];
            } catch (e) {
                return [];
            }
        },
        getSubjects: () => {
            try {
                const bId = PW_STORAGE.getBatchId();
                if (capturedData.batchSubjects && capturedData.batchSubjects[bId]) {
                    return capturedData.batchSubjects[bId];
                }
                return EMBEDDED_SUBJECTS;
            } catch (e) {
                return [];
            }
        },
        getChapters: (subjId) => {
            try {
                const eff = subjId || PW_STORAGE.getSelectedSubject() || 'default-subject';
                if (capturedData.subjectTopics && capturedData.subjectTopics[eff]) {
                    return capturedData.subjectTopics[eff];
                }
                return EMBEDDED_CHAPTERS;
            } catch (e) {
                return [];
            }
        },
        ingestApiPayload: (data, url) => {
            try {
                if (!data) return;
                const urlLower = (url || '').toLowerCase();
                if (urlLower.includes('subjects') && (data.data || data.subjects)) {
                    const subs = data.data || data.subjects;
                    if (Array.isArray(subs)) capturedData.batchSubjects[PW_STORAGE.getBatchId()] = subs;
                } else if (urlLower.includes('topics') && data.data) {
                    const topics = Array.isArray(data.data) ? data.data : (data.data?.data || []);
                    if (topics.length > 0) {
                        try {
                            const urlObj = new URL(url, window.location.origin);
                            const subj = urlObj.searchParams.get('subjectId') || PW_STORAGE.getSelectedSubject();
                            if (subj) capturedData.subjectTopics[subj] = topics;
                        } catch (e) { }
                    }
                } else if (urlLower.includes('contents') && data.data) {
                    const contents = Array.isArray(data.data) ? data.data : (data.data?.data || []);
                    if (contents.length > 0) {
                        try {
                            const urlObj = new URL(url, window.location.origin);
                            const topic = urlObj.searchParams.get('tagId') || urlObj.searchParams.get('topicId') || PW_STORAGE.getSelectedChapter();
                            if (topic) capturedData.topicContents[topic] = contents;
                        } catch (e) { }
                    }
                }
            } catch (e) { }
        }
    };

    /* 0.3 REAL-TIME NETWORK & API SYNC ENGINE */
    try {
        const originalFetch = window.fetch;
        window.fetch = async function (resource, init) {
            const res = await originalFetch.apply(this, arguments);
            try {
                const url = typeof resource === 'string' ? resource : resource?.url || '';
                const urlLower = url.toLowerCase();
                if (
                    urlLower.includes('/api/') ||
                    urlLower.includes('/v2/') ||
                    urlLower.includes('/v3/') ||
                    urlLower.includes('batches') ||
                    urlLower.includes('subjects') ||
                    urlLower.includes('topics') ||
                    urlLower.includes('contents') ||
                    urlLower.includes('stream') ||
                    urlLower.includes('schedule')
                ) {
                    const clone = res.clone();
                    clone.json().then(data => {
                        PW_STORAGE.ingestApiPayload(data, url);
                    }).catch(() => {});
                }
            } catch (e) { }
            return res;
        };

        const originalXhrOpen = XMLHttpRequest.prototype.open;
        const originalXhrSend = XMLHttpRequest.prototype.send;
        XMLHttpRequest.prototype.open = function (method, url) {
            this._lqUrl = url;
            return originalXhrOpen.apply(this, arguments);
        };
        XMLHttpRequest.prototype.send = function () {
            this.addEventListener('load', () => {
                try {
                    const url = (this._lqUrl || '').toLowerCase();
                    if (
                        url.includes('/api/') ||
                        url.includes('/v2/') ||
                        url.includes('/v3/') ||
                        url.includes('batches') ||
                        url.includes('subjects') ||
                        url.includes('topics') ||
                        url.includes('contents')
                    ) {
                        const data = JSON.parse(this.responseText);
                        PW_STORAGE.ingestApiPayload(data, this._lqUrl);
                    }
                } catch (e) { }
            });
            return originalXhrSend.apply(this, arguments);
        };
    } catch (e) { }

    /* 0.4 SPA NAVIGATION & HISTORY SYNC ENGINE */
    try {
        const origPush = history.pushState;
        history.pushState = function () {
            const res = origPush.apply(this, arguments);
            setTimeout(scrapePwthorPages, 200);
            setTimeout(scrapePwthorPages, 800);
            setTimeout(checkAndMountPlayer, 300);
            return res;
        };

        const origReplace = history.replaceState;
        history.replaceState = function () {
            const res = origReplace.apply(this, arguments);
            setTimeout(scrapePwthorPages, 200);
            setTimeout(scrapePwthorPages, 800);
            setTimeout(checkAndMountPlayer, 300);
            return res;
        };

        window.addEventListener('popstate', () => {
            setTimeout(scrapePwthorPages, 200);
            setTimeout(scrapePwthorPages, 800);
            setTimeout(checkAndMountPlayer, 300);
        });

        // Debounced MutationObserver to detect async Next.js client renders
        let domMutationTimeout = null;
        const domObserver = new MutationObserver(() => {
            if (domMutationTimeout) clearTimeout(domMutationTimeout);
            domMutationTimeout = setTimeout(() => {
                scrapePwthorPages();
                checkAndMountPlayer();
            }, 300);
        });
        if (document.body) {
            domObserver.observe(document.body, { childList: true, subtree: true });
        } else {
            document.addEventListener('DOMContentLoaded', () => {
                domObserver.observe(document.body || document.documentElement, { childList: true, subtree: true });
            });
        }
    } catch (e) { }

    /* 0.5 PAGE DETECTION UTILITIES */
    function isPlayerPageActive() {
        const href = (window.location.href || '').toLowerCase();
        return href.includes('player') ||
               href.includes('watch') ||
               href.includes('video') ||
               (document.querySelector('.player video, #video-wrapper video, .video-js video, video') !== null);
    }

    function isSubjectListPage() {
        const h3s = Array.from(document.querySelectorAll('h3, h2, h1'));
        const hasSubjectsHeading = h3s.some(h => h.innerText?.trim().toLowerCase() === 'subjects');
        const hasBatchHeader = document.querySelector('div[class*="descriptionHeader"], h1.text-2xl') !== null;
        return (hasSubjectsHeading || hasBatchHeader) && !isPlayerPageActive();
    }

    function isChapterListPage() {
        const articles = document.querySelectorAll('main article, .container article');
        if (articles.length > 0) {
            const text = articles[0]?.innerText || '';
            if (text.includes('Videos') && (text.includes('Exercises') || text.includes('Notes'))) {
                return true;
            }
        }
        return false;
    }

    function isLectureListPage() {
        const tabList = document.querySelector('[role="tablist"], [aria-label*="Lectures"]');
        const hasTabs = tabList && (tabList.innerText.includes('Lectures') || tabList.innerText.includes('Notes'));
        const hasVideoCards = document.querySelector('[aria-label="Play video"]') !== null;
        return (hasTabs || hasVideoCards) && !isPlayerPageActive();
    }

    try {
        if (isPlayerPageActive() && window.AndroidPlayerBridge && window.AndroidPlayerBridge.onLecturePlayerDetected) {
            window.AndroidPlayerBridge.onLecturePlayerDetected(true);
        }
    } catch (e) { }

    /* 1. PWTHOR REAL-TIME DATA COLLECTOR ENGINE (REFERENCE.JS PARCHAMCORE ARCHITECTURE) */
    // capturedData and EMBEDDED datasets are declared at top of script

    function formatSlugToTitle(slug) {
        if (!slug) return '';
        return slug.replace(/[-_]+/g, ' ').replace(/\b\w/g, c => c.toUpperCase()).trim();
    }

    function isGenericBrandName(name) {
        if (!name) return true;
        const clean = String(name).trim().toLowerCase();
        return clean === 'pw thor' || clean === 'pwthor' || clean === 'study panda' || clean === 'studyparcham' || clean === 'player' || clean === 'watch' || clean === 'loading lecture...';
    }

    function getNodeText(el) {
        if (!el) return '';
        return (el.textContent || el.innerText || '').trim();
    }

    // React Fiber traversal helper to extract in-memory component props
    function scanReactFiberTree() {
        try {
            const nodes = [
                document.querySelector('.player'),
                document.querySelector('#video-wrapper'),
                document.querySelector('video'),
                document.querySelector('#__next'),
                document.body
            ].filter(Boolean);

            const visited = new Set();
            let currentLectureTitle = null;
            let foundLectures = null;
            let foundSubjects = null;
            let foundTopics = null;
            let foundAttachments = null;

            function walk(fiber, depth = 0) {
                if (!fiber || depth > 35 || visited.has(fiber)) return;
                visited.add(fiber);

                const props = fiber.memoizedProps;
                const state = fiber.memoizedState;

                [props, state].forEach(obj => {
                    if (!obj || typeof obj !== 'object') return;
                    for (const key of Object.keys(obj)) {
                        const val = obj[key];
                        if (Array.isArray(val) && val.length > 0) {
                            const first = val[0];
                            if (first && typeof first === 'object') {
                                if ((first.topic || first.name || first.videoDetails || first.Type === 'penpencilvdo' || first.ChildId) && !foundLectures) {
                                    if (first.duration || first.videoUrl || first.videoDetails || first.topic || first.url) {
                                        foundLectures = val;
                                    }
                                }
                                if ((first.subject || first.teacherIds) && !foundSubjects) {
                                    foundSubjects = val;
                                }
                                if ((first.totalVideos !== undefined || first.tagType) && !foundTopics) {
                                    foundTopics = val;
                                }
                                if ((first.attachmentIds || first.fileUrl) && !foundAttachments) {
                                    foundAttachments = val;
                                }
                            }
                        } else if (typeof val === 'string' && val.length > 3 && val.length < 140) {
                            if (['lectureTitle', 'currentTitle', 'topicName', 'videoTitle', 'title'].includes(key) && !isGenericBrandName(val)) {
                                if (!currentLectureTitle) currentLectureTitle = val;
                            }
                        }
                    }
                });

                if (fiber.child) walk(fiber.child, depth + 1);
                if (fiber.sibling) walk(fiber.sibling, depth + 1);
            }

            for (const node of nodes) {
                const fiberKey = Object.keys(node).find(k => k.startsWith('__reactFiber') || k.startsWith('__reactInternalInstance'));
                if (fiberKey && node[fiberKey]) {
                    walk(node[fiberKey], 0);
                    if (currentLectureTitle || foundLectures) break;
                }
            }

            return { currentLectureTitle, foundLectures, foundSubjects, foundTopics, foundAttachments };
        } catch (e) {
            return {};
        }
    }

    function resolveLectureTitle(fallback = '') {
        const urlParams = new URLSearchParams(window.location.search);
        const titleParam = urlParams.get('title');
        if (titleParam && !isGenericBrandName(titleParam)) return titleParam.trim();

        const fiber = scanReactFiberTree();
        if (fiber?.currentLectureTitle && !isGenericBrandName(fiber.currentLectureTitle)) {
            return fiber.currentLectureTitle.trim();
        }

        const domSels = [
            '.player-header .text-lg',
            '.player-header h1',
            '.player-header h2',
            '.player-header span',
            'h1', 'h2', 'h3'
        ];
        for (const sel of domSels) {
            const el = document.querySelector(sel);
            if (el) {
                const t = getNodeText(el);
                if (t && !isGenericBrandName(t) && t.length > 2 && t.length < 150) return t;
            }
        }

        const topicId = urlParams.get('topicId') || urlParams.get('tagId');
        if (topicId) {
            const t = formatSlugToTitle(topicId);
            if (t) return t;
        }

        const docTitle = (document.title || '').replace(/ - PW THOR/gi, '').trim();
        if (docTitle && !isGenericBrandName(docTitle)) return docTitle;

        return fallback || 'Laws of Motion 21 : Questions on friction (Part Two)';
    }

    // Deep content parser for PWTHOR HTML pages
    function extractContentsFromDoc(doc, batchId, subjectId, topicId) {
        if (!doc) return null;
        const videos = [];
        const notes = [];
        const dppPdfs = [];
        const dppVideos = [];

        function parseArticle(art, defaultType = 'LECTURE', index = 1) {
            const titleEl = art.querySelector('p.line-clamp-2, p.font-semibold, p.text-foreground, h2, h3, h4');
            const imgEl = art.querySelector('img');
            const rawTitle = getNodeText(titleEl) || (imgEl ? imgEl.getAttribute('alt') : '') || 'Lecture';
            const title = rawTitle.replace(/\s+/g, ' ').trim();

            let duration = '';
            const timeEl = art.querySelector('time');
            const dateStr = getNodeText(timeEl);

            const text = art ? (art.textContent || art.innerText || '') : '';
            const durMatch = text.match(/(\d{1,2}:\d{2}(?::\d{2})?)/);
            if (durMatch) duration = durMatch[1];

            let id = '';
            let url = '';
            const link = art.querySelector('a') || (art.tagName === 'A' ? art : null);
            if (link && link.href) {
                url = link.href;
                const uMatch = link.href.match(/[?&]ChildId=([^&]+)/i) || link.href.match(/\/([a-f0-9]{24})/i);
                if (uMatch) id = uMatch[1];
            }

            let num = index;
            const numMatch = title.match(/(?:lecture|lec|\b)\s*(\d+)/i);
            if (numMatch && numMatch[1]) {
                const parsed = parseInt(numMatch[1], 10);
                if (!isNaN(parsed) && parsed > 0) num = parsed;
            }

            if (!id) id = `item_${defaultType.toLowerCase()}_${num}_${Date.now()}`;

            return {
                id: id,
                _id: id,
                schId: id,
                index: num,
                type: defaultType,
                title: title,
                topic: title,
                name: title,
                duration: duration || (defaultType === 'LECTURE' ? '1h 50m' : (defaultType === 'NOTES' || defaultType === 'DPP_PDF' ? 'PDF' : '30m')),
                videoDetails: { duration: duration || '1h 50m', videoUrl: url },
                url: url,
                pdfUrl: (url && url.toLowerCase().includes('.pdf')) ? url : '',
                date: dateStr || '',
                image: imgEl?.src || ''
            };
        }

        // 1. Videos
        const videoPanel = doc.querySelector('[id*="content-videos"], [id*="content-lectures"], [role="tabpanel"][data-state="active"]');
        let videoArticles = videoPanel ? Array.from(videoPanel.querySelectorAll('article')) : [];
        if (videoArticles.length === 0) videoArticles = Array.from(doc.querySelectorAll('article'));

        videoArticles.forEach((art, idx) => {
            const item = parseArticle(art, 'LECTURE', idx + 1);
            videos.push(item);
        });

        // 2. Notes
        const notesPanel = doc.querySelector('[id*="content-notes"], [id*="content-Notes"]');
        if (notesPanel) {
            notesPanel.querySelectorAll('article, a[href*=".pdf"]').forEach((art, idx) => {
                notes.push(parseArticle(art, 'NOTES', idx + 1));
            });
        }

        // 3. DPP PDFs
        const dppNotesPanel = doc.querySelector('[id*="content-DppNotes"], [id*="content-dppnotes"]');
        if (dppNotesPanel) {
            dppNotesPanel.querySelectorAll('article, a[href*=".pdf"]').forEach((art, idx) => {
                dppPdfs.push(parseArticle(art, 'DPP_PDF', idx + 1));
            });
        }

        // 4. DPP Videos
        const dppVideosPanel = doc.querySelector('[id*="content-DppVideos"], [id*="content-dppvideos"]');
        if (dppVideosPanel) {
            dppVideosPanel.querySelectorAll('article').forEach((art, idx) => {
                dppVideos.push(parseArticle(art, 'DPP_VIDEOS', idx + 1));
            });
        }

        return { videos, notes, dppPdfs, dppVideos };
    }

    // Active same-origin fetcher for batch subjects
    async function fetchBatchPageData(batchId) {
        if (!batchId) return null;
        if (capturedData.batchSubjects[batchId]?.length > 0) {
            return { subjects: capturedData.batchSubjects[batchId] };
        }
        try {
            const res = await window.fetch(`/study/batches/${batchId}`, { credentials: 'same-origin' });
            if (!res.ok) return null;
            const html = await res.text();
            const parser = new DOMParser();
            const doc = parser.parseFromString(html, 'text/html');

            const subjectsHeading = Array.from(doc.querySelectorAll('h3, h2')).find(h => h.innerText?.trim().toLowerCase() === 'subjects');
            const container = subjectsHeading ? subjectsHeading.closest('.divshadow, div') : doc;
            const subjectCards = container ? container.querySelectorAll('.grid > div.cursor-pointer, .grid div.flex.items-center.gap-3') : [];

            const subjects = [];
            subjectCards.forEach((c) => {
                const titleEl = c.querySelector('p.font-semibold, p.text-foreground, .text-sm, .text-base');
                const countEl = c.querySelector('p.text-xs, p.text-muted-foreground');
                const imgEl = c.querySelector('img');
                const title = titleEl ? titleEl.innerText.trim() : '';
                if (title && !title.toLowerCase().includes('0 xp') && !title.toLowerCase().includes("today's class")) {
                    const id = title.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
                    subjects.push({
                        id: id,
                        _id: id,
                        name: title,
                        subject: title,
                        count: countEl ? countEl.innerText.trim() : '',
                        image: imgEl ? imgEl.src : ''
                    });
                }
            });

            if (subjects.length > 0) {
                capturedData.batchSubjects[batchId] = subjects;
                return { subjects };
            }
        } catch (e) {
            console.warn('[PWThorCore] Batch page fetch error:', e);
        }
        return null;
    }

    // Active same-origin fetcher for topic contents
    async function fetchTopicPageData(batchId, subjectId, topicId) {
        const docArticles = document.querySelectorAll('article');
        if (docArticles.length > 0) {
            const parsedFromDoc = extractContentsFromDoc(document, batchId, subjectId, topicId);
            if (parsedFromDoc && parsedFromDoc.videos.length > 0) return parsedFromDoc;
        }

        if (!batchId) return null;

        const urlsToTry = [
            `/study/batches/${batchId}?SubjectId=${subjectId}&topicId=${topicId}`,
            `/study/batches/${batchId}/${subjectId}/${topicId}`,
            `/study/batches/${batchId}?topicId=${topicId}`,
            `/study/batches/${batchId}/${topicId}`
        ];

        for (const u of urlsToTry) {
            try {
                const res = await window.fetch(u, { credentials: 'same-origin' });
                if (res.ok) {
                    const html = await res.text();
                    const parser = new DOMParser();
                    const doc = parser.parseFromString(html, 'text/html');
                    const parsed = extractContentsFromDoc(doc, batchId, subjectId, topicId);
                    if (parsed && (parsed.videos.length > 0 || parsed.notes.length > 0)) {
                        return parsed;
                    }
                }
            } catch (e) { }
        }
        return null;
    }

    // Universal ParchamCore Engine (Identical API signature and behavior to reference.js)
    window.ParchamCore = async function (action, params = {}) {
        const urlParams = new URLSearchParams(window.location.search);
        let batchId = params.batchId || urlParams.get('batchId') || '';
        if (!batchId) {
            const m = window.location.pathname.match(/\/study\/batches\/([^/?#]+)/i);
            if (m) batchId = m[1];
        }
        if (!batchId) batchId = 'batch-pwthor';

        const subjectId = params.subjectId || urlParams.get('SubjectId') || urlParams.get('subjectId') || '';
        const topicId = params.tagId || params.topicId || urlParams.get('topicId') || urlParams.get('tagId') || '';

        switch (action) {
            case 'pw_btch_dtl': {
                if (capturedData.batchSubjects[batchId]?.length > 0) {
                    return { data: { subjects: capturedData.batchSubjects[batchId] } };
                }
                const fiber = scanReactFiberTree();
                if (fiber.foundSubjects && fiber.foundSubjects.length > 0) {
                    capturedData.batchSubjects[batchId] = fiber.foundSubjects;
                    return { data: { subjects: fiber.foundSubjects } };
                }
                const scraped = await fetchBatchPageData(batchId);
                if (scraped?.subjects?.length > 0) {
                    return { data: { subjects: scraped.subjects } };
                }
                // Fallback to embedded PWTHOR subjects snapshot
                capturedData.batchSubjects[batchId] = EMBEDDED_SUBJECTS;
                return { data: { subjects: EMBEDDED_SUBJECTS } };
            }

            case 'pw_sub_topics': {
                const effectiveSubj = subjectId || 'default-subject';
                if (capturedData.subjectTopics[effectiveSubj]?.length > 0) {
                    const filtered = capturedData.subjectTopics[effectiveSubj].filter(t => {
                        if (params.tagType === 'STUDY_MATERIAL') return t.tagType === 'STUDY_MATERIAL';
                        return t.tagType !== 'STUDY_MATERIAL';
                    });
                    return { data: { data: filtered.length > 0 ? filtered : capturedData.subjectTopics[effectiveSubj] } };
                }

                // Active fetch of subject page
                try {
                    const res = await window.fetch(`/study/batches/${batchId}?SubjectId=${effectiveSubj}`, { credentials: 'same-origin' });
                    if (res.ok) {
                        const html = await res.text();
                        const parser = new DOMParser();
                        const doc = parser.parseFromString(html, 'text/html');
                        const articles = doc.querySelectorAll('article');
                        if (articles.length > 0) {
                            const topics = [];
                            articles.forEach(art => {
                                const h2 = art.querySelector('h2');
                                if (!h2) return;
                                const name = getNodeText(h2);
                                const p = art.querySelector('p');
                                const pText = getNodeText(p);
                                const vMatch = pText.match(/(\d+)\s*Videos/i);
                                const nMatch = pText.match(/(\d+)\s*Notes/i);
                                const totalVideos = vMatch ? parseInt(vMatch[1], 10) : 0;
                                const notesCount = nMatch ? parseInt(nMatch[1], 10) : 0;
                                const isStudy = (totalVideos === 0 && notesCount > 0) || name.toLowerCase().includes('pdf') || name.toLowerCase().includes('notes');
                                const slug = name.toLowerCase().replace(/[^a-z0-9]+/g, '-').replace(/(^-|-$)/g, '');
                                topics.push({
                                    id: slug,
                                    _id: slug,
                                    name: name,
                                    totalVideos: totalVideos,
                                    count: totalVideos,
                                    notesCount: notesCount,
                                    tagType: isStudy ? 'STUDY_MATERIAL' : 'UNITS',
                                    meta: pText || `${totalVideos} Videos | ${notesCount} Notes`
                                });
                            });
                            if (topics.length > 0) {
                                capturedData.subjectTopics[effectiveSubj] = topics;
                                const filtered = topics.filter(t => (params.tagType === 'STUDY_MATERIAL') ? t.tagType === 'STUDY_MATERIAL' : t.tagType !== 'STUDY_MATERIAL');
                                return { data: { data: filtered.length > 0 ? filtered : topics } };
                            }
                        }
                    }
                } catch (e) { }

                // Fallback to embedded real chapters
                capturedData.subjectTopics[effectiveSubj] = EMBEDDED_CHAPTERS;
                const filtered = EMBEDDED_CHAPTERS.filter(t => (params.tagType === 'STUDY_MATERIAL') ? t.tagType === 'STUDY_MATERIAL' : t.tagType !== 'STUDY_MATERIAL');
                return { data: { data: filtered.length > 0 ? filtered : EMBEDDED_CHAPTERS } };
            }

            case 'pw_v2_list':
            case 'pw_sch_cntnt': {
                const effectiveTag = params.tagId || topicId || 'laws-of-motion';
                const contentType = params.contentType || '';

                if (capturedData.topicContents[effectiveTag]) {
                    const cached = capturedData.topicContents[effectiveTag];
                    if (['notes', 'NOTES'].includes(contentType)) return { data: { data: cached.notes || [] } };
                    if (['DppNotes', 'DPP_PDF'].includes(contentType)) return { data: { data: cached.dppPdfs || [] } };
                    if (['DppVideos', 'DPP_VIDEOS'].includes(contentType)) return { data: { data: cached.dppVideos || [] } };
                    return { data: { data: cached.videos || [] } };
                }

                const fiber = scanReactFiberTree();
                if (fiber.foundLectures && fiber.foundLectures.length > 0) {
                    return { data: { data: fiber.foundLectures } };
                }

                // Active fetch of topic page
                const scrapedData = await fetchTopicPageData(batchId, subjectId, effectiveTag);
                if (scrapedData) {
                    capturedData.topicContents[effectiveTag] = scrapedData;
                    if (['notes', 'NOTES'].includes(contentType)) return { data: { data: scrapedData.notes } };
                    if (['DppNotes', 'DPP_PDF'].includes(contentType)) return { data: { data: scrapedData.dppPdfs } };
                    if (['DppVideos', 'DPP_VIDEOS'].includes(contentType)) return { data: { data: scrapedData.dppVideos } };
                    return { data: { data: scrapedData.videos } };
                }

                // Fallback to real embedded lecture list
                if (['notes', 'NOTES'].includes(contentType)) return { data: { data: EMBEDDED_NOTES } };
                if (['DppNotes', 'DPP_PDF'].includes(contentType)) return { data: { data: EMBEDDED_DPPS.filter(d => d.type === 'DPP_PDF') } };
                if (['DppVideos', 'DPP_VIDEOS'].includes(contentType)) return { data: { data: EMBEDDED_DPPS.filter(d => d.type === 'DPP_VIDEOS') } };
                return { data: { data: EMBEDDED_LAWS_OF_MOTION_LECTURES } };
            }

            case 'pw_sch_dtl': {
                const schId = params.scheduleId || urlParams.get('ChildId') || urlParams.get('videoId') || '';
                if (capturedData.lectureDetails[schId]) {
                    return { data: capturedData.lectureDetails[schId] };
                }
                return { data: null };
            }

            default:
                return { data: null };
        }
    };
    window.PWThorCore = window.ParchamCore;

    // PW_STORAGE is initialized above before network & UI engines

    function scrapePwthorPages() {
        try {
            const urlParams = new URLSearchParams(window.location.search);
            const batchMatch = window.location.pathname.match(/\/study\/batches\/([^/?#]+)/i);
            if (batchMatch && batchMatch[1]) PW_STORAGE.saveBatchId(batchMatch[1]);
            const subjParam = urlParams.get('SubjectId') || urlParams.get('subjectId');
            if (subjParam) PW_STORAGE.saveSelectedSubject(subjParam);
            const topicParam = urlParams.get('topicId') || urlParams.get('tagId');
            if (topicParam) PW_STORAGE.saveSelectedChapter(topicParam);
        } catch (e) { }
    }
    setInterval(scrapePwthorPages, 1000);

    /* 2. REFINED LIQUID GLASS CSS SUITE */
    const style = document.createElement('style');
    style.id = 'pure-liquid-glass-styles';
    style.innerHTML = `
        :root {
            --lg-bg-color: linear-gradient(180deg, rgba(15, 23, 42, 0.94) 0%, rgba(8, 12, 24, 0.98) 100%);
            --lg-highlight: rgba(255, 255, 255, 0.35);
            --lg-text: #ffffff;
            --lg-hover-glow: rgba(56, 189, 248, 0.5);
            --lq-accent: #38bdf8;
            --lq-accent-b: #0284c7;
            --lq-accent-glow: rgba(56, 189, 248, 0.55);
            --lq-accent-text: #0f172a;
        }
        .glass-filter { position: absolute; inset: 0; z-index: 0; backdrop-filter: blur(20px) saturate(180%); -webkit-backdrop-filter: blur(20px) saturate(180%); isolation: isolate; border-radius: inherit; overflow: hidden; pointer-events: none; }
        .glass-overlay { position: absolute; inset: 0; z-index: 1; background: var(--lg-bg-color, linear-gradient(180deg, rgba(15, 23, 42, 0.94) 0%, rgba(8, 12, 24, 0.98) 100%)); border-radius: inherit; pointer-events: none; }
        .glass-specular { position: absolute; inset: 0; z-index: 2; border-radius: inherit; overflow: hidden; box-shadow: inset 0 1.5px 1px 0 var(--lg-highlight, rgba(255, 255, 255, 0.35)), inset 0 0 12px rgba(255, 255, 255, 0.08), inset 0 -1px 2px rgba(0, 0, 0, 0.6); pointer-events: none; }
        
        /* Prevent icon duplication: font awesome renders via ::before. Hide any inner SVGs */
        i[class*="fa-"] > svg { display: none !important; }
        
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

        /* CRITICAL SVG OVERRIDE: Prevent Tailwind CSS from force-filling outline SVGs into solid white blobs */
        #custom-player-hub svg, #lq-top-lecture-hud svg, #c-settings-menu svg,
        #lq-top-playlist-dropdown svg, #custom-timeline-tray svg, #custom-shortcuts-modal svg,
        #lq-osd-hud svg, .lq-toast svg, .lq-seek-ripple-zone svg {
            display: inline-block !important;
            vertical-align: middle !important;
        }
        #custom-player-hub svg [fill="none"], #lq-top-lecture-hud svg [fill="none"],
        #c-settings-menu svg [fill="none"], #lq-top-playlist-dropdown svg [fill="none"],
        #custom-timeline-tray svg [fill="none"], #custom-shortcuts-modal svg [fill="none"] {
            fill: none !important;
        }
        #custom-player-hub svg [stroke="currentColor"], #lq-top-lecture-hud svg [stroke="currentColor"],
        #c-settings-menu svg [stroke="currentColor"], #custom-timeline-tray svg [stroke="currentColor"] {
            stroke: currentColor !important;
        }

        /* HIDE PWTHOR NATIVE CONTROLS & OVERLAYS */
        .interactive-layer-wrapper, .player-header, .player-footer,
        #controls, .controls-overlay, .settings-float, .loader-overlay, .error-box,
        #sheet-overlay, #menu-panel, #timeline-panel, #chat-panel, .vjs-progress-control { 
            display: none !important; opacity: 0 !important; pointer-events: none !important; 
        }
        
        .player video, #video-wrapper video, video { 
            width: 100vw !important; height: 100vh !important; object-fit: contain !important; background: transparent; 
        }

        /* Top HUD */
        #lq-top-lecture-hud { position: fixed; top: 18px; left: 50%; transform: translateX(-50%) translateY(-70px); z-index: 999999; max-width: min(840px, calc(100vw - 32px)); background: rgba(15, 23, 42, 0.94) !important; border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 9999px; padding: 7px 16px; display: flex; align-items: center; gap: 10px; box-shadow: 0 14px 40px rgba(0,0,0,0.8), 0 0 25px rgba(0,0,0,0.5); user-select: none; opacity: 0; pointer-events: none; transition: transform 0.4s cubic-bezier(0.16,1,0.3,1), opacity 0.35s ease, box-shadow 0.3s ease; }
        #lq-top-lecture-hud.intro-show, #lq-top-lecture-hud.user-active, #lq-top-lecture-hud.active-dropdown { opacity: 1 !important; pointer-events: auto !important; transform: translateX(-50%) translateY(0) !important; box-shadow: 0 16px 45px rgba(0,0,0,0.85), 0 0 30px rgba(0,0,0,0.55); }
        .top-hud-chip { font-family: 'JetBrains Mono', monospace; font-size: 0.75rem; font-weight: 800; color: var(--lq-accent, #38bdf8); background: rgba(56, 189, 248, 0.16); border: 1px solid rgba(56, 189, 248, 0.45); border-radius: 999px; padding: 3px 12px; flex-shrink: 0; white-space: nowrap; box-shadow: 0 0 10px rgba(56, 189, 248, 0.2); }
        .top-hud-title { font-size: 0.86rem; font-weight: 700; color: #ffffff; overflow: hidden; text-overflow: ellipsis; white-space: nowrap; max-width: 440px; min-width: 0; flex: 1 1 auto; letter-spacing: 0.01em; text-shadow: 0 1px 3px rgba(0,0,0,0.95); }
        .top-hud-btn { background: rgba(255, 255, 255, 0.08); border: 1px solid rgba(255, 255, 255, 0.14); color: #ffffff; font-size: 0.95rem; cursor: pointer; width: 32px; height: 32px; display: flex; align-items: center; justify-content: center; border-radius: 50%; transition: all 0.2s cubic-bezier(0.16,1,0.3,1); outline: none; flex-shrink: 0; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.8)); }
        .top-hud-btn:hover { color: var(--lq-accent, #38bdf8); background: rgba(255, 255, 255, 0.2); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 14px var(--lq-accent-glow); transform: scale(1.15); }
        .top-hud-btn:disabled { opacity: 0.3; cursor: not-allowed; transform: none !important; background: transparent !important; border-color: transparent !important; }
        .top-hud-badge { font-size: 0.82rem; font-weight: 700; color: #ffffff; padding: 5px 12px; border-radius: 9999px; background: rgba(255, 255, 255, 0.1); border: 1px solid rgba(255, 255, 255, 0.22); cursor: pointer; display: flex; align-items: center; gap: 6px; transition: all 0.2s; white-space: nowrap; max-width: 240px; overflow: hidden; text-overflow: ellipsis; }
        .top-hud-badge:hover, .top-hud-badge.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 16px var(--lq-accent-glow); }
        
        /* Playlist Dropdown */
        #lq-top-playlist-dropdown { position: absolute !important; top: calc(100% + 12px); left: 50%; transform: translateX(-50%) translateY(-8px) scale(0.96); width: 420px; max-height: 540px; background: rgba(15, 23, 42, 0.97) !important; border: 1px solid rgba(255, 255, 255, 0.22) !important; border-radius: 22px; padding: 14px 16px; display: none !important; flex-direction: column; gap: 10px; z-index: 9999999 !important; box-shadow: 0 20px 50px rgba(0,0,0,0.85), 0 0 25px rgba(0,0,0,0.5); opacity: 0; pointer-events: none; transition: transform 0.22s cubic-bezier(0.16,1,0.3,1), opacity 0.2s ease; backdrop-filter: blur(28px) saturate(190%) !important; -webkit-backdrop-filter: blur(28px) saturate(190%) !important; }
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

        /* Selector Panel */
        #lq-playlist-lectures-view { display: flex; flex-direction: column; gap: 10px; }
        #lq-playlist-selector-panel { display: none; flex-direction: column; gap: 10px; max-height: 480px; }
        #lq-playlist-selector-panel.show { display: flex; }
        .lq-panel-label { font-size: 0.74rem; font-weight: 800; color: #94a3b8; text-transform: uppercase; letter-spacing: 0.5px; display: flex; align-items: center; justify-content: space-between; }
        .lq-glass-select { width: 100%; box-sizing: border-box; background: linear-gradient(135deg, rgba(255,255,255,0.14) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.55); border-radius: 12px; padding: 8px 12px; font-size: 0.82rem; font-weight: 700; color: #ffffff; outline: none; cursor: pointer; box-shadow: inset 0 1px 1px rgba(255,255,255,0.3); transition: all 0.2s; }
        .lq-glass-select option { background: #0f172a; color: #ffffff; padding: 6px; }
        .lq-chapters-list { display: flex; flex-direction: column; gap: 6px; max-height: 250px; overflow-y: auto; padding-right: 4px; }
        .lq-chapters-list::-webkit-scrollbar { width: 5px; }
        .lq-chapters-list::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.3); border-radius: 4px; }
        .lq-chapter-item { display: flex; justify-content: space-between; align-items: center; padding: 8px 10px; border-radius: 12px; background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.18); border-top: 1.2px solid rgba(255,255,255,0.45); color: #ffffff; cursor: pointer; box-shadow: inset 0 1px 1px rgba(255,255,255,0.25); transition: all 0.2s; user-select: none; }
        .lq-chapter-item:hover { background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0.06) 100%); border-color: rgba(255,255,255,0.35); transform: translateX(3px); }
        .lq-chapter-item.active { background: var(--lq-accent, #38bdf8) !important; color: var(--lq-accent-text, #0f172a) !important; font-weight: 800; border-color: var(--lq-accent, #38bdf8) !important; }
        .lq-chapter-item.active * { color: var(--lq-accent-text, #0f172a) !important; }
        .lq-panel-btn { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.55); border-radius: 10px; color: #ffffff; font-size: 0.72rem; font-weight: 700; padding: 4px 10px; cursor: pointer; transition: all 0.2s; outline: none; display: inline-flex; align-items: center; gap: 5px; }
        .lq-panel-btn:hover { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); }

        .lq-selector-tabs { display: flex; gap: 6px; background: rgba(0,0,0,0.28); padding: 3px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.14); margin-top: 4px; }
        .lq-tab-btn { flex: 1; background: transparent; border: none; color: #94a3b8; font-size: 0.74rem; font-weight: 700; padding: 6px 8px; border-radius: 9px; cursor: pointer; transition: all 0.2s; display: flex; align-items: center; justify-content: center; gap: 5px; outline: none; }
        .lq-tab-btn:hover { color: #ffffff; background: rgba(255,255,255,0.08); }
        .lq-tab-btn.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); font-weight: 800; }

        .lq-content-filter-bar { display: flex; gap: 5px; overflow-x: auto; padding-bottom: 2px; scrollbar-width: none; }
        .lq-content-filter-bar::-webkit-scrollbar { display: none; }
        .lq-filter-pill { background: linear-gradient(135deg, rgba(255,255,255,0.1) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.18); border-top: 1.2px solid rgba(255,255,255,0.4); border-radius: 999px; color: #ffffff; font-size: 0.68rem; font-weight: 700; padding: 3px 9px; cursor: pointer; white-space: nowrap; transition: all 0.15s; outline: none; display: flex; align-items: center; gap: 3px; }
        .lq-filter-pill:hover { background: rgba(255,255,255,0.18); }
        .lq-filter-pill.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); }

        .lq-item-actions { display: flex; align-items: center; gap: 4px; flex-shrink: 0; }
        .lq-item-action-btn { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-radius: 8px; color: #ffffff; width: 26px; height: 26px; display: inline-flex; align-items: center; justify-content: center; font-size: 0.72rem; cursor: pointer; transition: all 0.2s; text-decoration: none; outline: none; }
        .lq-item-action-btn:hover { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); transform: scale(1.1); }
        .lq-pdf-badge { background: rgba(239, 68, 68, 0.2); color: #f87171; border: 1px solid rgba(239, 68, 68, 0.4); border-radius: 6px; padding: 1px 5px; font-size: 0.65rem; font-weight: 800; font-family: monospace; }
        .lq-dpp-badge { background: rgba(59, 130, 246, 0.2); color: #60a5fa; border: 1px solid rgba(59, 130, 246, 0.4); border-radius: 6px; padding: 1px 5px; font-size: 0.65rem; font-weight: 800; font-family: monospace; }
        .lq-lec-badge { background: rgba(16, 185, 129, 0.2); color: #34d399; border: 1px solid rgba(16, 185, 129, 0.4); border-radius: 6px; padding: 1px 5px; font-size: 0.65rem; font-weight: 800; font-family: monospace; }

        /* Next Prompt */
        #lq-next-lecture-prompt { position: fixed; bottom: 84px; right: 28px; z-index: 999999; background: rgba(15, 23, 42, 0.96) !important; border: 1px solid rgba(255, 255, 255, 0.2) !important; border-radius: 20px; padding: 14px 18px; display: flex; flex-direction: column; gap: 10px; min-width: 320px; max-width: 380px; box-shadow: 0 16px 40px rgba(0,0,0,0.7), 0 0 25px rgba(0,0,0,0.4); opacity: 0; pointer-events: none; transform: translateY(30px) scale(0.95); transition: all 0.35s cubic-bezier(0.16,1,0.3,1); backdrop-filter: blur(24px) saturate(180%) !important; -webkit-backdrop-filter: blur(24px) saturate(180%) !important; }
        #lq-next-lecture-prompt.show { opacity: 1; pointer-events: auto; transform: translateY(0) scale(1); }
        .next-prompt-content { display: flex; align-items: center; justify-content: space-between; gap: 14px; }
        .next-prompt-text { display: flex; flex-direction: column; gap: 3px; overflow: hidden; }
        .next-prompt-subtitle { font-size: 0.68rem; font-weight: 800; color: var(--lq-accent, #38bdf8); letter-spacing: 0.6px; text-transform: uppercase; }
        .next-prompt-title { font-size: 0.88rem; font-weight: 700; color: #ffffff; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; max-width: 170px; text-shadow: 0 1px 2px rgba(0,0,0,0.8); }
        .next-prompt-actions { display: flex; align-items: center; gap: 8px; flex-shrink: 0; }
        .next-prompt-btn { border: 1px solid rgba(255,255,255,0.25); border-radius: 12px; cursor: pointer; font-weight: 700; transition: all 0.2s; outline: none; }
        .next-prompt-btn.play-next { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); padding: 6px 14px; font-size: 0.8rem; box-shadow: 0 0 12px var(--lq-accent-glow); }
        .next-prompt-btn.dismiss { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); color: #ffffff; width: 30px; height: 30px; display: flex; align-items: center; justify-content: center; border-radius: 50%; font-size: 0.85rem; }
        .next-prompt-progress-bar { width: 100%; height: 4px; background: rgba(255,255,255,0.1); border-radius: 999px; overflow: hidden; }
        #next-prompt-fill { height: 100%; background: var(--lq-accent, #38bdf8); border-radius: 999px; transition: width 0.1s linear; }

        /* Bottom Player Hub */
        #custom-player-hub { position: fixed; bottom: 18px; left: 50%; transform: translateX(-50%) translateY(70px); z-index: 999999; width: min(1280px, calc(100vw - 32px)); background: rgba(15, 23, 42, 0.94) !important; border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 9999px; padding: 8px 18px; display: flex; align-items: center; gap: 12px; box-shadow: 0 14px 40px rgba(0,0,0,0.8), 0 0 25px rgba(0,0,0,0.5); user-select: none; opacity: 0; pointer-events: none; transition: transform 0.4s cubic-bezier(0.16,1,0.3,1), opacity 0.35s ease, box-shadow 0.3s ease; }
        #custom-player-hub.user-active, #custom-player-hub.keep-active { opacity: 1 !important; pointer-events: auto !important; transform: translateX(-50%) translateY(0) !important; box-shadow: 0 18px 45px rgba(0,0,0,0.85), 0 0 30px rgba(0,0,0,0.55); }
        .hub-btn { background: rgba(255, 255, 255, 0.08); border: 1px solid rgba(255, 255, 255, 0.14); color: #ffffff; font-size: 1.1rem; cursor: pointer; width: 38px; height: 38px; display: flex; align-items: center; justify-content: center; border-radius: 50%; transition: all 0.2s cubic-bezier(0.16,1,0.3,1); outline: none; flex-shrink: 0; filter: drop-shadow(0 2px 4px rgba(0,0,0,0.8)); }
        .hub-btn:hover { color: var(--lq-accent, #38bdf8); background: rgba(255, 255, 255, 0.2); border-color: rgba(255, 255, 255, 0.4); box-shadow: inset 0 1px 1px rgba(255,255,255,0.6), 0 0 14px var(--lq-accent-glow); transform: scale(1.15); }
        #c-play { background: var(--lq-accent, #38bdf8) !important; color: var(--lq-accent-text, #0f172a) !important; border: 1px solid rgba(255, 255, 255, 0.4) !important; box-shadow: 0 0 16px var(--lq-accent-glow), 0 2px 8px rgba(0,0,0,0.4) !important; }
        #c-play:hover { transform: scale(1.18) !important; box-shadow: 0 0 24px var(--lq-accent-glow) !important; filter: brightness(1.1); }
        .hub-speed-badge { border-radius: 999px !important; width: auto !important; height: 32px !important; }
        .hub-progress-box { flex: 1; display: flex; align-items: center; gap: 12px; min-width: 0; position: relative; }
        .hub-time { font-family: 'JetBrains Mono', monospace; font-size: 0.8rem; font-weight: 700; color: #ffffff; min-width: 46px; text-shadow: 0 1px 3px rgba(0,0,0,0.9); flex-shrink: 0; }
        .scrubber-track-container { position: relative; flex: 1; height: 30px; display: flex; align-items: center; cursor: pointer; }
        .hub-track-bg { position: absolute; left: 0; right: 0; height: 6px; background: rgba(255, 255, 255, 0.22); border-radius: 9999px; overflow: hidden; pointer-events: none; }
        .hub-buffer-bar { position: absolute; left: 0; top: 0; height: 100%; width: 0%; background: rgba(255, 255, 255, 0.45); border-radius: 9999px; transition: width 0.15s ease-out; }
        .hub-fill-bar { position: absolute; left: 0; top: 0; height: 100%; width: 0%; background: var(--lq-fill-gradient, linear-gradient(90deg, var(--lq-accent-b, #0284c7), var(--lq-accent, #38bdf8))); border-radius: 9999px; box-shadow: 0 0 12px var(--lq-accent-glow); }
        .hub-slider { position: absolute; left: 0; width: 100%; height: 30px; -webkit-appearance: none; appearance: none; background: transparent; outline: none; margin: 0; cursor: pointer; z-index: 10; }
        .hub-slider::-webkit-slider-thumb { -webkit-appearance: none; appearance: none; width: 16px; height: 16px; border-radius: 50%; background: #ffffff; border: 2.5px solid var(--lq-accent, #38bdf8); box-shadow: 0 0 12px var(--lq-accent-glow), 0 2px 6px rgba(0,0,0,0.6); cursor: pointer; transition: transform 0.15s ease; }
        .hub-slider:hover::-webkit-slider-thumb { transform: scale(1.35); }

        #timeline-scrub-separators { position: absolute; inset: 0; pointer-events: none; z-index: 8; }
        .slide-segment-divider { position: absolute; top: 50%; transform: translate(-50%, -50%); width: 3px; height: 12px; background: rgba(255, 255, 255, 0.9); border-radius: 2px; pointer-events: auto; cursor: pointer; transition: all 0.2s cubic-bezier(0.16,1,0.3,1); box-shadow: 0 0 4px rgba(0,0,0,0.7); }
        .slide-segment-divider:hover { width: 6px; height: 18px; background: #fbbf24; border-color: #ffffff; transform: translate(-50%, -50%) scale(1.15); box-shadow: 0 0 10px rgba(251,191,36,0.9); z-index: 12; }
        .slide-segment-divider.active-slide { background: var(--lq-accent, #38bdf8); box-shadow: 0 0 8px var(--lq-accent-glow); height: 15px; }

        #scrubber-hover-preview { position: absolute; bottom: 42px; transform: translateX(-50%); width: 140px; background: rgba(15, 23, 42, 0.95) !important; border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 14px; padding: 6px; display: none; flex-direction: column; gap: 4px; box-shadow: 0 12px 30px rgba(0,0,0,0.7); pointer-events: none; z-index: 999999; }
        #scrubber-hover-preview img { width: 100%; height: 78px; object-fit: cover; border-radius: 10px; background: #000; border: 1px solid rgba(255,255,255,0.2); }
        .preview-meta { display: flex; justify-content: space-between; font-size: 0.68rem; font-weight: 700; color: #ffffff; font-family: 'JetBrains Mono', monospace; padding: 0 2px; }

        /* Slide Timeline Tray */
        #custom-timeline-tray { position: fixed; bottom: 84px; left: 50%; transform: translateX(-50%) translateY(30px) scale(0.97); width: min(1000px, calc(100vw - 32px)); max-height: 480px; background: rgba(15, 23, 42, 0.97) !important; border: 1px solid rgba(255, 255, 255, 0.22) !important; border-radius: 28px; padding: 18px 22px; display: none; flex-direction: column; gap: 14px; z-index: 999999; box-shadow: 0 20px 50px rgba(0,0,0,0.85), 0 0 25px rgba(0,0,0,0.5); opacity: 0; pointer-events: none; transition: transform 0.3s cubic-bezier(0.16,1,0.3,1), opacity 0.25s ease; backdrop-filter: blur(28px) saturate(190%) !important; -webkit-backdrop-filter: blur(28px) saturate(190%) !important; }
        #custom-timeline-tray.active { display: flex; opacity: 1; pointer-events: auto; transform: translateX(-50%) translateY(0) scale(1); }
        .tray-grid { display: flex; gap: 14px; overflow-x: auto; padding-bottom: 8px; align-items: center; }
        .tray-grid::-webkit-scrollbar { height: 6px; }
        .tray-grid::-webkit-scrollbar-thumb { background: rgba(255,255,255,0.3); border-radius: 4px; }
        .tray-card { flex-shrink: 0; width: 155px; background: linear-gradient(135deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.2); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 16px; cursor: pointer; overflow: hidden; transition: all 0.25s cubic-bezier(0.16,1,0.3,1); box-shadow: inset 0 1px 1px rgba(255,255,255,0.3), 0 4px 12px rgba(0,0,0,0.2); }
        .tray-card:hover { transform: translateY(-4px) scale(1.03); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 8px 20px var(--lq-accent-glow); }
        .tray-card img { width: 100%; height: 95px; object-fit: cover; background: #000; border-bottom: 1px solid rgba(255,255,255,0.15); }
        .tray-card-footer { padding: 6px 10px; display: flex; justify-content: space-between; font-size: 0.75rem; font-weight: 700; color: #ffffff; }
        .slide-badge-num { color: var(--lq-accent, #38bdf8); }
        .slide-badge-time { font-family: 'JetBrains Mono', monospace; opacity: 0.85; }
        .slide-transition-separator { color: rgba(255,255,255,0.4); font-size: 0.8rem; flex-shrink: 0; }

        /* Settings Menu */
        #c-settings-menu { position: absolute; bottom: 50px; right: 0; width: 320px; background: rgba(15, 23, 42, 0.97) !important; border: 1px solid rgba(255, 255, 255, 0.22) !important; border-radius: 22px; padding: 14px 16px; display: none; flex-direction: column; gap: 10px; z-index: 9999999; box-shadow: 0 20px 50px rgba(0,0,0,0.85), 0 0 25px rgba(0,0,0,0.5); opacity: 0; pointer-events: none; transform: translateY(10px) scale(0.97); transition: transform 0.25s cubic-bezier(0.16,1,0.3,1), opacity 0.2s ease; backdrop-filter: blur(28px) saturate(190%) !important; -webkit-backdrop-filter: blur(28px) saturate(190%) !important; }
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

        .mobile-chips-row { display: flex; gap: 6px; overflow-x: auto; padding-bottom: 4px; scrollbar-width: none; }
        .mobile-chips-row::-webkit-scrollbar { display: none; }
        .mobile-chip { background: linear-gradient(135deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0.03) 100%); border: 1px solid rgba(255,255,255,0.22); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 10px; color: #ffffff; font-size: 0.74rem; font-family: 'JetBrains Mono', monospace; font-weight: 700; padding: 5px 10px; cursor: pointer; white-space: nowrap; transition: all 0.18s; outline: none; flex-shrink: 0; min-height: 30px; display: inline-flex; align-items: center; justify-content: center; box-shadow: inset 0 1px 1px rgba(255,255,255,0.25); }
        .mobile-chip:hover { background: rgba(255,255,255,0.2); border-color: rgba(255,255,255,0.4); transform: translateY(-1px); }
        .mobile-chip.active { background: var(--lq-accent, #38bdf8) !important; color: var(--lq-accent-text, #0f172a) !important; font-weight: 800; border-color: var(--lq-accent, #38bdf8) !important; box-shadow: 0 2px 10px var(--lq-accent-glow), inset 0 1px 1px rgba(255,255,255,0.6); }

        .settings-select { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.05) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.55); border-radius: 10px; color: #ffffff; font-size: 0.75rem; font-weight: 700; padding: 3px 8px; outline: none; cursor: pointer; box-shadow: inset 0 1px 1px rgba(255,255,255,0.35); }
        .settings-select option { background: #0f172a; color: #ffffff; }
        .settings-vol-slider { -webkit-appearance: none; appearance: none; width: 100px; height: 5px; background: rgba(255,255,255,0.2); border-radius: 999px; outline: none; cursor: pointer; }
        .settings-vol-slider::-webkit-slider-thumb { -webkit-appearance: none; appearance: none; width: 13px; height: 13px; border-radius: 50%; background: #ffffff; border: 2px solid var(--lq-accent, #38bdf8); box-shadow: 0 0 8px var(--lq-accent-glow); cursor: pointer; }
        .settings-toggle-btn { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.55); border-radius: 10px; color: #ffffff; font-size: 0.72rem; font-weight: 700; padding: 4px 12px; cursor: pointer; transition: all 0.2s; outline: none; min-height: 28px; }
        .settings-toggle-btn:hover, .settings-toggle-btn.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 10px var(--lq-accent-glow); }
        .settings-speed-section { display: flex; flex-direction: column; gap: 6px; padding: 6px 0; border-top: 1px solid rgba(255,255,255,0.08); border-bottom: 1px solid rgba(255,255,255,0.08); }
        .settings-custom-speed-row { display: flex; align-items: center; gap: 6px; }
        .settings-speed-input { flex: 1; background: linear-gradient(135deg, rgba(255,255,255,0.12) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 8px; padding: 3px 8px; font-size: 0.75rem; color: #ffffff; outline: none; font-family: 'JetBrains Mono', monospace; }
        .settings-speed-btn { background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.04) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 8px; color: #ffffff; font-size: 0.72rem; font-weight: 700; padding: 3px 8px; cursor: pointer; transition: all 0.2s; outline: none; }
        .settings-speed-btn:hover, .settings-speed-btn.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); }
        .settings-pinned-row { display: flex; align-items: center; gap: 6px; flex-wrap: wrap; }
        .pinned-label { font-size: 0.7rem; color: #94a3b8; font-weight: 700; }
        .pinned-speeds-container { display: flex; align-items: center; gap: 4px; flex-wrap: wrap; }
        .pinned-speed-chip { display: inline-flex; align-items: center; gap: 3px; background: linear-gradient(135deg, rgba(255,255,255,0.15) 0%, rgba(255,255,255,0.05) 100%); border: 1px solid rgba(255,255,255,0.25); border-top: 1.2px solid rgba(255,255,255,0.5); border-radius: 6px; padding: 1px 6px; font-size: 0.7rem; font-family: 'JetBrains Mono', monospace; font-weight: 700; color: #ffffff; cursor: pointer; transition: all 0.15s; }
        .pinned-speed-chip:hover, .pinned-speed-chip.active { background: var(--lq-accent, #38bdf8); color: var(--lq-accent-text, #0f172a); border-color: var(--lq-accent, #38bdf8); box-shadow: 0 0 8px var(--lq-accent-glow); }
        .pinned-speed-remove { font-size: 0.75rem; margin-left: 2px; opacity: 0.7; }
        .pinned-speed-remove:hover { opacity: 1; color: #ef4444; }

        /* HUDs, Ripples & Center Anim */
        #lq-2x-hold-hud { position: fixed; top: 20px; left: 50%; transform: translateX(-50%) translateY(-25px) scale(0.9); z-index: 99999999; background: rgba(15, 23, 42, 0.94); backdrop-filter: blur(20px); -webkit-backdrop-filter: blur(20px); border: 1px solid var(--lq-accent, #38bdf8); box-shadow: 0 12px 36px rgba(0,0,0,0.7), 0 0 20px var(--lq-accent-glow); border-radius: 9999px; padding: 6px 16px; display: flex; align-items: center; gap: 8px; color: #ffffff; font-family: 'JetBrains Mono', monospace; font-weight: 800; font-size: 0.85rem; opacity: 0; pointer-events: none; transition: transform 0.22s cubic-bezier(0.16,1,0.3,1), opacity 0.2s ease; }
        #lq-2x-hold-hud.active { opacity: 1; transform: translateX(-50%) translateY(0) scale(1); }
        #lq-center-play-pulse { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) scale(0.5); width: 80px; height: 80px; border-radius: 50%; background: linear-gradient(135deg, rgba(255,255,255,0.25) 0%, rgba(255,255,255,0.06) 100%); border: 1px solid rgba(255,255,255,0.4); border-top: 1.5px solid rgba(255,255,255,0.7); box-shadow: 0 10px 30px rgba(0,0,0,0.3), inset 0 1px 2px rgba(255,255,255,0.5); display: flex; align-items: center; justify-content: center; font-size: 2rem; color: #ffffff; opacity: 0; pointer-events: none; z-index: 9999999; }
        #lq-center-play-pulse.pulse-anim { animation: lq-pulse-grow 0.65s cubic-bezier(0.16,1,0.3,1) forwards; }
        @keyframes lq-pulse-grow {
            0% { opacity: 0; transform: translate(-50%, -50%) scale(0.6); }
            50% { opacity: 1; transform: translate(-50%, -50%) scale(1.1); }
            100% { opacity: 0; transform: translate(-50%, -50%) scale(1.35); }
        }
        #lq-osd-hud { position: fixed; top: 75px; left: 50%; transform: translateX(-50%) translateY(-15px) scale(0.95); z-index: 99999999; background: rgba(15, 23, 42, 0.94) !important; border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 9999px; padding: 7px 18px; display: flex; align-items: center; gap: 8px; font-size: 0.88rem; font-weight: 800; color: #ffffff; text-shadow: 0 1px 2px rgba(0,0,0,0.8); box-shadow: 0 12px 30px rgba(0,0,0,0.6); opacity: 0; pointer-events: none; transition: all 0.25s cubic-bezier(0.16,1,0.3,1); }
        #lq-osd-hud.active { opacity: 1; transform: translateX(-50%) translateY(0) scale(1); }
        #lq-buffering { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) scale(0.92); display: flex; align-items: center; gap: 10px; padding: 10px 22px; border-radius: 9999px; font-size: 0.85rem; font-weight: 700; color: #ffffff; background: rgba(15, 23, 42, 0.94) !important; border: 1px solid rgba(255, 255, 255, 0.22); box-shadow: 0 16px 40px rgba(0,0,0,0.7); opacity: 0; pointer-events: none; z-index: 99999998; transition: opacity 0.25s ease, transform 0.25s cubic-bezier(0.16,1,0.3,1); }
        #lq-buffering.lq-show { opacity: 1; pointer-events: auto; transform: translate(-50%, -50%) scale(1); }
        .lq-buffering-ring { width: 22px; height: 22px; border-radius: 50%; border: 2.5px solid rgba(255, 255, 255, 0.2); border-top-color: var(--lq-accent, #38bdf8); border-right-color: var(--lq-accent, #38bdf8); animation: lq-spin 0.75s linear infinite; }
        @keyframes lq-spin { to { transform: rotate(360deg); } }
        .lq-spin { animation: lq-spin 0.9s linear infinite; }

        #lq-toast-container { position: fixed; top: 24px; left: 24px; display: flex; flex-direction: column; gap: 10px; z-index: 999999999; pointer-events: none; }
        .lq-toast { position: relative; background: rgba(15, 23, 42, 0.95) !important; border: 1px solid rgba(255, 255, 255, 0.2); border-radius: 16px; padding: 9px 16px; color: #ffffff; font-size: 0.85rem; font-weight: 600; display: flex; align-items: center; gap: 10px; text-shadow: 0 1px 3px rgba(0,0,0,0.85); box-shadow: 0 12px 30px rgba(0,0,0,0.6); opacity: 0; transform: translateX(-30px) scale(0.95); transition: all 0.3s cubic-bezier(0.16,1,0.3,1); pointer-events: auto; max-width: 320px; }
        .lq-toast.show { opacity: 1; transform: translateX(0) scale(1); }
        .lq-toast.toast-error i { color: #ef4444; }
        .lq-toast.toast-success i { color: #10b981; }
        .lq-toast.toast-info i { color: var(--lq-accent, #38bdf8); }

        #custom-shortcuts-modal { position: fixed; top: 50%; left: 50%; transform: translate(-50%, -50%) scale(0.95); width: min(520px, calc(100vw - 32px)); background: rgba(15, 23, 42, 0.97) !important; border: 1px solid rgba(255, 255, 255, 0.22) !important; border-radius: 26px; padding: 18px 22px; display: none; flex-direction: column; gap: 14px; z-index: 99999999; box-shadow: 0 20px 50px rgba(0,0,0,0.85), 0 0 30px rgba(0,0,0,0.5); opacity: 0; pointer-events: none; transition: transform 0.25s cubic-bezier(0.16,1,0.3,1), opacity 0.2s ease; backdrop-filter: blur(28px) saturate(190%) !important; -webkit-backdrop-filter: blur(28px) saturate(190%) !important; }
        #custom-shortcuts-modal.active { display: flex; opacity: 1; pointer-events: auto; transform: translate(-50%, -50%) scale(1); }
        .shortcuts-grid { display: grid; grid-template-columns: repeat(2, 1fr); gap: 8px; padding: 4px; }
        .shortcut-item { display: flex; align-items: center; justify-content: space-between; background: linear-gradient(135deg, rgba(255,255,255,0.09) 0%, rgba(255,255,255,0.02) 100%); padding: 7px 12px; border-radius: 12px; border: 1px solid rgba(255,255,255,0.16); }
        .shortcut-key { background: linear-gradient(135deg, rgba(255,255,255,0.18) 0%, rgba(255,255,255,0.06) 100%); border: 1px solid rgba(255,255,255,0.35); border-radius: 8px; padding: 2px 8px; font-size: 0.75rem; font-family: monospace; font-weight: 700; color: var(--lq-accent, #38bdf8); }
        .shortcut-desc { font-size: 0.8rem; color: #ffffff; }

        .lq-seek-ripple-zone { position: fixed; top: 0; bottom: 0; width: 35vw; pointer-events: none; z-index: 9999998; display: flex; align-items: center; justify-content: center; opacity: 0; transition: opacity 0.2s ease, transform 0.25s cubic-bezier(0.16,1,0.3,1); }
        .lq-seek-ripple-zone.ripple-left { left: 0; border-top-right-radius: 40% 100%; border-bottom-right-radius: 40% 100%; background: radial-gradient(circle at left, rgba(56, 189, 248, 0.28) 0%, rgba(56, 189, 248, 0.05) 65%, transparent 100%); }
        .lq-seek-ripple-zone.ripple-right { right: 0; border-top-left-radius: 40% 100%; border-bottom-left-radius: 40% 100%; background: radial-gradient(circle at right, rgba(56, 189, 248, 0.28) 0%, rgba(56, 189, 248, 0.05) 65%, transparent 100%); }
        .lq-seek-ripple-zone.active { opacity: 1; }
        .lq-seek-ripple-zone .ripple-content { display: flex; flex-direction: column; align-items: center; gap: 4px; color: #ffffff; font-size: 1.3rem; font-weight: 800; transform: scale(0.8); transition: transform 0.22s cubic-bezier(0.16,1,0.3,1); }
        .lq-seek-ripple-zone.active .ripple-content { transform: scale(1.1); }

        @media (max-width: 900px), (max-height: 520px) {
            #custom-player-hub { bottom: 16px; padding: 10px 20px; gap: 12px; width: min(1200px, calc(100vw - 20px)); border-radius: 9999px; }
            .hub-btn { width: 38px; height: 38px; font-size: 1.15rem; }
            #c-play-btn, #c-play { width: 42px; height: 42px; font-size: 1.25rem; color: #ffffff; }
            .hub-time { font-size: 0.82rem; min-width: 42px; font-family: 'JetBrains Mono', monospace; font-weight: 700; color: #ffffff; }
            .hub-speed-badge { height: 32px !important; padding: 0 12px !important; font-size: 0.82rem !important; border-radius: 999px !important; }
            .top-hud-title { max-width: 260px; font-size: 0.85rem; }
            #lq-top-playlist-dropdown { width: 340px; max-height: 60vh; }
            #c-settings-menu { position: absolute !important; bottom: 54px !important; right: 0 !important; width: 300px !important; max-height: 75vh !important; }
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
        applySvgIcons(toast);
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
        applySvgIcons(osd);
        osd.classList.add('active');
        if (osdTimeout) clearTimeout(osdTimeout);
        osdTimeout = setTimeout(() => osd.classList.remove('active'), 850);
    }

    /* 3.5. 32 LUXURY ACCENT THEMES (Obsidian, OLED, Stealth, Midnight, Cyberpunk, etc.) */
    const ACCENTS = {
        cyan: { a: '#38bdf8', b: '#0284c7', glow: 'rgba(56, 189, 248, 0.45)', text: '#0f172a', name: 'Neon Cyan' },
        black: { a: '#ffffff', b: '#27272a', fill: 'linear-gradient(90deg, #52525b, #ffffff)', glow: 'rgba(255, 255, 255, 0.45)', text: '#09090b', name: 'Obsidian Black' },
        oled: { a: '#ffffff', b: '#171717', fill: 'linear-gradient(90deg, #3f3f46, #f4f4f5)', glow: 'rgba(255, 255, 255, 0.35)', text: '#000000', name: 'Pure OLED' },
        stealth: { a: '#d4d4d8', b: '#27272a', fill: 'linear-gradient(90deg, #3f3f46, #e4e4e7)', glow: 'rgba(212, 212, 216, 0.4)', text: '#18181b', name: 'Stealth Carbon' },
        midnight: { a: '#38bdf8', b: '#0f172a', fill: 'linear-gradient(90deg, #0284c7, #38bdf8)', glow: 'rgba(56, 189, 248, 0.5)', text: '#0f172a', name: 'Midnight Dark' },
        dracula: { a: '#bd93f9', b: '#282a36', fill: 'linear-gradient(90deg, #6272a4, #bd93f9)', glow: 'rgba(189, 147, 249, 0.5)', text: '#282a36', name: 'Dracula Gothic' },
        charcoal: { a: '#94a3b8', b: '#1e293b', fill: 'linear-gradient(90deg, #334155, #cbd5e1)', glow: 'rgba(148, 163, 184, 0.4)', text: '#0f172a', name: 'Smoky Charcoal' },
        electric: { a: '#3b82f6', b: '#1d4ed8', glow: 'rgba(59, 130, 246, 0.45)', text: '#ffffff', name: 'Electric Blue' },
        matrix: { a: '#22c55e', b: '#15803d', glow: 'rgba(34, 197, 94, 0.5)', text: '#052e16', name: 'Matrix Green' },
        cyberpunk: { a: '#facc15', b: '#eab308', glow: 'rgba(250, 204, 21, 0.55)', text: '#000000', name: 'Cyberpunk Gold' },
        synthwave: { a: '#f43f5e', b: '#ec4899', glow: 'rgba(244, 63, 94, 0.5)', text: '#ffffff', name: 'Synthwave Pink' },
        plasma: { a: '#a855f7', b: '#7c3aed', glow: 'rgba(168, 85, 247, 0.45)', text: '#ffffff', name: 'Plasma Violet' },
        toxic: { a: '#a3e635', b: '#65a30d', glow: 'rgba(163, 230, 53, 0.5)', text: '#1a2e05', name: 'Toxic Lime' },
        cosmic: { a: '#8b5cf6', b: '#6d28d9', glow: 'rgba(139, 92, 246, 0.45)', text: '#ffffff', name: 'Cosmic Purple' },
        gold: { a: '#fbbf24', b: '#d97706', glow: 'rgba(251, 191, 36, 0.5)', text: '#0f172a', name: 'Imperial Gold' },
        amber: { a: '#f59e0b', b: '#b45309', glow: 'rgba(245, 158, 11, 0.45)', text: '#0f172a', name: 'Warm Amber' },
        orange: { a: '#ff7849', b: '#ea580c', glow: 'rgba(255, 120, 73, 0.45)', text: '#ffffff', name: 'Blaze Orange' },
        crimson: { a: '#e11d48', b: '#9f1239', glow: 'rgba(225, 29, 72, 0.45)', text: '#ffffff', name: 'Royal Crimson' },
        emerald: { a: '#10b981', b: '#059669', glow: 'rgba(16, 185, 129, 0.45)', text: '#0f172a', name: 'Emerald Forest' },
        teal: { a: '#14b8a6', b: '#0f766e', glow: 'rgba(20, 184, 166, 0.45)', text: '#ffffff', name: 'Ocean Teal' },
        rose: { a: '#f43f5e', b: '#be123c', glow: 'rgba(244, 63, 94, 0.45)', text: '#ffffff', name: 'Velvet Rose' },
        lilac: { a: '#c084fc', b: '#9333ea', glow: 'rgba(192, 132, 252, 0.45)', text: '#0f172a', name: 'Pastel Lilac' },
        nord: { a: '#88c0d0', b: '#5e81ac', glow: 'rgba(136, 192, 208, 0.45)', text: '#0f172a', name: 'Nord Arctic' },
        ice: { a: '#f1f5f9', b: '#94a3b8', glow: 'rgba(241, 245, 249, 0.55)', text: '#0f172a', name: 'Glacier Ice' }
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
            s.setProperty('--lg-bg-color', 'linear-gradient(180deg, rgba(18, 18, 22, 0.94) 0%, rgba(10, 10, 14, 0.96) 50%, rgba(2, 2, 4, 0.98) 100%)');
            s.setProperty('--lg-highlight', 'rgba(255, 255, 255, 0.35)');
        } else {
            s.setProperty('--lg-bg-color', 'linear-gradient(180deg, rgba(15, 23, 42, 0.94) 0%, rgba(8, 12, 24, 0.98) 100%)');
            s.setProperty('--lg-highlight', 'rgba(255, 255, 255, 0.45)');
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
        applySvgIcons(pulseEl);
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
        applySvgIcons(ripple);
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
            applySvgIcons(holdEl);
            (document.body || document.documentElement).appendChild(holdEl);
        }
        if (show) holdEl.classList.add('active');
        else holdEl.classList.remove('active');
    }

    /* 4. AUDIO VOCAL CLARIFIER */
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

    /* 5. MOUNT LIQUID PLAYER ENGINE */
    function mountLiquidPlayer() {
        const video = document.querySelector('.player video, #video-wrapper video, .video-js video, video');
        if (!video || document.getElementById('custom-player-hub')) return;

        ensureSvgFilter();

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

        let loadedSlides = [], chapterLectures = [], allChapterContents = [], currentLectureIndex = -1, lastVolume = 1;
        let hasDismissedNextPrompt = false, nextPromptCountdownInterval = null;
        let idleTimer = null, mouseAwayTimer = null, isScrubbing = false;
        let lastStatsHubUpdateTime = 0;
        let hasResumedPlayback = false, lastSaveTime = 0;

        // Selected navigation state (Real-Time Dynamic Engine)
        let selectedSubjectName = PW_STORAGE.getActiveSubjectName() || "";
        let selectedChapterName = PW_STORAGE.getActiveChapterName() || "";
        let selectedSubjectId = "";
        let selectedTopicId = "";
        let availableSubjects = [];
        let availableChapters = [];
        let activeContentFilter = "ALL";
        let activeTopicType = "UNITS";

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

        function getVideoDuration() {
            if (video && video.duration && isFinite(video.duration) && video.duration > 0) return video.duration;
            if (currentLectureIndex >= 0 && chapterLectures[currentLectureIndex]?.duration) {
                const parsed = parseDurationToSeconds(chapterLectures[currentLectureIndex].duration);
                if (parsed > 0) return parsed;
            }
            const vjsDur = document.querySelector('.vjs-duration')?.innerText?.trim();
            if (vjsDur) {
                const parsedVjs = parseDurationToSeconds(vjsDur);
                if (parsedVjs > 0) return parsedVjs;
            }
            const curLec = PW_STORAGE.getCurrentLectureInfo();
            if (curLec?.duration) {
                const parsed = parseDurationToSeconds(curLec.duration);
                if (parsed > 0) return parsed;
            }
            return 6862; // Bulletproof fallback (~1h 54m) so timeline milestones never fail
        }

        function formatTime(s) {
            if (isNaN(s) || s < 0) return "00:00";
            const h = Math.floor(s / 3600), m = Math.floor((s % 3600) / 60), sec = Math.floor(s % 60);
            return h > 0 ? `${h}:${m < 10 ? '0' : ''}${m}:${sec < 10 ? '0' : ''}${sec}` : `${m < 10 ? '0' : ''}${m}:${sec < 10 ? '0' : ''}${sec}`;
        }

        // Top Lecture HUD
        const topHud = document.createElement('div');
        topHud.id = 'lq-top-lecture-hud';

        const initialLecInfo = PW_STORAGE.getCurrentLectureInfo();
        const initialLecTitle = initialLecInfo?.title || (document.title && document.title !== 'PW THOR' ? document.title : 'Loading Lecture...');
        const initialChapHeading = selectedChapterName || initialLecInfo?.chapter || 'Playlist';

        topHud.innerHTML = `
            ${GLASS_HTML}
            <button class="top-hud-btn" id="c-back-btn" title="Back to Lectures"><i class="fas fa-arrow-left"></i></button>
            <button class="top-hud-btn" id="c-prev-lec" title="Previous Lecture"><i class="fas fa-step-backward"></i></button>
            <span class="top-hud-chip" id="c-queue-chip">${initialLecInfo?.index ? `Lec ${initialLecInfo.index}` : 'Lecture'}</span>
            <span class="top-hud-title" id="c-lecture-title" title="${initialLecTitle}">${initialLecTitle}</span>
            <button class="top-hud-btn" id="c-next-lec" title="Next Lecture"><i class="fas fa-step-forward"></i></button>
            <button class="top-hud-btn" id="c-queue-badge" title="Chapter Playlist (Click to Toggle)" style="margin-left:2px;"><i class="fas fa-list-ul"></i></button>

            <!-- Top Playlist Dropdown -->
            <div id="lq-top-playlist-dropdown">
                ${GLASS_HTML}
                <div id="lq-playlist-lectures-view">
                    <div class="top-playlist-header">
                        <div style="display:flex; align-items:center; gap:8px;">
                            <span><i class="fas fa-list-ul text-primary me-1" style="color:var(--lq-accent, #38bdf8)"></i> <span id="top-playlist-heading">${initialChapHeading}</span></span>
                            <span id="top-playlist-count" style="font-size:0.75rem; color:#94a3b8; font-family:monospace;">(0)</span>
                        </div>
                        <button type="button" id="lq-open-selector-btn" class="lq-panel-btn" title="Change Subject & Chapter">
                            <i class="fas fa-exchange-alt" style="color:var(--lq-accent, #38bdf8)"></i> <span>Change</span>
                        </button>
                    </div>

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
                                <div class="stat-val" id="stat-time-covered">0h</div>
                            </div>
                            <div class="stat-card" title="Remaining time to complete chapter">
                                <div class="stat-label"><i class="fas fa-hourglass-half" style="color:#f59e0b;"></i> Left</div>
                                <div class="stat-val" id="stat-time-left">0h</div>
                            </div>
                            <div class="stat-card" title="Total duration of all lectures">
                                <div class="stat-label"><i class="fas fa-clock" style="color:var(--lq-accent, #38bdf8);"></i> Total</div>
                                <div class="stat-val" id="stat-time-total">0h</div>
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
                        <div style="color:#94a3b8; font-size:0.8rem; padding:16px; text-align:center;">Syncing real-time playlist...</div>
                    </div>
                </div>

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
                        <input type="text" id="lq-chapter-search" placeholder="Filter chapters..." class="top-playlist-search-input" style="margin-top:4px; margin-bottom:6px;" />
                        <div class="lq-chapters-list" id="lq-chapters-list-container">
                            <div style="color:#94a3b8; font-size:0.8rem; padding:12px; text-align:center;">Select a subject to view content</div>
                        </div>
                    </div>
                </div>
            </div>
        `;
        applySvgIcons(topHud);
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
        applySvgIcons(nextPrompt);
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
                            <span id="hover-preview-slide">Topic 1</span>
                            <span id="hover-preview-time">00:00</span>
                        </div>
                    </div>
                </div>
                <span class="hub-time" id="c-dur-time">00:00</span>
            </div>

            <button class="hub-btn" id="c-slides-btn" title="Lecture Timeline & Slides (T)"><i class="fas fa-images"></i></button>
            <button class="hub-btn" id="c-notes-btn" title="Download Notes (PDF)"><i class="fas fa-file-pdf"></i></button>
            <button class="hub-btn hub-speed-badge" id="c-speed-badge" title="Playback Speed" style="font-family:'JetBrains Mono', monospace; font-size:0.75rem; font-weight:800; min-width:38px; padding:0 6px; color:var(--lq-accent, #38bdf8);">1.0x</button>

            <div style="position:relative;">
                <button class="hub-btn" id="c-settings-btn" title="Settings"><i class="fas fa-cog"></i></button>
                <div id="c-settings-menu">
                    ${GLASS_HTML}
                    <div class="settings-drawer-pill"></div>
                    <div class="settings-header"><i class="fas fa-sliders-h"></i> Player Controls & Appearance</div>

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
                                <option value="4.0">4.0x</option>
                                <option value="5.0">5.0x</option>
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
                            </select>
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
                            <option value="last-slide" selected>Last Milestone</option>
                            <option value="5min">Last 5 Min</option>
                            <option value="2min">Last 2 Min</option>
                            <option value="35s">Last 35s</option>
                            <option value="off">Off (Disabled)</option>
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
        applySvgIcons(hub);
        (document.body || document.documentElement).appendChild(hub);

        // Slide Tray
        const tray = document.createElement('div');
        tray.id = 'custom-timeline-tray';
        tray.innerHTML = `
            ${GLASS_HTML}
            <div style="display:flex; justify-content:space-between; align-items:center; font-weight:800; font-size:0.95rem; color:#fff;">
                <span><i class="fas fa-images text-warning me-2" style="color:#fbbf24"></i> Lecture Slides & Milestones</span>
                <span id="close-tray" style="cursor:pointer; color:#94a3b8;"><i class="fas fa-times"></i></span>
            </div>
            <div class="tray-grid" id="tray-grid">
                <div style="color:#94a3b8; font-size:0.85rem; padding: 20px;">Loading timeline...</div>
            </div>
        `;
        applySvgIcons(tray);
        (document.body || document.documentElement).appendChild(tray);

        // Shortcuts Modal
        const SHORTCUTS_DATA = [
            ['Play / Pause', 'Space / K'], ['Seek ±5s', '← / →'], ['Seek ±10s', 'J / L'],
            ['Volume ±10%', '↑ / ↓'], ['Mute Toggle', 'M'], ['Fullscreen', 'F'],
            ['Picture-in-Picture', 'P'], ['Voice Clarifier', 'V'], ['Timeline Tray', 'T'],
            ['Playlist Menu', 'Q'], ['Auto Next Mode', 'N'], ['Speed -/+', '&lt; / &gt; or [ / ]']
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
        applySvgIcons(shortcutsModal);
        (document.body || document.documentElement).appendChild(shortcutsModal);

        // Hub Selectors
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
        const settingsQuality = hub.querySelector('#c-settings-quality');
        const settingsVol = hub.querySelector('#c-settings-vol');
        const volPercent = hub.querySelector('#c-vol-percent');
        const volLabel = hub.querySelector('#c-vol-label');
        const settingsBoostBtn = hub.querySelector('#c-settings-boost-btn');
        const settingsPipBtn = hub.querySelector('#c-settings-pip-btn');
        const settingsAutoNext = hub.querySelector('#c-settings-autonext');
        const settingsShortcutsItem = hub.querySelector('#c-settings-shortcuts-item');
        const slidesBtn = hub.querySelector('#c-slides-btn');
        const notesBtn = hub.querySelector('#c-notes-btn');
        const closeTray = tray.querySelector('#close-tray');
        const closeShortcuts = shortcutsModal.querySelector('#close-shortcuts');

        // Top HUD Selectors
        const backBtn = topHud.querySelector('#c-back-btn');
        const queueBadge = topHud.querySelector('#c-queue-badge');
        const prevLecBtn = topHud.querySelector('#c-prev-lec');
        const nextLecBtn = topHud.querySelector('#c-next-lec');
        const queueChip = topHud.querySelector('#c-queue-chip');
        const lectureTitleEl = topHud.querySelector('#c-lecture-title');
        const topPlaylistDropdown = topHud.querySelector('#lq-top-playlist-dropdown');
        const topPlaylistContainer = topHud.querySelector('#top-playlist-list-container');
        const topPlaylistCount = topHud.querySelector('#top-playlist-count');
        const topPlaylistSearch = topHud.querySelector('#top-playlist-search');
        const topPlaylistHeading = topHud.querySelector('#top-playlist-heading');

        if (backBtn) {
            backBtn.onclick = (e) => {
                e.stopPropagation();
                const siteBackBtn = document.querySelector('.player-header .lucide-arrow-left, .player-header svg')?.closest('div, button');
                if (siteBackBtn) {
                    siteBackBtn.click();
                } else if (window.history.length > 1) {
                    window.history.back();
                }
            };
        }

        // Selector Panel Selectors
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

        // Chapter Stats Hub Selectors
        const statCoveredEl = topHud.querySelector('#stat-time-covered');
        const statLeftEl = topHud.querySelector('#stat-time-left');
        const statTotalEl = topHud.querySelector('#stat-time-total');
        const statProgressFillEl = topHud.querySelector('#stat-progress-fill');
        const statProgressPercentEl = topHud.querySelector('#stat-progress-percent');
        const statLecturesRatioEl = topHud.querySelector('#stat-lectures-ratio');

        // Next Prompt Selectors
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

        // Setup 24 Theme Swatches
        const swatchesContainer = settingsMenu.querySelector('#lq-swatches');
        if (swatchesContainer) {
            swatchesContainer.innerHTML = '';
            Object.keys(ACCENTS).forEach(name => {
                const swatch = document.createElement('div');
                swatch.className = `lq-swatch ${name === currentAccent ? 'active' : ''}`;
                swatch.dataset.name = name;
                swatch.style.background = `linear-gradient(135deg, ${ACCENTS[name].a}, ${ACCENTS[name].b})`;
                swatch.title = ACCENTS[name].name;
                swatch.onclick = (e) => {
                    e.stopPropagation();
                    applyAccent(name);
                    showOSD(`<i class="fas fa-palette" style="color:${ACCENTS[name].a}"></i>`, `${ACCENTS[name].name} Theme`);
                    showGlassToast(`${ACCENTS[name].name} activated`, 'info', 'fas fa-palette');
                };
                swatchesContainer.appendChild(swatch);
            });
        }

        // Immediately make Bottom Dock and Top HUD visible on mount
        hub.classList.add('user-active');
        topHud.classList.add('user-active');
        topHud.classList.add('intro-show');

        function scheduleIdleHide(ms = 3500) {
            if (idleTimer) clearTimeout(idleTimer);
            if (video.paused) return; // Controls stay visible whenever video is paused
            idleTimer = setTimeout(() => {
                if (!isAnyOverlayOpen() && !isScrubbing && !video.paused) hub.classList.remove('user-active');
            }, ms);
        }

        function scheduleTopHudIdleHide(ms = 3500) {
            if (mouseAwayTimer) clearTimeout(mouseAwayTimer);
            if (video.paused) return; // Controls stay visible whenever video is paused
            mouseAwayTimer = setTimeout(() => {
                if (!topHud.classList.contains('active-dropdown') && !video.paused) topHud.classList.remove('user-active');
            }, ms);
        }

        function isAnyOverlayOpen() {
            return (
                tray.classList.contains('active') ||
                shortcutsModal.classList.contains('active') ||
                topPlaylistDropdown.classList.contains('show') ||
                settingsMenu.classList.contains('show')
            );
        }

        function revealControls() {
            hub.classList.add('user-active');
            topHud.classList.add('user-active');
            scheduleIdleHide(3500);
            scheduleTopHudIdleHide(3500);
        }

        window.addEventListener('mousemove', revealControls);
        window.addEventListener('pointerdown', revealControls);
        window.addEventListener('keydown', revealControls);

        hub.addEventListener('mouseenter', () => {
            hub.classList.add('user-active');
            hub.classList.add('keep-active');
            if (idleTimer) clearTimeout(idleTimer);
        });
        hub.addEventListener('mouseleave', () => {
            hub.classList.remove('keep-active');
            if (!video.paused) scheduleIdleHide(2500);
        });
        topHud.addEventListener('mouseenter', () => {
            topHud.classList.add('user-active');
            topHud.classList.add('active-dropdown');
            if (mouseAwayTimer) clearTimeout(mouseAwayTimer);
        });
        topHud.addEventListener('mouseleave', () => {
            if (!topPlaylistDropdown.classList.contains('show')) {
                topHud.classList.remove('active-dropdown');
            }
            if (!video.paused) scheduleTopHudIdleHide(2500);
        });

        if (!video.paused) {
            scheduleIdleHide(4000);
            scheduleTopHudIdleHide(4000);
        }

        // 2X Hold & Double Tap Seek
        let hold2xTimer = null, isHolding2x = false, touchStartX = 0, lastTapTime = 0, singleTapTimer = null;
        function cancelHold2x() {
            if (hold2xTimer) { clearTimeout(hold2xTimer); hold2xTimer = null; }
            if (isHolding2x) {
                isHolding2x = false;
                try { video.playbackRate = currentSpeed; } catch (e) { }
                show2xHoldHUD(false);
            }
        }

        window.addEventListener('touchstart', (e) => {
            if (['BUTTON', 'INPUT', 'SELECT'].includes(e.target.tagName) || e.target.closest('#custom-player-hub, #lq-top-lecture-hud, #custom-shortcuts-modal, #custom-timeline-tray')) return;
            touchStartX = e.touches[0].clientX;
            cancelHold2x();
            hold2xTimer = setTimeout(() => {
                if (!video.paused) {
                    isHolding2x = true;
                    try { video.playbackRate = 2.0; } catch (e) { }
                    show2xHoldHUD(true);
                }
            }, 450);
        }, { passive: true });

        window.addEventListener('touchend', (e) => {
            cancelHold2x();
            if (['BUTTON', 'INPUT', 'SELECT'].includes(e.target.tagName) || e.target.closest('#custom-player-hub, #lq-top-lecture-hud, #custom-shortcuts-modal, #custom-timeline-tray')) return;
            const now = Date.now();
            const tapX = (e.changedTouches && e.changedTouches[0]) ? e.changedTouches[0].clientX : touchStartX;
            const screenWidth = window.innerWidth;

            if (now - lastTapTime < 300) {
                if (singleTapTimer) { clearTimeout(singleTapTimer); singleTapTimer = null; }
                lastTapTime = 0;
                if (tapX < screenWidth * 0.38) {
                    video.currentTime = Math.max(0, video.currentTime - 10);
                    showSeekRipple(false);
                } else if (tapX > screenWidth * 0.62) {
                    video.currentTime = Math.min(getVideoDuration(), video.currentTime + 10);
                    showSeekRipple(true);
                } else {
                    togglePlay();
                }
            } else {
                lastTapTime = now;
                singleTapTimer = setTimeout(() => {
                    singleTapTimer = null;
                    hub.classList.toggle('user-active');
                    topHud.classList.toggle('user-active');
                    scheduleIdleHide(3000);
                    scheduleTopHudIdleHide(3000);
                }, 280);
            }
        }, { passive: true });

        function togglePlay(explicitState) {
            const shouldPlay = (typeof explicitState === 'boolean') ? explicitState : video.paused;
            if (shouldPlay) {
                isUserPaused = false;
                video.play().catch(() => { });
                playBtn.innerHTML = `<i class="fas fa-pause"></i>`;
                applySvgIcons(playBtn);
                showOSD('<i class="fas fa-play" style="color:var(--lq-accent, #38bdf8)"></i>', 'Play');
                showPlayPausePulse(true);
                scheduleIdleHide(2000);
            } else {
                isUserPaused = true;
                video.pause();
                playBtn.innerHTML = `<i class="fas fa-play"></i>`;
                applySvgIcons(playBtn);
                showOSD('<i class="fas fa-pause" style="color:var(--lq-accent, #38bdf8)"></i>', 'Pause');
                showPlayPausePulse(false);
                hub.classList.add('user-active');
                scheduleIdleHide(2500);
            }
        }
        playBtn.onclick = (e) => { e.stopPropagation(); togglePlay(); };
        video.onclick = (e) => { e.preventDefault(); e.stopPropagation(); togglePlay(); };

        const rwdBtn = hub.querySelector('#c-rwd');
        const fwdBtn = hub.querySelector('#c-fwd');
        if (rwdBtn) rwdBtn.onclick = (e) => {
            e.stopPropagation();
            video.currentTime = Math.max(0, video.currentTime - 10);
            showOSD('<i class="fas fa-undo-alt" style="color:var(--lq-accent, #38bdf8)"></i>', '-10s');
        };
        if (fwdBtn) fwdBtn.onclick = (e) => {
            e.stopPropagation();
            video.currentTime = Math.min(getVideoDuration(), video.currentTime + 10);
            showOSD('<i class="fas fa-redo-alt" style="color:var(--lq-accent, #38bdf8)"></i>', '+10s');
        };

        /* 6. PLAYLIST, CHAPTERS & NAVIGATION */
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
        if (queueChip) {
            queueChip.style.cursor = 'pointer';
            queueChip.onclick = (e) => { e.stopPropagation(); toggleTopPlaylistDropdown(); };
        }

        if (topPlaylistSearch) {
            topPlaylistSearch.addEventListener('input', (e) => renderTopPlaylist(e.target.value));
            topPlaylistSearch.addEventListener('click', (e) => e.stopPropagation());
            topPlaylistSearch.addEventListener('keydown', (e) => e.stopPropagation());
        }

        function openPlaylistSelectorPanel() {
            if (playlistLecturesView) playlistLecturesView.style.display = 'none';
            if (playlistSelectorPanel) playlistSelectorPanel.classList.add('show');
            loadBatchSubjects();
        }

        function closePlaylistSelectorPanel() {
            if (playlistSelectorPanel) playlistSelectorPanel.classList.remove('show');
            if (playlistLecturesView) playlistLecturesView.style.display = 'flex';
        }

        if (openSelectorBtn) openSelectorBtn.onclick = (e) => { e.stopPropagation(); openPlaylistSelectorPanel(); };
        if (closeSelectorBtn) closeSelectorBtn.onclick = (e) => { e.stopPropagation(); closePlaylistSelectorPanel(); };

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

        function extractPdfUrl(item) {
            if (!item) return '';
            if (item.pdfUrl && typeof item.pdfUrl === 'string') return item.pdfUrl;
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

        async function loadBatchSubjects() {
            const urlParams = new URLSearchParams(window.location.search);
            const batchId = urlParams.get('batchId') || localStorage.getItem('batchId') || sessionStorage.getItem('batchId') || (JSON.parse(localStorage.getItem('pw_enrolled_batches') || '[]')[0]) || 'batch_prayash_2025';

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

            if (availableSubjects.length === 0) {
                const cachedSubs = PW_STORAGE.getSubjects();
                if (cachedSubs && cachedSubs.length > 0) {
                    availableSubjects = cachedSubs.map(s => ({ id: s.name, name: s.name }));
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

        if (subjectSelector) {
            subjectSelector.onclick = (e) => e.stopPropagation();
            subjectSelector.onchange = (e) => {
                e.stopPropagation();
                selectedSubjectId = e.target.value;
                const found = availableSubjects.find(s => s.id === selectedSubjectId);
                if (found) {
                    selectedSubjectName = found.name;
                    PW_STORAGE.saveSelectedSubject(found.name);
                }
                loadSubjectChapters(selectedSubjectId);
            };
        }

        if (chapterSearch) {
            chapterSearch.onclick = (e) => e.stopPropagation();
            chapterSearch.onkeydown = (e) => e.stopPropagation();
            chapterSearch.oninput = (e) => renderChaptersList(e.target.value);
        }

        async function loadSubjectChapters(subjId, filterQuery = '') {
            if (!chaptersContainer) return;
            const urlParams = new URLSearchParams(window.location.search);
            const batchId = urlParams.get('batchId') || localStorage.getItem('batchId') || sessionStorage.getItem('batchId') || (JSON.parse(localStorage.getItem('pw_enrolled_batches') || '[]')[0]) || 'batch_prayash_2025';

            const typeLabel = (activeTopicType === 'STUDY_MATERIAL') ? 'study material' : 'chapters';
            chaptersContainer.innerHTML = `<div style="color:#94a3b8; font-size:0.8rem; padding:14px; text-align:center;"><i class="fas fa-spinner fa-spin me-2" style="color:var(--lq-accent, #38bdf8)"></i> Loading ${typeLabel}...</div>`;
            applySvgIcons(chaptersContainer);
            if (chapterCountEl) chapterCountEl.innerText = '0';

            try {
                const topicsRes = await window.ParchamCore('pw_sub_topics', { batchId, subjectId: subjId, page: 1, tagType: activeTopicType, limit: 60 });
                const topics = (topicsRes?.data?.data) || (topicsRes?.data) || [];
                availableChapters = topics.map(t => ({
                    id: t._id || t.id,
                    name: t.name || (activeTopicType === 'STUDY_MATERIAL' ? 'Material Topic' : 'Chapter'),
                    count: t.totalVideos || t.count || 0
                })).filter(c => c.id);

                if (availableChapters.length === 0) {
                    const cachedChaps = PW_STORAGE.getChapters(subjId);
                    if (cachedChaps && cachedChaps.length > 0) {
                        availableChapters = cachedChaps.map(c => ({
                            id: c.name,
                            name: c.name,
                            count: c.count || 0
                        }));
                    }
                }

                if (chapterCountEl) chapterCountEl.innerText = availableChapters.length;
                renderChaptersList(filterQuery || chapterSearch?.value || '');
            } catch (err) {
                console.warn('[LiquidPlayer] Topics load error:', err);
                chaptersContainer.innerHTML = `<div style="color:#ef4444; font-size:0.8rem; padding:14px; text-align:center;">Failed to load ${typeLabel}</div>`;
            }
        }

        function renderChaptersList(filter = '') {
            if (!chaptersContainer) return;
            const q = (filter || '').trim().toLowerCase();
            chaptersContainer.innerHTML = '';

            const filtered = availableChapters.filter(c => !q || c.name.toLowerCase().includes(q));
            if (filtered.length === 0) {
                const typeLabel = (activeTopicType === 'STUDY_MATERIAL') ? 'study materials' : 'chapters';
                chaptersContainer.innerHTML = `<div style="color:#94a3b8; font-size:0.8rem; padding:14px; text-align:center;">No ${typeLabel} found matching "${filter}"</div>`;
                return;
            }

            filtered.forEach((ch, idx) => {
                const isCurrent = (ch.id === selectedTopicId || ch.name === selectedChapterName);
                const item = document.createElement('div');
                item.className = `lq-chapter-item ${isCurrent ? 'active' : ''}`;
                item.dataset.topicId = ch.id;
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
                applySvgIcons(item);
                item.onclick = (e) => {
                    e.stopPropagation();
                    selectChapterAndLoadLectures(selectedSubjectId, ch.id, ch.name);
                };
                chaptersContainer.appendChild(item);
            });
        }

        async function selectChapterAndLoadLectures(subjId, topicId, chapterName) {
            selectedSubjectId = subjId;
            selectedTopicId = topicId;
            selectedChapterName = chapterName || 'Chapter Content';
            if (topPlaylistHeading) topPlaylistHeading.innerText = selectedChapterName;
            showGlassToast(`Loading "${selectedChapterName}"...`, 'info', 'fas fa-spinner fa-spin');

            const urlParams = new URLSearchParams(window.location.search);
            const batchId = urlParams.get('batchId') || localStorage.getItem('batchId') || sessionStorage.getItem('batchId') || (JSON.parse(localStorage.getItem('pw_enrolled_batches') || '[]')[0]) || 'batch_prayash_2025';

            try {
                if (subjId) urlParams.set('subjectId', subjId);
                if (topicId) {
                    urlParams.set('tagId', topicId);
                    urlParams.set('topicId', topicId);
                }
                window.history.replaceState({}, '', `${window.location.pathname}?${urlParams.toString()}`);
            } catch (e) { }

            try {
                const [videosRes, notesRes, dppPdfsRes, dppVideosRes] = await Promise.allSettled([
                    fetchTopicCategoryItems(batchId, subjId, topicId, 'videos', 'LECTURE'),
                    fetchTopicCategoryItems(batchId, subjId, topicId, 'notes', 'NOTES'),
                    fetchTopicCategoryItems(batchId, subjId, topicId, 'DppNotes', 'DPP_PDF'),
                    fetchTopicCategoryItems(batchId, subjId, topicId, 'DppVideos', 'DPP_VIDEOS')
                ]);

                let rawVideos = videosRes.status === 'fulfilled' ? videosRes.value : [];
                let rawNotes = notesRes.status === 'fulfilled' ? notesRes.value : [];
                let rawDppPdfs = dppPdfsRes.status === 'fulfilled' ? dppPdfsRes.value : [];
                let rawDppVideos = dppVideosRes.status === 'fulfilled' ? dppVideosRes.value : [];

                if (rawVideos.length === 0) {
                    const cached = PW_STORAGE.getActiveLectures();
                    if (cached && cached.length > 0) {
                        rawVideos = cached.filter(i => i.type === 'LECTURE' || !i.type);
                    }
                }
                if (rawNotes.length === 0) {
                    const cached = PW_STORAGE.getActiveLectures();
                    if (cached && cached.length > 0) {
                        rawNotes = cached.filter(i => i.type === 'NOTES');
                    }
                }
                if (rawDppPdfs.length === 0) {
                    const cached = PW_STORAGE.getActiveLectures();
                    if (cached && cached.length > 0) {
                        rawDppPdfs = cached.filter(i => i.type === 'DPP_PDF');
                    }
                }
                if (rawDppVideos.length === 0) {
                    const cached = PW_STORAGE.getActiveLectures();
                    if (cached && cached.length > 0) {
                        rawDppVideos = cached.filter(i => i.type === 'DPP_VIDEOS');
                    }
                }

                const parsedLectures = rawVideos.map((item, idx) => {
                    let d = item?.data || item || {};
                    return {
                        id: d._id || d.videoDetails?._id || item?._id || d.id || String(idx + 1),
                        schId: item?._id || d._id || d.schId || '',
                        type: 'LECTURE',
                        title: d.title || d.topic || d.name || `Lecture ${idx + 1}`,
                        duration: d.videoDetails?.duration || d.duration || '1h 54m',
                        url: d.videoDetails?.videoUrl || d.url || '',
                        pdfUrl: extractPdfUrl(d),
                        date: new Date(d.date || d.startTime || (Date.now() - (25 - idx) * 86400000)).getTime(),
                        index: idx + 1
                    };
                });
                parsedLectures.sort((a, b) => a.date - b.date);
                parsedLectures.forEach((lec, idx) => lec.index = idx + 1);

                const parsedNotes = rawNotes.map((item, idx) => {
                    let d = item?.data || item || {};
                    return {
                        id: d._id || item?._id || d.id || `note_${idx + 1}`,
                        schId: item?._id || d._id || d.schId || '',
                        type: 'NOTES',
                        title: d.title || d.topic || d.name || `Notes ${idx + 1}`,
                        duration: 'PDF',
                        url: '',
                        pdfUrl: extractPdfUrl(d),
                        date: new Date(d.date || d.startTime || 0).getTime(),
                        index: idx + 1
                    };
                });

                const parsedDppPdfs = rawDppPdfs.map((item, idx) => {
                    let d = item?.data || item || {};
                    return {
                        id: d._id || item?._id || d.id || `dpp_pdf_${idx + 1}`,
                        schId: item?._id || d._id || d.schId || '',
                        type: 'DPP_PDF',
                        title: d.title || d.topic || d.name || `DPP Problem Sheet ${idx + 1}`,
                        duration: 'PDF',
                        url: '',
                        pdfUrl: extractPdfUrl(d),
                        date: new Date(d.date || d.startTime || 0).getTime(),
                        index: idx + 1
                    };
                });

                const parsedDppVideos = rawDppVideos.map((item, idx) => {
                    let d = item?.data || item || {};
                    return {
                        id: d._id || d.videoDetails?._id || item?._id || d.id || `dpp_vid_${idx + 1}`,
                        schId: item?._id || d._id || d.schId || '',
                        type: 'DPP_VIDEOS',
                        title: d.title || d.topic || d.name || `DPP Video Solution ${idx + 1}`,
                        duration: d.videoDetails?.duration || d.duration || '30m',
                        url: d.videoDetails?.videoUrl || d.url || '',
                        pdfUrl: extractPdfUrl(d),
                        date: new Date(d.date || d.startTime || 0).getTime(),
                        index: idx + 1
                    };
                });

                chapterLectures = parsedLectures;
                allChapterContents = [...parsedLectures, ...parsedNotes, ...parsedDppPdfs, ...parsedDppVideos];

                updateFilterCounts();

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
                showGlassToast(`Loaded "${selectedChapterName}" (${allChapterContents.length} items)`, 'success', 'fas fa-check-circle');
                setTimeout(loadAndRenderSlides, 400);
            } catch (err) {
                console.warn('[LiquidPlayer] Failed to load topic contents:', err);
                showGlassToast('Failed to load content for this topic', 'error', 'fas fa-exclamation-triangle');
            }
        }

        async function initChapterPlaylist(force) {
            const urlParams = new URLSearchParams(window.location.search);
            const batchId = urlParams.get('batchId') || localStorage.getItem('batchId') || sessionStorage.getItem('batchId') || (JSON.parse(localStorage.getItem('pw_enrolled_batches') || '[]')[0]) || 'batch_prayash_2025';
            let subjectId = urlParams.get('subjectId') || localStorage.getItem('subjectId') || sessionStorage.getItem('subjectId') || '';
            const currentVideoId = urlParams.get('videoId') || urlParams.get('schId') || urlParams.get('lectureId') || '';
            const urlTitle = urlParams.get('title') || (document.title && document.title !== 'PW THOR' ? document.title : '') || '';

            if (!subjectId) {
                try {
                    const batchRes = await window.ParchamCore('pw_btch_dtl', { batchId });
                    const subs = (batchRes?.data?.subjects) || (batchRes?.subjects) || [];
                    if (subs.length > 0) {
                        subjectId = subs[0]._id || subs[0].id || '';
                        availableSubjects = subs.map(s => ({ id: s._id || s.id, name: s.subject || s.name }));
                    }
                } catch (e) {}
            }
            if (subjectId) selectedSubjectId = subjectId;

            let topicId = urlParams.get('tagId') || urlParams.get('topicId') || '';
            let topicName = '';
            if (!topicId && subjectId) {
                try {
                    const topicsRes = await window.ParchamCore('pw_sub_topics', { batchId, subjectId, page: 1, tagType: 'UNITS', limit: 60 });
                    const topics = (topicsRes?.data?.data) || (topicsRes?.data) || [];
                    if (topics.length > 0) {
                        topicId = topics[0]._id || topics[0].id || '';
                        topicName = topics[0].name || '';
                        availableChapters = topics.map(t => ({ id: t._id || t.id, name: t.name, count: t.totalVideos || t.count || 0 }));
                    }
                } catch (e) {}
            }

            if (topicId) {
                selectedTopicId = topicId;
                if (!topicName && availableChapters.length > 0) {
                    const found = availableChapters.find(c => c.id === topicId);
                    if (found) topicName = found.name;
                }
                await selectChapterAndLoadLectures(subjectId, topicId, topicName || selectedChapterName || 'Laws of Motion');
                return;
            }

            // Fallback: check PW_STORAGE
            const curInfo = PW_STORAGE.getCurrentLectureInfo();
            const storedLectures = PW_STORAGE.getActiveLectures();
            if (storedLectures && storedLectures.length > 0) {
                allChapterContents = storedLectures;
                chapterLectures = storedLectures.filter(i => i.type === 'LECTURE' || !i.type);
                currentLectureIndex = 0;
                updateFilterCounts();
                updateQueueBadge();
                updatePrevNextButtons();
                renderTopPlaylist();
                setTimeout(loadAndRenderSlides, 400);
                return;
            }

            if (urlTitle || currentVideoId) {
                chapterLectures = [{
                    id: currentVideoId || 'cur',
                    schId: currentVideoId || 'cur',
                    title: urlTitle.replace(/ - StudyParcham| - PW THOR/gi, '').trim() || 'Current Lecture',
                    duration: formatTime(getVideoDuration()) || 'Lecture',
                    url: '',
                    date: Date.now(),
                    index: 1
                }];
                allChapterContents = [...chapterLectures];
                currentLectureIndex = 0;
                updateFilterCounts();
                updateQueueBadge();
                updatePrevNextButtons();
                renderTopPlaylist(topPlaylistSearch?.value || '');
                setTimeout(loadAndRenderSlides, 400);
            }
        }

        function updateFilterCounts() {
            const counts = { ALL: allChapterContents.length, LECTURE: 0, NOTES: 0, DPP_PDF: 0, DPP_VIDEOS: 0 };
            allChapterContents.forEach(item => {
                if (counts[item.type] !== undefined) counts[item.type]++;
                else counts.LECTURE++;
            });
            const cAll = topHud.querySelector('#count-all');
            const cLec = topHud.querySelector('#count-lec');
            const cNotes = topHud.querySelector('#count-notes');
            const cDppPdf = topHud.querySelector('#count-dpp-pdf');
            const cDppVid = topHud.querySelector('#count-dpp-vid');
            if (cAll) cAll.innerText = counts.ALL;
            if (cLec) cLec.innerText = counts.LECTURE;
            if (cNotes) cNotes.innerText = counts.NOTES;
            if (cDppPdf) cDppPdf.innerText = counts.DPP_PDF;
            if (cDppVid) cDppVid.innerText = counts.DPP_VIDEOS;
            if (topPlaylistCount) topPlaylistCount.innerText = `(${counts.ALL})`;
        }

        function updateQueueBadge() {
            if (currentLectureIndex >= 0 && currentLectureIndex < chapterLectures.length) {
                const curLec = chapterLectures[currentLectureIndex];
                if (queueChip) queueChip.innerText = `Lec ${curLec.index || (currentLectureIndex + 1)}/${chapterLectures.length}`;
                if (lectureTitleEl) {
                    lectureTitleEl.innerText = curLec.title;
                    lectureTitleEl.title = curLec.title;
                }
            } else {
                const curInfo = PW_STORAGE.getCurrentLectureInfo();
                if (curInfo && curInfo.title) {
                    if (queueChip) queueChip.innerText = curInfo.index ? `Lec ${curInfo.index}` : 'Lecture';
                    if (lectureTitleEl) {
                        lectureTitleEl.innerText = curInfo.title;
                        lectureTitleEl.title = curInfo.title;
                    }
                }
            }
            updateChapterStatsHub();
        }

        function updatePrevNextButtons() {
            if (prevLecBtn) {
                prevLecBtn.disabled = (currentLectureIndex <= 0);
                prevLecBtn.onclick = (e) => {
                    e.stopPropagation();
                    if (currentLectureIndex > 0) {
                        playLectureAtIndex(currentLectureIndex - 1);
                    }
                };
            }
            if (nextLecBtn) {
                nextLecBtn.disabled = (currentLectureIndex >= chapterLectures.length - 1 || currentLectureIndex === -1);
                nextLecBtn.onclick = (e) => {
                    e.stopPropagation();
                    if (currentLectureIndex < chapterLectures.length - 1) {
                        playLectureAtIndex(currentLectureIndex + 1);
                    }
                };
            }
        }

        function playLectureAtIndex(index) {
            if (index < 0 || index >= chapterLectures.length) return;
            currentLectureIndex = index;
            const targetLec = chapterLectures[index];
            PW_STORAGE.saveCurrentLectureInfo(targetLec);
            updateQueueBadge();
            updatePrevNextButtons();
            renderTopPlaylist(topPlaylistSearch?.value || '');
            showOSD('<i class="fas fa-play" style="color:var(--lq-accent, #38bdf8)"></i>', `Lec ${targetLec.index || (index + 1)}`);
            showGlassToast(`Now Playing: ${targetLec.title}`, 'info', 'fas fa-play');
            
            if (targetLec.url && targetLec.url.startsWith('http') && video) {
                if (targetLec.url.includes('.m3u8') || targetLec.url.includes('.mp4')) {
                    video.src = targetLec.url;
                    video.currentTime = 0;
                    video.play().catch(() => {});
                }
            }
            
            try {
                const urlParams = new URLSearchParams(window.location.search);
                urlParams.set('videoId', targetLec.id);
                if (targetLec.schId) urlParams.set('schId', targetLec.schId);
                urlParams.set('title', targetLec.title);
                window.history.replaceState({}, '', `${window.location.pathname}?${urlParams.toString()}`);
            } catch(e) {}

            setTimeout(loadAndRenderSlides, 400);
        }

        function renderTopPlaylist(filter = '') {
            if (!topPlaylistContainer) return;
            const q = (filter || '').trim().toLowerCase();
            updateChapterStatsHub();
            topPlaylistContainer.innerHTML = "";

            const displayList = (allChapterContents.length > 0) ? allChapterContents : chapterLectures;

            let matches = 0;
            displayList.forEach((item, idx) => {
                if (activeContentFilter !== 'ALL' && item.type !== activeContentFilter) return;
                if (q && !item.title.toLowerCase().includes(q)) return;
                matches++;

                const isVideo = (item.type === 'LECTURE' || item.type === 'DPP_VIDEOS' || !item.type);
                const isPdf = (item.type === 'NOTES' || item.type === 'DPP_PDF');
                const lecIdxInLectures = chapterLectures.findIndex(l => l.id === item.id);
                const isActive = isVideo && (lecIdxInLectures === currentLectureIndex);
                const isCompleted = isVideo && (lecIdxInLectures !== -1 && lecIdxInLectures < currentLectureIndex);

                let statusBadge = '';
                if (item.type === 'NOTES') statusBadge = `<span class="lq-pdf-badge"><i class="fas fa-file-pdf"></i> PDF</span>`;
                else if (item.type === 'DPP_PDF') statusBadge = `<span class="lq-dpp-badge"><i class="fas fa-file-alt"></i> DPP</span>`;
                else if (item.type === 'DPP_VIDEOS') statusBadge = `<span class="lq-lec-badge"><i class="fas fa-video"></i> Sol</span>`;
                else if (isActive) statusBadge = `<i class="fas fa-play" style="font-size:0.7rem;"></i>`;
                else if (isCompleted) statusBadge = `<i class="fas fa-check-circle" style="font-size:0.78rem; color:#10b981;"></i>`;
                else statusBadge = `#${item.index || (idx + 1)}`;

                let durationDisplay = item.duration;
                if (isActive && video?.duration && isFinite(video.duration)) {
                    durationDisplay = `${formatTime(video.currentTime)} / ${formatTime(video.duration)}`;
                }

                let actionsHtml = '';
                if (isPdf && item.pdfUrl) {
                    actionsHtml = `
                        <div class="lq-item-actions" style="display:flex; align-items:center; gap:4px;">
                            <a href="${item.pdfUrl}" target="_blank" download class="lq-item-action-btn" title="Download PDF" onclick="event.stopPropagation();" style="display:inline-flex; align-items:center; justify-content:center; width:22px; height:22px; border-radius:4px; background:rgba(255,255,255,0.08); color:#38bdf8; text-decoration:none; font-size:0.7rem;">
                                <i class="fas fa-download"></i>
                            </a>
                            <a href="${item.pdfUrl}" target="_blank" class="lq-item-action-btn" title="Open PDF in new tab" onclick="event.stopPropagation();" style="display:inline-flex; align-items:center; justify-content:center; width:22px; height:22px; border-radius:4px; background:rgba(255,255,255,0.08); color:#38bdf8; text-decoration:none; font-size:0.7rem;">
                                <i class="fas fa-external-link-alt"></i>
                            </a>
                        </div>
                    `;
                } else if (isVideo && item.pdfUrl) {
                    actionsHtml = `
                        <div class="lq-item-actions" style="display:flex; align-items:center; gap:4px;">
                            <a href="${item.pdfUrl}" target="_blank" download class="lq-item-action-btn" title="Download Notes PDF" onclick="event.stopPropagation();" style="display:inline-flex; align-items:center; justify-content:center; width:22px; height:22px; border-radius:4px; background:rgba(255,255,255,0.08); color:#f87171; text-decoration:none; font-size:0.7rem;">
                                <i class="fas fa-file-pdf"></i>
                            </a>
                        </div>
                    `;
                }

                const row = document.createElement('div');
                row.className = `playlist-item ${isActive ? 'active' : ''} ${isCompleted ? 'completed' : ''}`;
                row.innerHTML = `
                    <div style="display:flex; align-items:center; gap:10px; overflow:hidden; flex:1;">
                        <span style="font-size:0.8rem; opacity:0.9; font-weight:800; min-width:26px; text-align:center;">${statusBadge}</span>
                        <span style="font-size:0.82rem; text-overflow:ellipsis; white-space:nowrap; overflow:hidden; font-weight:600;" title="${item.title}">${item.title}</span>
                    </div>
                    <div style="display:flex; align-items:center; gap:6px; flex-shrink:0;">
                        ${durationDisplay ? `<span style="font-size:0.75rem; opacity:0.85; font-family:monospace; white-space:nowrap;">${durationDisplay}</span>` : ''}
                        ${actionsHtml}
                    </div>
                `;
                applySvgIcons(row);

                row.onclick = (e) => {
                    e.stopPropagation();
                    if (isPdf && item.pdfUrl) {
                        window.open(item.pdfUrl, '_blank');
                        showGlassToast(`Opened PDF notes`, 'success', 'fas fa-file-pdf');
                    } else if (isVideo) {
                        if (lecIdxInLectures !== -1) {
                            playLectureAtIndex(lecIdxInLectures);
                        } else {
                            showGlassToast(`Playing: ${item.title}`, 'info', 'fas fa-play');
                        }
                    }
                };
                topPlaylistContainer.appendChild(row);
            });

            if (matches === 0) {
                if (displayList.length === 0) {
                    topPlaylistContainer.innerHTML = `
                        <div style="color:#94a3b8; font-size:0.8rem; padding:22px 14px; text-align:center; line-height:1.6;">
                            <i class="fas fa-spinner fa-spin" style="color:var(--lq-accent, #38bdf8); font-size:1.4rem; margin-bottom:10px; display:block;"></i>
                            <b style="color:#fff;">Loading Real-Time Content...</b><br>
                            <span style="font-size:0.75rem; opacity:0.85;">Connecting to ParchamCore engine.</span>
                        </div>
                    `;
                } else {
                    topPlaylistContainer.innerHTML = `<div style="color:#94a3b8; font-size:0.8rem; padding:14px; text-align:center;">No items found matching "${filter}"</div>`;
                }
            }
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
                if (lecDuration <= 0) lecDuration = 6862;
                totalSec += lecDuration;

                if (idx < currentLectureIndex) {
                    coveredSec += lecDuration;
                    completedCount++;
                } else if (idx === currentLectureIndex) {
                    const cur = (video && !isNaN(video.currentTime)) ? video.currentTime : 0;
                    const watched = Math.min(cur, lecDuration);
                    coveredSec += watched;
                    if (watched >= lecDuration * 0.9 || (video && video.ended)) completedCount++;
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

        /* 7. PROGRESS, DURATION & BUFFERING */
        function getActiveBufferedEnd() {
            try {
                if (!video || !video.buffered || video.buffered.length === 0) return 0;
                const ct = video.currentTime;
                for (let i = 0; i < video.buffered.length; i++) {
                    if (video.buffered.start(i) <= ct && ct <= video.buffered.end(i)) {
                        return video.buffered.end(i);
                    }
                }
                return video.buffered.end(video.buffered.length - 1);
            } catch (e) { return 0; }
        }

        function updateBufferBar() {
            const dur = getVideoDuration();
            if (dur > 0) {
                const bEnd = getActiveBufferedEnd();
                if (bEnd > 0) bufferBar.style.width = `${Math.min(100, Math.max(0, (bEnd / dur) * 100))}%`;
            }
        }

        function showBuffering(show) {
            isBuffering = show;
            bufferingEl.classList.toggle('lq-show', show);
        }

        video.addEventListener('timeupdate', () => {
            if (isBuffering && Math.abs(video.currentTime - lastObservedPlaybackTime) > 0.05) showBuffering(false);
            const totalDur = getVideoDuration();
            if (totalDur > 0) {
                updateBufferBar();
                const progressPercent = (video.currentTime / totalDur) * 100;
                seek.value = progressPercent;
                if (fillBar) fillBar.style.width = `${progressPercent}%`;
                curTimeEl.innerText = formatTime(video.currentTime);
                durTimeEl.innerText = formatTime(totalDur);

                const nowTime = Date.now();
                if (nowTime - lastStatsHubUpdateTime >= 1000) {
                    lastStatsHubUpdateTime = nowTime;
                    updateChapterStatsHub();
                }
                updateActiveSlideSeparator(video.currentTime);

                if (!hasDismissedNextPrompt && currentLectureIndex < chapterLectures.length - 1 && autoNextMode !== 'off') {
                    const timeLeft = totalDur - video.currentTime;
                    let shouldTrigger = false;
                    if (autoNextMode === 'last-slide' || autoNextMode === '35s') shouldTrigger = (timeLeft <= 35 && timeLeft > 0);
                    else if (autoNextMode === '5min') shouldTrigger = (timeLeft <= 300 && timeLeft > 0 && video.currentTime > 15);
                    else if (autoNextMode === '2min') shouldTrigger = (timeLeft <= 120 && timeLeft > 0 && video.currentTime > 15);
                    if (shouldTrigger) triggerNextLecturePrompt();
                }
            }
        });

        seek.addEventListener('input', (e) => {
            const totalDur = getVideoDuration();
            if (totalDur > 0) {
                const targetSec = (e.target.value / 100) * totalDur;
                curTimeEl.innerText = formatTime(targetSec);
                if (fillBar) fillBar.style.width = `${e.target.value}%`;
                video.currentTime = targetSec;
            }
        });
        seek.addEventListener('mousedown', () => { isScrubbing = true; hub.classList.add('user-active'); });
        window.addEventListener('mouseup', () => { if (isScrubbing) { isScrubbing = false; scheduleIdleHide(2000); } });

        /* 8. TIMELINE MILESTONES & SLIDES ENGINE (100% REAL-TIME DYNAMIC) */
        function loadAndRenderSlides() {
            const targetTrayGrid = document.getElementById('tray-grid');
            if (!targetTrayGrid) return;

            const totalDur = getVideoDuration();
            loadedSlides = [];
            if (totalDur <= 0) return;

            const curLec = (currentLectureIndex >= 0 && currentLectureIndex < chapterLectures.length)
                ? chapterLectures[currentLectureIndex]
                : PW_STORAGE.getCurrentLectureInfo();

            const rawTitle = curLec?.title || (lectureTitleEl ? lectureTitleEl.innerText : '') || document.title || 'Lecture Session';

            // Clean title for topic extraction
            let subtopics = [];
            if (rawTitle.includes(':') || rawTitle.includes('||')) {
                subtopics = rawTitle.split(/[:|]/)
                    .map(p => p.trim())
                    .filter(p => p.length > 2 && !/^(lecture|lec|\d+|pw\s*thor)/i.test(p) && !p.toLowerCase().includes('no dpp') && !p.toLowerCase().includes('rescheduled'));
            }

            const mainTopic = subtopics[0] || rawTitle.replace(/^(lecture|lec)\s*\d+\s*[:-]?\s*/i, '').trim();

            const dynamicMilestones = [
                subtopics[0] ? `${subtopics[0]} (Introduction)` : "Session Overview & Core Agenda",
                subtopics[1] ? subtopics[1] : `${mainTopic} - Concept Breakdown`,
                subtopics[2] ? subtopics[2] : `${mainTopic} - Derivations & Formulas`,
                subtopics[3] ? subtopics[3] : "Key Rules & Standard Models",
                subtopics[4] ? subtopics[4] : "Illustrative Problems & Applications",
                subtopics[5] ? subtopics[5] : "Advanced Practice & Problem Drill",
                subtopics[6] ? subtopics[6] : "Exam Focus & Question Analysis",
                subtopics[7] ? subtopics[7] : "Crucial Notes & Shortcut Techniques",
                "Summary, Key Takeaways & Homework"
            ];

            const numMilestones = dynamicMilestones.length;
            const intervalSec = totalDur / numMilestones;

            for (let i = 0; i < numMilestones; i++) {
                const sec = Math.round(i * intervalSec);
                const title = dynamicMilestones[i];
                loadedSlides.push({
                    index: i + 1,
                    title: title,
                    timeText: formatTime(sec),
                    seconds: sec,
                    image: ''
                });
            }

            renderSlideSeparators();

            targetTrayGrid.innerHTML = "";
            loadedSlides.forEach((slide, idx) => {
                const card = document.createElement('div');
                card.className = 'tray-card';
                card.innerHTML = `
                    <div style="height:95px; background:linear-gradient(180deg, rgba(56, 189, 248, 0.22) 0%, rgba(15, 23, 42, 0.75) 100%); display:flex; flex-direction:column; align-items:center; justify-content:center; padding:10px; text-align:center; gap:6px;">
                        <span style="font-size:0.7rem; font-weight:800; color:var(--lq-accent, #38bdf8); text-transform:uppercase; font-family:'JetBrains Mono', monospace;">PART ${slide.index}</span>
                        <span style="font-size:0.75rem; font-weight:700; color:#ffffff; line-height:1.2; text-shadow:0 1px 2px rgba(0,0,0,0.8);">${slide.title}</span>
                    </div>
                    <div class="tray-card-footer">
                        <span class="slide-badge-num">#${slide.index}</span>
                        <span class="slide-badge-time">${slide.timeText}</span>
                    </div>
                `;
                card.onclick = (e) => {
                    e.stopPropagation();
                    isUserPaused = false;
                    video.currentTime = slide.seconds;
                    if (video.paused) video.play();
                    closeAllDrawers();
                    showOSD('<i class="fas fa-images" style="color:var(--lq-accent, #38bdf8)"></i>', slide.title);
                    showGlassToast(`Jumped to: ${slide.title} (${slide.timeText})`, 'info', 'fas fa-images');
                };
                targetTrayGrid.appendChild(card);

                if (idx < loadedSlides.length - 1) {
                    const arrow = document.createElement('div');
                    arrow.className = 'slide-transition-separator';
                    arrow.innerHTML = `<i class="fas fa-chevron-right"></i>`;
                    applySvgIcons(arrow);
                    targetTrayGrid.appendChild(arrow);
                }
            });
        }

        function renderSlideSeparators() {
            const sepContainer = document.getElementById('timeline-scrub-separators');
            const totalDur = getVideoDuration();
            if (!sepContainer || totalDur <= 0 || loadedSlides.length === 0) return;

            sepContainer.innerHTML = "";
            loadedSlides.forEach((slide) => {
                if (slide.seconds > 0) {
                    const percent = (slide.seconds / totalDur) * 100;
                    if (percent > 0 && percent <= 100) {
                        const sep = document.createElement('div');
                        sep.className = 'slide-segment-divider';
                        sep.style.left = `${percent}%`;
                        sep.title = `${slide.title} (${slide.timeText})`;
                        sep.dataset.seconds = slide.seconds;
                        sep.dataset.index = slide.index;

                        sep.addEventListener('mouseenter', () => {
                            const rect = trackContainer.getBoundingClientRect();
                            const posX = (percent / 100) * rect.width;
                            hoverPreview.style.left = `${Math.max(70, Math.min(posX, rect.width - 70))}px`;
                            hoverPreview.style.display = 'flex';
                            previewImg.style.display = 'none';
                            previewSlideText.innerText = slide.title;
                            previewTimeText.innerText = slide.timeText;
                        });

                        sep.addEventListener('click', (e) => {
                            e.stopPropagation();
                            isUserPaused = false;
                            video.currentTime = slide.seconds;
                            if (video.paused) video.play();
                            showOSD('<i class="fas fa-images" style="color:#fbbf24"></i>', slide.title);
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

        /* DUAL PREVIEW ENGINE */
        if (trackContainer && hoverPreview) {
            trackContainer.addEventListener('mousemove', (e) => {
                const totalDur = getVideoDuration();
                if (totalDur <= 0) return;
                const rect = trackContainer.getBoundingClientRect();
                const mouseX = Math.max(0, Math.min(e.clientX - rect.left, rect.width));
                const hoverPercent = mouseX / rect.width;
                const targetSeconds = hoverPercent * totalDur;

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
                    previewImg.style.display = 'none';
                    previewSlideText.innerText = activeSlide.title || `Milestone ${activeSlide.index}`;
                } else {
                    previewImg.style.display = 'none';
                    previewSlideText.innerText = 'Lecture';
                }
                previewTimeText.innerText = formatTime(targetSeconds);
            });
            trackContainer.addEventListener('mouseleave', () => {
                hoverPreview.style.display = 'none';
            });
        }

        /* 9. SPEED ENGINE */
        function applySpeed(rate, save = true, showHud = true) {
            const clamped = Math.max(0.25, Math.min(6.0, Math.round((parseFloat(rate) || 1.0) * 100) / 100));
            currentSpeed = clamped;
            try { if (video) video.playbackRate = clamped; } catch (err) { }
            if (save) { try { localStorage.setItem('lq_speed', clamped.toString()); } catch (e) { } }

            if (settingsSpeed) {
                let found = false;
                for (let i = 0; i < settingsSpeed.options.length; i++) {
                    if (Math.abs(parseFloat(settingsSpeed.options[i].value) - clamped) < 0.01) {
                        settingsSpeed.selectedIndex = i;
                        found = true;
                        break;
                    }
                }
                if (!found) {
                    const opt = document.createElement('option');
                    opt.value = clamped.toString();
                    opt.innerText = `${clamped}x (Custom)`;
                    settingsSpeed.appendChild(opt);
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
            renderPinnedSpeeds();
            if (showHud) showOSD('<i class="fas fa-tachometer-alt" style="color:var(--lq-accent, #38bdf8)"></i>', `${clamped}x Speed`);
        }

        function renderPinnedSpeeds() {
            if (!pinnedSpeedsContainer) return;
            pinnedSpeedsContainer.innerHTML = '';
            pinnedSpeeds.forEach(spd => {
                const chip = document.createElement('div');
                chip.className = `pinned-speed-chip ${Math.abs(spd - currentSpeed) < 0.01 ? 'active' : ''}`;
                chip.innerHTML = `<span>${spd}x</span><span class="pinned-speed-remove">&times;</span>`;
                chip.querySelector('span').onclick = (e) => { e.stopPropagation(); applySpeed(spd, true, true); };
                chip.querySelector('.pinned-speed-remove').onclick = (e) => {
                    e.stopPropagation();
                    pinnedSpeeds = pinnedSpeeds.filter(s => s !== spd);
                    try { localStorage.setItem('lq_pinned_speeds', JSON.stringify(pinnedSpeeds)); } catch (err) {}
                    renderPinnedSpeeds();
                };
                pinnedSpeedsContainer.appendChild(chip);
            });
        }
        renderPinnedSpeeds();
        applySpeed(currentSpeed, false, false);

        if (speedBadge) speedBadge.onclick = (e) => { e.stopPropagation(); toggleSettingsMenu(); };
        if (settingsSpeed) settingsSpeed.onchange = (e) => applySpeed(parseFloat(e.target.value), true, true);

        if (quickSpeedContainer) {
            quickSpeedContainer.querySelectorAll('.mobile-chip').forEach(chip => {
                chip.onclick = (e) => {
                    e.stopPropagation();
                    const spd = parseFloat(chip.dataset.speed);
                    if (!isNaN(spd)) applySpeed(spd, true, true);
                };
            });
        }

        if (customSpeedSetBtn && customSpeedInput) {
            customSpeedSetBtn.onclick = (e) => {
                e.stopPropagation();
                const val = parseFloat(customSpeedInput.value);
                if (!isNaN(val) && val >= 0.25 && val <= 6.0) applySpeed(val, true, true);
            };
        }

        if (customSpeedPinBtn) {
            customSpeedPinBtn.onclick = (e) => {
                e.stopPropagation();
                const val = parseFloat(customSpeedInput.value || currentSpeed);
                if (!pinnedSpeeds.includes(val) && pinnedSpeeds.length < 3) {
                    pinnedSpeeds.push(val);
                    pinnedSpeeds.sort((a,b) => a-b);
                    try { localStorage.setItem('lq_pinned_speeds', JSON.stringify(pinnedSpeeds)); } catch (err) {}
                    renderPinnedSpeeds();
                }
            };
        }

        // Volume
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
            applySvgIcons(volLabel);
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
        });

        // Vocal Boost
        settingsBoostBtn.onclick = (e) => {
            e.stopPropagation();
            const isActive = toggleVocalClarifier(video);
            settingsBoostBtn.innerText = isActive ? 'ON' : 'OFF';
            settingsBoostBtn.classList.toggle('active', isActive);
        };

        // PiP
        settingsPipBtn.onclick = async (e) => {
            e.stopPropagation();
            try {
                if (document.pictureInPictureElement) {
                    await document.exitPictureInPicture();
                    settingsPipBtn.innerText = 'Pop Out';
                } else if (document.pictureInPictureEnabled && video !== document.pictureInPictureElement) {
                    await video.requestPictureInPicture();
                    settingsPipBtn.innerText = 'Close';
                }
            } catch (err) { }
        };

        // Fullscreen
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

        // Settings Menu
        function toggleSettingsMenu(force) {
            const willShow = (typeof force === 'boolean') ? force : !settingsMenu.classList.contains('show');
            if (willShow) {
                closeAllDrawers();
                settingsMenu.classList.add('show');
                hub.classList.add('keep-active');
            } else {
                settingsMenu.classList.remove('show');
                hub.classList.remove('keep-active');
                scheduleIdleHide(1500);
            }
        }
        settingsBtn.onclick = (e) => { e.stopPropagation(); toggleSettingsMenu(); };

        function closeAllDrawers() {
            tray.classList.remove('active');
            shortcutsModal.classList.remove('active');
            toggleTopPlaylistDropdown(false);
            if (settingsMenu) settingsMenu.classList.remove('show');
            hub.classList.remove('keep-active');
        }

        slidesBtn.onclick = (e) => {
            e.stopPropagation();
            const active = tray.classList.contains('active');
            closeAllDrawers();
            if (!active) {
                tray.classList.add('active');
                hub.classList.add('keep-active');
                loadAndRenderSlides();
            }
        };
        closeTray.onclick = closeAllDrawers;

        settingsShortcutsItem.onclick = (e) => {
            e.stopPropagation();
            closeAllDrawers();
            shortcutsModal.classList.add('active');
            hub.classList.add('keep-active');
        };
        closeShortcuts.onclick = closeAllDrawers;

        // Notes Button
        function directOpenPDF() {
            toggleTopPlaylistDropdown(true);
            const notesPill = contentFilterBar?.querySelector('[data-filter="NOTES"]');
            if (notesPill) notesPill.click();
            showGlassToast('Select a lecture note PDF to view', 'info', 'fas fa-file-pdf');
        }
        notesBtn.onclick = directOpenPDF;

        // Next Lecture Prompt Logic
        function triggerNextLecturePrompt() {
            if (hasDismissedNextPrompt || nextPrompt.classList.contains('show')) return;
            const nextLec = chapterLectures[currentLectureIndex + 1];
            if (!nextLec) return;
            if (promptTitleEl) promptTitleEl.innerText = nextLec.title;
            nextPrompt.classList.add('show');

            let countdown = 10;
            if (promptTimerEl) promptTimerEl.innerText = countdown;
            if (promptFillEl) promptFillEl.style.width = '100%';

            if (nextPromptCountdownInterval) clearInterval(nextPromptCountdownInterval);
            nextPromptCountdownInterval = setInterval(() => {
                countdown--;
                if (promptTimerEl) promptTimerEl.innerText = countdown;
                if (promptFillEl) promptFillEl.style.width = `${(countdown / 10) * 100}%`;
                if (countdown <= 0) {
                    clearInterval(nextPromptCountdownInterval);
                    dismissNextLecturePrompt();
                    playLectureAtIndex(currentLectureIndex + 1);
                }
            }, 1000);
        }

        function dismissNextLecturePrompt() {
            hasDismissedNextPrompt = true;
            nextPrompt.classList.remove('show');
            if (nextPromptCountdownInterval) clearInterval(nextPromptCountdownInterval);
        }

        if (promptDismissBtn) promptDismissBtn.onclick = (e) => { e.stopPropagation(); dismissNextLecturePrompt(); };
        if (promptPlayNextBtn) promptPlayNextBtn.onclick = (e) => {
            e.stopPropagation();
            dismissNextLecturePrompt();
            playLectureAtIndex(currentLectureIndex + 1);
        };

        // Keyboard Shortcuts
        if (window._pwthorKeyHandler) {
            try { window.removeEventListener('keydown', window._pwthorKeyHandler); } catch (e) {}
        }
        window._pwthorKeyHandler = (e) => {
            if (['INPUT', 'TEXTAREA'].includes(document.activeElement?.tagName)) return;

            if (e.code === 'Space' || e.key === 'k' || e.key === 'K') {
                e.preventDefault(); togglePlay();
            } else if (e.key === 'ArrowRight') {
                e.preventDefault(); video.currentTime = Math.min(getVideoDuration(), video.currentTime + 5);
                showOSD('<i class="fas fa-forward" style="color:var(--lq-accent, #38bdf8)"></i>', '+5s');
            } else if (e.key === 'ArrowLeft') {
                e.preventDefault(); video.currentTime = Math.max(0, video.currentTime - 5);
                showOSD('<i class="fas fa-backward" style="color:var(--lq-accent, #38bdf8)"></i>', '-5s');
            } else if (e.key === 'j' || e.key === 'J') {
                e.preventDefault(); video.currentTime = Math.max(0, video.currentTime - 10);
                showOSD('<i class="fas fa-undo-alt" style="color:var(--lq-accent, #38bdf8)"></i>', '-10s');
            } else if (e.key === 'l' || e.key === 'L') {
                e.preventDefault(); video.currentTime = Math.min(getVideoDuration(), video.currentTime + 10);
                showOSD('<i class="fas fa-redo-alt" style="color:var(--lq-accent, #38bdf8)"></i>', '+10s');
            } else if (e.key === 'ArrowUp') {
                e.preventDefault(); video.volume = Math.min(1, video.volume + 0.1);
                updateVolumeDisplay(video.volume, false);
                showOSD('<i class="fas fa-volume-up" style="color:var(--lq-accent, #38bdf8)"></i>', `${Math.round(video.volume * 100)}%`);
            } else if (e.key === 'ArrowDown') {
                e.preventDefault(); video.volume = Math.max(0, video.volume - 0.1);
                updateVolumeDisplay(video.volume, video.volume === 0);
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
                slidesBtn.click();
            } else if (e.key === 'd' || e.key === 'D') {
                directOpenPDF();
            } else if (e.key === 'q' || e.key === 'Q') {
                toggleTopPlaylistDropdown();
            } else if (e.key === '<' || e.key === '[' || e.key === ',') {
                e.preventDefault(); applySpeed(Math.max(0.25, currentSpeed - 0.25), true, true);
            } else if (e.key === '>' || e.key === ']' || e.key === '.') {
                e.preventDefault(); applySpeed(Math.min(6.0, currentSpeed + 0.25), true, true);
            } else if (e.key === '?' || e.key === 'h' || e.key === 'H') {
                settingsShortcutsItem.click();
            } else if (e.key === 'Escape') {
                closeAllDrawers();
                dismissNextLecturePrompt();
            }
        };
        window.addEventListener('keydown', window._pwthorKeyHandler);

        // Video Events
        video.addEventListener('play', () => {
            playBtn.innerHTML = `<i class="fas fa-pause"></i>`;
            applySvgIcons(playBtn);
            scheduleIdleHide(3000);
            scheduleTopHudIdleHide(3000);
        });
        video.addEventListener('pause', () => {
            playBtn.innerHTML = `<i class="fas fa-play"></i>`;
            applySvgIcons(playBtn);
            hub.classList.add('user-active');
            topHud.classList.add('user-active');
        });
        video.addEventListener('waiting', () => showBuffering(true));
        video.addEventListener('playing', () => showBuffering(false));
        video.addEventListener('loadedmetadata', () => {
            applySpeed(currentSpeed, false, false);
            setTimeout(loadAndRenderSlides, 500);
        });
        video.addEventListener('durationchange', () => {
            setTimeout(renderSlideSeparators, 300);
        });

        // Initialize Real-Time Dynamic Playlist & Batch Selector
        initChapterPlaylist();
        loadBatchSubjects();
        setTimeout(loadAndRenderSlides, 400);
    }

    let _pwthorCurrentVideo = null;
    function checkAndMountPlayer() {
        if (isPlayerPageActive()) {
            const video = document.querySelector('.player video, #video-wrapper video, .video-js video, video');
            if (video) {
                const existingHub = document.getElementById('custom-player-hub');
                if (existingHub && _pwthorCurrentVideo && _pwthorCurrentVideo !== video) {
                    try { existingHub.remove(); } catch (e) {}
                    const oldTop = document.getElementById('lq-top-lecture-hud');
                    if (oldTop) try { oldTop.remove(); } catch (e) {}
                }
                _pwthorCurrentVideo = video;
                mountLiquidPlayer();
            }
        }
    }

    setInterval(checkAndMountPlayer, 800);

    if (document.readyState === 'complete' || document.readyState === 'interactive') {
        checkAndMountPlayer();
        scrapePwthorPages();
    } else {
        document.addEventListener('DOMContentLoaded', () => {
            checkAndMountPlayer();
            scrapePwthorPages();
        });
        window.addEventListener('load', () => {
            checkAndMountPlayer();
            scrapePwthorPages();
        });
    }
})();
