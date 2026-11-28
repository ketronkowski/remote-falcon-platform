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

/** postMessage type the framing page listens for after a request goes through. */
export const REQUEST_MESSAGE_TYPE = "rf-request";

/**
 * In embed mode, tells the framing page that a song request was accepted, so
 * it can keep its own per-viewer count (the family live page logs which invite
 * link asked for what). Only the song name goes out, which the page already
 * shows publicly, so any parent origin may receive it; the parent is
 * responsible for checking the message's origin. Does nothing outside embed
 * mode or when the page isn't framed.
 */
export const notifyEmbedParent = (
  embed,
  sequence,
  win = typeof window !== "undefined" ? window : null
) => {
  if (!embed || !win || !sequence) return false;
  const parent = win.parent;
  if (!parent || parent === win) return false;
  try {
    parent.postMessage(
      { type: REQUEST_MESSAGE_TYPE, sequence: String(sequence) },
      "*"
    );
    return true;
  } catch {
    return false;
  }
};
