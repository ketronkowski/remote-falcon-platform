/**
 * Embed mode, for showing this viewer page inside another page (the family
 * live-stream page frames it beside the video).
 *
 *   ?embed=1            turns it on
 *   ?embed=1&code=1234  also fills in the location code for the viewer
 *
 * In embed mode the page skips the browser location prompt and the "allow
 * location" help (a framed page usually can't get a location anyway, and the
 * code is what lets these viewers request), and adds the `rf-embed` class to
 * <html> so a viewer page template can hide parts that don't apply, such as the
 * FM radio row. The code is only a prefill: the viewer service still checks it.
 */

export const EMBED_CLASS = "rf-embed";

const CODE_PATTERN = /^\d{1,9}$/;

const isOn = (value) =>
  value != null &&
  ["1", "true", "yes"].includes(String(value).trim().toLowerCase());

/** `{ embed: boolean, code: string | null }`, from a query string such as `window.location.search`. */
export const readEmbedParams = (search) => {
  let params;
  try {
    params = new URLSearchParams(search || "");
  } catch {
    return { embed: false, code: null };
  }
  const embed = isOn(params.get("embed"));
  const rawCode = (params.get("code") || "").trim();
  return {
    embed,
    code: embed && CODE_PATTERN.test(rawCode) ? rawCode : null,
  };
};

/** Adds or removes the embed class on the page's root element. */
export const applyEmbedClass = (
  embed,
  root = typeof document !== "undefined" ? document.documentElement : null
) => {
  if (!root) return;
  if (embed) {
    root.classList.add(EMBED_CLASS);
  } else {
    root.classList.remove(EMBED_CLASS);
  }
};
