/*
 * StockBot front-end. Deliberately thin: it only sends what the user typed to the backend
 * and displays what comes back. All understanding and all data access happens on the server.
 */
(function () {
    'use strict';

    var root = document.getElementById('chatbot');
    if (!root) return;

    var messagesEl = document.getElementById('chatbot-messages');
    var chipsEl = document.getElementById('chatbot-chips');
    var form = document.getElementById('chatbot-form');
    var input = document.getElementById('chatbot-input');
    var sendBtn = document.getElementById('chatbot-send');

    var messageUrl = root.dataset.messageUrl;
    var welcomeUrl = root.dataset.welcomeUrl;
    var csrfToken = root.dataset.csrfToken;
    var csrfHeader = root.dataset.csrfHeader;
    var busy = false;

    function scrollToBottom() {
        messagesEl.scrollTop = messagesEl.scrollHeight;
    }

    function addBubble(role, text, extraClass) {
        var el = document.createElement('div');
        el.className = 'chatbot-msg ' + role + (extraClass ? ' ' + extraClass : '');
        el.textContent = text;   // textContent => never interprets HTML from the server
        messagesEl.appendChild(el);
        scrollToBottom();
        return el;
    }

    function renderTable(table) {
        var wrap = document.createElement('div');
        wrap.className = 'chatbot-table-wrap';
        var t = document.createElement('table');
        t.className = 'chatbot-table';

        var head = document.createElement('tr');
        table.headers.forEach(function (h) {
            var th = document.createElement('th');
            th.textContent = h;
            head.appendChild(th);
        });
        var thead = document.createElement('thead');
        thead.appendChild(head);
        t.appendChild(thead);

        var tbody = document.createElement('tbody');
        table.rows.forEach(function (row) {
            var tr = document.createElement('tr');
            row.forEach(function (cell) {
                var td = document.createElement('td');
                td.textContent = cell;
                tr.appendChild(td);
            });
            tbody.appendChild(tr);
        });
        t.appendChild(tbody);
        wrap.appendChild(t);
        return wrap;
    }

    function renderChips(suggestions) {
        chipsEl.innerHTML = '';
        (suggestions || []).forEach(function (s) {
            var b = document.createElement('button');
            b.type = 'button';
            b.className = 'chatbot-chip';
            b.textContent = s;
            b.addEventListener('click', function () { send(s); });
            chipsEl.appendChild(b);
        });
    }

    function renderBot(data) {
        var bubble = addBubble('bot', data.reply);
        if (data.table && data.table.rows && data.table.rows.length) {
            bubble.appendChild(renderTable(data.table));
            scrollToBottom();
        }
        renderChips(data.suggestions);
    }

    function setBusy(state) {
        busy = state;
        input.disabled = state;
        sendBtn.disabled = state;
        if (!state) input.focus();
    }

    async function request(url, options) {
        var res = await fetch(url, Object.assign({ credentials: 'same-origin' }, options));
        var type = res.headers.get('content-type') || '';
        // A login redirect (expired session) comes back as an HTML page, not JSON.
        if (res.redirected || type.indexOf('application/json') === -1) {
            throw new Error('Your session has expired. Please refresh the page and log in again.');
        }
        var data = await res.json();
        if (!res.ok) {
            throw new Error(data && data.reply ? data.reply : 'Something went wrong. Please try again.');
        }
        return data;
    }

    async function send(text) {
        text = (text || '').trim();
        if (!text || busy) return;

        addBubble('user', text);
        input.value = '';
        chipsEl.innerHTML = '';
        setBusy(true);
        var typing = addBubble('bot', 'StockBot is typing…', 'chatbot-typing');

        try {
            var headers = { 'Content-Type': 'application/json', 'Accept': 'application/json' };
            headers[csrfHeader] = csrfToken;
            var data = await request(messageUrl, {
                method: 'POST',
                headers: headers,
                body: JSON.stringify({ message: text })
            });
            typing.remove();
            renderBot(data);
        } catch (err) {
            typing.remove();
            addBubble('bot', err.message || 'Could not reach the server.', 'error');
        } finally {
            setBusy(false);
        }
    }

    form.addEventListener('submit', function (e) {
        e.preventDefault();
        send(input.value);
    });

    // Opening greeting comes from the backend too (includes a live low-stock alert).
    (async function init() {
        setBusy(true);
        try {
            renderBot(await request(welcomeUrl, { method: 'GET', headers: { 'Accept': 'application/json' } }));
        } catch (err) {
            addBubble('bot', err.message || 'Could not reach the server.', 'error');
        } finally {
            setBusy(false);
        }
    })();
})();
