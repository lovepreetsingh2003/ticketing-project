(function () {
    'use strict';

    var CREATE_URL = '/bin/ticketing/ticket/create';
    var VALID_PRIORITIES = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];

    function getEl(id) { return document.getElementById(id); }

    function setError(fieldId, message) {
        var el = getEl(fieldId);
        if (el) el.textContent = message;
    }

    function clearAllErrors() {
        ['title', 'description', 'priority', 'assignee', 'reporter'].forEach(function (f) {
            setError('ticket-' + f + '-error', '');
        });
        var submitError = getEl('ticket-form-error');
        if (submitError) submitError.style.display = 'none';
    }

    function markInvalid(inputId) {
        var el = getEl(inputId);
        if (el) el.classList.add('cmp-form__input--invalid');
    }

    function clearInvalid(inputId) {
        var el = getEl(inputId);
        if (el) el.classList.remove('cmp-form__input--invalid');
    }

    function validateForm() {
        clearAllErrors();
        var valid = true;
        var fields = ['title', 'description', 'priority', 'assignee', 'reporter'];
        fields.forEach(function (f) { clearInvalid('ticket-' + f); });

        var title = (getEl('ticket-title') || {}).value || '';
        if (!title.trim()) {
            setError('ticket-title-error', 'Title is required');
            markInvalid('ticket-title');
            valid = false;
        } else if (title.length > 200) {
            setError('ticket-title-error', 'Title must not exceed 200 characters');
            markInvalid('ticket-title');
            valid = false;
        }

        var description = (getEl('ticket-description') || {}).value || '';
        if (!description.trim()) {
            setError('ticket-description-error', 'Description is required');
            markInvalid('ticket-description');
            valid = false;
        } else if (description.length > 5000) {
            setError('ticket-description-error', 'Description must not exceed 5000 characters');
            markInvalid('ticket-description');
            valid = false;
        }

        var priority = (getEl('ticket-priority') || {}).value || '';
        if (!priority) {
            setError('ticket-priority-error', 'Priority is required');
            markInvalid('ticket-priority');
            valid = false;
        } else if (VALID_PRIORITIES.indexOf(priority) === -1) {
            setError('ticket-priority-error', 'Invalid priority value');
            markInvalid('ticket-priority');
            valid = false;
        }

        var assignee = (getEl('ticket-assignee') || {}).value || '';
        if (!assignee.trim()) {
            setError('ticket-assignee-error', 'Assignee is required');
            markInvalid('ticket-assignee');
            valid = false;
        }

        var reporter = (getEl('ticket-reporter') || {}).value || '';
        if (!reporter.trim()) {
            setError('ticket-reporter-error', 'Reporter is required');
            markInvalid('ticket-reporter');
            valid = false;
        }

        return valid;
    }

    function handleSubmit(e) {
        e.preventDefault();
        if (!validateForm()) return;

        var submitBtn  = getEl('ticket-submit-btn');
        var errorEl    = getEl('ticket-form-error');
        var successEl  = getEl('ticket-form-success');
        if (submitBtn) { submitBtn.disabled = true; submitBtn.textContent = 'Creating...'; }
        if (errorEl)   { errorEl.style.display = 'none'; }
        if (successEl) { successEl.style.display = 'none'; }

        var formData = new URLSearchParams();
        formData.append('title',       (getEl('ticket-title')       || {}).value || '');
        formData.append('description', (getEl('ticket-description') || {}).value || '');
        formData.append('priority',    (getEl('ticket-priority')    || {}).value || '');
        formData.append('assignee',    (getEl('ticket-assignee')    || {}).value || '');
        formData.append('reporter',    (getEl('ticket-reporter')    || {}).value || '');

        window.TicketingCsrf.postForm(CREATE_URL, formData.toString())
        .then(function (res) { return res.json().then(function (d) { return { status: res.status, data: d }; }); })
        .then(function (result) {
            if (result.data.success) {
                if (successEl) {
                    successEl.textContent = 'Ticket ' + (result.data.ticketId || '') + ' created successfully! Redirecting...';
                    successEl.style.display = 'block';
                }
                setTimeout(function () {
                    window.location.href = '/content/ticketingApp/us/en/tickets/detail.html?ticketPath=' +
                        encodeURIComponent(result.data.path);
                }, 1200);
            } else {
                if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = 'Create Ticket'; }
                var msg = result.data.error || 'Failed to create ticket';
                if (result.status === 400) {
                    if (errorEl) { errorEl.textContent = msg; errorEl.style.display = 'block'; }
                } else {
                    if (errorEl) { errorEl.textContent = 'An unexpected error occurred: ' + msg; errorEl.style.display = 'block'; }
                }
            }
        })
        .catch(function () {
            if (submitBtn) { submitBtn.disabled = false; submitBtn.textContent = 'Create Ticket'; }
            if (errorEl) { errorEl.textContent = 'Network error — please try again.'; errorEl.style.display = 'block'; }
        });
    }

    function init() {
        var root = document.querySelector('[data-cmp-is="ticketForm"]');
        if (!root) return;
        var form = getEl('ticket-create-form');
        if (form) form.addEventListener('submit', handleSubmit);

        ['title', 'description', 'assignee', 'reporter'].forEach(function (f) {
            var el = getEl('ticket-' + f);
            if (el) {
                el.addEventListener('input', function () {
                    if (el.value.trim()) {
                        setError('ticket-' + f + '-error', '');
                        clearInvalid('ticket-' + f);
                    }
                });
            }
        });
        var priorityEl = getEl('ticket-priority');
        if (priorityEl) {
            priorityEl.addEventListener('change', function () {
                if (priorityEl.value) {
                    setError('ticket-priority-error', '');
                    clearInvalid('ticket-priority');
                }
            });
        }
    }

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
}());
