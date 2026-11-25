import { useEffect } from 'react';

import PropTypes from 'prop-types';
import { useNavigate } from 'react-router-dom';

import useAuth from '../../hooks/useAuth';

import { isExternalViewer, viewerRedirectTarget } from './helpers/helpers';

/**
 * Guest guard for routes having no auth required
 * @param {PropTypes.node} children children element/node
 */

const ViewerGuard = ({ children }) => {
  const { isLoggedIn } = useAuth();
  const navigate = useNavigate();

  useEffect(() => {
    if (isExternalViewer()) {
      const target = viewerRedirectTarget(window.location);
      if (target) navigate(target, { replace: true });
    }
  }, [isLoggedIn, navigate]);

  return children;
};

ViewerGuard.propTypes = {
  children: PropTypes.node
};

export default ViewerGuard;
