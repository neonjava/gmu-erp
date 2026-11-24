// Dashboard feature search: hides tiles (and empty groups) that don't match.
document.addEventListener('DOMContentLoaded', () => {
    const box = document.getElementById('feature-search');
    if (!box) return;
    box.addEventListener('input', () => {
        const q = box.value.trim().toLowerCase();
        document.querySelectorAll('[data-group]').forEach(group => {
            let visible = 0;
            group.querySelectorAll('.tile').forEach(tile => {
                const match = tile.textContent.toLowerCase().includes(q);
                tile.style.display = match ? '' : 'none';
                if (match) visible++;
            });
            group.style.display = visible ? '' : 'none';
        });
    });
});


// Highlight the current section in the top nav.
document.addEventListener('DOMContentLoaded', () => {
    const path = location.pathname;
    let best = null;
    document.querySelectorAll('.nav a').forEach(a => {
        const href = new URL(a.href).pathname;
        const hit = href === '/' ? path === '/' : (path === href || path.startsWith(href + '/'));
        if (hit && (!best || href.length > new URL(best.href).pathname.length)) best = a;
    });
    if (best) best.classList.add('active');
});

// ---------- Island clock: date, time and a ring showing how much of the college day (09:00-16:00) has passed ----------
(function () {
    const DAY_START = 9 * 60, DAY_END = 16 * 60;
    function tick() {
        const now = new Date();
        const mins = now.getHours() * 60 + now.getMinutes();
        const p = Math.max(0, Math.min(100, (mins - DAY_START) * 100 / (DAY_END - DAY_START)));
        document.querySelectorAll('.clock').forEach(c => {
            c.querySelector('.clock-date').textContent =
                now.toLocaleDateString('en-IN', { weekday: 'short', day: 'numeric', month: 'short' });
            c.querySelector('.clock-time').textContent =
                now.toLocaleTimeString('en-IN', { hour: 'numeric', minute: '2-digit' });
            c.style.setProperty('--p', p);
        });
        updateToday(mins);
    }

    // Today widget: highlight the class running now or the next one, with a live countdown.
    function updateToday(mins) {
        const box = document.querySelector('[data-today]');
        if (!box) return;
        const items = [...box.querySelectorAll('.track li')];
        if (!items.length) return;
        const toMin = t => { const [h, m] = t.split(':').map(Number); return h * 60 + m; };
        const LENGTH = 55;
        let current = null, next = null;
        items.forEach(li => {
            const start = toMin(li.dataset.time);
            li.classList.toggle('done', mins >= start + LENGTH);
            li.classList.remove('now', 'next');
            if (mins >= start && mins < start + LENGTH) current = li;
            else if (!next && start > mins) next = li;
        });
        const bar = box.querySelector('[data-day-bar]');
        if (bar) bar.style.width = Math.max(0, Math.min(100, (mins - DAY_START) * 100 / (DAY_END - DAY_START))) + '%';
        const set = (sel, text) => { const el = box.querySelector(sel); if (el) el.textContent = text; };
        const show = current || next;
        if (!show) {
            set('[data-next-label]', 'Done for today');
            set('[data-next-name]', 'No more classes today');
            set('[data-next-time]', ''); set('[data-next-room]', ''); set('[data-next-in]', '');
            return;
        }
        show.classList.add(current ? 'now' : 'next');
        set('[data-next-label]', current ? 'In class now' : 'Next class');
        set('[data-next-name]', show.dataset.name);
        set('[data-next-time]', show.dataset.time);
        set('[data-next-room]', show.dataset.room);
        const start = toMin(show.dataset.time);
        let note;
        if (current) note = (start + LENGTH - mins) + ' min left';
        else {
            const d = start - mins, h = Math.floor(d / 60), m = d % 60;
            note = 'in ' + (h ? h + ' h ' : '') + m + ' min';
        }
        set('[data-next-in]', note);
    }

    document.addEventListener('DOMContentLoaded', () => { tick(); setInterval(tick, 30000); });
})();

// ---------- Toasts drop out of the island; success hides itself, errors stay until clicked ----------
document.addEventListener('DOMContentLoaded', () => {
    document.querySelectorAll('.toasts .flash').forEach(t => {
        t.addEventListener('click', () => t.classList.add('gone'));
        if (!t.classList.contains('error')) setTimeout(() => t.classList.add('gone'), 4500);
    });
});

