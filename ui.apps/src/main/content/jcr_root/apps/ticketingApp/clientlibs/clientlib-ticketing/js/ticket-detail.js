(function () {
    'use strict';

    var DETAIL_URL  = '/bin/ticketing/ticket/detail';
    var STATUS_URL  = '/bin/ticketing/ticket/status';
    var UPDATE_URL  = '/bin/ticketing/ticket/update';
    var COMMENT_URL = '/bin/ticketing/ticket/comment';

    var VALID_TRANSITIONS = {
        OPEN:        [{ status: 'IN_PROGRESS', label: 'Start Progress' }, { status: 'CANCELLED', label: 'Cancel' }],
        IN_PROGRESS: [{ status: 'RESOLVED',    label: 'Resolve' },         { status: 'CANCELLED', label: 'Cancel' }],
        RESOLVED:    [{ status: 'CLOSED',       label: 'Close' }],
        CLOSED:      [],
        CANCELLED:   []
    };

    var currentTicket = null;

    function getParam(name) {
        var search = window.location.search.substring(1);
        var params = search.split('&');
        for (var i = 0; i < params.length; i++) {
            var pair = params[i].split('=');
            if (decodeURIComponent(pair[0]) === name) {
                return decodeURIComponent(pair[1] || '');
            }
        }
        return null;
    }

    function escapeHtml(str) {
        return String(str || '')
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;');
    }

    function formatDate(ms) {
        if (!ms) return '—';
        return new Date(ms).toLocaleString();
    }

    function statusLabel(s) {
        return { OPEN: 'Open', IN_PROGRESS: 'In Progress', RESOLVED: 'Resolved', CLOSED: 'Closed', CANCELLED: 'Cancelled' }[s] || s;
    }

    function priorityLabel(p) {
        return { LOW: 'Low', MEDIUM: 'Medium', HIGH: 'High', CRITICAL: 'Critical' }[p] || p;
    }

    function showBanner(msg, isError) {
        var banner  = document.getElementById('ticket-detail-banner');
        var msgEl   = document.getElementById('ticket-detail-banner-msg');
        if (!banner || !msgEl) return;
        msgEl.textContent = msg;
        banner.className = 'cmp-ticket-detail__banner' + (isError ? ' cmp-ticket-detail__banner--error' : ' cmp-ticket-detail__banner--success');
        banner.style.display = 'block';
    }

    function hideBanner() {
        var banner = document.getElementById('ticket-detail-banner');
        if (banner) banner.style.display = 'none';
    }

    function showError(msg) {
        var el = document.getElementById('ticket-detail-error');
        if (el) { el.textContent = msg; el.style.display = 'block'; }
    }

    function setFieldError(fieldId, msg) {
        var el = document.getElementById(fieldId);
        if (el) { el.textContent = msg; }
    }

    function clearFieldErrors(prefix) {
        ['title', 'description', 'priority', 'assignee'].forEach(function (f) {
            setFieldError(prefix + '-' + f + '-error', '');
        });
    }

    function renderTicket(ticket) {
        currentTicket = ticket;
        document.getElementById('td-ticket-id').textContent  = ticket.ticketId || '';
        var statusBadge = document.getElementById('td-status');
        statusBadge.textContent  = statusLabel(ticket.status);
        statusBadge.className    = 'cmp-ticket-detail__status-badge status-badge status-badge--' +
            (ticket.status || '').toLowerCase().replace('_', '-');
        var priorityBadge = document.getElementById('td-priority');
        priorityBadge.textContent = priorityLabel(ticket.priority);
        priorityBadge.className  = 'cmp-ticket-detail__priority-badge priority-badge priority-badge--' +
            (ticket.priority || '').toLowerCase();
        document.getElementById('td-title').textContent       = ticket.title || '';
        document.getElementById('td-description').textContent = ticket.description || '';
        document.getElementById('td-reporter').textContent    = ticket.reporter || '—';
        document.getElementById('td-assignee').textContent    = ticket.assignee || '—';
        document.getElementById('td-created').textContent     = formatDate(ticket.created);
        document.getElementById('td-updated').textContent     = formatDate(ticket.updated);

        renderTransitionButtons(ticket.status);
        renderComments(ticket.comments || []);

        var editToggle = document.getElementById('td-edit-toggle');
        var isTerminal = ticket.status === 'CLOSED' || ticket.status === 'CANCELLED';
        if (editToggle) editToggle.style.display = isTerminal ? 'none' : '';

        document.getElementById('ticket-detail-loading').style.display = 'none';
        document.getElementById('ticket-detail-content').style.display = 'block';
    }

    function renderTransitionButtons(status) {
        var container = document.getElementById('td-transitions');
        if (!container) return;
        var transitions = VALID_TRANSITIONS[status] || [];
        if (transitions.length === 0) {
            container.innerHTML = '<p class="cmp-ticket-detail__terminal-note">This ticket is in a terminal state.</p>';
            return;
        }
        container.innerHTML = transitions.map(function (t) {
            return '<button class="btn btn--transition btn--transition-' + t.status.toLowerCase().replace('_', '-') + '" ' +
                'data-status="' + t.status + '">' + escapeHtml(t.label) + '</button>';
        }).join(' ');

        container.querySelectorAll('.btn--transition').forEach(function (btn) {
            btn.addEventListener('click', function () {
                handleTransition(btn.getAttribute('data-status'));
            });
        });
    }

    function renderComments(comments) {
        var list = document.getElementById('td-comments-list');
        if (!list) return;
        if (!comments.length) {
            list.innerHTML = '<p class="cmp-ticket-detail__no-comments">No comments yet.</p>';
            return;
        }
        list.innerHTML = comments.map(function (c) {
            return '<div class="ticket-comment">' +
                '<div class="ticket-comment__header">' +
                    '<strong class="ticket-comment__author">' + escapeHtml(c.author || 'Anonymous') + '</strong>' +
                    '<span class="ticket-comment__date">' + formatDate(c.created) + '</span>' +
                '</div>' +
                '<p class="ticket-comment__body">' + escapeHtml(c.body || '') + '</p>' +
            '</div>';
        }).join('');
    }

    function handleTransition(newStatus) {
        if (!currentTicket) return;
        hideBanner();
        var formData = new URLSearchParams();
        formData.append('ticketPath', currentTicket.path);
        formData.append('status', newStatus);

        window.TicketingCsrf.postForm(STATUS_URL, formData.toString())
        .then(function (res) {
            return res.json().then(function (data) { return { status: res.status, data: data }; });
        })
        .then(function (result) {
            if (result.status === 200) {
                currentTicket.status = newStatus;
                renderTransitionButtons(newStatus);
                document.getElementById('td-status').textContent = statusLabel(newStatus);
                document.getElementById('td-status').className =
                    'cmp-ticket-detail__status-badge status-badge status-badge--' + newStatus.toLowerCase().replace('_', '-');
                showBanner('Status updated to ' + statusLabel(newStatus), false);
            } else if (result.status === 409) {
                showBanner('Transition not allowed: ' + (result.data.error || 'Invalid transition'), true);
            } else {
                showBanner('Failed to update status: ' + (result.data.error || 'Unknown error'), true);
            }
        })
        .catch(function () { showBanner('Network error — please try again.', true); });
    }

    function validateUpdateForm() {
        var valid = true;
        clearFieldErrors('update');
        var title       = (document.getElementById('update-title')       || {}).value || '';
        var description = (document.getElementById('update-description') || {}).value || '';
        var priority    = (document.getElementById('update-priority')    || {}).value || '';

        if (!title.trim()) {
            setFieldError('update-title-error', 'Title is required');
            valid = false;
        } else if (title.length > 200) {
            setFieldError('update-title-error', 'Title must not exceed 200 characters');
            valid = false;
        }
        if (!description.trim()) {
            setFieldError('update-description-error', 'Description is required');
            valid = false;
        }
        if (!priority) {
            setFieldError('update-priority-error', 'Priority is required');
            valid = false;
        }
        return valid;
    }

    function handleUpdateSubmit(e) {
        e.preventDefault();
        if (!validateUpdateForm()) return;
        var errorEl   = document.getElementById('update-form-error');
        var successEl = document.getElementById('update-form-success');
        if (errorEl)   { errorEl.style.display = 'none'; }
        if (successEl) { successEl.style.display = 'none'; }

        var formData = new URLSearchParams();
        formData.append('ticketPath',  (document.getElementById('update-ticket-path')  || {}).value || '');
        formData.append('title',       (document.getElementById('update-title')         || {}).value || '');
        formData.append('description', (document.getElementById('update-description')  || {}).value || '');
        formData.append('priority',    (document.getElementById('update-priority')      || {}).value || '');
        formData.append('assignee',    (document.getElementById('update-assignee')      || {}).value || '');

        window.TicketingCsrf.postForm(UPDATE_URL, formData.toString())
        .then(function (res) { return res.json().then(function (d) { return { status: res.status, data: d }; }); })
        .then(function (result) {
            if (result.data.success) {
                if (successEl) { successEl.textContent = 'Ticket updated successfully.'; successEl.style.display = 'block'; }
                document.getElementById('td-title').textContent       = (document.getElementById('update-title') || {}).value || '';
                document.getElementById('td-description').textContent = (document.getElementById('update-description') || {}).value || '';
                document.getElementById('td-assignee').textContent    = (document.getElementById('update-assignee') || {}).value || '—';
                var newPriority = (document.getElementById('update-priority') || {}).value;
                document.getElementById('td-priority').textContent    = priorityLabel(newPriority);
                if (currentTicket) {
                    currentTicket.title       = (document.getElementById('update-title') || {}).value;
                    currentTicket.description = (document.getElementById('update-description') || {}).value;
                    currentTicket.priority    = newPriority;
                    currentTicket.assignee    = (document.getElementById('update-assignee') || {}).value;
                }
                document.getElementById('td-edit-form').style.display = 'none';
            } else {
                if (errorEl) { errorEl.textContent = result.data.error || 'Update failed'; errorEl.style.display = 'block'; }
            }
        })
        .catch(function () {
            if (errorEl) { errorEl.textContent = 'Network error — please try again.'; errorEl.style.display = 'block'; }
        });
    }

    function handleCommentSubmit(e) {
        e.preventDefault();
        var bodyEl    = document.getElementById('comment-body');
        var bodyError = document.getElementById('comment-body-error');
        var errorEl   = document.getElementById('comment-form-error');
        if (bodyError) bodyError.textContent = '';
        if (errorEl)   { errorEl.style.display = 'none'; }

        var body = bodyEl ? bodyEl.value : '';
        if (!body.trim()) {
            if (bodyError) bodyError.textContent = 'Comment body is required';
            return;
        }

        var formData = new URLSearchParams();
        formData.append('ticketPath', currentTicket.path);
        formData.append('author',     (document.getElementById('comment-author') || {}).value || '');
        formData.append('body',       body);

        window.TicketingCsrf.postForm(COMMENT_URL, formData.toString())
        .then(function (res) { return res.json().then(function (d) { return { status: res.status, data: d }; }); })
        .then(function (result) {
            if (result.data.success) {
                if (bodyEl) bodyEl.value = '';
                var author = (document.getElementById('comment-author') || {}).value || 'Anonymous';
                var comments = currentTicket.comments || [];
                comments.push({ author: author, body: body, created: Date.now() });
                currentTicket.comments = comments;
                renderComments(comments);
            } else {
                if (errorEl) { errorEl.textContent = result.data.error || 'Failed to add comment'; errorEl.style.display = 'block'; }
            }
        })
        .catch(function () {
            if (errorEl) { errorEl.textContent = 'Network error — please try again.'; errorEl.style.display = 'block'; }
        });
    }

    function loadTicket(ticketPath) {
        fetch(DETAIL_URL + '?ticketPath=' + encodeURIComponent(ticketPath), { credentials: 'same-origin' })
            .then(function (res) { return res.json().then(function (d) { return { status: res.status, data: d }; }); })
            .then(function (result) {
                if (result.status === 404) {
                    document.getElementById('ticket-detail-loading').style.display = 'none';
                    showError('Ticket not found.');
                    return;
                }
                if (result.data.error) {
                    document.getElementById('ticket-detail-loading').style.display = 'none';
                    showError('Failed to load ticket: ' + result.data.error);
                    return;
                }
                renderTicket(result.data);
            })
            .catch(function () {
                document.getElementById('ticket-detail-loading').style.display = 'none';
                showError('Network error — unable to load ticket.');
            });
    }

    function init() {
        var root = document.querySelector('[data-cmp-is="ticketDetail"]');
        if (!root) return;

        var ticketPath = getParam('ticketPath');
        if (!ticketPath) {
            document.getElementById('ticket-detail-loading').style.display = 'none';
            showError('No ticket specified. Please select a ticket from the list.');
            return;
        }

        loadTicket(ticketPath);

        var bannerClose = document.getElementById('ticket-detail-banner-close');
        if (bannerClose) bannerClose.addEventListener('click', hideBanner);

        var editToggle = document.getElementById('td-edit-toggle');
        var editForm   = document.getElementById('td-edit-form');
        var editCancel = document.getElementById('td-edit-cancel');

        if (editToggle && editForm) {
            editToggle.addEventListener('click', function () {
                if (!currentTicket) return;
                document.getElementById('update-ticket-path').value  = currentTicket.path        || '';
                document.getElementById('update-title').value        = currentTicket.title       || '';
                document.getElementById('update-description').value  = currentTicket.description || '';
                document.getElementById('update-priority').value     = currentTicket.priority    || '';
                document.getElementById('update-assignee').value     = currentTicket.assignee    || '';
                editForm.style.display = 'block';
                editToggle.style.display = 'none';
            });
        }
        if (editCancel && editForm) {
            editCancel.addEventListener('click', function () {
                editForm.style.display   = 'none';
                editToggle.style.display = '';
            });
        }

        var updateForm = document.getElementById('ticket-update-form');
        if (updateForm) updateForm.addEventListener('submit', handleUpdateSubmit);

        var commentForm = document.getElementById('ticket-comment-form');
        if (commentForm) commentForm.addEventListener('submit', handleCommentSubmit);
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
}());
