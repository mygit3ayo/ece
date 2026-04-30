const materialsList = document.getElementById("materials-list");
const recommendedList = document.getElementById("recommended-list");
const searchInput = document.getElementById("search-input");
const uploadForm = document.getElementById("upload-form");
const uploadStatus = document.getElementById("upload-status");
const fileInput = document.getElementById("file-input");
const dropZone = document.getElementById("drop-zone");
const selectedFileText = document.getElementById("selected-file-text");
const uploadProgress = document.getElementById("upload-progress");
const uploadProgressFill = document.getElementById("upload-progress-fill");
const uploadProgressText = document.getElementById("upload-progress-text");
const toggleLibraryButton = document.getElementById("toggle-library");
const libraryContent = document.getElementById("library-content");
const showMoreMaterialsButton = document.getElementById("show-more-materials");

let allMaterials = [];
let filteredMaterials = [];
let visibleMaterialCount = 6;
const MATERIALS_PAGE_SIZE = 6;

async function loadMaterials() {
    try {
        const response = await fetch("/api/materials");
        const materials = await parseJsonResponse(response);
        if (!response.ok) {
            throw new Error(materials.error || "Could not load materials.");
        }

        allMaterials = materials;
        filteredMaterials = materials;
        visibleMaterialCount = MATERIALS_PAGE_SIZE;
        renderMaterials(filteredMaterials);
        renderRecommendations(materials);
    } catch (error) {
        const message = `<p class="empty-state">${escapeHtml(error.message)}</p>`;
        if (materialsList) {
            materialsList.innerHTML = message;
        }
        if (recommendedList) {
            recommendedList.innerHTML = message;
        }
    }
}

function renderMaterials(materials) {
    if (!materialsList) {
        return;
    }

    if (!materials.length) {
        materialsList.innerHTML = '<p class="empty-state">No books or notes found yet.</p>';
        if (showMoreMaterialsButton) {
            showMoreMaterialsButton.hidden = true;
        }
        return;
    }

    const visibleMaterials = materials.slice(0, visibleMaterialCount);
    materialsList.innerHTML = visibleMaterials.map((material) => `
        <article class="material-card">
            <h3>${escapeHtml(material.title)}</h3>
            <p class="material-meta">${escapeHtml(material.author_name)} | ${escapeHtml(material.course_code)}</p>
            <a class="download-link button secondary" href="${material.download_url}" download>Download in one click</a>
        </article>
    `).join("");

    if (showMoreMaterialsButton) {
        const hasMore = materials.length > visibleMaterialCount;
        showMoreMaterialsButton.hidden = !hasMore;
        showMoreMaterialsButton.textContent = hasMore ? "Show More" : "All Materials Loaded";
    }
}

function renderRecommendations(materials) {
    if (!recommendedList) {
        return;
    }

    const recommended = materials.slice(0, 3);
    if (!recommended.length) {
        recommendedList.innerHTML = '<p class="empty-state">Recommendations will appear here.</p>';
        return;
    }

    recommendedList.innerHTML = recommended.map((material) => `
        <article class="recommended-card">
            <h3>${escapeHtml(material.title)}</h3>
            <p class="material-meta">${escapeHtml(material.author_name)} | ${escapeHtml(material.course_code)}</p>
            <a class="download-link button primary" href="${material.download_url}" download>Quick download</a>
        </article>
    `).join("");
}

searchInput?.addEventListener("input", () => {
    const query = searchInput.value.trim().toLowerCase();
    if (!query) {
        filteredMaterials = allMaterials;
        visibleMaterialCount = MATERIALS_PAGE_SIZE;
        renderMaterials(filteredMaterials);
        renderRecommendations(allMaterials);
        return;
    }

    filteredMaterials = allMaterials.filter((material) =>
        material.title.toLowerCase().includes(query)
        || material.author_name.toLowerCase().includes(query)
        || material.course_code.toLowerCase().includes(query)
    );

    visibleMaterialCount = MATERIALS_PAGE_SIZE;
    renderMaterials(filteredMaterials);
    renderRecommendations(filteredMaterials);
});

dropZone?.addEventListener("click", () => fileInput?.click());
fileInput?.addEventListener("change", () => updateSelectedFile(fileInput.files?.[0] || null));

["dragenter", "dragover"].forEach((eventName) => {
    dropZone?.addEventListener(eventName, (event) => {
        event.preventDefault();
        dropZone.classList.add("drag-active");
    });
});

