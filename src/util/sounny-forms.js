/**
 * SounnyForms Client Library (v2.0.0)
 * Ultra-lightweight (~2.5KB), zero-dependency form handler with automatic AJAX submission,
 * multipart file attachment support, honeypot injection, bot velocity prevention,
 * offline submission queueing, and glassmorphic UI state management.
 *
 * Usage:
 *   <script src="sounny-forms.js" data-endpoint="https://sounny-forms.web.app/api/submit"></script>
 *
 *   <form data-sounnyform="services_inquiry">
 *     <input type="text" name="name" required>
 *     <input type="email" name="email" required>
 *     <input type="file" name="attachment">
 *     <button type="submit">Send Message</button>
 *   </form>
 */

(function (window, document) {
  "use strict";

  const STORAGE_QUEUE_KEY = "sounnyforms_offline_queue";

  const SounnyForms = {
    defaultEndpoint: "https://sounny-forms.web.app/api/submit",

    /**
     * Initializes all forms on the page marked with [data-sounnyform]
     */
    init: function (options = {}) {
      const currentScript = document.currentScript || document.querySelector("script[data-endpoint]");
      if (currentScript && currentScript.dataset.endpoint) {
        this.defaultEndpoint = currentScript.dataset.endpoint;
      }
      if (options.endpoint) {
        this.defaultEndpoint = options.endpoint;
      }

      const forms = document.querySelectorAll("form[data-sounnyform]");
      forms.forEach((form) => this.attach(form, options));

      // Setup offline sync listener
      window.addEventListener("online", () => this.flushOfflineQueue());
    },

    /**
     * Attaches SounnyForms handler to a specific form element
     * @param {HTMLFormElement} form
     * @param {Object} opts
     */
    attach: function (form, opts = {}) {
      if (!form || form.dataset.sounnyformInitialized) return;
      form.dataset.sounnyformInitialized = "true";

      const formId = form.dataset.sounnyform || form.getAttribute("name") || form.id || "default";
      const endpoint = form.dataset.endpoint || form.action || opts.endpoint || this.defaultEndpoint;

      // 1. Inject Honeypot & Timing Fields if not present
      if (!form.querySelector('input[name="_honey"]')) {
        const honeyInput = document.createElement("input");
        honeyInput.type = "text";
        honeyInput.name = "_honey";
        honeyInput.style.cssText = "position:absolute!important;left:-9999px!important;opacity:0!important;pointer-events:none!important;visibility:hidden!important;";
        honeyInput.tabIndex = -1;
        honeyInput.autocomplete = "off";
        form.appendChild(honeyInput);
      }

      let tsInput = form.querySelector('input[name="_ts"]');
      if (!tsInput) {
        tsInput = document.createElement("input");
        tsInput.type = "hidden";
        tsInput.name = "_ts";
        tsInput.value = Date.now();
        form.appendChild(tsInput);
      }

      form.addEventListener("focusin", function onFirstFocus() {
        tsInput.value = Date.now();
        form.removeEventListener("focusin", onFirstFocus);
      });

      // 2. Intercept Submit Event
      form.addEventListener("submit", async function (e) {
        e.preventDefault();

        let feedbackEl = form.querySelector(".sounnyform-feedback");
        if (!feedbackEl) {
          feedbackEl = document.createElement("div");
          feedbackEl.className = "sounnyform-feedback";
          feedbackEl.style.cssText = "display:none; margin-top: 14px; padding: 12px 16px; border-radius: 8px; font-size: 14px; line-height: 1.4;";
          form.appendChild(feedbackEl);
        }

        const submitBtn = form.querySelector('button[type="submit"], input[type="submit"]');
        const originalBtnHtml = submitBtn ? submitBtn.innerHTML : "Submit";

        // Check if form contains file inputs
        const hasFiles = Array.from(form.querySelectorAll('input[type="file"]')).some(input => input.files && input.files.length > 0);

        const formData = new FormData(form);
        formData.append("_formId", formId);

        // UI Loading State
        if (submitBtn) {
          submitBtn.disabled = true;
          submitBtn.setAttribute("data-original-text", originalBtnHtml);
          submitBtn.innerHTML = `
            <span style="display:inline-flex; align-items:center; gap:8px;">
              <svg style="animation: sounnySpin 1s linear infinite; width: 16px; height: 16px;" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5">
                <circle cx="12" cy="12" r="10" stroke-opacity="0.25"></circle>
                <path d="M12 2a10 10 0 0 1 10 10" stroke-linecap="round"></path>
              </svg>
              <span>${hasFiles ? 'Uploading & Sending...' : 'Sending...'}</span>
            </span>
          `;
        }
        feedbackEl.style.display = "none";

        const targetUrl = endpoint.includes("formId=") ? endpoint : `${endpoint}${endpoint.includes("?") ? "&" : "?"}formId=${encodeURIComponent(formId)}`;

        // Handle Offline State
        if (!navigator.onLine) {
          SounnyForms.queueOffline(formId, Object.fromEntries(formData.entries()), targetUrl);
          feedbackEl.style.display = "block";
          feedbackEl.style.backgroundColor = "rgba(245, 158, 11, 0.12)";
          feedbackEl.style.border = "1px solid rgba(245, 158, 11, 0.35)";
          feedbackEl.style.color = "#f59e0b";
          feedbackEl.innerHTML = "ℹ️ You are currently offline. Your submission has been saved locally and will automatically send when reconnected.";
          form.reset();
          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnHtml;
          }
          return;
        }

        try {
          let fetchOptions = {};

          if (hasFiles) {
            // Send Multipart FormData
            fetchOptions = {
              method: "POST",
              headers: { Accept: "application/json" },
              body: formData,
            };
          } else {
            // Send JSON
            const payload = Object.fromEntries(formData.entries());
            fetchOptions = {
              method: "POST",
              headers: {
                "Content-Type": "application/json",
                Accept: "application/json",
              },
              body: JSON.stringify(payload),
            };
          }

          const response = await fetch(targetUrl, fetchOptions);
          const result = await response.json().catch(() => ({ success: response.ok }));

          if (response.ok && result.success) {
            feedbackEl.style.display = "block";
            feedbackEl.style.backgroundColor = "rgba(16, 185, 129, 0.12)";
            feedbackEl.style.border = "1px solid rgba(16, 185, 129, 0.35)";
            feedbackEl.style.color = "#10b981";
            feedbackEl.innerHTML = `✓ ${result.message || "Thank you! Your message has been received."}`;

            form.reset();
            tsInput.value = Date.now();

            if (submitBtn) {
              submitBtn.innerHTML = `<span>Sent ✓</span>`;
              setTimeout(() => {
                submitBtn.disabled = false;
                submitBtn.innerHTML = originalBtnHtml;
              }, 3000);
            }

            form.dispatchEvent(new CustomEvent("sounnyforms:success", { bubbles: true, detail: { formId, result } }));
          } else {
            throw new Error(result.error || "Submission failed. Please try again.");
          }
        } catch (err) {
          console.error("SounnyForms Error:", err);
          feedbackEl.style.display = "block";
          feedbackEl.style.backgroundColor = "rgba(244, 63, 94, 0.12)";
          feedbackEl.style.border = "1px solid rgba(244, 63, 94, 0.35)";
          feedbackEl.style.color = "#f43f5e";
          feedbackEl.innerHTML = `⚠️ ${err.message || "An error occurred while sending. Please try again or email directly."}`;

          if (submitBtn) {
            submitBtn.disabled = false;
            submitBtn.innerHTML = originalBtnHtml;
          }

          form.dispatchEvent(new CustomEvent("sounnyforms:error", { bubbles: true, detail: { formId, error: err } }));
        }
      });
    },

    queueOffline: function (formId, data, targetUrl) {
      try {
        const queue = JSON.parse(localStorage.getItem(STORAGE_QUEUE_KEY) || "[]");
        queue.push({ formId, data, targetUrl, queuedAt: Date.now() });
        localStorage.setItem(STORAGE_QUEUE_KEY, JSON.stringify(queue));
      } catch (e) {
        console.warn("Could not save to localStorage offline queue:", e);
      }
    },

    flushOfflineQueue: async function () {
      try {
        const queue = JSON.parse(localStorage.getItem(STORAGE_QUEUE_KEY) || "[]");
        if (queue.length === 0) return;

        console.log(`[SounnyForms] Flushing ${queue.length} offline submission(s)...`);
        const remaining = [];

        for (const item of queue) {
          try {
            await fetch(item.targetUrl, {
              method: "POST",
              headers: { "Content-Type": "application/json", Accept: "application/json" },
              body: JSON.stringify(item.data),
            });
          } catch {
            remaining.push(item);
          }
        }

        localStorage.setItem(STORAGE_QUEUE_KEY, JSON.stringify(remaining));
      } catch (e) {
        console.warn("Error flushing offline queue:", e);
      }
    },
  };

  // Inject CSS keyframe animation for the spinner
  if (!document.getElementById("sounnyforms-styles")) {
    const styleEl = document.createElement("style");
    styleEl.id = "sounnyforms-styles";
    styleEl.textContent = `
      @keyframes sounnySpin {
        0% { transform: rotate(0deg); }
        100% { transform: rotate(360deg); }
      }
    `;
    document.head.appendChild(styleEl);
  }

  // Auto-initialize on DOM ready
  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", () => SounnyForms.init());
  } else {
    SounnyForms.init();
  }

  window.SounnyForms = SounnyForms;
})(window, document);
