const accessView = document.querySelector("#access-view");
const dashboardView = document.querySelector("#dashboard-view");
const loginForm = document.querySelector("#login-form");
const registerForm = document.querySelector("#register-form");
const accessMessage = document.querySelector("#access-message");
const studentPanel = document.querySelector("#student-panel");
const professorPanel = document.querySelector("#professor-panel");
const gradesBody = document.querySelector("#grades-body");
const enrollmentsBody = document.querySelector("#enrollments-body");
const gradeForm = document.querySelector("#grade-form");
const enrollForm = document.querySelector("#enroll-form");
const subjectSelect = document.querySelector("#enroll-subject");
const logoutButton = document.querySelector("#logout-button");

let credentials = null;
let selectedEnrollment = null;

function basicHeader() {
    const raw = `${credentials.email}:${credentials.password}`;
    const bytes = new TextEncoder().encode(raw);
    const binary = Array.from(bytes, byte => String.fromCharCode(byte)).join("");
    return `Basic ${btoa(binary)}`;
}

async function api(path, options = {}) {
    const headers = new Headers(options.headers || {});
    headers.set("Accept", "application/json");
    if (credentials) headers.set("Authorization", basicHeader());
    if (options.body) headers.set("Content-Type", "application/json");

    const response = await fetch(path, { ...options, headers });
    const text = await response.text();
    let data = null;
    if (text) {
        try { data = JSON.parse(text); } catch { data = text; }
    }
    if (!response.ok) {
        throw new Error(data?.message || data?.detail || `Error ${response.status}`);
    }
    return data;
}

function setMessage(element, text, kind = "") {
    element.textContent = text;
    element.className = `message ${kind}`.trim();
}

function escapeHtml(value) {
    return String(value ?? "").replace(/[&<>"']/g, character => ({
        "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;"
    })[character]);
}

function fechaCorta(value) {
    if (!value) return "—";
    const date = new Date(value);
    return Number.isNaN(date.getTime()) ? "—" : date.toLocaleDateString("es-CO");
}

function showAccess(message = "") {
    dashboardView.hidden = true;
    accessView.hidden = false;
    logoutButton.hidden = true;
    credentials = null;
    selectedEnrollment = null;
    gradeForm.hidden = true;
    gradeForm.reset();
    enrollForm.reset();
    setMessage(document.querySelector("#professor-message"), "");
    setMessage(document.querySelector("#enroll-message"), "");
    setMessage(document.querySelector("#student-message"), "");
    if (message) setMessage(accessMessage, message, "error");
}

async function loadStudentGrades() {
    const grades = await api("/api/estudiante/mis-notas");
    gradesBody.innerHTML = "";

    if (!grades.length) {
        gradesBody.innerHTML = '<tr><td colspan="4" class="empty-state">Todavía no tienes calificaciones registradas.</td></tr>';
        document.querySelector("#student-summary").innerHTML = '<span class="summary-chip">0 evaluaciones</span>';
        return;
    }

    const average = grades.reduce((sum, item) => sum + Number(item.nota), 0) / grades.length;
    document.querySelector("#student-summary").innerHTML =
        `<span class="summary-chip">${grades.length} evaluaciones</span><span class="summary-chip">Promedio: ${average.toFixed(2)}</span>`;

    gradesBody.innerHTML = grades.map(item => `
        <tr>
            <td>${escapeHtml(item.asignatura)}</td>
            <td>${escapeHtml(item.evaluacion)}</td>
            <td><span class="grade-pill">${Number(item.nota).toFixed(2)}</span></td>
            <td>${fechaCorta(item.fecha)}</td>
        </tr>
    `).join("");
}

async function loadProfessorEnrollments() {
    const enrollments = await api("/api/profesor/matriculas");
    enrollmentsBody.innerHTML = "";

    if (!enrollments.length) {
        enrollmentsBody.innerHTML = '<tr><td colspan="5" class="empty-state">No hay estudiantes matriculados en tus asignaturas.</td></tr>';
        return;
    }

    enrollmentsBody.innerHTML = enrollments.map(item => `
        <tr>
            <td>${escapeHtml(item.estudiante)}</td>
            <td>${escapeHtml(item.correo)}</td>
            <td>${escapeHtml(item.asignatura)}</td>
            <td>${item.id}</td>
            <td><button class="button select-row" type="button" data-enrollment-id="${item.id}">Elegir</button></td>
        </tr>
    `).join("");
}

async function loadProfessorSubjects() {
    const subjects = await api("/api/profesor/asignaturas");
    subjectSelect.innerHTML = '<option value="">Selecciona una asignatura...</option>';
    subjectSelect.innerHTML += subjects.map(item =>
        '<option value="' + item.id + '">' + escapeHtml(item.nombre) +
        ' (' + escapeHtml(item.codigo) + ')</option>'
    ).join("");
}

