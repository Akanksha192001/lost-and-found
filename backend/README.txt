npm install
// Install necessary dependencies using npm
// Make sure to have Node.js and npm installed on your system
// You can then start the frontend application using the following command
npm start
// This will start the frontend application on http://localhost:3000 by default
// You can change the port by modifying the .env file in the root directory
// For more information on npm and Node.js, refer to their official documentation
// Node.js: https://nodejs.org/
// npm: https://www.npmjs.com/
// Happy coding!



mvn spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
// Run above command to start the backend with remote debugging enabled on port 5005
// Make sure to have Maven and Java installed on your system
// You can then attach your IDE debugger to localhost:5005

// To stop the backend, simply terminate the process in your terminal
// Ensure that your backend is running before starting the frontend application
// The backend should be accessible at http://localhost:8080 by default
// You can change the port by modifying the application.properties file in the resources folder
// For more information on Spring Boot and Maven, refer to their official documentation
// Spring Boot: https://spring.io/projects/spring-boot
// Maven: https://maven.apache.org/
// Happy coding!
