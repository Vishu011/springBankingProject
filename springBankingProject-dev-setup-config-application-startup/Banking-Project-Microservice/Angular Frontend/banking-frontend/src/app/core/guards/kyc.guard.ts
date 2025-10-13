import { inject } from '@angular/core';
import { CanActivateFn, Router, UrlTree } from '@angular/router';
import { of } from 'rxjs';
import { map, catchError } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';
import { UserProfileService } from '../../features/user-profile/user-profile.service';
import { KycStatus } from '../../shared/models/user.model';

/**
 * Guard that ensures the user's KYC is VERIFIED before allowing access.
 * If not verified, redirects to /kyc to complete submission.
 */
export const kycVerifiedGuard: CanActivateFn = (route, state) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const userProfileService = inject(UserProfileService);

  if (!authService.isLoggedIn()) {
    return router.createUrlTree(['/login']) as UrlTree;
  }

  const userId = authService.getIdentityClaims()?.sub;
  if (!userId) {
    return router.createUrlTree(['/login']) as UrlTree;
  }

  return userProfileService.getUserProfile(userId).pipe(
    map(profile => {
      if (profile.kycStatus === KycStatus.VERIFIED) {
        return true;
      }
      // Allow direct access to /kyc so user can complete verification
      if (state.url.startsWith('/kyc')) {
        return true;
      }
      return router.createUrlTree(['/kyc']);
    }),
    catchError(() => of(router.createUrlTree(['/kyc'])))
  );
};
