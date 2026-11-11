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

// Ask before destructive actions: <form data-confirm="Delete X?">
document.addEventListener('submit', e => {
    const msg = e.target.getAttribute('data-confirm');
    if (msg && !window.confirm(msg)) e.preventDefault();
});
