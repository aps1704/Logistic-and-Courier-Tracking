const $ = (id) => document.getElementById(id);

async function csrfToken() {
    const response = await fetch("/api/csrf");

    if (!response.ok) {
        throw new Error("Unable to obtain security token.");
    }

    return (await response.json()).token;
}

async function api(url, options = {}) {
    const method = (options.method || "GET").toUpperCase();
    const headers = {
        ...(options.headers || {})
    };

    if (options.body) {
        headers["Content-Type"] = "application/json";
    }

    if (!["GET", "HEAD", "OPTIONS"].includes(method)) {
        headers["X-CSRF-TOKEN"] = await csrfToken();
    }

    const response = await fetch(url, {
        ...options,
        method,
        headers,
        credentials: "same-origin"
    });

    if (response.status === 401) {
        throw new Error("Your session has expired. Please log in again.");
    }

    if (response.status === 403) {
        throw new Error("Access denied. Check your account permissions.");
    }

    if (response.status === 204) {
        return null;
    }

    const data = await response.json().catch(() => ({}));

    if (!response.ok) {
        throw new Error(data.message || "Request failed.");
    }

    return data;
}

function message(id, text) {
    const element = $(id);
    if (element) element.textContent = text;
}

function showDetails(container, pairs) {
    container.replaceChildren();

    for (const [label, value] of pairs) {
        const p = document.createElement("p");
        p.textContent = `${label}: ${value ?? ""}`;
        container.appendChild(p);
    }
}

function addText(parent, text) {
    const p = document.createElement("p");
    p.textContent = text;
    parent.appendChild(p);
}

function addButton(parent, text, handler) {
    const button = document.createElement("button");
    button.type = "button";
    button.textContent = text;
    button.addEventListener("click", handler);
    parent.appendChild(button);
}

function showError(id, error) {
    message(id, error.message || "Something went wrong.");
}

// Registration
if ($("registerForm")) {
    $("registerForm").addEventListener("submit", async (event) => {
        event.preventDefault();

        try {
            const result = await api("/api/auth/register", {
                method: "POST",
                body: JSON.stringify({
                    email: $("registerEmail").value.trim(),
                    password: $("registerPassword").value
                })
            });

            message("registerMessage", result.message);
            $("registerForm").reset();
        } catch (error) {
            showError("registerMessage", error);
        }
    });
}

// Login
if ($("loginForm")) {
    $("loginForm").addEventListener("submit", async (event) => {
        event.preventDefault();

        try {
            const result = await api("/api/auth/login", {
                method: "POST",
                body: JSON.stringify({
                    email: $("loginEmail").value.trim(),
                    password: $("loginPassword").value
                })
            });

            // Obtain a fresh CSRF token after authentication.
            await csrfToken();

            window.location.href = result.role === "ADMIN"
                ? "/admin-dashboard.html"
                : "/customer-dashboard.html";
        } catch (error) {
            showError("loginMessage", error);
        }
    });
}

// Public tracking
if ($("trackingForm")) {
    $("trackingForm").addEventListener("submit", async (event) => {
        event.preventDefault();

        const id = $("trackingId").value.trim();
        const result = $("trackingResult");

        result.textContent = "Searching...";

        try {
            const parcel = await api(
                `/api/tracking/${encodeURIComponent(id)}`
            );

            result.replaceChildren();

            showDetails(result, [
                ["Tracking ID", parcel.trackingId],
                ["Current status", parcel.status]
            ]);

            const heading = document.createElement("h3");
            heading.textContent = "Tracking history";
            result.appendChild(heading);

            if (!parcel.history.length) {
                addText(result, "No tracking events recorded yet.");
            }

            for (const event of parcel.history) {
                addText(
                    result,
                    `${event.eventTime} — ${event.status}` +
                    (event.location ? ` — ${event.location}` : "") +
                    (event.description ? ` — ${event.description}` : "")
                );
            }
        } catch (error) {
            showError("trackingResult", error);
        }
    });
}

// Current session and logout
async function requireRole(role) {
    try {
        const user = await api("/api/auth/me");

        if (user.role !== role) {
            window.location.href = "/login.html";
            return false;
        }

        return true;
    } catch {
        window.location.href = "/login.html";
        return false;
    }
}

if ($("logoutButton")) {
    $("logoutButton").addEventListener("click", async () => {
        try {
            await api("/api/auth/logout", { method: "POST" });
        } finally {
            window.location.href = "/login.html";
        }
    });
}

// Customer dashboard
let customerData = [];

async function loadCustomerParcels() {
    try {
        customerData = await api("/api/customer/parcels");
        filterCustomerParcels();
    } catch (error) {
        showError("customerParcels", error);
    }
}

function filterCustomerParcels() {
    if (!$("customerParcels")) return;

    const search = $("customerSearch").value.trim().toLowerCase();
    const status = $("customerStatus").value;

    const filtered = customerData.filter((p) =>
        (!search ||
            p.trackingId.toLowerCase().includes(search) ||
            p.receiver.toLowerCase().includes(search)) &&
        (!status || p.status === status)
    );

    const container = $("customerParcels");
    container.replaceChildren();

    if (!filtered.length) {
        addText(container, "No matching parcels found.");
    }

    for (const parcel of filtered) {
        const card = document.createElement("section");
        card.className = "parcel";

        showDetails(card, [
            ["Tracking ID", parcel.trackingId],
            ["Receiver", parcel.receiver],
            ["Status", parcel.status]
        ]);
addButton(card, "Track", () => {
    window.location.href =
        `/?tracking=${encodeURIComponent(parcel.trackingId)}`;
});

if (parcel.status === "Booked") {
    addButton(card, "Cancel Parcel", async () => {
        if (!confirm(`Cancel parcel ${parcel.trackingId}?`)) return;

        try {
            const result = await api(
                `/api/customer/parcels/${parcel.id}/cancel`,
                { method: "POST" }
            );

            alert(result.message);
            await loadCustomerParcels();
        } catch (error) {
            alert(error.message);
        }
    });
}

container.appendChild(card);
    }
}

