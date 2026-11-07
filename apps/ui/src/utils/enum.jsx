export const Environments = {
  LOCAL: 'local',
  TEST: 'test',
  PROD: 'prod'
};

export const ViewerControlMode = {
  JUKEBOX: 'JUKEBOX',
  VOTING: 'VOTING'
};

export const LocationCheckMethod = {
  GEO: 'GEO',
  CODE: 'CODE',
  GEO_OR_CODE: 'GEO_OR_CODE',
  NONE: 'NONE'
};

// GEO_OR_CODE accepts either check: nearby viewers by GPS, remote viewers
// (e.g. watching a live stream) by entering the location code.
export const usesGeoCheck = (method) => method === LocationCheckMethod.GEO || method === LocationCheckMethod.GEO_OR_CODE;
export const usesCodeCheck = (method) => method === LocationCheckMethod.CODE || method === LocationCheckMethod.GEO_OR_CODE;

export const StatusResponse = {
  SHOW_EXISTS: 'SHOW_EXISTS',
  EMAIL_NOT_VERIFIED: 'EMAIL_NOT_VERIFIED',
  EMAIL_CANNOT_BE_SENT: 'EMAIL_CANNOT_BE_SENT',
  SHOW_NOT_FOUND: 'SHOW_NOT_FOUND',
  UNAUTHORIZED: 'UNAUTHORIZED',
  INVALID_JWT: 'INVALID_JWT',
  API_ACCESS_REQUESTED: 'API_ACCESS_REQUESTED',
  UNEXPECTED_ERROR: 'UNEXPECTED_ERROR',
  OWNER_REQUESTED: 'OWNER_REQUESTED',
  // PSA-v2 PR-5 — raised by updatePsaEnabled / setNextPsaOverride
  // when the name doesn't match any PSA in Show.psaSequences[].
  INVALID_PSA_NAME: 'INVALID_PSA_NAME',
  // #191: raised by updatePages when a save would grow the page list past
  // the per-show cap.
  PAGE_LIMIT_REACHED: 'PAGE_LIMIT_REACHED',
  // TOTP 2FA — raised by verifyMfa / confirmMfaEnrollment / disableMfa /
  // regenerateRecoveryCodes.
  INVALID_MFA_CODE: 'INVALID_MFA_CODE',
  MFA_RATE_LIMITED: 'MFA_RATE_LIMITED',
  MFA_CHALLENGE_EXPIRED: 'MFA_CHALLENGE_EXPIRED',
  MFA_ALREADY_ENABLED: 'MFA_ALREADY_ENABLED',
  MFA_NOT_ENABLED: 'MFA_NOT_ENABLED',
  MFA_NOT_CONFIGURED: 'MFA_NOT_CONFIGURED'
};
