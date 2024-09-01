import { Injectable } from '@angular/core';

@Injectable({
  providedIn: 'root'
})
export class AuthService {

  private loggedIn: boolean = false;

  constructor() { }

  login(username: string, password: string): boolean {
    if (username === 'admin' && password === 'admin') {
      this.loggedIn = true;
      localStorage.setItem('user', 'admin'); // Set a user token or flag in localStorage
      return true;
    }
    return false;
  }

  logout(): void {
    this.loggedIn = false;
    localStorage.removeItem('user'); // Clear the user token or flag from localStorage
  }

  isLoggedIn(): boolean {
    return !!localStorage.getItem('user'); // Check if the user token exists
  }
}
