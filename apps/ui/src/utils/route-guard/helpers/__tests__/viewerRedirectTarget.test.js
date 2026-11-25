import { describe, it, expect } from 'vitest';

import { viewerRedirectTarget } from '../helpers';

describe('viewerRedirectTarget', () => {
  it('keeps the embed query string when sending a viewer to /remote-falcon', () => {
    expect(viewerRedirectTarget({ pathname: '/', search: '?embed=1&code=4821', hash: '' })).toEqual({
      pathname: '/remote-falcon',
      search: '?embed=1&code=4821',
      hash: ''
    });
  });

  it('keeps utm params and the hash', () => {
    expect(viewerRedirectTarget({ pathname: '/', search: '?utm_source=qr', hash: '#x' })).toEqual({
      pathname: '/remote-falcon',
      search: '?utm_source=qr',
      hash: '#x'
    });
  });

  it('does nothing when the viewer is already on /remote-falcon', () => {
    expect(viewerRedirectTarget({ pathname: '/remote-falcon', search: '?embed=1&code=4821', hash: '' })).toBeNull();
  });

  it('handles a missing location', () => {
    expect(viewerRedirectTarget(undefined)).toEqual({ pathname: '/remote-falcon', search: '', hash: '' });
  });
});
