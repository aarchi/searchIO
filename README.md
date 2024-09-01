```markdown
# SearchIO

## Mission

Our mission is to build a comprehensive search engine for UBS that integrates information from every source within the organization. By harnessing AI technology, we aim to streamline the search process, saving you valuable time and effort while providing precise and insightful results. Our goal is to create a powerful tool that simplifies access to critical information across UBS.

## Current Status

SearchIO currently supports searching from:
- UBS Stack Overflow
- Assist
- GitHub
- Incidents (managed by application teams)

We are using Nucleus for AI purposes to enhance search capabilities and provide more accurate results.

## Project Structure

```
root/
├── backend/ (Spring Boot application)
├── frontend/ (Angular application)
├── pom.xml (Parent POM for Maven build)
└── README.md
```

## Backend Setup (Spring Boot)

### Prerequisites

- Java 17 or later
- Maven 3.8 or later

### Configuration

1. **Clone the Repository**

    ```bash
    git clone <repository-url>
    cd root/backend
    ```

2. **Build the Project**

    ```bash
    mvn clean install
    ```

3. **Run the Application**

    ```bash
    mvn spring-boot:run
    ```

    The Spring Boot application will start on `http://localhost:8080`.

### Configuration Properties

The backend configuration is located in `src/main/resources/application.properties`. Ensure the following properties are correctly set:

```properties
# Application Name
spring.application.name=searchIOBackend

# Logging Levels
logging.level.root=INFO
logging.level.com.hackathon.searchIOBackend.controller=INFO
logging.level.org.springframework=ERROR
logging.level.org.apache.catalina=ERROR
logging.level.org.apache.tomcat=ERROR

# Stack Overflow API Configuration
stack.api.key=<YOUR_STACK_API_KEY>
stack.api.url=https://api.stackexchange.com/2.3
stack.api.q.url=https://stackoverflow.com/questions/
stack.max.urls=5

# GPT API Configuration
gpt.api.key=<YOUR_GPT_API_KEY>
gpt.api.url=https://api.openai.com/v1/chat/completions

gitlab.api.url=https://gitlab.com/api/v4
gitlab.api.token=<YOUR_GITLAB_API_TOKEN>

ubs.api.url=https://test.com
# Server Configuration

pdf.storage.dir=pdf-storage
api.upload.url=https://test.com
# API host
api.host=openai-nucleus-dev.azpriv-cloud.ubs.net
# Organization ID
api.organizationId=test_manual
# API token
api.token=<YOUR_API_TOKEN>

# URL for the talk2docs service API
openai.api.url=https://openai-nucleus-dev.azpriv-cloud.ubs.net/api/v1/talk2docs/service/chat/organizations/{organizationId}/documents
# Organization ID
openai.organization.id=your_organization_id
# API Key for authentication
openai.api.key=your_api_key

server.port=8080
```

Replace `<YOUR_STACK_API_KEY>`, `<YOUR_GPT_API_KEY>`, `<YOUR_GITLAB_API_TOKEN>`, and `<YOUR_API_TOKEN>` with the actual values.

### CORS Configuration

By default, CORS is enabled for `http://localhost:4200`. Modify the `WebConfig` or `SecurityConfig` classes if needed.

## Frontend Setup (Angular)

### Prerequisites

- Node.js (version 14 or later)
- Angular CLI (version 14 or later)

### Configuration

1. **Clone the Repository**

    ```bash
    git clone <repository-url>
    cd root/frontend
    ```

2. **Install Dependencies**

    ```bash
    npm install
    ```

3. **Serve the Application**

    ```bash
    ng serve
    ```

    The Angular application will start on `http://localhost:4200`.

### Configuration

Update the `src/environments/environment.ts` file to match your backend API configuration:

```typescript
export const environment = {
  production: false,
  apiUrl: 'http://localhost:8080'
};
```

For production builds, update `src/environments/environment.prod.ts`:

```typescript
export const environment = {
  production: true,
  apiUrl: 'http://your-production-api-url'
};
```

## Building the Project

To build both the backend and frontend:

1. **Build the Backend**

    ```bash
    cd backend
    mvn clean package
    ```

2. **Build the Frontend**

    ```bash
    cd frontend
    ng build --prod
    ```

    The built Angular files will be located in the `dist/` directory.

## Contributing

Feel free to fork the repository, make changes, and submit pull requests. Ensure your changes are well-tested and documented.

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contact

For any questions or support, please contact [Your Name] at [Your Email Address].
```

Replace `<repository-url>`, `[Your Name]`, and `[Your Email Address]` with the relevant information.