["dragleave", "drop"].forEach((eventName) => {
    dropZone?.addEventListener(eventName, (event) => {
        event.preventDefault();
        dropZone.classList.remove("drag-active");
    });
});

dropZone?.addEventListener("drop", (event) => {
    const file = event.dataTransfer?.files?.[0] || null;
    if (!fileInput || !file) {
        return;
    }

    const dataTransfer = new DataTransfer();
    dataTransfer.items.add(file);
    fileInput.files = dataTransfer.files;
    updateSelectedFile(file);
});

uploadForm?.addEventListener("submit", async (event) => {
    event.preventDefault();
    uploadStatus.textContent = "Uploading file. Please wait...";
    setUploadProgress(0);
    uploadProgress.hidden = false;

    if (!fileInput?.files?.length) {
        uploadStatus.textContent = "Please drag and drop a PDF or DOCX file first.";
        uploadProgress.hidden = true;
        return;
    }

    try {
        const response = await uploadWithProgress(new FormData(uploadForm));

        const result = await parseJsonResponse(response);
        if (!response.ok) {
            throw new Error(result.error || "Upload failed.");
        }

        const uploadedMaterial = {
            title: result.title,
            author_name: result.author_name,
            course_code: result.course_code,
            download_url: result.download_url
        };

        allMaterials = [uploadedMaterial, ...allMaterials];
        filteredMaterials = allMaterials;
        visibleMaterialCount = MATERIALS_PAGE_SIZE;
        renderMaterials(filteredMaterials);
        renderRecommendations(allMaterials);

        uploadStatus.textContent = "Upload successful. Redirecting...";
        uploadForm.reset();
        if (fileInput) {
            fileInput.value = "";
        }
        updateSelectedFile(null);
        setUploadProgress(100);

        setTimeout(() => {
            window.location.assign("/message.html");
        }, 900);
    } catch (error) {
        uploadStatus.textContent = error.message;
        uploadProgress.hidden = true;
    }
});

toggleLibraryButton?.addEventListener("click", () => {
    const expanded = toggleLibraryButton.getAttribute("aria-expanded") === "true";
    toggleLibraryButton.setAttribute("aria-expanded", String(!expanded));
    toggleLibraryButton.textContent = expanded ? "Expand" : "Collapse";
    libraryContent.hidden = expanded;
});

showMoreMaterialsButton?.addEventListener("click", () => {
    visibleMaterialCount += MATERIALS_PAGE_SIZE;
    renderMaterials(filteredMaterials);
});

function updateSelectedFile(file) {
    if (!selectedFileText) {
        return;
    }
    selectedFileText.textContent = file
        ? `Selected file: ${file.name}`
        : "Drop a PDF or DOCX file here, or click to choose one.";
}

function setUploadProgress(percent) {
    if (!uploadProgressFill || !uploadProgressText) {
        return;
    }
    const safePercent = Math.max(0, Math.min(100, Math.round(percent)));
    uploadProgressFill.style.width = `${safePercent}%`;
    uploadProgressText.textContent = `${safePercent}%`;
}

function uploadWithProgress(formData) {
    return new Promise((resolve, reject) => {
        const xhr = new XMLHttpRequest();
        xhr.open("POST", "/api/materials/upload");

        xhr.upload.addEventListener("progress", (event) => {
            if (event.lengthComputable) {
                setUploadProgress((event.loaded / event.total) * 100);
            }
        });

        xhr.addEventListener("load", () => {
            const headers = new Headers();
            const rawHeaders = xhr.getAllResponseHeaders().trim().split(/[\r\n]+/);
            rawHeaders.forEach((line) => {
                if (!line) {
                    return;
                }
                const parts = line.split(": ");
                const header = parts.shift();
                const value = parts.join(": ");
                if (header) {
                    headers.append(header, value);
                }
            });

            resolve(new Response(xhr.responseText, {
                status: xhr.status,
                statusText: xhr.statusText,
                headers
            }));
        });

        xhr.addEventListener("error", () => reject(new Error("Upload request failed.")));
        xhr.send(formData);
    });
}

async function parseJsonResponse(response) {
    const contentType = response.headers.get("Content-Type") || "";
    const bodyText = await response.text();
    if (!contentType.includes("application/json")) {
        throw new Error("Server returned HTML instead of JSON. Restart the server and try again.");
    }
    return JSON.parse(bodyText);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#39;");
}

updateSelectedFile(null);
loadMaterials();