if ($("customerParcels")) {
    requireRole("USER").then((allowed) => {
        if (allowed) loadCustomerParcels();
    });

    $("customerSearch").addEventListener("input", filterCustomerParcels);
    $("customerStatus").addEventListener("change", filterCustomerParcels);
}

// Admin dashboard
let adminData = [];

async function loadAdminParcels() {
    try {
        adminData = await api("/api/admin/parcels");
        filterAdminParcels();
    } catch (error) {
        showError("adminParcels", error);
    }
}

function filterAdminParcels() {
    if (!$("adminParcels")) return;

    const search = $("adminSearch").value.trim().toLowerCase();
    const status = $("adminStatusFilter").value;

    const filtered = adminData.filter((p) =>
        (!search ||
            p.trackingId.toLowerCase().includes(search) ||
            p.receiver.toLowerCase().includes(search)) &&
        (!status || p.status === status)
    );

    const container = $("adminParcels");
    container.replaceChildren();

    if (!filtered.length) {
        addText(container, "No matching parcels found.");
    }

    for (const parcel of filtered) {
        const card = document.createElement("section");
        card.className = "parcel";

        showDetails(card, [
            ["Tracking ID", parcel.trackingId],
            ["Sender", parcel.sender],
            ["Receiver", parcel.receiver],
            ["Customer", parcel.customerEmail],
            ["Status", parcel.status]
        ]);

        const select = document.createElement("select");

        for (const status of [
            "Booked", "In Transit", "Out for Delivery",
            "Delivered", "Delayed", "Cancelled"
        ]) {
            const option = document.createElement("option");
            option.value = status;
            option.textContent = status;
            option.selected = status === parcel.status;
            select.appendChild(option);
        }

        card.appendChild(select);

        const location = document.createElement("input");
        location.placeholder = "Current location";
        card.appendChild(location);

        const description = document.createElement("input");
        description.placeholder = "Tracking update";
        card.appendChild(description);

        addButton(card, "Update Status", async () => {
            try {
                await api(`/api/admin/parcels/${parcel.id}/status`, {
                    method: "PUT",
                    body: JSON.stringify({
                        status: select.value,
                        location: location.value,
                        description: description.value
                    })
                });

                await loadAdminParcels();
            } catch (error) {
                alert(error.message);
            }
        });

        addButton(card, "Delete Parcel", async () => {
            if (!confirm(`Delete ${parcel.trackingId}?`)) return;

            try {
                await api(`/api/admin/parcels/${parcel.id}`, {
                    method: "DELETE"
                });

                await loadAdminParcels();
            } catch (error) {
                alert(error.message);
            }
        });

        container.appendChild(card);
    }
}

if ($("adminParcels")) {
    requireRole("ADMIN").then((allowed) => {
        if (allowed) loadAdminParcels();
    });

    $("adminSearch").addEventListener("input", filterAdminParcels);
    $("adminStatusFilter").addEventListener("change", filterAdminParcels);
}

if ($("adminCreateForm")) {
    $("adminCreateForm").addEventListener("submit", async (event) => {
        event.preventDefault();

        try {
            const result = await api("/api/admin/parcels", {
                method: "POST",
                body: JSON.stringify({
                    trackingId: $("adminTrackingId").value.trim(),
                    sender: $("adminSender").value.trim(),
                    receiver: $("adminReceiver").value.trim(),
                    customerEmail: $("adminCustomerEmail").value.trim()
                })
            });

            message("adminCreateMessage", result.message);
            $("adminCreateForm").reset();
            await loadAdminParcels();
        } catch (error) {
            showError("adminCreateMessage", error);
        }
    });
}

// Support tracking links opened from the customer dashboard.
if ($("trackingForm")) {
    const params = new URLSearchParams(window.location.search);
    const trackingId = params.get("tracking");

    if (trackingId) {
        $("trackingId").value = trackingId;
        $("trackingForm").requestSubmit();
    }
}

if ($("customerCreateForm")) {
    $("customerCreateForm").addEventListener("submit", async (event) => {
        event.preventDefault();

        const button = $("customerCreateForm").querySelector(
            'button[type="submit"]'
        );

        button.disabled = true;
        message("customerCreateMessage", "Placing your order...");

        try {
            const result = await api("/api/customer/parcels", {
                method: "POST",
                body: JSON.stringify({
                    sender: $("sender").value.trim(),
                    senderPhone: $("senderPhone").value.trim(),
                    pickupAddress: $("pickupAddress").value.trim(),
                    receiver: $("receiver").value.trim(),
                    receiverPhone: $("receiverPhone").value.trim(),
                    deliveryAddress: $("deliveryAddress").value.trim(),
                    weight: $("weight").value
                })
            });

            $("customerCreateForm").reset();

            message(
                "customerCreateMessage",
                `${result.message} Your tracking ID is ${result.trackingId}.`
            );

            await loadCustomerParcels();
        } catch (error) {
            showError("customerCreateMessage", error);
        } finally {
            button.disabled = false;
        }
    });
}