// ---------- Login: demo buttons fill in the form ----------
document.addEventListener('click', e => {
    const chip = e.target.closest('.chip[data-user]');
    if (!chip) return;
    document.getElementById('username').value = chip.dataset.user;
    document.getElementById('password').value = chip.dataset.pass;
    document.querySelectorAll('.chip').forEach(c => c.classList.toggle('on', c === chip));
    document.querySelector('.login-box button.primary[type=submit]').focus();
});

// ---------- Confirm sheet: a macOS-style sheet instead of the browser's confirm() ----------
// <form data-confirm="Question?" data-ok="Delete" data-danger="1">
(function () {
    let sheet, pending;
    function build() {
        sheet = document.createElement('dialog');
        sheet.className = 'sheet';
        sheet.innerHTML = '<p></p><div class="form-actions"><button type="button" data-no>Cancel</button>' +
            '<button type="button" class="primary" data-yes>OK</button></div>';
        document.body.appendChild(sheet);
        sheet.querySelector('[data-no]').addEventListener('click', () => sheet.close());
        sheet.querySelector('[data-yes]').addEventListener('click', () => {
            sheet.close();
            if (pending) { pending.dataset.confirmed = '1'; pending.requestSubmit ? pending.requestSubmit() : pending.submit(); }
        });
        sheet.addEventListener('click', e => { if (e.target === sheet) sheet.close(); });
    }
    document.addEventListener('submit', e => {
        const f = e.target, msg = f.getAttribute('data-confirm');
        if (!msg) return;
        if (f.dataset.confirmed) { delete f.dataset.confirmed; return; }
        e.preventDefault();
        if (!window.HTMLDialogElement) { if (window.confirm(msg)) f.submit(); return; }
        if (!sheet) build();
        pending = f;
        sheet.querySelector('p').textContent = msg;
        const yes = sheet.querySelector('[data-yes]');
        yes.textContent = f.getAttribute('data-ok') || 'OK';
        yes.className = f.hasAttribute('data-danger') ? 'danger' : 'primary';
        sheet.showModal();
        yes.focus();
    }, true);
})();

// ---------- Password fields: show/hide, Caps Lock warning, strength meter ----------
document.addEventListener('click', e => {
    const t = e.target.closest('.pw-toggle');
    if (!t) return;
    const input = t.parentElement.querySelector('input');
    const show = input.type === 'password';
    input.type = show ? 'text' : 'password';
    t.textContent = show ? 'Hide' : 'Show';
    t.setAttribute('aria-pressed', show);
    input.focus();
});
['keydown', 'keyup'].forEach(type => document.addEventListener(type, e => {
    if (!e.target.matches || !e.target.matches('#password') || !e.getModifierState) return;
    const caps = e.target.closest('.field').querySelector('.caps');
    if (caps) caps.hidden = !e.getModifierState('CapsLock');
}));
document.addEventListener('input', e => {
    if (!e.target.matches('[data-strength]')) return;
    const v = e.target.value, meter = e.target.parentElement.querySelector('.strength');
    let score = 0;
    if (v.length >= 6) score++;
    if (v.length >= 10) score++;
    if (/[A-Z]/.test(v) && /[a-z]/.test(v)) score++;
    if (/\d/.test(v) && /[^A-Za-z0-9]/.test(v)) score++;
    if (v.length < 6) score = 0;
    const words = ['At least 6 characters', 'Weak, add more characters', 'Okay', 'Strong', 'Very strong'];
    meter.dataset.level = score;
    meter.style.setProperty('--s', v.length < 6 ? Math.min(v.length / 6, .9) : score);
    meter.querySelector('span').textContent = words[score];
});

