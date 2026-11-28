import { describe, expect, it, vi } from "vitest";

import {
  applyEmbedClass,
  EMBED_CLASS,
  notifyEmbedParent,
  readEmbedParams,
  REQUEST_MESSAGE_TYPE,
} from "../embedMode";

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

describe("notifyEmbedParent", () => {
  const framed = () => {
    const parent = { postMessage: vi.fn() };
    return { win: { parent }, parent };
  };

  it("posts the song to the framing page in embed mode", () => {
    const { win, parent } = framed();
    expect(notifyEmbedParent(true, "Jingle Bells", win)).toBe(true);
    expect(parent.postMessage).toHaveBeenCalledWith(
      { type: REQUEST_MESSAGE_TYPE, sequence: "Jingle Bells" },
      "*"
    );
  });

  it("does nothing outside embed mode, unframed, or without a song", () => {
    const { win, parent } = framed();
    expect(notifyEmbedParent(false, "Jingle Bells", win)).toBe(false);
    expect(notifyEmbedParent(true, "", win)).toBe(false);
    const top = {};
    top.parent = top;
    expect(notifyEmbedParent(true, "Jingle Bells", top)).toBe(false);
    expect(parent.postMessage).not.toHaveBeenCalled();
  });

  it("swallows a postMessage failure", () => {
    const win = { parent: { postMessage: () => { throw new Error("x"); } } };
    expect(notifyEmbedParent(true, "Song", win)).toBe(false);
  });
});
