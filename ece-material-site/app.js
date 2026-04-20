const materialsList = document.getElementById("materials-list");
const uploadForm = document.getElementById("upload-form");
const uploadStatus = document.getElementById("upload-status");

async function loadMaterials() {
    try {
        const response = await fetch("/api/materials");
        if (!response.ok) {
            throw new Error("Could not load materials.");
        }

        const materials = await response.json();
        renderMaterials(materials);
    } catch (error) {
        materialsList.innerHTML = `<p class="empty-state">${error.message}</p>`;
    }
}

function renderMaterials(materials) {
    if (!materials.length) {
        materialsList.innerHTML = '<p class="empty-state">No materials yet. Run the importer or upload the first file.</p>';
        return;
    }

    materialsList.innerHTML = materials.map((material) => `
        <article class="material-card">
            <h3>${escapeHtml(material.title)}</h3>
            <p class="material-meta">${escapeHtml(material.course_code)}</p>
            <a class="download-link" href="${material.download_url}">Download material</a>
        </article>
    `).join("");
}

uploadForm?.addEventListener("submit", async (event) => {
    event.preventDefault();
    uploadStatus.textContent = "Uploading...";

    const formData = new FormData(uploadForm);

    try {
        const response = await fetch("/api/materials/upload", {
            method: "POST",
            body: formData
        });

        const payload = await response.json();
        if (!response.ok) {
            throw new Error(payload.error || "Upload failed.");
        }

        uploadStatus.textContent = "Upload successful.";
        uploadForm.reset();
        await loadMaterials();
    } catch (error) {
        uploadStatus.textContent = error.message;
    }
});

function escapeHtml(value) {
    return value
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

loadMaterials();