// ---------- Falling leaves on the sign-in page and the dashboard ----------
// A small canvas animation: a handful of soft maroon and gold leaves drifting down.
// Turned off for reduced motion, paused when the tab is hidden, and the leaf button in the island toggles it.
(function () {
    const KEY = 'gmu-leaves';
    let canvas, ctx, leaves = [], raf = 0, w = 0, h = 0, last = 0;
    const COLORS = ['rgba(198,162,88,.55)', 'rgba(140,74,78,.32)', 'rgba(214,150,120,.45)', 'rgba(220,190,120,.5)'];
    const reduce = window.matchMedia('(prefers-reduced-motion: reduce)');

    function wanted() {
        try { return localStorage.getItem(KEY) !== 'off'; } catch (e) { return true; }
    }
    function leaf(top) {
        return {
            x: Math.random() * w, y: top ? Math.random() * h : -30,
            s: 9 + Math.random() * 10,            // size
            vy: 18 + Math.random() * 22,          // fall speed, px per second
            sway: 20 + Math.random() * 30, phase: Math.random() * 6.28, freq: .4 + Math.random() * .5,
            rot: Math.random() * 6.28, vr: (Math.random() - .5) * 1.2,
            c: COLORS[(Math.random() * COLORS.length) | 0]
        };
    }
    function resize() {
        const dpr = Math.min(window.devicePixelRatio || 1, 2);
        w = innerWidth; h = innerHeight;
        canvas.width = w * dpr; canvas.height = h * dpr;
        canvas.style.width = w + 'px'; canvas.style.height = h + 'px';
        ctx.setTransform(dpr, 0, 0, dpr, 0, 0);
    }
    function draw(l, t) {
        const x = l.x + Math.sin(t * l.freq + l.phase) * l.sway;
        ctx.save();
        ctx.translate(x, l.y);
        ctx.rotate(l.rot + Math.sin(t * l.freq + l.phase) * .6);
        ctx.fillStyle = l.c;
        ctx.beginPath();                       // a simple leaf: two curves and a midrib
        ctx.moveTo(0, -l.s);
        ctx.quadraticCurveTo(l.s * .8, -l.s * .2, 0, l.s);
        ctx.quadraticCurveTo(-l.s * .8, -l.s * .2, 0, -l.s);
        ctx.fill();
        ctx.strokeStyle = 'rgba(255,255,255,.35)';
        ctx.lineWidth = 1;
        ctx.beginPath(); ctx.moveTo(0, -l.s * .8); ctx.lineTo(0, l.s * .9); ctx.stroke();
        ctx.restore();
    }
    function frame(now) {
        const dt = Math.min((now - last) / 1000, .05); last = now;
        const t = now / 1000;
        ctx.clearRect(0, 0, w, h);
        for (const l of leaves) {
            l.y += l.vy * dt; l.rot += l.vr * dt;
            if (l.y > h + 30) Object.assign(l, leaf(false));
            draw(l, t);
        }
        raf = requestAnimationFrame(frame);
    }
    function start() {
        if (raf || reduce.matches) return;
        if (!canvas) {
            canvas = document.createElement('canvas'); canvas.id = 'leaves'; canvas.setAttribute('aria-hidden', 'true');
            document.body.prepend(canvas); ctx = canvas.getContext('2d');
            addEventListener('resize', resize);
        }
        canvas.hidden = false; resize();
        const count = Math.round(Math.min(18, Math.max(8, w / 100)));
        leaves = Array.from({ length: count }, () => leaf(true));
        last = performance.now(); raf = requestAnimationFrame(frame);
    }
    function stop() {
        cancelAnimationFrame(raf); raf = 0;
        if (canvas) { ctx.clearRect(0, 0, w, h); canvas.hidden = true; }
    }

    document.addEventListener('DOMContentLoaded', () => {
        const here = document.body.hasAttribute('data-leaves') || document.querySelector('.hero');
        const btn = document.querySelector('[data-leaf-toggle]');
        if (!here) return;
        if (btn && !reduce.matches) {
            btn.hidden = false;
            const sync = () => { const on = wanted(); btn.classList.toggle('on', on); btn.setAttribute('aria-pressed', on); };
            btn.addEventListener('click', () => {
                const on = !wanted();
                try { localStorage.setItem(KEY, on ? 'on' : 'off'); } catch (e) {}
                on ? start() : stop(); sync();
            });
            sync();
        }
        if (wanted()) start();
        document.addEventListener('visibilitychange', () => {
            if (document.hidden) { cancelAnimationFrame(raf); raf = 0; }
            else if (wanted() && canvas && !canvas.hidden) { last = performance.now(); raf = requestAnimationFrame(frame); }
        });
    });
})();