async function enterPortal() {
    selectedEnrollment = null;
    gradeForm.hidden = true;
    gradeForm.reset();
    setMessage(document.querySelector("#professor-message"), "");
    setMessage(document.querySelector("#enroll-message"), "");
    setMessage(document.querySelector("#student-message"), "");
    const profile = await api("/api/auth/perfil");
    accessView.hidden = true;
    dashboardView.hidden = false;
    logoutButton.hidden = false;
    document.querySelector("#welcome-title").textContent = `Hola, ${profile.nombre}`;
    document.querySelector("#welcome-copy").textContent = profile.correo;
    document.querySelector("#role-badge").textContent = profile.rol;

    if (profile.rol === "ESTUDIANTE") {
        document.querySelector("#role-label").textContent = "PANEL DE ESTUDIANTE";
        studentPanel.hidden = false;
        professorPanel.hidden = true;
        await loadStudentGrades();
    } else if (profile.rol === "PROFESOR") {
        document.querySelector("#role-label").textContent = "PANEL DE PROFESOR";
        professorPanel.hidden = false;
        studentPanel.hidden = true;
        await loadProfessorSubjects();
        await loadProfessorEnrollments();
    } else {
        throw new Error("El rol de esta cuenta no es válido.");
    }
}

document.querySelector("#login-tab").addEventListener("click", () => {
    document.querySelector("#login-tab").classList.add("active");
    document.querySelector("#register-tab").classList.remove("active");
    loginForm.hidden = false;
    registerForm.hidden = true;
    setMessage(accessMessage, "");
});

document.querySelector("#register-tab").addEventListener("click", () => {
    document.querySelector("#register-tab").classList.add("active");
    document.querySelector("#login-tab").classList.remove("active");
    registerForm.hidden = false;
    loginForm.hidden = true;
    setMessage(accessMessage, "");
});

loginForm.addEventListener("submit", async event => {
    event.preventDefault();
    credentials = {
        email: document.querySelector("#login-email").value.trim().toLowerCase(),
        password: document.querySelector("#login-password").value
    };
    try {
        await enterPortal();
        setMessage(accessMessage, "");
    } catch (error) {
        showAccess(error.message || "No se pudo iniciar sesión.");
    }
});

registerForm.addEventListener("submit", async event => {
    event.preventDefault();
    const data = {
        nombre: document.querySelector("#register-name").value.trim(),
        correo: document.querySelector("#register-email").value.trim().toLowerCase(),
        password: document.querySelector("#register-password").value
    };
    try {
        const user = await api("/api/auth/registro", {
            method: "POST",
            body: JSON.stringify(data)
        });
        document.querySelector("#login-email").value = user.correo;
        document.querySelector("#login-password").value = data.password;
        document.querySelector("#login-tab").click();
        setMessage(accessMessage, "Cuenta creada. Ya puedes iniciar sesión.", "success");
        registerForm.reset();
    } catch (error) {
        setMessage(accessMessage, error.message || "No se pudo crear la cuenta.", "error");
    }
});

enrollForm.addEventListener("submit", async event => {
    event.preventDefault();
    const datos = {
        correoEstudiante: document.querySelector("#enroll-email").value.trim().toLowerCase(),
        asignaturaId: Number(subjectSelect.value)
    };
    try {
        const saved = await api("/api/profesor/matriculas", {
            method: "POST",
            body: JSON.stringify(datos)
        });
        document.querySelector("#enroll-email").value = "";
        setMessage(
            document.querySelector("#enroll-message"),
            saved.estudiante + " quedó matriculado en " + saved.asignatura + ".",
            "success"
        );
        await loadProfessorEnrollments();
    } catch (error) {
        setMessage(document.querySelector("#enroll-message"), error.message, "error");
    }
});

enrollmentsBody.addEventListener("click", event => {
    const button = event.target.closest("[data-enrollment-id]");
    if (!button) return;
    selectedEnrollment = {
        id: Number(button.dataset.enrollmentId),
        student: button.closest("tr").cells[0].textContent,
        course: button.closest("tr").cells[2].textContent
    };
    document.querySelector("#selected-enrollment").textContent =
        `${selectedEnrollment.student} · ${selectedEnrollment.course} · Matrícula ${selectedEnrollment.id}`;
    gradeForm.hidden = false;
    document.querySelector("#evaluation-name").focus();
});

gradeForm.addEventListener("submit", async event => {
    event.preventDefault();
    if (!selectedEnrollment) return;
    const datos = {
        evaluacion: document.querySelector("#evaluation-name").value.trim(),
        nota: Number(document.querySelector("#grade-value").value)
    };
    try {
        const saved = await api(`/api/profesor/matriculas/${selectedEnrollment.id}/notas`, {
            method: "POST",
            body: JSON.stringify(datos)
        });
        setMessage(
            document.querySelector("#professor-message"),
            `Nota ${Number(saved.nota).toFixed(2)} guardada para ${saved.estudiante}.`,
            "success"
        );
        gradeForm.reset();
        await loadProfessorEnrollments();
    } catch (error) {
        setMessage(document.querySelector("#professor-message"), error.message, "error");
    }
});

document.querySelector("#refresh-grades").addEventListener("click", async () => {
    try {
        await loadStudentGrades();
        setMessage(document.querySelector("#student-message"), "Notas actualizadas.", "success");
    } catch (error) {
        setMessage(document.querySelector("#student-message"), error.message, "error");
    }
});

document.querySelector("#refresh-enrollments").addEventListener("click", async () => {
    try {
        await loadProfessorEnrollments();
        setMessage(document.querySelector("#professor-message"), "Matrículas actualizadas.", "success");
    } catch (error) {
        setMessage(document.querySelector("#professor-message"), error.message, "error");
    }
});

logoutButton.addEventListener("click", () => {
    loginForm.reset();
    showAccess("Sesión cerrada.");
});
