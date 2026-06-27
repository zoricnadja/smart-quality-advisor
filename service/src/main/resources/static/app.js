const output = document.querySelector("#output");
const statusLabel = document.querySelector("#service-status");
const buttons = document.querySelectorAll("button[data-endpoint]");

async function runDemo(button) {
    const endpoint = button.dataset.endpoint;
    buttons.forEach((item) => {
        item.disabled = true;
        item.classList.toggle("is-loading", item === button);
    });
    statusLabel.textContent = "Running";
    output.textContent = `Calling ${endpoint} ...`;

    try {
        const response = await fetch(endpoint, { headers: { Accept: "text/plain" } });
        const text = await response.text();

        if (!response.ok) {
            throw new Error(text || `Request failed with HTTP ${response.status}`);
        }

        output.textContent = text;
        statusLabel.textContent = "Complete";
    } catch (error) {
        output.textContent = error.message;
        statusLabel.textContent = "Error";
    } finally {
        buttons.forEach((item) => {
            item.disabled = false;
            item.classList.remove("is-loading");
        });
    }
}

buttons.forEach((button) => {
    button.addEventListener("click", () => runDemo(button));
});
