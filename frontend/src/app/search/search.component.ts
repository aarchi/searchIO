import { HttpClient } from '@angular/common/http';
import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../auth.service';

interface AggregatedResult {
  title: string;
  summary: string;
  urls: string[];
}

interface AggregatedSearchResponse {
  results: AggregatedResult[];
}

@Component({
  selector: 'app-search',
  templateUrl: './search.component.html',
  styleUrls: ['./search.component.css']
})
export class SearchComponent {
  searchTerm: string = '';
  searchResponse: AggregatedResult[] | null = null;
  loading: boolean = false; // Track loading state
  errorMessage: string | null = null; // Error message for search

  username: string = '';
  password: string = '';
  loginErrorMessage: string | null = null;
  showModal: boolean = false; // Modal visibility

  constructor(private authService: AuthService, private router: Router, private http: HttpClient) {}

  // Handle pressing Enter key for search
  handleKeyPress(event: KeyboardEvent) {
    if (event.key === 'Enter') {
      this.performSearch();
    }
  }

  // Perform search operation
  performSearch() {
    if (this.searchTerm.trim()) {
      this.callSearchService(this.searchTerm);
    } else {
      console.warn('Search term cannot be empty');
    }
  }

  // Call search service and handle response
  callSearchService(searchTerm: string) {
    this.loading = true; // Show spinner
    this.errorMessage = null; // Clear previous error message
    const apiUrl = 'http://localhost:8080/api/search-all'; // Updated API URL

    this.http.post<AggregatedSearchResponse>(apiUrl, { searchTerm })
      .subscribe({
        next: (response: AggregatedSearchResponse) => {
          this.searchResponse = response.results;
          this.searchTerm = '';
          this.loading = false; // Hide spinner
        },
        error: (error) => {
          console.error("Error searching: ", error);
          this.errorMessage = "Failed to connect to the search service. Please try again later.";
          this.loading = false; // Hide spinner even on error
        }
      });
  }

  // Open login modal
  openLoginModal() {
    this.showModal = true;
    this.loginErrorMessage = null; // Clear previous login error message
  }

  // Close login modal and clear input fields
  closeLoginModal() {
    this.showModal = false;
    this.username = '';
    this.password = '';
    this.loginErrorMessage = null; // Clear login error message on close
  }

  // Handle login
  login() {
    if (this.authService.login(this.username, this.password)) {
      this.router.navigate(['/admin']).then(success => {
        if (success) {
          console.log('Navigation successful');
        } else {
          console.error('Navigation failed');
        }
      }).catch(err => {
        console.error('Navigation error:', err);
      });
    } else {
      this.loginErrorMessage = 'Invalid credentials';
    }
  }
}
