// The frame takes one fixed accent colour. The page has a light and a dark palette,
// so hand the frame the navy that reads on each one.
const scheme = matchMedia("(prefers-color-scheme: dark)");
const paint = () => document.querySelectorAll("cocode-head, cocode-foot")
  .forEach((el) => el.setAttribute("accent", scheme.matches ? "#9cc0e8" : "#1e3a5f"));
scheme.addEventListener("change", paint);
paint();
