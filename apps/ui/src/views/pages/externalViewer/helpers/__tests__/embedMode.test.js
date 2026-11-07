import { describe, expect, it } from "vitest";

import { applyEmbedClass, EMBED_CLASS, readEmbedParams } from "../embedMode";

describe("readEmbedParams", () => {
  it("is off for a normal visit", () => {
    expect(readEmbedParams("")).toEqual({ embed: false, code: null });
    expect(readEmbedParams(undefined)).toEqual({ embed: false, code: null });
    expect(readEmbedParams("?utm_source=qr")).toEqual({
      embed: false,
      code: null,
    });
  });

  it("turns on with embed=1, true or yes", () => {
    expect(readEmbedParams("?embed=1").embed).toBe(true);
    expect(readEmbedParams("?embed=true").embed).toBe(true);
    expect(readEmbedParams("?embed=YES").embed).toBe(true);
    expect(readEmbedParams("?embed=0").embed).toBe(false);
  });

  it("reads a numeric code only in embed mode", () => {
    expect(readEmbedParams("?embed=1&code=4821")).toEqual({
      embed: true,
      code: "4821",
    });
    expect(readEmbedParams("?code=4821")).toEqual({ embed: false, code: null });
  });

  it("ignores a code that is not just digits", () => {
    expect(readEmbedParams("?embed=1&code=48a1").code).toBeNull();
    expect(readEmbedParams("?embed=1&code=").code).toBeNull();
    expect(readEmbedParams("?embed=1&code=1234567890").code).toBeNull();
  });
});

describe("applyEmbedClass", () => {
  it("adds and removes the class on the given root", () => {
    const root = document.createElement("div");
    applyEmbedClass(true, root);
    expect(root.classList.contains(EMBED_CLASS)).toBe(true);
    applyEmbedClass(false, root);
    expect(root.classList.contains(EMBED_CLASS)).toBe(false);
  });

  it("does nothing without a root", () => {
    expect(() => applyEmbedClass(true, null)).not.toThrow();
  });
});
