// Static preview helpers (GitHub Pages build).
(function () {
  var ROOT = document.currentScript.getAttribute('data-root');
  document.addEventListener('submit', function (e) {
    var f = e.target;
    e.preventDefault(); e.stopImmediatePropagation();
    if ((f.getAttribute('data-action') || '').indexOf('logout') >= 0) { location.href = ROOT + 'index.html'; return; }
    toast('This is a static preview, so saving is turned off. Run the Java app to use forms.');
  }, true);
  document.addEventListener('change', function (e) { e.stopImmediatePropagation(); }, true);
  function toast(msg) {
    var t = document.getElementById('demo-toast');
    if (!t) { t = document.createElement('div'); t.id = 'demo-toast'; document.body.appendChild(t); }
    t.textContent = msg; t.className = 'show';
    clearTimeout(t._h); t._h = setTimeout(function () { t.className = ''; }, 3200);
  }
  document.addEventListener('DOMContentLoaded', function () {
    var here = location.href.split(/[?#]/)[0];
    document.querySelectorAll('.nav a').forEach(function (a) { if (a.href === here) a.classList.add('active'); });
    var b = document.createElement('div'); b.id = 'demo-badge';
    b.textContent = 'Static preview · read-only demo data';
    document.body.appendChild(b);
  });
})();
