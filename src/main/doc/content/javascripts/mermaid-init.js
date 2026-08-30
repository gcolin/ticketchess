(function () {
  function initMermaid() {
    if (typeof mermaid === "undefined") {
      return;
    }

    mermaid.initialize({
      startOnLoad: false,
      securityLevel: "loose"
    });

    var blocks = document.querySelectorAll("pre code.language-mermaid");
    blocks.forEach(function (block, index) {
      var pre = block.parentElement;
      if (!pre || pre.dataset.mermaidProcessed === "true") {
        return;
      }

      var graphDefinition = block.textContent;
      var container = document.createElement("div");
      container.className = "mermaid";
      container.textContent = graphDefinition;

      pre.dataset.mermaidProcessed = "true";
      pre.replaceWith(container);

      mermaid
        .run({
          nodes: [container]
        })
        .catch(function () {
          // Keep the page usable even if Mermaid cannot render one diagram.
        });
    });
  }

  if (document.readyState === "loading") {
    document.addEventListener("DOMContentLoaded", initMermaid);
  } else {
    initMermaid();
  }
})();
