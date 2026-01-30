# API Configuration Guide

## Overview

This Next.js frontend application is configured to communicate with a Spring Boot backend API using environment variables. This guide explains how to configure the API URL for different environments.

## Environment Variable

The application uses the `NEXT_PUBLIC_API_URL` environment variable to configure the backend API base URL.

### Why NEXT_PUBLIC_?

In Next.js, environment variables prefixed with `NEXT_PUBLIC_` are exposed to the browser. This is required for client-side API calls.

## Configuration

### Development (Local)

For local development, the application defaults to `http://localhost:8080` if `NEXT_PUBLIC_API_URL` is not set.

To customize the API URL in development:

1. Create a `.env.local` file in the project root:
   ```bash
   cp .env.example .env.local
   ```

2. Edit `.env.local` and set your API URL:
   ```env
   NEXT_PUBLIC_API_URL=http://localhost:8080
   ```
   
   Or point to a remote development server:
   ```env
   NEXT_PUBLIC_API_URL=http://136.248.123.125:8080
   ```

3. Restart your development server:
   ```bash
   npm run dev
   ```

**Note:** `.env.local` is gitignored and will not be committed to the repository.

### Production (Vercel)

To configure the API URL on Vercel:

1. Go to your Vercel project dashboard
2. Navigate to **Settings** → **Environment Variables**
3. Add a new environment variable:
   - **Name:** `NEXT_PUBLIC_API_URL`
   - **Value:** `http://136.248.123.125:8080` (your production backend URL)
   - **Environment:** Select "Production", "Preview", and "Development" as needed
4. Click **Save**
5. Redeploy your application for the changes to take effect

### Other Deployment Platforms

For other platforms (Netlify, AWS, etc.), add the environment variable through their respective dashboards:

**Netlify:**
- Site Settings → Build & Deploy → Environment Variables

**AWS Amplify:**
- App Settings → Environment Variables

**Docker:**
```bash
docker run -e NEXT_PUBLIC_API_URL=http://136.248.123.125:8080 your-image
```

## Architecture

### Centralized API Configuration

All API calls go through a centralized configuration in `lib/api.ts`:

- **`API_BASE_URL`**: The base URL for all API requests
- **`getApiUrl(path)`**: Builds full API URLs from paths
- **`apiFetch(path, options)`**: Convenience wrapper around fetch

### API Client

The `lib/api-client.ts` module provides:

- **`apiClient.get(path, options)`**: GET requests
- **`apiClient.post(path, body, options)`**: POST requests
- **`apiClient.patch(path, body, options)`**: PATCH requests
- **`apiClient.delete(path, options)`**: DELETE requests

All methods automatically:
- Prefix the API base URL
- Include authentication headers
- Handle token refresh
- Parse responses

### Example Usage

```typescript
import { apiClient } from '@/lib/api-client'

// Fetch dashboard data
const data = await apiClient.get('/api/dashboard/monthly?month=2024-01')

// Create a new transaction
await apiClient.post('/api/txns', {
  accountId: 1,
  amountCents: 5000,
  direction: 'OUT',
  description: 'Coffee',
})
```

## Files Modified

### Created
- `lib/api.ts` - Centralized API configuration module
- `.env.example` - Example environment variables file
- `API-CONFIGURATION.md` - This documentation

### Modified
- `lib/api-client.ts` - Updated to use centralized API configuration
- `.gitignore` - Added `.env.local` exclusion

### Unchanged
- All page components continue to work without changes
- Authentication logic remains intact
- No endpoints were modified

## Testing

### Local Development

1. Start your backend (if running locally):
   ```bash
   cd backend
   ./mvnw spring-boot:run
   ```

2. Start the frontend:
   ```bash
   npm run dev
   ```

3. Open http://localhost:3000

### With Remote Backend

1. Set the environment variable:
   ```bash
   NEXT_PUBLIC_API_URL=http://136.248.123.125:8080 npm run dev
   ```

2. Or create `.env.local` with the URL and run:
   ```bash
   npm run dev
   ```

### Build Test

To verify the production build works:

```bash
# Build the application
npm run build

# Start production server
npm start
```

## Troubleshooting

### Error: "API URL not configured"

This error appears when:
- `NEXT_PUBLIC_API_URL` is not set in production
- The application is trying to make an API call

**Solution:** Set the environment variable as described above.

### CORS Errors

If you see CORS errors, ensure your backend is configured to allow requests from your frontend domain:

```java
// Spring Boot CORS configuration
@Configuration
public class CorsConfig {
    @Bean
    public WebMvcConfigurer corsConfigurer() {
        return new WebMvcConfigurer() {
            @Override
            public void addCorsMappings(CorsRegistry registry) {
                registry.addMapping("/**")
                    .allowedOrigins("http://localhost:3000", "https://your-domain.vercel.app")
                    .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH")
                    .allowCredentials(true);
            }
        };
    }
}
```

### Environment Variable Not Working

Remember:
- Restart the dev server after changing `.env.local`
- Redeploy your app after changing environment variables in production
- Environment variables must start with `NEXT_PUBLIC_` to be accessible in the browser

## Security Considerations

- Never commit `.env.local` or any file containing sensitive credentials
- Use different API URLs for development, staging, and production
- Ensure your backend has proper CORS configuration
- Keep API tokens secure and implement proper token rotation

## Support

For issues or questions:
1. Check the [Next.js Environment Variables documentation](https://nextjs.org/docs/basic-features/environment-variables)
2. Review the backend API logs for connection issues
3. Verify network connectivity between frontend and backend
