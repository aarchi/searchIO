import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { HttpClient } from '@angular/common/http'; // Import HttpClient
import { AuthService } from '../auth.service';
import { NgForm } from '@angular/forms'; // Import NgForm

@Component({
  selector: 'app-admin',
  templateUrl: './admin.component.html',
  styleUrls: ['./admin.component.css']
})
export class AdminComponent {
  // Define form fields with initial empty values
  incident: string = '';
  impact: string = '';
  resolution: string = '';

  // Inject AuthService, Router, and HttpClient in the constructor
  constructor(private authService: AuthService, private router: Router, private http: HttpClient) { }

  // Method to handle form submission
  submitForm(form: NgForm) {
    // Check if all form fields are filled
    if (this.incident && this.impact && this.resolution) {
      // Create a JSON object with the form data
      const formData = {
        incident: this.incident,
        impact: this.impact,
        resolution: this.resolution
      };

      // Send the form data to the backend using HTTP POST request
      this.http.post('http://localhost:8080/api/admin', formData).subscribe(
        response => {
          // Handle successful response
          console.log('Form data sent successfully', response);
          
          // Show an alert with the response message
          alert(response['message']); // Assuming the backend sends a JSON object with a 'message' property
          
          // Clear the form fields and reset the form after successful submission
          this.resetForm(form);
        },
        error => {
          // Handle error response
          console.error('Error sending form data', error);
          // Show an alert with the error message
          alert('Error sending form data: ' + (error.error?.message || error.message || 'Unknown error'));
        }
      );
    } else {
      // Log a message if form fields are invalid
      console.log('Form is invalid');
      alert('Please fill in all required fields');
    }
  }

  // Method to handle user logout
  logout() {
    this.authService.logout(); // Call logout method from AuthService
    this.router.navigate(['/']); // Navigate to the search page
  }

  // Method to reset the form
  resetForm(form: NgForm) {
    // Reset the form fields
    this.incident = '';
    this.impact = '';
    this.resolution = '';

    // Reset the form control state
    form.resetForm(); // This method will clear the form state and reset the form validation
  }
}
