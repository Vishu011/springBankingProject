// import { Component } from '@angular/core';
// import { AuthService } from '../../../core/services/auth.service';
// import { Router } from '@angular/router';

// @Component({
//   selector: 'app-login',
//   standalone: true,
//   templateUrl: './login.component.html',
//   styleUrls: ['./login.component.css']
// })
// export class LoginComponent {

//   constructor(
//     private authService: AuthService,
//     private router: Router
//   ) { }

//   /**
//    * Initiates the Keycloak login flow
//    */
//   login(): void {
//     // Add loading state or animation here if needed
//     this.authService.login();
//   }

//   /**
//    * Navigate to registration page
//    */
//   register(): void {
//     // Navigate to registration page
//     this.router.navigate(['/register']);

//     // Or if registration is handled by Keycloak:
//     // window.open('your-keycloak-registration-url', '_blank');
//   }

//   /**
//    * Show help or support options
//    */
//   getHelp(): void {
//     // Navigate to help page or open support modal
//     this.router.navigate(['/help']);

//     // Or open external help link:
//     // window.open('https://your-bank-help.com', '_blank');
//   }

//   /**
//    * Show demo or product tour
//    */
//   viewDemo(): void {
//     // Navigate to demo page or start interactive tour
//     this.router.navigate(['/demo']);

//     // Or open demo in new tab:
//     // window.open('https://your-bank-demo.com', '_blank');
//   }
// }





import { Component, AfterViewInit } from '@angular/core';
import { AuthService } from '../../../core/services/auth.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-login',
  standalone: true,
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements AfterViewInit {

  constructor(
    private authService: AuthService,
    private router: Router
  ) { }

  /**
   * Initiates the Keycloak login flow
   */
  login(): void {
    this.authService.login();
  }

  /**
   * Navigate to registration page
   */
  register(): void {
    this.router.navigate(['/register']);
  }

  /**
   * Show help or support options
   */
  getHelp(): void {
    this.router.navigate(['/help']);
  }

  /**
   * Show demo or product tour
   */
  viewDemo(): void {
    this.router.navigate(['/demo']);
  }

  /**
   * Automatically scroll the page smoothly from top to bottom over 3 seconds
   */
  ngAfterViewInit(): void {
    this.smoothScrollToBottom(2000);
  }

  private smoothScrollToBottom(duration: number): void {
    const start = window.scrollY;
    const end = document.body.scrollHeight - window.innerHeight;
    const distance = end - start;
    const startTime = performance.now();
  

    const animateScroll = (currentTime: number) => {
      const elapsed = currentTime - startTime;
      const progress = Math.min(elapsed / duration, 1);
      window.scrollTo(0, start + distance * progress);
      if (progress < 1) requestAnimationFrame(animateScroll);
    };

    requestAnimationFrame(animateScroll);
  }
}
