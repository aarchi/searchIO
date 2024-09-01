import { Injectable } from '@angular/core';
import { CanActivate, Router } from '@angular/router';

@Injectable({
  providedIn: 'root'
})
export class AuthGuard implements CanActivate {

  constructor(private router: Router) {}

  canActivate(): boolean {
    const isAuthenticated = !!localStorage.getItem('user'); // Check if user is authenticated

    if (!isAuthenticated) {
      console.log('Access denied. Redirecting to search page.'); // Debugging statement
      this.router.navigate(['/']); // Redirect to search page if not authenticated
    }

    return isAuthenticated; // Allow access if authenticated
  }
}
