import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { HttpClient } from '@angular/common/http';
import { Router } from '@angular/router';

import { environment } from '../../../../environments/environment';
import { KycStatus, UserRole } from '../../../shared/models/user.model';
import { OtpService, GenerateOtpRequest, VerifyOtpRequest } from '../../otp/otp.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  // Model for the registration form data
  registrationForm = {
    username: '',
    password: '',
    email: '',
    firstName: '',
    lastName: '',
    dateOfBirth: '',
    phoneNumber: '',
    role: UserRole.CUSTOMER,
    kycStatus: KycStatus.PENDING
  };

  // UI State variables
  errorMessage: string | null = null;
  successMessage: string | null = null;
  showPassword: boolean = false;
  agreeToTerms: boolean = false;
  isSubmitting: boolean = false;

  // OTP state
  otpCode: string = '';
  otpVerified: boolean = false;
  otpStatus: 'idle' | 'sent' | 'verifying' | 'verified' | 'failed' = 'idle';
  otpRequestId?: string;
  otpError?: string;
  isGeneratingOtp: boolean = false;
  isVerifyingOtp: boolean = false;

  constructor(private http: HttpClient, private router: Router, private otpService: OtpService) { }

  /**
   * Generate CONTACT_VERIFICATION OTP to the user's email.
   */
  generateContactOtp(): void {
    if (!this.registrationForm.email || !this.registrationForm.phoneNumber) {
      this.errorMessage = 'Enter email and phone number before generating OTP.';
      return;
    }
    this.errorMessage = null;
    this.otpError = undefined;
    this.isGeneratingOtp = true;

    const userId = this.registrationForm.email.trim();
    const req: GenerateOtpRequest = {
      userId,
      purpose: 'CONTACT_VERIFICATION',
      channels: ['EMAIL']
    };

    this.otpService.generatePublic(req).subscribe({
      next: (res) => {
        this.isGeneratingOtp = false;
        this.otpStatus = 'sent';
        this.otpRequestId = res.requestId;
        this.successMessage = 'OTP sent to your email address.';
      },
      error: (err) => {
        this.isGeneratingOtp = false;
        this.otpStatus = 'failed';
        this.otpError = err?.error?.message || 'Failed to send OTP.';
      }
    });
  }

  /**
   * Verify CONTACT_VERIFICATION OTP entered by the user.
   */
  verifyContactOtp(): void {
    if (!this.registrationForm.email) {
      this.otpError = 'Email is required to verify OTP.';
      return;
    }
    if (!this.otpCode || this.otpCode.trim().length === 0) {
      this.otpError = 'Enter the OTP code.';
      return;
    }
    this.otpError = undefined;
    this.isVerifyingOtp = true;

    const userId = this.registrationForm.email.trim();
    const req: VerifyOtpRequest = {
      userId,
      purpose: 'CONTACT_VERIFICATION',
      code: this.otpCode.trim()
    };

    this.otpService.verifyPublic(req).subscribe({
      next: (res) => {
        this.isVerifyingOtp = false;
        if (res.verified) {
          this.otpVerified = true;
          this.otpStatus = 'verified';
          this.successMessage = 'Contact verification successful.';
        } else {
          this.otpVerified = false;
          this.otpStatus = 'failed';
          this.otpError = res.message || 'Invalid or expired OTP.';
        }
      },
      error: (err) => {
        this.isVerifyingOtp = false;
        this.otpVerified = false;
        this.otpStatus = 'failed';
        this.otpError = err?.error?.message || 'Failed to verify OTP.';
      }
    });
  }

  /**
   * Handles the registration form submission.
   * Calls the User Microservice to create the user profile.
   */
  onSubmit(): void {
    if (!this.agreeToTerms) {
      this.errorMessage = 'Please agree to the terms and conditions';
      return;
    }
    if (!this.otpVerified) {
      this.errorMessage = 'Please verify the OTP sent to your email before creating the account.';
      return;
    }

    this.errorMessage = null;
    this.successMessage = null;
    this.isSubmitting = true;

    // Construct the request payload for the User Microservice
    const payload = {
      userId: null,
      username: this.registrationForm.username,
      password: this.registrationForm.password,
      email: this.registrationForm.email,
      role: this.registrationForm.role,
      firstName: this.registrationForm.firstName,
      lastName: this.registrationForm.lastName,
      dateOfBirth: this.registrationForm.dateOfBirth,
      phoneNumber: this.registrationForm.phoneNumber,
      kycStatus: this.registrationForm.kycStatus
    };

    // Make the POST request to the User Microservice via API Gateway
    this.http.post(`${environment.apiUrl}/auth/register`, payload).subscribe({
      next: (response) => {
        console.log('Registration successful:', response);
        this.successMessage = 'Registration successful! Your account is pending admin approval.';
        this.isSubmitting = false;

        // Redirect to login page after a delay
        setTimeout(() => {
          this.router.navigate(['/login']);
        }, 3000);
      },
      error: (error) => {
        console.error('Registration failed:', error);
        this.errorMessage = error.error?.message || 'Registration failed. Please try again.';
        this.isSubmitting = false;
      }
    });
  }

  /**
   * Navigate to login page
   */
  goToLogin(): void {
    this.router.navigate(['/login']);
  }

  /**
   * Toggle password visibility
   */
  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  /**
   * Get password strength based on criteria
   */
  getPasswordStrength(): 'weak' | 'medium' | 'strong' {
    const password = this.registrationForm.password;

    if (password.length === 0) return 'weak';

    let score = 0;

    // Length check
    if (password.length >= 8) score++;
    if (password.length >= 12) score++;

    // Character variety checks
    if (/[a-z]/.test(password)) score++;
    if (/[A-Z]/.test(password)) score++;
    if (/[0-9]/.test(password)) score++;
    if (/[^A-Za-z0-9]/.test(password)) score++;

    if (score <= 2) return 'weak';
    if (score <= 4) return 'medium';
    return 'strong';
  }

  /**
   * Get password strength text
   */
  getPasswordStrengthText(): string {
    const strength = this.getPasswordStrength();
    switch (strength) {
      case 'weak': return 'Weak';
      case 'medium': return 'Medium';
      case 'strong': return 'Strong';
      default: return '';
    }
  }

  /**
   * Clear error message when user starts typing
   */
  clearError(): void {
    if (this.errorMessage) {
      this.errorMessage = null;
    }
  }
}
