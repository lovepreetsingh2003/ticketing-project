(function () {
    'use strict';

    var SEARCH_URL = '/bin/ticketing/tickets/search';
    var debounceTimer = null;

    function debounce(fn, delay) {
        return function () {
            clearTimeout(debounceTimer);
            debounceTimer = setTimeout(fn, delay);
        };
    }

    function formatDate(ms) {
        if (!ms) return '—';
        return new Date(ms).toLocaleString();
    }

    function statusLabel(status) {
        var labels = {
            OPEN: 'Open',
            IN_PROGRESS: 'In Progress',
            RESOLVED: 'Resolved',
            CLOSED: 'Closed',
            CANCELLED: 'Cancelled'
        };
        return labels[status] || status;
    }

    function priorityLabel(priority) {
        var labels = { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', CRITICAL: 'Critical' };
        return labels[priority] || priority;
    }

    function showError(msg) {
        var el = document.getElementById('ticket-list-error');
        if (el) {
            el.textContent = msg;
            el.style.display = 'block';
        }
    }

    function hideError() {
        var el = document.getElementById('ticket-list-error');
        if (el) {
            el.style.display = 'none';
            el.textContent = '';
        }
    }

    function buildCard(ticket) {
        var detailUrl = '/content/ticketingApp/us/en/tickets/detail.html?ticketPath=' + encodeURIComponent(ticket.path);
        return '<div class="ticket-card ticket-card--' + (ticket.status || '').toLowerCase().replace('_', '-') + '">' +
            '<div class="ticket-card__header">' +
                '<span class="ticket-card__id">' + (ticket.ticketId || '') + '</span>' +
                '<span class="ticket-card__status status-badge status-badge--' + (ticket.status || '').toLowerCase().replace('_', '-') + '">' +
                    statusLabel(ticket.status) + '</span>' +
                '<span class="ticket-card__priority priority-badge priority-badge--' + (ticket.priority || '').toLowerCase() + '">' +
                    priorityLabel(ticket.priority) + '</span>' +
            '</div>' +
            '<h3 class="ticket-card__title"><a href="' + detailUrl + '">' + escapeHtml(ticket.title || '') + '</a></h3>' +
            '<p class="ticket-card__description">' + escapeHtml((ticket.description || '').substring(0, 150)) +
                (ticket.description && ticket.description.length > 150 ? '...' : '') + '</p>' +
            '<div class="ticket-card__meta">' +
                '<span>Assignee: <strong>' + escapeHtml(ticket.assignee || '—') + '</strong></span>' +
                '<span>Reporter: <strong>' + escapeHtml(ticket.reporter || '—') + '</strong></span>' +
                '<span>Created: ' + formatDate(ticket.created) + '</span>' +
            '</div>' +
        '</div>';
    }

    function escapeHtml(str) {
        return String(str)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function renderTickets(tickets) {
        var container = document.getElementById('ticket-list-container');
        if (!container) return;
        if (!tickets || tickets.length === 0) {
            container.innerHTML = '<div class="cmp-ticket-list__empty">No tickets found. <a href="/content/ticketingApp/us/en/tickets/create.html">Create one?</a></div>';
            return;
        }
        container.innerHTML = tickets.map(buildCard).join('');
    }

    function fetchTickets() {
        var keyword  = (document.getElementById('ticket-keyword')  || {}).value || '';
        var status   = (document.getElementById('ticket-status-filter')   || {}).value || '';
        var priority = (document.getElementById('ticket-priority-filter') || {}).value || '';

        var params = [];
        if (keyword)  params.push('keyword='  + encodeURIComponent(keyword));
        if (status)   params.push('status='   + encodeURIComponent(status));
        if (priority) params.push('priority=' + encodeURIComponent(priority));

        var url = SEARCH_URL + (params.length ? '?' + params.join('&') : '');

        var container = document.getElementById('ticket-list-container');
        if (container) {
            container.innerHTML = '<div class="cmp-ticket-list__loading">Loading tickets...</div>';
        }
        hideError();

        fetch(url, { credentials: 'same-origin' })
            .then(function (res) { return res.json(); })
            .then(function (data) {
                if (data.error) {
                    showError('Error loading tickets: ' + data.error);
                    return;
                }
                renderTickets(data.tickets || []);
            })
            .catch(function (err) {
                showError('Failed to load tickets. Please try again.');
            });
    }

    function init() {
        var root = document.querySelector('[data-cmp-is="ticketList"]');
        if (!root) return;

        var keywordInput = document.getElementById('ticket-keyword');
        var statusFilter = document.getElementById('ticket-status-filter');
        var priorityFilter = document.getElementById('ticket-priority-filter');

        if (keywordInput) {
            keywordInput.addEventListener('input', debounce(fetchTickets, 400));
        }
        if (statusFilter)   statusFilter.addEventListener('change', fetchTickets);
        if (priorityFilter) priorityFilter.addEventListener('change', fetchTickets);

        fetchTickets();
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
}());
